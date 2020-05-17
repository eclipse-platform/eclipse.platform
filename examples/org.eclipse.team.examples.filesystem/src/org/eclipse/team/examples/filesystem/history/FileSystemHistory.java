/*******************************************************************************
 * Copyright (c) 2006, 2009 IBM Corporation and others.
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
package org.eclipse.team.examples.filesystem.history;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFileState;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.history.IFileRevision;
import org.eclipse.team.core.history.provider.FileHistory;
import org.eclipse.team.core.variants.IResourceVariant;
import org.eclipse.team.examples.filesystem.FileSystemProvider;
import org.eclipse.team.examples.filesystem.subscriber.FileSystemResourceVariant;
import org.eclipse.team.internal.core.history.LocalFileRevision;

public class FileSystemHistory extends FileHistory {

	private IFile file;
	protected IFileRevision[] revisions;

	public FileSystemHistory(IFile file) {
		this.file = file;
	}

	@Override
	public IFileRevision[] getContributors(IFileRevision revision) {
		return null;
	}

	@Override
	public IFileRevision getFileRevision(String id) {
		return null;
	}

	@Override
	public IFileRevision[] getFileRevisions() {
		return revisions;
	}

	@Override
	public IFileRevision[] getTargets(IFileRevision revision) {
		return null;
	}

	public void refresh(IProgressMonitor monitor) {
		try {
			RepositoryProvider provider = RepositoryProvider.getProvider(file.getProject());
			if (provider != null && provider instanceof FileSystemProvider) {
				FileSystemProvider fileProvider = (FileSystemProvider) provider;
				IResourceVariant resVar = fileProvider.getResourceVariant(file);
				if (resVar instanceof FileSystemResourceVariant) {
					FileSystemResourceVariant resVarF = (FileSystemResourceVariant) resVar;
					java.io.File javaFile = resVarF.getFile();
					//Get local history items
					IFileState[] states = file.getHistory(monitor);
					revisions = new IFileRevision[states.length + 1];
					int i = 0;
					for (; i < states.length; i++) {
						revisions[i] = new LocalFileRevision(states[i]);
					}
					revisions[i] = new FileSystemFileRevision(javaFile);
				}
			}
		} catch (TeamException e) {
			// ignore
		} catch (CoreException e) {
			// ignore
		}
	}

}
