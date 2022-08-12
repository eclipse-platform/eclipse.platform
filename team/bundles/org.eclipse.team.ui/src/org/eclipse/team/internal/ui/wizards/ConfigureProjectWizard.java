/*******************************************************************************
 * Copyright (c) 2000, 2017 IBM Corporation and others.
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
package org.eclipse.team.internal.ui.wizards;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.internal.ui.ITeamUIImages;
import org.eclipse.team.internal.ui.TeamUIMessages;
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.activities.IActivityManager;
import org.eclipse.ui.activities.IIdentifier;
import org.eclipse.ui.model.AdaptableList;

/**
 * The wizard for associating projects with team providers
 */
public class ConfigureProjectWizard extends Wizard {
	protected IProject[] projects;
	protected ConfigureProjectWizardMainPage mainPage;

	protected final static String PT_CONFIGURATION ="configurationWizards"; //$NON-NLS-1$
	protected final static String TAG_WIZARD = "wizard"; //$NON-NLS-1$
	protected final static String TAG_DESCRIPTION = "description"; //$NON-NLS-1$
	protected final static String ATT_NAME = "name"; //$NON-NLS-1$
	protected final static String ATT_CLASS = "class"; //$NON-NLS-1$
	protected final static String ATT_ICON = "icon"; //$NON-NLS-1$
	protected final static String ATT_ID = "id"; //$NON-NLS-1$

	private ConfigureProjectWizard(IProject[] projects) {
		this.projects = projects;
		setNeedsProgressMonitor(true);
		setWindowTitle(TeamUIMessages.ConfigureProjectWizard_title);
	}

	@Override
	public void addPages() {
		AdaptableList disabledWizards = new AdaptableList();
		AdaptableList wizards = getAvailableWizards(disabledWizards);
		mainPage = new ConfigureProjectWizardMainPage("configurePage1", TeamUIMessages.ConfigureProjectWizard_configureProject, TeamUIPlugin.getImageDescriptor(ITeamUIImages.IMG_WIZBAN_SHARE), wizards, disabledWizards); //$NON-NLS-1$
		mainPage.setDescription(TeamUIMessages.ConfigureProjectWizard_description);
		mainPage.setProjects(projects);
		addPage(mainPage);
	}

	@Override
	public boolean canFinish() {
		// If we are on the first page, never allow finish unless the selected wizard has no pages.
		if (getContainer().getCurrentPage() == mainPage) {
			if (mainPage.getSelectedWizard() != null && mainPage.getNextPage() == null) {
				return true;
			}
			return false;
		}
		return super.canFinish();
	}

	@Override
	public boolean performFinish() {
		// If we are on the first page and the selected wizard has no pages then
		// allow it to finish.

		// save dialog settings
		mainPage.performFinish();

		if (getContainer().getCurrentPage() == mainPage) {
			IWizard noPageWizard = mainPage.getSelectedWizard();
			if (noPageWizard != null) {
				if (noPageWizard.canFinish())
				{
					return noPageWizard.performFinish();
				}
			}
		}
		// If the wizard has pages and there are several
		// wizards registered then the registered wizard
		// will call it's own performFinish().
		return true;
	}

	private static class ResizeWizardDialog extends WizardDialog {
		public ResizeWizardDialog(Shell parentShell, IWizard newWizard) {
			super(parentShell, newWizard);
			setShellStyle(getShellStyle() | SWT.RESIZE);
		}
	}

	public static void shareProjects(Shell shell, IProject[] projects) {
		IWizard wizard = null;
		// If we only have one wizard registered, we'll just use that wizard
		// unless it doesn't have any pages
		AdaptableList disabledWizards = new AdaptableList();
		AdaptableList wizards = getAvailableWizards(disabledWizards);
		if (wizards.size() == 1 && disabledWizards.size() == 0) {
			ConfigurationWizardElement element = (ConfigurationWizardElement)wizards.getChildren()[0];
			if (element.wizardHasPages(projects)) {
				try {
					wizard = element.createExecutableExtension(projects);
				} catch (CoreException e) {
					// Log the exception and fall through to show the wizard
					TeamUIPlugin.log(e);
				}
			}
		}
		if (wizard == null) {
			wizard = new ConfigureProjectWizard(projects);
			((ConfigureProjectWizard)wizard).setForcePreviousAndNextButtons(true);
		}
		openWizard(shell, wizard);
	}

	/**
	 * Returns the configuration wizards that are available for invocation.
	 *
	 * @return the available wizards
	 */
	private static AdaptableList getAvailableWizards(AdaptableList disabledWizards) {
		AdaptableList result = new AdaptableList();
		IExtensionRegistry registry = Platform.getExtensionRegistry();
		IExtensionPoint point = registry.getExtensionPoint(TeamUIPlugin.PLUGIN_ID, PT_CONFIGURATION);
		if (point != null) {
			IExtension[] extensions = point.getExtensions();
			for (IExtension extension : extensions) {
				IConfigurationElement[] elements = extension.getConfigurationElements();
				for (IConfigurationElement element : elements) {
					if (element.getName().equals(TAG_WIZARD)) {
						ConfigurationWizardElement wizard = createWizardElement(element);
						if (wizard != null && filterItem(element)) {
							disabledWizards.add(wizard);
						} else if (wizard != null) {
							result.add(wizard);
						}
					}
				}
			}
		}
		return result;
	}

	private static boolean filterItem(IConfigurationElement element) {
		String extensionId = element.getAttribute(ATT_ID);
		String extensionPluginId = element.getNamespaceIdentifier();
		IActivityManager activityMgr = PlatformUI.getWorkbench().getActivitySupport().getActivityManager();
		IIdentifier id = activityMgr.getIdentifier(extensionPluginId + "/" +  extensionId); //$NON-NLS-1$
		return (!id.isEnabled());
	}

	/**
	 * Returns a new ConfigurationWizardElement configured according to the parameters
	 * contained in the passed Registry.
	 *
	 * May answer null if there was not enough information in the Extension to create
	 * an adequate wizard
	 *
	 * @param element  the element for which to create a wizard element
	 * @return the wizard element for the given element
	 */
	private static ConfigurationWizardElement createWizardElement(IConfigurationElement element) {
		// WizardElements must have a name attribute
		String nameString = element.getAttribute(ATT_NAME);
		if (nameString == null) {
			// Missing attribute
			return null;
		}
		ConfigurationWizardElement result = new ConfigurationWizardElement(nameString);
		if (initializeWizard(result, element)) {
			// initialization was successful
			return result;
		}
		return null;
	}
	/**
	 *	Initialize the passed element's properties based on the contents of
	 *	the passed registry.  Answer a boolean indicating whether the element
	 *	was able to be adequately initialized.
	 *
	 *	@param element  the element to initialize the properties for
	 *	@param config  the registry to get properties from
	 *	@return whether initialization was successful
	 */
	private static boolean initializeWizard(ConfigurationWizardElement element, IConfigurationElement config) {
		element.setID(config.getAttribute(ATT_ID));
		String description = ""; //$NON-NLS-1$
		IConfigurationElement [] children = config.getChildren(TAG_DESCRIPTION);
		if (children.length >= 1) {
			description = children[0].getValue();
		}

		element.setDescription(description);

		// apply CLASS and ICON properties
		element.setConfigurationElement(config);
		String iconName = config.getAttribute(ATT_ICON);
		if (iconName != null) {
			IExtension extension = config.getDeclaringExtension();
			element.setImageDescriptor(TeamUIPlugin.getImageDescriptorFromExtension(extension, iconName));
		}
		// ensure that a class was specified
		if (element.getConfigurationElement() == null) {
			// Missing attribute
			return false;
		}
		return true;
	}

	public static void openWizard(Shell shell, IWizard wizard) {
		WizardDialog dialog = new ResizeWizardDialog(shell, wizard);
		dialog.open();
	}
}
