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
package org.eclipse.update.internal.mirror;
import java.io.*;
import java.net.*;

import org.eclipse.core.runtime.*;
import org.eclipse.update.core.*;
import org.eclipse.update.core.model.*;
import org.eclipse.update.internal.core.*;
import org.eclipse.update.internal.model.*;
import org.eclipse.update.standalone.*;
import org.xml.sax.*;

public class MirrorSiteFactory extends BaseSiteFactory {
	/*
	 * @see SiteModelFactory#createSiteMapModel()
	 */
	public SiteModel createSiteMapModel() {
		return new MirrorSite(this);
	}
	/*
	 * @see ISiteFactory#createSite(URL)
	 */
	public ISite createSite(URL url)
		throws CoreException, InvalidSiteTypeException {
		return createSite(new File(url.getFile()));
	}
	/*
	 * @see ISiteFactory#createSite(URL)
	 */
	public ISite createSite(File siteLocation)
		throws CoreException, InvalidSiteTypeException {

		InputStream siteStream = null;

		if (!siteLocation.exists()) {
			if (!siteLocation.mkdirs()) {
				throw Utilities.newCoreException(
					"Directory " //$NON-NLS-1$
						+ siteLocation.getAbsolutePath()
						+ " could not be created.", //$NON-NLS-1$
					null);
			}
		}
		if (!siteLocation.isDirectory() || !siteLocation.canWrite())
			throw Utilities.newCoreException(
				siteLocation.getAbsolutePath()
					+ " is not a directory or is not writtable.", //$NON-NLS-1$
				null);

		MirrorSite site = null;
		// parse public features
		if (new File(siteLocation, Site.SITE_XML).exists()) {
			try {
				siteStream =
					new FileInputStream(new File(siteLocation, Site.SITE_XML));
			} catch (FileNotFoundException fnfe) {
			}
			site = (MirrorSite) parseSite(siteStream);
			try {
				if (siteStream != null)
					siteStream.close();
			} catch (IOException e) {
			}
		}
		if (site == null) {
			site = (MirrorSite) createSiteMapModel();
		}
		// parse downloaded plugins and fragments
		parseDownloadedPluginsAndFragments(
			site,
			new File(siteLocation, Site.DEFAULT_PLUGIN_PATH));
		// parse downloaded features
		parseDownloadedFeatures(
			site,
			new File(siteLocation, Site.DEFAULT_FEATURE_PATH));

		URL url;
		try {
			url = siteLocation.toURL();
		} catch (MalformedURLException mue) {
			throw Utilities.newCoreException(
				"A URL for site " //$NON-NLS-1$
					+ siteLocation.getAbsolutePath()
					+ " could not be created.", //$NON-NLS-1$
				mue);
		}
		SiteContentProvider contentProvider = null;
		contentProvider = new SiteFileContentProvider(url);

		site.setSiteContentProvider(contentProvider);
		contentProvider.setSite(site);
		try {
			site.resolve(url, url);
		} catch (MalformedURLException mue) {
			throw Utilities.newCoreException(
				"Unable to resolve URL " //$NON-NLS-1$
					+ (url == null ? "" : url.toExternalForm()), //$NON-NLS-1$
				mue);
		}
		return site;
	}
	/**
	 *  
	 */
	private void parseDownloadedPluginsAndFragments(MirrorSite site,
			File pluginDir) throws CoreException {
		if (!pluginDir.exists()) {
			return;
		}
		String[] dir = pluginDir.list(FeaturePackagedContentProvider.filter);
		for (int i = 0; i < dir.length; i++) {
			InputStream in = null;
			try {
				File file = new File(pluginDir, dir[i]);
				JarContentReference jarReference = new JarContentReference(
						null, file);
				ContentReference ref = jarReference.peek("META-INF/MANIFEST.MF", null, null); //$NON-NLS-1$
				if (ref != null) {
					try {
						in = ref.getInputStream();
					}
					catch (SecurityException e) {
						// in case of an invalid signature in jar, we will catch 
						// and re-throw a little more specific message. Otherwise, it's 
						// impossible to tell which jar had the problem. 
						String filename = file.getName();
						CoreException updateException = Utilities.newCoreException(filename,e);
						throw updateException;
					}
					BundleManifest manifest = new BundleManifest(in);
					if (manifest.exists()) {
						site
								.addDownloadedPluginEntry(manifest
										.getPluginEntry());
						continue;
					}
				}
				ref = jarReference.peek("plugin.xml", null, null); //$NON-NLS-1$
				if (ref == null) {
					ref = jarReference.peek("fragment.xml", null, null); //$NON-NLS-1$
				}
				if (ref != null) {
					in = ref.getInputStream();
					PluginEntry entry = new DefaultPluginParser().parse(in);
					site.addDownloadedPluginEntry(entry);
				}
			} catch (IOException e) {
				StandaloneUpdateApplication.exceptionLogged();
				UpdateCore.log(e);
			} catch (SAXException e) {
				StandaloneUpdateApplication.exceptionLogged();
				UpdateCore.log(e);
			} finally {
				if(in !=null){
					try{
						in.close();
					}catch(IOException ce){
					}
				}
			}
		}
	}
	/**
	* Method parseFeature.
	* @throws CoreException
	*/
	private void parseDownloadedFeatures(MirrorSite site, File featureDir)
		throws CoreException {
		if (featureDir.exists()) {
			String[] dir;
			SiteFeatureReferenceModel featureRef;
			URL featureURL;
			File currentFeatureFile;
			String newFilePath = null;

			try {
				// only list JAR files
				dir = featureDir.list(FeaturePackagedContentProvider.filter);
				for (int index = 0; index < dir.length; index++) {

					// check if the JAR file contains a feature.xml
					currentFeatureFile = new File(featureDir, dir[index]);
					JarContentReference ref =
						new JarContentReference("", currentFeatureFile); //$NON-NLS-1$
					ContentReference result = null;
					try {
						result = ref.peek(Feature.FEATURE_XML, null, null);
					} catch (IOException e) {
						UpdateCore.warn(
							"Exception retrieving feature.xml in file:" //$NON-NLS-1$
								+ currentFeatureFile,
							e);
					}
					if (result == null) {
						UpdateCore.warn(
							"Unable to find feature.xml in file:" //$NON-NLS-1$
								+ currentFeatureFile);
					} else {
						featureURL = currentFeatureFile.toURL();
						featureRef = createFeatureReferenceModel();
						featureRef.setSiteModel(site);
						featureRef.setURLString(featureURL.toExternalForm());
						featureRef.setType(ISite.DEFAULT_PACKAGED_FEATURE_TYPE);
						featureRef.setFeatureIdentifier(
							featureRef
								.getVersionedIdentifier()
								.getIdentifier());
						featureRef.setFeatureVersion(
							featureRef
								.getVersionedIdentifier()
								.getVersion()
								.toString());
						site.addDownloadedFeatureReferenceModel(featureRef);
					}
				}
			} catch (MalformedURLException e) {
				throw Utilities.newCoreException(
					"Unable to create URL for file " + newFilePath + ".", //$NON-NLS-1$ //$NON-NLS-2$
					e);
			}
		}
	}

}
