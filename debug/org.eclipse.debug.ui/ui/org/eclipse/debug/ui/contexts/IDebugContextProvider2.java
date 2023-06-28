/*****************************************************************
 * Copyright (c) 2010, 2011 Texas Instruments and others
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Patrick Chuong (Texas Instruments) - Allow multiple debug views and
 *     		multiple debug context providers (Bug 327263)
 *****************************************************************/
package org.eclipse.debug.ui.contexts;

import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;


/**
 * This extension to {@link IDebugContextProvider} allows clients to specify the scope that the context
 * provider will apply to. With {@link IDebugContextProvider2}, a provider indicate if it should be a context
 * provider for the entire {@link IWorkbenchWindow} or only for is given {@link IWorkbenchPart}.
 *
 * <p>
 * This interface is intended to be implemented by clients
 * </p>
 *
 * @since 3.7
 */
public interface IDebugContextProvider2 extends IDebugContextProvider {
	/**
	 * Return whether the provider can be set as an active provider for the
	 * window.
	 * <p>
	 * If <code>true</code>, when the provider's part is
	 * activated this provider will become the active debug context provider
	 * for the whole window.  If <code>false</code>, this provider will
	 * only set the active context in a given workbench part.
	 *
	 * @return <code>true</code> if this provider can act as the provider for the entire window, <code>false</code> if it can
	 * only be the provider for its given part.
	 * provider for a window.
	 */
	boolean isWindowContextProvider();
}
