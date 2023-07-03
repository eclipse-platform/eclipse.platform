/*******************************************************************************
 * Copyright (c) 2007, 2013 IBM Corporation and others.
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
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.debug.core.IRequest;
import org.eclipse.debug.core.commands.AbstractDebugCommand;
import org.eclipse.debug.core.commands.IDebugCommandRequest;
import org.eclipse.debug.core.commands.IEnabledStateRequest;

/**
 * A command that operates on each element individually.
 *
 * @since 3.3
 */
public abstract class ForEachCommand extends AbstractDebugCommand {

	private final ExclusiveRule exclusiveRule = new ExclusiveRule();

	@Override
	protected void doExecute(Object[] targets, IProgressMonitor monitor, IRequest request) throws CoreException {
		for (Object target : targets) {
			execute(target);
			monitor.worked(1);
		}
	}

	protected abstract void execute(Object target) throws CoreException;

	@Override
	protected boolean isExecutable(Object[] targets, IProgressMonitor monitor, IEnabledStateRequest request) throws CoreException {
		for (Object target : targets) {
			if (!isExecutable(target)) {
				return false;
			}
			monitor.worked(1);
		}
		return true;
	}

	protected abstract boolean isExecutable(Object target);

	/*
	 * Do not allow parallel update requests for the same command, since those
	 * can result in race conditions, where one selected element enables a
	 * command and another selected element disables the command. Depending on
	 * which request is processed first, the debug command could end up with the
	 * wrong enabled state. See bug 560274.
	 */
	@Override
	protected ISchedulingRule getEnabledStateSchedulingRule(IDebugCommandRequest request) {
		return exclusiveRule;
	}

	static class ExclusiveRule implements ISchedulingRule {

		@Override
		public boolean contains(ISchedulingRule rule) {
			return rule == this;
		}

		@Override
		public boolean isConflicting(ISchedulingRule rule) {
			return contains(rule);
		}
	}
}
