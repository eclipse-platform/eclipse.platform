/*******************************************************************************
 * Copyright (c) 2005, 2007 IBM Corporation and others.
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
package org.eclipse.debug.ui.contexts;


/**
 * A debug context listener is notified of debug context events.
 * <p>
 * Clients may implement this interface.
 * </p>
 * @see DebugContextEvent
 * @see IDebugContextManager
 * @see IDebugContextService
 * @see IDebugContextProvider
 * @since 3.3
 */
public interface IDebugContextListener {

	/**
	 * Notification the debug context has changed as specified by the given event.
	 *
	 * @param event debug context event
	 */
	void debugContextChanged(DebugContextEvent event);

}
