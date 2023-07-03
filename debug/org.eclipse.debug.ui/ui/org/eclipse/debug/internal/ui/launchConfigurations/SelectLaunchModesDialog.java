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
package org.eclipse.debug.internal.ui.launchConfigurations;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.internal.ui.AbstractDebugListSelectionDialog;
import org.eclipse.debug.internal.ui.IDebugHelpContextIds;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Shell;

/**
 * This class provides a dialog to present the user with a list of of viable launch options in the event
 * the plug-in that provides either a launch option or a contributed launch delegate is no longer available.
 * The user can select one of the launch mode/option configuration from this dialog and repair the option
 * configuration state of the the current launch configuration
 *
 *  @since 3.3
 */
public class SelectLaunchModesDialog extends AbstractDebugListSelectionDialog{

	/**
	 * Builds labels for list control
	 */
	static class OptionsLabelProvider implements ILabelProvider {
		@Override
		public Image getImage(Object element) {return null;}
		@Override
		public String getText(Object element) {
			Set<?> vals = (Set<?>) element;
			Set<String> modes = new HashSet<>(vals.size());
			for (Object o : vals) {
				modes.add((String) o);
			}
			List<String> names = LaunchConfigurationPresentationManager.getDefault().getLaunchModeNames(modes);
			return names.toString();
		}
		@Override
		public void addListener(ILabelProviderListener listener) {}
		@Override
		public void dispose() {}
		@Override
		public boolean isLabelProperty(Object element, String property) {return false;}
		@Override
		public void removeListener(ILabelProviderListener listener) {}
	}

	private List<Set<String>> fValidModes = null;

	/**
	 * Constructor
	 * @param parentShell the parent shell
	 * @param mode the current mode context
	 * @param configuration the current launch configuration context
	 *
	 * @throws CoreException
	 */
	public SelectLaunchModesDialog(Shell parentShell, String mode, ILaunchConfiguration configuration) throws CoreException {
		super(parentShell);
		super.setTitle(LaunchConfigurationsMessages.SelectLaunchOptionsDialog_3);
		setShellStyle(getShellStyle() | SWT.RESIZE);
		fValidModes = new ArrayList<>();
		Set<Set<String>> modes = configuration.getType().getSupportedModeCombinations();
		for (Set<String> modeset : modes) {
			if(modeset.contains(mode)) {
				fValidModes.add(modeset);
			}
		}
	}

	@Override
	protected String getDialogSettingsId() {
		return IDebugUIConstants.PLUGIN_ID + ".SELECT_LAUNCH_MODES_DIALOG"; //$NON-NLS-1$
	}

	@Override
	protected IBaseLabelProvider getLabelProvider() {
		return new OptionsLabelProvider();
	}

	@Override
	protected Object getViewerInput() {
		return fValidModes.toArray();
	}

	@Override
	protected String getHelpContextId() {
		return IDebugHelpContextIds.SELECT_LAUNCH_MODES_DIALOG;
	}

	@Override
	protected String getViewerLabel() {
		return LaunchConfigurationsMessages.SelectLaunchOptionsDialog_4;
	}
}
