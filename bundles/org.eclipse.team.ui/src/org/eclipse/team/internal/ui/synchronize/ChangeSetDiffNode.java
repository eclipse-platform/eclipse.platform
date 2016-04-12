/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ui.synchronize;

import org.eclipse.compare.structuremergeviewer.IDiffContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.team.internal.core.subscribers.ChangeSet;
import org.eclipse.team.internal.ui.ITeamUIImages;
import org.eclipse.team.internal.ui.TeamUIPlugin;

/**
 * Node that represents a Change set in a synchronize page.
 */
public class ChangeSetDiffNode extends SynchronizeModelElement {

    private final ChangeSet set;

    public ChangeSetDiffNode(IDiffContainer parent, ChangeSet set) {
        super(parent);
        this.set = set;
    }

    /* (non-Javadoc)
     * @see org.eclipse.team.ui.synchronize.ISynchronizeModelElement#getResource()
     */
    @Override
	public IResource getResource() {
        return null;
    }

    public ChangeSet getSet() {
        return set;
    }

	/* (non-Javadoc)
	 * @see org.eclipse.ui.model.IWorkbenchAdapter#getImageDescriptor(java.lang.Object)
	 */
	@Override
	public ImageDescriptor getImageDescriptor(Object object) {
		return TeamUIPlugin.getImageDescriptor(ITeamUIImages.IMG_CHANGE_SET);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.compare.structuremergeviewer.DiffNode#getName()
	 */
	@Override
	public String getName() {
		return set.getName();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.SyncInfoModelElement#toString()
	 */
	@Override
	public String toString() {
		return getName();
	}

	/* (non-Javadoc)
     * @see org.eclipse.team.internal.ui.synchronize.SynchronizeModelElement#hashCode()
     */
    @Override
	public int hashCode() {
        return set.hashCode();
    }

    /* (non-Javadoc)
     * @see org.eclipse.team.internal.ui.synchronize.SynchronizeModelElement#equals(java.lang.Object)
     */
    @Override
	public boolean equals(Object object) {
        if (object instanceof ChangeSetDiffNode) {
            return((ChangeSetDiffNode)object).getSet() == set;
        }
        return super.equals(object);
    }

    /* (non-Javadoc)
     * @see org.eclipse.team.internal.ui.synchronize.SynchronizeModelElement#getAdapter(java.lang.Class)
     */
    @Override
	public Object getAdapter(Class adapter) {
        if (adapter.equals(ChangeSet.class)) {
            return set;
        }
        return super.getAdapter(adapter);
    }
}
