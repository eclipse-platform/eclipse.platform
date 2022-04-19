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

/**
 * An interface to mark an Operation that needs an ISchedulingRule.
 *
 * @since 3.13
 *
 */
public interface ISchedulableOperation {
	/**
	 * @return an ISchedulingRule that the operation will acquire - if any. A caller
	 *         can make sure that this ISchedulableOperation is locked before
	 *         calling the operation. Returns null if no rule needed.
	 *
	 * @since 3.13
	 */
	ISchedulingRule getSchedulingRule();
}
