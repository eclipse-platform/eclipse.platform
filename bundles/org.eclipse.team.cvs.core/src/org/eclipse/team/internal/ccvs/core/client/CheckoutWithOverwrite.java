/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
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
package org.eclipse.team.internal.ccvs.core.client;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.team.internal.ccvs.core.CVSException;
import org.eclipse.team.internal.ccvs.core.client.listeners.ICommandOutputListener;

/**
 * This checkout will overwrite any resources even for created responses
 */
public class CheckoutWithOverwrite extends Checkout {

	/**
	 * This class overrides the "Created" handler but uses the "Updated"
	 * behavior which will overwrite existing files.
	 */
	public class CreatedResponseHandler extends UpdatedHandler {
		public CreatedResponseHandler() {
			super(UpdatedHandler.HANDLE_UPDATED);
		}
		public String getResponseID() {
			return "Created"; //$NON-NLS-1$
		}
	}

	@Override
	protected IStatus doExecute(
		Session session,
		GlobalOption[] globalOptions,
		LocalOption[] localOptions,
		String[] arguments,
		ICommandOutputListener listener,
		IProgressMonitor monitor)
		throws CVSException {
		
		ResponseHandler newCreated = new CreatedResponseHandler();
		ResponseHandler oldCreated = session.getResponseHandler(newCreated.getResponseID());
		session.registerResponseHandler(newCreated);
		try {
			return super.doExecute(
				session,
				globalOptions,
				localOptions,
				arguments,
				listener,
				monitor);
		} finally {
			session.registerResponseHandler(oldCreated);
		}
	}

}
