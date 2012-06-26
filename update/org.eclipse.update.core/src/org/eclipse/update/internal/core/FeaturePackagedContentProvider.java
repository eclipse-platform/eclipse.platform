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
package org.eclipse.update.internal.core;

import org.eclipse.update.core.FeatureContentProvider;

import java.io.*;
import java.net.URL;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.osgi.signedcontent.SignedContentFactory;
import org.eclipse.osgi.util.NLS;
import org.eclipse.update.core.*;
import org.eclipse.update.internal.jarprocessor.JarProcessor;
import org.eclipse.update.internal.jarprocessor.Utils;
import org.eclipse.update.internal.security.JarVerifier;
import org.eclipse.update.internal.verifier.CertVerifier;

/**
 * Content Provider of a Feature Package
 */
public class FeaturePackagedContentProvider extends FeatureContentProvider {

	private ContentReference localManifest = null;
	private ContentReference[] localFeatureFiles = new ContentReference[0];
	private IVerifier jarVerifier = null;
	private ExtendedSite siteModel = null;
	private boolean continueOnError;
	/*
	 * filter for file with .jar
	 */
	public static final FilenameFilter filter = new FilenameFilter() {
		public boolean accept(File dir, String name) {
			return name.endsWith(FeatureContentProvider.JAR_EXTENSION);
		}
	};

	/*
	 * Constructor
	 */
	public FeaturePackagedContentProvider(URL url, ISite site) {
		super(url);
		if (site instanceof ExtendedSite) {
			this.siteModel = (ExtendedSite) site;
		}
	}
	
	/*
	 * Returns a new verifier for each top-level install
	 * (if the verifier has a parent, return the verifier
	 * otherwise reinitialize)
	 */
	public IVerifier getVerifier() throws CoreException {
		SignedContentFactory factory = UpdateCore.getPlugin().getSignedContentFactory();
		if (jarVerifier == null || jarVerifier.getParent() == null) {
			if (factory != null)
				jarVerifier = new CertVerifier(UpdateCore.getPlugin().getSignedContentFactory());
			else
				jarVerifier = new JarVerifier();
			return jarVerifier;
		}

		// re-init will be done if the parent changes
		return jarVerifier;
	}

	/*
	 * @see IFeatureContentProvider#getFeatureManifestReference()
	 */
	public ContentReference getFeatureManifestReference(InstallMonitor monitor) throws CoreException {

		// check to see if we already have local copy of the manifest
		if (localManifest != null)
			return localManifest;
		ContentReference[] featureArchiveReference = getFeatureEntryArchiveReferences(monitor);
		JarContentReference featureJarReference = null;
		try {

			// force feature archive to local.
			// This content provider always assumes exactly 1 archive file (index [0])		
			featureJarReference = (JarContentReference) asLocalReference(featureArchiveReference[0], null);
			// we need to unpack archive locally for UI browser references to be resolved correctly
			localFeatureFiles = featureJarReference.unpack(getWorkingDirectory(), null, monitor);
		} catch (IOException e) {
			throw errorRetrieving(Feature.FEATURE_XML, featureJarReference, e); 
		}

		// find the manifest in the unpacked feature files
		for (int i = 0; i < localFeatureFiles.length; i++) {
			if (localFeatureFiles[i].getIdentifier().equals(Feature.FEATURE_XML)) {
				localManifest = localFeatureFiles[i];
				// cache reference to manifest
				return localManifest;
			}
		}

		// the manifest has not been found
		String[] values = new String[] { Feature.FEATURE_XML, getURL().toExternalForm()};
		throw Utilities.newCoreException(NLS.bind(Messages.FeaturePackagedContentProvider_NoManifestFile, values), new Exception()); 

	}

	/*
	 * @see IFeatureContentProvider#getArchiveReferences()
	 */
	public ContentReference[] getArchiveReferences(InstallMonitor monitor) throws CoreException {

		IPluginEntry[] entries = getFeature().getPluginEntries();
		INonPluginEntry[] nonEntries = getFeature().getNonPluginEntries();
		List listAllContentRef = new ArrayList();
		ContentReference[] allContentRef = new ContentReference[0];

		// feature
		listAllContentRef.addAll(Arrays.asList(getFeatureEntryArchiveReferences(monitor)));

		// plugins
		for (int i = 0; i < entries.length; i++) {
			listAllContentRef.addAll(Arrays.asList(getPluginEntryArchiveReferences(entries[i], monitor)));
		}

		// non plugins
		for (int i = 0; i < nonEntries.length; i++) {
			listAllContentRef.addAll(Arrays.asList(getNonPluginEntryArchiveReferences(nonEntries[i], monitor)));
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
	public ContentReference[] getFeatureEntryArchiveReferences(InstallMonitor monitor) throws CoreException {

		//1 jar file <-> 1 feature
		// we will return the JAR file 
		ContentReference[] references = new ContentReference[1];
		ContentReference currentReference = null;
		String archiveID = null;

		try {
			archiveID = (getFeature() != null) ? getFeature().getVersionedIdentifier().toString() : "";	//$NON-NLS-1$
			currentReference = new JarContentReference(archiveID, getURL());
			currentReference = asLocalReference(currentReference, monitor);
			references[0] = currentReference;
		} catch (IOException e) {
			references[0] = continueOnErrorOrRethrow(archiveID, e); 
		}		
		return references;
	}

	/*
	 * @see IFeatureContentProvider#getPluginEntryArchiveReferences(IPluginEntry)
	 */
	public ContentReference[] getPluginEntryArchiveReferences(IPluginEntry pluginEntry, InstallMonitor monitor) throws CoreException {

		// 1 plugin <-> 1 jar
		// we return the JAR file	
		ContentReference[] references = new ContentReference[1];
		String archiveID = getPathID(pluginEntry);
		ISite site = (getFeature() == null) ? null : getFeature().getSite();
		ISiteContentProvider siteContentProvider = (site == null) ? null : site.getSiteContentProvider();
		URL url = (siteContentProvider == null) ? null : siteContentProvider.getArchiveReference(archiveID);

		try {
			references[0] = retrieveLocalJar(new JarContentReference(archiveID, url), monitor);
		} catch (IOException e) {
			references[0] = continueOnErrorOrRethrow(archiveID, e);
		}
		return references;
	}

	private ContentReference retrieveLocalJar(JarContentReference reference, InstallMonitor monitor) throws IOException, CoreException {
		//If the site does not support pack200, just get the jar as normal
		if(siteModel == null || !siteModel.supportsPack200() || !JarProcessor.canPerformUnpack()) {
			ContentReference contentReference = null;
			try {
				contentReference = asLocalReference(reference, monitor);
			}
			catch (FileNotFoundException e) {
				contentReference = continueOnErrorOrRethrow(reference.getIdentifier(), e);
			}
			catch (IOException e) {
				contentReference = continueOnErrorOrRethrow(reference.getIdentifier(), e);
			}
			catch (CoreException e) {
				contentReference = continueOnErrorOrRethrow(reference.getIdentifier(), e);
			}
			return contentReference;
		}
		
		ContentReference packedRef = null;
		String key = reference.toString();
		Object jarLock = LockManager.getLock(key);
		synchronized (jarLock) {
			//do we have this jar already?
			File localFile = Utilities.lookupLocalFile(key);
			if (localFile != null) {
				// check if the cached file is still valid (no newer version on server)
				if (UpdateManagerUtils.isSameTimestamp(reference.asURL(), localFile.lastModified())) {
					LockManager.returnLock(key);
					return reference.createContentReference(reference.getIdentifier(), localFile);
				}
			}

			try {
				//don't have jar, check for pack.gz
				URL packGZURL = new URL(reference.asURL().toExternalForm() + ".pack.gz"); //$NON-NLS-1$
				packedRef = asLocalReference(new JarContentReference(reference.getIdentifier(), packGZURL), monitor);
			} catch (IOException e) {
				//no pack.gz
			} catch (CoreException e){
				//no pack.gz
			}
		}
		
		if (packedRef == null) {
			//no pack.gz on server, get normal jar
			ContentReference contentReference = null;
			try {
				contentReference = asLocalReference(reference, monitor);
			}
			catch (FileNotFoundException e) {
				contentReference = continueOnErrorOrRethrow(reference.getIdentifier(), e);
			}
			catch (IOException e) {
				contentReference = continueOnErrorOrRethrow(reference.getIdentifier(), e);
			}
			catch (CoreException e) {
				contentReference = continueOnErrorOrRethrow(reference.getIdentifier(), e);
			}
			return contentReference;
		}

		boolean success = false;
		synchronized (jarLock) {
			String packed = packedRef.toString();
			Object packedLock = LockManager.getLock(packed);
			synchronized (packedLock) {
				try {
					File tempFile = packedRef.asFile();
					long timeStamp = tempFile.lastModified();
	
					JarProcessor processor = JarProcessor.getUnpackProcessor(null);
					processor.setWorkingDirectory(tempFile.getParent());
	
					File packedFile = new File(tempFile.toString() + Utils.PACKED_SUFFIX);
					tempFile.renameTo(packedFile);
					
					if (monitor != null) {
						monitor.saveState();
						monitor.subTask(Messages.JarContentReference_Unpacking + " " + reference.getIdentifier() + Utils.PACKED_SUFFIX);  //$NON-NLS-1$
						monitor.showCopyDetails(false);
					}
					//unpacking the jar will strip the ".pack.gz" and leave us back with the original filename
					try {
						processor.processJar(packedFile);
					} catch (Throwable e) {
						//something is wrong unpacking
					}
	
					if(tempFile.exists() && tempFile.length() > 0){
						success = true;
						tempFile.setLastModified(timeStamp);
						Utilities.mapLocalFile(key, tempFile);
						UpdateCore.getPlugin().getUpdateSession().markVisited(reference.asURL());
					}
				} finally {
					LockManager.returnLock(packed);
					LockManager.returnLock(key);
					if(monitor != null)
						monitor.restoreState();
				}
			}
		}
		if(!success){
			//Something went wrong with the unpack, get the normal jar.
			ContentReference contentReference = null;
			try {
				contentReference = asLocalReference(reference, monitor);
			}
			catch (FileNotFoundException e) {
				contentReference = continueOnErrorOrRethrow(reference.getIdentifier(), e);
			}
			catch (IOException e) {
				contentReference = continueOnErrorOrRethrow(reference.getIdentifier(), e);
			}
			catch (CoreException e) {
				contentReference = continueOnErrorOrRethrow(reference.getIdentifier(), e);
			}
			return contentReference;
		}
		return packedRef;
	}

	/*
	 * @see IFeatureContentProvider#getNonPluginEntryArchiveReferences(INonPluginEntry)
	 */
	public ContentReference[] getNonPluginEntryArchiveReferences(INonPluginEntry nonPluginEntry, InstallMonitor monitor) throws CoreException {

		// archive = feature/<id>_<ver>/<file>
		String archiveID = Site.DEFAULT_FEATURE_PATH + ((getFeature() != null) ? getFeature().getVersionedIdentifier().toString() : ""); //$NON-NLS-1$
		archiveID += "/" + nonPluginEntry.getIdentifier(); //$NON-NLS-1$

		ContentReference[] references = new ContentReference[1];
		ContentReference currentReference = null;

		try {
			ISite site = (getFeature() == null) ? null : getFeature().getSite();
			ISiteContentProvider siteContentProvider = (site == null) ? null : site.getSiteContentProvider();
			URL url = (siteContentProvider == null) ? null : siteContentProvider.getArchiveReference(archiveID);

			currentReference = new ContentReference(nonPluginEntry.getIdentifier(), url);
			currentReference = asLocalReference(currentReference, monitor);
			references[0] = currentReference;

		} catch (IOException e) {
			references[0] = continueOnErrorOrRethrow(archiveID, e);
		}

		return references;
	}


	/*
	 * @see IFeatureContentProvider#getFeatureEntryContentReferences()
	 */
	public ContentReference[] getFeatureEntryContentReferences(InstallMonitor monitor) throws CoreException {

		return localFeatureFiles; // return cached feature references
		// Note: assumes this content provider is always called first to
		//       get the feature manifest. This forces the feature files
		//       to be unpacked and caches the references
	}

	/*
	 * @see IFeatureContentProvider#getPluginEntryContentReferences(IPluginEntry)
	 */
	public ContentReference[] getPluginEntryContentReferences(IPluginEntry pluginEntry, InstallMonitor monitor) throws CoreException {

		ContentReference[] references = getPluginEntryArchiveReferences(pluginEntry, monitor);
		ContentReference[] pluginReferences = new ContentReference[0];

		try {
			if (references[0] instanceof JarContentReference) {
				JarContentReference localRef = (JarContentReference) asLocalReference(references[0], monitor);
				pluginReferences = localRef.peek(null, monitor);
			} else {
				// return the list of all subdirectories
				List files = getFiles(references[0].asFile());
				pluginReferences = new ContentReference[files.size()];
				for (int i = 0; i < pluginReferences.length; i++) {
					File currentFile = (File) files.get(i);
					pluginReferences[i] = new ContentReference(null, currentFile.toURL());
				}
			}

			//[20866] we did not preserve executable bit
			validatePermissions(pluginReferences);

		} catch (IOException e) {
			throw errorRetrieving(pluginEntry.getVersionedIdentifier().toString(), references[0], e);
		}
		return pluginReferences;
	}

	/*
	 * return all the files under the directory
	 */
	private List getFiles(File dir) throws IOException {
		List result = new ArrayList();

		if (!dir.isDirectory())
			throw new IOException(NLS.bind(Messages.FeaturePackagedContentProvider_InvalidDirectory, (new String[] { dir.getPath() })));

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
	private CoreException errorRetrieving(String obj, ContentReference archive, Exception e) {

		String[] values = new String[] { obj };

		return Utilities.newCoreException(NLS.bind(Messages.FeaturePackagedContentProvider_ErrorRetrieving, values), e);	 	

	}


	public void setContinueOnError(boolean continueOnError) {
		this.continueOnError = continueOnError;
	}
	
	/** 
	 * This method is used for when a core exception is detected, so, if its decided to rethrow, then 
	 * a core exception odes not have to be recreated. 
	 * 
	 * @param archiveID id of the archive file
	 * @param CoreException 
	 * @return NullReference if its decided not to continue
	 * @throws CoreException
	 */
	/*private ContentReference continueOrErrorOrRethrow(String archiveID, CoreException coreException) throws CoreException {
		ContentReference reference = null;

		if (continueOnError) {
			// this ContentReference without a file or URL is purely a
			// "missing jar" reference.
			reference = new NullContentReference(archiveID);

			String msg = "    ContinueOnError: The following ID was not found, so was skipped, and is not on miror site: " + archiveID; //$NON-NLS-1$
			String id = UpdateCore.getPlugin().getBundle().getSymbolicName();
			IStatus status = new Status(IStatus.WARNING, id , 0, msg, null);
			UpdateCore.log(status);
			
		}
		else {
			throw coreException;
		}
		return reference;
	}*/
	
	private ContentReference continueOnErrorOrRethrow(String archiveID, Exception e) throws CoreException {
		ContentReference reference = null;

		if (continueOnError) { 
			// this ContentReference without a file or URL is purely a
			// "missing jar" reference.
			reference = new NullContentReference(archiveID);
			
			String msg = "    ContinueOnError: The following ID was not found, so was skipped, and is not on miror site: " + archiveID; //$NON-NLS-1$
			String id = UpdateCore.getPlugin().getBundle().getSymbolicName();
			IStatus status = new Status(IStatus.WARNING, id , 0, msg, null);
			UpdateCore.log(status);	
			
		}
		else {
			throw errorRetrieving(archiveID, reference, e);
		}
		return reference;
	}

}
