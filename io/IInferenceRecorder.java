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

public interface IInferenceRecorder {

    /**
     * Initialize the window and the file
     */
    public abstract void init();

    /**
     * Show the window
     */
    public abstract void show();

    /**
     * Begin the display
     */
    public abstract void play();

    /**
     * Stop the display
     */
    public abstract void stop();

    /**
     * Add new text to display
     *
     * @param s The line to be displayed
     */
    public abstract void append(String s);

    /**
     * Open the log file
     */
    public abstract void openLogFile();

    /**
     * Close the log file
     */
    public abstract void closeLogFile();

    /**
     * Check file logging
     *
     * @return If the file logging is going on
     */
    public abstract boolean isLogging();
}