/*
 * (c) Copyright 2001 MyCorporation.
 * All Rights Reserved.
 */
package org.eclipse.ant.internal.core.old;

import java.util.*;

import org.apache.tools.ant.BuildEvent;

/**
 * Proxy class for providing updates to several ant listeners.
 */
public class AntNotificationManager implements IAntRunnerListener {
	protected List listeners = Collections.synchronizedList(new ArrayList());

/**
 * Registers an ant build listener for notification.
 */
public void addListener(IAntRunnerListener listener) {
	listeners.add(listener);
}
protected IAntRunnerListener[] getListeners() {
	return (IAntRunnerListener[]) listeners.toArray(new IAntRunnerListener[listeners.size()]);
}
/**
 * @see IAntRunnerListener#messageLogged(String, int)
 */
public void messageLogged(String message, int priority) {
	IAntRunnerListener[] list = getListeners();
	for (int i = 0; i < list.length; i++) {
		list[i].messageLogged(message, priority);
	}
}
/**
 * @see IAntRunnerListener#executeTargetStarted(BuildEvent)
 */
public void executeTargetStarted(BuildEvent event) {
	IAntRunnerListener[] list = getListeners();
	for (int i = 0; i < list.length; i++) {
		list[i].executeTargetStarted(event);
	}
}
/**
 * @see IAntRunnerListener#executeTargetFinished(BuildEvent)
 */
public void executeTargetFinished(BuildEvent event) {
	IAntRunnerListener[] list = getListeners();
	for (int i = 0; i < list.length; i++) {
		list[i].executeTargetFinished(event);
	}
}
/**
 * @see BuildListener#buildStarted(BuildEvent)
 */
public void buildStarted(BuildEvent event) {
	IAntRunnerListener[] list = getListeners();
	for (int i = 0; i < list.length; i++) {
		list[i].buildStarted(event);
	}
}
/**
 * @see BuildListener#buildFinished(BuildEvent)
 */
public void buildFinished(BuildEvent event) {
	IAntRunnerListener[] list = getListeners();
	for (int i = 0; i < list.length; i++) {
		list[i].buildFinished(event);
	}
}
/**
 * Removes an ant listener from the list.
 */
public void removeListener(IAntRunnerListener listener) {
	listeners.remove(listener);
}
/**
 * @see BuildListener#targetStarted(BuildEvent)
 */
public void targetStarted(BuildEvent event) {
	IAntRunnerListener[] list = getListeners();
	for (int i = 0; i < list.length; i++) {
		list[i].targetStarted(event);
	}
}
/**
 * @see BuildListener#targetFinished(BuildEvent)
 */
public void targetFinished(BuildEvent event) {
	IAntRunnerListener[] list = getListeners();
	for (int i = 0; i < list.length; i++) {
		list[i].targetFinished(event);
	}
}
/**
 * @see BuildListener#taskStarted(BuildEvent)
 */
public void taskStarted(BuildEvent event) {
	IAntRunnerListener[] list = getListeners();
	for (int i = 0; i < list.length; i++) {
		list[i].taskStarted(event);
	}
}
/**
 * @see BuildListener#taskFinished(BuildEvent)
 */
public void taskFinished(BuildEvent event) {
	IAntRunnerListener[] list = getListeners();
	for (int i = 0; i < list.length; i++) {
		list[i].taskFinished(event);
	}
}
/**
 * @see BuildListener#messageLogged(BuildEvent)
 */
public void messageLogged(BuildEvent event) {
	IAntRunnerListener[] list = getListeners();
	for (int i = 0; i < list.length; i++) {
		list[i].messageLogged(event);
	}
}
}
