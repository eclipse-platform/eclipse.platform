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
package org.eclipse.team.internal.ccvs.ui.tags;


import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.ui.model.CVSResourceElement;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

public class CVSFolderElement extends CVSResourceElement {
	
	private ICVSFolder folder;
	private boolean includeUnmanaged;
	
	public CVSFolderElement(ICVSFolder folder, boolean includeUnmanaged) {
		this.folder = folder;
		this.includeUnmanaged = includeUnmanaged;
	}
	
	/**
	 * Returns CVSResourceElement instances
	 */
	@Override
	public Object[] fetchChildren(Object o, IProgressMonitor monitor) throws TeamException {
		ICVSResource[] children = folder.fetchChildren(monitor);
		CVSResourceElement[] elements = new CVSResourceElement[children.length];
		for (int i = 0; i < children.length; i++) {
			ICVSResource resource = children[i];
			if(resource.isFolder()) {
				elements[i] = new CVSFolderElement((ICVSFolder)resource, includeUnmanaged);
			} else {
				elements[i] = new CVSFileElement((ICVSFile)resource);
			}
		}
		return elements;
	}
	
	/**
	 * @see org.eclipse.team.internal.ccvs.ui.model.CVSModelElement#isRemoteElement()
	 */
	public boolean isRemoteElement() {
		return true;
	}
	
	/**
	 * Overridden to append the version name to remote folders which
	 * have version tags and are top-level folders.
	 */
	@Override
	public String getLabel(Object o) {
		return folder.getName();
	}

	@Override
	public ImageDescriptor getImageDescriptor(Object object) {
		return PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_OBJ_FOLDER);
	}	
	
	@Override
	public Object getParent(Object o) {
		return new CVSFolderElement(folder.getParent(), includeUnmanaged);
	}
	
	@Override
	public ICVSResource getCVSResource() {
		return folder ;
	}
}
