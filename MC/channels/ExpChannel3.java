package nars.MC.channels;

import nars.MC.EventBufferMC;
import nars.MC.OperationMC;
import nars.MC.SensorimotorChannelMC;
import nars.entity.Task;
import nars.io.StringParser;
import nars.storage.Memory;

import java.util.ArrayList;

public class ExpChannel3 extends SensorimotorChannelMC {

    private int counter = 0;

    private ArrayList<OperationMC> atomic_operations = null;

    public ExpChannel3(String channel_ID, EventBufferMC eventBuffer, Memory memory) {
        super(channel_ID, eventBuffer, memory);
    }

    @Override
    protected ArrayList<Task> gathering() {
        if (counter <= 100) {
            ArrayList<Task> tasks = new ArrayList<>();
            tasks.add(StringParser.parseTask("<(*,{Channel_3}, nothing) --> ^see>. :|:", this.memory, memory.getTime()));
            System.out.println("Time " + this.counter + " | Input from Channel #3: " + "<(*,{Channel_3}, nothing) --> ^see>. :|:");
            counter += 1;
            return tasks;
        }
        else if (counter <= 300) {
            ArrayList<Task> tasks = new ArrayList<>();
            tasks.add(StringParser.parseTask("<(*,{Channel_3}, medicine) --> ^see>. :|:", this.memory, memory.getTime()));
            System.out.println("Time " + this.counter + " | Input from Channel #3: " + "<(*,{Channel_3}, medicine) --> ^see>. :|:");
            counter += 1;
            return tasks;
        }
        else if (counter < 400) {
            ArrayList<Task> tasks = new ArrayList<>();
            tasks.add(StringParser.parseTask("<(*,{Channel_3}, nothing) --> ^see>. :|:", this.memory, memory.getTime()));
            System.out.println("Time " + this.counter + " | Input from Channel #3: " + "<(*,{Channel_3}, nothing) --> ^see>. :|:");
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
