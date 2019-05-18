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

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.ui.CVSUIPlugin;
import org.eclipse.team.internal.ccvs.ui.repo.RepositoryManager;
import org.eclipse.ui.PlatformUI;

/**
 * A tag source that returns the tags associated with a single remote folder
 */
public class SingleFolderTagSource extends TagSource {

	public static CVSTag[] getTags(ICVSFolder folder, int type) {
		if (type == CVSTag.HEAD)
			return new CVSTag[] { CVSTag.DEFAULT } ;
		return CVSUIPlugin.getPlugin().getRepositoryManager().getKnownTags(folder, type);
	}
	
	private ICVSFolder folder;
	
	/* package */ SingleFolderTagSource(ICVSFolder folder) {
		this.folder = folder;
	}

	@Override
	public CVSTag[] getTags(int type) {
		if (type == CVSTag.HEAD || type == BASE) {
			return super.getTags(type);
		}
		return getTags(getFolder(), type);
	}

	/**
	 * Return the folder the tags are obtained from
	 * @return the folder the tags are obtained from
	 */
	public ICVSFolder getFolder() {
		return folder;
	}

	@Override
	public CVSTag[] refresh(boolean bestEffort, IProgressMonitor monitor) throws TeamException {
		CVSTag[] tags = CVSUIPlugin.getPlugin().getRepositoryManager().refreshDefinedTags(getFolder(), bestEffort /* recurse */, true /* notify */, monitor);
		fireChange();
		return tags;
	}

	@Override
	public ICVSRepositoryLocation getLocation() {
		RepositoryManager mgr = CVSUIPlugin.getPlugin().getRepositoryManager();
		ICVSRepositoryLocation location = mgr.getRepositoryLocationFor(getFolder());
		return location;
	}

	@Override
	public String getShortDescription() {
		return getFolder().getName();
	}

	@Override
	public void commit(final CVSTag[] tags, final boolean replace, IProgressMonitor monitor) throws CVSException {
		try {
			final RepositoryManager manager = CVSUIPlugin.getPlugin().getRepositoryManager();	
			manager.run(monitor1 -> {
				try {
					ICVSFolder folder = getFolder();
					if (replace) {
						CVSTag[] oldTags = manager.getKnownTags(folder);
						manager.removeTags(folder, oldTags);
					}
					manager.addTags(folder, tags);
				} catch (CVSException e) {
					throw new InvocationTargetException(e);
				}
			}, monitor);
		} catch (InvocationTargetException e) {
			throw CVSException.wrapException(e);
		} catch (InterruptedException e) {
			// Ignore
		}
		fireChange();
	}

	@Override
	public ICVSResource[] getCVSResources() {
		final ICVSResource[][] resources = new ICVSResource[][] { null };
		try {
			getRunnableContext().run(true, true, monitor -> {
				try {
					resources[0] = folder.fetchChildren(monitor);
				} catch (TeamException e) {
					throw new InvocationTargetException(e);
				} finally {
					monitor.done();
				}
			});
			return resources[0];
		} catch (InvocationTargetException e) {
			CVSUIPlugin.log(CVSException.wrapException(e));
		} catch (InterruptedException e) {
			// Ignore
		}
		return new ICVSResource[] { folder };
	}
	
	private IRunnableContext getRunnableContext() {
		return PlatformUI.getWorkbench().getProgressService();
	}

}
