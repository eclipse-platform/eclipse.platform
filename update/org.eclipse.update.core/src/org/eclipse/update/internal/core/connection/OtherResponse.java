/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.update.internal.core.connection;

import org.eclipse.update.internal.core.UpdateCore;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

public class OtherResponse extends AbstractResponse {

	protected URL url;
	protected InputStream in;
	protected long lastModified;

	protected OtherResponse(URL url) {
		this.url = url;
	}

	public InputStream getInputStream() throws IOException {
		if (in == null && url != null) {
            if (connection == null)
                connection = url.openConnection();
			in = connection.getInputStream();
			this.lastModified = connection.getLastModified();
		}
		return in;
	}
	
	public void close() {
        if( null != in ) {
                try {
					in.close();
				} catch (IOException e) {
				}
                in = null;
        }
	}	
	
	/**
	 * @see IResponse#getInputStream(IProgressMonitor)
	 */
	public InputStream getInputStream(IProgressMonitor monitor)
		throws IOException, CoreException {
		if (in == null && url != null) {
            if (connection == null)
                connection = url.openConnection();

			if (monitor != null) {
				this.in =
					openStreamWithCancel(connection, monitor);
			} else {
				this.in = connection.getInputStream();
			}
			if (in != null) {
				this.lastModified = connection.getLastModified();
			}
		}
		return in;
	}

	public long getContentLength() {
		if (connection != null)
			return connection.getContentLength();
		return 0;
	}

	public int getStatusCode() {
		return UpdateCore.HTTP_OK;
	}

	public String getStatusMessage() {
		return ""; //$NON-NLS-1$
	}

	public long getLastModified() {
		if (lastModified == 0 && connection != null) {
			lastModified = connection.getLastModified();
		}
		return lastModified;
	}
	

}
