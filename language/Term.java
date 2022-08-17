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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import nars.inference.TemporalRules;
import nars.io.Symbols;
import nars.storage.Memory;

/**
 * Term is the basic component of Narsese, and the object of processing in NARS.
 * <p>
 * A Term may have an associated Concept containing relations with other Terms.
 * It is not linked in the Term, because a Concept may be forgot while the Term
 * exists. Multiple objects may represent the same Term.
 */
public class Term implements Cloneable, Comparable<Term> {

    /**
     * A Term is identified uniquely by its name, a sequence of characters in a
     * given alphabet (ASCII or Unicode)
     */
    protected String name;
    
    public static Term SELF = new SetExt(new ArrayList<Term>(Arrays.asList(new Term("SELF"))));

    /**
     * Default constructor that build an internal Term
     */
    protected Term() {
    }

    /**
     * Constructor with a given name
     *
     * @param name A String as the name of the Term
     */
    public Term(String name) {
        this.name = name;
    }
    
    
    public boolean isExecutable(Memory mem){
        return this instanceof Operation;
    }
    
    /**
     * Reporting the name of the current Term.
     *
     * @return The name of the term as a String
     */
    public String getName() {
        return name;
    }
    
    public void setName(String name){
        this.name = name;
    }

    /**
     * Make a new Term with the same name.
     *
     * @return The new Term
     */
    @Override
    public Term clone() /*throws CloneNotSupportedException*/ {
        return new Term(name);
    }

    /**
     * Equal terms have identical name, though not necessarily the same
     * reference.
     *
     * @return Whether the two Terms are equal
     * @param that The Term to be compared with the current Term
     */
    @Override
    public boolean equals(Object that) {
        return (that instanceof Term) && name.equals(((Term) that).getName());
    }

    /**
     * Produce a hash code for the term
     *
     * @return An integer hash code
     */
    @Override
    public int hashCode() {
        return (name != null ? name.hashCode() : 7);
    }

    /**
     * Check whether the current Term can name a Concept.
     *
     * @return A Term is constant by default
     */
    public boolean isConstant() {
        return true;
    }

    /**
     * Blank method to be override in CompoundTerm
     */
    public void renameVariables() {
    }

    public int getTemporalOrder(){
        return TemporalRules.ORDER_NONE;
    }
    
    public int containedTemporalRelations(){
        return 0;
    }
    
    /**
     * The syntactic complexity, for constant atomic Term, is 1.
     *
     * @return The complexity of the term, an integer
     */
    public int getComplexity() {
        return 1;
    }
    
    /**
     * Gets simplicity of the term based on the number of atomic components
     * Single atomic term has max simplicity of 1. Power was chosen as -0.5 and
     * subject for research
     * @return 
     */
    public double getSimplicity(){
        return Math.pow(this.getComplexity(), -0.5);
    }

    /**
     * Orders among terms: variable < atomic < compound
     * @param that The Term to be compared with the current Term
     * @return The same as compareTo as defined on Strings
     */
    @Override
    public int compareTo(Term that) {
        if (that instanceof CompoundTerm) {
            return -1;
        } else if (that instanceof Variable) {
            return 1;
        } else {
            return name.compareTo(that.getName());
        }
    }

    /**
     * Recursively check if a compound contains a term
     *
     * @param target The term to be searched
     * @return Whether the two have the same content
     */
    public boolean containTerm(Term target) {
        return equals(target);
    }
    
    /**
     * The same as getName by default, used in display only.
     *
     * @return The name of the term as a String
     */
    @Override
    public final String toString() {
        return name;
    }
    
    public boolean hasVarQuery(){
        return false;
    }
    
    public boolean hasVarIndep(){
        return false;
    }
    
    public boolean hasVarDep(){
        return false;
    }
    
    public boolean hasInterval(){
        return false;
    }
    
    public boolean hasVar(char type){
        
        switch (type){
            case Symbols.VAR_DEPENDENT: return hasVarDep();
            case Symbols.VAR_INDEPENDENT: return hasVarIndep();
            case Symbols.VAR_QUERY: return hasVarQuery();
        }
        throw new IllegalStateException("Invalid variable type: " + type);
    }
    
    public boolean hasVar(){
        return false;
    }
    
    public Map<Term, Integer> countTerm(Map<Term, Integer> map){
        
        if(map == null)
            map = new LinkedHashMap<Term, Integer>();
        
        map.put(this, map.getOrDefault(this, 0) + 1);
        
        return map;
        
    }
    
    public static ArrayList<Term> toSortedSetArray(ArrayList<Term> arg){
        
        switch(arg.size()){
            
            case 0: return new ArrayList();
            case 1: return new ArrayList(Arrays.asList(arg.get(0)));
            case 2: Term a = arg.get(0);
                    Term b = arg.get(1);
                    int c  = a.compareTo(b);
                    
                    if(c < 0) return new ArrayList(Arrays.asList(a, b));
                    else if(c > 0) return new ArrayList(Arrays.asList(b, a));
                    else return new ArrayList(Arrays.asList(a));
        }
            
        return arg;
    }
    
}
