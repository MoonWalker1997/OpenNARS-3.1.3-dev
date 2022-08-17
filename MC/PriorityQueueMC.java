package nars.MC;

import java.util.ArrayList;

public class PriorityQueueMC {

    /*
    Maybe this is not a good name, since it not only supports to get the maximum, it also needs to get the minimum,
    and it needs to change the priority value of existed instance and maintain the order.
     */

    private int capacity = 0;

    private ArrayList<PriorityPairMC> data = new ArrayList<PriorityPairMC>();

    public PriorityQueueMC(int capacity) {
        this.capacity = capacity;
    }

    public void update(PriorityPairMC new_pair) {
        Boolean added = false;
        for (int i = 0; i < this.data.size(); i++) {
            // it exists, delete first
            if (new_pair.getContent().getContent().equals(this.data.get(i).getContent().getContent())) {
                this.data.remove(i);
                break;
            }
        }
        for (int i = 0; i < this.data.size(); i++) {
            if (new_pair.getPriority_value() > this.data.get(i).getPriority_value()) {
                this.data.add(i, new_pair);
                added = true;
                break;
            }
        }
        if (!added) {
            this.data.add(this.data.size(), new_pair);
        }
        if (this.data.size() > this.capacity) {
            this.data.remove(this.data.size() - 1);
        }

    }

    public PriorityPairMC get_highest(Boolean rmv) {
        if (this.data.size() == 0) {
            return null;
        }
        if (!rmv) {
            return this.data.get(0);
        }
        else {
            PriorityPairMC tmp = data.get(0);
            this.data.remove(0);
            return tmp;
        }
    }

    public ArrayList<PriorityPairMC> get_data() {
        return this.data;
    }

}
