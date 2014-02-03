/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
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

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.eclipse.update.core.BaseSiteFactory;
import org.eclipse.update.core.ContentReference;
import org.eclipse.update.core.Feature;
import org.eclipse.update.core.ISite;
import org.eclipse.update.core.JarContentReference;
import org.eclipse.update.core.PluginEntry;
import org.eclipse.update.core.Site;
import org.eclipse.update.core.SiteContentProvider;
import org.eclipse.update.core.SiteFeatureReferenceModel;
import org.eclipse.update.core.Utilities;
import org.eclipse.update.core.model.ArchiveReferenceModel;
import org.eclipse.update.core.model.InvalidSiteTypeException;
import org.eclipse.update.core.model.SiteModel;
import org.eclipse.update.core.model.SiteModelFactory;
import org.eclipse.update.internal.model.BundleManifest;
import org.eclipse.update.internal.model.DefaultPluginParser;
import org.xml.sax.SAXException;

public class SiteFileFactory extends BaseSiteFactory {

	// private when parsing file system
	private SiteFile site;

	/*
	 * @see ISiteFactory#createSite(URL,boolean)
	 */
	public ISite createSite(URL url) throws CoreException, InvalidSiteTypeException {

		Site site = null;
		InputStream siteStream = null;
		SiteModelFactory factory = this;

		try {
			// if url points to a directory
			// attempt to parse site.xml
			String path = url.getFile();
			File siteLocation = new File(path);
			if (siteLocation.isDirectory()) {
				url = siteLocation.toURL();
				File siteXMLFile = new File(siteLocation, Site.SITE_XML);
				if (siteXMLFile.exists()) {
					siteStream = new FileInputStream(siteXMLFile);
					site = (Site) factory.parseSite(siteStream);
				} else {
					// parse siteLocation
					site = parseSite(siteLocation);
				}
			} else {
				// we are not pointing to a directory
				// attempt to parse the file
				try {
					URL resolvedURL = URLEncoder.encode(url);
					siteStream = openStream(resolvedURL);
					site = (Site) factory.parseSite(siteStream);
				} catch (IOException e) {

					// attempt to parse parent directory
					File file = new File(url.getFile());
					File parentDirectory = file.getParentFile();

					// do not create directory if it doesn't exist	[18318]
					// instead hrow error					
					if (parentDirectory != null && !parentDirectory.exists())
						throw Utilities.newCoreException(NLS.bind(Messages.SiteFileFactory_DirectoryDoesNotExist, (new String[] {file.getAbsolutePath()})), null);

					if (parentDirectory == null || !parentDirectory.isDirectory())
						throw Utilities.newCoreException(NLS.bind(Messages.SiteFileFactory_UnableToObtainParentDirectory, (new String[] {file.getAbsolutePath()})), null);

					site = parseSite(parentDirectory);

				}
			}

			SiteContentProvider contentProvider = new SiteFileContentProvider(url);
			site.setSiteContentProvider(contentProvider);
			contentProvider.setSite(site);
			site.resolve(url, url);

			// Do not set read only as may install in it
			//site.markReadOnly();
		} catch (MalformedURLException e) {
			throw Utilities.newCoreException(NLS.bind(Messages.SiteFileFactory_UnableToCreateURL, (new String[] {url == null ? "" : url.toExternalForm()})), e); //$NON-NLS-1$
		} catch (IOException e) {
			throw Utilities.newCoreException(Messages.SiteFileFactory_UnableToAccessSite, ISite.SITE_ACCESS_EXCEPTION, e);
		} finally {
			try {
				if (siteStream != null)
					siteStream.close();
			} catch (IOException e) {
			}
		}
		return site;
	}

	/**
	 * Method parseSite.
	 */
	private Site parseSite(File directory) throws CoreException {

		this.site = (SiteFile) createSiteMapModel();

		if (!directory.exists())
			throw Utilities.newCoreException(NLS.bind(Messages.SiteFileFactory_FileDoesNotExist, (new String[] {directory.getAbsolutePath()})), null);

		File pluginPath = new File(directory, Site.DEFAULT_PLUGIN_PATH);

		//PACKAGED
		try {
			parsePackagedFeature(directory); // in case it contains JAR files
		} catch (EmptyDirectoryException ede) {
			UpdateCore.log(ede.getStatus());
		}

		try {
			parsePackagedPlugins(pluginPath);
		} catch (EmptyDirectoryException ede) {
			UpdateCore.log(ede.getStatus());
		}

		// INSTALLED
		try {
			parseInstalledFeature(directory);
		} catch (EmptyDirectoryException ede) {
			UpdateCore.log(ede.getStatus());
		}

		try {
			parseInstalledPlugins(pluginPath);
		} catch (EmptyDirectoryException ede) {
			UpdateCore.log(ede.getStatus());
		}

		return site;

	}

	/**
	 * Method parseFeature.
	 * @throws CoreException
	 */
	private void parseInstalledFeature(File directory) throws CoreException {

		File featureDir = new File(directory, Site.DEFAULT_INSTALLED_FEATURE_PATH);
		if (featureDir.exists()) {
			String[] dir;
			SiteFeatureReferenceModel featureRef;
			URL featureURL;
			File currentFeatureDir;
			String newFilePath = null;

			try {
				// handle the installed featuresConfigured under featuresConfigured subdirectory
				dir = featureDir.list();
				if (dir == null) {
					throw new EmptyDirectoryException(new Status(IStatus.WARNING, UpdateCore.getPlugin().getBundle().getSymbolicName(), IStatus.OK, directory.getName() + File.separator + directory.getName() + "directory is empty", null)); //$NON-NLS-1$
				}
				for (int index = 0; index < dir.length; index++) {

					// the URL must ends with '/' for the bundle to be resolved
					newFilePath = dir[index] + (dir[index].endsWith("/") ? "/" : ""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					currentFeatureDir = new File(featureDir, newFilePath);
					// check if feature.xml exists
					File featureXMLFile = new File(currentFeatureDir, Feature.FEATURE_XML);
					if (!featureXMLFile.exists()) {
						UpdateCore.warn("Unable to find feature.xml in directory:" + currentFeatureDir); //$NON-NLS-1$
					} else {
						// PERF: remove code
						//SiteFileFactory archiveFactory = new SiteFileFactory();
						featureURL = currentFeatureDir.toURL();
						featureRef = createFeatureReferenceModel();
						featureRef.setSiteModel(site);
						featureRef.setURLString(featureURL.toExternalForm());
						featureRef.setType(ISite.DEFAULT_INSTALLED_FEATURE_TYPE);
						((Site) site).addFeatureReferenceModel(featureRef);
					}
				}
			} catch (MalformedURLException e) {
				throw Utilities.newCoreException(NLS.bind(Messages.SiteFileFactory_UnableToCreateURLForFile, (new String[] {newFilePath})), e);
			}
		}
	}

	/**
	* Method parseFeature.
	* @throws CoreException
	*/
	private void parsePackagedFeature(File directory) throws CoreException {

		// FEATURES
		File featureDir = new File(directory, Site.DEFAULT_FEATURE_PATH);
		if (featureDir.exists()) {
			String[] dir;
			SiteFeatureReferenceModel featureRef;
			URL featureURL;
			File currentFeatureFile;
			String newFilePath = null;

			try {
				// only list JAR files
				dir = featureDir.list(FeaturePackagedContentProvider.filter);
				if (dir == null) {
					throw new EmptyDirectoryException(new Status(IStatus.WARNING, UpdateCore.getPlugin().getBundle().getSymbolicName(), IStatus.OK, directory.getName() + File.separator + directory.getName() + "directory is empty", null)); //$NON-NLS-1$
				}

				for (int index = 0; index < dir.length; index++) {

					// check if the JAR file contains a feature.xml
					currentFeatureFile = new File(featureDir, dir[index]);
					JarContentReference ref = new JarContentReference("", currentFeatureFile); //$NON-NLS-1$
					ContentReference result = null;
					try {
						result = ref.peek(Feature.FEATURE_XML, null, null);
					} catch (IOException e) {
						UpdateCore.warn("Exception retrieving feature.xml in file:" + currentFeatureFile, e); //$NON-NLS-1$
					}
					if (result == null) {
						UpdateCore.warn("Unable to find feature.xml in file:" + currentFeatureFile); //$NON-NLS-1$
					} else {
						featureURL = currentFeatureFile.toURL();
						// PERF: remove code
						//SiteFileFactory archiveFactory = new SiteFileFactory();
						featureRef = createFeatureReferenceModel();
						featureRef.setSiteModel(site);
						featureRef.setURLString(featureURL.toExternalForm());
						featureRef.setType(ISite.DEFAULT_PACKAGED_FEATURE_TYPE);
						site.addFeatureReferenceModel(featureRef);
					}
				}
			} catch (MalformedURLException e) {
				throw Utilities.newCoreException(NLS.bind(Messages.SiteFileFactory_UnableToCreateURLForFile, (new String[] {newFilePath})), e);
			}
		}
	}

	/**
	 * Method parsePlugins.
	 * 
	 * look into each plugin/fragment directory, crack the plugin.xml open (or
	 * fragment.xml ???) get id and version, calculate URL...
	 * 
	 * @throws CoreException
	 */
	private void parseInstalledPlugins(File pluginsDir) throws CoreException {
		if (!pluginsDir.exists() || !pluginsDir.isDirectory()) {
			return;
		}
		File[] dirs = pluginsDir.listFiles(new FileFilter() {
			public boolean accept(File f) {
				return f.isDirectory();
			}
		});
		DefaultPluginParser parser = new DefaultPluginParser();

		if (dirs == null) {
			throw new EmptyDirectoryException(new Status(IStatus.WARNING, UpdateCore.getPlugin().getBundle().getSymbolicName(), IStatus.OK, pluginsDir.getName() + File.separator + pluginsDir.getName() + "directory is empty", null)); //$NON-NLS-1$
		}
		for (int i = 0; i < dirs.length; i++) {
			File pluginFile = new File(dirs[i], "META-INF/MANIFEST.MF"); //$NON-NLS-1$
			InputStream in = null;
			try {
				BundleManifest bundleManifest = new BundleManifest(pluginFile);
				if (bundleManifest.exists()) {
					PluginEntry entry = bundleManifest.getPluginEntry();
					addParsedPlugin(entry, dirs[i]);
				} else {
					if (!(pluginFile = new File(dirs[i], "plugin.xml")) //$NON-NLS-1$
							.exists()) {
						pluginFile = new File(dirs[i], "fragment.xml"); //$NON-NLS-1$
					}
					if (pluginFile != null && pluginFile.exists() && !pluginFile.isDirectory()) {
						in = new FileInputStream(pluginFile);
						PluginEntry entry = parser.parse(in);
						addParsedPlugin(entry, dirs[i]);
					}
				}
			} catch (IOException e) {
				String pluginFileString = (pluginFile == null) ? null : pluginFile.getAbsolutePath();
				UpdateCore.log(Utilities.newCoreException(NLS.bind(Messages.SiteFileFactory_ErrorAccessing, (new String[] {pluginFileString})), e));
			} catch (SAXException e) {
				String pluginFileString = (pluginFile == null) ? null : pluginFile.getAbsolutePath();
				UpdateCore.log(Utilities.newCoreException(NLS.bind(Messages.SiteFileFactory_ErrorParsingFile, (new String[] {pluginFileString})), e));
			} finally {
				if (in != null) {
					try {
						in.close();
					} catch (IOException e) {
					}
				}
			}
		}
	}

	/**
	 * transform each Plugin and Fragment into an ArchiveReferenceModel
	 * and a PluginEntry for the Site	 
	 */
	// PERF: removed intermediate Plugin object
	private void addParsedPlugin(PluginEntry entry, File file) throws CoreException {

		String location = null;
		try {
			if (entry != null) {

				// create the plugin Entry
				((Site) site).addPluginEntry(entry);

				// Create the Site mapping ArchiveRef->PluginEntry
				// the id of the archiveRef is plugins\<pluginid>_<ver>.jar as per the specs
				// PERF: remove code
				//SiteFileFactory archiveFactory = new SiteFileFactory();				
				ArchiveReferenceModel archive = createArchiveReferenceModel();
				String id = (entry.getVersionedIdentifier().toString());
				String pluginID = Site.DEFAULT_PLUGIN_PATH + id + FeatureContentProvider.JAR_EXTENSION;
				archive.setPath(pluginID);
				location = file.toURL().toExternalForm();
				archive.setURLString(location);
				((Site) site).addArchiveReferenceModel(archive);

				// TRACE				
				if (UpdateCore.DEBUG && UpdateCore.DEBUG_SHOW_PARSING) {
					UpdateCore.debug("Added archive to site:" + pluginID + " pointing to: " + location); //$NON-NLS-1$ //$NON-NLS-2$
				}
			}
		} catch (MalformedURLException e) {
			throw Utilities.newCoreException(NLS.bind(Messages.SiteFileFactory_UnableToCreateURLForFile, (new String[] {location})), e);
		}
	}

	/**
	 *  
	 */
	private void parsePackagedPlugins(File pluginDir) throws CoreException {
		if (!pluginDir.exists()) {
			return;
		}
		String[] dir = pluginDir.list(FeaturePackagedContentProvider.filter);

		if (dir == null) {
			throw new EmptyDirectoryException(new Status(IStatus.WARNING, UpdateCore.getPlugin().getBundle().getSymbolicName(), IStatus.OK, pluginDir.getName() + File.separator + pluginDir.getName() + "directory is empty", null)); //$NON-NLS-1$
		}
		for (int i = 0; i < dir.length; i++) {
			ContentReference ref = null;
			String refString = null;
			InputStream in = null;
			JarContentReference jarReference = null;
			try {
				File file = new File(pluginDir, dir[i]);
				jarReference = new JarContentReference(null, file);
				ref = jarReference.peek("META-INF/MANIFEST.MF", null, null); //$NON-NLS-1$
				if (ref != null) {
					in = ref.getInputStream();
					BundleManifest manifest = new BundleManifest(in);
					if (manifest.exists()) {

						addParsedPlugin(manifest.getPluginEntry(), file);
						continue;
					}
				}
				ref = jarReference.peek("plugin.xml", null, null);//$NON-NLS-1$
				if (ref == null) {
					ref = jarReference.peek("fragment.xml", null, null); //$NON-NLS-1$
				}
				if (ref != null) {
					in = ref.getInputStream();
					PluginEntry entry = new DefaultPluginParser().parse(in);
					addParsedPlugin(entry, file);
				}
			} catch (Exception e) {
				try {
					refString = (ref == null) ? null : ref.asURL().toExternalForm();
				} catch (IOException ioe) {
				}

				String message;

				if (e instanceof IOException) {
					message = NLS.bind(Messages.SiteFileFactory_ErrorAccessing, (new String[] {refString}));
				} else if (e instanceof SAXException) {
					message = NLS.bind(Messages.SiteFileFactory_ErrorParsingFile, (new String[] {refString}));
				} else {
					message = NLS.bind(Messages.SiteFileFactory_ErrorAccessing, (new String[] {refString}));
				}// end if

				// Log exception, but do not throw to caller.
				// Continue with processing remaining plug-ins
				UpdateCore.log(message, e);
			} finally {
				if (in != null) {
					try {
						in.close();
					} catch (IOException ce) {
					}
				}
				if (jarReference != null) {
					try {
						jarReference.closeArchive();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}
	}

	/*
	 * @see SiteModelFactory#createSiteMapModel()
	 */
	public SiteModel createSiteMapModel() {
		return new SiteFile();
	}

	/*
	 * @see SiteModelFactory#canParseSiteType(String)
	 */
	public boolean canParseSiteType(String type) {
		return (super.canParseSiteType(type) || SiteFileContentProvider.SITE_TYPE.equalsIgnoreCase(type));
	}

}
