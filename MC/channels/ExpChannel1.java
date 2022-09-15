package nars.MC.channels;

import nars.MC.EventBufferMC;
import nars.MC.OperationMC;
import nars.MC.SensorimotorChannelMC;
import nars.entity.Task;
import nars.io.StringParser;
import nars.storage.Memory;

import java.util.ArrayList;

public class ExpChannel1 extends SensorimotorChannelMC {

    private int counter = 0;

    public ExpChannel1(String channel_ID, EventBufferMC eventBuffer, Memory memory) {
        super(channel_ID, eventBuffer, memory);
    }

    @Override
    protected ArrayList<Task> gathering() {
        if (counter < 400) {
            ArrayList<Task> tasks = new ArrayList<>();
            tasks.add(StringParser.parseTask("<(*,{Channel_1}, sting) --> ^see>. :|:", this.memory, memory.getTime()));
            System.out.println("Time " + this.counter + " | Input from Channel #1: " + "<(*,{Channel_1}, sting) --> ^see>. :|:");
            counter += 1;
            return tasks;
        } else {
            ArrayList<Task> tasks = new ArrayList<>();
            return tasks;
        }
    }

    @Override
    public void execute(String name) {}
}
