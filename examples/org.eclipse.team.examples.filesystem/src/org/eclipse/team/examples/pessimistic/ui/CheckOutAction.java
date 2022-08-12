/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
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
package org.eclipse.team.examples.pessimistic.ui;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.examples.pessimistic.PessimisticFilesystemProvider;

/**
 * Performs a check out on the selected resources.  If a folder is
 * selected all of its children are recursively checked out.
 */
public class CheckOutAction extends SourceManagementAction {
	/**
	 * Answers <code>true</code> if and only if the <code>resource</code>
	 * is not <code>null</code>, controlled, not ignored and not checked out.
	 *
	 * @see PessimisticProviderAction#shouldEnableFor(IResource)
	 */
	@Override
	protected boolean shouldEnableFor(IResource resource) {
		if (resource == null)
			return false;
		PessimisticFilesystemProvider provider= getProvider(resource);
		if (provider == null)
			return false;
		if (!provider.isControlled(resource))
			return false;
		if (provider.isIgnored(resource))
			return false;
		return !provider.isCheckedout(resource);
	}

	@Override
	protected void manageResources(PessimisticFilesystemProvider provider, IResource[] resources, IProgressMonitor monitor) {
		provider.checkout(resources, monitor);
	}
}
