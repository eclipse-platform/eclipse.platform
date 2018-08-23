/*******************************************************************************
 * Copyright (c) 2006, 2018 IBM Corporation and others.
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
package org.eclipse.team.internal.ccvs.core.filehistory;

import org.eclipse.core.filesystem.URIUtil;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.team.internal.ccvs.core.ICVSFile;
import org.eclipse.team.internal.ccvs.core.resources.CVSWorkspaceRoot;
import org.eclipse.team.internal.core.history.LocalFileRevision;

/**
 * This subclass is used exclusively for displaying a LocalFileRevision
 * in a CVSHistoryPage. This class was required to link local file revisions
 * to the CVSHistoryPageSource through the use of the adapter mechanism.
 *
 * @since 3.2
 */
public class CVSLocalFileRevision extends LocalFileRevision implements IAdaptable {

	public CVSLocalFileRevision(IFile file) {
		super(file);
	}

	public CVSLocalFileRevision(IFileState fileState) {
		super(fileState);
	}

	public <T> T getAdapter(Class<T> adapter) {
		if (adapter == ICVSFile.class)
			return adapter.cast(CVSWorkspaceRoot.getCVSFileFor(ResourcesPlugin.getWorkspace().getRoot().getFile(URIUtil.toPath(getURI()))));
		
		return null;
	}


}
