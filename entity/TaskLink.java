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

import nars.language.Term;
import nars.main.Parameters;

/**
 * Reference to a Task.
 * <p>
 * The reason to separate a Task and a TaskLink is that the same Task can be
 * linked from multiple Concepts, with different BudgetValue.
 */
public class TaskLink extends TermLink {

    /**
     * The Task linked. The "target" field in TermLink is not used here.
     */
    private Task targetTask;
    /**
     * Remember the TermLinks that has been used recently with this TaskLink
     */
    private String recordedLinks[];
    /**
     * Remember the time when each TermLink is used with this TaskLink
     */
    private long recordingTime[];
    /**
     * The number of TermLinks remembered
     */
    int counter;

    /**
     * Constructor
     * <p>
     * only called in Memory.continuedProcess
     *
     * @param t The target Task
     * @param template The TermLink template
     * @param v The budget
     */
    public TaskLink(Task t, TermLink template, BudgetValue v) {
        super("", v);
        targetTask = t;
        if (template == null) {
            type = TermLink.SELF;
            index = null;
        } else {
            type = template.getType();
            index = template.getIndices();
        }
        recordedLinks = new String[Parameters.TERM_LINK_RECORD_LENGTH];
        recordingTime = new long[Parameters.TERM_LINK_RECORD_LENGTH];
        counter = 0;
        setKey();   // as defined in TermLink
        key += t.getKey();
    }

    /**
     * Get the target Task
     *
     * @return The linked Task
     */
    public Task getTargetTask() {
        return targetTask;
    }

    /**
     * To check whether a TaskLink should use a TermLink, return false if they
     * interacted recently
     * <p>
     * called in TermLinkBag only
     *
     * @param termLink The TermLink to be checked
     * @param currentTime The current time
     * @return Whether they are novel to each other
     */
    /*public boolean novel(TermLink termLink, long currentTime) {
        Term bTerm = termLink.getTarget();
        if (bTerm.equals(targetTask.getSentence().getContent())) {
            return false;
        }
        String linkKey = termLink.getKey();
        int next, i;
        for (i = 0; i < counter; i++) {
            next = i % Parameters.TERM_LINK_RECORD_LENGTH;
            if (linkKey.equals(recordedLinks[next])) {
                if (currentTime < recordingTime[next] + Parameters.TERM_LINK_RECORD_LENGTH) {
                    return false;
                } else {
                    recordingTime[next] = currentTime;
                    return true;
                }
            }
        }
        
        next = i % Parameters.TERM_LINK_RECORD_LENGTH;
        recordedLinks[next] = linkKey;       // add knowledge reference to recordedLinks
        recordingTime[next] = currentTime;
        if (counter < Parameters.TERM_LINK_RECORD_LENGTH) { // keep a constant length
            counter++;
        }
        return true;
    }*/
    
    public boolean novel(TermLink termLink, long currentTime) {
        Term bTerm = termLink.getTarget();
        if (bTerm.equals(targetTask.getSentence().getContent())) {
            return false;
        }
        String linkKey = termLink.getKey();
        int next, i;
        for (i = 0; i < counter; i++) {
            next = i % Parameters.TERM_LINK_RECORD_LENGTH;
            if (linkKey.equals(recordedLinks[next])) {
                if (currentTime < recordingTime[next] + Parameters.TERM_LINK_RECORD_LENGTH) {
                    return false;
                } else {
                    recordingTime[next] = currentTime;
                    return true;
                }
            }
        }
        
        if (counter < Parameters.TERM_LINK_RECORD_LENGTH) { // keep a constant length
            next = i % Parameters.TERM_LINK_RECORD_LENGTH;
            recordedLinks[next] = linkKey;       // add knowledge reference to recordedLinks
            recordingTime[next] = currentTime;
            counter++;
        }else{        
            int inde = forgetIndex(recordingTime);
            recordedLinks[inde] = linkKey;
            recordingTime[inde] = currentTime;
        }
        return true;
    }
    
    public int forgetIndex(long[] times){
        
        int inde = 0;
        long earliest = times[0];
        
        for (int i = 1; i < times.length; i++) {
            
            if(times[i] < earliest){
                earliest = times[i];
                inde = i;
            }
            
        }
        
        return inde;
    }

    @Override
    public String toString() {
        return super.toString() + " " + getTargetTask().getSentence().getStamp();
    }
}
