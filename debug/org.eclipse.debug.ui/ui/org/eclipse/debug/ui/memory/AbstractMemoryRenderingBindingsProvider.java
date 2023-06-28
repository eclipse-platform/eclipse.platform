/*******************************************************************************
 * Copyright (c) 2004, 2016 IBM Corporation and others.
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

import org.eclipse.core.runtime.ISafeRunnable;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.debug.internal.ui.DebugUIPlugin;

/**
 * Common function for a dynamic memory rendering bindings provider.
 * <p>
 * Clients may subclass this class.
 * </p>
 * @see org.eclipse.debug.ui.memory.IMemoryRenderingBindingsProvider
 * @since 3.1
 */
public abstract class AbstractMemoryRenderingBindingsProvider implements IMemoryRenderingBindingsProvider {

	// list of binding listeners
	private ListenerList<IMemoryRenderingBindingsListener> fListeners;

	@Override
	public void addListener(IMemoryRenderingBindingsListener listener) {
		if (fListeners == null) {
			fListeners = new ListenerList<>();
		}
		fListeners.add(listener);
	}

	@Override
	public void removeListener(IMemoryRenderingBindingsListener listener) {
		if (fListeners != null) {
			fListeners.remove(listener);
		}
	}

	/**
	 * Notifies all registered listeners that bindings have changed.
	 */
	protected void fireBindingsChanged() {
		if (fListeners == null) {
			return;
		}

		for (IMemoryRenderingBindingsListener iMemoryRenderingBindingsListener : fListeners) {
			final IMemoryRenderingBindingsListener listener = iMemoryRenderingBindingsListener;
			ISafeRunnable runnable = new ISafeRunnable() {
				@Override
				public void handleException(Throwable exception) {
					DebugUIPlugin.log(exception);
				}

				@Override
				public void run() throws Exception {
					listener.memoryRenderingBindingsChanged();
				}
			};
			SafeRunner.run(runnable);
		}
	}
}
