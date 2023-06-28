/*******************************************************************************
 * Copyright (c) 2009, 2015 Wind River Systems and others.
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
package org.eclipse.debug.examples.ui.pda.adapters;

import org.eclipse.core.runtime.IAdapterFactory;

import org.eclipse.debug.core.commands.IRestartHandler;
import org.eclipse.debug.examples.core.pda.model.PDADebugTarget;

/**
 * Adapter factory that provides debug command handler adapters for the
 * PDA debugger.
 *
 * @since 3.6
 */
public class CommandAdapterFactory implements IAdapterFactory {

	private static IRestartHandler fgRestartHandler = new PDARestartDebugCommand();

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getAdapter(Object adaptableObject, Class<T> adapterType) {
		if (IRestartHandler.class.equals(adapterType)) {
			if (adaptableObject instanceof PDADebugTarget) {
				return (T) fgRestartHandler;
			}
		}
		return null;
	}

	@Override
	public Class<?>[] getAdapterList() {
		return new Class[]{IRestartHandler.class};
	}

}
