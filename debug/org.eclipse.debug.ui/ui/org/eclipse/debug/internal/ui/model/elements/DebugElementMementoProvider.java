/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
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
package org.eclipse.debug.internal.ui.model.elements;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IPresentationContext;
import org.eclipse.ui.IMemento;

/**
 * Abstract memento provider debug elements.
 *
 * @since 3.4
 */
public abstract class DebugElementMementoProvider extends ElementMementoProvider {

	protected static final String ELEMENT_NAME = "ELEMENT_NAME"; //$NON-NLS-1$

	@Override
	protected boolean encodeElement(Object element, IMemento memento, IPresentationContext context) throws CoreException {
		if (supportsContext(context)) {
			String name = getElementName(element, context);
			memento.putString(ELEMENT_NAME, name);
			return true;
		}
		return false;
	}

	@Override
	protected boolean isEqual(Object element, IMemento memento, IPresentationContext context) throws CoreException {
		String mementoName = memento.getString(ELEMENT_NAME);
		if (mementoName != null) {
			String name = getElementName(element, context);
			if (name != null) {
				return name.equals(mementoName);
			}
		}
		return false;
	}

	/**
	 * Returns whether this adapter supports the given context.
	 *
	 * @param context
	 * @return whether this adapter supports the given context
	 */
	protected boolean supportsContext(IPresentationContext context) {
		return supportsContextId(context.getId());
	}

	/**
	 * Returns whether this adapter provides content in the specified context id.
	 *
	 * @param id part id
	 * @return whether this adapter provides content in the specified context id
	 */
	protected boolean supportsContextId(String id) {
		return true;
	}

	/**
	 * Returns the name of the given element to use in a memento in the given context,
	 * or <code>null</code> if unsupported.
	 *
	 * @param element model element
	 * @param context presentation context
	 * @return element name or <code>null</code> if none
	 * @throws CoreException
	 */
	protected abstract String getElementName(Object element, IPresentationContext context) throws CoreException;
}
