/*******************************************************************************
 * Copyright (c) 2003, 2012 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM - Initial API and implementation
 *******************************************************************************/
package org.eclipse.core.runtime.jobs;

/**
 * This adapter class provides default implementations for the
 * methods described by the <code>IJobChangeListener</code> interface.
 * <p>
 * Classes that wish to listen to the progress of scheduled jobs can
 * extend this class and override only the methods which they are
 * interested in.
 * </p>
 *
 * @see IJobChangeListener
 * @since 3.0
 */
public class JobChangeAdapter implements IJobChangeListener {
	@Override
	public void aboutToRun(IJobChangeEvent event) {
		// do nothing
	}

	@Override
	public void awake(IJobChangeEvent event) {
		// do nothing
	}

	@Override
	public void done(IJobChangeEvent event) {
		// do nothing
	}

	@Override
	public void running(IJobChangeEvent event) {
		// do nothing
	}

	@Override
	public void scheduled(IJobChangeEvent event) {
		// do nothing
	}

	@Override
	public void sleeping(IJobChangeEvent event) {
		// do nothing
	}
}
