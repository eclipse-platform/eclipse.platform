/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
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
import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.TreeViewer;

/**
 * Provides a collapse all button for the launch configuration viewer toolbar
 * @since 3.2
 */
public class CollapseAllLaunchConfigurationAction extends Action {

	/**
	 * Action identifier for IDebugView#getAction(String)
	 */
	public static final String ID_COLLAPSEALL_ACTION = DebugUIPlugin.getUniqueIdentifier() + ".ID_COLLAPSEALL_ACTION"; //$NON-NLS-1$

	/**
	 * the viewer to perform the collapse all on
	 */
	private TreeViewer fViewer = null;

	/**
	 * Constructor
	 * @param viewer the viewer to perform the collapse all on
	 */
	public CollapseAllLaunchConfigurationAction(TreeViewer viewer) {
		fViewer = viewer;
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		return DebugUITools.getImageDescriptor(IInternalDebugUIConstants.IMG_ELCL_COLLAPSE_ALL);
	}

	@Override
	public String getText() {
		return LaunchConfigurationsMessages.CollapseAllLaunchConfigurationAction_0;
	}

	@Override
	public String getToolTipText() {
		return LaunchConfigurationsMessages.CollapseAllLaunchConfigurationAction_1;
	}

	@Override
	public ImageDescriptor getDisabledImageDescriptor() {
		return DebugUITools.getImageDescriptor(IInternalDebugUIConstants.IMG_DLCL_COLLAPSE_ALL);
	}

	@Override
	public void run() {
		fViewer.collapseAll();
	}

	@Override
	public String getDescription() {
		return LaunchConfigurationsMessages.CollapseAllLaunchConfigurationAction_2;
	}
}
