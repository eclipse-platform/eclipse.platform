/*******************************************************************************
 * Copyright (c) 2006, 2015 IBM Corporation and others.
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
 *     Lars Vogel <Lars.Vogel@vogella.com> - Bug 473427
 *******************************************************************************/
package org.eclipse.core.internal.resources.mapping;

import java.util.*;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;

/**
 * Concrete implementation of IResourceDelta used for operation validation
 */
public final class ProposedResourceDelta extends PlatformObject implements IResourceDelta {
	protected static int KIND_MASK = 0xFF;

	private HashMap<String, ProposedResourceDelta> children = new HashMap<>(8);
	private IPath movedFromPath;
	private IPath movedToPath;
	private IResource resource;
	private int status;

	public ProposedResourceDelta(IResource resource) {
		this.resource = resource;
	}

	@Override
	public void accept(IResourceDeltaVisitor visitor) throws CoreException {
		accept(visitor, 0);
	}

	@Override
	public void accept(IResourceDeltaVisitor visitor, boolean includePhantoms) throws CoreException {
		accept(visitor, includePhantoms ? IContainer.INCLUDE_PHANTOMS : 0);
	}

	@Override
	public void accept(IResourceDeltaVisitor visitor, int memberFlags) throws CoreException {
		if (!visitor.visit(this))
			return;
		for (ProposedResourceDelta childDelta : children.values()) {
			childDelta.accept(visitor, memberFlags);
		}
	}

	/**
	 * Adds a child delta to the list of children for this delta node.
	 * @param delta
	 */
	protected void add(ProposedResourceDelta delta) {
		if (children.isEmpty() && status == 0)
			setKind(IResourceDelta.CHANGED);
		children.put(delta.getResource().getName(), delta);
	}

	/**
	 * Adds the given flags to this delta.
	 * @param flags The flags to add
	 */
	protected void addFlags(int flags) {
		//make sure the provided flags don't influence the kind
		this.status |= (flags & ~KIND_MASK);
	}

	@Override
	public IResourceDelta findMember(IPath path) {
		int segmentCount = path.segmentCount();
		if (segmentCount == 0)
			return this;

		//iterate over the path and find matching child delta
		ProposedResourceDelta current = this;
		for (int i = 0; i < segmentCount; i++) {
			current = current.children.get(path.segment(i));
			if (current == null)
				return null;
		}
		return current;
	}

	@Override
	public IResourceDelta[] getAffectedChildren() {
		return getAffectedChildren(ADDED | REMOVED | CHANGED, IResource.NONE);
	}

	@Override
	public IResourceDelta[] getAffectedChildren(int kindMask) {
		return getAffectedChildren(kindMask, IResource.NONE);
	}

	@Override
	public IResourceDelta[] getAffectedChildren(int kindMask, int memberFlags) {
		List<ProposedResourceDelta> result = new ArrayList<>();
		for (ProposedResourceDelta child : children.values()) {
			if ((child.getKind() & kindMask) != 0)
				result.add(child);
		}
		return result.toArray(new IResourceDelta[result.size()]);
	}

	/**
	 * Returns the child delta corresponding to the given child resource name,
	 * or <code>null</code>.
	 */
	ProposedResourceDelta getChild(String name) {
		return children.get(name);
	}

	@Override
	public int getFlags() {
		return status & ~KIND_MASK;
	}

	@Override
	public IPath getFullPath() {
		return getResource().getFullPath();
	}

	@Override
	public int getKind() {
		return status & KIND_MASK;
	}

	@Override
	public IMarkerDelta[] getMarkerDeltas() {
		return new IMarkerDelta[0];
	}

	@Override
	public IPath getMovedFromPath() {
		return movedFromPath;
	}

	@Override
	public IPath getMovedToPath() {
		return movedToPath;
	}

	@Override
	public IPath getProjectRelativePath() {
		return getResource().getProjectRelativePath();
	}

	@Override
	public IResource getResource() {
		return resource;
	}

	public void setFlags(int flags) {
		status = getKind() | (flags & ~KIND_MASK);
	}

	protected void setKind(int kind) {
		status = getFlags() | (kind & KIND_MASK);
	}

	protected void setMovedFromPath(IPath path) {
		movedFromPath = path;
	}

	protected void setMovedToPath(IPath path) {
		movedToPath = path;
	}

	/**
	 * For debugging purposes only.
	 */
	@Override
	public String toString() {
		return "ProposedDelta(" + resource + ')'; //$NON-NLS-1$
	}
}
