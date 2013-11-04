/* $Id: KeepClassMemberOptionsPanel.java,v 1.6 2003/12/19 23:15:20 eric Exp $
 *
 * ProGuard -- obfuscation and shrinking package for Java class files.
 *
 * Copyright (c) 2002-2003 Eric Lafortune (eric@graphics.cornell.edu)
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
 * KeepClassMemberOption entries in a list.
 *
 * @author Eric Lafortune
 */
class KeepClassMemberOptionsPanel extends ListPanel
{
    private KeepClassMemberOptionDialog keepFieldDialog;
    private KeepClassMemberOptionDialog keepMethodDialog;


    public KeepClassMemberOptionsPanel(JDialog owner)
    {
        super();

        firstSelectionButton = 3;

        list.setCellRenderer(new MyListCellRenderer());

        keepFieldDialog  = new KeepClassMemberOptionDialog(owner, true);
        keepMethodDialog = new KeepClassMemberOptionDialog(owner, false);

        addAddFieldButton();
        addAddMethodButton();
        addEditButton();
        addRemoveButton();
        addUpButton();
        addDownButton();

        enableSelectionButtons();
    }


    protected void addAddFieldButton()
    {
        JButton addFieldButton = new JButton(GUIResources.getMessage("addField"));
        addFieldButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                keepFieldDialog.setKeepClassMemberOption(new KeepClassMemberOption());
                int returnValue = keepFieldDialog.showDialog();
                if (returnValue == KeepClassMemberOptionDialog.APPROVE_OPTION)
                {
                    // Add the new element.
                    addElement(new MyKeepClassMemberOptionWrapper(keepFieldDialog.getKeepClassMemberOption(),
                                                                  true));
                }
            }
        });

        addButton(addFieldButton);
    }


    protected void addAddMethodButton()
    {
        JButton addMethodButton = new JButton(GUIResources.getMessage("addMethod"));
        addMethodButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                keepMethodDialog.setKeepClassMemberOption(new KeepClassMemberOption());
                int returnValue = keepMethodDialog.showDialog();
                if (returnValue == KeepClassMemberOptionDialog.APPROVE_OPTION)
                {
                    // Add the new element.
                    addElement(new MyKeepClassMemberOptionWrapper(keepMethodDialog.getKeepClassMemberOption(),
                                                                  false));
                }
            }
        });

        addButton(addMethodButton);
    }


    protected void addEditButton()
    {
        JButton editButton = new JButton(GUIResources.getMessage("edit"));
        editButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                MyKeepClassMemberOptionWrapper wrapper =
                    (MyKeepClassMemberOptionWrapper)list.getSelectedValue();

                KeepClassMemberOptionDialog keepClassMemberOptionDialog =
                    wrapper.isField ?
                        keepFieldDialog :
                        keepMethodDialog;

                keepClassMemberOptionDialog.setKeepClassMemberOption(wrapper.keepClassMemberOption);
                int returnValue = keepClassMemberOptionDialog.showDialog();
                if (returnValue == KeepClassMemberOptionDialog.APPROVE_OPTION)
                {
                    // Replace the old element.
                    wrapper.keepClassMemberOption = keepClassMemberOptionDialog.getKeepClassMemberOption();
                    setElementAt(wrapper,
                                 list.getSelectedIndex());
                }
            }
        });

        addButton(editButton);
    }


    /**
     * Sets the KeepClassMemberOption objects to be represented in this panel.
     *
     * @param elementList
     */
    public void setKeepClassMemberOptions(List keepFieldOptions,
                                          List keepMethodOptions)
    {
        listModel.clear();

        if (keepFieldOptions != null)
        {
            for (int index = 0; index < keepFieldOptions.size(); index++)
            {
                listModel.addElement(
                    new MyKeepClassMemberOptionWrapper((KeepClassMemberOption)keepFieldOptions.get(index),
                                                       true));
            }
        }

        if (keepMethodOptions != null)
        {
            for (int index = 0; index < keepMethodOptions.size(); index++)
            {
                listModel.addElement(
                    new MyKeepClassMemberOptionWrapper((KeepClassMemberOption)keepMethodOptions.get(index),
                                                       false));
            }
        }

        // Make sure the selection buttons are properly enabled,
        // since the clear method doesn't seem to notify the listener.
        enableSelectionButtons();
    }


    /**
     * Returns the KeepClassMemberOption objects currently represented in this
     * panel that refer to fields or to methods.
     *
     * @param isField specifies whether options referring to fields or options
     *                refering to method should be returned.
     */
    public List getKeepKeepClassMemberOptions(boolean isField)
    {
        int size = listModel.size();
        if (size == 0)
        {
            return null;
        }
        
        List keepClassMemberOptions = new ArrayList(size);
        for (int index = 0; index < size; index++)
        {
            MyKeepClassMemberOptionWrapper wrapper =
                (MyKeepClassMemberOptionWrapper)listModel.get(index);

            if (wrapper.isField == isField)
            {
                keepClassMemberOptions.add(wrapper.keepClassMemberOption);
            }
        }

        return keepClassMemberOptions;
    }


    /**
     * This ListCellRenderer renders KeepClassMemberOption objects.
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
            MyKeepClassMemberOptionWrapper wrapper = (MyKeepClassMemberOptionWrapper)value;

            KeepClassMemberOption option = wrapper.keepClassMemberOption;
            String name = option.name;
            label.setText(wrapper.isField ?
                (name == null ? "<fields>"  : ClassUtil.externalFullFieldDescription(0, option.name, option.descriptor)) :
                (name == null ? "<methods>" : ClassUtil.externalFullMethodDescription("<init>", 0, option.name, option.descriptor)));

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


    /**
     * This class wraps a KeepClassMemberOption, additionally storing whether
     * the option refers to a field or to a method.
     */
    private class MyKeepClassMemberOptionWrapper
    {
        public KeepClassMemberOption keepClassMemberOption;
        public boolean               isField;

        public MyKeepClassMemberOptionWrapper(KeepClassMemberOption keepClassMemberOption,
                                              boolean               isField)
        {
            this.keepClassMemberOption = keepClassMemberOption;
            this.isField               = isField;
        }
    }
}
