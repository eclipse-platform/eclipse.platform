package org.eclipse.update.core;
/*
 * (c) Copyright IBM Corp. 2000, 2002.
 * All Rights Reserved.
 */

import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.update.core.model.*;
import org.eclipse.update.internal.core.*;
import org.eclipse.update.internal.core.Policy;
import org.eclipse.update.internal.core.UpdateManagerPlugin;

/**
 * Base implementation of a feature content provider.
 * This class provides a set of helper methods useful for implementing
 * feature content providers. In particular, methods dealing with
 * downloading and caching of feature files. 
 * <p>
 * This class must be subclassed by clients.
 * </p> 
 * @see org.eclipse.update.core.IFeatureContentProvider
 * @since 2.0
 */
public abstract class FeatureContentProvider implements IFeatureContentProvider {

	/**
	 * 
	 */
	public class FileFilter {

		private String filterString = null;
		private IPath filterPath = null;

		/**
		 * Constructor for FileFilter.
		 */
		public FileFilter(String filter) {
			super();
			this.filterString = filter;
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

			// check same file extension  if extension exists (a.txt/*.txt)
			// or same file name (a.txt,a.*)
			String extension = filterPath.getFileExtension();
			if (!extension.equals("*")) {
				if (!extension.equalsIgnoreCase(namePath.getFileExtension()))
					return false;
			} else {
				IPath noExtension = filterPath.removeFileExtension();
				String fileName = noExtension.lastSegment();
				if (!fileName.equals("*")) {
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

	private static final String DOT_PERMISSIONS = "permissions.properties";
	private static final String EXECUTABLES = "permissions.executable";

	// lock
	private final static Object lock = new Object();

	/**
	 * Feature content provider constructor
	 * 
	 * @param base feature URL. The interpretation of this URL 
	 * is specific to each content provider.
	 * @since 2.0
	 */
	public FeatureContentProvider(URL base) {
		this.base = base;
		this.feature = null;
	}

	/**
	 * Returns the feature url. 
	 * @see IFeatureContentProvider#getURL()
	 */
	public URL getURL() {
		return base;
	}

	/**
	 * Returns the feature associated with this content provider.
	 * @see IFeatureContentProvider#getFeature()
	 */
	public IFeature getFeature() {
		return feature;
	}

	/**
	 * Sets the feature associated with this content provider.
	 * @see IFeatureContentProvider#setFeature(IFeature)
	 */
	public void setFeature(IFeature feature) {
		this.feature = feature;
	}

	/**
	 * Returns the specified reference as a local file system reference.
	 * If required, the file represented by the specified content
	 * reference is first downloaded to the local system
	 * 
	 * @param ref content reference
	 * @param monitor progress monitor, can be <code>null</code>
	 * @exception IOException
	 * @exception CoreException
	 * @since 2.0
	 */
	public ContentReference asLocalReference(ContentReference ref, InstallMonitor monitor) throws  IOException, CoreException {

		// check to see if this is already a local reference
		if (ref.isLocalReference())
			return ref;

		// check to see if we already have a local file for this reference
		String key = ref.toString();

		// need to synch as another thread my have created the file but
		// is still copying into it
		File localFile = null;
		synchronized (lock) {

			localFile = Utilities.lookupLocalFile(key);
			if (localFile != null)
				return ref.createContentReference(ref.getIdentifier(), localFile);
			// 
			// download the referenced file into local temporary area
			InputStream is = null;
			OutputStream os = null;
			localFile = Utilities.createLocalFile(getWorkingDirectory(), null /*name*/
			);
			boolean sucess = false;
			if (monitor != null) {
				monitor.saveState();
				monitor.setTaskName(Policy.bind("FeatureContentProvider.Downloading"));
				//$NON-NLS-1$
				monitor.subTask(ref.getIdentifier() + " "); //$NON-NLS-1$
				monitor.setTotalCount(ref.getInputSize());
				monitor.showCopyDetails(true);
			}

			try {
				try {
					is = ref.getInputStream();
				} catch (IOException e) {
					throw Utilities.newCoreException(Policy.bind("FeatureContentProvider.UnableToRetrieve", new Object[] { ref }), e);
				}

				try {
					os = new BufferedOutputStream(new FileOutputStream(localFile));
				} catch (FileNotFoundException e) {
					throw Utilities.newCoreException(Policy.bind("FeatureContentProvider.UnableToCreate", new Object[] { localFile }), e);
				}
				Utilities.copy(is, os, monitor);
				sucess = true;
				
				// file is downloaded succesfully, map it 
				Utilities.mapLocalFile(key,localFile);
			} catch (ClassCastException e) {
				throw Utilities.newCoreException(Policy.bind("FeatureContentProvider.UnableToCreate", new Object[] { localFile }), e);
			} finally {
				//Do not close IS if user cancel,
				//closing IS will read the entire Stream until the end
				if (sucess && is != null)
					try {
						is.close();
					} catch (IOException e) {
					}
				if (os != null)
					try {
						os.close(); // should flush buffer stream
					} catch (IOException e) {
					}
				if (monitor != null)
					monitor.restoreState();
			}
		} // end lock
		ContentReference reference = ref.createContentReference(ref.getIdentifier(), localFile);
		return reference;
	}

	/**
	 * Returns the specified reference as a local file.
	 * If required, the file represented by the specified content
	 * reference is first downloaded to the local system
	 * 
	 * @param ref content reference
	 * @param monitor progress monitor, can be <code>null</code>
	 * @exception IOException	
	 * @exception CoreException  
	 * @since 2.0
	 */
	public File asLocalFile(ContentReference ref, InstallMonitor monitor) throws IOException, CoreException {
		File file = ref.asFile();
		if (file != null)
			return file;
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
	 * Returns the total size of all archives required for the
	 * specified plug-in and non-plug-in entries (the "packaging" view).
	 * @see IFeatureContentProvider#getDownloadSizeFor(IPluginEntry[], INonPluginEntry[])
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
	 * Returns the total size of all files required for the
	 * specified plug-in and non-plug-in entries (the "logical" view).
	 * @see IFeatureContentProvider#getInstallSizeFor(IPluginEntry[], INonPluginEntry[])
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
	 * Returns the path identifier for a plugin entry.
	 * <code>plugins/&lt;pluginId>_&lt;pluginVersion>.jar</code> 
	 * @return the path identifier
	 */
	protected String getPathID(IPluginEntry entry) {
		return Site.DEFAULT_PLUGIN_PATH + entry.getVersionedIdentifier().toString() + JAR_EXTENSION;
	}

	/**
	 * Returns the path identifer for a non plugin entry.
	 * <code>features/&lt;featureId>_&lt;featureVersion>/&lt;dataId></code>
	 * @return the path identifier
		 */
	protected String getPathID(INonPluginEntry entry) {
		String nonPluginBaseID = Site.DEFAULT_FEATURE_PATH + feature.getVersionedIdentifier().toString() + "/";
		//$NON-NLS-1$
		return nonPluginBaseID + entry.getIdentifier();
	}

	/**
	 * Sets the permission of all the ContentReferences
	 * Check for the .permissions contentReference and use it
	 * to set the permissions of other ContentReference 
	 * 
	 * @return void
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
	 * Returns the value of the matching rule or <code>null</code> if none found.
	 * A rule is matched if the id is equals to a key, or if the id is resolved by a key.
	 * if the id is <code>/path/file.txt</code> it is resolved by <code>/path/*</code>
	 * or <code>/path/*.txt</code>
	 * 
	 * @param id the identifier
	 * @param permissions list of rules
	 * @return Object the value of the matcing rule or <code>null</code>
	 */
	private Object matchesOneRule(String id, Map permissions) {
		Object result;

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
		try {
			prop.load(permissionReference.getInputStream());
		} catch (IOException e) {
			UpdateManagerPlugin.warn("", e);
		}

		String executables = prop.getProperty(EXECUTABLES);
		if (executables == null)
			return result;

		StringTokenizer tokenizer = new StringTokenizer(executables, ",");
		Integer defaultExecutablePermission = new Integer(ContentReference.DEFAULT_EXECUTABLE_PERMISSION);
		while (tokenizer.hasMoreTokens()) {
			FileFilter filter = new FileFilter(tokenizer.nextToken());
			result.put(filter, defaultExecutablePermission);
		}

		return result;
	}
}