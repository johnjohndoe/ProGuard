/* $Id: ReTrace.java,v 1.2 2002/09/03 08:27:54 eric Exp $
 *
 * ProGuard -- obfuscation and shrinking package for Java class files.
 *
 * Copyright (C) 2002 Eric Lafortune (eric@graphics.cornell.edu)
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
package proguard;

import proguard.classfile.*;
import proguard.classfile.util.*;
import proguard.classfile.visitor.*;

import java.io.*;
import java.util.*;


/**
 * Tool for de-obfuscating stack traces of applications that were obfuscated
 * with ProGuard.
 *
 * @author  Eric Lafortune
 * @created August 20, 2002
 */
public class ReTrace
{
    private static final String VERBOSE_OPTION = "-verbose";
    private static final String SPACES         = "                                                                ";

    // The class settings.
    private boolean verbose;
    private String  mappingFileName;
    private String  stackTraceFileName;

    // The stack trace.
    private String obfuscatedExceptionClassName;
    private String originalExceptionClassName;
    private String exceptionMessage;
    private Vector stackTraceItems = new Vector();


    public ReTrace(boolean verbose,
                   String  mappingFileName,
                   String  stackTraceFileName)
    {
        this.verbose            = verbose;
        this.mappingFileName    = mappingFileName;
        this.stackTraceFileName = stackTraceFileName;
    }


    public void execute()
    {
        try
        {
            readStackTrace();
            resolveStackTrace();
            printStackTrace();
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }


    /**
     * Reads the stack trace file.
     */
    private void readStackTrace() throws IOException
    {
        LineNumberReader lineNumberReader = null;

        try
        {
          Reader reader = stackTraceFileName == null ?
              (Reader)new InputStreamReader(System.in) :
              (Reader)new BufferedReader(new FileReader(stackTraceFileName));

          lineNumberReader = new LineNumberReader(reader);

            // Read the exception class name.
            while (true)
            {
                String line = lineNumberReader.readLine();
                if (line == null)
                {
                    throw new IOException("Can't find exception in stack trace");
                }

                line = line.trim();

                // See if we can parse "Exception in thread "___" ___:___",
                // containing the optional thread name, the exception class
                // name and the exception message.
                // Just skip the line if some essential token is missing.

                // Trim away the thread message part, if any.
                if (line.startsWith("Exception in thread \""))
                {
                    int quote_index = line.indexOf('"', 21);
                    if (quote_index < 1)
                    {
                        continue;
                    }

                    line = line.substring(quote_index+1).trim();
                }

                int colonIndex = line.indexOf(':');
                if (colonIndex < 1)
                {
                    continue;
                }

                obfuscatedExceptionClassName = line.substring(0, colonIndex).trim();
                exceptionMessage             = line.substring(colonIndex+1).trim();
                break;
            }

            // Read the subsequent stack trace items.
            while (true)
            {
                String line = lineNumberReader.readLine();
                if (line == null)
                {
                    break;
                }

                line = line.trim();

                // See if we can parse "at ___.___(___:___)", containing
                // the class name, the method name, the source file, and the
                // optional line number.
                // Just skip the line if some essential token is missing.

                if (!line.startsWith("at "))
                {
                    continue;
                }

                int openParenthesisIndex = line.indexOf('(', 3);
                if (openParenthesisIndex < 0)
                {
                    continue;
                }

                int colonIndex = line.indexOf(':', openParenthesisIndex+1);

                int closeParenthesisIndex = line.indexOf(')', Math.max(openParenthesisIndex, colonIndex)+1);
                if (closeParenthesisIndex < 0)
                {
                    continue;
                }

                int periodIndex = line.lastIndexOf('.', openParenthesisIndex-1);
                if (periodIndex < 0)
                {
                    continue;
                }

                String obfuscatedClassName  = line.substring(3, periodIndex).trim();
                String obfuscatedMethodName = line.substring(periodIndex+1, openParenthesisIndex).trim();
                String sourceFile           = line.substring(openParenthesisIndex+1, colonIndex < 0 ? closeParenthesisIndex : colonIndex).trim();
                int    lineNumber           = colonIndex < 0 ?
                    0 :
                    Integer.parseInt(line.substring(colonIndex+1, closeParenthesisIndex).trim());

                stackTraceItems.addElement(
                    new MyStackTraceItem(obfuscatedClassName,
                                         obfuscatedMethodName,
                                         sourceFile,
                                         lineNumber));
            }
        }
        finally
        {
            if (stackTraceFileName != null &&
                lineNumberReader != null)
            {
                try
                {
                    lineNumberReader.close();
                }
                catch (IOException ex)
                {
                }
            }
        }
    }


    /**
     * Reads the mapping file, de-obfuscating the stack trace along the way.
     */
    private void resolveStackTrace() throws IOException
    {
        // First put the stack trace items in easily accessible structures.
        Hashtable obfuscatedMethods = new Hashtable();

        for (int index = 0; index < stackTraceItems.size(); index++)
        {
            MyStackTraceItem item = (MyStackTraceItem)stackTraceItems.elementAt(index);

            // See if this class already has other methods on the stack trace.
            HashSet classStackTraceItems =
                (HashSet)obfuscatedMethods.get(item.obfuscatedClassName);

            // Create a new entry if not.
            if (classStackTraceItems == null)
            {
                classStackTraceItems = new HashSet();
                obfuscatedMethods.put(item.obfuscatedClassName, classStackTraceItems);
            }

            // Add the item to this class's set.
            classStackTraceItems.add(item);
        }



        // Read the mapping file, resolving the stack trace as we go.
        LineNumberReader reader = null;

        try
        {
            reader = new LineNumberReader(
                     new BufferedReader(
                     new FileReader(mappingFileName)));

            String line = reader.readLine();

            // Read the subsequent class mappings.
            while (true)
            {
                if (line == null)
                {
                    break;
                }

                line = line.trim();

                // See if we can parse "___ -> ___:", containing the original
                // class name and the obfuscated class name.

                int colonIndex = line.indexOf(':');
                if (colonIndex < 1)
                {
                    continue;
                }

                int arrowIndex = line.indexOf("->");
                if (arrowIndex < 1)
                {
                    continue;
                }

                String originalClassName   = line.substring(0, arrowIndex).trim();
                String obfuscatedClassName = line.substring(arrowIndex+2, colonIndex).trim();

                // Is this the obfuscated class name of the exception?
                if (obfuscatedExceptionClassName.equals(obfuscatedClassName))
                {
                    // Resolve the exception class name.
                    originalExceptionClassName = originalClassName;
                }

                // Is this obfuscated class name used elsewhere in the stack trace?
                HashSet classStackTraceItems =
                    (HashSet)obfuscatedMethods.get(obfuscatedClassName);

                if (classStackTraceItems != null)
                {
                    // Resolve the class names in the stack trace items.
                    Iterator items = classStackTraceItems.iterator();
                    while (items.hasNext())
                    {
                        MyStackTraceItem item = (MyStackTraceItem)items.next();
                        item.originalClassName = originalClassName;
                    }
                }

                // Read the subsequent method mappings of this class.
                while (true)
                {
                    line = reader.readLine();

                    // See if we can parse "    ___:___:___ ___(___) -> ___",
                    // containing the line numbers, the return type, the original
                    // method name, the original method arguments, and the
                    // obfuscated method name.

                    if (line == null ||
                        !line.startsWith("    "))
                    {
                        break;
                    }

                    // Just ignore these class members if their class isn't
                    // in the stack trace anyway.
                    if (classStackTraceItems == null)
                    {
                        continue;
                    }

                    line = line.trim();

                    int colonIndex1 = line.indexOf(':');
                    int colonIndex2 = line.indexOf(':', colonIndex1+1);

                    int spaceIndex = line.indexOf(' ', colonIndex2+1);
                    if (spaceIndex < 0)
                    {
                        continue;
                    }

                    int openParenthesisIndex = line.indexOf('(', spaceIndex+1);
                    if (openParenthesisIndex < 0)
                    {
                        continue;
                    }

                    int closeParenthesisIndex = line.indexOf(')', openParenthesisIndex+1);
                    if (closeParenthesisIndex < 0)
                    {
                        continue;
                    }

                    arrowIndex = line.indexOf("->", closeParenthesisIndex+1);
                    if (arrowIndex < 1)
                    {
                        continue;
                    }

                    int firstLineNumber = colonIndex1 < 0 ?
                        0 :
                        Integer.parseInt(line.substring(0, colonIndex1).trim());

                    int lastLineNumber = colonIndex1 < 0 || colonIndex2 < 0 ?
                        0 :
                        Integer.parseInt(line.substring(colonIndex1+1, colonIndex2).trim());

                    // Include return type and arguments in the method name if
                    // we're in verbose mode.
                    String originalMethodName   = verbose ?
                        line.substring(colonIndex2+1, closeParenthesisIndex+1).trim() :
                        line.substring(spaceIndex+1, openParenthesisIndex).trim();
                    String obfuscatedMethodName = line.substring(arrowIndex+2).trim();

                    // Resolve the method names in the stack trace items.
                    Iterator items = classStackTraceItems.iterator();
                    while (items.hasNext())
                    {
                        MyStackTraceItem item = (MyStackTraceItem)items.next();
                        if (!item.uniqueSolution &&
                            item.obfuscatedMethodName.equals(obfuscatedMethodName) &&
                            (item.lineNumber == 0 ||
                             firstLineNumber == 0 ||
                             lastLineNumber  == 0 ||
                             (firstLineNumber <= item.lineNumber &&
                              lastLineNumber  >= item.lineNumber)))
                        {
                            // Create a Vector for storing solutions for this
                            // method name.
                            if (item.originalMethodNames == null)
                            {
                                item.originalMethodNames = new Vector();
                            }

                            // Does the method have line numbers?
                            if (firstLineNumber != 0 &&
                                lastLineNumber  != 0)
                            {
                                // Then it will be the one and only solution.
                                item.uniqueSolution = true;
                                item.originalMethodNames.clear();
                            }

                            // Add this method name solution to the list.
                            item.originalMethodNames.add(originalMethodName);
                        }
                    }
                }
            }
        }
        finally
        {
            if (reader != null)
            {
                try
                {
                    reader.close();
                }
                catch (IOException ex)
                {
                }
            }
        }
    }


    /**
     * Prints out the de-obfuscated stack trace.
     */
    private void printStackTrace()
    {
        String exceptionClassName = originalExceptionClassName != null ?
            originalExceptionClassName :
            obfuscatedExceptionClassName;

        System.out.println(obfuscatedExceptionClassName + ":" + exceptionMessage);

        for (int index = 0; index < stackTraceItems.size(); index++)
        {
            MyStackTraceItem item = (MyStackTraceItem)stackTraceItems.elementAt(index);

            // Get the original class name, if we found it.
            String className = item.originalClassName != null ?
                item.originalClassName :
                item.obfuscatedClassName;

            // Get the first original method name, if we found it.
            String methodName = item.originalMethodNames != null ?
                (String)item.originalMethodNames.elementAt(0) :
                item.obfuscatedMethodName;

            // Compose the source file with the line number, if any.
            String sourceFile = item.sourceFile;
            String source = item.lineNumber != 0 ?
                sourceFile + ":" + item.lineNumber :
                sourceFile;

            // Print out the resolved stack trace item.
            System.out.println("        at " + className + "." + methodName + "(" + source + ")");

            // Print out alternatives, if any.
            if (item.originalMethodNames != null)
            {
                for (int otherMethodNameIndex = 1; otherMethodNameIndex < item.originalMethodNames.size(); otherMethodNameIndex++) {
                    String otherMethodName = (String)item.originalMethodNames.elementAt(otherMethodNameIndex);
                    System.out.println(spaces(className.length()+12) + otherMethodName);
                }
            }
        }
    }


    /**
     * Returns a String of spaces of the given length, up to the maximum length
     * of <code>SPACES</code>.
     */
    private String spaces(int aCount)
    {
        return SPACES.substring(0, Math.min(aCount, SPACES.length()));
    }


    /**
     * This class represents an obfuscated stack trace item.
     */
    private static class MyStackTraceItem
    {
        public String  obfuscatedClassName;
        public String  obfuscatedMethodName;
        public String  sourceFile;
        public int     lineNumber;

        public String  originalClassName;
        public Vector  originalMethodNames;
        public boolean uniqueSolution;


        public MyStackTraceItem(String obfuscatedClassName,
                                String obfuscatedMethodName,
                                String sourceFile,
                                int    lineNumber)
        {
            this.obfuscatedClassName  = obfuscatedClassName;
            this.obfuscatedMethodName = obfuscatedMethodName;
            this.sourceFile           = sourceFile;
            this.lineNumber           = lineNumber;
        }
    }


    /**
     * The main program for ProComment.
     *
     * @param args the command line arguments: a comments file and a jar file.
     */
    public static void main(String[] args)
    {
        if (args.length < 1)
        {
            System.err.println("Usage: java proguard.ReTrace [-verbose] <mapping_file> [<stacktrace_file>]");
            System.exit(-1);
        }

        int argumentIndex = 0;

        boolean verbose = false;
        if (args[argumentIndex].equals(VERBOSE_OPTION))
        {
            verbose = true;
            argumentIndex++;

            if (args.length < 2)
            {
                System.err.println("Usage: java proguard.ReTrace [-verbose] <mapping_file> [<stacktrace_file>]");
                System.exit(-1);
            }
        }

        String mappingFileName    = args[argumentIndex++];
        String stackTraceFileName = argumentIndex < args.length ?
            args[argumentIndex++] :
            null;

        ReTrace proComment = new ReTrace(verbose,
                                         mappingFileName,
                                         stackTraceFileName);
        proComment.execute();

        System.exit(0);
    }
}
