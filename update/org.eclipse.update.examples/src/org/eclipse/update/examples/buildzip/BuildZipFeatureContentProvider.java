package org.eclipse.update.examples.buildzip;

/*
 * (c) Copyright IBM Corp. 2000, 2002.
 * All Rights Reserved.
 */ 

import java.io.*;
import java.net.URL;
import java.util.jar.JarEntry;

import org.eclipse.core.runtime.*;
import org.eclipse.update.core.*;
import org.eclipse.update.core.JarContentReference.ContentSelector;
import org.eclipse.update.core.model.FeatureModel;
import org.eclipse.update.core.model.InstallAbortedException;

/**
 * An example feature content provider. It handles features packaged as
 * build zip's using the format used for integration and stable builds
 * posted on the downloads pages at www.eclipse.org
 * </p>
 * @since 2.0
 */

public class BuildZipFeatureContentProvider extends FeatureContentProvider implements IFeatureContentProvider {

	private JarContentReference baseReference;
	private ContentReference generatedFeatureManifest;	
	private ContentReference[] featureEntryContentReferences;
	private IPluginEntry currentPluginEntry;
	private INonPluginEntry currentNonPluginEntry;
	
	public BuildZipFeatureContentProvider(URL base) {
		super(base);
		this.baseReference = new JarContentReference("build.zip",base);
	}
	
	/*
	 * @see IFeatureContentProvider#getFeatureManifest()
	 */
	public ContentReference getFeatureManifestReference(InstallMonitor monitor) throws CoreException {
		if (generatedFeatureManifest == null) {
			throw newCoreException("Feature manifest is not available",null);
		}
		else
			return generatedFeatureManifest;
	}

	/*
	 * @see IFeatureContentProvider#getArchiveReferences()
	 */
	public ContentReference[] getArchiveReferences(InstallMonitor monitor) {
		// the feature and all its plugins files are packaged in a single archive
		return new ContentReference[] { baseReference };
	}

	/*
	 * @see IFeatureContentProvider#getFeatureEntryArchiveReferences()
	 */
	public ContentReference[] getFeatureEntryArchiveReferences(InstallMonitor monitor) {
		// the feature and all its plugins files are packaged in a single archive
		return new ContentReference[] { baseReference };
	}

	/*
	 * @see IFeatureContentProvider#getPluginEntryArchiveReferences(IPluginEntry)
	 */
	public ContentReference[] getPluginEntryArchiveReferences(IPluginEntry pluginEntry, InstallMonitor monitor) {
		// the feature and all its plugins files are packaged in a single archive
		return new ContentReference[] { baseReference };
	}

	/*
	 * @see IFeatureContentProvider#getNonPluginEntryArchiveReferences(INonPluginEntry)
	 */
	public ContentReference[] getNonPluginEntryArchiveReferences(INonPluginEntry nonPluginEntry, InstallMonitor monitor) 
		throws CoreException {
		try {
			return peekNonPluginEntryContent(nonPluginEntry,monitor);
		} catch(IOException e) {
			throw newCoreException("Unable to return content for non plugin entry "+nonPluginEntry.getIdentifier(),e);
		}
	}

	/*
	 * @see IFeatureContentProvider#getFeatureEntryContentReferences()
	 */
	public ContentReference[] getFeatureEntryContentReferences(InstallMonitor monitor) {
		if (featureEntryContentReferences == null)
			return new ContentReference[0];
		else
			return  featureEntryContentReferences;
	}

	/*
	 * @see IFeatureContentProvider#getPluginEntryContentReferences(IPluginEntry)
	 */
	public ContentReference[] getPluginEntryContentReferences(IPluginEntry pluginEntry, InstallMonitor monitor)
		throws CoreException {
		try {
			return peekPluginEntryContent(pluginEntry, monitor);
		} catch(IOException e) {
			throw newCoreException("Unable to return content for plugin entry "+pluginEntry.getVersionedIdentifier(),e);
		}
	}
	
	public URL getFeatureBaseURL() throws IOException {
		return getWorkingDirectory().toURL();
			
	}

	ContentReference getFeatureBuildManifest() throws Exception {
		return baseReference.peek("eclipse/buildmanifest.properties",null/*ContentSelector*/, null/*ProgressMonitor*/);
	}

	ContentReference getPluginManifest(String pluginId, boolean isFragment) throws Exception {			
		String manifestName = "eclipse/plugins/" + pluginId + "/" + (isFragment ? "fragment.xml" : "plugin.xml");
		return baseReference.peek(manifestName, null/*ContentSelector*/, null/*ProgressMonitor*/);
	}
	
	void unpackFeatureEntryContent(FeatureModel feature, InstallMonitor monitor) throws IOException, InstallAbortedException {
			
		// define selector for feature entry files
		ContentSelector selector = new ContentSelector() {
			public boolean include(JarEntry jarEntry) {
				if (jarEntry.isDirectory())
					return false;					
				String entry = jarEntry.getName();
				if (entry.startsWith("eclipse/readme/"))
					return true;
				else if (entry.startsWith("eclipse/splash/"))
					return true;
				else if (entry.startsWith("eclipse/") && entry.indexOf("/",8)==-1 && entry.endsWith(".html"))
					return true;
				else
					return false;	
			}
			public String defineIdentifier(JarEntry jarEntry) {
				String entry = jarEntry.getName();
				if (entry.startsWith("eclipse/"))
					return entry.substring(8);
				else
					return entry;
			}
		};
		
		// unpack feature entry files
		File tmpDir = getWorkingDirectory();
		ContentReference[] refs = baseReference.unpack(tmpDir, selector, monitor);
		
		// write out feature manifest (feature.xml);
		File manifest = Utilities.createLocalFile(tmpDir,"feature.xml");
		ContentReference manifestReference = new ContentReference("feature.xml", manifest);
		DefaultModelWriter w = new DefaultModelWriter(feature);
		FileOutputStream os = null;
		try {
			os = new FileOutputStream(manifest);
			w.writeFeatureManifest(os);
		} finally {
			if (os != null) try { os.close(); } catch(IOException e) {}
		}
		this.generatedFeatureManifest = manifestReference;
		
		// save references (newly-written feature.xml plus rest of selected feature files)
		ContentReference[] allRefs = new ContentReference[refs.length+1];
		allRefs[0] = manifestReference;
		System.arraycopy(refs,0,allRefs,1,refs.length);
		featureEntryContentReferences = allRefs;
	}
		
	ContentReference[] peekPluginEntryContent(IPluginEntry plugin, InstallMonitor monitor) throws IOException {
		
		// define selector for plugin entry files
		ContentSelector selector = new ContentSelector() {
			public boolean include(JarEntry jarEntry) {
				if (jarEntry.isDirectory())
					return false;
				String entry = jarEntry.getName();
				String id = currentPluginEntry.getVersionedIdentifier().getIdentifier();
				if (id==null)
					return false;
				else if (entry.startsWith("eclipse/plugins/"+id+"/"))
					return true;
				else
					return false;	
			}
			public String defineIdentifier(JarEntry jarEntry) {
				String entry = jarEntry.getName();
				int ix = entry.indexOf("/",16);
				if (ix != -1) {
					return entry.substring(ix+1);
				} else {
					return entry;
				}
			}
		};
		
		// unpack plugin entry files
		currentPluginEntry = plugin;
		return baseReference.peek(selector, monitor);
	}
		
	ContentReference[] peekNonPluginEntryContent(INonPluginEntry data, InstallMonitor monitor) throws IOException {
		
		// define selector for non plugin entry files
		ContentSelector selector = new ContentSelector() {
			public boolean include(JarEntry jarEntry) {
				if (jarEntry.isDirectory())
					return false;
				String entry = jarEntry.getName();
				String id = currentNonPluginEntry.getIdentifier();
				if (!id.equals("root"))
					return false;
				else if (!entry.startsWith("eclipse/plugins/"))
					return true;
				else
					return false;	
			}
			public String defineIdentifier(JarEntry jarEntry) {
				String entry = jarEntry.getName();
				if (entry.startsWith("eclipse/"))
					return entry.substring(8);
				else
					return entry;
			}
		};
		
		// unpack non plugin entry files
		currentNonPluginEntry = data;
		return baseReference.peek(selector, monitor);
	}
	
	private CoreException newCoreException(String s, Throwable e) throws CoreException {
		return new CoreException(new Status(IStatus.ERROR,"org.eclipse.update.examples",0,s,e));
	}
	/*
	 * @see IFeatureContentProvider#getVerifier()
	 */
	public IVerifier getVerifier() throws CoreException {
		return null;
	}

}
