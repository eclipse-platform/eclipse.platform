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
package org.eclipse.team.core.mapping.provider;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Assert;
import org.eclipse.team.core.diff.provider.TwoWayDiff;
import org.eclipse.team.core.history.IFileRevision;
import org.eclipse.team.core.mapping.IResourceDiff;

/**
 * Implementation of {@link IResourceDiff}.
 * <p>
 * This class may be subclassed by clients.
 *
 * @since 3.2
 */
public class ResourceDiff extends TwoWayDiff implements IResourceDiff {

	private final IFileRevision before;
	private final IFileRevision after;
	private final IResource resource;

	/**
	 * Create a two-way resource diff
	 * @param resource the resource
	 * @param kind the kind of change (ADDED, REMOVED or CHANGED)
	 * @param flags additional flags that describe the change
	 * @param before the before state of the model object
	 * @param after the after state of the model object
	 */
	public ResourceDiff(IResource resource, int kind, int flags, IFileRevision before, IFileRevision after) {
		super(resource.getFullPath(), kind, flags);
		this.resource = resource;
		this.before = before;
		this.after = after;
	}

	/**
	 * Convenience constructor for creating a simple folder diff that consists of a
	 * resource and a kind only. It is equivalent to
	 * <code>ResourceDiff(resource, kind, 0, null, null)</code>
	 *
	 * @param resource a resource
	 * @param kind     the kind of change (ADDED, REMOVED or CHANGED)
	 */
	public ResourceDiff(IResource resource, int kind) {
		this(resource, kind, 0, null, null);
		Assert.isTrue(resource.getType() != IResource.FILE);
	}

	@Override
	public IFileRevision getBeforeState() {
		return before;
	}

	@Override
	public IFileRevision getAfterState() {
		return after;
	}

	@Override
	public IResource getResource() {
		return resource;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this)
			return true;
		if (super.equals(obj)) {
			if (obj instanceof ResourceDiff) {
				ResourceDiff other = (ResourceDiff) obj;
				return getResource().equals(getResource())
					&& revisionsEqual(getBeforeState(), other.getBeforeState())
					&& revisionsEqual(getAfterState(), other.getAfterState());
			}
		}
		return false;
	}

	private boolean revisionsEqual(IFileRevision revision, IFileRevision revision2) {
		if (revision == null)
			return revision2 == null;
		if (revision2 == null)
			return false;
		return revision.equals(revision2);
	}

}
