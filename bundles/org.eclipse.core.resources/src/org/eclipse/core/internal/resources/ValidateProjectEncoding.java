/*******************************************************************************
 * Copyright (c) 2022 Simeon Andreev and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Simeon Andreev - initial API and implementation
 *     Christoph Läubrich - Issue #80 - CharsetManager access the ResourcesPlugin.getWorkspace before init
 *******************************************************************************/
package org.eclipse.core.internal.resources;

import java.util.*;
import org.eclipse.core.internal.utils.Messages;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.osgi.util.NLS;

/**
 * Reports warning markers on projects without an explicit encoding setting.
 */
public class ValidateProjectEncoding extends InternalWorkspaceJob {

	public static final String MARKER_ID = "noExplicitEncoding"; //$NON-NLS-1$

	public static final String MARKER_TYPE = ResourcesPlugin.getPlugin().getBundle().getSymbolicName() + "." //$NON-NLS-1$
			+ MARKER_ID;

	public static void scheduleWorkspaceValidation(Workspace workspace) {
		IProject[] projects = workspace.getRoot().getProjects();
		ValidateProjectEncoding validateProjectEncoding = new ValidateProjectEncoding(workspace, projects);
		validateProjectEncoding.setRule(workspace.getRoot());
		validateProjectEncoding.schedule();
	}

	public static void scheduleProjectValidation(Workspace workspace, IProject project) {
		// schedule a job only if marker state would change
		boolean shouldScheduleValidation = shouldScheduleValidation(project);
		if (shouldScheduleValidation) {
			ValidateProjectEncoding validateProjectEncoding = new ValidateProjectEncoding(workspace, project);
			validateProjectEncoding.setRule(project);
			validateProjectEncoding.schedule();
		}
	}

	private final IProject[] projects;

	private ValidateProjectEncoding(Workspace workspace, IProject... projects) {
		super(Messages.resources_checkExplicitEncoding_jobName, workspace);
		setSystem(true);
		this.projects = projects;
	}

	@Override
	public boolean belongsTo(Object family) {
		return family == ValidateProjectEncoding.class;
	}

	@Override
	public IStatus runInWorkspace(IProgressMonitor monitor) {
		if (monitor.isCanceled()) {
			return Status.CANCEL_STATUS;
		}
		SubMonitor subMonitor = SubMonitor.convert(monitor, projects.length);
		for (IProject project : projects) {
			subMonitor.checkCanceled();
			subMonitor.setTaskName(NLS.bind(Messages.resources_checkExplicitEncoding_taskName, project.getName()));
			updateMissingEncodingMarker(project);
			subMonitor.worked(1);
		}
		return Status.OK_STATUS;
	}

	/**
	 * Must be called from a workspace job
	 *
	 * @param project non null
	 */
	static void updateMissingEncodingMarker(IProject project) {
		try {
			if (project.isAccessible() && !project.isHidden()) {
				String defaultCharset = getDefaultCharset(project);
				if (defaultCharset == null) {
					createEncodingMarker(project);
				} else {
					deleteEncodingMarkers(project);
				}
			}
		} catch (CoreException e) {
			logException(e);
		}
	}

	private static boolean shouldScheduleValidation(IProject project) {
		boolean shouldScheduleValidation = true;
		try {
			if (project.isHidden()) {
				shouldScheduleValidation = false;
			} else if (project.isAccessible()) {
				String defaultCharset = getDefaultCharset(project);
				boolean hasDefaultEncoding = defaultCharset != null;
				IMarker[] encodingMarkers = getEncodingMarkers(project);
				boolean hasEncodingMarkers = encodingMarkers != null && encodingMarkers.length > 0;
				if (hasEncodingMarkers && !hasDefaultEncoding) {
					// don't validate again if the project already has a marker and has no encoding
					shouldScheduleValidation = false;
				} else if (!hasEncodingMarkers && hasDefaultEncoding) {
					// don't validate again if the project has no marker and has encoding
					shouldScheduleValidation = false;
				}
			}
		} catch (CoreException e) {
			logException(e);
		}
		return shouldScheduleValidation;
	}

	private static String getDefaultCharset(IProject project) throws CoreException {
		boolean checkImplicit = false;
		String defaultCharset = project.getDefaultCharset(checkImplicit);
		return defaultCharset;
	}

	private static void createEncodingMarker(IProject project) throws CoreException {
		String message = NLS.bind(Messages.resources_checkExplicitEncoding_problemText, project.getName());

		String[] attributeNames = { IMarker.MESSAGE, IMarker.SEVERITY, IMarker.LOCATION };
		Object[] attributevalues = { message, IMarker.SEVERITY_WARNING, project.getFullPath().toString() };

		IMarker[] existing = project.findMarkers(MARKER_TYPE, false, IResource.DEPTH_ONE);
		for (IMarker marker : existing) {
			Object[] markerValues = marker.getAttributes(attributeNames);
			if (Arrays.equals(attributevalues, markerValues)) {
				return;
			}
		}

		Map<String, Object> attributes = new HashMap<>();
		for (int i = 0; i < attributeNames.length; i++) {
		    attributes.put(attributeNames[i], attributevalues[i]);
		}
		project.createMarker(MARKER_TYPE, attributes);
	}

	private static void deleteEncodingMarkers(IProject project) throws CoreException {
		IMarker[] existing = getEncodingMarkers(project);
		for (IMarker marker : existing) {
			marker.delete();
		}
	}

	private static IMarker[] getEncodingMarkers(IProject project) throws CoreException {
		IMarker[] existing = project.findMarkers(MARKER_TYPE, false, IResource.DEPTH_ONE);
		return existing;
	}

	private static void logException(CoreException e) {
		boolean logException = true;
		if (e instanceof ResourceException) {
			int code = e.getStatus().getCode();
			if (code == IResourceStatus.RESOURCE_NOT_FOUND || code == IResourceStatus.PROJECT_NOT_OPEN) {
				logException = false;
			}
		}
		if (logException) {
			ResourcesPlugin.getPlugin().getLog().log(e.getStatus());
		}
	}

}
