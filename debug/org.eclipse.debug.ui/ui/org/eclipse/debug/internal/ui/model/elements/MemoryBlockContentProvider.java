/*******************************************************************************
 * Copyright (c) 2006, 2007 IBM Corporation and others.
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
import org.eclipse.debug.internal.ui.viewers.model.provisional.IPresentationContext;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IViewerUpdate;
import org.eclipse.debug.ui.IDebugUIConstants;

/**
 * This content provider is required in order to have selection maintained properly
 * when swtiching between session.  The problem is, when swtich debug session, the memory view reset the input
 * to the viewer.
 *
 * After the input is set, viewer's doInitialRestore is called.  At this time, the elemtns
 * are not mapped in the viewer yet, as a result, the selection cannot be maintained.
 *
 * The viewer tries to restore selection again after elements are added to the view.  This is done
 * in the HasChildrenJob.  However, this job will not get scheduled unless the element provides a content
 * provider adapter.  As a result, the job is never scheduled and the selection cannot be maintained.
 *
 */
public class MemoryBlockContentProvider extends ElementContentProvider {

	@Override
	protected int getChildCount(Object element, IPresentationContext context,
			IViewerUpdate monitor) throws CoreException {
		return 0;
	}

	@Override
	protected Object[] getChildren(Object parent, int index, int length,
			IPresentationContext context, IViewerUpdate monitor)
			throws CoreException {
		return EMPTY;
	}

	@Override
	protected boolean supportsContextId(String id) {
		if (id.equals(IDebugUIConstants.ID_MEMORY_VIEW))
			return true;
		return false;
	}

}
