/*******************************************************************************
 * Copyright (c) 2000, 2014 IBM Corporation and others.
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
package org.eclipse.core.internal.localstore;

import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.internal.resources.Container;
import org.eclipse.core.internal.resources.Resource;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * Performs a local refresh, and additionally updates all aliases of the
 * refreshed resource.
 */
public class RefreshLocalAliasVisitor extends RefreshLocalVisitor {
	public RefreshLocalAliasVisitor(IProgressMonitor monitor) {
		super(monitor);
	}

	@Override
	protected void createResource(UnifiedTreeNode node, Resource target) throws CoreException {
		super.createResource(node, target);
		IFileStore store = node.getStore();
		if (store == null)
			return;
		IResource[] aliases = workspace.getAliasManager().computeAliases(target, store);
		if (aliases != null)
			for (IResource alias : aliases) {
				if (alias.getProject().isOpen() && !((Resource) alias).isFiltered()) {
					super.createResource(node, (Resource) alias);
				}
			}
	}

	@Override
	protected void deleteResource(UnifiedTreeNode node, Resource target) throws CoreException {
		super.deleteResource(node, target);
		IFileStore store = node.getStore();
		if (store == null)
			return;
		IResource[] aliases = workspace.getAliasManager().computeAliases(target, store);
		if (aliases != null) {
			boolean wasFilteredOut = false;
			if (store.fetchInfo() != null && store.fetchInfo().exists())
				wasFilteredOut = target.isFiltered();
			for (IResource aliase : aliases) {
				if (aliase.getProject().isOpen()) {
					if (wasFilteredOut) {
						if (((Resource) aliase).isFiltered())
							super.deleteResource(node, (Resource) aliase);
					} else
						super.deleteResource(node, (Resource) aliase);
				}
			}
		}
	}

	@Override
	protected void resourceChanged(UnifiedTreeNode node, Resource target) {
		super.resourceChanged(node, target);
		IFileStore store = node.getStore();
		if (store == null)
			return;
		IResource[] aliases = workspace.getAliasManager().computeAliases(target, store);
		if (aliases != null)
			for (IResource aliase : aliases) {
				if (aliase.getProject().isOpen())
					super.resourceChanged(node, (Resource) aliase);
			}
	}

	@Override
	protected void fileToFolder(UnifiedTreeNode node, Resource target) throws CoreException {
		super.fileToFolder(node, target);
		IFileStore store = node.getStore();
		if (store == null)
			return;
		IResource[] aliases = workspace.getAliasManager().computeAliases(target, store);
		if (aliases != null)
			for (IResource aliase : aliases)
				super.fileToFolder(node, (Resource) aliase);
	}

	@Override
	protected void folderToFile(UnifiedTreeNode node, Resource target) throws CoreException {
		super.folderToFile(node, target);
		IFileStore store = node.getStore();
		if (store == null)
			return;
		IResource[] aliases = workspace.getAliasManager().computeAliases(target, store);
		if (aliases != null)
			for (IResource aliase : aliases)
				super.folderToFile(node, (Resource) aliase);
	}

	@Override
	protected void refresh(Container parent) throws CoreException {
		parent.getLocalManager().refresh(parent, IResource.DEPTH_ZERO, true, null);
	}

}
