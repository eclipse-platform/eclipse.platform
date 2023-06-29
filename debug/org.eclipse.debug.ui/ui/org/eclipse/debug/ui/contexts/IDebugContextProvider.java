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

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchPart;

/**
 * Interface common to all objects that provide a debug context. A context provider
 * is registered with a debug context service associated with a specific window.
 * <p>
 * A context provider can provide context information for a specific workbench part.
 * There can only be one context provider registered per part with a context
 * service. When there is more than one context provider per window, the context provider
 * associated with the most recently active part provides the context for that window.
 * </p>
 * <p>
 * A context provider does not have to be associated with a part. In this case the provider
 * specifies <code>null</code> for its part, and provides context information for the window.
 * There can only be one context provider without an associated part registered per context
 * service (i.e. per window). A context provider that provides context without an associated
 * part is only active (i.e. used to provide context information) when there are no other
 * context providers with associated parts registered with that service.
 * </p>
 * <p>
 * Clients may implement this interface.
 * </p>
 * @see IDebugContextManager
 * @see IDebugContextService
 * @see IDebugContextListener
 * @since 3.3
 */
public interface IDebugContextProvider {

	/**
	 * Returns the part associated with this context provider or <code>null</code>
	 * if none.
	 *
	 * @return part associated with this context provider or <code>null</code>
	 */
	IWorkbenchPart getPart();

	/**
	 * Registers the given listener for debug context events.
	 *
	 * @param listener event listener
	 */
	void addDebugContextListener(IDebugContextListener listener);

	/**
	 * Unregisters the given listener for debug context events.
	 *
	 * @param listener event listener
	 */
	void removeDebugContextListener(IDebugContextListener listener);

	/**
	 * Returns the currently active context, possibly empty or <code>null</code>.
	 *
	 * @return active context, possibly empty or <code>null</code>.
	 */
	ISelection getActiveContext();

}
