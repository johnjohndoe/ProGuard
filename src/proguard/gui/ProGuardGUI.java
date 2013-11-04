/* $Id: ProGuardGUI.java,v 1.15 2003/12/13 20:09:41 eric Exp $
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
import proguard.util.*;
import proguard.classfile.util.*;
import proguard.gui.splash.*;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.List;


/**
 * GUI for configuring and executing ProGuard and ReTrace.
 *
 * @author Eric Lafortune
 */
public class ProGuardGUI extends JFrame
{
    private static final String NO_SPLASH_OPTION = "-nosplash";

    private static final String TITLE_IMAGE_FILE            = "vtitle.gif";
    private static final String BOILERPLATE_KEEP_CLASS_FILE = "boilerplate.pro";
    private static final String DEFAULT_CONFIGURATION       = "default.pro";

    private static final Border BORDER = BorderFactory.createEtchedBorder(EtchedBorder.RAISED);

    static boolean systemOutRedirected;

    private JFileChooser configurationChooser = new JFileChooser("");
    private JFileChooser fileChooser          = new JFileChooser("");

    private SplashPanel splashPanel;

    private ClassPathPanel programPanel  = new ClassPathPanel(this);
    private ClassPathPanel resourcePanel = new ClassPathPanel(this);
    private ClassPathPanel libraryPanel  = new ClassPathPanel(this);
    private ClassPathPanel outPanel      = new ClassPathPanel(this);

    private KeepClassFileOption[] boilerplateKeepClassFileOptions;
    private JCheckBox[]           boilerplateKeepCheckBoxes;
    private JTextField[]          boilerplateKeepTextFields;

    private KeepClassFileOptionsPanel specialKeepPanel = new KeepClassFileOptionsPanel(this);

    private JCheckBox shrinkCheckBox                      = new JCheckBox(msg("shrink"));
    private JCheckBox printUsageCheckBox                  = new JCheckBox(msg("printUsage"));

    private JCheckBox obfuscateCheckBox                   = new JCheckBox(msg("obfuscate"));
    private JCheckBox printMappingCheckBox                = new JCheckBox(msg("printMapping"));
    private JCheckBox applyMappingCheckBox                = new JCheckBox(msg("applyMapping"));
    private JCheckBox useMixedCaseClassNamesCheckBox      = new JCheckBox(msg("useMixedCaseClassNames"));
    private JCheckBox overloadAggressivelyCheckBox        = new JCheckBox(msg("overloadAggressively"));
    private JCheckBox defaultPackageCheckBox              = new JCheckBox(msg("defaultPackage"));
    private JCheckBox keepAttributesCheckBox              = new JCheckBox(msg("keepAttributes"));
    private JCheckBox newSourceFileAttributeCheckBox      = new JCheckBox(msg("renameSourceFileAttribute"));

    private JCheckBox verboseCheckBox                     = new JCheckBox(msg("verbose"));
    private JCheckBox printSeedsCheckBox                  = new JCheckBox(msg("printSeeds"));
    private JCheckBox ignoreWarningsCheckBox              = new JCheckBox(msg("ignoreWarnings"));
    private JCheckBox warnCheckBox                        = new JCheckBox(msg("warn"));
    private JCheckBox noteCheckBox                        = new JCheckBox(msg("note"));
    private JCheckBox skipNonPublicLibraryClassesCheckBox = new JCheckBox(msg("skipNonPublicLibraryClasses"));

    private JTextField printUsageTextField                = new JTextField(40);
    private JTextField printMappingTextField              = new JTextField(40);
    private JTextField applyMappingTextField              = new JTextField(40);
    private JTextField defaultPackageTextField            = new JTextField(40);
    private JTextField keepAttributesTextField            = new JTextField(40);
    private JTextField newSourceFileAttributeTextField    = new JTextField(40);
    private JTextField printSeedsTextField                = new JTextField(40);

    private JTextArea  consoleTextArea                    = new JTextArea(msg("processingInfo"), 3, 40);

    private JCheckBox  reTraceVerboseCheckBox             = new JCheckBox(msg("verbose"));
    private JTextField reTraceMappingTextField            = new JTextField(40);
    private JTextArea  stackTraceTextArea                 = new JTextArea(3, 40);
    private JTextArea  reTraceTextArea                    = new JTextArea(msg("reTraceInfo"), 3, 40);


    /**
     * Creates a new ProGuardGUI.
     */
    public ProGuardGUI()
    {
        setTitle("ProGuard");
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        // Create some constraints that can be reused.
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.anchor    = GridBagConstraints.WEST;
        constraints.insets    = new Insets(0, 4, 0, 4);

        GridBagConstraints constraintsStretch = new GridBagConstraints();
        constraintsStretch.fill      = GridBagConstraints.HORIZONTAL;
        constraintsStretch.weightx   = 1.0;
        constraintsStretch.anchor    = GridBagConstraints.WEST;
        constraintsStretch.insets    = constraints.insets;

        GridBagConstraints constraintsLast = new GridBagConstraints();
        constraintsLast.gridwidth = GridBagConstraints.REMAINDER;
        constraintsLast.anchor    = GridBagConstraints.WEST;
        constraintsLast.insets    = constraints.insets;

        GridBagConstraints constraintsLastStretch = new GridBagConstraints();
        constraintsLastStretch.gridwidth = GridBagConstraints.REMAINDER;
        constraintsLastStretch.fill      = GridBagConstraints.HORIZONTAL;
        constraintsLastStretch.weightx   = 1.0;
        constraintsLastStretch.anchor    = GridBagConstraints.WEST;
        constraintsLastStretch.insets    = constraints.insets;

        GridBagConstraints welcomeTextAreaConstraints = new GridBagConstraints();
        welcomeTextAreaConstraints.gridwidth = GridBagConstraints.REMAINDER;
        welcomeTextAreaConstraints.fill      = GridBagConstraints.HORIZONTAL;
        welcomeTextAreaConstraints.weightx   = 1.0;
        welcomeTextAreaConstraints.anchor    = GridBagConstraints.NORTHWEST;
        welcomeTextAreaConstraints.insets    = new Insets(20, 40, 20, 40);

        GridBagConstraints panelConstraints = new GridBagConstraints();
        panelConstraints.gridwidth = GridBagConstraints.REMAINDER;
        panelConstraints.fill      = GridBagConstraints.HORIZONTAL;
        panelConstraints.weightx   = 1.0;
        panelConstraints.anchor    = GridBagConstraints.NORTHWEST;
        panelConstraints.insets    = constraints.insets;

        GridBagConstraints stretchPanelConstraints = new GridBagConstraints();
        stretchPanelConstraints.gridwidth = GridBagConstraints.REMAINDER;
        stretchPanelConstraints.fill      = GridBagConstraints.BOTH;
        stretchPanelConstraints.weightx   = 1.0;
        stretchPanelConstraints.weighty   = 1.0;
        stretchPanelConstraints.anchor    = GridBagConstraints.NORTHWEST;
        stretchPanelConstraints.insets    = constraints.insets;

        GridBagConstraints glueConstraints = new GridBagConstraints();
        glueConstraints.gridheight = GridBagConstraints.REMAINDER;
        glueConstraints.fill       = GridBagConstraints.BOTH;
        glueConstraints.weightx    = 0.01;
        glueConstraints.weighty    = 0.01;
        glueConstraints.anchor     = GridBagConstraints.NORTHWEST;
        glueConstraints.insets     = constraints.insets;

        GridBagConstraints bottomButtonConstraints = new GridBagConstraints();
        bottomButtonConstraints.anchor = GridBagConstraints.SOUTHEAST;
        bottomButtonConstraints.insets = new Insets(2, 2, 4, 6);
        bottomButtonConstraints.ipadx  = 10;
        bottomButtonConstraints.ipady  = 2;

        GridBagConstraints lastBottomButtonConstraints = new GridBagConstraints();
        lastBottomButtonConstraints.gridwidth = GridBagConstraints.REMAINDER;
        lastBottomButtonConstraints.anchor    = GridBagConstraints.SOUTHEAST;
        lastBottomButtonConstraints.insets    = bottomButtonConstraints.insets;
        lastBottomButtonConstraints.ipadx     = bottomButtonConstraints.ipadx;
        lastBottomButtonConstraints.ipady     = bottomButtonConstraints.ipady;

        GridBagLayout layout = new GridBagLayout();

        configurationChooser.addChoosableFileFilter(
            new ExtensionFileFilter(msg("proExtension"), new String[] { ".pro" }));

        // Create the opening panel.
        //JLabel titleLabel = new JLabel("ProGuard", JLabel.CENTER);
        //titleLabel.setFont(new Font("serif", Font.BOLD, 40));
        //titleLabel.setForeground(Color.gray);

        Font font = new Font("sansserif", Font.BOLD, 50);
        Color fontColor = Color.white;

        Sprite splash =
            new CompositeSprite(new Sprite[]
        {
            new TextSprite(new ConstantString("ProGuard"),
                           new ConstantFont(new Font("sansserif", Font.BOLD, 90)),
                           new ConstantColor(Color.gray),
                           new ConstantInt(160),
                           new LinearInt(-10, 100, new SmoothTiming(500, 1000))),

            new ShadowedSprite(new ConstantInt(3),
                               new ConstantInt(3),
                               new ConstantDouble(0.4),
                               new ConstantInt(2),
                               new CompositeSprite(new Sprite[]
            {
                new TextSprite(new ConstantString(msg("shrinking")),
                               new ConstantFont(font),
                               new ConstantColor(fontColor),
                               new LinearInt(1000, 100, new SmoothTiming(1000, 2000)),
                               new ConstantInt(50)),
                new TextSprite(new ConstantString(msg("obfuscation")),
                               new ConstantFont(font),
                               new ConstantColor(fontColor),
                               new LinearInt(1000, 350, new SmoothTiming(2000, 3000)),
                               new ConstantInt(120)),
                new TextSprite(new TypeWriterString(msg("developed"), new LinearTiming(3000, 5000)),
                               new ConstantFont(new Font("monospaced", Font.BOLD, 20)),
                               new ConstantColor(fontColor),
                               new ConstantInt(250),
                               new ConstantInt(150)),
            })),
        });
        splashPanel = new SplashPanel(splash, 0.5, 5000L);

        JTextArea welcomeTextArea = new JTextArea(msg("proGuardInfo"));
        welcomeTextArea.setOpaque(false);
        welcomeTextArea.setEditable(false);
        welcomeTextArea.setLineWrap(true);
        welcomeTextArea.setWrapStyleWord(true);
        welcomeTextArea.setBorder(new EmptyBorder(20, 20, 20, 20));
        addBorder(welcomeTextArea, "welcome");

        JPanel proGuardPanel = new JPanel(layout);
        proGuardPanel.add(splashPanel,      stretchPanelConstraints);
        proGuardPanel.add(welcomeTextArea,  welcomeTextAreaConstraints);
        proGuardPanel.add(Box.createGlue(), stretchPanelConstraints);

        // Create the input panel.
        // TODO: properly clone the ClassPath objects. This is awkward to
        // implement in the generic ListPanel.addElements(...) method, since
        // the Object.clone() method is not public.
        programPanel.addCopyToPanelButton(msg("moveToLibraries"), libraryPanel,  true);
        programPanel.addCopyToPanelButton(msg("copyToResources"), resourcePanel, false);
        libraryPanel.addCopyToPanelButton(msg("moveToProgram"),   programPanel,  true);

        // Collect all buttons of these panels and make sure they are equally
        // sized.
        List panelButtons = new ArrayList();
        panelButtons.addAll(programPanel .getButtons());
        panelButtons.addAll(resourcePanel.getButtons());
        panelButtons.addAll(libraryPanel .getButtons());
        setCommonPreferredSize(panelButtons);
        panelButtons = null;

        addBorder(programPanel,  "programJars" );
        addBorder(resourcePanel, "resourceJars");
        addBorder(libraryPanel,  "libraryJars" );
        addBorder(outPanel,      "outputJars"  );

        JPanel inputPanel = new JPanel(layout);
        inputPanel.add(programPanel,  stretchPanelConstraints);
        inputPanel.add(resourcePanel, stretchPanelConstraints);
        inputPanel.add(libraryPanel,  stretchPanelConstraints);

        // Create the keep panel.
        JPanel keepPanel = new JPanel(layout);

        // Create the boiler plate keep panels.
        loadBoilerplateKeepClassFileOptions();

        boilerplateKeepCheckBoxes = new JCheckBox[boilerplateKeepClassFileOptions.length];
        boilerplateKeepTextFields = new JTextField[boilerplateKeepClassFileOptions.length];

        String lastPanelName = null;
        JPanel keepSubpanel  = null;
        for (int index = 0; index < boilerplateKeepClassFileOptions.length; index++)
        {
            KeepClassFileOption keepClassFileOption =
                boilerplateKeepClassFileOptions[index];

            // The panel structure is derived from the comments.
            String comments = keepClassFileOption.comments;
            int dashIndex = comments.indexOf('-');
            int periodIndex = comments.indexOf('.', dashIndex);
            String panelName = comments.substring(0, dashIndex).trim();
            String optionName = comments.substring(dashIndex + 1, periodIndex).trim();
            if (!panelName.equals(lastPanelName))
            {
                // Create a new keep subpanel and add it.
                keepSubpanel = new JPanel(layout);
                String titleKey = panelName.toLowerCase().replace(' ', '_');
                addBorder(keepSubpanel, titleKey);
                keepPanel.add(keepSubpanel, panelConstraints);

                lastPanelName = panelName;
            }

            // Add the keep check box to the subpanel.
            String messageKey = optionName.toLowerCase().replace(' ', '_');
            boilerplateKeepCheckBoxes[index] = new JCheckBox(msg(messageKey));
            boilerplateKeepTextFields[index] = new JTextField(40);

            keepSubpanel.add(boilerplateKeepCheckBoxes[index], constraints);
            keepSubpanel.add(boilerplateKeepTextFields[index], constraintsLastStretch);
        }

        addBorder(specialKeepPanel, "special");

        keepPanel.add(specialKeepPanel, stretchPanelConstraints);

        // Create the output panel.
        JButton printUsageBrowseButton   = createBrowseButton(printUsageTextField,
                                                              msg("selectUsageFile"));
        JButton printMappingBrowseButton = createBrowseButton(printMappingTextField,
                                                              msg("selectPrintMappingFile"));
        JButton applyMappingBrowseButton = createBrowseButton(applyMappingTextField,
                                                              msg("selectApplyMappingFile"));
        JButton printSeedsBrowseButton   = createBrowseButton(printSeedsTextField,
                                                              msg("selectSeedsFile"));

        JPanel shrinkingPanel = new JPanel(layout);
        addBorder(shrinkingPanel, "shrinking");

        shrinkingPanel.add(shrinkCheckBox,                         constraintsLastStretch);
        shrinkingPanel.add(printUsageCheckBox,                     constraints);
        shrinkingPanel.add(printUsageTextField,                    constraintsStretch);
        shrinkingPanel.add(printUsageBrowseButton,                 constraintsLast);

        JPanel obfuscationPanel = new JPanel(layout);
        addBorder(obfuscationPanel, "obfuscation");

        obfuscationPanel.add(obfuscateCheckBox,                    constraintsLastStretch);
        obfuscationPanel.add(printMappingCheckBox,                 constraints);
        obfuscationPanel.add(printMappingTextField,                constraintsStretch);
        obfuscationPanel.add(printMappingBrowseButton,             constraintsLast);
        obfuscationPanel.add(applyMappingCheckBox,                 constraints);
        obfuscationPanel.add(applyMappingTextField,                constraintsStretch);
        obfuscationPanel.add(applyMappingBrowseButton,             constraintsLast);
        obfuscationPanel.add(useMixedCaseClassNamesCheckBox,       constraintsLastStretch);
        obfuscationPanel.add(overloadAggressivelyCheckBox,         constraintsLastStretch);
        obfuscationPanel.add(defaultPackageCheckBox,               constraints);
        obfuscationPanel.add(defaultPackageTextField,              constraintsLastStretch);
        obfuscationPanel.add(keepAttributesCheckBox,               constraints);
        obfuscationPanel.add(keepAttributesTextField,              constraintsLastStretch);
        obfuscationPanel.add(newSourceFileAttributeCheckBox,       constraints);
        obfuscationPanel.add(newSourceFileAttributeTextField,      constraintsLastStretch);

        JPanel consistencyPanel = new JPanel(layout);
        addBorder(consistencyPanel, "consistencyAndCorrectness");

        consistencyPanel.add(verboseCheckBox,                      constraintsLastStretch);
        consistencyPanel.add(printSeedsCheckBox,                   constraints);
        consistencyPanel.add(printSeedsTextField,                  constraintsStretch);
        consistencyPanel.add(printSeedsBrowseButton,               constraintsLast);
        consistencyPanel.add(noteCheckBox,                         constraintsLastStretch);
        consistencyPanel.add(warnCheckBox,                         constraintsLastStretch);
        consistencyPanel.add(ignoreWarningsCheckBox,               constraintsLastStretch);
        consistencyPanel.add(skipNonPublicLibraryClassesCheckBox,  constraintsLastStretch);

        // Collect all components that are followed by text fields and make
        // sure they are equally sized. That way the text fields start at the
        // same horizontal position.
        setCommonPreferredSize(Arrays.asList(new JComponent[] {
            printUsageCheckBox,
            printMappingCheckBox,
            applyMappingCheckBox,
            defaultPackageCheckBox,
            newSourceFileAttributeCheckBox,
            printSeedsCheckBox
        }));

        JPanel outputPanel = new JPanel(layout);
        outputPanel.add(outPanel,         stretchPanelConstraints);
        outputPanel.add(shrinkingPanel,   panelConstraints);
        outputPanel.add(obfuscationPanel, panelConstraints);
        outputPanel.add(consistencyPanel, panelConstraints);

        // Create the process panel.
        consoleTextArea.setOpaque(false);
        consoleTextArea.setEditable(false);
        consoleTextArea.setLineWrap(false);
        consoleTextArea.setWrapStyleWord(false);
        JScrollPane consoleScrollPane = new JScrollPane(consoleTextArea);
        consoleScrollPane.setBorder(new EmptyBorder(1, 1, 1, 1));
        addBorder(consoleScrollPane, "processingConsole");

        JPanel processPanel = new JPanel(layout);
        processPanel.add(consoleScrollPane, stretchPanelConstraints);

        // Create the load, save, and process buttons.
        JButton loadButton = new JButton(msg("loadConfiguration"));
        loadButton.addActionListener(new MyLoadConfigurationActionListener());

        JButton viewButton = new JButton(msg("viewConfiguration"));
        viewButton.addActionListener(new MyViewConfigurationActionListener());

        JButton saveButton = new JButton(msg("saveConfiguration"));
        saveButton.addActionListener(new MySaveConfigurationActionListener());

        JButton processButton = new JButton(msg("process"));
        processButton.addActionListener(new MyProcessActionListener());

        // Create the ReTrace panel.
        JPanel reTraceSettingsPanel = new JPanel(layout);
        addBorder(reTraceSettingsPanel, "reTraceSettings");

        JButton reTraceMappingBrowseButton = createBrowseButton(reTraceMappingTextField,
                                                                msg("selectApplyMappingFile"));

        JLabel reTraceMappingLabel = new JLabel(msg("mappingFile"));
        reTraceMappingLabel.setForeground(reTraceVerboseCheckBox.getForeground());

        reTraceSettingsPanel.add(reTraceVerboseCheckBox,     constraintsLastStretch);
        reTraceSettingsPanel.add(reTraceMappingLabel,        constraints);
        reTraceSettingsPanel.add(reTraceMappingTextField,    constraintsStretch);
        reTraceSettingsPanel.add(reTraceMappingBrowseButton, constraintsLast);

        stackTraceTextArea.setOpaque(true);
        stackTraceTextArea.setEditable(true);
        stackTraceTextArea.setLineWrap(false);
        stackTraceTextArea.setWrapStyleWord(true);
        JScrollPane stackTraceScrollPane = new JScrollPane(stackTraceTextArea);
        addBorder(stackTraceScrollPane, "obfuscatedStackTrace");

        reTraceTextArea.setOpaque(false);
        reTraceTextArea.setEditable(false);
        reTraceTextArea.setLineWrap(true);
        reTraceTextArea.setWrapStyleWord(true);
        JScrollPane reTraceScrollPane = new JScrollPane(reTraceTextArea);
        reTraceScrollPane.setBorder(new EmptyBorder(1, 1, 1, 1));
        addBorder(reTraceScrollPane, "deobfuscatedStackTrace");

        JPanel reTracePanel = new JPanel(layout);
        reTracePanel.add(reTraceSettingsPanel, panelConstraints);
        reTracePanel.add(stackTraceScrollPane, panelConstraints);
        reTracePanel.add(reTraceScrollPane,    stretchPanelConstraints);

        // Create the load button.
        JButton loadStackTraceButton = new JButton(msg("loadStackTrace"));
        loadStackTraceButton.addActionListener(new MyLoadStackTraceActionListener());

        JButton reTraceButton = new JButton(msg("reTrace"));
        reTraceButton.addActionListener(new MyReTraceActionListener());

        // Create the main tabbed pane.
        TabbedPane tabs = new TabbedPane();
        tabs.add(msg("proGuardTab"), proGuardPanel);
        tabs.add(msg("inputTab"),    inputPanel);
        tabs.add(msg("keepTab"),     keepPanel);
        tabs.add(msg("outputTab"),   outputPanel);
        tabs.add(msg("processTab"),  processPanel);
        tabs.add(msg("reTraceTab"),  reTracePanel);
        tabs.addImage(Toolkit.getDefaultToolkit().createImage(
            this.getClass().getResource(TITLE_IMAGE_FILE)));

        // Add the bottom buttons to each panel.
        proGuardPanel.add(Box.createGlue(),          glueConstraints);
        proGuardPanel.add(loadButton,                bottomButtonConstraints);
        proGuardPanel.add(createNextButton(tabs),    lastBottomButtonConstraints);

        inputPanel  .add(Box.createGlue(),           glueConstraints);
        inputPanel  .add(createPreviousButton(tabs), bottomButtonConstraints);
        inputPanel  .add(createNextButton(tabs),     lastBottomButtonConstraints);

        keepPanel   .add(Box.createGlue(),           glueConstraints);
        keepPanel   .add(createPreviousButton(tabs), bottomButtonConstraints);
        keepPanel   .add(createNextButton(tabs),     lastBottomButtonConstraints);

        outputPanel .add(Box.createGlue(),           glueConstraints);
        outputPanel .add(createPreviousButton(tabs), bottomButtonConstraints);
        outputPanel .add(createNextButton(tabs),     lastBottomButtonConstraints);

        processPanel.add(Box.createGlue(),           glueConstraints);
        processPanel.add(createPreviousButton(tabs), bottomButtonConstraints);
        processPanel.add(viewButton,                 bottomButtonConstraints);
        processPanel.add(saveButton,                 bottomButtonConstraints);
        processPanel.add(processButton,              lastBottomButtonConstraints);

        reTracePanel.add(Box.createGlue(),           glueConstraints);
        reTracePanel.add(loadStackTraceButton,       bottomButtonConstraints);
        reTracePanel.add(reTraceButton,              lastBottomButtonConstraints);

        // Initialize the GUI settings to reasonable defaults.
        loadConfiguration(this.getClass().getResource(DEFAULT_CONFIGURATION));

        // Add the main tabs to the frame and pack it.
        getContentPane().add(tabs);
    }


    public void startSplash()
    {
        splashPanel.start();
    }


    public void skipSplash()
    {
        splashPanel.stop();
    }


    /**
     * Loads the boilerplate keep class file options from the boilerplate file
     * into the boilerplate array.
     */
    private void loadBoilerplateKeepClassFileOptions()
    {
        try
        {
            // Parse the boilerplate configuration file.
            ConfigurationParser parser = new ConfigurationParser(
                this.getClass().getResource(BOILERPLATE_KEEP_CLASS_FILE));
            Configuration configuration = new Configuration();
            parser.parse(configuration);

            // We're only interested in the keep options.
            boilerplateKeepClassFileOptions = new KeepClassFileOption[configuration.keepClassFileOptions.size()];
            configuration.keepClassFileOptions.toArray(boilerplateKeepClassFileOptions);
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }


    /**
     * Adds a standard border with the title that corresponds to the given key
     * in the GUI resources.
     */
    private void addBorder(JComponent component, String titleKey)
    {
        Border oldBorder = component.getBorder();
        Border newBorder = BorderFactory.createTitledBorder(BORDER, msg(titleKey));

        component.setBorder(oldBorder == null ?
            newBorder :
            new CompoundBorder(newBorder, oldBorder));
    }


    /**
     * Creates a Previous button for the given tabbed pane.
     */
    private JButton createPreviousButton(final TabbedPane tabbedPane)
    {
        JButton browseButton = new JButton(msg("previous"));
        browseButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                tabbedPane.previous();
            }
        });

        return browseButton;
    }


    /**
     * Creates a Next button for the given tabbed pane.
     */
    private JButton createNextButton(final TabbedPane tabbedPane)
    {
        JButton browseButton = new JButton(msg("next"));
        browseButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                tabbedPane.next();
            }
        });

        return browseButton;
    }


    /**
     * Creates a browse button that opens a file browser for the given text field.
     */
    private JButton createBrowseButton(final JTextField textField,
                                       final String     title)
    {
        JButton browseButton = new JButton(msg("browse"));
        browseButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                fileChooser.setDialogTitle(title);
                fileChooser.setSelectedFile(new File(textField.getText()));

                int returnVal = fileChooser.showDialog(ProGuardGUI.this, msg("ok"));
                if (returnVal == JFileChooser.APPROVE_OPTION)
                {
                    textField.setText(fileChooser.getSelectedFile().getPath());
                }
            }
        });

        return browseButton;
    }


    /**
     * Sets the preferred sizes of the given components to the maximum of their
     * current preferred sizes.
     */
    private void setCommonPreferredSize(List components)
    {
        // Find the maximum preferred size.
        Dimension maximumSize = null;
        for (int index = 0; index < components.size(); index++)
        {
            JComponent component = (JComponent)components.get(index);
            Dimension  size      = component.getPreferredSize();
            if (maximumSize == null ||
                size.getWidth() > maximumSize.getWidth())
            {
                maximumSize = size;
            }
        }

        // Set the size that we found as the preferred size for all components.
        for (int index = 0; index < components.size(); index++)
        {
            JComponent component = (JComponent)components.get(index);
            component.setPreferredSize(maximumSize);
        }
    }


    /**
     * Updates to GUI settings to reflect the given ProGuard configuration.
     */
    private void setProGuardConfiguration(Configuration configuration)
    {
        // Set up the input and output jars and directories.
        programPanel .setClassPath(configuration.inJars);
        resourcePanel.setClassPath(configuration.resourceJars);
        libraryPanel .setClassPath(configuration.libraryJars);
        outPanel     .setClassPath(configuration.outJars);

        // Set up the boilerplate keep options.
        for (int index = 0; index < boilerplateKeepClassFileOptions.length; index++)
        {
            String classNames =
                findKeepClassFileOptions(boilerplateKeepClassFileOptions[index],
                                         configuration.keepClassFileOptions);

            boilerplateKeepCheckBoxes[index].setSelected(classNames != null);
            boilerplateKeepTextFields[index].setText(classNames == null ? "*" : classNames);
        }

        // Set up the special keep options. Note that the matched boilerplate
        // options have been removed from the list.
        specialKeepPanel.setKeepClassFileOptions(configuration.keepClassFileOptions);

        // Set up the other options.
        printSeedsCheckBox                 .setSelected(configuration.printSeeds   != null);
        printUsageCheckBox                 .setSelected(configuration.printUsage   != null);
        printMappingCheckBox               .setSelected(configuration.printMapping != null);
        applyMappingCheckBox               .setSelected(configuration.applyMapping != null);
        verboseCheckBox                    .setSelected(configuration.verbose);
        ignoreWarningsCheckBox             .setSelected(configuration.ignoreWarnings);
        warnCheckBox                       .setSelected(configuration.warn);
        noteCheckBox                       .setSelected(configuration.note);
        shrinkCheckBox                     .setSelected(configuration.shrink);
        obfuscateCheckBox                  .setSelected(configuration.obfuscate);
        useMixedCaseClassNamesCheckBox     .setSelected(configuration.useMixedCaseClassNames);
        overloadAggressivelyCheckBox       .setSelected(configuration.overloadAggressively);
        defaultPackageCheckBox             .setSelected(configuration.defaultPackage != null);
        keepAttributesCheckBox             .setSelected(configuration.keepAttributes != null);
        newSourceFileAttributeCheckBox     .setSelected(configuration.newSourceFileAttribute != null);
        skipNonPublicLibraryClassesCheckBox.setSelected(configuration.skipNonPublicLibraryClasses);

        printSeedsTextField                .setText(configuration.printSeeds);
        printUsageTextField                .setText(configuration.printUsage);
        printMappingTextField              .setText(configuration.printMapping);
        applyMappingTextField              .setText(configuration.applyMapping);
        defaultPackageTextField            .setText(configuration.defaultPackage);
        keepAttributesTextField            .setText(configuration.keepAttributes         == null ? "InnerClasses,SourceFile,LineNumberTable,Deprecated" : ListUtil.commaSeparatedString(configuration.keepAttributes));
        newSourceFileAttributeTextField    .setText(configuration.newSourceFileAttribute == null ? "SourceFile" : configuration.newSourceFileAttribute);

        if (configuration.printMapping != null)
        {
            reTraceMappingTextField.setText(configuration.printMapping);
        }
    }


    /**
     * Returns the ProGuard configuration that reflects the current GUI settings.
     */
    private Configuration getProGuardConfiguration()
    {
        Configuration configuration = new Configuration();

        // Get the input and output jars and directories.
        configuration.inJars       = programPanel .getClassPath();
        configuration.resourceJars = resourcePanel.getClassPath();
        configuration.libraryJars  = libraryPanel .getClassPath();
        configuration.outJars      = outPanel     .getClassPath();

        // Collect the boilerplate keep options.
        List keepClassFileOptions = new ArrayList();

        for (int index = 0; index < boilerplateKeepClassFileOptions.length; index++)
        {
            if (boilerplateKeepCheckBoxes[index].isSelected())
            {
                addKeepClassFileOptions(keepClassFileOptions,
                                        boilerplateKeepClassFileOptions[index],
                                        boilerplateKeepTextFields[index].getText());
            }
        }

        // Collect the special keep options.
        List specialKeepClassFileOptions = specialKeepPanel.getKeepClassFileOptions();
        if (specialKeepClassFileOptions != null)
        {
            keepClassFileOptions.addAll(specialKeepClassFileOptions);
        }

        // Put the list of keep options in the configuration.
        if (keepClassFileOptions.size() > 0)
        {
            configuration.keepClassFileOptions = keepClassFileOptions;
        }

        // Get the other options.
        configuration.printSeeds                  = printSeedsCheckBox                 .isSelected() ? printSeedsTextField                                .getText() : null;
        configuration.printUsage                  = printUsageCheckBox                 .isSelected() ? printUsageTextField                                .getText() : null;
        configuration.printMapping                = printMappingCheckBox               .isSelected() ? printMappingTextField                              .getText() : null;
        configuration.applyMapping                = applyMappingCheckBox               .isSelected() ? applyMappingTextField                              .getText() : null;
        configuration.verbose                     = verboseCheckBox                    .isSelected();
        configuration.ignoreWarnings              = ignoreWarningsCheckBox             .isSelected();
        configuration.warn                        = warnCheckBox                       .isSelected();
        configuration.note                        = noteCheckBox                       .isSelected();
        configuration.shrink                      = shrinkCheckBox                     .isSelected();
        configuration.obfuscate                   = obfuscateCheckBox                  .isSelected();
        configuration.useMixedCaseClassNames      = useMixedCaseClassNamesCheckBox     .isSelected();
        configuration.overloadAggressively        = overloadAggressivelyCheckBox       .isSelected();
        configuration.defaultPackage              = defaultPackageCheckBox             .isSelected() ? defaultPackageTextField                            .getText()  : null;
        configuration.keepAttributes              = keepAttributesCheckBox             .isSelected() ? ListUtil.commaSeparatedList(keepAttributesTextField.getText()) : null;
        configuration.newSourceFileAttribute      = newSourceFileAttributeCheckBox     .isSelected() ? newSourceFileAttributeTextField                    .getText()  : null;
        configuration.skipNonPublicLibraryClasses = skipNonPublicLibraryClassesCheckBox.isSelected();

        return configuration;
    }


    /**
     * Looks in the given list for ProGuard options that match the given template.
     * Returns a comma-separated string of class file names from matching options
     * and removes the matching options as a side effect.
     */
    private String findKeepClassFileOptions(KeepClassFileOption keepClassFileOptionTemplate,
                                            List                keepClassFileOptions)
    {
        if (keepClassFileOptions == null)
        {
            return null;
        }

        StringBuffer buffer = null;

        for (int index = 0; index < keepClassFileOptions.size(); index++)
        {
            KeepClassFileOption listedKeepClassFileOption =
                (KeepClassFileOption)keepClassFileOptions.get(index);
            String className = listedKeepClassFileOption.className;
            keepClassFileOptionTemplate.className = className;
            if (keepClassFileOptionTemplate.equals(listedKeepClassFileOption))
            {
                if (buffer == null)
                {
                    buffer = new StringBuffer();
                }
                else
                {
                    buffer.append(',');
                }
                buffer.append(className == null ? "*" : ClassUtil.externalClassName(className));

                // Remove the matching option as a side effect.
                keepClassFileOptions.remove(index--);
            }
        }

        return buffer == null ? null : buffer.toString();
    }


    /**
     * Adds ProGuard options to the given list, based on the given option
     * template and the comma-separated list of class names to be filled in.
     */
    private void addKeepClassFileOptions(List                keepClassFileOptions,
                                         KeepClassFileOption keepClassFileOptionTemplate,
                                         String              classNames)
    {
        List keepClassFileStrings = ListUtil.commaSeparatedList(classNames);

        for (int index = 0; index < keepClassFileStrings.size(); index++)
        {
            String keepClassFileString = (String)keepClassFileStrings.get(index);

            // Create a copy of the template.
            KeepClassFileOption keepClassFileOption =
                (KeepClassFileOption)keepClassFileOptionTemplate.clone();

            // Set the class name in the copy.
            keepClassFileOption.className =
                keepClassFileString.equals("") ||
                keepClassFileString.equals("*") ?
                    null :
                    ClassUtil.internalClassName(keepClassFileString);

            // Add the copy to the list.
            keepClassFileOptions.add(keepClassFileOption);
        }
    }


    // Methods and internal classes related to actions.

    /**
     * Loads the given ProGuard configuration into the GUI.
     */
    private void loadConfiguration(String fileName)
    {
        try
        {
            // Parse the configuration file.
            ConfigurationParser parser = new ConfigurationParser(fileName);
            Configuration configuration = new Configuration();
            parser.parse(configuration);

            // Let the GUI reflect the configuration.
            setProGuardConfiguration(configuration);
        }
        catch (IOException ex)
        {
            JOptionPane.showMessageDialog(getContentPane(),
                                          msg("cantOpenConfigurationFile", fileName),
                                          msg("warning"),
                                          JOptionPane.ERROR_MESSAGE);
        }
        catch (ParseException ex)
        {
            JOptionPane.showMessageDialog(getContentPane(),
                                          msg("cantParseConfigurationFile", fileName),
                                          msg("warning"),
                                          JOptionPane.ERROR_MESSAGE);
        }
    }


    /**
     * Loads the given ProGuard configuration into the GUI.
     */
    private void loadConfiguration(URL url)
    {
        try
        {
            // Parse the configuration file.
            ConfigurationParser parser = new ConfigurationParser(url);
            Configuration configuration = new Configuration();
            parser.parse(configuration);

            // Let the GUI reflect the configuration.
            setProGuardConfiguration(configuration);
        }
        catch (IOException ex)
        {
            JOptionPane.showMessageDialog(getContentPane(),
                                          msg("cantOpenConfigurationFile", url),
                                          msg("warning"),
                                          JOptionPane.ERROR_MESSAGE);
        }
        catch (ParseException ex)
        {
            JOptionPane.showMessageDialog(getContentPane(),
                                          msg("cantParseConfigurationFile", url),
                                          msg("warning"),
                                          JOptionPane.ERROR_MESSAGE);
        }
    }


    /**
     * Saves the current ProGuard configuration to the given file.
     */
    private void saveConfiguration(String fileName)
    {
        try
        {
            // Save the configuration file.
            ConfigurationWriter writer = new ConfigurationWriter(fileName);
            writer.write(getProGuardConfiguration());
            writer.close();
        }
        catch (Exception ex)
        {
            JOptionPane.showMessageDialog(getContentPane(),
                                          msg("cantSaveConfigurationFile", fileName),
                                          msg("warning"),
                                          JOptionPane.ERROR_MESSAGE);
        }
    }


    /**
     * Loads the given stack trace into the GUI.
     */
    private void loadStackTrace(String fileName)
    {
        try
        {
            // Read the entire stack trace file into a buffer.
            File file = new File(fileName);
            byte[] buffer = new byte[(int)file.length()];
            InputStream inputStream = new FileInputStream(file);
            inputStream.read(buffer);
            inputStream.close();

            // Put the stack trace in the text area.
            stackTraceTextArea.setText(new String(buffer));
        }
        catch (IOException ex)
        {
            JOptionPane.showMessageDialog(getContentPane(),
                                          msg("cantOpenStackTraceFile", fileName),
                                          msg("warning"),
                                          JOptionPane.ERROR_MESSAGE);
        }
    }


    /**
     * This ActionListener loads a ProGuard configuration file and initializes
     * the GUI accordingly.
     */
    private class MyLoadConfigurationActionListener implements ActionListener
    {
        public void actionPerformed(ActionEvent e)
        {
            configurationChooser.setDialogTitle(msg("selectConfigurationFile"));

            int returnValue = configurationChooser.showOpenDialog(ProGuardGUI.this);
            if (returnValue == JFileChooser.APPROVE_OPTION)
            {
                File selectedFile = configurationChooser.getSelectedFile();
                String fileName = selectedFile.getPath();

                loadConfiguration(fileName);
            }
        }
    }


    /**
     * This ActionListener saves a ProGuard configuration file based on the
     * current GUI settings.
     */
    private class MySaveConfigurationActionListener implements ActionListener
    {
        public void actionPerformed(ActionEvent e)
        {
            configurationChooser.setDialogTitle(msg("saveConfigurationFile"));

            int returnVal = configurationChooser.showSaveDialog(ProGuardGUI.this);
            if (returnVal == JFileChooser.APPROVE_OPTION)
            {
                File selectedFile = configurationChooser.getSelectedFile();
                String fileName = selectedFile.getPath();

                saveConfiguration(fileName);
            }
        }
    }


    /**
     * This ActionListener displays the ProGuard configuration specified by the
     * current GUI settings.
     */
    private class MyViewConfigurationActionListener implements ActionListener
    {
        public void actionPerformed(ActionEvent e)
        {
            // Make sure System.out has not been redirected yet.
            if (!systemOutRedirected)
            {
                consoleTextArea.setText("");

                TextAreaOutputStream outputStream =
                    new TextAreaOutputStream(consoleTextArea);

                try
                {
                    // TODO: write out relative path names and path names with system
                    // properties.

                    // Write the configuration.
                    ConfigurationWriter writer = new ConfigurationWriter(outputStream);
                    writer.write(getProGuardConfiguration());
                    writer.close();
                }
                catch (IOException ex)
                {
                }

                try
                {
                    outputStream.flush();
                }
                catch (IOException ex)
                {
                }
            }
        }
    }


    /**
     * This ActionListener executes ProGuard based on the current GUI settings.
     */
    private class MyProcessActionListener implements ActionListener
    {
        public void actionPerformed(ActionEvent e)
        {
            // Make sure System.out has not been redirected yet.
            if (!systemOutRedirected)
            {
                systemOutRedirected = true;

                // Get the informational configuration file name.
                File configurationFile = configurationChooser.getSelectedFile();
                String configurationFileName = configurationFile != null ?
                    configurationFile.getName() :
                    msg("sampleConfigurationFileName");

                // Create the ProGuard thread.
                Thread proGuardThread =
                    new Thread(new ProGuardRunnable(consoleTextArea,
                                                    getProGuardConfiguration(),
                                                    configurationFileName));

                // Run it.
                proGuardThread.start();
            }
        }
    }


    /**
     * This ActionListener loads an obfuscated stack trace from a file and puts
     * it in the proper text area.
     */
    private class MyLoadStackTraceActionListener implements ActionListener
    {
        public void actionPerformed(ActionEvent e)
        {
            fileChooser.setDialogTitle(msg("selectStackTraceFile"));
            fileChooser.setSelectedFile(null);

            int returnValue = fileChooser.showOpenDialog(ProGuardGUI.this);
            if (returnValue == JFileChooser.APPROVE_OPTION)
            {
                File selectedFile = fileChooser.getSelectedFile();
                String fileName = selectedFile.getPath();

                loadStackTrace(fileName);
            }
        }
    }


    /**
     * This ActionListener executes ReTrace based on the current GUI settings.
     */
    private class MyReTraceActionListener implements ActionListener
    {
        public void actionPerformed(ActionEvent e)
        {
            // Make sure System.out has not been redirected yet.
            if (!systemOutRedirected)
            {
                systemOutRedirected = true;

                boolean verbose            = reTraceVerboseCheckBox.isSelected();
                String  retraceMappingFile = reTraceMappingTextField.getText();
                String  stackTrace         = stackTraceTextArea.getText();

                // Create the ReTrace runnable.
                Runnable reTraceRunnable = new ReTraceRunnable(reTraceTextArea,
                                                               verbose,
                                                               retraceMappingFile,
                                                               stackTrace);

                // Run it in this thread, because it won't take long anyway.
                reTraceRunnable.run();
            }
        }
    }


    // Small utility methods.

    /**
     * Returns the message from the GUI resources that corresponds to the given
     * key.
     */
    private String msg(String messageKey)
    {
         return GUIResources.getMessage(messageKey);
    }


    /**
     * Returns the message from the GUI resources that corresponds to the given
     * key and argument.
     */
    private String msg(String messageKey,
                       Object messageArgument)
    {
         return GUIResources.getMessage(messageKey, new Object[] {messageArgument});
    }


    /**
     * The main method for the ProGuard GUI.
     */
    public static void main(String[] args)
    {
        ProGuardGUI gui = new ProGuardGUI();
        gui.pack();

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        Dimension guiSize    = gui.getSize();
        gui.setLocation((screenSize.width - guiSize.width)   / 2,
                        (screenSize.height - guiSize.height) / 2);
        gui.show();

        // Start the splash animation, unless specified otherwise.
        int argIndex = 0;
        if (argIndex < args.length &&
            NO_SPLASH_OPTION.startsWith(args[argIndex]))
        {
            gui.skipSplash();
            argIndex++;
        }
        else
        {
            gui.startSplash();
        }

        // Load an initial configuration, if specified.
        if (argIndex < args.length)
        {
            gui.loadConfiguration(args[argIndex]);
            argIndex++;
        }

        if (argIndex < args.length)
        {
            System.out.println(gui.getClass().getName() + ": ignoring extra arguments [" + args[argIndex] + "...]");
        }
    }
}
