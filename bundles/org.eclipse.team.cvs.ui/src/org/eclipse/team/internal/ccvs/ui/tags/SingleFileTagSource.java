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
		Set tagSet = new HashSet();
		ILogEntry[] entries = file.getLogEntries(monitor);
		for (int j = 0; j < entries.length; j++) {
			CVSTag[] tags = entries[j].getTags();
			for (int k = 0; k < tags.length; k++) {
				tagSet.add(tags[k]);
			}
		}
		return (CVSTag[])tagSet.toArray(new CVSTag[tagSet.size()]);
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

    /* (non-Javadoc)
     * @see org.eclipse.team.internal.ccvs.ui.tags.TagSource#getTags(int)
     */
    public CVSTag[] getTags(int type) {
        return parentFolderTagSource.getTags(type);
    }

    /* (non-Javadoc)
     * @see org.eclipse.team.internal.ccvs.ui.tags.TagSource#refresh(org.eclipse.core.runtime.IProgressMonitor)
     */
    public CVSTag[] refresh(boolean bestEffort, IProgressMonitor monitor) throws TeamException {
        CVSTag[] tags = fetchTagsFor(file, monitor); 
        commit(tags, false, monitor);
        fireChange();
        return tags;
    }

    /* (non-Javadoc)
     * @see org.eclipse.team.internal.ccvs.ui.tags.TagSource#getLocation()
     */
    public ICVSRepositoryLocation getLocation() {
		RepositoryManager mgr = CVSUIPlugin.getPlugin().getRepositoryManager();
		ICVSRepositoryLocation location = mgr.getRepositoryLocationFor(file);
		return location;
    }

    /* (non-Javadoc)
     * @see org.eclipse.team.internal.ccvs.ui.tags.TagSource#getShortDescription()
     */
    public String getShortDescription() {
        return file.getName();
    }

    /* (non-Javadoc)
     * @see org.eclipse.team.internal.ccvs.ui.tags.TagSource#commit(org.eclipse.team.internal.ccvs.core.CVSTag[], boolean, org.eclipse.core.runtime.IProgressMonitor)
     */
    public void commit(CVSTag[] tags, boolean replace, IProgressMonitor monitor) throws CVSException {
        parentFolderTagSource.commit(tags, replace, monitor);
        fireChange();
    }

    /* (non-Javadoc)
     * @see org.eclipse.team.internal.ccvs.ui.tags.TagSource#getCVSResources()
     */
    public ICVSResource[] getCVSResources() {
        return new ICVSResource[] { file };
    }

}
