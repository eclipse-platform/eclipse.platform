/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.compare;

import org.eclipse.jface.util.IPropertyChangeListener;

/**
 * Interface common to all objects that provide a means for registering
 * for property change notification.
 * <p>
 * Clients may implement this interface.
 * </p>
 *
 * @see org.eclipse.jface.util.IPropertyChangeListener
 */
public interface IPropertyChangeNotifier {
	
	/**
	 * Adds a listener for property changes to this notifier.
	 * Has no effect if an identical listener is already registered.
	 *
	 * @param listener a property change listener
	 */
	void addPropertyChangeListener(IPropertyChangeListener listener);
	
	/**
	 * Removes the given content change listener from this notifier.
	 * Has no effect if the identical listener is not registered.
	 *
	 * @param listener a property change listener
	 */
	void removePropertyChangeListener(IPropertyChangeListener listener);
}
