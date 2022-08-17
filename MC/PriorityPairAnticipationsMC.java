package nars.MC;

import nars.entity.Anticipation;

public class PriorityPairAnticipationsMC {
    private double priority_value = 0;
    private AnticipationMC content = null;

    public double getPriority_value() {
        return priority_value;
    }

    public AnticipationMC getContent() {
        return content;
    }

    public PriorityPairAnticipationsMC(double priority_value, AnticipationMC content) {
        this.priority_value = priority_value;
        this.content = content;
    }
}
