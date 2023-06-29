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
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IPresentationContext;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IViewerUpdate;
import org.eclipse.debug.ui.IDebugUIConstants;

/**
 * @since 3.3
 */
public class StackFrameContentProvider extends ElementContentProvider {

	@Override
	protected int getChildCount(Object element, IPresentationContext context, IViewerUpdate monitor) throws CoreException {
		return getAllChildren(element, context, monitor).length;
	}

	@Override
	protected Object[] getChildren(Object parent, int index, int length, IPresentationContext context, IViewerUpdate monitor) throws CoreException {
		return getElements(getAllChildren(parent, context, monitor), index, length);
	}

	/**
	 * This method retrieves all of the children for the specified parent given the current context
	 * @param parent the parent ot get the children for
	 * @param context the context for which to get the children for
	 * @param monitor the monitor for progress
	 * @return the collection of children, or an empty collection, never <code>null</code>
	 * @throws CoreException
	 */
	protected Object[] getAllChildren(Object parent, IPresentationContext context, IViewerUpdate monitor) throws CoreException {
		if (parent instanceof IStackFrame) {
			String id = context.getId();
			IStackFrame frame = (IStackFrame) parent;
			if (id.equals(IDebugUIConstants.ID_VARIABLE_VIEW)) {
				return frame.getVariables();
			} else if (id.equals(IDebugUIConstants.ID_REGISTER_VIEW)) {
				return frame.getRegisterGroups();
			}
		} else {
			monitor.cancel();
		}
		return EMPTY;
	}

	@Override
	protected boolean supportsContextId(String id) {
		return id.equals(IDebugUIConstants.ID_VARIABLE_VIEW) || id.equals(IDebugUIConstants.ID_REGISTER_VIEW);
	}

	@Override
	protected boolean hasChildren(Object element, IPresentationContext context, IViewerUpdate monitor) throws CoreException {
		 String id = context.getId();
		IStackFrame frame = (IStackFrame) element;
		if (id.equals(IDebugUIConstants.ID_VARIABLE_VIEW)) {
			return frame.hasVariables();
		} else if (id.equals(IDebugUIConstants.ID_REGISTER_VIEW)) {
			return frame.hasRegisterGroups();
		}
		return false;
	}

}
