package nars.MC;

import nars.entity.Task;
import nars.io.StringParser;
import nars.storage.Memory;

import java.util.ArrayList;

/**
 * sensorimotor channels may have a different design from the extended class, but currently, they use the same design
 */
public abstract class SensorimotorChannelMC extends ChannelMC {

    public SensorimotorChannelMC(String channel_ID, EventBufferMC eventBuffer, Memory memory) {
        super(channel_ID, eventBuffer, memory);
    }

    public abstract void execute(String name);

//    public abstract void execute();
}
