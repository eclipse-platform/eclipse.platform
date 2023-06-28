/*******************************************************************************
 * Copyright (c) 2006, 2018 IBM Corporation and others.
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

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.IRequest;
import org.eclipse.debug.core.commands.AbstractDebugCommand;
import org.eclipse.debug.core.commands.IEnabledStateRequest;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.debug.core.model.IStep;
import org.eclipse.debug.core.model.IThread;

/**
 * Common function for step commands.
 *
 * @since 3.3
 */
public abstract class StepCommand extends AbstractDebugCommand {

	@Override
	protected void doExecute(Object[] targets, IProgressMonitor monitor, IRequest request) throws CoreException {
		for (Object target : targets) {
			step(target);
		}
	}

	protected abstract void step(Object target) throws CoreException ;

	@Override
	protected boolean isExecutable(Object[] targets, IProgressMonitor monitor, IEnabledStateRequest collector) throws CoreException {
		if (isThreadCompatible(targets)) {
			for (Object target : targets) {
				if (!isSteppable(target)) {
					return false;
				}
			}
			return true;
		} else {
			return false;
		}
	}

	protected abstract boolean isSteppable(Object target) throws CoreException;

	protected boolean isThreadCompatible(Object[] targets) {
		if (targets.length == 1) {
			return true;
		}
		// check if frames from same thread
		Set<IThread> threads = new HashSet<>(targets.length);
		for (Object object : targets) {
			IStackFrame frame = null;
			if (object instanceof IStackFrame) {
				frame = (IStackFrame) object;
			} else if (object instanceof IAdaptable) {
				frame = ((IAdaptable)object).getAdapter(IStackFrame.class);
			}
			if (frame != null) {
				if (!threads.add(frame.getThread())) {
					return false;
				}
			}
		}
		return true;
	}

	@Override
	protected Object getTarget(Object element) {
		return getAdapter(element, IStep.class);
	}

}
