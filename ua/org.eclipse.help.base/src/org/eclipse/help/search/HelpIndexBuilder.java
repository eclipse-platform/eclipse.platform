/*******************************************************************************
 * Copyright (c) 2004, 2016 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.help.search;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.help.internal.base.HelpBasePlugin;
import org.eclipse.help.internal.base.HelpBaseResources;
import org.eclipse.help.internal.entityresolver.LocalEntityResolver;
import org.eclipse.help.internal.search.AnalyzerDescriptor;
import org.eclipse.help.internal.search.PluginVersionInfo;
import org.eclipse.help.internal.search.SearchIndex;
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * Builds a help search index for a plug-in by looking for the
 * <code>org.eclipse.help.toc</code> extensions in the provided manifest file.
 * Search index is only created if index path is specified in the extension.
 * Index will contain data for all the topics listed in all the TOCs declared in
 * the plug-in.
 * <p>
 * If the index is created for a fragment, the manifest must point at the
 * referenced fragment plug-in, while the destination should be the fragment
 * itself.
 * </p>
 * <p>
 * Starting from the provided destination directory, index for each locale will
 * be placed in a directory with the following path:
 * </p>
 *
 * <pre>
 *                  destination/nl/country/
 *
 *                  or
 *
 *                  destination/nl/country/language/
 * </pre>
 * <p>
 * The relative directory specified in the <code>index</code> element of the
 * <code>org.eclipse.help.toc</code> extention will be created in each of the
 * locale-specific paths (one per locale).
 * </p>
 * An instance of <code>HelpIndexBuilder</code> can be cached and used multiple
 * times for different manifest and destination values.
 *
 * @since 3.1
 */

public class HelpIndexBuilder {
	private static final String POINT_TOC = "org.eclipse.help.toc"; //$NON-NLS-1$

	private static final String EL_TOC = "toc"; //$NON-NLS-1$

	private static final String EL_INDEX = "index"; //$NON-NLS-1$

	private File manifest;

	private String indexPath;

	private File destination;

	private ArrayList<TocFile> tocFiles = new ArrayList<>();

	private ArrayList<LocaleDir> localeDirs = new ArrayList<>();

	private static Locale[] legalLocales = Locale.getAvailableLocales();
	private static HashSet<String> legalLanguages = null;
	private static HashSet<String> legalCountries = null;

	static class PluginIdentifier {
		String id;

		Version version;

		public PluginIdentifier(String id, String version) {
			this.id = id;
			this.version = new Version(version);
		}
	}

	static class LocaleDir {
		String locale;
		String relativePath;
		ArrayList<File> dirs = new ArrayList<>();

		public LocaleDir(String locale, String relativePath) {
			this.locale = locale;
			this.relativePath = relativePath;
		}

		public File findFile(String file) {
			for (File dir : dirs) {
				File absoluteFile = new File(dir, file);
				if (absoluteFile.exists())
					return absoluteFile;
			}
			return null;
		}
		public URL findURL(String href) {
			File file = findFile(href);
			if (file!=null) {
				try {
						return file.toURI().toURL();
					}
				catch (MalformedURLException e) {
				}
			}
			return null;
		}
		public void addDirectory(File directory) {
			dirs.add(directory);
		}
	}

	class IndexerPluginVersionInfo extends PluginVersionInfo {
		private static final long serialVersionUID = 1L;

		public IndexerPluginVersionInfo(PluginIdentifier id,
				PluginIdentifier fid, File dir) {
			super(SearchIndex.INDEXED_CONTRIBUTION_INFO_FILE, null, dir, false);
			createInfo(id, fid);
		}

		@Override
		protected void createTable(Collection<String> docBundleIds) {
			// do nothing
		}

		protected void createInfo(PluginIdentifier id, PluginIdentifier fid) {
			// We will ignore docBundleIds which is null anyway,
			// and use id and fid to create plugin info
			// for the destination
			StringBuilder buffer = new StringBuilder();
			appendBundleInformation(buffer, id.id, id.version.toString());
			if (fid != null)
				appendBundleInformation(buffer, fid.id, fid.version.toString());

			this.put(id.id, buffer.toString());
		}
	}

	static class TocFile {
		String href;
		boolean primary;
		String extraDir;

		public TocFile(String href, boolean primary, String extraDir) {
			this.href = href;
			this.primary = primary;
			this.extraDir = extraDir;
		}
	}

	/**
	 * Creates a new instance of the help index builder.
	 */
	public HelpIndexBuilder() {
	}

	/**
	 * Returns the manifest file for this builder. If the target is a plug-in,
	 * it is the absolute path of <code>plugin.xml</code> file. If the target
	 * is a fragment, it is the absolute path of <code>plugin.xml</code> of
	 * the fragment plug-in.
	 *
	 * @return the file that contains TOC extensions
	 */
	public File getManifest() {
		return manifest;
	}

	/**
	 * Sets the new manifest file (plugin.xml) to be used by this builder. If
	 * the target is a plug-in, it is the absolute path of
	 * <code>plugin.xml</code> file. If the target is a fragment, it is the
	 * absolute path of <code>plugin.xml</code> of the fragment plug-in.
	 *
	 * @param manifest
	 *            the file that contains TOC extensions
	 */
	public void setManifest(File manifest) {
		if (manifest.getName().equalsIgnoreCase("MANIFEST.MF")) { //$NON-NLS-1$
			File parent = manifest.getParentFile();
			if (parent.getName().equalsIgnoreCase("META-INF")) { //$NON-NLS-1$
				File project = parent.getParentFile();
				manifest = new File(project, "plugin.xml"); //$NON-NLS-1$
				if (!manifest.exists())
					manifest=null;
			}
		}
		this.manifest = manifest;
	}

	/**
	 * Returns the destination directory where index should be created.
	 *
	 * @return the destination index directory
	 */
	public File getDestination() {
		return destination;
	}

	/**
	 * Sets the destination directory where index should be created.
	 * Locale-specific directories will be created starting from this directory.
	 *
	 * @param destination
	 *            the directory where index should be created
	 */
	public void setDestination(File destination) {
		this.destination = destination;
	}

	/**
	 * Creates the plug-in search index by parsing the provided plugin.xml file,
	 * looking for TOC extensions. If at least one of them has
	 * <code>index</code> element, all topics listed in all the TOCs in the
	 * plug-in will be indexed and stored in the path specified by the
	 * <code>index</code> element.
	 *
	 * @param monitor
	 *            the monitor to track index creation progress
	 * @throws CoreException
	 *             if there are problems during index creation.
	 */

	public void execute(IProgressMonitor monitor) throws CoreException {
		reset();
		if (manifest == null || destination == null)
			return;
		Document doc = readXMLFile(manifest);
		if (doc == null)
			return;

		PluginIdentifier pid = getPluginID(manifest.getParentFile(), doc);
		PluginIdentifier fid = null;

		if (!manifest.getParentFile().equals(destination)) {
			// target is a fragment, source is a plug-in
			File fragmentFile = new File(destination, "fragment.xml"); //$NON-NLS-1$
			Document fdoc=null;
			if (fragmentFile.exists())
				fdoc = readXMLFile(fragmentFile);
			fid = getPluginID(destination, fdoc);
			fdoc=null;
		}

		Element[] extensions = getTocExtensions(doc);
		for (int i = 0; i < extensions.length; i++) {
			processExtension(extensions[i]);
		}
		if (indexPath == null) {
			throwCoreException(HelpBaseResources.HelpIndexBuilder_noDestinationPath, null);
		}
		doc = null; // discard the DOM

		// compute the dir tree
		computeLocaleDirs(fid!=null);

		SubMonitor subMonitor = SubMonitor.convert(monitor, HelpBaseResources.HelpIndexBuilder_buildingIndex, localeDirs.size());
		MultiStatus multiStatus = null;

		for (LocaleDir localeDir : localeDirs) {
			MultiStatus localeStatus = processLocaleDir(pid, fid, localeDir, subMonitor.split(1));
			if (localeStatus != null) {
				if (multiStatus == null)
					multiStatus = localeStatus;
				else
					multiStatus.addAll(localeStatus);
			}
		}
		subMonitor.done();
		if (multiStatus != null)
			throw new CoreException(multiStatus);
	}

	/*
	 * Extracts TOCs and the index path from the extensions.
	 */
	private void processExtension(Element extensionNode) {
		NodeList children = extensionNode.getElementsByTagName(EL_TOC);
		for (int i = 0; i < children.getLength(); i++) {
			Node node = children.item(i);
			String file = getAttribute(node, "file"); //$NON-NLS-1$
			String primary = getAttribute(node, "primary"); //$NON-NLS-1$
			String extradir = getAttribute(node, "extradir"); //$NON-NLS-1$
			addTocFile(file, primary, extradir);
		}
		children = extensionNode.getElementsByTagName(EL_INDEX);
		if (children.getLength() == 1) {
			Node node = children.item(0);
			indexPath = getAttribute(node, "path"); //$NON-NLS-1$
		}
	}

	private void addTocFile(String file, String primary, String extradir) {
		boolean isPrimary = primary != null && primary.equalsIgnoreCase("true"); //$NON-NLS-1$
		tocFiles.add(new TocFile(file, isPrimary, extradir));
	}

	/*
	 * Computes the all os/*, ws/*, nl/language/ and
	 * nl/language/country/ locale dirs that contain files. We will
	 * produce an index for each one.
	 */
	private void computeLocaleDirs(boolean fragment) throws CoreException {
		if (!fragment) {
			LocaleDir dir = new LocaleDir(null, "/"); //$NON-NLS-1$
			dir.addDirectory(destination);
			localeDirs.add(dir);
		}
		File ws = new File(destination, "ws"); //$NON-NLS-1$
		computeSystem(ws, Platform.knownWSValues());
		File os = new File(destination, "os"); //$NON-NLS-1$
		computeSystem(os, Platform.knownOSValues());

		File nl = new File(destination, "nl"); //$NON-NLS-1$
		if (!nl.exists() || !nl.isDirectory())
			return;
		File [] languages = listFiles(nl);
		HashSet<String> locales = new HashSet<>();
		for (File language : languages) {
			if (!language.isDirectory())
				continue;
			if (!isValidLanguage(language.getName()))
				continue;
			File [] countries = listFiles(language);
			for (File country : countries) {
				String locale;
				boolean hasCountry = false;
				if (country.isDirectory() && isValidCountry(country.getName()))
					hasCountry = true;
				if (hasCountry)
					locale = language.getName()+"_"+country.getName(); //$NON-NLS-1$
				else
					locale = language.getName();
				if (isValidLocale(locale) && !locales.contains(locale)) {
					String relativePath;
					if (hasCountry)
						relativePath = "/nl/"+language.getName()+"/"+country.getName(); //$NON-NLS-1$ //$NON-NLS-2$
					else
						relativePath = "/nl/"+language.getName(); //$NON-NLS-1$
					LocaleDir dir = new LocaleDir(locale, relativePath);
					if (hasCountry)
						dir.addDirectory(country);
					dir.addDirectory(language);
					dir.addDirectory(destination);
					localeDirs.add(dir);
					locales.add(locale);
				}
			}
		}
	}

	private void computeSystem(File systemRoot, String [] values) throws CoreException {
		if (systemRoot.exists() && systemRoot.isDirectory()) {
			// check
			File [] files = listFiles(systemRoot);
			for (File sdir : files) {
				if (!sdir.isDirectory())
					continue;
				String sname = sdir.getName();
				for (String value : values) {
					if (value.equals(sname)) {
						// valid
						String relativePath="/"+systemRoot.getName()+"/"+sname; //$NON-NLS-1$ //$NON-NLS-2$
						LocaleDir dir = new LocaleDir(sname, relativePath);
						dir.addDirectory(sdir);
						dir.addDirectory(destination);
						localeDirs.add(dir);
						break;
					}
				}
			}
		}
	}

	/*
	 * Reject bogus directories.
	 */
	private boolean isValidLocale(String locale) {
		for (Locale legalLocale : legalLocales) {
			if (legalLocale.toString().equals(locale))
				return true;
		}
		return false;
	}

	private boolean isValidLanguage(String language) {
		if (legalLanguages==null) {
			legalLanguages = new HashSet<>();
			String [] choices = Locale.getISOLanguages();
			Collections.addAll(legalLanguages, choices);
		}
		return legalLanguages.contains(language);
	}

	private boolean isValidCountry(String country) {
		if (legalCountries==null) {
			legalCountries = new HashSet<>();
			String [] choices = Locale.getISOCountries();
			Collections.addAll(legalCountries, choices);
		}
		return legalCountries.contains(country);
	}

	/*
	 * Build an index for the locale directory by collecting
	 * documents according to the tocs, then building the index.
	 */

	private MultiStatus processLocaleDir(PluginIdentifier id, PluginIdentifier fid, LocaleDir localeDir,
			SubMonitor monitor) throws CoreException {
		File directory = localeDir.dirs.get(0);
		File indexDirectory = new File(directory, indexPath);
		prepareDirectory(indexDirectory);
		Collection<String> docs = collectDocs(localeDir);
		MultiStatus status = null;
		if (docs.size()>0) {
			String locale = localeDir.locale!=null?localeDir.locale:Platform.getNL();
			SearchIndex index = new SearchIndex(indexDirectory, locale,
				new AnalyzerDescriptor(locale), null, localeDir.relativePath);
			IndexerPluginVersionInfo docPlugins = new IndexerPluginVersionInfo(id,
				fid, indexDirectory);
			index.setDocPlugins(docPlugins);
			status = createIndex(id.id, fid != null, localeDir, index, docs, monitor);
			index.deleteLockFile();
		}
		return status;
	}

	/*
	 * Using TOC files found either in the fragment or in the plug-in,
	 * collect hrefs for the topics.
	 */

	private Collection<String> collectDocs(LocaleDir localeDir)
			throws CoreException {
		HashSet<String> docs = new HashSet<>();
		for (TocFile tocFile : tocFiles) {
			collectDocs(docs, getTocFile(localeDir, tocFile.href));
			if (tocFile.extraDir!=null) {
				//TODO also include all the indexable documents
				//in the extraDir
			}
		}
		return docs;
	}

	/*
	 * Try to find the actual file for the TOC href. Look in the
	 * locale dirs first (best match first). If not found,
	 * look in the plug-in itself (for fragments).
	 */

	private File getTocFile(LocaleDir localeDir, String href) {
		// try the locale dir
		File file = localeDir.findFile(href);
		if (file!=null)
			return file;
		// try the plug-in
		File pdir = manifest.getParentFile();
		return new File(pdir, href);
	}

	/*
	 * Collect hrefs starting from the 'toc' element.
	 */
	private void collectDocs(Set<String> docs, File tocFile)
			throws CoreException {
		if (!tocFile.exists())
			return;
		Document doc = readXMLFile(tocFile);
		add(doc.getDocumentElement(), docs);
	}

	/*
	 * Recursive collection of hrefs from topics.
	 */
	private void add(Element topic, Set<String> hrefs) {
		String href = getAttribute(topic, "href"); //$NON-NLS-1$
		if (topic.getTagName().equals("toc")) { //$NON-NLS-1$
			// toc element has 'topic' attribute. Don't ask why :-)
			href = getAttribute(topic, "topic"); //$NON-NLS-1$
		}
		if (href != null
				&& !href.isEmpty() && !href.startsWith("http://") && !href.startsWith("https://")) { //$NON-NLS-1$ //$NON-NLS-2$
			href = SearchIndex.getIndexableHref(href);
			if (href != null)
				hrefs.add(href);
		}
		NodeList subtopics = topic.getElementsByTagName("topic"); //$NON-NLS-1$
		for (int i = 0; i < subtopics.getLength(); i++) {
			Element subtopic = (Element) subtopics.item(i);
			href = getAttribute(subtopic, "href"); //$NON-NLS-1$
			if (href != null && !href.isEmpty() && !href.startsWith("http://") && !href.startsWith("https://")) { //$NON-NLS-1$ //$NON-NLS-2$
				href = SearchIndex.getIndexableHref(href);
				if (href != null)
					hrefs.add(href);
			}
		}
	}

	/*
	 * Creates index for the locale dir by iterating over the doc hrefs
	 * and adding them into the index. Documents that cannot be found
	 * will be ignored.
	 */

	private MultiStatus createIndex(String pluginId, boolean fragment, LocaleDir localeDir, SearchIndex index,
			Collection<String> addedDocs, SubMonitor monitor)
			throws CoreException {
		// build an index for each locale directory
		String taskName = NLS.bind(HelpBaseResources.HelpIndexBuilder_indexFor, localeDir.dirs.get(0).getName());
		monitor.beginTask(taskName + HelpBaseResources.UpdatingIndex, addedDocs.size());
		if (!index.beginAddBatch(true)) {
			throwCoreException(HelpBaseResources.HelpIndexBuilder_error, null);
		}
		checkCancelled(monitor);
		MultiStatus multiStatus = null;

		for (String href : addedDocs) {
			URL url = localeDir.findURL(href);
			if (url != null) {
				IStatus status = index
						.addDocument(getName(pluginId, href), url);
				if (status.getCode() != IStatus.OK) {
					if (multiStatus == null)
						multiStatus = createMultiStatus();
					multiStatus.add(status);
				}
			}
			else {
				// report missing document when in a plug-in
				String locale = localeDir.locale!=null?localeDir.locale:Platform.getNL();
				String message = NLS.bind(HelpBaseResources.HelpIndexBuilder_cannotFindDoc, locale, href);
				IStatus status = new Status(IStatus.WARNING, pluginId, IStatus.OK, message, null);
				if (multiStatus == null)
					multiStatus = createMultiStatus();
				multiStatus.add(status);
			}
			checkCancelled(monitor);
			monitor.worked(1);
		}
		monitor.subTask(taskName + HelpBaseResources.Writing_index);
		if (!index.endAddBatch(true, true)) {
			IStatus status = new Status(IStatus.ERROR, HelpBasePlugin.PLUGIN_ID,
					IStatus.OK, HelpBaseResources.HelpIndexBuilder_errorWriting, null);
			if (multiStatus==null)
				multiStatus = createMultiStatus();
			multiStatus.add(status);
		}
		return multiStatus;
	}

	private MultiStatus createMultiStatus() {
		return new MultiStatus(
				HelpBasePlugin.PLUGIN_ID,
				IStatus.OK,
				HelpBaseResources.HelpIndexBuilder_incompleteIndex,
				null);
	}

	private void checkCancelled(IProgressMonitor pm)
			throws OperationCanceledException {
		if (pm.isCanceled())
			throw new OperationCanceledException();
	}

	private String getName(String pluginId, String href) {
		// remove query string if any
		int i = href.indexOf('?');
		if (i != -1)
			href = href.substring(0, i);
		return "/" + pluginId + "/" + href; //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * If the path does not exist, create it. Otherwise, delete all the files in
	 * it.
	 *
	 * @param indexDirectory
	 */

	private void prepareDirectory(File indexDirectory) throws CoreException {
		if (indexDirectory.exists()) {
			File[] files = listFiles(indexDirectory);
			for (File file : files) {
				boolean result = file.delete();
				if (!result)
					throwCoreException(
							HelpBaseResources.HelpIndexBuilder_cannotScrub, null);
			}
		} else {
			boolean result = indexDirectory.mkdirs();
			if (!result)
				throwCoreException(HelpBaseResources.HelpIndexBuilder_cannotCreateDest,
						null);
		}
	}

	private void reset() {
		localeDirs.clear();
		tocFiles.clear();
		indexPath = null;
	}

	private PluginIdentifier getPluginID(File dir, Document doc) throws CoreException {
		String id = null;
		String version = null;
		if (doc != null) {
			Node root = doc.getDocumentElement();
			id = getAttribute(root, "id"); //$NON-NLS-1$
			version = getAttribute(root, "version"); //$NON-NLS-1$
			if (id != null && version != null)
				return new PluginIdentifier(id, version);
		}
		// check for the OSGi manifest
		File OSGiFile = new File(dir,
				"META-INF/MANIFEST.MF"); //$NON-NLS-1$

		if (OSGiFile.exists()) {
			try (FileInputStream fis = new FileInputStream(OSGiFile)) {
				Manifest OSGiManifest = new Manifest(fis);
				Properties headers = manifestToProperties(OSGiManifest
						.getMainAttributes());
				String value = headers.get(Constants.BUNDLE_SYMBOLICNAME)
						.toString();
				if (value == null)
					return null;
				ManifestElement[] elements = ManifestElement.parseHeader(
						Constants.BUNDLE_SYMBOLICNAME, value);
				if (elements.length > 0)
					id = elements[0].getValue();
				value = headers.get(Constants.BUNDLE_VERSION).toString();
				if (value == null)
					return null;
				elements = ManifestElement.parseHeader(
						Constants.BUNDLE_VERSION, value);
				if (elements.length > 0)
					version = elements[0].getValue();
				if (id != null && version != null)
					return new PluginIdentifier(id, version);
			} catch (Exception e1) {
				throwCoreException(HelpBaseResources.HelpIndexBuilder_errorExtractingId, e1);
			}
		}
		return null;
	}

	private String getAttribute(Node node, String name) {
		NamedNodeMap atts = node.getAttributes();
		if (atts != null) {
			Node att = atts.getNamedItem(name);
			if (att != null)
				return att.getNodeValue();
		}
		return null;
	}

	private Document readXMLFile(File file) throws CoreException {
		Document d = null;
		try (InputStream stream = new FileInputStream(file);
				InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
			InputSource inputSource = new InputSource(reader);
			inputSource.setSystemId(manifest.toString());
			d = LocalEntityResolver.parse(inputSource);
		} catch (Exception e) {
			String message = NLS.bind(HelpBaseResources.HelpIndexBuilder_errorParsing, file.getName());
			throwCoreException(message, e);
		}
		return d;
	}

	private Element[] getTocExtensions(Document doc) {
		ArrayList<Node> list = new ArrayList<>();
		//Node root = doc.getDocumentElement();
		NodeList children = doc.getElementsByTagName("extension"); //$NON-NLS-1$
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			String point = getAttribute(child, "point"); //$NON-NLS-1$
			if (point.equals(POINT_TOC))
				list.add(child);
		}
		return list.toArray(new Element[list.size()]);
	}

	private Properties manifestToProperties(Attributes d) {
		Iterator<Object> iter = d.keySet().iterator();
		Properties result = new Properties();
		while (iter.hasNext()) {
			Attributes.Name key = (Attributes.Name) iter.next();
			result.put(key.toString(), d.get(key));
		}
		return result;
	}

	private void throwCoreException(String message, Throwable t)
			throws CoreException {
		IStatus status = new Status(IStatus.ERROR, HelpBasePlugin.PLUGIN_ID,
				IStatus.OK, message, t);
		throw new CoreException(status);
	}

	private File[] listFiles(File file) throws CoreException {
		File[] fileList = file.listFiles();
		if(fileList == null) {
			throwCoreException("Content from directory '" + file.getAbsolutePath() + "' can not be listed.", null); //$NON-NLS-1$ //$NON-NLS-2$
		}
		return fileList;
	}
}
