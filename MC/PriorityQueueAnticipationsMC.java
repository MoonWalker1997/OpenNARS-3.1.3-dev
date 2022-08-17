package nars.MC;

import java.util.ArrayList;

public class PriorityQueueAnticipationsMC {

    private int capacity = 0;

    private ArrayList<PriorityPairAnticipationsMC> data = new ArrayList<PriorityPairAnticipationsMC>();

    public PriorityQueueAnticipationsMC(int capacity) {
        this.capacity = capacity;
    }

    public void update(PriorityPairAnticipationsMC new_pair) {
        for (int i = 0; i < this.data.size(); i++) {
            if (new_pair.getContent().getExpected_observation().getContent().equals(
                    this.data.get(i).getContent().getExpected_observation().getContent())) {
                this.data.remove(i);
                break;
            }
        }
        for (int i = 0; i < this.data.size(); i++) {
            if (new_pair.getPriority_value() > this.data.get(i).getPriority_value()) {
                this.data.add(i, new_pair);
            }
        }
        if (this.data.size() > this.capacity) {
            this.data.remove(this.data.size() - 1);
        }

    }

    public PriorityPairAnticipationsMC get_highest(Boolean rmv) {
        if (this.data.size() == 0) {
            return null;
        }
        if (!rmv) {
            return this.data.get(0);
        } else {
            PriorityPairAnticipationsMC tmp = data.get(0);
            this.data.remove(0);
            return tmp;
        }
    }

    public ArrayList<PriorityPairAnticipationsMC> get_data() {
        return this.data;
    }

}
