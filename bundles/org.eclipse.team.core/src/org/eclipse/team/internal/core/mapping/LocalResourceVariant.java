/*******************************************************************************
 * Copyright (c) 2000, 2017 IBM Corporation and others.
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
package org.eclipse.team.internal.core.mapping;

import java.util.Date;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.variants.IResourceVariant;

public class LocalResourceVariant implements IResourceVariant {
	private final IResource resource;

	public LocalResourceVariant(IResource resource) {
		this.resource = resource;
	}

	@Override
	public byte[] asBytes() {
		return getContentIdentifier().getBytes();
	}

	@Override
	public String getContentIdentifier() {
		return new Date(resource.getLocalTimeStamp()).toString();
	}

	@Override
	public IStorage getStorage(IProgressMonitor monitor) throws TeamException {
		if (resource.getType() == IResource.FILE) {
			return (IFile)resource;
		}
		return null;
	}

	@Override
	public boolean isContainer() {
		return resource.getType() != IResource.FILE;
	}

	@Override
	public String getName() {
		return resource.getName();
	}
}