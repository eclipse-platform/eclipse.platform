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
import org.eclipse.debug.core.sourcelookup.containers.LocalFileStorage;
import org.eclipse.debug.core.sourcelookup.containers.ZipEntryStorage;
import org.eclipse.debug.internal.core.IInternalDebugCoreConstants;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.model.IWorkbenchAdapter;

/**
 * Workbench adapter for source elements.
 *
 * @since 3.0
 */
public class SourceElementWorkbenchAdapter implements IWorkbenchAdapter {
	@Override
	public Object[] getChildren(Object o) {
		return null;
	}
	@Override
	public ImageDescriptor getImageDescriptor(Object o) {
		if (o instanceof LocalFileStorage || o instanceof ZipEntryStorage) {
			return PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_OBJ_FILE);
		}
		return null;
	}
	@SuppressWarnings("resource")
	@Override
	public String getLabel(Object o) {
		if (o instanceof LocalFileStorage) {
			LocalFileStorage storage = (LocalFileStorage) o;
			IPath path = storage.getFullPath();
			return getQualifiedName(path);
		}
		if (o instanceof ZipEntryStorage) {
			ZipEntryStorage storage = (ZipEntryStorage)o;
			StringBuilder buffer = new StringBuilder();
			buffer.append(storage.getZipEntry().getName());
			buffer.append(" - "); //$NON-NLS-1$
			buffer.append(storage.getArchive().getName());
			return buffer.toString();
		}
		return IInternalDebugCoreConstants.EMPTY_STRING;
	}
	@Override
	public Object getParent(Object o) {
		return null;
	}

	public static String getQualifiedName(IPath path) {
		StringBuilder buffer = new StringBuilder();
		String[] segments = path.segments();
		if (segments.length > 0) {
			buffer.append(path.lastSegment());
			if (segments.length > 1) {
				buffer.append(" - "); //$NON-NLS-1$
				if (path.getDevice() != null) {
					buffer.append(path.getDevice());
				}
				for (int i = 0; i < segments.length - 1; i++) {
					buffer.append(File.separatorChar);
					buffer.append(segments[i]);
				}
			}
			return buffer.toString();
		}
		return IInternalDebugCoreConstants.EMPTY_STRING;
	}
}
