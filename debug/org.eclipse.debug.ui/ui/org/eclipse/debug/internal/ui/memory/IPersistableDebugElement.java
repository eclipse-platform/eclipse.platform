/*******************************************************************************
 * Copyright (c) 2005, 2013 IBM Corporation and others.
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

/**
 * Represents an element that is capable of persisting properties.
 * @since 3.2
 */
public interface IPersistableDebugElement {

	/**
	 * Return the property with the specified propertyId.
	 * @param context is the context who is asking for this property.
	 * @param propertyId is the property id of the property.
	 * @return the value of the specified property
	 * @throws CoreException when an error has occurred getting this property
	 */
	Object getProperty(Object context, String propertyId) throws CoreException;

	/**
	 * Sets the property with the specified propertyId.  Clients are expected
	 * to save the properties specified.
	 * @param context is the context who is asking for this property to be saved.
	 * @param propertyId is the id of the property to be saved
	 * @param value is the value of the property
	 * @throws CoreException when an error has occurred setting this property
	 */
	void setProperty(Object context, String propertyId, Object value) throws CoreException;

	/**
	 * @param context is the contex who is asking if this property is supported
	 * @param propertyId
	 * @return true if the peristable debug element wishes to handle persistence of
	 * the specified property.
	 */
	boolean supportsProperty(Object context, String propertyId);

}
