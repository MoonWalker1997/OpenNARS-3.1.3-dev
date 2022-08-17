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

import nars.inference.TemporalRules;
import nars.io.Symbols;
import nars.storage.Memory;

import java.util.ArrayList;
import java.util.LinkedHashSet;

/**
 * Conjunction of statements
 */
public class Conjunction extends CompoundTerm {


    private int temporalOrder;

    /**
     * Constructor with partial values, called by make
     *
     * @param arg The component list of the term
     */
    protected Conjunction(ArrayList<Term> arg) {
        super(arg);
    }

    protected Conjunction(String name, ArrayList<Term> arg, int temporalOrder) {

        super(name, arg);
        this.temporalOrder = temporalOrder;
    }

    /**
     * Constructor with full values, called by clone
     *
     * @param n   The name of the term
     * @param cs  Component list
     * @param con Whether the term is a constant
     * @param i   Syntactic complexity of the compound
     */
    private Conjunction(String n, ArrayList<Term> cs, boolean con, short i) {
        super(n, cs, con, i);
    }

    /**
     * Try to make a new Disjunction from a set of components.Called by the
     * public make methods.
     *
     * @param list
     * @param temporalOrder
     * @param set           a set of Term as components
     * @param memory        Reference to the memory
     * @return the Term generated from the arguments
     */
    public static Term make(ArrayList<Term> list, int temporalOrder, Memory memory) {
        if (list.isEmpty()) {
            return null;
        }                         // special case: single component
        if (list.size() == 1) {
            return list.get(0);
        }                         // special case: single component

        // System.out.println(list.toString());

        String name = "";

        switch (temporalOrder) {
            case TemporalRules.ORDER_FORWARD:
                //System.out.println(list);
                name = makeCompoundName(Symbols.CONJUNCTION_SEQUENCE, list);
                break;
            case TemporalRules.ORDER_CONCURRENT:
                name = makeCompoundName(Symbols.CONJUNCTION_PARALLEL, list);
                break;
            default:
                name = makeCompoundName(Symbols.CONJUNCTION_OPERATOR, list);
                break;
        }

        Term t = memory.nameToListedTerm(name);
        return (t != null) ? t : new Conjunction(name, list, temporalOrder);
    }

    /**
     * Try to make a new compound from two components.Called by the inference
     * rules.
     *
     * @param term1         The first component
     * @param term2         The second component
     * @param temporalOrder
     * @param memory        Reference to the memory
     * @return A compound generated or a term it reduced to
     */
    public static Term make(Term term1, Term term2, int temporalOrder, Memory memory) {

        //System.out.println(temporalOrder);
        LinkedHashSet<Term> set;
        if (term1 instanceof Conjunction && term1.getTemporalOrder() == temporalOrder) {

            set = new LinkedHashSet(((CompoundTerm) term1).cloneComponents());
            if (term2 instanceof Conjunction && term2.getTemporalOrder() == temporalOrder) {
                set.addAll(((CompoundTerm) term2).cloneComponents());
            } // (&,(&,P,Q),(&,R,S)) = (&,P,Q,R,S)
            else {
                set.add(term2.clone());
            }                          // (&,(&,P,Q),R) = (&,P,Q,R)
        } else if (term2 instanceof Conjunction && term2.getTemporalOrder() == temporalOrder) {
            set = new LinkedHashSet(((CompoundTerm) term2).cloneComponents());
            set.add(term1.clone());                              // (&,R,(&,P,Q)) = (&,P,Q,R)
        } else {
            set = new LinkedHashSet();

            set.add(term1.clone());
            set.add(term2.clone());
        }


        ArrayList<Term> list = new ArrayList(set);
        return make(list, temporalOrder, memory);
    }

    /**
     * 如果词项t为一个conjucntion，判断t是否与给定的order为同一个时序
     *
     * @param t
     * @param order
     * @return
     */
    public static boolean sameOrder(Term t, int order) {

        if (t instanceof Conjunction) {
            Conjunction c = (Conjunction) t;
            return c.getTemporalOrder() == order;
        }
        return false;

    }

    /**
     * Try to make a new compound from a list of components. Called by
     * StringParser.
     *
     * @return the Term generated from the arguments
     * @param argList the list of arguments
     * @param memory Reference to the memory
     */
    /*public static Term make(ArrayList<Term> argList, int temporalOrder, Memory memory) {
        //TreeSet<Term> set = new TreeSet(argList); // sort/merge arguments
        return make(argList, temporalOrder, memory);
    }*/

    /**
     * Clone an object
     *
     * @return A new object
     */
    @Override
    public Conjunction clone() {
        return new Conjunction(name, cloneList(components), temporalOrder);
    }

    // overload this method by term type?

    /**
     * Get the operator of the term.
     *
     * @return the operator of the term
     */
    @Override
    public String operator() {

        switch (temporalOrder) {
            case TemporalRules.ORDER_FORWARD:
                return Symbols.CONJUNCTION_SEQUENCE;
            case TemporalRules.ORDER_CONCURRENT:
                return Symbols.CONJUNCTION_PARALLEL;
            default:
                return Symbols.CONJUNCTION_OPERATOR;
        }
    }

    /**
     * Check if the compound is commutative.
     *
     * @return true for commutative
     */
    @Override
    public boolean isCommutative() {
        return temporalOrder != TemporalRules.ORDER_FORWARD;
    }

    @Override
    public int getTemporalOrder() {

        return temporalOrder;

    }


}
