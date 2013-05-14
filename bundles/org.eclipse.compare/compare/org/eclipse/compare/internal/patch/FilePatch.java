/*******************************************************************************
 * Copyright (c) 2009, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.compare.internal.patch;

import org.eclipse.compare.internal.core.patch.FilePatch2;
import org.eclipse.compare.patch.IFilePatch;
import org.eclipse.compare.patch.IFilePatchResult;
import org.eclipse.compare.patch.PatchConfiguration;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;

public class FilePatch extends FilePatch2 implements IFilePatch {

	public FilePatch(IPath oldPath, long oldDate, IPath newPath,
			long newDate) {
		super(oldPath, oldDate, newPath, newDate);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.compare.patch.IFilePatch#apply(org.eclipse.core.resources
	 * .IStorage, org.eclipse.compare.patch.PatchConfiguration,
	 * org.eclipse.core.runtime.IProgressMonitor)
	 */
	public IFilePatchResult apply(IStorage content,
			PatchConfiguration configuration, IProgressMonitor monitor) {
		return apply(content != null ? Utilities.getReaderCreator(content)
				: null, configuration, monitor);
	}

	protected FilePatch2 create(IPath oldPath, long oldDate, IPath newPath,
			long newDate) {
		return new FilePatch(oldPath, oldDate, newPath, newDate);
	}

}
