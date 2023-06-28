/*******************************************************************************
 * Copyright (c) 2000, 2018 IBM Corporation and others.
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
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchMode;
import org.eclipse.debug.core.model.IDebugElement;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.internal.core.IInternalDebugCoreConstants;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.IDebugHelpContextIds;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.debug.ui.ILaunchGroup;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.SelectionListenerAction;

/**
 * Adds the selected launch configuration to the launch favorites.
 */
public class AddToFavoritesAction extends SelectionListenerAction {

	private ILaunchConfiguration fConfiguration = null;
	private String fMode =null;
	private ILaunchGroup fGroup = null;

	/**
	 * Constructs a new action.
	 */
	public AddToFavoritesAction() {
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
		setGroup(null);
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
					ILaunchGroup group= DebugUITools.getLaunchGroup(configuration, getMode());
					if (group == null) {
						return false;
					}
					setGroup(group);
					setLaunchConfiguration(configuration);
					setMode(launch.getLaunchMode());
					setText(MessageFormat.format(ActionMessages.AddToFavoritesAction_1, new Object[] { DebugUIPlugin.removeAccelerators(getGroup().getLabel()) }));
				}
			}
		}

		// Disable the action if the launch config is private
		ILaunchConfiguration config = getLaunchConfiguration();
		if (config == null) {
			return false;
		}
		if (DebugUITools.isPrivate(config)) {
				return false;
		}

		if (getGroup() != null) {
			try {
				List<String> groups = config.getAttribute(IDebugUIConstants.ATTR_FAVORITE_GROUPS, (List<String>) null);
				if (groups != null) {
					return !groups.contains(getGroup().getIdentifier());
				}
				return true;
			} catch (CoreException e) {
			}

		}

		return false;
	}

	/**
	 * Allows the underlying <code>ILaunchConfiguration</code> to be set
	 * @param configuration the new configuration to set
	 */
	protected void setLaunchConfiguration(ILaunchConfiguration configuration) {
		fConfiguration = configuration;
	}

	/**
	 * Returns the underlying <code>ILaunchConfiguration</code>
	 * @return the underlying <code>ILaunchConfiguration</code>
	 */
	protected ILaunchConfiguration getLaunchConfiguration() {
		return fConfiguration;
	}

	/**
	 * Sets the mode this action applies to
	 * @param mode the modes to set
	 */
	protected void setMode(String mode) {
		fMode = mode;
	}

	/**
	 * Returns the mode this action applies to
	 * @return the {@link ILaunchMode} this action applies to
	 */
	protected String getMode() {
		return fMode;
	}

	/**
	 * Sets the <code>ILaunchGroup</code> this action applies to
	 * @param group the new <code>ILaunchGroup</code>
	 */
	protected void setGroup(ILaunchGroup group) {
		fGroup = group;
	}

	/**
	 * Returns the underlying <code>ILaunchGroup</code>
	 * @return the underlying <code>ILaunchGroup</code>
	 */
	protected ILaunchGroup getGroup() {
		return fGroup;
	}

	/**
	 * @see org.eclipse.jface.action.IAction#run()
	 */
	@Override
	public void run() {
		final CoreException[] ex = new CoreException[1];
		BusyIndicator.showWhile(DebugUIPlugin.getStandardDisplay(), () -> {
			try {
				List<String> list = getLaunchConfiguration().getAttribute(IDebugUIConstants.ATTR_FAVORITE_GROUPS, (List<String>) null);
				if (list == null) {
					list = new ArrayList<>();
				}
				list.add(getGroup().getIdentifier());
				ILaunchConfigurationWorkingCopy copy = getLaunchConfiguration().getWorkingCopy();
				copy.setAttribute(IDebugUIConstants.ATTR_FAVORITE_GROUPS, list);
				copy.doSave();
				setEnabled(false);
			} catch (CoreException e) {
				ex[0] = e;
			}
		});
		if (ex[0] != null) {
			DebugUIPlugin.errorDialog(DebugUIPlugin.getShell(), ActionMessages.AddToFavoritesAction_2, ActionMessages.AddToFavoritesAction_3, ex[0].getStatus()); //
		}
	}

}
