/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
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
package org.eclipse.debug.internal.ui.model.elements;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.model.IMemoryBlock;
import org.eclipse.debug.core.model.IMemoryBlockExtension;
import org.eclipse.debug.internal.ui.DebugPluginImages;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IPresentationContext;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.TreePath;

public class MemoryBlockLabelProvider extends DebugElementLabelProvider {

	@Override
	protected String getLabel(TreePath elementPath,
			IPresentationContext presentationContext, String columnId)
			throws CoreException {
		Object element = elementPath.getLastSegment();

		if (element instanceof IMemoryBlock)
			return getLabel((IMemoryBlock)element);

		return super.getLabel(elementPath, presentationContext, columnId);
	}

	@Override
	protected ImageDescriptor getImageDescriptor(TreePath elementPath,
			IPresentationContext presentationContext, String columnId)
			throws CoreException {

		Object element = elementPath.getLastSegment();

		if (element instanceof IMemoryBlock)
			return DebugPluginImages.getImageDescriptor(IDebugUIConstants.IMG_OBJS_VARIABLE);

		return super.getImageDescriptor(elementPath, presentationContext, columnId);
	}

	/**
	 * @param memoryBlockLabel
	 * @return
	 */
	private String getLabel(IMemoryBlock memoryBlock) {

		String memoryBlockLabel = " "; //$NON-NLS-1$
		if (memoryBlock instanceof IMemoryBlockExtension)
		{
			// simply return the expression without the address
			// do not want to keep track of changes in the address
			if (((IMemoryBlockExtension)memoryBlock).getExpression() != null)
			{
				memoryBlockLabel += ((IMemoryBlockExtension)memoryBlock).getExpression();
			}
		}
		else
		{
			long address = memoryBlock.getStartAddress();
			memoryBlockLabel = Long.toHexString(address);
		}
		return memoryBlockLabel;
	}

}
