/*******************************************************************************
 * Copyright (c) 2006, 2011 IBM Corporation and others.
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
package org.eclipse.compare.internal.patch;

import org.eclipse.compare.IResourceProvider;
import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.patch.PatchConfiguration;
import org.eclipse.compare.structuremergeviewer.DiffNode;
import org.eclipse.compare.structuremergeviewer.IDiffContainer;
import org.eclipse.core.resources.IResource;

public abstract class PatchDiffNode extends DiffNode implements IResourceProvider {

	private Object fElement;

	public PatchDiffNode(Object patchElement, IDiffContainer parent, int kind,
			ITypedElement ancestor, ITypedElement left, ITypedElement right) {
		super(parent, kind, ancestor, left, right);
		fElement = patchElement;
	}

	public PatchDiffNode(Object patchElement, IDiffContainer parent, int kind) {
		super(parent, kind);
		fElement = patchElement;
	}

	public boolean isEnabled() {
		return getPatcher().isEnabled(getPatchElement());
	}

	public void setEnabled(boolean enabled) {
		getPatcher().setEnabled(getPatchElement(), enabled);
	}

	protected final Patcher getPatcher() {
		return Patcher.getPatcher(getConfiguration());
	}

	public Object getPatchElement() {
		return fElement;
	}

	protected abstract PatchConfiguration getConfiguration();

	@Override
	public boolean equals(Object other) {
		if (other instanceof PatchDiffNode) {
			PatchDiffNode node = (PatchDiffNode) other;
			return (node.getPatchElement().equals(getPatchElement()));
		}
		return super.equals(other);
	}

	@Override
	public int hashCode() {
		return getPatchElement().hashCode();
	}

	@Override
	public IResource getResource() {
		return null;
	}
}
