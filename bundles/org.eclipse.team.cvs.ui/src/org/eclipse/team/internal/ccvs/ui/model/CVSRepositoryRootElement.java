/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
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
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.team.internal.ccvs.core.CVSTag;
import org.eclipse.team.internal.ccvs.core.ICVSRepositoryLocation;
import org.eclipse.team.internal.ccvs.ui.CVSUIPlugin;
import org.eclipse.team.internal.ccvs.ui.ICVSUIConstants;
import org.eclipse.team.internal.ccvs.ui.repo.RepositoryRoot;

/**
 * RemoteRootElement is the model element for a repository that
 * appears in the repositories view. Its children are:
 * <ul>
 * <li>HEAD
 * <li>Branch tags category
 * <li>Version tags category
 * <li>Date tags category
 * </ul>
 */
public class CVSRepositoryRootElement extends CVSModelElement {
	public ImageDescriptor getImageDescriptor(Object object) {
		if (object instanceof ICVSRepositoryLocation || object instanceof RepositoryRoot) {
			return CVSUIPlugin.getPlugin().getImageDescriptor(ICVSUIConstants.IMG_REPOSITORY);
		}
		return null;
	}
	public String getLabel(Object o) {
		if (o instanceof ICVSRepositoryLocation) {
			ICVSRepositoryLocation root = (ICVSRepositoryLocation)o;
			o = CVSUIPlugin.getPlugin().getRepositoryManager().getRepositoryRootFor(root);
			if (o == null) {
				return root.getLocation(true);
			}
		}
		if (o instanceof RepositoryRoot) {
			RepositoryRoot root = (RepositoryRoot)o;
			String name = root.getName();
			if (name == null)
				return root.getRoot().getLocation(true);
			else
				return name;
		}
		return null;
	}
	public Object getParent(Object o) {
		return null;
	}
	public Object[] fetchChildren(Object o, IProgressMonitor monitor) {
		ICVSRepositoryLocation location = null;
		if (o instanceof ICVSRepositoryLocation) {
			location = (ICVSRepositoryLocation)o;
		}
		if (o instanceof RepositoryRoot) {
			RepositoryRoot root = (RepositoryRoot)o;
			location = root.getRoot();
		}
		if (location == null) return null;
		return new Object[] {
			new CVSTagElement(CVSTag.DEFAULT, location),
			new BranchCategory(location),
			new VersionCategory(location),
			new DateTagCategory(location)
		};
	}
}
