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
 * A Statement about an Equivalence relation.
 */
public class Equivalence extends Statement {

    private int temporalOrder;
    //private static String name;
    /**
     * Constructor with partial values, called by make
     * @param components The component list of the term
     */
    protected Equivalence(ArrayList<Term> components) {
        super(components);
    }

    /**
     * Constructor with full values, called by clone
     * @param n The name of the term
     * @param components Component list
     * @param constant Whether the statement contains open variable
     * @param complexity Syntactic complexity of the compound
     */
    protected Equivalence(String n, ArrayList<Term> components, boolean constant, short complexity) {
        super(n, components, constant, complexity);
    }
    
    protected Equivalence(String name, ArrayList<Term> arg, int temporalOrder, long interval){
        
        super(name, arg);
        this.temporalOrder = temporalOrder;
        this.setInterval(interval);
        //System.out.println("name: " + name);
    }

    /**
     * Clone an object
     * @return A new object
     */
    @Override
    public Equivalence clone() {
        return new Equivalence(name, (ArrayList<Term>) cloneList(components), temporalOrder, this.getInterval());
    }
    
    /**
     * Try to make a new compound from two components.Called by the inference rules.
     * @param subject The first component
     * @param predicate The second component
     * @param temporalOrder
     * @param memory Reference to the memory
     * @param interval
     * @return A compound generated or null
     */
    public static Equivalence make(Term subject, Term predicate, int temporalOrder,  Memory memory, long interval) {  // to be extended to check if subject is Conjunction
        if ((subject instanceof Implication) || (subject instanceof Equivalence)) {
            return null;
        }
        if ((predicate instanceof Implication) || (predicate instanceof Equivalence)) {
            return null;
        }
        if (invalidStatement(subject, predicate)) {
            return null;
        }      
        
        if (subject.compareTo(predicate) > 0 && temporalOrder != TemporalRules.ORDER_BACKWARD && temporalOrder != TemporalRules.ORDER_FORWARD) {
            Term interm = subject;
            subject = predicate;
            predicate = interm;
        }
        
        String name = "";
        
        switch(temporalOrder){
            
            case TemporalRules.ORDER_BACKWARD:
                temporalOrder = TemporalRules.ORDER_FORWARD; 
            case TemporalRules.ORDER_FORWARD:
                name = makeStatementName(subject, Symbols.EQUIVALENCE_AFTER, predicate);
                break;
            case TemporalRules.ORDER_CONCURRENT:
                name = makeStatementName(subject, Symbols.EQUIVALENCE_WHEN, predicate);
                break;
            default:
                name = makeStatementName(subject, Symbols.EQUIVALENCE_RELATION, predicate);
                break;
            
        }
        
        ArrayList<Term> argument = argumentsToList(subject, predicate);
        return new Equivalence(name, argument, temporalOrder, interval);
    }

    /**
     * Get the operator of the term.
     * @return the operator of the term
     */
    @Override
    public String operator() {
        
        switch(temporalOrder){
            
            case TemporalRules.ORDER_FORWARD:
                return Symbols.EQUIVALENCE_AFTER;
            case TemporalRules.ORDER_CONCURRENT:
                return Symbols.EQUIVALENCE_WHEN;
            case TemporalRules.ORDER_BACKWARD:
                return Symbols.EQUIVALENCE_BEFORE;
            
        }
        
        return Symbols.EQUIVALENCE_RELATION;
    }
    
    @Override
    public int getTemporalOrder(){
        return temporalOrder;
    }

    /**
     * Check if the compound is commutative.
     * @return true for commutative
     */
    @Override
    public boolean isCommutative() {
        return true;
    }
}
