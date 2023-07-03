/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
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
package org.eclipse.debug.internal.ui.sourcelookup;

import java.io.File;

import org.eclipse.core.runtime.IPath;
import org.eclipse.debug.core.sourcelookup.containers.ArchiveSourceContainer;
import org.eclipse.debug.core.sourcelookup.containers.DirectorySourceContainer;
import org.eclipse.debug.core.sourcelookup.containers.ExternalArchiveSourceContainer;
import org.eclipse.debug.core.sourcelookup.containers.FolderSourceContainer;
import org.eclipse.debug.internal.core.IInternalDebugCoreConstants;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.model.IWorkbenchAdapter;

/**
 * Workbench adapter for standard source containers.
 *
 * @since 3.0
 */
public class SourceContainerWorkbenchAdapter implements IWorkbenchAdapter {
	@Override
	public Object[] getChildren(Object o) {
		return null;
	}
	@Override
	public ImageDescriptor getImageDescriptor(Object object) {
		return null;
	}
	@Override
	public String getLabel(Object o) {
		if (o instanceof DirectorySourceContainer) {
			DirectorySourceContainer container = (DirectorySourceContainer) o;
			File file = container.getDirectory();
			IPath path = IPath.fromOSString(file.getAbsolutePath());
			return SourceElementWorkbenchAdapter.getQualifiedName(path);
		}
		if (o instanceof FolderSourceContainer) {
			FolderSourceContainer container = (FolderSourceContainer) o;
			return SourceElementWorkbenchAdapter.getQualifiedName(container.getContainer().getFullPath());
		}
		if (o instanceof ArchiveSourceContainer) {
			ArchiveSourceContainer container = (ArchiveSourceContainer)o;
			return SourceElementWorkbenchAdapter.getQualifiedName(container.getFile().getFullPath());
		}
		if (o instanceof ExternalArchiveSourceContainer) {
			ExternalArchiveSourceContainer container = (ExternalArchiveSourceContainer)o;
			IPath path = IPath.fromOSString(container.getName());
			return SourceElementWorkbenchAdapter.getQualifiedName(path);
		}
		return IInternalDebugCoreConstants.EMPTY_STRING;
	}
	@Override
	public Object getParent(Object o) {
		return null;
	}
}
