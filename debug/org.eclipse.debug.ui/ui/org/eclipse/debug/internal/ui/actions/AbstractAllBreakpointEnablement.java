/*******************************************************************************
 * Copyright (c) 2024, 2025 IBM Corporation and others.
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
package org.eclipse.debug.internal.ui.actions;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IBreakpointManager;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.preferences.IDebugPreferenceConstants;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.IActionDelegate2;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

public abstract class AbstractAllBreakpointEnablement
		implements IViewActionDelegate, IActionDelegate2, IWorkbenchWindowActionDelegate {

	private IAction fAction;


	/**
	 * Needed for reflective creation
	 */

	@Override
	public void dispose() {
		fAction = null;
	}

	@Override
	public void init(IAction action) {
		fAction = action;
	}

	/**
	 * Returns this delegate's action.
	 *
	 * @return the underlying <code>IAction</code>
	 */
	protected IAction getAction() {
		return fAction;
	}

	@Override
	public void runWithEvent(IAction action, Event event) {
		run(action);
	}

	@Override
	public void init(IViewPart view) {
		initialize();
		update();
	}

	@Override
	public void init(IWorkbenchWindow window) {
		initialize();
		update();
	}

	/**
	 * Initializes any listeners, etc.
	 */
	protected abstract void initialize();

	/**
	 * Update enablement.
	 */
	protected void update() {
		IAction action = getAction();
		if (action != null) {
			action.setEnabled(isEnabled());
		}
	}

	@Override
	public void selectionChanged(IAction action, ISelection s) {
		// do nothing
	}

	@Override
	public void run(IAction action) {

	}

	protected abstract boolean isEnabled();

	/**
	 * Schedules the enablement operation for breakpoints
	 *
	 * @param jobName          Name of the job
	 * @param enablementStatus <code>true</code> for enabling all breakpoints;
	 *                         <code>false</code> for disabling all breakpoints;
	 */
	protected void scheduleEnablement(String jobName, boolean enablementStatus) {
		final IBreakpoint[] breakpoints = getBreakpoints();
		new Job(jobName) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					for (IBreakpoint breakpoint : breakpoints) {
						breakpoint.setEnabled(enablementStatus);
					}
				} catch (CoreException e) {
					DebugUIPlugin.log(e);
					return Status.CANCEL_STATUS;
				}
				return Status.OK_STATUS;
			}
		}.schedule();
	}
	protected IBreakpoint[] getBreakpoints() {
		IBreakpointManager breakpointManager = DebugPlugin.getDefault().getBreakpointManager();
		return breakpointManager.getBreakpoints();
	}

	/**
	 * Update the enablement status of every breakpoints
	 *
	 * @param enablementStatus <code>true</code> for enabling all breakpoints;
	 *                         <code>false</code> for disabling all breakpoints;
	 */
	protected void updateBreakpoints(boolean enablementStatus) {
		IBreakpoint[] breakpoints = getBreakpoints();
		if (breakpoints.length < 1) {
			return;
		}
		IWorkbenchWindow window = DebugUIPlugin.getActiveWorkbenchWindow();
		if (window == null) {
			return;
		}
		IPreferenceStore store = DebugUIPlugin.getDefault().getPreferenceStore();
		boolean prompt = store
				.getBoolean(enablementStatus ? IDebugPreferenceConstants.PREF_PROMPT_ENABLE_ALL_BREAKPOINTS
						: IDebugPreferenceConstants.PREF_PROMPT_DISABLE_ALL_BREAKPOINTS);
		boolean proceed = true;
		if (prompt) {
			MessageDialogWithToggle mdwt = MessageDialogWithToggle.openYesNoQuestion(window.getShell(),
					enablementStatus ? ActionMessages.EnableAllBreakpointsAction_0
							: ActionMessages.DisableAllBreakPointsAction_0,
					enablementStatus ? ActionMessages.EnableAllBreakpointsAction_1
							: ActionMessages.DisableAllBreakPointsAction_1,
					enablementStatus ? ActionMessages.EnableAllBreakpointsAction_3
							: ActionMessages.DisableAllBreakPointsAction_2,
					!prompt, null, null);

			if (mdwt.getReturnCode() != IDialogConstants.YES_ID) {
				proceed = false;
			} else {
				store.setValue(
						enablementStatus ? IDebugPreferenceConstants.PREF_PROMPT_ENABLE_ALL_BREAKPOINTS
								: IDebugPreferenceConstants.PREF_PROMPT_DISABLE_ALL_BREAKPOINTS,
						!mdwt.getToggleState());
			}
		}
		if (proceed) {
			if (enablementStatus) {
				scheduleEnablement(ActionMessages.EnableAllBreakpointsAction_0, true);
			} else {
				scheduleEnablement(ActionMessages.DisableAllBreakPointsAction_0, false);
			}
		}
	}
}
