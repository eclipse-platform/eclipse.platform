/*******************************************************************************
 * Copyright (c) 2005, 2015 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.ant.internal.launching.debug;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Stack;
import java.util.Vector;

import org.apache.tools.ant.BuildEvent;
import org.apache.tools.ant.Location;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Target;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.MacroInstance;
import org.eclipse.ant.internal.launching.debug.model.DebugMessageIds;

public class AntDebugState {

	private static final String fgAntTaskName = "ant"; //$NON-NLS-1$
	private static final String fgAntCallTaskName = "antcall"; //$NON-NLS-1$

	private final IDebugBuildLogger fLogger;
	@SuppressWarnings("unused")
	private final Stack<Task> fTasks = new Stack<>();
	@SuppressWarnings("unused")
	private final Map<Task, Object> fTaskToProxies = new HashMap<>();
	private Task fCurrentTask;
	private Task fStepOverTask;
	private Task fStepIntoTask;
	private Task fLastTaskFinished;

	// properties set before execution
	private Map<String, Object> fInitialProperties = null;
	private Map<String, Object> fProperties = null;

	private Map<Project, Vector<?>> fProjectToTargetNames = null;
	private Map<Project, Map<Target, Vector<Target>>> fProjectToMapOfTargetToBuildSequence = null;
	@SuppressWarnings("unused")
	private final Stack<Target> fTargetsToExecute = new Stack<>();
	@SuppressWarnings("unused")
	private final Stack<Target> fTargetsExecuting = new Stack<>();

	private boolean fConsiderTargetBreakpoints = false;
	private boolean fShouldSuspend;
	private boolean fClientSuspend = false;
	private boolean fStepIntoSuspend = false;
	private boolean fIsAfterTaskEvent = false;

	public AntDebugState(IDebugBuildLogger logger) {
		fLogger = logger;
	}

	@SuppressWarnings("unused")
	public void buildStarted() {
		fProjectToTargetNames = new HashMap<>();
		fProjectToMapOfTargetToBuildSequence = new HashMap<>();
	}

	/**
	 * Call-back from {@link org.eclipse.ant.internal.launching.runtime.logger.AntProcessDebugBuildLogger} when the build has finished
	 *
	 * @since 1.0.1
	 */
	public void buildFinished() {
		if (fProjectToTargetNames != null) {
			fProjectToTargetNames.clear();
		}
		if (fProjectToMapOfTargetToBuildSequence != null) {
			fProjectToMapOfTargetToBuildSequence.clear();
		}
		fTargetsExecuting.clear();
		fTargetsToExecute.clear();
		if (fInitialProperties != null) {
			fInitialProperties.clear();
		}
		if (fProperties != null) {
			fProperties.clear();
		}
		if (fTaskToProxies != null) {
			fTaskToProxies.clear();
		}
		if (fTasks != null) {
			fTasks.clear();
		}
	}

	public void waitIfSuspended() {
		fLogger.waitIfSuspended();
	}

	public Task getLastTaskFinished() {
		return fLastTaskFinished;
	}

	private void setLastTaskFinished(Task lastTaskFinished) {
		fLastTaskFinished = lastTaskFinished;

	}

	public Task getCurrentTask() {
		return fCurrentTask;
	}

	public void setCurrentTask(Task currentTask) {
		fCurrentTask = currentTask;

	}

	private Map<String, Object> getInitialProperties() {
		return fInitialProperties;
	}

	public Task getStepOverTask() {
		return fStepOverTask;
	}

	public void setStepOverTask(Task stepOverTask) {
		fStepOverTask = stepOverTask;

	}

	private boolean considerTargetBreakpoints() {
		return fConsiderTargetBreakpoints;
	}

	private void setConsiderTargetBreakpoints(boolean considerTargetBreakpoints) {
		fConsiderTargetBreakpoints = considerTargetBreakpoints;
	}

	private Stack<Task> getTasks() {
		return fTasks;
	}

	public void setShouldSuspend(boolean shouldSuspend) {
		fShouldSuspend = shouldSuspend;
	}

	public boolean shouldSuspend() {
		return fShouldSuspend;
	}

	private Map<Target, Vector<Target>> getTargetToBuildSequence(Project project) {
		return fProjectToMapOfTargetToBuildSequence.get(project);
	}

	public void setTargetToExecute(Target target) {
		if (target == null) {
			if (!fTargetsToExecute.isEmpty()) {
				fTargetsToExecute.pop();
			}
		} else {
			fTargetsToExecute.push(target);
		}
	}

	public void setTargetExecuting(Target target) {
		if (target == null) {
			if (!fTargetsExecuting.isEmpty()) {
				fTargetsExecuting.pop();
			}
		} else {
			fTargetsExecuting.push(target);
		}
	}

	private Target getTargetToExecute() {
		if (fTargetsToExecute.isEmpty()) {
			return null;
		}
		return fTargetsToExecute.peek();
	}

	private Target getTargetExecuting() {
		if (fTargetsExecuting.isEmpty()) {
			return null;
		}
		return fTargetsExecuting.peek();
	}

	public boolean isStepIntoSuspend() {
		return isAfterTaskEvent() && fStepIntoSuspend;
	}

	public void setStepIntoSuspend(boolean stepIntoSuspend) {
		fStepIntoSuspend = stepIntoSuspend;
	}

	public boolean isClientSuspend() {
		return fClientSuspend;
	}

	public void setClientSuspend(boolean clientSuspend) {
		fClientSuspend = clientSuspend;
	}

	public Task getStepIntoTask() {
		return fStepIntoTask;
	}

	public void setStepIntoTask(Task stepIntoTask) {
		fStepIntoTask = stepIntoTask;
	}

	public void resume() {
		fLogger.notifyAll();
	}

	public Map<String, Object> getProperties() {
		return fProperties;
	}

	public Location getBreakpointLocation() {
		if (isAfterTaskEvent() && getCurrentTask() != null) {
			return getCurrentTask().getLocation();
		}
		if (considerTargetBreakpoints()) {
			Target targetExecuting = getTargetExecuting();
			if (targetExecuting != null) {
				return getLocation(targetExecuting);
			}
		}
		return null;
	}

	private boolean isAfterTaskEvent() {
		return fIsAfterTaskEvent;
	}

	private void setAfterTaskEvent(boolean isAfterTaskEvent) {
		fIsAfterTaskEvent = isAfterTaskEvent;
	}

	public void taskStarted(BuildEvent event) {
		setAfterTaskEvent(true);
		if (getInitialProperties() == null) {// implicit or top level target does not fire targetStarted()
			fInitialProperties = event.getProject().getProperties();
		}

		setCurrentTask(event.getTask());
		setConsiderTargetBreakpoints(false);
		Stack<Task> tasks = getTasks();
		if (!tasks.isEmpty()) {
			// cache the parent task proxy as when that task is started or finished the
			// proxy is not yet available or is nulled out
			Task parentTask = tasks.peek();
			Object proxy = parentTask.getRuntimeConfigurableWrapper().getProxy();
			if (proxy != null) {
				fTaskToProxies.put(parentTask, proxy);
			}
		}
		tasks.push(getCurrentTask());
		waitIfSuspended();
	}

	public void taskFinished() {
		Stack<Task> tasks = getTasks();
		if (!tasks.empty()) {
			Task lastTask = tasks.pop();
			setLastTaskFinished(lastTask);
			setCurrentTask(null);
			String taskName = lastTask.getTaskName();

			if (getStepOverTask() != null) {
				if ((fgAntCallTaskName.equals(taskName) || fgAntTaskName.equals(taskName))
						&& (!fgAntCallTaskName.equals(getStepOverTask().getTaskName()) && !fgAntTaskName.equals(getStepOverTask().getTaskName()))) {
					setShouldSuspend(true);
				} else if (fTaskToProxies.remove(lastTask) instanceof MacroInstance) {
					setShouldSuspend(true);
				}
			}
		}
		waitIfSuspended();
	}

	public void stepOver() {
		setStepOverTask(getCurrentTask());
		if (getCurrentTask() == null) {
			// stepping over target breakpoint
			setShouldSuspend(true);
		}
		resume();
	}

	public void targetStarted(BuildEvent event) {
		setAfterTaskEvent(false);
		Project eventProject = event.getProject();
		if (getInitialProperties() == null) {
			fInitialProperties = eventProject.getProperties();
		}
		if (fProjectToTargetNames.get(eventProject) == null) {
			Object ref = eventProject.getReference(IAntCoreConstants.TARGET_VECTOR_NAME);
			if (ref != null) {
				fProjectToTargetNames.put(eventProject, (Vector<?>) ref);
				@SuppressWarnings("unused")
				HashMap<Target, Vector<Target>> targetToBuildSequence = new HashMap<>();
				setTargetToExecute(initializeBuildSequenceInformation(event, targetToBuildSequence));
				fProjectToMapOfTargetToBuildSequence.put(eventProject, targetToBuildSequence);
			}
		}

		setTargetExecuting(event.getTarget());
		if (event.getTarget().equals(getTargetToExecute())) {
			// the dependencies of the target to execute have been met
			// prepare for the next target
			Vector<?> targets = fProjectToTargetNames.get(eventProject);
			if (!targets.isEmpty()) {
				setTargetToExecute(eventProject.getTargets().get(targets.remove(0)));
			} else {
				setTargetToExecute(null);
			}
		}
		setConsiderTargetBreakpoints(true);
	}

	public int getLineNumber(Location location) {
		try { // succeeds with Ant newer than 1.6
			return location.getLineNumber();
		}
		catch (NoSuchMethodError e) {
			// Ant before 1.6
			String locationString = location.toString();
			if (locationString.length() == 0) {
				return 0;
			}
			// filename: lineNumber: ("c:\buildfile.xml: 12: ")
			int lastIndex = locationString.lastIndexOf(':');
			int index = locationString.lastIndexOf(':', lastIndex - 1);
			if (index != -1) {
				try {
					return Integer.parseInt(locationString.substring(index + 1, lastIndex));
				}
				catch (NumberFormatException nfe) {
					return 0;
				}
			}
			return 0;
		}
	}

	public static Location getLocation(Target target) {
		try {// succeeds with Ant newer than 1.6.2
			return target.getLocation();
		}
		catch (NoSuchMethodError e) {
			return Location.UNKNOWN_LOCATION;
		}
	}

	public String getFileName(Location location) {
		try {// succeeds with Ant newer than 1.6
			return location.getFileName();
		}
		catch (NoSuchMethodError e) {
			// Ant before 1.6
			String locationString = location.toString();
			if (locationString.length() == 0) {
				return null;
			}
			// filename: lineNumber: ("c:\buildfile.xml: 12: ")
			int lastIndex = locationString.lastIndexOf(':');
			int index = locationString.lastIndexOf(':', lastIndex - 1);
			if (index == -1) {
				index = lastIndex; // only the filename is known
			}
			if (index != -1) {
				// bug 84403
				// if (locationString.startsWith("file:")) {
				// return FileUtils.newFileUtils().fromURI(locationString);
				// }
				// remove file:
				return locationString.substring(5, index);
			}
			return null;
		}
	}

	private void appendToStack(StringBuffer stackRepresentation, String targetName, String taskName, Location location) {
		stackRepresentation.append(targetName);
		stackRepresentation.append(DebugMessageIds.MESSAGE_DELIMITER);
		stackRepresentation.append(taskName);
		stackRepresentation.append(DebugMessageIds.MESSAGE_DELIMITER);

		stackRepresentation.append(getFileName(location));
		stackRepresentation.append(DebugMessageIds.MESSAGE_DELIMITER);
		stackRepresentation.append(getLineNumber(location));
		stackRepresentation.append(DebugMessageIds.MESSAGE_DELIMITER);
	}

	public void marshalStack(StringBuffer stackRepresentation) {
		Stack<Task> tasks = getTasks();

		stackRepresentation.append(DebugMessageIds.STACK);
		stackRepresentation.append(DebugMessageIds.MESSAGE_DELIMITER);

		Target targetToExecute = getTargetToExecute();
		Target targetExecuting = getTargetExecuting();

		Project projectExecuting = null;
		if (targetExecuting != null) {
			projectExecuting = targetExecuting.getProject();
		} else if (!tasks.empty()) { // no target...must be a task
			Task task = tasks.peek();
			projectExecuting = task.getProject();
		}

		if (!isAfterTaskEvent()) {
			appendToStack(stackRepresentation, targetExecuting.getName(), IAntCoreConstants.EMPTY_STRING, getLocation(targetExecuting));
		}
		for (int i = tasks.size() - 1; i >= 0; i--) {
			Task task = tasks.get(i);
			if (task.getProject() == projectExecuting) {
				appendToStack(stackRepresentation, task.getOwningTarget().getName(), task.getTaskName(), task.getLocation());
			} else {
				// sub build target dependencies
				String targetName = task.getOwningTarget().getName();
				if (targetName != null && targetName.length() != 0) { // skip for implicit target
					Iterator<Target> itr = fTargetsToExecute.iterator();
					while (itr.hasNext()) {
						Target target = itr.next();
						if (target.getProject() != projectExecuting) {
							targetToExecute = target;
							continue;
						}
						marshalTargetDependancyStack(stackRepresentation, target, targetExecuting);
					}
				}
				projectExecuting = task.getProject();
				targetExecuting = task.getOwningTarget();
				appendToStack(stackRepresentation, targetExecuting.getName(), task.getTaskName(), task.getLocation());
			}
		}

		// target dependency stack
		marshalTargetDependancyStack(stackRepresentation, targetToExecute, targetExecuting);
	}

	private void marshalTargetDependancyStack(StringBuffer stackRepresentation, Target targetToExecute, Target targetExecuting) {
		if (targetToExecute != null) {
			Vector<Target> buildSequence = getTargetToBuildSequence(targetToExecute.getProject()).get(targetToExecute);
			int startIndex = buildSequence.indexOf(targetExecuting) + 1;
			int dependancyStackDepth = buildSequence.indexOf(targetToExecute);

			Target stackTarget;
			for (int i = startIndex; i <= dependancyStackDepth; i++) {
				stackTarget = buildSequence.get(i);
				if (stackTarget.dependsOn(targetExecuting.getName())) {
					appendToStack(stackRepresentation, stackTarget.getName(), IAntCoreConstants.EMPTY_STRING, getLocation(stackTarget));
				}
			}
		}
	}

	public void marshallProperties(StringBuffer propertiesRepresentation, boolean escapeLineSep) {
		Stack<Task> tasks = getTasks();
		if (tasks.isEmpty()) {
			return;
		}
		propertiesRepresentation.append(DebugMessageIds.PROPERTIES);
		propertiesRepresentation.append(DebugMessageIds.MESSAGE_DELIMITER);

		Project project = tasks.peek().getProject();
		Map<String, Object> lastProperties = getProperties();

		Map<String, Object> currentProperties = project.getProperties();
		if (lastProperties != null && currentProperties.size() == lastProperties.size()) {
			// no new properties
			return;
		}

		Map<String, Object> initialProperties = getInitialProperties();
		Map<String, Object> currentUserProperties = project.getUserProperties();
		String originalPropertyName;
		String propertyValue;
		for (String propertyName : currentProperties.keySet()) {
			originalPropertyName = propertyName;
			if (lastProperties == null || lastProperties.get(propertyName) == null) { // new property
				if (escapeLineSep) {
					propertyName = escapeLineSeparator(propertyName);
				}
				propertiesRepresentation.append(propertyName.length());
				propertiesRepresentation.append(DebugMessageIds.MESSAGE_DELIMITER);
				propertiesRepresentation.append(propertyName);
				propertiesRepresentation.append(DebugMessageIds.MESSAGE_DELIMITER);
				propertyValue = (String) currentProperties.get(originalPropertyName);
				if (escapeLineSep) {
					propertyValue = escapeLineSeparator(propertyValue);
				}
				propertiesRepresentation.append(propertyValue.length());
				propertiesRepresentation.append(DebugMessageIds.MESSAGE_DELIMITER);
				propertiesRepresentation.append(propertyValue);
				propertiesRepresentation.append(DebugMessageIds.MESSAGE_DELIMITER);
				propertiesRepresentation.append(getPropertyType(initialProperties, currentUserProperties, originalPropertyName));
				propertiesRepresentation.append(DebugMessageIds.MESSAGE_DELIMITER);
			}
		}
		propertiesRepresentation.deleteCharAt(propertiesRepresentation.length() - 1);
		fProperties = currentProperties;
	}

	private int getPropertyType(Map<String, Object> initialProperties, Map<String, Object> currentUserProperties, String propertyName) {
		if (initialProperties.get(propertyName) != null) { // properties set before the start of the build
			if (currentUserProperties.get(propertyName) == null) {
				return DebugMessageIds.PROPERTY_SYSTEM;
			}
			return DebugMessageIds.PROPERTY_USER;
		} else if (currentUserProperties.get(propertyName) == null) {
			return DebugMessageIds.PROPERTY_RUNTIME;
		} else {
			return DebugMessageIds.PROPERTY_USER;
		}
	}

	private String escapeLineSeparator(String stringToEscape) {
		if (!(stringToEscape.indexOf('\r') != -1 || stringToEscape.indexOf('\n') != -1 || stringToEscape.contains("\\r") //$NON-NLS-1$

				|| stringToEscape.contains("\\n"))) { //$NON-NLS-1$
			return stringToEscape;
		}
		StringBuilder escapedValue = new StringBuilder(stringToEscape);
		for (int i = 0; i < escapedValue.length(); i++) {
			switch (escapedValue.charAt(i)) {
				case '\r':
					escapedValue.replace(i, i + 1, "\\r"); //$NON-NLS-1$
					i++;
					break;
				case '\n':
					escapedValue.replace(i, i + 1, "\\n"); //$NON-NLS-1$
					i++;
					break;
				case '\\':
					if (escapedValue.charAt(i + 1) == 'r' || escapedValue.charAt(i + 1) == 'n') {
						escapedValue.replace(i, i + 1, "\\\\"); //$NON-NLS-1$
						i++;
					}
					break;
				default:
					break;
			}
		}

		return escapedValue.toString();
	}

	private Target initializeBuildSequenceInformation(BuildEvent event, Map<Target, Vector<Target>> targetToBuildSequence) {
		Project antProject = event.getProject();
		Vector<String> targets = antProject.getReference(IAntCoreConstants.TARGET_VECTOR_NAME);
		if (targets == null || targets.size() < 1) {
			return null;
		}
		Hashtable<String, Target> allTargets = antProject.getTargets();
		Vector<Target> sortedTargets;
		for (String targetName : targets) {
			sortedTargets = antProject.topoSort(targetName, allTargets);
			targetToBuildSequence.put(allTargets.get(targetName), sortedTargets);
		}
		// the target to execute
		return allTargets.get(targets.remove(0));
	}
}
