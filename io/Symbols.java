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

/**
 * The ASCII symbols used in I/O.
 */
public class Symbols {

    /* sentence type and delimitors */
    public static final char JUDGMENT_MARK = '.';
    public static final char QUESTION_MARK = '?';
    public static final char GOAL_MARK = '!';
    public static final char QUEST_MARK = '@';

    /* Tense markers */
    public static final String TENSE_MARK = ":";
    public static final String TENSE_PAST = ":\\:";
    public static final String TENSE_PRESENT = ":|:";
    public static final String TENSE_FUTURE = ":/:";
    
    /* variable type */
    public static final char VAR_INDEPENDENT = '$';
    public static final char VAR_DEPENDENT = '#';
    public static final char VAR_QUERY = '?';

    /* numerical value delimitors, must be different from the Term delimitors */
    public static final char BUDGET_VALUE_MARK = '$';
    public static final char TRUTH_VALUE_MARK = '%';
    public static final char VALUE_SEPARATOR = ';';

    /* CompountTerm delimitors, must use 4 different pairs */
    public static final char COMPOUND_TERM_OPENER = '(';
    public static final char COMPOUND_TERM_CLOSER = ')';
    public static final char STATEMENT_OPENER = '<';
    public static final char STATEMENT_CLOSER = '>';
    public static final char SET_EXT_OPENER = '{';
    public static final char SET_EXT_CLOSER = '}';
    public static final char SET_INT_OPENER = '[';
    public static final char SET_INT_CLOSER = ']';
    
    /* special characors in argument list */
    public static final char ARGUMENT_SEPARATOR = ',';
    public static final char IMAGE_PLACE_HOLDER = '_';

    /* CompountTerm operators, length = 1 */
    public static final String INTERSECTION_EXT_OPERATOR = "&";
    public static final String INTERSECTION_INT_OPERATOR = "|";
    public static final String DIFFERENCE_EXT_OPERATOR = "-";
    public static final String DIFFERENCE_INT_OPERATOR = "~";
    public static final String PRODUCT_OPERATOR = "*";
    public static final String IMAGE_EXT_OPERATOR = "/";
    public static final String IMAGE_INT_OPERATOR = "\\";

    /* CompoundStatement operators, length = 2 */
    public static final String NEGATION_OPERATOR = "--";
    public static final String DISJUNCTION_OPERATOR = "||";
    
    public static final String CONJUNCTION_OPERATOR = "&&";
    public static final String CONJUNCTION_SEQUENCE = "&/";
    public static final String CONJUNCTION_PARALLEL = "&|";
    
    /* built-in relations, length = 3 */
    public static final String INHERITANCE_RELATION = "-->";
    public static final String SIMILARITY_RELATION = "<->";
    public static final String INSTANCE_RELATION = "{--";
    public static final String PROPERTY_RELATION = "--]";
    public static final String INSTANCE_PROPERTY_RELATION = "{-]";

    public static final String EQUIVALENCE_RELATION = "<=>";
    public static final String EQUIVALENCE_AFTER = "</>";
    public static final String EQUIVALENCE_WHEN = "<|>";
    public static final String EQUIVALENCE_BEFORE = "<\\>";
    
    public static final String IMPLICATION_RELATION = "==>";
    public static final String IMPLICATION_BEFORE = "=\\>";
    public static final String IMPLICATION_WHEN = "=|>";
    public static final String IMPLICATION_AFTER = "=/>";

    /* experience line prefix */
    public static final String INPUT_LINE = "IN";
    public static final String OUTPUT_LINE = "OUT";
    public static final char PREFIX_MARK = ':';
    public static final char RESET_MARK = '*';
    public static final char COMMENT_MARK = '/';

    /* Stamp, display only */
    public static final char STAMP_OPENER = '{';
    public static final char STAMP_CLOSER = '}';
    public static final char STAMP_SEPARATOR = ';';
    public static final char STAMP_STARTER = ':';
    
    /* TermLink type, display only */
    public static final String TO_COMPONENT_1 = " @(";
    public static final String TO_COMPONENT_2 = ")_ ";
    public static final String TO_COMPOUND_1 = " _@(";
    public static final String TO_COMPOUND_2 = ") ";
    
    public static final char INTERVAL_PREFIX = '+';
}
