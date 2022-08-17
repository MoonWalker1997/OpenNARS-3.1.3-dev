/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nars.storage;

import nars.entity.BudgetValue;
import nars.entity.Sentence;
import nars.entity.Stamp;
import nars.entity.Task;
import nars.entity.TruthValue;
import nars.inference.BudgetFunctions;
import nars.io.Symbols;
import nars.language.Conjunction;
import nars.language.Operation;
import nars.language.Tense;
import nars.language.Term;
import nars.main.Parameters;

/**
 * @author Xiang
 */
public class InternalExperience extends EventBuffer {

    public InternalExperience(Memory memory, int duration, String name) {
        super(memory, duration, name);
    }

    public int implicationListCapacity() {
        return Parameters.ANTICIPATION_LIST_CAPACITY;
    }

    @Override
    public int capacity() {
        return Parameters.INTERNAL_BUFFER_SIZE;
    }

    @Override
    public int anticipationCapacity() {
        return Parameters.ANTICIPATION_LIST_CAPACITY;
    }

    /**
     * If the task which is taken out is an operation, directly generate the operation
     *
     * @param temporalInduction
     * @param allowOverlap
     * @return
     */
    @Override
    public Task observe(boolean temporalInduction, boolean allowOverlap) {
        // the central idea is that: if it is a goal,

        Task task = super.observe(temporalInduction, allowOverlap);  // these two parameters are useless
        Task newTask = null;

        if (task == null)
            return null;

        // the task is an operation goal
        if (task.getSentence().isGoal() && task.getContent() instanceof Operation) {

            // if it is an operation goal, then it is assumed to be carried out and then be to be true
            TruthValue truth = new TruthValue(1.0f, Parameters.DEFAULT_JUDGMENT_CONFIDENCE);

            // default budget for this new task
            float priority = Parameters.DEFAULT_EVENT_PRIORITY;
            float durability = Parameters.DEFAULT_EVENT_DURABILITY;
            float quality = BudgetFunctions.truthToQuality(truth);
            BudgetValue budget = new BudgetValue(priority, durability, quality);

            Stamp stamp = new Stamp(Tense.Present, this.getMemory().getTime());
            stamp.setOccurrenceTime(this.getMemory().getTime());
            Sentence sentence = new Sentence(task.getContent(), Symbols.JUDGMENT_MARK, truth, stamp);
            // make it a new task, created and occurred at the current moment
            newTask = new Task(sentence, budget);

        } else if (task.getSentence().isGoal() && task.getContent() instanceof Conjunction) {
            // the task is a conjunction goal

            Term operation = ((Conjunction) task.getContent()).getComponents().get(1);
            // TODO, why get(1) is an operation?

            // if it is an operation, then process it like above
            TruthValue truth = new TruthValue(1.0f, Parameters.DEFAULT_JUDGMENT_CONFIDENCE);

            float priority = Parameters.DEFAULT_EVENT_PRIORITY;
            float durability = Parameters.DEFAULT_EVENT_DURABILITY;
            float quality = BudgetFunctions.truthToQuality(truth);

            BudgetValue budget = new BudgetValue(priority, durability, quality);
            Stamp stamp = new Stamp(Tense.Present, this.getMemory().getTime());
            Sentence sentence = new Sentence(operation, Symbols.JUDGMENT_MARK, truth, stamp);
            newTask = new Task(sentence, budget);

        }

        if (newTask != null)
            return newTask;
        else if (newTask == null && task != null)
            return task;
        else
            return null;

    }

}
