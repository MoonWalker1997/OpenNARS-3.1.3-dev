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

import java.awt.FileDialog;
import java.io.*;

import nars.io.IInferenceRecorder;

/**
 * Inference log, which record input/output of each inference step
 * interface with 1 implementation: GUI ( batch not implemented )
 */
public class InferenceRecorder implements IInferenceRecorder {

    /** the display window */
    private InferenceWindow window = new InferenceWindow(this);
    /** whether to display */
    private boolean isReporting = false;
    /** the log file */
    private PrintWriter logFile = null;

    @Override
	public void init() {
        window.clear();
    }

    @Override
	public void show() {
        window.setVisible(true);
    }

    @Override
	public void play() {
        isReporting = true;
    }

    @Override
	public void stop() {
        isReporting = false;
    }

    @Override
	public void append(String s) {
        if (isReporting) {
            window.append(s);
        }
        if (logFile != null) {
            logFile.println(s);
        }
    }

    @Override
	public void openLogFile() {
        FileDialog dialog = new FileDialog((FileDialog) null, "Inference Log", FileDialog.SAVE);
        dialog.setVisible(true);
        String directoryName = dialog.getDirectory();
        String fileName = dialog.getFile();
        try {
            logFile = new PrintWriter(new FileWriter(directoryName + fileName));
        } catch (IOException ex) {
            System.out.println("i/o error: " + ex.getMessage());
        }
        window.switchBackground();
        window.setVisible(true);
    }

    @Override
	public void closeLogFile() {
        logFile.close();
        logFile = null;
        window.resetBackground();
    }

    @Override
	public boolean isLogging() {
        return (logFile != null);
    }
}

