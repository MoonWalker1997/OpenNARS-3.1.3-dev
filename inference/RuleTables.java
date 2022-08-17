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
package nars.inference;

import nars.entity.*;
import nars.language.*;
import nars.storage.Memory;
import nars.io.Symbols;

/**
 * Table of inference rules, indexed by the TermLinks for the task and the
 * belief. Used in indirective processing of a task, to dispatch inference cases
 * to the relevant inference rules.
 */
public class RuleTables {

    /**
     * Entry point of the inference engine
     *
     * @param tLink The selected TaskLink, which will provide a task
     * @param bLink The selected TermLink, which may provide a belief
     * @param memory Reference to the memory
     */
    public static void reason(TaskLink tLink, TermLink bLink, Memory memory) {
        
        //System.out.println("New Cycle: " + memory.getTime());
        Task task = memory.currentTask;
        Sentence taskSentence = task.getSentence();
        Term taskTerm = (Term) taskSentence.getContent().clone();         // cloning for substitution
        Term beliefTerm = (Term) bLink.getTarget().clone();       // cloning for substitution
        Concept beliefConcept = memory.termToConcept(beliefTerm);
        Sentence belief = null;
        if (beliefConcept != null) {
            belief = beliefConcept.getBelief(task);
        }
        
        /*if(task != null && belief != null){
            
            System.out.println("task: " + task.getName());
            System.out.println("belief: " + belief.getContent().getName());
            
        }*/
        
        memory.currentBelief = belief;  // may be null
        if (belief != null) {
            LocalRules.match(task, belief, memory);
            //System.out.println("Task: " + taskSentence.toString());
            //System.out.println("Belief: " + belief.toString());
        }
        if (!memory.noResult() && task.getSentence().isJudgment()) {
            return;
        }

        short tIndex = tLink.getIndex(0);
        short bIndex = bLink.getIndex(0);
        switch (tLink.getType()) {          // dispatch first by TaskLink type
            case TermLink.SELF:
                switch (bLink.getType()) {
                    case TermLink.COMPONENT:
                        //System.out.println("1");
                        compoundAndSelf((CompoundTerm) taskTerm, beliefTerm, true, bIndex, memory);
                        break;
                    case TermLink.COMPOUND:
                        //System.out.println("2");
                        compoundAndSelf((CompoundTerm) beliefTerm, taskTerm, false, bIndex, memory);
                        break;
                    case TermLink.COMPONENT_STATEMENT:
                        //System.out.println("3");
                        //System.out.println("Task: " + task.getName());
                        if (belief != null) {
                            SyllogisticRules.detachment(task.getSentence(), belief, bIndex, memory, false);
                        }
                        break;
                    case TermLink.COMPOUND_STATEMENT:
                        //System.out.println("4");
                        if (belief != null) {
                            SyllogisticRules.detachment(belief, task.getSentence(), bIndex, memory, false);
                        }
                        break;
                    case TermLink.COMPONENT_CONDITION:
                        //System.out.println("5");
                        if (belief != null && taskTerm instanceof Implication) {
                            bIndex = bLink.getIndex(1);
                            SyllogisticRules.conditionalDedInd((Implication) taskTerm, bIndex, beliefTerm, tIndex, memory);
                        }
                        break;
                    case TermLink.COMPOUND_CONDITION:
                        //System.out.println("6");
                        if (belief != null && beliefTerm instanceof Implication) {
                            bIndex = bLink.getIndex(1);
                            SyllogisticRules.conditionalDedInd((Implication) beliefTerm, bIndex, taskTerm, tIndex, memory);
                        }
                        break;
                }
                break;
            case TermLink.COMPOUND:
                switch (bLink.getType()) {
                    case TermLink.COMPOUND:
                        //System.out.println("7");
                        compoundAndCompound((CompoundTerm) taskTerm, (CompoundTerm) beliefTerm, tIndex, bIndex, memory);
                        break;
                    case TermLink.COMPOUND_STATEMENT:
                        //System.out.println("8");
                        compoundAndStatement((CompoundTerm) taskTerm, tIndex, (Statement) beliefTerm, bIndex, beliefTerm, memory);
                        break;
                    case TermLink.COMPOUND_CONDITION:
                        //System.out.println("9");
                        if (belief != null) {
                            if (beliefTerm instanceof Implication) {
                                if (Variable.unify(Symbols.VAR_INDEPENDENT, ((Implication) beliefTerm).getSubject(), taskTerm, beliefTerm, taskTerm)) {
                                    //System.out.println("9-1");
                                    detachmentWithVar(belief, taskSentence, bIndex, memory);
                                } else {
                                    //System.out.println("9-2");
                                    SyllogisticRules.conditionalDedInd((Implication) beliefTerm, bIndex, taskTerm, -1, memory);
                                }
                            } else if (beliefTerm instanceof Equivalence) {
                                //System.out.println("9-3");
                                SyllogisticRules.conditionalAna((Equivalence) beliefTerm, bIndex, taskTerm, -1, memory);
                            }
                        }
                        break;
                }
                break;
            case TermLink.COMPOUND_STATEMENT:
                switch (bLink.getType()) {
                    case TermLink.COMPONENT:
                        //System.out.println("10");
                        componentAndStatement((CompoundTerm) memory.currentTerm, bIndex, (Statement) taskTerm, tIndex, memory);
                        break;
                    case TermLink.COMPOUND:
                        //System.out.println("11");
                        compoundAndStatement((CompoundTerm) beliefTerm, bIndex, (Statement) taskTerm, tIndex, beliefTerm, memory);
                        break;
                    case TermLink.COMPOUND_STATEMENT:
                        //System.out.println("12");
                        if (belief != null) {
                            syllogisms(tLink, bLink, taskTerm, beliefTerm, memory);
                        }
                        break;
                    case TermLink.COMPOUND_CONDITION:
                        //System.out.println("13");
                        if (belief != null) {
                            bIndex = bLink.getIndex(1);
                            if (beliefTerm instanceof Implication) {
                                conditionalDedIndWithVar((Implication) beliefTerm, bIndex, (Statement) taskTerm, tIndex, memory);
                            }
                        }
                        break;
                }
                break;
            case TermLink.COMPOUND_CONDITION:
                switch (bLink.getType()) {
                    case TermLink.COMPOUND:
                        //System.out.println("14");
                        if (belief != null) {
                            detachmentWithVar(taskSentence, belief, tIndex, memory);
                        }
                        break;
                    case TermLink.COMPOUND_STATEMENT:
                        //System.out.println("15");
                        if (belief != null) {
                            if (taskTerm instanceof Implication) // TODO maybe put instanceof test within conditionalDedIndWithVar()
                            {
                                Term subj = ((Implication) taskTerm).getSubject();
                                if (subj instanceof Negation) {
                                    if (task.getSentence().isJudgment()) {
                                        componentAndStatement((CompoundTerm) subj, bIndex, (Statement) taskTerm, tIndex, memory);
                                    } else {
                                        componentAndStatement((CompoundTerm) subj, tIndex, (Statement) beliefTerm, bIndex, memory);
                                    }
                                } else {
                                    conditionalDedIndWithVar((Implication) taskTerm, tIndex, (Statement) beliefTerm, bIndex, memory);
                                }
                            }
                            break;
                        }
                        break;
                }
        }
    }

    /* ----- syllogistic inferences ----- */
    /**
     * Meta-table of syllogistic rules, indexed by the content classes of the
     * taskSentence and the belief
     *
     * @param tLink The link to task
     * @param bLink The link to belief
     * @param taskTerm The content of task
     * @param beliefTerm The content of belief
     * @param memory Reference to the memory
     */
    private static void syllogisms(TaskLink tLink, TermLink bLink, Term taskTerm, Term beliefTerm, Memory memory) {
        Sentence taskSentence = memory.currentTask.getSentence();
        //System.out.println(taskSentence.getContent().getName());
        Sentence belief = memory.currentBelief;
        //System.out.println(belief.getContent().getName());
        int figure;
        if (taskTerm instanceof Inheritance) {
            if (beliefTerm instanceof Inheritance) {
                figure = indexToFigure(tLink, bLink);
                //System.out.println("16");
                asymmetricAsymmetric(taskSentence, belief, figure, memory);
            } else if (beliefTerm instanceof Similarity) {
                //System.out.println("17");
                figure = indexToFigure(tLink, bLink);
                asymmetricSymmetric(taskSentence, belief, figure, memory);
            } else {
                //System.out.println("18");
                detachmentWithVar(belief, taskSentence, bLink.getIndex(0), memory);
            }
        } else if (taskTerm instanceof Similarity) {
            if (beliefTerm instanceof Inheritance) {
                //System.out.println("19");
                figure = indexToFigure(bLink, tLink);
                asymmetricSymmetric(belief, taskSentence, figure, memory);
            } else if (beliefTerm instanceof Similarity) {
                //System.out.println("20");
                figure = indexToFigure(bLink, tLink);
                symmetricSymmetric(belief, taskSentence, figure, memory);
            }
        } else if (taskTerm instanceof Implication) {
            //System.out.println("21");
            if (beliefTerm instanceof Implication) {
                //System.out.println("22");
                figure = indexToFigure(tLink, bLink);
                asymmetricAsymmetric(taskSentence, belief, figure, memory);
            } else if (beliefTerm instanceof Equivalence) {
                //System.out.println("23");
                figure = indexToFigure(tLink, bLink);
                asymmetricSymmetric(taskSentence, belief, figure, memory);
            } else if (beliefTerm instanceof Inheritance) {
                //System.out.println("24");
                detachmentWithVar(taskSentence, belief, tLink.getIndex(0), memory);
            }
        } else if (taskTerm instanceof Equivalence) {
            if (beliefTerm instanceof Implication) {
                //System.out.println("25");
                figure = indexToFigure(bLink, tLink);
                asymmetricSymmetric(belief, taskSentence, figure, memory);
            } else if (beliefTerm instanceof Equivalence) {
                //System.out.println("26");
                figure = indexToFigure(bLink, tLink);
                symmetricSymmetric(belief, taskSentence, figure, memory);
            } else if (beliefTerm instanceof Inheritance) {
                //System.out.println("27");
                detachmentWithVar(taskSentence, belief, tLink.getIndex(0), memory);
            }
        }
    }

    /**
     * Decide the figure of syllogism according to the locations of the common
     * term in the premises
     *
     * @param link1 The link to the first premise
     * @param link2 The link to the second premise
     * @return The figure of the syllogism, one of the four: 11, 12, 21, or 22
     */
    private static int indexToFigure(TermLink link1, TermLink link2) {
        return (link1.getIndex(0) + 1) * 10 + (link2.getIndex(0) + 1);
    }

    /**
     * Syllogistic rules whose both premises are on the same asymmetric relation
     *
     * @param sentence The taskSentence in the task
     * @param belief The judgment in the belief
     * @param figure The location of the shared term
     * @param memory Reference to the memory
     */
    private static void asymmetricAsymmetric(Sentence sentence, Sentence belief, int figure, Memory memory) {
        
        if(!sentence.isEternal() || !belief.isEternal())
            return;
        
        Statement s1 = (Statement) sentence.cloneContent();
        Statement s2 = (Statement) belief.cloneContent();   
        
        /*System.out.println("s1: " + s1.getName());
        System.out.println("s2: " + s2.getName());*/
        
        Term t1, t2;
        switch (figure) {
            case 11:    // induction
                //System.out.println("28");
                if (Variable.unify(Symbols.VAR_INDEPENDENT, s1.getSubject(), s2.getSubject(), s1, s2)) {
                    
                    if (s1.equals(s2)) {
                        return;
                    }
                    t1 = s2.getPredicate();
                    t2 = s1.getPredicate();
                    CompositionalRules.composeCompound(s1, s2, 0, memory);
                    SyllogisticRules.abdIndCom(t1, t2, sentence, belief, figure, memory);
                }

                break;
            case 12:    // deduction
                //System.out.println("29");
                if (Variable.unify(Symbols.VAR_INDEPENDENT, s1.getSubject(), s2.getPredicate(), s1, s2)) {
                    
                    if (s1.equals(s2)) {
                        return;
                    }
                    t1 = s2.getSubject();
                    t2 = s1.getPredicate();
                    //System.out.println("30");
                    if (Variable.unify(Symbols.VAR_QUERY, t1, t2, s1, s2)) {
                        System.out.println("31");
                        LocalRules.matchReverse(memory);
                    } else {
                        //System.out.println("32");
                        SyllogisticRules.dedExe(t1, t2, sentence, belief, memory);
                    }
                }
                break;
            case 21:    // exemplification
                //System.out.println("33");
                if (Variable.unify(Symbols.VAR_INDEPENDENT, s1.getPredicate(), s2.getSubject(), s1, s2)) {
                    
                    if (s1.equals(s2)) {
                        return;
                    }
                    t1 = s1.getSubject();
                    t2 = s2.getPredicate();
                    //System.out.println("34");
                    if (Variable.unify(Symbols.VAR_QUERY, t1, t2, s1, s2)) {
                        LocalRules.matchReverse(memory);
                    } else {
                        SyllogisticRules.dedExe(t1, t2, sentence, belief, memory);
                    }
                }
                break;
            case 22:    // abduction
                //System.out.println("35");
                if (Variable.unify(Symbols.VAR_INDEPENDENT, s1.getPredicate(), s2.getPredicate(), s1, s2)) {
                    
                    if (s1.equals(s2)) {
                        return;
                    }
                    t1 = s1.getSubject();
                    t2 = s2.getSubject();
                    if (!SyllogisticRules.conditionalAbd(t1, t2, s1, s2, memory)) {         // if conditional abduction, skip the following
                        CompositionalRules.composeCompound(s1, s2, 1, memory);
                        SyllogisticRules.abdIndCom(t1, t2, sentence, belief, figure, memory);
                    }
                }
                break;
            default:
        }
    }

    /**
     * Syllogistic rules whose first premise is on an asymmetric relation, and
     * the second on a symmetric relation
     *
     * @param asym The asymmetric premise
     * @param sym The symmetric premise
     * @param figure The location of the shared term
     * @param memory Reference to the memory
     */
    private static void asymmetricSymmetric(Sentence asym, Sentence sym, int figure, Memory memory) {
        Statement asymSt = (Statement) asym.cloneContent();
        Statement symSt = (Statement) sym.cloneContent();
        Term t1, t2;
        switch (figure) {
            case 11:
                //System.out.println("36");
                if (Variable.unify(Symbols.VAR_INDEPENDENT, asymSt.getSubject(), symSt.getSubject(), asymSt, symSt)) {
                    
                    t1 = asymSt.getPredicate();
                    t2 = symSt.getPredicate();
                    //System.out.println("37");
                    if (Variable.unify(Symbols.VAR_QUERY, t1, t2, asymSt, symSt)) {
                        
                        LocalRules.matchAsymSym(asym, sym, figure, memory);
                    } else {
                        SyllogisticRules.analogy(t2, t1, asym, sym, figure, memory);
                    }
                }
                break;
            case 12:
                //System.out.println("38");
                if (Variable.unify(Symbols.VAR_INDEPENDENT, asymSt.getSubject(), symSt.getPredicate(), asymSt, symSt)) {
                    
                    t1 = asymSt.getPredicate();
                    t2 = symSt.getSubject();
                    //System.out.println("39");
                    if (Variable.unify(Symbols.VAR_QUERY, t1, t2, asymSt, symSt)) {
                        
                        LocalRules.matchAsymSym(asym, sym, figure, memory);
                    } else {
                        SyllogisticRules.analogy(t2, t1, asym, sym, figure, memory);
                    }
                }
                break;
            case 21:
                //System.out.println("40");
                if (Variable.unify(Symbols.VAR_INDEPENDENT, asymSt.getPredicate(), symSt.getSubject(), asymSt, symSt)) {
                    
                    t1 = asymSt.getSubject();
                    t2 = symSt.getPredicate();
                    //System.out.println("41");
                    if (Variable.unify(Symbols.VAR_QUERY, t1, t2, asymSt, symSt)) {
                        
                        LocalRules.matchAsymSym(asym, sym, figure, memory);
                    } else {
                        SyllogisticRules.analogy(t1, t2, asym, sym, figure, memory);
                    }
                }
                break;
            case 22:
                //System.out.println("42");
                if (Variable.unify(Symbols.VAR_INDEPENDENT, asymSt.getPredicate(), symSt.getPredicate(), asymSt, symSt)) {
                    
                    t1 = asymSt.getSubject();
                    t2 = symSt.getSubject();
                    //System.out.println("43");
                    if (Variable.unify(Symbols.VAR_QUERY, t1, t2, asymSt, symSt)) {
                        
                        LocalRules.matchAsymSym(asym, sym, figure, memory);
                    } else {
                        SyllogisticRules.analogy(t1, t2, asym, sym, figure, memory);
                    }
                }
                break;
        }
    }

    /**
     * Syllogistic rules whose both premises are on the same symmetric relation
     *
     * @param belief The premise that comes from a belief
     * @param taskSentence The premise that comes from a task
     * @param figure The location of the shared term
     * @param memory Reference to the memory
     */
    private static void symmetricSymmetric(Sentence belief, Sentence taskSentence, int figure, Memory memory) {
        Statement s1 = (Statement) belief.cloneContent();
        Statement s2 = (Statement) taskSentence.cloneContent();
        switch (figure) {
            case 11:
                //System.out.println("44");
                if (Variable.unify(Symbols.VAR_INDEPENDENT, s1.getSubject(), s2.getSubject(), s1, s2)) {
                    
                    SyllogisticRules.resemblance(s1.getPredicate(), s2.getPredicate(), belief, taskSentence, figure, memory);
                }
                break;
            case 12:
                //System.out.println("45");
                if (Variable.unify(Symbols.VAR_INDEPENDENT, s1.getSubject(), s2.getPredicate(), s1, s2)) {
                    
                    SyllogisticRules.resemblance(s1.getPredicate(), s2.getSubject(), belief, taskSentence, figure, memory);
                }
                break;
            case 21:
                //System.out.println("46");
                if (Variable.unify(Symbols.VAR_INDEPENDENT, s1.getPredicate(), s2.getSubject(), s1, s2)) {
                    
                    SyllogisticRules.resemblance(s1.getSubject(), s2.getPredicate(), belief, taskSentence, figure, memory);
                }
                break;
            case 22:
                //System.out.println("47");
                if (Variable.unify(Symbols.VAR_INDEPENDENT, s1.getPredicate(), s2.getPredicate(), s1, s2)) {
                    
                    SyllogisticRules.resemblance(s1.getSubject(), s2.getSubject(), belief, taskSentence, figure, memory);
                }
                break;
        }
    }

    /* ----- conditional inferences ----- */
    /**
     * The detachment rule, with variable unification
     *
     * @param originalMainSentence The premise that is an Implication or
     * Equivalence
     * @param subSentence The premise that is the subject or predicate of the
     * first one
     * @param index The location of the second premise in the first
     * @param memory Reference to the memory
     */
    private static void detachmentWithVar(Sentence originalMainSentence, Sentence subSentence, int index, Memory memory) {
        // 主句
        Sentence mainSentence = (Sentence) originalMainSentence.clone();   // for substitution
        // 主句的statement形式
        Statement statement = (Statement) mainSentence.getContent();
        Term component = statement.componentAt(index);
        Term content = subSentence.getContent();
        if (((component instanceof Inheritance) || (component instanceof Negation)) && (memory.currentBelief != null)) {
            if (component.isConstant()) {
                //System.out.println("48");
                SyllogisticRules.detachment(mainSentence, subSentence, index, memory, false);
            } else if (Variable.unify(Symbols.VAR_INDEPENDENT, component, content, statement, content)) {
                //System.out.println("49");
                SyllogisticRules.detachment(mainSentence, subSentence, index, memory, false);
            } else if ((statement instanceof Implication) && (statement.getPredicate() instanceof Statement) && (memory.currentTask.getSentence().isJudgment())) {
                //System.out.println("50");
                Statement s2 = (Statement) statement.getPredicate();
                if (s2.getSubject().equals(((Statement) content).getSubject())) {
                    CompositionalRules.introVarInner((Statement) content, s2, statement, memory);
                }
                CompositionalRules.IntroVarSameSubjectOrPredicate(originalMainSentence,subSentence,component,content,index,memory);
            } else if ((statement instanceof Equivalence) && (statement.getPredicate() instanceof Statement) && (memory.currentTask.getSentence().isJudgment())) {
                CompositionalRules.IntroVarSameSubjectOrPredicate(originalMainSentence,subSentence,component,content,index,memory);
            }
        }
    }

    /**
     * Conditional deduction or induction, with variable unification
     *
     * @param conditional The premise that is an Implication with a Conjunction
     * as condition
     * @param index The location of the shared term in the condition
     * @param statement The second premise that is a statement
     * @param side The location of the shared term in the statement
     * @param memory Reference to the memory
     */
    private static void conditionalDedIndWithVar(Implication conditional, short index, Statement statement, short side, Memory memory) {
        
        CompoundTerm condition = (CompoundTerm) conditional.getSubject();
        Term component = condition.componentAt(index);
        Term component2 = null;
        if (statement instanceof Inheritance) {
            //System.out.println("51");
            component2 = statement;
            side = -1;
        } else if (statement instanceof Implication) {
            //System.out.println("52");
            component2 = statement.componentAt(side);
        }
        if (component2 != null) {
            //System.out.println("53");
            boolean unifiable = Variable.unify(Symbols.VAR_INDEPENDENT, component, component2, conditional, statement);
            if (!unifiable) {
                //System.out.println("54");
                unifiable = Variable.unify(Symbols.VAR_DEPENDENT, component, component2, conditional, statement);
                
            }
            if (unifiable) {
                //System.out.println("55");
                SyllogisticRules.conditionalDedInd(conditional, index, statement, side, memory);
            }
        }
    }

    /* ----- structural inferences ----- */
    /**
     * Inference between a compound term and a component of it
     *
     * @param compound The compound term
     * @param component The component term
     * @param compoundTask Whether the compound comes from the task
     * @param memory Reference to the memory
     */
    private static void compoundAndSelf(CompoundTerm compound, Term component, boolean compoundTask, int index, Memory memory) {
        if ((compound instanceof Conjunction) || (compound instanceof Disjunction)) {
            //System.out.println("56");
            if (memory.currentBelief != null) {
                //System.out.println("99");
                /*if(compound.containComponent(component)){
                    System.out.println("99-1");
                    StructuralRules.structuralCompound(compound, component, compoundTask, index, memory);   
                }*/
                //System.out.println("99-2");
                CompositionalRules.decomposeStatement(compound, component, compoundTask, index, memory);
                
            } else if (compound.containComponent(component)) {
                //System.out.println("57");
                StructuralRules.structuralCompound(compound, component, compoundTask, index, memory);
            }
//        } else if ((compound instanceof Negation) && !memory.currentTask.isStructural()) {
        } else if (compound instanceof Negation) {
            if (compoundTask) {
                //System.out.println("58");
                StructuralRules.transformNegation(((Negation) compound).componentAt(0), memory);
            } else {
                //System.out.println("59");
                StructuralRules.transformNegation(compound, memory);
            }
        }
    }

    /**
     * Inference between two compound terms
     *
     * @param taskTerm The compound from the task
     * @param beliefTerm The compound from the belief
     * @param memory Reference to the memory
     */
    private static void compoundAndCompound(CompoundTerm taskTerm, CompoundTerm beliefTerm, int tIndex, int bIndex, Memory memory) {
        if (taskTerm.getClass() == beliefTerm.getClass()) {
            if (taskTerm.size() > beliefTerm.size()) {
                //System.out.println("60");
                compoundAndSelf(taskTerm, beliefTerm, true, tIndex, memory);
            } else if (taskTerm.size() < beliefTerm.size()) {
                //System.out.println("61");
                compoundAndSelf(beliefTerm, taskTerm, false, bIndex, memory);
            }
        }
    }

    /**
     * Inference between a compound term and a statement
     *
     * @param compound The compound term
     * @param index The location of the current term in the compound
     * @param statement The statement
     * @param side The location of the current term in the statement
     * @param beliefTerm The content of the belief
     * @param memory Reference to the memory
     */
    private static void compoundAndStatement(CompoundTerm compound, short index, Statement statement, short side, Term beliefTerm, Memory memory) {
        Term component = compound.componentAt(index);
        Task task = memory.currentTask;
        
        if (component.getClass() == statement.getClass()) {
            //System.out.println("61");
            if ((compound instanceof Conjunction) && (memory.currentBelief != null)) {
                //System.out.println("62");
                if (Variable.unify(Symbols.VAR_DEPENDENT, component, statement, compound, statement)) {
                    //System.out.println("63");
                    SyllogisticRules.elimiVarDep(compound, component, statement.equals(beliefTerm), index, memory);
                } else if (task.getSentence().isJudgment()) { // && !compound.containComponent(component)) {
                    CompositionalRules.introVarInner(statement, (Statement) component, compound, memory);
                    //System.out.println("64");
                } else if (Variable.unify(Symbols.VAR_QUERY, component, statement, compound, statement)) {
                    //System.out.println("65");
                    CompositionalRules.decomposeStatement(compound, component, true, index, memory);
                }
            }
        } else {
//            if (!task.isStructural() && task.getSentence().isJudgment()) {
            if (task.getSentence().isJudgment()) {
                //System.out.println("66");
                if (statement instanceof Inheritance) {
                    //System.out.println("67");                 
                    StructuralRules.structuralCompose1(compound, index, statement, memory);
//                    if (!(compound instanceof SetExt) && !(compound instanceof SetInt)) {
                    if (!(compound instanceof SetExt || compound instanceof SetInt || compound instanceof Negation)) {
                        //System.out.println("68");
                        StructuralRules.structuralCompose2(compound, index, statement, side, memory);
                    }    // {A --> B, A @ (A&C)} |- (A&C) --> (B&C)
                } else if ((statement instanceof Similarity) && !(compound instanceof Conjunction)) {
                    //System.out.println("69");
                    StructuralRules.structuralCompose2(compound, index, statement, side, memory);
                }       // {A <-> B, A @ (A&C)} |- (A&C) <-> (B&C)
            }
        }
    }

    /**
     * Inference between a component term (of the current term) and a statement
     *
     * @param compound The compound term
     * @param index The location of the current term in the compound
     * @param statement The statement
     * @param side The location of the current term in the statement
     * @param memory Reference to the memory
     */
    private static void componentAndStatement(CompoundTerm compound, short index, Statement statement, short side, Memory memory) {
//        if (!memory.currentTask.isStructural()) {
        if (statement instanceof Inheritance) {
            //System.out.println("70");
            StructuralRules.structuralDecompose1(compound, index, statement, memory);
            if (!(compound instanceof SetExt) && !(compound instanceof SetInt)) {
                //System.out.println("71");
                StructuralRules.structuralDecompose2(statement, index, memory);    // {(C-B) --> (C-A), A @ (C-A)} |- A --> B
            } else {
                //System.out.println("72");
                StructuralRules.transformSetRelation(compound, statement, side, memory);
            }
        } else if (statement instanceof Similarity) {
            StructuralRules.structuralDecompose2(statement, index, memory);        // {(C-B) --> (C-A), A @ (C-A)} |- A --> B
            if ((compound instanceof SetExt) || (compound instanceof SetInt)) {
                //System.out.println("73");
                StructuralRules.transformSetRelation(compound, statement, side, memory);
            }
        } else if ((statement instanceof Implication) && (compound instanceof Negation)) {
            if (index == 0) {
                //System.out.println("74");
                StructuralRules.contraposition(statement, memory.currentTask.getSentence(), memory);
            } else {
                //System.out.println("75");
                StructuralRules.contraposition(statement, memory.currentBelief, memory);
            }
        }
//        }
    }

    /* ----- inference with one TaskLink only ----- */
    /**
     * The TaskLink is of type TRANSFORM, and the conclusion is an equivalent
     * transformation
     *
     * @param tLink The task link
     * @param memory Reference to the memory
     */
    public static void transformTask(TaskLink tLink, Memory memory) {

        CompoundTerm content = (CompoundTerm) memory.currentTask.getContent();
        short[] indices = tLink.getIndices();
        Term inh = null;
        if ((indices.length == 2) || (content instanceof Inheritance)) {          // <(*, term, #) --> #>
            //System.out.println("75");
            inh = content;
        } else if (indices.length == 3) {   // <<(*, term, #) --> #> ==> #>
            //System.out.println("76");
            inh = content.componentAt(indices[0]);
        } else if (indices.length == 4) {   // <(&&, <(*, term, #) --> #>, #) ==> #>
            //System.out.println("77");
            Term component = content.componentAt(indices[0]);
            if ((component instanceof Conjunction) && (((content instanceof Implication) && (indices[0] == 0)) || (content instanceof Equivalence))) {
                //System.out.println("78");
                inh = ((CompoundTerm) component).componentAt(indices[1]);
            } else {
                //System.out.println("79");
                return;
            }
        }
        
        if (inh instanceof Inheritance) {
            //System.out.println("80");
            StructuralRules.transformProductImage((Inheritance) inh, content, indices, memory);
        }
    }
}
