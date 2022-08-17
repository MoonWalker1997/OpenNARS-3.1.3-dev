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

import java.util.*;

import nars.entity.*;
import nars.language.*;
import nars.main.Parameters;
import nars.storage.Memory;

/**
 * Compound term composition and decomposition rules, with two premises.
 * <p>
 * Forward inference only, except the last group (dependent variable
 * introduction) can also be used backward.
 */
public final class CompositionalRules {

    static void IntroVarSameSubjectOrPredicate(Sentence originalMainSentence, Sentence subSentence, Term component, Term content, int index,Memory memory) {
        Sentence cloned=(Sentence) originalMainSentence.clone();
        Term T1=cloned.getContent();
        if(!(T1 instanceof CompoundTerm) || !(content instanceof CompoundTerm)) {
            return;
        }
        CompoundTerm T=(CompoundTerm) T1;
        CompoundTerm T2=(CompoundTerm) content.clone();
        if((component instanceof Inheritance && content instanceof Inheritance) ||
           (component instanceof Similarity && content instanceof Similarity)) {           
            if(component.equals(content)) {
                return; //wouldnt make sense to create a conjunction here, would contain a statement twice
            }
            if(((Statement)component).getPredicate().equals(((Statement)content).getPredicate()) && !(((Statement)component).getPredicate() instanceof Variable)) {
                Variable V=new Variable("#depIndVar1");
                CompoundTerm zw=(CompoundTerm) T.getComponents().get(index).clone();
                zw=(CompoundTerm) CompoundTerm.setComponent(zw,1,V,memory);
                T2=(CompoundTerm) CompoundTerm.setComponent(T2,1,V,memory);
                if(zw == null || T2 == null || zw.equals(T2)) {
                    return;
                }
                Conjunction res=(Conjunction) Conjunction.make(zw, T2, TemporalRules.ORDER_NONE, memory);
                T=(CompoundTerm) CompoundTerm.setComponent(T, index, res, memory);
            }
            else 
            if(((Statement)component).getSubject().equals(((Statement)content).getSubject()) && !(((Statement)component).getSubject() instanceof Variable)) {
                Variable V=new Variable("#depIndVar2");
                CompoundTerm zw=(CompoundTerm) T.getComponents().get(index).clone();
                zw=(CompoundTerm) CompoundTerm.setComponent(zw,0,V,memory);
                T2=(CompoundTerm) CompoundTerm.setComponent(T2,0,V,memory);
                if(zw == null || T2 == null || zw.equals(T2)) {
                    return;
                }
                Conjunction res=(Conjunction) Conjunction.make(zw, T2, TemporalRules.ORDER_NONE, memory);
                T=(CompoundTerm) CompoundTerm.setComponent(T, index, res, memory);
            }
            TruthValue truth = TruthFunctions.induction(originalMainSentence.getTruth(), subSentence.getTruth());
            BudgetValue budget = BudgetFunctions.compoundForward(truth, T, memory);
            memory.doublePremiseTask(T, truth, budget);
        }
    }
    
    /* -------------------- intersections and differences -------------------- */
    /**
     * {<S ==> M>, <P ==> M>} |- {<(S|P) ==> M>, <(S&P) ==> M>, <(S-P) ==> M>,
     * <(P-S) ==> M>}
     *
     * @param taskSentence The first premise
     * @param belief The second premise
     * @param index The location of the shared term
     * @param memory Reference to the memory
     */
    static void composeCompound(Statement taskContent, Statement beliefContent, int index, Memory memory) {
        
        // 如果当前任务的语句不是judgment 或者 任务与信念不是同一种语句类型，则返回
        if(!memory.currentTask.getSentence().isJudgment() || taskContent.getClass() != beliefContent.getClass())
            return;
        // 任务时序
        int taskOrder = taskContent.getTemporalOrder();
        // 信念时序
        int beliefOrder = beliefContent.getTemporalOrder();
        // 根据信念时序与任务时序得到
        int order = TemporalRules.composeOrder(taskOrder, beliefOrder);
        // 如果结果时序不合法，则直接返回
        if(order == TemporalRules.ORDER_INVALID)
            return;
        int timeDiff = 0;
        
        if(order != TemporalRules.ORDER_NONE)
            timeDiff = Math.abs((int)(memory.currentTask.getSentence().getOccurrenceTime() - memory.currentBelief.getOccurrenceTime()));
        
        // index为公共项的位置，0就是都在谓语，1就是都在主语
        // 1 - index 就是非公共项的位置
        Term componentT = taskContent.componentAt(1 - index);
        Term componentB = beliefContent.componentAt(1 - index);
        Term componentCommon = taskContent.componentAt(index);
        
        // 如果任务是一个复合词项，并且任务包含信念中所有的词项
        // 先将符合词项中包含信念的词项给删除掉，做decompose
        if ((componentT instanceof CompoundTerm) && ((CompoundTerm) componentT).containAllComponents(componentB)) {
            decomposeCompound((CompoundTerm) componentT, componentB, componentCommon, index, true, order, memory);
            return;
        // 信念同理
        } else if ((componentB instanceof CompoundTerm) && ((CompoundTerm) componentB).containAllComponents(componentT)) {
            decomposeCompound((CompoundTerm) componentB, componentT, componentCommon, index, false, order, memory);
            return;
        }
        
        TruthValue truthT = memory.currentTask.getSentence().getTruth();
        TruthValue truthB = memory.currentBelief.getTruth();
        TruthValue truthOr = TruthFunctions.union(truthT, truthB);
        TruthValue truthAnd = TruthFunctions.intersection(truthT, truthB);
        TruthValue truthDif = null;
        Term termOr = null;
        Term termAnd = null;
        Term termDif = null;
        if (index == 0) {
            if (taskContent instanceof Inheritance) {
                termOr = IntersectionInt.make(componentT, componentB, memory);
                termAnd = IntersectionExt.make(componentT, componentB, memory);
                if (truthB.isNegative()) {
                    if (!truthT.isNegative()) {
                        termDif = DifferenceExt.make(componentT, componentB, memory);
                        truthDif = TruthFunctions.intersection(truthT, TruthFunctions.negation(truthB));
                    }
                } else if (truthT.isNegative()) {
                    termDif = DifferenceExt.make(componentB, componentT, memory);
                    truthDif = TruthFunctions.intersection(truthB, TruthFunctions.negation(truthT));
                }
            } else if (taskContent instanceof Implication) {
                termOr = Disjunction.make(componentT, componentB, memory);
                
                if(order != TemporalRules.ORDER_NONE){
                    if(memory.currentTask.getSentence().getOccurrenceTime() > memory.currentBelief.getOccurrenceTime())
                        termAnd = Conjunction.make(componentB, componentT, TemporalRules.ORDER_FORWARD, memory);
                    else if(memory.currentTask.getSentence().getOccurrenceTime() < memory.currentBelief.getOccurrenceTime())
                        termAnd = Conjunction.make(componentT, componentB, TemporalRules.ORDER_FORWARD, memory);
                }else{
                    termAnd = Conjunction.make(componentT, componentB, order, memory);
                }
                    
            }
            
            if(!(componentT.equals(componentB))){
                processComposed(taskContent, (Term) componentCommon.clone(), termOr, truthOr, order, memory);
                processComposed(taskContent, (Term) componentCommon.clone(), termAnd, truthAnd, order, memory);
            }
            processComposed(taskContent, (Term) componentCommon.clone(), termDif, truthDif, order, memory);
        } else {    // index == 1
            if (taskContent instanceof Inheritance) {
                termOr = IntersectionExt.make(componentT, componentB, memory);
                termAnd = IntersectionInt.make(componentT, componentB, memory);
                if (truthB.isNegative()) {
                    if (!truthT.isNegative()) {
                        termDif = DifferenceInt.make(componentT, componentB, memory);
                        truthDif = TruthFunctions.intersection(truthT, TruthFunctions.negation(truthB));
                    }
                } else if (truthT.isNegative()) {
                    termDif = DifferenceInt.make(componentB, componentT, memory);
                    truthDif = TruthFunctions.intersection(truthB, TruthFunctions.negation(truthT));
                }
            } else if (taskContent instanceof Implication) {
                if(order != TemporalRules.ORDER_NONE){
                    if(memory.currentTask.getSentence().getOccurrenceTime() > memory.currentBelief.getOccurrenceTime())
                        termOr = Conjunction.make(componentB, componentT, TemporalRules.ORDER_FORWARD, memory);
                    else if(memory.currentTask.getSentence().getOccurrenceTime() < memory.currentBelief.getOccurrenceTime())
                        termOr = Conjunction.make(componentT, componentB, TemporalRules.ORDER_FORWARD, memory);
                }else{
                    termOr = Conjunction.make(componentT, componentB, order, memory);
                }
                termAnd = Disjunction.make(componentT, componentB, memory);
            }
            
            if(!(componentT.equals(componentB))){
                processComposed(taskContent, termOr, (Term) componentCommon.clone(), truthOr, order, memory);
                processComposed(taskContent, termAnd, (Term) componentCommon.clone(), truthAnd, order, memory);
            }
            processComposed(taskContent, termDif, (Term) componentCommon.clone(), truthDif, order, memory);
        }
        if (taskContent instanceof Inheritance) {
            introVarOuter(taskContent, beliefContent, index, order, memory);//            introVarImage(taskContent, beliefContent, index, memory);
        }
    }

    /**
     * Finish composing implication term
     *
     * @param premise1 Type of the contentInd
     * @param subject Subject of contentInd
     * @param predicate Predicate of contentInd
     * @param truth TruthValue of the contentInd
     * @param memory Reference to the memory
     */
    private static void processComposed(Statement statement, Term subject, Term predicate, TruthValue truth, int temporalOrder, Memory memory) {
        if ((subject == null) || (predicate == null)) {
            return;
        }
        Term content = Statement.make(statement, subject, predicate, temporalOrder, memory);
        if ((content == null) || content.equals(statement) || content.equals(memory.currentBelief.getContent())) {
            return;
        }
        BudgetValue budget = BudgetFunctions.compoundForward(truth, content, memory);
        memory.doublePremiseTask(content, truth, budget);
    }

    /**
     * {<(S|P) ==> M>, <P ==> M>} |- <S ==> M>
     * 拆解复合词项
     * 
     * @param implication The implication term to be decomposed
     * @param componentCommon The part of the implication to be removed
     * @param term1 The other term in the contentInd
     * @param index The location of the shared term: 0 for subject, 1 for
     * predicate
     * @param compoundTask Whether the implication comes from the task
     * @param memory Reference to the memory
     */
    private static void decomposeCompound(CompoundTerm compound, Term component, Term term1, int index, boolean compoundTask, int temporalOrder, Memory memory) {
        // 如果复合词项是一个statement，或者复合词项为一个ImageExt，或者是一个ImageInt，则返回
        if ((compound instanceof Statement) || (compound instanceof ImageExt) || (compound instanceof ImageInt)) {
            return;
        }
        // 将component从compound中移除之后生成的新复合词项(S|P), P ==> S
        Term term2 = CompoundTerm.reduceComponents(compound, component, memory);
        if (term2 == null) {
            return;
        }
        
        // 假如compound带有时间间隔，并且早发生的事件被消除
        // 将余下的复合词项中的时间间隔删除，并且提取出时间间隔
         
        // 当前任务
        Task task = memory.currentTask;
        // 任务语句
        Sentence sentence = task.getSentence();
        // 当前信念
        Sentence belief = memory.currentBelief;
        // 任务的词项
        Statement oldContent = (Statement) task.getContent();
        
        // 如果compound是任务，v1就等于任务的真值，v2为信念真值
        // 反之就换顺序
        TruthValue v1,v2;
        if (compoundTask) {
            v1 = sentence.getTruth();
            v2 = belief.getTruth();
        } else {
            v1 = belief.getTruth();
            v2 = sentence.getTruth();
        }
        TruthValue truth = null;
        Term content;
        
        if (index == 0) {
            content = Statement.make(oldContent, term1, term2, temporalOrder, memory);
            if (content == null) {
                return;
            }
            if (oldContent instanceof Inheritance) {
                if (compound instanceof IntersectionExt) {
                    truth = TruthFunctions.reduceConjunction(v1, v2);
                } else if (compound instanceof IntersectionInt) {
                    truth = TruthFunctions.reduceDisjunction(v1, v2);
                } else if ((compound instanceof SetInt) && (component instanceof SetInt)) {
                    truth = TruthFunctions.reduceConjunction(v1, v2);
                } else if ((compound instanceof SetExt) && (component instanceof SetExt)) {
                    truth = TruthFunctions.reduceDisjunction(v1, v2);
                } else if (compound instanceof DifferenceExt) {
                    if (compound.componentAt(0).equals(component)) {
                        truth = TruthFunctions.reduceDisjunction(v2, v1);
                    } else {
                        truth = TruthFunctions.reduceConjunctionNeg(v1, v2);
                    }
                }
            } else if (oldContent instanceof Implication) {
                if (compound instanceof Conjunction) {
                    truth = TruthFunctions.reduceConjunction(v1, v2);
                } else if (compound instanceof Disjunction) {
                    truth = TruthFunctions.reduceDisjunction(v1, v2);
                }
            }
        } else {
            content = Statement.make(oldContent, term2, term1, temporalOrder, memory);
            
            if (content == null) {
                return;
            }
            if (oldContent instanceof Inheritance) {
                if (compound instanceof IntersectionInt) {
                    truth = TruthFunctions.reduceConjunction(v1, v2);
                } else if (compound instanceof IntersectionExt) {
                    truth = TruthFunctions.reduceDisjunction(v1, v2);
                } else if ((compound instanceof SetExt) && (component instanceof SetExt)) {
                    truth = TruthFunctions.reduceConjunction(v1, v2);
                } else if ((compound instanceof SetInt) && (component instanceof SetInt)) {
                    truth = TruthFunctions.reduceDisjunction(v1, v2);
                } else if (compound instanceof DifferenceInt) {
                    if (compound.componentAt(1).equals(component)) {
                        truth = TruthFunctions.reduceDisjunction(v2, v1);
                    } else {
                        truth = TruthFunctions.reduceConjunctionNeg(v1, v2);
                    }
                }
            } else if (oldContent instanceof Implication) {
                if (compound instanceof Disjunction) {
                    truth = TruthFunctions.reduceConjunction(v1, v2);
                } else if (compound instanceof Conjunction) {
                    truth = TruthFunctions.reduceDisjunction(v1, v2);
                }
            }
        }
        if (truth != null) {
            BudgetValue budget = BudgetFunctions.compoundForward(truth, content, memory);            
            memory.doublePremiseTask(content, truth, budget);
        }
    }
    
    /**
     * {(||, S, P), P} |- S {(&&, S, P), P} |- S
     *
     * @param implication The implication term to be decomposed
     * @param componentCommon The part of the implication to be removed
     * @param compoundTask Whether the implication comes from the task
     * @param memory Reference to the memory
     */
    static void decomposeStatement(CompoundTerm compound, Term component, boolean compoundTask, int index, Memory memory) {
        
        if(compound instanceof Conjunction && compound.getTemporalOrder() == TemporalRules.ORDER_FORWARD){
            //System.out.println("???");
            return;
        }
        
        long occurrenceTime = memory.currentTask.getStamp().getOccurrenceTime();
        
        Task task = memory.currentTask;
        Sentence sentence = task.getSentence();
        Sentence belief = memory.currentBelief;
        Term content = CompoundTerm.reduceComponents(compound, component, memory);
        if (content == null) {
            return;
        }
        TruthValue truth = null;
        BudgetValue budget;
        if (sentence.isQuestion() || sentence.isQuest()) {
            budget = BudgetFunctions.compoundBackward(content, memory);
            memory.getNewStamp().setOccurrenceTime(occurrenceTime);
            memory.doublePremiseTask(content, truth, budget);
            // special inference to answer conjunctive questions with query variables
            if (Variable.containVarQuery(sentence.getContent().getName())) {
                Concept contentConcept = memory.termToConcept(content);
                if (contentConcept == null) {
                    return;
                }
                Sentence contentBelief = contentConcept.getBelief(task);
                if (contentBelief == null) {
                    return;
                }
                Task contentTask = new Task(contentBelief, task.getBudget());
                memory.currentTask = contentTask;
                Term conj = Conjunction.make(component, content, TemporalRules.ORDER_NONE, memory);
                truth = TruthFunctions.intersection(contentBelief.getTruth(), belief.getTruth());
                budget = BudgetFunctions.compoundForward(truth, conj, memory);
                //memory.getNewStamp().setOccurrenceTime(occurrenceTime);
                memory.doublePremiseTask(conj, truth, budget);
            }
        } else {
            TruthValue v1, v2;
            if (compoundTask) {
                v1 = sentence.getTruth();
                v2 = belief.getTruth();
            } else {
                v1 = belief.getTruth();
                v2 = sentence.getTruth();
            }
            
            if(compound instanceof Conjunction || compound instanceof Disjunction){
                if(sentence.isGoal() && !compoundTask)
                    return;
            }else{
                return;
            }
            
            if (compound instanceof Conjunction) {
                if(sentence.isGoal())
                    truth = TruthFunctions.intersection(v1, v2);
                else
                    truth = TruthFunctions.reduceConjunction(v1, v2);
            } else if (compound instanceof Disjunction) {
                if(sentence.isGoal())
                    truth = TruthFunctions.reduceConjunction(v2, v1);
                else
                    truth = TruthFunctions.reduceDisjunction(v1, v2);
            } else {
                return;
            }        
            
            budget = BudgetFunctions.compoundForward(truth, content, memory);
            //memory.getNewStamp().setOccurrenceTime(occurrenceTime);
            memory.doublePremiseTask(content, truth, budget);
        }
    }

    /* --------------- rules used for variable introduction --------------- */
    /**
     * Introduce a dependent variable in an outer-layer conjunction
     *
     * @param taskContent The first premise <M --> S>
     * @param beliefContent The second premise <M --> P>
     * @param index The location of the shared term: 0 for subject, 1 for
     * predicate
     * @param memory Reference to the memory
     */
    private static void introVarOuter(Statement taskContent, Statement beliefContent, int index, int temporalOrder, Memory memory) {
        TruthValue truthT = memory.currentTask.getSentence().getTruth();
        TruthValue truthB = memory.currentBelief.getTruth();
        Variable varInd = new Variable("$varInd1");
        Variable varInd2 = new Variable("$varInd2");
        Term term11, term12, term21, term22, commonTerm;
        HashMap<Term, Term> subs = new HashMap<>();
        if (index == 0) {
            term11 = varInd;
            term21 = varInd;
            term12 = taskContent.getPredicate();
            term22 = beliefContent.getPredicate();
            if ((term12 instanceof ImageExt) && (term22 instanceof ImageExt)) {
                commonTerm = ((ImageExt) term12).getTheOtherComponent();
                if ((commonTerm == null) || !((ImageExt) term22).containTerm(commonTerm)) {
                    commonTerm = ((ImageExt) term22).getTheOtherComponent();
                    if ((commonTerm == null) || !((ImageExt) term12).containTerm(commonTerm)) {
                        commonTerm = null;
                    }
                }
                if (commonTerm != null) {
                    subs.put(commonTerm, varInd2);
                    ((ImageExt) term12).applySubstitute(subs);
                    ((ImageExt) term22).applySubstitute(subs);
                }
            }
        } else {
            term11 = taskContent.getSubject();
            term21 = beliefContent.getSubject();
            term12 = varInd;
            term22 = varInd;
            if ((term11 instanceof ImageInt) && (term21 instanceof ImageInt)) {
                commonTerm = ((ImageInt) term11).getTheOtherComponent();
                if ((commonTerm == null) || !((ImageInt) term21).containTerm(commonTerm)) {
                    commonTerm = ((ImageInt) term21).getTheOtherComponent();
                    if ((commonTerm == null) || !((ImageInt) term11).containTerm(commonTerm)) {
                        commonTerm = null;
                    }
                }
                if (commonTerm != null) {
                    subs.put(commonTerm, varInd2);
                    ((ImageInt) term11).applySubstitute(subs);
                    ((ImageInt) term21).applySubstitute(subs);
                }
            }
        }
        Statement state1 = Inheritance.make(term11, term12, memory);
        Statement state2 = Inheritance.make(term21, term22, memory);
        Term content = null;
        if(temporalOrder == TemporalRules.ORDER_FORWARD || temporalOrder == TemporalRules.ORDER_BACKWARD)
            content = Implication.make(state1, state2, temporalOrder, Parameters.DEFAULT_TIME_INTERVAL, memory);
        else
            content = Implication.make(state1, state2, temporalOrder, Parameters.DEFAULT_TIME_INTERVAL, memory);
        
        if (content == null) {
            return;
        }
        TruthValue truth = TruthFunctions.induction(truthT, truthB);
        BudgetValue budget = BudgetFunctions.compoundForward(truth, content, memory);
        memory.doublePremiseTask(content, truth, budget);
        if(temporalOrder == TemporalRules.ORDER_FORWARD || temporalOrder == TemporalRules.ORDER_BACKWARD)
            content = Implication.make(state1, state2, temporalOrder, Parameters.DEFAULT_TIME_INTERVAL, memory);
        else
            content = Implication.make(state1, state2, temporalOrder, Parameters.DEFAULT_TIME_INTERVAL, memory);
        truth = TruthFunctions.induction(truthB, truthT);
        budget = BudgetFunctions.compoundForward(truth, content, memory);
        memory.doublePremiseTask(content, truth, budget);
        content = Equivalence.make(state1, state2, temporalOrder, memory, Parameters.DEFAULT_TIME_INTERVAL);
        truth = TruthFunctions.comparison(truthT, truthB);
        budget = BudgetFunctions.compoundForward(truth, content, memory);
        memory.doublePremiseTask(content, truth, budget);
        Variable varDep = new Variable("#varDep");
        if (index == 0) {
            state1 = Inheritance.make(varDep, taskContent.getPredicate(), memory);
            state2 = Inheritance.make(varDep, beliefContent.getPredicate(), memory);
        } else {
            state1 = Inheritance.make(taskContent.getSubject(), varDep, memory);
            state2 = Inheritance.make(beliefContent.getSubject(), varDep, memory);
        }
        content = Conjunction.make(state1, state2, TemporalRules.ORDER_NONE, memory);
        truth = TruthFunctions.intersection(truthT, truthB);
        budget = BudgetFunctions.compoundForward(truth, content, memory);
        memory.doublePremiseTask(content, truth, budget, false);
    }

    /**
     * {<M --> S>, <C ==> <M --> P>>} |- <(&&, <#x --> S>, C) ==> <#x --> P>>
     * {<M --> S>, (&&, C, <M --> P>)} |- (&&, C, <<#x --> S> ==> <#x --> P>>)
     *
     * @param taskContent The first premise directly used in internal induction,
     * <M --> S>
     * @param beliefContent The componentCommon to be used as a premise in
     * internal induction, <M --> P>
     * @param oldCompound The whole contentInd of the first premise, Implication
     * or Conjunction
     * @param memory Reference to the memory
     */
    static void introVarInner(Statement premise1, Statement premise2, CompoundTerm oldCompound, Memory memory) {
        
        if(oldCompound instanceof Conjunction && ((Conjunction)oldCompound).getTemporalOrder() != 2)
            return;
           
        
        Task task = memory.currentTask;
        Sentence taskSentence = task.getSentence();
        if (!taskSentence.isJudgment() || (premise1.getClass() != premise2.getClass()) || oldCompound.containComponent(premise1)) {
            return;
        }
        Term subject1 = premise1.getSubject();
        Term subject2 = premise2.getSubject();
        Term predicate1 = premise1.getPredicate();
        Term predicate2 = premise2.getPredicate();
        Term commonTerm1, commonTerm2;
        if (subject1.equals(subject2)) {
            commonTerm1 = subject1;
            commonTerm2 = secondCommonTerm(predicate1, predicate2, 0);
        } else if (predicate1.equals(predicate2)) {
            commonTerm1 = predicate1;
            commonTerm2 = secondCommonTerm(subject1, subject2, 0);
        } else {
            return;
        }
        Sentence belief = memory.currentBelief;
        HashMap<Term, Term> substitute = new HashMap<>();
        substitute.put(commonTerm1, new Variable("#varDep2"));
        CompoundTerm content = (CompoundTerm) Conjunction.make(premise1, oldCompound, TemporalRules.ORDER_NONE, memory);
        content.applySubstitute(substitute);
        TruthValue truth = TruthFunctions.intersection(taskSentence.getTruth(), belief.getTruth());
        BudgetValue budget = BudgetFunctions.forward(truth, memory);
        memory.doublePremiseTask(content, truth, budget, false);
        substitute.clear();
        substitute.put(commonTerm1, new Variable("$varInd1"));
        if (commonTerm2 != null) {
            substitute.put(commonTerm2, new Variable("$varInd2"));
        }
        content = Implication.make(premise1, oldCompound, TemporalRules.ORDER_NONE, 0, memory);
        if (content == null) {
            return;
        }
        content.applySubstitute(substitute);
        if (premise1.equals(taskSentence.getContent())) {
            truth = TruthFunctions.induction(belief.getTruth(), taskSentence.getTruth());
        } else {
            truth = TruthFunctions.induction(taskSentence.getTruth(), belief.getTruth());
        }
        budget = BudgetFunctions.forward(truth, memory);
        memory.doublePremiseTask(content, truth, budget);
    }

    /**
     * Introduce a second independent variable into two terms with a common
     * component
     *
     * @param term1 The first term
     * @param term2 The second term
     * @param index The index of the terms in their statement
     */
    private static Term secondCommonTerm(Term term1, Term term2, int index) {
        Term commonTerm = null;
        if (index == 0) {
            if ((term1 instanceof ImageExt) && (term2 instanceof ImageExt)) {
                commonTerm = ((ImageExt) term1).getTheOtherComponent();
                if ((commonTerm == null) || !((ImageExt) term2).containTerm(commonTerm)) {
                    commonTerm = ((ImageExt) term2).getTheOtherComponent();
                    if ((commonTerm == null) || !((ImageExt) term1).containTerm(commonTerm)) {
                        commonTerm = null;
                    }
                }
            }
        } else {
            if ((term1 instanceof ImageInt) && (term2 instanceof ImageInt)) {
                commonTerm = ((ImageInt) term1).getTheOtherComponent();
                if ((commonTerm == null) || !((ImageInt) term2).containTerm(commonTerm)) {
                    commonTerm = ((ImageInt) term2).getTheOtherComponent();
                    if ((commonTerm == null) || !((ImageExt) term1).containTerm(commonTerm)) {
                        commonTerm = null;
                    }
                }
            }
        }
        return commonTerm;
    }
}
