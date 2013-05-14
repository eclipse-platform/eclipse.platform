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

/**
 * Interface common to all objects that provide a means for registering
 * for content change notification.
 * <p>
 * Clients may implement this interface.
 * </p>
 *
 * @see IContentChangeListener
 */
public interface IContentChangeNotifier {
	
	/**
	 * Adds a content change listener to this notifier.
	 * Has no effect if an identical listener is already registered.
	 *
	 * @param listener a content changed listener
	 */
	void addContentChangeListener(IContentChangeListener listener);
	
	/**
	 * Removes the given content changed listener from this notifier.
	 * Has no effect if the listener is not registered.
	 *
	 * @param listener a content changed listener
	 */
	void removeContentChangeListener(IContentChangeListener listener);
}
