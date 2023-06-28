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
package org.eclipse.debug.internal.ui.actions;


import java.text.MessageFormat;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.IDebugElement;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.internal.core.IInternalDebugCoreConstants;
import org.eclipse.debug.internal.ui.DebugPluginImages;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.IDebugHelpContextIds;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.ILaunchGroup;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.SelectionListenerAction;

/**
 * Opens the launch configuration dialog on a single launch configuration, based
 * on the the launch associated with the selected element.
 */
public class EditLaunchConfigurationAction extends SelectionListenerAction {

	private ILaunchConfiguration fConfiguration = null;
	private String fMode = null;
	private boolean fTerminated = false;

	/**
	 * Constructs a new action.
	 */
	public EditLaunchConfigurationAction() {
		super(IInternalDebugCoreConstants.EMPTY_STRING);
		setEnabled(false);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IDebugHelpContextIds.EDIT_LAUNCH_CONFIGURATION_ACTION);
	}

	/**
	 * @see org.eclipse.ui.actions.SelectionListenerAction#updateSelection(org.eclipse.jface.viewers.IStructuredSelection)
	 */
	@Override
	protected boolean updateSelection(IStructuredSelection selection) {
		setLaunchConfiguration(null);
		setMode(null);
		if (selection.size() == 1) {
			Object object = selection.getFirstElement();
			ILaunch launch = null;
			if (object instanceof IAdaptable) {
				launch = ((IAdaptable)object).getAdapter(ILaunch.class);
			}
			if (launch == null) {
				if (object instanceof ILaunch) {
					launch = (ILaunch)object;
				} else if (object instanceof IDebugElement) {
					launch = ((IDebugElement)object).getLaunch();
				} else if (object instanceof IProcess) {
					launch = ((IProcess)object).getLaunch();
				}
			}
			if (launch != null) {
				ILaunchConfiguration configuration = launch.getLaunchConfiguration();
				if (configuration != null) {
					try {
						// The DebugUIPlugin creates stand-in launches with copied configurations
						// while a launch is waiting for a build. These copied configurations
						// have an attribute that points to the config that the user is really
						// launching.
						String underlyingHandle = configuration.getAttribute(DebugUIPlugin.ATTR_LAUNCHING_CONFIG_HANDLE, IInternalDebugCoreConstants.EMPTY_STRING);
						if (underlyingHandle.length() > 0) {
							ILaunchConfiguration underlyingConfig = DebugPlugin.getDefault().getLaunchManager().getLaunchConfiguration(underlyingHandle);
							if (underlyingConfig != null) {
								configuration = underlyingConfig;
							}
						}
					} catch (CoreException e1) {
					}
					setLaunchConfiguration(configuration);
					setMode(launch.getLaunchMode());
					setIsTerminated(launch.isTerminated());
					setText(MessageFormat.format(ActionMessages.EditLaunchConfigurationAction_1, new Object[] { configuration.getName() }));
					ImageDescriptor descriptor = null;
					try {
						descriptor = DebugPluginImages.getImageDescriptor(configuration.getType().getIdentifier());
					} catch (CoreException e) {
						DebugUIPlugin.log(e);
					}
					setImageDescriptor(descriptor);
				}
			}
		}

		// Disable the action if the launch config is private
		ILaunchConfiguration config = getLaunchConfiguration();
		if (config == null) {
			return false;
		}
		return !DebugUITools.isPrivate(config);
	}

	protected void setLaunchConfiguration(ILaunchConfiguration configuration) {
		fConfiguration = configuration;
	}

	protected ILaunchConfiguration getLaunchConfiguration() {
		return fConfiguration;
	}

	protected void setMode(String mode) {
		fMode = mode;
	}

	protected String getMode() {
		return fMode;
	}

	protected boolean isTerminated() {
		return fTerminated;
	}

	protected void setIsTerminated(boolean terminated) {
		fTerminated = terminated;
	}

	/**
	 * @see org.eclipse.jface.action.IAction#run()
	 */
	@Override
	public void run() {
		ILaunchGroup group = DebugUITools.getLaunchGroup(getLaunchConfiguration(), getMode());
		if (group != null) {
			if(isTerminated()) {
				DebugUITools.openLaunchConfigurationDialog(
					DebugUIPlugin.getShell(), getLaunchConfiguration(),
					group.getIdentifier(), null);
			}
			else {
				DebugUIPlugin.openLaunchConfigurationEditDialog(
						DebugUIPlugin.getShell(), getLaunchConfiguration(),
						group.getIdentifier(), null, false);
			}
		}
	}

}
