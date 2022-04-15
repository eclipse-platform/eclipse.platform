/*******************************************************************************
 * Copyright (c) 2016-2022 Red Hat Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Mickael Istria (Red Hat Inc.) - initial API and implementation
 *     Christoph Läubrich - Issue #52 - Make ResourcesPlugin more dynamic and better handling early start-up
 *******************************************************************************/
package org.eclipse.core.internal.resources;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.eclipse.core.internal.utils.FileUtil;
import org.eclipse.core.internal.utils.Messages;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.IPreferenceChangeListener;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.PreferenceChangeEvent;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.osgi.util.NLS;

public class CheckMissingNaturesListener implements IResourceChangeListener, IPreferenceChangeListener {

	public static final String MARKER_TYPE = ResourcesPlugin.getPlugin().getBundle().getSymbolicName() + ".unknownNature"; //$NON-NLS-1$
	public static final String NATURE_ID_ATTRIBUTE = "natureId"; //$NON-NLS-1$
	private final Workspace workspace;

	public CheckMissingNaturesListener(Workspace workspace) {
		this.workspace = workspace;
	}

	@Override
	public void resourceChanged(IResourceChangeEvent event) {
		if (event.getDelta() == null) {
			return;
		}
		try {
			final Set<IProject> modifiedProjects = new HashSet<>();
			event.getDelta().accept(delta -> {
				if (delta.getResource() != null && delta.getResource().getType() == IResource.PROJECT && (delta.getKind() == IResourceDelta.ADDED || delta.getKind() == IResourceDelta.CHANGED)) {
					modifiedProjects.add((IProject) delta.getResource());
				}
				return delta.getResource() == null || delta.getResource().getType() == IResource.ROOT;
			});
			updateMarkers(modifiedProjects);
		} catch (CoreException e) {
			ResourcesPlugin.getPlugin().getLog().log(new Status(IStatus.ERROR, ResourcesPlugin.getPlugin().getBundle().getSymbolicName(), e.getMessage(), e));
		}
	}

	int getMissingNatureSeverity(final IProject project) {
		int severity = PreferenceInitializer.PREF_MISSING_NATURE_MARKER_SEVERITY_DEFAULT;
		IEclipsePreferences node = InstanceScope.INSTANCE.getNode(ResourcesPlugin.PI_RESOURCES);
		if (node != null) {
			severity = node.getInt(ResourcesPlugin.PREF_MISSING_NATURE_MARKER_SEVERITY, PreferenceInitializer.PREF_MISSING_NATURE_MARKER_SEVERITY_DEFAULT);
		}
		return severity;
	}

	@Override
	public void preferenceChange(PreferenceChangeEvent event) {
		if (ResourcesPlugin.PREF_MISSING_NATURE_MARKER_SEVERITY.equals(event.getKey())) {
			final int newSeverity = event.getNewValue() != null ? Integer.parseInt((String) event.getNewValue()) : PreferenceInitializer.PREF_MISSING_NATURE_MARKER_SEVERITY_DEFAULT;
			final int oldSeverity = event.getOldValue() != null ? Integer.parseInt((String) event.getOldValue()) : PreferenceInitializer.PREF_MISSING_NATURE_MARKER_SEVERITY_DEFAULT;
			if (newSeverity < 0) {
				removeAllMarkers(workspace.getRoot());
			} else if (oldSeverity < 0 && newSeverity >= 0) {
				updateMarkers(Arrays.asList(workspace.getRoot().getProjects()));
			} else {
				updateExistingMarkersSeverity(workspace.getRoot(), newSeverity);
			}
		}
	}

	private void removeAllMarkers(IContainer workspaceRootOrProject) {
		final Collection<IMarker> markers = getRelatedMarkers(workspaceRootOrProject);
		if (markers.isEmpty()) {
			return;
		}
		WorkspaceJob job = new WorkspaceJob(Messages.updateUnknownNatureMarkers) {
			@Override
			public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
				for (IMarker marker : markers) {
					if (marker.exists() && marker.getResource().isAccessible()) {
						marker.delete();
					}
				}
				return Status.OK_STATUS;
			}

			@Override
			public boolean belongsTo(Object family) {
				return super.belongsTo(family) || MARKER_TYPE.equals(family);
			}
		};
		job.setUser(false);
		job.setSystem(true);
		job.setPriority(Job.DECORATE);
		job.setRule(workspaceRootOrProject);
		job.schedule();
	}

	private void updateExistingMarkersSeverity(IContainer workspaceRootOrProject, int newSeverity) {
		final Collection<IMarker> markers = getRelatedMarkers(workspaceRootOrProject);
		if (markers.isEmpty()) {
			return;
		}
		WorkspaceJob job = new WorkspaceJob(Messages.updateUnknownNatureMarkers) {
			@Override
			public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
				for (IMarker marker : markers) {
					if (marker.exists() && marker.getResource().isAccessible()) {
						marker.setAttribute(IMarker.SEVERITY, newSeverity);
					}
				}
				return Status.OK_STATUS;
			}

			@Override
			public boolean belongsTo(Object family) {
				return super.belongsTo(family) || MARKER_TYPE.equals(family);
			}
		};
		job.setUser(false);
		job.setSystem(true);
		job.setPriority(Job.DECORATE);
		job.setRule(workspaceRootOrProject);
		job.schedule();
	}

	private void updateMarkers(Collection<IProject> projects) {
		for (IProject project : projects) {
			if (!project.isAccessible()) {
				continue;
			}
			int severity = getMissingNatureSeverity(project);
			try {
				if (severity < 0) {
					removeAllMarkers(project);
					continue;
				}

				final Set<String> missingNatures = new HashSet<>();
				for (String natureId : project.getDescription().getNatureIds()) {
					if (project.getWorkspace().getNatureDescriptor(natureId) == null) {
						missingNatures.add(natureId);
					}
				}

				final Set<IMarker> toRemove = new HashSet<>();
				for (IMarker existingMarker : getRelatedProjectMarkers(project)) {
					String markerNature = existingMarker.getAttribute(NATURE_ID_ATTRIBUTE, ""); //$NON-NLS-1$
					if (!missingNatures.contains(markerNature)) {
						toRemove.add(existingMarker);
					} else {
						// no need to create a new marker
						missingNatures.remove(markerNature);
					}
				}

				if (!toRemove.isEmpty() || !missingNatures.isEmpty()) {
					WorkspaceJob workspaceJob = new WorkspaceJob(Messages.updateUnknownNatureMarkers) {
						@Override
						public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
							for (IMarker marker : toRemove) {
								marker.delete();
							}
							IResource targetResource = project.getFile(IProjectDescription.DESCRIPTION_FILE_NAME);
							if (!targetResource.isAccessible()) {
								targetResource = project;
							}
							for (String natureId : missingNatures) {

								Map<String, Object> attributes = new HashMap<>();
								attributes.put(IMarker.SEVERITY, getMissingNatureSeverity(project));
								attributes.put(IMarker.MESSAGE, NLS.bind(Messages.natures_missingNature, natureId));
								attributes.put(NATURE_ID_ATTRIBUTE, natureId);
								IMarker marker = targetResource.createMarker(MARKER_TYPE, attributes);
								if (targetResource.getType() == IResource.FILE) {
									updateRange(marker, natureId, (IFile) targetResource);
								}
							}
							return Status.OK_STATUS;
						}

						@Override
						public boolean belongsTo(Object family) {
							return super.belongsTo(family) || MARKER_TYPE.equals(family);
						}
					};
					workspaceJob.setRule(project);
					workspaceJob.setUser(false);
					workspaceJob.setSystem(true);
					workspaceJob.setPriority(Job.DECORATE);
					workspaceJob.schedule();
				}
			} catch (CoreException e) {
				ResourcesPlugin.getPlugin().getLog().log(new Status(IStatus.ERROR, ResourcesPlugin.PI_RESOURCES, e.getMessage(), e));
			}
		}
	}

	protected void updateRange(IMarker marker, String natureId, IFile file) {
		if (!file.isAccessible()) {
			return;
		}
		Pattern pattern = Pattern.compile(".*<" + IModelObjectConstants.NATURE + ">\\s*(" + natureId.replace(".", "\\.") + ")\\s*</" + IModelObjectConstants.NATURE + ">.*", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
				Pattern.DOTALL);
		try (
			InputStream input = file.getContents();
			ByteArrayOutputStream output = new ByteArrayOutputStream();
		) {
			IPath path = file.getLocation();
			if (path == null) {
				path = file.getFullPath();
			}
			FileUtil.transferStreams(input, output, path.toString(), new NullProgressMonitor());
			String content = output.toString();
			Matcher matcher = pattern.matcher(content);
			if (matcher.matches() && matcher.groupCount() > 0) {
				marker.setAttribute(IMarker.CHAR_START, matcher.start(1));
				marker.setAttribute(IMarker.CHAR_END, matcher.end(1));
			}
		} catch (IOException | CoreException e) {
			ResourcesPlugin.getPlugin().getLog().log(new Status(IStatus.ERROR, ResourcesPlugin.PI_RESOURCES, e.getMessage(), e));
		}
	}

	protected Collection<IMarker> getRelatedMarkers(IContainer rootOrProject) {
		switch (rootOrProject.getType()) {
			case IResource.ROOT :
				return getRelatedRootMarkers((IWorkspaceRoot) rootOrProject);
			case IResource.PROJECT :
				return getRelatedProjectMarkers((IProject) rootOrProject);
		}
		return Collections.emptyList();
	}

	protected Collection<IMarker> getRelatedRootMarkers(IWorkspaceRoot root) {
		if (!root.isAccessible()) {
			return Collections.emptyList();
		}
		Set<IMarker> res = new HashSet<>();
		for (IProject project : root.getProjects()) {
			res.addAll(getRelatedProjectMarkers(project));
		}
		return res;
	}

	protected Collection<IMarker> getRelatedProjectMarkers(IProject project) {
		if (!project.isAccessible()) {
			return Collections.emptyList();
		}
		try {
			return Arrays.asList(project.findMarkers(MARKER_TYPE, true, IResource.DEPTH_ONE));
		} catch (CoreException e) {
			ResourcesPlugin.getPlugin().getLog().log(new Status(IStatus.ERROR, ResourcesPlugin.PI_RESOURCES, e.getMessage(), e));
			return Collections.emptyList();
		}
	}

}
