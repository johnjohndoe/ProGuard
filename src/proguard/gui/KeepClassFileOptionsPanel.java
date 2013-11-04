/* $Id: KeepClassFileOptionsPanel.java,v 1.5 2003/12/19 23:15:20 eric Exp $
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

import proguard.*;
import proguard.classfile.util.ClassUtil;

import java.awt.Component;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;


/**
 * This <code>ListPanel</code> allows the user to add, edit, move, and remove
 * KeepClassFileOption entries in a list.
 *
 * @author Eric Lafortune
 */
class KeepClassFileOptionsPanel extends ListPanel
{
    private KeepClassFileOptionDialog keepClassFileOptionDialog;


    public KeepClassFileOptionsPanel(JFrame owner)
    {
        super();

        list.setCellRenderer(new MyListCellRenderer());

        keepClassFileOptionDialog = new KeepClassFileOptionDialog(owner);

        addAddButton();
        addEditButton();
        addRemoveButton();
        addUpButton();
        addDownButton();

        enableSelectionButtons();
    }


    protected void addAddButton()
    {
        JButton addButton = new JButton(GUIResources.getMessage("add"));
        addButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                keepClassFileOptionDialog.setKeepClassFileOption(new KeepClassFileOption());
                int returnValue = keepClassFileOptionDialog.showDialog();
                if (returnValue == KeepClassFileOptionDialog.APPROVE_OPTION)
                {
                    // Add the new element.
                    addElement(keepClassFileOptionDialog.getKeepClassFileOption());
                }
            }
        });

        addButton(addButton);
    }


    protected void addEditButton()
    {
        JButton editButton = new JButton(GUIResources.getMessage("edit"));
        editButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                KeepClassFileOption selectedKeepClassFileOption =
                    (KeepClassFileOption)list.getSelectedValue();

                keepClassFileOptionDialog.setKeepClassFileOption(selectedKeepClassFileOption);
                int returnValue = keepClassFileOptionDialog.showDialog();
                if (returnValue == KeepClassFileOptionDialog.APPROVE_OPTION)
                {
                    // Replace the old element.
                    setElementAt(keepClassFileOptionDialog.getKeepClassFileOption(),
                                 list.getSelectedIndex());
                }
            }
        });

        addButton(editButton);
    }


    /**
     * Sets the KeepClassFileOption objects to be represented in this panel.
     */
    public void setKeepClassFileOptions(List keepClassFileOptions)
    {
        listModel.clear();

        if (keepClassFileOptions != null)
        {
            for (int index = 0; index < keepClassFileOptions.size(); index++)
            {
                listModel.addElement(keepClassFileOptions.get(index));
            }
        }

        // Make sure the selection buttons are properly enabled,
        // since the clear method doesn't seem to notify the listener.
        enableSelectionButtons();
    }


    /**
     * Returns the KeepClassFileOption objects currently represented in this panel.
     */
    public List getKeepClassFileOptions()
    {
        int size = listModel.size();
        if (size == 0)
        {
            return null;
        }
        
        List keepClassFileOptions = new ArrayList(size);
        for (int index = 0; index < size; index++)
        {
            keepClassFileOptions.add((KeepClassFileOption)listModel.get(index));
        }

        return keepClassFileOptions;
    }


    /**
     * This ListCellRenderer renders KeepClassFileOption objects.
     */
    private class MyListCellRenderer implements ListCellRenderer
    {
        JLabel label = new JLabel();


        // Implementations for ListCellRenderer.

        public Component getListCellRendererComponent(JList   list,
                                                      Object  value,
                                                      int     index,
                                                      boolean isSelected,
                                                      boolean cellHasFocus)
        {
            KeepClassFileOption option = (KeepClassFileOption)value;

            String comments = option.comments;

            label.setText(comments                 != null ? comments.trim()                                                           :
                          option.className         != null ? (GUIResources.getMessage("class") + " " + ClassUtil.externalClassName(option.className))                :
                          option.extendsClassName  != null ? (GUIResources.getMessage("extensionsOf") + ClassUtil.externalClassName(option.extendsClassName)) :
                                                             (GUIResources.getMessage("specificationNumber") + index));

            if (isSelected)
            {
                label.setBackground(list.getSelectionBackground());
                label.setForeground(list.getSelectionForeground());
            }
            else
            {
                label.setBackground(list.getBackground());
                label.setForeground(list.getForeground());
            }

            label.setOpaque(true);

            return label;
        }
    }
}
