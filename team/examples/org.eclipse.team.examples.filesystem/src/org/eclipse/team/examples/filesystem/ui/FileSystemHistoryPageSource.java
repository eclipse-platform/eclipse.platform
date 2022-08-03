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
package org.eclipse.team.examples.filesystem.ui;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.examples.filesystem.FileSystemProvider;
import org.eclipse.team.ui.history.HistoryPageSource;
import org.eclipse.ui.part.Page;

public class FileSystemHistoryPageSource extends HistoryPageSource {

	@Override
	public boolean canShowHistoryFor(Object object) {
		if (object instanceof IResource && ((IResource) object).getType() == IResource.FILE) {
			RepositoryProvider provider = RepositoryProvider.getProvider(((IFile) object).getProject());
			if (provider instanceof FileSystemProvider)
				return true;
		}

		return false;
	}

	@Override
	public Page createPage(Object object) {
		FileSystemHistoryPage page = new FileSystemHistoryPage();
		return page;
	}

}
