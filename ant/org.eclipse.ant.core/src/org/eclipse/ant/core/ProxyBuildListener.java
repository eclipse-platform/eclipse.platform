package org.eclipse.ant.core;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.*;

import org.apache.tools.ant.*;
import org.apache.tools.ant.BuildEvent;
import org.apache.tools.ant.BuildListener;
/**
 * Proxy for BuildListener objects.
 */
public class ProxyBuildListener implements BuildListener {

	private List listeners;
	private ClassLoader pluginLoader;

/**
 * Constructor for ProxyBuildListener.
 */
public ProxyBuildListener() {
	listeners = new ArrayList(10);
}

public void setPluginLoader(ClassLoader pluginLoader) {
	this.pluginLoader = pluginLoader;
}

public void addListener(Object listener) {
	listeners.add(listener);
}

/*
 * @see BuildListener#buildStarted(BuildEvent)
 */
public void buildStarted(BuildEvent event) {
}

/*
 * @see BuildListener#buildFinished(BuildEvent)
 */
public void buildFinished(BuildEvent event) {
}

/*
 * @see BuildListener#targetStarted(BuildEvent)
 */
public void targetStarted(BuildEvent event) {
}

/*
 * @see BuildListener#targetFinished(BuildEvent)
 */
public void targetFinished(BuildEvent event) {
}

/*
 * @see BuildListener#taskStarted(BuildEvent)
 */
public void taskStarted(BuildEvent event) {
}

/*
 * @see BuildListener#taskFinished(BuildEvent)
 */
public void taskFinished(BuildEvent event) {
}

/*
 * @see BuildListener#messageLogged(BuildEvent)
 */
public void messageLogged(BuildEvent event) {
	String message = event.getMessage();
	int priority = event.getPriority();
	try {
		Class classBuildEvent = pluginLoader.loadClass("org.apache.tools.ant.BuildEvent");
		Class classBuildListener = pluginLoader.loadClass("org.apache.tools.ant.BuildListener");
		Method messageLogged = classBuildListener.getMethod("messageLogged", new Class[]{classBuildEvent});

		Class classProject = pluginLoader.loadClass("org.apache.tools.ant.Project");
		Constructor constructor = classBuildEvent.getConstructor(new Class[]{classProject});
		Object newEvent = constructor.newInstance(new Object[]{classProject.newInstance()});
		Method setMessage = classBuildEvent.getMethod("setMessage", new Class[]{String.class, int.class});
		setMessage.invoke(newEvent, new Object[]{message, new Integer(priority)});

		for (Iterator iterator = listeners.iterator(); iterator.hasNext();) {
			Object listener = iterator.next();
			messageLogged.invoke(listener, new Object[]{newEvent});
		}
	} catch (Exception e) {
		throw new BuildException(e);
	}
}

}
