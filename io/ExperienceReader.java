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
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import nars.main.NAR;

/**
 * To read and write experience as Task streams
 */
public class ExperienceReader implements InputChannel {

    /**
     * Reference to the reasoner
     */
    private NAR reasoner;
    /**
     * Input experience from a file
     */
    private BufferedReader inExp;
    /**
     * Remaining working cycles before reading the next line
     */
    private int timer;

    /**
     * Default constructor
     *
     * @param reasoner Backward link to the reasoner
     */
    public ExperienceReader(NAR reasoner) {
        this.reasoner = reasoner;
        inExp = null;
    }

    /**
     * Open an input experience file with a FileDialog
     */
    public void openLoadFile() {
        FileDialog dialog = new FileDialog((FileDialog) null, "Load experience", FileDialog.LOAD);
        dialog.setVisible(true);
        String directoryName = dialog.getDirectory();
        String fileName = dialog.getFile();
        String filePath = directoryName + fileName;
        openLoadFile(filePath);
    }

    /**
     * Open an input experience file from given file Path
     *
     * @param filePath File to be read as experience
     */
    public void openLoadFile(String filePath) {
        try {
            inExp = new BufferedReader(new FileReader(filePath));
        } catch (IOException ex) {
            System.out.println("i/o error: " + ex.getMessage());
        }
//        reasoner.addInputChannel(this);
    }

    /**
     * Close an input experience file (close the reader in fact)
     */
    public void closeLoadFile() {
        try {
            inExp.close();
        } catch (IOException ex) {
            System.out.println("i/o error: " + ex.getMessage());
        }
//        reasoner.removeInputChannel(this);
    }

    public void setBufferedReader(BufferedReader inExp) {
        this.inExp = inExp;
//        reasoner.addInputChannel(this);
    }

    /**
     * Process the next chunk of input data;
     * TODO some duplicated code with
     * {@link nars.gui.InputWindow#nextInput()}
     *
     * @return Whether the input channel should be checked again
     */
    @Override
    public boolean nextInput() {
        if (timer > 0) {
            timer--;
            return true;
        }
        if (inExp == null) {
            return false;
        }
        String line = null;
        while (timer == 0) {
            try {
                line = inExp.readLine();
                if (line == null) {
                    inExp.close();
                    inExp = null;
                    return false;
                }
            } catch (IOException ex) {
                System.out.println("i/o error: " + ex.getMessage());
            }
            line = line.trim();
            // read NARS language or an integer
            if (line.length() > 0) {
                try {
                    timer = Integer.parseInt(line);
                    reasoner.walk(timer);
                } catch (NumberFormatException e) {
                    reasoner.textInputLine(line);
                }
            }
        }
        return true;
    }
}
