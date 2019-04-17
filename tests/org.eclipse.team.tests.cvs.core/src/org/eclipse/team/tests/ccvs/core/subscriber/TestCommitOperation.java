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
package org.eclipse.team.tests.ccvs.core.subscriber;

import org.eclipse.compare.structuremergeviewer.IDiffElement;
import org.eclipse.core.resources.IResource;
import org.eclipse.team.core.synchronize.SyncInfoSet;
import org.eclipse.team.internal.ccvs.ui.repo.RepositoryManager;
import org.eclipse.team.internal.ccvs.ui.subscriber.WorkspaceCommitOperation;

class TestCommitOperation extends WorkspaceCommitOperation {
	
	private boolean prompted;

	public TestCommitOperation(IDiffElement[] elements, boolean override) {
		super(null, elements, override);
	}

	@Override
	protected boolean canRunAsJob() {
		return false;
	}

	@Override
	protected String promptForComment(RepositoryManager manager, IResource[] resourcesToCommit) {
		return "dummy comment";
	}
	
	@Override
	protected int promptForConflicts(SyncInfoSet syncSet) {
		this.prompted = true;
		return 0; // ok to commit all conflicts
	}
	
	@Override
	protected IResource[] promptForResourcesToBeAdded(RepositoryManager manager, IResource[] unadded) {
		return unadded;
	}

	@Override
	protected boolean promptForConflictHandling(SyncInfoSet syncSet) {
		return true;
	}
	
	public boolean isPrompted() {
		return this.prompted;
	}
}
