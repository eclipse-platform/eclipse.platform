/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.update.internal.core;
import java.io.*;
import java.net.*;
import java.util.*;

import org.eclipse.core.runtime.*;
import org.eclipse.update.core.*;

/**
 * Plugin Content Consumer on a Site
 */
public class SiteFilePluginContentConsumer extends ContentConsumer {

	private IPluginEntry pluginEntry;
	private ISite site;
	private boolean closed = false;

	// recovery
	// temporary name to original name map
	private Map renames = new HashMap(2);

	// for abort
	private List /*of path as String */
	installedFiles;

	/*
	 * Constructor
	 */
	public SiteFilePluginContentConsumer(IPluginEntry pluginEntry, ISite site) {
		this.pluginEntry = pluginEntry;
		this.site = site;
		installedFiles = new ArrayList();
	}

	/*
	 * @see ISiteContentConsumer#store(ContentReference, IProgressMonitor)
	 */
	public void store(ContentReference contentReference, IProgressMonitor monitor) throws CoreException {
		InputStream inStream = null;
		String pluginPath = null;

		if (closed) {
			UpdateCore.warn("Attempt to store in a closed SiteFilePluginContentConsumer", new Exception()); //$NON-NLS-1$
			return;
		}

		try {
			URL newURL = new URL(site.getURL(), Site.DEFAULT_PLUGIN_PATH + pluginEntry.getVersionedIdentifier().toString());
			pluginPath = newURL.getFile(); 
			String contentKey = contentReference.getIdentifier();
			inStream = contentReference.getInputStream();
			pluginPath += pluginPath.endsWith(File.separator) ? contentKey : File.separator + contentKey;

			// error recovery
			String logEntry=null;
			if ("plugin.xml".equals(contentKey)) { //$NON-NLS-1$
				logEntry=ErrorRecoveryLog.PLUGIN_ENTRY;
			} else if ("fragment.xml".equals(contentKey)) { //$NON-NLS-1$
				logEntry=ErrorRecoveryLog.FRAGMENT_ENTRY;
			} else if ("META-INF/MANIFEST.MF".equals(contentKey)) { //$NON-NLS-1$
				logEntry=ErrorRecoveryLog.BUNDLE_MANIFEST_ENTRY;
			}
			if (logEntry!=null) {
				String originalName = pluginPath.replace(File.separatorChar, '/');
				File localFile = new File(originalName);
				if (localFile.exists()) {
					throw Utilities.newCoreException(Policy.bind("UpdateManagerUtils.FileAlreadyExists", new Object[] { localFile }), null); //$NON-NLS-1$
				}
				pluginPath = ErrorRecoveryLog.getLocalRandomIdentifier(pluginPath);
				renames.put(pluginPath, originalName);
				ErrorRecoveryLog.getLog().appendPath(logEntry, pluginPath);
			}
			//
			UpdateManagerUtils.copyToLocal(inStream, pluginPath, null);
			UpdateManagerUtils.checkPermissions(contentReference, pluginPath); // 20305
			installedFiles.add(pluginPath);
		} catch (IOException e) {
			throw Utilities.newCoreException(Policy.bind("GlobalConsumer.ErrorCreatingFile", pluginPath), e);//$NON-NLS-1$
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
	public void close() throws CoreException {

		if (closed) {
			UpdateCore.warn("Attempt to close a closed SiteFilePluginContentConsumer", new Exception()); //$NON-NLS-1$
			return;
		}

		for(Iterator it = renames.entrySet().iterator(); it.hasNext();){
			// rename file 
			Map.Entry entry = (Map.Entry)it.next();
			String temporary = (String) entry.getKey();
			String original = (String) entry.getValue();
			ErrorRecoveryLog.getLog().appendPath(ErrorRecoveryLog.RENAME_ENTRY, temporary);
			File fileToRename = new File(temporary);
			boolean sucess = false;
			if (fileToRename.exists()) {
				File renamedFile = new File(original);
				sucess = fileToRename.renameTo(renamedFile);
			}
			if (!sucess) {
				String msg = Policy.bind("ContentConsumer.UnableToRename", temporary, original); //$NON-NLS-1$
				throw Utilities.newCoreException(msg, new Exception(msg));
			}
		}

		if (site instanceof SiteFile)
			 ((SiteFile) site).addPluginEntry(pluginEntry);
		closed = true;
	}

	/*
	 * 
	 */
	public void abort() throws CoreException {

		if (closed) {
			UpdateCore.warn("Attempt to abort a closed SiteFilePluginContentConsumer", new Exception()); //$NON-NLS-1$
			return;
		}

		boolean success = true;
		InstallRegistry.unregisterPlugin(pluginEntry);

		// delete plugin manifests first
		for(Iterator it = renames.values().iterator(); it.hasNext();){
			String originalName = (String) it.next();

			ErrorRecoveryLog.getLog().appendPath(ErrorRecoveryLog.DELETE_ENTRY, originalName);
			File fileToRemove = new File(originalName);
			if (fileToRemove.exists()) {
				if(!fileToRemove.delete()){
					String msg = Policy.bind("Unable to delete", originalName); //$NON-NLS-1$
					UpdateCore.log(msg, null);	
					success = false;
				}
			}
		}

		if (success) {
			// remove the plugin files;
			Iterator iter = installedFiles.iterator();
			File featureFile = null;
			while (iter.hasNext()) {
				String path = (String) iter.next();
				featureFile = new File(path);
				UpdateManagerUtils.removeFromFileSystem(featureFile);
			}

			// remove the plugin directory if empty
			try {
				URL newURL = new URL(site.getURL(), Site.DEFAULT_PLUGIN_PATH + pluginEntry.getVersionedIdentifier().toString());
				String pluginPath = newURL.getFile();
				UpdateManagerUtils.removeEmptyDirectoriesFromFileSystem(new File(pluginPath));
			} catch (MalformedURLException e) {
				throw Utilities.newCoreException(e.getMessage(), e);
			}
		}
		closed = true;
	}

}
