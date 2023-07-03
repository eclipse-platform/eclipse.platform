/*******************************************************************************
 * Copyright (c) 2005, 2009 IBM Corporation and others.
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
 *     Pawel Piech (Wind River) - ported PDA Virtual Machine to Java (Bug 261400)
 *******************************************************************************/

package org.eclipse.debug.examples.ui.pda.adapters;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.examples.core.pda.model.PDADebugTarget;
import org.eclipse.debug.examples.core.pda.model.PDAThread;
import org.eclipse.debug.internal.ui.model.elements.ElementContentProvider;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IPresentationContext;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IViewerUpdate;
import org.eclipse.debug.ui.IDebugUIConstants;


public class PDADebugTargetContentProvider extends ElementContentProvider {

	@Override
	protected int getChildCount(Object element, IPresentationContext context, IViewerUpdate monitor) throws CoreException {
		PDAThread thread = ((PDADebugTarget) element).getThread(0);
		if (thread != null) {
			if (thread.hasStackFrames()) {
				return thread.getStackFrames().length;
			}
		}
		return 0;
	}

	@Override
	protected boolean hasChildren(Object element, IPresentationContext context, IViewerUpdate monitor) throws CoreException {
		PDAThread thread = ((PDADebugTarget) element).getThread(0);
		if (thread != null) {
			return thread.hasStackFrames();
		}
		return false;
	}

	@Override
	protected Object[] getChildren(Object parent, int index, int length, IPresentationContext context, IViewerUpdate monitor) throws CoreException {
		if (context.getId().equals(IDebugUIConstants.ID_DEBUG_VIEW)) {
			PDAThread thread = ((PDADebugTarget) parent).getThread(0);
			if (thread != null) {
				return getElements(thread.getStackFrames(), index, length);
			}
		}
		return EMPTY;
	}

	@Override
	protected boolean supportsContextId(String id) {
		return IDebugUIConstants.ID_DEBUG_VIEW.equals(id);
	}

}
