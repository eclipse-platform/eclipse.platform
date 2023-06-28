/*******************************************************************************
 * Copyright (c) 2009 Wind River Systems and others.
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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.IRequest;
import org.eclipse.debug.core.commands.AbstractDebugCommand;
import org.eclipse.debug.core.commands.IEnabledStateRequest;
import org.eclipse.debug.core.commands.IRestartHandler;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.examples.core.pda.model.PDADebugTarget;

/**
 * Restart debug command handler.  It restarts the current debug session.
 */
public class PDARestartDebugCommand extends AbstractDebugCommand implements IRestartHandler {

	@Override
	protected void doExecute(Object[] targets, IProgressMonitor monitor, IRequest request) throws CoreException {
		for (int i = 0; i < targets.length; i++) {
			((PDADebugTarget)targets[i]).restart();
			monitor.worked(1);
		}
	}

	@Override
	protected Object getTarget(Object element) {
		IDebugTarget target = (IDebugTarget)getAdapter(element, IDebugTarget.class);
		if (target instanceof PDADebugTarget) {
			return target;
		}
		return null;
	}

	@Override
	protected boolean isExecutable(Object[] targets, IProgressMonitor monitor, IEnabledStateRequest request)
		throws CoreException
	{
		for (int i = 0; i < targets.length; i++) {
			if (((PDADebugTarget)targets[i]).isTerminated()) {
				return false;
			}
			monitor.worked(1);
		}
		return true;
	}

}
