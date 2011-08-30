/*******************************************************************************
 *  Copyright (c) 2000, 2011 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - derived implementation
 *     Blake Meike (blakem@world.std.com)- patch for bug 31691 and bug 34488
 *******************************************************************************/

package org.eclipse.ant.internal.core.ant;
 
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
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
import org.apache.tools.ant.XmlLogger;
import org.eclipse.ant.core.AntCorePlugin;
import org.eclipse.ant.core.AntCorePreferences;
import org.eclipse.ant.core.AntSecurityException;
import org.eclipse.ant.core.Property;
import org.eclipse.ant.core.Type;
import org.eclipse.ant.internal.core.AbstractEclipseBuildLogger;
import org.eclipse.ant.internal.core.AntCoreUtil;
import org.eclipse.ant.internal.core.AntSecurityManager;
import org.eclipse.ant.internal.core.IAntCoreConstants;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.variables.VariablesPlugin;
import org.osgi.framework.Bundle;

/**
 * Eclipse application entry point into Ant. Derived from the original Ant Main class
 * to ensure that the functionality is equivalent when running in the platform.
 */
public class InternalAntRunner {

	private IProgressMonitor monitor;

	private List buildListeners;

	private String buildFileLocation;

	/** 
	 * Targets we want to run.
	 */
	private Vector targets;

	private Map userProperties;
	private boolean noExplicitUserProperties= true;
	
	private Project currentProject;
	
	private String defaultTarget;
	
	private BuildLogger buildLogger= null;
	
	/**
	 * Cache of the Ant version number when it has been loaded
	 */
	private String antVersionNumber= null;

	/** Current message output status. Follows Project.MSG_XXX */
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
	 * name indicates that the <code>org.apache.tools.ant.DefaultLogger</code> will be used.
	 */
	private String loggerClassname = null;

	/** Extra arguments to be parsed as command line arguments. */
	private String[] extraArguments = null;
	
	private boolean executed = false;
	
	private List propertyFiles= new ArrayList();
	
	private URL[] customClasspath= null;
	
	/**
     * The Ant InputHandler class. There may be only one input handler.
     */
    private String inputHandlerClassname = null;
    
    private String buildAntHome= null;
    
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
    
    private String fEarlyErrorMessage= null;

	private boolean unknownTargetsFound = false;
    
	/**
	 * Adds a build listener.
	 * 
	 * @param classNames the fully qualified names of the build listeners to be added
	 */
	public void addBuildListeners(List classNames) {
		if (buildListeners == null) {
			buildListeners = new ArrayList(classNames.size());
		}
		buildListeners.addAll(classNames);
	}

	/**
	 * Adds a build logger. There can be only one build logger.
	 * @param className The fully qualified name of the build logger to add
	 */
	public void addBuildLogger(String className) {
		loggerClassname = className;
	}

	/**
	 * Adds user properties to the current collection of user properties.
	 * @param properties The user properties to be added
	 */
	public void addUserProperties(Map properties) {
		if (userProperties == null) {
			userProperties= new HashMap(properties);
		} else {
			userProperties.putAll(properties);
		}
		noExplicitUserProperties= false;
	}
	
	/**
	 * Adds user property files.
	 * @param additionalPropertyFiles The property files to add
	 * @since 2.1
	 */
	public void addPropertyFiles(String[] additionalPropertyFiles) {
		propertyFiles.addAll(Arrays.asList(additionalPropertyFiles));
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
			String message = MessageFormat.format(InternalAntMessages.InternalAntRunner_not_an_instance_of_apache_ant_BuildListener, new String[]{className});
			logMessage(null, message, Project.MSG_ERR);
			throw new BuildException(message, e);
		} catch (BuildException e) {
			throw e;
		} catch (Exception e) {
			throw new BuildException(e);
		}
	}

	private void setProperties(Project project, boolean substituteVariables) {
		setBuiltInProperties(project);
		if (userProperties != null) {
			for (Iterator iterator = userProperties.entrySet().iterator(); iterator.hasNext();) {
				Map.Entry entry = (Map.Entry) iterator.next();
				String value= (String) entry.getValue();
				if (substituteVariables && value != null) {
					try {
						value= VariablesPlugin.getDefault().getStringVariableManager().performStringSubstitution(value);
					} catch (CoreException e) {
					}
				}
				project.setUserProperty((String) entry.getKey(), value);
				
			}
			//may have properties set (always have the Ant process ID)
			//using the Arguments and not the Properties page
			//if set using the arguments, still include the global properties
			if (noExplicitUserProperties) {
				setGlobalProperties(project, substituteVariables);
			}
		} else {
			setGlobalProperties(project, substituteVariables);
		}
	}

	private void setBuiltInProperties(Project project) {
		//note also see processAntHome for system properties that are set
		project.setUserProperty("ant.file", getBuildFileLocation()); //$NON-NLS-1$
		project.setUserProperty("ant.version", Main.getAntVersion()); //$NON-NLS-1$
	}
	
	private void setGlobalProperties(Project project, boolean substituteVariables) {
		AntCorePreferences prefs= AntCorePlugin.getPlugin().getPreferences();
		List properties= prefs.getProperties();
		if (properties != null) {
			for (Iterator iter = properties.iterator(); iter.hasNext();) {
				Property property = (Property) iter.next();
				String value= property.getValue(substituteVariables);
				if (value != null) {
					project.setUserProperty(property.getName(), value);
				}
			}
		}
	}

	private void setTasks(Project project) {
		List tasks = AntCorePlugin.getPlugin().getPreferences().getTasks();
		
		for (Iterator iterator = tasks.iterator(); iterator.hasNext();) {
			org.eclipse.ant.core.Task task = (org.eclipse.ant.core.Task) iterator.next();
			
			if (isVersionCompatible("1.6")) { //$NON-NLS-1$
				AntTypeDefinition def= new AntTypeDefinition();
				String name = ProjectHelper.genComponentName(task.getURI(),task.getTaskName());
				def.setName(name);
	            def.setClassName(task.getClassName());
	            def.setClassLoader(this.getClass().getClassLoader());
	            def.setAdaptToClass(Task.class);
	            def.setAdapterClass(TaskAdapter.class);
	            ComponentHelper.getComponentHelper(project).addDataTypeDefinition(def);
			} else {
				try {
					Class taskClass = Class.forName(task.getClassName());
					if (isVersionCompatible("1.5")) { //$NON-NLS-1$
						try {
							project.checkTaskClass(taskClass);
						} catch (BuildException e) {
							IStatus status = new Status(IStatus.ERROR, AntCorePlugin.PI_ANTCORE, AntCorePlugin.ERROR_RUNNING_BUILD, MessageFormat.format(InternalAntMessages.InternalAntRunner_Error_setting_Ant_task, new String[]{task.getTaskName()}), e);
							AntCorePlugin.getPlugin().getLog().log(status);
							continue;
						}
						}
					project.addTaskDefinition(task.getTaskName(), taskClass);
				} catch (ClassNotFoundException e) {
					IStatus status = new Status(IStatus.ERROR, AntCorePlugin.PI_ANTCORE, AntCorePlugin.ERROR_RUNNING_BUILD, MessageFormat.format(InternalAntMessages.InternalAntRunner_Class_not_found_for_task, new String[]{task.getClassName(), task.getTaskName()}), e);
					AntCorePlugin.getPlugin().getLog().log(status);
					}
				}
		}
	}

	private void setTypes(Project project) {
		List types = AntCorePlugin.getPlugin().getPreferences().getTypes();
		for (Iterator iterator = types.iterator(); iterator.hasNext();) {
			Type type = (Type) iterator.next();
			if (isVersionCompatible("1.6")) { //$NON-NLS-1$
				AntTypeDefinition def = new AntTypeDefinition();
				String name= ProjectHelper.genComponentName(type.getURI(), type.getTypeName());
                def.setName(name);
                def.setClassName(type.getClassName());
                def.setClassLoader(this.getClass().getClassLoader());
                ComponentHelper.getComponentHelper(project).addDataTypeDefinition(def);
			} else {
				try {
					Class typeClass = Class.forName(type.getClassName());
					project.addDataTypeDefinition(type.getTypeName(), typeClass);
				} catch (ClassNotFoundException e) {
					IStatus status = new Status(IStatus.ERROR, AntCorePlugin.PI_ANTCORE, AntCorePlugin.ERROR_RUNNING_BUILD, MessageFormat.format(InternalAntMessages.InternalAntRunner_Class_not_found_for_type, new String[]{type.getClassName(), type.getTypeName()}), e);
					AntCorePlugin.getPlugin().getLog().log(status);
				}
			}
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
			throw new BuildException(MessageFormat.format(InternalAntMessages.InternalAntRunner_Buildfile_does_not_exist,
						 new String[]{buildFile.getAbsolutePath()}));
		}
		if (!buildFile.isFile()) {
			throw new BuildException(MessageFormat.format(InternalAntMessages.InternalAntRunner_Buildfile_is_not_a_file,
							new String[]{buildFile.getAbsolutePath()}));
		}
		
        if (!isVersionCompatible("1.5")) { //$NON-NLS-1$
            parseBuildFile(project, buildFile);
        } else {
            ProjectHelper helper = ProjectHelper.getProjectHelper();
            project.addReference("ant.projectHelper", helper); //$NON-NLS-1$
            helper.parse(project, buildFile);
        }
	}
    
    /**
     * @deprecated support for Ant older than 1.5
     */
    private void parseBuildFile(Project project, File buildFile) {
        ProjectHelper.configureProject(project, buildFile);   
    }

	/**
	 * Gets all the target information from the build script.
	 * Returns a list of lists. Each item in the enclosing list represents a
	 * target, where the first element is the name, the
	 * second element is the description, the third element is the
	 * project name, and the last elements is an array of dependencies.
	 * @return a list of lists representing the targets
	 */
	public List getTargets() {
		try {
		    setJavaClassPath();
			Project antProject;
		
			antProject = getProject();
			processAntHome(false);
			antProject.init();
			setTypes(antProject);
			boolean exceptionState= processProperties(AntCoreUtil.getArrayList(extraArguments));
            if (fEarlyErrorMessage != null) {
                if (exceptionState) {
                    throw new BuildException(fEarlyErrorMessage);
                }
            }
			
			setProperties(antProject, false);
			if (isVersionCompatible("1.5")) { //$NON-NLS-1$
				new InputHandlerSetter().setInputHandler(antProject, "org.eclipse.ant.internal.core.ant.NullInputHandler"); //$NON-NLS-1$
			}
			parseBuildFile(antProject);
			defaultTarget = antProject.getDefaultTarget();
			Enumeration projectTargets = antProject.getTargets().elements();
			List infos= new ArrayList();
			infos.add(antProject.getName());
			infos.add(antProject.getDescription());
			List info;
			Target target;
			boolean defaultFound= false;
			while (projectTargets.hasMoreElements()) {
				target = (Target) projectTargets.nextElement();
				String name= target.getName();
				if (name.length() == 0) {
					//"no name" implicit target of Ant 1.6
					continue;
				}
				info= new ArrayList(4);
				info.add(name);
				if (target.getName().equals(defaultTarget)) {
					defaultFound= true;
				}
				info.add(target.getDescription());
				List dependencies= new ArrayList();
				Enumeration enumeration= target.getDependencies();
				while (enumeration.hasMoreElements()) {
					dependencies.add(enumeration.nextElement());
				}
				String[] dependencyArray= new String[dependencies.size()];
				dependencies.toArray(dependencyArray);
				info.add(dependencyArray);
				infos.add(info);
			}
			if (!defaultFound) {
				//default target must exist
				throw new BuildException(MessageFormat.format(InternalAntMessages.InternalAntRunner_Default_target_does_not_exist, new String[]{"'", defaultTarget, "'"})); //$NON-NLS-1$ //$NON-NLS-2$
			}
			return infos;
		} finally {
			processAntHome(true);
		}
	}
	
	/**
	 * Returns a list of target names in the build script.
	 * 
	 * @return a list of target names
	 */
	private List getTargetNames() {
		try {
		    setJavaClassPath();
			Project antProject;
		
			antProject = getProject();
			processAntHome(false);
			antProject.init();
			setTypes(antProject);
			processProperties(AntCoreUtil.getArrayList(extraArguments));
			
			setProperties(antProject, false);
			if (isVersionCompatible("1.5")) { //$NON-NLS-1$
				new InputHandlerSetter().setInputHandler(antProject, "org.eclipse.ant.internal.core.ant.NullInputHandler"); //$NON-NLS-1$
			}
			parseBuildFile(antProject);
			Enumeration projectTargets = antProject.getTargets().elements();
			List names = new ArrayList();
			Target target;
			while (projectTargets.hasMoreElements()) {
				target = (Target) projectTargets.nextElement();
				String name= target.getName();
				if (name.length() == 0) {
					//"no name" implicit target of Ant 1.6
					continue;
				}
				names.add(name);
			}
			return names;
		} finally {
			processAntHome(true);
		}
	}	

	private Project getProject() {
		Project antProject;
		if (isVersionCompatible("1.6")) { //$NON-NLS-1$
			//in Ant version 1.6 or greater all tasks can exist outside the scope of a target
			if (isVersionCompatible("1.6.3")) { //$NON-NLS-1$
				antProject= new InternalProject2();
			} else {
				antProject= new Project();
			}
		} else {
			antProject= new InternalProject();
		}
		return antProject;
	}
	
	/**
	 * Returns the default target name that was last computed or <code>null</code>
	 * if no default target has been computed.
	 * @return the default target name
	 */
	public String getDefaultTarget() {
		return defaultTarget;
	}

	/**
	 * Runs the build script.
	 */
	public void run() {
		run(AntCoreUtil.getArrayList(extraArguments));
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
		project.log(MessageFormat.format(InternalAntMessages.InternalAntRunner_Arguments, new String[]{sb.toString().trim()}));
	}

	private void createMonitorBuildListener(Project project) {
		if (monitor == null) {
			return;
		}
		List chosenTargets = targets;
		if (chosenTargets == null || chosenTargets.isEmpty()) {
			chosenTargets = new ArrayList(1);
			String defltTarget= project.getDefaultTarget();
			if (defltTarget != null) {
				chosenTargets.add(defltTarget);
			}
		}
		project.addBuildListener(new ProgressBuildListener(project, chosenTargets, monitor));
	}

	/**
	 * Invokes the building of a project object and executes a build using either a given
	 * target or the default target. This method is called if running in
	 * headless mode.
	 * @see org.eclipse.ant.core.AntRunner#run(Object)
	 * @param argArray the command line arguments
	 * @exception Exception execution exceptions
	 */
	public void run(Object argArray) throws Exception {
		run(AntCoreUtil.getArrayList((String[]) argArray));
	}

	/*
	 * Note that the list passed to this method must support
	 * List#remove(Object)
	 */
	private void run(List argList) {
		setCurrentProject(new Project());
        if (isVersionCompatible("1.6.3")) { //$NON-NLS-1$
           new ExecutorSetter().setExecutor(getCurrentProject());
        }
		Throwable error = null;
		PrintStream originalErr = System.err;
		PrintStream originalOut = System.out;
		InputStream originalIn= System.in;
		
		SecurityManager originalSM= System.getSecurityManager();
		setJavaClassPath();
		executed = true;
		processAntHome(false);
		try {
			if (argList != null && (argList.remove("-projecthelp") || argList.remove("-p"))) { //$NON-NLS-1$ //$NON-NLS-2$
				projectHelp = true;
			}
			getCurrentProject().init();
			if (argList != null) {
				executed = preprocessCommandLine(argList);
				if (!executed) {
					return;
				}
			}
			
            boolean exceptionState= processProperties(argList);
            
			addBuildListeners(getCurrentProject());
            
			addInputHandler(getCurrentProject());
			
			remapSystemIn();
			System.setOut(new PrintStream(new DemuxOutputStream(getCurrentProject(), false)));
			System.setErr(new PrintStream(new DemuxOutputStream(getCurrentProject(), true)));
			
			if (!projectHelp) {
				fireBuildStarted(getCurrentProject());
			}
            
            if (fEarlyErrorMessage != null) {
                //an error occurred processing the properties
                //build started has fired and we have
                //listeners/loggers to report the error
                logMessage(getCurrentProject(), fEarlyErrorMessage, Project.MSG_ERR);
                if (exceptionState) {
                    throw new BuildException(fEarlyErrorMessage);
                }
            }
            
            //properties can only be set after buildStarted as some listeners/loggers
            //depend on this (e.g. XMLLogger)
            setProperties(getCurrentProject(), true);
			
			if (argList != null && !argList.isEmpty()) {
				try {
					executed = processCommandLine(argList);
				} catch (BuildException e) {
					executed = false;
					throw e;
				}
			}
			if (!executed) {
				return;
			}
			
            //needs to occur after processCommandLine(List)
			if (allowInput && (inputHandlerClassname != null && inputHandlerClassname.length() > 0)) {
				if (isVersionCompatible("1.6")) { //$NON-NLS-1$
					//https://bugs.eclipse.org/bugs/show_bug.cgi?id=182577
					//getCurrentProject().setDefaultInputStream(originalIn);
					System.getProperties().remove("eclipse.ant.noInput");  //$NON-NLS-1$
				}
			} else {
				//set the system property that any input handler
				//can check to see if handling input is allowed
				System.setProperty("eclipse.ant.noInput", "true");  //$NON-NLS-1$//$NON-NLS-2$
				if (isVersionCompatible("1.5") && (inputHandlerClassname == null || inputHandlerClassname.length() == 0)) { //$NON-NLS-1$
					InputHandlerSetter setter= new InputHandlerSetter();
					setter.setInputHandler(getCurrentProject(), "org.eclipse.ant.internal.core.ant.FailInputHandler"); //$NON-NLS-1$
				}
			}

			if(!projectHelp) {
				getCurrentProject().log(MessageFormat.format(InternalAntMessages.InternalAntRunner_Build_file, new String[]{getBuildFileLocation()}));

				setTasks(getCurrentProject());
				setTypes(getCurrentProject());

				if (isVersionCompatible("1.6")) { //$NON-NLS-1$
					getCurrentProject().setKeepGoingMode(keepGoing);
				}
				parseBuildFile(getCurrentProject());
			}
			
			createMonitorBuildListener(getCurrentProject());
			
			if (projectHelp) {
				if (isVersionCompatible("1.7")) { //$NON-NLS-1$
					new EclipseMainHelper().runProjectHelp(getBuildFileLocation(), getCurrentProject());
					return;
				} 
				getCurrentProject().log(InternalAntMessages.InternalAntRunner_ant_1_7_needed_for_help_info);
				executed = false;
				return;
			}
			
			if (extraArguments != null) {
				printArguments(getCurrentProject());
			}
			System.setSecurityManager(new AntSecurityManager(originalSM, Thread.currentThread()));
			
			if (targets == null) {
                targets= new Vector(1);
            }
            if (targets.isEmpty() && getCurrentProject().getDefaultTarget() != null) {
                targets.add(getCurrentProject().getDefaultTarget());
            }
			if (!isVersionCompatible("1.6.3")) {  //$NON-NLS-1$
	            getCurrentProject().addReference("eclipse.ant.targetVector", targets); //$NON-NLS-1$
			}
			getCurrentProject().executeTargets(targets);
		} catch (OperationCanceledException e) {
			executed = false;
			logMessage(getCurrentProject(), e.getMessage(), Project.MSG_INFO);
			throw e;
		} catch (AntSecurityException e) {
			//expected
		} catch (RuntimeException e) {
			error = e;
			throw e;
		} catch (Error e) {
			error = e;
			throw e;
		} finally {
			System.setErr(originalErr);
			System.setOut(originalOut);
			System.setIn(originalIn);
			if (System.getSecurityManager() instanceof AntSecurityManager) {
				System.setSecurityManager(originalSM);
			}
			
			if (!projectHelp) {		
				if (AntCorePlugin.getPlugin().getBundle().getState() != Bundle.ACTIVE) {
					return;
				}
				fireBuildFinished(getCurrentProject(), error);
			}
						
			//close any user specified build log
			if (err != originalErr) {
				err.close();
			}
			if (out != originalOut) {
				out.close();
			}
			
			processAntHome(true);
			if (!allowInput) {
				System.getProperties().remove("eclipse.ant.noInput");  //$NON-NLS-1$
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

	private void processAntHome(boolean finished) {
		AntCorePreferences prefs= AntCorePlugin.getPlugin().getPreferences();
		String antHome= prefs.getAntHome();
		if (buildAntHome != null && !finished) {
			antHome= buildAntHome;
		}
		if (antHome == null || antHome.length() == 0) {
			System.getProperties().remove("ant.home"); //$NON-NLS-1$
			System.getProperties().remove("ant.library.dir"); //$NON-NLS-1$
		} else {
			System.setProperty("ant.home", antHome); //$NON-NLS-1$
			File antLibDir= new File(antHome, "lib"); //$NON-NLS-1$
			System.setProperty("ant.library.dir", antLibDir.getAbsolutePath()); //$NON-NLS-1$
		}
	}
	
	public void setAntHome(String antHome) {
		this.buildAntHome= antHome;
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
		} else if (!IAntCoreConstants.EMPTY_STRING.equals(loggerClassname)) {
			try {
				buildLogger = (BuildLogger) (Class.forName(loggerClassname).newInstance());
			} catch (ClassCastException e) {
				String message = MessageFormat.format(InternalAntMessages.InternalAntRunner_not_an_instance_of_apache_ant_BuildLogger, new String[]{loggerClassname});
				logMessage(null, message, Project.MSG_ERR);
				throw new BuildException(message, e);
			} catch (Exception e) {
				String message = MessageFormat.format(InternalAntMessages.InternalAntRunner_Unable_to_instantiate_logger, new String[]{loggerClassname});
				logMessage(null, message, Project.MSG_ERR);
				throw new BuildException(message, e);
			}
		} 
		
		if (buildLogger != null) {
			buildLogger.setMessageOutputLevel(messageOutputLevel);
			buildLogger.setOutputPrintStream(out);
			buildLogger.setErrorPrintStream(err);
			buildLogger.setEmacsMode(emacsMode);
            if (buildLogger instanceof AbstractEclipseBuildLogger) {
                ((AbstractEclipseBuildLogger) buildLogger).configure(userProperties);
            }
		}

		return buildLogger;
	}

	/**
	 * Project.fireBuildStarted is protected in Ant earlier than 1.5.*.
     * Provides backwards compatibility with old Ant installs.
	 */
	private void fireBuildStarted(Project project) {
        if (!isVersionCompatible("1.5")) { //$NON-NLS-1$
            BuildEvent event = new BuildEvent(project);
            Vector listeners= (Vector) project.getBuildListeners().clone();
            for (Iterator iterator = listeners.iterator(); iterator.hasNext();) {
                BuildListener listener = (BuildListener) iterator.next();
                listener.buildStarted(event);
            }
        } else {
            project.fireBuildStarted();
        }
	}

	private void fireBuildFinished(Project project, Throwable error) {
		if(usingXmlLogger()) {
			//generate the log file in the correct location
			String fileName= project.getProperty("XmlLogger.file"); //$NON-NLS-1$
			if (fileName == null) {
				fileName= "log.xml"; //$NON-NLS-1$
			}
			String realPath= new Path(getBuildFileLocation()).toFile().getAbsolutePath();
			IPath path= new Path(realPath);
			path= path.removeLastSegments(1);
			path= path.addTrailingSeparator();
			path= path.append(fileName);
		
			project.setProperty("XmlLogger.file", path.toOSString()); //$NON-NLS-1$
		}
		if (error == null && executed) {
			logMessage(project, InternalAntMessages.InternalAntRunner_BUILD_SUCCESSFUL_1, messageOutputLevel);
		} 
        if (!isVersionCompatible("1.5")) { //$NON-NLS-1$
            BuildEvent event = new BuildEvent(project);
            event.setException(error);
            Vector listeners= (Vector) project.getBuildListeners().clone();
            Iterator iter= listeners.iterator();
            while (iter.hasNext()) {
                BuildListener listener= (BuildListener) iter.next();
                listener.buildFinished(event);
            }   
        } else {
            project.fireBuildFinished(error);
        }
	}

	private boolean usingXmlLogger() {
		if (buildLogger instanceof XmlLogger) {
			return true;
		}
		if (buildListeners != null) {
			Enumeration e= getCurrentProject().getBuildListeners().elements();
			while (e.hasMoreElements()) {
				BuildListener element = (BuildListener) e.nextElement();
				if (element instanceof XmlLogger) {
					return true;
				}
			}
		}
		
		return false;
	}

	private void logMessage(Project project, String message, int priority) {
		if (project != null) {
			project.log(message, priority);	
		} else {
			if (buildListeners != null) {
				Project p = new Project();
				BuildEvent event = new BuildEvent(p);
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
			} else {
				IStatus s = new Status(IStatus.ERROR, AntCorePlugin.PI_ANTCORE, AntCorePlugin.INTERNAL_ERROR, message, null);
				AntCorePlugin.getPlugin().getLog().log(s);
			}
		}
	}

	/**
	 * Sets the buildFileLocation.
	 * 
	 * @param buildFileLocation the file system location of the build file
	 */
	public void setBuildFileLocation(String buildFileLocation) {
		this.buildFileLocation = buildFileLocation;
		if (getCurrentProject() != null) {
			getCurrentProject().setUserProperty("ant.file", buildFileLocation); //$NON-NLS-1$
		}
	}
	
	/**
	 * Sets the input handler class name.
	 * 
	 * @param inputHandlerClassname the name of the class to use for the input handler
	 */
	public void setInputHandler(String inputHandlerClassname) {
		this.inputHandlerClassname= inputHandlerClassname;
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
	public void setMessageOutputLevel(int level) {
		messageOutputLevel = level;
		if (buildLogger != null) {
			buildLogger.setMessageOutputLevel(level);
		}
	}

	/**
	 * Sets the extra user arguments
	 * @param args The extra user arguments
	 */
	public void setArguments(String[] args) {
		extraArguments = args;
	}

	/**
	 * Sets the execution targets.
	 * @param executionTargets The targets to execute for the build
	 */
	public void setExecutionTargets(String[] executionTargets) {
		targets = new Vector(executionTargets.length);
		for (int i = 0; i < executionTargets.length; i++) {
			targets.add(executionTargets[i]);
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
				throw new BuildException(MessageFormat.format(InternalAntMessages.InternalAntRunner_Could_not_load_the_version_information, new String[]{ioe.getMessage()}));
			} catch (NullPointerException npe) {
				throw new BuildException(MessageFormat.format(InternalAntMessages.InternalAntRunner_Could_not_load_the_version_information, new String[] {npe.getMessage()}));
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
		
		String arg = AntCoreUtil.getArgument(commands, "-listener"); //$NON-NLS-1$
		while (arg != null) {
			if (arg.length() == 0) {
				throw new BuildException(InternalAntMessages.InternalAntRunner_specify_a_classname_using_the_listener_argument);
			} 
			if (buildListeners == null) {
				buildListeners= new ArrayList(1);
			}
			buildListeners.add(arg);
			arg = AntCoreUtil.getArgument(commands, "-listener"); //$NON-NLS-1$
		}

		arg = AntCoreUtil.getArgument(commands, "-logger"); //$NON-NLS-1$
		if (arg != null) {
			if (arg.length() == 0) {
				throw new BuildException(InternalAntMessages.InternalAntRunner_specify_a_classname_using_the_logger_argument);
			} 
			loggerClassname = arg;
		}
		arg = AntCoreUtil.getArgument(commands, "-logger"); //$NON-NLS-1$
		if (arg != null) {
			throw new BuildException(InternalAntMessages.InternalAntRunner_Only_one_logger_class_may_be_specified);
		}
		
		arg = AntCoreUtil.getArgument(commands, "-inputhandler"); //$NON-NLS-1$
		if (arg != null) {
			if (!isVersionCompatible("1.5")) { //$NON-NLS-1$
				throw new BuildException(InternalAntMessages.InternalAntRunner_Specifying_an_InputHandler_is_an_Ant_1_5_feature);
			}
			if (arg.length() == 0) {
				throw new BuildException(InternalAntMessages.InternalAntRunner_specify_a_classname_the_inputhandler_argument);
			} 
			inputHandlerClassname = arg;
		}
		arg = AntCoreUtil.getArgument(commands, "-inputhandler"); //$NON-NLS-1$
		if (arg != null) {
			throw new BuildException(InternalAntMessages.InternalAntRunner_Only_one_input_handler_class_may_be_specified);
		}
		return true;
	}
	
	/*
	 * Looks for interesting command line arguments. 
	 * Returns whether it is OK to run the script.
	 */
	private boolean processCommandLine(List commands) {
		
		if (commands.remove("-help") || commands.remove("-h")) { //$NON-NLS-1$ //$NON-NLS-2$
			if (isVersionCompatible("1.7")) { //$NON-NLS-1$
				new EclipseMainHelper().runUsage(getBuildFileLocation(), getCurrentProject());
			} else {
				getCurrentProject().log(InternalAntMessages.InternalAntRunner_ant_1_7_needed_for_help_message);
			}
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
				throw new BuildException(InternalAntMessages.InternalAntRunner_The_diagnositics_options_is_an_Ant_1_5_feature);
			}
			try {
				Diagnostics.doReport(System.out);
			} catch (NullPointerException e) {
				logMessage(getCurrentProject(), InternalAntMessages.InternalAntRunner_anthome_must_be_set_to_use_ant_diagnostics, Project.MSG_ERR);
			}
			return false;
		}
		
		String arg = AntCoreUtil.getArgument(commands, "-logfile"); //$NON-NLS-1$
		if (arg == null) {
			arg = AntCoreUtil.getArgument(commands, "-l"); //$NON-NLS-1$
		}
		if (arg != null) {
			if (arg.length() == 0) {
				String message= InternalAntMessages.InternalAntRunner_specify_a_log_file_using_the_log_argument;
				logMessage(currentProject, message, Project.MSG_ERR); 
				throw new BuildException(message);
			} 
			try {
				createLogFile(arg);
			} catch (IOException e) {
				// just log message and ignore exception
				logMessage(getCurrentProject(), MessageFormat.format(InternalAntMessages.InternalAntRunner_Could_not_write_to_log_file, new String[]{arg}), Project.MSG_ERR);
				return false;
			}
		
		}
		
		arg = AntCoreUtil.getArgument(commands, "-buildfile"); //$NON-NLS-1$
		if (arg == null) {
			arg = AntCoreUtil.getArgument(commands, "-file"); //$NON-NLS-1$
			if (arg == null) {
				arg = AntCoreUtil.getArgument(commands, "-f"); //$NON-NLS-1$
			}
		}
		
		if (arg != null) {
			if (arg.length() == 0) {
				String message= InternalAntMessages.InternalAntRunner_specify_a_buildfile_using_the_buildfile_argument;
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
			arg= AntCoreUtil.getArgument(commands, "-lib"); //$NON-NLS-1$
			if (arg != null) {
				logMessage(currentProject, InternalAntMessages.InternalAntRunner_157, Project.MSG_ERR);
				return false;
			}
		}
		
		arg= AntCoreUtil.getArgument(commands, "-find"); //$NON-NLS-1$
		if (arg == null) {
			arg= AntCoreUtil.getArgument(commands, "-s"); //$NON-NLS-1$
		}
		if (arg != null) {
			logMessage(currentProject, InternalAntMessages.InternalAntRunner_find_not_supported, Project.MSG_ERR);
			return false;
		}

		if (!commands.isEmpty()) {
			processUnrecognizedCommands(commands);
		}

		if(!commands.isEmpty()) {
			processUnrecognizedTargets(commands);
		}
		
		if (!commands.isEmpty()) {
			processTargets(commands);
		}
		
		if (unknownTargetsFound && (targets == null  || targets.isEmpty())) {
			// Some targets are specified but none of them are good. Hence quit
			// https://bugs.eclipse.org/bugs/show_bug.cgi?id=352536
			logMessage(currentProject, InternalAntMessages.InternalAntRunner_no_known_target, Project.MSG_ERR);
			return false;
		}
		return true;
	}
	
	/**
	 * Checks for unrecognized targets on the command line and
	 * removes them.
	 * 
	 * @since 3.6
	 */
	private void processUnrecognizedTargets(List commands) {
		List names = getTargetNames();
		ListIterator iterator = commands.listIterator();
		
		while (iterator.hasNext()) {
			String target = (String) iterator.next();
			if (!names.contains(target)) {
				iterator.remove();
				String message = MessageFormat.format(InternalAntMessages.InternalAntRunner_unknown_target, new Object[]{target});
				logMessage(currentProject, message, Project.MSG_WARN);
				unknownTargetsFound = true;
			}
		}
	}

	
	/*
	 * Checks for unrecognized arguments on the command line.
	 * Since there is no syntactic way to distinguish between
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
			if (((String) commands.get(i)).startsWith("-")) { //$NON-NLS-1$
				p = i;
				break;
			}
		}
		if (p < 0) { return; }

		// remove everything preceding that last '-arg'
		String s = IAntCoreConstants.EMPTY_STRING;
		for (int i = 0; i <= p; i++) {
			s += " " + ((String) commands.get(0)); //$NON-NLS-1$
			commands.remove(0);
		}
		
		// warn of ignored commands
		String message = MessageFormat.format(InternalAntMessages.InternalAntRunner_Unknown_argument, new Object[]{ s.substring(1) });
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
		logMessage(getCurrentProject(), MessageFormat.format(InternalAntMessages.InternalAntRunner_Using_file_as_build_log, new String[]{logFile.getCanonicalPath()}), Project.MSG_INFO);
		if (buildLogger != null) {
			buildLogger.setErrorPrintStream(err);
			buildLogger.setOutputPrintStream(out);
		}
	}

	private File getFileRelativeToBaseDir(String fileName) {
	    return AntCoreUtil.getFileRelativeToBaseDir(fileName, getCurrentProject().getUserProperty("basedir"), getBuildFileLocation()); //$NON-NLS-1$
	}

	/*
	 * Processes cmd line properties and adds the user properties
	 * Any user properties that have been explicitly set are set as well.
	 * Ensures that -D properties take precedence.
	 */
	private boolean processProperties(List commands) {
        boolean exceptionToBeThrown= false;
		//MULTIPLE property files are allowed
		String arg= AntCoreUtil.getArgument(commands, "-propertyfile"); //$NON-NLS-1$
		while (arg != null) {
			if (!isVersionCompatible("1.5")) { //$NON-NLS-1$
				fEarlyErrorMessage= InternalAntMessages.InternalAntRunner_Specifying_property_files_is_a_Ant_1_5_feature;
				break;
			}
			if (arg.length() == 0) {
                fEarlyErrorMessage= InternalAntMessages.InternalAntRunner_specify_a_property_filename_when_using_propertyfile_argument;
                exceptionToBeThrown= true;
                break;
			} 
			
			propertyFiles.add(arg);
			arg= AntCoreUtil.getArgument(commands, "-propertyfile"); //$NON-NLS-1$
		}
		
		String[] globalPropertyFiles= AntCorePlugin.getPlugin().getPreferences().getCustomPropertyFiles();
		if (globalPropertyFiles.length > 0) {
            if (!isVersionCompatible("1.5")) { //$NON-NLS-1$
                fEarlyErrorMessage= InternalAntMessages.InternalAntRunner_Specifying_property_files_is_a_Ant_1_5_feature;
            } else {
                if (propertyFiles == null) {
                    propertyFiles= new ArrayList(globalPropertyFiles.length);
                }
                propertyFiles.addAll(Arrays.asList(globalPropertyFiles));
            }
		}
		
		if (propertyFiles != null && !propertyFiles.isEmpty()) {
            loadPropertyFiles();
		}
		
		if (commands != null) {
			processMinusDProperties(commands);
        }
        return exceptionToBeThrown;
	}

	private void processMinusDProperties(List commands) {
	    if (!commands.isEmpty() && userProperties == null) {
			userProperties= new HashMap();
		}
		AntCoreUtil.processMinusDProperties(commands, userProperties);
	}

	/*
	 * Logs a message with the client indicating the version of <b>Ant</b> that this class
	 * fronts.
	 */
	private void printVersion() {
		logMessage(getCurrentProject(), Main.getAntVersion(), Project.MSG_INFO);
	}

	
	/**
	 * Sets the build progress monitor.
	 * @param monitor The progress monitor to use
	 */
	public void setProgressMonitor(IProgressMonitor monitor) {
		this.monitor = monitor;
	}

	private Project getCurrentProject() {
		return currentProject;
	}

	private void setCurrentProject(Project currentProject) {
		this.currentProject = currentProject;
	}
	
	public String getBuildExceptionErrorMessage(Throwable t) {
		if (t instanceof BuildException) {
			return t.toString();
		}
		return null;
	}
	
	/**
	 * Load all properties from the files 
	 * specified by -propertyfile.
	 */
	private void loadPropertyFiles() {
	    if (userProperties == null) {
        	userProperties= new HashMap();
        }
        try {
            List allProperties = AntCoreUtil.loadPropertyFiles(propertyFiles, getCurrentProject().getUserProperty("basedir"), getBuildFileLocation()); //$NON-NLS-1$
	        Iterator iter= allProperties.iterator();
	        while (iter.hasNext()) {
	            Properties props = (Properties) iter.next();
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
        } catch (IOException e) {
            fEarlyErrorMessage= MessageFormat.format(InternalAntMessages.InternalAntRunner_could_not_load_property_file, new String[]{e.getMessage()});
        }
	}
	
	/*
     * Creates the InputHandler and adds it to the project.
     *
     * @exception BuildException if a specified InputHandler
     *                           implementation could not be loaded.
     */
    private void addInputHandler(Project project) {
    	if (!isVersionCompatible("1.5") || (inputHandlerClassname != null && inputHandlerClassname.length() == 0)) { //$NON-NLS-1$
			return;
		}
		InputHandlerSetter setter= new InputHandlerSetter();
		setter.setInputHandler(project, inputHandlerClassname);
    }

	/*
	 * Sets the Java class path in org.apache.tools.ant.types.Path
	 */
	private void setJavaClassPath() {
		URL[] antClasspath= null;
		AntCorePreferences prefs= AntCorePlugin.getPlugin().getPreferences();
		if (customClasspath == null) {
			antClasspath= prefs.getURLs();
		} else {
			URL[] extraClasspath= prefs.getExtraClasspathURLs();
			antClasspath= new URL[customClasspath.length + extraClasspath.length];
			System.arraycopy(customClasspath, 0, antClasspath, 0, customClasspath.length);
			System.arraycopy(extraClasspath, 0, antClasspath, customClasspath.length, extraClasspath.length);
		}
		StringBuffer buff= new StringBuffer();
		File file= null;
		for (int i = 0; i < antClasspath.length; i++) {
			try {
				file = new File(FileLocator.toFileURL(antClasspath[i]).getPath());
			} catch (IOException e) {
				continue;
			}
			buff.append(file.getAbsolutePath());
			buff.append("; "); //$NON-NLS-1$
		}

		org.apache.tools.ant.types.Path systemClasspath= new org.apache.tools.ant.types.Path(null, buff.substring(0, buff.length() - 2));
		org.apache.tools.ant.types.Path.systemClasspath= systemClasspath;
	}
	
	/**
	 * Sets the custom classpath to be included when setting the Java classpath for this build.
	 * @param classpath The custom classpath for this build.
	 */
	public void setCustomClasspath(URL[] classpath) {
		customClasspath= classpath;
	}
}