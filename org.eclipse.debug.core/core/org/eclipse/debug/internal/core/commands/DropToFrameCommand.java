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
import org.eclipse.debug.core.commands.IDropToFrameHandler;
import org.eclipse.debug.core.model.IDropToFrame;

/**
 * Default drop to frame command for the standard debug model.
 *
 * @since 3.3
 */
public class DropToFrameCommand extends StepCommand implements IDropToFrameHandler {

	@Override
	protected Object getTarget(Object element) {
		return getAdapter(element, IDropToFrame.class);
	}

	@Override
	protected boolean isSteppable(Object target) throws CoreException {
		return ((IDropToFrame)target).canDropToFrame();
	}

	@Override
	protected void step(Object target) throws CoreException {
		((IDropToFrame)target).dropToFrame();
	}

	@Override
	protected Object getEnabledStateJobFamily(IDebugCommandRequest request) {
		return IDropToFrameHandler.class;
	}
}
