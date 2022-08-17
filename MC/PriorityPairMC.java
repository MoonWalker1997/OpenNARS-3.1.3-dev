package nars.MC;


import nars.entity.Anticipation;
import nars.entity.Task;

public class PriorityPairMC {

    private double priority_value = 0;
    private Task content = null;

    public double getPriority_value() {
        return priority_value;
    }

    public Task getContent() {
        return content;
    }

    public PriorityPairMC(double priority_value, Task content) {
        this.priority_value = priority_value;
        this.content = content;
    }
}
