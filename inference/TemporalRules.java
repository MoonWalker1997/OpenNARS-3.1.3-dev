/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nars.inference;

import nars.entity.BudgetValue;
import nars.entity.Concept;
import nars.entity.Sentence;
import nars.entity.Stamp;
import nars.entity.Task;
import nars.entity.TruthValue;
import nars.io.Symbols;
import nars.language.Conjunction;
import nars.language.Implication;
import nars.language.Inheritance;
import nars.language.Operation;
import nars.language.Similarity;
import nars.language.Statement;
import nars.language.Term;
import nars.main.Parameters;
import nars.storage.Memory;

/**
 *
 * @author Xiang
 */
public class TemporalRules {
    
    public static final int ORDER_NONE = 2;
    public static final int ORDER_FORWARD = 1;
    public static final int ORDER_CONCURRENT = 0;
    public static final int ORDER_BACKWARD = -1;
    public static final int ORDER_INVALID = -2;
    
    public final static int reverseOrder(final int order){
        if(order == ORDER_NONE)
            return ORDER_NONE;
        else
            return -order;
    }
    
    public final static boolean matchingOrder(final Sentence a, final Sentence b){
        return matchingOrder(a.getTemporalOrder(), b.getTemporalOrder());
    }
    
    public final static boolean matchingOrder(final int order1, final int order2){
        return (order1 == order2) || (order1 == ORDER_NONE) || (order2 == ORDER_NONE);
    }
    
    public final static int dedExeOrder(final int order1, final int order2){
        int order = ORDER_INVALID;
        if((order1 == order2) || (order2 == TemporalRules.ORDER_NONE))
            order = order1;
        else if ((order1 == TemporalRules.ORDER_NONE) || (order1 == TemporalRules.ORDER_CONCURRENT))
            order = order2;
        else if (order2 == TemporalRules.ORDER_CONCURRENT)
            order = order1;
        
        return order;
    }
    
    public final static int abdIndComOrder(final int order1, final int order2){
        int order = ORDER_INVALID;
        if(order2 == TemporalRules.ORDER_NONE)
            order = order1;
        else if (order1 == TemporalRules.ORDER_NONE || order1 == TemporalRules.ORDER_CONCURRENT)
            order = reverseOrder(order2);
        else if (order2 == TemporalRules.ORDER_CONCURRENT || order1 == -order2)
            order = order1;
        
        return order;  
    }
    
    public final static int analogyOrder(final int order1, final int order2, final int figure) {
        int order = ORDER_INVALID;
        if ((order2 == TemporalRules.ORDER_NONE) || (order2 == TemporalRules.ORDER_CONCURRENT)) {
            order = order1;
        } else if ((order1 == TemporalRules.ORDER_NONE) || (order1 == TemporalRules.ORDER_CONCURRENT)) {
            order = (figure < 20) ? order2 : reverseOrder(order2);
        } else if (order1 == order2) {
            if ((figure == 12) || (figure == 21)) {
                order = order1;
            }
        } else if ((order1 == -order2)) {
            if ((figure == 11) || (figure == 22)) {
                order = order1;
            }
        }
        return order;
    }

    public static final int resemblanceOrder(final int order1, final int order2, final int figure) {
        int order = ORDER_INVALID;
        final int order1Reverse = reverseOrder(order1);
        
        if ((order2 == TemporalRules.ORDER_NONE)) {
            order = (figure > 20) ? order1 : order1Reverse; // switch when 11 or 12
        } else if ((order1 == TemporalRules.ORDER_NONE) || (order1 == TemporalRules.ORDER_CONCURRENT)) {
            order = (figure % 10 == 1) ? order2 : reverseOrder(order2); // switch when 12 or 22
        } else if (order2 == TemporalRules.ORDER_CONCURRENT) {
            order = (figure > 20) ? order1 : order1Reverse; // switch when 11 or 12
        } else if (order1 == order2) {
            order = (figure == 21) ? order1 : -order1;
        }
        return order;
    }

    /*public static final int composeOrder(final int order1, final int order2) {
        int order = ORDER_INVALID;
        if (order2 == TemporalRules.ORDER_NONE) {
            order = order1;
        } else if (order1 == TemporalRules.ORDER_NONE) {
            order = order2;
        } else if (order1 == order2) {
            order = order1;
        }
        return order;
    }*/
    
     public static final int composeOrder(final int order1, final int order2) {
        
        if(order1 == TemporalRules.ORDER_NONE && order2 == TemporalRules.ORDER_NONE)
            return ORDER_NONE;
        else if(order1 != TemporalRules.ORDER_NONE && order2 != TemporalRules.ORDER_NONE && order1 == order2)
            return order1;
        else 
            return ORDER_INVALID;

    }
    
    /** whether temporal induction can generate a task by avoiding producing wrong terms; only one temporal operator is allowed
     * @param t
     * @return  */
    public final static boolean tooMuchTemporalStatements(final Term t) {
        return (t == null) || (t.containedTemporalRelations() > 1);
    }
    
    /** whether a term can be used in temoralInduction(,,)
     * @param t */
    protected static boolean termForTemporalInduction(final Term t) {
        return (t instanceof Inheritance) || (t instanceof Similarity);
    }

    public static Task temporalInduction(Sentence s1, Sentence s2, Task currentTask, Sentence belief, boolean sequenceInduction, Memory memory) {      
        
        // 如果有语句的真值为空，或者有语句不是判断句，或者有语句是永恒的，则无法做时间归纳
        if ((s1.getTruth() == null) || (s2.getTruth() == null) || s1.getPunctuation() != Symbols.JUDGMENT_MARK || s2.getPunctuation() != Symbols.JUDGMENT_MARK
                || s1.isEternal() || s2.isEternal())
            return null;
            
        Term t1 = s1.getContent();
        Term t2 = s2.getContent();
        
        Stamp stamp1 = s1.getStamp();
        Stamp stamp2 = s2.getStamp();
        
        if(Statement.invalidStatement(t1, t2))
            return null;      

        // s1的发生时间
        final long time1 = s1.getOccurrenceTime();
        // s2的发生时间
        final long time2 = s2.getOccurrenceTime();
        // s1与s2的时间差
        final long timeDiff = (int)(time2 - time1);
        
        if(timeDiff == 0)
            return null;

        // 判断两个时间是否发生在同一时间，时间差大于2.5或者小于-2.5
        // 如果两个语句并不是同一时间发生，用时间差计算出时间间隔
        int order = order(timeDiff, Parameters.DURATION);    
        
        final TruthValue truth1 = TruthFunctions.induction(s1.getTruth(), s2.getTruth());
        final TruthValue truth4 = TruthFunctions.intersection(s1.getTruth(), s2.getTruth());
        final BudgetValue budget1 = BudgetFunctions.forward(truth1, memory);
        final BudgetValue budget4 = BudgetFunctions.forward(truth4, memory); //this one is sequence in sequenceBag, no need to reduce here
        
        Task newTask = null;
        
        if((!(t1 instanceof Operation) && !(t2 instanceof Operation)) && !(t2 instanceof Conjunction)){
        
            if(t1 instanceof Conjunction){
                
                if((((Conjunction) t1).getComponents()).size() == 2){
                    
                    if(!((((Conjunction) t1).getComponents()).get(0) instanceof Operation) 
                            && ((((Conjunction) t1).getComponents()).get(1) instanceof Operation)){
                        
                        Statement statement1 = Implication.make(t1, t2, order, Math.abs(timeDiff), memory);     
                        Stamp newStamp = new Stamp(stamp1,stamp2, memory.getTime());
                        newStamp.setOccurrenceTime(Stamp.ETERNAL);
                        Sentence sentence = new Sentence(statement1, s1.getPunctuation(), truth1, newStamp, true);
                        newTask = new Task(sentence, budget1, currentTask, belief); 
                        
                    }
                }
            }else{
                                  

                Statement statement1 = Implication.make(t1, t2, order, Math.abs(timeDiff), memory);     
                Stamp newStamp = new Stamp(stamp1,stamp2, memory.getTime());
                newStamp.setOccurrenceTime(Stamp.ETERNAL);
                Sentence sentence = new Sentence(statement1, s1.getPunctuation(), truth1, newStamp, true);
                newTask = new Task(sentence, budget1, currentTask, belief); 
                
            }            
        }
        
        if(newTask != null){
            //memory.generalInfoReport("New Implication: " + newTask.getName());
            System.out.println(" OUT: " + newTask.toString());
            memory.immediateProcess(newTask);
        }
        
        if(!sequenceInduction){
            return null;
        }
        
        Term statement4 = null;
        
        //memory.generalInfoReport("????????");
        
        switch (order) {
            case TemporalRules.ORDER_FORWARD:
                statement4 = Conjunction.make(s1.getContent(), s2.getContent(), order, memory);
                break;
            case TemporalRules.ORDER_BACKWARD:
                statement4 = Conjunction.make(s2.getContent(), s1.getContent(), reverseOrder(order), memory);
                break;
            default:
                statement4 = Conjunction.make(s1.getContent(), s2.getContent(), order, memory);
                break;
        }      
        
        Stamp newStamp2 = new Stamp(stamp1, stamp2, memory.getTime());
        newStamp2.setOccurrenceTime(time2);
        Sentence sentence = new Sentence(statement4, s2.getPunctuation(), truth4, newStamp2, true);
        newTask = new Task(sentence, budget4, currentTask, belief);
        newTask.setTemporalInduction(true);
        //memory.generalInfoReport(newTask.getName());
        
        return newTask;        
    }
    
    public static int order(final long timeDiff, final double durationCycles) {
        final double halfDuration = durationCycles/2;
        if (timeDiff > halfDuration) {
            return ORDER_FORWARD;
        } else if (timeDiff < -halfDuration) {
            return ORDER_BACKWARD;
        } else {
            return ORDER_CONCURRENT;
        }
    }
    
//    public static int order(final long timeDiff, final double durationCycles){
//
//        if(timeDiff > 0)
//            return ORDER_FORWARD;
//        else if(timeDiff < 0)
//            return ORDER_BACKWARD;
//        else
//            return ORDER_CONCURRENT;
//    }
    
    /** if (relative) event B after (stationary) event A then order=forward;
     *                event B before       then order=backward
     *                occur at the same time, re
     * @param a
     * @param b
     * @param durationCycles
     * @return 
     */
    public static int order(final long a, final long b, final int durationCycles) {        
        if ((a == Stamp.ETERNAL) || (b == Stamp.ETERNAL))
            throw new IllegalStateException("order() does not compare ETERNAL times");
        
        return order(b - a, durationCycles);
    }
    
    public static boolean concurrent(final long a, final long b, final int durationCycles) {        
        //since Stamp.ETERNAL is Integer.MIN_VALUE, 
        //avoid any overflow errors by checking eternal first
        
        if (a == Stamp.ETERNAL) {
            //if both are eternal, consider concurrent.  this is consistent with the original
            //method of calculation which compared equivalent integer values only
            return (b == Stamp.ETERNAL);
        }
        else if (b == Stamp.ETERNAL) {
            return false; //a==b was compared above
        }
        else {        
            return order(a, b, durationCycles) == ORDER_CONCURRENT;
        }
    }
    
}
