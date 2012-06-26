/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

public interface IResponse {

	/**
	 * Method getInputStream.
	 * 
	 * @return InputStream
	 */
	public InputStream getInputStream() throws IOException;

	/**
	 * A special version of 'getInputStream' that can be canceled.
	 * A monitor thread checks the state of the monitor
	 * and disconnects the connection if 'isCanceled()' is detected.
	 * 
	 * @param monitor
	 *            the progress monitor
	 * @return InputStream an opened stream or null if failed.
	 * @throws IOException
	 *             if there are problems
	 * @throws CoreException
	 *             if no more connection threads are available
	 */
	public InputStream getInputStream(IProgressMonitor monitor)
		throws IOException, CoreException, TooManyOpenConnectionsException;

	/**
	 * Method getContentLength.
	 * 
	 * @return long
	 */
	public long getContentLength();

	/**
	 * Method getStatusCode.
	 * 
	 * @return int
	 */
	public int getStatusCode();

	/**
	 * Method getStatusMessage.
	 * 
	 * @return String
	 */
	public String getStatusMessage();

	/**
	 * Returns the timestamp of last modification to the resource
	 * 
	 * @return
	 */
	public long getLastModified();
	
	/**
	 * Close the connection if open.
	 */
	public void close();
}
