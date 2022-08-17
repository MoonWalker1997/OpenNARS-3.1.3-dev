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
package nars.entity;

import java.util.ArrayList;
import nars.inference.BudgetFunctions;
import nars.inference.LocalRules;
import nars.inference.RuleTables;
import nars.inference.StructuralRules;
import nars.inference.TemporalRules;
import nars.inference.TruthFunctions;
import nars.inference.UtilityFunctions;
import nars.io.Symbols;
import nars.language.CompoundTerm;
import nars.language.Conjunction;
import nars.language.Equivalence;
import nars.language.Implication;
import nars.language.Negation;
import nars.language.Term;
import nars.main.NARSBatch;
import nars.main.Parameters;
import nars.storage.Memory;
import nars.storage.TaskLinkBag;
import nars.storage.TermLinkBag;

/**
 * A concept contains information associated with a term, including directly and
 * indirectly related tasks and beliefs.
 * <p>
 * To make sure the space will be released, the only allowed reference to a
 * concept are those in a ConceptBag. All other access go through the Term that
 * names the concept.
 */
public final class Concept extends Item {

    /**
     * The term is the unique ID of the concept
     */
    private final Term term;
    /**
     * Task links for indirect processing
     */
    private final TaskLinkBag taskLinks;
    /**
     * Term links between the term and its components and compounds
     */
    private final TermLinkBag termLinks;
    /**
     * Link templates of TermLink, only in concepts with CompoundTerm jmv TODO
     * explain more
     */
    private final ArrayList<TermLink> termLinkTemplates;
    /**
     * Question directly asked about the term
     */
    private final ArrayList<Task> questions;
    /**
     * Sentences directly made about the term, with non-future tense
     */
    private final ArrayList<Sentence> beliefs;
    
    //private final ArrayList<Task> anticipationList;
    private final ArrayList<Concept> anticipationList;

    private final ArrayList<Task> quests;
    
    private final ArrayList<Task> desires;
    
    private ArrayList<Concept> executable_preconditions;
    
    private ArrayList<Task> general_executable_preconditions;
    
    public boolean observable = false;

    //public List<AnticipationEntry> anticipations = new ArrayList<>();
    /**
     *  Reference to memory where this concept is
     */
    private Memory memory;
    /**
     * The display window
     */
    private EntityObserver entityObserver;

    /**
     * Constructor, called in Memory.getConcept only
     *
     * @param tm A term corresponding to the concept
     * @param memory A reference to the memory
     */
    public Concept(Term tm, Memory memory) {
        super(tm.getName());
        term = tm;
        this.memory = memory;
        questions = new ArrayList();
        beliefs = new ArrayList();
        taskLinks = new TaskLinkBag(memory);
        termLinks = new TermLinkBag(memory);
        quests = new ArrayList();
        desires = new ArrayList();
        anticipationList = new ArrayList();
        this.executable_preconditions = new ArrayList();
        this.general_executable_preconditions = new ArrayList();
        
        if (tm instanceof CompoundTerm) {
            termLinkTemplates = ((CompoundTerm) tm).prepareComponentLinks();
        }else{
            termLinkTemplates = null;
        }
        
        this.entityObserver = new NullEntityObserver(); // For future use
    }

    /**
     * Directly process a new task. Called exactly once on each task. Using
     * local information and finishing in a constant time. Provide feedback in
     * the taskBudget value of the task.
     * <p>
     * called in Memory.immediateProcess only
     *
     * @param task The task to be processed
     */
    public void directProcess(Task task) {
        
        if (task.getSentence().isJudgment()) {
            processJudgment(task);
        } else if(task.getSentence().isQuestion() || task.getSentence().isQuest()){
            processQuestion(task);
        } else if(task.getSentence().isGoal()){
            processGoal(task);
        }

        if (task.getBudget().aboveThreshold() && !task.getSentence().isGoal()) {    // still need to be processed
            linkToTask(task);
        }
        entityObserver.refresh(toStringConceptContent());
    }

    /**
     * To accept a new judgment as isBelief, and check for revisions and
     * solutions
     *
     * @param task The task to be processed
     * @return Whether to continue the processing of the task
     */
    public void processJudgment(Task task) {
        
        Sentence judg = task.getSentence();
        Sentence oldBelief = evaluation(judg, beliefs);     
        
        if (oldBelief != null) {
            Stamp newStamp = judg.getStamp();
            Stamp oldStamp = oldBelief.getStamp();
            if (newStamp.equals(oldStamp)) {  
                return;
            } else if (LocalRules.revisible(judg, oldBelief)) {
                memory.newStamp = Stamp.make(newStamp, oldStamp, memory.getTime());
                if (memory.newStamp != null) {
                    memory.currentBelief = oldBelief;
                    LocalRules.revision(judg, oldBelief, false, memory);
                }
            }
        }
        
        if (task.getBudget().aboveThreshold()) {
            for (Task ques : questions) 
                LocalRules.trySolution(judg, ques, memory);          
            
            if(!task.isEternal()){
                
                for (Task desire : desires) {
                    
                    if(desire.getBestSolution() != null && task.getStamp().getOccurrenceTime() > desire.getBestSolution().getOccurrenceTime())
                        desire.setBestSolution(task.getSentence());
                }
                
            }else{
                for (Task desire : desires)
                    LocalRules.trySolution(judg, desire, memory);
            }
            addToTable(judg, beliefs, Parameters.MAXIMUM_BELIEF_LENGTH);
        }
        // if the new judgment is implication generate anticipation relation in the concept of the 
        // subject and goal relation in the post condition
        generateAnticipations(task.getSentence());
        this.generateGoalPreconditions(task.getSentence());
    } 
    
     /**
     * Use implication relation to generate anticipation within concept
     * Called from Concept.processJudgment() and LocalRules.java
     */
    public void generateAnticipations(Sentence sentence){
               
        Term term = sentence.getContent();

        // if the term has variable, return
        if(term.hasVar())
            return;
        
        if((term instanceof Implication && 
                ((((Implication)term).getTemporalOrder() == TemporalRules.ORDER_FORWARD) 
             || (((Implication)term).getTemporalOrder() == TemporalRules.ORDER_BACKWARD)))
             || ((term instanceof Equivalence) && 
                ((Equivalence)term).getTemporalOrder() == TemporalRules.ORDER_FORWARD)){
            //System.out.println("Term: " + term.getName());
            Term precondition = null;

            // find the precondition, which is the subject of the implication
            if(term instanceof Implication && (((Implication)term).getTemporalOrder() == TemporalRules.ORDER_FORWARD)){
                
                if((((Implication)term).getSubject() instanceof Conjunction) && (((Implication)term).getPredicate()) instanceof Conjunction)
                    return;
                
                precondition = ((Implication)term).getSubject();
            }
            else if(term instanceof Implication && (((Implication)term).getTemporalOrder() == TemporalRules.ORDER_BACKWARD)){
                precondition = ((Implication)term).getPredicate();
            }
            else if(term instanceof Equivalence && (((Equivalence)term).getTemporalOrder() == TemporalRules.ORDER_FORWARD)){
                precondition = ((Equivalence)term).getSubject();
            }
            else{
                return;
            }
                   
            // get the concept of the precondition      
            Concept preConcept = memory.getConcept(precondition);
            Concept concept = memory.getConcept(term);

            // add this relation to the table of anticipation of the concept of the precondition
            if(preConcept != null)     
                preConcept.addToTable(concept, preConcept.getAnticipationList(), Parameters.ANTICIPATION_LIST_CAPACITY);
        }
    }
    
    /**
     * Form hypothesis and generate goal preconditions
     * Called from Concept.processJudgment() and LocalRules.java
     */
    public void generateGoalPreconditions(Sentence sentence){
        
        Term term = sentence.getContent();
        if(term.hasVar())
            return;
        
        if((term instanceof Implication && 
                ((((Implication)term).getTemporalOrder() == TemporalRules.ORDER_FORWARD) 
             /*|| (((Implication)term).getTemporalOrder() == TemporalRules.ORDER_BACKWARD)))
             || ((term instanceof Equivalence) && 
                ((Equivalence)term).getTemporalOrder() == TemporalRules.ORDER_FORWARD)*/))){
            
            if(!(((Implication)term).getSubject() instanceof Conjunction))
                return;
            
            Term postCondition = null;

            // similar with anticipation, but adding the implication relation to the postcondition
            if(term instanceof Implication && (((Implication)term).getTemporalOrder() == TemporalRules.ORDER_FORWARD))
                postCondition = ((Implication)term).getPredicate();
            else if(term instanceof Implication && (((Implication)term).getTemporalOrder() == TemporalRules.ORDER_BACKWARD))
                postCondition = ((Implication)term).getSubject();
            else
                postCondition = ((Equivalence)term).getPredicate();
                
            Concept postConcept = memory.getConcept(postCondition);
            Concept concept = memory.getConcept(term);
            
            if(postConcept != null){
                postConcept.addToTable(concept, postConcept.getExecutable_preconditions(), Parameters.GOAL_PRECONDITION_CAPACITY);
            }
        }
        
    }

    /*
     * To answer a question by existing beliefs
     *
     * @param task The task to be processed
     * @return Whether to continue the processing of the task
     */
    public float processQuestion(Task task) {
        Sentence ques = task.getSentence();
        boolean newQuestion = true;
        if (questions != null) {
            for (Task t : questions) {
                Sentence q = t.getSentence();
                if (q.getContent().equals(ques.getContent())) {
                    ques = q;
                    newQuestion = false;
                    break;
                }
            }
        }
        
        if (newQuestion) {
            questions.add(task);
        }
        
        if (questions.size() > Parameters.MAXIMUM_QUESTIONS_LENGTH) {
            questions.remove(0);    // FIFO
        }
        
        Sentence newAnswer = evaluation(ques, beliefs);
        if (newAnswer != null) {
            LocalRules.trySolution(newAnswer, task, memory);
            return newAnswer.getTruth().getExpectation();
        } else {
            return 0.5f;
        }
    }
    
    /**
     * For procedural learning, goal processing and 
     * @param task 
     */
    private void processGoal(Task task){
        
        if(task.getContent() instanceof Negation){
            memory.currentTask = task;
            Task newTask = StructuralRules.transformNegation(task, memory);
            Concept c = memory.getConcept(newTask.getContent());
            
            if(c != null)
                c.processGoal(newTask);
            
            //System.out.println("!!!!!!!!!");
            memory.derivedTask(newTask);
        }
        
        // goal sentence
        Sentence goal = task.getSentence();
        // select the goal with highest confidence from the desires list
        Task oldGoal = selectOldGoal();
        // new time stamp
        Stamp newStamp = goal.getStamp();
        // if the old goal is not null
        if(oldGoal != null){
            Stamp oldStamp = oldGoal.getStamp();
            // if the new stamp is identical to the old one return
            if(newStamp.equals(oldStamp))
                return;
        }
        
        /*=====================================================================*/
        Sentence belief = null;
        // if the budget of the task is above the threshold 
        if(task.getBudget().aboveThreshold()){
            // select the belief with the highest confidence to offer the new solution
            belief = evaluation(task.getSentence(), beliefs);
            // go over the question to see if the task can be a good solution for the question
            for(Task iQuest : getQuests())
                LocalRules.trySolution(task.getSentence(), iQuest, memory);
            // fi the belief is not null, see if the belief can be a solution to be task
            if(belief != null){
                LocalRules.trySolution(belief, task, memory);
            }
        }
        
        // Do revision if two goals are revisible
        if(oldGoal != null && oldGoal.getBudget().aboveThreshold() && LocalRules.revisible(goal, oldGoal.getSentence())){
            // old stamp
            Stamp oldStamp = oldGoal.getStamp();
            // combine stamps
            memory.setNewStamp(newStamp, oldStamp, memory.getTime());
            // do the time projection for the old goal
            Sentence projectedGoal = oldGoal.getSentence().projection(task.getSentence().getOccurrenceTime(), newStamp.getOccurrenceTime(), memory);
            // if the projected goal is not null
            if(projectedGoal != null){
                
                memory.currentBelief = projectedGoal;
             
                LocalRules.revision(task.getSentence(), projectedGoal, false, memory);
                
                return;
            }
        }
        Stamp goalStamp = goal.getStamp().clone();
        
        if(!goalStamp.isEternal()){
            
            goalStamp.setOccurrenceTime(memory.getTime());
            if(goalStamp.after(task.getSentence().getStamp(), Parameters.DURATION)){
                
            Sentence proGoal = task.getSentence().projection(memory.getTime(), Parameters.DURATION, memory);
            
            if(proGoal != null && proGoal.getTruth().getExpectation() > Parameters.DECISION_THRESHOLD)
                memory.singlePremiseTask(proGoal.getContent(), proGoal.getTruth(), task.getBudget());
            }
            
        }else if(task.getSentence().getTruth().getExpectation() > Parameters.DECISION_THRESHOLD){
            memory.singlePremiseTask(task.getContent(), task.getSentence().getTruth(), task.getBudget());
        }
        
        if(!task.getBudget().aboveThreshold())
            return;
        
        float antiSatisfaction = 0.5f;    
        
        if(belief != null){
            Sentence projectedBelief = belief.projection(task.getSentence().getOccurrenceTime(), memory.getTime(), memory);
            antiSatisfaction = task.getSentence().getTruth().getExpDifAbs(projectedBelief.getTruth());
        }
        
        task.setPriority(task.getPriority() * antiSatisfaction);
        if (!task.getBudget().aboveThreshold()) {
            return;
        }

//        reactionToGoal();
        addToTable(task, desires, Parameters.MAXIMUM_GOAL_LENGTH);                      
        //reactionToGoal();
    }
    
    /**
     * Select the existing goal with the highest confidence from the desire list
     * @return 
     */
    private Task selectOldGoal(){
        
        Task oldGoal = null;
        
        if(!desires.isEmpty())
            oldGoal = desires.get(0);
        
        for (Task goal : desires) {
            
            if(oldGoal != null){
                if(oldGoal.getSentence().getTruth().getConfidence() < goal.getSentence().getTruth().getConfidence())
                    oldGoal = goal;
            }
            
        }
        
        if(oldGoal == null)
            return null;
            
        return oldGoal;
    }
    
    /**
     * If there is a goal is unfulfilled, then what system need to do to fulfill the goal
     */
//    public void reactionToGoal(){
//
//        if(!desires.isEmpty()){
//
//            /*if(desires.get(0).fulfilled()){
//                return;
//            }*/
//
//            // best operation, if there are several operations to reach the goal
//            Concept bestop = null;
//
//            Term precondition = null;
//
//            //memory.generalInfoReport(executable_preconditions.toString());
//
//            if(!executable_preconditions.isEmpty()){
//                // go over the executable preconditions list
//                for (Concept c : executable_preconditions) {
//
//                    if(c != null && c.getTerm() instanceof Implication)
//                        precondition = ((Implication)c.getTerm()).getSubject();
//
//                    if(precondition == null)
//                        return;
//
//                    // if precondition is conjunction
//                    if(precondition instanceof Conjunction){
//
//                        // if the sequence list in the overall buffer is not null
//                        if(memory.getOveralExperience().getSequenceList().size() >= 1){
//
//                            boolean happening = false;
//
//                            // check if the precondition is happening
//                            for(int i = memory.getOveralExperience().getSequenceList().size() - 1; i >= 0; i--){
//
//                                if(((Conjunction)precondition).getComponents().get(0).getName().equals(memory.getOveralExperience().getSequenceList().get(i).getName())){
//                                    happening = true;
//                                    break;
//                                }
//
//                            }
//
//                            if(!happening)
//                                continue;
//
//                            // if best operation is null assign the current operation to it
//                            if(bestop == null)
//                                bestop = c;
//                            // if not null, check if the current relation gives better solution
//                            else if(c.getBeliefs().get(0).getTruth().getExpectation() > bestop.getBeliefs().get(0).getTruth().getExpectation())
//                                bestop = c;
//
//                        }
//
//                    }
//
//                }
//
//                if(bestop == null)
//                    return;
//
//                TruthValue truth = null;
//                if(bestop.getBeliefs() != null)
//                    truth = TruthFunctions.deduction(desires.get(0).getSentence().getTruth(), bestop.getBeliefs().get(0).getTruth());
//
//                if(truth != null ){
//                    float priority = Parameters.DEFAULT_SUBGOAL_PRIORITY;
//                    float durability = Parameters.DEFAULT_EVENT_DURABILITY;
//                    float quality = BudgetFunctions.truthToQuality(truth);
//
//                    BudgetValue budget = new BudgetValue(priority, durability, quality);
//                    Stamp stamp = new Stamp(memory.getTime());
//
//                    Sentence sentence = null;
//
//                    precondition = ((Implication)bestop.getTerm()).getSubject();
//
//                    if(precondition instanceof Conjunction)
//                        sentence = new Sentence(((Conjunction)precondition).getComponents().get(1), Symbols.GOAL_MARK, truth, stamp);
//
//                    if(sentence == null)
//                        return;
//
//                    Task task = new Task(sentence, budget);
//                    memory.report(task.getSentence(), false, false);
//                    memory.getReasoner().getInternalBuffer().putInSequenceList(task, memory.getTime());
//                }
//
//            }
//
//        }
//    }
    
    /**
     * Traverse belief table and find the highest quality belief using
     * LocalRules.solutionQuality. Called from Concept.java and EventBuffer.java
     * 
     * @param query
     * @param list
     * @return
     */
    public Sentence evaluation(Sentence query, ArrayList<Sentence> list) {
        if (list == null) {
            return null;
        }
        // Best option initialization
        float currentBest = 0;
        float beliefQuality;
        Sentence candidate = null;
        // Find the highest quality belief linearly 
        for (Sentence judg : list) {
            beliefQuality = LocalRules.solutionQuality(query, judg, memory);
            if (beliefQuality > currentBest) {
                currentBest = beliefQuality;
                candidate = judg;
            }
        }
        return candidate;
    }
    
    /**
     * Add a new belief (or goal) into the table Sort the beliefs/goals by rank,
     * and remove redundant or low rank one
     *
     * @param newSentence The judgment to be processed
     * @param table The table to be revised
     * @param capacity The capacity of the table
     */
    public void addToTable(Sentence newSentence, ArrayList<Sentence> table, int capacity) {
        float rank1 = BudgetFunctions.rankBelief(newSentence);    // for the new isBelief
        Sentence judgment2;
        float rank2;
        int i;
        for (i = 0; i < table.size(); i++) {
            judgment2 = table.get(i);
            rank2 = BudgetFunctions.rankBelief(judgment2);
            if (rank1 >= rank2) {
                if (newSentence.equivalentTo(judgment2)) {
                    return;
                }
                table.add(i, newSentence);
                break;
            }
        }
        if (table.size() >= capacity) {
            while (table.size() > capacity) {
                table.remove(table.size() - 1);
            }
        } else if (i == table.size()) {
            table.add(newSentence);
        }
    }
    
    
    /**
     * Use for adding the task into the corresponding list, used for adding 
     * goals into desire list
     * @param task
     * @param list the desire list
     * @param capacity of the desire list
     * @return true if added successfully
     */
    public boolean addToTable(Task task, ArrayList<Task> list, int capacity){
        
        if(list.contains(task))
            return false;
        
        if(list.isEmpty())
            list.add(task);
        else{
            
            if(list.size() < capacity){
                
                int i = 0;
                
                while(task.getPriority() < list.get(i).getPriority() && i < list.size() - 1)
                    i++;
                    
                list.add(i, task);
                return true;
                
            }else{
            
                if(task.getPriority() < list.get(list.size() - 1).getPriority()){
                    
                    return false;
                    
                }else{
                    
                    list.remove(list.size() - 1);
                    
                    int i = 0;
                            
                    while(task.getPriority() < list.get(i).getPriority() && i < list.size() - 1)
                        i++;
                    
                    list.add(i, task);
                    return true;
                    
                }
            }
        }
        return false;
    }
    
    /**
     * Adding concept to the anticipation list
     * @param concept the concept of the pre-condtion
     * @param list which concepts are added to
     * @return if the concept is added successfully
     */
    private boolean addToTable(Concept concept, ArrayList<Concept> list, int capacity){
        
        if(list.contains(concept)){
            
            list.remove(concept);
            
        }
        
        if(list.isEmpty()){
            
            list.add(concept);
            
        }else{
            
            if(list.size() < capacity){
                
                int i = 0;
                
                if(concept.getBeliefs().isEmpty())
                    return false;
                
                while(concept.getBeliefs().get(0).getTruth().getExpectation() < list.get(i).getBeliefs().get(0).getTruth().getExpectation() && i < list.size() - 1)
                    i++;
                
                list.add(i, concept);
                return true;
                
            }else{
            
                if(concept.getBeliefs().isEmpty())
                    return false;
                
                if(concept.getBeliefs().get(0).getTruth().getExpectation() < list.get(list.size() - 1).getBeliefs().get(0).getTruth().getExpectation()){
                    return false;
                }else{
                    
                    list.remove(list.size() - 1);
                    
                    int i = 0;                 
                            
                    while(concept.getBeliefs().get(0).getTruth().getExpectation() < list.get(i).getBeliefs().get(0).getTruth().getExpectation() && i < list.size() - 1)
                        i++;
                    
                    list.add(i, concept);
                    return true;
                    
                }
            }
        }
        return false;
    }
    
    /**
     * Link to a new task from all relevant concepts for continued processing in
     * the near future for unspecified time.
     * <p>
     * The only method that calls the TaskLink constructor.
     *
     * @param task The task to be linked
     */
    private void linkToTask(Task task) {
        BudgetValue taskBudget = task.getBudget();
        TaskLink taskLink = new TaskLink(task, null, taskBudget);   // link type: SELF
        insertTaskLink(taskLink);
        if (term instanceof CompoundTerm) {
            if (termLinkTemplates.size() > 0) {
                BudgetValue subBudget = BudgetFunctions.distributeAmongLinks(taskBudget, termLinkTemplates.size());
                if (subBudget.aboveThreshold()) {
                    Term componentTerm;
                    Concept componentConcept;
                    for (TermLink termLink : termLinkTemplates) {
//                        if (!(task.isStructural() && (termLink.getType() == TermLink.TRANSFORM))) { // avoid circular transform
                        taskLink = new TaskLink(task, termLink, subBudget);
                        componentTerm = termLink.getTarget();
                        componentConcept = memory.getConcept(componentTerm);
                        if (componentConcept != null) {
                            componentConcept.insertTaskLink(taskLink);
                        }
//                        }
                    }
                    buildTermLinks(taskBudget);  // recursively insert TermLink
                }
            }
        }
    }
    
    /**
     * Insert a TaskLink into the TaskLink bag
     * <p>
     * called only from Memory.continuedProcess
     *
     * @param taskLink The termLink to be inserted
     */
    public void insertTaskLink(TaskLink taskLink) {
        BudgetValue taskBudget = taskLink.getBudget();
        taskLinks.putIn(taskLink);
        memory.activateConcept(this, taskBudget);
    }

    /**
     * Recursively build TermLinks between a compound and its components
     * <p>
     * called only from Memory.continuedProcess
     *
     * @param taskBudget The BudgetValue of the task
     */
    public void buildTermLinks(BudgetValue taskBudget) {
        Term t;
        Concept concept;
        TermLink termLink1, termLink2;
        if (termLinkTemplates.size() > 0) {
            BudgetValue subBudget = BudgetFunctions.distributeAmongLinks(taskBudget, termLinkTemplates.size());
            if (subBudget.aboveThreshold()) {
                for (TermLink template : termLinkTemplates) {
                    if (template.getType() != TermLink.TRANSFORM) {
                        t = template.getTarget();
                        concept = memory.getConcept(t);
                        if (concept != null) {
                            termLink1 = new TermLink(t, template, subBudget);
                            insertTermLink(termLink1);   // this termLink to that
                            termLink2 = new TermLink(term, template, subBudget);
                            concept.insertTermLink(termLink2);   // that termLink to this
                            if (t instanceof CompoundTerm) {
                                concept.buildTermLinks(subBudget);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Insert a TermLink into the TermLink bag
     * <p>
     * called from buildTermLinks only
     *
     * @param termLink The termLink to be inserted
     */
    public void insertTermLink(TermLink termLink) {
        termLinks.putIn(termLink);
    }

    /**
     * Return the associated term, called from Memory only
     * @return The associated term
     */
    public Term getTerm() {
        return term;
    }

    /**
     * Return a string representation of the concept, called in ConceptBag only
     * @return The concept name, with taskBudget in the full version
     */
    @Override
    public String toString() {  // called from concept bag
        if (NARSBatch.isStandAlone()) {
            return (super.toStringBrief() + " " + key);
        } else {
            return key;
        }
    }

    /**
     * called from {@link NARSBatch}
     * @return A string representation of the concept
     */
    @Override
    public String toStringLong() {
        String res = toStringBrief() + " " + key
                + toStringIfNotNull(termLinks, "termLinks")
                + toStringIfNotNull(taskLinks, "taskLinks");
        res += toStringIfNotNull(null, "questions");
        for (Task t : questions) {
            res += t.toString();
        }
        // TODO other details?
        return res;
    }

    private String toStringIfNotNull(Object item, String title) {
        return item == null ? "" : "\n " + title + ":" + item.toString();
    }
    
    /**
     * Prints Beliefs and Questions of a concept
     *
     * @return String representation of direct content
     */
    public String toStringConceptContent() {
        StringBuilder buffer = new StringBuilder();
        buffer.append("\n  Beliefs:\n");
        if (beliefs.size() > 0) {
            for (Sentence s : beliefs) {
                buffer.append(s).append("\n");
            }
        }
        buffer.append("\n  Question:\n");
        if (questions.size() > 0) {
            for (Task t : questions) {
                buffer.append(t).append("\n");
            }
        }
        return buffer.toString();
    }

    /**
     * Recalculate the quality of the concept [to be refined to show
     * extension/intension balance]
     * @return The quality value
     */
    @Override
    public float getQuality() {
        float linkPriority = termLinks.averagePriority();
        float termComplexityFactor = 1.0f / term.getComplexity();
        return UtilityFunctions.or(linkPriority, termComplexityFactor);
    }

    /**
     * Return the templates for TermLinks, only called in
     * Memory.continuedProcess
     *
     * @return The template get
     */
    public ArrayList<TermLink> getTermLinkTemplates() {
        return termLinkTemplates;
    }

    /**
     * Select a isBelief to interact with the given task in inference
     * <p>
     * get the first qualified one
     * <p>
     * only called in RuleTables.reason
     *
     * @param task The selected task
     * @return The selected isBelief
     */
    public Sentence getBelief(Task task) {
        Sentence taskSentence = task.getSentence();
        for (Sentence belief : beliefs) {
            memory.getRecorder().append(" * Selected Belief: " + belief + "\n");
            memory.newStamp = Stamp.make(taskSentence.getStamp(), belief.getStamp(), memory.getTime());
            if (memory.newStamp != null) {
                Sentence belief2 = (Sentence) belief.clone();   // will this mess up priority adjustment?
                return belief2;
            }
        }
        return null;
    }
    
    public TruthValue getDesire(){
        
        if(desires.isEmpty())
            return null;
        
        TruthValue topValue = desires.get(0).getSentence().getTruth();
        return topValue;
        
    }
    
    public TaskLinkBag getTaskLinkBag(){
        return taskLinks;
    }
    
    public ArrayList<Concept> getExecutable_preconditions() {
        return executable_preconditions;
    }

    public ArrayList<Task> getGeneral_executable_preconditions() {
        return general_executable_preconditions;
    }

    public void setGeneral_executable_preconditions(ArrayList<Task> general_executable_preconditions) {
        this.general_executable_preconditions = general_executable_preconditions;
    }

    public boolean isObservable() {
        return observable;
    }

    public void setObservable(boolean observable) {
        this.observable = observable;
    }

    public Memory getMemory() {
        return memory;
    }

    public void setMemory(Memory memory) {
        this.memory = memory;
    }

    public EntityObserver getEntityObserver() {
        return entityObserver;
    }

    public void setEntityObserver(EntityObserver entityObserver) {
        this.entityObserver = entityObserver;
    }
    
    public ArrayList<Task> getDesires(){
        return desires;
    }

    public ArrayList<Sentence> getBeliefs(){
        return beliefs;
    }
    
    public ArrayList<Task> getQuests(){
        return quests;
    }
    
    public ArrayList<Concept> getAnticipationList(){
        return anticipationList;
    }
    
    public TermLinkBag getTermLinkBag(){
        return termLinks;
    }
    
    /**
     * Main Loop
     * An atomic step in a concept, only called in
     */
    public void fire() {
        
//        reactionToGoal();
        
        TaskLink currentTaskLink = taskLinks.takeOut();
        if (currentTaskLink == null) {
            return;
        }
        memory.currentTaskLink = currentTaskLink;
        memory.currentBeliefLink = null;
        memory.getRecorder().append(" * Selected TaskLink: " + currentTaskLink + "\n");
        Task task = currentTaskLink.getTargetTask();
        memory.currentTask = task;  // one of the two places where this variable is set
//      memory.getRecorder().append(" * Selected Task: " + task + "\n");    // for debugging
        if (currentTaskLink.getType() == TermLink.TRANSFORM) {
            memory.currentBelief = null;
            RuleTables.transformTask(currentTaskLink, memory);  // to turn this into structural inference as below?
        } else {
            int termLinkCount = Parameters.MAX_REASONED_TERM_LINK;
//        while (memory.noResult() && (termLinkCount > 0)) {
            while (termLinkCount > 0) {
                TermLink termLink = termLinks.takeOut(currentTaskLink, memory.getTime());
                if (termLink != null) {
                    memory.getRecorder().append(" * Selected TermLink: " + termLink + "\n");
                    memory.currentBeliefLink = termLink;                 
                    
                    RuleTables.reason(currentTaskLink, termLink, memory);
                    termLinks.putBack(termLink);
                    termLinkCount--;
                } else {
                    termLinkCount = 0;
                }
            }
        }
        taskLinks.putBack(currentTaskLink);
    }
}