/*******************************************************************************
 * Copyright (c) 2004, 2013 IBM Corporation and others.
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
package org.eclipse.debug.internal.core;

import java.text.MessageFormat;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchMode;

/**
 * Proxy to a launch mode extension.
 *
 * @see IConfigurationElementConstants
 */
public class LaunchMode implements ILaunchMode {

	private IConfigurationElement fConfigurationElement;

	/**
	 * Constructs a new launch mode.
	 *
	 * @param element configuration element
	 * @exception CoreException if required attributes are missing
	 */
	public LaunchMode(IConfigurationElement element) throws CoreException {
		fConfigurationElement = element;
		verifyAttributes();
	}

	/**
	 * Verifies required attributes.
	 *
	 * @exception CoreException if required attributes are missing
	 */
	private void verifyAttributes() throws CoreException {
		verifyAttributeExists(IConfigurationElementConstants.MODE);
		verifyAttributeExists(IConfigurationElementConstants.LABEL);
	}

	/**
	 * Verifies the given attribute exists
	 * @param name the attribute name to check
	 *
	 * @exception CoreException if attribute does not exist
	 */
	private void verifyAttributeExists(String name) throws CoreException {
		if (fConfigurationElement.getAttribute(name) == null) {
			missingAttribute(name);
		}
	}

	/**
	 * This method is used to create a new internal error describing that the specified attribute
	 * is missing
	 * @param attrName the name of the attribute that is missing
	 * @throws CoreException if a problem is encountered
	 */
	private void missingAttribute(String attrName) throws CoreException {
		throw new CoreException(new Status(IStatus.ERROR, DebugPlugin.getUniqueIdentifier(), DebugPlugin.ERROR, MessageFormat.format(DebugCoreMessages.LaunchMode_1, new Object[] { attrName }), null));
	}

	@Override
	public String getIdentifier() {
		return fConfigurationElement.getAttribute(IConfigurationElementConstants.MODE);
	}
	@Override
	public String getLabel() {
		return fConfigurationElement.getAttribute(IConfigurationElementConstants.LABEL);
	}

	@Override
	public String getLaunchAsLabel() {
		String label = fConfigurationElement.getAttribute(IConfigurationElementConstants.LAUNCH_AS_LABEL);
		if (label == null) {
			return MessageFormat.format(DebugCoreMessages.LaunchMode_0, new Object[] { getLabel() });
		}
		return label;
	}
}
