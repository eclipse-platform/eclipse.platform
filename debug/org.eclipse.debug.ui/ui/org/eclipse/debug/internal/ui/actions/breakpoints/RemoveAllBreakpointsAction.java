/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
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
package org.eclipse.debug.internal.ui.actions.breakpoints;


import java.util.LinkedHashMap;

import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IBreakpointManager;
import org.eclipse.debug.core.IBreakpointsListener;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.actions.AbstractRemoveAllActionDelegate;
import org.eclipse.debug.internal.ui.actions.ActionMessages;
import org.eclipse.debug.internal.ui.preferences.IDebugPreferenceConstants;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchWindow;

/**
 * Removes all breakpoints from the source (markers) and remove all
 * breakpoints from processes
 */
public class RemoveAllBreakpointsAction extends AbstractRemoveAllActionDelegate implements IBreakpointsListener {

	private Shell fShell;

	@Override
	protected boolean isEnabled() {
		return DebugPlugin.getDefault().getBreakpointManager().hasBreakpoints();
	}

	@Override
	public void breakpointsAdded(IBreakpoint[] breakpoints) {
		update();
	}

	@Override
	public void breakpointsChanged(IBreakpoint[] breakpoints, IMarkerDelta[] deltas) {
	}

	@Override
	public void breakpointsRemoved(IBreakpoint[] breakpoints, IMarkerDelta[] deltas) {
		if (getAction() != null) {
			update();
		}
	}

	@Override
	protected void initialize() {
		DebugPlugin.getDefault().getBreakpointManager().addBreakpointListener(this);
	}

	@Override
	public void dispose() {
		DebugPlugin.getDefault().getBreakpointManager().removeBreakpointListener(this);
		super.dispose();
	}

	@Override
	public void run(IAction action) {
		final IBreakpointManager breakpointManager= DebugPlugin.getDefault().getBreakpointManager();
		final IBreakpoint[] breakpoints = breakpointManager.getBreakpoints();
		if (breakpoints.length < 1) {
			return;
		}
		IWorkbenchWindow window= DebugUIPlugin.getActiveWorkbenchWindow();
		if (window == null) {
			return;
		}
		IPreferenceStore store = DebugUIPlugin.getDefault().getPreferenceStore();
		boolean prompt = store.getBoolean(IDebugPreferenceConstants.PREF_PROMPT_REMOVE_ALL_BREAKPOINTS);
		boolean proceed = true;
		if(prompt) {
			LinkedHashMap<String, Integer> buttonLabelToId = new LinkedHashMap<>();
			buttonLabelToId.put(ActionMessages.RemoveAllBreakpointsAction_4, IDialogConstants.YES_ID);
			buttonLabelToId.put(IDialogConstants.NO_LABEL, IDialogConstants.NO_ID);
			MessageDialogWithToggle mdwt =
					new MessageDialogWithToggle(
							window.getShell(),
							ActionMessages.RemoveAllBreakpointsAction_0,
							null,
							ActionMessages.RemoveAllBreakpointsAction_1,
							MessageDialog.QUESTION,
							buttonLabelToId,
							0,
							ActionMessages.RemoveAllBreakpointsAction_3,
							!prompt
							);
			mdwt.open();
			if (mdwt.getReturnCode() != IDialogConstants.YES_ID) {
				proceed = false;
			}
			else {
				store.setValue(IDebugPreferenceConstants.PREF_PROMPT_REMOVE_ALL_BREAKPOINTS, !mdwt.getToggleState());
			}
		}
		if (proceed) {
			new Job(ActionMessages.RemoveAllBreakpointsAction_2) {
				@Override
				protected IStatus run(IProgressMonitor monitor) {
					try {
						DebugUITools.deleteBreakpoints(breakpoints, fShell, monitor);
					} catch (CoreException e) {
						DebugUIPlugin.log(e);
						return Status.CANCEL_STATUS;
					}
					return Status.OK_STATUS;
				}
			}.schedule();
		}
	}

	@Override
	public void init(IViewPart view) {
		super.init(view);
		fShell= view.getSite().getShell();
	}

	@Override
	public void init(IWorkbenchWindow window) {
		super.init(window);
		fShell= window.getShell();
	}
}
