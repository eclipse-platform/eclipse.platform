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
import org.eclipse.team.internal.ccvs.ui.CVSUIPlugin;
import org.eclipse.team.internal.ccvs.ui.ICVSUIConstants;
import org.eclipse.team.internal.ccvs.ui.repo.RepositoryManager;
import org.eclipse.ui.model.IWorkbenchAdapter;
import org.eclipse.ui.progress.IDeferredWorkbenchAdapter;
import org.eclipse.ui.progress.IElementCollector;

/**
 * This class represents an IProject resource in a repository. The children of a
 * RemoteModule are its versions. A RemoteModule is a child of the
 * VersionsCategory.
 */
public class RemoteModule extends CVSModelElement implements IAdaptable,
		IDeferredWorkbenchAdapter {
	ICVSRemoteFolder folder;

	VersionCategory parent;

	/**
	 * RemoteProject constructor.
	 */
	public RemoteModule(ICVSRemoteFolder folder, VersionCategory parent) {
		this.folder = folder;
		this.parent = parent;
	}

	/**
	 * Returns an object which is an instance of the given class associated with
	 * this object. Returns <code>null</code> if no such object can be found.
	 */
	@Override
	public <T> T getAdapter(Class<T> adapter) {
		if (adapter == IWorkbenchAdapter.class)
			return adapter.cast(this);
		if (adapter == IDeferredWorkbenchAdapter.class)
			return adapter.cast(this);
		return null;
	}

	/**
	 * Returns an image to be used for displaying an object in the desktop.
	 * 
	 * @param object The object to get an image for.
	 * @param owner The viewer that the image will be used in. The image will be
	 *            disposed when this viewer is closed. If the owner is null, a
	 *            new image is returned, and the caller is responsible for
	 *            disposing it.
	 */
	@Override
	public ImageDescriptor getImageDescriptor(Object object) {
		return CVSUIPlugin.getPlugin().getImageDescriptor(
				ICVSUIConstants.IMG_PROJECT_VERSION);
	}

	/**
	 * Returns the name of this element. This will typically be used to assign a
	 * label to this object when displayed in the UI.
	 */
	@Override
	public String getLabel(Object o) {
		return folder.getName();
	}

	/**
	 * Returns the logical parent of the given object in its tree.
	 */
	@Override
	public Object getParent(Object o) {
		return parent;
	}

	/**
	 * Return the repository the given element belongs to.
	 */
	public ICVSRepositoryLocation getRepository(Object o) {
		return folder.getRepository();
	}

	/*
	 * For debugging purposes only.
	 */
	@Override
	public String toString() {
		return "RemoteModule(" + folder.getName() + ")"; //$NON-NLS-1$ //$NON-NLS-2$
	}

	public ICVSRemoteResource getCVSResource() {
		return folder;
	}

	/**
	 * Returns the children of this object. When this object is displayed in a
	 * tree, the returned objects will be this element's children. Returns an
	 * empty enumeration if this object has no children. The children of the
	 * RemoteModule are the versions for that module.
	 * 
	 * @see org.eclipse.team.internal.ccvs.ui.model.CVSModelElement#internalGetChildren(java.lang.Object,
	 *      org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public Object[] fetchChildren(Object o, IProgressMonitor monitor)
			throws TeamException {
		RepositoryManager manager = CVSUIPlugin.getPlugin()
				.getRepositoryManager();
		try {
			manager.refreshDefinedTags(folder, false /* recurse */, false /* notify */, monitor);
		} catch (TeamException e) {
			// continue
		}
		CVSTag[] tags = CVSUIPlugin.getPlugin().getRepositoryManager()
				.getKnownTags(folder, CVSTag.VERSION);
		Object[] versions = new Object[tags.length];
		for (int i = 0; i < versions.length; i++) {
			versions[i] = folder.forTag(tags[i]);
		}
		return versions;
	}

	@Override
	public void fetchDeferredChildren(Object o, IElementCollector collector,
			IProgressMonitor monitor) {
		try {
			collector.add(fetchChildren(o, monitor), monitor);
		} catch (TeamException e) {
			handle(collector, e);
		}
	}

	@Override
	public boolean isContainer() {
		return true;
	}

	@Override
	public ISchedulingRule getRule(Object element) {
		return new RepositoryLocationSchedulingRule(folder.getRepository()); 
	}
}
