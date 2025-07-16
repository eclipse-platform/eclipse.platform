/*******************************************************************************
 * Copyright (c) 2011 - 2025 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 * Alexander Fedorov (ArSysOp) - further evolution
 *******************************************************************************/
package org.eclipse.terminal.view.core;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Terminal service.
 */
public interface ITerminalService {

	/**
	 * Opens a terminal asynchronously and invokes the given callback if done.
	 *
	 * @param properties The terminal properties. Must not be <code>null</code>.
	 * @return the {@link CompletableFuture}
	 */
	public CompletableFuture<?> openConsole(Map<String, Object> properties);

	/**
	 * Close the terminal asynchronously.
	 *
	 * @param properties The terminal properties. Must not be <code>null</code>.
	 * @return the {@link CompletableFuture}
	 */
	public CompletableFuture<?> closeConsole(Map<String, Object> properties);

	/**
	 * Terminate (disconnect) the terminal asynchronously and invokes the given callback if done.
	 *
	 * @param properties The terminal properties. Must not be <code>null</code>.
	 * @return the {@link CompletableFuture}
	 */
	public CompletableFuture<?> terminateConsole(Map<String, Object> properties);

	/**
	 * Register the given listener to receive notifications about terminal events.
	 * Calling this method multiple times with the same listener has no effect.

	 * @param listener The terminal tab listener. Must not be <code>null</code>.
	 */
	public void addTerminalTabListener(ITerminalTabListener listener);

	/**
	 * Unregister the given listener from receiving notifications about terminal
	 * events. Calling this method multiple times with the same listener
	 * has no effect.
	 *
	 * @param listener The terminal tab listener. Must not be <code>null</code>.
	 */
	public void removeTerminalTabListener(ITerminalTabListener listener);

}
