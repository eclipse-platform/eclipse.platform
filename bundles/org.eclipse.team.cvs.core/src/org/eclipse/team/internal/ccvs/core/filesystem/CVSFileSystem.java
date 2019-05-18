/*******************************************************************************
 * Copyright (c) 2000, 2018 IBM Corporation and others.
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
package org.eclipse.team.internal.ccvs.core.filesystem;
import java.net.URI;
import java.util.HashMap;

import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.filesystem.provider.FileSystem;
import org.eclipse.core.runtime.*;
import org.eclipse.team.internal.ccvs.core.CVSMessages;
import org.eclipse.team.internal.ccvs.core.CVSProviderPlugin;
import org.eclipse.team.internal.ccvs.core.ICVSRemoteFolder;
import org.eclipse.team.internal.ccvs.core.resources.RemoteFolderTree;

public class CVSFileSystem extends FileSystem {


	private CVSFileTree cvsTree;

	public CVSFileSystem() {
		super();
	}

	public IFileStore getStore(URI uri) {
		return new CVSFileStore(CVSURI.fromUri(uri), null);
	}

	public boolean canReturnFullTree(){
		return true;
	}
	
	public CVSFileTree getFullTree(URI uri, IProgressMonitor monitor){
		try{
		monitor.beginTask(CVSMessages.CVSFileSystem_FetchTree, 100);
		//always return new tree
		//monitor.subTask(NLS.bind(message, binding));
		if (cvsTree != null)
		return cvsTree;

		return this.refreshTree(uri, monitor);
		}
		finally{
			monitor.done();
		}
	}
	

	public CVSFileTree refreshTree(URI uri, IProgressMonitor monitor){
		CVSURI cvsURI = CVSURI.fromUri(uri);
		
		//Make sure that we're building the tree from the topmost level - keep cycling until you hit null
		ICVSRemoteFolder folder = cvsURI.getProjectURI().toFolder();
		
		try {
			RemoteLogger logger = new RemoteLogger(folder);

			RemoteFolderTree remoteTree = logger.fetchTree(SubMonitor.convert(monitor,80));
			HashMap folderMap = logger.getFolderMap();
			HashMap logMap = logger.getLogMap();
			folderMap.put(folder.getName(), remoteTree);
			//Save tree
			cvsTree = new CVSFileTree(new CVSFileStore(cvsURI, null), cvsURI, remoteTree, folderMap, logMap);

			return cvsTree;
		} catch (CoreException e) {
			CVSProviderPlugin.log(e);
			return null;
		}
		
	
	}
}
