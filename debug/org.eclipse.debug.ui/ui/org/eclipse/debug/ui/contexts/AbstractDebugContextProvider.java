/*******************************************************************************
 * Copyright (c) 2006, 2016 IBM Corporation and others.
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

import org.eclipse.core.runtime.ISafeRunnable;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.ui.IWorkbenchPart;

/**
 * Abstract implementation of a debug context provider.
 * <p>
 * Clients implementing context providers should subclass this class.
 * </p>
 * @since 3.3
 */
public abstract class AbstractDebugContextProvider implements IDebugContextProvider {

	/**
	 * Event listeners
	 */
	private ListenerList<IDebugContextListener> fListeners = new ListenerList<>();

	/**
	 * Part or <code>null</code>
	 */
	private IWorkbenchPart fPart;

	/**
	 * Constructs a context provider for the specified part, possibly <code>null</code>.
	 *
	 * @param part workbench part or <code>null</code>
	 */
	public AbstractDebugContextProvider(IWorkbenchPart part) {
		fPart = part;
	}

	@Override
	public void addDebugContextListener(IDebugContextListener listener) {
		fListeners.add(listener);
	}

	@Override
	public IWorkbenchPart getPart() {
		return fPart;
	}

	@Override
	public void removeDebugContextListener(IDebugContextListener listener) {
		fListeners.remove(listener);
	}

	/**
	 * Fires the given context event to all registered listeners.
	 *
	 * @param event debug context event
	 */
	protected void fire(final DebugContextEvent event) {
		for (IDebugContextListener iDebugContextListener : fListeners) {
			final IDebugContextListener listener = iDebugContextListener;
			SafeRunner.run(new ISafeRunnable() {
				@Override
				public void run() throws Exception {
					listener.debugContextChanged(event);
				}
				@Override
				public void handleException(Throwable exception) {
					DebugUIPlugin.log(exception);
				}
			});

		}
	}
}
