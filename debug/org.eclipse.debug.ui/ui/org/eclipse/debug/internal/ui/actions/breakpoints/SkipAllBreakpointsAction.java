/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IBreakpointManager;
import org.eclipse.debug.core.IBreakpointManagerListener;
import org.eclipse.debug.internal.ui.DebugPluginImages;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.IDebugHelpContextIds;
import org.eclipse.debug.internal.ui.actions.ActionMessages;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.IActionDelegate2;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.IWorkbenchSiteProgressService;

/**
 * An action which toggles the breakpoint manager's enablement.
 * This causes debug targets which honor the manager's enablement
 * to skip (not suspend for) all breakpoints.
 *
 * This class also implements the window action delegate for the action presented as
 * part of the "Breakpoints" group for the "Run" menu.
 */
public class SkipAllBreakpointsAction extends Action implements IWorkbenchWindowActionDelegate, IActionDelegate2, IBreakpointManagerListener {

	public static final String ACTION_ID = "org.eclipse.debug.ui.actions.SkipAllBreakpoints"; //$NON-NLS-1$
	public static final String ACTION_DEFINITION_ID = "org.eclipse.debug.ui.commands.SkipAllBreakpoints"; //$NON-NLS-1$

	//The real action if this is an action delegate
	private IAction fAction;

	/**
	 * Workbench part or <code>null</code> if not installed in a part
	 */
	private IWorkbenchPart fPart = null;

	public SkipAllBreakpointsAction() {
		super(ActionMessages.SkipAllBreakpointsAction_0, AS_CHECK_BOX);
		setToolTipText(ActionMessages.SkipAllBreakpointsAction_0);
		setDescription(ActionMessages.SkipAllBreakpointsAction_2);
		setImageDescriptor(DebugPluginImages.getImageDescriptor(IDebugUIConstants.IMG_SKIP_BREAKPOINTS));
		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IDebugHelpContextIds.SKIP_ALL_BREAKPOINT_ACTION);
	}

	/**
	 * Constructs an action in the given part.
	 *
	 * @param part the part this action is created for
	 */
	public SkipAllBreakpointsAction(IWorkbenchPart part) {
		this();
		fPart = part;
		setId(ACTION_ID); // set action ID when created programmatically.
		updateActionCheckedState();
	}

	@Override
	public void run(){
		IWorkbenchSiteProgressService progressService = null;
		if (fPart != null) {
			 progressService =  fPart.getSite().
			 	getAdapter(IWorkbenchSiteProgressService.class);
		}
		final boolean enabled = !getBreakpointManager().isEnabled();
		Job job = new Job(getText()) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				if (!monitor.isCanceled()) {
					IBreakpointManager bm = getBreakpointManager();
					bm.setEnabled(enabled);
				}
				return Status.OK_STATUS;
			}
		};
		if (progressService != null) {
			progressService.schedule(job);
		} else {
			job.schedule();
		}
	}

	/**
	 * Updates the action's checked state to be opposite the enabled
	 * state of the breakpoint manager.
	 */
	public void updateActionCheckedState() {
		if (fAction != null) {
			fAction.setChecked(!getBreakpointManager().isEnabled());
		} else {
			setChecked(!getBreakpointManager().isEnabled());
		}
	}

	/**
	 * Returns the global breakpoint manager.
	 *
	 * @return the global breakpoint manager
	 */
	public static IBreakpointManager getBreakpointManager() {
		return DebugPlugin.getDefault().getBreakpointManager();
	}

	@Override
	public void dispose() {
		getBreakpointManager().removeBreakpointManagerListener(this);
	}

	@Override
	public void init(IWorkbenchWindow window) {
		updateActionCheckedState();
		getBreakpointManager().addBreakpointManagerListener(this);
	}

	@Override
	public void run(IAction action) {
		setChecked(action.isChecked());
		run();
		// when run from the workbench window action, need to keep view action state in synch (in case view has been closed)
		String prefKey = IDebugUIConstants.ID_BREAKPOINT_VIEW + '+' + action.getId();
		IPreferenceStore prefStore = DebugUIPlugin.getDefault().getPreferenceStore();
		prefStore.setValue(prefKey, action.isChecked());
	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		fAction = action;
	}

	@Override
	public void breakpointManagerEnablementChanged(boolean enabled) {
		if (fAction != null) {
			fAction.setChecked(!enabled);
		}
	}

	@Override
	public void init(IAction action) {
		fAction = action;
		updateActionCheckedState();
	}

	@Override
	public void runWithEvent(IAction action, Event event) {
		run(action);
	}
}
