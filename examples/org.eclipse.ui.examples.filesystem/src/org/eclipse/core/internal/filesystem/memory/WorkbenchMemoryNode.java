/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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

	public Object[] getChildren(Object parent) {
		try {
			return ((MemoryFileStore) parent).childStores(EFS.NONE, null);
		} catch (CoreException e) {
			return new Object[0];
		}
	}

	public ImageDescriptor getImageDescriptor(Object object) {
		return null;
	}

	public String getLabel(Object o) {
		return ((MemoryFileStore) o).getName();
	}

	public Object getParent(Object o) {
		return ((MemoryFileStore) o).getParent();
	}

}
