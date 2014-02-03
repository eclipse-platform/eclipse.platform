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
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.core.runtime.*;
import org.eclipse.osgi.util.NLS;
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

		if (!result.endsWith(".jar") && !result.endsWith("/") && !result.endsWith(File.separator)) //$NON-NLS-1$ //$NON-NLS-2$
			result += File.separator;
		File pluginPath = new File(result);
		if (!pluginPath.exists())
			throw new IOException(
				NLS.bind(Messages.FeatureExecutableContentProvider_FileDoesNotExist, (new String[] { result })));

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
				NLS.bind(Messages.FeatureExecutableContentProvider_FileDoesNotExist, (new String[] { result })));

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
				NLS.bind(Messages.FeatureExecutableContentProvider_InvalidDirectory, (new String[] { dir.getAbsolutePath() }));

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
				NLS.bind(Messages.FeatureExecutableContentProvider_UnableToCreateURLFor, (new String[] { getURL().toExternalForm() + " " + Feature.FEATURE_XML })), //$NON-NLS-1$
				e);
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
		String archiveID = getPathID(pluginEntry);
		try {
			File archiveFile = new File(getPath(pluginEntry));
			if(!archiveFile.isDirectory() && archiveFile.getName().endsWith(".jar")){ //$NON-NLS-1$
				result[0] = new JarContentReference(archiveID, archiveFile);				
			} else {
				result[0] =
					new ContentReference(archiveID, archiveFile);
			}
		} catch (IOException e) {
			throw Utilities.newCoreException(
					NLS.bind(Messages.FeatureExecutableContentProvider_UnableToRetrievePluginEntry, (new String[] { pluginEntry.getVersionedIdentifier().toString() })),
					e);
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
				NLS.bind(Messages.FeatureExecutableContentProvider_FileDoesNotExist, (new String[] { fileString })), 
				null); 

		try {
			result[0] =
				new ContentReference(nonPluginEntry.getIdentifier(), nonPluginData.toURL());
		} catch (MalformedURLException e) {
			throw Utilities.newCoreException(
				NLS.bind(Messages.FeatureExecutableContentProvider_UnableToRetrieveNonPluginEntry, (new String[] { nonPluginEntry.getIdentifier().toString() })),
				e);
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
				NLS.bind(Messages.FeatureExecutableContentProvider_UnableToRetrieveFeatureEntry, (new String[] { getFeature().getVersionedIdentifier().toString() })),
				e);
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

		ContentReference[] references = getPluginEntryArchiveReferences(pluginEntry, monitor);
		ContentReference[] result = new ContentReference[0];

		try {
			if (references[0] instanceof JarContentReference) {
				result = ((JarContentReference)references[0]).peek(null, monitor);
			} else {
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
			}
		} catch (IOException e) {
			throw Utilities.newCoreException(
				Messages.FeatureExecutableContentProvider_UnableToRetriveArchiveContentRef
					+ pluginEntry.getVersionedIdentifier().toString(),
				e);
		}
		
		//[20866] we did not preserve executable bit
		validatePermissions(result);
		
		return result;
	}
}
