/*******************************************************************************
 * Copyright (c) 2005, 2017 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.core.mapping;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.RepositoryProviderType;
import org.eclipse.team.internal.core.IRepositoryProviderListener;
import org.eclipse.team.internal.core.RepositoryProviderManager;
import org.eclipse.team.internal.core.TeamPlugin;

/**
 * Supports the tracking of related changes for the purpose of grouping then using an {@link IChangeGroupingRequestor}.
 * <p>
 * Clients may subclass this class.
 * </p>
 * @see IChangeGroupingRequestor
 * @since 3.3
 */
public abstract class ChangeTracker {

	private Map<IProject, IChangeGroupingRequestor> trackedProjects = new HashMap<>(); // Map IProject->IChangeGroupingRequestor
	private boolean disposed;
	private ChangeListener changeListener = new ChangeListener();

	private class ChangeListener implements IResourceChangeListener, IRepositoryProviderListener {
		/**
		 * Handle a resource change event.
		 * Update the set of projects for which we can track changes
		 * by listening for project changes and project description changes.
		 * @param event the change event
		 */
		@Override
		public void resourceChanged(IResourceChangeEvent event) {
			if (disposed) return;
			IResourceDelta delta = event.getDelta();
			IResourceDelta[] projectDeltas = delta.getAffectedChildren(IResourceDelta.ADDED | IResourceDelta.CHANGED | IResourceDelta.REMOVED);
			for (IResourceDelta projectDelta : projectDeltas) {
				IResource resource = projectDelta.getResource();
				if (resource.getType() == IResource.PROJECT) {
					IProject project = (IProject)resource;
					if (isProjectOfInterest(project)) {
						if (isProjectTracked(project)) {
							IResource[] resources = getProjectChanges(project, projectDelta);
							if (resources.length > 0)
								handleChanges(project, resources);
						} else {
							trackProject(project);
						}
					} else {
						stopTrackingProject(project);
					}
				}
			}
		}

		/**
		 * When a project is shared, start tracking it if it is of interest.
		 * @param provider the repository provider
		 */
		@Override
		public void providerMapped(RepositoryProvider provider) {
			if (disposed) return;
			if (isProjectOfInterest(provider.getProject())) {
				trackProject(provider.getProject());
			}
		}

		/**
		 * When a project is no longer shared, stop tracking the project.
		 * @param project the project
		 */
		@Override
		public void providerUnmapped(IProject project) {
			if (disposed) return;
			stopTrackingProject(project);
		}
	}

	/**
	 * Create a change tracker
	 */
	public ChangeTracker() {
		super();
	}

	/**
	 * Start tracking changes. This registers listeners with the workspace
	 * and team.
	 */
	public void start() {
		ResourcesPlugin.getWorkspace().addResourceChangeListener(changeListener, IResourceChangeEvent.POST_CHANGE);
		RepositoryProviderManager.getInstance().addListener(changeListener);
		IProject[] allProjects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		for (IProject project : allProjects) {
			if (isProjectOfInterest(project))
				trackProject(project);
		}
	}

	/**
	 * Remove any listeners for this tracker. Subclasses
	 * may extend this method but must call this method if they do.
	 */
	public void dispose() {
		disposed = true;
		ResourcesPlugin.getWorkspace().removeResourceChangeListener(changeListener);
		RepositoryProviderManager.getInstance().removeListener(changeListener);
	}

	private IResource[] getProjectChanges(IProject project, IResourceDelta projectDelta) {
		final List<IResource> result = new ArrayList<>();
		try {
			projectDelta.accept(delta -> {
				if (isResourceOfInterest(delta.getResource()) & isChangeOfInterest(delta)) {
					result.add(delta.getResource());
				}
				return true;
			});
		} catch (CoreException e) {
			TeamPlugin.log(e);
		}
		return result.toArray(new IResource[result.size()]);
	}

	/**
	 * Return whether the given delta represents a change of interest.
	 * @param delta the delta
	 * @return whether the given delta represents a change of interest
	 */
	protected boolean isChangeOfInterest(IResourceDelta delta) {
		return (delta.getKind() & (IResourceDelta.ADDED | IResourceDelta.REMOVED | IResourceDelta.CHANGED)) > 0;
	}

	/**
	 * Stop tracking changes for the given project. Subclasses
	 * may extend but must call this method.
	 * @param project the project
	 */
	protected void stopTrackingProject(IProject project) {
		trackedProjects.remove(project);
	}

	/**
	 * Return whether the given project is being tracked.
	 * @param project the project
	 * @return whether the given project is being tracked
	 */
	protected final boolean isProjectTracked(IProject project) {
		return trackedProjects.containsKey(project);
	}

	/**
	 * Return whether the given project is of interest to this
	 * tracker. By default, <code>true</code> is returned if the
	 * project is accessible. Subclasses may extend but should
	 * still check for project accessibility either by calling
	 * {@link IResource#isAccessible()} or by invoking the
	 * overridden method.
	 * @param project the project
	 * @return whether the given project is of interest to this
	 * tracker
	 */
	protected boolean isProjectOfInterest(IProject project) {
		return project.isAccessible();
	}

	/**
	 * Return whether the given resource is of interest to the tracker.
	 * @param resource the resource
	 * @return whether the given resource is of interest to the tracker
	 */
	protected abstract boolean isResourceOfInterest(IResource resource);

	/**
	 * The given resources of interest have changed in the given project.
	 * @param project the project
	 * @param resources the resources
	 */
	protected abstract void handleChanges(IProject project, IResource[] resources);

	/**
	 * Resources of interest in the given project have changed but the
	 * specific changes are not known. Implementors must search the project for
	 * changes of interest.
	 * @param project the project
	 */
	protected abstract void handleProjectChange(IProject project);

	/**
	 * Track the given project if it has a change set collector. If the project
	 * does not have a collector, the project is not tracked.
	 * @param project the project
	 * @return whether the project is being tracked
	 */
	protected final boolean trackProject(IProject project) {
		if (RepositoryProvider.isShared(project)) {
			try {
				String currentId = project.getPersistentProperty(TeamPlugin.PROVIDER_PROP_KEY);
				if (currentId != null) {
					RepositoryProviderType type = RepositoryProviderType.getProviderType(currentId);
					if (type != null) {
						IChangeGroupingRequestor collector = getCollector(type);
						if (collector != null) {
							trackedProjects.put(project, collector);
							// Ensure that an appropriate change set exists if needed
							// We can do this here because we know that the number of files
							// to test is small.
							projectTracked(project);
							return true;
						}
					}
				}
			} catch (CoreException e) {
				TeamPlugin.log(e);
			}
		}
		return false;
	}

	private IChangeGroupingRequestor getCollector(RepositoryProviderType type) {
		if (type != null) {
			IChangeGroupingRequestor o = type.getAdapter(IChangeGroupingRequestor.class);
			if (o != null) {
				return o;
			}
		}
		return null;
	}

	/**
	 * Callback made from {@link #trackProject(IProject)} when a project is tracked.
	 * By default, {@link #handleProjectChange(IProject)} is called by subclasses may override.
	 * @param project the project
	 */
	protected void projectTracked(IProject project) {
		handleProjectChange(project);
	}

	/**
	 * Group the given modified file into a change set with the given name.
	 * @param project the project
	 * @param name the unique name used to identify the change set
	 * @param files the change files to be grouped
	 * @throws CoreException if an error occurs
	 */
	protected void ensureGrouped(IProject project, String name, IFile[] files) throws CoreException {
		IChangeGroupingRequestor collector = getCollector(project);
		if (collector != null) {
			collector.ensureChangesGrouped(project, files, name);
		}
	}

	private IChangeGroupingRequestor getCollector(IProject project) {
		return trackedProjects.get(project);
	}

	/**
	 * Return whether the given file is modified with respect to the
	 * repository provider associated with the file's project.
	 * @param file the file
	 * @return whether the given file is modified
	 * @throws CoreException if an error occurs while trying to determine the modification state
	 * of the file
	 */
	protected boolean isModified(IFile file) throws CoreException {
		IChangeGroupingRequestor collector = getCollector(file.getProject());
		if (collector != null)
			return collector.isModified(file);
		return false;
	}

}
