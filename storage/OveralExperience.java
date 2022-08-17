/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nars.storage;

import nars.main.Parameters;

/**
 * @author Xiang
 */
public class OveralExperience extends EventBuffer {

    public OveralExperience(Memory memory, int duration, String name) {
        super(memory, duration, name);
    }

    @Override
    public int capacity() {
        return Parameters.GLOBAL_BUFFER_SIZE;
    }

    @Override
    public int anticipationCapacity() {
        return Parameters.ANTICIPATION_LIST_CAPACITY;
    }

}
