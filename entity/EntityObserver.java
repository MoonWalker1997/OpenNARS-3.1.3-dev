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

import nars.storage.BagObserver;

/**
 * Observer for a {@link Concept} object; similar to Observer design pattern,
 * except that here we have a single observer; NOTE: very similar to interface
 * {@link nars.storage.BagObserver}
 */
public interface EntityObserver {

    /**
     * Display the content of the concept
     *
     * @param str The text to be displayed
     */
    public abstract void post(String str);

    /**
     * create a {@link BagObserver} of the right type (Factory design pattern)
     */
    @SuppressWarnings("rawtypes")
    public abstract BagObserver createBagObserver();

    /**
     * Set the observed Concept
     *
     * @param showLinks unused : TODO : is this forgotten ?
     */
    public abstract void startPlay(Concept concept, boolean showLinks);

    /**
     * put in non-showing state
     */
    public abstract void stop();

    /**
     * Refresh display if in showing state
     */
    void refresh(String message);

}
