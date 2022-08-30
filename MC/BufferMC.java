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

    private double priority_decay; // when the priority will be punished, the default hyperparameter
    private double priority_develop; // when the priority will be rewarded, the default hyperparameter

    // since each buffer needs to communicate with the main memory, here is a reference
    // make it static since different buffers share the same memory reference
    private Memory memory;

    // 0 initialization for the storage hyperparameters
    private int observation_capacity;
    private int anticipation_capacity;
    private int num_slot_one_side;
    private int num_slot;

    private ArrayList<SlotMC> timeSlots; // first part of the buffer, time slots
    private PriorityQueueMC predictionTable; // second part of the buffer, prediction table

    private int present; // an indicator of the index of the present time slot

    // can be used to filter out the compound with a very low priority
//     private double compound_generation_threshold = 0.1;

    // abduction truth-value function, which is already in NARS
//    static TruthValue abduction(TruthValue v1, TruthValue v2) {
//        if (v1.getAnalytic() || v2.getAnalytic()) {
//            return new TruthValue(0.5f, 0f);
//        }
//        float f1 = v1.getFrequency();
//        float f2 = v2.getFrequency();
//        float c1 = v1.getConfidence();
//        float c2 = v2.getConfidence();
//        float w = UtilityFunctions.and(f2, c1, c2);
//        float c = UtilityFunctions.w2c(w);
//        return new TruthValue(f1, c);
//    }
//
//    static TruthValue induction(TruthValue v1, TruthValue v2) {
//        return abduction(v2, v1);
//    }

    public BufferMC(int num_slot, int observation_capacity, int anticipation_capacity,
                         int prediction_capacity, Memory memory) {

        this.memory = memory;
        this.observation_capacity = observation_capacity;
        this.anticipation_capacity = anticipation_capacity;

        this.num_slot_one_side = num_slot;
        this.num_slot = 2 * num_slot + 1;

        for (int i = 0; i < this.num_slot; i++) {
            this.timeSlots.add(new SlotMC(observation_capacity, anticipation_capacity));
        }
        this.present = num_slot;
        this.predictionTable = new PriorityQueueMC(prediction_capacity);
    }

    /**
     * For n atomic events, this method will recursively give 2^n combinations to this.concurrent_observations.
     * Code modified from: https://segmentfault.com/a/1190000040142137.
     *
     * @param Tasks: atomic events to generate compounds
     * @param n:     num of atomic events
     */
    private void dfs(ArrayList<Task> Tasks, int index, int count, boolean[] flag, int n) {
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
     * Considers using concurrent compounds only, so there are only concurrent compounds generated here.
     * For n atomic input events, 2^n all combinations will be generated, and top-n of them will be kept.
     */
    public void compound_generation() {
        ArrayList<Task> cpd = new ArrayList<Task>();

        ArrayList<Task> concurrentTasks = new ArrayList<>();
        ArrayList<PriorityPairMC> currentTasks = this.timeSlots.get(this.present).getConcurrent_observations().get_data();
        for (int i = 0; i < currentTasks.size(); i++) {
            Task singleTask = currentTasks.get(i).getContent();
            concurrentTasks.add(singleTask);
        }
        ArrayList<Task> compoundTasks = new ArrayList<>();
        int N = concurrentTasks.size();
        boolean[] flag = new boolean[N];
        for (int k = 1; k <= N; k++) {
            dfs(concurrentTasks, -1, k, flag, N);
        }
        if (this.timeSlots.get(this.present).getConcurrent_observations().get_highest(false) != null) {
            this.timeSlots.get(this.present).setHighest_concurrent_compound(this.timeSlots.get(this.present).getConcurrent_observations().get_highest(false).getContent());
        }
    }

    /**
     * All "priority" mentioned below is not the priority in budget.
     * Four steps:
     * 1. If components and compounds coexist, components will get a priority penalty.
     * 2. Check all predictions, applicable predictions will fire.
     * 3. Check all anticipations, satisfied anticipation will get a priority bonus and one more positive evidence.
     * Unsatisfied anticipations will get a priority decay and one more negative evidence.
     * 4. Unexpected observations will get a priority bonus.
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
//            for (int j = 0; j < this.timeSlots.get(this.present).getHistorical_observations().get_data().size(); j++) {
//                if (subject.equals(this.timeSlots.get(this.present).getHistorical_observations().get_data().get(j).getContent().getContent())) {
//                    TruthValue predicted_observation_truth = TruthFunctions.deduction(this.timeSlots.get(this.present).getHistorical_observations().get_data().get(j).getContent().getSentence().getTruth(),
//                            this.predictionTable.get_data().get(i).getContent().getSentence().getTruth());
//                    BudgetFunctions.merge(this.predictionTable.get_data().get(i).getContent().getBudget(),
//                            this.timeSlots.get(this.present).getHistorical_observations().get_data().get(j).getContent().getBudget());
//                    BudgetValue predicted_observation_budget = this.predictionTable.get_data().get(i).getContent().getBudget();
//                    Sentence sentence = new Sentence(predicate, Symbols.JUDGMENT_MARK, predicted_observation_truth, new Stamp(this.memory.getTime()));
//                    Task task = new Task(sentence, predicted_observation_budget);
//                    if (interval <= this.num_slot_one_side && interval >= 0) {
//                        this.timeSlots.get((int) interval).put_anticipation(new AnticipationMC(this.predictionTable.get_data().get(i).getContent(), task));
//                    }
//                }
//            }
        }
        this.timeSlots.get(this.present).check_anticipations(this.predictionTable);
    }

    /**
     * Check all concurrent compounds in the main memory. If it is in the memory, it will get a priority bonus.
     */
    public void memory_based_evaluations() {
        ArrayList<Task> concurrent_observation_updating_list = new ArrayList<Task>();
//        ArrayList<Task> historical_observation_updating_list = new ArrayList<Task>();
        // check all concurrent observations with the memory, but the priority merge is not applied here
        for (int i = 0; i < this.timeSlots.get(this.present).getConcurrent_observations().get_data().size(); i++) {
            if (this.memory.getConcepts().contains(memory.nameToConcept(this.timeSlots.get(this.present).getConcurrent_observations().get_data().get(i).getContent().getSentence().getContent().getName()))) {
                Task task = this.timeSlots.get(this.present).getConcurrent_observations().get_data().get(i).getContent();
                BudgetValue previous_budget = task.getBudget();
                BudgetValue new_budget = new BudgetValue(
                        min((float) 0.99, (float) (previous_budget.getPriority() * priority_develop)),
                        min((float) 0.99, (float) (previous_budget.getDurability() * priority_develop)),
                        min((float) 0.99, (float) (previous_budget.getQuality() * priority_develop)));
                concurrent_observation_updating_list.add(new Task(task.getSentence(), new_budget));
            }
        }
//        for (int i = 0; i < this.timeSlots.get(this.present).getHistorical_observations().get_data().size(); i++) {
//            if (this.memory.getConcepts().contains(memory.nameToConcept(this.timeSlots.get(this.present).getHistorical_observations().get_data().get(i).getContent().getSentence().getContent().getName()))) {
//                Task task = this.timeSlots.get(this.present).getHistorical_observations().get_data().get(i).getContent();
//                BudgetValue previous_budget = task.getBudget();
//                BudgetValue new_budget = new BudgetValue(
//                        min((float) 0.99, (float) (previous_budget.getPriority() * priority_develop)),
//                        min((float) 0.99, (float) (previous_budget.getDurability() * priority_develop)),
//                        min((float) 0.99, (float) (previous_budget.getQuality() * priority_develop)));
//                historical_observation_updating_list.add(new Task(task.getSentence(), new_budget));
//            }
//        }
        for (int i = 0; i < concurrent_observation_updating_list.size(); i++) {
            this.timeSlots.get(this.present).put_concurrent_observation(concurrent_observation_updating_list.get(i));
        }
//        for (int i = 0; i < historical_observation_updating_list.size(); i++) {
//            this.timeSlots.get(this.present).put_historical_observation(historical_observation_updating_list.get(i));
//        }
    }

    /**
     * Figure out the highest concurrent compound and figure out the highest compound (considering the highest temporal compound).
     * Then create predictions with the highest compound as the predict.
     */
    public void prediction_generation() {
        // set the highest concurrent/historical observation
        if (this.timeSlots.get(this.present).getConcurrent_observations().get_data().size() != 0 && this.timeSlots.get(this.present).getConcurrent_observations().get_data().get(0).getContent() != null) {
            this.timeSlots.get(this.present).setHighest_concurrent_compound(this.timeSlots.get(this.present).getConcurrent_observations().get_highest(false).getContent());
        }
        if (this.timeSlots.get(this.present).getHistorical_observations().get_data().size() != 0 && this.timeSlots.get(this.present).getHistorical_observations().get_data().get(0).getContent() != null) {
            this.timeSlots.get(this.present).setHighest_historical_compound(this.timeSlots.get(this.present).getHistorical_observations().get_highest(false).getContent());
        }

        // find the highest compound (from concurrent and historical highest compound)
        if (this.timeSlots.get(this.present).getHighest_concurrent_compound() != null && this.timeSlots.get(this.present).getHighest_historical_compound() != null) {
            if (UtilityMC.priority(this.timeSlots.get(this.present).getHighest_concurrent_compound()) > UtilityMC.priority(this.timeSlots.get(this.present).getHighest_historical_compound())) {
                this.timeSlots.get(this.present).setHighest_compound(this.timeSlots.get(this.present).getHighest_concurrent_compound());
            } else {
                this.timeSlots.get(this.present).setHighest_compound(this.timeSlots.get(this.present).getHighest_historical_compound());
            }
        } else if (this.timeSlots.get(this.present).getHighest_concurrent_compound() != null) {
            this.timeSlots.get(this.present).setHighest_compound(this.timeSlots.get(this.present).getHighest_concurrent_compound());
        } else if (this.timeSlots.get(this.present).getHighest_historical_compound() != null) {
            this.timeSlots.get(this.present).setHighest_compound(this.timeSlots.get(this.present).getHighest_historical_compound());
        } else {
            return;
        }
        // prediction generation is a temporal behavior, so it is temporally disabled
//        for (int i = 0; i < this.num_slot_one_side; i++) {
//            if (this.data.get(i).getHighest_concurrent_compound() != null) {
//                Term subject = this.data.get(i).getHighest_concurrent_compound().getContent();
//                Term predicate = this.data.get(this.curr).getHighest_compound().getContent();
//                Term term = Implication.make(subject, predicate, TemporalRules.ORDER_BACKWARD, this.curr - i, this.memory);
//                if (term == null) {
//                    continue;
//                }
//                TruthValue truth = induction(this.data.get(i).getHighest_concurrent_compound().getSentence().getTruth(),
//                        this.data.get(this.curr).getHighest_compound().getSentence().getTruth());
//                BudgetValue budget = this.data.get(this.curr).getHighest_compound().getBudget();
//                Sentence sentence = new Sentence(term, Symbols.JUDGMENT_MARK, truth, new Stamp(this.memory.getTime()));
//                Task task = new Task(sentence, budget);
//                this.predictions.update(new PriorityPairMC(UtilityMC.priority(task), task));
//            }
//            if (this.data.get(i).getHighest_historical_compound() != null) {
//                Term subject = this.data.get(i).getHighest_historical_compound().getContent();
//                Term predicate = this.data.get(this.curr).getHighest_compound().getContent();
//                Term term = Implication.make(subject, predicate, TemporalRules.ORDER_BACKWARD, this.curr - i, this.memory);
//                if (term == null) {
//                    continue;
//                }
//                TruthValue truth = induction(this.data.get(i).getHighest_historical_compound().getSentence().getTruth(),
//                        this.data.get(this.curr).getHighest_compound().getSentence().getTruth());
//                BudgetValue budget = this.data.get(this.curr).getHighest_compound().getBudget();
//                Sentence sentence = new Sentence(term, Symbols.JUDGMENT_MARK, truth, new Stamp(this.memory.getTime()));
//                Task task = new Task(sentence, budget);
//                this.predictions.update(new PriorityPairMC(UtilityMC.priority(task), task));
//            }
//        }
    }

    /**
     * Buffer cycle. Four steps with some pre-processing
     * @param new_contents: atomic events captured by the sensorimotor channel
     * @param show: for debugging, show the compounds generated and the final event selected
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

        this.compound_generation();
        // for debugging
//        if (show) {
//            for (int i = 0; i < this.data.get(this.curr).getConcurrent_observations().get_data().size(); i++) {
//                System.out.println(this.data.get(this.curr).getConcurrent_observations().get_data().get(i).getContent().getName());
//            }
//        }
        this.local_evaluation();
        this.memory_based_evaluations();
        this.prediction_generation();

        // for debugging
//        if (show) {
//            System.out.println(this.timeSlots.get(this.present).getHighest_compound().getContent().getName());
//        }

        return this.timeSlots.get(this.present).getHighest_compound();
    }

    /**
     * Check whether this.concurrent_observations is empty, usually used for checking whether the channel has
     * some initial atomic inputs at the beginning.
     * @return: whether this.concurrent_observations is empty
     */
    public Boolean isEmpty() {
        return this.timeSlots.get(this.present).getConcurrent_observations().get_data().size() == 0;
    }
}
