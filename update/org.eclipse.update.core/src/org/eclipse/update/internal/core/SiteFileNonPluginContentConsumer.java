/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.update.internal.core;
import java.io.*;

import org.eclipse.core.runtime.*;
import org.eclipse.osgi.util.NLS;
import org.eclipse.update.core.*;

/**
 * Plugin Content Consumer on a Site
 */
public class SiteFileNonPluginContentConsumer extends ContentConsumer {

	private String path;
	private boolean closed = false;

	/*
	 * Constructor 
	 */
	public SiteFileNonPluginContentConsumer(String featurePath) {
		this.path = featurePath;
	}

	/*
	 * @see ISiteContentConsumer#store(ContentReference, IProgressMonitor)
	 */
	public void store(ContentReference contentReference, IProgressMonitor monitor) throws CoreException {

		if (closed) {
			UpdateCore.warn("Attempt to store in a closed SiteFileNonPluginContentConsumer", new Exception()); //$NON-NLS-1$
			return;
		}

		InputStream inStream = null;
		String featurePath = path;
		String contentKey = contentReference.getIdentifier();
		featurePath += contentKey;
		try {
			inStream = contentReference.getInputStream();
			UpdateManagerUtils.copyToLocal(inStream, featurePath, null);
			UpdateManagerUtils.checkPermissions(contentReference, featurePath); // 20305
		} catch (IOException e) {
			throw Utilities.newCoreException(NLS.bind(Messages.GlobalConsumer_ErrorCreatingFile, (new String[] { featurePath })), e);
		} finally {
			if (inStream != null) {
				try {
					// close stream
					inStream.close();
				} catch (IOException e) {
				}
			}
		}

	}

	/*
	 * @see ISiteContentConsumer#close()
	 */
	public void close() {
		if (closed) {
			UpdateCore.warn("Attempt to close a closed SiteFileNonPluginContentConsumer", new Exception()); //$NON-NLS-1$
			return;
		}
		closed = true;
	}

}
