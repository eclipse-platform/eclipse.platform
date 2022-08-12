/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
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
package org.eclipse.team.tests.core.mapping;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.subscribers.Subscriber;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.core.variants.IResourceVariant;
import org.eclipse.team.core.variants.IResourceVariantComparator;

public class ScopeTestSubscriber extends Subscriber {

	@Override
	public String getName() {
		return "Scope Tests";
	}

	@Override
	public IResourceVariantComparator getResourceComparator() {
		return new IResourceVariantComparator() {
			@Override
			public boolean isThreeWay() {
				return false;
			}
			@Override
			public boolean compare(IResourceVariant base, IResourceVariant remote) {
				return false;
			}
			@Override
			public boolean compare(IResource local, IResourceVariant remote) {
				return false;
			}

		};
	}

	@Override
	public SyncInfo getSyncInfo(IResource resource) throws TeamException {
		return null;
	}

	@Override
	public boolean isSupervised(IResource resource) throws TeamException {
		return false;
	}

	@Override
	public IResource[] members(IResource resource) throws TeamException {
		if (resource instanceof IContainer) {
			IContainer c = (IContainer) resource;
			try {
				return c.members();
			} catch (CoreException e) {
				throw TeamException.asTeamException(e);
			}
		}
		return  new IResource[0];
	}

	@Override
	public void refresh(IResource[] resources, int depth,
			IProgressMonitor monitor) throws TeamException {
		// Nothing to do
	}

	@Override
	public IResource[] roots() {
		return ResourcesPlugin.getWorkspace().getRoot().getProjects();
	}

}
