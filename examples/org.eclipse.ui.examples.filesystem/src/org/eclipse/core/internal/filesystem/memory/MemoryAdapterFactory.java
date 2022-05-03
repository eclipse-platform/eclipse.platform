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

import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.ui.model.IWorkbenchAdapter;

/**
 * A factory that adapts MemoryTree nodes to WorkbenchAdapter for display
 * in the UI.
 */
public class MemoryAdapterFactory implements IAdapterFactory {
	private Class[] ADAPTER_LIST = new Class[] {IWorkbenchAdapter.class};
	private WorkbenchMemoryNode memoryAdapter = new WorkbenchMemoryNode();

	public Object getAdapter(Object adaptableObject, Class adapterType) {
		if (adaptableObject instanceof MemoryFileStore && adapterType.equals(IWorkbenchAdapter.class))
			return memoryAdapter;
		return null;
	}

	public Class[] getAdapterList() {
		return ADAPTER_LIST;
	}

}
