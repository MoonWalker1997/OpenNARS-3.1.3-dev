package nars.MC.channels;

import nars.MC.EventBufferMC;
import nars.MC.OperationMC;
import nars.MC.SensorimotorChannelMC;
import nars.entity.Task;
import nars.io.StringParser;
import nars.storage.Memory;

import java.util.ArrayList;

public class ExpChannel4 extends SensorimotorChannelMC {

    private ArrayList<OperationMC> atomic_operations = null;

    public ExpChannel4(String channel_ID, EventBufferMC eventBuffer, Memory memory) {
        super(channel_ID, eventBuffer, memory);
    }

    @Override
    protected ArrayList<Task> gathering() {
        ArrayList<Task> tasks = new ArrayList<>();
        tasks.add(StringParser.parseTask("<(*,{Channel_1}, furnishing) --> ^see>.", this.memory, memory.getTime()));
//        tasks.add(StringParser.parseTask("A.", this.memory, memory.getTime()));
//        tasks.add(StringParser.parseTask("B.", this.memory, memory.getTime()));
//        tasks.add(StringParser.parseTask("C.", this.memory, memory.getTime()));
//        tasks.add(StringParser.parseTask("D.", this.memory, memory.getTime()));
//        tasks.add(StringParser.parseTask("E.", this.memory, memory.getTime()));
//        System.out.println("Time " + this.counter + " | Input from Channel #1: " + "<(*,{Channel_1}, furnishing) --> ^see>.");
        return tasks;
    }

    @Override
    public void execute(String name) {}
}
