package nars.main;

import nars.MC.*;
import nars.MC.channels.ExpChannel4;
import nars.MC.channels.ExpChannel5;
import nars.MC.channels.ExpChannel6;
import nars.entity.Stamp;
import nars.entity.Task;
import nars.io.OutputChannel;
import nars.io.StringParser;
import nars.io.Symbols;
import nars.storage.Memory;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class NAR {

    /**
     * global DEBUG print switch
     */
    public static final boolean DEBUG = false;
    /**
     * The name of the reasoner
     */
    protected String name;
    /**
     * The memory of the reasoner
     */
    protected Memory memory;
    /**
     * The input channels of the reasoner
     * We many have other types of channels.
     */
    protected ArrayList<SensorimotorChannelMC> inputChannels;
    /**
     * The output channels of the reasoner
     */
    protected ArrayList<OutputChannel> outputChannels;
    /**
     * System clock, relatively defined to guarantee the repeatability of behaviors
     */
    private long clock;
    /**
     * Flag for running continuously
     */
    private boolean running;
    /**
     * The remaining number of steps to be carried out (walk mode)
     */
    private int walkingSteps;
    /**
     * Determines the end of {@link NARSBatch} program (set but not accessed in this class)
     */
    private boolean finishedInputs;
    /**
     * System clock - number of cycles since last output
     */
    private long timer;
    /**
     * Budget Threshold - show output if its budget average above threshold
     */
    private final AtomicInteger silenceValue = new AtomicInteger(Parameters.SILENT_LEVEL);
    /**
     * Internal Experience Buffer for derivations
     */
    private final InternalBufferMC internalBuffer;
    /**
     * Overall Experience Buffer for input and tasks from internalBuffer
     */
    private final OverallBufferMC overallBuffer;
    //private final Experience_From_Narsese narsese_Channel;

    private final int internal_Duration = Parameters.MAX_BUFFER_DURATION_FACTOR * Parameters.DURATION_FOR_INTERNAL_BUFFER;
    private final int global_Duration = Parameters.MAX_BUFFER_DURATION_FACTOR * Parameters.DURATION_FOR_GLOBAL_BUFFER;

    public NAR() {
        memory = new Memory(this); // memory initialization
        inputChannels = new ArrayList(); // a container of sensorimotor channels

        // each sensorimotor channel contains one event buffer
        EventBufferMC event_buffer1 = new EventBufferMC(5, 5, 5, 5, memory, false);
        EventBufferMC event_buffer2 = new EventBufferMC(5, 5, 5, 5, memory, false);
//        EventBufferMC event_buffer3 = new EventBufferMC(5, 5, 5, 5, memory);

        SensorimotorChannelMC channel1 = new ExpChannel4("channel1", event_buffer1, memory);
//        SensorimotorChannelMC channel2 = new ExpChannel5("channel2", event_buffer2, memory);

        SensorimotorChannelMC channel2 = new ExpChannel6("channel2", event_buffer2, memory);

//        SensoryMotorChannelMC channel3 = new ExpChannel3(event_buffer3, null, memory);

        // channel registration
//        inputChannels.add(channel1);
        inputChannels.add(channel2);
//        inputChannels.add(channel3);

        outputChannels = new ArrayList(); // TODO: a container of output channels

        // an internal buffer
        internalBuffer = new InternalBufferMC(5, 5, 5, 5, memory, false);
        OperationOutputBuffer OOB = new OperationOutputBuffer();
        OOB.initialize_operation_checklist(inputChannels);
        internalBuffer.setOOB(OOB);

        // an overall buffer
        overallBuffer = new OverallBufferMC(5, 5, 5, 5, memory, false);
    }

    public void addInputChannel(SensorimotorChannelMC channel) {
        inputChannels.add(channel);
    }

    public void removeInputChannel(SensorimotorChannelMC channel) {
        inputChannels.remove(channel);
    }

    public void addOutputChannel(OutputChannel channel) {
        outputChannels.add(channel);
    }

    public void removeOutputChannel(OutputChannel channel) {
        outputChannels.remove(channel);
    }

    /**
     * Reset the system with an empty memory and reset clock.
     */
    public void reset() {
        //CompositionalRules.rand = new Random(1);
        running = false;
        walkingSteps = 0;
        clock = 0;
        memory.init();
        Stamp.init();
        //timer = 0;
    }

    /**
     * Start the inference process
     */
    public void run() {
        running = true;
    }

    /**
     * Will carry the inference process for a certain number of steps
     *
     * @param n The number of inference steps to be carried
     */
    public void walk(int n) {
        walkingSteps = n;
    }

    /**
     * Will stop the inference process
     */
    public void stop() {
        running = false;
    }

    /**
     * A system cycle. Run one working workCycle or read input.
     * Called from Shell.java and NARS.java
     * only.
     */
    public void cycle() {
//        if (DEBUG) {
//            if (running || walkingSteps > 0 || !finishedInputs) {
//                System.out.println("// In Cycle: "
//                        + "walkingSteps " + walkingSteps
//                        + ", clock " + clock
//                        + ", getTimer " + getTimer()
//                        + "\n//    memory.getExportStrings() " + memory.getExportStrings());
//                System.out.flush();
//            }
//        }
//        // find whether this should run, by default it should not run
//        // but if some channels have input, it should run
//        if (walkingSteps == 0) {
//            boolean reasonerShouldRun = false;
//            for (InputChannel channelIn : inputChannels) {
//                reasonerShouldRun = reasonerShouldRun || channelIn.nextInput();
//                // actually at here, nextInput() has already put text inputs into the memory
//            }
//            finishedInputs = !reasonerShouldRun;
//        }
//        // forward to output Channels
//        ArrayList<String> output = memory.getExportStrings();
//        if (!output.isEmpty()) {
//            for (OutputChannel channelOut : outputChannels) {
//                channelOut.nextOutput(output);
//            }
//            output.clear();    // this will trigger display the current value of timer in Memory.report()
//        }
//        // if it should run, go cycle()
        if (running || walkingSteps > 0) {
            clock++;
            tickTimer();

            // get all previous inference results; they are the input of the internal buffer
            ArrayList<Task> previous_inference_results = this.memory.getPrevious_inference_result();
            this.memory.setPrevious_inference_result(new ArrayList<>());

            // following the buffer cycle of the internal buffer, the task forwarded to the overall buffer
            Task task_from_internal_buffer = this.internalBuffer.step(previous_inference_results, false);

            // a container of tasks inputted from sensorimotor channels
            ArrayList<Task> tasks_from_channels = new ArrayList<>();
            for (int i = 0; i < this.inputChannels.size(); i++) {
                Task tmp = this.inputChannels.get(i).generate_Narsese_input();
                if (tmp != null) {
                    tasks_from_channels.add(tmp);
                }
            }

            // merge
            if (task_from_internal_buffer != null) {
                tasks_from_channels.add(task_from_internal_buffer);
            }

            ArrayList<Task> tasks_for_overall_buffer = tasks_from_channels; // just give it a new name
            Task task_from_overall_buffer = this.overallBuffer.step(tasks_for_overall_buffer, false);

            // task selected by the overall buffer will be sent to the memory
            if (task_from_overall_buffer != null) {
                this.memory.immediateProcess(task_from_overall_buffer);
            }

            // a quick reference
//            public void immediateProcess(Task task) {
//                currentTask = task; // one of the two places where this variable is set
//                recorder.append("!!! Insert: " + task + "\n");
//                //System.out.println("!!! Insert: " + task + "\n");
//                currentTerm = task.getContent();
//                currentConcept = getConcept(currentTerm);// Creates a new concept
//                if (currentConcept != null) {
//                    activateConcept(currentConcept, task.getBudget()); // initializes this concept
//                    currentConcept.directProcess(task);
//                }
//            }

            memory.workCycle(clock);

            if (walkingSteps > 0) {
                walkingSteps--;
            }
        }
    }

//    public void special_cycle() {
//        if (running || walkingSteps > 0) {
//            clock++;
//            tickTimer();
//
//            ArrayList<Task> tasks_from_channels = new ArrayList<>();
//            for (int i = 0; i < this.inputChannels.size(); i++) {
//                tasks_from_channels.add(this.inputChannels.get(i).generate_special_Narsese_input());
//            }
//
//            ArrayList<Task> previous_inference_results = this.memory.getPrevious_inference_result();
//
//            this.memory.setPrevious_inference_result(new ArrayList<>());
//
//            Task task_from_internal_buffer = this.internalBuffer.step(previous_inference_results);
//
//            if (task_from_internal_buffer != null) {
//                tasks_from_channels.add(task_from_internal_buffer);
//            }
//
//            ArrayList<Task> tasks_for_overall_buffer = tasks_from_channels;
//
//            Task task_from_overall_buffer = this.globalBuffer.step(tasks_for_overall_buffer);
//
//            if (task_from_overall_buffer != null) {
//                this.memory.immediateProcess(task_from_overall_buffer);
//            }
//
//            memory.workCycle(clock);
//
//            if (walkingSteps > 0) {
//                walkingSteps--;
//            }
//        }
//    }

    /**
     * To process a line of input text
     *
     * @param text
     */
    public void textInputLine(String text) {
        if (text.isEmpty()) {
            return;
        }
        char c = text.charAt(0);
        if (c == Symbols.RESET_MARK) {
            reset();
            memory.getExportStrings().add(text);
        } else if (c != Symbols.COMMENT_MARK) {
            // read NARS language or an integer : TODO duplicated code
            try {
                int i = Integer.parseInt(text);
                walk(i);
            } catch (NumberFormatException e) {
                Task task = StringParser.parseExperience(new StringBuffer(text), memory, clock);
                if (task != null) {
                    inputNarseseTask(task);
                }
            }
        }
    }

    /**
     * Adds task to main memory
     *
     * @param task
     */
    private void inputNarseseTask(Task task) {
        if (task.getBudget().aboveThreshold()) {
            memory.getRecorder().append("!!! Perceived: " + task + "\n");
            memory.report(task.getSentence(), true, false);
            task.getBudget().incPriority((float) 0.1);
//            globalBuffer.preProcessing(task, true);
            //globalBuffer.putInSequenceList(task, memory.getTime());
        } else {
            memory.getRecorder().append("!!! Neglected: " + task + "\n");
        }
    }

    public Memory getMemory() {
        return memory;
    }

    /**
     * Get the current time from the clock Called in {@link nars.entity.Stamp}
     *
     * @return The current time
     */
    public long getTime() {
        return clock;
    }

    public int getWalkingSteps() {
        return walkingSteps;
    }

    public InternalBufferMC getInternalBuffer() {
        return internalBuffer;
    }

    public OverallBufferMC getOverallBuffer() {
        return overallBuffer;
    }

    /**
     * Report Silence Level
     */
    public AtomicInteger getSilenceValue() {
        return silenceValue;
    }

    /**
     * determines the end of {@link NARSBatch} program
     *
     * @return
     */
    public boolean isFinishedInputs() {
        return finishedInputs;
    }

    /**
     * To get the timer value and then to
     * reset it by {@link #initTimer()};
     *
     * @return The previous timer value
     */
    public long updateTimer() {
        long i = getTimer();
        initTimer();
        return i;
    }

    /**
     * Reset timer
     */
    public void initTimer() {
        setTimer(0);
    }

    /**
     * Update timer
     */
    public void tickTimer() {
        setTimer(getTimer() + 1);
    }

    /**
     * @return System clock : number of cycles since last output
     */
    public long getTimer() {
        return timer;
    }

    /**
     * set System clock : number of cycles since last output
     */
    private void setTimer(long timer) {
        this.timer = timer;
    }

    @Override
    public String toString() {
        return memory.toString();
    }
}