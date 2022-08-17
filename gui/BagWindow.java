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

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Scrollbar;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.WindowEvent;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import nars.entity.Item;
import nars.main.Parameters;
import nars.storage.Bag;
import nars.storage.BagObserver;



/**
 * JWindow display the priority distribution of items within a given bag
 */
public class BagWindow<BagType extends Item> extends NarsFrame implements ActionListener, AdjustmentListener,
		BagObserver<BagType> {

    /**
     * The bag to be displayed
     */
    private Bag<?> bag;
    /**
     * Control buttons
     */
    private final JButton playButton, stopButton, closeButton;
    /**
     * Display area
     */
    private final JTextArea text;
    /**
     * Display label
     */
    private final JLabel valueLabel;
    /**
     * Adjustable display level
     */
    private final JScrollBar valueBar;
    /**
     * The location of the display area, shifted according to the number of
     * windows opened
     */
    private static int counter;
    /**
     * whether this bag window is active
     */
    private boolean showing;

    public BagWindow() {
        /* The lowest level displayed */
        int showLevel = Parameters.BAG_THRESHOLD;
        getContentPane().setBackground(MULTIPLE_WINDOW_COLOR);

        text = new JTextArea("");
        text.setBackground(DISPLAY_BACKGROUND_COLOR);
        text.setEditable(false);
        JScrollPane textScrollPane = new JScrollPane(text);
        valueLabel = new JLabel( "00", JLabel.RIGHT);
        valueBar = new JScrollBar(Scrollbar.HORIZONTAL, showLevel, 0, 1, Parameters.BAG_LEVEL);
        valueBar.addAdjustmentListener(this);
        stopButton = new JButton(NarsFrame.OFF_LABEL);
        stopButton.addActionListener(this);
        playButton = new JButton(NarsFrame.ON_LABEL);
        playButton.addActionListener(this);
        closeButton = new JButton("Close");
        closeButton.addActionListener(this);
        
        applyBorderLayout(textScrollPane);

        setBounds(600, 60 + counter * 40, 600, 300);
        counter++;        
        adjustLabelAndCursor(showLevel);
        setVisible(true);
    }

	private void applyBorderLayout(JScrollPane textScrollPane) {
		setLayout(new BorderLayout());
        add(textScrollPane, BorderLayout.CENTER );
        JPanel bottomPanel = new JPanel();
        add( bottomPanel, BorderLayout.SOUTH );
        bottomPanel.add(valueLabel );
        bottomPanel.add(valueBar );
        bottomPanel.add(playButton );
        bottomPanel.add(stopButton );
        bottomPanel.add(closeButton );
	}

    private void adjustLabelAndCursor(int showLevel) {
        String valueText = String.valueOf(showLevel);
        // always occupy 3 characters (padding):
        valueText = showLevel> 9 ? "0" + valueText : "00" + valueText;
        valueText = showLevel > 99 ? ""+showLevel : valueText;
		valueLabel.setText(valueText);
        valueBar.setValue(showLevel);		
	}

	@Override
    public void post(String str) {
        showing = true;
        text.setText(str);
    }

    /**
     * Handling button click
     *
     * @param e The ActionEvent
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        Object source = e.getSource();
        if (source == playButton) {
            bag.play();
        } else if (source == stopButton) {
            bag.stop();
        } else if (source == closeButton) {
            close();
        }
    }

    /**
     * Close the window
     */
    private void close() {
        bag.stop();
        dispose();
        counter--;
    }

    @Override
    public void windowClosing(WindowEvent arg0) {
        close();
    }

    /**
     * Handling scrollbar movement
     *
     * @param e The AdjustmentEvent
     */
    @Override
    public void adjustmentValueChanged(AdjustmentEvent e) {
        if (e.getSource() == valueBar) {
            int showLevel = valueBar.getValue();
            adjustLabelAndCursor(showLevel);
            bag.setShowLevel(showLevel);
            bag.play();
        }
    }

    @Override
	public void setBag( Bag<BagType> bag ) {
        this.bag = bag;
    }

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

	@SuppressWarnings("unused")
	private void applyGridBagLayout(JScrollPane textScrollPane) {
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
	    gridbag.setConstraints(textScrollPane, c);
	    add(textScrollPane);   
	    c.weighty = 0.0;
	    c.gridwidth = 1;
	    gridbag.setConstraints(valueLabel, c);
	    add(valueLabel);
	    gridbag.setConstraints(valueBar, c);
	    add(valueBar);
	    gridbag.setConstraints(playButton, c);
	    add(playButton);
	    gridbag.setConstraints(stopButton, c);
	    add(stopButton);
	    gridbag.setConstraints(closeButton, c);
	    add(closeButton);
	}
}
