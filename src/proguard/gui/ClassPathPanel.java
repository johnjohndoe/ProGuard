/* $Id: ClassPathPanel.java,v 1.8 2003/12/19 23:15:20 eric Exp $
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

import java.awt.*;
import java.awt.event.*;
import java.io.File;

import javax.swing.*;

import proguard.*;

/**
 * This <code>ListPanel</code> allows the user to add, edit, filter, move, and
 * remove ClassPathEntry objects in a ClassPath object.
 *
 * @author Eric Lafortune
 */
class ClassPathPanel extends ListPanel
{
    private JFrame       owner;
    private JFileChooser chooser;
    private StringDialog filterDialog;


    public ClassPathPanel(JFrame owner)
    {
        super();

        this.owner = owner;

        list.setCellRenderer(new MyListCellRenderer());

        chooser = new JFileChooser("");
        chooser.setMultiSelectionEnabled(true);
        chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        chooser.addChoosableFileFilter(
            new ExtensionFileFilter(GUIResources.getMessage("jarZipExtensions"),
                                    new String[] { ".jar", ".zip" }));
        chooser.setApproveButtonText(GUIResources.getMessage("ok"));

        filterDialog = new StringDialog(owner, GUIResources.getMessage("enterFilter"));

        addAddButton();
        addEditButton();
        addFilterButton();
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
                chooser.setDialogTitle(GUIResources.getMessage("addJars"));
                chooser.setSelectedFile(null);
                chooser.setSelectedFiles(null);

                int returnValue = chooser.showOpenDialog(owner);
                if (returnValue == JFileChooser.APPROVE_OPTION)
                {
                    File[] selectedFiles = chooser.getSelectedFiles();
                    ClassPathEntry[] entries = classPathEntries(selectedFiles);

                    // Add the new elements.
                    addElements(entries);
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
                int[] selectedIndices = list.getSelectedIndices();

                // Copy the Object array into a File array.
                File[] selectedFiles = new File[selectedIndices.length];
                for (int index = 0; index < selectedFiles.length; index++)
                {
                    ClassPathEntry entry =
                        (ClassPathEntry)listModel.getElementAt(selectedIndices[index]);

                    selectedFiles[index] = new File(entry.getName());
                }

                chooser.setDialogTitle(GUIResources.getMessage("chooseJars"));

                // Up to JDK 1.3.1, setSelectedFiles doesn't show in the file
                // chooser, so we just use setSelectedFile first. It also sets
                // the current directory.
                chooser.setSelectedFile(selectedFiles[0]);
                chooser.setSelectedFiles(selectedFiles);

                int returnValue = chooser.showOpenDialog(owner);
                if (returnValue == JFileChooser.APPROVE_OPTION)
                {
                    selectedFiles = chooser.getSelectedFiles();
                    ClassPathEntry[] entries = classPathEntries(selectedFiles);

                    // If there are the same number of files selected now as
                    // there were before, we can just replace the old ones.
                    if (selectedIndices.length == selectedFiles.length)
                    {
                        // Replace the old elements.
                        setElementsAt(entries, selectedIndices);
                    }
                    else
                    {
                        // Remove the old elements.
                        removeElementsAt(selectedIndices);

                        // Add the new elements.
                        addElements(entries);
                    }
                }
            }
        });

        addButton(editButton);
    }


    protected void addFilterButton()
    {
        JButton filterButton = new JButton(GUIResources.getMessage("filter"));
        filterButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                if (!list.isSelectionEmpty())
                {
                    int[] selectedIndices = list.getSelectedIndices();

                    ClassPathEntry firstEntry =
                        (ClassPathEntry)listModel.getElementAt(selectedIndices[0]);
                    filterDialog.setString(firstEntry.getFilter());

                    int returnValue = filterDialog.showDialog();
                    if (returnValue == StringDialog.APPROVE_OPTION)
                    {
                        setFiltersAt(selectedIndices,
                                     filterDialog.getString());
                    }
                }
            }
        });

        addButton(filterButton);
    }


    /**
     * Sets the ClassPath to be represented in this panel.
     *
     * @param elementList
     */
    public void setClassPath(ClassPath classPath)
    {
        listModel.clear();

        if (classPath != null)
        {
            for (int index = 0; index < classPath.size(); index++)
            {
                listModel.addElement(classPath.get(index));
            }
        }

        // Make sure the selection buttons are properly enabled,
        // since the clear method doesn't seem to notify the listener.
        enableSelectionButtons();
    }


    /**
     * Returns the ClassPath currently represented in this panel.
     */
    public ClassPath getClassPath()
    {
        int size = listModel.size();
        if (size == 0)
        {
            return null;
        }
        
        ClassPath classPath = new ClassPath();
        for (int index = 0; index < size; index++)
        {
            classPath.add((ClassPathEntry)listModel.get(index));
        }

        return classPath;
    }


    /**
     * Converts the given array of File objects into a corresponding array of
     * ClassPathEntry objects.
     */
    private ClassPathEntry[] classPathEntries(File[] files)
    {
        ClassPathEntry[] entries = new ClassPathEntry[files.length];
        for (int index = 0; index < entries.length; index++)
        {
            entries[index] = new ClassPathEntry(files[index].toString());
        }
        return entries;
    }


    /**
     * Applies the given filter to the specified class path entries.
     * Any previously set filter is discarded.
     */
    private void setFiltersAt(int[] indices, String filter)
    {
        for (int index = indices.length - 1; index >= 0; index--)
        {
            ClassPathEntry entry = (ClassPathEntry)listModel.get(indices[index]);
            entry.setFilter(filter);
        }

        // Make sure they are selected and thus repainted.
        list.setSelectedIndices(indices);
    }


    /**
     * This ListCellRenderer renders ClassPathEntry objects.
     */
    private class MyListCellRenderer implements ListCellRenderer
    {
        JPanel cellPanel    = new JPanel(new GridBagLayout());
        JLabel jarNameLabel = new JLabel("", JLabel.RIGHT);
        JLabel filterLabel  = new JLabel("", JLabel.RIGHT);


        public MyListCellRenderer()
        {
            GridBagConstraints jarNameLabelConstraints = new GridBagConstraints();
            jarNameLabelConstraints.anchor             = GridBagConstraints.WEST;
            jarNameLabelConstraints.insets             = new Insets(1, 2, 1, 2);

            GridBagConstraints filterLabelConstraints  = new GridBagConstraints();
            filterLabelConstraints.gridwidth           = GridBagConstraints.REMAINDER;
            filterLabelConstraints.fill                = GridBagConstraints.HORIZONTAL;
            filterLabelConstraints.weightx             = 1.0;
            filterLabelConstraints.anchor              = GridBagConstraints.EAST;
            filterLabelConstraints.insets              = jarNameLabelConstraints.insets;

            cellPanel.add(jarNameLabel, jarNameLabelConstraints);
            cellPanel.add(filterLabel,  filterLabelConstraints);
        }


        // Implementations for ListCellRenderer.

        public Component getListCellRendererComponent(JList   list,
                                                      Object  value,
                                                      int     index,
                                                      boolean isSelected,
                                                      boolean cellHasFocus)
        {
            ClassPathEntry entry = (ClassPathEntry)value;

            jarNameLabel.setText(entry.getName());
            filterLabel.setText(entry.getFilter() != null ?
                                ("(" + entry.getFilter() + ")") :
                                "");

            if (isSelected)
            {
                cellPanel.setBackground(list.getSelectionBackground());
                jarNameLabel.setForeground(list.getSelectionForeground());
                filterLabel.setForeground(list.getSelectionForeground());
            }
            else
            {
                cellPanel.setBackground(list.getBackground());
                jarNameLabel.setForeground(list.getForeground());
                filterLabel.setForeground(list.getForeground());
            }

            cellPanel.setOpaque(true);

            return cellPanel;
        }
    }
}
