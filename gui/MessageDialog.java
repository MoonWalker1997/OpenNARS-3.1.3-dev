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

/**
 * Pop-up message for the user to accept
 */
public class MessageDialog extends JDialog implements ActionListener, WindowListener {

    protected JButton button;
    protected JTextArea text;

    /**
     * Constructor
     * @param parent The parent Frame
     * @param message The text to be displayed
     */
    public MessageDialog(Frame parent, String message) {
        super(parent, "Message", false);
        setLayout(new BorderLayout(5, 5));
        getContentPane().setBackground(NarsFrame.SINGLE_WINDOW_COLOR);
        setFont(NarsFrame.NarsFont);
        text = new JTextArea(message);
        text.setBackground(NarsFrame.DISPLAY_BACKGROUND_COLOR);
        this.add("Center", text);
        button = new JButton(" OK ");
        button.addActionListener(this);
        JPanel p = new JPanel();
        p.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
        p.add(button);
        this.add("South", p);
        setModal(true);
        setBounds(200, 250, 400, 180);
        addWindowListener(this);
        setVisible(true);
    }

    /**
     * Handling button click
     * @param e The ActionEvent
     */
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == button) {
            close();
        }
    }

    private void close() {
        this.setVisible(false);
        this.dispose();
    }

    @Override
    public void windowActivated(WindowEvent arg0) {
    }

    @Override
    public void windowClosed(WindowEvent arg0) {
    }

    @Override
    public void windowClosing(WindowEvent arg0) {
        close();
    }

    @Override
    public void windowDeactivated(WindowEvent arg0) {
    }

    @Override
    public void windowDeiconified(WindowEvent arg0) {
    }

    @Override
    public void windowIconified(WindowEvent arg0) {
    }

    @Override
    public void windowOpened(WindowEvent arg0) {
    }
}
