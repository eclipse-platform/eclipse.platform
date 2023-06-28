/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
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
package org.eclipse.debug.internal.ui.commands.actions;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IStatusMonitor;

/**
 * Common function for request monitors
 *
 * @since 3.3
 *
 */
public abstract class AbstractRequestMonitor implements IStatusMonitor {

	private IStatus fStatus;
	private boolean fCancelled = false;

	@Override
	public void setStatus(IStatus status) {
		fStatus = status;
	}

	@Override
	public void beginTask(String name, int totalWork) {
	}

	@Override
	public void internalWorked(double work) {
	}

	@Override
	public boolean isCanceled() {
		return fCancelled;
	}

	@Override
	public void setCanceled(boolean value) {
		fCancelled = value;
	}

	@Override
	public void setTaskName(String name) {
	}

	@Override
	public void subTask(String name) {
	}

	@Override
	public void worked(int work) {
	}

	@Override
	public IStatus getStatus() {
		return fStatus;
	}
}
