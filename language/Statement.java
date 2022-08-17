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
import nars.inference.TemporalRules;

import nars.io.Symbols;
import nars.main.Parameters;
import nars.storage.Memory;

/**
 * A statement is a compound term, consisting of a subject, a predicate, and a
 * relation symbol in between. It can be of either first-order or higher-order.
 */
public abstract class Statement extends CompoundTerm {

    private long interval;
    /**
     * Constructor with partial values, called by make
     *
     * @param arg The component list of the term
     */
    protected Statement(ArrayList<Term> arg) {
        super(arg);
    }   
    
    protected Statement(String name, ArrayList<Term> arg){
        super(name, arg);
    }

    /**
     * Default constructor
     */
    protected Statement() {
    }
    
    /**
     * Constructor with full values, called by clone
     *
     * @param n The nameStr of the term
     * @param cs Component list
     * @param con Constant indicator
     * @param i Syntactic complexity of the compound
     */
    protected Statement(String n, ArrayList<Term> cs, boolean con, short i) {
        super(n, cs, con, i);
    }

    public long getInterval(){
        return interval;
    }
    
    public void setInterval(long interval){
        this.interval = interval;
    }
    
    /**
     * Make a Statement from String, called by StringParser
     *
     * @param relation The relation String
     * @param subject The first component
     * @param predicate The second component
     * @param memory Reference to the memory
     * @return The Statement built
     */
    public static Statement make(String relation, Term subject, Term predicate, Memory memory) {
        //System.out.println("make relation: " + relation);
        if (invalidStatement(subject, predicate)) {
            return null;
        }
        if (relation.equals(Symbols.INHERITANCE_RELATION)) {
            return Inheritance.make(subject, predicate, memory);
        }
        if (relation.equals(Symbols.SIMILARITY_RELATION)) {
            return Similarity.make(subject, predicate, memory);
        }
        if (relation.equals(Symbols.INSTANCE_RELATION)) {
            return Instance.make(subject, predicate, memory);
        }
        if (relation.equals(Symbols.PROPERTY_RELATION)) {
            return Property.make(subject, predicate, memory);
        }
        if (relation.equals(Symbols.INSTANCE_PROPERTY_RELATION)) {
            return InstanceProperty.make(subject, predicate, memory);
        }
        if (relation.equals(Symbols.IMPLICATION_RELATION) ) {
            return Implication.make(subject, predicate, TemporalRules.ORDER_NONE, 0, memory);
        }
        if (relation.equals(Symbols.IMPLICATION_AFTER)){
            return Implication.make(subject, predicate, TemporalRules.ORDER_FORWARD, Parameters.DEFAULT_TIME_INTERVAL , memory);
        }
        if (relation.equals(Symbols.IMPLICATION_BEFORE)){
            //System.out.println("relation2: " + relation);
            return Implication.make(subject, predicate, TemporalRules.ORDER_BACKWARD, Parameters.DEFAULT_TIME_INTERVAL , memory);
        }
        if (relation.equals(Symbols.IMPLICATION_WHEN)){
            return Implication.make(subject, predicate, TemporalRules.ORDER_CONCURRENT, 0, memory);
        }
        if(relation.equals(Symbols.EQUIVALENCE_AFTER)){
            return Equivalence.make(subject, predicate, TemporalRules.ORDER_FORWARD, memory, Parameters.DEFAULT_TIME_INTERVAL);
        }
        if(relation.equals(Symbols.EQUIVALENCE_WHEN)){
            return Equivalence.make(subject, predicate, TemporalRules.ORDER_CONCURRENT, memory, Parameters.DEFAULT_TIME_INTERVAL);
        }
        if (relation.equals(Symbols.EQUIVALENCE_RELATION)) {
            return Equivalence.make(subject, predicate, TemporalRules.ORDER_NONE, memory, 0);
        }
        return null;
    }

    /**
     * Make a Statement from given components, called by the rules
     *
     * @param temporalOrder
     * @return The Statement built
     * @param subj The first component
     * @param pred The second component
     * @param statement A sample statement providing the class type
     * @param memory Reference to the memory
     */
    public static Statement make(Statement statement, Term subj, Term pred, int temporalOrder, Memory memory) {
        if (statement instanceof Inheritance) {
            return Inheritance.make(subj, pred, memory);
        }
        if (statement instanceof Similarity) {
            return Similarity.make(subj, pred, memory);
        }
        if (statement instanceof Implication) {
            if(temporalOrder == TemporalRules.ORDER_FORWARD || temporalOrder == TemporalRules.ORDER_BACKWARD)
                return Implication.make(subj, pred, temporalOrder, Parameters.DEFAULT_TIME_INTERVAL, memory);
            else
                return Implication.make(subj, pred, temporalOrder, 0, memory);
        }
        if (statement instanceof Equivalence) {
            return Equivalence.make(subj, pred, temporalOrder, memory, 0);
        }
        return null;
    }

    /**
     * Make a symmetric Statement from given components and temporal
     * information, called by the rules
     *
     * @param statement A sample asymmetric statement providing the class type
     * @param subj The first component
     * @param pred The second component
     * @param memory Reference to the memory
     * @return The Statement built
     */
    public static Statement makeSym(Statement statement, Term subj, Term pred, int temporalOrder, Memory memory) {
        if (statement instanceof Inheritance) {
            return Similarity.make(subj, pred, memory);
        }
        if (statement instanceof Implication) {
            return Equivalence.make(subj, pred, temporalOrder, memory, ((Implication)statement).getInterval());
        }
        return null;
    }

    /**
     * Check Statement relation symbol, called in StringPaser
     *
     * @param s0 The String to be checked
     * @return if the given String is a relation symbol
     */
    public static boolean isRelation(String s0) {
        String s = s0.trim();
        if (s.length() != 3) {
            return false;
        }
        return (s.equals(Symbols.INHERITANCE_RELATION)
                || s.equals(Symbols.SIMILARITY_RELATION)
                || s.equals(Symbols.INSTANCE_RELATION)
                || s.equals(Symbols.PROPERTY_RELATION)
                || s.equals(Symbols.INSTANCE_PROPERTY_RELATION)
                || s.equals(Symbols.IMPLICATION_RELATION)
                || s.equals(Symbols.IMPLICATION_BEFORE)
                || s.equals(Symbols.IMPLICATION_WHEN)
                || s.equals(Symbols.IMPLICATION_AFTER)
                || s.equals(Symbols.EQUIVALENCE_AFTER)
                || s.equals(Symbols.EQUIVALENCE_WHEN)
                || s.equals(Symbols.EQUIVALENCE_BEFORE)
                || s.equals(Symbols.EQUIVALENCE_RELATION));
    }

    /**
     * Override the default in making the nameStr of the current term from
     * existing fields
     *
     * @return the nameStr of the term
     */
    @Override
    protected String makeName() {
        //System.out.println("???: " + operator());
        return makeStatementName(getSubject(), operator(), getPredicate());
    }

    /**
     * Default method to make the nameStr of an image term from given fields
     *
     * @param subject The first component
     * @param predicate The second component
     * @param relation The relation operator
     * @return The nameStr of the term
     */
    protected static String makeStatementName(Term subject, String relation, Term predicate) {
        //System.out.println("relation3: " + relation);
        StringBuilder nameStr = new StringBuilder();
        nameStr.append(Symbols.STATEMENT_OPENER);
        nameStr.append(subject.getName());
        nameStr.append(' ').append(relation).append(' ');
        nameStr.append(predicate.getName());
        nameStr.append(Symbols.STATEMENT_CLOSER);
        //System.out.println("sss: " + nameStr.toString());
        return nameStr.toString();
    }

    /**
     * Check the validity of a potential Statement. [To be refined]
     *
     * @param subject The first component
     * @param predicate The second component
     * @return Whether The Statement is invalid
     */
    public static boolean invalidStatement(Term subject, Term predicate) {
//        if (subject == null || predicate == null || subject.equals(predicate)) {
//            return true;
//        }
        if (subject == null || predicate == null) {
            return true;
        }
        if (invalidReflexive(subject, predicate)) {
            return true;
        }
        if (invalidReflexive(predicate, subject)) {
            return true;
        }
        if ((subject instanceof Statement) && (predicate instanceof Statement)) {
            Statement s1 = (Statement) subject;
            Statement s2 = (Statement) predicate;
            Term t11 = s1.getSubject();
            Term t12 = s1.getPredicate();
            Term t21 = s2.getSubject();
            Term t22 = s2.getPredicate();
            if (t11.equals(t22) && t12.equals(t21)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if one term is identical to or included in another one, except in a
     * reflexive relation
     * 检查一个词项是否与另一个相同，或者被包含到另外一个词项中，除非在一个反身的关系中
     * <p>
     * @param t1 The first term
     * @param t2 The second term
     * @return Whether they cannot be related in a statement
     */
    private static boolean invalidReflexive(Term t1, Term t2) {
        // 如果t1不是一个复合词项，直接返回false，因为原子词项不可能包含另一个原子词项
        if (!(t1 instanceof CompoundTerm)) {
            return false;
        }
        
        CompoundTerm com = (CompoundTerm) t1;
        // 假如t1是一个外延镜像或者是内涵镜像，则返回false
        if ((com instanceof ImageExt) || (com instanceof ImageInt)) {
            return false;
        }
        
        // 返回t1是否包含t2
        return com.containComponent(t2);
    }

    public static boolean invalidPair(String s1, String s2) {
        if (Variable.containVarIndep(s1) && !Variable.containVarIndep(s2)) {
            return true;
        } else if (!Variable.containVarIndep(s1) && Variable.containVarIndep(s2)) {
            return true;
        }
        return false;
    }

    /**
     * Check the validity of a potential Statement. [To be refined]
     * <p>
     * Minimum requirement: the two terms cannot be the same, or containing each
     * other as component
     *
     * @return Whether The Statement is invalid
     */
    public boolean invalid() {
        return invalidStatement(getSubject(), getPredicate());
    }

    /**
     * Return the first component of the statement
     *
     * @return The first component
     */
    public Term getSubject() {
        return components.get(0);
    }

    /**
     * Return the second component of the statement
     *
     * @return The second component
     */
    public Term getPredicate() {
        return components.get(1);
    }
}
