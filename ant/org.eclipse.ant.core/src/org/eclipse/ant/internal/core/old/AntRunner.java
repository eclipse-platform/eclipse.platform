package org.eclipse.ant.internal.core.old;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 1999, 2000 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "The Jakarta Project", "Ant", and "Apache Software
 *    Foundation" must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache"
 *    nor may "Apache" appear in their names without prior written
 *    permission of the Apache Group.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 */
import org.eclipse.core.boot.IPlatformRunnable;
import java.io.*;
import java.util.*;
import org.apache.tools.ant.*;

/**
 * Eclipse application entry point into Ant. Derived from the original Ant Main class
 * to ensure that the functionality was equivalent when running in the platform.
 * <p>
 * <b>Note:</b> This class/interface is part of an interim API that is still under 
 * development and expected to change significantly before reaching stability. 
 * It is being made available at this early stage to solicit feedback from pioneering 
 * adopters on the understanding that any code that uses this API will almost 
 * certainly be broken (repeatedly) as the API evolves.
 * </p>
 */

public class AntRunner implements IPlatformRunnable {

	/** The default build file name */
	public static final String DEFAULT_BUILD_FILENAME = "build.xml";

	/** Our current message output status. Follows Project.MSG_XXX */
	private int msgOutputLevel = Project.MSG_INFO;

	/** File that we are using for configuration */
	private File buildFile; /** null */

	/** Stream that we are using for logging */
	private PrintStream out = System.out;

	/** Stream that we are using for logging error messages */
	private PrintStream err = System.err;

	/** The build targets */
	private Vector targets = new Vector(5);

	/** Set of properties that can be used by tasks */
	private Properties definedProps = new Properties();

	/** Names of classes to add as listeners to project */
	private Vector listeners = new Vector(5);

	/** Names of classes to add as listeners to project */
	private IAntRunnerListener clientListener;

	/**
	 * The Ant logger class. There may be only one logger. It will have the
	 * right to use the 'out' PrintStream. The class must implements the BuildLogger
	 * interface
	 */
	private String loggerClassname = null;

	/**
	 * Indicates whether output to the log is to be unadorned.
	 */
	private boolean emacsMode = false;

	/**
	 * Indicates if this ant should be run.
	 */
	private boolean readyToRun = false;

	/**
	 * Indicates we should only parse and display the project help information
	 */
	private boolean projectHelp = false;
	
    private static String antVersion = null;
    
    /**
	 * Adds a logger and all registered build listeners to an ant project.
	 * 
	 * @param project the project to add listeners to
	 */
protected void addBuildListeners(Project project) {
	// If we have a client listener then use that.  Otherwise add the default listener
	if (clientListener != null)
		project.addBuildListener(clientListener);
	else
		project.addBuildListener(createLogger());
	for (int i= 0; i < listeners.size(); i++) {
		String className = (String) listeners.elementAt(i);
		try {
			BuildListener listener = (BuildListener) Class.forName(className).newInstance();
			project.addBuildListener(listener);
		} catch (Exception exc) {
			throw new BuildException(Policy.bind("exception.cannotCreateListener",className), exc);
		}
	}
}

/**
 * Creates and returns the default build logger for logging build events to the ant log.
 * 
 * @return the default build logger for logging build events to the ant log
 */
private BuildLogger createLogger() {
	BuildLogger logger = null;
	if (loggerClassname != null) {
		try {
			logger = (BuildLogger) (Class.forName(loggerClassname).newInstance());
		} catch (ClassCastException e) {
			System.err.println(Policy.bind("exception.loggerDoesNotImplementInterface",loggerClassname));
			throw new RuntimeException();
		} catch (Exception e) {
			System.err.println(Policy.bind("exception.cannotCreateLogger",loggerClassname));
			throw new RuntimeException();
		}
	} else {
		logger = new DefaultLogger();
	}
	logger.setMessageOutputLevel(msgOutputLevel);
	logger.setOutputPrintStream(out);
	logger.setErrorPrintStream(err);
	logger.setEmacsMode(emacsMode);
	return logger;
}

/**
 * Search parent directories for the build file.
 *
 * <p>Takes the given target as a suffix to append to each
 * parent directory in seach of a build file.  Once the
 * root of the file-system has been reached an exception
 * is thrown.
 * </p>
 * @param suffix Suffix filename to look for in parents.
 * @return A handle to the build file
 * @exception BuildException Failed to locate a build file
 */
private File findBuildFile(String start, String suffix) throws BuildException {
	logMessage(Policy.bind("info.searchingFor",suffix), Project.MSG_INFO);
	File parent = new File(new File(start).getAbsolutePath());
	File file = new File(parent, suffix);
	// check if the target file exists in the current directory
	while (!file.exists()) {
		// change to parent directory
		parent = getParentFile(parent);
		// if parent is null, then we are at the root of the fs,
		// complain that we can't find the build file.
		if (parent == null)
			throw new BuildException(Policy.bind("exception.noBuildFile"));
		// refresh our file handle
		file = new File(parent, suffix);
	}
	return file;
}

/**
 * Returns the appropriate insertion index for a given string into a sorted collection.
 * 
 * @return the insertion index
 * @param names the initial collection of sorted strings
 * @param name the string whose insertion index into <code>names</code> is to be determined
 */
private int findTargetPosition(Vector names, String name) {
	int result = names.size();
	for (int i = 0; i < names.size() && result == names.size(); i++) {
		if (name.compareTo((String) names.elementAt(i)) < 0)
			result = i;
	}
	return result;
}

/**
 * Helper to get the parent file for a given file.
 * <p>Added to simulate File.getParentFile() from JDK 1.2.</p>
 *
 * @param file File
 * @return Parent file or null if none
 */
private File getParentFile(File file) {
	String filename = file.getAbsolutePath();
	file = new File(filename);
	filename = file.getParent();
	logMessage(Policy.bind("info.searchingIn",filename), Project.MSG_VERBOSE);
	return (filename == null) ? null : new File(filename);
}
private void logMessage(String message, int severity) {
	if (clientListener != null)
		clientListener.messageLogged(message, severity);
}
/**
 * Command-line invocation method.
 * 
 * @param args the string arguments present on the command line
 */
public static void main(String[] args) throws Exception {
	new AntRunner().run(args);
}

/**
 * Equivalent to the standard command-line invocation method except
 * that command-line arguments are provided in the form of a string
 * instead of an array.
 * 
 * @param argString the string arguments present on the command line
 */
public static void main(String argString) throws Exception {
	main(tokenizeArgs(argString));
}

/**
 * Returns the output message level that has been requested by the
 * client.  This value will be one of <code>Project.MSG_ERR</code>,
 * <code>Project.MSG_WARN</code>, <code>Project.MSG_INFO</code>,
 * <code>Project.MSG_VERBOSE</code> or <code>Project.MSG_DEBUG</code>.
 * 
 * @see org.apache.tools.ant.Project
 * @return the output message level that has been requested by the client
 */
public int getOutputMessageLevel() {
	return msgOutputLevel;
}

/**
 * Prints the message of the Throwable if it is not null.
 * 
 * @param t the throwable whose message is to be displayed
 */
private void printMessage(Throwable t) {
	String message= t.getMessage();
	if (message != null)
		System.err.println(message);
}

/**
 * Logs a message with the client that lists the target names and optional descriptions
 * 
 * @param names the targets names
 * @param descriptions the corresponding descriptions
 * @param heading the message heading
 * @param maxlen maximum length that can be allocated for a name
 */
private void printTargets(Vector names, Vector descriptions, String heading, int maxlen) {
	// now, start printing the targets and their descriptions
	String lSep = System.getProperty("line.separator");
	// got a bit annoyed that I couldn't find a pad function
	String spaces = "    ";
	while (spaces.length() < maxlen) {
		spaces += spaces;
	}
	StringBuffer msg = new StringBuffer();
	msg.append(heading + lSep + lSep);
	for (int i= 0; i < names.size(); i++) {
		msg.append(" ");
		msg.append(names.elementAt(i));
		if (descriptions != null) {
			msg.append(spaces.substring(0, maxlen - ((String) names.elementAt(i)).length() + 2));
			msg.append(descriptions.elementAt(i));
		}
		msg.append(lSep);
	}
	logMessage(msg.toString(), Project.MSG_INFO);
}

/**
 * Logs a message with the client that lists the targets
 * in a project
 * 
 * @param project the project to list targets from
 */
private void printTargets(Project project) {
    // find the target with the longest name
    int maxLength = 0;
    Enumeration ptargets = project.getTargets().elements();
    String targetName;
    String targetDescription;
    Target currentTarget;
    // split the targets in top-level and sub-targets depending
    // on the presence of a description
    Vector topNames = new Vector();
    Vector topDescriptions = new Vector();
    Vector subNames = new Vector();

    while (ptargets.hasMoreElements()) {
        currentTarget = (Target)ptargets.nextElement();
        targetName = currentTarget.getName();
        targetDescription = currentTarget.getDescription();
        // maintain a sorted list of targets
        if (targetDescription == null) {
            int pos = findTargetPosition(subNames, targetName);
            subNames.insertElementAt(targetName, pos);
        } else {
            int pos = findTargetPosition(topNames, targetName);
            topNames.insertElementAt(targetName, pos);
            topDescriptions.insertElementAt(targetDescription, pos);
            if (targetName.length() > maxLength) {
                maxLength = targetName.length();
            }
        }
    }

    String defaultTarget = project.getDefaultTarget();
    if (defaultTarget != null && !"".equals(defaultTarget)) { // shouldn't need to check but...
        Vector defaultName = new Vector();
        Vector defaultDesc = null;
        defaultName.addElement(defaultTarget);

        int indexOfDefDesc = topNames.indexOf(defaultTarget);
        if (indexOfDefDesc >= 0) {
            defaultDesc = new Vector();
            defaultDesc.addElement(topDescriptions.elementAt(indexOfDefDesc));
        }
        printTargets(defaultName, defaultDesc, Policy.bind("label.defaultTarget"), maxLength);

    }

    printTargets(topNames, topDescriptions, Policy.bind("label.mainTargets"), maxLength);
    printTargets(subNames, null, Policy.bind("label.subTargets"), 0);
}

/**
 * Logs a message with the client outlining the usage of <b>Ant</b>.
 */
private void printUsage() {
	String lSep = System.getProperty("line.separator");
	StringBuffer msg = new StringBuffer();
	msg.append("ant [" + Policy.bind("usage.options") + "] [" 
				+ Policy.bind("usage.target") + " ["
				+ Policy.bind("usage.target") + "2 ["
				+ Policy.bind("usage.target") + "3] ...]]" + lSep);
	msg.append(Policy.bind("usage.Options") + ": " + lSep);
	msg.append("  -help                  " + Policy.bind("usage.printMessage") + lSep);
	msg.append("  -projecthelp           " + Policy.bind("usage.projectHelp") + lSep);
	msg.append("  -version               " + Policy.bind("usage.versionInfo") + lSep);
	msg.append("  -quiet                 " + Policy.bind("usage.beQuiet") + lSep);
	msg.append("  -verbose               " + Policy.bind("usage.beVerbose") + lSep);
	msg.append("  -debug                 " + Policy.bind("usage.printDebugInfo") + lSep);
	msg.append("  -emacs                 " + Policy.bind("usage.emacsLog") + lSep);
	msg.append("  -logfile <file>        " + Policy.bind("usage.useFile") + lSep);
	msg.append("  -logger <classname>    " + Policy.bind("usage.logClass") + lSep);
	msg.append("  -listener <classname>  " + Policy.bind("usage.listenerClass") + lSep);
	msg.append("  -buildfile <file>      " + Policy.bind("usage.fileToBuild") + lSep);
	msg.append("  -D<property>=<value>   " + Policy.bind("usage.propertiesValues") + lSep);
	msg.append("  -find <file>           " + Policy.bind("usage.findFileToBuild") + lSep);
	
	logMessage(msg.toString(), Project.MSG_INFO);
}

/**
 * Logs a message with the client indicating the version of <b>Ant</b> that this class
 * fronts.
 */
private void printVersion() {
 	logMessage(getAntVersion(), Project.MSG_INFO);
}

public synchronized static String getAntVersion() throws BuildException {
    if (antVersion == null) {
        try {
            Properties props = new Properties();
            InputStream in =
                Main.class.getResourceAsStream("/org/apache/tools/ant/version.txt");
            props.load(in);
            in.close();
            
            String lSep = System.getProperty("line.separator");
            StringBuffer msg = new StringBuffer();
            msg.append(Policy.bind("usage.antVersion"));
            msg.append(props.getProperty("VERSION") + " ");
            msg.append(Policy.bind("usage.compiledOn"));
            msg.append(props.getProperty("DATE"));
            antVersion = msg.toString();
        } catch (IOException ioe) {
            throw new BuildException(Policy.bind("exception.cannotLoadVersionInfo", ioe.getMessage()));
        } catch (NullPointerException npe) {
            throw new BuildException(Policy.bind("exception.cannotLoadVersionInfo", ""));
        }
    }
    return antVersion;
}

/**
 * Processes the command line passed in by the client.
 * 
 * @execption BuildException occurs if the build file is not properly specified
 * @param args the collection of arguments
 */
protected void processCommandLine(String[] args) throws BuildException {
	String searchForThis = null;
	// cycle through given args
	boolean canBeTarget = false;
	for (int i= 0; i < args.length; i++) {
		String arg = args[i];
		if (arg.equals("-help")) {
			printUsage();
			return;
		} 
		if (arg.equals("-version")) {
			printVersion();
			return;
		}
		if (arg.equals("-quiet") || arg.equals("-q")) {
			msgOutputLevel = Project.MSG_WARN;
			canBeTarget = false;
		} else if (arg.equals("-verbose") || arg.equals("-v")) {
			printVersion();
			msgOutputLevel = Project.MSG_VERBOSE;
			canBeTarget = false;
		} else if (arg.equals("-debug")) {
			printVersion();
			msgOutputLevel = Project.MSG_DEBUG;
			canBeTarget = false;
		} else if (arg.equals("-logfile") || arg.equals("-l")) {
			try {
				File logFile = new File(args[i + 1]);
				i++;
				out= new PrintStream(new FileOutputStream(logFile));
				err = out;
				System.setOut(out);
				System.setErr(out);
				canBeTarget = false;
			} catch (IOException ioe) {
				logMessage(Policy.bind("exception.cannotWriteToLog"), Project.MSG_INFO);
				return;
			} catch (ArrayIndexOutOfBoundsException aioobe) {
				logMessage(Policy.bind("exception.missingLogFile"), Project.MSG_INFO);
				return;
			}
		} else if (arg.equals("-buildfile") || arg.equals("-file") || arg.equals("-f")) {
			try {
				buildFile = new File(args[i + 1]);
				i++;
				canBeTarget = true;
			} catch (ArrayIndexOutOfBoundsException aioobe) {
				logMessage(Policy.bind("exception.missingBuildFile"), Project.MSG_INFO);
				return;
			}
		} else if (arg.equals("-listener")) {
			try {
				listeners.addElement(args[i + 1]);
				i++;
				canBeTarget = false;
			} catch (ArrayIndexOutOfBoundsException aioobe) {
				logMessage(Policy.bind("exception.missingClassName"), Project.MSG_INFO);
				return;
			}
		} else if (arg.startsWith("-D")) {

			/* Interestingly enough, we get to here when a user
			 * uses -Dname=value. However, in some cases, the JDK
			 * goes ahead * and parses this out to args
			 *   {"-Dname", "value"}
			 * so instead of parsing on "=", we just make the "-D"
			 * characters go away and skip one argument forward.
			 *
			 * I don't know how to predict when the JDK is going
			 * to help or not, so we simply look for the equals sign.
			 */

			String name = arg.substring(2, arg.length());
			String value = null;
			int posEq = name.indexOf("=");
			if (posEq > 0) {
				value = name.substring(posEq + 1);
				name = name.substring(0, posEq);
			} else if (i < args.length - 1)
				value = args[++i];

			definedProps.put(name, value);
			canBeTarget = false;
		} else if (arg.equals("-logger")) {
			if (loggerClassname != null) {
				logMessage(Policy.bind("exception.multipleLoggers"), Project.MSG_INFO);
				return;
			}
			loggerClassname = args[++i];
			canBeTarget = false;
		} else if (arg.equals("-emacs")) {
			emacsMode = true;
			canBeTarget = false;
		} else if (arg.equals("-projecthelp")) {
			projectHelp = true; // set the flag to display the targets and quit
			canBeTarget = false;
		} else if (arg.equals("-find")) {
			// eat up next arg if present, default to build.xml
			if (i < args.length - 1)
				searchForThis = args[++i];
			else
				searchForThis = DEFAULT_BUILD_FILENAME;
			canBeTarget = false;
		} else if (arg.startsWith("-")) {
			// we don't have any more args to recognize!
			logMessage(Policy.bind("exception.unknownArgument",arg), Project.MSG_INFO);
//			printUsage();
//			return;
			canBeTarget = false;
		} else {
			if (canBeTarget)
				targets.addElement(arg);
		}
	}
	// if buildFile was not specified on the command line,
	if (buildFile == null) {
		// but -find then search for it
		if (searchForThis != null)
			buildFile = findBuildFile(".", searchForThis);
		else
			buildFile = new File(DEFAULT_BUILD_FILENAME);
	}
	// make sure buildfile exists
	if (!buildFile.getAbsoluteFile().exists()) {
		logMessage(Policy.bind("exception.buildFileNotFound",buildFile.toString()), Project.MSG_INFO);
		throw new BuildException(Policy.bind("error.buildFailed"));
	}
	// make sure it's not a directory (this falls into the ultra
	// paranoid lets check everything catagory
	if (buildFile.isDirectory()) {
		logMessage(Policy.bind("exception.buildFileIsDirectory",buildFile.toString()), Project.MSG_INFO);
		throw new BuildException(Policy.bind("error.buildFailed"));
	}
	readyToRun= true;
}

/**
 * Invokes the building of a project object and executes a build using either a given
 * target or the default target.
 *
 * @param argArray the command line arguments
 * @exception execution exceptions
 */
public Object run(Object argArray) throws Exception {
	String[] args = (String[]) argArray;
	processCommandLine(args);
	try {
		runBuild(null);
	} catch (BuildException e) {
		if (err != System.err)
			printMessage(e);
		throw e;
	} catch (ThreadDeath e) {
		throw e;
	} catch (Throwable e) {
		printMessage(e);
	}
	return null;
}

/**
 * Invokes the building of a project object and executes a build using either a given
 * target or the default target.
 *
 * @param argArray the command line arguments
 * @param listener the client listener
 * @exception execution exceptions
 */
public Object run(Object argArray, IAntRunnerListener listener) throws Exception {
	clientListener = listener;
	return run(argArray);
}

/**
 * Executes the build.
 * 
 * @exception BuildException thrown if there is a problem during building.
 */
private void runBuild(ClassLoader coreLoader) throws BuildException {

    if (!readyToRun) {
        return;
    }

	logMessage(Policy.bind("label.buildFile",buildFile.toString()),Project.MSG_INFO);

    final EclipseProject project = new EclipseProject();
    project.setCoreLoader(coreLoader);
    
    Throwable error = null;

    try {
        addBuildListeners(project);

        PrintStream err = System.err;
        PrintStream out = System.out;
        SecurityManager oldsm = System.getSecurityManager();

        try {
            System.setOut(new PrintStream(new DemuxOutputStream(project, false)));
            System.setErr(new PrintStream(new DemuxOutputStream(project, true)));
            project.fireBuildStarted();
            project.init();
            project.setUserProperty("ant.version", getAntVersion());

            // set user-define properties
            Enumeration e = definedProps.keys();
            while (e.hasMoreElements()) {
                String arg = (String)e.nextElement();
                String value = (String)definedProps.get(arg);
                project.setUserProperty(arg, value);
            }
            
            project.setUserProperty("ant.file" , buildFile.getAbsolutePath() );
            
            // first use the ProjectHelper to create the project object
            // from the given build file.
            try {
                Class.forName("javax.xml.parsers.SAXParserFactory");
                ProjectHelper.configureProject(project, buildFile);
            } catch (NoClassDefFoundError ncdfe) {
                throw new BuildException(Policy.bind("exception.noParser"), ncdfe);
            } catch (ClassNotFoundException cnfe) {
                throw new BuildException(Policy.bind("exception.noParser"), cnfe);
            } catch (NullPointerException npe) {
                throw new BuildException(Policy.bind("exception.noParser"), npe);
            }
            
            // make sure that we have a target to execute
            if (targets.size() == 0) {
                targets.addElement(project.getDefaultTarget());
            }
            
            if (!projectHelp) {
                project.executeTargets(targets);
            }
        }
        finally {
            System.setOut(out);
            System.setErr(err);
        }
        if (projectHelp) {
            printDescription(project);
            printTargets(project);
        }
    }
    catch(RuntimeException exc) {
        error = exc;
        throw exc;
    }
    catch(Error err) {
        error = err;
        throw err;
    }
    finally {
        project.fireBuildFinished(error);
    }
}
/**
 * Print the project description, if any
 */
private static void printDescription(Project project) {
   if (project.getDescription() != null) {
      System.out.println(project.getDescription());
   }
}

/**
 * Returns a tokenized version of a string.
 * 
 * @return a tokenized version of a string
 * @param argString the original argument string
 */
public static String[] tokenizeArgs(String argString) throws Exception {
	Vector list = new Vector(5);
	for (StringTokenizer tokens = new StringTokenizer(argString, " "); tokens.hasMoreElements();)
		list.addElement((String) tokens.nextElement());
	return (String[]) list.toArray(new String[list.size()]);
}
}
