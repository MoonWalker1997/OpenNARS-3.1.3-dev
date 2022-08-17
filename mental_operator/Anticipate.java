/*
 * The MIT License
 *
 * Copyright 2018 The OpenNARS authors.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package nars.mental_operator;


import nars.entity.BudgetValue;
import nars.entity.Task;
import nars.entity.TruthValue;
import nars.language.Operation;
import nars.language.Operator;
import nars.language.Term;
import nars.storage.Memory;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Operator that creates a judgment with a given statement
 */
public class Anticipate extends Operator {

    //public final Map<Prediction,LinkedHashSet<Term>> anticipations = new LinkedHashMap();

    private transient Set<Term> newTasks = new LinkedHashSet();

    private TruthValue expiredTruth = null;
    private BudgetValue expiredBudget = null;

    //internal experience has less durability?
    public float ANTICIPATION_DURABILITY_MUL = 0.1f; //0.1
    //internal experience has less priority?
    public float ANTICIPATION_PRIORITY_MUL = 0.1f; //0.1


    public Anticipate() {
        super("^anticipate");
    }

    public Anticipate(float ANTICIPATION_DURABILITY_MUL, float ANTICIPATION_PRIORITY_MUL) {
        this();
        this.ANTICIPATION_DURABILITY_MUL = ANTICIPATION_DURABILITY_MUL;
        this.ANTICIPATION_PRIORITY_MUL = ANTICIPATION_PRIORITY_MUL;
    }

    public void anticipationFeedback(final Term content, final Task t, final Memory memory, final long time) {
        /*if(anticipationOperator) {
            final Operation op=(Operation) Operation.make(Product.make(Term.SELF,content), this);
            final TruthValue truth=new TruthValue(1.0f,memory.narParameters.DEFAULT_JUDGMENT_CONFIDENCE, memory.narParameters);
            final Stamp st;
            if(t==null) {
                st=new Stamp(time, memory);
            } else {
                st=t.sentence.stamp.clone();
                st.setOccurrenceTime(time.time());
            }

            final Sentence s=new Sentence(
                    op,
                    Symbols.JUDGMENT_MARK,
                    truth,
                    st);

            final BudgetValue budgetForNewTask = new BudgetValue(
                memory.narParameters.DEFAULT_JUDGMENT_PRIORITY*ANTICIPATION_PRIORITY_MUL,
                memory.narParameters.DEFAULT_JUDGMENT_DURABILITY*ANTICIPATION_DURABILITY_MUL,
                BudgetFunctions.truthToQuality(truth), memory.narParameters);
            final Task newTask = new Task(s, budgetForNewTask, Task.EnumType.INPUT);

            memory.addNewTask(newTask, "Internal");
        }*/
    }

    @Override
    protected ArrayList<Task> execute(Operation operation, ArrayList<Term> args, Memory memory, long time) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
