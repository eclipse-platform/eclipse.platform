package org.eclipse.ant.core;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
import java.io.File;
import java.lang.reflect.Method;
import java.net.*;
import java.util.*;

import org.apache.tools.ant.*;
import org.eclipse.core.runtime.*;
/**
 * Entry point for running Ant scripts inside Eclipse.
 */
public class AntRunner implements IAntCoreConstants {

	protected String buildFileLocation = DEFAULT_BUILD_FILENAME;
	protected List buildListeners;
	
	/**
	 * Should only be accessed by the getProject() method.
	 */
	private Project project;
	
	private ClassLoader loader;

public AntRunner() {
	buildListeners = new ArrayList(5);
	try {
		URL[] path = new URL[] {
			new URL("file:c:/eclipse/workspaces/newpde/org.apache.xerces/xerces.jar"),
			new URL("file:c:/eclipse/workspaces/newpde/org.apache.ant/ant.jar"),
			new URL("file:c:/eclipse/workspaces/newpde/org.eclipse.ant.core/bin/"),
			new URL("file:c:/ibm-jdk/lib/tools.jar")
		};
		loader = new URLClassLoader(path, null);
	} catch (MalformedURLException e) {
		// should never happen
		e.printStackTrace();
	}
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
	project = null;
}

/**
 * Adds a build listener.
 * 
 * @param buildListener a build listener
 */
public void addBuildListener(BuildListener buildListener) {
	if (buildListener == null)
		return;
	buildListeners.add(buildListener);
	project = null;
}

/**
 * Runs the build script.
 */
public void run() throws CoreException {
	try {
//		String defaultTarget = getProject().getDefaultTarget();
//		getProject().executeTarget(defaultTarget);
		invokeProject();
	} catch (BuildException e) {
		throw new CoreException(new Status(IStatus.ERROR, PI_ANTCORE, ERROR_RUNNING_SCRIPT, e.getMessage(), e));
	}
}

protected Project getProject() throws CoreException {
	if (project != null)
		return project;
	project = new Project();
	try {
		project.init();
		addBuildListeners(project);
		parseScript(project);
	} catch (BuildException e) {
		throw new CoreException(new Status(IStatus.ERROR, PI_ANTCORE, ERROR_COULD_NOT_CONFIGURE_PROJECT, e.getMessage(), e));
	}
	return project;
}

protected void invokeProject() throws CoreException {
	try {
		Class classProject = loader.loadClass("org.apache.tools.ant.Project");
		Object project = classProject.newInstance();

		// init
		Method init = classProject.getMethod("init", null);
		init.invoke(project, null);

		// add listeners
		Class classBuildListener = loader.loadClass("org.apache.tools.ant.BuildListener");
		Method addBuildListener = classProject.getMethod("addBuildListener", new Class[] {classBuildListener});

		Class classProxyBuildListener = loader.loadClass("org.eclipse.ant.core.ProxyBuildListener");
		Object proxyBuildListener = classProxyBuildListener.newInstance();
		addBuildListener.invoke(project, new Object[] {proxyBuildListener});

		Method addListener = classProxyBuildListener.getMethod("addListener", new Class[] {Object.class});
		for (Iterator iterator = buildListeners.iterator(); iterator.hasNext();)
			addListener.invoke(proxyBuildListener, new Object[] {iterator.next()});

		Method setPluginLoader = classProxyBuildListener.getMethod("setPluginLoader", new Class[] {ClassLoader.class});
		setPluginLoader.invoke(proxyBuildListener, new Object[] {Platform.getPlugin(PI_ANTCORE).getDescriptor().getPluginClassLoader()});

		// parse script
		File buildFile = new File(buildFileLocation);
		Class projectHelper = loader.loadClass("org.apache.tools.ant.ProjectHelper");
		Method configureProject = projectHelper.getMethod("configureProject", new Class[] {classProject, File.class});
		configureProject.invoke(null, new Object[]{project, buildFile});

		// run
		Method getDefaultTarget = classProject.getMethod("getDefaultTarget", null);
		String defaultTarget = (String) getDefaultTarget.invoke(project, null);

		Method executeTarget = classProject.getMethod("executeTarget", new Class[]{String.class});
		executeTarget.invoke(project, new Object[]{defaultTarget});
		
	} catch (Exception e) {
		// FIXME:
		throw new CoreException(new Status(IStatus.ERROR, PI_ANTCORE, -1, e.getMessage(), e));
	}
}

protected void addBuildListeners(Project project) {
	for (Iterator iterator = buildListeners.iterator(); iterator.hasNext();)
		project.addBuildListener((BuildListener) iterator.next());
}

/**
 * Parses the build script and adds necessary information into
 * the given project.
 */
protected void parseScript(Project project) throws CoreException {
	File buildFile = new File(buildFileLocation);
	try {
		ProjectHelper.configureProject(project, buildFile);
	} catch (BuildException e) {
		throw new CoreException(new Status(IStatus.ERROR, PI_ANTCORE, ERROR_PARSING_SCRIPT, e.getMessage(), e));
	}
}
}