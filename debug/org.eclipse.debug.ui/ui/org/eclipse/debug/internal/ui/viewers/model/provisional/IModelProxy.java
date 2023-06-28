/*******************************************************************************
 * Copyright (c) 2005, 2011 IBM Corporation and others.
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
package org.eclipse.debug.internal.ui.viewers.model.provisional;

import org.eclipse.debug.internal.ui.viewers.provisional.AbstractModelProxy;
import org.eclipse.jface.viewers.Viewer;

/**
 * A model proxy represents a model for a specific presentation context and
 * fires deltas to notify listeners of changes in the model. A model proxy
 * is created by a model proxy factory.
 * <p>
 * When an element is added to an asynchronous viewer, its associated model proxy
 * factory is queried to create a model proxy for that element. The model proxy
 * is then installed into the viewer and the viewer listens to model deltas
 * in order to update that element. Generally, a model proxy factory creates
 * model proxies for root elements in a model, and then represents all elements
 * within that model for a specific presentation context.
 * </p>
 * <p>
 * Note: provider methods are called in the Display thread of the viewer.
 * To avoid blocking the UI, long running operations should be performed
 * asynchronously.
 * </p>
 *
 * @noimplement Clients are not intended to implement this interface directly. Instead, clients
 * creating and firing model deltas should create instances of {@link AbstractModelProxy}.
 * @see IModelDelta
 * @see IModelProxyFactory
 * @see IModelChangedListener
 * @see ICheckboxModelProxy
 * @see IModelProxy2
 * @since 3.2
 */
public interface IModelProxy {

	/**
	 * Notification this model proxy has been created and is about to be installed
	 * in the following context. This is the first method called after a model proxy
	 * is created and it's called in a job thread and not on a display thread.
	 * <p>
	 * This method is called by the asynchronous viewer framework and should not
	 * be called by clients.
	 * </p>
	 * @param context presentation context in which the proxy will be installed
	 * @see IModelProxy2#initialize(ITreeModelViewer)
	 */
	void init(IPresentationContext context);

	/**
	 * Notification this model proxy has been installed in the specified
	 * viewer. This indicates that the model proxy has been created and registered
	 * model change listeners are ready to process deltas.  This method is called
	 * by the {@link AbstractModelProxy} base class using a job and NOT in viewers
	 * display thread. It allows the client to initialize the proxy without
	 * blocking the UI. The default implementaiton is a no-op.
	 * <p>
	 * This method is called by the asynchronous viewer framework and should not
	 * be called by clients.
	 * </p>
	 * @param viewer viewer
	 * @see IModelProxy2#initialize(ITreeModelViewer)
	 * @since 3.3
	 */
	void installed(Viewer viewer);

	/**
	 * Disposes this model proxy.
	 * <p>
	 * This method is called by the asynchronous viewer framework and should not
	 * be called by clients.
	 * </p>
	 */
	void dispose();

	/**
	 * Registers the given listener for model delta notification.
	 *
	 * @param listener model delta listener
	 */
	void addModelChangedListener(IModelChangedListener listener);

	/**
	 * Unregisters the given listener from model delta notification.
	 *
	 * @param listener model delta listener
	 */
	void removeModelChangedListener(IModelChangedListener listener);

	/**
	 * Returns whether this proxy has been disposed.
	 *
	 * @return whether this proxy has been disposed
	 * @since 3.3
	 */
	boolean isDisposed();

}
