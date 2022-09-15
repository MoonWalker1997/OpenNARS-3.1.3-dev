package nars.MC;

import nars.entity.Task;
import nars.language.Operation;
import nars.storage.Memory;

import java.util.ArrayList;

/**
 * This class is a base class for all
 */
public abstract class ChannelMC {

    protected String channel_ID;

    // input buffer
    protected EventBufferMC eventBuffer;

    private ArrayList<OperationMC> atomic_operations;

    public ArrayList<OperationMC> getAtomic_operations() {
        return this.atomic_operations;
    }

    // the generated Narsese needs a timestamp from the main memory, this is the reference
    protected Memory memory;

    // TODO
//     protected OutputBufferMC output_buffer;

    // to make it clear, this function just needs to return a bunch of Narsese sentences (with truth-values and budgets)
    // it will be automatically called by the reporting function
    protected abstract ArrayList<Task> gathering();

    public Task generate_Narsese_input() {
        return this.eventBuffer.step(this.gathering(), true);
    }

    // TODO
//    public abstract void distribute_Operations();

    public ChannelMC (String channel_ID, EventBufferMC eventBuffer, Memory memory) {
        this.channel_ID = channel_ID;
        this.eventBuffer = eventBuffer;
        this.memory = memory;
    }

}
