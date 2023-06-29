/*******************************************************************************
 * Copyright (c) 2011 Wind River Systems and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.debug.internal.ui.viewers.model.provisional;

import org.eclipse.debug.internal.ui.viewers.provisional.AbstractModelProxy;
import org.eclipse.jface.viewers.Viewer;

/**
 * Extension to the model proxy interface which allows the proxy to be initialized
 * on the viewer's Display thread
 *
 * @noimplement Clients are not intended to implement this interface directly. Instead, clients
 * creating and firing model deltas should create instances of {@link AbstractModelProxy}.
 * @since 3.8
 */
public interface IModelProxy2 extends IModelProxy {

	/**
	 * Initialize model proxy with given tree model viewer.  This method is
	 * called on the viewer's Display thread and is guaranteed to be called
	 * before the dispose() method is called on the same proxy.  The default
	 * implementation of this method calls {@link #init(IPresentationContext)}
	 * and {@link #installed(Viewer)} asynchornously and not in the Display
	 * thread.
	 * <p>
	 * This method is called by the asynchronous viewer framework and should
	 * not be called by clients.
	 * </p>
	 * @param viewer Viewer that is installing this model proxy.
	 *
	 */
	void initialize(ITreeModelViewer viewer);

}
