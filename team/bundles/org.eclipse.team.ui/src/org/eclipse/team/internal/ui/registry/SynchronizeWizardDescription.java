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
package org.eclipse.team.internal.ui.registry;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.team.internal.ui.TeamUIPlugin;

/**
 * Descriptor for accessing and creating synchronize wizards
 */
public class SynchronizeWizardDescription {

	public  static final String ATT_ID = "id"; //$NON-NLS-1$
	public  static final String ATT_NAME = "name"; //$NON-NLS-1$
	public  static final String ATT_ICON = "icon"; //$NON-NLS-1$
	public  static final String ATT_CLASS = "class"; //$NON-NLS-1$
	public  static final String ATT_DESCRIPTION = "description"; //$NON-NLS-1$

	private String label;
	private String className;
	private String description;
	private String id;
	private ImageDescriptor imageDescriptor;

	private IConfigurationElement configElement;

	public SynchronizeWizardDescription(IConfigurationElement e, String descText) throws CoreException {
		configElement = e;
		loadFromExtension();
	}

	public IWizard createWizard() throws CoreException {
		Object obj = RegistryReader.createExtension(configElement, ATT_CLASS);
		return (IWizard) obj;
	}

	private void loadFromExtension() throws CoreException {
		String identifier = configElement.getAttribute(ATT_ID);
		label = configElement.getAttribute(ATT_NAME);
		className = configElement.getAttribute(ATT_CLASS);
		description = configElement.getAttribute(ATT_DESCRIPTION);

		// Sanity check.
		if ((label == null) || (className == null) || (identifier == null) || (description == null)) {
			throw new CoreException(new Status(IStatus.ERROR, configElement.getContributor().getName(), 0, "Invalid extension (missing label or class name): " + identifier, //$NON-NLS-1$
					null));
		}

		id = identifier;
	}

	public String getId() {
		return id;
	}

	public String getDescription() {
		return description;
	}

	public ImageDescriptor getImageDescriptor() {
		if (imageDescriptor != null)
			return imageDescriptor;
		String iconName = configElement.getAttribute(ATT_ICON);
		if (iconName == null)
			return null;
		imageDescriptor = TeamUIPlugin.getImageDescriptorFromExtension(configElement.getDeclaringExtension(), iconName);
		return imageDescriptor;
	}

	public String getName() {
		return label;
	}

	@Override
	public String toString() {
		return "Synchronize Participant Creation Wizard(" + getId() + ")"; //$NON-NLS-2$//$NON-NLS-1$
	}
}
