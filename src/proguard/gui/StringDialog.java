/* $Id: StringDialog.java,v 1.3 2003/12/06 22:12:42 eric Exp $
 *
 * ProGuard -- obfuscation and shrinking package for Java class files.
 *
 * Copyright (c) 2002-2004 Eric Lafortune (eric@graphics.cornell.edu)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package proguard.gui;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

/**
 * This <code>JDialog</code> allows the user to enter a String.
 *
 * @author Eric Lafortune
 */
public class StringDialog extends JDialog
{
    /**
     * Return value if the dialog is canceled (with the Cancel button or by
     * closing the dialog window).
     */
    public static final int CANCEL_OPTION = 1;

    /**
     * Return value if the dialog is approved (with the Ok button).
     */
    public static final int APPROVE_OPTION = 0;


    private JTextField textField = new JTextField(40);
    private int        returnValue;


    public StringDialog(JFrame owner,
                        String explanation)
    {
        super(owner, true);
        setResizable(true);

        GridBagConstraints textConstraints = new GridBagConstraints();
        textConstraints.gridwidth = GridBagConstraints.REMAINDER;
        textConstraints.fill      = GridBagConstraints.HORIZONTAL;
        textConstraints.weightx   = 1.0;
        textConstraints.weighty   = 1.0;
        textConstraints.anchor    = GridBagConstraints.NORTHWEST;
        textConstraints.insets    = new Insets(10, 10, 10, 10);

        GridBagConstraints cancelButtonConstraints = new GridBagConstraints();
        cancelButtonConstraints.weightx = 1.0;
        cancelButtonConstraints.weighty = 1.0;
        cancelButtonConstraints.anchor  = GridBagConstraints.SOUTHEAST;
        cancelButtonConstraints.insets  = new Insets(4, 4, 8, 4);

        GridBagConstraints okButtonConstraints = new GridBagConstraints();
        okButtonConstraints.gridwidth = GridBagConstraints.REMAINDER;;
        okButtonConstraints.weighty   = 1.0;
        okButtonConstraints.anchor    = GridBagConstraints.SOUTHEAST;
        okButtonConstraints.insets    = cancelButtonConstraints.insets;

        JTextArea explanationTextArea = new JTextArea(explanation, 3, 0);
        explanationTextArea.setOpaque(false);
        explanationTextArea.setEditable(false);
        explanationTextArea.setLineWrap(true);
        explanationTextArea.setWrapStyleWord(true);

        JButton cancelButton = new JButton(GUIResources.getMessage("cancel"));
        cancelButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                hide();
            }
        });

        JButton okButton = new JButton(GUIResources.getMessage("ok"));
        okButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                returnValue = APPROVE_OPTION;
                hide();
            }
        });

        JPanel filterPanel = new JPanel(new GridBagLayout());
        filterPanel.add(explanationTextArea, textConstraints);
        filterPanel.add(textField,           textConstraints);
        filterPanel.add(cancelButton,        cancelButtonConstraints);
        filterPanel.add(okButton,            okButtonConstraints);

        getContentPane().add(filterPanel);
    }


    /**
     * Sets the String to be represented in this dialog.
     */
    public void setString(String string)
    {
        textField.setText(string);
    }


    /**
     * Returns String currently represented in this dialog.
     */
    public String getString()
    {
        return textField.getText();
    }


    /**
     * Shows this dialog. This method only returns when the dialog is closed.
     *
     * @return <code>CANCEL_OPTION</code> or <code>APPROVE_OPTION</code>,
     *         depending on the choice of the user.
     */
    public int showDialog()
    {
        returnValue = CANCEL_OPTION;

        // Open the dialog in the right place, then wait for it to be closed,
        // one way or another.
        pack();
        setLocationRelativeTo(getOwner());
        show();

        return returnValue;
    }
}
