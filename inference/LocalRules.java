/* 
 * The MIT License
 *
 * Copyright 2019 The OpenNARS authors.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package nars.inference;

import nars.storage.Memory;
import nars.entity.*;
import nars.language.*;
import nars.io.Symbols;
import nars.main.Parameters;

/**
 * Directly process a task by a oldBelief, with only two Terms in both. In
 * matching, the new task is compared with all existing direct Tasks in that
 * Concept, to carry out:
 * <p>
 * revision: between judgments on non-overlapping evidence; revision: between
 * judgments; satisfy: between a Sentence and a Question/Goal; merge: between
 * items of the same type and stamp; conversion: between different inheritance
 * relations.
 */
public class LocalRules {

    /* -------------------- same contents -------------------- */
    /**
     * The task and belief have the same content
     * <p>
     * called in RuleTables.reason
     *
     * @param task The task
     * @param belief The belief
     * @param memory Reference to the memory
     */
    public static void match(Task task, Sentence belief, Memory memory) {
        Sentence sentence = (Sentence) task.getSentence().clone();
        //System.out.println("sentence: " + sentence.toString());
        if (sentence.isJudgment()) {
            //System.out.println("!!!");
            if (revisible(sentence, belief)) {
                revision(sentence, belief, true, memory);
            }
        }else{
            if(TemporalRules.matchingOrder(sentence, belief)){
                if (Variable.unify(Symbols.VAR_QUERY, sentence.getContent(), (Term) belief.getContent().clone())) {
                    trySolution(belief, task, memory);
                }
            }
        } 
    }

    /**
     * Check whether two sentences can be used in revision
     *
     * @param s1 The first sentence
     * @param s2 The second sentence
     * @return If revision is possible between the two sentences
     */
    public static boolean revisible(Sentence s1, Sentence s2) {        
        
        if(!s1.isEternal() && !s2.isEternal() && Math.abs(s1.getOccurrenceTime() - s2.getOccurrenceTime()) > Parameters.REVISION_MAX_OCCURRENCE_DISTANCE){
            return false;
        }
        
        return (s1.getContent().equals(s2.getContent()) && s1.getRevisible() 
                && !Stamp.baseOverlap(s1.getStamp(), s2.getStamp()));
        
    }

    /**
     * Belief revision
     * <p>
     * called from Concept.reviseTable and match
     *
     * @param newBelief The new belief in task
     * @param oldBelief The previous belief with the same content
     * @param feedbackToLinks Whether to send feedback to the links
     * @param memory Reference to the memory
     */
    public static void revision(Sentence newBelief, Sentence oldBelief, boolean feedbackToLinks, Memory memory) {                 
        
        TruthValue newTruth = newBelief.getTruth();
        TruthValue oldTruth = oldBelief.getTruth();

        float factor = 1;
        
        if(newBelief.isEternal() && !oldBelief.isEternal())
            oldTruth = TruthFunctions.eternalize(oldTruth);
        else if (!newBelief.isEternal() && oldBelief.isEternal())
            newTruth = TruthFunctions.eternalize(newTruth);
        else if(newBelief.getOccurrenceTime() != Stamp.ETERNAL && oldBelief.getOccurrenceTime() != Stamp.ETERNAL)
            factor = TruthFunctions.temporalProjection(oldBelief.getOccurrenceTime(), newBelief.getOccurrenceTime(), memory.getTime());
     
        oldTruth = oldTruth.setConfidence(factor * oldTruth.getConfidence());
        TruthValue truth = TruthFunctions.revision(newTruth, oldTruth);
        BudgetValue budget = BudgetFunctions.revise(newTruth, oldTruth, truth, feedbackToLinks, memory);
            
        long newInterval = 0;
        if(newBelief.getContent() instanceof Statement && oldBelief.getContent() instanceof Statement)
            newInterval = (((Statement)newBelief.getContent()).getInterval() + ((Statement)oldBelief.getContent()).getInterval())/2;
            
        Term content = newBelief.getContent();
        
        if(content instanceof Implication)
            ((Implication) content).setInterval(newInterval);                 

        Stamp deriveStamp = memory.getNewStamp().clone();
        memory.resetOccurrenceTime();
        
        if (content != null) {
            Sentence newSentence = new Sentence(content, newBelief.getPunctuation(), truth, deriveStamp);
            Concept c = memory.getConcept(content);
            
            if(newBelief.getPunctuation() == Symbols.JUDGMENT_MARK){
                c.addToTable(newSentence, c.getBeliefs(), Parameters.MAXIMUM_BELIEF_LENGTH);
                c.generateAnticipations(newSentence);
                c.generateGoalPreconditions(newSentence);                       
                
            }else if(newBelief.getPunctuation() == Symbols.GOAL_MARK){
                Task newTask = new Task(newSentence, budget, memory.currentTask, memory.currentBelief);               
                c.addToTable(newTask, c.getDesires(), Parameters.MAXIMUM_GOAL_LENGTH);
            }
        }
    }
    
    /**
     * Check if a Sentence provide a better answer to a Question or Goal
     *
     * @param belief The proposed answer
     * @param task The task to be processed
     * @param memory Reference to the memory
     */
    public static void trySolution(Sentence belief, Task task, Memory memory) {
        Sentence problem = task.getSentence();
        Sentence oldBest = task.getBestSolution();

        float newQ = solutionQuality(problem, belief, memory);
        if (oldBest != null) {
            float oldQ = solutionQuality(problem, oldBest, memory);
            if (oldQ >= newQ) {
                if(problem.isGoal() && memory.getEmotion() != null)
                    memory.getEmotion().adjustSatisfation(oldQ, newQ, memory);
                return;
            }
        }
        task.setBestSolution(belief);   
        
        //System.out.println("Task: " + task.getName());
        
        if (task.isInput() && task.getSentence().isQuestion()) {    // moved from Sentence
            System.out.println("2");
            memory.report(belief, false, true);
        }
        BudgetValue budget = BudgetFunctions.solutionEval(problem, belief, task, memory);
        if ((budget != null) && budget.aboveThreshold()) {
            memory.activatedTask(budget, belief, task.getParentBelief());
        }
    }

    /**
     * Evaluate the quality of the judgment as a solution to a problem
     *
     * @param problem A goal or question
     * @param solution The solution to be evaluated
     * @return The quality of the judgment as the solution
     */
    public static float solutionQuality(Sentence problem, Sentence solution, Memory memory) {
        // 如果问题为空，则返回solution的期望
        if (problem == null) {
            return solution.getTruth().getExpectation();
        }
        // 如果问题与答案的标识符不同
        // 并且答案带有疑问变量
        // 或者问题与答案的时序不match
        if((problem.getPunctuation() != solution.getPunctuation() && solution.getContent().hasVarQuery()) ||
                !TemporalRules.matchingOrder(problem.getTemporalOrder(), solution.getTemporalOrder()))
            return 0.0f;
        // 答案的真值
        TruthValue truth = solution.getTruth();
        // 如果问题与答案不在同一时间，答案按照问题时间做时间投影
        if(problem.getOccurrenceTime() != solution.getOccurrenceTime()){
            truth = solution.projectionTruth(problem.getOccurrenceTime(), memory.getTime(), memory);
            //solution.getStamp().setOccurrenceTime(problem.getOccurrenceTime());
        }
        // 如果问题本身包含问句，这是问what问题，返回真值的期望
        // 如果回答是不是的问题，或者problem不是一个问题，那么返回信心
        if (problem.containQueryVar()) {    // "what" question or goal
            return truth.getExpectation() / solution.getContent().getComplexity();
        } else {                                   // "yes/no" question
            return truth.getConfidence();
        }
    }

    /* -------------------- same terms, difference relations -------------------- */
    /**
     * The task and belief match reversely
     *
     * @param memory Reference to the memory
     */
    public static void matchReverse(Memory memory) {
        Task task = memory.currentTask;
        Sentence belief = memory.currentBelief;
        Sentence sentence = task.getSentence();
        //System.out.println("Sentence: " + sentence.getContent().getName());
        
        if(TemporalRules.matchingOrder(sentence.getTemporalOrder(), TemporalRules.reverseOrder(belief.getTemporalOrder()))){
            if (sentence.isJudgment()) {
                inferToSym((Sentence) sentence, belief, memory);
            } else {
                conversion(memory);
            }
        }
    }

    /**
     * Inheritance/Implication matches Similarity/Equivalence
     *
     * @param asym A Inheritance/Implication sentence
     * @param sym A Similarity/Equivalence sentence
     * @param figure location of the shared term
     * @param memory Reference to the memory
     */
    public static void matchAsymSym(Sentence asym, Sentence sym, int figure, Memory memory) {
        if (memory.currentTask.getSentence().isJudgment()) {
            inferToAsym((Sentence) asym, (Sentence) sym, memory);
        } else {
            convertRelation(memory);
        }
    }

    /* -------------------- two-premise inference rules -------------------- */
    /**
     * {<S --> P>, <P --> S} |- <S <-> p> Produce Similarity/Equivalence from a
     * pair of reversed Inheritance/Implication
     *
     * @param judgment1 The first premise
     * @param judgment2 The second premise
     * @param memory Reference to the memory
     */
    private static void inferToSym(Sentence judgment1, Sentence judgment2, Memory memory) {
        
        Statement s1 = (Statement) judgment1.getContent();
        
        Term t1 = null;
        Term t2 = null;
        
        if(s1.getTemporalOrder() == TemporalRules.ORDER_FORWARD){
            t1 = s1.getSubject();
            t2 = s1.getPredicate();
        }else if(s1.getTemporalOrder() == TemporalRules.ORDER_BACKWARD){
            t1 = s1.getPredicate();
            t2 = s1.getSubject();
        }
               
        Term content;
        if (s1 instanceof Inheritance) {
            content = Similarity.make(t1, t2, memory);
        } else {
            content = Equivalence.make(t1, t2, TemporalRules.ORDER_FORWARD, memory, s1.getInterval());
        }
        TruthValue value1 = judgment1.getTruth();
        TruthValue value2 = judgment2.getTruth();
        TruthValue truth = TruthFunctions.intersection(value1, value2);
        BudgetValue budget = BudgetFunctions.forward(truth, memory);
        memory.doublePremiseTask(content, truth, budget);
    }

    /**
     * {<S <-> P>, <P --> S>} |- <S --> P> Produce an Inheritance/Implication
     * from a Similarity/Equivalence and a reversed Inheritance/Implication
     *
     * @param asym The asymmetric premise
     * @param sym The symmetric premise
     * @param memory Reference to the memory
     */
    private static void inferToAsym(Sentence asym, Sentence sym, Memory memory) {
        Statement statement = (Statement) asym.getContent();
        Term sub = statement.getPredicate();
        Term pre = statement.getSubject();
        Statement content = Statement.make(statement, sub, pre, statement.getTemporalOrder(), memory);
        TruthValue truth = TruthFunctions.reduceConjunction(sym.getTruth(), asym.getTruth());
        BudgetValue budget = BudgetFunctions.forward(truth, memory);
        memory.doublePremiseTask(content, truth, budget);
    }

    /* -------------------- one-premise inference rules -------------------- */
    /**
     * {<P --> S>} |- <S --> P> Produce an Inheritance/Implication from a
     * reversed Inheritance/Implication
     *
     * @param memory Reference to the memory
     */
    private static void conversion(Memory memory) {
        TruthValue truth = TruthFunctions.conversion(memory.currentBelief.getTruth());
        BudgetValue budget = BudgetFunctions.forward(truth, memory);
        convertedJudgment(truth, budget, memory);
    }

    /**
     * {<S --> P>} |- <S <-> P> {<S <-> P>} |- <S --> P> Switch between
     * Inheritance/Implication and Similarity/Equivalence
     *
     * @param memory Reference to the memory
     */
    private static void convertRelation(Memory memory) {
        TruthValue truth = memory.currentBelief.getTruth();
        if (((Statement) memory.currentTask.getContent()).isCommutative()) {
            truth = TruthFunctions.abduction(truth, 1.0f);
        } else {
            truth = TruthFunctions.deduction(truth, 1.0f);
        }
        BudgetValue budget = BudgetFunctions.forward(truth, memory);
        convertedJudgment(truth, budget, memory);
    }

    /**
     * Convert judgment into different relation
     * <p>
     * called in MatchingRules
     *
     * @param budget The budget value of the new task
     * @param truth The truth value of the new task
     * @param memory Reference to the memory
     */
    private static void convertedJudgment(TruthValue newTruth, BudgetValue newBudget, Memory memory) {
        Statement content = (Statement) memory.currentTask.getContent();
        Statement beliefContent = (Statement) memory.currentBelief.getContent();
        Term subjT = content.getSubject();
        Term predT = content.getPredicate();
        Term subjB = beliefContent.getSubject();
        Term predB = beliefContent.getPredicate();
        Term otherTerm;
        
        int order = TemporalRules.reverseOrder(beliefContent.getTemporalOrder());
        
        if (Variable.containVarQuery(subjT.getName())) {
            otherTerm = (predT.equals(subjB)) ? predB : subjB;
            content = Statement.make(content, otherTerm, predT, order, memory);
        }
        if (Variable.containVarQuery(predT.getName())) {
            otherTerm = (subjT.equals(subjB)) ? predB : subjB;
            content = Statement.make(content, subjT, otherTerm, order, memory);
        }
        memory.singlePremiseTask(content, Symbols.JUDGMENT_MARK, newTruth, newBudget);
    }
}
