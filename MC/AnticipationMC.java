package nars.MC;

import nars.entity.*;
import nars.main.Parameters;

import static java.lang.Float.min;

public class AnticipationMC {

    private Task parent_prediction = null;

    private Task expected_observation = null;

    public AnticipationMC(Task parent_prediction, Task expected_observation) {
        this.parent_prediction = parent_prediction;
        this.expected_observation = expected_observation;
    }

    static float c2w(float c) {
        return Parameters.HORIZON * c / (1 - c);
    }

    static float w2c(float w) {
        return w / (w + Parameters.HORIZON);
    }

    static TruthValue revision(TruthValue v1, TruthValue v2) {
        float f1 = v1.getFrequency();
        float f2 = v2.getFrequency();
        float c1 = v1.getConfidence();
        float c2 = v2.getConfidence();
        float w1 = c2w(c1);
        float w2 = c2w(c2);
        float w = w1 + w2;
        float f = (w1 * f1 + w2 * f2) / w;
        float c = w2c(w);
        return new TruthValue(f, c);
    }

//    static BudgetValue revise(TruthValue tTruth, TruthValue bTruth, TruthValue truth, boolean feedbackToLinks, Memory memory) {
//        float difT = truth.getExpDifAbs(tTruth);
//        Task task = memory.currentTask;
//        task.decPriority(1 - difT);
//        task.decDurability(1 - difT);
//        if (feedbackToLinks) {
//            TaskLink tLink = memory.currentTaskLink;
//            tLink.decPriority(1 - difT);
//            tLink.decDurability(1 - difT);
//            TermLink bLink = memory.currentBeliefLink;
//            float difB = truth.getExpDifAbs(bTruth);
//            bLink.decPriority(1 - difB);
//            bLink.decDurability(1 - difB);
//        }
//        float dif = truth.getConfidence() - Math.max(tTruth.getConfidence(), bTruth.getConfidence());
//        float priority = UtilityFunctions.or(dif, task.getPriority());
//        float durability = UtilityFunctions.aveAri(dif, task.getDurability());
//        float quality = BudgetFunctions.truthToQuality(truth);
//        return new BudgetValue(priority, durability, quality);
//    }

    public void satisfied(PriorityQueueMC predictions) {
        TruthValue add_truth = new TruthValue(1.0f, Parameters.DEFAULT_JUDGMENT_CONFIDENCE);
        TruthValue revised_truth = revision(add_truth, this.parent_prediction.getSentence().getTruth());
        Sentence sentence = this.parent_prediction.getSentence();
        sentence.setTruth(revised_truth);

        float priority = min((float) 0.99, (float) (this.parent_prediction.getPriority()*1.2));
        float durability = min((float) 0.99, (float) (this.parent_prediction.getDurability()*1.2));
        float quality = min((float) 0.99, (float) (this.parent_prediction.getQuality()*1.2));
        BudgetValue revised_budget = new BudgetValue(priority, durability, quality);

        Task task_for_replacement = new Task(sentence, revised_budget);

        predictions.update(new PriorityPairMC(UtilityMC.priority(task_for_replacement), task_for_replacement));
    }

    public void unsatisfied(PriorityQueueMC predictions) {
        TruthValue add_truth = new TruthValue(0.0f, Parameters.DEFAULT_JUDGMENT_CONFIDENCE);
        TruthValue revised_truth = revision(add_truth, this.parent_prediction.getSentence().getTruth());
        Sentence sentence = this.parent_prediction.getSentence();
        sentence.setTruth(revised_truth);

        float priority = min((float) 0.99, (float) (this.parent_prediction.getPriority()*0.8));
        float durability = min((float) 0.99, (float) (this.parent_prediction.getDurability()*0.8));
        float quality = min((float) 0.99, (float) (this.parent_prediction.getQuality()*0.8));
        BudgetValue revised_budget = new BudgetValue(priority, durability, quality);

        Task task_for_replacement = new Task(sentence, revised_budget);

        predictions.update(new PriorityPairMC(UtilityMC.priority(task_for_replacement), task_for_replacement));
    }

    public Task getExpected_observation() {
        return expected_observation;
    }

}
