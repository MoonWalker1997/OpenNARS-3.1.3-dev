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
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;

import javax.swing.JButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import nars.io.ExperienceReader;
import nars.io.InputChannel;
import nars.main.NAR;

/**
 * Input window, accepting user tasks
 */
public class InputWindow extends NarsFrame implements ActionListener, InputChannel {
    private NAR reasoner;
    /** Control buttons */
    private JButton okButton, holdButton, clearButton, closeButton;
    /** Input area */
    private JTextArea inputText;
    /** Whether the window is ready to accept new input (in fact whether the Reasoner will read the content of {@link #inputText} ) */
    private boolean ready;
    /** number of cycles between experience lines */
    private int timer;

    /**
     * Constructor
     * @param reasoner The reasoner
     * @param title The title of the window
     */
    public InputWindow(NAR reasoner, String title) {
        super(title + " - Input Window");
        getContentPane().setBackground(SINGLE_WINDOW_COLOR);
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        setLayout(gridbag);
        c.ipadx = 3;
        c.ipady = 3;
        c.insets = new Insets(5, 5, 5, 5);
        c.fill = GridBagConstraints.BOTH;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.weightx = 1.0;
        c.weighty = 1.0;
        inputText = new JTextArea("");
        JScrollPane scrollPane = new JScrollPane(inputText);
//        gridbag.setConstraints(inputText, c);
        gridbag.setConstraints(scrollPane, c);
		add(scrollPane);
//        add(inputText);
        c.weighty = 0.0;
        c.gridwidth = 1;
        okButton = new JButton("OK");
        okButton.addActionListener(this);
        gridbag.setConstraints(okButton, c);
        add(okButton);
        holdButton = new JButton("Hold");
        holdButton.addActionListener(this);
        gridbag.setConstraints(holdButton, c);
        add(holdButton);
        clearButton = new JButton("Clear");
        clearButton.addActionListener(this);
        gridbag.setConstraints(clearButton, c);
        add(clearButton);
        closeButton = new JButton("Hide");
        closeButton.addActionListener(this);
        gridbag.setConstraints(closeButton, c);
        add(closeButton);
        setBounds(0, 22, 600, 200);
        setVisible(true);

        this.reasoner = reasoner;
    }

    /**
     * Initialize the window
     */
    public void init() {
        ready = false;
        inputText.setText("");
    }

    /**
     * Handling button click
     * @param e The ActionEvent
     */
    public void actionPerformed(ActionEvent e) {
        JButton b = (JButton) e.getSource();
        if (b == okButton) {
            ready = true;
        } else if (b == holdButton) {
            ready = false;
        } else if (b == clearButton) {
            inputText.setText("");
        } else if (b == closeButton) {
            close();
        }
    }

    private void close() {
        setVisible(false);
    }

    @Override
    public void windowClosing(WindowEvent arg0) {
        close();
    }

    /**
     * Accept text input in a cycle, which can be multiple lines
 TODO some duplicated code with {@link ExperienceReader#nextInput()}
     * @return Whether to check this channel again
     */
    public boolean nextInput() {
        if (timer > 0) {  // wait until the timer
            timer--;
            return true;
        }
        if (!ready) {
            return false;
        }
        String text = inputText.getText().trim();
        String line;    // The next line of text
        int endOfLine;
        // The process steps at a number or no more text
        while ((text.length() > 0) && (timer == 0)) {
        	endOfLine = text.indexOf('\n');
        	if (endOfLine < 0) {	// this code is reached at end of text
        		line = text;
        		text = "";
        	} else {	// this code is reached for ordinary lines
        		line = text.substring(0, endOfLine).trim();
        		text = text.substring(endOfLine + 1);	// text becomes rest of text
        	}

        	try { 	// read NARS language or an integer
        		timer = Integer.parseInt(line);
        		reasoner.walk(timer);
        	} catch (NumberFormatException e) {
                    try {
                        //System.out.println("Line: " + line);
                        reasoner.textInputLine(line);
                    } catch (NullPointerException e1) {
                        System.out.println("InputWindow.nextInput() - NullPointerException: please correct the input" );
//                      throw new RuntimeException( "Uncorrect line: please correct the input", e1 );
                        ready = false;
                        return false;
                    }
        	}
        	inputText.setText(text);	// update input Text widget to rest of text
        	if (text.isEmpty()) {
        		ready = false;
        	}
        }
        return ((text.length() > 0) || (timer > 0));
    }
}
