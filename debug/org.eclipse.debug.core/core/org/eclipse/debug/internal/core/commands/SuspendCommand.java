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
import org.eclipse.debug.core.commands.ISuspendHandler;
import org.eclipse.debug.core.model.ISuspendResume;

/**
 * Default suspend command for the standard debug model.
 *
 * @since 3.3
 */
public class SuspendCommand extends ForEachCommand implements ISuspendHandler {

	@Override
	protected Object getTarget(Object element) {
		return getAdapter(element, ISuspendResume.class);
	}

	@Override
	protected void execute(Object target) throws CoreException {
		((ISuspendResume)target).suspend();
	}

	@Override
	protected boolean isExecutable(Object target) {
		return ((ISuspendResume)target).canSuspend();
	}

	@Override
	protected Object getEnabledStateJobFamily(IDebugCommandRequest request) {
		return ISuspendHandler.class;
	}
}
