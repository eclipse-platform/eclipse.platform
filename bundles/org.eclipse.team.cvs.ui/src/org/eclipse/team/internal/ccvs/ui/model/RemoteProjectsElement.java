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
package org.eclipse.team.internal.ccvs.ui.model;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.internal.ccvs.core.CVSTag;
import org.eclipse.team.internal.ccvs.core.ICVSRepositoryLocation;

/**
 * This model element provides the remote projects for a given repository and
 * tag.
 */
public class RemoteProjectsElement extends CVSTagElement {
	
	/**
	 * Constructor for RemoteProjectsElement.
	 */
	public RemoteProjectsElement() {
		super(CVSTag.DEFAULT, null);
	}
	
	/**
	 * Sets the root.
	 * @param root The root to set
	 */
	public void setRoot(ICVSRepositoryLocation root) {
		this.root = root;
	}

	/**
	 * Sets the tag.
	 * @param tag The tag to set
	 */
	public void setTag(CVSTag tag) {
		this.tag = tag;
	}
	/**
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object o) {
		if (!(o instanceof RemoteProjectsElement)) return false;
		RemoteProjectsElement element = (RemoteProjectsElement)o;
		if (root == null) {
			return element.root == null && tag.equals(element.tag);
		}
		return super.equals(o);
	}

	/**
	 * @see org.eclipse.ui.model.IWorkbenchAdapter#getParent(java.lang.Object)
	 */
	public Object getParent(Object o) {
		return null;
	}

	/**
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		if (root == null) return tag.hashCode();
		return super.hashCode();
	}

	/**
	 * @see org.eclipse.team.internal.ccvs.ui.model.CVSModelElement#internalGetChildren(java.lang.Object, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public Object[] fetchChildren(Object o, IProgressMonitor monitor) throws TeamException {
		if (root == null) return new Object[0];
		return super.fetchChildren(o, monitor);
	}
}
