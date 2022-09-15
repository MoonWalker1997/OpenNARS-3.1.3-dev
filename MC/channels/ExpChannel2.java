package nars.MC.channels;

import nars.MC.EventBufferMC;
import nars.MC.OperationMC;
import nars.MC.SensorimotorChannelMC;
import nars.entity.Task;
import nars.io.StringParser;
import nars.storage.Memory;

import java.util.ArrayList;

public class ExpChannel2 extends SensorimotorChannelMC {

    private int counter = 0;

    private ArrayList<OperationMC> atomic_operations = null;

    public ExpChannel2(String channel_ID, EventBufferMC eventBuffer, Memory memory) {
        super(channel_ID, eventBuffer, memory);
    }

    @Override
    protected ArrayList<Task> gathering() {
        if (counter <= 100) {
            ArrayList<Task> tasks = new ArrayList<>();
            tasks.add(StringParser.parseTask("<(*,{Channel_2}, pain) --> ^feel>. :|:", this.memory, memory.getTime()));
            System.out.println("Time " + this.counter + " | Input from Channel #2: " + "<(*,{Channel_2}, pain) --> ^feel>. :|:");
            counter += 1;
            return tasks;
        }
        else if (counter <= 300) {
            ArrayList<Task> tasks = new ArrayList<>();
            tasks.add(StringParser.parseTask("<(*,{Channel_2}, nothing) --> ^feel>. :|:", this.memory, memory.getTime()));
            System.out.println("Time " + this.counter + " | Input from Channel #2: " + "<(*,{Channel_2}, nothing) --> ^feel>. :|:");
            counter += 1;
            return tasks;
        }
        else if (counter < 400) {
            ArrayList<Task> tasks = new ArrayList<>();
            tasks.add(StringParser.parseTask("<(*,{Channel_2}, pain) --> ^feel>. :|:", this.memory, memory.getTime()));
            System.out.println("Time " + this.counter + " | Input from Channel #2: " + "<(*,{Channel_2}, pain) --> ^feel>. :|:");
            counter += 1;
            return tasks;
        }
        else {
            ArrayList<Task> tasks = new ArrayList<>();
            return tasks;
        }
    }

    @Override
    public void execute(String name) {}
}
