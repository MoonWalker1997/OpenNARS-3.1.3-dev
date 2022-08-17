package nars.MC;

import nars.entity.*;
import nars.inference.BudgetFunctions;
import nars.inference.TemporalRules;
import nars.inference.TruthFunctions;
import nars.inference.UtilityFunctions;
import nars.io.Symbols;
import nars.language.*;
import nars.storage.Memory;

import java.util.ArrayList;

import static java.lang.Float.min;

public class OverallBufferMC {

    private double priority_decay = 0.1;
    private double priority_develop = 10;

    private Memory memory = null;

    private int observation_capacity = 0;

    private int anticipation_capacity = 0;

    private int num_slot_one_side = 0;

    private int num_slot = 0;

    private ArrayList<SlotMC> data = new ArrayList<SlotMC>();

    private int curr = 0;

    private PriorityQueueMC predictions = null;

    private double compound_generation_threshold = 0.1;

    static TruthValue abduction(TruthValue v1, TruthValue v2) {
        if (v1.getAnalytic() || v2.getAnalytic()) {
            return new TruthValue(0.5f, 0f);
        }
        float f1 = v1.getFrequency();
        float f2 = v2.getFrequency();
        float c1 = v1.getConfidence();
        float c2 = v2.getConfidence();
        float w = UtilityFunctions.and(f2, c1, c2);
        float c = UtilityFunctions.w2c(w);
        return new TruthValue(f1, c);
    }

    static TruthValue induction(TruthValue v1, TruthValue v2) {
        return abduction(v2, v1);
    }

    public OverallBufferMC(int num_slot, int observation_capacity, int anticipation_capacity, int prediction_capacity, Memory memory) {
        this.memory = memory;
        this.observation_capacity = observation_capacity;
        this.anticipation_capacity = anticipation_capacity;
        this.num_slot_one_side = num_slot;
        this.num_slot = 2 * num_slot + 1;
        for (int i = 0; i < this.num_slot; i++) {
            this.data.add(new SlotMC(observation_capacity, anticipation_capacity));
        }
        this.curr = num_slot;
        this.predictions = new PriorityQueueMC(prediction_capacity);
    }

    // This new version ONLY considers using concurrent compound for episodic information.
    // Code modified from: https://segmentfault.com/a/1190000040142137
    private void dfs(ArrayList<Task> Tasks, int index, int count, boolean[] flag, int n) {
        if (count == 0) {
            ArrayList<Term> tempTerms = new ArrayList<Term>();
            for (int i = 0; i < n; i++) {
                if (flag[i]) {
                    tempTerms.add(Tasks.get(i).getContent());
                }
            }
            Term conpoundTerm = Conjunction.make(tempTerms, TemporalRules.ORDER_CONCURRENT, this.memory);
            char punctuation = Symbols.JUDGMENT_MARK;
            TruthValue truth = Tasks.get(0).getSentence().getTruth();
            for (int i = 1; i < Tasks.size(); i++) {
                truth = induction(truth, Tasks.get(i).getSentence().getTruth());
            }
            Stamp stamp = new Stamp(memory.getTime());
            Sentence sentence = new Sentence(conpoundTerm, punctuation, truth, stamp, true);
            Task compoundTask = new Task(sentence, Tasks.get(0).getBudget());

            this.data.get(this.curr).put_concurrent_observation(compoundTask);
        } else {
            for (int i = index + 1; i < n; i++) {
                flag[i] = true;
                dfs(Tasks, i, count - 1, flag, n);
                flag[i] = false;
            }
        }
    }

    // This old version considers using concurrent compound AND historical compound.
    public void compound_generation() {
        ArrayList<Task> cpd = new ArrayList<Task>();

        ArrayList<Task> concurrentTasks = new ArrayList<>();
        ArrayList<PriorityPairMC> currentTasks = this.data.get(this.curr).getConcurrent_observations().get_data();
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
        if (this.data.get(this.curr).getConcurrent_observations().get_highest(false) != null) {
            this.data.get(this.curr).setHighest_concurrent_compound(this.data.get(this.curr).getConcurrent_observations().get_highest(false).getContent());
        }
    }

    public void local_evaluation() {
        ArrayList<Term> compound_contains = new ArrayList<>();
        for (int i = 0; i < this.data.get(this.curr).getConcurrent_observations().get_data().size(); i++) {
            if (this.data.get(this.curr).getConcurrent_observations().get_data().get(i).getContent().getName().charAt(1) == '&') {
                CompoundTerm cpdt = (CompoundTerm) this.data.get(this.curr).getConcurrent_observations().get_data().get(i).getContent().getContent();
                compound_contains.addAll(cpdt.getComponents());
            }
        }
        ArrayList<Task> revised_tasks = new ArrayList<>();
        for (int i = 0; i < this.data.get(this.curr).getConcurrent_observations().get_data().size(); i++) {
            for (int j = 0; j < compound_contains.size(); j++) {
                if (this.data.get(this.curr).getConcurrent_observations().get_data().get(i).getContent().getContent().equals(compound_contains.get(j))) {
                    Task tmp = this.data.get(this.curr).getConcurrent_observations().get_data().get(i).getContent();
                    float priority = min((float) 0.99, (float) (tmp.getPriority() * priority_decay));
                    float durability = min((float) 0.99, (float) (tmp.getDurability() * priority_decay));
                    float quality = min((float) 0.99, (float) (tmp.getQuality() * priority_decay));
                    BudgetValue revised_budget = new BudgetValue(priority, durability, quality);
                    revised_tasks.add(new Task(tmp.getSentence(), revised_budget));
                    break;
                }
            }
        }
        for (int i = 0; i < revised_tasks.size(); i++) {
            this.data.get(this.curr).put_concurrent_observation(revised_tasks.get(i));
        }

        for (int i = 0; i < this.predictions.get_data().size(); i++) {
            Statement statement = (Statement) this.predictions.get_data().get(i).getContent().getSentence().cloneContent();
            Term subject = statement.getSubject();
            Term predicate = statement.getPredicate();
            long interval = statement.getInterval();
            for (int j = 0; j < this.data.get(this.curr).getConcurrent_observations().get_data().size(); j++) {
                if (subject.equals(this.data.get(this.curr).getConcurrent_observations().get_data().get(j).getContent().getContent())) {
                    TruthValue predicted_observation_truth = TruthFunctions.deduction(this.data.get(this.curr).getConcurrent_observations().get_data().get(j).getContent().getSentence().getTruth(),
                            this.predictions.get_data().get(i).getContent().getSentence().getTruth());
                    BudgetFunctions.merge(this.predictions.get_data().get(i).getContent().getBudget(),
                            this.data.get(this.curr).getConcurrent_observations().get_data().get(j).getContent().getBudget());
                    BudgetValue predicted_observation_budget = this.predictions.get_data().get(i).getContent().getBudget();
                    Sentence sentence = new Sentence(predicate, Symbols.JUDGMENT_MARK, predicted_observation_truth, new Stamp(this.memory.getTime()));
                    Task task = new Task(sentence, predicted_observation_budget);
                    if (interval <= this.num_slot_one_side && interval >= 0) {
                        this.data.get((int) interval).put_anticipation(new AnticipationMC(this.predictions.get_data().get(i).getContent(), task));
                    }
                }
            }
            for (int j = 0; j < this.data.get(this.curr).getHistorical_observations().get_data().size(); j++) {
                if (subject.equals(this.data.get(this.curr).getHistorical_observations().get_data().get(j).getContent().getContent())) {
                    TruthValue predicted_observation_truth = TruthFunctions.deduction(this.data.get(this.curr).getHistorical_observations().get_data().get(j).getContent().getSentence().getTruth(),
                            this.predictions.get_data().get(i).getContent().getSentence().getTruth());
                    BudgetFunctions.merge(this.predictions.get_data().get(i).getContent().getBudget(),
                            this.data.get(this.curr).getHistorical_observations().get_data().get(j).getContent().getBudget());
                    BudgetValue predicted_observation_budget = this.predictions.get_data().get(i).getContent().getBudget();
                    Sentence sentence = new Sentence(predicate, Symbols.JUDGMENT_MARK, predicted_observation_truth, new Stamp(this.memory.getTime()));
                    Task task = new Task(sentence, predicted_observation_budget);
                    if (interval <= this.num_slot_one_side && interval >= 0) {
                        this.data.get((int) interval).put_anticipation(new AnticipationMC(this.predictions.get_data().get(i).getContent(), task));
                    }
                }
            }
        }
        this.data.get(this.curr).check_anticipations(this.predictions);
    }

    public void memory_based_evaluations() {
        ArrayList<Task> concurrent_observation_updating_list = new ArrayList<Task>();
        ArrayList<Task> historical_observation_updating_list = new ArrayList<Task>();
        for (int i = 0; i < this.data.get(this.curr).getConcurrent_observations().get_data().size(); i++) {
            if (this.memory.getConcepts().contains(memory.nameToConcept(this.data.get(this.curr).getConcurrent_observations().get_data().get(i).getContent().getSentence().getContent().getName()))) {
                Task task = this.data.get(this.curr).getConcurrent_observations().get_data().get(i).getContent();
                BudgetValue previous_budget = task.getBudget();
                BudgetValue new_budget = new BudgetValue(
                        min((float) 0.99, (float) (previous_budget.getPriority() * priority_develop)),
                        min((float) 0.99, (float) (previous_budget.getDurability() * priority_develop)),
                        min((float) 0.99, (float) (previous_budget.getQuality() * priority_develop)));
                concurrent_observation_updating_list.add(new Task(task.getSentence(), new_budget));
            }
        }
        for (int i = 0; i < this.data.get(this.curr).getHistorical_observations().get_data().size(); i++) {
            if (this.memory.getConcepts().contains(memory.nameToConcept(this.data.get(this.curr).getHistorical_observations().get_data().get(i).getContent().getSentence().getContent().getName()))) {
                Task task = this.data.get(this.curr).getHistorical_observations().get_data().get(i).getContent();
                BudgetValue previous_budget = task.getBudget();
                BudgetValue new_budget = new BudgetValue(
                        min((float) 0.99, (float) (previous_budget.getPriority() * priority_develop)),
                        min((float) 0.99, (float) (previous_budget.getDurability() * priority_develop)),
                        min((float) 0.99, (float) (previous_budget.getQuality() * priority_develop)));
                historical_observation_updating_list.add(new Task(task.getSentence(), new_budget));
            }
        }
        for (int i = 0; i < concurrent_observation_updating_list.size(); i++) {
            this.data.get(this.curr).put_concurrent_observation(concurrent_observation_updating_list.get(i));
        }
        for (int i = 0; i < historical_observation_updating_list.size(); i++) {
            this.data.get(this.curr).put_historical_observation(historical_observation_updating_list.get(i));
        }
    }

    public void prediction_generation() {
        // set the highest concurrent/historical observation
        if (this.data.get(this.curr).getConcurrent_observations().get_data().size() != 0 && this.data.get(this.curr).getConcurrent_observations().get_data().get(0).getContent() != null) {
            this.data.get(this.curr).setHighest_concurrent_compound(this.data.get(this.curr).getConcurrent_observations().get_highest(false).getContent());
        }
        if (this.data.get(this.curr).getHistorical_observations().get_data().size() != 0 && this.data.get(this.curr).getHistorical_observations().get_data().get(0).getContent() != null) {
            this.data.get(this.curr).setHighest_historical_compound(this.data.get(this.curr).getHistorical_observations().get_highest(false).getContent());
        }

        // find the highest compound (from concurrent and historical highest compound)
        if (this.data.get(this.curr).getHighest_concurrent_compound() != null && this.data.get(this.curr).getHighest_historical_compound() != null) {
            if (UtilityMC.priority(this.data.get(this.curr).getHighest_concurrent_compound()) > UtilityMC.priority(this.data.get(this.curr).getHighest_historical_compound())) {
                this.data.get(this.curr).setHighest_compound(this.data.get(this.curr).getHighest_concurrent_compound());
            } else {
                this.data.get(this.curr).setHighest_compound(this.data.get(this.curr).getHighest_historical_compound());
            }
        } else if (this.data.get(this.curr).getHighest_concurrent_compound() != null) {
            this.data.get(this.curr).setHighest_compound(this.data.get(this.curr).getHighest_concurrent_compound());
        } else if (this.data.get(this.curr).getHighest_historical_compound() != null) {
            this.data.get(this.curr).setHighest_compound(this.data.get(this.curr).getHighest_historical_compound());
        } else {
            return;
        }
        // =/> prediction generation
//        for (int i = 0; i < this.num_slot_one_side; i++) {
//            if (this.data.get(i).getHighest_concurrent_compound() != null) {
//                Term subject = this.data.get(i).getHighest_concurrent_compound().getContent();
//                Term predicate = this.data.get(this.curr).getHighest_compound().getContent();
//                Term term = Implication.make(subject, predicate, TemporalRules.ORDER_BACKWARD, this.curr - i, this.memory);
//                if (term == null) {continue;}
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
//                if (term == null) {continue;}
//                TruthValue truth = induction(this.data.get(i).getHighest_historical_compound().getSentence().getTruth(),
//                        this.data.get(this.curr).getHighest_compound().getSentence().getTruth());
//                BudgetValue budget = this.data.get(this.curr).getHighest_compound().getBudget();
//                Sentence sentence = new Sentence(term, Symbols.JUDGMENT_MARK, truth, new Stamp(this.memory.getTime()));
//                Task task = new Task(sentence, budget);
//                this.predictions.update(new PriorityPairMC(UtilityMC.priority(task), task));
//            }
//        }
        // =|> prediction generation
        ArrayList<Task> predictions = new ArrayList<>();
        for (int i = 0; i < this.data.get(this.curr).getConcurrent_observations().get_data().size(); i++) {
            if (i == 0) {
                continue;
            }
            Term subject = this.data.get(this.curr).getHighest_concurrent_compound().getContent();
            Term predicate = this.data.get(this.curr).getConcurrent_observations().get_data().get(i).getContent().getContent();
            Term term = Implication.make(subject, predicate, TemporalRules.ORDER_CONCURRENT, 0, this.memory);
            if (term != null) {
                TruthValue truth = induction(this.data.get(this.curr).getHighest_concurrent_compound().getSentence().getTruth(),
                        this.data.get(this.curr).getConcurrent_observations().get_data().get(i).getContent().getSentence().getTruth());
                BudgetValue budget = this.data.get(this.curr).getConcurrent_observations().get_data().get(i).getContent().getBudget();
                Sentence sentence = new Sentence(term, Symbols.JUDGMENT_MARK, truth, new Stamp(this.memory.getTime()));
                Task task = new Task(sentence, budget);
                predictions.add(task);
            }
        }
        for (int i = 0; i < predictions.size(); i++) {
            this.predictions.update(new PriorityPairMC(UtilityMC.priority(predictions.get(i)), predictions.get(i)));
        }
    }

    public Task step(ArrayList<Task> new_contents) {

        this.data.remove(0);
        this.data.add(new SlotMC(this.observation_capacity, this.anticipation_capacity));

        if (!new_contents.isEmpty()) {
            for (int i = 0; i < new_contents.size(); i++) {
                if (new_contents.get(i) != null) {
                    this.data.get(this.curr).put_concurrent_observation(new_contents.get(i));
                }
            }
        }
        this.compound_generation();
        this.local_evaluation();
        this.memory_based_evaluations();
        this.prediction_generation();
        return this.data.get(this.curr).getHighest_compound();
    }

    public Boolean isEmpty() {
        return this.data.get(this.curr).getConcurrent_observations().get_data().size() == 0;
    }

}
