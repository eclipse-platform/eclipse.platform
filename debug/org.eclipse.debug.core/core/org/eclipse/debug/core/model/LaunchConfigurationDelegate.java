/*******************************************************************************
 * Copyright (c) 2004, 2019 IBM Corporation and others.
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
 *     Mike Morearty - Bug 255310: Launching only gets the progress bar to 91%
 *******************************************************************************/
package org.eclipse.debug.core.model;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IBreakpointManager;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.IStatusHandler;
import org.eclipse.debug.internal.core.DebugCoreMessages;
import org.eclipse.debug.internal.core.IInternalDebugCoreConstants;

/**
 * Default implementation of a launch configuration delegate. Provides
 * convenience methods for computing the build order of projects,
 * building projects, and searching for errors in the workspace. The
 * default pre-launch check prompts the user to launch in debug mode
 * if breakpoints are present in the workspace.
 * <p>
 * Clients implementing launch configuration delegates should subclass
 * this class.
 * </p>
 * @since 3.0
 */
public abstract class LaunchConfigurationDelegate implements ILaunchConfigurationDelegate2 {

	/**
	 * Constant to define debug.core for the status codes
	 *
	 * @since 3.2
	 */
	private static final String DEBUG_CORE = "org.eclipse.debug.core"; //$NON-NLS-1$

	/**
	 * Constant to define debug.ui for the status codes
	 *
	 * @since 3.2
	 */
	private static final String DEBUG_UI = "org.eclipse.debug.ui"; //$NON-NLS-1$

	/**
	 * Status code for which a UI prompter is registered.
	 */
	protected static final IStatus promptStatus = new Status(IStatus.INFO, DEBUG_UI, 200, IInternalDebugCoreConstants.EMPTY_STRING, null);

	/**
	 * Status code for which a prompter is registered to ask the user if they
	 * want to launch in debug mode when breakpoints are present.
	 */
	protected static final IStatus switchToDebugPromptStatus = new Status(IStatus.INFO, DEBUG_CORE, 201, IInternalDebugCoreConstants.EMPTY_STRING, null);

	/**
	 * Status code for which a prompter is registered to ask the user if the
	 * want to continue launch despite existing compile errors
	 */
	protected static final IStatus complileErrorPromptStatus = new Status(IStatus.INFO, DEBUG_CORE, 202, IInternalDebugCoreConstants.EMPTY_STRING, null);

	/**
	 * Status code for which a prompter will ask the user to save any/all of the dirty editors which have only to do
	 * with this launch (scoping them to the current launch/build)
	 *
	 * @since 3.2
	 */
	protected static final IStatus saveScopedDirtyEditors = new Status(IStatus.INFO, DEBUG_CORE, 222, IInternalDebugCoreConstants.EMPTY_STRING, null);

	/**
	 * Status code for which a prompter is registered to ask the user if the
	 * want to continue launch despite existing compile errors in specific
	 * projects. This enhances the 'compileErrorPromptStatus' by specifying
	 * which projects the errors exist in.
	 *
	 * @since 3.1
	 */
	protected static final IStatus complileErrorProjectPromptStatus = new Status(IStatus.INFO, DEBUG_CORE, 203, IInternalDebugCoreConstants.EMPTY_STRING, null);

	@Override
	public ILaunch getLaunch(ILaunchConfiguration configuration, String mode) throws CoreException {
		return null;
	}

	@Override
	public boolean buildForLaunch(ILaunchConfiguration configuration, String mode, IProgressMonitor monitor) throws CoreException {
		IProject[] projects = getBuildOrder(configuration, mode);
		if (projects == null) {
			return true;
		}
		buildProjects(projects, SubMonitor.convert(monitor, 1));
		return false;
	}

	/**
	 * Returns the projects to build before launching the given launch configuration
	 * or <code>null</code> if the entire workspace should be built incrementally.
	 * Subclasses should override as required.
	 *
	 * @param configuration the configuration being launched
	 * @param mode launch mode
	 * @return projects to build, in build order, or <code>null</code>
	 * @throws CoreException if an exception occurs
	 */
	protected IProject[] getBuildOrder(ILaunchConfiguration configuration, String mode) throws CoreException {
		return null;
	}

	/**
	 * Returns the set of projects to use when searching for errors or <code>null</code>
	 * if no search is to be done.
	 *
	 * @param configuration the configuration being launched
	 * @param mode launch mode
	 * @return a list of projects or <code>null</code>
	 * @throws CoreException if an exception occurs
	 */
	protected IProject[] getProjectsForProblemSearch(ILaunchConfiguration configuration, String mode) throws CoreException {
		return null;
	}

	@Override
	public boolean finalLaunchCheck(ILaunchConfiguration configuration, String mode, IProgressMonitor monitor) throws CoreException {
		monitor.beginTask("", 1); //$NON-NLS-1$
		try {
			IProject[] projects = getProjectsForProblemSearch(configuration, mode);
			if (projects == null) {
				return true; //continue launch
			}
			boolean continueLaunch = true;

			monitor.subTask(DebugCoreMessages.LaunchConfigurationDelegate_6);
			List<IAdaptable> errors = new ArrayList<>();
			for (IProject project : projects) {
				monitor.subTask(MessageFormat.format(DebugCoreMessages.LaunchConfigurationDelegate_7, new Object[] { project.getName() }));
				if (existsProblems(project)) {
					errors.add(project);
				}
			}
			if (!errors.isEmpty()) {
				errors.add(0, configuration);
				IStatusHandler prompter = DebugPlugin.getDefault().getStatusHandler(promptStatus);
				if (prompter != null) {
					continueLaunch = ((Boolean) prompter.handleStatus(complileErrorProjectPromptStatus, errors)).booleanValue();
				}
			}

			return continueLaunch;
		} finally {
			monitor.done();
		}
	}

	/*
	 * If launching in run mode, and the configuration supports debug mode,
	 * check if there are any breakpoints in the workspace, and ask the user if
	 * they'd rather launch in debug mode. <p> Since 3.2, this check also
	 * performs saving of resources before launching. </p>
	 * @see
	 * org.eclipse.debug.core.model.ILaunchConfigurationDelegate2#preLaunchCheck
	 * (org.eclipse.debug.core.ILaunchConfiguration, java.lang.String,
	 * org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public boolean preLaunchCheck(ILaunchConfiguration configuration, String mode, IProgressMonitor monitor) throws CoreException {
		if (!saveBeforeLaunch(configuration, mode, monitor)) {
			return false;
		}
		if (mode.equals(ILaunchManager.RUN_MODE) && configuration.supportsMode(ILaunchManager.DEBUG_MODE)) {
			IBreakpoint[] breakpoints= getBreakpoints(configuration);
			if (breakpoints == null) {
				return true;
			}
			for (IBreakpoint breakpoint : breakpoints) {
				if (breakpoint.isEnabled()) {
					IStatusHandler prompter = DebugPlugin.getDefault().getStatusHandler(promptStatus);
					if (prompter != null) {
						boolean launchInDebugModeInstead = ((Boolean)prompter.handleStatus(switchToDebugPromptStatus, configuration)).booleanValue();
						if (launchInDebugModeInstead) {
							return false; //kill this launch
						}
					}
					// if no user prompt, or user says to continue (no need to check other breakpoints)
					return true;
				}
			}
		}
		// no enabled breakpoints... continue launch
		return true;
	}

	/**
	 * Performs the scoped saving of resources before launching and returns whether
	 * the launch should continue. By default, only resources contained within the projects
	 * which are part of the build scope are considered.
	 * <p>
	 * Subclasses may override this method if required.
	 * </p>
	 *
	 * @param configuration the configuration being launched
	 * @param mode the launch mode
	 * @param monitor progress monitor
	 * @return whether the launch should continue
	 * @throws CoreException if an exception occurs during the save
	 * @since 3.2
	 */
	protected boolean saveBeforeLaunch(ILaunchConfiguration configuration, String mode, IProgressMonitor monitor) throws CoreException {
		monitor.beginTask("", 1); //$NON-NLS-1$
		try {
			IStatusHandler prompter = DebugPlugin.getDefault().getStatusHandler(promptStatus);
			if(prompter != null) {
				//do save here and remove saving from DebugUIPlugin to avoid it 'trumping' this save
				IProject[] buildOrder = getBuildOrder(configuration, mode);
				if(!((Boolean)prompter.handleStatus(saveScopedDirtyEditors, new Object[]{configuration, buildOrder})).booleanValue()) {
					return false;
				}
			}
			return true;
		} finally {
			monitor.done();
		}
	}

	/**
	 * Returns the breakpoint collection that is relevant for this launch delegate.
	 * By default this is all the breakpoints registered with the Debug breakpoint manager.
	 *
	 * @param configuration the configuration to get associated breakpoints for
	 * @since 3.1
	 * @return the breakpoints that are relevant for this launch delegate
	 */
	protected IBreakpoint[] getBreakpoints(ILaunchConfiguration configuration) {
		IBreakpointManager breakpointManager = DebugPlugin.getDefault().getBreakpointManager();
		if (!breakpointManager.isEnabled()) {
			// no need to check breakpoints individually.
			return null;
		}
		return breakpointManager.getBreakpoints();
	}

	/**
	 * Returns an array of projects in their suggested build order
	 * containing all of the projects specified by <code>baseProjects</code>
	 * and all of their referenced projects.
	 *
	 * @param baseProjects a collection of projects
	 * @return an array of projects in their suggested build order
	 * containing all of the projects specified by <code>baseProjects</code>
	 * @throws CoreException if an error occurs while computing referenced
	 *  projects
	 */
	protected IProject[] computeReferencedBuildOrder(IProject[] baseProjects) throws CoreException {
		HashSet<IProject> unorderedProjects = new HashSet<>();
		for (IProject baseProject : baseProjects) {
			unorderedProjects.add(baseProject);
			addReferencedProjects(baseProject, unorderedProjects);
		}
		IProject[] projectSet = unorderedProjects.toArray(new IProject[unorderedProjects.size()]);
		return computeBuildOrder(projectSet);
	}


	/**
	 * Adds all projects referenced by <code>project</code> to the given
	 * set.
	 *
	 * @param project project
	 * @param references set to which referenced projects are added
	 * @throws CoreException if an error occurs while computing referenced
	 *  projects
	 */
	protected void addReferencedProjects(IProject project, Set<IProject> references) throws CoreException {
		if (project.isOpen()) {
			for (IProject refProject : project.getReferencedProjects()) {
				if (refProject.exists() && !references.contains(refProject)) {
					references.add(refProject);
					addReferencedProjects(refProject, references);
				}
			}
		}
	}

	/**
	 * Returns a list of projects in their suggested build order from the
	 * given unordered list of projects.
	 *
	 * @param projects the list of projects to sort into build order
	 * @return a new array containing all projects from <code>projects</code> sorted
	 *   according to their build order.
	 */
	protected IProject[] computeBuildOrder(IProject[] projects) {
		String[] orderedNames = ResourcesPlugin.getWorkspace().getDescription().getBuildOrder();
		if (orderedNames != null) {
			List<IProject> orderedProjects = new ArrayList<>(projects.length);
			//Projects may not be in the build order but should be built if selected
			List<IProject> unorderedProjects = new ArrayList<>(projects.length);
			Collections.addAll(unorderedProjects, projects);

			for (String projectName : orderedNames) {
				for (Iterator<IProject> iterator = unorderedProjects.iterator(); iterator.hasNext();) {
					IProject project = iterator.next();
					if (project.getName().equals(projectName)) {
						orderedProjects.add(project);
						iterator.remove();
						break;
					}
				}
			}
			//Add anything not specified before we return
			orderedProjects.addAll(unorderedProjects);
			return orderedProjects.toArray(new IProject[orderedProjects.size()]);
		}

		// Computing build order returned null, try the project prerequisite order
		IWorkspace.ProjectOrder po = ResourcesPlugin.getWorkspace().computeProjectOrder(projects);
		return po.projects;
	}

	/**
	 * Returns whether the given project contains any problem markers of the
	 * specified severity.
	 *
	 * @param proj the project to search
	 * @return whether the given project contains any problems that should
	 *  stop it from launching
	 * @throws CoreException if an error occurs while searching for
	 *  problem markers
	 */
	protected boolean existsProblems(IProject proj) throws CoreException {
		IMarker[] markers = proj.findMarkers(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE);
		if (markers.length > 0) {
			for (IMarker marker : markers) {
				if (isLaunchProblem(marker)) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Returns whether the given problem should potentially abort the launch.
	 * By default if the problem has an error severity, the problem is considered
	 * a potential launch problem. Subclasses may override to specialize error
	 * detection.
	 *
	 * @param problemMarker candidate problem
	 * @return whether the given problem should potentially abort the launch
	 * @throws CoreException if any exceptions occur while accessing marker attributes
	 */
	protected boolean isLaunchProblem(IMarker problemMarker) throws CoreException {
		try {
			Integer severity = (Integer) problemMarker.getAttribute(IMarker.SEVERITY);
			if (severity != null) {
				return severity.intValue() >= IMarker.SEVERITY_ERROR;
			}
		} catch (CoreException e) {
			// Ignore if the marker no longer exists, see:
			// https://github.com/eclipse-platform/eclipse.platform.debug/issues/136
			if (problemMarker.exists()) {
				IStatus status = new Status(IStatus.ERROR, DebugPlugin.getUniqueIdentifier(), DebugCoreMessages.ErrorAccessingProblemMarker, e);
				throw new CoreException(status);
			}
		}

		return false;
	}

	/**
	 * Performs an incremental build on each of the given projects.
	 *
	 * @param projects projects to build
	 * @param monitor progress monitor
	 * @throws CoreException if an exception occurs while building
	 */
	protected void buildProjects(final IProject[] projects, IProgressMonitor monitor) throws CoreException {
		IWorkspaceRunnable build = pm -> {
			SubMonitor localmonitor = SubMonitor.convert(pm, DebugCoreMessages.LaunchConfigurationDelegate_scoped_incremental_build, projects.length);
			try {
				for (IProject project : projects) {
					if (localmonitor.isCanceled()) {
						throw new OperationCanceledException();
					}
					project.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, localmonitor.newChild(1));
				}
			} finally {
				localmonitor.done();
			}
		};
		ResourcesPlugin.getWorkspace().run(build, monitor);
	}
}
