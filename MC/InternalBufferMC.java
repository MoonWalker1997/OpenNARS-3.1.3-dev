package nars.MC;

import nars.entity.*;
import nars.inference.TemporalRules;
import nars.inference.TruthFunctions;
import nars.io.Symbols;
import nars.language.Implication;
import nars.language.Term;
import nars.storage.Memory;

import java.util.ArrayList;

public class InternalBufferMC extends InputBufferMC {

    private OperationOutputBuffer OOB = null;

    public void setOOB(OperationOutputBuffer OOB) {
        this.OOB = OOB;
    }

    public InternalBufferMC(int num_slot, int observation_capacity, int anticipation_capacity, int prediction_capacity, Memory memory, Boolean temporal) {
        super(num_slot, observation_capacity, anticipation_capacity, prediction_capacity, memory, temporal);
    }

    /**
     * As the compound generation method in the internal buffer, it is assumed to consider the mental operations carried out.
     * Though they are not here yet. // TODO
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

        if (this.timeSlots.get(this.present).getHighest_compound() != null) {
            if (this.timeSlots.get(this.present).getHighest_compound().getName().equals("^move") || this.timeSlots.get(this.present).getHighest_compound().getName().equals("^turn")) {
                this.OOB.distribute_and_execute(this.timeSlots.get(this.present).getHighest_compound());
            }
        }

        return this.timeSlots.get(this.present).getHighest_compound();
    }
}
