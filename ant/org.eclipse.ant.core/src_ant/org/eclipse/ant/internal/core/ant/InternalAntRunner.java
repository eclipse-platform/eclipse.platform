/**********************************************************************
 * Copyright (c) 2002 IBM Corporation and others.
 * All rights reserved.   This program and the accompanying materials
 * are made available under the terms of the Common Public License v0.5
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v05.html
 * 
 * Contributors: 
 * IBM - Initial API and implementation
 **********************************************************************/
package org.eclipse.ant.internal.core.ant;

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
import java.io.*;
import java.util.*;

import org.apache.tools.ant.*;
import org.eclipse.ant.core.AntCorePlugin;
import org.eclipse.ant.internal.core.Task;
import org.eclipse.ant.internal.core.Type;
import org.eclipse.core.runtime.IProgressMonitor;
/**
 * Eclipse application entry point into Ant. Derived from the original Ant Main class
 * to ensure that the functionality is equivalent when running in the platform.
 * <p>
 * <b>Note:</b> This class/interface is part of an interim API that is still under 
 * development and expected to change significantly before reaching stability. 
 * It is being made available at this early stage to solicit feedback from pioneering 
 * adopters on the understanding that any code that uses this API will almost 
 * certainly be broken (repeatedly) as the API evolves.
 * </p>
 */
public class InternalAntRunner {

	/**
	 *
	 */
	protected IProgressMonitor monitor;

	/**
	 *
	 */
	protected List buildListeners;

	/**
	 *
	 */
	protected String buildFileLocation;

	/** Targets we want to run. */
	protected Vector targets;

	/**
	 *
	 */
	protected Map userProperties;

	/** Our current message output status. Follows Project.MSG_XXX */
	protected int messageOutputLevel = Project.MSG_INFO;

	/** Indicates whether output to the log is to be unadorned. */
	protected boolean emacsMode = false;

	/** Indicates we should only parse and display the project help information */
	protected boolean projectHelp = false;

	/** Stream that we are using for logging */
	private PrintStream out = System.out;

	/** Stream that we are using for logging error messages */
	private PrintStream err = System.err;

	/**
	 * The Ant logger class. There may be only one logger. It will have the
	 * right to use the 'out' PrintStream. The class must implements the BuildLogger
	 * interface.
	 */
	protected String loggerClassname = null;

	/** Extra arguments to be parsed as command line arguments. */
	protected String extraArguments = null;

	// properties
	private static final String PROPERTY_ECLIPSE_RUNNING = "eclipse.running"; //$NON-NLS-1$

public InternalAntRunner() {
	buildListeners = new ArrayList(5);
}

/**
 * Adds a build listener.
 * 
 * @param buildListener a build listener
 */
public void addBuildListeners(List classNames) {
	this.buildListeners = classNames;
}

/**
 * Adds a build logger .
 * 
 * @param
 */
public void addBuildLogger(String className) {
	this.loggerClassname = className;
}

/**
 * Adds user properties.
 */
public void addUserProperties(Map properties) {
	this.userProperties = properties;
}

protected void addBuildListeners(Project project) {
	try {
		project.addBuildListener(createLogger());
		for (Iterator iterator = buildListeners.iterator(); iterator.hasNext();) {
			String className = (String) iterator.next();
			Class listener = Class.forName(className);
			project.addBuildListener((BuildListener) listener.newInstance());
		}
	} catch (Exception e) {
		throw new BuildException(e);
	}
}


protected void setProperties(Project project) {
	project.setUserProperty(PROPERTY_ECLIPSE_RUNNING, "true"); //$NON-NLS-1$
    project.setUserProperty("ant.file" , getBuildFileLocation()); //$NON-NLS-1$
	project.setUserProperty("ant.version", getAntVersion()); //$NON-NLS-1$
    if (userProperties == null)
    	return;
    for (Iterator iterator = userProperties.entrySet().iterator(); iterator.hasNext();) {
		Map.Entry entry = (Map.Entry) iterator.next();
		project.setUserProperty((String) entry.getKey(), (String) entry.getValue());
	}
}

protected void setTasks(Project project) {
	List tasks = AntCorePlugin.getPlugin().getPreferences().getTasks();
	if (tasks == null)
		return;
	try {
		for (Iterator iterator = tasks.iterator(); iterator.hasNext();) {
			Task task = (Task) iterator.next();
			Class taskClass = Class.forName(task.getClassName());
			project.addTaskDefinition(task.getTaskName(), taskClass);
		}
	} catch (ClassNotFoundException e) {
		throw new BuildException(e);
	}
}

protected void setTypes(Project project) {
	List types = AntCorePlugin.getPlugin().getPreferences().getTypes();
	if (types == null)
		return;
	try {
		for (Iterator iterator = types.iterator(); iterator.hasNext();) {
			Type type = (Type) iterator.next();
			Class typeClass = Class.forName(type.getClassName());
			project.addDataTypeDefinition(type.getTypeName(), typeClass);
		}
	} catch (Exception e) {
		throw new BuildException(e);
	}
}


/**
 * Parses the build script and adds necessary information into
 * the given project.
 */
protected void parseScript(Project project) {
	File buildFile = new File(getBuildFileLocation());
	ProjectHelper.configureProject(project, buildFile);
}

/**
 * Runs the build script.
 */
public void run() {
	if (extraArguments != null)
		processCommandLine(getArrayList(extraArguments));
	Project project = new Project();
	Throwable error = null;
    PrintStream originalErr = System.err;
    PrintStream originalOut = System.out;
	try {
        System.setOut(new PrintStream(new DemuxOutputStream(project, false)));
        System.setErr(new PrintStream(new DemuxOutputStream(project, true)));
		project.log(Policy.bind("label.buildFile", getBuildFileLocation())); //$NON-NLS-1$
		project.init();
		addBuildListeners(project);
		setProperties(project);
		setTasks(project);
		setTypes(project);
		parseScript(project);
		if (projectHelp) {
			printHelp(project);
			return;
		}
		createMonitorBuildListener(project);
        fireBuildStarted(project);
		if (extraArguments != null && !extraArguments.trim().equals("")) //$NON-NLS-1$
			project.log(Policy.bind("label.arguments", extraArguments)); //$NON-NLS-1$
		if (targets != null && !targets.isEmpty())
			project.executeTargets(targets);
		else
			project.executeTarget(project.getDefaultTarget());
	} catch(RuntimeException e) {
        error = e;
        throw e;
    } catch(Error e) {
        error = e;
        throw e;
	} finally {
		if (error != null)
			error.printStackTrace();
        System.setErr(originalErr);
        System.setOut(originalOut);
		fireBuildFinished(project, error);
	}
}

protected void createMonitorBuildListener(Project project) {
	if (monitor == null)
		return;
	Vector chosenTargets = targets;
	if (chosenTargets == null || chosenTargets.isEmpty()) {
		chosenTargets = new Vector();
		chosenTargets.add(project.getDefaultTarget());
	}
	project.addBuildListener(new ProgressBuildListener(project, chosenTargets, monitor));
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
    if (defaultTarget != null && !"".equals(defaultTarget)) { // shouldn't need to check but... //$NON-NLS-1$
        Vector defaultName = new Vector();
        Vector defaultDesc = null;
        defaultName.addElement(defaultTarget);

        int indexOfDefDesc = topNames.indexOf(defaultTarget);
        if (indexOfDefDesc >= 0) {
            defaultDesc = new Vector();
            defaultDesc.addElement(topDescriptions.elementAt(indexOfDefDesc));
        }
        printTargets(project, defaultName, defaultDesc, Policy.bind("label.defaultTarget"), maxLength); //$NON-NLS-1$

    }

    printTargets(project, topNames, topDescriptions, Policy.bind("label.mainTargets"), maxLength); //$NON-NLS-1$
    printTargets(project, subNames, null, Policy.bind("label.subTargets"), 0); //$NON-NLS-1$
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
 * Logs a message with the client that lists the target names and optional descriptions
 * 
 * @param names the targets names
 * @param descriptions the corresponding descriptions
 * @param heading the message heading
 * @param maxlen maximum length that can be allocated for a name
 */
private void printTargets(Project project, Vector names, Vector descriptions, String heading, int maxlen) {
	// now, start printing the targets and their descriptions
	String lSep = System.getProperty("line.separator"); //$NON-NLS-1$
	// got a bit annoyed that I couldn't find a pad function
	String spaces = "    "; //$NON-NLS-1$
	while (spaces.length() < maxlen) {
		spaces += spaces;
	}
	StringBuffer msg = new StringBuffer();
	msg.append(heading + lSep + lSep);
	for (int i= 0; i < names.size(); i++) {
		msg.append(" "); //$NON-NLS-1$
		msg.append(names.elementAt(i));
		if (descriptions != null) {
			msg.append(spaces.substring(0, maxlen - ((String) names.elementAt(i)).length() + 2));
			msg.append(descriptions.elementAt(i));
		}
		msg.append(lSep);
	}
	logMessage(project, msg.toString(), Project.MSG_INFO);
}

/**
 * Invokes the building of a project object and executes a build using either a given
 * target or the default target.
 *
 * @param argArray the command line arguments
 * @exception execution exceptions
 */
public void run(Object argArray) throws Exception {
	boolean success = processCommandLine(getArrayList((String[]) argArray));
    if (!success)
        return;
	try {
		run();
	} catch (Exception e) {
		printMessage(e);
		throw e;
	}
}

/**
 * Prints the message of the Throwable if it is not null.
 * 
 * @param t the throwable whose message is to be displayed
 */
protected void printMessage(Throwable t) {
	String message= t.getMessage();
	if (message != null)
		logMessage(null, message, Project.MSG_ERR);
}

/**
 * Creates and returns the default build logger for logging build events to the ant log.
 * 
 * @return the default build logger for logging build events to the ant log
 */
protected BuildLogger createLogger() {
	BuildLogger logger = null;
	if (loggerClassname != null) {
		try {
			logger = (BuildLogger) (Class.forName(loggerClassname).newInstance());
		} catch (Exception e) {
			String message = Policy.bind("exception.cannotCreateLogger", loggerClassname); //$NON-NLS-1$
			logMessage(null, message, Project.MSG_ERR);
			throw new BuildException(e);
		}
	} else {
		logger = new DefaultLogger();
	}
	logger.setMessageOutputLevel(messageOutputLevel);
	logger.setOutputPrintStream(out);
	logger.setErrorPrintStream(err);
	logger.setEmacsMode(emacsMode);
	return logger;
}

/**
 * We only have to do this because Project.fireBuildStarted is protected. If it becomes
 * public we should remove this method and call the appropriate one.
 */
private void fireBuildStarted(Project project) {
    BuildEvent event = new BuildEvent(project);
    for (Iterator iterator = project.getBuildListeners().iterator(); iterator.hasNext();) {
        BuildListener listener = (BuildListener) iterator.next();
        listener.buildStarted(event);
	}
}

/**
 * We only have to do this because Project.fireBuildFinished is protected. If it becomes
 * public we should remove this method and call the appropriate one.
 */
private void fireBuildFinished(Project project, Throwable error) {
    BuildEvent event = new BuildEvent(project);
    event.setException(error);
    for (Iterator iterator = project.getBuildListeners().iterator(); iterator.hasNext();) {
        BuildListener listener = (BuildListener) iterator.next();
        listener.buildFinished(event);
	}
}

protected void logMessage(Project project, String message, int priority) {
	if (project == null)
		project = new Project();
    BuildEvent event = new BuildEvent(project);
    event.setMessage(message, priority);
    for (Iterator iterator = buildListeners.iterator(); iterator.hasNext();) {
        BuildListener listener = (BuildListener) iterator.next();
        listener.messageLogged(event);
	}
}

/**
 * Sets the buildFileLocation.
 * 
 * @param buildFileLocation the file system location of the build file
 */
public void setBuildFileLocation(String buildFileLocation) {
	this.buildFileLocation = buildFileLocation;
}

protected String getBuildFileLocation() {
	if (buildFileLocation == null)
		buildFileLocation = new File("build.xml").getAbsolutePath(); //$NON-NLS-1$
	return buildFileLocation;
}

/**
 * Sets the message output level. Use -1 for none.
 * 
 * @param 
 */
public void setMessageOutputLevel(int level) {
	this.messageOutputLevel = level;
}

/**
 * 
 */
public void setArguments(String args) {
	this.extraArguments = args;
}

/**
 * Sets the execution targets.
 * 
 */
public void setExecutionTargets(Vector executiongTargets) {
	targets = executiongTargets;
}

protected static String getAntVersion() throws BuildException {
    try {
        Properties props = new Properties();
        InputStream in = Main.class.getResourceAsStream("/org/apache/tools/ant/version.txt"); //$NON-NLS-1$
        props.load(in);
        in.close();
        
        StringBuffer msg = new StringBuffer();
        msg.append(Policy.bind("usage.antVersion")); //$NON-NLS-1$
        msg.append(props.getProperty("VERSION") + " "); //$NON-NLS-1$ //$NON-NLS-2$
        msg.append(Policy.bind("usage.compiledOn")); //$NON-NLS-1$
        msg.append(props.getProperty("DATE")); //$NON-NLS-1$
        return msg.toString();
    } catch (IOException ioe) {
        throw new BuildException(Policy.bind("exception.cannotLoadVersionInfo", ioe.getMessage())); //$NON-NLS-1$
    } catch (NullPointerException npe) {
        throw new BuildException(Policy.bind("exception.cannotLoadVersionInfo", "")); //$NON-NLS-1$ //$NON-NLS-2$
    }
}

/**
 * Looks for interesting command line arguments. Returns true is it is OK to run
 * the script.
 */
protected boolean processCommandLine(List commands) {
	// looks for flag-like commands
	if (commands.remove("-help")) { //$NON-NLS-1$
		printUsage();
		return false;
	} 
	if (commands.remove("-version")) { //$NON-NLS-1$
		printVersion();
		return false;
	} 
	if (commands.remove("-quiet") || commands.remove("-q")) { //$NON-NLS-1$ //$NON-NLS-2$
		messageOutputLevel = Project.MSG_WARN;
	} 
	if (commands.remove("-verbose") || commands.remove("-v")) { //$NON-NLS-1$ //$NON-NLS-2$
		messageOutputLevel = Project.MSG_VERBOSE;
	} 
	if (commands.remove("-debug")) { //$NON-NLS-1$
		messageOutputLevel = Project.MSG_DEBUG;
	}
	if (commands.remove("-emacs")) { //$NON-NLS-1$
		emacsMode = true;
	}
	if (commands.remove("-projecthelp")) { //$NON-NLS-1$
		projectHelp = true;
	} 
	
	// look for argumments
	String[] args = getArguments(commands, "-logfile"); //$NON-NLS-1$
	if (args == null) {
		args = getArguments(commands, "-l"); //$NON-NLS-1$
	}
	if (args != null) {
		try {
			File logFile = new File(args[0]);
			out = new PrintStream(new FileOutputStream(logFile));
			err = out;
		} catch (IOException e) {
			// just log message and ignore exception
			logMessage(null, Policy.bind("exception.cannotWriteToLog"), Project.MSG_INFO); //$NON-NLS-1$
			return false;
		}
	}

	args = getArguments(commands, "-buildfile"); //$NON-NLS-1$
	if (args == null) {
		args = getArguments(commands, "-file"); //$NON-NLS-1$
		if (args == null)
			args = getArguments(commands, "-f"); //$NON-NLS-1$
	}
	if (args != null) {
		buildFileLocation = args[0];
		targets = new Vector();
		for (int i = 1; i < args.length; i++)
			targets.add(args[i]);
	}

	args = getArguments(commands, "-listener"); //$NON-NLS-1$
	if (args != null)
		buildListeners.add(args[0]);

	args = getArguments(commands, "-logger"); //$NON-NLS-1$
	if (args != null)
		loggerClassname = args[0];

	processProperties(commands);

	return true;
}

protected void processProperties(List commands) {
	userProperties = new HashMap(10);
	String[] args = (String[]) commands.toArray(new String[commands.size()]);
	for (int i = 0; i < args.length; i++) {
		String arg = args[i];
		if (arg.startsWith("-D")) { //$NON-NLS-1$

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
			int posEq = name.indexOf("="); //$NON-NLS-1$
			if (posEq > 0) {
				value = name.substring(posEq + 1);
				name = name.substring(0, posEq);
			} else if (i < args.length - 1)
				value = args[++i];
	
			userProperties.put(name, value);
			commands.remove(args[i]);
		}
	}
}

/**
 * Print the project description, if any
 */
protected void printHelp(Project project) {
	if (project.getDescription() != null)
		logMessage(project, project.getDescription(), Project.MSG_INFO);
	printTargets(project);
}

/**
 * Logs a message with the client indicating the version of <b>Ant</b> that this class
 * fronts.
 */
protected void printVersion() {
 	logMessage(null, getAntVersion(), Project.MSG_INFO);
}

/**
 * Logs a message with the client outlining the usage of <b>Ant</b>.
 */
protected void printUsage() {
	String lSep = System.getProperty("line.separator"); //$NON-NLS-1$
	StringBuffer msg = new StringBuffer();
	msg.append("ant [" + Policy.bind("usage.options") + "] ["  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				+ Policy.bind("usage.target") + " [" //$NON-NLS-1$ //$NON-NLS-2$
				+ Policy.bind("usage.target") + "2 [" //$NON-NLS-1$ //$NON-NLS-2$
				+ Policy.bind("usage.target") + "3] ...]]" + lSep); //$NON-NLS-1$ //$NON-NLS-2$
	msg.append(Policy.bind("usage.Options") + ": " + lSep); //$NON-NLS-1$ //$NON-NLS-2$
	msg.append("  -help                  " + Policy.bind("usage.printMessage") + lSep); //$NON-NLS-1$ //$NON-NLS-2$
	msg.append("  -projecthelp           " + Policy.bind("usage.projectHelp") + lSep); //$NON-NLS-1$ //$NON-NLS-2$
	msg.append("  -version               " + Policy.bind("usage.versionInfo") + lSep); //$NON-NLS-1$ //$NON-NLS-2$
	msg.append("  -quiet                 " + Policy.bind("usage.beQuiet") + lSep); //$NON-NLS-1$ //$NON-NLS-2$
	msg.append("  -verbose               " + Policy.bind("usage.beVerbose") + lSep); //$NON-NLS-1$ //$NON-NLS-2$
	msg.append("  -debug                 " + Policy.bind("usage.printDebugInfo") + lSep); //$NON-NLS-1$ //$NON-NLS-2$
	msg.append("  -emacs                 " + Policy.bind("usage.emacsLog") + lSep); //$NON-NLS-1$ //$NON-NLS-2$
	msg.append("  -logfile <file>        " + Policy.bind("usage.useFile") + lSep); //$NON-NLS-1$ //$NON-NLS-2$
	msg.append("  -logger <classname>    " + Policy.bind("usage.logClass") + lSep); //$NON-NLS-1$ //$NON-NLS-2$
	msg.append("  -listener <classname>  " + Policy.bind("usage.listenerClass") + lSep); //$NON-NLS-1$ //$NON-NLS-2$
	msg.append("  -buildfile <file>      " + Policy.bind("usage.fileToBuild") + lSep); //$NON-NLS-1$ //$NON-NLS-2$
	msg.append("  -D<property>=<value>   " + Policy.bind("usage.propertiesValues") + lSep); //$NON-NLS-1$ //$NON-NLS-2$
//	msg.append("  -find <file>           " + Policy.bind("usage.findFileToBuild") + lSep);
	
	logMessage(null, msg.toString(), Project.MSG_INFO);
}

/**
 * From a command line list, get the array of arguments of a given parameter.
 * The parameter and its arguments are removed from the list.
 * @return null if the parameter is not found or has no arguments
 */
protected String[] getArguments(List commands, String param) {
	int index = commands.indexOf(param);
	if (index == -1)
		return null;
	commands.remove(index);
	if (index == commands.size()) // if this is the last command
		return null;
	List args = new ArrayList(commands.size());
	while (index < commands.size()) { // while not the last command
		String command = (String) commands.get(index);
		if (command.startsWith("-")) // is it a new parameter? //$NON-NLS-1$
			break;
		args.add(command);
		commands.remove(index);
	}
	if (args.isEmpty())
		return null;
	return (String[]) args.toArray(new String[args.size()]);
}

/**
 * Helper method to ensure an array is converted into an ArrayList.
 */
private ArrayList getArrayList(String[] args) {
	// We could be using Arrays.asList() here, but it does not specify
	// what kind of list it will return. We do need a list that
	// implements the method List.remove(int) and ArrayList does.
	ArrayList result = new ArrayList(args.length);
	for (int i = 0; i < args.length; i++)
		result.add(args[i]);
	return result;
}

/**
 * Helper method to ensure an array is converted into an ArrayList.
 */
private ArrayList getArrayList(String args) {
	StringBuffer sb = new StringBuffer();
	boolean waitingForQuote = false;
	ArrayList result = new ArrayList();
	for (StringTokenizer tokens = new StringTokenizer(args, ", \"", true); tokens.hasMoreTokens();) { //$NON-NLS-1$
		String token = tokens.nextToken();
		if (waitingForQuote) {
			if (token.equals("\"")) { //$NON-NLS-1$
				result.add(sb.toString());
				sb.setLength(0);
				waitingForQuote = false;
			} else
				sb.append(token);
		} else {
			if (token.equals("\"")) { //$NON-NLS-1$
				// test if we have something like -Dproperty="value"
				if (result.size() > 0) {
					int index = result.size() - 1;
					String last = (String) result.get(index);
					if (last.charAt(last.length()-1) == '=') {
						result.remove(index);
						sb.append(last);
					}
				}
				waitingForQuote = true;
			} else {
				if (!(token.equals(",") || token.equals(" "))) //$NON-NLS-1$ //$NON-NLS-2$
					result.add(token);
			}
		}
	}
	return result;
}

/**
 * Sets the build progress monitor.
 */
public void setProgressMonitor(IProgressMonitor monitor) {
	this.monitor = monitor;
}
/**
 * Gets all the target information from the build script.
 * Returns a two dimension array. Each row represents a
 * target, where the first column is the name and the
 * second column is the description. The last row is
 * special and represents the name of the default target.
 * This default target name is in the first column, the
 * second column is null. Note, the default name can be
 * null.
 */
public String[][] getTargets() {
	// create a project and initialize it
	Project antProject = new Project();
	antProject.init();
	antProject.setProperty("ant.file", getBuildFileLocation()); //$NON-NLS-1$
	parseScript(antProject);
	String defaultName = antProject.getDefaultTarget();
	Collection targets = antProject.getTargets().values();
	String[][] infos = new String[targets.size() + 1][2];
	Iterator enum = targets.iterator();
	int i = 0;
	while (enum.hasNext()) {
		Target target = (Target) enum.next();
		infos[i][0] = target.getName();
		infos[i][1] = target.getDescription();
		i++;
	}
	infos[i][0] = defaultName;
	return infos;
}
/**
 * Gets all the target information from the build script.
 * Returns a two dimension array. Each row represents a
 * target, where the first column is the name and the
 * second column is the description. The last row is
 * special and represents the name of the default target.
 * This default target name is in the first column, the
 * second column is null. Note, the default name can be
 * null.
 */
public String[][] getTargets() {
	// create a project and initialize it
	Project antProject = new Project();
	antProject.init();
	antProject.setProperty("ant.file", getBuildFileLocation()); //$NON-NLS-1$
	parseScript(antProject);
	String defaultName = antProject.getDefaultTarget();
	Collection targets = antProject.getTargets().values();
	String[][] infos = new String[targets.size() + 1][2];
	Iterator enum = targets.iterator();
	int i = 0;
	while (enum.hasNext()) {
		Target target = (Target) enum.next();
		infos[i][0] = target.getName();
		infos[i][1] = target.getDescription();
		i++;
	}
	infos[i][0] = defaultName;
	return infos;
}}