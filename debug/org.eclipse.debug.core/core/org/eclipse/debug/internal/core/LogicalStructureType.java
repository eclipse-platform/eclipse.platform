/*******************************************************************************
 * Copyright (c) 2000, 2013 IBM Corporation and others.
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
import org.eclipse.debug.core.ILogicalStructureType;
import org.eclipse.debug.core.model.ILogicalStructureTypeDelegate;
import org.eclipse.debug.core.model.ILogicalStructureTypeDelegate2;
import org.eclipse.debug.core.model.ILogicalStructureTypeDelegate3;
import org.eclipse.debug.core.model.IValue;

/**
 * Proxy to a logical structure type extension.
 *
 * @see IConfigurationElementConstants
 */
public class LogicalStructureType implements ILogicalStructureType, ILogicalStructureTypeDelegate3 {

	private IConfigurationElement fConfigurationElement;
	private ILogicalStructureTypeDelegate fDelegate;
	private String fModelId;
	// whether the 'description' attribute has been verified to exist: it is only
	// required when the delegate does *not* implement ILogicalStructureTypeDelegate2.
	private boolean fVerifiedDescription = false;

	/**
	 * Constructs a new logical structure type, and verifies required attributes.
	 *
	 * @param element configuration element
	 * @exception CoreException if required attributes are missing
	 */
	public LogicalStructureType(IConfigurationElement element) throws CoreException {
		fConfigurationElement = element;
		verifyAttributes();
	}

	/**
	 * Verifies required attributes.
	 *
	 * @exception CoreException if required attributes are missing
	 */
	private void verifyAttributes() throws CoreException {
		verifyAttributeExists(IConfigurationElementConstants.ID);
		verifyAttributeExists(IConfigurationElementConstants.CLASS);
		fModelId = fConfigurationElement.getAttribute(IConfigurationElementConstants.MODEL_IDENTIFIER);
		if (fModelId == null) {
			missingAttribute(IConfigurationElementConstants.MODEL_IDENTIFIER);
		}
	}

	/**
	 * Verifies the given attribute exists
	 * @param name the name to verify
	 *
	 * @exception CoreException if attribute does not exist
	 */
	private void verifyAttributeExists(String name) throws CoreException {
		if (fConfigurationElement.getAttribute(name) == null) {
			missingAttribute(name);
		}
	}

	/**
	 * Throws a new <code>CoreException</code> about the specified attribute being missing
	 * @param attrName the name of the missing attribute
	 * @throws CoreException if a problem is encountered
	 */
	private void missingAttribute(String attrName) throws CoreException {
		throw new CoreException(new Status(IStatus.ERROR, DebugPlugin.getUniqueIdentifier(), DebugPlugin.ERROR, MessageFormat.format(DebugCoreMessages.LogicalStructureType_1, new Object[] { attrName }), null));
	}

	@Override
	public String getDescription() {
		return fConfigurationElement.getAttribute(IConfigurationElementConstants.DESCRIPTION);
	}

	@Override
	public String getId() {
		return fConfigurationElement.getAttribute(IConfigurationElementConstants.ID);
	}

	@Override
	public IValue getLogicalStructure(IValue value) throws CoreException {
		return getDelegate().getLogicalStructure(value);
	}

	@Override
	public boolean providesLogicalStructure(IValue value) {
		if (value.getModelIdentifier().equals(fModelId)) {
			return getDelegate().providesLogicalStructure(value);
		}
		return false;
	}

	@Override
	public void releaseValue(IValue logicalStructure) {
		ILogicalStructureTypeDelegate delegate = getDelegate();
		if (delegate instanceof ILogicalStructureTypeDelegate3) {
			((ILogicalStructureTypeDelegate3) delegate).releaseValue(logicalStructure);
		}
	}

	/**
	 * Returns the <code>ILogicalStructuresTypeDelegate</code> delegate
	 * @return the delegate
	 */
	protected ILogicalStructureTypeDelegate getDelegate() {
		if (fDelegate == null) {
			try {
				fDelegate = (ILogicalStructureTypeDelegate) fConfigurationElement.createExecutableExtension(IConfigurationElementConstants.CLASS);
			} catch (CoreException e) {
				DebugPlugin.log(e);
			}
		}
		return fDelegate;
	}

	@Override
	public String getDescription(IValue value) {
		ILogicalStructureTypeDelegate delegate = getDelegate();
		if (delegate instanceof ILogicalStructureTypeDelegate2) {
			ILogicalStructureTypeDelegate2 d2 = (ILogicalStructureTypeDelegate2) delegate;
			return d2.getDescription(value);
		}
		if (!fVerifiedDescription) {
			fVerifiedDescription = true;
			try {
				verifyAttributeExists(IConfigurationElementConstants.DESCRIPTION);
			} catch (CoreException e) {
				DebugPlugin.log(e);
			}
		}
		String description = getDescription();
		if (description == null) {
			return DebugCoreMessages.LogicalStructureType_0;
		}
		return description;
	}
}
