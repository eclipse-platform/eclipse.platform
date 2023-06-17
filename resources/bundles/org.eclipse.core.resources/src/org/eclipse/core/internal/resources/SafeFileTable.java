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
 *     James Blackburn (Broadcom Corp.) - ongoing development
 *     Christoph Läubrich - Issue #77 - SaveManager access the ResourcesPlugin.getWorkspace at init phase
 *******************************************************************************/
package org.eclipse.core.internal.resources;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.Set;
import org.eclipse.core.internal.utils.Messages;
import org.eclipse.core.resources.IResourceStatus;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;

/**
 * Represents a table of keys and paths used by a plugin to maintain its
 * configuration files' names.
 */
public class SafeFileTable {
	protected IPath location;
	protected Properties table;
	private Workspace workspace;

	public SafeFileTable(String pluginId, Workspace workspace) throws CoreException {
		this.workspace = workspace;
		location = getWorkspace().getMetaArea().getSafeTableLocationFor(pluginId);
		restore();
	}

	public IPath[] getFiles() {
		Set<Object> set = table.keySet();
		String[] keys = set.toArray(new String[set.size()]);
		IPath[] files = new IPath[keys.length];
		for (int i = 0; i < keys.length; i++)
			files[i] = IPath.fromOSString(keys[i]);
		return files;
	}

	protected Workspace getWorkspace() {
		return workspace;
	}

	public IPath lookup(IPath file) {
		String result = table.getProperty(file.toOSString());
		return result == null ? null : IPath.fromOSString(result);
	}

	public void map(IPath file, IPath aLocation) {
		if (aLocation == null)
			table.remove(file);
		else
			table.setProperty(file.toOSString(), aLocation.toOSString());
	}

	public void restore() throws CoreException {
		java.io.File target = location.toFile();
		table = new Properties();
		if (!target.exists())
			return;
		try (FileInputStream input = new FileInputStream(target)) {
			table.load(input);
		} catch (IOException e) {
			String message = Messages.resources_exSafeRead;
			throw new ResourceException(IResourceStatus.INTERNAL_ERROR, null, message, e);
		}
	}

	public void save() throws CoreException {
		java.io.File target = location.toFile();
		try (FileOutputStream output = new FileOutputStream(target)) {
			table.store(output, "safe table"); //$NON-NLS-1$
		} catch (IOException e) {
			String message = Messages.resources_exSafeSave;
			throw new ResourceException(IResourceStatus.INTERNAL_ERROR, null, message, e);
		}
	}

	public void setLocation(IPath location) {
		if (location != null)
			this.location = location;
	}
}
