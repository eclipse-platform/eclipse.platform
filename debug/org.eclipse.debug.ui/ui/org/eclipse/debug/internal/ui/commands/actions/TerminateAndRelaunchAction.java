/*******************************************************************************
 *  Copyright (c) 2004, 2018 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial implementation
 *******************************************************************************/
package org.eclipse.debug.internal.ui.commands.actions;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.IRequest;
import org.eclipse.debug.core.commands.ITerminateHandler;
import org.eclipse.debug.internal.ui.DebugPluginImages;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.IInternalDebugUIConstants;
import org.eclipse.debug.internal.ui.actions.ActionMessages;
import org.eclipse.debug.internal.ui.actions.RelaunchActionDelegate;
import org.eclipse.debug.internal.ui.launchConfigurations.LaunchConfigurationManager;
import org.eclipse.debug.ui.actions.DebugCommandAction;
import org.eclipse.debug.ui.contexts.DebugContextEvent;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;

/**
 * Action which terminates a launch and then re-launches it.
 */
public class TerminateAndRelaunchAction extends DebugCommandAction {

	@Override
	public void postExecute(IRequest request, final Object[] targets) {
		if (request.getStatus() == null || request.getStatus().isOK()) {
			DebugUIPlugin.getStandardDisplay().asyncExec(() -> {
				// Must be run in the UI thread since the launch can require
				// prompting to proceed
				for (Object target : targets) {
					ILaunch launch = DebugUIPlugin.getLaunch(target);
					if (launch != null) {
						RelaunchActionDelegate.relaunch(launch.getLaunchConfiguration(), launch.getLaunchMode());
					}
				}
			});
		}
	}

	@Override
	protected ISelection getContext() {
		// Convert action context to contain only launch objects (bug 356651).
		ISelection context = super.getContext();
		if (context instanceof IStructuredSelection && !context.isEmpty()) {
			IStructuredSelection ss = (IStructuredSelection)context;
			Set<ILaunch> launches = new HashSet<>(ss.size());
			for (Iterator<Object> itr = ss.iterator(); itr.hasNext();) {
				ILaunch launch = DebugUIPlugin.getLaunch(itr.next());
				if (launch != null) {
					launches.add(launch);
				}
			}
			return new StructuredSelection(launches.toArray());
		}
		return super.getContext();
	}


	@Override
	protected Class<ITerminateHandler> getCommandType() {
		return ITerminateHandler.class;
	}

	@Override
	public void debugContextChanged(DebugContextEvent event) {
		ISelection context = event.getContext();
		if (context instanceof IStructuredSelection) {
			Object[] elements = ((IStructuredSelection)context).toArray();
			for (Object element : elements) {
				if (!canRelaunch(element)) {
					setEnabled(false);
					return;
				}
			}
		}
		super.debugContextChanged(event);
	}

	protected boolean canRelaunch(Object element) {
		ILaunch launch = DebugUIPlugin.getLaunch(element);
		if (launch != null) {
			ILaunchConfiguration configuration = launch.getLaunchConfiguration();
			if (configuration != null) {
				return LaunchConfigurationManager.isVisible(configuration);
			}
		}
		return false;
	}

	@Override
	public String getActionDefinitionId() {
		return ActionMessages.TerminateAndRelaunchAction_0;
	}

	@Override
	public String getHelpContextId() {
		return "org.eclipse.debug.ui.terminate_and_relaunch_action_context"; //$NON-NLS-1$
	}

	@Override
	public String getId() {
		return "org.eclipse.debug.ui.debugview.popupMenu.TerminateAndRelaunch"; //$NON-NLS-1$
	}

	@Override
	public String getText() {
		return ActionMessages.TerminateAndRelaunchAction_3;
	}

	@Override
	public String getToolTipText() {
		return ActionMessages.TerminateAndRelaunchAction_4;
	}

	@Override
	public ImageDescriptor getDisabledImageDescriptor() {
		return DebugPluginImages.getImageDescriptor(IInternalDebugUIConstants.IMG_DLCL_TERMINATE_AND_RELAUNCH);
	}

	@Override
	public ImageDescriptor getHoverImageDescriptor() {
		return DebugPluginImages.getImageDescriptor(IInternalDebugUIConstants.IMG_ELCL_TERMINATE_AND_RELAUNCH);
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		return DebugPluginImages.getImageDescriptor(IInternalDebugUIConstants.IMG_ELCL_TERMINATE_AND_RELAUNCH);
	}
}
