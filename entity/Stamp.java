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

import java.util.*;
import nars.inference.TemporalRules;
import static nars.inference.TemporalRules.ORDER_BACKWARD;
import static nars.inference.TemporalRules.ORDER_CONCURRENT;
import static nars.inference.TemporalRules.ORDER_FORWARD;

import nars.io.Symbols;
import nars.language.Tense;
import static nars.language.Tense.*;
import nars.main.Debug;
import nars.main.Parameters;
import nars.main.NAR;

/**
 * Each Sentence has a time stamp, consisting the following components:
 * (1) The creation time of the sentence, 
 * (2) A evidentialBase of serial numbers of sentence, from which the sentence is derived.
 * Each input sentence gets a unique serial number, though the creation time may be not unique.
 * The derived sentences inherits serial numbers from its parents, cut at the baseLength limit.
 */
public class Stamp implements Cloneable {

    /** *  serial number, for the whole system 
     * TODO : should it really be static?
     * or a Stamp be a field in {@link NAR} ? */
    private static long currentSerial = 0;
    /** serial numbers */
    private long[] evidentialBase;
    /** evidentialBase baseLength */
    private int baseLength;
    /** creation time of the stamp */
    private long creationTime;
    
    private long putInTime;

    private Tense tense;
    
    private long occurrenceTime;
    
    // default for a temporal events means "always" in Judgment/Question, but 
    // "current" in Goal/Quest
    public static final long ETERNAL = Integer.MIN_VALUE;
    
    public boolean before(final Stamp s, final int duration){
        if(isEternal() || s.isEternal())
            return false;
        
        return order(s.occurrenceTime, occurrenceTime, duration) == TemporalRules.ORDER_BACKWARD;
    }
    
    public boolean after(final Stamp s, final int duration){
        if(isEternal() || s.isEternal())
            return false;
        return order(s.occurrenceTime, occurrenceTime, duration) == TemporalRules.ORDER_FORWARD;
    }
    
    public boolean isEternal(){
        final boolean eternalOccurrence = occurrenceTime == ETERNAL;
        
        if(Debug.DETAILED){
            if (eternalOccurrence && tense != Tense.Eternal)
                throw new IllegalStateException("Stamp has inconsistent tense and eternal ocurrenceTime: tense=" + tense);
        }
        
        return eternalOccurrence;
    }
    
    public static boolean baseOverlap(Stamp a, Stamp b){
        
        long[] base1 = a.evidentialBase;
        long[] base2 = b.evidentialBase;
        
        Set task_base = new LinkedHashSet(base1.length + base2.length);
        
        for(long base : base1){
            if(task_base.contains(base))
                return true;
            task_base.add(base);
        }
        
        for(long base : base2){
            if(task_base.contains(base))
                return true;
            task_base.add(base);
        }
        return false;
    }
    
    public long getOccurrenceTime(){
        return occurrenceTime;
    }
    
    public static int order(final long timeDiff, final int durationCycles){
        final int halfDuration = durationCycles/2;
        if(timeDiff > halfDuration)
            return ORDER_FORWARD;
        else if(timeDiff < -halfDuration)
            return ORDER_BACKWARD;
        else
            return ORDER_CONCURRENT;
    }
    
    public static int order(final long a, final long b, final int durationCycles){
        if((a == Stamp.ETERNAL) || (b == Stamp.ETERNAL))
            throw new IllegalStateException("order() does not compare ETERNAL times");
        
        return order(b - a, durationCycles);
    }
    
    /**
     * Generate a new stamp, with a new serial number, for a new Task
     * @param time Creation time of the stamp
     */
    public Stamp(long time) {
        currentSerial++;
        baseLength = 1;
        evidentialBase = new long[baseLength];
        evidentialBase[0] = currentSerial;
        creationTime = time;
    }

    /**
     * Generate a new stamp identical with a given one
     * @param old The stamp to be cloned
     */
    public Stamp(Stamp old) {
        baseLength = old.length();
        evidentialBase = old.getBase();
        creationTime = old.getCreationTime();
        occurrenceTime = old.getOccurrenceTime();
    }

    /**
     * Generate a new stamp from an existing one, with the same evidentialBase but different creation time
     * <p>
     * For single-premise rules
     * @param old The stamp of the single premise
     * @param time The current time
     */
    public Stamp(Stamp old, long time) {
        baseLength = old.length();
        evidentialBase = old.getBase();
        creationTime = time;
        occurrenceTime = old.getOccurrenceTime();
    }
    
    public Stamp(Tense tense, long time){
        currentSerial++;
        baseLength = 1;
        evidentialBase = new long[baseLength];
        evidentialBase[0] = currentSerial;
        this.tense = tense;
        //this.creationTime = -1;
        setCreationTime(time, Parameters.DURATION);
    }

    /**
     * Generate a new stamp for derived sentence by merging the two from parents
     * the first one is no shorter than the second
     * @param first The first Stamp
     * @param second The second Stamp
     */
    public Stamp(Stamp first, Stamp second, long time) {
        int i1, i2, j;
        i1 = i2 = j = 0;
        baseLength = Math.min(first.length() + second.length(), Parameters.MAXIMUM_STAMP_LENGTH);
        evidentialBase = new long[baseLength];
        
        if(first.baseLength > second.baseLength){
            
            while(i2 < second.baseLength && j < baseLength){
                
                evidentialBase[j] = first.get(i1);
                i1++;
                j++;
                evidentialBase[j] = second.get(i2);
                i2++;
                j++;
                
            }
            
            while(i1 < first.baseLength && j < baseLength){
                evidentialBase[j] = first.get(i1);
                i1++;
                j++;
            }
            
        }else{
        
            while(i1 < first.baseLength && j < baseLength){
                
                evidentialBase[j] = first.get(i1);
                i1++;
                j++;
                evidentialBase[j] = second.get(i2);
                i2++;
                j++;
                
            }
            
            while(i2 < second.baseLength && j < baseLength){
                evidentialBase[j] = second.get(i2);
                i2++;
                j++;
            }
        }
        
        creationTime = time;
        
        if(first.isEternal() || second.isEternal())
            occurrenceTime = ETERNAL;
        else
            occurrenceTime = first.getOccurrenceTime();
    }
    
    public Stamp(long time, Tense tense, long serial, int duration){
        
        this(tense, serial);
        setCreationTime(time, duration);
    }
    
    public void setCreationTime(long time){
        creationTime = time;
    }
    
    public void setCreationTime(long time, int duration){
        
        creationTime = time;
        
        if(tense == null)
            occurrenceTime = ETERNAL;
        else switch (tense) {
            case Past:
                occurrenceTime = time - duration;
                break;
            case Future:
                occurrenceTime = time + duration;
                break;
            default:
                occurrenceTime = time;
                break;
        }
        
    }
    
    /**
     * Get the creationTime of the truth-value
     * @return The creation time
     */
    public long getCreationTime() {
        return creationTime;
    }
    
    public void setEternal(){
        
        occurrenceTime = ETERNAL;
        
    }
    
    public String getTense(long currentTime, int duration){
        
        if(isEternal())
            return "";
        
        switch(TemporalRules.order(currentTime, occurrenceTime, duration)){
            
            case ORDER_FORWARD:
                return Symbols.TENSE_FUTURE;
            case ORDER_BACKWARD:
                return Symbols.TENSE_PAST;
            default:
                return Symbols.TENSE_PRESENT;
            
        }
        
    }
    
    public void setOccurrenceTime(long time){
        
        if(occurrenceTime != time){   
            occurrenceTime = time;
            if(time == ETERNAL)
                tense = Tense.Eternal;
        } 
    }

    /**
     * Try to merge two Stamps, return null if have overlap
     * <p>
     * By default, the event time of the first stamp is used in the result
     * @param first The first Stamp
     * @param second The second Stamp
     * @param time The new creation time
     * @return The merged Stamp, or null
     */
    public static Stamp make(Stamp first, Stamp second, long time) {
        for (int i = 0; i < first.length(); i++) {
            for (int j = 0; j < second.length(); j++) {
                if (first.get(i) == second.get(j)) {
                    return null;
                }
            }
        }
        if (first.length() > second.length()) {
            return new Stamp(first, second, time);
        } else {
            return new Stamp(second, first, time);
        }
    }

    /**
     * Clone a stamp
     * @return The cloned stamp
     */
    @Override
    public Stamp clone() {
        return new Stamp(this);
    }

    /**
     * Initialize the stamp mechanism of the system, called in Reasoner
     */
    public static void init() {
        currentSerial = 0;
    }

    /**
     * Return the baseLength of the evidentialBase
     * @return Length of the Stamp
     */
    public int length() {
        return baseLength;
    }

    /**
     * Get a number from the evidentialBase by index, called in this class only
     * @param i The index
     * @return The number at the index
     */
    long get(int i) {
        return evidentialBase[i];
    }

    /**
     * Get the evidentialBase, called in this class only
     * @return The evidentialBase of numbers
     */
    private long[] getBase() {
        return evidentialBase;
    }
    
    /**
     * Get Directness, the shorter the basis the more direct is the derivation
     * Power value of -0.5 is subject to further research
     * @return double value
     */
    private double getDirectness() {
        return Math.pow(this.evidentialBase.length, -0.5);
    }

    /**
     * Convert the evidentialBase into a set
     * @return The TreeSet representation of the evidential base
     */
    public TreeSet<Long> toSet() {
        TreeSet<Long> set = new TreeSet<>();
        for (int i = 0; i < baseLength; i++) {
            set.add(evidentialBase[i]);
        }
        return set;
    }

    /**
     * Check if two stamps contains the same content
     * @param that The Stamp to be compared
     * @return Whether the two have contain the same elements
     */
    @Override
    public boolean equals(Object that) {
        if (!(that instanceof Stamp)) {
            return false;
        }
        TreeSet<Long> set1 = toSet();
        TreeSet<Long> set2 = ((Stamp) that).toSet();
        return (set1.containsAll(set2) && set2.containsAll(set1));
    }

    /**
     * The hash code of Stamp
     * @return The hash code
     */
    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    /**
     * Get a String form of the Stamp for display
     * Format: {creationTime [: eventTime] : evidentialBase}
     * @return The Stamp as a String
     */
    @Override
    public String toString() {
        StringBuilder buffer = new StringBuilder(" " + Symbols.STAMP_OPENER + creationTime);
        buffer.append(" ").append(Symbols.STAMP_STARTER).append(" ");
        for (int i = 0; i < baseLength; i++) {
            buffer.append(Long.toString(evidentialBase[i]));
            if (i < (baseLength - 1)) {
                buffer.append(Symbols.STAMP_SEPARATOR);
            } else {
                buffer.append(Symbols.STAMP_CLOSER).append(" ");
            }
        }
        return buffer.toString();
    }
    
    public long getPutInTime(){
        return putInTime;
    }
    
    public void setPutInTime(long time){
        putInTime = time;
    }
    
    public Tense getTense(){
        return tense;
    }
    
    public boolean evidenceIsCyclic(){
        
        TreeSet<Long> taskBase = new TreeSet(); 
        
        for(long l : evidentialBase){
            if(!taskBase.add(l))
                return true;
        }
        
        return false;
    }
    
}
