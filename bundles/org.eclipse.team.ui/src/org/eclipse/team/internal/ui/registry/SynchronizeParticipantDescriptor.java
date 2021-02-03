/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
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
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.team.ui.synchronize.ISynchronizeParticipantDescriptor;

public class SynchronizeParticipantDescriptor implements ISynchronizeParticipantDescriptor {
	public  static final String ATT_ID = "id"; //$NON-NLS-1$
	public  static final String ATT_NAME = "name"; //$NON-NLS-1$
	public  static final String ATT_ICON = "icon"; //$NON-NLS-1$
	public  static final String ATT_CLASS = "class"; //$NON-NLS-1$
	private static final String ATT_PERSISTENT = "persistent"; //$NON-NLS-1$
	public static final String ATT_HELP_CONTEXT_ID = "helpContextId"; //$NON-NLS-1$

	private String label;
	private String className;
	private String id;
	private boolean persistent;
	private ImageDescriptor imageDescriptor;
	private String description;
	private String helpContextId;

	private IConfigurationElement configElement;

	/**
	 * Create a new ViewDescriptor for an extension.
	 * @param e the configuration element for an extension
	 * @param desc the view's description
	 * @throws CoreException if loading the view descriptor from the registry fails
	 */
	public SynchronizeParticipantDescriptor(IConfigurationElement e, String desc) throws CoreException {
		configElement = e;
		description = desc;
		loadFromExtension();
	}

	public IConfigurationElement getConfigurationElement() {
		return configElement;
	}

	/**
	 * Returns this view's description. This is the value of its <code>"description"</code>
	 * attribute.
	 *
	 * @return the description
	 */
	@Override
	public String getDescription() {
		return description;
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		if (imageDescriptor != null)
			return imageDescriptor;
		String iconName = configElement.getAttribute(ATT_ICON);
		if (iconName == null)
			return null;
		imageDescriptor = TeamUIPlugin.getImageDescriptorFromExtension(configElement.getDeclaringExtension(), iconName);
		return imageDescriptor;
	}

	@Override
	public String getName() {
		return label;
	}

	@Override
	public boolean isPersistent() {
		return persistent;
	}

	public String getHelpContextId() {
		return helpContextId;
	}

	/**
	 * Loads a view descriptor from the registry.
	 */
	private void loadFromExtension() throws CoreException {
		String identifier = configElement.getAttribute(ATT_ID);
		label = configElement.getAttribute(ATT_NAME);
		className = configElement.getAttribute(ATT_CLASS);
		String persistentString = configElement.getAttribute(ATT_PERSISTENT);
		if(persistentString == null) {
			persistent = true;
		} else {
			persistent = Boolean.parseBoolean(persistentString);
		}
		helpContextId = configElement.getAttribute(ATT_HELP_CONTEXT_ID);
		// Sanity check.
		if ((label == null) || (className == null) || (identifier == null)) {
			throw new CoreException(new Status(IStatus.ERROR, configElement.getNamespaceIdentifier(), 0, "Invalid extension (missing label or class name): " + identifier, //$NON-NLS-1$
					null));
		}
		id = identifier;
	}

	/**
	 * Returns a string representation of this descriptor. For debugging
	 * purposes only.
	 */
	@Override
	public String toString() {
		return "Synchronize Participant(" + getId() + ")"; //$NON-NLS-2$//$NON-NLS-1$
	}
}
