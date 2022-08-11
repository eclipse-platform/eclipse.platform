/*******************************************************************************
 *  Copyright (c) 2000, 2015 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.tests.resources.usecase;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * This operation does not change any resource. It only has one
 * syncPoint in order to inform that it's running.
 */
public class ConcurrentOperation01 extends ConcurrentOperation {

	/** indicates if this operation has entered the run() method */
	protected boolean isRunning;

	public ConcurrentOperation01(IWorkspace workspace) {
		super(workspace);
		reset();
	}

	@Override
	protected void assertRequisites() throws Exception {
	}

	public boolean isRunning() {
		return isRunning;
	}

	@Override
	public void reset() {
		super.reset();
		isRunning = false;
	}

	@Override
	public void run(IProgressMonitor monitor) {
		isRunning = true;
		syncPoint();
	}
}
