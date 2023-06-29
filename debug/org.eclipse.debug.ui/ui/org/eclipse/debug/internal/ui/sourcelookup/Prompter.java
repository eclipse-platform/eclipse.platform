/*******************************************************************************
 * Copyright (c) 2000, 2018 IBM Corporation and others.
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
package org.eclipse.debug.internal.ui.sourcelookup;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IStatusHandler;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.swt.widgets.Display;

/**
 * Prompts the user in the UI (asynchronously), on behalf of a non-UI client,
 * blocking the calling thread until a response is received.
 * <p>
 * This status handler is registered for for the debug UI plug-in,
 * with a status code of <code>STATUS_HANDLER_PROMPT</code>.
 * </p>
 * @since 3.0
 */
public class Prompter implements IStatusHandler {
	/**
	 * Prompts the user for input based on the given status and source
	 * object, blocking the calling thread until the status is resolved.
	 *
	 * @param status client status code for which a status handler must
	 *  be registered
	 * @param source object requesting the status to be resolved
	 * @return result of resolving the given status
	 * @see org.eclipse.debug.core.IStatusHandler#handleStatus(org.eclipse.core.runtime.IStatus, java.lang.Object)
	 */
	@Override
	public Object handleStatus(final IStatus status, final Object source) throws CoreException {
		DebugPlugin dp = DebugPlugin.getDefault();
		// on shutdown the debug plug-in can be null
		if (dp == null) {
			throw new CoreException(new Status(IStatus.INFO,
					IDebugUIConstants.PLUGIN_ID,
					IStatus.OK,
					SourceLookupUIMessages.Prompter_0,
					null));
		}
		final IStatusHandler handler = dp.getStatusHandler(status);
		if (handler == null) {
			throw new CoreException(new Status(IStatus.ERROR,
									IDebugUIConstants.PLUGIN_ID,
									IStatus.OK,
									SourceLookupUIMessages.Prompter_0,
									null));
		}
		Display display = DebugUIPlugin.getStandardDisplay();
		if (display.getThread().equals(Thread.currentThread())) {
			return handler.handleStatus(status, source);
		}
		final Object[] result = new Object[1];
		final CoreException[] exception = new CoreException[1];
		final Object lock = this;
		Runnable r = () -> {
			try {
				result[0] = handler.handleStatus(status, source);
			} catch (CoreException e) {
				exception[0] = e;
			}
			synchronized (lock) {
				lock.notifyAll();
			}
		};
		DebugUIPlugin.getStandardDisplay().syncExec(r);

		if (exception[0] != null ) {
			throw exception[0];
		}
		return result[0];
	}
}
