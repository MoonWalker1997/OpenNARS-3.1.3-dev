package nars.MC;

import java.util.ArrayList;

/*
* Agenda is a
* */

public class AgendaMC {

    private int remaining_time = 0;

    private ArrayList<OperationMC> todo_list = new ArrayList<>();

    public AgendaMC(int remaining_time, ArrayList<OperationMC> todo_list) {
        this.remaining_time = remaining_time;
        this.todo_list = todo_list;
    }

    public void execute() {
        for (int i=0;i<this.todo_list.size();i++) {
            this.todo_list.get(i).execute();
        }
    }

    public void count_down() {
        this.remaining_time -= 1;
    }
}
