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

import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.ui.model.IWorkbenchAdapter;

/**
 * A factory that adapts MemoryTree nodes to WorkbenchAdapter for display
 * in the UI.
 */
public class MemoryAdapterFactory implements IAdapterFactory {
	private Class<?>[] ADAPTER_LIST = new Class[] {IWorkbenchAdapter.class};
	private WorkbenchMemoryNode memoryAdapter = new WorkbenchMemoryNode();

	@Override
	public <T> T getAdapter(Object adaptableObject, Class<T> adapterType) {
		if (adaptableObject instanceof MemoryFileStore && adapterType.equals(IWorkbenchAdapter.class))
			return adapterType.cast(memoryAdapter);
		return null;
	}

	@Override
	public Class<?>[] getAdapterList() {
		return ADAPTER_LIST;
	}

}
