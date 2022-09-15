package nars.MC;

import nars.entity.*;
import nars.inference.TemporalRules;
import nars.inference.TruthFunctions;
import nars.io.Symbols;
import nars.language.Implication;
import nars.language.Operation;
import nars.language.Operator;
import nars.language.Term;
import nars.storage.Memory;
import java.util.ArrayList;

public class OverallBufferMC extends InputBufferMC {

    public OverallBufferMC(int num_slot, int observation_capacity, int anticipation_capacity, int prediction_capacity, Memory memory, Boolean temporal) {
        super(num_slot, observation_capacity, anticipation_capacity, prediction_capacity, memory, temporal);
    }

    /**
     * As the prediction generation in the overall buffer, it is able to generate concurrent implications (=|>).
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

        // =/> prediction generation
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

        // =|> prediction generation
        // though this is also called "prediction generation", but it is not a "temporal" processing
        // since it is only about "the present time slot"
        ArrayList<Task> predictions = new ArrayList<>();
        for (int i = 0; i < this.timeSlots.get(this.present).getConcurrent_observations().get_data().size(); i++) {
            if (i == 0) {
                continue;
            }
            Term subject = this.timeSlots.get(this.present).getHighest_concurrent_compound().getContent();
            Term predicate = this.timeSlots.get(this.present).getConcurrent_observations().get_data().get(i).getContent().getContent();
            Term term = Implication.make(subject, predicate, TemporalRules.ORDER_CONCURRENT, 0, this.memory);
            if (term != null) {
                TruthValue truth = TruthFunctions.induction(this.timeSlots.get(this.present).getHighest_concurrent_compound().getSentence().getTruth(),
                        this.timeSlots.get(this.present).getConcurrent_observations().get_data().get(i).getContent().getSentence().getTruth());
                BudgetValue budget = this.timeSlots.get(this.present).getConcurrent_observations().get_data().get(i).getContent().getBudget();
                Sentence sentence = new Sentence(term, Symbols.JUDGMENT_MARK, truth, new Stamp(this.memory.getTime()));
                Task task = new Task(sentence, budget);
                predictions.add(task);
            }
        }
        for (int i = 0; i < predictions.size(); i++) {
            this.predictionTable.update(new PriorityPairMC(UtilityMC.priority(predictions.get(i)), predictions.get(i)));
        }
    }

//    /**
//     * A very naive checking of operators, note that "operations" (e.g., a->b) is different from "operators".
//     * And since "operators" has an ambiguous meaning in the current code design, this method is definitely subject to
//     * change in the future.
//     *
//     * @param t: a task for checking, currently, only atomic operations are expected to pass this check, e.g., ^move.
//     * @return: just a Boolean value
//     */
//    private Boolean operator_check(Task t) {
//        return t.getName().charAt(0) == '^';
//    }

//    /**
//     * Overall buffer's buffer cycle needs to consider the input "operation goals".
//     * This will not hurt the previous buffer cycle, but it will add something additionally.
//     * These operation goals will also be forwarded to an output buffer.
//     *
//     * @param new_contents: atomic events captured by the sensorimotor channel
//     * @param show:         for debugging, show the compounds generated and the final event selected
//     * @return: the final event selected
//     */
//    public Task step(ArrayList<Task> new_contents, Boolean show) {
//        // new slot generation
//        this.timeSlots.remove(0);
//        this.timeSlots.add(new SlotMC(this.observation_capacity, this.anticipation_capacity));
//
//        // atomic events input
//        if (!new_contents.isEmpty()) {
//            for (int i = 0; i < new_contents.size(); i++) {
//                if (new_contents.get(i) != null) {
//                    this.timeSlots.get(this.present).put_concurrent_observation(new_contents.get(i));
//                    // check operation goals, TODO
//                    if (this.operator_check(new_contents.get(i)) && new_contents.get(i).getSentence().isGoal()) {
//
//                    }
//                }
//            }
//        }
//
//        // four steps
//        this.compound_generation();
//        // for debugging
//        if (show) {
//            for (int i = 0; i < this.timeSlots.get(this.present).getConcurrent_observations().get_data().size(); i++) {
//                System.out.println(this.timeSlots.get(this.present).getConcurrent_observations().get_data().get(i).getContent().getName());
//            }
//        }
//        this.local_evaluation();
//        this.memory_based_evaluations();
//        this.prediction_generation();
//        // for debugging
//        if (show) {
//            System.out.println(this.timeSlots.get(this.present).getHighest_compound().getContent().getName());
//        }
//
//        return this.timeSlots.get(this.present).getHighest_compound();
//    }

}
