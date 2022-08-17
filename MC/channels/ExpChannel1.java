package nars.MC.channels;

import nars.MC.EventBufferMC;
import nars.MC.OperationMC;
import nars.MC.SensoryMotorChannelMC;
import nars.entity.Task;
import nars.io.StringParser;
import nars.storage.Memory;

import java.util.ArrayList;

public class ExpChannel1 extends SensoryMotorChannelMC {

    private int counter = 0;

    public ExpChannel1(EventBufferMC event_buffer, ArrayList<OperationMC> atomic_operations, Memory memory) {
        super(event_buffer, atomic_operations, memory);
    }

    @Override
    public Task generate_Narsese_input() {
        if (counter < 400) {
            ArrayList<Task> tasks = new ArrayList<>();
            tasks.add(StringParser.parseTask("<(*,{Channel_1}, sting) --> ^see>. :|:", this.memory, memory.getTime()));
            System.out.println("Time " + this.counter + " | Input from Channel #1: " + "<(*,{Channel_1}, sting) --> ^see>. :|:");
            Task ret = this.event_buffer.step(tasks, false);
            counter += 1;
            return ret;
        } else {
            ArrayList<Task> tasks = new ArrayList<>();
            Task ret = this.event_buffer.step(tasks, false);
            return ret;
        }
    }
}
