/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ant.internal.core.ant;


import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.tools.ant.BuildEvent;
import org.apache.tools.ant.BuildListener;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Target;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.Ant;
import org.apache.tools.ant.taskdefs.CallTarget;
import org.eclipse.ant.core.AntCorePlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;

/**
 * Reports progress and checks for cancelation of a script execution.
 */
public class ProgressBuildListener implements BuildListener {

	protected Map projects;
	protected Project mainProject;
	protected Project parentProject;
	private Thread currentTaskThread;

	/**
	 *  Contains the progress monitor instances for the various
	 *	projects in a chain.
	 */
	protected class ProjectMonitors {
		/**
		 *  This field is null for the main project
		 */
		private Target mainTarget;
		private IProgressMonitor mainMonitor;
		private IProgressMonitor targetMonitor;
		private IProgressMonitor taskMonitor;
		
		protected IProgressMonitor getMainMonitor() {
			return mainMonitor;
		}

		protected Target getMainTarget() {
			return mainTarget;
		}

		protected IProgressMonitor getTargetMonitor() {
			return targetMonitor;
		}

		protected IProgressMonitor getTaskMonitor() {
			return taskMonitor;
		}

		protected void setMainMonitor(IProgressMonitor mainMonitor) {
			this.mainMonitor = mainMonitor;
		}

		protected void setMainTarget(Target mainTarget) {
			this.mainTarget = mainTarget;
		}

		protected void setTargetMonitor(IProgressMonitor targetMonitor) {
			this.targetMonitor = targetMonitor;
		}

		protected void setTaskMonitor(IProgressMonitor taskMonitor) {
			this.taskMonitor = taskMonitor;
		}

	}

	public ProgressBuildListener(Project project, List targetNames, IProgressMonitor monitor) {
		projects = new HashMap();
		mainProject = project;
		ProjectMonitors monitors = new ProjectMonitors();
		if (monitor == null) {
			monitor= new NullProgressMonitor();
		}
		monitors.setMainMonitor(monitor);
		projects.put(mainProject, monitors);
		List targets= new ArrayList(targetNames.size());
		for (int i = 0; i < targetNames.size(); i++) {
			String targetName = (String) targetNames.get(i);
			Target target= (Target) mainProject.getTargets().get(targetName);
			if (target != null) {
				targets.add(target);
			}
		}
		int work = computeWork(targets);
		monitors.getMainMonitor().beginTask("", work);  //$NON-NLS-1$
	}

	/* (non-Javadoc)
	 * @see org.apache.tools.ant.BuildListener#buildStarted(org.apache.tools.ant.BuildEvent)
	 */
	public void buildStarted(BuildEvent event) {
		checkCanceled();
	}

	protected int computeWork(List targets) {
		int result = 0;
		for (int i = 0; i < targets.size(); i++) {
			result = result + countTarget((Target)targets.get(i), new ArrayList());
		}
		return result;
	}

	protected int countTarget(Target target, List alreadySeen) {
		int result = 1;
		Project project = target.getProject();
		Hashtable targets= project.getTargets();
        String targetName;
        Target dependency;
		for (Enumeration dependencies = target.getDependencies(); dependencies.hasMoreElements();) {
			targetName = (String) dependencies.nextElement();
            if (alreadySeen.contains(targetName)) { //circular dependency or common dependancy
				return result;
            }
            alreadySeen.add(targetName);
			dependency = (Target)targets.get(targetName);
			if (dependency != null) {
				result = result + countTarget(dependency, alreadySeen);
			}
		}
		// we have to handle antcall tasks as well
		Task[] tasks = target.getTasks();
		for (int i = 0; i < tasks.length; i++) {
			if (tasks[i] instanceof CallTarget) {
				// As we do not have access to the information (at least in Ant 1.4.1)
				// describing what target is executed by this antcall task, we assume
				// a scenario where it depends on all targets of the project but itself.
				result = result + (targets.size() - 1);
			}
		}
		return result;
	}

	/* (non-Javadoc)
	 * @see org.apache.tools.ant.BuildListener#buildFinished(org.apache.tools.ant.BuildEvent)
	 */
	public void buildFinished(BuildEvent event) {
		ProjectMonitors monitors = (ProjectMonitors) projects.get(mainProject);
		monitors.getMainMonitor().done();
		Set keys= projects.keySet();
		Iterator itr= keys.iterator();
		while (itr.hasNext()) {
			Project project = (Project) itr.next();
			project.removeBuildListener(this);
			project.getReferences().remove(AntCorePlugin.ECLIPSE_PROGRESS_MONITOR);
		}
	}

	/* (non-Javadoc)
	 * @see org.apache.tools.ant.BuildListener#targetStarted(org.apache.tools.ant.BuildEvent)
	 */
	public void targetStarted(BuildEvent event) {
		checkCanceled();
		Project currentProject = event.getProject();
		if (currentProject == null) {
			return;
		}
		Target target = event.getTarget();
		ProjectMonitors monitors = (ProjectMonitors) projects.get(currentProject);

		// if monitors is null we are in a new script
		if (monitors == null) {
			monitors = createMonitors(currentProject, target);
		}

		monitors.setTargetMonitor(subMonitorFor(monitors.getMainMonitor(), 1));
		int work = (target != null) ? target.getTasks().length : 100;
		monitors.getTargetMonitor().beginTask("", work);  //$NON-NLS-1$
	}

	protected ProjectMonitors createMonitors(Project currentProject, Target target) {
		ProjectMonitors monitors = new ProjectMonitors();
		// remember the target so we can remove this monitors object later
		monitors.setMainTarget(target);
		List targets= new ArrayList(1);
		targets.add(target);
		int work = computeWork(targets);
		ProjectMonitors parentMonitors = null;
		if (parentProject == null) {
			parentMonitors = (ProjectMonitors) projects.get(mainProject);
			monitors.setMainMonitor(subMonitorFor(parentMonitors.getMainMonitor(), 1));
		} else {
			parentMonitors = (ProjectMonitors) projects.get(parentProject);
			parentProject = null;
			monitors.setMainMonitor(subMonitorFor(parentMonitors.getTaskMonitor(), 1));
		}
		monitors.getMainMonitor().beginTask("", work);  //$NON-NLS-1$
		projects.put(currentProject, monitors);
		return monitors;
	}

	/* (non-Javadoc)
	 * @see org.apache.tools.ant.BuildListener#targetFinished(org.apache.tools.ant.BuildEvent)
	 */
	public void targetFinished(BuildEvent event) {
		checkCanceled();
		Project currentProject = event.getProject();
		if (currentProject == null) {
			return;
		}
		ProjectMonitors monitors = (ProjectMonitors) projects.get(currentProject);
		if (monitors == null) {
			return;
		}
		monitors.getTargetMonitor().done();
		// if this is not the main project test if we are done with this project
		if ((currentProject != mainProject) && (monitors.getMainTarget() == event.getTarget())) {
			monitors.getMainMonitor().done();
			projects.remove(currentProject);
		}
	}

	/* (non-Javadoc)
	 * @see org.apache.tools.ant.BuildListener#taskStarted(org.apache.tools.ant.BuildEvent)
	 */
	public void taskStarted(BuildEvent event) {
		checkCanceled();
		Project currentProject = event.getProject();
		if (currentProject == null) {
			return;
		}
		currentProject.getReferences().remove(AntCorePlugin.ECLIPSE_PROGRESS_MONITOR);
		ProjectMonitors monitors = (ProjectMonitors) projects.get(currentProject);
		if (monitors == null) {
			return;
		}
		Task task = event.getTask();
		if (task == null) {
			return;
		}
		currentTaskThread= Thread.currentThread();
		monitors.setTaskMonitor(subMonitorFor(monitors.getTargetMonitor(), 1));
		monitors.getTaskMonitor().beginTask("", 1);  //$NON-NLS-1$
		// If this script is calling another one, track the project chain.
		if (task instanceof Ant) {
			parentProject = currentProject;
		} else {
			currentProject.addReference(AntCorePlugin.ECLIPSE_PROGRESS_MONITOR, monitors.getTaskMonitor());
		}
	}

	/* (non-Javadoc)
	 * @see org.apache.tools.ant.BuildListener#taskFinished(org.apache.tools.ant.BuildEvent)
	 */
	public void taskFinished(BuildEvent event) {
		checkCanceled();
		Project project = event.getProject();
		if (project == null) {
			return;
		}
		project.getReferences().remove(AntCorePlugin.ECLIPSE_PROGRESS_MONITOR);
		ProjectMonitors monitors = (ProjectMonitors) projects.get(project);
		if (monitors == null) {
			return;
		}
		monitors.getTaskMonitor().done();
		currentTaskThread= null;
	}

	/* (non-Javadoc)
	 * @see org.apache.tools.ant.BuildListener#messageLogged(org.apache.tools.ant.BuildEvent)
	 */
	public void messageLogged(BuildEvent event) {
		checkCanceled();
	}

	protected void checkCanceled() {
		//only cancel if the current task thread matches the current thread
		//do not want to throw an exception in a separate thread or process
		//see bug 32657
		if (currentTaskThread != null && currentTaskThread != Thread.currentThread()) {
			return;
		}
		ProjectMonitors monitors = (ProjectMonitors) projects.get(mainProject);
		if (monitors.getMainMonitor().isCanceled()) {
			currentTaskThread= null;
			throw new OperationCanceledException(InternalAntMessages.ProgressBuildListener_Build_cancelled__5);
		}
	}

	protected IProgressMonitor subMonitorFor(IProgressMonitor monitor, int ticks) {
		if (monitor == null) {
			return new NullProgressMonitor();
		}
		if (monitor instanceof NullProgressMonitor) {
			return monitor;
		}
		return new SubProgressMonitor(monitor, ticks);
	}
}
