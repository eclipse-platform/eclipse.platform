/*******************************************************************************
 *  Copyright (c) 2000, 2013 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Wind River Systems - added support for IToggleBreakpointsTargetFactory
 *******************************************************************************/
package org.eclipse.debug.internal.ui.actions.breakpoints;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.debug.internal.ui.actions.RetargetAction;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.actions.IToggleBreakpointsTarget;
import org.eclipse.debug.ui.actions.IToggleBreakpointsTargetManager;
import org.eclipse.debug.ui.actions.IToggleBreakpointsTargetManagerListener;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;


/**
 * Retargettable breakpoint action.
 *
 * @since 3.0
 */
public abstract class RetargetBreakpointAction extends RetargetAction implements IToggleBreakpointsTargetManagerListener {

	private IAction fAction;

	@Override
	protected Class<?> getAdapterClass() {
		return IToggleBreakpointsTarget.class;
	}

	@Override
	protected Object getAdapter(IAdaptable adaptable) {
		IToggleBreakpointsTargetManager manager = DebugUITools.getToggleBreakpointsTargetManager();
		IWorkbenchPart activePart = getActivePart();
		if (activePart != null) {
			return manager.getToggleBreakpointsTarget(activePart, getTargetSelection());
		}
		return null;
	}

	@Override
	public void init(IWorkbenchWindow window) {
		super.init(window);
		DebugUITools.getToggleBreakpointsTargetManager().addChangedListener(this);
	}

	@Override
	public void init(IAction action) {
		super.init(action);
		DebugUITools.getToggleBreakpointsTargetManager().addChangedListener(this);
	}

	@Override
	public void dispose() {
		DebugUITools.getToggleBreakpointsTargetManager().removeChangedListener(this);
		super.dispose();
	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		fAction = action;
		super.selectionChanged(action, selection);
	}

	@Override
	public void preferredTargetsChanged() {
		if (fAction != null) {
			IWorkbenchPart activePart = getActivePart();
			if (activePart != null) {
				ISelectionProvider provider = activePart.getSite().getSelectionProvider();
				if (provider != null) {
					ISelection selection = provider.getSelection();
						// Force the toggle target to be refreshed.
						super.clearPart(activePart);
						super.partActivated(activePart);
						super.selectionChanged(fAction, selection);
				}
			}
		}
	}
}
