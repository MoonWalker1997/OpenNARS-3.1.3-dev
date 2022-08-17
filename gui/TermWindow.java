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

import javax.swing.*;

import java.awt.*;
import java.awt.event.*;

import nars.entity.Concept;
import nars.entity.EntityObserver;
//import nars.io.StringParser;
import nars.storage.Memory;

/**
 * JWindow accept a Term, then display the content of the corresponding Concept
 */
public class TermWindow extends NarsFrame implements ActionListener {

    /**
     * Display label
     */
    private JLabel termLabel;
    /**
     * Input field for term name
     */
    private JTextField termField;
    /**
     * Control buttons
     */
    private JButton playButton, hideButton;
    /**
     * Reference to the memory
     */
    private Memory memory;

    /**
     * Constructor
     */
    TermWindow(Memory memory) {
        super("Term Window");
        this.memory = memory;

        getContentPane().setBackground(SINGLE_WINDOW_COLOR);
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        setLayout(gridbag);

        c.ipadx = 3;
        c.ipady = 3;
        c.insets = new Insets(5, 5, 5, 5);
        c.fill = GridBagConstraints.BOTH;
        c.gridwidth = 1;
        c.weightx = 0.0;
        c.weighty = 0.0;
        termLabel = new JLabel("Term:", JLabel.RIGHT);
        termLabel.setBackground(SINGLE_WINDOW_COLOR);
        gridbag.setConstraints(termLabel, c);
        add(termLabel);

        c.weightx = 1.0;
        termField = new JTextField("");
        JScrollPane scrollPane = new JScrollPane(termField);
        gridbag.setConstraints(scrollPane, c);
        add(scrollPane);

        c.weightx = 0.0;
        playButton = new JButton("Show");
        playButton.addActionListener(this);
        gridbag.setConstraints(playButton, c);
        add(playButton);

        hideButton = new JButton("Hide");
        hideButton.addActionListener(this);
        gridbag.setConstraints(hideButton, c);
        add(hideButton);

        setBounds(600, 22, 600, 100);
    }

    /**
     * Handling button click
     *
     * @param e The ActionEvent
     */
    public void actionPerformed(ActionEvent e) {
        JButton b = (JButton) e.getSource();
        if (b == playButton) {
            Concept concept = memory.nameToConcept(termField.getText().trim());
            if (concept != null) {
                EntityObserver entityObserver = new ConceptWindow(concept);
                StartPlay.startPlayConcept(concept, entityObserver, true);
            }
        } else if (b == hideButton) {
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
}
