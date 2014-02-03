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

import java.io.*;
import java.net.*;

import org.eclipse.core.runtime.*;

public class FileResponse implements IResponse {

	protected URL url;
	protected long lastModified;

	protected FileResponse(URL url) {
		this.url = url;
	}

	public InputStream getInputStream() throws IOException {
		return url.openStream();
	}

	public InputStream getInputStream(IProgressMonitor monitor)
		throws IOException, CoreException {
		return getInputStream();
	}

	public long getContentLength() {
		return 0;
	}

	public int getStatusCode() {
		return UpdateCore.HTTP_OK;
	}
	
	public void close() {
		// nothing to close
	}

	public String getStatusMessage() {
		return ""; //$NON-NLS-1$
	}

	public long getLastModified() {
		if (lastModified == 0) {
			File f = new File(url.getFile());
			if (f.isDirectory())
				f = new File(f, "site.xml"); //$NON-NLS-1$
			lastModified = f.lastModified();
		}
		return lastModified;
	}
}
