/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
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
 *     James Blackburn (Broadcom Corp.) - ongoing development
 *******************************************************************************/
package org.eclipse.core.internal.localstore;

import org.eclipse.core.internal.resources.Resource;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * Visits a unified tree, and throws a ResourceChangedException on the first
 * node that is discovered to be out of sync.  The exception that is thrown
 * will not have any meaningful status, message, or stack trace. However it
 * does contain the target resource which can be used to bring the Resource
 * back into sync.
 */
public class IsSynchronizedVisitor extends CollectSyncStatusVisitor {
	static class ResourceChangedException extends RuntimeException {
		private static final long serialVersionUID = 1L;
		public final IResource target;

		public ResourceChangedException(IResource target) {
			this.target = target;
		}
	}

	/**
	 * Creates a new IsSynchronizedVisitor.
	 */
	public IsSynchronizedVisitor(IProgressMonitor monitor) {
		super("", monitor); //$NON-NLS-1$
	}

	/**
	 * @see CollectSyncStatusVisitor#changed(Resource)
	 */
	@Override
	protected void changed(Resource target) {
		throw new ResourceChangedException(target);
	}

	@Override
	protected void fileToFolder(UnifiedTreeNode node, Resource target) {
		changed((Resource) workspace.getRoot().getFolder(target.getFullPath()));
	}

	@Override
	protected void folderToFile(UnifiedTreeNode node, Resource target) {
		// Pass correct gender to changed for notification and async-refresh
		changed((Resource) workspace.getRoot().getFile(target.getFullPath()));
	}
}
