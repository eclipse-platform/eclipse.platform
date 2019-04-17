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
import org.eclipse.team.internal.ccvs.core.ICVSFile;
import org.eclipse.team.internal.ccvs.core.ICVSResource;
import org.eclipse.team.internal.ccvs.core.syncinfo.ResourceSyncInfo;
import org.eclipse.team.internal.ccvs.ui.model.CVSResourceElement;
import org.eclipse.ui.PlatformUI;

public class CVSFileElement extends CVSResourceElement {
	
	private ICVSFile file;
	
	public CVSFileElement(ICVSFile file) {
		this.file = file;
	}
	
	/**
	 * Initial implementation: return null;
	 */
	@Override
	public Object[] fetchChildren(Object o, IProgressMonitor monitor) {
		return new Object[0];
	}
	/**
	 * Initial implementation: return null.
	 */
	@Override
	public ImageDescriptor getImageDescriptor(Object object) {
		return PlatformUI.getWorkbench().getEditorRegistry().getImageDescriptor(file.getName());
	}
	/**
	 * Initial implementation: return the file's name and version
	 */
	@Override
	public String getLabel(Object o) {
		try {
			ResourceSyncInfo info = file.getSyncInfo();
			if(info!=null) {
				return file.getName() + " " + info.getRevision(); //$NON-NLS-1$
			} else {
				return file.getName();
			}
		} catch (TeamException e) {
			handle(null, null, e);
			return null;
		}
	}

	@Override
	public Object getParent(Object o) {
		return null;
	}
	
	public ICVSFile getCVSFile() {
		return file;
	}

	@Override
	public ICVSResource getCVSResource() {
		return file;
	}
}
