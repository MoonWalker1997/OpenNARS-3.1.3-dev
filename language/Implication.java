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
package nars.language;

import java.util.*;
import nars.inference.TemporalRules;

import nars.io.Symbols;
import nars.storage.Memory;

/**
 * A Statement about an Inheritance relation.
 */
public class Implication extends Statement {

    private int temporalOrder;
    /**
     * Constructor with partial values, called by make
     * @param arg The component list of the term
     */
    protected Implication(ArrayList<Term> arg) {
        super(arg);
    }

    /**
     * Constructor with full values, called by clone
     * @param n The name of the term
     * @param cs Component list
     * @param con Whether it is a constant term
     * @param i Syntactic complexity of the compound
     */
    protected Implication(String n, ArrayList<Term> cs, boolean con, short i) {
        super(n, cs, con, i);
    }
    
    protected Implication(String name, ArrayList<Term> arg, int temporalOrder, long interval){
        super(name, arg);
        this.temporalOrder = temporalOrder;
        this.setInterval(interval);
    }

    /**
     * Clone an object
     * @return A new object
     */
    @Override
    public Implication clone() {
        return new Implication(name, (ArrayList<Term>) cloneList(components), temporalOrder, this.getInterval());
    }

    /**
     * Try to make a new compound from two components.Called by the inference rules.
     * @param subject The first component
     * @param predicate The second component
     * @param temporalOrder
     * @param interval
     * @param memory Reference to the memory
     * @return A compound generated or a term it reduced to
     */   
    
    public static Implication make(Term subject, Term predicate, int temporalOrder, long interval, Memory memory) {
        if ((subject == null) || (predicate == null)) {
            return null;
        }
        if ((subject == null) || (predicate == null)) {
            return null;
        }
        if ((subject instanceof Implication) || (subject instanceof Equivalence) || (predicate instanceof Equivalence)) {
            return null;
        }
        if (invalidStatement(subject, predicate)) {
            return null;
        }
        
        String name = "";
        //System.out.println("order: " + temporalOrder);
        switch (temporalOrder) {
            case TemporalRules.ORDER_FORWARD:
                name = makeStatementName(subject, Symbols.IMPLICATION_AFTER, predicate);
                break;
            case TemporalRules.ORDER_BACKWARD:
                //System.out.println("backward");
                name = makeStatementName(subject, Symbols.IMPLICATION_BEFORE, predicate);
                break;
            case TemporalRules.ORDER_CONCURRENT:
                //System.out.println("current");
                name = makeStatementName(subject, Symbols.IMPLICATION_WHEN, predicate);
                break;
            default:
                name = makeStatementName(subject, Symbols.IMPLICATION_RELATION, predicate);
                break;
        }
        
        if (predicate instanceof Implication) {
            Term oldCondition = ((Implication) predicate).getSubject();
            if ((oldCondition instanceof Conjunction) && ((Conjunction) oldCondition).containComponent(subject)) {
                return null;
            }
            Term newCondition = Conjunction.make(subject, oldCondition, temporalOrder, memory);
            return make(newCondition, ((Implication) predicate).getPredicate(), temporalOrder, ((Implication) predicate).getInterval(), memory);
        } else {
            ArrayList<Term> argument = argumentsToList(subject, predicate);
            return new Implication(name, argument, temporalOrder, interval);
        }
    }
    
    /**
     * Get the operator of the term.
     * @return the operator of the term
     */
    @Override
    public String operator() {
        
        switch(temporalOrder){
            
            case TemporalRules.ORDER_FORWARD:
                return Symbols.IMPLICATION_AFTER;
            case TemporalRules.ORDER_CONCURRENT:
                //System.out.println("what?");
                return Symbols.IMPLICATION_WHEN;
            case TemporalRules.ORDER_BACKWARD:
                return Symbols.IMPLICATION_BEFORE;         
        }
        return Symbols.IMPLICATION_RELATION;
    }
    
    @Override
    public int getTemporalOrder(){
        return temporalOrder;
    }
    
}
