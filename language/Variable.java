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

import nars.io.Symbols;
import nars.storage.Memory;

/**
 * A variable term, which does not correspond to a concept
 */
public class Variable extends Term {
    /**
     * Constructor, from a given variable name
     *
     * @param s A String read from input
     */
    public Variable(String s) {
        super(s);
    }

    /**
     * Clone a Variable
     *
     * @return The cloned Variable
     */
    @Override
    public Variable clone() {
        return new Variable(name);
    }

    /**
     * Get the type of the variable
     *
     * @return The variable type
     */
    public char getType() {
        return name.charAt(0);
    }

    /**
     * A variable is not constant
     *
     * @return false
     */
    @Override
    public boolean isConstant() {
        return false;
    }

    /**
     * The syntactic complexity of a variable is 0, because it does not refer to
     * any concept.
     *
     * @return The complexity of the term, an integer
     */
    @Override
    public int getComplexity() {
        return 0;
    }

    /**
     * Check whether a string represent a name of a term that contains an
     * independent variable
     *
     * @param n The string name to be checked
     * @return Whether the name contains an independent variable
     */
    public static boolean containVarIndep(String n) {
        return n.indexOf(Symbols.VAR_INDEPENDENT) >= 0;
    }

    /**
     * Check whether a string represent a name of a term that contains a
     * dependent variable
     *
     * @param n The string name to be checked
     * @return Whether the name contains a dependent variable
     */
    public static boolean containVarDep(String n) {
        return n.indexOf(Symbols.VAR_DEPENDENT) >= 0;
    }

    /**
     * Check whether a string represent a name of a term that contains a query
     * variable
     *
     * @param n The string name to be checked
     * @return Whether the name contains a query variable
     */
    public static boolean containVarQuery(String n) {
        return n.indexOf(Symbols.VAR_QUERY) >= 0;
    }

    /**
     * Check whether a string represent a name of a term that contains a
     * variable
     *
     * @param n The string name to be checked
     * @return Whether the name contains a variable
     */
    public static boolean containVar(String n) {
        return containVarIndep(n) || containVarDep(n) || containVarQuery(n);
    }

    /**
     * To unify two terms
     *
     * @param type The type of variable that can be substituted
     * @param t1 The first term
     * @param t2 The second term
     * @return Whether the unification is possible
     */
    public static boolean unify(char type, Term t1, Term t2) {
        return unify(type, t1, t2, t1, t2);
    }

    /**
     * To unify two terms
     *
     * @param type The type of variable that can be substituted
     * @param t1 The first term to be unified
     * @param t2 The second term to be unified
     * @param compound1 The compound containing the first term
     * @param compound2 The compound containing the second term
     * @return Whether the unification is possible
     */
    public static boolean unify(char type, Term t1, Term t2, Term compound1, Term compound2) {
        //System.out.println("456");
        HashMap<Term, Term> map1 = new HashMap<>();
        HashMap<Term, Term> map2 = new HashMap<>();    
        
        boolean hasSubs = findSubstitute(type, t1, t2, map1, map2); // find substitution
        if (hasSubs) {
//            renameVar(map1, compound1, "-1");
//            renameVar(map2, compound2, "-2");
            //System.out.println("compound1 " + compound1.getName());
            if (!map1.isEmpty()) {
                ((CompoundTerm) compound1).applySubstitute(map1);
                //System.out.println("compound1 " + compound1.getName());
                ((CompoundTerm) compound1).renameVariables();

            }
            if (!map2.isEmpty()) {
                ((CompoundTerm) compound2).applySubstitute(map2);
                ((CompoundTerm) compound2).renameVariables();
            }
        }
        return hasSubs;
    }

    /**
     * To recursively find a substitution that can unify two Terms without
     * changing them
     *
     * @param type The type of Variable to be substituted
     * @param term1 The first Term to be unified
     * @param term2 The second Term to be unified
     * @param map1 The substitution for term1 formed so far
     * @param map2 The substitution for term2 formed so far
     * @return Whether there is a substitution that unifies the two Terms
     */
    public static boolean findSubstitute(char type, Term term1, Term term2,
            HashMap<Term, Term> map1, HashMap<Term, Term> map2) {
        //System.out.println("789");
        Term t;
        
        /*System.out.println("term1: " + term1.getName());
        System.out.println("Type: " + type);
        System.out.println("term2: " + term2.getName());*/
        
        if ((term1 instanceof Variable) && (((Variable) term1).getType() == type)) {
            //System.out.println("987");
            Variable var1 = (Variable) term1;
            //System.out.println("term1 type: " + var1.getType());
            t = map1.get(var1);
            if (t != null) {    // already mapped
                return findSubstitute(type, t, term2, map1, map2);
            } else {            // not mapped yet
                //System.out.println("term2: " + term2.getName());
                if ((term2 instanceof Variable) && (((Variable) term2).getType() == type)) {
                    //System.out.println("term2 is a variable");
                    Variable CommonVar = makeCommonVariable(term1, term2);
                    //System.out.println("commonvar: " + CommonVar.getName());
                    //System.out.println("commonvar: " + CommonVar.getType());
                    map1.put(var1, CommonVar);  // unify
                    map2.put(term2, CommonVar);  // unify
                    //System.out.println("map1: " + map1.toString());
                    //System.out.println("map2: " + map2.toString());
                } else {
                    map1.put(var1, term2);  // elimination
                    //System.out.println("map1: " + map1.toString());
                    if (isCommonVariable(var1)) {
                        map2.put(var1, term2);
                        //System.out.println("map2: " + map2.toString());
                    }
                    //System.out.println("Out map2: " + map2.toString());
                }
                return true;
            }
        } else if ((term2 instanceof Variable) && (((Variable) term2).getType() == type)) {
            //System.out.println("654");
            //System.out.println("term1 is not variable, term2 is variable");
            Variable var2 = (Variable) term2;
            /*System.out.println("term2: " + term2.getName());
            System.out.println("map2: " + map2.toString());*/
            t = map2.get(var2);
            if (t != null) {    // already mapped
                return findSubstitute(type, term1, t, map1, map2);
            } else {            // not mapped yet
                map2.put(var2, term1);  // elimination
                //System.out.println("3 map2: " + map2.toString());
                if (isCommonVariable(var2)) {
                    map1.put(var2, term1);
                    //System.out.println("map1: " + map1.toString());
                }
                return true;
            }
        } else if ((term1 instanceof CompoundTerm) && term1.getClass().equals(term2.getClass())) {
            
            CompoundTerm cTerm1 = (CompoundTerm) term1;
            CompoundTerm cTerm2 = (CompoundTerm) term2;
            //System.out.println("cterm1: " + cTerm1.getName());
            //System.out.println("cterm2: " + cTerm2.getName());
            if (cTerm1.size() != (cTerm2).size()) {
                return false;
            }
            if ((cTerm1 instanceof ImageExt) && (((ImageExt) cTerm1).getRelationIndex() != ((ImageExt) cTerm2).getRelationIndex())
                    || (cTerm1 instanceof ImageInt) && (((ImageInt) cTerm1).getRelationIndex() != ((ImageInt) cTerm2).getRelationIndex())) {
                return false;
            }
            ArrayList<Term> list = cTerm1.cloneComponents();
            //System.out.println("Before shuffle: " + list.toString());
            if (cTerm1.isCommutative()) {
                 Collections.shuffle(list, Memory.randomNumber);
            } 
            //System.out.println("After shuffle: " + list.toString());
            for (int i = 0; i < cTerm1.size(); i++) {   // assuming matching order
                Term t1 = list.get(i);
                Term t2 = cTerm2.componentAt(i);
                //System.out.println("t1: " + t1.getName());
                //System.out.println("t2: " + t2.getName());
                if (!findSubstitute(type, t1, t2, map1, map2)) {
                    return false;
                }
            }
            return true;
        }
        return term1.equals(term2); // for atomic constant terms
    }

    private static Variable makeCommonVariable(Term v1, Term v2) {
        return new Variable(v1.getName() + v2.getName() + '$');
    }

    private static boolean isCommonVariable(Variable v) {
        String s = v.getName();
        return s.charAt(s.length() - 1) == '$';
    }

    /**
     * Check if two terms can be unified
     *
     * @param type The type of variable that can be substituted
     * @param term1 The first term to be unified
     * @param term2 The second term to be unified
     * @return Whether there is a substitution
     */
    public static boolean hasSubstitute(char type, Term term1, Term term2) {
        return findSubstitute(type, term1, term2, new HashMap<Term, Term>(), new HashMap<Term, Term>());
    }

    /**
     * Rename the variables to prepare for unification of two terms
     *
     * @param map The substitution so far
     * @param term The term to be processed
     * @param suffix The suffix that distinguish the variables in one premise
     * from those from the other
     */
//    private static void renameVar(HashMap<Term, Term> map, Term term, String suffix) {
//        if (term instanceof Variable) {
//            Term t = map.get(term);
//            if (t == null) {    // new mapped yet
//                map.put(term, new Variable(term.getName() + suffix));  // rename             
//            }
//        } else if (term instanceof CompoundTerm) {
//            for (Term t : ((CompoundTerm) term).components) {   // assuming matching order, to be refined in the future
//                renameVar(map, t, suffix);
//            }
//        }
//    }
    /**
     * variable terms are listed first alphabetically
     *
     * @param that The Term to be compared with the current Term
     * @return The same as compareTo as defined on Strings
     */
    @Override
    public final int compareTo(Term that) {
        return (that instanceof Variable) ? name.compareTo(that.getName()) : -1;
    }
}
