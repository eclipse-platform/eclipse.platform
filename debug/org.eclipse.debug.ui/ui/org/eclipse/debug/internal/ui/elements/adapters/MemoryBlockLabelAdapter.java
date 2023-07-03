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
import org.eclipse.debug.core.model.IMemoryBlock;
import org.eclipse.debug.core.model.IMemoryBlockExtension;
import org.eclipse.debug.internal.ui.DebugPluginImages;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IPresentationContext;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.jface.resource.ImageDescriptor;

public class MemoryBlockLabelAdapter extends AsynchronousDebugLabelAdapter {

	@Override
	protected ImageDescriptor[] getImageDescriptors(Object element, IPresentationContext context) throws CoreException {
		if (element instanceof IMemoryBlock)
			return new ImageDescriptor[]{DebugPluginImages.getImageDescriptor(IDebugUIConstants.IMG_OBJS_VARIABLE)};

		return new ImageDescriptor[0];
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

	@Override
	protected String[] getLabels(Object element, IPresentationContext context) throws CoreException {
		if (element instanceof IMemoryBlock)
			return new String[]{getLabel((IMemoryBlock)element)};
		return super.getLabels(element, context);
	}
}
