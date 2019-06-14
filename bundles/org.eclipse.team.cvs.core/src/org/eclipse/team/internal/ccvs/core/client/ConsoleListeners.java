/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
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
package org.eclipse.team.internal.ccvs.core.client;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.team.internal.ccvs.core.client.listeners.IConsoleListener;

/**
 * Class which forwards CVS console output to 1 or more
 * registered console listeners.
 */
public class ConsoleListeners implements IConsoleListener {

	private static ConsoleListeners instance = new ConsoleListeners();
	private Set listeners = new HashSet();
	
	/**
	 * Return the console listeners
	 * @return the console listeners
	 */
	public static ConsoleListeners getInstance() {
		return instance;
	}
	
	public void addListener(IConsoleListener listener) {
		synchronized(listeners) {
			listeners.add(listener);
		}
	}
	
	public void removeListener(IConsoleListener listener) {
		synchronized(listeners) {
			listeners.remove(listener);
		}
	}

	private IConsoleListener[] getListeners() {
		synchronized(listeners) {
			return (IConsoleListener[]) listeners.toArray(new IConsoleListener[listeners.size()]);
		}
	}
	
	@Override
	public void commandInvoked(final Session session, final String line) {
		if (listeners.isEmpty()) return;
		IConsoleListener[] listeners = getListeners();
		for (IConsoleListener listener : listeners) {
			Platform.run(new ISafeRunnable() {
				public void handleException(Throwable exception) {
					// Exception logged by Platform
				}
				public void run() throws Exception {
					listener.commandInvoked(session, line);
				}
			});
		}
	}

	@Override
	public void messageLineReceived(final Session session, final String line, final IStatus status) {
		if (listeners.isEmpty()) return;
		IConsoleListener[] listeners = getListeners();
		for (IConsoleListener listener : listeners) {
			Platform.run(new ISafeRunnable() {
				public void handleException(Throwable exception) {
					// Exception logged by Platform
				}
				public void run() throws Exception {
					listener.messageLineReceived(session, line, status);
				}
			});
		}
	}

	@Override
	public void errorLineReceived(final Session session, final String line, final IStatus status) {
		if (listeners.isEmpty()) return;
		IConsoleListener[] listeners = getListeners();
		for (IConsoleListener listener : listeners) {
			Platform.run(new ISafeRunnable() {
				public void handleException(Throwable exception) {
					// Exception logged by Platform
				}
				public void run() throws Exception {
					listener.errorLineReceived(session, line, status);
				}
			});
		}
	}

	@Override
	public void commandCompleted(final Session session, final IStatus status, final Exception exception) {
		if (listeners.isEmpty()) return;
		IConsoleListener[] listeners = getListeners();
		for (IConsoleListener listener : listeners) {
			Platform.run(new ISafeRunnable() {
				public void handleException(Throwable exception) {
					// Exception logged by Platform
				}
				public void run() throws Exception {
					listener.commandCompleted(session, status, exception);
				}
			});
		}
	}
}
