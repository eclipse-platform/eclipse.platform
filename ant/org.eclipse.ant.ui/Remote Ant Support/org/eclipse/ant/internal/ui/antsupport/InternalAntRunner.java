/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * Portions Copyright  2000-2005 The Apache Software Foundation
 * All rights reserved.  This program and the accompanying materials are made 
 * available under the terms of the Apache Software License v2.0 which 
 * accompanies this distribution and is available at 
 * http://www.apache.org/licenses/LICENSE-2.0.
 * 
 * Contributors:
 *     IBM Corporation - derived implementation
 *******************************************************************************/

package org.eclipse.ant.internal.ui.antsupport;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;

import org.apache.tools.ant.AntTypeDefinition;
import org.apache.tools.ant.BuildEvent;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.BuildListener;
import org.apache.tools.ant.BuildLogger;
import org.apache.tools.ant.ComponentHelper;
import org.apache.tools.ant.DefaultLogger;
import org.apache.tools.ant.DemuxOutputStream;
import org.apache.tools.ant.Diagnostics;
import org.apache.tools.ant.Main;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectHelper;
import org.apache.tools.ant.Target;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.TaskAdapter;
import org.apache.tools.ant.util.FileUtils;

/**
 * Eclipse application entry point into Ant in a separate VM. Derived from the original Ant Main class
 * to ensure that the functionality is equivalent when running in the platform.
 */
public class InternalAntRunner {

	/**
	 *  Message priority for project help messages. 
	 */
	public static final int MSG_PROJECT_HELP= Project.MSG_DEBUG + 1;
	
	private List buildListeners;

	private String buildFileLocation;

	/** 
	 * Targets we want to run.
	 */
	private Vector targets;

	private Map userProperties;
	
	private Project currentProject;
	
	private String defaultTarget;
	
	private BuildLogger buildLogger= null;
	
	private Map eclipseSpecifiedTasks;
	private Map eclipseSpecifiedTypes;
	
	/**
	 * Cache of the Ant version number when it has been loaded
	 */
	private String antVersionNumber= null;

	/** Our current message output status. Follows Project.MSG_XXX */
	private int messageOutputLevel = Project.MSG_INFO;

	/** Indicates whether output to the log is to be unadorned. */
	private boolean emacsMode = false;

	/** Indicates we should only parse and display the project help information */
	private boolean projectHelp = false;

	/** Stream that we are using for logging */
	private PrintStream out = System.out;

	/** Stream that we are using for logging error messages */
	private PrintStream err = System.err;

	/**
	 * The Ant logger class. There may be only one logger. It will have the
	 * right to use the 'out' PrintStream. The class must implement the BuildLogger
	 * interface.  An empty String indicates that no logger is to be used.  A <code>null</code>
	 * name indicates that the org.apache.tools.ant.DefaultLogger will be used.
	 */
	private String loggerClassname = null;

	/** Extra arguments to be parsed as command line arguments. */
	private String[] extraArguments = null;
	
	private boolean scriptExecuted= false;
	
	private List propertyFiles= new ArrayList();
	
	/**
     * The Ant InputHandler class. There may be only one input handler.
     */
    private String inputHandlerClassname = null;
    
    /** 
     * Indicates whether to execute all targets that 
     * do not depend on failed targets
     * @since Ant 1.6.0
     */
    private boolean keepGoing= false;

    /** 
     * Indicates whether this build is to support interactive input 
     * @since Ant 1.6.0
     */
    private boolean allowInput = true;
    
    public static void main(String[] args) {
    	try {
    		new InternalAntRunner().run(getArrayList(args));
    	} catch (Throwable t) {
    	    t.printStackTrace();
    		System.exit(1);
    	}
		System.exit(0);
	}

	private void addBuildListeners(Project project) {
		String className= null;
		try {
			BuildLogger logger= createLogger();
			if (logger != null) {
				project.addBuildListener(logger);
			}
			if (buildListeners != null) {
				for (Iterator iterator = buildListeners.iterator(); iterator.hasNext();) {
					className = (String) iterator.next();
					Class listener = Class.forName(className);
					project.addBuildListener((BuildListener) listener.newInstance());
				}
			}
		} catch (ClassCastException e) {
			String message = MessageFormat.format(InternalAntMessages.getString("InternalAntRunner.{0}_which_was_specified_to_be_a_build_listener_is_not_an_instance_of_org.apache.tools.ant.BuildListener._1"), new String[]{className}); //$NON-NLS-1$
			logMessage(null, message, Project.MSG_ERR);
			throw new BuildException(message, e);
		} catch (BuildException e) {
			throw e;
		} catch (Exception e) {
			throw new BuildException(e);
		}
	}

	/**
	 * Parses the build file and adds necessary information into
	 * the given project.
	 * @param project The project to configure
	 */
	private void parseBuildFile(Project project) {
		File buildFile = new File(getBuildFileLocation());
		if (!buildFile.exists()) {
			throw new BuildException(MessageFormat.format(InternalAntMessages.getString("InternalAntRunner.Buildfile__{0}_does_not_exist_!_1"), //$NON-NLS-1$
						 new String[]{buildFile.getAbsolutePath()}));
		}
		if (!buildFile.isFile()) {
			throw new BuildException(MessageFormat.format(InternalAntMessages.getString("InternalAntRunner.Buildfile__{0}_is_not_a_file_1"), //$NON-NLS-1$
							new String[]{buildFile.getAbsolutePath()}));
		}
		
		ProjectHelper helper = ProjectHelper.getProjectHelper();
		project.addReference("ant.projectHelper", helper); //$NON-NLS-1$
		helper.parse(project, buildFile);
	}

	private void printArguments(Project project) {
		if ((messageOutputLevel != Project.MSG_DEBUG) && (messageOutputLevel != Project.MSG_VERBOSE)) {
			return;
		}
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < extraArguments.length; i++) {
			sb.append(extraArguments[i]);
			sb.append(' ');
		}
		project.log(MessageFormat.format(InternalAntMessages.getString("InternalAntRunner.Arguments__{0}_2"), new String[]{sb.toString().trim()})); //$NON-NLS-1$
	}


	/**
	 * Logs a message with the client that lists the targets
	 * in a project
	 * 
	 * @param project the project to list targets from
	 */
	private void printTargets(Project project) {
		//notify the logger that project help message are coming
		//since there is no buildstarted or targetstarted to 
		//to be used to establish the connection
		logMessage(project, "", MSG_PROJECT_HELP); //$NON-NLS-1$
		// find the target with the longest name
		int maxLength = 0;
		Enumeration ptargets = project.getTargets().elements();
		String targetName;
		String targetDescription;
		Target currentTarget;
		// split the targets in top-level and sub-targets depending
		// on the presence of a description
		List topNames = new ArrayList();
		List topDescriptions = new ArrayList();
		List subNames = new ArrayList();

		while (ptargets.hasMoreElements()) {
			currentTarget = (Target) ptargets.nextElement();
			targetName = currentTarget.getName();
			targetDescription = currentTarget.getDescription();
			if (targetDescription == null) {
				subNames.add(targetName);
			} else {
				topNames.add(targetName);
				topDescriptions.add(targetDescription);
				if (targetName.length() > maxLength) {
					maxLength = targetName.length();
				}
			}
		}

		Collections.sort(subNames);
		Collections.sort(topNames);
		Collections.sort(topDescriptions);
		
		String defaultTargetName = project.getDefaultTarget();
		if (defaultTargetName != null && !"".equals(defaultTargetName)) { // shouldn't need to check but... //$NON-NLS-1$
			List defaultName = new ArrayList(1);
			List defaultDesc = null;
			defaultName.add(defaultTargetName);

			int indexOfDefDesc = topNames.indexOf(defaultTargetName);
			if (indexOfDefDesc >= 0) {
				defaultDesc = new ArrayList(1);
				defaultDesc.add(topDescriptions.get(indexOfDefDesc));
			}
			printTargets(project, defaultName, defaultDesc, InternalAntMessages.getString("InternalAntRunner.Default_target__3"), maxLength); //$NON-NLS-1$

		}

		printTargets(project, topNames, topDescriptions, InternalAntMessages.getString("InternalAntRunner.Main_targets__4"), maxLength); //$NON-NLS-1$
		printTargets(project, subNames, null, InternalAntMessages.getString("InternalAntRunner.Subtargets__5"), 0); //$NON-NLS-1$
	}

	/**
	 * Logs a message with the client that lists the target names and optional descriptions
	 * 
	 * @param project the enclosing target
	 * @param names the targets names
	 * @param descriptions the corresponding descriptions
	 * @param heading the message heading
	 * @param maxlen maximum length that can be allocated for a name
	 */
	private void printTargets(Project project, List names, List descriptions, String heading, int maxlen) {
		// now, start printing the targets and their descriptions
		String lSep = System.getProperty("line.separator"); //$NON-NLS-1$
		
		String spaces = "    "; //$NON-NLS-1$
		while (spaces.length() < maxlen) {
			spaces += spaces;
		}
		StringBuffer msg = new StringBuffer();
		msg.append(heading + lSep + lSep);
		for (int i = 0; i < names.size(); i++) {
			msg.append(' ');
			msg.append(names.get(i));
			if (descriptions != null) {
				msg.append(spaces.substring(0, maxlen - ((String) names.get(i)).length() + 2));
				msg.append(descriptions.get(i));
			}
			msg.append(lSep);
		}
		logMessage(project, msg.toString(), Project.MSG_INFO);
	}

	/*
	 * Note that the list passed to this method must support
	 * List#remove(Object)
	 */
	private void run(List argList) {
		setCurrentProject(new Project());
		Throwable error = null;
		PrintStream originalErr = System.err;
		PrintStream originalOut = System.out;
		InputStream originalIn= System.in;
		
		SecurityManager originalSM= System.getSecurityManager();
		scriptExecuted= true;
		try {
			if (argList != null && (argList.remove("-projecthelp") || argList.remove("-p"))) { //$NON-NLS-1$ //$NON-NLS-2$
				projectHelp = true;
			}
			getCurrentProject().init();
			if (argList != null) {
				scriptExecuted= preprocessCommandLine(argList);
			
				if (!scriptExecuted) {
					return;
				}
			}
			
			addBuildListeners(getCurrentProject());
		
			processProperties(argList);
			
			setProperties(getCurrentProject());
			
			addInputHandler(getCurrentProject());
			
			remapSystemIn();
			System.setOut(new PrintStream(new DemuxOutputStream(getCurrentProject(), false)));
			System.setErr(new PrintStream(new DemuxOutputStream(getCurrentProject(), true)));
			
			if (!projectHelp) {
				fireBuildStarted(getCurrentProject());
			}
			
			if (argList != null && !argList.isEmpty()) {
				try {
					scriptExecuted= processCommandLine(argList);
				} catch (BuildException e) {
					scriptExecuted= false;
					throw e;
				}
			}
			if (!scriptExecuted) {
				return;
			}
			
			//we do not want to set the default input stream for the default input handler
			//as we currently have no means for querying from System.in for input.
			//see bug 45484
			if (allowInput && inputHandlerClassname != null) {
				if (isVersionCompatible("1.6")) { //$NON-NLS-1$
					getCurrentProject().setDefaultInputStream(originalIn);
				}
			}
			
			getCurrentProject().log(MessageFormat.format(InternalAntMessages.getString("InternalAntRunner.Build_file__{0}_1"), new String[]{getBuildFileLocation()})); //$NON-NLS-1$
			
			setTasks();
			setTypes();
			
			if (isVersionCompatible("1.6")) { //$NON-NLS-1$
				getCurrentProject().setKeepGoingMode(keepGoing);
			}
			
			parseBuildFile(getCurrentProject());
			validateDefaultTarget();
			
			if (projectHelp) {
				printHelp(getCurrentProject());
				scriptExecuted= false;
				return;
			}
			
			if (extraArguments != null) {
				printArguments(getCurrentProject());
			}
			
			System.setSecurityManager(new AntSecurityManager(originalSM));
			
			if (targets != null && !targets.isEmpty()) {
				getCurrentProject().executeTargets(targets);
			} else {
				getCurrentProject().executeTarget(getCurrentProject().getDefaultTarget());
			}
		} catch (AntSecurityException e) {
			//expected
		} catch (Throwable e) {
			error = e;
		} finally {
			System.setErr(originalErr);
			System.setOut(originalOut);
			System.setIn(originalIn);
			if (System.getSecurityManager() instanceof AntSecurityManager) {
				System.setSecurityManager(originalSM);
			}
			
			if (!projectHelp) {				
				fireBuildFinished(getCurrentProject(), error);
			}
						
			//close any user specified build log
			if (err != originalErr) {
				err.close();
			}
			if (out != originalOut) {
				out.close();
			}
		}
	}
	
	private void setTasks() {
		if (eclipseSpecifiedTasks != null) {
			Iterator itr= eclipseSpecifiedTasks.keySet().iterator();
			String taskName;
			String taskClassName;
			while (itr.hasNext()) {
				taskName= (String) itr.next();
				taskClassName= (String) eclipseSpecifiedTasks.get(taskName);
				
				if (isVersionCompatible("1.6")) { //$NON-NLS-1$
					AntTypeDefinition def= new AntTypeDefinition();
					def.setName(taskName);
		            def.setClassName(taskClassName);
		            def.setClassLoader(this.getClass().getClassLoader());
		            def.setAdaptToClass(Task.class);
		            def.setAdapterClass(TaskAdapter.class);
		            ComponentHelper.getComponentHelper(getCurrentProject()).addDataTypeDefinition(def);
				} else {
					try {
						Class taskClass = Class.forName(taskClassName);
						getCurrentProject().addTaskDefinition(taskName, taskClass);
					} catch (ClassNotFoundException e) {
						String message= MessageFormat.format(InternalAntMessages.getString("InternalAntRunner.161"), new String[]{taskClassName, taskName}); //$NON-NLS-1$
						getCurrentProject().log(message, Project.MSG_WARN);
					}	
				}
			}
		}
	}

	private void setTypes() {
		if (eclipseSpecifiedTypes != null) {
			Iterator itr= eclipseSpecifiedTypes.keySet().iterator();
			String typeName;
			String typeClassName;
			while (itr.hasNext()) {
				typeName = (String) itr.next();
				typeClassName= (String) eclipseSpecifiedTypes.get(typeName);
				if (isVersionCompatible("1.6")) { //$NON-NLS-1$
					AntTypeDefinition def = new AntTypeDefinition();
	                def.setName(typeName);
	                def.setClassName(typeClassName);
	                def.setClassLoader(this.getClass().getClassLoader());
	                ComponentHelper.getComponentHelper(getCurrentProject()).addDataTypeDefinition(def);
				} else {
					try {
						Class typeClass = Class.forName(typeClassName);
						getCurrentProject().addDataTypeDefinition(typeName, typeClass);
					} catch (ClassNotFoundException e) {
						String message= MessageFormat.format(InternalAntMessages.getString("InternalAntRunner.162"), new String[]{typeClassName, typeName}); //$NON-NLS-1$
						getCurrentProject().log(message, Project.MSG_WARN);
					}	
				}
			}
		}
	}

	private void remapSystemIn() {
		if (!isVersionCompatible("1.6")) { //$NON-NLS-1$
			return;
		}
		DemuxInputStreamSetter setter= new DemuxInputStreamSetter();
		setter.remapSystemIn(getCurrentProject());
	}
	
	private void validateDefaultTarget() {
		defaultTarget = getCurrentProject().getDefaultTarget();
		
		Enumeration currentTargets = getCurrentProject().getTargets().elements();
		boolean defaultFound= false;
		while (currentTargets.hasMoreElements()) {
			Target target = (Target) currentTargets.nextElement();
			if (target.getName().equals(defaultTarget)) {
				defaultFound= true;
				break;
			}
		}
		
		if (!defaultFound) {
			//default target must exist
			throw new BuildException(MessageFormat.format(InternalAntMessages.getString("InternalAntRunner.Default_target_{0}{1}{2}_does_not_exist_in_this_project_1"), new String[]{"'", defaultTarget, "'"})); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
	}

	/**
	 * Creates and returns the default build logger for logging build events to the ant log.
	 * 
	 * @return the default build logger for logging build events to the ant log
	 * 			can return <code>null</code> if no logging is to occur
	 */
	private BuildLogger createLogger() {
		if (loggerClassname == null) {
			buildLogger= new DefaultLogger();
		} else if (!"".equals(loggerClassname)) { //$NON-NLS-1$
			try {
				buildLogger = (BuildLogger) (Class.forName(loggerClassname).newInstance());
			} catch (ClassCastException e) {
				String message = MessageFormat.format(InternalAntMessages.getString("InternalAntRunner.{0}_which_was_specified_to_perform_logging_is_not_an_instance_of_org.apache.tools.ant.BuildLogger._2"), new String[]{loggerClassname}); //$NON-NLS-1$
				logMessage(null, message, Project.MSG_ERR);
				throw new BuildException(message, e);
			} catch (Exception e) {
				String message = MessageFormat.format(InternalAntMessages.getString("InternalAntRunner.Unable_to_instantiate_logger__{0}_6"), new String[]{loggerClassname}); //$NON-NLS-1$
				logMessage(null, message, Project.MSG_ERR);
				throw new BuildException(message, e);
			}
		} 
		
		if (buildLogger != null) {
			buildLogger.setMessageOutputLevel(messageOutputLevel);
			buildLogger.setOutputPrintStream(out);
			buildLogger.setErrorPrintStream(err);
			buildLogger.setEmacsMode(emacsMode);
		}

		return buildLogger;
	}

	/*
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

	private void fireBuildFinished(Project project, Throwable error) {
		if (error == null && scriptExecuted) {
			logMessage(project, InternalAntMessages.getString("InternalAntRunner.BUILD_SUCCESSFUL_1"), messageOutputLevel); //$NON-NLS-1$
		}
		project.fireBuildFinished(error);
	}

	private void logMessage(Project project, String message, int priority) {
		if (project != null) {
			project.log(message, priority);	
		} else {
			if (buildListeners != null) {
				project = new Project();
				BuildEvent event = new BuildEvent(project);
				event.setMessage(message, priority);
				//notify the build listeners that are not registered as
				//no project existed
				for (Iterator iterator = buildListeners.iterator(); iterator.hasNext();) {
					try {
						BuildListener listener = (BuildListener) iterator.next();
						listener.messageLogged(event);
					} catch (ClassCastException e) {
						//ignore we could be trying to log that a build listener is the
						//wrong type of class
					}
				}
			}
		}
	}

	/**
	 * Sets the buildFileLocation.
	 * 
	 * @param buildFileLocation the file system location of the build file
	 */
	private void setBuildFileLocation(String buildFileLocation) {
		this.buildFileLocation = buildFileLocation;
		if (getCurrentProject() != null) {
			getCurrentProject().setUserProperty("ant.file", buildFileLocation); //$NON-NLS-1$
		}
	}

	private String getBuildFileLocation() {
		if (buildFileLocation == null) {
			buildFileLocation = new File("build.xml").getAbsolutePath(); //$NON-NLS-1$
		}
		return buildFileLocation;
	}

	/**
	 * Sets the message output level. Use -1 for none.
	 * @param level The message output level
	 */
	private void setMessageOutputLevel(int level) {
		messageOutputLevel = level;
		if (buildLogger != null) {
			buildLogger.setMessageOutputLevel(level);
		}
	}
	
	/*
	 * Returns a String representation of the Ant version number as specified
	 * in the version.txt file.
	 */
	private String getAntVersionNumber() throws BuildException {
		if (antVersionNumber == null) {
			try {
				Properties props = new Properties();
				InputStream in = Main.class.getResourceAsStream("/org/apache/tools/ant/version.txt"); //$NON-NLS-1$
				props.load(in);
				in.close();
				String versionNumber= props.getProperty("VERSION");  //$NON-NLS-1$
				antVersionNumber= versionNumber;
			} catch (IOException ioe) {
				throw new BuildException(MessageFormat.format(InternalAntMessages.getString("InternalAntRunner.Could_not_load_the_version_information._{0}_9"), new String[]{ioe.getMessage()})); //$NON-NLS-1$
			} catch (NullPointerException npe) {
				throw new BuildException(InternalAntMessages.getString("InternalAntRunner.Could_not_load_the_version_information._10")); //$NON-NLS-1$
			}
		}
		return antVersionNumber;
	}
	
	/*
	 * Returns whether the given version is compatible with the
	 * current Ant version. A version is compatible if it is less
	 * than or equal to the current version. 
	 */
	private boolean isVersionCompatible(String comparison) {
		String version= getAntVersionNumber();
		return version.compareTo(comparison) >= 0;
	}
	
	private boolean preprocessCommandLine(List commands) {
		
		String arg = getArgument(commands, "-listener"); //$NON-NLS-1$
		while (arg != null) {
			if (arg.length() == 0) {
				throw new BuildException(InternalAntMessages.getString("InternalAntRunner.You_must_specify_a_classname_when_using_the_-listener_argument_1")); //$NON-NLS-1$
			} 
			if (buildListeners == null) {
				buildListeners= new ArrayList(1);
			}
			buildListeners.add(arg);
			arg = getArgument(commands, "-listener"); //$NON-NLS-1$
		}

		arg = getArgument(commands, "-logger"); //$NON-NLS-1$
		if (arg != null) {
			if (arg.length() == 0) {
				throw new BuildException(InternalAntMessages.getString("InternalAntRunner.You_must_specify_a_classname_when_using_the_-logger_argument_2")); //$NON-NLS-1$
			} 
			loggerClassname = arg;
		}
		arg = getArgument(commands, "-logger"); //$NON-NLS-1$
		if (arg != null) {
			throw new BuildException(InternalAntMessages.getString("InternalAntRunner.Only_one_logger_class_may_be_specified_1")); //$NON-NLS-1$
		}
		
		arg = getArgument(commands, "-inputhandler"); //$NON-NLS-1$
		if (arg != null) {
			if (!isVersionCompatible("1.5")) { //$NON-NLS-1$
				throw new BuildException(InternalAntMessages.getString("InternalAntRunner.Specifying_an_InputHandler_is_an_Ant_1.5.*_feature._Please_update_your_Ant_classpath_to_include_an_Ant_version_greater_than_this._2")); //$NON-NLS-1$
			}
			if (arg.length() == 0) {
				throw new BuildException(InternalAntMessages.getString("InternalAntRunner.You_must_specify_a_classname_when_using_the_-inputhandler_argument_1")); //$NON-NLS-1$
			} 
			inputHandlerClassname = arg;
		}
		arg = getArgument(commands, "-inputhandler"); //$NON-NLS-1$
		if (arg != null) {
			throw new BuildException(InternalAntMessages.getString("InternalAntRunner.Only_one_input_handler_class_may_be_specified._2")); //$NON-NLS-1$
		}
		return true;
	}
	
	/*
	 * Looks for interesting command line arguments. 
	 * Returns whether it is OK to run the script.
	 */
	private boolean processCommandLine(List commands) {
		
		if (commands.remove("-help") || commands.remove("-h")) { //$NON-NLS-1$ //$NON-NLS-2$
			printUsage();
			return false;
		}
		
		if (commands.remove("-version")) { //$NON-NLS-1$
			printVersion();
			return false;
		}
		
		if (commands.remove("-verbose") || commands.remove("-v")) { //$NON-NLS-1$ //$NON-NLS-2$
			printVersion();
			setMessageOutputLevel(Project.MSG_VERBOSE);
		}
		
		if (commands.remove("-debug") || commands.remove("-d")) { //$NON-NLS-1$ //$NON-NLS-2$
			printVersion();
			setMessageOutputLevel(Project.MSG_DEBUG);
		}
		
		if (commands.remove("-quiet") || commands.remove("-q")) { //$NON-NLS-1$ //$NON-NLS-2$
			setMessageOutputLevel(Project.MSG_WARN);
		}

		if (commands.remove("-emacs") || commands.remove("-e")) { //$NON-NLS-1$ //$NON-NLS-2$
			emacsMode = true;
			if (buildLogger != null) {
				buildLogger.setEmacsMode(true);
			}
		}
		
		if (commands.remove("-diagnostics")) { //$NON-NLS-1$
			if (!isVersionCompatible("1.5")) { //$NON-NLS-1$
				throw new BuildException(InternalAntMessages.getString("InternalAntRunner.The_diagnositics_options_is_an_Ant_1.5.*_feature._Please_update_your_Ant_classpath_to_include_an_Ant_version_greater_than_this._4")); //$NON-NLS-1$
			}
			try {
				Diagnostics.doReport(System.out);
			} catch (NullPointerException e) {
				logMessage(getCurrentProject(), InternalAntMessages.getString("InternalAntRunner.ANT_HOME_must_be_set_to_use_Ant_diagnostics_2"), Project.MSG_ERR); //$NON-NLS-1$
			}
			return false;
		}
		
		String arg = getArgument(commands, "-logfile"); //$NON-NLS-1$
		if (arg == null) {
			arg = getArgument(commands, "-l"); //$NON-NLS-1$
		}
		if (arg != null) {
			if (arg.length() == 0) {
				String message= InternalAntMessages.getString("InternalAntRunner.You_must_specify_a_log_file_when_using_the_-log_argument_3"); //$NON-NLS-1$
				logMessage(currentProject, message, Project.MSG_ERR); 
				throw new BuildException(message);
			} 
			try {
				createLogFile(arg);
			} catch (IOException e) {
				// just log message and ignore exception
				logMessage(getCurrentProject(), MessageFormat.format(InternalAntMessages.getString("InternalAntRunner.Could_not_write_to_the_specified_log_file__{0}._Make_sure_the_path_exists_and_you_have_write_permissions._2"), new String[]{arg}), Project.MSG_ERR); //$NON-NLS-1$
				return false;
			}
		
		}
		
		arg = getArgument(commands, "-buildfile"); //$NON-NLS-1$
		if (arg == null) {
			arg = getArgument(commands, "-file"); //$NON-NLS-1$
			if (arg == null) {
				arg = getArgument(commands, "-f"); //$NON-NLS-1$
			}
		}
		
		if (arg != null) {
			if (arg.length() == 0) {
				String message= InternalAntMessages.getString("InternalAntRunner.You_must_specify_a_buildfile_when_using_the_-buildfile_argument_4"); //$NON-NLS-1$
				logMessage(currentProject, message, Project.MSG_ERR); 
				throw new BuildException(message);
			} 
			setBuildFileLocation(arg);
		}
		
		if (isVersionCompatible("1.6")) { //$NON-NLS-1$
			if (commands.remove("-k") || commands.remove("-keep-going")) { //$NON-NLS-1$ //$NON-NLS-2$
				keepGoing= true;
			}
			if (commands.remove("-noinput")) { //$NON-NLS-1$
				allowInput= false;
			}
			arg= getArgument(commands, "-lib"); //$NON-NLS-1$
			if (arg != null) {
				logMessage(currentProject, InternalAntMessages.getString("InternalAntRunner.157"), Project.MSG_ERR); //$NON-NLS-1$
				return false;
			}
		}
		
		arg= getArgument(commands, "-find"); //$NON-NLS-1$
		if (arg == null) {
			arg= getArgument(commands, "-s"); //$NON-NLS-1$
		}
		if (arg != null) {
			logMessage(currentProject, InternalAntMessages.getString("InternalAntRunner.-find_not_supported"), Project.MSG_ERR); //$NON-NLS-1$
			return false;
		}
		
		processTasksAndTypes(commands);
		
		if ((commands != null) && (!commands.isEmpty())) {
			processUnrecognizedCommands(commands);
		}

		if ((commands != null) && (!commands.isEmpty())) {
			processTargets(commands);
		}
		
		return true;
	}
	
	private void processTasksAndTypes(List commands) {
		String arg = getArgument(commands, "-eclipseTask"); //$NON-NLS-1$
		while (arg != null) {
			if (eclipseSpecifiedTasks == null) {
				eclipseSpecifiedTasks= new HashMap();
			}
			int index= arg.indexOf(',');
			if (index != -1) {
				String name= arg.substring(0, index);
				String className= arg.substring(index + 1);
				eclipseSpecifiedTasks.put(name, className);
			}
			arg = getArgument(commands, "-eclipseTask"); //$NON-NLS-1$
		}
		
		arg = getArgument(commands, "-eclipseType"); //$NON-NLS-1$
		while (arg != null) {
			if (eclipseSpecifiedTypes == null) {
				eclipseSpecifiedTypes= new HashMap();
			}
			int index= arg.indexOf(',');
			if (index != -1) {	
				String name= arg.substring(0, index);
				String className= arg.substring(index + 1);
				eclipseSpecifiedTypes.put(name, className);
			}
			arg = getArgument(commands, "-eclipseType"); //$NON-NLS-1$
		}
	}

	/*
	 * Checks for unrecognized arguments on the command line.
	 * Since there is no syntactic way to distingush between
	 * ant -foo target1 target2
	 * ant -foo fooarg target
	 * we remove everything up to the last argument that
	 * begins with a '-'.  In the latter case, above, that
	 * means that there will be an extra target, 'fooarg',
	 * left lying around.
	 */
	private void processUnrecognizedCommands(List commands) {
		int p = -1;

		// find the last arg that begins with '-'
		for (int i = commands.size() - 1; i >= 0; i--) {
			if (((String) commands.get(0)).startsWith("-")) { //$NON-NLS-1$
				p = i;
				break;
			}
		}
		if (p < 0) { return; }

		// remove everything preceding that last '-arg'
		String s = ""; //$NON-NLS-1$
		for (int i = 0; i <= p; i++) {
			s += " " + ((String) commands.get(0)); //$NON-NLS-1$
			commands.remove(0);
		}
		
		// warn of ignored commands
		String message = MessageFormat.format(InternalAntMessages.getString("InternalAntRunner.Unknown_argument__{0}_2"), new Object[]{ s.substring(1) }); //$NON-NLS-1$
		logMessage(currentProject, message, Project.MSG_WARN); 
	}
	

	/*
	 * Checks for targets specified at the command line.
	 */
	private void processTargets(List commands) {
		if (targets == null) {
			targets = new Vector(commands.size());
		}
		for (Iterator iter = commands.iterator(); iter.hasNext();) {
			targets.add(iter.next());
		}
	}

	/*
	 * Creates the log file with the name specified by the user.
	 * If the fileName is not absolute, the file will be created in the
	 * working directory if specified or in the same directory as the location
	 * of the build file.
	 */
	private void createLogFile(String fileName) throws FileNotFoundException, IOException {
		File logFile = getFileRelativeToBaseDir(fileName);
		
		//this stream is closed in the finally block of run(list)
		out = new PrintStream(new FileOutputStream(logFile));
		err = out;
		logMessage(getCurrentProject(), MessageFormat.format(InternalAntMessages.getString("InternalAntRunner.Using_{0}_file_as_build_log._1"), new String[]{logFile.getCanonicalPath()}), Project.MSG_INFO); //$NON-NLS-1$
		if (buildLogger != null) {
			buildLogger.setErrorPrintStream(err);
			buildLogger.setOutputPrintStream(out);
		}
	}

	private File getFileRelativeToBaseDir(String fileName) {
		File parentFile= null;
		
		String base= getCurrentProject().getUserProperty("basedir"); //$NON-NLS-1$
		if (base != null) {
			parentFile= new File(base);
		} else {
			//relative to the build file location
			parentFile= new File(getBuildFileLocation()).getParentFile();
		}
		
		return FileUtils.newFileUtils().resolveFile(parentFile, fileName);
	}

	/*
	 * Processes cmd line properties and adds the user properties
	 * Any user properties that have been explicitly set are set as well.
	 * Ensures that -D properties take precedence.
	 */
	private void processProperties(List commands) {
		//MULTIPLE property files are allowed
		String arg= getArgument(commands, "-propertyfile"); //$NON-NLS-1$
		while (arg != null) {
			if (!isVersionCompatible("1.5")) { //$NON-NLS-1$
				logMessage(currentProject, InternalAntMessages.getString("InternalAntRunner.Specifying_property_files_is_a_Ant_1.5.*_feature._Please_update_your_Ant_classpath._6"), Project.MSG_ERR); //$NON-NLS-1$
				break;
			}
			if (arg.length() == 0) {
				String message= InternalAntMessages.getString("InternalAntRunner.You_must_specify_a_property_filename_when_using_the_-propertyfile_argument_3"); //$NON-NLS-1$
				logMessage(currentProject, message, Project.MSG_ERR); 
				throw new BuildException(message);
			} 
			
			propertyFiles.add(arg);
			arg= getArgument(commands, "-propertyfile"); //$NON-NLS-1$
		}
		
		if (propertyFiles != null && !propertyFiles.isEmpty()) {
			loadPropertyFiles();
		}
		
		if (commands == null) {
			return;
		}
		processMinusDProperties(commands);
	}

	private void processMinusDProperties(List commands) {
		String[] args = (String[]) commands.toArray(new String[commands.size()]);
		for (int i = 0; i < args.length; i++) {
			String arg = args[i];
			if (arg.startsWith("-D")) { //$NON-NLS-1$
				String name = arg.substring(2, arg.length());
				String value = null;
				int posEq = name.indexOf("="); //$NON-NLS-1$
				if (posEq == 0) {
					value= name.substring(1);
					name= ""; //$NON-NLS-1$
				} else if (posEq > 0 && posEq != name.length() - 1) {
					value = name.substring(posEq + 1).trim();
					name = name.substring(0, posEq);
				}
				
				if (value == null) {
					//the user has specified something like "-Debug"
					continue;
				}
				if (userProperties == null) {
					userProperties= new HashMap();
				}
				userProperties.put(name, value);
				commands.remove(args[i]);
			}
		}
	}
	
	private void setProperties(Project project) {
		setBuiltInProperties(project);
		if (userProperties != null) {
			for (Iterator iterator = userProperties.entrySet().iterator(); iterator.hasNext();) {
				Map.Entry entry = (Map.Entry) iterator.next();
				project.setUserProperty((String) entry.getKey(), (String) entry.getValue());
			}
		} 
	}

	private void setBuiltInProperties(Project project) {
		project.setUserProperty("ant.file", getBuildFileLocation()); //$NON-NLS-1$
		project.setUserProperty("ant.version", Main.getAntVersion()); //$NON-NLS-1$
	}

	/*
	 * Print the project description, if any
	 */
	private void printHelp(Project project) {
		if (project.getDescription() != null) {
			logMessage(project, project.getDescription(), Project.MSG_INFO);
		}
		printTargets(project);
	}

	/*
	 * Logs a message with the client indicating the version of <b>Ant</b> that this class
	 * fronts.
	 */
	private void printVersion() {
		logMessage(getCurrentProject(), Main.getAntVersion(), Project.MSG_INFO);
	}

	/*
	 * Logs a message with the client outlining the usage of <b>Ant</b>.
	 */
	private void printUsage() {
		String lSep = System.getProperty("line.separator"); //$NON-NLS-1$
		StringBuffer msg = new StringBuffer();
		msg.append("ant ["); //$NON-NLS-1$
		msg.append(InternalAntMessages.getString("InternalAntRunner.options_13")); //$NON-NLS-1$
		msg.append("] ["); //$NON-NLS-1$
		msg.append(InternalAntMessages.getString("InternalAntRunner.target_15")); //$NON-NLS-1$
		msg.append(" ["); //$NON-NLS-1$
		msg.append(InternalAntMessages.getString("InternalAntRunner.target_15")); //$NON-NLS-1$
		msg.append("2 ["); //$NON-NLS-1$
		msg.append(InternalAntMessages.getString("InternalAntRunner.target_15")); //$NON-NLS-1$
		msg.append("3] ...]]"); //$NON-NLS-1$
		msg.append(lSep);
		msg.append(InternalAntMessages.getString("InternalAntRunner.Options___21")); //$NON-NLS-1$
		msg.append(lSep);
		msg.append("\t-help, -h\t\t\t\t"); //$NON-NLS-1$
		msg.append(InternalAntMessages.getString("InternalAntRunner.print_this_message_23")); //$NON-NLS-1$
		msg.append(lSep);
		msg.append("\t-projecthelp, -p\t\t"); //$NON-NLS-1$
		msg.append(InternalAntMessages.getString("InternalAntRunner.print_project_help_information_25")); //$NON-NLS-1$
		msg.append(lSep);
		msg.append("\t-version\t\t\t\t"); //$NON-NLS-1$
		msg.append(InternalAntMessages.getString("InternalAntRunner.print_the_version_information_and_exit_27")); //$NON-NLS-1$
		msg.append(lSep); 
		msg.append("\t-diagnostics\t\t\t"); //$NON-NLS-1$
		msg.append(InternalAntMessages.getString("InternalAntRunner.12")); //$NON-NLS-1$
		msg.append(lSep);
		msg.append(InternalAntMessages.getString("InternalAntRunner.13")); //$NON-NLS-1$
		msg.append(lSep);
		msg.append("\t-quiet, -q\t\t\t"); //$NON-NLS-1$
		msg.append(InternalAntMessages.getString("InternalAntRunner.be_extra_quiet_29")); //$NON-NLS-1$
		msg.append(lSep);
		msg.append("\t-verbose, -v\t\t\t"); //$NON-NLS-1$
		msg.append(InternalAntMessages.getString("InternalAntRunner.be_extra_verbose_31")); //$NON-NLS-1$
		msg.append(lSep);
		msg.append("\t-debug, -d\t\t\t"); //$NON-NLS-1$
		msg.append(InternalAntMessages.getString("InternalAntRunner.print_debugging_information_33")); //$NON-NLS-1$
		msg.append(lSep);
		msg.append("\t-emacs, -e\t\t\t"); //$NON-NLS-1$
		msg.append(InternalAntMessages.getString("InternalAntRunner.produce_logging_information_without_adornments_35")); //$NON-NLS-1$
		msg.append(lSep);
		msg.append("\t-logfile\t<file>\t\t"); //$NON-NLS-1$
		msg.append(InternalAntMessages.getString("InternalAntRunner.use_given_file_for_log_37")); //$NON-NLS-1$
		msg.append(lSep);
		msg.append("\t\t-l\t<file>"); //$NON-NLS-1$
		msg.append(InternalAntMessages.getString("InternalAntRunner.1")); //$NON-NLS-1$ //$NON-NLS-2$
		msg.append(lSep);  
		msg.append("\t-logger <classname>\t\t"); //$NON-NLS-1$
		msg.append(InternalAntMessages.getString("InternalAntRunner.the_class_which_is_to_perform_logging_39")); //$NON-NLS-1$
		msg.append(lSep);  
		msg.append("\t-listener <classname>\t"); //$NON-NLS-1$
		msg.append(InternalAntMessages.getString("InternalAntRunner.add_an_instance_of_class_as_a_project_listener_41")); //$NON-NLS-1$
		msg.append(lSep);
		msg.append("\t-noinput\t"); //$NON-NLS-1$
		msg.append(InternalAntMessages.getString("InternalAntRunner.158")); //$NON-NLS-1$
		msg.append(lSep); 
		msg.append("\t-buildfile\t<file>\t"); //$NON-NLS-1$
		msg.append(InternalAntMessages.getString("InternalAntRunner.use_given_buildfile_43")); //$NON-NLS-1$
		msg.append(lSep); 
		msg.append("\t\t-file\t<file>"); //$NON-NLS-1$
		msg.append(InternalAntMessages.getString("InternalAntRunner.1")); //$NON-NLS-1$
		msg.append(lSep);
		msg.append("\t\t-f\t\t<file>"); //$NON-NLS-1$
		msg.append(InternalAntMessages.getString("InternalAntRunner.1")); //$NON-NLS-1$
		msg.append(lSep);
		msg.append("\t-D<property>=<value>\t"); //$NON-NLS-1$
		msg.append(InternalAntMessages.getString("InternalAntRunner.use_value_for_given_property_45")); //$NON-NLS-1$
		msg.append(lSep);
		msg.append("\t-keep-going, -k"); //$NON-NLS-1$
		msg.append(InternalAntMessages.getString("InternalAntRunner.159")); //$NON-NLS-1$
		msg.append(lSep);
		msg.append(InternalAntMessages.getString("InternalAntRunner.160")); //$NON-NLS-1$
		msg.append(lSep); 
		msg.append("\t-propertyfile <name>\t"); //$NON-NLS-1$
		msg.append(InternalAntMessages.getString("InternalAntRunner.19")); //$NON-NLS-1$
		msg.append(lSep);
		msg.append(InternalAntMessages.getString("InternalAntRunner.20")); //$NON-NLS-1$
		msg.append(lSep);
		msg.append("\t-inputhandler <class>\t"); //$NON-NLS-1$
		msg.append(InternalAntMessages.getString("InternalAntRunner.22")); //$NON-NLS-1$
		msg.append(lSep);

		logMessage(getCurrentProject(), msg.toString(), Project.MSG_INFO);
	}

	/*
	 * From a command line list, return the argument for the given parameter.
	 * The parameter and its argument are removed from the list.
	 * 
	 * @return <code>null</code> if the parameter is not found 
	 * 			or an empty String if no arguments are found
	 */
	private String getArgument(List commands, String param) {
		if (commands == null) {
			return null;
		}
		int index = commands.indexOf(param);
		if (index == -1) {
			return null;
		}
		commands.remove(index);
		if (index == commands.size()) {// if this is the last command
			return ""; //$NON-NLS-1$
		}
		
		String command = (String) commands.get(index);
		if (command.startsWith("-")) { //new parameter //$NON-NLS-1$
			return ""; //$NON-NLS-1$
		}
		
		commands.remove(index);
		return command;
	}

	/*
	 * Helper method to ensure an array is converted into an ArrayList.
	 */
	private static ArrayList getArrayList(String[] args) {
		if (args == null) {
			return null;
		}
		// We could be using Arrays.asList() here, but it does not specify
		// what kind of list it will return. We need a list that
		// implements the method List.remove(Object) and ArrayList does.
		ArrayList result = new ArrayList(args.length);
		for (int i = 0; i < args.length; i++) {
			result.add(args[i]);
		}
		return result;
	}

	private Project getCurrentProject() {
		return currentProject;
	}

	private void setCurrentProject(Project currentProject) {
		this.currentProject = currentProject;
	}
	
	/**
	 * Load all properties from the files 
	 * specified by -propertyfile.
	 */
	private void loadPropertyFiles() {
		Iterator itr= propertyFiles.iterator();
        while (itr.hasNext()) {
            String filename= (String) itr.next();
           	File file= getFileRelativeToBaseDir(filename);
            Properties props = new Properties();
            FileInputStream fis = null;
            try {
                fis = new FileInputStream(file);
                props.load(fis);
            } catch (IOException e) {
            	String msg= MessageFormat.format(InternalAntMessages.getString("InternalAntRunner.Could_not_load_property_file_{0}__{1}_4"), new String[]{filename, e.getMessage()}); //$NON-NLS-1$
            	logMessage(getCurrentProject(), msg, Project.MSG_ERR);
            } finally {
                if (fis != null) {
                    try {
                        fis.close();
                    } catch (IOException e){
                    }
                }
            }

            if (userProperties == null) {
            	userProperties= new HashMap();
            }
            Enumeration propertyNames = props.propertyNames();
            while (propertyNames.hasMoreElements()) {
                String name = (String) propertyNames.nextElement();
                //most specific to global
                //do not overwrite specific with a global property
                if (userProperties.get(name) == null) {
            		userProperties.put(name, props.getProperty(name));
                }
            }
        }
	}
	
	/*
     * Creates the InputHandler and adds it to the project.
     *
     * @exception BuildException if a specified InputHandler
     *                           implementation could not be loaded.
     */
    private void addInputHandler(Project project) {
    	if (!isVersionCompatible("1.5")) { //$NON-NLS-1$
			return;
		}
		InputHandlerSetter setter= new InputHandlerSetter();
		setter.setInputHandler(project, inputHandlerClassname);
    }
}