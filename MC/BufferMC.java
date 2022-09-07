package nars.MC;

import nars.entity.*;
import nars.inference.BudgetFunctions;
import nars.inference.TemporalRules;
import nars.inference.TruthFunctions;
import nars.io.Symbols;
import nars.language.CompoundTerm;
import nars.language.Conjunction;
import nars.language.Statement;
import nars.language.Term;
import nars.storage.Memory;

import java.util.ArrayList;

import static java.lang.Float.min;

public abstract class BufferMC {

    protected double priority_decay; // when the priority will be punished, the default hyperparameter
    protected double priority_develop; // when the priority will be rewarded, the default hyperparameter

    // since each buffer needs to communicate with the main memory, here is a reference
    // make it static since different buffers share the same memory reference
    protected Memory memory;

    // 0 initialization for the storage hyperparameters
    protected int observation_capacity;
    protected int anticipation_capacity;
    protected int num_slot_one_side;
    protected int num_slot;

    protected ArrayList<SlotMC> timeSlots = new ArrayList<>(); // first part of the buffer, time slots
    protected PriorityQueueMC predictionTable; // second part of the buffer, prediction table

    protected int present; // an indicator of the index of the present time slot

    protected Boolean temporal; // whether this buffer supports the temporal reasoning

    /**
     * Constructor, different buffers shall use the same constructor
     *
     * @param num_slot:              number of future/previous time slots on ONE side
     * @param observation_capacity:  number of observations allowed in each time slot
     * @param anticipation_capacity: number of anticipations allowed in each time slot
     * @param prediction_capacity:   number of predictions allowed in the prediction table
     * @param memory:                memory referred
     * @param temporal:              whether this buffer allows temporal-related processing (inter-time slots precessing)
     */
    public BufferMC(int num_slot, int observation_capacity, int anticipation_capacity,
                    int prediction_capacity, Memory memory, Boolean temporal) {

        this.memory = memory;
        this.observation_capacity = observation_capacity;
        this.anticipation_capacity = anticipation_capacity;

        this.num_slot_one_side = num_slot;
        this.num_slot = 2 * num_slot + 1;

        for (int i = 0; i < this.num_slot; i++) {
            this.timeSlots.add(new SlotMC(observation_capacity, anticipation_capacity));
        }
        this.present = num_slot;
        this.temporal = temporal;
        this.predictionTable = new PriorityQueueMC(prediction_capacity);
    }

    /**
     * Check whether this.concurrent_observations is empty, usually used for checking whether the channel has
     * some initial atomic inputs at the beginning.
     *
     * @return: whether this.concurrent_observations is empty
     */
    public Boolean isEmpty() {
        return this.timeSlots.get(this.present).getConcurrent_observations().get_data().size() == 0;
    }

    /**
     * For n atomic events, this method will recursively give 2^n combinations to this.concurrent_observations.
     * Code modified from: https://segmentfault.com/a/1190000040142137.
     *
     * @param Tasks: atomic events to generate compounds
     * @param n:     num of atomic events
     */
    protected void dfs(ArrayList<Task> Tasks, int index, int count, boolean[] flag, int n) {
        if (count == 0) {
            ArrayList<Term> tempTerms = new ArrayList<Term>();
            for (int i = 0; i < n; i++) {
                if (flag[i]) {
                    tempTerms.add(Tasks.get(i).getContent());
                }
            }
            Term compoundTerm = Conjunction.make(tempTerms, TemporalRules.ORDER_CONCURRENT, this.memory);
            char punctuation = Symbols.JUDGMENT_MARK;
            TruthValue truth = Tasks.get(0).getSentence().getTruth();
            for (int i = 1; i < Tasks.size(); i++) {
                truth = TruthFunctions.induction(truth, Tasks.get(i).getSentence().getTruth());
            }
            Stamp stamp = new Stamp(memory.getTime());
            Sentence sentence = new Sentence(compoundTerm, punctuation, truth, stamp, true);
            Task compoundTask = new Task(sentence, Tasks.get(0).getBudget());
            this.timeSlots.get(this.present).put_concurrent_observation(compoundTask);
        } else {
            for (int i = index + 1; i < n; i++) {
                flag[i] = true;
                dfs(Tasks, i, count - 1, flag, n);
                flag[i] = false;
            }
        }
    }

    /**
     * Considers using atomic events only, to generate compounds and filter.
     * Different buffers may have different compound generation methods.
     */
    public void compound_generation() {}

    /**
     * All "priority" mentioned below is not the priority in budget.
     * Four steps:
     * 1. If components and compounds coexist, components will get a priority penalty.
     * 2. Check all predictions, applicable predictions will fire.
     * 3. Check all anticipations, satisfied anticipation will get a priority bonus and one more positive evidence.
     * Unsatisfied anticipations will get a priority decay and one more negative evidence.
     * 4. Unexpected observations will get a priority bonus.
     * Different buffers shall use the same local evaluation methods.
     */
    public void local_evaluation() {
        // go through all events (including compounds) and collect their components
        ArrayList<Term> compound_contains = new ArrayList<>();
        for (int i = 0; i < this.timeSlots.get(this.present).getConcurrent_observations().get_data().size(); i++) {
            if (this.timeSlots.get(this.present).getConcurrent_observations().get_data().get(i).getContent().getName().charAt(1) == '&') {
                CompoundTerm cpdt = (CompoundTerm) this.timeSlots.get(this.present).getConcurrent_observations().get_data().get(i).getContent().getContent();
                compound_contains.addAll(cpdt.getComponents());
            }
        }
        // a double for-loop checking all events (include compounds) and making penalty
        ArrayList<Task> revised_tasks = new ArrayList<>();
        for (int i = 0; i < this.timeSlots.get(this.present).getConcurrent_observations().get_data().size(); i++) {
            for (int j = 0; j < compound_contains.size(); j++) {
                if (this.timeSlots.get(this.present).getConcurrent_observations().get_data().get(i).getContent().getContent().equals(compound_contains.get(j))) {
                    Task tmp = this.timeSlots.get(this.present).getConcurrent_observations().get_data().get(i).getContent();
                    // TODO
                    float priority = min((float) 0.99, (float) (tmp.getPriority() * priority_decay));
                    float durability = min((float) 0.99, (float) (tmp.getDurability() * priority_decay));
                    float quality = min((float) 0.99, (float) (tmp.getQuality() * priority_decay));
                    BudgetValue revised_budget = new BudgetValue(priority, durability, quality);
                    revised_tasks.add(new Task(tmp.getSentence(), revised_budget));
                    break; // one observation can match at most once
                }
            }
        }
        // apply the change above
        // since we cannot go through the array while changing it, all changes are applied later
        for (int i = 0; i < revised_tasks.size(); i++) {
            this.timeSlots.get(this.present).put_concurrent_observation(revised_tasks.get(i));
        }
        // check all predictions, whether there is an event (compound) matches its precondition
        for (int i = 0; i < this.predictionTable.get_data().size(); i++) {
            Statement statement = (Statement) this.predictionTable.get_data().get(i).getContent().getSentence().cloneContent();
            Term subject = statement.getSubject();
            Term predicate = statement.getPredicate();
            long interval = statement.getInterval();
            // this is for checking all the concurrent compounds
            for (int j = 0; j < this.timeSlots.get(this.present).getConcurrent_observations().get_data().size(); j++) {
                if (subject.equals(this.timeSlots.get(this.present).getConcurrent_observations().get_data().get(j).getContent().getContent())) {
                    TruthValue predicted_observation_truth = TruthFunctions.deduction(this.timeSlots.get(this.present).getConcurrent_observations().get_data().get(j).getContent().getSentence().getTruth(),
                            this.predictionTable.get_data().get(i).getContent().getSentence().getTruth());
                    BudgetFunctions.merge(this.predictionTable.get_data().get(i).getContent().getBudget(),
                            this.timeSlots.get(this.present).getConcurrent_observations().get_data().get(j).getContent().getBudget());
                    BudgetValue predicted_observation_budget = this.predictionTable.get_data().get(i).getContent().getBudget();
                    Sentence sentence = new Sentence(predicate, Symbols.JUDGMENT_MARK, predicted_observation_truth, new Stamp(this.memory.getTime()));
                    Task task = new Task(sentence, predicted_observation_budget);
                    if (interval <= this.num_slot_one_side && interval >= 0) {
                        this.timeSlots.get((int) interval).put_anticipation(new AnticipationMC(this.predictionTable.get_data().get(i).getContent(), task));
                    }
                }
            }
            // this is for checking all the historical (temporal) compounds
            if (this.temporal) {
                for (int j = 0; j < this.timeSlots.get(this.present).getHistorical_observations().get_data().size(); j++) {
                    if (subject.equals(this.timeSlots.get(this.present).getHistorical_observations().get_data().get(j).getContent().getContent())) {
                        TruthValue predicted_observation_truth = TruthFunctions.deduction(this.timeSlots.get(this.present).getHistorical_observations().get_data().get(j).getContent().getSentence().getTruth(),
                                this.predictionTable.get_data().get(i).getContent().getSentence().getTruth());
                        BudgetFunctions.merge(this.predictionTable.get_data().get(i).getContent().getBudget(),
                                this.timeSlots.get(this.present).getHistorical_observations().get_data().get(j).getContent().getBudget());
                        BudgetValue predicted_observation_budget = this.predictionTable.get_data().get(i).getContent().getBudget();
                        Sentence sentence = new Sentence(predicate, Symbols.JUDGMENT_MARK, predicted_observation_truth, new Stamp(this.memory.getTime()));
                        Task task = new Task(sentence, predicted_observation_budget);
                        if (interval <= this.num_slot_one_side && interval >= 0) {
                            this.timeSlots.get((int) interval).put_anticipation(new AnticipationMC(this.predictionTable.get_data().get(i).getContent(), task));
                        }
                    }
                }
            }
        }
        this.timeSlots.get(this.present).check_anticipations(this.predictionTable);
    }

    /**
     * Check all concurrent compounds in the main memory. If it is in the memory, it will get a priority bonus.
     * Different buffers shall use the same memory-based evaluations methods.
     */
    public void memory_based_evaluations() {
        ArrayList<Task> concurrent_observation_updating_list = new ArrayList<Task>();
        ArrayList<Task> historical_observation_updating_list = new ArrayList<Task>();
        // check all concurrent observations with the memory, but this will not influence the memory directly
        for (int i = 0; i < this.timeSlots.get(this.present).getConcurrent_observations().get_data().size(); i++) {
            if (this.memory.getConcepts().contains(memory.nameToConcept(this.timeSlots.get(this.present).getConcurrent_observations().get_data().get(i).getContent().getSentence().getContent().getName()))) {
                Task task = this.timeSlots.get(this.present).getConcurrent_observations().get_data().get(i).getContent();
                BudgetValue previous_budget = task.getBudget();
                // TODO
                BudgetValue new_budget = new BudgetValue(
                        min((float) 0.99, (float) (previous_budget.getPriority() * priority_develop)),
                        min((float) 0.99, (float) (previous_budget.getDurability() * priority_develop)),
                        min((float) 0.99, (float) (previous_budget.getQuality() * priority_develop)));
                concurrent_observation_updating_list.add(new Task(task.getSentence(), new_budget));
            }
        }
        // check all temporal observations with the memory, but this will not influence the memory directly
        if (this.temporal) {
            for (int i = 0; i < this.timeSlots.get(this.present).getHistorical_observations().get_data().size(); i++) {
                if (this.memory.getConcepts().contains(memory.nameToConcept(this.timeSlots.get(this.present).getHistorical_observations().get_data().get(i).getContent().getSentence().getContent().getName()))) {
                    Task task = this.timeSlots.get(this.present).getHistorical_observations().get_data().get(i).getContent();
                    BudgetValue previous_budget = task.getBudget();
                    // TODO
                    BudgetValue new_budget = new BudgetValue(
                            min((float) 0.99, (float) (previous_budget.getPriority() * priority_develop)),
                            min((float) 0.99, (float) (previous_budget.getDurability() * priority_develop)),
                            min((float) 0.99, (float) (previous_budget.getQuality() * priority_develop)));
                    historical_observation_updating_list.add(new Task(task.getSentence(), new_budget));
                }
            }
        }
        // apply the change above
        for (int i = 0; i < concurrent_observation_updating_list.size(); i++) {
            this.timeSlots.get(this.present).put_concurrent_observation(concurrent_observation_updating_list.get(i));
        }
        if (this.temporal) {
            for (int i = 0; i < historical_observation_updating_list.size(); i++) {
                this.timeSlots.get(this.present).put_historical_observation(historical_observation_updating_list.get(i));
            }
        }
    }

    /**
     * Figuring out the present attention, then the buffer shall make predictions about it.
     * The philosophy is that "what makes this attention".
     * Different event buffer may have different prediction generation methods.
     */
    public void prediction_generation() {}

    /**
     * Buffer cycle. Four steps with some pre-processing
     *
     * @param new_contents: atomic events captured by the sensorimotor channel
     * @param show:         for debugging, show the compounds generated and the final event selected
     * @return: the final event selected
     */
    public Task step(ArrayList<Task> new_contents, Boolean show) {
        // new slot generation
        this.timeSlots.remove(0);
        this.timeSlots.add(new SlotMC(this.observation_capacity, this.anticipation_capacity));

        // atomic events input
        if (!new_contents.isEmpty()) {
            for (int i = 0; i < new_contents.size(); i++) {
                if (new_contents.get(i) != null) {
                    this.timeSlots.get(this.present).put_concurrent_observation(new_contents.get(i));
                }
            }
        }

        // four steps
        this.compound_generation();
        // for debugging
        if (show) {
            for (int i = 0; i < this.timeSlots.get(this.present).getConcurrent_observations().get_data().size(); i++) {
                System.out.println(this.timeSlots.get(this.present).getConcurrent_observations().get_data().get(i).getContent().getName());
            }
        }
        this.local_evaluation();
        this.memory_based_evaluations();
        this.prediction_generation();
        // for debugging
        if (show) {
            System.out.println(this.timeSlots.get(this.present).getHighest_compound().getContent().getName());
        }

        return this.timeSlots.get(this.present).getHighest_compound();
    }
}
