/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
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

package org.eclipse.debug.internal.ui.elements.adapters;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.model.IMemoryBlockRetrieval;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IPresentationContext;
import org.eclipse.debug.internal.ui.viewers.provisional.AsynchronousContentAdapter;
import org.eclipse.debug.ui.IDebugUIConstants;

public class MemoryRetrievalContentAdapter extends AsynchronousContentAdapter{

	@Override
	protected Object[] getChildren(Object parent, IPresentationContext context) throws CoreException {
		String id = context.getId();
		if (id.equals(IDebugUIConstants.ID_MEMORY_VIEW))
		{
			if (parent instanceof IMemoryBlockRetrieval)
			{
				return DebugPlugin.getDefault().getMemoryBlockManager().getMemoryBlocks((IMemoryBlockRetrieval)parent);
			}
		}
		return EMPTY;
	}

	@Override
	protected boolean hasChildren(Object element, IPresentationContext context) throws CoreException {
		String id = context.getId();
		if (id.equals(IDebugUIConstants.ID_MEMORY_VIEW))
		{
			if (element instanceof IMemoryBlockRetrieval)
			{
				if (((IMemoryBlockRetrieval)element).supportsStorageRetrieval())
					return DebugPlugin.getDefault().getMemoryBlockManager().getMemoryBlocks((IMemoryBlockRetrieval)element).length > 0;
			}
		}
		return false;
	}

	@Override
	protected boolean supportsPartId(String id) {
		return id.equals(IDebugUIConstants.ID_MEMORY_VIEW);
	}

}
