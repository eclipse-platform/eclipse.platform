/*******************************************************************************
 * Copyright (c) 2004, 2006 IBM Corporation and others.
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
package org.eclipse.debug.ui.memory;

import org.eclipse.debug.core.model.IMemoryBlock;
import org.eclipse.jface.util.IPropertyChangeListener;


/**
 * Provides facilities related to the synchronization of memory renderings.
 * <p>
 * Clients hosting renderings may implement this interface.
 * </p>
 * @since 3.1
 */
public interface IMemoryRenderingSynchronizationService {

	/**
	 * Adds a listener for property changes notification for the specified properties.
	 * Specifying <code>null</code> indicates that the listener is interested in all
	 * properties. If an identical listener is already registered, the properties
	 * it is registered to listen for are updated.
	 *
	 * @param listener a property change listener
	 * @param properties properties the listener is interested in, or <code>null</code>
	 *  to indicate all properties.
	 */
	void addPropertyChangeListener(IPropertyChangeListener listener, String[] properties);

	/**
	 * Removes the given listener for property change notification.
	 * Has no effect if the identical listener is not registered.
	 *
	 * @param listener a property change listener
	 */
	void removePropertyChangeListener(IPropertyChangeListener listener);

	/**
	 * Returns the current value of the specified property for the given memory block, or
	 * <code>null</code> if none.
	 *
	 * @param block memory block for which a property is requested
	 * @param property the name of the property
	 * @return the property value or <code>null</code>
	 */
	Object getProperty(IMemoryBlock block, String property);

	/**
	 * Sets the rendering currently providing sychronization information for
	 * this synchronization service, or <code>null</code> if none.
	 *
	 * @param rendering active rendering providing synchronization information or
	 *  <code>null</code>
	 */
	void setSynchronizationProvider(IMemoryRendering rendering);

	/**
	 * Returns the rendering currently providing synchronization information for
	 * this synchronization service, or <code>null</code> if none.
	 *
	 * @return rendering providing synchronization information or <code>null</code>
	 */
	IMemoryRendering getSynchronizationProvider();
}
