package org.eclipse.ant.internal.core;

import java.io.File;
import java.util.Iterator;
import java.util.List;

import org.apache.tools.ant.*;
import org.eclipse.ant.core.IAntCoreConstants;


public class InternalAntRunner implements IAntCoreConstants {

	protected List buildListeners;
	protected String buildFileLocation;
	
	/**
	 * Should only be accessed by the getProject() method.
	 */
	private Project project;

public InternalAntRunner() {
}

/**
 * Adds a build listener.
 * 
 * @param buildListener a build listener
 */
public void addBuildListeners(List classNames) {
	this.buildListeners = classNames;
}

protected void addBuildListeners(Project project) {
	try {
		for (Iterator iterator = buildListeners.iterator(); iterator.hasNext();) {
			String className = (String) iterator.next();
			Class listener = Class.forName(className);
			project.addBuildListener((BuildListener) listener.newInstance());
		}
	} catch (Exception e) {
		throw new BuildException(e);
	}
}

protected Project getProject() {
	if (project != null)
		return project;
	project = new Project();
	project.init();
	addBuildListeners(project);
	setProperties(project);
	setTasks(project);
	parseScript(project);
	return project;
}

protected void setProperties(Project project) {
	project.setProperty(PROPERTY_ECLIPSE_RUNNING, "true");
}

protected void setTasks(Project project) {
	try {
		Class taskClass = Class.forName("org.eclipse.ant.internal.ui.tasks.RefreshLocalTask");
		project.addTaskDefinition("refreshLocal", taskClass);
	} catch (Exception e) {
		throw new BuildException(e);
	}
}


/**
 * Parses the build script and adds necessary information into
 * the given project.
 */
protected void parseScript(Project project) {
	File buildFile = new File(buildFileLocation);
	ProjectHelper.configureProject(project, buildFile);
}

/**
 * Runs the build script.
 */
public void run() {
	String defaultTarget = getProject().getDefaultTarget();
	getProject().executeTarget(defaultTarget);
}

/**
 * Sets the buildFileLocation.
 * 
 * @param buildFileLocation the file system location of the build file
 */
public void setBuildFileLocation(String buildFileLocation) {
	this.buildFileLocation = buildFileLocation;
}
}