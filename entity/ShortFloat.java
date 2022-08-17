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

/**
 * A float value in [0, 1], with 4 digits accuracy.
 */
public class ShortFloat implements Cloneable {

    /** To save space, the values are stored as short integers (-32768 to 32767, only 0 to 10000 used),
    but used as float */
    private short value;

    /**
     * Constructor
     * @param v The initial value
     */
    public ShortFloat(short v) {
        value = v;
    }

    /** 
     * Constructor
     * @param v The initial value in float
     */
    public ShortFloat(float v) {
        setValue(v);
    }

    /**
     * To access the value as float
     * @return The current value in float
     */
    public float getValue() {
        return value * 0.0001f;
    }

    /**
     * To access the value as short
     * @return The current value in short
     */
    private short getShortValue() {
        return value;
    }

    /**
     * Set new value, rounded, with validity checking
     * @param v The new value
     */
    public final void setValue(float v) {
        if ((v < 0) || (v > 1)) {
            //throw new ArithmeticException("Invalid value: " + v);
            value = (short)(0.5);
        } else {
            value = (short) (v * 10000.0 + 0.5);
        }
    }

    /**
     * Compare two ShortFloat values
     * @param that The other value to be compared
     * @return Whether the two have the same value
     */
    @Override
    public boolean equals(Object that) {
        return ((that instanceof ShortFloat) && (value == ((ShortFloat) that).getShortValue()));
    }

    /**
     * The hash code of the ShortFloat
     * @return The hash code
     */
    @Override
    public int hashCode() {
        return this.value + 17;
    }

    /**
     * To create an identical copy of the ShortFloat
     * @return A cloned ShortFloat
     */
    @Override
    public Object clone() {
        return new ShortFloat(value);
    }

    /**
     * Convert the value into a String
     * @return The String representation, with 4 digits accuracy
     */
    @Override
    public String toString() {
        if (value >= 10000) {
            return "1.0000";
        } else {
            String s = String.valueOf(value);
            while (s.length() < 4) {
                s = "0" + s;
            }
            return "0." + s;
        }
    }

    /**
     * Round the value into a short String
     * @return The String representation, with 2 digits accuracy
     */
    public String toStringBrief() {
        value += 50;
        String s = toString();
        value -= 50;
        if (s.length() > 4) {
            return s.substring(0, 4);
        } else {
            return s;
        }
    }


}
