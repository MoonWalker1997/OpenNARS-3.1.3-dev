package nars.MC;

import nars.entity.Task;

public class UtilityMC {

    public UtilityMC() {}

    public static double priority(Task t) {
//        return t.getBudget().summary() * t.getSentence().getTruth().getExpectation() /
//                Math.pow(t.getSentence().getContent().getComplexity(), 2);
        return t.getBudget().summary() * t.getSentence().getTruth().getExpectation() /
                Math.pow(t.getSentence().getContent().getComplexity(), 4);
    }

}
