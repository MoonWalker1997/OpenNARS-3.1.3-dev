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

import nars.entity.*;
import nars.language.*;
import nars.io.Symbols;
import nars.storage.Memory;

/**
 * Syllogisms: Inference rules based on the transitivity of the relation.
 */
public final class SyllogisticRules {

    /* --------------- rules used in both first-tense inference and higher-tense inference --------------- */
    /**
     * <pre>
     * {<S ==> M>, <M ==> P>} |- {<S ==> P>, <P ==> S>}
     * </pre>
     *
     * @param term1 Subject of the first new task
     * @param term2 Predicate of the first new task
     * @param sentence The first premise
     * @param belief The second premise
     * @param memory Reference to the memory
     */
    static void dedExe(Term term1, Term term2, Sentence sentence, Sentence belief, Memory memory) {
        if (Statement.invalidStatement(term1, term2)) {
            return;
        }
        TruthValue value1 = sentence.getTruth();
        TruthValue value2 = belief.getTruth();
        TruthValue truth1 = null;
        TruthValue truth2 = null;
        int order1 = sentence.getTemporalOrder();
        int order2 = belief.getTemporalOrder();
        
        int order = TemporalRules.dedExeOrder(order1, order2);
        
        if(order == TemporalRules.ORDER_INVALID)
            return;
        
        BudgetValue budget1, budget2;
        
        if(!(sentence.isQuestion() || sentence.isQuest())){
            
            if(sentence.isGoal()){
                truth1 = TruthFunctions.desireWeak(value1, value2);
                truth2 = TruthFunctions.desireWeak(value1, value2);
            }else{
                truth1 = TruthFunctions.deduction(value1, value2);
                truth2 = TruthFunctions.exemplification(value1, value2);
            }
            
        }
        
        if (sentence.isQuestion()) {
            budget1 = BudgetFunctions.backwardWeak(value2, memory);
            budget2 = BudgetFunctions.backwardWeak(value2, memory);
        } else if (sentence.isQuest()){
            budget1 = BudgetFunctions.backward(truth2, memory);
            budget2 = BudgetFunctions.backward(truth2, memory);  
        }else {
            budget1 = BudgetFunctions.forward(truth1, memory);
            budget2 = BudgetFunctions.forward(truth2, memory);
        }
        
        Statement content = (Statement) sentence.getContent();
        Statement content1 = Statement.make(content, term1, term2, order, memory);
        Statement content2 = Statement.make(content, term2, term1, TemporalRules.reverseOrder(order), memory);
        
        long interval = 0;
        if(term1 instanceof Statement && term2 instanceof Statement && (order != TemporalRules.ORDER_NONE || order != TemporalRules.ORDER_INVALID))
            interval = ((Statement)term1).getInterval() + ((Statement)term2).getInterval();
        
        if(interval > 0){
            content1.setInterval(interval);
            content2.setInterval(interval);
        }
            
        
        if(content1 == null || content2 == null)
            return;
        
        memory.doublePremiseTask(content1, truth1, budget1);
        memory.doublePremiseTask(content2, truth2, budget2);
    }

    /**
     * {<M ==> S>, <M ==> P>} |- {<S ==> P>, <P ==> S>, <S <=> P>}
     *
     * @param term1 如果figure是11 那么term1为第一句的谓语，如果figure是22，那么term1为第一句的主语
     * @param term2 如果figure是11 那么term1为第二句的谓语，如果figure是22，那么term2为第一句的主语
     * @param taskSentence The first premise
     * @param belief The second premise
     * @param figure Locations of the shared term in premises
     * @param memory Reference to the memory
     */
    static void abdIndCom(Term term1, Term term2, Sentence taskSentence, Sentence belief, int figure, Memory memory) {
        if (Statement.invalidStatement(term1, term2) || Statement.invalidPair(term1.getName(), term2.getName())) {
            return;
        }
        Statement taskContent = (Statement) taskSentence.getContent();
        TruthValue truth1 = null;
        TruthValue truth2 = null;
        TruthValue truth3 = null;
        BudgetValue budget1, budget2, budget3;
        TruthValue value1 = taskSentence.getTruth();
        TruthValue value2 = belief.getTruth();
        
        int order1 = taskSentence.getTemporalOrder();
        int order2 = belief.getTemporalOrder();
        int order = TemporalRules.abdIndComOrder(order1, order2);
        
        if(order == TemporalRules.ORDER_INVALID)
            return;
        
        if(taskSentence.isGoal()){
            truth1 = TruthFunctions.desireStrong(value1, value2);
            truth2 = TruthFunctions.desireWeak(value2, value1);
            truth3 = TruthFunctions.desireStrong(value1, value2);
        }else if(taskSentence.isJudgment()){
            truth1 = TruthFunctions.abduction(value1, value2);
            truth2 = TruthFunctions.abduction(value2, value1);
            truth3 = TruthFunctions.comparison(value1, value2);                       
        }
        
        if (taskSentence.isQuestion()) {
            budget1 = BudgetFunctions.backward(value2, memory);
            budget2 = BudgetFunctions.backwardWeak(value2, memory);
            budget3 = BudgetFunctions.backward(value2, memory);
        } else if(taskSentence.isQuest()){
            budget1 = BudgetFunctions.backwardWeak(value2, memory);
            budget2 = BudgetFunctions.backward(value2, memory);
            budget3 = BudgetFunctions.backwardWeak(value2, memory); 
        }else {
            budget1 = BudgetFunctions.forward(truth1, memory);
            budget2 = BudgetFunctions.forward(truth2, memory);
            budget3 = BudgetFunctions.forward(truth3, memory);
        }
        
        long interval = 0;
        if(term1 instanceof Statement && term2 instanceof Statement && (order != TemporalRules.ORDER_NONE || order != TemporalRules.ORDER_INVALID))
            interval = ((Statement)term1).getInterval() + ((Statement)term2).getInterval();
        
        if(order != TemporalRules.ORDER_INVALID){
            Statement statement1 = Statement.make(taskContent, term1, term2, order, memory);
            Statement statement2 = Statement.make(taskContent, term2, term1, TemporalRules.reverseOrder(order), memory);
            Statement statement3 = Statement.makeSym(taskContent, term1, term2, order, memory);
            statement1.setInterval(interval);
            statement2.setInterval(interval);
            statement3.setInterval(interval);
            memory.doublePremiseTask(statement1, truth1, budget1);
            memory.doublePremiseTask(statement2, truth2, budget2);
            memory.doublePremiseTask(statement3, truth3, budget3);
        }
    }

    /**
     * {<S ==> P>, <M <=> P>} |- <S ==> P>
     *
     * @param subj Subject of the new task
     * @param pred Predicate of the new task
     * @param asym The asymmetric premise
     * @param sym The symmetric premise
     * @param figure Locations of the shared term in premises
     * @param memory Reference to the memory
     */
    static void analogy(Term subj, Term pred, Sentence asym, Sentence sym, int figure, Memory memory) {
        if (Statement.invalidStatement(subj, pred)) {
            return;
        }
        Statement st = (Statement) asym.getContent();
        Statement st2 = (Statement) sym.getContent();
        TruthValue truth = null;
        BudgetValue budget;
        Sentence sentence = memory.currentTask.getSentence();
        CompoundTerm taskTerm = (CompoundTerm) sentence.getContent();
        
        int order1 = asym.getTemporalOrder();
        int order2 = sym.getTemporalOrder();
        int order = TemporalRules.analogyOrder(order1, order2, figure);
        
        if(order == TemporalRules.ORDER_INVALID)
            return;
        
        if (sentence.isQuestion() || sentence.isQuest()) {
            if (taskTerm.isCommutative()) {
                budget = BudgetFunctions.backwardWeak(asym.getTruth(), memory);
            } else {
                budget = BudgetFunctions.backward(sym.getTruth(), memory);
            }
        } else {
            if(sentence.isGoal()){
                truth = TruthFunctions.analogy(asym.getTruth(), sym.getTruth());
            }else{
                truth = TruthFunctions.analogy(asym.getTruth(), sym.getTruth());
            }
            budget = BudgetFunctions.forward(truth, memory);
        }
        
        
        long interval = 0;
        if((order != TemporalRules.ORDER_NONE || order != TemporalRules.ORDER_INVALID))
            interval = st.getInterval() + st2.getInterval();

        Term content = Statement.make(st, subj, pred, order, memory);
        ((Statement)content).setInterval(interval);
        memory.doublePremiseTask(content, truth, budget);
    }

    /**
     * {<S <=> M>, <M <=> P>} |- <S <=> P>
     *
     * @param term1 Subject of the new task
     * @param term2 Predicate of the new task
     * @param belief The first premise
     * @param sentence The second premise
     * @param figure Locations of the shared term in premises
     * @param memory Reference to the memory
     */
    static void resemblance(Term term1, Term term2, Sentence belief, Sentence sentence, int figure, Memory memory) {
        if (Statement.invalidStatement(term1, term2)) {
            return;
        }
        
        int order1 = belief.getTemporalOrder();
        int order2 = belief.getTemporalOrder();
        int order = TemporalRules.resemblanceOrder(order1, order2, figure);
        
        if(order == TemporalRules.ORDER_INVALID)
            return;
        
        Statement st = (Statement) belief.getContent();
        TruthValue truth = null;
        BudgetValue budget;
        
        if (!(sentence.isQuestion() || sentence.isQuest())) {
            if (sentence.isGoal()) {
                truth = TruthFunctions.desireStrong(sentence.getTruth(), belief.getTruth());
            } else {
                // NOTE< this must be Judgement again ? >
                truth = TruthFunctions.resemblance(belief.getTruth(), sentence.getTruth());
            }
        }
        
        
        if (sentence.isQuestion() || sentence.isQuest()) {
            budget = BudgetFunctions.backward(belief.getTruth(), memory);
        } else {
            budget = BudgetFunctions.forward(truth, memory);
        }
        
        long interval = 0;
        if(term1 instanceof Statement && term2 instanceof Statement && (order != TemporalRules.ORDER_NONE || order != TemporalRules.ORDER_INVALID))
            interval = ((Statement)term1).getInterval() + ((Statement)term2).getInterval();
        
        Term statement = Statement.make(st, term1, term2, order, memory);
        ((Statement)statement).setInterval(interval);
        memory.doublePremiseTask(statement, truth, budget);
    }

    /* --------------- rules used only in conditional inference --------------- */
    /**
     * {<<M --> S> ==> <M --> P>>, <M --> S>} |- <M --> P> 
     * {<<M --> S> ==> <M --> P>>, <M --> P>} |- <M --> S> 
     * {<<M --> S> <=> <M --> P>>, <M --> S>} |- <M --> P> 
     * {<<M --> S> <=> <M --> P>>, <M --> P>} |- <M --> S>
     *
     * @param mainSentence The implication/equivalence premise
     * @param subSentence The premise on part of s1
     * @param side The location of s2 in s1
     * @param memory Reference to the memory
     * @param anticipation
     * @return 
     */
    public static Task detachment(Sentence mainSentence, Sentence subSentence, int side, Memory memory, boolean anticipation) {
        // 主要语句
        
        Statement statement = (Statement) mainSentence.getContent();
        // 如果语句不是implication 也不是 equivalence，直接返回
        if (!(statement instanceof Implication) && !(statement instanceof Equivalence)) {
            return null;
        }

        // 语句的主语
        Term subject = statement.getSubject();
        // 语句的谓语
        Term predicate = statement.getPredicate();
        Term content;
        // 主语的词项
        Term term = subSentence.getContent();
        // 如果subSentence在主句中的位置是主语
        // {<<M --> S> ==> <M --> P>>, <M --> S>} |- <M --> P> 
        // {<<M --> S> <=> <M --> P>>, <M --> S>} |- <M --> P> 
        if ((side == 0) && term.equals(subject)) {
            // 将谓语赋值给content
            content = predicate;
        // 如果subSentence在主句中的位置是谓语
        // {<<M --> S> ==> <M --> P>>, <M --> P>} |- <M --> S> 
        // {<<M --> S> <=> <M --> P>>, <M --> P>} |- <M --> S>
        } else if ((side == 1) && term.equals(predicate)) {
            // 将谓语赋值给content
            content = subject;
        // 啥也不是，返回
        } else {
            return null;
        }
        // 如果content是一个语句，并且content不合法，返回
        if ((content instanceof Statement) && ((Statement) content).invalid()) {
            return null;
        }
        
        if(mainSentence.getTemporalOrder() != TemporalRules.ORDER_INVALID || mainSentence.getTemporalOrder() != TemporalRules.ORDER_NONE){
            
            if(subSentence.getPunctuation() == Symbols.GOAL_MARK){
                
                if(mainSentence.getTemporalOrder() == TemporalRules.ORDER_FORWARD && side == 0)
                    return null;
                else if(mainSentence.getTemporalOrder() == TemporalRules.ORDER_BACKWARD && side == 1)
                    return null;
                
            }
            
        }
        
        if(mainSentence.getTemporalOrder() == TemporalRules.ORDER_NONE && subSentence.getPunctuation() == Symbols.GOAL_MARK)
            return null;
        
        // 当前的任务语句
        Sentence taskSentence = memory.currentTask.getSentence();
        // 当前的信念语句
        Sentence beliefSentence = memory.currentBelief;
        // 信念真值
        TruthValue beliefTruth = beliefSentence.getTruth();
        // 主句的真值
        TruthValue truth1 = mainSentence.getTruth();
        // 次句的真值
        TruthValue truth2 = subSentence.getTruth();
        TruthValue truth = null;
        BudgetValue budget;
        // 假如任务是一个问题
        boolean strong = false;
        
        if (!(taskSentence.isQuestion() || taskSentence.isQuest())) {
            if (taskSentence.isGoal()) {
                strong = statement instanceof Equivalence || side != 0;
            } else { // isJudgment
                strong = statement instanceof Equivalence || side == 0;
            }
        }

        if (!(taskSentence.isQuestion() || taskSentence.isQuest())) {
            if (taskSentence.isGoal()) {
                if (statement instanceof Equivalence) {
                    truth = TruthFunctions.desireStrong(truth1, truth2);
                } else if (side == 0) {
                    truth = TruthFunctions.desireInd(truth1, truth2);
                } else {
                    truth = TruthFunctions.desireDed(truth1, truth2);
                }
            } else { // isJudgment
                if (statement instanceof Equivalence) {
                    truth = TruthFunctions.analogy(truth2, truth1);
                } else if (side == 0) {
                    truth = TruthFunctions.deduction(truth1, truth2);
                } else {
                    truth = TruthFunctions.abduction(truth2, truth1);
                }
            }
        }

        if (taskSentence.isQuestion()) {
            if (statement instanceof Equivalence) {
                budget = BudgetFunctions.backward(beliefTruth, memory);
            } else if (side == 0) {
                budget = BudgetFunctions.backwardWeak(beliefTruth, memory);
            } else {
                budget = BudgetFunctions.backward(beliefTruth, memory);
            }
        } else if (taskSentence.isQuest()) {
            if (statement instanceof Equivalence) {
                budget = BudgetFunctions.backwardWeak(beliefTruth, memory);
            } else if (side == 0) {
                budget = BudgetFunctions.backward(beliefTruth, memory);
            } else {
               budget = BudgetFunctions.backwardWeak(beliefTruth, memory);
            }
        } else {
            budget = BudgetFunctions.forward(truth, memory);
        }
               
        int order = statement.getTemporalOrder();
        long occurrenceTime = subSentence.getStamp().getOccurrenceTime();
        
        if((order != TemporalRules.ORDER_NONE) && (order != TemporalRules.ORDER_INVALID) && occurrenceTime != Stamp.ETERNAL){
            
            // {<<M --> S> ==> <M --> P>>, <M --> S>} |- <M --> P> 
            // {<<M --> S> <=> <M --> P>>, <M --> S>} |- <M --> P> 
            if ((side == 0) && term.equals(subject) && order == TemporalRules.ORDER_FORWARD) {
            // 将谓语赋值给content
                occurrenceTime += statement.getInterval();
            // 如果subSentence在主句中的位置是谓语
            // {<<M --> S> ==> <M --> P>>, <M --> P>} |- <M --> S> 
            // {<<M --> S> <=> <M --> P>>, <M --> P>} |- <M --> S>
            } else if ((side == 1) && term.equals(predicate)) {
            // 将谓语赋值给content
                if(order == TemporalRules.ORDER_FORWARD)
                    occurrenceTime -= statement.getInterval();
                else
                    occurrenceTime += statement.getInterval();
            // 啥也不是，返回
            } 
                
        }
        
        if(occurrenceTime != Stamp.ETERNAL){
            memory.newStamp = new Stamp(memory.currentTask.getStamp(), memory.currentBelief.getStamp(), memory.getTime());
            memory.newStamp.setOccurrenceTime(occurrenceTime);
        }
        else{
            memory.newStamp = new Stamp(memory.currentTask.getStamp(), memory.currentBelief.getStamp(), memory.getTime());
            memory.newStamp.setOccurrenceTime(Stamp.ETERNAL);
        }
        
        Task newTask;
        
        if(anticipation){          
            Sentence newSentence = new Sentence(content, memory.currentTask.getSentence().getPunctuation(), truth, memory.getNewStamp());
            newTask = new Task(newSentence, budget, memory.currentTask, memory.currentBelief);
        }else{
            newTask = memory.doublePremiseTask(content, truth, budget);
        }
        
        return newTask;
    }

    /**
     * {<(&&, S1, S2, S3) ==> P>, S1} |- <(&&, S2, S3) ==> P> {<(&&, S2, S3) ==>
     * P>, <S1 ==> S2>} |- <(&&, S1, S3) ==> P> {<(&&, S1, S3) ==> P>, <S1 ==>
     * S2>} |- <(&&, S2, S3) ==> P>
     *
     * @param premise1 The conditional premise
     * @param index The location of the shared term in the condition of premise1
     * @param premise2 The premise which, or part of which, appears in the
     * condition of premise1
     * @param side The location of the shared term in premise2: 0 for subject, 1
     * for predicate, -1 for the whole term
     * @param memory Reference to the memory
     */
    static void conditionalDedInd(Implication premise1, short index, Term premise2, int side, Memory memory) {
        
        Task task = memory.currentTask;
        Sentence taskSentence = task.getSentence();
        Sentence belief = memory.currentBelief;
        boolean conditionalTask = Variable.hasSubstitute(Symbols.VAR_INDEPENDENT, premise2, belief.getContent());
        Term commonComponent;
        Term newComponent = null;
        switch (side) {
            case 0:
                commonComponent = ((Statement) premise2).getSubject();
                newComponent = ((Statement) premise2).getPredicate();
                break;
            case 1:
                commonComponent = ((Statement) premise2).getPredicate();
                newComponent = ((Statement) premise2).getSubject();
                break;
            default:
                commonComponent = premise2;
                break;
        }
        Term tm = premise1.getSubject();
        if (!(tm instanceof Conjunction))
            return;
        Conjunction oldCondition = (Conjunction) tm;
        boolean match = Variable.unify(Symbols.VAR_DEPENDENT, oldCondition.componentAt(index), commonComponent, premise1, premise2);
        //System.out.println("28");
        if (!match && (commonComponent.getClass() == oldCondition.getClass())) {
            match = Variable.unify(Symbols.VAR_DEPENDENT, oldCondition.componentAt(index), ((CompoundTerm) commonComponent).componentAt(index), premise1, premise2);
            //System.out.println("29");
        }
        if (!match) {
            return;
        }
        
        int conjunctionOrder = oldCondition.getTemporalOrder();
        
        if(conjunctionOrder == TemporalRules.ORDER_FORWARD){
        
            if(index > 0)
                return;
            
            if(side == 0 && premise2.getTemporalOrder() == TemporalRules.ORDER_FORWARD)
                return;
            
            if(side == 1 && premise2.getTemporalOrder() == TemporalRules.ORDER_BACKWARD)
                return;
            
        }
            
        
        Term newCondition;
        if (oldCondition.equals(commonComponent)) {
            newCondition = null;
        } else {
            newCondition = CompoundTerm.setComponent(oldCondition, index, newComponent, memory);
        }
        Term content;
        if (newCondition != null) {
            content = Statement.make(premise1, newCondition, premise1.getPredicate(), premise1.getTemporalOrder(), memory);
            if(content != null)
                ((Statement)content).setInterval(premise1.getInterval());
        } else {
            content = premise1.getPredicate();
        }
        if (content == null) {
            return;
        }
        TruthValue truth1 = taskSentence.getTruth();
        TruthValue truth2 = belief.getTruth();
        TruthValue truth = null;
        BudgetValue budget;
        if (taskSentence.isQuestion() || taskSentence.isQuest()) {
            budget = BudgetFunctions.backwardWeak(truth2, memory);
        } else {
            if (conditionalTask) {
                truth = TruthFunctions.comparison(truth1, truth2);
            } else {
                truth = TruthFunctions.analogy(truth1, truth2);
            }
            budget = BudgetFunctions.forward(truth, memory);
        }
        memory.doublePremiseTask(content, truth, budget, false);
        
    }

    /**
     * {<(&&, S1, S2) <=> P>, (&&, S1, S2)} |- P
     *
     * @param premise1 The equivalence premise
     * @param index The location of the shared term in the condition of premise1
     * @param premise2 The premise which, or part of which, appears in the
     * condition of premise1
     * @param side The location of the shared term in premise2: 0 for subject, 1
     * for predicate, -1 for the whole term
     * @param memory Reference to the memory
     */
    static void conditionalAna(Equivalence premise1, short index, Term premise2, int side, Memory memory) {
        Task task = memory.currentTask;
        Sentence taskSentence = task.getSentence();
        Sentence belief = memory.currentBelief;
        //System.out.println("faksdhfasd");
        boolean conditionalTask = Variable.hasSubstitute(Symbols.VAR_INDEPENDENT, premise2, belief.getContent());
        Term commonComponent;
        Term newComponent = null;
        if (side == 0) {
            commonComponent = ((Statement) premise2).getSubject();
            newComponent = ((Statement) premise2).getPredicate();
        } else if (side == 1) {
            commonComponent = ((Statement) premise2).getPredicate();
            newComponent = ((Statement) premise2).getSubject();
        } else {
            commonComponent = premise2;
        }
        Term tm = premise1.getSubject();
        if (!(tm instanceof Conjunction))
            return;
        Conjunction oldCondition = (Conjunction) tm;
        boolean match = Variable.unify(Symbols.VAR_DEPENDENT, oldCondition.componentAt(index), commonComponent, premise1, premise2);
        if (!match && (commonComponent.getClass() == oldCondition.getClass())) {
            match = Variable.unify(Symbols.VAR_DEPENDENT, oldCondition.componentAt(index), ((CompoundTerm) commonComponent).componentAt(index), premise1, premise2);
        }
        if (!match) {
            return;
        }
        Term newCondition;
        if (oldCondition.equals(commonComponent)) {
            newCondition = null;
        } else {
            newCondition = CompoundTerm.setComponent(oldCondition, index, newComponent, memory);
        }
        Term content;
        if (newCondition != null) {
            content = Statement.make(premise1, newCondition, premise1.getPredicate(), premise1.getTemporalOrder(), memory);
        } else {
            content = premise1.getPredicate();
        }
        if (content == null) {
            return;
        }
        TruthValue truth1 = taskSentence.getTruth();
        TruthValue truth2 = belief.getTruth();
        TruthValue truth = null;
        BudgetValue budget;
        if (taskSentence.isQuestion()) {
            budget = BudgetFunctions.backwardWeak(truth2, memory);
        } else {
            if (conditionalTask) {
                truth = TruthFunctions.comparison(truth1, truth2);
            } else {
                truth = TruthFunctions.analogy(truth1, truth2);
            }
            budget = BudgetFunctions.forward(truth, memory);
        }
        memory.doublePremiseTask(content, truth, budget);
    }

    /**
     * {<(&&, S2, S3) ==> P>, <(&&, S1, S3) ==> P>} |- <S1 ==> S2>
     *
     * @param cond1 The condition of the first premise
     * @param cond2 The condition of the second premise
     * @param taskContent The first premise
     * @param st2 The second premise
     * @param memory Reference to the memory
     * @return Whether there are derived tasks
     */
    static boolean conditionalAbd(Term cond1, Term cond2, Statement st1, Statement st2, Memory memory) {
        if (!(st1 instanceof Implication) || !(st2 instanceof Implication)) {
            return false;
        }
        if (!(cond1 instanceof Conjunction) && !(cond2 instanceof Conjunction)) {
            return false;
        }
        Term term1 = null;
        Term term2 = null;
//        if ((cond1 instanceof Conjunction) && !Variable.containVarDep(cond1.getName())) {
        if (cond1 instanceof Conjunction) {
            term1 = CompoundTerm.reduceComponents((Conjunction) cond1, cond2, memory);
        }
//        if ((cond2 instanceof Conjunction) && !Variable.containVarDep(cond2.getName())) {
        if (cond2 instanceof Conjunction) {
            term2 = CompoundTerm.reduceComponents((Conjunction) cond2, cond1, memory);
        }
        if ((term1 == null) && (term2 == null)) {
            return false;
        }
        Task task = memory.currentTask;
        Sentence sentence = task.getSentence();
        Sentence belief = memory.currentBelief;
        TruthValue value1 = sentence.getTruth();
        TruthValue value2 = belief.getTruth();
        Term content;
        TruthValue truth = null;
        BudgetValue budget;
        if (term1 != null) {
            if (term2 != null) {
                content = Statement.make(st2, term2, term1, st2.getTemporalOrder(), memory);
            } else {
                content = term1;
            }
            if (sentence.isQuestion()) {
                budget = BudgetFunctions.backwardWeak(value2, memory);
            } else {
                truth = TruthFunctions.abduction(value2, value1);
                budget = BudgetFunctions.forward(truth, memory);
            }
            memory.doublePremiseTask(content, truth, budget);
        }
        if (term2 != null) {
            if (term1 != null) {
                content = Statement.make(st1, term1, term2, st1.getTemporalOrder(), memory);
            } else {
                content = term2;
            }
            if (sentence.isQuestion()) {
                budget = BudgetFunctions.backwardWeak(value2, memory);
            } else {
                truth = TruthFunctions.abduction(value1, value2);
                budget = BudgetFunctions.forward(truth, memory);
            }
            memory.doublePremiseTask(content, truth, budget);
        }
        return true;
    }

    /**
     * {(&&, <#x() --> S>, <#x() --> P>>, <M --> P>} |- <M --> S>
     *
     * @param compound The compound term to be decomposed
     * @param component The part of the compound to be removed
     * @param compoundTask Whether the compound comes from the task
     * @param memory Reference to the memory
     */
    static void elimiVarDep(CompoundTerm compound, Term component, boolean compoundTask, int index, Memory memory) {
        Term content = CompoundTerm.reduceComponents(compound, component, memory);
        if ((content == null) || ((content instanceof Statement) && ((Statement)content).invalid()))
            return;
        
        if(compound.getTemporalOrder() == TemporalRules.ORDER_FORWARD)
            return;
        
        Task task = memory.currentTask;
        Sentence sentence = task.getSentence();
        Sentence belief = memory.currentBelief;
        TruthValue v1 = sentence.getTruth();
        TruthValue v2 = belief.getTruth();
        TruthValue truth = null;
        BudgetValue budget;
        if (sentence.isQuestion() || sentence.isQuest()) {
            budget = (compoundTask ? BudgetFunctions.backward(v2, memory) : BudgetFunctions.backwardWeak(v2, memory));
        } else {
            truth = (compoundTask ? TruthFunctions.anonymousAnalogy(v1, v2) : TruthFunctions.anonymousAnalogy(v2, v1));
            budget = BudgetFunctions.compoundForward(truth, content, memory);
        }
        
        /*long occurrenceTime = compoundTask ? memory.currentTask.getStamp().getOccurrenceTime() + ((Conjunction)compound).getInterval().get(0) 
                : memory.currentBelief.getStamp().getOccurrenceTime() + ((Conjunction)compound).getInterval().get(0);*/   
        
        //memory.getNewStamp().setOccurrenceTime(occurrenceTime);
        memory.doublePremiseTask(content, truth, budget);
    }
}
