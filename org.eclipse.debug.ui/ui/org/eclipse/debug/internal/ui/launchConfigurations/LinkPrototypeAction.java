/*******************************************************************************
 * Copyright (c) 2017 Obeo.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Obeo - initial API and implementation
 *******************************************************************************/
package org.eclipse.debug.internal.ui.launchConfigurations;

import java.util.Collection;
import java.util.HashSet;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.IInternalDebugUIConstants;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.DecoratingLabelProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;

/**
 * Link attributes from a prototype to the selected launch configuration(s).
 *
 * @since 3.13
 */
public class LinkPrototypeAction extends AbstractLaunchConfigurationAction {

	/**
	 * Action identifier for IDebugView#getAction(String)
	 */
	public static final String ID_LINK_PROTOTYPE_ACTION = DebugUIPlugin.getUniqueIdentifier() + ".ID_LINK_PROTOTYPE_ACTION"; //$NON-NLS-1$

	/**
	 * Constructs an action to apply a prototype to a launch configuration
	 *
	 * @param viewer the viewer
	 * @param mode the mode the action applies to
	 */
	public LinkPrototypeAction(Viewer viewer, String mode) {
		super(LaunchConfigurationsMessages.LinkPrototypeAction_Link_prototype_1, viewer, mode);
	}

	/**
	 * @see AbstractLaunchConfigurationAction#performAction()
	 */
	@Override
	protected void performAction() {
		try {
			ILaunchConfiguration firstLaunchConfiguration = (ILaunchConfiguration) getStructuredSelection().getFirstElement();
			ILaunchConfigurationType type = firstLaunchConfiguration.getType();
			ILaunchConfiguration[] prototypes = type.getPrototypes();
			// Select the prototype
			DecoratingLabelProvider labelProvider = new DecoratingLabelProvider(DebugUITools.newDebugModelPresentation(), PlatformUI.getWorkbench().getDecoratorManager().getLabelDecorator());
			ElementListSelectionDialog selectPrototypeDialog = new ElementListSelectionDialog(getShell(), labelProvider);
			selectPrototypeDialog.setElements(prototypes);
			selectPrototypeDialog.setInitialSelections(firstLaunchConfiguration.getPrototype());
			selectPrototypeDialog.setMultipleSelection(false);
			selectPrototypeDialog.setEmptySelectionMessage("You have to select a prototype."); //$NON-NLS-1$
			selectPrototypeDialog.setTitle("Please select a prototype"); //$NON-NLS-1$
			int open = selectPrototypeDialog.open();
			if (open == Window.OK) {
				Object selectedPrototype = selectPrototypeDialog.getFirstResult();
				for (Object launchConfiguration : getStructuredSelection().toList()) {
					if (launchConfiguration instanceof ILaunchConfiguration) {
						// Link the prototype attributes to the selected launch
						// configuration
						ILaunchConfigurationWorkingCopy workingCopy = ((ILaunchConfiguration) launchConfiguration).getWorkingCopy();
						workingCopy.setPrototype((ILaunchConfiguration) selectedPrototype, true);
						workingCopy.doSave();
						// if only one configuration is selected, refresh the
						// tabs to display visible attributes values from the
						// prototype
						if (getStructuredSelection().size() == 1) {
							ILaunchConfigurationDialog dialog = LaunchConfigurationsDialog.getCurrentlyVisibleLaunchConfigurationDialog();
							if (dialog instanceof LaunchConfigurationsDialog) {
								((LaunchConfigurationsDialog) dialog).getTabViewer().setInput(workingCopy);
							}
						}
					}
				}
				getViewer().refresh();
			}
		} catch (CoreException e) {
			errorDialog(e);
		}
	}

	/**
	 * @see org.eclipse.ui.actions.SelectionListenerAction#updateSelection(org.eclipse.jface.viewers.IStructuredSelection)
	 */
	@Override
	protected boolean updateSelection(IStructuredSelection selection) {
		// Enable action only if launch configuration(s) of the same type
		// is(are) selected and the launch configuration type allows prototypes
		Collection<ILaunchConfigurationType> launchConfigurationTypes = new HashSet<>();
		for (Object object : selection.toList()) {
			if (object instanceof ILaunchConfiguration) {
				if (((ILaunchConfiguration) object).isPrototype()) {
					return false;
				} else {
					ILaunchConfigurationType type = null;
					try {
						type = ((ILaunchConfiguration) object).getType();
					} catch (CoreException e) {
						DebugUIPlugin.log(e.getStatus());
					}
					if (type != null) {
						launchConfigurationTypes.add(type);
					} else {
						return false;
					}
				}
			} else {
				return false;
			}
		}
		if (launchConfigurationTypes.size() == 1) {
			return launchConfigurationTypes.iterator().next().supportsPrototypes();
		}
		return false;
	}

	@Override
	public ImageDescriptor getDisabledImageDescriptor() {
		return DebugUITools.getImageDescriptor(IInternalDebugUIConstants.IMG_DLCL_LINK_PROTO);
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		return DebugUITools.getImageDescriptor(IInternalDebugUIConstants.IMG_ELCL_LINK_PROTO);
	}

	@Override
	public String getToolTipText() {
		return LaunchConfigurationsMessages.LaunchConfigurationsDialog_7;
	}
}
