package org.eclipse.ant.core;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
import java.io.File;
import java.lang.reflect.Method;
import java.net.*;
import java.util.*;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.eclipse.ant.internal.core.AntClassLoader;
import org.eclipse.core.runtime.*;
/**
 * Entry point for running Ant scripts inside Eclipse.
 */
public class AntRunner implements IAntCoreConstants {

	protected String buildFileLocation = DEFAULT_BUILD_FILENAME;
	protected List buildListeners;
	protected Vector targets;

public AntRunner() {
	buildListeners = new ArrayList(5);
}

protected ClassLoader getClassLoader() {	
	URL[] urls = AntCorePlugin.getPlugin().getPreferences().getURLs();
	ClassLoader[] pluginLoaders = {
		Platform.getPlugin("org.eclipse.ant.ui").getClass().getClassLoader(), // TEST: it might not work if plugin does not have a non-default Plugin class
		Platform.getPlugin("org.eclipse.core.resources").getClass().getClassLoader(),
		Platform.getPlugin("org.eclipse.ant.core").getClass().getClassLoader(),
	};
	return new AntClassLoader(urls, pluginLoaders, null);
}

/**
 * Sets the buildFileLocation.
 * 
 * @param buildFileLocation the file system location of the build file
 */
public void setBuildFileLocation(String buildFileLocation) {
	if (buildFileLocation == null)
		this.buildFileLocation = DEFAULT_BUILD_FILENAME;
	else
		this.buildFileLocation = buildFileLocation;
}

/**
 * Sets the executionTargets in the order they need to run.
 * 
 */
public void setExecutionTargets(String[] executionTargets) {
	targets = new Vector(10);
	for (int i = 0; i < executionTargets.length; i++)
		targets.add(executionTargets[i]);
}

/**
 * Adds a build listener.
 * 
 * @param buildListener a build listener
 */
public void addBuildListener(String className) {
	if (className == null)
		return;
	buildListeners.add(className);
}

/**
 * Runs the build script.
 */
public void run() throws CoreException {
	try {
		ClassLoader loader = getClassLoader();
		Class classInternalAntRunner = loader.loadClass("org.eclipse.ant.internal.core.ant.InternalAntRunner");
		Object runner = classInternalAntRunner.newInstance();
		// set build file
		Method setBuildFileLocation = classInternalAntRunner.getMethod("setBuildFileLocation", new Class[] {String.class});
		setBuildFileLocation.invoke(runner, new Object[] {buildFileLocation});
		// add listeners
		Method addBuildListeners = classInternalAntRunner.getMethod("addBuildListeners", new Class[] {List.class});
		addBuildListeners.invoke(runner, new Object[] {buildListeners});
		// set execution targets
		if (targets != null) {
			Method setExecutionTargets = classInternalAntRunner.getMethod("setExecutionTargets", new Class[] {Vector.class});
			setExecutionTargets.invoke(runner, new Object[] {targets});
		}
		// run
		Method run = classInternalAntRunner.getMethod("run", null);
		run.invoke(runner, null);
	} catch (Exception e) {
		throw new CoreException(new Status(IStatus.ERROR, PI_ANTCORE, ERROR_RUNNING_SCRIPT, e.getMessage(), e));
	}
}




}