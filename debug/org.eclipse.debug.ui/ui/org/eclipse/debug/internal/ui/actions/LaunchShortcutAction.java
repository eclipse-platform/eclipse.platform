/*******************************************************************************
 * Copyright (c) 2000, 2026 IBM Corporation and others.
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


import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.eclipse.core.expressions.Expression;
import org.eclipse.core.expressions.IEvaluationContext;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.TerminateToggleValue;
import org.eclipse.debug.internal.ui.contextlaunching.LaunchingResourceManager;
import org.eclipse.debug.internal.ui.launchConfigurations.LaunchConfigurationManager;
import org.eclipse.debug.internal.ui.launchConfigurations.LaunchConfigurationsMessages;
import org.eclipse.debug.internal.ui.launchConfigurations.LaunchShortcutExtension;
import org.eclipse.debug.internal.ui.stringsubstitution.SelectedResourceManager;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.ILaunchGroup;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.IEditorPart;

/**
 * Launch shortcut action (proxy to a launch shortcut extension)
 */
public class LaunchShortcutAction extends Action {

	private final String fMode;
	private final LaunchShortcutExtension fShortcut;
	private ILaunchConfiguration configuration;

	/**
	 * @param mode the mode to launch in
	 * @param shortcut the underlying shortcut
	 */
	public LaunchShortcutAction(String mode, LaunchShortcutExtension shortcut) {
		super(shortcut.getLabel(), shortcut.getImageDescriptor());
		fShortcut = shortcut;
		fMode = mode;
		updateEnablement();
	}

	public LaunchShortcutAction(String mode, LaunchShortcutExtension shortcut, ILaunchConfiguration configuration) {
		super(configuration.getName(), shortcut.getImageDescriptor());
		fShortcut = shortcut;
		fMode = mode;
		this.configuration = configuration;
		updateEnablement();
	}

	/**
	 * Runs with either the active editor or workbench selection.
	 *
	 * @see IAction#run()
	 */
	@Override
	public void run() {
		runInternal(false);
	}

	private void runInternal(boolean isShift) {
		if (configuration != null) {
			DebugUITools.launch(configuration, fMode);
			return;
		}
		IStructuredSelection ss = SelectedResourceManager.getDefault().getCurrentSelection();
		Object o = ss.getFirstElement();
		// store if Shift was pressed to toggle terminate before launch
		// preference
		if(o instanceof IEditorPart) {
			DebugUITools.storeLaunchToggleTerminate(o, isShift);
			fShortcut.launch((IEditorPart) o, fMode);
			DebugUITools.removeLaunchToggleTerminate(o);
		}
		else {
			DebugUITools.storeLaunchToggleTerminate(ss, new TerminateToggleValue(isShift, fShortcut));
			fShortcut.launch(ss, fMode);
			DebugUITools.removeLaunchToggleTerminate(ss);
		}
	}

	@Override
	public void runWithEvent(Event event) {
		if ((event.stateMask & SWT.MOD1) > 0) {
			openConfigurationDialog();
		}
		else {
			runInternal(((event.stateMask & SWT.SHIFT) > 0) ? true : false);
		}
	}

	private void openConfigurationDialog() {
		Set<String> types = fShortcut.getAssociatedConfigurationTypes();
		if (!types.isEmpty()) {
			LaunchingResourceManager lrm = DebugUIPlugin.getDefault().getLaunchingResourceManager();
			IStructuredSelection selection = SelectedResourceManager.getDefault().getCurrentSelection();
			ArrayList<LaunchShortcutExtension> shortcuts = new ArrayList<>();
			shortcuts.add(fShortcut);
			IResource resource = SelectedResourceManager.getDefault().getSelectedResource();
			if (resource == null) {
				resource = lrm.getLaunchableResource(shortcuts, selection);
			}
			List<ILaunchConfiguration> configs = lrm.getParticipatingLaunchConfigurations(selection, resource,
					shortcuts, fMode);
			LaunchConfigurationManager lcm = DebugUIPlugin.getDefault().getLaunchConfigurationManager();
			// Keep only configs whose type belongs to this shortcut
			List<ILaunchConfiguration> ownedConfigs = new ArrayList<>();
			try {
				for (ILaunchConfiguration c : configs) {
					if (types.contains(c.getType().getIdentifier())) {
						ownedConfigs.add(c);
					}
				}
			} catch (CoreException e) {
				DebugUIPlugin.log(e);
				return;
			}
			ILaunchConfiguration config = null;
			ILaunchConfigurationType type = null;
			try {
				if (!ownedConfigs.isEmpty()) {
					ILaunchGroup mruGroup = lcm.getLaunchGroup(ownedConfigs.get(0).getType(), fMode);
					config = lcm.getMRUConfiguration(ownedConfigs, mruGroup, resource);
					if (config == null) {
						config = ownedConfigs.get(0);
					}
					type = config.getType();
				} else {
					// No existing config: pick the first type this shortcut declares
					// that supports the current mode. Shortcuts virtually always declare
					// exactly one type, so no disambiguation dialog is needed.
					for (String id : types) {
						ILaunchConfigurationType candidate = DebugPlugin.getDefault().getLaunchManager()
								.getLaunchConfigurationType(id);
						if (candidate != null && candidate.supportsMode(fMode)) {
							type = candidate;
							break;
						}
					}
					if (type == null) {
						return;
					}
				}
			} catch (CoreException e) {
				DebugUIPlugin.log(e);
				return;
			}
			String groupid = null;
			ILaunchGroup group = lcm.getLaunchGroup(type, fMode);
			if (group != null) {
				groupid = group.getIdentifier();
			}
			if (config == null) {
				try {
					String baseName = resource != null
							? resource.getFullPath().removeFileExtension().lastSegment() : null;
					if (baseName == null || baseName.isEmpty()) {
						baseName = LaunchConfigurationsMessages.CreateLaunchConfigurationAction_New_configuration_2;
					}
					String name = DebugPlugin.getDefault().getLaunchManager()
							.generateLaunchConfigurationName(baseName);
					ILaunchConfigurationWorkingCopy wc = type.newInstance(null, name);
					DebugUIPlugin.openLaunchConfigurationsDialog(DebugUIPlugin.getShell(),
							new StructuredSelection(wc), groupid, true);
				} catch (CoreException e) {
					DebugUIPlugin.log(e);
				}
				return;
			}
			selection = new StructuredSelection(config);
			DebugUITools.openLaunchConfigurationDialogOnGroup(DebugUIPlugin.getShell(), selection, groupid);
		} else {
			run();
		}
	}

	/**
	 * Since these actions are re-created each time the run/debug as menu is
	 * filled, the enablement of this action is static.
	 */
	private void updateEnablement() {
		boolean enabled = false;
		IStructuredSelection ss = SelectedResourceManager.getDefault().getCurrentSelection();
		Object o = ss.getFirstElement();
		if(o instanceof IEditorPart) {
			enabled = true;
		}
		else {
			try {
				// check enablement logic, if any
				Expression expression = fShortcut.getShortcutEnablementExpression();
				if (expression == null) {
					enabled = !ss.isEmpty();
				} else {
					List<?> list = ss.toList();
					IEvaluationContext context = DebugUIPlugin.createEvaluationContext(list);
					context.addVariable("selection", list); //$NON-NLS-1$
					enabled = fShortcut.evalEnablementExpression(context, expression);
				}
			} catch (CoreException e) {}
		}
		setEnabled(enabled);
	}

}
