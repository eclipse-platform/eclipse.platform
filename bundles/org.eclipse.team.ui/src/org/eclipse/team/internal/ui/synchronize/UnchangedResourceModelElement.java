/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ui.synchronize;

import org.eclipse.compare.structuremergeviewer.IDiffContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Assert;

/**
 * DiffNode that represents a resource that is in sync.
 */
public class UnchangedResourceModelElement extends SynchronizeModelElement {

	private final IResource resource;

	public UnchangedResourceModelElement(IDiffContainer parent, IResource resource) {
		super(parent);
		Assert.isNotNull(resource);
		this.resource = resource;
	}

	/**
	 * @return Returns the resource.
	 */
	@Override
	public IResource getResource() {
		return resource;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.compare.structuremergeviewer.DiffNode#getName()
	 */
	@Override
	public String getName() {
		return resource.getName();
	}
}
