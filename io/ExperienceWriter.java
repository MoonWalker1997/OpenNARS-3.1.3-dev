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

import java.awt.FileDialog;
import java.io.*;
import java.util.*;

import nars.main.NAR;

/**
 * To read and write experience as Task streams
 */
public class ExperienceWriter implements OutputChannel {

    private NAR reasoner;
    /**
     * Input experience from a file
     */
    private PrintWriter outExp;

    /**
     * Default constructor
     *
     * @param reasoner
     */
    public ExperienceWriter(NAR reasoner) {
        this.reasoner = reasoner;
    }

    public ExperienceWriter(NAR reasoner, PrintWriter outExp) {
        this(reasoner);
        this.outExp = outExp;
    }

    /**
     * Open an output experience file
     */
    public void openSaveFile() {
        FileDialog dialog = new FileDialog((FileDialog) null, "Save experience", FileDialog.SAVE);
        dialog.setVisible(true);
        String directoryName = dialog.getDirectory();
        String fileName = dialog.getFile();
        try {
            outExp = new PrintWriter(new FileWriter(directoryName + fileName));
        } catch (IOException ex) {
            System.out.println("i/o error: " + ex.getMessage());
        }
        reasoner.addOutputChannel(this);
    }

    /**
     * Close an output experience file
     */
    public void closeSaveFile() {
        outExp.close();
        reasoner.removeOutputChannel(this);
    }

    /**
     * Process the next chunk of output data
     *
     * @param lines The text to be displayed
     */
    @Override
    public void nextOutput(ArrayList<String> lines) {
        if (outExp != null) {
            for (Object line : lines) {
                outExp.println(line.toString());
            }
        }
    }

    @Override
    public void tickTimer() {
    }
}
