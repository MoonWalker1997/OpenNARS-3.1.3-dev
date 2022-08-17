/*
 * The MIT License
 *
 * Copyright 2021 The OpenNARS authors.
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
package nars.storage;

import nars.MC.EventBufferMC;
import nars.MC.InternalBufferMC;
import nars.MC.OverallBufferMC;
import nars.MC.SensoryMotorChannelMC;
import nars.MC.channels.ExpChannel1;
import nars.MC.channels.ExpChannel2;
import nars.entity.*;
import nars.inference.BudgetFunctions;
import nars.io.IInferenceRecorder;
import nars.io.NullInferenceRecorder;
import nars.io.Symbols;
import nars.language.*;
import nars.main.NAR;
import nars.main.Parameters;
import nars.mental.Emotion;
import nars.mental_operator.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The main memory of the system.
 */
public class Memory {

//    // [TL]
//
//    private long timer;
//    private long clock = 0;
//
//    private ArrayList<SensoryMotorChannelMC> channels = new ArrayList<>();
//
//    private InternalBufferMC internal_buffer = new InternalBufferMC(5, 5, 5, 5, this);
//    private OverallBufferMC overall_buffer = new OverallBufferMC(5, 5, 5, 5, this);
//
//    private SensoryMotorChannelMC channel1;
//    private SensoryMotorChannelMC channel2;
//
//    public OverallBufferMC getOverall_buffer() {
//        return overall_buffer;
//    }
//
//    public InternalBufferMC getInternal_buffer() {
//        return internal_buffer;
//    }

    ArrayList<Task> previous_inference_result = new ArrayList<>();

    public ArrayList<Task> getPrevious_inference_result() {
        return previous_inference_result;
    }

    public void setPrevious_inference_result(ArrayList<Task> previous_inference_result) {
        this.previous_inference_result = previous_inference_result;
    }

    /**
     * Backward pointer to the reasoner
     */
     private NAR reasoner = null;
    /**
     * Concept bag. Containing all Concepts of the system
     */
    private final ConceptBag concepts;
    /**
     * New tasks with novel composed terms, for delayed and selective processing
     */
    //private final NovelTaskBag novelTasks;
    /**
     * Inference record text to be written into a log file, currently not used
     */
    private IInferenceRecorder recorder;
    private final AtomicInteger beliefForgettingRate = new AtomicInteger(Parameters.TERM_LINK_FORGETTING_CYCLE);
    private final AtomicInteger taskForgettingRate = new AtomicInteger(Parameters.TASK_LINK_FORGETTING_CYCLE);
    private final AtomicInteger conceptForgettingRate = new AtomicInteger(Parameters.CONCEPT_FORGETTING_CYCLE);
    /**
     * List of new tasks accumulated in one cycle, to be processed in the next cycle
     */
    //private final LinkedList<Task> newTasks;
    /**
     * List of Strings or Tasks to be sent to the output channels
     */
    private final ArrayList<String> exportStrings;
    /**
     * The selected Term
     */
    public Term currentTerm;
    /**
     * The selected Concept
     */
    public Concept currentConcept;
    /**
     * The selected TaskLink
     */
    public TaskLink currentTaskLink;
    /**
     * The selected Task
     */
    public Task currentTask;
    /**
     * The selected TermLink
     */
    public TermLink currentBeliefLink;
    /**
     * The selected belief
     */
    public Sentence currentBelief;
    /**
     * The new Stamp
     */
    public Stamp newStamp;

    /**
     * The substitution that unify the common term in the Task and the Belief
     * TODO unused
     */
    protected HashMap<Term, Term> substitute;

    public static Random randomNumber = new Random(1);

    private Map<String, Operator> operators;

    private Emotion emotion;

    /**
     * Creates a new main memory. Called in NAR only
     *
     * @param reasoner
     */
    public Memory(NAR reasoner) {
        this.reasoner = reasoner;
        recorder = new NullInferenceRecorder();
        concepts = new ConceptBag(this);
        exportStrings = new ArrayList<>();
        operators = new LinkedHashMap();
        initOperators();
        newStamp = null;
        emotion = new Emotion();
    }

//    public Memory() {
//
//        EventBufferMC event_buffer1 = new EventBufferMC(5,5,5,5,this);
//        EventBufferMC event_buffer2 = new EventBufferMC(5,5,5,5,this);
//
//        channel1 = new ExpChannel1(event_buffer1, null, this);
//        channel2 = new ExpChannel2(event_buffer2, null, this);
//
//        channels.add(this.channel1);
//        channels.add(this.channel2);
//        recorder = new NullInferenceRecorder();
//        concepts = new ConceptBag(this);
//        exportStrings = new ArrayList<>();
//        operators = new LinkedHashMap();
//        newStamp = null;
//        emotion = new Emotion();
//    }

    public void initOperators() {

        Operator anti = new Anticipate();
        Operator escape = new Escape();
        Operator left = new Left();
        Operator right = new Right();
        Operator deactive = new Deactive();
        Operator fire = new Fire();

        operators.put("^anticipate", anti);
        operators.put("^escape", escape);
        operators.put("^left", left);
        operators.put("^right", right);
        operators.put("^deactivate", deactive);
        operators.put("^fire", fire);
    }

    public void init() {
        concepts.init();
        exportStrings.clear();
        reasoner.initTimer();
        randomNumber = new Random(1);
        recorder.append("\n-----RESET-----\n");
    }

    /**
     * Actually means that there are no new Tasks
     *
     * @return
     */
    public boolean noResult() {
        return reasoner.getGlobalBuffer().isEmpty();
    }

    /**
     * Get an existing Concept for a given name <p> called from Term and
     * ConceptWindow.
     *
     * @param name the name of a concept
     * @return a Concept or null
     */
    public Concept nameToConcept(String name) {
        return concepts.get(name);
    }

    /**
     * Get an existing Concept for a given Term.
     *
     * @param term The Term naming a concept
     * @return a Concept or null
     */
    public Concept termToConcept(Term term) {
        return nameToConcept(term.getName());
    }

    /**
     * Get the Concept associated to a Term, or create it.
     *
     * @param term indicating the concept
     * @return an existing Concept, or a new one, or null ( TODO bad smell )
     */
    public Concept getConcept(Term term) {
        if (!term.isConstant()) {
            return null;
        }
        String n = term.getName();
        Concept concept = concepts.get(n);
        if (concept == null) {
            concept = new Concept(term, this); // the only place to make a new Concept
            boolean created = concepts.putIn(concept);
            if (!created) {
                return null;
            }
        }
        return concept;
    }

    public Stamp getNewStamp() {
        if (newStamp == null) {
            newStamp = new Stamp(getTime());
            resetOccurrenceTime();
        }
        return newStamp;
    }

    public Stamp setNewStamp(Stamp first, Stamp second, long time) {
        newStamp = new Stamp(first, second, time);
        return newStamp;
    }

    public void resetOccurrenceTime() {
        newStamp.setOccurrenceTime(Stamp.ETERNAL);
    }

    /**
     * Get a Term for a given name of a Concept or Operator <p> called in
     * StringParser and the make methods of compound terms.
     *
     * @param name the name of a concept or operator
     * @return a Term or null (if no Concept/Operator has this name)
     */
    public Term nameToListedTerm(String name) {
        Concept concept = concepts.get(name);
        if (concept != null) {
            return concept.getTerm();
        }
        return null;
    }

    /**
     * Get the current activation level of a concept.
     *
     * @param t The Term naming a concept
     * @return the priority value of the concept
     */
    public float getConceptActivation(Term t) {
        Concept c = termToConcept(t);
        return (c == null) ? 0f : c.getPriority();
    }

    /**
     * Adjust the activation level of a Concept <p> called in
     * Concept.insertTaskLink only
     *
     * @param c the concept to be adjusted
     * @param b the new BudgetValue
     */
    public void activateConcept(Concept c, BudgetValue b) {
        concepts.pickOut(c.getKey());
        BudgetFunctions.activate(c, b);
        concepts.putBack(c);
    }

    /**
     * Activated task called in MatchingRules.trySolution and
     * Concept.processGoal
     *
     * @param budget          The budget value of the new Task
     * @param sentence        The content of the new Task
     * @param candidateBelief The belief to be used in future inference, for
     *                        forward/backward correspondence
     */
    public void activatedTask(BudgetValue budget, Sentence sentence, Sentence candidateBelief) {
        Task task = new Task(sentence, budget, currentTask, sentence, candidateBelief);
        recorder.append("!!! Activated: " + task.toString() + "\n");
        if (sentence.isQuestion()) {
            float s = task.getBudget().totalBudget();
//            float minSilent = reasoner.getMainWindow().silentW.value() / 100.0f;
            float minSilent = Parameters.SILENT_LEVEL / 100.0f;
            if (s > minSilent) {  // only report significant derived Tasks
                //generalInfoReport("4");
                report(task.getSentence(), false, false);
            }
        }
//        reasoner.getInternalBuffer().putInSequenceList(task, getTime());
    }

    /**
     * Derived task comes from the inference rules.
     *
     * @param task the derived task
     */
//    public void derivedTask(Task task) {
//
//        if (task.getBudget().aboveThreshold()) {
//            recorder.append(" OUT: " + task + "\n");
//            float budget = task.getBudget().totalBudget();
//            float minSilent = reasoner.getSilenceValue().get() / 100.0f;
//
//            //System.out.println(task.getSentence().getContent().getName());
//            if (budget > minSilent) {  // only report significant derived Tasks
//                //generalInfoReport("5");
//                report(task.getSentence(), false, false);
//            }
//
//            //generalInfoReport(task.getSentence().toString());
//
//            reasoner.getInternalBuffer().putInSequenceList(task, getTime());
//
//        } else {
//            recorder.append("!!! Ignored: " + task + "\n");
//        }
//    }
    public void derivedTask(Task task) {

        if (task.getBudget().aboveThreshold()) {
            recorder.append(" OUT: " + task + "\n");
            float budget = task.getBudget().totalBudget();
            float minSilent = Parameters.SILENT_LEVEL / 100.0f;

            //System.out.println(task.getSentence().getContent().getName());
            if (budget > minSilent) {  // only report significant derived Tasks
                //generalInfoReport("5");
                report(task.getSentence(), false, false);
            }

            //generalInfoReport(task.getSentence().toString());
            this.previous_inference_result.add(task);
//            System.out.println("Output: " + task.getName()); [TLTLTLTL]
        } else {
            recorder.append("!!! Ignored: " + task + "\n");
        }
    }

    /* --------------- new task building --------------- */

    /**
     * Shared final operations by all double-premise rules, called from the
     * rules except StructuralRules
     *
     * @param newContent The content of the sentence in task
     * @param newTruth   The truth value of the sentence in task
     * @param newBudget  The budget value in task
     * @return
     */
    public Task doublePremiseTask(Term newContent, TruthValue newTruth, BudgetValue newBudget) {
        Stamp deriveStamp = newStamp.clone();
        resetOccurrenceTime();

        Task newTask = null;

        if (newContent != null) {
            Sentence newSentence = new Sentence(newContent, currentTask.getSentence().getPunctuation(), newTruth, deriveStamp);

            //System.out.println("Sentence: " + newSentence.getContent().getName());
            newTask = new Task(newSentence, newBudget, currentTask, currentBelief);
            derivedTask(newTask);
        }

        return newTask;
    }

    /**
     * Shared final operations by all double-premise rules, called from the
     * rules except StructuralRules
     *
     * @param newContent The content of the sentence in task
     * @param newTruth   The truth value of the sentence in task
     * @param newBudget  The budget value in task
     * @param revisible  Whether the sentence is revisible
     */
    public void doublePremiseTask(Term newContent, TruthValue newTruth, BudgetValue newBudget, boolean revisible) {

        Stamp deriveStamp = newStamp.clone();
        resetOccurrenceTime();

        if (newContent != null) {
            Sentence taskSentence = currentTask.getSentence();
            Sentence newSentence = new Sentence(newContent, taskSentence.getPunctuation(), newTruth, deriveStamp, revisible);
            Task newTask = new Task(newSentence, newBudget, currentTask, currentBelief);
            //newTask.setElemOfSequenceBuffer(false);
            //System.out.println("777");
            derivedTask(newTask);
        }
    }

    /**
     * Shared final operations by all single-premise rules, called in
     * StructuralRules
     *
     * @param newContent The content of the sentence in task
     * @param newTruth   The truth value of the sentence in task
     * @param newBudget  The budget value in task
     */
    public void singlePremiseTask(Term newContent, TruthValue newTruth, BudgetValue newBudget) {
        singlePremiseTask(newContent, currentTask.getSentence().getPunctuation(), newTruth, newBudget);
    }

    /**
     * Shared final operations by all single-premise rules, called in
     * StructuralRules
     *
     * @param newContent  The content of the sentence in task
     * @param punctuation The punctuation of the sentence in task
     * @param newTruth    The truth value of the sentence in task
     * @param newBudget   The budget value in task
     */
    public void singlePremiseTask(Term newContent, char punctuation, TruthValue newTruth, BudgetValue newBudget) {
        Task parentTask = currentTask.getParentTask();
        if (parentTask != null && newContent.equals(parentTask.getContent())) { // circular structural inference
            return;
        }

        //System.out.println("Single: " + newContent.getName());

        Sentence taskSentence = currentTask.getSentence();
        if (taskSentence.isJudgment() || currentBelief == null) {
            newStamp = new Stamp(taskSentence.getStamp(), getTime());
        } else {    // to answer a question with negation in NAL-5 --- move to activated task?
            newStamp = new Stamp(currentBelief.getStamp(), getTime());
        }

        Stamp deriveStamp = newStamp.clone();
        resetOccurrenceTime();

        Sentence newSentence = new Sentence(newContent, punctuation, newTruth, deriveStamp, taskSentence.getRevisible());
        Task newTask = new Task(newSentence, newBudget, currentTask, null);
        //System.out.println("888");
        derivedTask(newTask);
    }

    /* ---------- system working workCycle ---------- */

    /**
     * An atomic working cycle of the system: process new Tasks, then fire a
     * concept <p> Called from Reasoner.cycle only
     *
     * @param clock The current time to be displayed
     */
    public void workCycle(long clock) {
        recorder.append(" --- " + clock + " ---\n");
//        processBuffer();
        processConcept();
        //reasoner.getGlobalBuffer().refresh();
    }

    /**
     * Select task from Internal Buffer and insert to Global Buffer
     * Select task from Global Buffer and insert to Main Memory
     */
//    private void processBuffer() {
//        //reasoner.getGlobalBuffer().print();
//        if (!reasoner.getInternalBuffer().isEmpty()) {
//            Task task = reasoner.getInternalBuffer().observe(true, false);
//            //report(task.getSentence(), false, false);
//            if (task != null)
//                reasoner.getGlobalBuffer().preProcessing(task, true);
//        }
//        //reasoner.getGlobalBuffer().print();
//        Task task = reasoner.getGlobalBuffer().observe(true, true); //Args do not matter
//        //generalInfoReport("Take Out: " + task);
//        if (task != null) {
//            //System.out.println("Task take out: " + task.getName() + " " + task.getSentence().getTruth().toString());
//            immediateProcess(task);
//        }
//    }
//    private void processBuffer() {
//
//        // TODO, add something here [TL], this is for channels auto-input
//
//        Task task_from_internal_buffer = this.internal_buffer.step(previous_inference_result);
//
//        previous_inference_result = new ArrayList<>();
//
//        ArrayList<Task> task_for_overall_buffer = new ArrayList<>();
//
//        for (int i = 0; i < this.channels.size(); i++) {
//            task_for_overall_buffer.add(this.channels.get(i).generate_Narsese_input());
//        }
//
//        task_for_overall_buffer.add(task_from_internal_buffer);
//
//        Task task_for_memory = this.overall_buffer.step(task_for_overall_buffer);
//
//        if (task_for_memory != null) {
//            immediateProcess(task_for_memory);
//        }
//    }

    /**
     * Select a concept to fire.
     */
    private void processConcept() {
        currentConcept = concepts.takeOut();

        //System.out.println(" * Selected Concept: " + currentConcept.getTerm());

        if (currentConcept != null) {
            currentTerm = currentConcept.getTerm();
            recorder.append(" * Selected Concept: " + currentTerm + "\n");
            //System.out.println(" * Selected Concept: " + currentTerm + "\n");
            concepts.putBack(currentConcept);   // current Concept remains in the bag all the time
            //generalInfoReport("Concept selected: " + currentConcept.toStringBrief());
            currentConcept.fire();              // a working workCycle
        }
    }

    /**
     * Immediate processing of a new task, in constant time Local processing, in
     * one concept only
     *
     * @param task the task to be accepted
     */
    public void immediateProcess(Task task) {
        currentTask = task; // one of the two places where this variable is set
        recorder.append("!!! Insert: " + task + "\n");
        //System.out.println("!!! Insert: " + task + "\n");
        currentTerm = task.getContent();
        currentConcept = getConcept(currentTerm);// Creates a new concept
        if (currentConcept != null) {
            activateConcept(currentConcept, task.getBudget()); // initializes this concept
            currentConcept.directProcess(task);
        }
    }

    /**
     * Display input/output sentence in the output channels.The only place to
     * add Objects into exportStrings. Currently only Strings are added, though
     * in the future there can be outgoing Tasks; also if exportStrings is empty
     * display the current value of timer ( exportStrings is emptied in
     *
     * @param sentence the sentence to be displayed
     * @param input    whether the task is input
     * @param answer
     */
    public void report(Sentence sentence, boolean input, boolean answer) {
        if (NAR.DEBUG) {
            System.out.println("// report( clock " + reasoner.getTime()
                    + ", input " + input
                    + ", timer " + reasoner.getTimer()
                    + ", Sentence " + sentence
                    + ", exportStrings " + exportStrings);
            System.out.flush();
        }
        if (exportStrings.isEmpty()) {
            long timer = reasoner.updateTimer();
            if (timer > 0) {
                exportStrings.add(String.valueOf(timer));
            }
        }

        long interval = 0;
        if (sentence.getContent() instanceof Implication)
            interval = ((Implication) (sentence.getContent())).getInterval();
        else if (sentence.getContent() instanceof Equivalence)
            interval = ((Equivalence) (sentence.getContent())).getInterval();

        boolean execution = sentence.getContent() instanceof Operation;

        String s;
        if (input) {
            s = "  IN: ";
        } else if (execution) {
            s = "EXE: ";
        } else {
            if (answer)
                s = "Answer: ";
            else
                s = " OUT: ";
        }
        //System.out.println(sentence.toStringBrief());
        //System.out.println("occurrence Time: " + sentence.getStamp().getOccurrenceTime());
        if (!sentence.getStamp().isEternal()) {
            String tense = sentence.getStamp().getTense(getTime(), Parameters.DURATION);
            if (tense == null) {
                s += sentence.toStringBrief(tense) + " Eternal";
            } else {

                if (execution && !input)
                    s += ((Operation) sentence.getContent()).getPredicate().getName();
                else
                    s += sentence.toStringBrief(tense);
                
                /*switch (tense) {
                    case Symbols.TENSE_FUTURE:
                        s += " Will happen at : " + sentence.getOccurrenceTime();
                        break;
                    case Symbols.TENSE_PAST:
                        s+= " Happened at: " + sentence.getOccurrenceTime();
                        break;
                    default:
                        s+= " Happening";
                        break;
                }*/
            }
        } else {
            s += sentence.toStringBrief();
        }
        if (interval > 0)
            s += " Interval is " + interval;
        //System.out.println("s: " + s);
        exportStrings.add(s);
    }

    public void generalInfoReport(String s) {
        exportStrings.add(s);
    }

    /**
     * Task execution called only from Operator.java
     *
     * @param time
     * @param operation
     * @param truth
     */
    public void executeTask(long time, Operation operation, TruthValue truth) {

        Stamp stamp = new Stamp(Tense.Present, time);
        Sentence sentence = new Sentence(operation, Symbols.JUDGMENT_MARK, truth, stamp);

        BudgetValue budgetForNewTask = new BudgetValue(Parameters.DEFAULT_FEEDBACK_PRIORITY, Parameters.DEFAULT_FEEDBACK_DURABILITY,
                BudgetFunctions.truthToQuality(truth));

        Task newTask = new Task(sentence, budgetForNewTask);

//        reasoner.getInternalBuffer().putInSequenceList(newTask, getTime());
    }

    public Operator getOperator(String op) {
        return operators.get(op);
    }

    public Operator addOperator(Operator op) {
        operators.put(op.getName(), op);
        return op;
    }

    public Operator removeOperator(Operator op) {
        return operators.remove(op.getName());
    }

    public AtomicInteger getTaskForgettingRate() {
        return taskForgettingRate;
    }

    public AtomicInteger getBeliefForgettingRate() {
        return beliefForgettingRate;
    }

    public AtomicInteger getConceptForgettingRate() {
        return conceptForgettingRate;
    }

    public ConceptBag getConcepts() {
        return concepts;
    }

    /**
     * Returns priority of currently selected concept
     *
     * @param c type Concept
     * @return
     */
    public float getConceptPriority(Concept c) {
        return c.getPriority();
    }

    /**
     * Returns Truth Value (aka Desire Value) of highest priority desire task
     * from currently selected concept
     *
     * @param c type Concept
     * @return
     */
    public TruthValue getConceptDesireTruth(Concept c) {
        return c.getDesire();
    }

    /**
     * Returns Truth Value of highest CONFIDENCE belief sentence
     * from currently selected concept
     *
     * @param c type Concept
     * @return
     */
    public TruthValue getConceptBeliefTruth(Concept c) {
        return c.getBeliefs().get(0).getTruth();
    }

//    public NAR getReasoner() {
//        return reasoner;
//    }

//    public EventBuffer getOveralExperience() {
//        return reasoner.getGlobalBuffer();
//    }

//    public EventBuffer getInternalExperience() {
//        return reasoner.getInternalBuffer();
//    }

    public ArrayList<String> getExportStrings() {
        return exportStrings;
    }

    public IInferenceRecorder getRecorder() {
        return recorder;
    }

    // Only Called from GUI
    public void setRecorder(IInferenceRecorder recorder) {
        this.recorder = recorder;
    }

    public long getTime() {
        return reasoner.getTime();
    }

    public Emotion getEmotion() {
        return emotion;
    }

    @Override
    public String toString() {
        return toStringLongIfNotNull(concepts, "concepts")
                + toStringLongIfNotNull(currentTask, "currentTask")
                + toStringLongIfNotNull(currentBeliefLink, "currentBeliefLink")
                + toStringIfNotNull(currentBelief, "currentBelief");
    }

    private String toStringLongIfNotNull(Bag<?> item, String title) {
        return item == null ? "" : "\n " + title + ":\n"
                + item.toStringLong();
    }

    private String toStringLongIfNotNull(Item item, String title) {
        return item == null ? "" : "\n " + title + ":\n"
                + item.toStringLong();
    }

    private String toStringIfNotNull(Object item, String title) {
        return item == null ? "" : "\n " + title + ":\n"
                + item.toString();
    }

}