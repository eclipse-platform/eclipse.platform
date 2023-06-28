/*******************************************************************************
 * Copyright (c) 2004, 2005 IBM Corporation and others.
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
package org.eclipse.debug.internal.ui.memory;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.debug.ui.memory.IMemoryRendering;
import org.eclipse.debug.ui.memory.IMemoryRenderingType;
import org.eclipse.debug.ui.memory.IMemoryRenderingTypeDelegate;

/**
 * A contributed memory rendering type.
 *
 * @since 3.1
 */
class MemoryRenderingType implements IMemoryRenderingType {

	private IConfigurationElement fConfigurationElement;
	private IMemoryRenderingTypeDelegate fDelegate;

	// attributes for a memoryRenderingType
	static final String ATTR_MEM_RENDERING_TYPE_NAME = "name"; //$NON-NLS-1$
	static final String ATTR_MEM_RENDERING_TYPE_ID = "id"; //$NON-NLS-1$
	static final String ATTR_MEM_RENDERING_TYPE_DELEGATE = "class"; //$NON-NLS-1$

	/**
	 * Constructs a rendering type from a contribution.
	 */
	MemoryRenderingType(IConfigurationElement element) {
		fConfigurationElement = element;
	}

	@Override
	public String getLabel() {
		return fConfigurationElement.getAttribute(ATTR_MEM_RENDERING_TYPE_NAME);
	}

	@Override
	public String getId() {
		return fConfigurationElement.getAttribute(ATTR_MEM_RENDERING_TYPE_ID);
	}

	@Override
	public IMemoryRendering createRendering() throws CoreException {
		if (fDelegate == null) {
			fDelegate = (IMemoryRenderingTypeDelegate) fConfigurationElement.createExecutableExtension(ATTR_MEM_RENDERING_TYPE_DELEGATE);
		}
		return fDelegate.createRendering(getId());
	}

	/**
	 * Validates this contribution.
	 *
	 * @exception CoreException if invalid
	 */
	void validate() throws CoreException {
		verifyPresent(ATTR_MEM_RENDERING_TYPE_ID);
		verifyPresent(ATTR_MEM_RENDERING_TYPE_NAME);
		verifyPresent(ATTR_MEM_RENDERING_TYPE_DELEGATE);
	}

	private void verifyPresent(String attrName) throws CoreException {
		if (fConfigurationElement.getAttribute(attrName) == null) {
			Status status = new Status(IStatus.ERROR, DebugUIPlugin.getUniqueIdentifier(), IDebugUIConstants.INTERNAL_ERROR,
					"<memoryRenderingType> element missing required attribute: " + attrName, null); //$NON-NLS-1$
			throw new CoreException(status);
		}
	}
}
