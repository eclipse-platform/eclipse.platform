/*******************************************************************************
 * Copyright (c) 2006, 2017 IBM Corporation and others.
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
package org.eclipse.team.internal.core.mapping;

import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.team.core.history.IFileRevision;
import org.eclipse.team.core.history.provider.FileRevision;
import org.eclipse.team.core.variants.IResourceVariant;

public class ResourceVariantFileRevision extends FileRevision implements IAdaptable {
	private final IResourceVariant variant;

	public ResourceVariantFileRevision(IResourceVariant variant) {
		this.variant = variant;
	}

	@Override
	public IStorage getStorage(IProgressMonitor monitor) throws CoreException {
		return variant.getStorage(monitor);
	}

	@Override
	public String getName() {
		return variant.getName();
	}

	@Override
	public String getContentIdentifier() {
		return variant.getContentIdentifier();
	}

	public IResourceVariant getVariant() {
		return variant;
	}

	@Override
	public boolean isPropertyMissing() {
		return false;
	}

	@Override
	public IFileRevision withAllProperties(IProgressMonitor monitor) throws CoreException {
		return this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T getAdapter(Class<T> adapter) {
		if (adapter == IResourceVariant.class)
			return (T) variant;
		Object object = Platform.getAdapterManager().getAdapter(this, adapter);
		if (object != null)
			return (T) object;
		if (variant instanceof IAdaptable ) {
			IAdaptable  adaptable = (IAdaptable ) variant;
			return adaptable.getAdapter(adapter);
		}
		return null;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof ResourceVariantFileRevision) {
			ResourceVariantFileRevision fileRevision = (ResourceVariantFileRevision) obj;
			return fileRevision.getVariant().equals(getVariant());
		}
		return false;
	}

	@Override
	public int hashCode() {
		return getVariant().hashCode();
	}
}