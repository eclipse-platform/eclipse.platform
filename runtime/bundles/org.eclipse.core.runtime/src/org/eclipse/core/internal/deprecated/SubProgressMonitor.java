/*******************************************************************************
 * Copyright (c) 2023 SSI and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     SSI - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.internal.deprecated;

import org.eclipse.core.runtime.IProgressMonitor;

@SuppressWarnings("deprecation")
/**
 * A not deprecated version of SubProgressMonitor
 */
public class SubProgressMonitor extends org.eclipse.core.runtime.SubProgressMonitor {

	protected SubProgressMonitor(IProgressMonitor monitor, int ticks, int style) {
		super(monitor, ticks, style);
	}

	protected SubProgressMonitor(IProgressMonitor monitor, int ticks) {
		super(monitor, ticks);
	}

	@SuppressWarnings("hiding")
	public static final int SUPPRESS_SUBTASK_LABEL = org.eclipse.core.runtime.SubProgressMonitor.SUPPRESS_SUBTASK_LABEL;
	@SuppressWarnings("hiding")
	public static final int PREPEND_MAIN_LABEL_TO_SUBTASK = org.eclipse.core.runtime.SubProgressMonitor.PREPEND_MAIN_LABEL_TO_SUBTASK;

	public static SubProgressMonitor create(IProgressMonitor monitor, int ticks, int style) {
		return new SubProgressMonitor(monitor, ticks, style);
	}

	public static SubProgressMonitor create(IProgressMonitor monitor, int ticks) {
		return new SubProgressMonitor(monitor, ticks);
	}

	@Override
	public void worked(int work) {
		super.worked(work);
	}

	@Override
	public void subTask(String name) {
		super.subTask(name);
	}

	@Override
	public void done() {
		super.done();
	}

	@Override
	public void beginTask(String name, int totalWork) {
		super.beginTask(name, totalWork);
	}
}
