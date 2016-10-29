/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.core.history;

import java.util.ArrayList;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFileState;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.history.IFileRevision;
import org.eclipse.team.core.history.provider.FileHistory;
import org.eclipse.team.internal.core.Messages;

public class LocalFileHistory extends FileHistory {

	protected IFile file;
	//used to hold all revisions (changes based on filtering)
	protected IFileRevision[] revisions;
	private final boolean includeCurrent;

	/*
	 * Creates a new CVSFile history that will fetch remote revisions by default.
	 */
	public LocalFileHistory(IFile file, boolean includeCurrent) {
		this.file = file;
		this.includeCurrent = includeCurrent;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.core.history.IFileHistory#getContributors(org.eclipse.team.core.history.IFileRevision)
	 */
	public IFileRevision[] getContributors(IFileRevision revision) {

		IFileRevision[] revisions = getFileRevisions();

		//the predecessor is the file with a timestamp that is the largest timestamp
		//from the set of all timestamps smaller than the root file's timestamp
		IFileRevision fileRevision = null;
		for (int i = 0; i < revisions.length; i++) {
			if (((LocalFileRevision) revisions[i]).isPredecessorOf(revision)) {
				//no revision has been set as of yet
				if (fileRevision == null)
					fileRevision = revisions[i];
				//this revision is a predecessor - now check to see if it comes
				//after the current predecessor, if it does make it the current predecessor
				if (fileRevision != null && revisions[i].getTimestamp() > fileRevision.getTimestamp()) {
					fileRevision = revisions[i];
				}
			}
		}
		if (fileRevision == null)
			return new IFileRevision[0];
		return new IFileRevision[] {fileRevision};
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.core.history.IFileHistory#getFileRevision(java.lang.String)
	 */
	public IFileRevision getFileRevision(String id) {
		if (revisions != null) {
			for (int i = 0; i < revisions.length; i++) {
				IFileRevision revision = revisions[i];
				if (revision.getContentIdentifier().equals(id)) {
					return revision;
				}
			}
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.core.history.IFileHistory#getFileRevisions()
	 */
	public IFileRevision[] getFileRevisions() {
		if (revisions == null)
			return new IFileRevision[0];
		return revisions;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.core.history.IFileHistory#getTargets(org.eclipse.team.core.history.IFileRevision)
	 */
	public IFileRevision[] getTargets(IFileRevision revision) {
		IFileRevision[] revisions = getFileRevisions();
		ArrayList directDescendents = new ArrayList();

		for (int i = 0; i < revisions.length; i++) {
			if (((LocalFileRevision) revisions[i]).isDescendentOf(revision)) {
				directDescendents.add(revisions[i]);
			}
		}
		return (IFileRevision[]) directDescendents.toArray(new IFileRevision[directDescendents.size()]);
	}

	/**
	 * Refreshes the revisions for this local file.
	 *
	 * @param monitor	a progress monitor
	 * @throws TeamException
	 */
	public void refresh(IProgressMonitor monitor) throws TeamException {
		monitor.beginTask(Messages.LocalFileHistory_RefreshLocalHistory/*, file.getProjectRelativePath().toString())*/, 300);
		try {
			// Include the file's current state if and only if the file exists.
			LocalFileRevision currentRevision =
				(includeRevisionForFile() ? new LocalFileRevision(file) : null);
			IFileState[] fileStates = file.getHistory(monitor);
			int numRevisions = fileStates.length + (currentRevision != null ? 1 : 0);
			revisions = new LocalFileRevision[numRevisions];
			for (int i = 0; i < fileStates.length; i++) {
				revisions[i] = new LocalFileRevision(fileStates[i]);
			}
			if (currentRevision != null)
				revisions[fileStates.length] = currentRevision;
		} catch (CoreException e) {
			throw TeamException.asTeamException(e);
		} finally {
			monitor.done();
		}
	}

	private boolean includeRevisionForFile() {
		return file.exists() && includeCurrent;
	}

}
