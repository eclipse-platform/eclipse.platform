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
package org.eclipse.team.internal.ccvs.core.client;


import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.team.internal.ccvs.core.CVSException;
import org.eclipse.team.internal.ccvs.core.ICVSFile;
import org.eclipse.team.internal.ccvs.core.ICVSFolder;
import org.eclipse.team.internal.ccvs.core.ICVSResource;
import org.eclipse.team.internal.ccvs.core.ICVSResourceVisitor;

/**
 * Goes recursivly through the folders checks if they are empyty
 * and deletes them. Of course it is starting at the leaves of the
 * recusion (the folders that do not have subfolders).
 */
public class PruneFolderVisitor implements ICVSResourceVisitor {
	
	private ICVSFolder localRoot;
	
	public PruneFolderVisitor() {
	}
	
	/**
	 * This method is used to visit a set of ICVSResources.
	 */
	public void visit(Session s, ICVSResource[] resources) throws CVSException {
		visit(s.getLocalRoot(), resources);
	}
	
	/**
	 * This method is used to visit a set of ICVSResources.
	 */
	public void visit(ICVSFolder root, ICVSResource[] resources) throws CVSException {
		localRoot = root;
		
		// Visit the resources
		Set prunableParents = new HashSet();
		for (ICVSResource cvsResource : resources) {
			// prune the resource and it's children when appropriate
			cvsResource.accept(this);
			// if the resource doesn't exists, attempt to prune it's parent
			if (!cvsResource.exists())
				prunableParents.add(cvsResource.getParent());
		}
		for (Iterator iter = prunableParents.iterator(); iter.hasNext();) {
			ICVSFolder cvsFolder = (ICVSFolder)iter.next();
			pruneFolderAndParentsIfAppropriate(cvsFolder);
		}
	}
	
	@Override
	public void visitFile(ICVSFile file) throws CVSException {
		// nothing to do here
	}

	@Override
	public void visitFolder(ICVSFolder folder) throws CVSException {
		// First prune any empty children
		folder.acceptChildren(this);
		// Then prune the folder if it is empty
		pruneFolderIfAppropriate(folder);
	}
	
	private void pruneFolderIfAppropriate(ICVSFolder folder) throws CVSException {
		// Only prune managed folders that are not the root of the operation
		if (folder.exists() && folder.isManaged() 
			&& ! folder.equals(getLocalRoot())
			&& folder.members(ICVSFolder.ALL_EXISTING_MEMBERS).length == 0) {
			
			// Delete the folder but keep a phantom for local folders
			folder.delete();
		}
	}
	
	private ICVSFolder getLocalRoot() {
		return localRoot;
	}

	/**
	 * Attempt to prune the given folder. If the folder is pruned, attempt to prune it's parent.
	 */
	private void pruneFolderAndParentsIfAppropriate(ICVSFolder folder) throws CVSException {
		pruneFolderIfAppropriate(folder);
		if (!folder.exists()) {
			ICVSFolder parent = folder.getParent();
			pruneFolderAndParentsIfAppropriate(parent);
		}
	}
}
