package nars.MC;

import nars.entity.*;
import nars.inference.BudgetFunctions;
import nars.main.Parameters;

import java.util.ArrayList;

import static java.lang.Float.min;

public class SlotMC {

    private int observation_capacity = 0;

    private int anticipation_capacity = 0;

    private PriorityQueueMC concurrent_observations = null;

    private PriorityQueueMC historical_observations = null;

    private PriorityQueueAnticipationsMC anticipations = null;

    private Task highest_concurrent_compound = null;

    public Task getHighest_concurrent_compound() {
        return highest_concurrent_compound;
    }

    public void setHighest_concurrent_compound(Task highest_compound) {
        this.highest_concurrent_compound = highest_compound;
    }

    private Task highest_historical_compound = null;

    public Task getHighest_historical_compound() {
        return highest_historical_compound;
    }

    public void setHighest_historical_compound(Task highest_historical_compound) {
        this.highest_historical_compound = highest_historical_compound;
    }

    private Task highest_compound = null;

    public Task getHighest_compound() {
        return highest_compound;
    }

    public void setHighest_compound(Task highest_compound) {
        this.highest_compound = highest_compound;
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

//    static BudgetValue revise(TruthValue tTruth, TruthValue bTruth, TruthValue truth, boolean feedbackToLinks,
//                              Memory memory) {
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

    public SlotMC(int observation_capacity, int anticipation_capacity) {
        this.observation_capacity = observation_capacity;
        this.anticipation_capacity = anticipation_capacity;
        this.concurrent_observations = new PriorityQueueMC(observation_capacity);
        this.historical_observations = new PriorityQueueMC(observation_capacity);
        this.anticipations = new PriorityQueueAnticipationsMC(anticipation_capacity);
    }

    public void put_concurrent_observation(Task t) {
        this.concurrent_observations.update(new PriorityPairMC(UtilityMC.priority(t), t));
    }

    public void put_historical_observation(Task t) {
        this.historical_observations.update(new PriorityPairMC(UtilityMC.priority(t), t));
    }

    public void put_anticipation(AnticipationMC a) {
        this.anticipations.update(new PriorityPairAnticipationsMC(UtilityMC.priority(a.getExpected_observation()), a));
    }

    public void check_anticipations(PriorityQueueMC predictions) {
        ArrayList<Task> observation_updating_list = new ArrayList<Task>();
        ArrayList<Task> unexpected_observations = new ArrayList<Task>();
        ArrayList<Integer> satisfied_anticipations = new ArrayList<Integer>();
        for (int i = 0; i < this.concurrent_observations.get_data().size(); i++) {
            boolean matched = false;
            Task each_observation = this.concurrent_observations.get_data().get(i).getContent();
            for (int j = 0; j < this.anticipations.get_data().size(); j++) {
                if (satisfied_anticipations.contains(j)) {
                    continue;
                }
                AnticipationMC each_anticipation = this.anticipations.get_data().get(j).getContent();
                if (each_observation.getSentence().equals(each_anticipation.getExpected_observation().getSentence())) {
                    TruthValue revised_truth = revision(each_observation.getSentence().getTruth(),
                            each_anticipation.getExpected_observation().getSentence().getTruth());
                    each_observation.getSentence().setTruth(revised_truth);
                    Sentence s = each_observation.getSentence();
                    BudgetFunctions.merge(each_observation.getBudget(),
                            each_anticipation.getExpected_observation().getBudget());
                    BudgetValue b = each_observation.getBudget();
                    observation_updating_list.add(new Task(s, b));
                    matched = true;
                    satisfied_anticipations.add(j);
                    break;
                }
            }
            if (!matched) {
                unexpected_observations.add(new Task(each_observation.getSentence(), each_observation.getBudget()));
            }
        }
        for (int i = 0; i < observation_updating_list.size(); i++) {
            put_concurrent_observation(observation_updating_list.get(i));
        }
        for (int i = 0; i < unexpected_observations.size(); i++) {
            BudgetValue previous_budget = unexpected_observations.get(i).getBudget();
            unexpected_observations.get(i).setBudget(new BudgetValue(
                    min((float) 0.99, (float) (previous_budget.getPriority() * 1.2)),
                    min((float) 0.99, (float) (previous_budget.getDurability() * 1.2)),
                    min((float) 0.99, (float) (previous_budget.getQuality() * 1.2))));
            put_concurrent_observation(unexpected_observations.get(i));
        }
        // do the same thing for this.historical_observations
        observation_updating_list = new ArrayList<Task>();
        unexpected_observations = new ArrayList<Task>();
        for (int i = 0; i < this.historical_observations.get_data().size(); i++) {
            boolean matched = false;
            Task each_observation = this.historical_observations.get_data().get(i).getContent();
            for (int j = 0; j < this.anticipations.get_data().size(); j++) {
                if (satisfied_anticipations.contains(j)) {
                    continue;
                }
                AnticipationMC each_anticipation = this.anticipations.get_data().get(j).getContent();
                if (each_observation.getSentence().equals(each_anticipation.getExpected_observation().getSentence())) {
                    TruthValue revised_truth = revision(each_observation.getSentence().getTruth(),
                            each_anticipation.getExpected_observation().getSentence().getTruth());
                    each_observation.getSentence().setTruth(revised_truth);
                    Sentence s = each_observation.getSentence();
                    BudgetFunctions.merge(each_observation.getBudget(),
                            each_anticipation.getExpected_observation().getBudget());
                    BudgetValue b = each_observation.getBudget();
                    observation_updating_list.add(new Task(s, b));
                    matched = true;
                    satisfied_anticipations.add(j);
                    break;
                }
            }
            if (!matched) {
                unexpected_observations.add(new Task(each_observation.getSentence(), each_observation.getBudget()));
            }
        }
        for (int i = 0; i < observation_updating_list.size(); i++) {
            put_historical_observation(observation_updating_list.get(i));
        }
        for (int i = 0; i < unexpected_observations.size(); i++) {
            BudgetValue previous_budget = unexpected_observations.get(i).getBudget();
            unexpected_observations.get(i).setBudget(new BudgetValue(
                    min((float) 0.99, (float) (previous_budget.getPriority() * 1.2)),
                    min((float) 0.99, (float) (previous_budget.getDurability() * 1.2)),
                    min((float) 0.99, (float) (previous_budget.getQuality() * 1.2))));
            put_historical_observation(unexpected_observations.get(i));
        }


    }

    public PriorityQueueMC getConcurrent_observations() {
        return concurrent_observations;
    }

    public PriorityQueueMC getHistorical_observations() {
        return historical_observations;
    }

}
