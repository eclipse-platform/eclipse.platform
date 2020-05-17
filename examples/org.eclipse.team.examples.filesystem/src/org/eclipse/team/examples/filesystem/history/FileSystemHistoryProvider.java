/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
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

import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.core.history.IFileHistory;
import org.eclipse.team.core.history.IFileRevision;
import org.eclipse.team.core.history.provider.FileHistoryProvider;

public class FileSystemHistoryProvider extends FileHistoryProvider {

	@Override
	public IFileHistory getFileHistoryFor(IResource resource, int flags, IProgressMonitor monitor) {
		return null;
	}

	@Override
	public IFileHistory getFileHistoryFor(IFileStore store, int flags, IProgressMonitor monitor) {
		return null;
	}

	@Override
	public IFileRevision getWorkspaceFileRevision(IResource resource) {
		return null;
	}

}
