package nars.MC.channels;

import nars.MC.EventBufferMC;
import nars.MC.OperationMC;
import nars.MC.SensoryMotorChannelMC;
import nars.entity.Task;
import nars.io.StringParser;
import nars.storage.Memory;

import java.util.ArrayList;

public class ExpChannel4 extends SensoryMotorChannelMC {

    public ExpChannel4(EventBufferMC event_buffer, ArrayList<OperationMC> atomic_operations, Memory memory) {
        super(event_buffer, atomic_operations, memory);
    }

    @Override
    public Task generate_Narsese_input() {
        ArrayList<Task> tasks = new ArrayList<>();
        tasks.add(StringParser.parseTask("<(*,{Channel_1}, furnishing) --> ^see>.", this.memory, memory.getTime()));
//        tasks.add(StringParser.parseTask("A.", this.memory, memory.getTime()));
//        tasks.add(StringParser.parseTask("B.", this.memory, memory.getTime()));
//        tasks.add(StringParser.parseTask("C.", this.memory, memory.getTime()));
//        tasks.add(StringParser.parseTask("D.", this.memory, memory.getTime()));
//        tasks.add(StringParser.parseTask("E.", this.memory, memory.getTime()));
//        System.out.println("Time " + this.counter + " | Input from Channel #1: " + "<(*,{Channel_1}, furnishing) --> ^see>.");
        Task ret = this.event_buffer.step(tasks, false);
        return ret;
    }
}
