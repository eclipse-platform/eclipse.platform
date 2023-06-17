/*******************************************************************************
 *  Copyright (c) 2000, 2022 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - derived implementation
 *     Blake Meike (blakem@world.std.com)- patch for bug 31691 and bug 34488
 *     Rob Stryker - Bug 459188 - InternalAntRunner tries to set null user property
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
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
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
import org.apache.tools.ant.TaskAdapter;
import org.apache.tools.ant.XmlLogger;
import org.eclipse.ant.core.AntCorePlugin;
import org.eclipse.ant.core.AntCorePreferences;
import org.eclipse.ant.core.AntSecurityException;
import org.eclipse.ant.core.ProjectInfo;
import org.eclipse.ant.core.Property;
import org.eclipse.ant.core.TargetInfo;
import org.eclipse.ant.core.Task;
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
import org.eclipse.core.runtime.Status;
import org.eclipse.core.variables.VariablesPlugin;
import org.osgi.framework.Bundle;
import org.osgi.framework.Version;

/**
 * Eclipse application entry point into Ant. Derived from the original Ant Main class to ensure that the functionality is equivalent when running in
 * the platform.
 */
@SuppressWarnings("removal") // SecurityManager
public class InternalAntRunner {

	private IProgressMonitor monitor;
	private ArrayList<String> buildListeners;
	private String buildFileLocation;

	/**
	 * Targets we want to run.
	 */
	private Vector<String> targets;

	private Map<String, String> userProperties;
	private boolean noExplicitUserProperties = true;

	private Project currentProject;

	private String defaultTarget;

	private BuildLogger buildLogger = null;

	/**
	 * Cache of the Ant version number when it has been loaded
	 */
	private String antVersionNumber = null;

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
	 * The Ant logger class. There may be only one logger. It will have the right to use the 'out' PrintStream. The class must implement the
	 * BuildLogger interface. An empty String indicates that no logger is to be used. A <code>null</code> name indicates that the
	 * <code>org.apache.tools.ant.DefaultLogger</code> will be used.
	 */
	private String loggerClassname = null;

	/** Extra arguments to be parsed as command line arguments. */
	private String[] extraArguments = null;

	private boolean executed = false;

	private ArrayList<String> propertyFiles = new ArrayList<>();

	private URL[] customClasspath = null;

	/**
	 * The Ant InputHandler class. There may be only one input handler.
	 */
	private String inputHandlerClassname = null;

	private String buildAntHome = null;

	/**
	 * Indicates whether to execute all targets that do not depend on failed targets
	 *
	 * @since Ant 1.6.0
	 */
	private boolean keepGoing = false;

	/**
	 * Indicates whether this build is to support interactive input
	 *
	 * @since Ant 1.6.0
	 */
	private boolean allowInput = true;

	private String fEarlyErrorMessage = null;

	private boolean unknownTargetsFound = false;

	/**
	 * Adds a build listener.
	 *
	 * @param classNames
	 *            the fully qualified names of the build listeners to be added
	 */
	public void addBuildListeners(List<String> classNames) {
		if (buildListeners == null) {
			buildListeners = new ArrayList<>(classNames.size());
		}
		buildListeners.addAll(classNames);
	}

	protected void addBuildListener(String clazz) {
		if (buildListeners == null) {
			buildListeners = new ArrayList<>();
		}
		buildListeners.add(clazz);
	}

	/**
	 * Adds a build logger. There can be only one build logger.
	 *
	 * @param className
	 *            The fully qualified name of the build logger to add
	 */
	public void addBuildLogger(String className) {
		loggerClassname = className;
	}

	/**
	 * Adds user properties to the current collection of user properties.
	 *
	 * @param properties
	 *            The user properties to be added
	 */
	public void addUserProperties(Map<String, String> properties) {
		if (userProperties == null) {
			userProperties = new HashMap<>(properties);
		} else {
			userProperties.putAll(properties);
		}
		noExplicitUserProperties = false;
	}

	/**
	 * Adds user property files.
	 *
	 * @param additionalPropertyFiles
	 *            The property files to add
	 * @since 2.1
	 */
	public void addPropertyFiles(String[] additionalPropertyFiles) {
		propertyFiles.addAll(Arrays.asList(additionalPropertyFiles));
	}

	/**
	 * Tries to add the build listeners to the backing {@link Project}
	 *
	 * @param project
	 *            the project to add the listeners to
	 * @param log
	 *            if we should be logging {@link ClassCastException}s here or not.
	 */
	protected void addBuildListeners(Project project, boolean log) {
		String className = null;
		try {
			BuildLogger logger = createLogger();
			if (logger != null) {
				project.addBuildListener(logger);
			}
			if (buildListeners != null) {
				for (Iterator<String> iterator = buildListeners.iterator(); iterator.hasNext();) {
					className = iterator.next();
					Class<?> listener = Class.forName(className);
					project.addBuildListener((BuildListener) listener.getConstructor().newInstance());
				}
			}
		}
		catch (ClassCastException e) {
			String message = MessageFormat.format(InternalAntMessages.InternalAntRunner_not_an_instance_of_apache_ant_BuildListener, new Object[] {
					className });
			if (log) {
				logMessage(null, message, Project.MSG_ERR);
			}
			throw new BuildException(message, e);
		}
		catch (BuildException e) {
			throw e;
		}
		catch (Exception e) {
			throw new BuildException(e);
		}
	}

	private void setProperties(Project project, boolean substituteVariables) {
		setBuiltInProperties(project);
		if (userProperties != null) {
			for (Entry<String, String> entry : userProperties.entrySet()) {
				String value = entry.getValue();
				if (substituteVariables && value != null) {
					try {
						value = VariablesPlugin.getDefault().getStringVariableManager().performStringSubstitution(value);
					}
					catch (CoreException e) {
						// do nothing
					}
				}
				if (value != null)
					project.setUserProperty(entry.getKey(), value);
			}
			// may have properties set (always have the Ant process ID)
			// using the Arguments and not the Properties page
			// if set using the arguments, still include the global properties
			if (noExplicitUserProperties) {
				setGlobalProperties(project, substituteVariables);
			}
		} else {
			setGlobalProperties(project, substituteVariables);
		}
	}

	/**
	 * Sets the default <code>ant.file</code> and <code>ant.version</code> properties in the given {@link Project}
	 *
	 * @param project
	 */
	protected void setBuiltInProperties(Project project) {
		// note also see processAntHome for system properties that are set
		project.setUserProperty("ant.file", getBuildFileLocation()); //$NON-NLS-1$
		project.setUserProperty("ant.version", Main.getAntVersion()); //$NON-NLS-1$
	}

	private void setGlobalProperties(Project project, boolean substituteVariables) {
		AntCorePreferences prefs = AntCorePlugin.getPlugin().getPreferences();
		List<Property> properties = prefs.getProperties();
		if (properties != null) {
			for (Property property : properties) {
				String value = property.getValue(substituteVariables);
				if (value != null) {
					project.setUserProperty(property.getName(), value);
				}
			}
		}
	}

	private void setTasks(Project project) {
		List<Task> tasks = AntCorePlugin.getPlugin().getPreferences().getTasks();
		for (Task task : tasks) {
			if (isVersionCompatible("1.6")) { //$NON-NLS-1$
				AntTypeDefinition def = new AntTypeDefinition();
				String name = ProjectHelper.genComponentName(task.getURI(), task.getTaskName());
				def.setName(name);
				def.setClassName(task.getClassName());
				def.setClassLoader(this.getClass().getClassLoader());
				def.setAdaptToClass(org.apache.tools.ant.Task.class);
				def.setAdapterClass(TaskAdapter.class);
				ComponentHelper.getComponentHelper(project).addDataTypeDefinition(def);
			} else {
				try {
					Class<?> taskClass = Class.forName(task.getClassName());
					if (isVersionCompatible("1.5")) { //$NON-NLS-1$
						try {
							project.checkTaskClass(taskClass);
						}
						catch (BuildException e) {
							IStatus status = new Status(IStatus.ERROR, AntCorePlugin.PI_ANTCORE, AntCorePlugin.ERROR_RUNNING_BUILD, MessageFormat.format(InternalAntMessages.InternalAntRunner_Error_setting_Ant_task, new Object[] {
									task.getTaskName() }), e);
							AntCorePlugin.getPlugin().getLog().log(status);
							continue;
						}
					}
					project.addTaskDefinition(task.getTaskName(), taskClass);
				}
				catch (ClassNotFoundException e) {
					IStatus status = new Status(IStatus.ERROR, AntCorePlugin.PI_ANTCORE, AntCorePlugin.ERROR_RUNNING_BUILD, MessageFormat.format(InternalAntMessages.InternalAntRunner_Class_not_found_for_task, new Object[] {
							task.getClassName(), task.getTaskName() }), e);
					AntCorePlugin.getPlugin().getLog().log(status);
				}
			}
		}
	}

	private void setTypes(Project project) {
		for (Type type : AntCorePlugin.getPlugin().getPreferences().getTypes()) {
			if (isVersionCompatible("1.6")) { //$NON-NLS-1$
				AntTypeDefinition def = new AntTypeDefinition();
				String name = ProjectHelper.genComponentName(type.getURI(), type.getTypeName());
				def.setName(name);
				def.setClassName(type.getClassName());
				def.setClassLoader(this.getClass().getClassLoader());
				ComponentHelper.getComponentHelper(project).addDataTypeDefinition(def);
			} else {
				try {
					Class<?> typeClass = Class.forName(type.getClassName());
					project.addDataTypeDefinition(type.getTypeName(), typeClass);
				}
				catch (ClassNotFoundException e) {
					IStatus status = new Status(IStatus.ERROR, AntCorePlugin.PI_ANTCORE, AntCorePlugin.ERROR_RUNNING_BUILD, MessageFormat.format(InternalAntMessages.InternalAntRunner_Class_not_found_for_type, new Object[] {
							type.getClassName(), type.getTypeName() }), e);
					AntCorePlugin.getPlugin().getLog().log(status);
				}
			}
		}
	}

	/**
	 * Parses the build file and adds necessary information into the given project.
	 *
	 * @param project
	 *            The project to configure
	 */
	protected void parseBuildFile(Project project) {
		File buildFile = new File(getBuildFileLocation());
		if (!buildFile.exists()) {
			throw new BuildException(MessageFormat.format(InternalAntMessages.InternalAntRunner_Buildfile_does_not_exist, new Object[] {
					buildFile.getAbsolutePath() }));
		}
		if (!buildFile.isFile()) {
			throw new BuildException(MessageFormat.format(InternalAntMessages.InternalAntRunner_Buildfile_is_not_a_file, new Object[] {
					buildFile.getAbsolutePath() }));
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
	@Deprecated
	private void parseBuildFile(Project project, File buildFile) {
		ProjectHelper.configureProject(project, buildFile);
	}

	/**
	 * Gets all the target information from the build script. Returns a list of lists. Each item in the enclosing list represents a target, where the
	 * first element is the name, the second element is the description, the third element is the project name, and the last elements is an array of
	 * dependencies.
	 *
	 * @return a list of {@link TargetInfo} objects
	 */
	public List<TargetInfo> getTargets() {
		try {
			setJavaClassPath();
			Project antProject = getProject();
			processAntHome(false);
			antProject.init();
			setTypes(antProject);
			boolean exceptionState = processProperties(AntCoreUtil.getArrayList(extraArguments));
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
			Hashtable<String, Target> projectTargets = antProject.getTargets();
			ArrayList<TargetInfo> infos = new ArrayList<>();
			ProjectInfo pinfo = new ProjectInfo(antProject.getName(), antProject.getDescription());
			boolean defaultFound = false;
			for (Target target : projectTargets.values()) {
				String name = target.getName();
				if (name.length() == 0) {
					// "no name" implicit target of Ant 1.6
					continue;
				}
				if (target.getName().equals(defaultTarget)) {
					defaultFound = true;
				}
				ArrayList<String> dependencies = new ArrayList<>();
				Enumeration<String> enumeration = target.getDependencies();
				while (enumeration.hasMoreElements()) {
					dependencies.add(enumeration.nextElement());
				}
				String[] dependencyArray = new String[dependencies.size()];
				dependencies.toArray(dependencyArray);
				TargetInfo info = new TargetInfo(pinfo, name, target.getDescription(), dependencyArray, defaultFound);
				infos.add(info);
			}
			if (!defaultFound) {
				// default target must exist
				throw new BuildException(MessageFormat.format(InternalAntMessages.InternalAntRunner_Default_target_does_not_exist, new Object[] { "'", //$NON-NLS-1$
						defaultTarget, "'" })); //$NON-NLS-1$
			}
			return infos;
		}
		finally {
			processAntHome(true);
		}
	}

	/**
	 * Returns a list of target names in the build script.
	 *
	 * @return a list of target names
	 */
	private List<String> getTargetNames() {
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
			Hashtable<String, Target> projectTargets = antProject.getTargets();
			ArrayList<String> names = new ArrayList<>();
			for (Target target : projectTargets.values()) {
				String name = target.getName();
				if (name.length() == 0) {
					// "no name" implicit target of Ant 1.6
					continue;
				}
				names.add(name);
			}
			return names;
		}
		finally {
			processAntHome(true);
		}
	}

	private Project getProject() {
		Project antProject;
		if (isVersionCompatible("1.6")) { //$NON-NLS-1$
			// in Ant version 1.6 or greater all tasks can exist outside the scope of a target
			if (isVersionCompatible("1.6.3")) { //$NON-NLS-1$
				antProject = new InternalProject2();
			} else {
				antProject = new Project();
			}
		} else {
			antProject = new InternalProject();
		}
		return antProject;
	}

	/**
	 * Returns the default target name that was last computed or <code>null</code> if no default target has been computed.
	 *
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
		StringBuilder sb = new StringBuilder();
		for (String extraArgument : extraArguments) {
			sb.append(extraArgument);
			sb.append(' ');
		}
		project.log(MessageFormat.format(InternalAntMessages.InternalAntRunner_Arguments, new Object[] { sb.toString().trim() }));
	}

	private void createMonitorBuildListener(Project project) {
		if (monitor == null) {
			return;
		}
		Vector<String> chosenTargets = targets;
		if (chosenTargets == null || chosenTargets.isEmpty()) {
			chosenTargets = new Vector<>(1);
			String defltTarget = project.getDefaultTarget();
			if (defltTarget != null) {
				chosenTargets.add(defltTarget);
			}
		}
		project.addBuildListener(new ProgressBuildListener(project, chosenTargets, monitor));
	}

	/**
	 * Invokes the building of a project object and executes a build using either a given target or the default target. This method is called if
	 * running in headless mode.
	 *
	 * @see org.eclipse.ant.core.AntRunner#run(Object)
	 * @param argArray
	 *            the command line arguments
	 * @exception Exception
	 *                execution exceptions
	 */
	public void run(Object argArray) throws Exception {
		run(AntCoreUtil.getArrayList((String[]) argArray));
	}

	/**
	 * Attempts to run the given list of command line arguments. Note that the list passed to this method must support {@link List#remove(Object)}.
	 * <br>
	 * <br>
	 * This method directly processes the following arguments:
	 * <ul>
	 * <li><b>-projecthelp</b>, <b>-p</b> - print project help information</li>
	 * </ul>
	 *
	 * @param argList
	 *            the raw list of command line arguments
	 */
	private void run(List<String> argList) {
		setCurrentProject(new Project());
		if (isVersionCompatible("1.6.3")) { //$NON-NLS-1$
			new ExecutorSetter().setExecutor(currentProject);
		}
		Throwable error = null;
		PrintStream originalErr = System.err;
		PrintStream originalOut = System.out;
		InputStream originalIn = System.in;

		SecurityManager originalSM = System.getSecurityManager();
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

			boolean exceptionState = processProperties(argList);

			addBuildListeners(getCurrentProject(), true);

			addInputHandler(getCurrentProject());

			remapSystemIn();
			System.setOut(new PrintStream(new DemuxOutputStream(getCurrentProject(), false)));
			System.setErr(new PrintStream(new DemuxOutputStream(getCurrentProject(), true)));

			if (!projectHelp) {
				fireBuildStarted(getCurrentProject());
			}

			if (fEarlyErrorMessage != null) {
				// an error occurred processing the properties
				// build started has fired and we have
				// listeners/loggers to report the error
				logMessage(getCurrentProject(), fEarlyErrorMessage, Project.MSG_ERR);
				if (exceptionState) {
					throw new BuildException(fEarlyErrorMessage);
				}
			}

			// properties can only be set after buildStarted as some listeners/loggers
			// depend on this (e.g. XMLLogger)
			setProperties(getCurrentProject(), true);

			if (argList != null && !argList.isEmpty()) {
				try {
					executed = processCommandLine(argList);
				}
				catch (BuildException e) {
					executed = false;
					throw e;
				}
			}
			if (!executed) {
				return;
			}

			// needs to occur after processCommandLine(List)
			if (allowInput && (inputHandlerClassname != null && inputHandlerClassname.length() > 0)) {
				if (isVersionCompatible("1.6")) { //$NON-NLS-1$
					// https://bugs.eclipse.org/bugs/show_bug.cgi?id=182577
					// getCurrentProject().setDefaultInputStream(originalIn);
					System.getProperties().remove("eclipse.ant.noInput"); //$NON-NLS-1$
				}
			} else {
				// set the system property that any input handler
				// can check to see if handling input is allowed
				System.setProperty("eclipse.ant.noInput", "true"); //$NON-NLS-1$//$NON-NLS-2$
				if (isVersionCompatible("1.5") && (inputHandlerClassname == null || inputHandlerClassname.length() == 0)) { //$NON-NLS-1$
					InputHandlerSetter setter = new InputHandlerSetter();
					setter.setInputHandler(getCurrentProject(), "org.eclipse.ant.internal.core.ant.FailInputHandler"); //$NON-NLS-1$
				}
			}

			if (!projectHelp) {
				logMessage(currentProject, MessageFormat.format(InternalAntMessages.InternalAntRunner_Build_file, new Object[] {
						getBuildFileLocation() }), Project.MSG_INFO);

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
				logMessage(currentProject, InternalAntMessages.InternalAntRunner_ant_1_7_needed_for_help_info, Project.MSG_ERR);
				executed = false;
				return;
			}

			if (extraArguments != null) {
				printArguments(getCurrentProject());
			}
			try {
				System.setSecurityManager(new AntSecurityManager(originalSM, Thread.currentThread()));
			}
			catch (UnsupportedOperationException ex) {
				AntCorePlugin.getPlugin().getLog().log(new Status(IStatus.ERROR, AntCorePlugin.PI_ANTCORE, 0, InternalAntMessages.InternalAntRunner_SecurityManagerError, null));
			}
			if (targets == null) {
				targets = new Vector<>(1);
			}
			String dtarget = currentProject.getDefaultTarget();
			if (targets.isEmpty() && dtarget != null) {
				targets.add(dtarget);
			}
			if (!isVersionCompatible("1.6.3")) { //$NON-NLS-1$
				getCurrentProject().addReference(IAntCoreConstants.TARGET_VECTOR_NAME, targets);
			}
			getCurrentProject().executeTargets(targets);
		}
		catch (OperationCanceledException e) {
			executed = false;
			logMessage(currentProject, e.getMessage(), Project.MSG_INFO);
			throw e;
		}
		catch (AntSecurityException e) {
			// expected
		}
		catch (RuntimeException e) {
			error = e;
			throw e;
		}
		catch (Error e) {
			error = e;
			throw e;
		}
		finally {
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

			// close any user specified build log
			if (err != originalErr) {
				err.close();
			}
			if (out != originalOut) {
				out.close();
			}

			processAntHome(true);
			if (!allowInput) {
				System.getProperties().remove("eclipse.ant.noInput"); //$NON-NLS-1$
			}
		}
	}

	/**
	 * Re-maps {@link System.in} to the Ant input stream setter
	 */
	protected void remapSystemIn() {
		if (!isVersionCompatible("1.6")) { //$NON-NLS-1$
			return;
		}
		DemuxInputStreamSetter setter = new DemuxInputStreamSetter();
		setter.remapSystemIn(currentProject);
	}

	private void processAntHome(boolean finished) {
		AntCorePreferences prefs = AntCorePlugin.getPlugin().getPreferences();
		String antHome = prefs.getAntHome();
		if (buildAntHome != null && !finished) {
			antHome = buildAntHome;
		}
		if (antHome == null || antHome.length() == 0) {
			System.getProperties().remove("ant.home"); //$NON-NLS-1$
			System.getProperties().remove("ant.library.dir"); //$NON-NLS-1$
		} else {
			System.setProperty("ant.home", antHome); //$NON-NLS-1$
			File antLibDir = new File(antHome, "lib"); //$NON-NLS-1$
			System.setProperty("ant.library.dir", antLibDir.getAbsolutePath()); //$NON-NLS-1$
		}
	}

	public void setAntHome(String antHome) {
		this.buildAntHome = antHome;
	}

	/**
	 * Creates and returns the default build logger for logging build events to the ant log.
	 *
	 * @return the default build logger for logging build events to the ant log can return <code>null</code> if no logging is to occur
	 */
	protected BuildLogger createLogger() {
		if (loggerClassname == null) {
			buildLogger = new DefaultLogger();
		} else if (!IAntCoreConstants.EMPTY_STRING.equals(loggerClassname)) {
			try {
				buildLogger = (BuildLogger) (Class.forName(loggerClassname).getConstructor().newInstance());
			}
			catch (ClassCastException e) {
				String message = MessageFormat.format(InternalAntMessages.InternalAntRunner_not_an_instance_of_apache_ant_BuildLogger, new Object[] {
						loggerClassname });
				logMessage(null, message, Project.MSG_ERR);
				throw new BuildException(message, e);
			}
			catch (Exception e) {
				String message = MessageFormat.format(InternalAntMessages.InternalAntRunner_Unable_to_instantiate_logger, new Object[] {
						loggerClassname });
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
	 * Project.fireBuildStarted is protected in Ant earlier than 1.5.*. Provides backwards compatibility with old Ant installs.
	 */
	protected void fireBuildStarted(Project project) {
		if (!isVersionCompatible("1.5")) { //$NON-NLS-1$
			BuildEvent event = new BuildEvent(project);
			for (BuildListener listener : project.getBuildListeners()) {
				listener.buildStarted(event);
			}
		} else {
			project.fireBuildStarted();
		}
	}

	/**
	 * Sends the the event to the backing project that the build has completed
	 *
	 * @param project
	 * @param error
	 */
	protected void fireBuildFinished(Project project, Throwable error) {
		if (usingXmlLogger()) {
			// generate the log file in the correct location
			String fileName = project.getProperty("XmlLogger.file"); //$NON-NLS-1$
			if (fileName == null) {
				fileName = "log.xml"; //$NON-NLS-1$
			}
			String realPath = IPath.fromOSString(getBuildFileLocation()).toFile().getAbsolutePath();
			IPath path = IPath.fromOSString(realPath);
			path = path.removeLastSegments(1);
			path = path.addTrailingSeparator();
			path = path.append(fileName);

			project.setProperty("XmlLogger.file", path.toOSString()); //$NON-NLS-1$
		}
		if (error == null && executed) {
			logMessage(project, InternalAntMessages.InternalAntRunner_BUILD_SUCCESSFUL_1, messageOutputLevel);
		}
		if (!isVersionCompatible("1.5")) { //$NON-NLS-1$
			BuildEvent event = new BuildEvent(project);
			event.setException(error);
			for (BuildListener listener : project.getBuildListeners()) {
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
			for (BuildListener listener : currentProject.getBuildListeners()) {
				if (listener instanceof XmlLogger) {
					return true;
				}
			}
		}
		return false;
	}

	protected void logMessage(Project project, String message, int priority) {
		if (project != null) {
			project.log(message, priority);
		} else {
			if (buildListeners != null) {
				Project p = new Project();
				addBuildListeners(p, false);
				p.log(message, priority);
			} else {
				IStatus s = new Status(IStatus.ERROR, AntCorePlugin.PI_ANTCORE, AntCorePlugin.INTERNAL_ERROR, message, null);
				AntCorePlugin.getPlugin().getLog().log(s);
			}
		}
	}

	/**
	 * Sets the buildFileLocation.
	 *
	 * @param buildFileLocation
	 *            the file system location of the build file
	 */
	public void setBuildFileLocation(String buildFileLocation) {
		this.buildFileLocation = buildFileLocation;
		if (currentProject != null) {
			currentProject.setUserProperty("ant.file", buildFileLocation); //$NON-NLS-1$
		}
	}

	/**
	 * Sets the input handler class name.
	 *
	 * @param inputHandlerClassname
	 *            the name of the class to use for the input handler
	 */
	public void setInputHandler(String inputHandlerClassname) {
		this.inputHandlerClassname = inputHandlerClassname;
	}

	/**
	 * Returns the class name of the input handler to use
	 *
	 * @return the input handler class name
	 */
	protected String getInputHandler() {
		return this.inputHandlerClassname;
	}

	protected String getBuildLogger() {
		return this.loggerClassname;
	}

	/**
	 * Returns the location of the buildfile. If one has not been supplied the default location of <code>build.xml</code> will be returned
	 *
	 * @return the absolute location of the build file
	 */
	protected String getBuildFileLocation() {
		if (buildFileLocation == null) {
			buildFileLocation = new File("build.xml").getAbsolutePath(); //$NON-NLS-1$
		}
		return buildFileLocation;
	}

	/**
	 * Sets the message output level. Use -1 for none.
	 *
	 * @param level
	 *            The message output level
	 */
	public void setMessageOutputLevel(int level) {
		messageOutputLevel = level;
		if (buildLogger != null) {
			buildLogger.setMessageOutputLevel(level);
		}
	}

	/**
	 * Sets the extra user arguments
	 *
	 * @param args
	 *            The extra user arguments
	 */
	public void setArguments(String[] args) {
		extraArguments = args;
	}

	/**
	 * Sets the execution targets.
	 *
	 * @param executionTargets
	 *            The targets to execute for the build
	 */
	public void setExecutionTargets(String[] executionTargets) {
		targets = new Vector<>(executionTargets.length);
		for (String executionTarget : executionTargets) {
			targets.add(executionTarget);
		}
	}

	/*
	 * Returns a String representation of the Ant version number as specified in the version.txt file.
	 */
	protected String getAntVersionNumber() throws BuildException {
		if (antVersionNumber == null) {
			try (InputStream in = Main.class.getResourceAsStream("/org/apache/tools/ant/version.txt")) { //$NON-NLS-1$
				Properties props = new Properties();
				props.load(in);
				String versionNumber = props.getProperty("VERSION"); //$NON-NLS-1$
				antVersionNumber = versionNumber;
			}
			catch (IOException ioe) {
				throw new BuildException(MessageFormat.format(InternalAntMessages.InternalAntRunner_Could_not_load_the_version_information, new Object[] {
						ioe.getMessage() }), ioe);
			}
			catch (NullPointerException npe) {
				throw new BuildException(MessageFormat.format(InternalAntMessages.InternalAntRunner_Could_not_load_the_version_information, new Object[] {
						npe.getMessage() }), npe);
			}
		}
		return antVersionNumber;
	}

	/*
	 * Returns whether the given version is compatible with the current Ant version. A version is compatible if it is less than or equal to the
	 * current version.
	 */
	protected boolean isVersionCompatible(String comparison) {
		String version = getAntVersionNumber();
		Version osgiVersion = new Version(version);
		Version osgiComparison = new Version(comparison);
		return osgiVersion.compareTo(osgiComparison) >= 0;
	}

	/**
	 * Pre-processes the raw command line to set up input handling and logging. <br>
	 * <br>
	 * This method checks for the following command line arguments:
	 * <ul>
	 * <li><b>-inputhandler</b> <em>&lt;class&gt;</em> - the class which will handle input requests</li>
	 * <li><b>-logger</b> <em>&lt;classname&gt;</em> - the class which is to perform logging</li>
	 * <li><b>-listener</b> <em>&lt;classname&gt;</em> - add an instance of class as a project listener</li>
	 * </ul>
	 *
	 * @param commands
	 *            the raw command line arguments passed in from the application
	 * @return <code>true</code> if it is OK to run with the given list of arguments <code>false</code> otherwise
	 */
	protected boolean preprocessCommandLine(List<String> commands) {
		String arg = AntCoreUtil.getArgument(commands, "-listener"); //$NON-NLS-1$
		while (arg != null) {
			if (arg.length() == 0) {
				throw new BuildException(InternalAntMessages.InternalAntRunner_specify_a_classname_using_the_listener_argument);
			}
			if (buildListeners == null) {
				buildListeners = new ArrayList<>(1);
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

	/**
	 * Process the command line arguments. <br>
	 * <br>
	 * The listing of supported Ant command line arguments is:
	 * <ul>
	 * <li><b>-buildfile</b>, <b>-file</b>, <b>-f</b> <em>&lt;file&gt;</em> - use given buildfile</li>
	 * <li><b>-debug</b>, <b>-d</b> - print debugging information</li>
	 * <li><b>-diagnostics</b> - print information that might be helpful diagnose or report problems</li>
	 * <li><b>-emacs</b>, <b>-e</b> - produce logging information without adornments</li>
	 * <li><b>-find</b>, <b>-s</b> <em>&lt;file&gt;</em> - search for buildfile towards the root of the filesystem and use it</li>
	 * <li><b>-help</b>, <b>-h</b> - print this message</li>
	 * <li><b>-keep-going</b>, <b>-k</b> - execute all targets that do not depend on failed target(s)</li>
	 * <li><b>-lib</b> <em>&lt;path&gt;</em> - specifies a path to search for jars and classes</li>
	 * <li><b>-logfile</b>, <b>-l</b> <em>&lt;file&gt;</em> - use given file for logging</li>
	 * <li><b>-noinput</b> - do not allow interactive input</li>
	 * <li><b>-quiet</b>, <b>-q</b> - be extra quiet</li>
	 * <li><b>-verbose</b>, <b>-v</b> - be extra verbose</li>
	 * <li><b>-version</b> - print the version information and exit</li>
	 * </ul>
	 * The list of other Ant command line arguments that we currently do not support:
	 * <ul>
	 * <li><b>-nice</b> <em>&lt;number&gt;</em> - A niceness value for the main thread - 1 (lowest) to 10 (highest); 5 is the default</li>
	 * <li><b>-nouserlib</b> - Run ant without using the jar files from ${user.home}/.ant/lib</li>
	 * <li><b>-noclasspath</b> - Run ant without using CLASSPATH</li>
	 * <li><b>-autoproxy</b> - Java 1.5+ : use the OS proxies</li>
	 * <li><b>-main</b> <em>&lt;class&gt;</em> - override Ant's normal entry point</li>
	 * </ul>
	 *
	 * @param commands
	 *            the raw command line arguments passed in from the application
	 * @return <code>true</code> if it is OK to run with the given list of arguments <code>false</code> otherwise
	 */
	private boolean processCommandLine(List<String> commands) {
		if (commands.remove("-help") || commands.remove("-h")) { //$NON-NLS-1$ //$NON-NLS-2$
			if (isVersionCompatible("1.7")) { //$NON-NLS-1$
				new EclipseMainHelper().runUsage(getBuildFileLocation(), currentProject);
			} else {
				logMessage(currentProject, InternalAntMessages.InternalAntRunner_ant_1_7_needed_for_help_message, Project.MSG_WARN);
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
			}
			catch (NullPointerException e) {
				logMessage(currentProject, InternalAntMessages.InternalAntRunner_anthome_must_be_set_to_use_ant_diagnostics, Project.MSG_ERR);
			}
			return false;
		}

		String arg = AntCoreUtil.getArgument(commands, "-logfile"); //$NON-NLS-1$
		if (arg == null) {
			arg = AntCoreUtil.getArgument(commands, "-l"); //$NON-NLS-1$
		}
		if (arg != null) {
			if (arg.length() == 0) {
				String message = InternalAntMessages.InternalAntRunner_specify_a_log_file_using_the_log_argument;
				logMessage(currentProject, message, Project.MSG_ERR);
				throw new BuildException(message);
			}
			try {
				createLogFile(arg);
			}
			catch (IOException e) {
				// just log message and ignore exception
				logMessage(currentProject, MessageFormat.format(InternalAntMessages.InternalAntRunner_Could_not_write_to_log_file, new Object[] {
						arg }), Project.MSG_ERR);
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
				String message = InternalAntMessages.InternalAntRunner_specify_a_buildfile_using_the_buildfile_argument;
				logMessage(currentProject, message, Project.MSG_ERR);
				throw new BuildException(message);
			}
			setBuildFileLocation(arg);
		}
		if (isVersionCompatible("1.6")) { //$NON-NLS-1$
			if (commands.remove("-k") || commands.remove("-keep-going")) { //$NON-NLS-1$ //$NON-NLS-2$
				keepGoing = true;
			}
			if (commands.remove("-noinput")) { //$NON-NLS-1$
				allowInput = false;
			}
			arg = AntCoreUtil.getArgument(commands, "-lib"); //$NON-NLS-1$
			if (arg != null) {
				logMessage(currentProject, InternalAntMessages.InternalAntRunner_157, Project.MSG_ERR);
				return false;
			}
		}

		arg = AntCoreUtil.getArgument(commands, "-find"); //$NON-NLS-1$
		if (arg == null) {
			arg = AntCoreUtil.getArgument(commands, "-s"); //$NON-NLS-1$
		}
		if (arg != null) {
			logMessage(currentProject, InternalAntMessages.InternalAntRunner_find_not_supported, Project.MSG_ERR);
			return false;
		}

		if (!commands.isEmpty()) {
			processUnrecognizedCommands(commands);
		}

		if (!commands.isEmpty()) {
			processUnrecognizedTargets(commands);
		}

		if (!commands.isEmpty()) {
			processTargets(commands);
		}

		if (unknownTargetsFound && (targets == null || targets.isEmpty())) {
			// Some targets are specified but none of them are good. Hence quit
			// https://bugs.eclipse.org/bugs/show_bug.cgi?id=352536
			logMessage(currentProject, InternalAntMessages.InternalAntRunner_no_known_target, Project.MSG_ERR);
			return false;
		}
		return true;
	}

	/**
	 * Checks for unrecognized targets on the command line and removes them.
	 *
	 * @since 3.6
	 */
	private void processUnrecognizedTargets(List<String> commands) {
		List<String> names = getTargetNames();
		ListIterator<String> iterator = commands.listIterator();
		while (iterator.hasNext()) {
			String target = iterator.next();
			if (!names.contains(target)) {
				iterator.remove();
				String message = MessageFormat.format(InternalAntMessages.InternalAntRunner_unknown_target, new Object[] { target });
				logMessage(currentProject, message, Project.MSG_WARN);
				unknownTargetsFound = true;
			}
		}
	}

	/**
	 * Checks for unrecognized arguments on the command line. Since there is no syntactic way to distinguish between ant -foo target1 target2 ant -foo
	 * fooarg target we remove everything up to the last argument that begins with a '-'. In the latter case, above, that means that there will be an
	 * extra target, 'fooarg', left lying around.
	 */
	protected void processUnrecognizedCommands(List<String> commands) {
		int p = -1;

		// find the last arg that begins with '-'
		for (int i = commands.size() - 1; i >= 0; i--) {
			if (commands.get(i).startsWith("-")) { //$NON-NLS-1$
				p = i;
				break;
			}
		}
		if (p < 0) {
			return;
		}

		// remove everything preceding that last '-arg'
		String s = IAntCoreConstants.EMPTY_STRING;
		for (int i = 0; i <= p; i++) {
			s += " " + (commands.get(0)); //$NON-NLS-1$
			commands.remove(0);
		}

		// warn of ignored commands
		String message = MessageFormat.format(InternalAntMessages.InternalAntRunner_Unknown_argument, new Object[] { s.substring(1) });
		logMessage(currentProject, message, Project.MSG_WARN);
	}

	/**
	 * Checks for targets specified at the command line.
	 */
	protected void processTargets(List<String> commands) {
		if (targets == null) {
			targets = new Vector<>(commands.size());
		}
		for (String string : commands) {
			targets.add(string);
		}
	}

	/**
	 * Creates the log file with the name specified by the user. If the fileName is not absolute, the file will be created in the working directory if
	 * specified or in the same directory as the location of the build file.
	 */
	protected void createLogFile(String fileName) throws FileNotFoundException, IOException {
		File logFile = AntCoreUtil.getFileRelativeToBaseDir(fileName, currentProject.getUserProperty("basedir"), getBuildFileLocation()); //$NON-NLS-1$
		// this stream is closed in the finally block of run(list)
		out = new PrintStream(new FileOutputStream(logFile));
		err = out;
		logMessage(currentProject, MessageFormat.format(InternalAntMessages.InternalAntRunner_Using_file_as_build_log, new Object[] {
				logFile.getCanonicalPath() }), Project.MSG_INFO);
		if (buildLogger != null) {
			buildLogger.setErrorPrintStream(err);
			buildLogger.setOutputPrintStream(out);
		}
	}

	/**
	 * Processes the command line properties and adds the user properties. <br>
	 * <br>
	 * Any user properties that have been explicitly set are set as well. Ensures that -D properties take precedence. <br>
	 * <br>
	 * This command lie arguments used are:
	 * <ul>
	 * <li><b>-D</b> <em>&lt;property&gt;=&lt;value&gt;</em> - use value for given property</li>
	 * <li><b>-propertyfile</b> <em>&lt;name&gt;</em> - load all properties from file with -D properties taking precedence</li>
	 * </ul>
	 */
	private boolean processProperties(List<String> commands) {
		boolean exceptionToBeThrown = false;
		// MULTIPLE property files are allowed
		String arg = AntCoreUtil.getArgument(commands, "-propertyfile"); //$NON-NLS-1$
		while (arg != null) {
			if (!isVersionCompatible("1.5")) { //$NON-NLS-1$
				fEarlyErrorMessage = InternalAntMessages.InternalAntRunner_Specifying_property_files_is_a_Ant_1_5_feature;
				break;
			}
			if (arg.length() == 0) {
				fEarlyErrorMessage = InternalAntMessages.InternalAntRunner_specify_a_property_filename_when_using_propertyfile_argument;
				exceptionToBeThrown = true;
				break;
			}

			propertyFiles.add(arg);
			arg = AntCoreUtil.getArgument(commands, "-propertyfile"); //$NON-NLS-1$
		}

		String[] globalPropertyFiles = AntCorePlugin.getPlugin().getPreferences().getCustomPropertyFiles();
		if (globalPropertyFiles.length > 0) {
			if (!isVersionCompatible("1.5")) { //$NON-NLS-1$
				fEarlyErrorMessage = InternalAntMessages.InternalAntRunner_Specifying_property_files_is_a_Ant_1_5_feature;
			} else {
				if (propertyFiles == null) {
					propertyFiles = new ArrayList<>(globalPropertyFiles.length);
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

	/**
	 * Process properties specified using <code>-D</code>
	 *
	 * @param commands
	 */
	protected void processMinusDProperties(List<String> commands) {
		if (!commands.isEmpty() && userProperties == null) {
			userProperties = new HashMap<>();
		}
		AntCoreUtil.processMinusDProperties(commands, userProperties);
	}

	/**
	 * Logs a message with the client indicating the version of <b>Ant</b> that this class fronts.
	 */
	protected void printVersion() {
		logMessage(currentProject, Main.getAntVersion(), Project.MSG_INFO);
	}

	/**
	 * Sets the build progress monitor.
	 *
	 * @param monitor
	 *            The progress monitor to use
	 */
	public void setProgressMonitor(IProgressMonitor monitor) {
		this.monitor = monitor;
	}

	/**
	 * Returns the current {@link Project} context
	 *
	 * @return the current {@link Project}
	 */
	protected Project getCurrentProject() {
		return currentProject;
	}

	/**
	 * Sets the current {@link Project} context
	 *
	 * @param currentProject
	 *            the new {@link Project}
	 */
	protected void setCurrentProject(Project currentProject) {
		this.currentProject = currentProject;
	}

	public String getBuildExceptionErrorMessage(Throwable t) {
		if (t instanceof BuildException) {
			return t.toString();
		}
		return null;
	}

	/**
	 * Load all properties from the files specified by -propertyfile.
	 */
	protected void loadPropertyFiles() {
		if (userProperties == null) {
			userProperties = new HashMap<>();
		}
		try {
			List<Properties> allProperties = AntCoreUtil.loadPropertyFiles(propertyFiles, currentProject.getUserProperty("basedir"), getBuildFileLocation()); //$NON-NLS-1$
			Iterator<Properties> iter = allProperties.iterator();
			while (iter.hasNext()) {
				Properties props = iter.next();
				Enumeration<?> propertyNames = props.propertyNames();
				while (propertyNames.hasMoreElements()) {
					String name = (String) propertyNames.nextElement();
					// most specific to global
					// do not overwrite specific with a global property
					if (userProperties.get(name) == null) {
						userProperties.put(name, props.getProperty(name));
					}
				}
			}
		}
		catch (IOException e) {
			fEarlyErrorMessage = MessageFormat.format(InternalAntMessages.InternalAntRunner_could_not_load_property_file, new Object[] {
					e.getMessage() });
		}
	}

	/**
	 * Creates the InputHandler and adds it to the project.
	 *
	 * @exception BuildException
	 *                if a specified InputHandler implementation could not be loaded.
	 */
	protected void addInputHandler(Project project) {
		if (!isVersionCompatible("1.5") || (inputHandlerClassname != null && inputHandlerClassname.length() == 0)) { //$NON-NLS-1$
			return;
		}
		InputHandlerSetter setter = new InputHandlerSetter();
		setter.setInputHandler(project, inputHandlerClassname);
	}

	/*
	 * Sets the Java class path in org.apache.tools.ant.types.Path
	 */
	private void setJavaClassPath() {
		URL[] antClasspath = null;
		AntCorePreferences prefs = AntCorePlugin.getPlugin().getPreferences();
		if (customClasspath == null) {
			antClasspath = prefs.getURLs();
		} else {
			URL[] extraClasspath = prefs.getExtraClasspathURLs();
			antClasspath = new URL[customClasspath.length + extraClasspath.length];
			System.arraycopy(customClasspath, 0, antClasspath, 0, customClasspath.length);
			System.arraycopy(extraClasspath, 0, antClasspath, customClasspath.length, extraClasspath.length);
		}
		StringBuilder buff = new StringBuilder();
		File file = null;
		for (URL element : antClasspath) {
			try {
				file = new File(FileLocator.toFileURL(element).getPath());
			}
			catch (IOException e) {
				continue;
			}
			buff.append(file.getAbsolutePath());
			buff.append("; "); //$NON-NLS-1$
		}

		org.apache.tools.ant.types.Path systemClasspath = new org.apache.tools.ant.types.Path(null, buff.substring(0, buff.length() - 2));
		org.apache.tools.ant.types.Path.systemClasspath = systemClasspath;
	}

	/**
	 * Sets the custom classpath to be included when setting the Java classpath for this build.
	 *
	 * @param classpath
	 *            The custom classpath for this build.
	 */
	public void setCustomClasspath(URL[] classpath) {
		customClasspath = classpath;
	}
}
