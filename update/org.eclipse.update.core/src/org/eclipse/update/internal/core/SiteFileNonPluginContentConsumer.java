package org.eclipse.update.internal.core;
/*
 * (c) Copyright IBM Corp. 2000, 2002.
 * All Rights Reserved.
 */
import java.io.*;

import org.eclipse.core.runtime.*;
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
	public SiteFileNonPluginContentConsumer(String featurePath){
		this.path = featurePath;
	}

	/*
	 * @see ISiteContentConsumer#store(ContentReference, IProgressMonitor)
	 */
	public void store(ContentReference contentReference, IProgressMonitor monitor) throws CoreException {
		
		if (closed){
			UpdateManagerPlugin.warn("Attempt to store in a closed SiteFileNonPluginContentConsumer",new Exception());						
			return;
		}	
		
		InputStream inStream = null;
		String featurePath = path;
		String contentKey = contentReference.getIdentifier();
		featurePath += contentKey ;
		try {
			inStream = contentReference.getInputStream();
			UpdateManagerUtils.copyToLocal(inStream, featurePath, null);
			UpdateManagerUtils.checkPermissions(contentReference,featurePath); // 20305
		} catch (IOException e) {
			throw Utilities.newCoreException(Policy.bind("GlobalConsumer.ErrorCreatingFile", featurePath), e); //$NON-NLS-1$
		} finally {
			try {
				// close stream
				inStream.close();
			} catch (Exception e) {}
		}
		
	}

	/*
	 * @see ISiteContentConsumer#close()
	 */
	public void close() {
		if (closed){
			UpdateManagerPlugin.warn("Attempt to close a closed SiteFileNonPluginContentConsumer",new Exception());						
			return;
		}	
		closed = true;		
	}
	
		
}