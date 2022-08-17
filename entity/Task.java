/*
 * The MIT License
 *
 * Copyright 2019 The OpenNARS authors.
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
package nars.entity;

import java.util.ArrayList;

import nars.language.Term;

/**
 * A task to be processed, consists of a Sentence and a BudgetValue
 */
public class Task extends Item {

    /**
     * The sentence of the Task
     */
    private Sentence sentence;
    /**
     * Task from which the Task is derived, or null if input
     */
    private Task parentTask;
    /**
     * Belief from which the Task is derived, or null if derived from a theorem
     */
    private Sentence parentBelief;
    /**
     * For Question and Goal: best solution found so far
     */
    private Sentence bestSolution;

    private ArrayList<Sentence> previousBestSolution;

    private boolean processed = false;

    private boolean putBack = false;

    private boolean isInput = true;

    private boolean wasInBuffer;

    private boolean generatedAnticipation = false;

    private boolean temporalInduction = false;

    /**
     * Constructor for input task
     *
     * @param s The sentence
     * @param b The budget
     */
    public Task(Sentence s, BudgetValue b) {
        super(s.toKey(), b); // change to toKey()
        this.wasInBuffer = false;
        sentence = s;
        sentence.setObservable(observable());
        key = sentence.toKey();
        previousBestSolution = new ArrayList();
    }

    /**
     * Constructor for a derived task
     *
     * @param s            The sentence
     * @param b            The budget
     * @param parentTask   The task from which this new task is derived
     * @param parentBelief The belief from which this new task is derived
     */
    public Task(Sentence s, BudgetValue b, Task parentTask, Sentence parentBelief) {
        this(s, b);
        this.wasInBuffer = false;
        this.parentTask = parentTask;
        this.parentBelief = parentBelief;
        previousBestSolution = new ArrayList();
    }

    /**
     * Constructor for an activated task
     *
     * @param s            The sentence
     * @param b            The budget
     * @param parentTask   The task from which this new task is derived
     * @param parentBelief The belief from which this new task is derived
     * @param solution     The belief to be used in future inference
     */
    public Task(Sentence s, BudgetValue b, Task parentTask, Sentence parentBelief, Sentence solution) {
        this(s, b, parentTask, parentBelief);
        this.wasInBuffer = false;
        this.bestSolution = solution;
        previousBestSolution = new ArrayList();
    }

    public boolean isPutBack() {
        return putBack;
    }

    public void switchPutBack() {
        putBack = !putBack;
    }

    public boolean getGeneratedAnticipation() {
        return generatedAnticipation;
    }

    public void setGeneratedAnticipation(boolean generatedAnticipation) {
        this.generatedAnticipation = generatedAnticipation;
    }

    public boolean observable() {
        return isInput |= sentence.getObservable();
    }

    public boolean fulfilled() {
        if (bestSolution == null)
            return false;
        return sentence.getTruth().getExpectation() - bestSolution.getTruth().getExpectation() <= 0;
    }

    public boolean wasInBuffer() {
        return wasInBuffer;
    }

    public void setWasInBuffer(boolean wasInBuffer) {
        this.wasInBuffer = wasInBuffer;
    }

    /**
     * Get the sentence
     *
     * @return The sentence
     */
    public Sentence getSentence() {
        return sentence;
    }

    /**
     * Directly get the content of the sentence
     *
     * @return The content of the sentence
     */
    public Term getContent() {
        return sentence.getContent();
    }

    /**
     * Directly get the creation time of the sentence
     *
     * @return The creation time of the sentence
     */
    public long getCreationTime() {
        return sentence.getStamp().getCreationTime();
    }

    /**
     * Check if a Task is a direct input
     *
     * @return Whether the Task is derived from another task
     */
    public boolean isInput() {
        if (parentTask != null)
            isInput = false;
        return isInput || temporalInduction;
    }

    public void addBestSolution(Sentence solution) {
        previousBestSolution.add(solution);
    }

    public boolean getTemporalInduction() {
        return temporalInduction;
    }

    public void setTemporalInduction(boolean temporalInduction) {
        this.temporalInduction = temporalInduction;
    }

    public void setIsInput(boolean isInput) {
        this.isInput = isInput;
    }

    /**
     * Check if a Task is derived by a StructuralRule
     *
     * @return Whether the Task is derived by a StructuralRule
     */
//    public boolean isStructural() {
//        return (parentBelief == null) && (parentTask != null);
//    }

    /**
     * Merge one Task into another
     *
     * @param that The other Task
     */
    @Override
    public void merge(Item that) {
        if (getCreationTime() >= ((Task) that).getCreationTime()) {
            super.merge(that);
        } else {
            that.merge(this);
        }
    }

    public boolean getProcessed() {
        return processed;
    }

    public void setProcessed(boolean processed) {
        this.processed = processed;
    }

    /**
     * Get the best-so-far solution for a Question or Goal
     *
     * @return The stored Sentence or null
     */
    public Sentence getBestSolution() {
        return bestSolution;
    }

    /**
     * Set the best-so-far solution for a Question or Goal, and report answer
     * for input question
     *
     * @param judg The solution to be remembered
     */
    public void setBestSolution(Sentence judg) {
        bestSolution = judg;
    }

    /**
     * Get the parent belief of a task
     *
     * @return The belief from which the task is derived
     */
    public Sentence getParentBelief() {
        return parentBelief;
    }

    public Stamp getStamp() {
        return sentence.getStamp();
    }

    public boolean isEternal() {
        return getStamp().getOccurrenceTime() == Stamp.ETERNAL;
    }

    public String getName() {
        return sentence.getContent().getName();
    }

    /**
     * Get the parent task of a task
     *
     * @return The task from which the task is derived
     */
    public Task getParentTask() {
        return parentTask;
    }

    /**
     * Returns Achieving level of a task. Idea is if a contradicting belief exists
     * then spend more resources on it, thus the less the achieving level,
     * the bigger the contradiction and more resources needs to be spent.
     * Only called from processBuffer() from Memory.java when a task is pre-processed
     *
     * @param c
     * @return
     */
    public double getAchievingLevel(Concept c) {
        if (this.sentence.isJudgment() || this.sentence.isGoal()) {
            Sentence MatchBelief = c.getBeliefs().get(0);
            if (MatchBelief != null) {
                return 1 - Math.abs(this.sentence.getTruth().getExpectation() - MatchBelief.getTruth().getExpectation());
            }
        } else if (this.sentence.isQuestion() || this.sentence.isQuest()) {
            if (this.sentence != null) {
                return 1 - Math.abs(this.sentence.getTruth().getExpectation() - this.bestSolution.getTruth().getExpectation());
            }
        }
        return Math.abs(this.sentence.getTruth().getExpectation() - 0.5);
    }

    /**
     * Get a String representation of the Task
     *
     * @return The Task as a String
     */
    @Override
    public String toString() {
        StringBuilder s = new StringBuilder();
        s.append(super.toString()).append(" ");
        s.append(getSentence().getStamp());
        if (parentTask != null) {
            s.append("  \n from task: ").append(parentTask.toStringBrief());
            if (parentBelief != null) {
                s.append("  \n from belief: ").append(parentBelief.toStringBrief());
            }
        }
        if (bestSolution != null) {
            s.append("  \n solution: ").append(bestSolution.toStringBrief());
        }
        return s.toString();
    }

    public void setBudget(BudgetValue new_budget) {
        this.budget = new_budget;
    }
}