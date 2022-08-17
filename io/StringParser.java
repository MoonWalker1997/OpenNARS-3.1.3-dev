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
package nars.io;

import java.util.*;

//import deductions.runtime.swing.TemporaryFrame;

import nars.entity.*;
import nars.inference.*;
import static nars.io.Symbols.ARGUMENT_SEPARATOR;
import static nars.io.Symbols.BUDGET_VALUE_MARK;
import static nars.io.Symbols.COMPOUND_TERM_CLOSER;
import static nars.io.Symbols.COMPOUND_TERM_OPENER;
import static nars.io.Symbols.GOAL_MARK;
import static nars.io.Symbols.INPUT_LINE;
import static nars.io.Symbols.JUDGMENT_MARK;
import static nars.io.Symbols.OUTPUT_LINE;
import static nars.io.Symbols.PREFIX_MARK;
import static nars.io.Symbols.QUESTION_MARK;
import static nars.io.Symbols.QUEST_MARK;
import static nars.io.Symbols.SET_EXT_CLOSER;
import static nars.io.Symbols.SET_EXT_OPENER;
import static nars.io.Symbols.SET_INT_CLOSER;
import static nars.io.Symbols.SET_INT_OPENER;
import static nars.io.Symbols.STAMP_CLOSER;
import static nars.io.Symbols.STAMP_OPENER;
import static nars.io.Symbols.STATEMENT_CLOSER;
import static nars.io.Symbols.STATEMENT_OPENER;
import static nars.io.Symbols.TRUTH_VALUE_MARK;
import static nars.io.Symbols.VALUE_SEPARATOR;
import nars.language.*;
import nars.main.Parameters;
import nars.storage.Memory;

/**
 * Parse input String into Task or Term. Abstract class with static methods
 * only.
 */
public abstract class StringParser extends Symbols {

    /**
     * All kinds of invalid input lines
     */
    private static class InvalidInputException extends Exception {

        /**
         * An invalid input line.
         *
         * @param s type of error
         */
        InvalidInputException(String s) {
            super(s);
        }
    }

    /**
     * Parse a line of input experience
     * <p>
     * called from ExperienceIO.loadLine
     *
     * @param buffer The line to be parsed
     * @param memory Reference to the memory
     * @param time The current time
     * @return An experienced task
     */
    public static Task parseExperience(StringBuffer buffer, Memory memory, long time) {
        int i = buffer.indexOf(PREFIX_MARK + "");
        if (i > 0) {
            String prefix = buffer.substring(0, i).trim();
            switch (prefix) {
                case OUTPUT_LINE:
                    return null;
                case INPUT_LINE:
                    buffer.delete(0, i + 1);
                    break;
            }
        }
        char c = buffer.charAt(buffer.length() - 1);
        if (c == STAMP_CLOSER) {
            int j = buffer.lastIndexOf(STAMP_OPENER + "");
            buffer.delete(j - 1, buffer.length());
        }
        //System.out.println(buffer.toString());
        return parseTask(buffer.toString().trim(), memory, time);
    }

    /**
     * Enter a new Task in String into the memory, called from InputWindow or
     * locally.
     *
     * @param s the single-line input String
     * @param memory Reference to the memory
     * @param time The current time
     * @return An experienced task
     */
    public static Task parseTask(String s, Memory memory, long time) {
        StringBuffer buffer = new StringBuffer(s);
        Task task = null;
        try {
            String budgetString = getBudgetString(buffer);
            String truthString = getTruthString(buffer);
            Tense tense = parseTense(buffer);
            String str = buffer.toString().trim();

            int last = str.length() - 1;
            char punc = str.charAt(last);
            Stamp stamp = new Stamp(tense, time);   
  
            TruthValue truth = parseTruth(truthString, punc);

            if(tense != null && truth != null){
                truth.setEternal(false);
            }
                        
            Term content = parseTerm(str.substring(0, last), memory);
            Sentence sentence = new Sentence(content, punc, truth, stamp);
            if ((content instanceof Conjunction) && Variable.containVarDep(content.getName())) {
                sentence.setRevisible(false);
            }
            //System.out.println("sentence order: " + sentence.getTemporalOrder());
            boolean isEvent = (tense != null);
            BudgetValue budget = parseBudget(budgetString, punc, truth, isEvent);
            task = new Task(sentence, budget);
            //System.out.println(task.getSentence().getContent().getName());
            //System.out.println(task.getSentence().getTemporalOrder());
        } catch (InvalidInputException e) {
            String message = " !!! INVALID INPUT: parseTask: " + buffer + " --- " + e.getMessage();
            System.out.println(message);
//            showWarning(message);
        }
        return task;
    }

    /* ---------- parse values ---------- */
    /**
     * Return the prefix of a task string that contains a BudgetValue
     *
     * @param s the input in a StringBuffer
     * @return a String containing a BudgetValue
     * @throws nars.io.StringParser.InvalidInputException if the input cannot be
     * parsed into a BudgetValue
     */
    private static String getBudgetString(StringBuffer s) throws InvalidInputException {
        if (s.charAt(0) != BUDGET_VALUE_MARK) {
            return null;
        }
        int i = s.indexOf(BUDGET_VALUE_MARK + "", 1);    // looking for the end
        if (i < 0) {
            throw new InvalidInputException("missing budget closer");
        }
        String budgetString = s.substring(1, i).trim();
        if (budgetString.length() == 0) {
            throw new InvalidInputException("empty budget");
        }
        s.delete(0, i + 1);
        return budgetString;
    }

    /**
     * Return the postfix of a task string that contains a TruthValue
     *
     * @return a String containing a TruthValue
     * @param s the input in a StringBuffer
     * @throws nars.io.StringParser.InvalidInputException if the input cannot be
     * parsed into a TruthValue
     */
    private static String getTruthString(StringBuffer s) throws InvalidInputException {
        int last = s.length() - 1;
        if (s.charAt(last) != TRUTH_VALUE_MARK) {       // use default
            return null;
        }
        int first = s.indexOf(TRUTH_VALUE_MARK + "");    // looking for the beginning
        if (first == last) { // no matching closer
            throw new InvalidInputException("missing truth mark");
        }
        String truthString = s.substring(first + 1, last).trim();
        if (truthString.length() == 0) {                // empty usage
            throw new InvalidInputException("empty truth");
        }
        s.delete(first, last + 1);                 // remaining input to be processed outside
        s.trimToSize();
        return truthString;
    }

    /**
     * parse the input String into a TruthValue (or DesireValue)
     *
     * @param s input String
     * @param type Task type
     * @return the input TruthValue
     */
    private static TruthValue parseTruth(String s, char type) {
        if (type == QUESTION_MARK) {
            return null;
        }
        float frequency = 1.0f;
        float confidence = Parameters.DEFAULT_JUDGMENT_CONFIDENCE;
        if (s != null) {
            int i = s.indexOf(VALUE_SEPARATOR);
            if (i < 0) {
                frequency = Float.parseFloat(s);
            } else {
                frequency = Float.parseFloat(s.substring(0, i));
                confidence = Float.parseFloat(s.substring(i + 1));
            }
        }
        return new TruthValue(frequency, confidence);
    }

    /**
     * parse the input String into a BudgetValue
     *
     * @param truth the TruthValue of the task
     * @param s input String
     * @param punctuation Task punctuation
     * @return the input BudgetValue
     * @throws nars.io.StringParser.InvalidInputException If the String cannot
     * be parsed into a BudgetValue
     */
    private static BudgetValue parseBudget(String s, char punctuation, TruthValue truth, boolean isEvent) throws InvalidInputException {
        float priority, durability;
        switch (punctuation) {
            case JUDGMENT_MARK:
                
                if(isEvent){
                    priority = Parameters.DEFAULT_EVENT_PRIORITY;
                    durability = Parameters.DEFAULT_EVENT_DURABILITY;
                }else{
                    priority = Parameters.DEFAULT_JUDGMENT_PRIORITY;
                    durability = Parameters.DEFAULT_JUDGMENT_DURABILITY;
                }
                break;
            case QUESTION_MARK:
                priority = Parameters.DEFAULT_QUESTION_PRIORITY;
                durability = Parameters.DEFAULT_QUESTION_DURABILITY;
                break;
            case GOAL_MARK:
                priority = Parameters.DEFAULT_GOAL_PRIORITY;
                durability = Parameters.DEFAULT_GOAL_DURABILITY;
                break;
            case QUEST_MARK:
                priority = Parameters.DEFAULT_QUEST_PRIORITY;
                durability = Parameters.DEFAULT_QUEST_DURABILITY;
                break;
            default:
                throw new InvalidInputException("unknown punctuation: '" + punctuation + "'");
        }
        if (s != null) { // overrite default
            int i = s.indexOf(VALUE_SEPARATOR);
            if (i < 0) {        // default durability
                priority = Float.parseFloat(s);
            } else {
                priority = Float.parseFloat(s.substring(0, i));
                durability = Float.parseFloat(s.substring(i + 1));
            }
        }
        float quality = (truth == null) ? 1 : BudgetFunctions.truthToQuality(truth);
        return new BudgetValue(priority, durability, quality);
    }

    /* ---------- parse String into term ---------- */
    /**
     * Top-level method that parse a Term in general, which may recursively call
     * itself.
     * <p>
     * There are 5 valid cases: 1. (Op, A1, ..., An) is a CompoundTerm if Op is
     * a built-in operator 2. {A1, ..., An} is an SetExt; 3. [A1, ..., An] is an
     * SetInt; 4. <T1 Re T2> is a Statement (including higher-order Statement);
     * 5. otherwise it is a simple term.
     *
     * @param s0 the String to be parsed
     * @param memory Reference to the memory
     * @return the Term generated from the String
     */
    public static Term parseTerm(String s0, Memory memory) {
        //System.out.println("test: " + s0);
        String s = s0.trim();
        try {
            if (s.length() == 0) {
                throw new InvalidInputException("missing content");
            }
            Term t = memory.nameToListedTerm(s);    // existing constant or operator
            if (t != null) {
                return t;
            }                           // existing Term
            int index = s.length() - 1;
            char first = s.charAt(0);
            char last = s.charAt(index);
            
            
            switch (first) {
                case COMPOUND_TERM_OPENER:
                    if (last == COMPOUND_TERM_CLOSER) {
                        return parseCompoundTerm(s.substring(1, index), memory);
                    } else {
                        throw new InvalidInputException("missing CompoundTerm closer");
                    }
                case SET_EXT_OPENER:
                    if (last == SET_EXT_CLOSER) {
                        return SetExt.make(parseArguments(s.substring(1, index) + ARGUMENT_SEPARATOR, memory), memory);
                    } else {
                        throw new InvalidInputException("missing ExtensionSet closer");
                    }
                case SET_INT_OPENER:
                    if (last == SET_INT_CLOSER) {
                        return SetInt.make(parseArguments(s.substring(1, index) + ARGUMENT_SEPARATOR, memory), memory);
                    } else {
                        throw new InvalidInputException("missing IntensionSet closer");
                    }
                case STATEMENT_OPENER:
                    if (last == STATEMENT_CLOSER) {
                        return parseStatement(s.substring(1, index), memory);
                    } else {
                        throw new InvalidInputException("missing Statement closer");
                    }
                default:
                    return parseAtomicTerm(s);
            }
        } catch (InvalidInputException e) {
            String message = " !!! INVALID INPUT: parseTerm: " + s + " --- " + e.getMessage();
            System.out.println(message);
//            showWarning(message);
        }
        return null;
    }

//    private static void showWarning(String message) {
//		new TemporaryFrame( message + "\n( the faulty line has been kept in the input window )",
//				40000, TemporaryFrame.WARNING );
//    }
    /**
     * Parse a Term that has no internal structure.
     * <p>
     * The Term can be a constant or a variable.
     *
     * @param s0 the String to be parsed
     * @throws nars.io.StringParser.InvalidInputException the String cannot be
     * parsed into a Term
     * @return the Term generated from the String
     */
    private static Term parseAtomicTerm(String s0) throws InvalidInputException {
        String s = s0.trim();
        if (s.length() == 0) {
            throw new InvalidInputException("missing term");
        }
        if (s.contains(" ")) // invalid characters in a name
        {
            throw new InvalidInputException("invalid term");
        }
        
        if (Variable.containVar(s)) {
            return new Variable(s);
        } else {
            return new Term(s);
        }
    }

    /**
     * Parse a String to create a Statement.
     *
     * @return the Statement generated from the String
     * @param s0 The input String to be parsed
     * @throws nars.io.StringParser.InvalidInputException the String cannot be
     * parsed into a Term
     */
    private static Statement parseStatement(String s0, Memory memory) throws InvalidInputException {
        String s = s0.trim();
        //System.out.println("parseStatement: " + s0);
        int i = topRelation(s);
        //System.out.println(i);
        if (i < 0) {
            throw new InvalidInputException("invalid statement");
        }
        //System.out.println("456: " + s);
        String relation = s.substring(i, i + 3);
        //System.out.println(relation);
        Term subject = parseTerm(s.substring(0, i), memory);
        Term predicate = parseTerm(s.substring(i + 3), memory);
        //System.out.println("reation: " + relation);
        Statement t = Statement.make(relation, subject, predicate, memory);
        //System.out.println(t.getName());
        if (t == null) {
            throw new InvalidInputException("invalid statement");
        }
        /*System.out.println("t: " + t.getName());
        System.out.println("order: " + t.getTemporalOrder());*/
        return t;
    }

    /**
     * Parse a String to create a CompoundTerm.
     *
     * @return the Term generated from the String
     * @param s0 The String to be parsed
     * @throws nars.io.StringParser.InvalidInputException the String cannot be
     * parsed into a Term
     */
    private static Term parseCompoundTerm(String s0, Memory memory) throws InvalidInputException {
        String s = s0.trim();
        int firstSeparator = s.indexOf(ARGUMENT_SEPARATOR);
        String op = s.substring(0, firstSeparator).trim();
        if (!CompoundTerm.isOperator(op)) {
            
            String operatorString = Operator.addPrefix(op);
            Operator operator = memory.getOperator(operatorString);
            
            if(operator != null){
                ArrayList<Term> args = parseArguments(s.substring(firstSeparator + 1) + ARGUMENT_SEPARATOR, memory);
                Operation o = Operation.make(operator, args);
                //System.out.println(o.getClass().toString());
                return o;
            }
            System.out.println("Invalid Compound or Operation");
            return null;
        }
        ArrayList<Term> arg = parseArguments(s.substring(firstSeparator + 1) + ARGUMENT_SEPARATOR, memory);
        Term t = CompoundTerm.make(op, arg, memory);
        if (t == null) {
            throw new InvalidInputException("invalid compound term");
        }
        return t;
    }

    /**
     * Parse a String into the argument get of a CompoundTerm.
     *
     * @return the arguments in an ArrayList
     * @param s0 The String to be parsed
     * @throws nars.io.StringParser.InvalidInputException the String cannot be
     * parsed into an argument get
     */
    private static ArrayList<Term> parseArguments(String s0, Memory memory) throws InvalidInputException {
        String s = s0.trim();
        ArrayList<Term> list = new ArrayList<>();
        int start = 0;
        int end = 0;
        Term t;
        while (end < s.length() - 1) {
            end = nextSeparator(s, start);
            t = parseTerm(s.substring(start, end), memory);     // recursive call
            list.add(t);
            start = end + 1;
        }
        if (list.isEmpty()) {
            throw new InvalidInputException("null argument");
        }
        return list;
    }

    /* ---------- locate top-level substring ---------- */
    /**
     * Locate the first top-level separator in a CompoundTerm
     *
     * @return the index of the next seperator in a String
     * @param s The String to be parsed
     * @param first The starting index
     */
    private static int nextSeparator(String s, int first) {
        int levelCounter = 0;
        int i = first;
        while (i < s.length() - 1) {
            if (isOpener(s, i)) {
                levelCounter++;
            } else if (isCloser(s, i)) {
                levelCounter--;
            } else if (s.charAt(i) == ARGUMENT_SEPARATOR) {
                if (levelCounter == 0) {
                    break;
                }
            }
            i++;
        }
        return i;
    }

    /**
     * locate the top-level relation in a statement
     *
     * @return the index of the top-level relation
     * @param s The String to be parsed
     */
    private static int topRelation(String s) {      // need efficiency improvement
        int levelCounter = 0;
        int i = 0;
        while (i < s.length() - 3) {    // don't need to check the last 3 characters
            if ((levelCounter == 0) && (Statement.isRelation(s.substring(i, i + 3)))) {
                return i;
            }
            if (isOpener(s, i)) {
                levelCounter++;
            } else if (isCloser(s, i)) {
                levelCounter--;
            }
            i++;
        }
        return -1;
    }

    /* ---------- recognize symbols ---------- */
    /**
     * Check CompoundTerm opener symbol
     *
     * @return if the given String is an opener symbol
     * @param s The String to be checked
     * @param i The starting index
     */
    private static boolean isOpener(String s, int i) {
        char c = s.charAt(i);
        boolean b = (c == COMPOUND_TERM_OPENER)
                || (c == SET_EXT_OPENER)
                || (c == SET_INT_OPENER)
                || (c == STATEMENT_OPENER);
        if (!b) {
            return false;
        }
        if (i + 3 <= s.length() && Statement.isRelation(s.substring(i, i + 3))) {
            return false;
        }
        return true;
    }

    /**
     * Check CompoundTerm closer symbol
     *
     * @return if the given String is a closer symbol
     * @param s The String to be checked
     * @param i The starting index
     */
    private static boolean isCloser(String s, int i) {
        char c = s.charAt(i);
        boolean b = (c == COMPOUND_TERM_CLOSER)
                || (c == SET_EXT_CLOSER)
                || (c == SET_INT_CLOSER)
                || (c == STATEMENT_CLOSER);
        if (!b) {
            return false;
        }
        if (i >= 2 && Statement.isRelation(s.substring(i - 2, i + 1))) {
            return false;
        }
        return true;
    }
    
    public static Tense parseTense(StringBuffer s){
        final int i = s.indexOf(Symbols.TENSE_MARK);
        String t = "";
        if(i > 0){
            t = s.substring(i).trim();
            s.delete(i, s.length());
        }
        //System.out.println("t: " + t);
        return Tense.tense(t);
                
    }
}
