/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
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

import org.eclipse.compare.ITypedElement;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.variants.IResourceVariant;
import org.eclipse.team.internal.core.mapping.ResourceVariantFileRevision;
import org.eclipse.team.internal.ui.StorageTypedElement;
import org.eclipse.team.internal.ui.history.FileRevisionEditorInput;
import org.eclipse.ui.IEditorInput;

/**
 * An {@link ITypedElement} wrapper for {@link IResourceVariant} for use with the
 * Compare framework.
 */
public class RemoteResourceTypedElement extends StorageTypedElement {

	private IResourceVariant remote;

	/**
	 * Creates a new content buffer for the given team node.
	 * @param remote the resource variant
	 * @param encoding the  encoding of the contents
	 */
	public RemoteResourceTypedElement(IResourceVariant remote, String encoding) {
		super(encoding);
		Assert.isNotNull(remote);
		this.remote = remote;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.compare.ITypedElement#getName()
	 */
	@Override
	public String getName() {
		return remote.getName();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ui.StorageTypedElement#getType()
	 */
	@Override
	public String getType() {
		if (remote.isContainer()) {
			return ITypedElement.FOLDER_TYPE;
		}
		return super.getType();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ui.StorageTypedElement#fetchContents(org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	protected IStorage fetchContents(IProgressMonitor monitor) throws TeamException {
		return remote.getStorage(monitor);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.compare.ISharedDocumentAdapter#getDocumentKey(java.lang.Object)
	 */
	@Override
	public IEditorInput getDocumentKey(Object element) {
		if (element == this && getBufferedStorage() != null) {
			return new FileRevisionEditorInput(new ResourceVariantFileRevision(remote), getBufferedStorage(), getLocalEncoding());
		}
		return null;
	}

}
