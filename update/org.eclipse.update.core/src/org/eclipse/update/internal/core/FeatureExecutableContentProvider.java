package org.eclipse.update.internal.core;

/*
 * (c) Copyright IBM Corp. 2000, 2002.
 * All Rights Reserved.
 */

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.*;
import org.eclipse.update.core.*;

/**
 * Default implementation of a Executable Feature Content Provider
 */

public class FeatureExecutableContentProvider extends FeatureContentProvider {

	/*
	 * Constructor 
	 */
	public FeatureExecutableContentProvider(URL url) {
		super(url);
	}

	/*
	 * Return the path for a pluginEntry
	 */
	private String getPath(IPluginEntry pluginEntry)
		throws IOException, CoreException {

		// get the URL of the Archive file that contains the plugin entry
		ISiteContentProvider provider = getFeature().getSite().getSiteContentProvider();
		URL fileURL = provider.getArchiveReference(getPathID(pluginEntry));
		String result = fileURL.getFile();

		// return the list of all subdirectories
		if (!result.endsWith(File.separator))
			result += File.separator;
		File pluginDir = new File(result);
		if (!pluginDir.exists())
			throw new IOException(
				Policy.bind("FeatureExecutableContentProvider.FileDoesNotExist", result));
		//$NON-NLS-1$ //$NON-NLS-2$

		return result;
	}

	/*
	 * Returns the path for the Feature
	 */
	private String getFeaturePath() throws IOException {
		String result = getFeature().getURL().getFile();

		// return the list of all subdirectories
		if (!(result.endsWith(File.separator) || result.endsWith("/"))) //$NON-NLS-1$
			result += File.separator;
		File pluginDir = new File(result);
		if (!pluginDir.exists())
			throw new IOException(
				Policy.bind("FeatureExecutableContentProvider.FileDoesNotExist", result));
		//$NON-NLS-1$ //$NON-NLS-2$

		return result;
	}

	/*
	 * Returns all the files under the directory
	 * Recursive call
	 */
	private List getFiles(File dir) throws IOException {
		List result = new ArrayList();

		if (!dir.isDirectory()) {
			String msg =
				Policy.bind(
					"FeatureExecutableContentProvider.InvalidDirectory",
					dir.getAbsolutePath());
			//$NON-NLS-1$
			throw new IOException(msg);

		}

		File[] files = dir.listFiles();
		if (files != null)
			for (int i = 0; i < files.length; ++i) {
				if (files[i].isDirectory()) {
					result.addAll(getFiles(files[i]));
				} else {
					result.add(files[i]);
				}
			}

		return result;
	}

	/*
	 * @see IFeatureContentProvider#getVerifier()
	 */
	public IVerifier getVerifier() throws CoreException {
		return null;
	}

	/*
	 * @see IFeatureContentProvider#getFeatureManifestReference()
	 */
	public ContentReference getFeatureManifestReference(InstallMonitor monitor)
		throws CoreException {
		ContentReference result = null;
		try {
			result = new ContentReference(Feature.FEATURE_XML, new URL(getURL(), Feature.FEATURE_XML));

		} catch (MalformedURLException e) {
			throw Utilities.newCoreException(
				Policy.bind(
					"FeatureExecutableContentProvider.UnableToCreateURLFor",
					getURL().toExternalForm() + " " + Feature.FEATURE_XML),
				e);
			//$NON-NLS-1$ //$NON-NLS-2$
		}
		return result;
	}

	/*
	 * @see IFeatureContentProvider#getArchiveReferences()
	 */
	public ContentReference[] getArchiveReferences(InstallMonitor monitor)
		throws CoreException {
		// executable feature does not contain archives
		return new ContentReference[0];
	}

	/*
	 * @see IFeatureContentProvider#getPluginEntryArchiveReferences(IPluginEntry)
	 */
	public ContentReference[] getPluginEntryArchiveReferences(
		IPluginEntry pluginEntry,
		InstallMonitor monitor)
		throws CoreException {
		ContentReference[] result = new ContentReference[1];
		try {
			result[0] =
				new ContentReference(getPathID(pluginEntry), new File(getPath(pluginEntry)));
		} catch (IOException e) {
			throw Utilities.newCoreException(
					Policy.bind(
						"FeatureExecutableContentProvider.UnableToRetrievePluginEntry",
						pluginEntry.getVersionedIdentifier().toString()),
					e);
			//$NON-NLS-1$
		}
		return result;
	}

	/*
	 * @see IFeatureContentProvider#getNonPluginEntryArchiveReferences(INonPluginEntry)
	 */
	public ContentReference[] getNonPluginEntryArchiveReferences(
		INonPluginEntry nonPluginEntry,
		InstallMonitor monitor)
		throws CoreException {

		ContentReference[] result = new ContentReference[1];
		URL fileURL;

		//try {
		// get the URL of the Archive file that contains the plugin entry
		ISiteContentProvider provider = getFeature().getSite().getSiteContentProvider();
		fileURL = provider.getArchiveReference(getPathID(nonPluginEntry));

		String fileString = fileURL.getFile();
		File nonPluginData = new File(fileString);
		if (!nonPluginData.exists())
			throw Utilities.newCoreException(
				Policy.bind("FeatureExecutableContentProvider.FileDoesNotExist", fileString),
				null);
		//$NON-NLS-1$ //$NON-NLS-2$

		try {
			result[0] =
				new ContentReference(nonPluginEntry.getIdentifier(), nonPluginData.toURL());
		} catch (MalformedURLException e) {
			throw Utilities.newCoreException(
				Policy.bind(
					"FeatureExecutableContentProvider.UnableToRetrieveNonPluginEntry",
					nonPluginEntry.getIdentifier().toString()),
				e);
			//$NON-NLS-1$
		}
		return result;
	}

	/*
	 * @see IFeatureContentProvider#getFeatureEntryArchiveReferences()
	 */
	public ContentReference[] getFeatureEntryArchiveReferences(InstallMonitor monitor)
		throws CoreException {
		ContentReference[] contentReferences = new ContentReference[1];
		contentReferences[0] = new ContentReference(null, getURL());
		return contentReferences;
	}

	/*
	 * @see IFeatureContentProvider#getFeatureEntryArchivesContentReferences()
	 */
	public ContentReference[] getFeatureEntryContentReferences(InstallMonitor monitor)
		throws CoreException {
		ContentReference[] result = new ContentReference[0];
		try {
			File featureDir = new File(getFeaturePath());
			List files = getFiles(featureDir);
			result = new ContentReference[files.size()];
			for (int i = 0; i < result.length; i++) {
				File currentFile = (File) files.get(i);
				result[i] = new ContentReference(currentFile.getName(), currentFile.toURL());
			}
		} catch (IOException e) {
			throw Utilities.newCoreException(
				Policy.bind(
					"FeatureExecutableContentProvider.UnableToRetrieveFeatureEntryContentRef",
					getFeature().getVersionedIdentifier().toString()),
				e);
			//$NON-NLS-1$
		}
		return result;
	}

	/*
	 * @see IFeatureContentProvider#getPluginEntryContentReferences(IPluginEntry)
	 */
	public ContentReference[] getPluginEntryContentReferences(
		IPluginEntry pluginEntry,
		InstallMonitor monitor)
		throws CoreException {

		ContentReference[] result = new ContentReference[0];

		try {
			// return the list of all subdirectories
			File pluginDir = new File(getPath(pluginEntry));
			URL pluginURL = pluginDir.toURL();
			List files = getFiles(pluginDir);
			result = new ContentReference[files.size()];
			for (int i = 0; i < result.length; i++) {
				File currentFile = (File) files.get(i);
				String relativeString = UpdateManagerUtils.getURLAsString(pluginURL, currentFile.toURL());
				result[i] = new ContentReference(relativeString, currentFile.toURL());
			}
		} catch (IOException e) {
			throw Utilities.newCoreException(
				Policy.bind(
					"FeatureExecutableContentProvider.UnableToRetriveArchiveContentRef")
					+ pluginEntry.getVersionedIdentifier().toString(),
				e);
			//$NON-NLS-1$
		}
		
		//[20866] we did not preserve executable bit
		validatePermissions(result);
		
		return result;
	}
}