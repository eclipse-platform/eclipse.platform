/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Michael Giroux (michael.giroux@objectweb.org) - bug 149739
 *******************************************************************************/
package org.eclipse.ant.core;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.eclipse.ant.internal.core.AntClassLoader;
import org.eclipse.ant.internal.core.IAntCoreConstants;
import org.eclipse.ant.internal.core.InternalCoreAntMessages;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.osgi.util.NLS;

/**
 * Entry point for running Ant builds inside Eclipse (within the same JRE). Clients may instantiate this class; it is not intended to be subclassed.
 * <p/>
 * <div class="TableSubHeadingColor"> <b>Usage note:</b><br/>
 * Clients may use the <code>addBuildListener</code>, <code>addBuildLogger</code> and <code>setInputHandler</code> methods to configure classes that
 * will be invoked during the build. When using these methods, it is necessary to package the classes in a jar that is not on the client plugin's
 * classpath. The jar must be added to the Ant classpath. One way to add the jar to the Ant classpath is to use the
 * <code>org.eclipse.ant.core.extraClasspathEntries</code> extension.
 * <p>
 * Refer to the "Platform Ant Support" chapter of the Programmer's Guide section in the Platform Plug-in Developer Guide for complete details.
 * </p>
 * </div>
 * 
 * @noextend This class is not intended to be subclassed by clients.
 */
public class AntRunner implements IApplication {

	private static boolean buildRunning = false;
	protected String buildFileLocation = IAntCoreConstants.DEFAULT_BUILD_FILENAME;
	protected List<String> buildListeners;
	protected String[] targets;
	protected Map<String, String> userProperties;
	protected int messageOutputLevel = 2; // Project.MSG_INFO
	protected String buildLoggerClassName;
	protected String inputHandlerClassName;
	protected String[] arguments;
	protected String[] propertyFiles;
	protected URL[] customClasspath;
	protected String antHome;
	private IProgressMonitor progressMonitor = null;

	/**
	 * Sets the build file location on the file system.
	 * 
	 * @param buildFileLocation
	 *            the file system location of the build file
	 */
	public void setBuildFileLocation(String buildFileLocation) {
		if (buildFileLocation == null) {
			this.buildFileLocation = IAntCoreConstants.DEFAULT_BUILD_FILENAME;
		} else {
			this.buildFileLocation = buildFileLocation;
		}
	}

	/**
	 * Set the message output level.
	 * <p>
	 * Valid values are:
	 * <ul>
	 * <li><code>org.apache.tools.ant.Project.ERR</code>,
	 * <li><code>org.apache.tools.ant.Project.WARN</code>,
	 * <li><code>org.apache.tools.ant.Project.INFO</code>,
	 * <li><code>org.apache.tools.ant.Project.VERBOSE</code> or
	 * <li><code>org.apache.tools.ant.Project.DEBUG</code>
	 * </ul>
	 * 
	 * @param level
	 *            the message output level
	 */
	public void setMessageOutputLevel(int level) {
		messageOutputLevel = level;
	}

	/**
	 * Sets the arguments to be passed to the build (e.g. -Dos=win32 -Dws=win32 - verbose).
	 * 
	 * @param arguments
	 *            the arguments to be passed to the build
	 */
	public void setArguments(String arguments) {
		this.arguments = getArray(arguments);
	}

	/*
	 * Helper method to ensure an array is converted into an ArrayList.
	 */
	private String[] getArray(String args) {
		StringBuffer sb = new StringBuffer();
		boolean waitingForQuote = false;
		ArrayList<String> result = new ArrayList<String>();
		for (StringTokenizer tokens = new StringTokenizer(args, ", \"", true); tokens.hasMoreTokens();) { //$NON-NLS-1$
			String token = tokens.nextToken();
			if (waitingForQuote) {
				if (token.equals("\"")) { //$NON-NLS-1$
					result.add(sb.toString());
					sb.setLength(0);
					waitingForQuote = false;
				} else {
					sb.append(token);
				}
			} else {
				if (token.equals("\"")) { //$NON-NLS-1$
					// test if we have something like -Dproperty="value"
					if (result.size() > 0) {
						int index = result.size() - 1;
						String last = result.get(index);
						if (last.charAt(last.length() - 1) == '=') {
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
		return result.toArray(new String[result.size()]);
	}

	/**
	 * Sets the arguments to be passed to the build (e.g. -Dos=win32 -Dws=win32 -verbose).
	 * 
	 * @param arguments
	 *            the arguments to be passed to the build
	 * @since 2.1
	 */
	public void setArguments(String[] arguments) {
		this.arguments = arguments;
	}

	/**
	 * Sets the targets and execution order.
	 * 
	 * @param executionTargets
	 *            which targets should be run and in which order
	 */
	public void setExecutionTargets(String[] executionTargets) {
		this.targets = executionTargets;
	}

	/**
	 * Adds a build listener. The parameter <code>className</code> is the class name of an <code>org.apache.tools.ant.BuildListener</code>
	 * implementation. The class will be instantiated at runtime and the listener will be called on build events (
	 * <code>org.apache.tools.ant.BuildEvent</code>).
	 * 
	 * <p>
	 * Refer to {@link AntRunner Usage Note} for implementation details.
	 * 
	 * @param className
	 *            a build listener class name
	 */
	public void addBuildListener(String className) {
		if (className == null) {
			return;
		}
		if (buildListeners == null) {
			buildListeners = new ArrayList<String>(5);
		}
		buildListeners.add(className);
	}

	/**
	 * Sets the build logger. The parameter <code>className</code> is the class name of an <code>org.apache.tools.ant.BuildLogger</code>
	 * implementation. The class will be instantiated at runtime and the logger will be called on build events (
	 * <code>org.apache.tools.ant.BuildEvent</code>). Only one build logger is permitted for any build.
	 * 
	 * <p>
	 * Refer to {@link AntRunner Usage Note} for implementation details.
	 * 
	 * @param className
	 *            a build logger class name
	 */
	public void addBuildLogger(String className) {
		buildLoggerClassName = className;
	}

	/**
	 * Adds user-defined properties. Keys and values must be String objects.
	 * 
	 * @param properties
	 *            a Map of user-defined properties
	 */
	public void addUserProperties(Map<String, String> properties) {
		if (userProperties == null) {
			userProperties = new HashMap<String, String>(properties);
		} else {
			userProperties.putAll(properties);
		}
	}

	/**
	 * Returns the buildfile target information.
	 * 
	 * @return an array containing the target information
	 * 
	 * @see TargetInfo
	 * @since 2.1
	 * @throws CoreException
	 *             Thrown if problem is encountered determining the targets
	 */
	public synchronized TargetInfo[] getAvailableTargets() throws CoreException {
		Class<?> classInternalAntRunner = null;
		Object runner = null;
		ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
		try {
			classInternalAntRunner = getInternalAntRunner();
			runner = classInternalAntRunner.newInstance();
			basicConfigure(classInternalAntRunner, runner);

			// get the info for each targets
			Method getTargets = classInternalAntRunner.getMethod("getTargets", (Class[]) null); //$NON-NLS-1$
			Object results = getTargets.invoke(runner, (Object[]) null);
			// collect the info into target objects
			List<?> infos = (List<?>) results;
			TargetInfo[] targetInfo = new TargetInfo[infos.size()];
			int i = 0;
			for (Object target : infos) {
				targetInfo[i++] = (TargetInfo) target;
			}
			return targetInfo;
		}
		catch (NoClassDefFoundError e) {
			problemLoadingClass(e);
			// not possible to reach this line
			return new TargetInfo[0];
		}
		catch (ClassNotFoundException e) {
			problemLoadingClass(e);
			// not possible to reach this line
			return new TargetInfo[0];
		}
		catch (InvocationTargetException e) {
			handleInvocationTargetException(runner, classInternalAntRunner, e);
			// not possible to reach this line
			return new TargetInfo[0];
		}
		catch (Exception e) {
			String message = (e.getMessage() == null) ? InternalCoreAntMessages.AntRunner_Build_Failed__3 : e.getMessage();
			throw new CoreException(new Status(IStatus.ERROR, AntCorePlugin.PI_ANTCORE, AntCorePlugin.ERROR_RUNNING_BUILD, message, e));
		}
		finally {
			Thread.currentThread().setContextClassLoader(originalClassLoader);
		}
	}

	private void basicConfigure(Class<?> classInternalAntRunner, Object runner) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
		Method setBuildFileLocation = classInternalAntRunner.getMethod("setBuildFileLocation", new Class[] { String.class }); //$NON-NLS-1$
		setBuildFileLocation.invoke(runner, new Object[] { buildFileLocation });

		if (antHome != null) {
			Method setAntHome = classInternalAntRunner.getMethod("setAntHome", new Class[] { String.class }); //$NON-NLS-1$
			setAntHome.invoke(runner, new Object[] { antHome });
		}

		setProperties(runner, classInternalAntRunner);

		if (arguments != null && arguments.length > 0) {
			Method setArguments = classInternalAntRunner.getMethod("setArguments", new Class[] { String[].class }); //$NON-NLS-1$
			setArguments.invoke(runner, new Object[] { arguments });
		}
	}

	/**
	 * Runs the build file. If a progress monitor is specified it will be available during the script execution as a reference in the Ant Project (
	 * <code>org.apache.tools.ant.Project.getReferences()</code>). A long- running task could, for example, get the monitor during its execution and
	 * check for cancellation. The key value to retrieve the progress monitor instance is <code>AntCorePlugin.ECLIPSE_PROGRESS_MONITOR</code>.
	 * 
	 * Only one build can occur at any given time.
	 * 
	 * Sets the current threads context class loader to the AntClassLoader for the duration of the build.
	 * 
	 * @param monitor
	 *            a progress monitor, or <code>null</code> if progress reporting and cancellation are not desired
	 * @throws CoreException
	 *             Thrown if a build is already occurring or if an exception occurs during the build
	 */
	public void run(IProgressMonitor monitor) throws CoreException {
		if (buildRunning) {
			IStatus status = new Status(IStatus.ERROR, AntCorePlugin.PI_ANTCORE, AntCorePlugin.ERROR_RUNNING_BUILD, NLS.bind(InternalCoreAntMessages.AntRunner_Already_in_progess, new String[] { buildFileLocation }), null);
			throw new CoreException(status);
		}
		buildRunning = true;
		Object runner = null;
		Class<?> classInternalAntRunner = null;
		ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
		try {
			classInternalAntRunner = getInternalAntRunner();
			runner = classInternalAntRunner.newInstance();
			// set build file
			Method setBuildFileLocation = classInternalAntRunner.getMethod("setBuildFileLocation", new Class[] { String.class }); //$NON-NLS-1$
			setBuildFileLocation.invoke(runner, new Object[] { buildFileLocation });

			// set the custom classpath
			if (customClasspath != null) {
				Method setCustomClasspath = classInternalAntRunner.getMethod("setCustomClasspath", new Class[] { URL[].class }); //$NON-NLS-1$
				setCustomClasspath.invoke(runner, new Object[] { customClasspath });
			}

			// add listeners
			if (buildListeners != null) {
				Method addBuildListeners = classInternalAntRunner.getMethod("addBuildListeners", new Class[] { List.class }); //$NON-NLS-1$
				addBuildListeners.invoke(runner, new Object[] { buildListeners });
			}

			if (buildLoggerClassName == null) {
				// indicate that the default logger is not to be used
				buildLoggerClassName = IAntCoreConstants.EMPTY_STRING;
			}
			// add build logger
			Method addBuildLogger = classInternalAntRunner.getMethod("addBuildLogger", new Class[] { String.class }); //$NON-NLS-1$
			addBuildLogger.invoke(runner, new Object[] { buildLoggerClassName });

			if (inputHandlerClassName != null) {
				// add the input handler
				Method setInputHandler = classInternalAntRunner.getMethod("setInputHandler", new Class[] { String.class }); //$NON-NLS-1$
				setInputHandler.invoke(runner, new Object[] { inputHandlerClassName });
			}

			basicConfigure(classInternalAntRunner, runner);

			// add progress monitor
			if (monitor != null) {
				progressMonitor = monitor;
				Method setProgressMonitor = classInternalAntRunner.getMethod("setProgressMonitor", new Class[] { IProgressMonitor.class }); //$NON-NLS-1$
				setProgressMonitor.invoke(runner, new Object[] { monitor });
			}

			// set message output level
			if (messageOutputLevel != 2) { // changed from the default Project.MSG_INFO
				Method setMessageOutputLevel = classInternalAntRunner.getMethod("setMessageOutputLevel", new Class[] { int.class }); //$NON-NLS-1$
				setMessageOutputLevel.invoke(runner, new Object[] { new Integer(messageOutputLevel) });
			}

			// set execution targets
			if (targets != null) {
				Method setExecutionTargets = classInternalAntRunner.getMethod("setExecutionTargets", new Class[] { String[].class }); //$NON-NLS-1$
				setExecutionTargets.invoke(runner, new Object[] { targets });
			}

			// run
			Method run = classInternalAntRunner.getMethod("run", (Class[]) null); //$NON-NLS-1$
			run.invoke(runner, (Object[]) null);
		}
		catch (NoClassDefFoundError e) {
			problemLoadingClass(e);
		}
		catch (ClassNotFoundException e) {
			problemLoadingClass(e);
		}
		catch (InvocationTargetException e) {
			handleInvocationTargetException(runner, classInternalAntRunner, e);
		}
		catch (Exception e) {
			String message = (e.getMessage() == null) ? InternalCoreAntMessages.AntRunner_Build_Failed__3 : e.getMessage();
			IStatus status = new Status(IStatus.ERROR, AntCorePlugin.PI_ANTCORE, AntCorePlugin.ERROR_RUNNING_BUILD, message, e);
			throw new CoreException(status);
		}
		finally {
			buildRunning = false;
			Thread.currentThread().setContextClassLoader(originalClassLoader);
		}
	}

	private Class<?> getInternalAntRunner() throws ClassNotFoundException {
		ClassLoader loader = getClassLoader();
		Thread.currentThread().setContextClassLoader(loader);
		return loader.loadClass("org.eclipse.ant.internal.core.ant.InternalAntRunner"); //$NON-NLS-1$
	}

	private void setProperties(Object runner, Class<?> classInternalAntRunner) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
		// add properties
		if (userProperties != null) {
			Method addUserProperties = classInternalAntRunner.getMethod("addUserProperties", new Class[] { Map.class }); //$NON-NLS-1$
			addUserProperties.invoke(runner, new Object[] { userProperties });
		}

		// add property files
		if (propertyFiles != null) {
			Method addPropertyFiles = classInternalAntRunner.getMethod("addPropertyFiles", new Class[] { String[].class }); //$NON-NLS-1$
			addPropertyFiles.invoke(runner, new Object[] { propertyFiles });
		}
	}

	/*
	 * Handles exceptions that are loaded by the Ant Class Loader by asking the Internal Ant Runner class for the correct error message.
	 * 
	 * Handles OperationCanceledExceptions, nested NoClassDefFoundError and nested ClassNotFoundException
	 */
	protected void handleInvocationTargetException(Object runner, Class<?> classInternalAntRunner, InvocationTargetException e) throws CoreException {
		Throwable realException = e.getTargetException();
		if (realException instanceof OperationCanceledException) {
			return;
		}
		String message = null;
		if (runner != null) {
			try {
				Method getBuildErrorMessage = classInternalAntRunner.getMethod("getBuildExceptionErrorMessage", new Class[] { Throwable.class }); //$NON-NLS-1$
				message = (String) getBuildErrorMessage.invoke(runner, new Object[] { realException });
			}
			catch (Exception ex) {
				// do nothing as already in error state
			}
		}
		// J9 throws NoClassDefFoundError nested in a InvocationTargetException
		if (message == null && ((realException instanceof NoClassDefFoundError) || (realException instanceof ClassNotFoundException))) {
			problemLoadingClass(realException);
			return;
		}
		boolean internalError = false;
		if (message == null) {
			// error did not result from a BuildException
			internalError = true;
			message = (realException.getMessage() == null) ? InternalCoreAntMessages.AntRunner_Build_Failed__3 : realException.getMessage();
		}
		IStatus status = new Status(IStatus.ERROR, AntCorePlugin.PI_ANTCORE, AntCorePlugin.ERROR_RUNNING_BUILD, message, realException);
		if (internalError) {
			AntCorePlugin.getPlugin().getLog().log(status);
		}
		throw new CoreException(status);
	}

	protected void problemLoadingClass(Throwable e) throws CoreException {
		String missingClassName = e.getMessage();
		String message;
		if (missingClassName != null) {
			missingClassName = missingClassName.replace('/', '.');
			message = InternalCoreAntMessages.AntRunner_Could_not_find_one_or_more_classes__Please_check_the_Ant_classpath__2;
			message = NLS.bind(message, new String[] { missingClassName });
		} else {
			message = InternalCoreAntMessages.AntRunner_Could_not_find_one_or_more_classes__Please_check_the_Ant_classpath__1;
		}
		IStatus status = new Status(IStatus.ERROR, AntCorePlugin.PI_ANTCORE, AntCorePlugin.ERROR_RUNNING_BUILD, message, e);
		AntCorePlugin.getPlugin().getLog().log(status);
		throw new CoreException(status);
	}

	/**
	 * Runs the build file.
	 * 
	 * @throws CoreException
	 *             Thrown if a build is already occurring or if an exception occurs during the build
	 */
	public void run() throws CoreException {
		run(/* IProgressMonitor */null);
	}

	/**
	 * Invokes the building of a project object and executes a build using either a given target or the default target. This method is called when
	 * running Eclipse headless and specifying <code>org.eclipse.ant.core.antRunner</code> as the application.
	 * 
	 * Sets the current threads context class loader to the AntClassLoader for the duration of the build.
	 * 
	 * @param argArray
	 *            the command line arguments
	 * @exception Exception
	 *                if a problem occurred during the buildfile execution
	 * @return an exit object (<code>EXIT_OK</code>) indicating normal termination if no exception occurs
	 */
	public Object run(Object argArray) throws Exception {
		ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
		try {
			// set the preferences for headless mode
			AntCorePlugin.getPlugin().setRunningHeadless(true);

			// Add debug information if necessary - fix for bug 5672.
			// Since the platform parses the -debug command line arg
			// and removes it from the args passed to the applications,
			// we have to check if Eclipse is in debug mode in order to
			// forward the -debug argument to Ant.
			if (Platform.inDebugMode()) {
				String[] args = (String[]) argArray;
				String[] newArgs = new String[args.length + 1];
				System.arraycopy(argArray, 0, newArgs, 0, args.length);
				newArgs[args.length] = "-debug"; //$NON-NLS-1$
				argArray = newArgs;
			}
			ClassLoader loader = getClassLoader();
			Thread.currentThread().setContextClassLoader(loader);
			Class<?> classInternalAntRunner = loader.loadClass("org.eclipse.ant.internal.core.ant.InternalAntRunner"); //$NON-NLS-1$
			Object runner = classInternalAntRunner.newInstance();
			Method run = classInternalAntRunner.getMethod("run", new Class[] { Object.class }); //$NON-NLS-1$
			run.invoke(runner, new Object[] { argArray });
		}
		finally {
			Thread.currentThread().setContextClassLoader(originalClassLoader);
		}

		return EXIT_OK;
	}

	private ClassLoader getClassLoader() {
		if (customClasspath == null) {
			return AntCorePlugin.getPlugin().getNewClassLoader();
		}
		AntCorePreferences preferences = AntCorePlugin.getPlugin().getPreferences();
		ArrayList<URL> fullClasspath = new ArrayList<URL>();
		fullClasspath.addAll(Arrays.asList(customClasspath));
		fullClasspath.addAll(Arrays.asList(preferences.getExtraClasspathURLs()));
		return new AntClassLoader(fullClasspath.toArray(new URL[fullClasspath.size()]), preferences.getPluginClassLoaders());
	}

	/**
	 * Sets the input handler. The parameter <code>className</code> is the class name of an <code>org.apache.tools.ant.input.InputHandler</code>
	 * implementation. The class will be instantiated at runtime and the input handler will be used to respond to &lt;input&gt; requests Only one
	 * input handler is permitted for any build.
	 * 
	 * <p>
	 * Refer to {@link AntRunner Usage Note} for implementation details.
	 * 
	 * @param className
	 *            an input handler class name
	 * @since 2.1
	 */
	public void setInputHandler(String className) {
		inputHandlerClassName = className;
	}

	/**
	 * Sets the user specified property files.
	 * 
	 * @param propertyFiles
	 *            array of property file paths
	 * @since 2.1
	 */
	public void setPropertyFiles(String[] propertyFiles) {
		this.propertyFiles = propertyFiles;
	}

	/**
	 * Sets the custom classpath to use for this build
	 * 
	 * @param customClasspath
	 *            array of URLs that define the custom classpath
	 */
	public void setCustomClasspath(URL[] customClasspath) {
		this.customClasspath = customClasspath;
	}

	/**
	 * Sets the Ant home to use for this build
	 * 
	 * @param antHome
	 *            String specifying the Ant home to use
	 * @since 2.1
	 */
	public void setAntHome(String antHome) {
		this.antHome = antHome;
	}

	/**
	 * Returns whether an Ant build is already in progress
	 * 
	 * Only one Ant build can occur at any given time.
	 * 
	 * @since 2.1
	 * @return boolean
	 */
	public static boolean isBuildRunning() {
		return buildRunning;
	}

	/**
	 * Invokes the building of a project object and executes a build using either a given target or the default target. This method is called when
	 * running Eclipse headless and specifying <code>org.eclipse.ant.core.antRunner</code> as the application.
	 * 
	 * Sets the current threads context class loader to the <code>AntClassLoader</code> for the duration of the build.
	 * 
	 * @param context
	 *            the context used to start the application
	 * @exception Exception
	 *                if a problem occurred during the buildfile execution
	 * @return an exit object (<code>EXIT_OK</code>) indicating normal termination if no exception occurs
	 * @see org.eclipse.equinox.app.IApplication#start(IApplicationContext)
	 */
	@Override
	public Object start(IApplicationContext context) throws Exception {
		context.applicationRunning();
		Map<String, Object> contextArguments = context.getArguments();
		return run(contextArguments.get(IApplicationContext.APPLICATION_ARGS));
	}

	/*
	 * @see org.eclipse.equinox.app.IApplication#stop()
	 */
	@Override
	public void stop() {
		if (progressMonitor != null) {
			progressMonitor.setCanceled(true);
		}
	}
}
