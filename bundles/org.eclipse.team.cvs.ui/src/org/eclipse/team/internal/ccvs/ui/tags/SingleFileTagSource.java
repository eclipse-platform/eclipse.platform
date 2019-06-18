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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.ui.CVSUIPlugin;
import org.eclipse.team.internal.ccvs.ui.repo.RepositoryManager;

/**
 * A tag source for a single ICVSFile
 */
public class SingleFileTagSource extends TagSource {
	
	public static CVSTag[] fetchTagsFor(ICVSFile file, IProgressMonitor monitor) throws TeamException {
		Set<CVSTag> tagSet = new HashSet<>();
		for (ILogEntry entry : file.getLogEntries(monitor)) {
			Collections.addAll(tagSet, entry.getTags());
		}
		return tagSet.toArray(new CVSTag[tagSet.size()]);
	}
	
	private ICVSFile file;
	private TagSource parentFolderTagSource;
	
	/* package */ /**
	 * 
	 */
	public SingleFileTagSource(ICVSFile file) {
		this.file = file;
		parentFolderTagSource = TagSource.create(new ICVSResource[] { file.getParent() });
	}

	@Override
	public CVSTag[] getTags(int type) {
		return parentFolderTagSource.getTags(type);
	}

	@Override
	public CVSTag[] refresh(boolean bestEffort, IProgressMonitor monitor) throws TeamException {
		CVSTag[] tags = fetchTagsFor(file, monitor); 
		commit(tags, false, monitor);
		fireChange();
		return tags;
	}

	@Override
	public ICVSRepositoryLocation getLocation() {
		RepositoryManager mgr = CVSUIPlugin.getPlugin().getRepositoryManager();
		ICVSRepositoryLocation location = mgr.getRepositoryLocationFor(file);
		return location;
	}

	@Override
	public String getShortDescription() {
		return file.getName();
	}

	@Override
	public void commit(CVSTag[] tags, boolean replace, IProgressMonitor monitor) throws CVSException {
		parentFolderTagSource.commit(tags, replace, monitor);
		fireChange();
	}

	@Override
	public ICVSResource[] getCVSResources() {
		return new ICVSResource[] { file };
	}

}
