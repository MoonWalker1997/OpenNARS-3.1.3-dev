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
package nars.main;

/**
 * Collected system parameters. To be modified before compiling.
 */
public class Parameters {
    
    /*----------- OpenNARS Info ----------------------------------------------*/
    /** The information about the version and date of the project. */
    public static final String VERSION = "3.1.2";
    public static final String VERSION_INFO = "OpenNARS Version " + VERSION + " October 2021\n";
    /** The project web sites. */
    public static final String WEBSITE = " OpenNARS website:  http://code.google.com/p/open-nars/ \n"
                                       + " NARS website:  http://sites.google.com/site/narswang/";

    /* ---------- initial values of run-time adjustable parameters ---------- */
    /** Concept decay rate in ConceptBag, in [1, 99]. */
    public static final int CONCEPT_FORGETTING_CYCLE = 10;
    /** TaskLink decay rate in TaskLinkBag, in [1, 99]. */
    public static final int TASK_LINK_FORGETTING_CYCLE = 20;
    /** TermLink decay rate in TermLinkBag, in [1, 99]. */
    public static final int TERM_LINK_FORGETTING_CYCLE = 50;
    /** Silent threshold for task reporting, in [0, 100]. */
    public static final int SILENT_LEVEL = 0;

    /* ---------- time management ---------- */
    /** Task decay rate in TaskBuffer, in [1, 99]. */
    public static final int NEW_TASK_FORGETTING_CYCLE = 1;
    /** Maximum TermLinks checked for novelty for each TaskLink in TermLinkBag */
    public static final int MAX_MATCHED_TERM_LINK = 10;
    /** Maximum TermLinks used in reasoning for each Task in Concept */
    public static final int MAX_REASONED_TERM_LINK = 3;

    /* ---------- logical parameters ---------- */
    /** Evidential Horizon, the amount of future evidence to be considered. */
    public static final int HORIZON = 1;    // or 2, can be float
    /** Reliance factor, the empirical confidence of analytical truth. */
    public static final float RELIANCE = (float) 0.9;    // the same as default confidence

    /* ---------- budget thresholds ---------- */
    /** The budget threshold rate for task to be accepted. */
    public static final float BUDGET_THRESHOLD = (float) 0.01;

    /* ---------- default input values ---------- */
    /** Default expectation for confirmation. */
    public static final float DEFAULT_CONFIRMATION_EXPECTATION = (float) 0.8;
    /** Default expectation for confirmation. */
    public static final float DEFAULT_CREATION_EXPECTATION = (float) 0.66;
    /** Default confidence of input judgment. */
    public static final float DEFAULT_JUDGMENT_CONFIDENCE = (float) 0.9;
    /** Default priority of input judgment */
    public static final float DEFAULT_JUDGMENT_PRIORITY = (float) 0.8;
    /** Default durability of input judgment */
    public static final float DEFAULT_JUDGMENT_DURABILITY = (float) 0.8;
    /** Default priority of input question */
    public static final float DEFAULT_QUESTION_PRIORITY = (float) 0.85;
    /** Default durability of input question */
    public static final float DEFAULT_QUESTION_DURABILITY = (float) 0.9;
    /** Default priority of input event **/
    public static final float DEFAULT_EVENT_PRIORITY = (float) 0.85;
    /** Default durability of event**/
    public static final float DEFAULT_EVENT_DURABILITY = (float) 0.2;
    
    
    /** Default confidence of input goal **/
    
    public static float DEFAULT_GOAL_CONFIDENCE = (float) 0.9;
    
    public static float DEFAULT_GOAL_PRIORITY = (float) 1;
    
    public static float DEFAULT_GOAL_DURABILITY = (float) 0.9;
    
    public static float DEFAULT_QUEST_PRIORITY = (float) 0.9;
    
    public static float DEFAULT_QUEST_DURABILITY = (float) 0.9;
    
    public static float DEFAULT_SUBGOAL_PRIORITY = (float) 1.0;

    /* ---------- space management ---------- */
    /** Level granularity in Bag, two digits */
    public static final int BAG_LEVEL = 100;
    /** Level separation in Bag, one digit, for display (run-time adjustable) and management (fixed) */
    public static final int BAG_THRESHOLD = 10;
    /** Hashtable load factor in Bag */
    public static final float LOAD_FACTOR = (float) 0.5;
    /** Size of ConceptBag */
    public static final int CONCEPT_BAG_SIZE = 1000;
    /** Size of TaskLinkBag */
    public static final int TASK_LINK_BAG_SIZE = 20;
    /** Size of TermLinkBag */
    public static final int TERM_LINK_BAG_SIZE = 100;
    /** Size of TaskBuffer */
    public static final int TASK_BUFFER_SIZE = 10;
    /** Size of global buffer */
    public static final int GLOBAL_BUFFER_SIZE = 20;
    /** Size of internal buffer */
    public static final int INTERNAL_BUFFER_SIZE = 10;
   
    
    /* ---------- avoiding repeated reasoning ---------- */
    /** Maximum length of Stamp, a power of 2 */
    public static final int MAXIMUM_STAMP_LENGTH = 8;
    /** Remember recently used TermLink on a Task */
    public static final int TERM_LINK_RECORD_LENGTH = 10;
    /** Maximum number of beliefs kept in a Concept */
    public static final int MAXIMUM_BELIEF_LENGTH = 7;
    /** Maximum number of goals kept in a Concept */
    public static final int MAXIMUM_QUESTIONS_LENGTH = 5;
    
        /* ----------------------Buffer---------------------*/
    /**
     * Duration for internal buffer
     * Cycles per duration
     * Past/future tense usage convention
     * How far away "past" and "future" is from "now" in cycles
     * The range of now is [-Duration/2, Duration/2]
     **/
    public static final int DURATION_FOR_INTERNAL_BUFFER = 2;
    public static final int DURATION_FOR_GLOBAL_BUFFER = 10;
    
    /**
     * This value multiplied with DURATION gives the time a buffer element can stay in a buffer 
     */
    public static final int MAX_BUFFER_DURATION_FACTOR = 2;
     
    /** 
       Cycles per duration.
       Past/future tense usage convention;
       How far away "past" and "future" is from "now", in cycles.         
       The range of "now" is [-DURATION/2, +DURATION/2];      */
    
    public static final int DURATION = 5;
    
    public static final int DEFAULT_TIME_INTERVAL = 3;
    /**Used in Sentence.java for Truth Value Projection */
    public static double PROJECTION_DECAY = 1000;
    /**Used in Sentence.java for Confidence ONLY Projection */
    public static double CONFIDENCE_PROJECTION_DECAY = -0.02;
    
    public static float TRUTH_EPSILON = 0.01f;
    
    public static int REVISION_MAX_OCCURRENCE_DISTANCE = 10;
    
    public static float ANTICIPATION_CONFIDENCE = 0.1f;
    
    public static float ANTICIPATION_TOLERANCE = 100.0f;
    
    /** Retrospective anticipation, allow to check memory for content in case of anticipation (potential issue with forgetting) */
    public static boolean RETROSPECTIVE_ANTICIPATIONS = false;
    
    public static float DECISION_THRESHOLD = 0.51f;
    
    /** Maximum anticipations about its content stored in a concept */
    public static int ANTICIPATIONS_PER_CONCEPT_MAX = 8;
    
    /** Default priority of execution feedback */
    public static float DEFAULT_FEEDBACK_PRIORITY = (float) 0.9;
    /** Default durability of execution feedback */
    public static float DEFAULT_FEEDBACK_DURABILITY = (float) 0.5; //was 0.8 in 1.5.5; 0.5 after
    
    public static int ANTICIPATION_LIST_CAPACITY = 5;
    
    public static int GOAL_PRECONDITION_CAPACITY = 10;
    
    public static int MAXIMUM_GOAL_LENGTH = 5;
    
}
