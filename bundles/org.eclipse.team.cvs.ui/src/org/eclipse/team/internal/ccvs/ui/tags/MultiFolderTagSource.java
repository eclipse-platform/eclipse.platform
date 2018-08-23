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
package org.eclipse.team.internal.ccvs.ui.tags;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.ui.CVSUIMessages;
import org.eclipse.team.internal.ccvs.ui.CVSUIPlugin;
import org.eclipse.team.internal.ccvs.ui.repo.RepositoryManager;

/**
 * A tag source that returns the tags associated with multiple remote folders.
 * 
 * TODO: Temporarily a subclass of single folder until I 
 * can figure out how to handle the multi-folder case.
 */
public class MultiFolderTagSource extends SingleFolderTagSource {

    private final ICVSFolder[] folders;

    /* package */ MultiFolderTagSource(ICVSFolder[] folders) {
        super(folders[0]);
        this.folders = folders;
    }
    
    /* (non-Javadoc)
     * @see org.eclipse.team.internal.ccvs.ui.merge.SingleFolderTagSource#getShortDescription()
     */
    public String getShortDescription() {
        return NLS.bind(CVSUIMessages.MultiFolderTagSource_0, new String[] { Integer.toString(folders.length) }); 
    }
    
    /* (non-Javadoc)
     * @see org.eclipse.team.internal.ccvs.ui.merge.TagSource#getTags(int)
     */
    public CVSTag[] getTags(int type) {
        if (type == CVSTag.HEAD || type == BASE) {
            return super.getTags(type);
        }
        Set tags= new HashSet();
        for (int i= 0; i < folders.length; i++) {
			tags.addAll(Arrays.asList(getTags(folders[i], type)));
		}
        return (CVSTag[]) tags.toArray(new CVSTag[tags.size()]);
    }
    
    /* (non-Javadoc)
     * @see org.eclipse.team.internal.ccvs.ui.tags.SingleFolderTagSource#refresh(boolean, org.eclipse.core.runtime.IProgressMonitor)
     */
    public CVSTag[] refresh(boolean bestEffort, IProgressMonitor monitor) throws TeamException {
		monitor.beginTask("", folders.length);  //$NON-NLS-1$
        Set result= new HashSet();
    	for (int i= 0; i < folders.length; i++) {
			ICVSFolder folder= folders[i];
			CVSTag[] tags = CVSUIPlugin.getPlugin().getRepositoryManager().refreshDefinedTags(folder, bestEffort /* recurse */, true /* notify */, Policy.subMonitorFor(monitor, 1));
			result.addAll(Arrays.asList(tags));
		}
    	monitor.done();
        fireChange();
        return (CVSTag[]) result.toArray(new CVSTag[result.size()]);
    }
    
    /* (non-Javadoc)
     * @see org.eclipse.team.internal.ccvs.ui.tags.SingleFolderTagSource#getCVSResources()
     */
    public ICVSResource[] getCVSResources() {
        return folders;
    }
    
    public ICVSFolder[] getFolders(){
    	return folders;
    }
    
    /* (non-Javadoc)
     * @see org.eclipse.team.internal.ccvs.ui.merge.TagSource#commit(org.eclipse.team.internal.ccvs.core.CVSTag[], boolean, org.eclipse.core.runtime.IProgressMonitor)
     */
    public void commit(final CVSTag[] tags, final boolean replace, IProgressMonitor monitor) throws CVSException {
		try {
            final RepositoryManager manager = CVSUIPlugin.getPlugin().getRepositoryManager();	
            manager.run(new IRunnableWithProgress() {
            	public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					try {
						ICVSFolder[] folders = getFolders();
						for (int i = 0; i < folders.length; i++) {
							if (replace) {
								CVSTag[] oldTags = manager
										.getKnownTags(folders[i]);
								manager.removeTags(folders[i], oldTags);
							}
							manager.addTags(folders[i], tags);
						}
					} catch (CVSException e) {
						throw new InvocationTargetException(e);
					}
				}
            }, monitor);
        } catch (InvocationTargetException e) {
            throw CVSException.wrapException(e);
        } catch (InterruptedException e) {
            // Ignore
        }
        fireChange();
    }
}
