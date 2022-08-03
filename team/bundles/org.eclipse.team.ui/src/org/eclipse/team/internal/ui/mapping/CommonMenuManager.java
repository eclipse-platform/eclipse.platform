/*******************************************************************************
 * Copyright (c) 2006, 2017 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ui.mapping;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.commands.IHandler;
import org.eclipse.jface.action.MenuManager;

/**
 * An extended menu manager that support handlers
 */
public class CommonMenuManager extends MenuManager {

	private Map<String, IHandler> handlers = new HashMap<>();

	public CommonMenuManager(String id) {
		super(id);
	}

	/**
	 * Clear are the handlers registered with this manager.
	 *
	 */
	public void clearHandlers() {
		handlers.clear();
	}

	/**
	 * Register the handler with the given actionId. Only one
	 * handler can be registered for each action. If multiple
	 * are registered, none will be used.
	 * @param actionId the action id
	 * @param handler the handler
	 */
	public void registerHandler(String actionId, IHandler handler) {
		// TODO Handle conflicts
		handlers.put(actionId, handler);
	}

	public IHandler getHandler(String actionId) {
		return handlers.get(actionId);
	}

}
