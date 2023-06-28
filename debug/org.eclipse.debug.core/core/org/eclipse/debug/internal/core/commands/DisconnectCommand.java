/*******************************************************************************
 * Copyright (c) 2006, 2013 IBM Corporation and others.
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
package org.eclipse.debug.internal.core.commands;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.commands.IDebugCommandRequest;
import org.eclipse.debug.core.commands.IDisconnectHandler;
import org.eclipse.debug.core.model.IDisconnect;

/**
 * Default disconnect command for the standard debug model.
 *
 * @since 3.3
 */
public class DisconnectCommand extends ForEachCommand implements IDisconnectHandler {

	@Override
	protected Object getTarget(Object element) {
		return getAdapter(element, IDisconnect.class);
	}

	@Override
	protected void execute(Object target) throws CoreException {
		((IDisconnect)target).disconnect();
	}

	@Override
	protected boolean isExecutable(Object target) {
		return ((IDisconnect)target).canDisconnect();
	}

	@Override
	protected Object getEnabledStateJobFamily(IDebugCommandRequest request) {
		return IDisconnectHandler.class;
	}
}
