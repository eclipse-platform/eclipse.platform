/*******************************************************************************
 *  Copyright (c) 2000, 2010 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.update.core;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.osgi.util.NLS;
import org.eclipse.update.core.model.ContentEntryModel;
import org.eclipse.update.core.model.InstallAbortedException;
import org.eclipse.update.core.model.NonPluginEntryModel;
import org.eclipse.update.core.model.PluginEntryModel;
import org.eclipse.update.internal.core.FatalIOException;
import org.eclipse.update.internal.core.FeatureDownloadException;
import org.eclipse.update.internal.core.FileFragment;
import org.eclipse.update.internal.core.InternalSiteManager;
import org.eclipse.update.internal.core.LockManager;
import org.eclipse.update.internal.core.Messages;
import org.eclipse.update.internal.core.UpdateCore;
import org.eclipse.update.internal.core.UpdateManagerUtils;

/**
 * Base implementation of a feature content provider. This class provides a set
 * of helper methods useful for implementing feature content providers. In
 * particular, methods dealing with downloading and caching of feature files.
 * <p>
 * This class must be subclassed by clients.
 * </p>
 * <p>
 * <b>Note:</b> This class/interface is part of an interim API that is still under development and expected to
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
 * (repeatedly) as the API evolves.
 * </p>
 * @see org.eclipse.update.core.IFeatureContentProvider
 * @since 2.0
 * @deprecated The org.eclipse.update component has been replaced by Equinox p2.
 * This API will be deleted in a future release. See bug 311590 for details.
 */
public abstract class FeatureContentProvider implements IFeatureContentProvider {

	/**
	 *  
	 */
	public class FileFilter {

		private IPath filterPath = null;

		/**
		 * Constructor for FileFilter.
		 */
		public FileFilter(String filter) {
			super();
			this.filterPath = new Path(filter);
		}

		/**
		 * returns true if the name matches the rule
		 */
		public boolean accept(String name) {

			if (name == null)
				return false;

			// no '*' pattern matching
			// must be equals
			IPath namePath = new Path(name);
			if (filterPath.lastSegment().indexOf('*') == -1) {
				return filterPath.equals(namePath);
			}

			// check same file extension if extension exists (a.txt/*.txt)
			// or same file name (a.txt,a.*)
			String extension = filterPath.getFileExtension();
			if (extension != null && !extension.equals("*")) { //$NON-NLS-1$
				if (!extension.equalsIgnoreCase(namePath.getFileExtension()))
					return false;
			} else {
				IPath noExtension = filterPath.removeFileExtension();
				String fileName = noExtension.lastSegment();
				if (!fileName.equals("*")) { //$NON-NLS-1$
					if (!namePath.lastSegment().startsWith(fileName))
						return false;
				}
			}

			// check same path
			IPath p1 = namePath.removeLastSegments(1);
			IPath p2 = filterPath.removeLastSegments(1);
			return p1.equals(p2);
		}

	}

	private URL base;
	private IFeature feature;
	private File tmpDir; // local work area for each provider
	public static final String JAR_EXTENSION = ".jar"; //$NON-NLS-1$	

	private static final String DOT_PERMISSIONS = "permissions.properties"; //$NON-NLS-1$
	private static final String EXECUTABLES = "permissions.executable"; //$NON-NLS-1$

	/**
	 * Feature content provider constructor
	 * 
	 * @param base
	 *            feature URL. The interpretation of this URL is specific to
	 *            each content provider.
	 * @since 2.0
	 */
	public FeatureContentProvider(URL base) {
		this.base = base;
		this.feature = null;
	}

	/**
	 * Returns the feature url.
	 * 
	 * @see IFeatureContentProvider#getURL()
	 */
	public URL getURL() {
		return base;
	}

	/**
	 * Returns the feature associated with this content provider.
	 * 
	 * @see IFeatureContentProvider#getFeature()
	 */
	public IFeature getFeature() {
		return feature;
	}

	/**
	 * Sets the feature associated with this content provider.
	 * 
	 * @see IFeatureContentProvider#setFeature(IFeature)
	 */
	public void setFeature(IFeature feature) {
		this.feature = feature;
	}

	/**
	 * Returns the specified reference as a local file system reference. If
	 * required, the file represented by the specified content reference is
	 * first downloaded to the local system
	 * 
	 * @param ref
	 *            content reference
	 * @param monitor
	 *            progress monitor, can be <code>null</code>
	 * @exception IOException
	 * @exception CoreException
	 * @since 2.0
	 */
	public ContentReference asLocalReference(ContentReference ref, InstallMonitor monitor) throws IOException, CoreException {

		// check to see if this is already a local reference
		if (ref.isLocalReference())
			return ref;

		// check to see if we already have a local file for this reference
		String key = ref.toString();

		// need to synch as another thread my have created the file but
		// is still copying into it
		File localFile = null;
		FileFragment localFileFragment = null;
		Object keyLock = LockManager.getLock(key);
		synchronized (keyLock) {
			localFile = Utilities.lookupLocalFile(key);
			if (localFile != null) {
				// check if the cached file is still valid (no newer version on
				// server)
				try {
				if (UpdateManagerUtils.isSameTimestamp(ref.asURL(), localFile.lastModified()))
					return ref.createContentReference(ref.getIdentifier(), localFile);
				} catch(FatalIOException e) {
					throw e;
				} catch(IOException e) {
					throw new FeatureDownloadException(NLS.bind(Messages.FeatureContentProvider_ExceptionDownloading, (new Object[] {getURL().toExternalForm()})), e);
				}
			}

			if (localFile == null) {
				localFileFragment = UpdateManagerUtils.lookupLocalFileFragment(key);
			}
			// 
			// download the referenced file into local temporary area
			InputStream is = null;
			OutputStream os = null;
			long bytesCopied = 0;
			long inputLength = 0;
			boolean success = false;
			if (monitor != null) {
				monitor.saveState();
				monitor.setTaskName(Messages.FeatureContentProvider_Downloading);
				monitor.subTask(ref.getIdentifier() + " "); //$NON-NLS-1$
				try {
					monitor.setTotalCount(ref.getInputSize());
				} catch (FatalIOException e) {
					throw e;
				} catch (IOException e) {
					throw new FeatureDownloadException(NLS.bind(Messages.FeatureContentProvider_ExceptionDownloading, (new Object[] {getURL().toExternalForm()})), e);
				}
				monitor.showCopyDetails(true);
			}

			try {
				//long startTime = System.nanoTime();
				if (localFileFragment != null && "http".equals(ref.asURL().getProtocol())) { //$NON-NLS-1$
					localFile = localFileFragment.getFile();
					try {
						// get partial input stream
						is = ref.getPartialInputStream(localFileFragment.getSize());
						inputLength = ref.getInputSize() - localFileFragment.getSize();
						// get output stream to append to file fragment
						os = new BufferedOutputStream(
						// PAL foundation
								//new FileOutputStream(localFile, true));
								new FileOutputStream(localFile.getPath(), true));
					} catch (FatalIOException e) {
						throw e;
					} catch (IOException e) {
						try {
							if (is != null)
								is.close();
						} catch (IOException ioe) {
						}
						is = null;
						os = null;
						localFileFragment = null;
						throw new FeatureDownloadException(NLS.bind(Messages.FeatureContentProvider_ExceptionDownloading, (new Object[] {getURL().toExternalForm()})), e);
					}
				}
				if (is == null) {
					// must download from scratch
					localFile = Utilities.createLocalFile(getWorkingDirectory(), null);
					try {
						is = ref.getInputStream();
						inputLength = ref.getInputSize();
					} catch (FatalIOException e) {
						throw Utilities.newCoreException(NLS.bind(Messages.FeatureContentProvider_UnableToRetrieve, (new Object[] {ref})), e);
					} catch (IOException e) {
						throw new FeatureDownloadException(NLS.bind(Messages.FeatureContentProvider_ExceptionDownloading, (new Object[] {getURL().toExternalForm()})), e);
					}

					try {
						os = new BufferedOutputStream(new FileOutputStream(localFile));
					} catch (FileNotFoundException e) {
						throw Utilities.newCoreException(NLS.bind(Messages.FeatureContentProvider_UnableToCreate, (new Object[] {localFile})), e);
					}
				}

				Date start = new Date();
				if (localFileFragment != null) {
					bytesCopied = localFileFragment.getSize();
					if (monitor != null) {
						monitor.setCopyCount(bytesCopied);
					}
				}

				// Transfer as many bytes as possible from input to output stream
				long offset = UpdateManagerUtils.copy(is, os, monitor, inputLength);
				if (offset != -1) {
					bytesCopied += offset;
					if (bytesCopied > 0) {
						// preserve partially downloaded file
						UpdateManagerUtils.mapLocalFileFragment(key, new FileFragment(localFile, bytesCopied));
					}
					if (monitor != null && monitor.isCanceled()) {
						String msg = Messages.Feature_InstallationCancelled;
						throw new InstallAbortedException(msg, null);
					} else {
						throw new FeatureDownloadException(NLS.bind(Messages.FeatureContentProvider_ExceptionDownloading, (new Object[] {getURL().toExternalForm()})), new IOException());
					}
				} else {
					UpdateManagerUtils.unMapLocalFileFragment(key);
				}

				Date stop = new Date();
				long timeInseconds = (stop.getTime() - start.getTime()) / 1000;
				// time in milliseconds /1000 = time in seconds
				InternalSiteManager.downloaded(
					ref.getInputSize(),
					(timeInseconds),
					ref.asURL());

				success = true;
				//long endTime = System.nanoTime();
				// file is downloaded succesfully, map it
				Utilities.mapLocalFile(key, localFile);
				
				/*if (ref.asURL().toExternalForm().endsWith("jar")) {
					synchronized(this.getClass()) {
						timer += (endTime - startTime);
						if (first == 0) {
							first = endTime - startTime;
						}
					}
				}*/
			} catch (ClassCastException e) {
				throw Utilities.newCoreException(
					NLS.bind(Messages.FeatureContentProvider_UnableToCreate, (new Object[] { localFile })),
					e);
			} finally {
				//Do not close IS if user cancel,
				//closing IS will read the entire Stream until the end
				if (success && is != null)
					try {
						is.close();
					} catch (IOException e) {
					}
				if (os != null)
					try {
						os.close(); // should flush buffer stream
					} catch (IOException e) {
					}

				if (success || bytesCopied > 0) {
					// set the timestamp on the temp file to match the remote
					// timestamp
					localFile.setLastModified(ref.getLastModified());
				}
				if (monitor != null)
					monitor.restoreState();
			}
			LockManager.returnLock(key);
		} // end lock
		ContentReference reference =
			ref.createContentReference(ref.getIdentifier(), localFile);
		
		UpdateCore.getPlugin().getUpdateSession().markVisited(ref.asURL());
		
		return reference;
	}

	/**
	 * Returns the specified reference as a local file. If required, the file
	 * represented by the specified content reference is first downloaded to
	 * the local system
	 * 
	 * @param ref
	 *            content reference
	 * @param monitor
	 *            progress monitor, can be <code>null</code>
	 * @exception IOException
	 * @exception CoreException
	 * @since 2.0
	 */
	public File asLocalFile(ContentReference ref, InstallMonitor monitor) throws IOException, CoreException {
		File file = ref.asFile();
		ContentReference localRef = asLocalReference(ref, monitor);
		file = localRef.asFile();
		return file;
	}

	/**
	 * Returns working directory for this content provider
	 * 
	 * @return working directory
	 * @exception IOException
	 * @since 2.0
	 */
	protected File getWorkingDirectory() throws IOException {
		if (tmpDir == null)
			tmpDir = Utilities.createWorkingDirectory();
		return tmpDir;
	}

	/**
	 * Returns the total size of all archives required for the specified
	 * plug-in and non-plug-in entries (the "packaging" view).
	 * 
	 * @see IFeatureContentProvider#getDownloadSizeFor(IPluginEntry[],
	 *      INonPluginEntry[])
	 */
	public long getDownloadSizeFor(IPluginEntry[] pluginEntries, INonPluginEntry[] nonPluginEntries) {
		long result = 0;

		// if both are null or empty, return UNKNOWN size
		if ((pluginEntries == null || pluginEntries.length == 0) && (nonPluginEntries == null || nonPluginEntries.length == 0)) {
			return ContentEntryModel.UNKNOWN_SIZE;
		}

		// loop on plugin entries
		long size = 0;
		if (pluginEntries != null)
			for (int i = 0; i < pluginEntries.length; i++) {
				size = ((PluginEntryModel) pluginEntries[i]).getDownloadSize();
				if (size == ContentEntryModel.UNKNOWN_SIZE) {
					return ContentEntryModel.UNKNOWN_SIZE;
				}
				result += size;
			}

		// loop on non plugin entries
		if (nonPluginEntries != null)
			for (int i = 0; i < nonPluginEntries.length; i++) {
				size = ((NonPluginEntryModel) nonPluginEntries[i]).getDownloadSize();
				if (size == ContentEntryModel.UNKNOWN_SIZE) {
					return ContentEntryModel.UNKNOWN_SIZE;
				}
				result += size;
			}

		return result;
	}

	/**
	 * Returns the total size of all files required for the specified plug-in
	 * and non-plug-in entries (the "logical" view).
	 * 
	 * @see IFeatureContentProvider#getInstallSizeFor(IPluginEntry[],
	 *      INonPluginEntry[])
	 */
	public long getInstallSizeFor(IPluginEntry[] pluginEntries, INonPluginEntry[] nonPluginEntries) {
		long result = 0;

		// if both are null or empty, return UNKNOWN size
		if ((pluginEntries == null || pluginEntries.length == 0) && (nonPluginEntries == null || nonPluginEntries.length == 0)) {
			return ContentEntryModel.UNKNOWN_SIZE;
		}

		// loop on plugin entries
		long size = 0;
		if (pluginEntries != null)
			for (int i = 0; i < pluginEntries.length; i++) {
				size = ((PluginEntryModel) pluginEntries[i]).getInstallSize();
				if (size == ContentEntryModel.UNKNOWN_SIZE) {
					return ContentEntryModel.UNKNOWN_SIZE;
				}
				result += size;
			}

		// loop on non plugin entries
		if (nonPluginEntries != null)
			for (int i = 0; i < nonPluginEntries.length; i++) {
				size = ((NonPluginEntryModel) nonPluginEntries[i]).getInstallSize();
				if (size == ContentEntryModel.UNKNOWN_SIZE) {
					return ContentEntryModel.UNKNOWN_SIZE;
				}
				result += size;
			}

		return result;
	}

	/**
	 * Returns the path identifier for a plugin entry. <code>plugins/&lt;pluginId>_&lt;pluginVersion>.jar</code>
	 * 
	 * @return the path identifier
	 */
	protected String getPathID(IPluginEntry entry) {
		return Site.DEFAULT_PLUGIN_PATH + entry.getVersionedIdentifier().toString() + JAR_EXTENSION;
	}

	/**
	 * Returns the path identifer for a non plugin entry. <code>features/&lt;featureId>_&lt;featureVersion>/&lt;dataId></code>
	 * 
	 * @return the path identifier
	 */
	protected String getPathID(INonPluginEntry entry) {
		String nonPluginBaseID = Site.DEFAULT_FEATURE_PATH + feature.getVersionedIdentifier().toString() + "/"; //$NON-NLS-1$
		return nonPluginBaseID + entry.getIdentifier();
	}

	/**
	 * Sets the permission of all the ContentReferences Check for the
	 * .permissions contentReference and use it to set the permissions of other
	 * ContentReference
	 */
	protected void validatePermissions(ContentReference[] references) {

		if (references == null || references.length == 0)
			return;

		Map permissions = getPermissions(references);
		if (permissions.isEmpty())
			return;

		for (int i = 0; i < references.length; i++) {
			ContentReference contentReference = references[i];
			String id = contentReference.getIdentifier();
			Object value = null;
			if ((value = matchesOneRule(id, permissions)) != null) {
				Integer permission = (Integer) value;
				contentReference.setPermission(permission.intValue());
			}
		}
	}

	/**
	 * Returns the value of the matching rule or <code>null</code> if none
	 * found. A rule is matched if the id is equals to a key, or if the id is
	 * resolved by a key. if the id is <code>/path/file.txt</code> it is
	 * resolved by <code>/path/*</code> or <code>/path/*.txt</code>
	 * 
	 * @param id
	 *            the identifier
	 * @param permissions
	 *            list of rules
	 * @return Object the value of the matching rule or <code>null</code>
	 */
	private Object matchesOneRule(String id, Map permissions) {

		Set keySet = permissions.keySet();
		Iterator iter = keySet.iterator();
		while (iter.hasNext()) {
			FileFilter rule = (FileFilter) iter.next();
			if (rule.accept(id)) {
				return permissions.get(rule);
			}
		}

		return null;
	}

	/*
	 * returns the permission MAP
	 */
	private Map getPermissions(ContentReference[] references) {

		Map result = new HashMap();
		// search for .permissions
		boolean notfound = true;
		ContentReference permissionReference = null;
		for (int i = 0; i < references.length && notfound; i++) {
			ContentReference contentReference = references[i];
			if (DOT_PERMISSIONS.equals(contentReference.getIdentifier())) {
				notfound = false;
				permissionReference = contentReference;
			}
		}
		if (notfound)
			return result;

		Properties prop = new Properties();
		InputStream propertyStream = null;
		try {
			try {
				propertyStream = permissionReference.getInputStream();
				prop.load(propertyStream);
			} finally {
				if (propertyStream != null)
					propertyStream.close();
			}
		} catch (IOException e) {
			UpdateCore.warn("", e); //$NON-NLS-1$
		}

		String executables = prop.getProperty(EXECUTABLES);
		if (executables == null)
			return result;

		StringTokenizer tokenizer = new StringTokenizer(executables, ","); //$NON-NLS-1$
		Integer defaultExecutablePermission = new Integer(ContentReference.DEFAULT_EXECUTABLE_PERMISSION);
		while (tokenizer.hasMoreTokens()) {
			FileFilter filter = new FileFilter(tokenizer.nextToken());
			result.put(filter, defaultExecutablePermission);
		}

		return result;
	}
}
