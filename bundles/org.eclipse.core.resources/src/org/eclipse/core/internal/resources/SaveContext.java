/*******************************************************************************
 * Copyright (c) 2000, 2022 IBM Corporation and others.
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
 *     Christoph Läubrich - Issue #77 - SaveManager access the ResourcesPlugin.getWorkspace at init phase
 *******************************************************************************/
package org.eclipse.core.internal.resources;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ISaveContext;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;

public class SaveContext implements ISaveContext {
	protected String pluginId;
	protected int kind;
	protected boolean needDelta;
	protected boolean needSaveNumber;
	protected SafeFileTable fileTable;
	protected int previousSaveNumber;
	protected IProject project;
	private Workspace workspace;

	protected SaveContext(String pluginId, int kind, IProject project, Workspace workspace) throws CoreException {
		this.kind = kind;
		this.project = project;
		this.pluginId = pluginId;
		needDelta = false;
		needSaveNumber = false;
		this.workspace = workspace;
		fileTable = new SafeFileTable(pluginId, workspace);
		previousSaveNumber = getWorkspace().getSaveManager().getSaveNumber(pluginId);
	}

	public void commit() throws CoreException {
		if (needSaveNumber) {
			IPath oldLocation = getWorkspace().getMetaArea().getSafeTableLocationFor(pluginId);
			getWorkspace().getSaveManager().setSaveNumber(pluginId, getSaveNumber());
			fileTable.setLocation(getWorkspace().getMetaArea().getSafeTableLocationFor(pluginId));
			fileTable.save();
			oldLocation.toFile().delete();
		}
	}

	/**
	 * @see ISaveContext
	 */
	@Override
	public IPath[] getFiles() {
		return getFileTable().getFiles();
	}

	protected SafeFileTable getFileTable() {
		return fileTable;
	}

	/**
	 * @see ISaveContext
	 */
	@Override
	public int getKind() {
		return kind;
	}

	public String getPluginId() {
		return pluginId;
	}

	/**
	 * @see ISaveContext
	 */
	@Override
	public int getPreviousSaveNumber() {
		return previousSaveNumber;
	}

	/**
	 * @see ISaveContext
	 */
	@Override
	public IProject getProject() {
		return project;
	}

	/**
	 * @see ISaveContext
	 */
	@Override
	public int getSaveNumber() {
		int result = getPreviousSaveNumber() + 1;
		return result > 0 ? result : 1;
	}

	protected Workspace getWorkspace() {
		return workspace;
	}

	public boolean isDeltaNeeded() {
		return needDelta;
	}

	/**
	 * @see ISaveContext
	 */
	@Override
	public IPath lookup(IPath file) {
		return getFileTable().lookup(file);
	}

	/**
	 * @see ISaveContext
	 */
	@Override
	public void map(IPath file, IPath location) {
		getFileTable().map(file, location);
	}

	/**
	 * @see ISaveContext
	 */
	@Override
	public void needDelta() {
		needDelta = true;
	}

	/**
	 * @see ISaveContext
	 */
	@Override
	public void needSaveNumber() {
		needSaveNumber = true;
	}
}
