/*******************************************************************************
 * Copyright (c) 2000, 2018 IBM Corporation and others.
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
package org.eclipse.team.internal.ccvs.ui.model;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.ui.*;
import org.eclipse.ui.model.IWorkbenchAdapter;
import org.eclipse.ui.progress.IDeferredWorkbenchAdapter;
import org.eclipse.ui.progress.IElementCollector;

public class VersionCategory extends CVSModelElement implements IAdaptable,
		IDeferredWorkbenchAdapter {
	private ICVSRepositoryLocation repository;

	/**
	 * ProjectVersionsCategory constructor.
	 */
	public VersionCategory(ICVSRepositoryLocation repo) {
		super();
		this.repository = repo;
	}

	/**
	 * Returns an object which is an instance of the given class associated with
	 * this object. Returns <code>null</code> if no such object can be found.
	 */
	public <T> T getAdapter(Class<T> adapter) {
		if (adapter == IWorkbenchAdapter.class)
			return adapter.cast(this);
		if (adapter == IDeferredWorkbenchAdapter.class)
			return adapter.cast(this);
		return null;
	}

	/**
	 * Returns the children of this object. When this object is displayed in a
	 * tree, the returned objects will be this element's children. Returns an
	 * empty enumeration if this object has no children.
	 */
	public Object[] fetchChildren(Object o, IProgressMonitor monitor)
			throws TeamException {
		if (CVSUIPlugin.getPlugin().getRepositoryManager()
				.isDisplayingProjectVersions(repository)) {
			return getProjectVersionChildren(o, monitor);
		} else {
			return getVersionTagChildren(o, monitor);
		}
	}

	/*
	 * Return the children as a list of versions whose children are projects
	 */
	private Object[] getVersionTagChildren(Object o, IProgressMonitor monitor)
			throws CVSException {
		CVSTag[] tags = CVSUIPlugin.getPlugin().getRepositoryManager()
				.getKnownTags(repository, getWorkingSet(), CVSTag.VERSION, monitor);
		CVSTagElement[] versionElements = new CVSTagElement[tags.length];
		for (int i = 0; i < tags.length; i++) {
			versionElements[i] = new CVSTagElement(tags[i], repository);
		}
		return versionElements;
	}

	/*
	 * Return the children as a list of projects whose children ar project
	 * versions
	 */
	private Object[] getProjectVersionChildren(Object o,
			IProgressMonitor monitor) throws TeamException {

		ICVSRemoteResource[] resources = CVSUIPlugin.getPlugin()
				.getRepositoryManager().getFoldersForTag(repository,
						CVSTag.DEFAULT, monitor);
		if (getWorkingSet() != null)
			resources = CVSUIPlugin.getPlugin().getRepositoryManager()
					.filterResources(getWorkingSet(), resources);
		Object[] modules = new Object[resources.length];
		for (int i = 0; i < resources.length; i++) {
			modules[i] = new RemoteModule((ICVSRemoteFolder) resources[i],
					VersionCategory.this);
		}
		return modules;
	}

	/**
	 * Returns an image descriptor to be used for displaying an object in the
	 * workbench. Returns null if there is no appropriate image.
	 * 
	 * @param object The object to get an image descriptor for.
	 */
	public ImageDescriptor getImageDescriptor(Object object) {
		return CVSUIPlugin.getPlugin().getImageDescriptor(
				ICVSUIConstants.IMG_VERSIONS_CATEGORY);
	}

	/**
	 * Returns the name of this element. This will typically be used to assign a
	 * label to this object when displayed in the UI. Returns an empty string if
	 * there is no appropriate name for this object.
	 * 
	 * @param object The object to get a label for.
	 */
	public String getLabel(Object o) {
		return CVSUIMessages.VersionCategory_Versions_1; 
	}

	/**
	 * Returns the logical parent of the given object in its tree. Returns null
	 * if there is no parent, or if this object doesn't belong to a tree.
	 * 
	 * @param object The object to get the parent for.
	 */
	public Object getParent(Object o) {
		return repository;
	}

	/**
	 * Return the repository the given element belongs to.
	 */
	public ICVSRepositoryLocation getRepository(Object o) {
		return repository;
	}

	public void fetchDeferredChildren(Object o, IElementCollector collector,
			IProgressMonitor monitor) {
		try {
			collector.add(fetchChildren(o, monitor), monitor);
		} catch (TeamException e) {
			handle(collector, e);
		}
	}

	public boolean isContainer() {
		return true;
	}

	public ISchedulingRule getRule(Object element) {
		return new RepositoryLocationSchedulingRule(repository); 
	}
}
