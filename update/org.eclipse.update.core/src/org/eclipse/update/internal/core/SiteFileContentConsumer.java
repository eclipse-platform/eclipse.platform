package org.eclipse.update.internal.core;
/*
 * (c) Copyright IBM Corp. 2000, 2002.
 * All Rights Reserved.
 */
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.update.core.*;
import org.eclipse.update.core.model.ArchiveReferenceModel;
import org.eclipse.update.core.model.FeatureReferenceModel;

/**
 * ContentConsummer for a SiteFile
 */
public class SiteFileContentConsumer extends SiteContentConsumer {

	private String path;
	private IFeature feature;
	private boolean closed = false;
	
	// recovery
	private String oldPath;
	private String newPath;
	
	//  for abort
	private List /* of SiteFilePluginContentConsumer */ contentConsumers;	
	private List /*of path as String */ installedFiles;

	/*
	 * Constructor 
	 */
	public SiteFileContentConsumer(IFeature feature) {
		this.feature = feature;
		installedFiles= new ArrayList();
	}

	/*
	 * Returns the path in which the Feature will be installed
	 */
	private String getFeaturePath() throws CoreException {
		String featurePath = null;
		try {
			VersionedIdentifier featureIdentifier =
				feature.getVersionedIdentifier();
			String path =
				Site.DEFAULT_INSTALLED_FEATURE_PATH
					+ featureIdentifier.toString()
					+ File.separator;
			URL newURL = new URL(getSite().getURL(), path);
			featurePath = newURL.getFile();
		} catch (MalformedURLException e) {
			throw Utilities.newCoreException(
				Policy.bind("SiteFileContentConsumer.UnableToCreateURL")
					+ e.getMessage(),
				e);
			//$NON-NLS-1$
		}
		return featurePath;
	}

	/*
	 * @see ISiteContentConsumer#open(INonPluginEntry)
	 */
	public IContentConsumer open(INonPluginEntry nonPluginEntry)
		throws CoreException {
		return  new SiteFileNonPluginContentConsumer(getFeaturePath());
	}

	/*
	 * @see ISiteContentConsumer#open(IPluginEntry)
	 */
	public IContentConsumer open(IPluginEntry pluginEntry)
		throws CoreException {
		SiteFilePluginContentConsumer cons = new SiteFilePluginContentConsumer(pluginEntry, getSite());
		addContentConsumers(cons);
		return cons;
	}

	/*
	 * @see ISiteContentConsumer#store(ContentReference, IProgressMonitor)
	 */
	public void store(
		ContentReference contentReference,
		IProgressMonitor monitor)
		throws CoreException {
			
		if (closed){
			UpdateManagerPlugin.warn("Attempt to store in a closed SiteFileContentConsumer",new Exception());
			return;
		}			
			
		InputStream inStream = null;
		String featurePath = getFeaturePath();
		String contentKey = contentReference.getIdentifier();
		featurePath += contentKey;

		// error recovery
		if (featurePath.endsWith(Feature.FEATURE_XML)) {
			oldPath=featurePath.replace(File.separatorChar,'/');
			File localFile = new File(oldPath);
			if (localFile.exists()){
				throw Utilities.newCoreException(Policy.bind("UpdateManagerUtils.FileAlreadyExists",new Object[]{localFile}),null);
			}			
			featurePath =
				ErrorRecoveryLog.getLocalRandomIdentifier(featurePath);
			newPath = featurePath;
			ErrorRecoveryLog.getLog().appendPath(
				ErrorRecoveryLog.FEATURE_ENTRY,
				featurePath);
		}

		try {
			inStream = contentReference.getInputStream();
			UpdateManagerUtils.copyToLocal(inStream, featurePath, null);
			UpdateManagerUtils.checkPermissions(contentReference,featurePath); // 20305
			installedFiles.add(featurePath);
		} catch (IOException e) {
			throw Utilities.newCoreException(
				Policy.bind("GlobalConsumer.ErrorCreatingFile", featurePath),
				e);
			//$NON-NLS-1$
		} finally {
			try {
				// close stream
				inStream.close();
			} catch (Exception e) {
			}
		}

	}

	/*
	 * @see ISiteContentConsumer#close()
	 */
	public IFeatureReference close() throws CoreException {

		if (closed)
			UpdateManagerPlugin.warn("Attempt to close a closed SiteFileContentConsumer",new Exception());

		// create a new Feature reference to be added to the site
		FeatureReference ref = new FeatureReference();
		ref.setSite(getSite());
		File file = null;

		try {
			file = new File(getFeaturePath());
			ref.setURL(file.toURL());
		} catch (MalformedURLException e) {
			throw Utilities.newCoreException(
				Policy.bind(
					"SiteFileContentConsumer.UnableToCreateURLForFile",
					file.getAbsolutePath()),
				e);
			//$NON-NLS-1$
		}

		//rename file back 
		if (newPath!=null){
			ErrorRecoveryLog.getLog().appendPath(ErrorRecoveryLog.RENAME_ENTRY, newPath);
			boolean sucess = false;
			File fileToRename = new File(newPath);
			if (fileToRename.exists()){
				File renamedFile = new File(oldPath);
				if (renamedFile.exists()) {
					UpdateManagerUtils.removeFromFileSystem(renamedFile);
					UpdateManagerPlugin.warn("Removing already existing file:"+oldPath);
				}
				sucess = fileToRename.renameTo(renamedFile);
			}	
			if(!sucess){
				String msg = Policy.bind("ContentConsumer.UnableToRename",newPath,oldPath);
				throw Utilities.newCoreException(msg,new Exception(msg));
			}			
		}
		
		// close plugin and non plugin content consumer
		if (contentConsumers!=null){
			Iterator iter = contentConsumers.iterator();
			while (iter.hasNext()) {
				ContentConsumer element = (ContentConsumer) iter.next();
				element.close();
			}
		}
		contentConsumers = null;		
		
		if (ref != null) {
			// the feature MUST have renamed the plugins at that point
			// (by closing the PluginContentConsumer)
			commitPlugins(ref);
			ref.markReadOnly();
		}

		closed = true;
		return ref;
	}

	/*
	 * @see ISiteContentConsumer#abort()
	 */
	public void abort() throws CoreException {
		
		if (closed){
			UpdateManagerPlugin.warn("Attempt to abort a closed SiteFileContentConsumer",new Exception());
			return;
		}			
		
		//abort all plugins content consumer opened
		if (contentConsumers!=null){
			Iterator iter = contentConsumers.iterator();
			while (iter.hasNext()) {
				SiteFilePluginContentConsumer element = (SiteFilePluginContentConsumer) iter.next();
				element.abort();
			}
		}
		contentConsumers = null;		
		boolean sucess = true;
		
		//Remove feature.xml first if it exists
		if (oldPath!=null){
			ErrorRecoveryLog.getLog().appendPath(ErrorRecoveryLog.DELETE_ENTRY, oldPath);
			File fileToDelete = new File(oldPath);
			if (fileToDelete.exists()){
				sucess = fileToDelete.delete();
			}	
		}		

		if(!sucess){
			String msg = Policy.bind("Unable to delete",oldPath);
			UpdateManagerPlugin.log(msg,null);
		} else {
			// remove the feature files;
			Iterator iter = installedFiles.iterator();
			File featureFile = null;
			while (iter.hasNext()) {
				String path = (String) iter.next();
				featureFile = new File(path);
				UpdateManagerUtils.removeFromFileSystem(featureFile);			
			}
			
			// remove the feature directory if empty
			String featurePath = getFeaturePath();
			UpdateManagerUtils.removeEmptyDirectoriesFromFileSystem(new File(featurePath));
		}		
		closed= true;
		return;
	}

	/*
	 * commit the plugins installed as archive on the site
	 * (creates the map between the plugin id and the location of the plugin)
	 */
	private void commitPlugins(IFeatureReference localFeatureReference)
		throws CoreException {

		// get the feature
		((SiteFile) getSite()).addFeatureReferenceModel(
			(FeatureReferenceModel) localFeatureReference);
		IFeature localFeature=null;			
		try {
			localFeature = localFeatureReference.getFeature();
		} catch (CoreException e) {
			UpdateManagerPlugin.warn(null,e);
			return;
		}

		if (localFeature==null) return;

		// add the installed plugins directories as archives entry
		SiteFileFactory archiveFactory = new SiteFileFactory();
		ArchiveReferenceModel archive = null;
		IPluginEntry[] pluginEntries = localFeature.getPluginEntries();
		for (int i = 0; i < pluginEntries.length; i++) {
			String versionId =
				pluginEntries[i].getVersionedIdentifier().toString();
			String pluginID =
				Site.DEFAULT_PLUGIN_PATH
					+ versionId
					+ FeaturePackagedContentProvider.JAR_EXTENSION;
			archive = archiveFactory.createArchiveReferenceModel();
			archive.setPath(pluginID);
			try {
				URL url =
					new URL(
						getSite().getURL(),
						Site.DEFAULT_PLUGIN_PATH + versionId + File.separator);
				archive.setURLString(url.toExternalForm());
				archive.resolve(url, null);
				((SiteFile) getSite()).addArchiveReferenceModel(archive);
			} catch (MalformedURLException e) {
				String id =
					UpdateManagerPlugin
						.getPlugin()
						.getDescriptor()
						.getUniqueIdentifier();
				String urlString =
					(getSite().getURL() != null)
						? getSite().getURL().toExternalForm()
						: "";
				//$NON-NLS-1$
				urlString += Site.DEFAULT_PLUGIN_PATH
					+ pluginEntries[i].toString();
				throw Utilities.newCoreException(
					Policy.bind("SiteFile.UnableToCreateURL", urlString),
					e);
				//$NON-NLS-1$
			}
		}
		return;
	}

	/*
	 * Adds a SiteFilePluginContentConsumer to the list
	 */
	private void addContentConsumers(ContentConsumer cons){
		if (contentConsumers == null)
			contentConsumers = new ArrayList();
		contentConsumers.add(cons);
	}

}