/*******************************************************************************
 * Copyright (c) 2000, 2014 IBM Corporation and others.
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
package org.eclipse.debug.internal.ui.commands.actions;

import java.util.Arrays;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.ILaunchesListener2;
import org.eclipse.debug.core.commands.ITerminateHandler;
import org.eclipse.debug.internal.ui.DebugPluginImages;
import org.eclipse.debug.internal.ui.IInternalDebugUIConstants;
import org.eclipse.debug.internal.ui.actions.ActionMessages;
import org.eclipse.debug.ui.actions.DebugCommandAction;
import org.eclipse.debug.ui.contexts.DebugContextEvent;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;

/**
 * Terminates all launches.
 *
 * @since 3.3
 */
public class TerminateAllAction extends DebugCommandAction implements ILaunchesListener2 {

	@Override
	protected ISelection getContext() {
		return new StructuredSelection(getLaunchManager().getLaunches());
	}

	@Override
	public void dispose() {
		getLaunchManager().removeLaunchListener(this);
		super.dispose();
	}

	private void attachSelfToLaunchManager() {
		ILaunchManager launchManager = getLaunchManager();
		launchManager.addLaunchListener(this);
		setEnabled(canTerminate());
	}

	private ILaunchManager getLaunchManager() {
		return DebugPlugin.getDefault().getLaunchManager();
	}

	@Override
	public String getHelpContextId() {
		return "org.eclipse.debug.ui.terminate_all_action_context"; //$NON-NLS-1$
	}

	@Override
	public String getId() {
		return "org.eclipse.debug.ui.debugview.popupMenu.terminateAll"; //$NON-NLS-1$
	}

	@Override
	public String getText() {
		return ActionMessages.TerminateAllAction_2;
	}

	@Override
	public String getToolTipText() {
		return ActionMessages.TerminateAllAction_3;
	}

	@Override
	public ImageDescriptor getDisabledImageDescriptor() {
		return DebugPluginImages
				.getImageDescriptor(IInternalDebugUIConstants.IMG_DLCL_TERMINATE_ALL);
	}

	@Override
	public ImageDescriptor getHoverImageDescriptor() {
		return DebugPluginImages
				.getImageDescriptor(IInternalDebugUIConstants.IMG_ELCL_TERMINATE_ALL);
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		return DebugPluginImages
				.getImageDescriptor(IInternalDebugUIConstants.IMG_ELCL_TERMINATE_ALL);
	}

	@Override
	protected Class<ITerminateHandler> getCommandType() {
		return ITerminateHandler.class;
	}

	@Override
	public void launchesTerminated(ILaunch[] launches) {
		setEnabled(canTerminate());
	}

	@Override
	public void launchesAdded(ILaunch[] launches) {
		setEnabled(canTerminate());
	}

	@Override
	public void launchesChanged(ILaunch[] launches) {
	}

	@Override
	public void launchesRemoved(ILaunch[] launches) {
		setEnabled(canTerminate());
	}

	@Override
	public void init(IWorkbenchPart part) {
		super.init(part);
		attachSelfToLaunchManager();
	}

	/**
	 * Initializes this action for the given workbench window.
	 *
	 * @param window the workbench window that this action is for
	 */
	@Override
	public void init(IWorkbenchWindow window) {
		super.init(window);
		attachSelfToLaunchManager();
	}

	@Override
	protected boolean getInitialEnablement() {
		return canTerminate();
	}

	private boolean canTerminate() {
		return Arrays.stream(getLaunchManager().getLaunches()).anyMatch(ILaunch::canTerminate);
	}

	@Override
	public void runWithEvent(Event event) {
		for (ILaunch l : getLaunchManager().getLaunches()) {
			try {
				if (l.canTerminate()) {
					l.terminate();
				}
			} catch (DebugException e) {
				ILog.get().log(Status.warning("Unable to terminate launch: " + e.getMessage(), e)); //$NON-NLS-1$
			}
		}
		setEnabled(canTerminate());
	}

	@Override
	public void debugContextChanged(DebugContextEvent event) {
		setEnabled(canTerminate());
	}
}
