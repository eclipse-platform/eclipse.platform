/*******************************************************************************
 * Copyright (c) 2006, 2016 IBM Corporation and others.
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
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.ILaunchGroup;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Event;

/**
 * This class provides an action wrapper for adding launch configuration actions to the context menu
 * of the Run->... menu item
 *
 * @since 3.3
 */
public class LaunchConfigurationAction extends Action {

	private ILaunchConfiguration fConfig;
	private String fMode;

	/**
	 * Constructor
	 * @param mode
	 * @param text the text for the action
	 * @param image the image for the action
	 */
	public LaunchConfigurationAction(ILaunchConfiguration config, String mode, String text, ImageDescriptor image, int accelerator) {
		super(MessageFormat.format(ActionMessages.LaunchConfigurationAction_0, new Object[] {
				Integer.toString(accelerator), text }), image);
		fConfig = config;
		fMode = mode;
	}

	/**
	 * Allows access to the launch configuration associated with the action
	 * @return the associated launch configuration
	 */
	public ILaunchConfiguration getLaunchConfiguration() {
		return fConfig;
	}

	@Override
	public void run() {
		runInternal(false);
	}

	@Override
	public void runWithEvent(Event event) {
		if ((event.stateMask & SWT.MOD1) > 0) {
			try {
				ILaunchGroup group = DebugUIPlugin.getDefault().getLaunchConfigurationManager().getLaunchGroup(fConfig.getType(), fMode);
				if(group != null) {
					DebugUITools.openLaunchConfigurationDialogOnGroup(DebugUIPlugin.getShell(), new StructuredSelection(fConfig), group.getIdentifier());
				}
				else {
					runInternal(((event.stateMask & SWT.SHIFT) > 0) ? true : false);
				}
			}
			catch(CoreException ce) {}
		}
		else {
			runInternal(((event.stateMask & SWT.SHIFT) > 0) ? true : false);
		}
	}

	private void runInternal(boolean isShift) {
		DebugUITools.launch(fConfig, fMode, isShift);
	}
}
