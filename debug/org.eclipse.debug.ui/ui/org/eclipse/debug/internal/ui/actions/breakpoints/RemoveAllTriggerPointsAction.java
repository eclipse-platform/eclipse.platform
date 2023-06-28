/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
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


import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IBreakpointsListener;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.actions.AbstractRemoveAllActionDelegate;
import org.eclipse.debug.internal.ui.actions.ActionMessages;
import org.eclipse.debug.internal.ui.preferences.IDebugPreferenceConstants;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.IWorkbenchWindow;

/**
 * Removes all triggerpoints from the source (markers)
 */
public class RemoveAllTriggerPointsAction extends AbstractRemoveAllActionDelegate implements IBreakpointsListener {

	@Override
	protected boolean isEnabled() {
		return DebugPlugin.getDefault().getBreakpointManager().getTriggerPoints().length > 0;
	}

	@Override
	public void breakpointsAdded(IBreakpoint[] breakpoints) {
		update();
	}

	@Override
	public void breakpointsChanged(IBreakpoint[] breakpoints, IMarkerDelta[] deltas) {
		update();
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
		IWorkbenchWindow window= DebugUIPlugin.getActiveWorkbenchWindow();
		if (window == null) {
			return;
		}
		IPreferenceStore store = DebugUIPlugin.getDefault().getPreferenceStore();
		boolean prompt = store.getBoolean(IDebugPreferenceConstants.PREF_PROMPT_REMOVE_ALL_TRIGGER_BREAKPOINTS);
		boolean proceed = true;
		if (prompt) {
			MessageDialogWithToggle mdwt = MessageDialogWithToggle.openYesNoQuestion(window.getShell(), ActionMessages.RemoveAllTriggerPointsAction_0, ActionMessages.RemoveAllTriggerPointsAction_1, ActionMessages.RemoveAllBreakpointsAction_3, !prompt, null, null);
			if (mdwt.getReturnCode() != IDialogConstants.YES_ID) {
				proceed = false;
			} else {
				store.setValue(IDebugPreferenceConstants.PREF_PROMPT_REMOVE_ALL_TRIGGER_BREAKPOINTS, !mdwt.getToggleState());
			}
		}
		if (proceed) {
			new Job(ActionMessages.RemoveAllTriggerPointsAction_1) {
				@Override
				protected IStatus run(IProgressMonitor monitor) {
					try {
						DebugPlugin.getDefault().getBreakpointManager().removeAllTriggerPoints();
					} catch (CoreException e) {
						DebugUIPlugin.log(e);
						return Status.CANCEL_STATUS;
					}
					return Status.OK_STATUS;
					}
			}.schedule();
		}
	}

}
