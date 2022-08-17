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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.PrintStream;
import java.io.PrintWriter;

import nars.io.ExperienceReader;
import nars.io.ExperienceWriter;

;

/**
 * The main class of the project.
 * <p>
 * Define an application with batch functionality; TODO check duplicated code
 * with {@link nars.main.NARS}
 * <p>
 * Manage the internal working thread. Communicate with Reasoner only.
 */
public class NARSBatch {

    /**
     * The reasoner
     */
    NAR reasoner;
    private boolean logging;
    private PrintStream out = System.out;
    private boolean dumpLastState = true;
    /**
     * Flag to distinguish the two running modes of the project.
     */
    private static boolean standAlone = false;

    /**
     * The entry point of the standalone application.
     * <p>
     * Create an instance of the class, then run the {@link #init(String[])} and
     * {@link #run()} methods.
     *
     * @param args optional argument used : one input file
     */
    public static void main(String args[]) {
        NARSBatch nars = new NARSBatch();
        setStandAlone(true);
        CommandLineParameters.decode(args, nars.getReasoner());
        nars.runInference(args);
        // TODO only if single run ( no reset in between )
        if (nars.dumpLastState) {
            System.out.println("\n==== Dump Last State ====\n"
                    + nars.reasoner.toString());
        }
    }

    public NARSBatch() {
        init();
    }

    /**
     * non-static equivalent to {@link #main(String[])} : run to completion from
     * an input file
     */
    public void runInference(String args[]) {
        init(args);
        run();
    }

    /**
     * initialize from an input file
     */
    public void init(String[] args) {
        if (args.length > 0) {
            ExperienceReader experienceReader = new ExperienceReader(reasoner);
            experienceReader.openLoadFile(args[0]);
        }
        reasoner.addOutputChannel(new ExperienceWriter(reasoner,
                new PrintWriter(out, true)));
    }

    /**
     * non-static equivalent to {@link #main(String[])} : run to completion from
     * a BufferedReader
     */
    public void runInference(BufferedReader r, BufferedWriter w) {
        init(r, w);
        run();
    }

    private void init(BufferedReader r, BufferedWriter w) {
        ExperienceReader experienceReader = new ExperienceReader(reasoner);
        experienceReader.setBufferedReader(r);
        reasoner.addOutputChannel(new ExperienceWriter(reasoner,
                new PrintWriter(w, true)));
    }

    /**
     * Initialize the system at the control center.<p>
     * Can instantiate multiple reasoners
     */
    public final void init() {
        reasoner = new NAR();
    }

    /**
     * Run to completion: repeatedly execute NARS working cycle, until Inputs
     * are Finished, or 1000 steps. This method is called when the Runnable's
     * thread is started.
     */
    public void run() {
        while (true) {
            log("NARSBatch.run():"
                    + " step " + reasoner.getTime()
                    + " " + reasoner.isFinishedInputs());
            reasoner.cycle();
            log("NARSBatch.run(): after tick"
                    + " step " + reasoner.getTime()
                    + " " + reasoner.isFinishedInputs());
            if (reasoner.isFinishedInputs()
                    || reasoner.getTime() == 1000) {
                break;
            }
        }
    }

    public void setPrintStream(PrintStream out) {
        this.out = out;
    }

    private void log(String mess) {
        if (logging) {
            System.out.println("/ " + mess);
        }
    }

    public NAR getReasoner() {
        return reasoner;
    }

    /**
     * Whether the project running as an application.
     *
     * @return true for application; false for applet.
     */
    public static boolean isStandAlone() {
        return standAlone;
    }

    public static void setStandAlone(boolean standAlone) {
        NARSBatch.standAlone = standAlone;
    }
}
