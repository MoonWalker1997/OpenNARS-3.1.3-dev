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
package nars.gui;

import nars.gui.NAR_GUI;
import nars.io.ExperienceReader;
import nars.main.CommandLineParameters;
import nars.main.NARSBatch;
import nars.main.NAR;

/**
 * The main class of OpenNARS with support of a GUI.
 * Alternatively it may start with shell only by invoking Shell.java.
 * Manage the internal working thread. Communicate with NAR_GUI only.
 */
public class main implements Runnable {

    /** The internal working thread of the system. */
    Thread narsThread = null;
    /** The reasoner */
    NAR_GUI reasoner;

    /** @param args Input file */
    public void init(String[] args) {
        reasoner = new NAR_GUI("NARS Reasoner with GUI support");
        if ((args.length > 0) && CommandLineParameters.isReallyFile(args[0])) {
            ExperienceReader experienceReader = new ExperienceReader(reasoner);
            experienceReader.openLoadFile(args[0]);
        }
        CommandLineParameters.decode(args, reasoner);
    }

    /**
     * Start the thread if necessary, called when the page containing the applet
     * first appears on the screen.
     */
    public void start() {
        if (narsThread == null) {
            narsThread = new Thread(this, "Inference");
            narsThread.start();
        }
    }

    /**
     * Repeatedly execute main working cycle. This method is called when the
     * Runnable's thread is started.
     */
    @Override
    public void run() {
        System.out.println("123");
        Thread thisThread = Thread.currentThread();
        while (narsThread == thisThread) {
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
            }
            try {
                reasoner.tick();
//                System.out.println("123");
            } catch (Exception e) {
            }
        }
    }

    /**
     * The entry point of the standalone application.
     * Create an instance of the class
     * @param args optional argument used : one input file, possibly followed by
     * --silence <integer>
     */
    public static void main(String args[]) {
        NARSBatch.setStandAlone(true);
        main nars = new main();
        nars.init(args);
        nars.start();
    }
}