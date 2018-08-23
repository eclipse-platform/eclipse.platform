/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
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
package org.eclipse.team.examples.filesystem.ui;

import org.eclipse.core.resources.mapping.ResourceTraversal;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.diff.*;
import org.eclipse.team.core.subscribers.SubscriberScopeManager;
import org.eclipse.team.examples.filesystem.FileSystemProvider;
import org.eclipse.team.examples.filesystem.Policy;
import org.eclipse.team.examples.filesystem.subscriber.FileSystemSubscriber;
import org.eclipse.ui.IWorkbenchPart;

/**
 * Operation for copying the selected resources to the file system location
 */
public class PutOperation extends FileSystemOperation {

	private boolean overwriteIncoming;

	/**
	 * Create the put operation
	 * @param part the originating part
	 * @param manager the scope manager
	 */
	protected PutOperation(IWorkbenchPart part, SubscriberScopeManager manager) {
		super(part, manager);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.examples.filesystem.ui.FileSystemOperation#execute(org.eclipse.team.examples.filesystem.FileSystemProvider, org.eclipse.core.resources.mapping.ResourceTraversal[], org.eclipse.core.runtime.SubProgressMonitor)
	 */
	protected void execute(FileSystemProvider provider,
			ResourceTraversal[] traversals, IProgressMonitor monitor)
			throws CoreException {
		provider.getOperations().checkin(traversals, isOverwriteIncoming(), monitor);
		if (!isOverwriteIncoming() && hasOutgoingChanges(traversals)) {
			throw new TeamException("Could not put all changes due to conflicts.");
		}

	}

	private boolean hasOutgoingChanges(ResourceTraversal[] traversals) throws CoreException {
		final RuntimeException found = new RuntimeException();
		try {
			FileSystemSubscriber.getInstance().accept(traversals, new IDiffVisitor() {
				public boolean visit(IDiff diff) {
					if (diff instanceof IThreeWayDiff) {
						IThreeWayDiff twd = (IThreeWayDiff) diff;
						if (twd.getDirection() == IThreeWayDiff.OUTGOING || twd.getDirection() == IThreeWayDiff.CONFLICTING) {
							throw found;
						}
					}
					return false;
				}
			});
		} catch (RuntimeException e) {
			if (e == found)
				return true;
			throw e;
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.examples.filesystem.ui.FileSystemOperation#getTaskName()
	 */
	protected String getTaskName() {
		return Policy.bind("PutAction.working"); //$NON-NLS-1$
	}

	/**
	 * Return whether incoming changes should be overwritten.
	 * @return whether incoming changes should be overwritten
	 */
	public boolean isOverwriteIncoming() {
		return overwriteIncoming;
	}

	/**
	 * Set whether incoming changes should be overwritten.
	 * @param overwriteIncoming whether incoming changes should be overwritten
	 */
	public void setOverwriteIncoming(boolean overwriteIncoming) {
		this.overwriteIncoming = overwriteIncoming;
	}

}
