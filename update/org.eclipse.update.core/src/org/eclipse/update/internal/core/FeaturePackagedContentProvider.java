package org.eclipse.update.internal.core;
/*
 * (c) Copyright IBM Corp. 2000, 2002.
 * All Rights Reserved.
 */
import java.io.*;
import java.net.URL;
import java.util.*;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.update.core.*;
import org.eclipse.update.internal.security.JarVerifier;

/**
 * Content Provider of a Feature Package
 */
public class FeaturePackagedContentProvider extends FeatureContentProvider {

	private ContentReference localManifest = null;
	private ContentReference[] localFeatureFiles = new ContentReference[0];
	private IVerifier jarVerifier = null;

	/*
	 * filer for file with .jar
	 */
	public static final FilenameFilter filter = new FilenameFilter() {
		public boolean accept(File dir, String name) {
			return name.endsWith(FeaturePackagedContentProvider.JAR_EXTENSION);
		}
	};

	/*
	 * Constructor 
	 */
	public FeaturePackagedContentProvider(URL url) {
		super(url);
	}

	/*
	 * Returns a new verifier for each top-level install
	 * (if the verifier has a parent, return the verifier
	 * otherwise reinitialize)
	 */
	public IVerifier getVerifier() throws CoreException {
		if (jarVerifier == null || jarVerifier.getParent() == null) {
			jarVerifier = new JarVerifier();
			return jarVerifier;
		}

		// re-init will be done if the parent changes
		return jarVerifier;
	}

	/*
	 * @see IFeatureContentProvider#getFeatureManifestReference()
	 */
	public ContentReference getFeatureManifestReference(InstallMonitor monitor)
		throws CoreException {

		// check to see if we already have local copy of the manifest
		if (localManifest != null)
			return localManifest;

		ContentReference[] featureArchiveReference =
			getFeatureEntryArchiveReferences(monitor);
		JarContentReference featureJarReference=null;
		try {

			// force feature archive to local.
			// This content provider always assumes exactly 1 archive file (index [0])		
			featureJarReference =
				(JarContentReference) asLocalReference(featureArchiveReference[0],
					null);

			// we need to unpack archive locally for UI browser references to be resolved correctly
			localFeatureFiles =
				featureJarReference.unpack(
					getWorkingDirectory(),
					null,
					monitor);
		} catch (IOException e) {
			throw errorRetrieving(Feature.FEATURE_XML, featureJarReference, getURL(), e);
			//$NON-NLS-1$ 
		}

		// find the manifest in the unpacked feature files
		for (int i = 0; i < localFeatureFiles.length; i++) {
			if (localFeatureFiles[i]
				.getIdentifier()
				.equals(Feature.FEATURE_XML)) {
				localManifest = localFeatureFiles[i];
				// cache reference to manifest
				return localManifest;
			}
		}

		// the manifest has not been found
		String[] values =
			new String[] { Feature.FEATURE_XML, getURL().toExternalForm()};
		throw Utilities.newCoreException(
			Policy.bind(
				"FeaturePackagedContentProvider.NoManifestFile",
				values),
			new Exception());
		//$NON-NLS-1$ 

	}

	/*
	 * @see IFeatureContentProvider#getArchiveReferences()
	 */
	public ContentReference[] getArchiveReferences(InstallMonitor monitor)
		throws CoreException {

		IPluginEntry[] entries = getFeature().getPluginEntries();
		INonPluginEntry[] nonEntries = getFeature().getNonPluginEntries();
		List listAllContentRef = new ArrayList();
		ContentReference[] allContentRef = new ContentReference[0];

		// feature
		listAllContentRef.addAll(
			Arrays.asList(getFeatureEntryArchiveReferences(monitor)));

		// plugins
		for (int i = 0; i < entries.length; i++) {
			listAllContentRef.addAll(
				Arrays.asList(
					getPluginEntryArchiveReferences(entries[i], monitor)));
		}

		// non plugins
		for (int i = 0; i < nonEntries.length; i++) {
			listAllContentRef.addAll(
				Arrays.asList(
					getNonPluginEntryArchiveReferences(
						nonEntries[i],
						monitor)));
		}

		// transform List in Array
		if (listAllContentRef.size() > 0) {
			allContentRef = new ContentReference[listAllContentRef.size()];
			listAllContentRef.toArray(allContentRef);
		}

		return allContentRef;
	}

	/*
	 * @see IFeatureContentProvider#getFeatureEntryArchiveReferences()
	 */
	public ContentReference[] getFeatureEntryArchiveReferences(InstallMonitor monitor)
		throws CoreException {

		//1 jar file <-> 1 feature
		// we will return the JAR file 
		ContentReference[] references = new ContentReference[1];
		ContentReference currentReference = null;
		String archiveID = null;

		try {
			archiveID =
				(getFeature() != null)
					? getFeature().getVersionedIdentifier().toString()
					: "";
			//$NON-NLS-1$
			currentReference = new JarContentReference(archiveID, getURL());
			currentReference = asLocalReference(currentReference, monitor);
			references[0] = currentReference;
		} catch (IOException e) {
			throw errorRetrieving(archiveID,currentReference,getFeature().getURL(),e);
			//$NON-NLS-1$
		}
		return references;
	}

	/*
	 * @see IFeatureContentProvider#getPluginEntryArchiveReferences(IPluginEntry)
	 */
	public ContentReference[] getPluginEntryArchiveReferences(
		IPluginEntry pluginEntry,
		InstallMonitor monitor)
		throws CoreException {

		// 1 plugin <-> 1 jar
		// we return the JAR file	
		ContentReference[] references = new ContentReference[1];
		String archiveID = getPathID(pluginEntry);
		ISite site = (getFeature() == null) ? null : getFeature().getSite();
		ISiteContentProvider siteContentProvider =
			(site == null) ? null : site.getSiteContentProvider();
		URL url =
			(siteContentProvider == null)
				? null
				: siteContentProvider.getArchiveReference(archiveID);

		try {
			references[0] =
				asLocalReference(
					new JarContentReference(archiveID, url),
					monitor);
		} catch (IOException e) {
			throw errorRetrieving(archiveID,references[0],getFeature().getURL(),e);
			//$NON-NLS-1$
		}
		return references;
	}

	/*
	 * @see IFeatureContentProvider#getNonPluginEntryArchiveReferences(INonPluginEntry)
	 */
	public ContentReference[] getNonPluginEntryArchiveReferences(
		INonPluginEntry nonPluginEntry,
		InstallMonitor monitor)
		throws CoreException {

		// archive = feature/<id>_<ver>/<file>
		String archiveID =
			Site.DEFAULT_FEATURE_PATH
				+ ((getFeature() != null)
					? getFeature().getVersionedIdentifier().toString()
					: "");
		//$NON-NLS-1$
		archiveID += "/" + nonPluginEntry.getIdentifier(); //$NON-NLS-1$

		ContentReference[] references = new ContentReference[1];
		ContentReference currentReference = null;

		try {
			ISite site = (getFeature() == null) ? null : getFeature().getSite();
			ISiteContentProvider siteContentProvider =
				(site == null) ? null : site.getSiteContentProvider();
			URL url =
				(siteContentProvider == null)
					? null
					: siteContentProvider.getArchiveReference(archiveID);

			currentReference =
				new ContentReference(nonPluginEntry.getIdentifier(), url);
			currentReference = asLocalReference(currentReference, monitor);
			references[0] = currentReference;

		} catch (IOException e) {
			throw errorRetrieving(archiveID,currentReference,getFeature().getURL(),e);
			//$NON-NLS-1$
		}

		return references;
	}

	/*
	 * @see IFeatureContentProvider#getFeatureEntryContentReferences()
	 */
	public ContentReference[] getFeatureEntryContentReferences(InstallMonitor monitor)
		throws CoreException {

		return localFeatureFiles; // return cached feature references
		// Note: assumes this content provider is always called first to
		//       get the feature manifest. This forces the feature files
		//       to be unpacked and caches the references
	}

	/*
	 * @see IFeatureContentProvider#getPluginEntryContentReferences(IPluginEntry)
	 */
	public ContentReference[] getPluginEntryContentReferences(IPluginEntry pluginEntry,InstallMonitor monitor) throws CoreException {

		ContentReference[] references =	getPluginEntryArchiveReferences(pluginEntry, monitor);
		ContentReference[] pluginReferences = new ContentReference[0];

		try {
			if (references[0] instanceof JarContentReference) {
				JarContentReference localRef = (JarContentReference) asLocalReference(references[0],monitor);
				pluginReferences = localRef.peek(null, monitor);
			} else {
				// return the list of all subdirectories
				List files = getFiles(references[0].asFile());
				pluginReferences = new ContentReference[files.size()];
				for (int i = 0; i < pluginReferences.length; i++) {
					File currentFile = (File) files.get(i);
					pluginReferences[i] = new ContentReference(null, currentFile.toURL());
				}
			};
			
		//[20866] we did not preserve executable bit
		validatePermissions(pluginReferences);

		} catch (IOException e) {
			throw errorRetrieving(pluginEntry.getVersionedIdentifier().toString(),references[0],getFeature().getURL(),e);
			//$NON-NLS-1$
		}
		return pluginReferences;
	}

	/*
	 * return all the files under the directory
	 */
	private List getFiles(File dir) throws IOException {
		List result = new ArrayList();

		if (!dir.isDirectory())
			throw new IOException(	Policy.bind("FeaturePackagedContentProvider.InvalidDirectory",dir.getPath()));//$NON-NLS-1$

		File[] files = dir.listFiles();
		if (files != null) // be careful since it can be null
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
	 * 
	 */
	 private CoreException errorRetrieving(String obj, ContentReference archive, URL url, Exception e){
	 	
			String[] values = new String[] { obj };
			
			return Utilities.newCoreException(
				Policy.bind("FeaturePackagedContentProvider.ErrorRetrieving",values),e);
			//$NON-NLS-1$	 	
	 	
	 }

}