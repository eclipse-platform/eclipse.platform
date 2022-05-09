/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package org.eclipse.core.internal.filesystem.memory;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.model.IWorkbenchAdapter;

/**
 * 
 */
public class WorkbenchMemoryNode implements IWorkbenchAdapter {

	@Override
	public Object[] getChildren(Object parent) {
		try {
			return ((MemoryFileStore) parent).childStores(EFS.NONE, null);
		} catch (CoreException e) {
			return new Object[0];
		}
	}

	@Override
	public ImageDescriptor getImageDescriptor(Object object) {
		return null;
	}

	@Override
	public String getLabel(Object o) {
		return ((MemoryFileStore) o).getName();
	}

	@Override
	public Object getParent(Object o) {
		return ((MemoryFileStore) o).getParent();
	}

}
