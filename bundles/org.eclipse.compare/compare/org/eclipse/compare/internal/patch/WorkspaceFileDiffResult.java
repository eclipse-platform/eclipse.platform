/*******************************************************************************
 * Copyright (c) 2007, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.compare.internal.patch;

import java.util.List;

import org.eclipse.compare.internal.core.patch.FileDiffResult;
import org.eclipse.compare.internal.core.patch.FilePatch2;
import org.eclipse.compare.patch.PatchConfiguration;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;

public class WorkspaceFileDiffResult extends FileDiffResult {

	public WorkspaceFileDiffResult(FilePatch2 diff,
			PatchConfiguration configuration) {
		super(diff, configuration);
	}
	
	protected boolean canCreateTarget(IStorage storage) {
		IProject project = getPatcher().getTargetProject(getDiff());
		return project != null && project.isAccessible();
	}
	
	protected boolean targetExists(IStorage storage) {
		IFile file= (IFile)storage;
		return file != null && file.isAccessible();
	}
	
	protected List getLines(IStorage storage, boolean create) {
		IFile file= getTargetFile();
		List lines = LineReader.load(file, create);
		return lines;
	}

	protected Patcher getPatcher() {
		return Patcher.getPatcher(getConfiguration());
	}

	public IFile getTargetFile() {
		return getPatcher().getTargetFile(getDiff());
	}
	
	public void refresh() {
		refresh(Utilities.getReaderCreator(getTargetFile()), null);
	}

	public String getCharset() {
		IFile file = getTargetFile();
		try {
			if (file != null)
				return file.getCharset();
		} catch (CoreException e) {
		}
		return ResourcesPlugin.getEncoding();
	}
}
