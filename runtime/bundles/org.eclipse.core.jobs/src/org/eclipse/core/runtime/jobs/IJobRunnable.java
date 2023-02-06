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
 * A pice of coude that runs inside a job and has access to a progressmonitor
 *
 * @since 3.14
 *
 */
public interface IJobRunnable {

	/**
	 * Method to run inside a job
	 *
	 * @param monitor the monitor, never <code>null</code>.
	 * @throws Exception in case of error
	 */
	void run(IProgressMonitor monitor) throws Exception;

}
