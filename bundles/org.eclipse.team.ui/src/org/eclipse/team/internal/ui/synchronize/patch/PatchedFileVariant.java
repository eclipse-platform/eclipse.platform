/*******************************************************************************
 * Copyright (c) 2009, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ui.synchronize.patch;

import java.io.InputStream;

import org.eclipse.compare.internal.core.patch.FileDiffResult;
import org.eclipse.compare.internal.core.patch.FilePatch2;
import org.eclipse.compare.internal.patch.WorkspacePatcher;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.*;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.variants.IResourceVariant;

public class PatchedFileVariant implements IResourceVariant {

	private FilePatch2 diff;
	private WorkspacePatcher patcher;

	public PatchedFileVariant(WorkspacePatcher patcher, FilePatch2 diff) {
		this.diff = diff;
		this.patcher = patcher;
	}

	@Override
	public byte[] asBytes() {
		// We don't persist the variant between sessions.
		return null;
	}

	@Override
	public String getContentIdentifier() {
		return "(After Patch)"; //$NON-NLS-1$
	}

	@Override
	public String getName() {
		return diff.getPath(patcher.isReversed()).lastSegment();
	}

	@Override
	public IStorage getStorage(IProgressMonitor monitor) throws TeamException {
		return new IStorage() {

			@Override
			public Object getAdapter(Class adapter) {
				return null;
			}

			@Override
			public boolean isReadOnly() {
				return true;
			}

			@Override
			public String getName() {
				return PatchedFileVariant.this.getName();
			}

			@Override
			public IPath getFullPath() {
				return null;
			}

			@Override
			public InputStream getContents() throws CoreException {
				FileDiffResult diffResult = patcher.getDiffResult(diff);
				return diffResult.getPatchedContents();
			}
		};
	}

	@Override
	public boolean isContainer() {
		return false;
	}

	FilePatch2 getDiff() {
		return diff;
	}
}
