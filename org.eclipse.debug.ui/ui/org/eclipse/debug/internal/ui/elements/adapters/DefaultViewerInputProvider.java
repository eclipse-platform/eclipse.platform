/*******************************************************************************
 * Copyright (c) 2007, 2010 Wind River Systems and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Wind River Systems - initial API and implementation
 *     Patrick Chuong (Texas Instruments) - Improve usability of the breakpoint view (Bug 238956)
 *******************************************************************************/
package org.eclipse.debug.internal.ui.elements.adapters;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.internal.ui.model.elements.ViewerInputProvider;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IPresentationContext;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IViewerUpdate;
import org.eclipse.debug.ui.IDebugUIConstants;

/**
 * Default input provider supplies the expression manager as input to the
 * expression view.
 *
 * @since 3.4
 */
public class DefaultViewerInputProvider extends ViewerInputProvider {

	@Override
	protected Object getViewerInput(Object source, IPresentationContext context, IViewerUpdate update) throws CoreException {
		if (IDebugUIConstants.ID_BREAKPOINT_VIEW.equals(context.getId())) {
			DefaultBreakpointsViewInput input = new DefaultBreakpointsViewInput(context);
			return input;
		}

		return DebugPlugin.getDefault().getExpressionManager();
	}

	@Override
	protected boolean supportsContextId(String id) {
		return IDebugUIConstants.ID_EXPRESSION_VIEW.equals(id) ||
			IDebugUIConstants.ID_BREAKPOINT_VIEW.equals(id);
	}

}
