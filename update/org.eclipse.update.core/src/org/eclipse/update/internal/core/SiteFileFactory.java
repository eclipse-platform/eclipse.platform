package org.eclipse.update.internal.core;
/*
 * (c) Copyright IBM Corp. 2000, 2002.
 * All Rights Reserved.
 */
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.core.runtime.*;
import org.eclipse.update.core.*;
import org.eclipse.update.core.model.*;
import org.eclipse.update.internal.model.*;
import org.xml.sax.SAXException;
import org.eclipse.update.internal.core.Policy;

public class SiteFileFactory extends BaseSiteFactory {

	// private when parsing file system
	private SiteFile site;

	/**
	 * manages the versionedIdentifier and location of parsed plugins
	 */
	public class PluginIdentifier {
		private VersionedIdentifier id;
		private File location;
		public PluginIdentifier(VersionedIdentifier id, File location) {
			this.id = id;
			this.location = location;
		}

		public String getIdentifier() {
			if (id != null)
				return id.getIdentifier();
			return null;
		}

		public PluginVersionIdentifier getVersion() {
			if (id != null)
				return id.getVersion();
			return null;
		}

		public File getLocation() {
			return location;
		}

		public String toString() {
			if (id != null)
				return id.toString();
			return ""; //$NON-NLS-1$
		}
	}

	/*
	 * @see ISiteFactory#createSite(URL,boolean)
	 */
	public ISite createSite(URL url)
		throws CoreException, InvalidSiteTypeException {

		Site site = null;
		InputStream siteStream = null;
		SiteModelFactory factory = (SiteModelFactory) this;

		try {
			// if url points to a directory
			// attempt to parse site.xml
			String path = url.getFile();
			File siteLocation = new File(path);
			if (siteLocation.isDirectory()) {
				path = siteLocation.getAbsolutePath().replace(File.separatorChar,'/');
				if (!path.endsWith("/"))
					path += "/";
				url = new URL("file:" + path); //$NON-NLS-1$

				if (new File(siteLocation, Site.SITE_XML).exists()) {
					siteStream = new FileInputStream(new File(siteLocation, Site.SITE_XML));
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
					siteStream = resolvedURL.openStream();
					site = (Site) factory.parseSite(siteStream);
				} catch (IOException e) {

					// attempt to parse parent directory
					File file = new File(url.getFile());
					File parentDirectory = file.getParentFile();

					// create directory if it doesn't exist						
					if (parentDirectory != null && !parentDirectory.exists()) {
						parentDirectory.mkdirs();
					}

					if (parentDirectory == null || !parentDirectory.isDirectory())
						throw Utilities.newCoreException(
							Policy.bind(
								"SiteFileFactory.UnableToObtainParentDirectory",
								file.getAbsolutePath()),
							null);
					//$NON-NLS-1$

					site = parseSite(parentDirectory);

				}
			}

			SiteContentProvider contentProvider = new SiteFileContentProvider(url);
			site.setSiteContentProvider(contentProvider);
			contentProvider.setSite(site);
			site.resolve(url, getResourceBundle(url));

			// Do not set read only as may install in it
			//site.markReadOnly();
		} catch (MalformedURLException e) {
			throw Utilities.newCoreException(
				Policy.bind("SiteFileFactory.UnableToCreateURL",url==null?"":url.toExternalForm()),
				e);
			//$NON-NLS-1$
		} catch (IOException e) {
			throw Utilities.newCoreException(
				Policy.bind("SiteFileFactory.UnableToAccessSite"),
				e);
			//$NON-NLS-1$
		} finally {
			try {
				if (siteStream != null)
					siteStream.close();
			} catch (Exception e) {
			}
		}
		return site;
	}
	/**
	 * Method parseSite.
	 */
	public Site parseSite(File directory) throws CoreException {

		this.site = (SiteFile) createSiteMapModel();

		if (!directory.exists())
			throw Utilities.newCoreException(
				Policy.bind("SiteFileFactory.FileDoesNotExist", directory.getAbsolutePath()),
				null);
		//$NON-NLS-1$

		File pluginPath = new File(directory, Site.DEFAULT_PLUGIN_PATH);

		//PACKAGED
		parsePackagedFeature(directory); // in case it contains JAR files

		parsePackagedPlugins(pluginPath);

		// INSTALLED	
		parseInstalledFeature(directory);

		parseInstalledPlugin(pluginPath);

		return (Site) site;

	}

	/**
	 * Method parseFeature.
	 * @throws CoreException
	 */
	private void parseInstalledFeature(File directory) throws CoreException {

		File featureDir = new File(directory, Site.DEFAULT_INSTALLED_FEATURE_PATH);
		if (featureDir.exists()) {
			String[] dir;
			FeatureReferenceModel featureRef;
			URL featureURL;
			String newFilePath = null;

			try {
				// handle the installed featuresConfigured under featuresConfigured subdirectory
				dir = featureDir.list();
				for (int index = 0; index < dir.length; index++) {

					SiteFileFactory archiveFactory = new SiteFileFactory();
					// the URL must ends with '/' for the bundle to be resolved
					newFilePath = dir[index] + (dir[index].endsWith("/") ? "/" : "");
					//$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					featureURL = new File(featureDir, newFilePath).toURL();

					featureRef = archiveFactory.createFeatureReferenceModel();
					featureRef.setSiteModel(site);
					featureRef.setURLString(featureURL.toExternalForm());
					featureRef.setType(ISite.DEFAULT_INSTALLED_FEATURE_TYPE);
					((Site) site).addFeatureReferenceModel(featureRef);

				}
			} catch (MalformedURLException e) {
				throw Utilities.newCoreException(
					Policy.bind("SiteFileFactory.UnableToCreateURLForFile", newFilePath),
					e);
				//$NON-NLS-1$
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
			FeatureReferenceModel featureRef;
			URL featureURL;
			String newFilePath = null;

			try {
				// handle the installed featuresConfigured under featuresConfigured subdirectory
				dir = featureDir.list(FeaturePackagedContentProvider.filter);
				for (int index = 0; index < dir.length; index++) {

					featureURL = new File(featureDir, dir[index]).toURL();

					SiteFileFactory archiveFactory = new SiteFileFactory();
					featureRef = archiveFactory.createFeatureReferenceModel();
					featureRef.setSiteModel(site);
					featureRef.setURLString(featureURL.toExternalForm());
					featureRef.setType(ISite.DEFAULT_PACKAGED_FEATURE_TYPE);
					site.addFeatureReferenceModel(featureRef);

				}
			} catch (MalformedURLException e) {
				throw Utilities.newCoreException(
					Policy.bind("SiteFileFactory.UnableToCreateURLForFile", newFilePath),
					e);
				//$NON-NLS-1$
			}
		}
	}

	/**
	 * Method parsePlugins.
	 * 
	 * look into each plugin/fragment directory, crack the plugin.xml open (or fragment.xml ???)
	 * get id and version, calculate URL...	
	 * 
	 * @return VersionedIdentifier
	 * @throws CoreException
	 */
	private void parseInstalledPlugin(File dir) throws CoreException {
		PluginIdentifier plugin = null;
		File pluginFile = null;

		try {
			if (dir.exists() && dir.isDirectory()) {
				File[] files = dir.listFiles();
				for (int i = 0; i < files.length; i++) {
					if (files[i].isDirectory()) {

						if (!(pluginFile = new File(files[i], "plugin.xml")).exists()) { //$NON-NLS-1$
							pluginFile = new File(files[i], "fragment.xml"); //$NON-NLS-1$
						}

						if (pluginFile != null && pluginFile.exists()) {
							IPluginEntry entry =
								new DefaultPluginParser().parse(new FileInputStream(pluginFile));
							VersionedIdentifier identifier = entry.getVersionedIdentifier();
							plugin = new PluginIdentifier(identifier, files[i]);

							addParsedPlugin(plugin);

						}
					} // files[i] is a directory
				}
			} // path is a directory
		} catch (IOException e) {
			throw Utilities.newCoreException(
				Policy.bind("SiteFileFactory.ErrorAccessing", pluginFile.getAbsolutePath()),
				e);
			//$NON-NLS-1$
		} catch (SAXException e) {
			throw Utilities.newCoreException(
				Policy.bind("SiteFileFactory.ErrorParsingFile", pluginFile.getAbsolutePath()),
				e);
			//$NON-NLS-1$
		}

	}

	/**
	 * Method addParsedPlugins.
	 * @param model
	 * @throws CoreException
	 */
	private void addParsedPlugin(PluginIdentifier plugin) throws CoreException {

		// tranform each Plugin and Fragment in an ArchiveEntry
		// and a PluginEntry for the Site
		String location = null;
		try {
			if (plugin != null) {
				PluginEntry entry = new PluginEntry();
				entry.setPluginIdentifier(plugin.getIdentifier());
				entry.setPluginVersion(plugin.getVersion().toString());
				((Site) site).addPluginEntry(entry);

				SiteFileFactory archiveFactory = new SiteFileFactory();
				// the id is plugins\<pluginid>_<ver>.jar as per the specs
				ArchiveReferenceModel archive = archiveFactory.createArchiveReferenceModel();
				String pluginID =
					Site.DEFAULT_PLUGIN_PATH
						+ plugin.toString()
						+ FeaturePackagedContentProvider.JAR_EXTENSION;
				archive.setPath(pluginID);
				location = plugin.getLocation().toURL().toExternalForm();
				archive.setURLString(location);
				((Site) site).addArchiveReferenceModel(archive);
			}
		} catch (MalformedURLException e) {
			throw Utilities.newCoreException(
				Policy.bind("SiteFileFactory.UnableToCreateURLForFile", location),
				e);
			//$NON-NLS-1$
		}
	}

	/**
	 * 
	 */
	private void parsePackagedPlugins(File pluginDir) throws CoreException {

		File file = null;
		String[] dir;

		PluginIdentifier plugin;
		ContentReference ref = null;
		String refString = null;

		try {
			if (pluginDir.exists()) {
				dir = pluginDir.list(FeaturePackagedContentProvider.filter);
				for (int i = 0; i < dir.length; i++) {

					file = new File(pluginDir, dir[i]);
					JarContentReference jarReference = new JarContentReference(null, file);
					ref = jarReference.peek("plugin.xml", null, null); //$NON-NLS-1$
					if (ref == null)
						jarReference.peek("fragment.xml", null, null); //$NON-NLS-1$

					refString = (ref == null) ? null : ref.asURL().toExternalForm();

					if (ref != null) {
						IPluginEntry entry = new DefaultPluginParser().parse(ref.getInputStream());
						VersionedIdentifier identifier = entry.getVersionedIdentifier();
						plugin = new PluginIdentifier(identifier, file);
						addParsedPlugin(plugin);
					} //ref!=null
				} //for
			}

		} catch (Exception e) {
			throw Utilities.newCoreException(
				Policy.bind("SiteFileFactory.ErrorAccessing", refString),
				e);
			//$NON-NLS-1$
		}

	}

	/**
	 * 
	 */
	private IFeature createFeature(URL url, String type) throws CoreException {
		FeatureReference ref = new InternalFeatureReference();
		ref.setSite(site);
		ref.setURL(url);
		ref.setType(type);
		return ref.getFeature();
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
		return (
			super.canParseSiteType(type)
				|| SiteFileContentProvider.SITE_TYPE.equalsIgnoreCase(type));
	}

	/**
	 * 
	 */
	public void abort() {
	}

}