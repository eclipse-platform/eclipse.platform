/*******************************************************************************
 * Copyright (c) 2003, 2013 IBM Corporation and others.
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
package org.eclipse.debug.core.sourcelookup.containers;

import org.eclipse.core.resources.IContainer;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.sourcelookup.ISourceContainerType;

/**
 * A folder in the workspace.
 * <p>
 * Clients may instantiate this class.
 * </p>
 * @since 3.0
 * @noextend This class is not intended to be subclassed by clients.
 */
public class FolderSourceContainer extends ContainerSourceContainer {

	/**
	 * Unique identifier for the folder source container type
	 * (value <code>org.eclipse.debug.core.containerType.folder</code>).
	 */
	public static final String TYPE_ID = DebugPlugin.getUniqueIdentifier() + ".containerType.folder";	 //$NON-NLS-1$


	/**
	 * Constructs a source container on the given folder.
	 *
	 * @param folder the folder to search for source in
	 * @param subfolders whether to search nested folders
	 */
	public FolderSourceContainer(IContainer folder, boolean subfolders) {
		super(folder, subfolders);
	}

	@Override
	public ISourceContainerType getType() {
		return getSourceContainerType(TYPE_ID);
	}

}
