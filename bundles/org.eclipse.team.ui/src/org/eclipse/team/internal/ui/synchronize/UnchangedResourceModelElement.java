/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
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

	@Override
	public String getName() {
		return resource.getName();
	}
}
