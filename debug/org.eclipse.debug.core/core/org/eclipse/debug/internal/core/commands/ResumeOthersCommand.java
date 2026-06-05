/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation.
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

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.IRequest;
import org.eclipse.debug.core.commands.IDebugCommandRequest;
import org.eclipse.debug.core.commands.IResumeOthersHandler;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IThread;

/**
 * Default resume others command for the standard debug model.
 *
 * @since 3.24
 */
public class ResumeOthersCommand extends SuspendCommand implements IResumeOthersHandler {

	@Override
	protected void execute(Object target) throws CoreException {
		IThread currentThread = (IThread) target;
		if (currentThread.canResume()) {
			currentThread.resume();
		}
	}
	@Override
	protected void doExecute(Object[] targets, IProgressMonitor monitor, IRequest request) throws CoreException {

		Map<IDebugTarget, Set<IThread>> selectedTargetThreads = Arrays.stream(targets).map(IThread.class::cast).collect(Collectors.groupingBy(IThread::getDebugTarget, Collectors.toSet()));
		for (Map.Entry<IDebugTarget, Set<IThread>> entry : selectedTargetThreads.entrySet()) {
			IDebugTarget debugTarget = entry.getKey();
			Set<IThread> selectedThreads = entry.getValue();
			for (IThread thread : debugTarget.getThreads()) {
				if (!selectedThreads.contains(thread)) {
					execute(thread);
					monitor.worked(1);
				}
			}
		}
	}

	@Override
	protected boolean isExecutable(Object target) {
		return ((IThread) target).canResume();
	}

	@Override
	protected Object getEnabledStateJobFamily(IDebugCommandRequest request) {
		return IResumeOthersHandler.class;
	}

}
