/*******************************************************************************
 * Copyright (c) 2005, 2010 IBM Corporation and others.
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
 *     Patrick Chuong (Texas Instruments) - Allow multiple debug views and
 *     		multiple debug context providers (Bug 327263)
 *******************************************************************************/
package org.eclipse.debug.ui.contexts;

import org.eclipse.jface.viewers.ISelection;


/**
 * Debug context service for a window. Clients may register for debug context
 * notification with this service. A context service is obtained from the
 * debug context manager.
 * @see IDebugContextManager
 * @since 3.3
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 */
public interface IDebugContextService {

	/**
	 * Registers for the given listener for debug context change notification
	 * in this service's window.
	 *
	 * @param listener debug context listener
	 */
	void addDebugContextListener(IDebugContextListener listener);
	/**
	 * Unregisters for the given listener for debug context change notification
	 * in this service's window.
	 *
	 * @param listener debug context listener
	 */
	void removeDebugContextListener(IDebugContextListener listener);

	/**
	 * Registers for the given debug context listener for context notification
	 * from the specified part in this service's window.
	 *
	 * @param listener debug context listener
	 * @param partId part identifier
	 */
	void addDebugContextListener(IDebugContextListener listener, String partId);

	/**
	 * Unregisters the given debug context listener for context change notification
	 * from the specified part in this service's window.
	 *
	 * @param listener debug context listener
	 * @param partId part identifier
	 */
	void removeDebugContextListener(IDebugContextListener listener, String partId);

	/**
	 * Returns the active context in this service's window
	 * or <code>null</code>.
	 *
	 * @return active context or <code>null</code>
	 */
	ISelection getActiveContext();

	/**
	 * Returns the active context in the specified part of this service's window
	 * or <code>null</code> if none.
	 *
	 * @param partId part identifier
	 * @return active context or <code>null</code>
	 */
	ISelection getActiveContext(String partId);

	/**
	 * Registers the given debug context listener for post context change notification
	 * in this service's window. Post listeners are notified of context changes after all
	 * non-post listeners are notified.
	 *
	 * @param listener debug context listener
	 */
	void addPostDebugContextListener(IDebugContextListener listener);

	/**
	 * Unregisters the given debug context listener for post context change notification
	 * in this service's window.
	 *
	 * @param listener debug context listener.
	 */
	void removePostDebugContextListener(IDebugContextListener listener);

	/**
	 * Registers the given debug context listener for post context change notification
	 * in the specified part of this service's window. Post listeners are notified of
	 * context changes after all non-post listeners are notified.
	 *
	 * @param listener debug context listener
	 * @param partId part identifier
	 */
	void addPostDebugContextListener(IDebugContextListener listener, String partId);

	/**
	 * Unregisters the given debug context listener for post context change notification
	 * in the specified part of this service's window.
	 *
	 * @param listener debug context listener
	 * @param partId part identifier
	 */
	void removePostDebugContextListener(IDebugContextListener listener, String partId);

	/**
	 * Registers the given debug context provider with this service.
	 *
	 * @param provider debug context provider
	 */
	void addDebugContextProvider(IDebugContextProvider provider);

	/**
	 * Unregisters the given debug context provider from this service.
	 *
	 * @param provider debug context provider
	 */
	void removeDebugContextProvider(IDebugContextProvider provider);

	/**
	 * Returns the active context in the specified part of this service's window
	 * or <code>null</code> if none.
	 *
	 * @param partId part identifier
	 * @param partSecondaryId part secondary identifier
	 * @return active context or <code>null</code>
	 * @since 3.7
	 */
	ISelection getActiveContext(String partId, String partSecondaryId);

	/**
	 * Registers for the given debug context listener for context notification
	 * from the specified part in this service's window.
	 *
	 * @param listener debug context listener
	 * @param partId part identifier
	 * @param partSecondaryId part secondary identifier
	 * @since 3.7
	 */
	void addDebugContextListener(IDebugContextListener listener, String partId, String partSecondaryId);

	/**
	 * Unregisters the given debug context listener for context change notification
	 * from the specified part in this service's window.
	 *
	 * @param listener debug context listener
	 * @param partId part identifier
	 * @param partSecondaryId part secondary identifier
	 * @since 3.7
	 */
	void removeDebugContextListener(IDebugContextListener listener, String partId, String partSecondaryId);

	/**
	 * Registers the given debug context listener for post context change notification
	 * in the specified part of this service's window. Post listeners are notified of
	 * context changes after all non-post listeners are notified.
	 *
	 * @param listener debug context listener
	 * @param partSecondaryId part secondary identifier
	 * @param partId part identifier
	 * @since 3.7
	 */
	void addPostDebugContextListener(IDebugContextListener listener, String partId, String partSecondaryId);

	/**
	 * Unregisters the given debug context listener for post context change notification
	 * in the specified part of this service's window.
	 *
	 * @param listener debug context listener
	 * @param partSecondaryId part secondary identifier
	 * @param partId part identifier
	 * @since 3.7
	 */
	void removePostDebugContextListener(IDebugContextListener listener, String partId, String partSecondaryId);
}
