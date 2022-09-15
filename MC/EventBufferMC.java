package nars.MC;

import nars.entity.*;
import nars.inference.TemporalRules;
import nars.inference.TruthFunctions;
import nars.io.Symbols;
import nars.language.*;
import nars.storage.Memory;

import java.util.ArrayList;

public class EventBufferMC extends InputBufferMC {

    protected double priority_decay = 0.1; // when the priority will be punished, the default hyperparameter
    protected double priority_develop = 10; // when the priority will be rewarded, the default hyperparameter

    public EventBufferMC(int num_slot, int observation_capacity, int anticipation_capacity,
                         int prediction_capacity, Memory memory, Boolean temporal) {
        super(num_slot, observation_capacity, anticipation_capacity, prediction_capacity, memory, temporal);
    }

    /**
     * Considers using concurrent compounds only.
     * For n atomic input events, 2^n all combinations will be generated, and top-n of them will be kept.
     */
    public void compound_generation() {
//        ArrayList<Task> cpd = new ArrayList<Task>();

        ArrayList<Task> concurrentTasks = new ArrayList<>();
        ArrayList<PriorityPairMC> currentTasks = this.timeSlots.get(this.present).getConcurrent_observations().get_data();
        for (int i = 0; i < currentTasks.size(); i++) {
            Task singleTask = currentTasks.get(i).getContent();
            concurrentTasks.add(singleTask);
        }
//        ArrayList<Task> compoundTasks = new ArrayList<>();
        int N = concurrentTasks.size();
        boolean[] flag = new boolean[N];
        for (int k = 1; k <= N; k++) {
            dfs(concurrentTasks, -1, k, flag, N);
        }
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

        // prediction generation is usually a temporal processing, since it is about the prediction from one time slot
        // to another
        if (this.temporal) {
            for (int i = 0; i < this.num_slot_one_side; i++) {
                if (this.timeSlots.get(i).getHighest_concurrent_compound() != null) {
                    Term subject = this.timeSlots.get(i).getHighest_concurrent_compound().getContent();
                    Term predicate = this.timeSlots.get(this.present).getHighest_compound().getContent();
                    Term term = Implication.make(subject, predicate, TemporalRules.ORDER_BACKWARD, this.present - i, this.memory);
                    if (term == null) {
                        continue;
                    }
                    TruthValue truth = TruthFunctions.induction(this.timeSlots.get(i).getHighest_concurrent_compound().getSentence().getTruth(),
                            this.timeSlots.get(this.present).getHighest_compound().getSentence().getTruth());
                    BudgetValue budget = this.timeSlots.get(this.present).getHighest_compound().getBudget();
                    Sentence sentence = new Sentence(term, Symbols.JUDGMENT_MARK, truth, new Stamp(this.memory.getTime()));
                    Task task = new Task(sentence, budget);
                    this.predictionTable.update(new PriorityPairMC(UtilityMC.priority(task), task));
                }
                if (this.timeSlots.get(i).getHighest_historical_compound() != null) {
                    Term subject = this.timeSlots.get(i).getHighest_historical_compound().getContent();
                    Term predicate = this.timeSlots.get(this.present).getHighest_compound().getContent();
                    Term term = Implication.make(subject, predicate, TemporalRules.ORDER_BACKWARD, this.present - i, this.memory);
                    if (term == null) {
                        continue;
                    }
                    TruthValue truth = TruthFunctions.induction(this.timeSlots.get(i).getHighest_historical_compound().getSentence().getTruth(),
                            this.timeSlots.get(this.present).getHighest_compound().getSentence().getTruth());
                    BudgetValue budget = this.timeSlots.get(this.present).getHighest_compound().getBudget();
                    Sentence sentence = new Sentence(term, Symbols.JUDGMENT_MARK, truth, new Stamp(this.memory.getTime()));
                    Task task = new Task(sentence, budget);
                    this.predictionTable.update(new PriorityPairMC(UtilityMC.priority(task), task));
                }
            }
        }
    }
}
