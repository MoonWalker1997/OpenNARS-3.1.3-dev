package nars.MC;

import nars.entity.Task;

import java.util.ArrayList;
import java.util.HashMap;

public class OperationOutputBuffer extends OutputBuffer {

    // a hash table for operations and channels, which is used to navigate these operations
    // here ths "operation" is represented as a string, but it should be a more specific class in the future
    private HashMap<String, SensorimotorChannelMC> operation_checklist = new HashMap<>();

    /**
     * Registering the atomic operations of the sensorimotor channels connected.
     *
     * @param inputChannels: an array of sensorimotor channels
     */
    public void initialize_operation_checklist(ArrayList<SensorimotorChannelMC> inputChannels) {
//        for (int i = 0; i < inputChannels.size(); i++) {
//            for (int j = 0; j < inputChannels.get(i).getAtomic_operations().size(); j++) {
//                operation_checklist.put(inputChannels.get(i).getAtomic_operations().get(j), inputChannels.get(i));
//            }
//        }
        operation_checklist.put("^move", inputChannels.get(0));
        operation_checklist.put("^turn", inputChannels.get(0));
    }

    /**
     * Currently this function does nothing, but as designed, it should be able to decompose "operators" into atomic ones.
     * The "operators" here is ambiguous, since it has different implementations with the same name. And in functionality,
     * they are largely different. TODO
     */
    private void decompose() {
    }

    /**
     * Currently, the only resource of operations is from the internal buffer. The one that is selected to be forwarded
     * to the global buffer.
     *
     * @param t: a task, but it should be an operator. Specifically, it should be an atomic operator now. TODO
     */
    public void distribute_and_execute(Task t) {
//        ArrayList<Task> decomposed_t = decompose(t);
        for (int i = 0; i < this.operation_checklist.size(); i++) {
            if (this.operation_checklist.containsKey(t.getName())) {
                this.operation_checklist.get(t.getName()).execute(t.getName());
            }
        }
    }

}
