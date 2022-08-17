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

import nars.entity.Concept;
import nars.entity.EntityObserver;
import nars.storage.BagObserver;
//import java.beans.PropertyChangeEvent;
//import java.beans.PropertyChangeListener;

/**
 * JWindow displaying the content of a Concept, such as beliefs, goals, and
 * questions
 */
public class ConceptWindow extends NarsFrame implements ActionListener, EntityObserver {

    /**
     * Control buttons
     */
    private final JButton playButton, stopButton, playInNewWindowButton, closeButton;
    /**
     * Display area
     */
    private final JTextArea text;
    /**
     * The concept to be displayed
     */
    private final Concept concept;
    /**
     * Whether the content of the concept is being displayed
     */
    private boolean showing = false;
    /**
     * Used to adjust the screen position
     */
    private static int instanceCount = 0;

    /**
     * Constructor
     *
     * @param concept The concept to be displayed
     */
    public ConceptWindow(Concept concept) {
        super(concept.getKey());
        this.concept = concept;
        getContentPane().setBackground(MULTIPLE_WINDOW_COLOR);
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
        text = new JTextArea("");
        text.setBackground(DISPLAY_BACKGROUND_COLOR);
        text.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(text);
        gridbag.setConstraints(scrollPane, c);
        add(scrollPane);

        c.weighty = 0.0;
        c.gridwidth = 1;
        playButton = new JButton(ON_LABEL);
        gridbag.setConstraints(playButton, c);
        playButton.addActionListener(this);
        add(playButton);

        stopButton = new JButton(OFF_LABEL);
        gridbag.setConstraints(stopButton, c);
        stopButton.addActionListener(this);
        add(stopButton);

        playInNewWindowButton = new JButton("Play in New Window");
        gridbag.setConstraints(playInNewWindowButton, c);
        playInNewWindowButton.addActionListener(this);
        add(playInNewWindowButton);

        closeButton = new JButton("Close");
        gridbag.setConstraints(closeButton, c);
        closeButton.addActionListener(this);
        add(closeButton);

        // Offset the screen location of each new instance.
        setBounds(600 + (instanceCount % 10) * 20, 60 + (instanceCount % 10) * 20, 600, 270);
        ++instanceCount;
        setVisible(true);
    }

    /* (non-Javadoc)
     * @see nars.gui.EntityObserver#post(java.lang.String)
     */
    @Override
    public void post(String str) {
        showing = true;
        text.setText(str);
    }

    /**
     * This is called when Concept removes this as its window.
     */
    public void detachFromConcept() {
        // The Play and Stop buttons and Derivation checkbox no longer do anything, so disable.
        playButton.setEnabled(false);
        stopButton.setEnabled(false);
    }

    /**
     * Handling button click
     *
     * @param e The ActionEvent
     */
    @Override
	public void actionPerformed(ActionEvent e) {
        Object s = e.getSource();
        if (s == playButton) {
            StartPlay.playConcept(concept);
        } else if (s == stopButton) {
            StartPlay.stopConcept(concept);
        } else if (s == playInNewWindowButton) {
            StartPlay.stopConcept(concept);
            EntityObserver entityObserver = new ConceptWindow(concept);
            StartPlay.startPlayConcept(concept, entityObserver, false);
        } else if (s == closeButton) {
            close();
        }
    }

    private void close() {
        StartPlay.stopConcept(concept);
        dispose();
    }

    @Override
    public void windowClosing(WindowEvent e) {
        close();
    }

    @Override
	public BagObserver<Concept> createBagObserver() {
		return new BagWindow<Concept>();
    }

    @Override
    public void startPlay(Concept concept, boolean showLinks) {
        if (this.isVisible()) {
            this.detachFromConcept();
        }
        showing = true;
        this.post(concept.toStringConceptContent());
    }

    /**
     * Refresh display if in showing state
     */
    @Override
    public void refresh(String message) {
        if (showing) {
            post(message);
        }
    }

    @Override
    public void stop() {
        showing = false;
    }
}
