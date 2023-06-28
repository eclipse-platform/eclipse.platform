/*******************************************************************************
 * Copyright (c) 2006, 2013 IBM Corporation and others.
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
 *     John Cortell (Freescale) - update javadoc tags (Bug 292301)
 *******************************************************************************/
package org.eclipse.debug.internal.core.commands;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.debug.core.IRequest;

/**
 * @since 3.3
 */
public class Request implements IRequest {

	private IStatus fStatus;
	private boolean fCanceled = false;

	@Override
	public void done() {
	}

	@Override
	public IStatus getStatus() {
		return fStatus;
	}

	@Override
	public void setStatus(IStatus status) {
		fStatus = status;
	}

	@Override
	public synchronized void cancel() {
		fCanceled = true;
	}

	@Override
	public synchronized boolean isCanceled() {
		return fCanceled;
	}

}
