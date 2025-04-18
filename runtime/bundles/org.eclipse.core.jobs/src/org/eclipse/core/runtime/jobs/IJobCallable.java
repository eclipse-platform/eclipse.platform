/*******************************************************************************
 * Copyright (c) 2023 Christoph Läubrich and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.runtime.jobs;

import org.eclipse.core.runtime.IProgressMonitor;

/**
 * A computation running inside a job
 *
 * @since 3.14
 *
 */
public interface IJobCallable<T> {

	/**
	 * Calles to compute a result inside a job.
	 *
	 * @param monitor the monitor, never <code>null</code>
	 * @return the result of the computation
	 * @throws Exception if anything goes wrong
	 */
	T call(IProgressMonitor monitor) throws Exception;

}
