/*******************************************************************************
 * Copyright (c) 2022 Joerg Kubitz.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Joerg Kubitz - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.runtime.jobs;

import org.eclipse.core.runtime.IProgressMonitor;

/**
 * An interface to mark an operation that needs an {@link ISchedulingRule}.
 *
 * @since 3.13
 */
public interface ISchedulableOperation {
	/**
	 * Gives the caller a hint whether this operation will acquire a rule to proceed
	 * in the current thread. If a {@link ISchedulingRule} is returned the caller
	 * should call {@link IJobManager#beginRule(ISchedulingRule, IProgressMonitor)}
	 * before and {@link IJobManager#endRule(ISchedulingRule)} after the operation.
	 *
	 * @return an {@link ISchedulingRule} that the operation will acquire in the
	 *         current thread - if any. Returns {@code null} if no rule needed - in
	 *         that case the caller should not call <code>beginRule</code> or
	 *         <code>endRule</code>. As this method returns only a hint the
	 *         operation can not assume that the caller already acquired the rule.
	 *         The operation still has to acquire it - which will lead to a nested
	 *         rule.
	 *
	 * @since 3.13
	 */
	ISchedulingRule getSchedulingRule();
}
