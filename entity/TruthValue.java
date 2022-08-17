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
package nars.entity;

import nars.io.Symbols;
import nars.main.Parameters;

/**
 * Frequency and confidence.
 */
public class TruthValue implements Cloneable { // implements Cloneable {

    /**
     * The character that marks the two ends of a truth value
     */
    private static final char DELIMITER = Symbols.TRUTH_VALUE_MARK;
    /**
     * The character that separates the factors in a truth value
     */
    private static final char SEPARATOR = Symbols.VALUE_SEPARATOR;
    /**
     * The frequency factor of the truth value
     */
    private ShortFloat frequency;
    /**
     * The confidence factor of the truth value
     */
    private ShortFloat confidence;
    /**
     * Whether the truth value is derived from a definition
     */
    private boolean isAnalytic = false;
    
    private boolean eternal;

    /**
     * Constructor with two ShortFloats
     *
     * @param f The frequency value
     * @param c The confidence value
     */
    public TruthValue(float f, float c) {
        frequency = new ShortFloat(f);
        confidence = (c < 1) ? new ShortFloat(c) : new ShortFloat(0.9999f);
        eternal = true;
    }
    
    public TruthValue(float f, float c, boolean b, boolean eternal){
        frequency = new ShortFloat(f);
        confidence = (c < 1) ? new ShortFloat(c) : new ShortFloat(0.9999f);
        isAnalytic = b;
        this.eternal = eternal;
    }

    /**
     * Constructor with two ShortFloats
     *
     * @param f The frequency value
     * @param c The confidence value
     *
     */
    public TruthValue(float f, float c, boolean b) {
        frequency = new ShortFloat(f);
        confidence = (c < 1) ? new ShortFloat(c) : new ShortFloat(0.9999f);
        isAnalytic = b;
        eternal = true;
    }

    /**
     * Constructor with a TruthValue to clone
     *
     * @param v The truth value to be cloned
     */
    public TruthValue(TruthValue v) {
        frequency = new ShortFloat(v.getFrequency());
        confidence = new ShortFloat(v.getConfidence());
        isAnalytic = v.getAnalytic();
        eternal = true;
    }

    /**
     * Get the frequency value
     *
     * @return The frequency value
     */
    public float getFrequency() {
        return frequency.getValue();
    }

    /**
     * Get the confidence value
     *
     * @return The confidence value
     */
    public float getConfidence() {
        return confidence.getValue();
    }

    /**
     * Get the isAnalytic flag
     *
     * @return The isAnalytic value
     */
    public boolean getAnalytic() {
        return isAnalytic;
    }

    /**
     * Set the isAnalytic flag
     */
    public void setAnalytic() {
        isAnalytic = true;
    }
    
    public boolean isEternal(){
        return eternal;
    }
    
    public void setEternal(boolean eternal){
        this.eternal = eternal;
    }

    /**
     * Calculate the expectation value of the truth value
     *
     * @return The expectation value
     */
    public float getExpectation() {return (float) (confidence.getValue() * (frequency.getValue() - 0.5) + 0.5);}

    /**
     * Calculate the absolute difference of the expectation value and that of a
     * given truth value
     *
     * @param t The given value
     * @return The absolute difference
     */
    public float getExpDifAbs(TruthValue t) {
        return Math.abs(getExpectation() - t.getExpectation());
    }
    
    public float getIgnorance(){
        return (float)(1-confidence.getValue());
    }
    
    public float getSharpness(){
        return (float)(2 * Math.abs(getExpectation() - 0.5)); // Change for 3.1.2
        //return (float)(2 * Math.pow(Math.abs(getExpectation() - 0.5), 4));
    }
    
    /**
     * Check if the truth value is negative
     *
     * @return True if the frequence is less than 1/2
     */
    public boolean isNegative() {
        return getFrequency() < 0.5;
    }
    
    public float getIgnornce(){
        return (float) (1.0 - confidence.getValue());
    }
    
    public TruthValue setConfidence(float c){
        float max_confidence = 1.0f - Parameters.TRUTH_EPSILON;
        this.confidence = new ShortFloat((c < max_confidence ? c : max_confidence));
        return this;
    }

    /**
     * Compare two truth values
     *
     * @param that The other TruthValue
     * @return Whether the two are equivalent
     */
    @Override
    public boolean equals(Object that) {
        return ((that instanceof TruthValue)
                && (getFrequency() == ((TruthValue) that).getFrequency())
                && (getConfidence() == ((TruthValue) that).getConfidence()));
    }

    /**
     * The hash code of a TruthValue
     *
     * @return The hash code
     */
    @Override
    public int hashCode() {
        return (int) (getExpectation() * 37);
    }

    @Override
    public TruthValue clone() {
        return new TruthValue(getFrequency(), getConfidence(), getAnalytic());
    }

    /**
     * The String representation of a TruthValue
     *
     * @return The String
     */
    @Override
    public String toString() {
        return DELIMITER + frequency.toString() + SEPARATOR + confidence.toString() + DELIMITER;
    }

    /**
     * A simplified String representation of a TruthValue, where each factor is
     * accruate to 1%
     *
     * @return The String
     */
    public String toStringBrief() {
        String s1 = DELIMITER + frequency.toStringBrief() + SEPARATOR;
        String s2 = confidence.toStringBrief();
        if (s2.equals("1.00")) {
            return s1 + "0.99" + DELIMITER;
        } else {
            return s1 + s2 + DELIMITER;
        }
    }
}
