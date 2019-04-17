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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.ui.model.IWorkbenchAdapter;

/**
 * This class provides common behavior between the branch and date tag categories
 */
public abstract class TagCategory extends CVSModelElement {
	protected ICVSRepositoryLocation repository;
	
	public TagCategory(ICVSRepositoryLocation repository) {
		this.repository = repository;
	}
	
	@Override
	public Object[] fetchChildren(Object o, IProgressMonitor monitor) throws CVSException {
		CVSTag[] tags = getTags(monitor);
		CVSTagElement[] elements = new CVSTagElement[tags.length];
		for (int i = 0; i < tags.length; i++) {
			elements[i] = new CVSTagElement(tags[i], repository);
		}
		return elements;
	}

	/**
	 * Return the tags that are to be displyed as children of this category
	 * @param monitor
	 * @return
	 */
	protected abstract CVSTag[] getTags(IProgressMonitor monitor) throws CVSException;

	@Override
	public Object getParent(Object o) {
		return repository;
	}
	
	/**
	 * Return the repository the given element belongs to.
	 */
	public ICVSRepositoryLocation getRepository(Object o) {
		return repository;
	}

	/**
	 * Returns an object which is an instance of the given class
	 * associated with this object. Returns <code>null</code> if
	 * no such object can be found.
	 */
	@Override
	public <T> T getAdapter(Class<T> adapter) {
		if (adapter == IWorkbenchAdapter.class) return adapter.cast(this);
		return null;
	}

}
