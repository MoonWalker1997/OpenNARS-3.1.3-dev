/* 
 * The MIT License
 *
 * Copyright 2021 The OpenNARS authors.
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

import nars.language.Term;

/**
 *
 * @author Xiang
 */
public class Anticipation implements Comparable<Anticipation>{
    
    private final Task task;
    private final Term relation;
    private final long expectationOccurrenceTime;
    private final long preconditionOccurrenceTime;
    
    public Anticipation(Task task, Term relation, long expectationOccurrenceTime, long preconditionOccurrenceTime){
        
        this.task = task;
        this.expectationOccurrenceTime = expectationOccurrenceTime;
        this.preconditionOccurrenceTime = preconditionOccurrenceTime;
        this.relation = relation;
        
    }
    
    public Task getTask(){
        return task;
    }
    
    public Term getRelation(){
        return relation;
    }
    
    public long getExpectationOccurrenceTime(){
        return expectationOccurrenceTime;
    }
    
    public long getPreconditionOccurrenceTime(){
        return preconditionOccurrenceTime;
    }

    @Override
    public int compareTo(Anticipation o) {
       
        if(task.getName().equals(o.getTask().getName()) && relation.equals(o.getRelation()) &&
                expectationOccurrenceTime == o.getExpectationOccurrenceTime() &&
                preconditionOccurrenceTime == o.getPreconditionOccurrenceTime())
            return 0;
        else if(task.getPriority() > o.getTask().getPriority())
            return 1;
        else
            return -1;
        
    }
}
