/*******************************************************************************
 * Copyright (c) 2000, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.update.internal.core.connection;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.update.internal.core.UpdateCore;

/**
 * @author btripkov
 *
 */
public abstract class AbstractResponse implements IResponse {

	private static final long POLLING_INTERVAL = 200;
	protected URLConnection connection;

	protected InputStream openStreamWithCancel(URLConnection urlConnection, IProgressMonitor monitor) throws IOException, CoreException, TooManyOpenConnectionsException {
	
		ConnectionThreadManager.StreamRunnable runnable =
			new ConnectionThreadManager.StreamRunnable(urlConnection);
		Thread t = ConnectionThreadManagerFactory.getConnectionManager().getConnectionThread(
				runnable);
		t.start();
		InputStream is = null;
		try {
			for (;;) {
				if (monitor.isCanceled()) {
					runnable.disconnect();
	                connection = null;
					break;
				}
				if (runnable.getInputStream() != null || !t.isAlive()) {
					is = runnable.getInputStream();
					break;
				}
				if (runnable.getIOException() != null) 
					throw runnable.getIOException();
				if (runnable.getException() != null) 
						throw new CoreException(new Status(IStatus.ERROR,
															UpdateCore.getPlugin().getBundle().getSymbolicName(), 
															IStatus.OK,
															runnable.getException().getMessage(), 
															runnable.getException()));
				t.join(POLLING_INTERVAL);
				}
		} catch (InterruptedException e) {
		}
		return is;
	}

	
	
}
