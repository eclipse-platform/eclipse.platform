/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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
package org.eclipse.debug.internal.ui.launchConfigurations;

import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.IInternalDebugUIConstants;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.ui.model.WorkbenchViewerComparator;

/**
 * provides the implementation for sorting the launch configurations within the
 * Launch Configuration Dialog
 */
public class SortLaunchConfigurationAction extends Action {

	/**
	 * Action identifier for IDebugView#getAction(String)
	 */
	public static final String ID_SORT_ACTION = DebugUIPlugin.getUniqueIdentifier() + ".ID_SORT_ACTION"; //$NON-NLS-1$

	private LaunchConfigurationFilteredTree fTree;

	public SortLaunchConfigurationAction(LaunchConfigurationFilteredTree tree) {
		super(LaunchConfigurationsMessages.SortLaunchConfigurationAction, IAction.AS_CHECK_BOX);
		fTree = tree;
		setChecked(DebugUIPlugin.getDefault().getPreferenceStore()
				.getBoolean(IInternalDebugUIConstants.PREF_LAUNCHCONFIG_SORT_ON_RECENT));
	}

	@Override
	public void run() {
		TreeViewer viewer = fTree.getViewer();
		IPreferenceStore prefStore = DebugUIPlugin.getDefault().getPreferenceStore();
		boolean isSortByRecent = prefStore.getBoolean(IInternalDebugUIConstants.PREF_LAUNCHCONFIG_SORT_ON_RECENT);
		viewer.setComparator(isSortByRecent ? new WorkbenchViewerComparator()
				: new LaunchConfigurationComparator(fTree.getLaunchGroup().getIdentifier()));
		prefStore.setValue(IInternalDebugUIConstants.PREF_LAUNCHCONFIG_SORT_ON_RECENT, !isSortByRecent);
		viewer.refresh();
	}

	@Override
	public String getDescription() {
		return LaunchConfigurationsMessages.SortLaunchConfigurationActionDesc;
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		return DebugUITools.getImageDescriptor(IDebugUIConstants.IMG_LCL_DETAIL_PANE_HIDE);
	}

	@Override
	public String getToolTipText() {
		return LaunchConfigurationsMessages.SortLaunchConfigurationActionDesc;
	}
}
