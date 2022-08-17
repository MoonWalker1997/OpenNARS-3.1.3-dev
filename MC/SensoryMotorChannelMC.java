package nars.MC;

import nars.entity.Task;
import nars.io.StringParser;
import nars.storage.Memory;

import java.util.ArrayList;

public class SensoryMotorChannelMC {

    int counter;

    protected EventBufferMC event_buffer;

    private ArrayList<OperationMC> atomic_operations;

    private ArrayList<AgendaMC> agenda;

    protected Memory memory;

    public SensoryMotorChannelMC(EventBufferMC event_buffer, ArrayList<OperationMC> atomic_operations, Memory memory) {
        this.event_buffer = event_buffer;
        this.atomic_operations = atomic_operations;
        this.memory = memory;
    }

    public Task generate_Narsese_input() {
        ArrayList<Task> tasks = new ArrayList<>();
        tasks.add(StringParser.parseTask("<A --> B>.", this.memory, memory.getTime()));
        Task ret = this.event_buffer.step(tasks, false);
        return ret;
    }

    public void setCounter(int counter) {
        this.counter = counter;
    }

//    public Task generate_special_Narsese_input() {
//        ArrayList<Task> tasks = new ArrayList<>();
//        tasks.add(StringParser.parseTask("<A --> B>.", this.memory, memory.getTime()));
//        Task ret = this.event_buffer.step(tasks);
//        return ret;
//    }

    public void load_operation(Task operation) {
        // TODO
    }

    public void exe() {
        // TODO
    }

    public void test_mode() {
        // TODO
    }

    public EventBufferMC getEvent_buffer() {
        return event_buffer;
    }
}
