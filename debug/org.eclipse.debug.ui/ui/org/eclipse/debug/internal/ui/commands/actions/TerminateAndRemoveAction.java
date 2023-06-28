/*******************************************************************************
 * Copyright (c) 2006, 2013 IBM Corporation and others.
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

import org.eclipse.core.runtime.IStatus;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.IRequest;
import org.eclipse.debug.core.commands.ITerminateHandler;
import org.eclipse.debug.internal.core.commands.Request;
import org.eclipse.debug.internal.ui.DebugPluginImages;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.IInternalDebugUIConstants;
import org.eclipse.debug.internal.ui.actions.ActionMessages;
import org.eclipse.debug.internal.ui.views.DebugUIViewsMessages;
import org.eclipse.debug.ui.actions.DebugCommandAction;
import org.eclipse.debug.ui.contexts.DebugContextEvent;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

/**
 * Terminate and remove action.
 *
 * @since 3.3
 */
public class TerminateAndRemoveAction extends DebugCommandAction {

	/**
	 * Whether the target can be terminated. The action is always enabled,
	 * but does not always need to terminate the target first.
	 */
	private boolean fCanTerminate = false;

	/**
	 * Local copy of part, possibly null
	 */
	private IWorkbenchPart fMyPart = null;

	@Override
	public String getText() {
		return ActionMessages.TerminateAndRemoveAction_0;
	}

	@Override
	public String getHelpContextId() {
		return "org.eclipse.debug.ui.terminate_and_remove_action_context"; //$NON-NLS-1$
	}

	@Override
	public String getId() {
		return "org.eclipse.debug.ui.debugview.popupMenu.terminateAndRemove"; //$NON-NLS-1$
	}

	@Override
	public String getToolTipText() {
		return ActionMessages.TerminateAndRemoveAction_3;
	}

	@Override
	public ImageDescriptor getDisabledImageDescriptor() {
		return DebugPluginImages.getImageDescriptor(IInternalDebugUIConstants.IMG_DLCL_TERMINATE_AND_REMOVE);
	}

	@Override
	public ImageDescriptor getHoverImageDescriptor() {
		return DebugPluginImages.getImageDescriptor(IInternalDebugUIConstants.IMG_ELCL_TERMINATE_AND_REMOVE);
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		return DebugPluginImages.getImageDescriptor(IInternalDebugUIConstants.IMG_ELCL_TERMINATE_AND_REMOVE);
	}

	@Override
	protected Class<ITerminateHandler> getCommandType() {
		return ITerminateHandler.class;
	}

	@Override
	public void debugContextChanged(DebugContextEvent event) {
		boolean isAllTerminated = true;
		ISelection context = event.getContext();
		if (context instanceof IStructuredSelection) {
			for (Object element : ((IStructuredSelection)context)) {
				if (!isTerminated(element)) {
					isAllTerminated = false;
					break;
				}
			}
		}
		// IF all elements are terminated, we don't need to query the terminate handler, just
		// enable the action, which whill just remove the terminated launches (bug 324959).
		fCanTerminate = !isAllTerminated;
		if (isAllTerminated) {
			setEnabled(true);
		} else {
			super.debugContextChanged(event);
		}
	}

	protected boolean isTerminated(Object element) {
		ILaunch launch = DebugUIPlugin.getLaunch(element);
		if (launch != null) {
			return launch.isTerminated();
		}
		return false;
	}


	@Override
	protected void postExecute(IRequest request, Object[] targets) {
		IStatus status = request.getStatus();
		if(status == null || status.isOK()) {
			for (Object target : targets) {
				ILaunch launch = DebugUIPlugin.getLaunch(target);
				if (launch != null) {
					DebugPlugin.getDefault().getLaunchManager().removeLaunch(launch);
				}
			}
		}
	}

	@Override
	public void runWithEvent(Event event) {
		if (fCanTerminate) {
			IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
			if (window != null) {
				if (!MessageDialog.openQuestion(window.getShell(), DebugUIViewsMessages.LaunchView_Terminate_and_Remove_1, DebugUIViewsMessages.LaunchView_Terminate_and_remove_selected__2)) {
					return;
				}
			}
			super.runWithEvent(event);
		} else {
			// don't terminate, just remove
			// TODO: make #getContext() API in next release
			ISelection sel = null;
			if (fMyPart != null) {
				sel = getDebugContextService().getActiveContext(fMyPart.getSite().getId());
			} else {
				sel = getDebugContextService().getActiveContext();
			}
			if (sel instanceof IStructuredSelection) {
				IStructuredSelection ss = (IStructuredSelection) sel;
				postExecute(new Request(), ss.toArray());
			}
		}
	}

	@Override
	public void init(IWorkbenchPart part) {
		super.init(part); // TODO: if #getContext() was API, this would not be needed
		fMyPart = part;
	}
}
