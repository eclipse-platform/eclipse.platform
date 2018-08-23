/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ccvs.ui.repo;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.team.internal.ccvs.core.ICVSRemoteFolder;
import org.eclipse.team.internal.ccvs.core.ICVSRemoteResource;
import org.eclipse.team.internal.ccvs.ui.model.RemoteModule;

public class RepositoriesFilter extends ViewerFilter {
	
	private boolean fShowModules;

	public RepositoriesFilter(boolean showModules) {
		fShowModules = showModules;
	}
	
	public boolean select(Viewer viewer, Object parentElement, Object element) {
		if (element instanceof ICVSRemoteFolder) {
			if (((ICVSRemoteFolder)element).isDefinedModule()) {
				return fShowModules;
			}
		}
		if (element instanceof RemoteModule) {
			ICVSRemoteResource resource = ((RemoteModule)element).getCVSResource();
			if (resource instanceof ICVSRemoteFolder) {
				ICVSRemoteFolder folder = (ICVSRemoteFolder) resource;
				if (folder.isDefinedModule()) {
					return fShowModules;
				}
			}
		}
		return true;
	}
	
	public boolean isShowModules() {
		return fShowModules;
	}
}
