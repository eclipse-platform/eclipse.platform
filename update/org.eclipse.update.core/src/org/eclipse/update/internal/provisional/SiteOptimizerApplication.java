/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Chris Aniszczyk (IBM Corp.) - NL-enabled the site optimizer
 *******************************************************************************/
package org.eclipse.update.internal.provisional;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPlatformRunnable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.eclipse.update.core.IIncludedFeatureReference;
import org.eclipse.update.core.IncludedFeatureReference;
import org.eclipse.update.core.model.DefaultSiteParser;
import org.eclipse.update.core.model.FeatureModel;
import org.eclipse.update.core.model.FeatureModelFactory;
import org.eclipse.update.core.model.FeatureReferenceModel;
import org.eclipse.update.core.model.ImportModel;
import org.eclipse.update.core.model.PluginEntryModel;
import org.eclipse.update.core.model.SiteModel;
import org.eclipse.update.internal.core.ExtendedSiteURLFactory;
import org.eclipse.update.internal.core.Messages;
import org.eclipse.update.internal.core.UpdateManagerUtils;
import org.eclipse.update.internal.jarprocessor.JarProcessor;
import org.eclipse.update.internal.jarprocessor.Main;
import org.xml.sax.SAXException;

/**
 * The application class used to perform update site optimizations.
 * <p>
 * This class can only be referenced from <code>org.eclipse.runtime.applications</code>
 * extension point. It should not be exteded or instantiated.
 * <p>
 * <b>Note:</b> This class/interface is part of an interim API that is still
 * under development and expected to change significantly before reaching
 * stability. It is being made available at this early stage to solicit feedback
 * from pioneering adopters on the understanding that any code that uses this
 * API will almost certainly be broken (repeatedly) as the API evolves.
 * </p>
 * 
 * @since 3.2
 */
public class SiteOptimizerApplication implements IPlatformRunnable {
	public final static Integer EXIT_ERROR = new Integer(1);

	public final static String JAR_PROCESSOR = "-jarProcessor"; //$NON-NLS-1$

	public final static String DIGEST_BUILDER = "-digestBuilder"; //$NON-NLS-1$

	public final static String INPUT = "input"; //$NON-NLS-1$

	public final static String OUTPUT_DIR = "-outputDir"; //$NON-NLS-1$

	public final static String VERBOSE = "-verbose"; //$NON-NLS-1$

	public final static String JAR_PROCESSOR_PACK = "-pack"; //$NON-NLS-1$

	public final static String JAR_PROCESSOR_UNPACK = "-unpack"; //$NON-NLS-1$

	public final static String JAR_PROCESSOR_REPACK = "-repack"; //$NON-NLS-1$

	public final static String JAR_PROCESSOR_SIGN = "-sign"; //$NON-NLS-1$
	
	public final static String JAR_PROCESSOR_PROCESS_ALL = "-processAll"; //$NON-NLS-1$

	public final static String SITE_XML = "-siteXML"; //$NON-NLS-1$

	public final static String SITE_ATTRIBUTES_FILE = "siteAttributes.txt"; //$NON-NLS-1$

	public final static String DIGEST_OUTPUT_DIR = "-digestOutputDir"; //$NON-NLS-1$

	/*
	 * private final static String DESCRIPTION = "DESCRIPTION"; private final
	 * static String LICENCE = "LICENCE"; private final static String COPYRIGHT =
	 * "COPYRIGHT"; private final static String FEATURE_LABEL = "FEATURE_LABEL";
	 */

	/**
	 * Parses the command line in the form: [-key [value]]* [inputvalue] If the
	 * last argument does not start with a "-" then it is taken as the input
	 * value and not the value for a preceding -key
	 * 
	 * @param args
	 * @return
	 */
	private Map parseCmdLine(String[] args) {
		Map cmds = new HashMap();
		for (int i = 0; i < args.length; i++) {
			if (i == args.length - 1 && !args[i].startsWith("-")) { //$NON-NLS-1$
				cmds.put(INPUT, args[i]);
			} else {
				String key = args[i];
				String val = null;
				if (i < args.length - 2 && !args[i + 1].startsWith("-")) { //$NON-NLS-1$
					val = args[++i];
				}

				if (key.startsWith(SITE_XML)) {
					// System.out.println(val.indexOf(":null"));
					val = key.substring(key.indexOf("=") + 1); //$NON-NLS-1$
					// System.out.println(key + ":" + val);
					cmds.put(SITE_XML, val);
				} else if (key.startsWith(DIGEST_OUTPUT_DIR)) {
					val = key.substring(key.indexOf("=") + 1); //$NON-NLS-1$
					// System.out.println(key + ":" + val);
					cmds.put(DIGEST_OUTPUT_DIR, val);
				} else {

					// System.out.println(key + ":" + val);
					cmds.put(key, val);
				}
			}
		}
		return cmds;
	}

	private boolean runJarProcessor(Map params) {
		Main.Options options = new Main.Options();
		options.pack = params.containsKey(JAR_PROCESSOR_PACK);
		options.unpack = params.containsKey(JAR_PROCESSOR_UNPACK);
		options.repack = params.containsKey(JAR_PROCESSOR_REPACK);
		options.processAll = params.containsKey(JAR_PROCESSOR_PROCESS_ALL);
		options.verbose = params.containsKey(VERBOSE);
		options.signCommand = (String) params.get(JAR_PROCESSOR_SIGN);
		options.outputDir = (String) params.get(OUTPUT_DIR);

		String problem = null;

		String input = (String) params.get(INPUT);
		if (input == null)
			problem = Messages.SiteOptimizer_inputNotSpecified;
		else {
			File inputFile = new File(input);
			if (inputFile.exists())
				options.input = inputFile;
			else
				problem = NLS.bind(Messages.SiteOptimizer_inputFileNotFound,
						new String[] { input });
		}

		if (options.unpack) {
			if (!JarProcessor.canPerformUnpack()) {
				problem = Messages.JarProcessor_unpackNotFound;
			} else if (options.pack || options.repack
					|| options.signCommand != null) {
				problem = Messages.JarProcessor_noPackUnpack;
			}
		} else if ((options.pack || options.repack)
				&& !JarProcessor.canPerformPack()) {
			problem = Messages.JarProcessor_packNotFound;
		}

		if (problem != null) {
			System.out.println(problem);
			return false;
		}

		Main.runJarProcessor(options);
		return true;
	}

	private boolean runDigestBuilder(Map params) {

		List featureList = getFeatureList(params);

		if ((featureList == null) || featureList.isEmpty()) {
			System.out.println("no features to process"); //$NON-NLS-1$
			return false;
		}
		Map perFeatureLocales = new HashMap();
		Map availableLocales = getAvailableLocales(featureList,
				perFeatureLocales);
		try {
			openInputStremas(availableLocales);
		} catch (IOException e1) {
			e1.printStackTrace();
			System.out.println("Can not create file in output direcotry"); //$NON-NLS-1$
			return false;
		}

		for(int i = 0; i < featureList.size(); i++) {
			
			String featureJarFileName = (String) featureList.get(i);

			if (featureJarFileName.endsWith("jar")) { //$NON-NLS-1$
				System.out.println("Processing... " + featureJarFileName); //$NON-NLS-1$
			} else {
				System.out.println("Skipping... " + featureJarFileName); //$NON-NLS-1$
				continue;
			}

			JarFile featureJar = null;
			try {
				featureJar = new JarFile(featureJarFileName);
			} catch (IOException e) {
				System.out.println("Problem with openning jar: " //$NON-NLS-1$
						+ featureJarFileName);
				e.printStackTrace();
				return false;
			}
			FeatureModelFactory fmf = new FeatureModelFactory();

			try {
				ZipEntry featureXMLEntry = featureJar.getEntry("feature.xml"); //$NON-NLS-1$
				Map featureProperties = loadProperties(featureJar,
						featureJarFileName, perFeatureLocales);

				FeatureModel featureModel = fmf.parseFeature(featureJar
						.getInputStream(featureXMLEntry));
				
				featureList = addFeaturesToList( (String) params.get(SITE_XML), featureList, featureModel.getFeatureIncluded(), availableLocales, perFeatureLocales);

				Iterator availableLocalesIterator = availableLocales.values()
						.iterator();
				while (availableLocalesIterator.hasNext()) {
					((AvailableLocale) availableLocalesIterator.next())
							.writeFeatureDigests(featureModel,
									featureProperties);
				}

			} catch (SAXException e) {
				e.printStackTrace();
				return false;
			} catch (IOException e) {
				e.printStackTrace();
				return false;
			} catch (CoreException e) {
				e.printStackTrace();
				return false;
			}
		}
		Iterator availableLocalesIterator = availableLocales.values()
				.iterator();
		String outputDirectory = (String) params.get(DIGEST_OUTPUT_DIR);

		outputDirectory = outputDirectory.substring(outputDirectory
				.indexOf("=") + 1); //$NON-NLS-1$
		if (!outputDirectory.endsWith(File.separator)) {
			outputDirectory = outputDirectory + File.separator;
		}
		while (availableLocalesIterator.hasNext()) {
			try {
				((AvailableLocale) availableLocalesIterator.next())
						.finishDigest(outputDirectory);
			} catch (IOException e) {
				System.out.println("Can not write in digest output directory: " //$NON-NLS-1$
						+ outputDirectory);
				e.printStackTrace();
				return false;
			}
		}
		System.out.println("Done"); //$NON-NLS-1$
		return true;
	}

	private List addFeaturesToList( String siteXML, List featureList, IIncludedFeatureReference[] iIncludedFeatureReferences, Map availableLocales, Map perFeatureLocales ) throws CoreException {
		
		String directoryName = (new File(siteXML)).getParent();
		if (!directoryName.endsWith(File.separator)) {
			directoryName = directoryName + File.separator;
		}
		directoryName = directoryName + "features" + File.separator; //$NON-NLS-1$
		
		for (int i = 0; i < iIncludedFeatureReferences.length; i++) {
			String featureURL = directoryName + iIncludedFeatureReferences[i].getVersionedIdentifier() + ".jar"; //$NON-NLS-1$
			if (!(isFeatureAlreadyInList(featureList, featureURL))) {
				try {
					System.out.println("Extracting locales from inlcuded feature " + featureURL); //$NON-NLS-1$
					processLocalesInJar(availableLocales, featureURL, perFeatureLocales, true);
				} catch (IOException e) {
					if (iIncludedFeatureReferences[i].isOptional()) 
						continue;
					System.out.println("Error while extracting locales from inlcuded feature " + featureURL);//$NON-NLS-1$	
					e.printStackTrace();
					throw new CoreException( new Status( IStatus.ERROR, "", IStatus.OK, "Error while extracting locales from inlcuded feature " + featureURL, e)); //$NON-NLS-1$ //$NON-NLS-2$
				}
				featureList.add(featureURL);
			}
		}
		
		return featureList;
	}

	private boolean isFeatureAlreadyInList(List featureList, String featureURL) {
		for (int i = 0; i < featureList.size(); i++) {
			String currentFeatureURL = (String)featureList.get(i);
			if (currentFeatureURL.equals(featureURL)) {
				return true;
			}
		}
		return false;
	}

	private Map loadProperties(JarFile featureJar, String featureJarFileName,
			Map perFeatureLocales) {
		// System.out.println(
		// ((List)perFeatureLocales.get(featureJarFileName)).size());
		Iterator it = ((List) perFeatureLocales.get(featureJarFileName))
				.iterator();
		Map result = new HashMap();
		while (it.hasNext()) {
			String propertyFileName = (String) it.next();

			ZipEntry featurePropertiesEntry = featureJar
					.getEntry(propertyFileName);
			Properties featureProperties = new Properties();
			if (featurePropertiesEntry != null) {
				try {
					featureProperties.load(featureJar
							.getInputStream(featurePropertiesEntry));
					String localeString = null;
					if (propertyFileName.endsWith("feature.properties")) { //$NON-NLS-1$
						localeString = ""; //$NON-NLS-1$
					} else {
						localeString = propertyFileName.substring(8,
								propertyFileName.indexOf('.'));
					}
					result.put(localeString, featureProperties);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return result;
	}

	private void openInputStremas(Map availableLocales) throws IOException {
		Iterator locales = availableLocales.values().iterator();
		while (locales.hasNext()) {
			AvailableLocale availableLocale = (AvailableLocale) locales.next();
			availableLocale.openLocalizedOutputStream();
		}
	}

	private Map getAvailableLocales(List featureList, Map perFeatureLocales) {
		Iterator features = featureList.iterator();
		Map locales = new HashMap();
		while (features.hasNext()) {
			String feature = (String) features.next();
			try {
				System.out.println("Extracting locales from " + feature); //$NON-NLS-1$
				processLocalesInJar(locales, feature, perFeatureLocales, false);
			} catch (IOException e) {
				System.out.println("Error while extracting locales from " //$NON-NLS-1$
						+ feature);
				e.printStackTrace();
				return null;
			}
		}
		return locales;
	}

	private void processLocalesInJar(Map locales, String feature,
			Map perFeatureLocales, boolean ignoreNewLocales) throws IOException {

		JarFile jar = new JarFile(feature);
		// System.out.println(feature);
		Enumeration files = jar.entries();

		List localesTemp = new ArrayList();
		perFeatureLocales.put(feature, localesTemp);

		while (files.hasMoreElements()) {
			ZipEntry file = (ZipEntry) files.nextElement();
			String localeString = null;
			String name = file.getName();
			// System.out.println("processLocalesInJar:"+name);
			if (name.startsWith("feature") && name.endsWith(".properties")) { //$NON-NLS-1$ //$NON-NLS-2$
				// System.out.println(name);
				localesTemp.add(name);
				// System.out.println(name);
				if (name.endsWith("feature.properties")) { //$NON-NLS-1$
					localeString = ""; //$NON-NLS-1$
				} else {
					localeString = name.substring(8, name.indexOf('.'));
				}
				// System.out.println(name +"::::\"" + localeString + "\"");
				if ( !ignoreNewLocales && !locales.containsKey(localeString)) {
					locales.put(localeString, new AvailableLocale(localeString));
				}
				if (locales.containsKey(localeString)) {
					AvailableLocale currentLocale = (AvailableLocale) locales.get(localeString);
					currentLocale.addFeatures(feature);
				}
			}
		}

	}

	private List getFeatureList(Map params) {
		if (params.containsKey(SITE_XML)
				&& (fileExists((String) params.get(SITE_XML)))) {
			return getFeatureListFromSiteXML((String) params.get(SITE_XML));
		} else if (params.containsKey(INPUT)
				&& isDirectory((String) params
						.get(SiteOptimizerApplication.INPUT))) {
			return getFeatureListFromDirectory((String) params.get(INPUT));
		}
		return null;
	}

	private boolean fileExists(String fileName) {
		// System.out.println("fileExists:"+fileName);
		File file = new File(fileName);
		if ((file != null) && file.exists())
			return true;
		return false;
	}

	private List getFeatureListFromDirectory(String directoryName) {
		List featuresURLs = new ArrayList();
		File directory = new File(directoryName);
		String[] featureJarFileNames = directory.list();
		for (int i = 0; i < featureJarFileNames.length; i++) {
			featuresURLs.add(directoryName + File.separator
					+ featureJarFileNames[i]);
		}
		return featuresURLs;
	}

	private boolean isDirectory(String fileName) {

		File directory = new File(fileName);
		if ((directory != null) && directory.exists()
				&& directory.isDirectory())
			return true;
		return false;
	}

	private List getFeatureListFromSiteXML(String siteXML) {

		List featuresURLs = new ArrayList();
		String directoryName = (new File(siteXML)).getParent();
		if (!directoryName.endsWith(File.separator)) {
			directoryName = directoryName + File.separator;
		}

		DefaultSiteParser siteParser = new DefaultSiteParser();
		siteParser.init(new ExtendedSiteURLFactory());

		try {
			SiteModel site = siteParser.parse(new FileInputStream(siteXML));
			site.getFeatureReferenceModels()[1].getURLString();
			FeatureReferenceModel[] featureReferenceModel = site
					.getFeatureReferenceModels();
			// System.out.println("featureReferenceModel# =" +
			// featureReferenceModel.length);
			for (int i = 0; i < featureReferenceModel.length; i++) {
				featuresURLs.add(directoryName
						+ featureReferenceModel[i].getURLString());
			}
			// System.out.println("featureReferenceModel# =" +
			// featuresURLs.size());
			return featuresURLs;
		} catch (FileNotFoundException e) {
			System.out.println("File not found: " + e.getMessage()); //$NON-NLS-1$
			e.printStackTrace();
		} catch (SAXException e) {
			System.out.println("Parsing problem: " + e.getMessage()); //$NON-NLS-1$
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println("Problem while parsing: " + e.getMessage()); //$NON-NLS-1$
			e.printStackTrace();
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.runtime.IPlatformRunnable#run(java.lang.Object)
	 */
	public Object run(Object args) throws Exception {
		Platform.endSplash();
		if (args == null)
			return EXIT_ERROR;
		if (args instanceof String[]) {
			Map params = parseCmdLine((String[]) args);
			if (params.containsKey(JAR_PROCESSOR)) {
				if (!runJarProcessor(params))
					return EXIT_ERROR;
			}

			if (params.containsKey(DIGEST_BUILDER)) {
				if (!runDigestBuilder(params))
					return EXIT_ERROR;
			}
		}
		return IPlatformRunnable.EXIT_OK;
	}

	private class AvailableLocale {

		private String PREFIX = "temp"; //$NON-NLS-1$

		private String locale;

		private Map /* VersionedIdentifier */features = new HashMap();

		private PrintStream localizedPrintStream;

		private File tempDigestDirectory;

		public Map availableLocales;

		public Map getAvailableLocales() {
			return availableLocales;
		}

		public void finishDigest(String outputDirectory) throws IOException {
			localizedPrintStream.println("</digest>"); //$NON-NLS-1$
			if (localizedPrintStream != null) {
				localizedPrintStream.close();
			}
			
			File digest = new File(outputDirectory + File.separator + "digest" //$NON-NLS-1$
					+ (locale == null || locale.equals("") ? "" : "_"+locale) + ".zip"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			System.out.println(digest.getAbsolutePath());
			System.out.println(digest.getName());
			if (digest.exists()) {
				digest.delete();
			}
			digest.createNewFile();
			OutputStream os = new FileOutputStream(digest);
			JarOutputStream jos = new JarOutputStream(os);
			jos.putNextEntry(new ZipEntry("digest.xml")); //$NON-NLS-1$
			InputStream is = new FileInputStream(tempDigestDirectory);
			byte[] b = new byte[4096];
			int bytesRead = 0;
			do {
				bytesRead = is.read(b);
				if (bytesRead > 0) {
					jos.write(b, 0, bytesRead);
				}
			} while (bytesRead > 0);

			jos.closeEntry();
			jos.close();
			os.close();
			is.close();
			tempDigestDirectory.delete();

		}

		public void setAvailableLocales(Map availableLocales) {
			this.availableLocales = availableLocales;
		}

		public AvailableLocale(String locale) {
			this.locale = locale;
		}

		public Map getFeatures() {
			return features;
		}

		public void addFeatures(String feature) {
			features.put(feature, feature);
		}

		public String getLocale() {
			return locale;
		}

		public PrintStream getLocalizedPrintStream() {
			return localizedPrintStream;
		}

		public void openLocalizedOutputStream() throws IOException {
			tempDigestDirectory = File.createTempFile(PREFIX, null);
			FileOutputStream fstream = new FileOutputStream(tempDigestDirectory);
			localizedPrintStream = new PrintStream(fstream);
			localizedPrintStream
					.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n <digest>"); //$NON-NLS-1$
			tempDigestDirectory.deleteOnExit();
		}

		public int hashCode() {
			return locale.hashCode();
		}

		public boolean equals(Object obj) {

			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			final AvailableLocale other = (AvailableLocale) obj;
			if (locale == null) {
				if (other.locale != null)
					return false;
			} else if (!locale.equals(other.locale))
				return false;
			return true;
		}

		public void writeFeatureDigests(FeatureModel featureModel,
				Map featureProperties) {

			if (this.locale.equals("")) { //$NON-NLS-1$
				writeFeatureDigest(localizedPrintStream, featureModel,
						(Properties) featureProperties.get("")); //$NON-NLS-1$
				return;
			}
			Properties temp = new Properties();
			if (locale.indexOf("_") < 0) { //$NON-NLS-1$
				temp = combineProperties(
						(Properties) featureProperties.get(""), //$NON-NLS-1$
						(Properties) featureProperties.get(locale), temp);
				writeFeatureDigest(localizedPrintStream, featureModel, temp);
			} else {
				temp = combineProperties((Properties) featureProperties
						.get(locale.substring(locale.indexOf("_") + 1)), //$NON-NLS-1$
						(Properties) featureProperties.get(locale), temp);
				writeFeatureDigest(localizedPrintStream, featureModel, temp);
			}

		}

		private Properties combineProperties(Properties properties,
				Properties properties2, Properties properties3) {
			return new CombinedProperties(properties3, properties2, properties);

		}

	}

	public static void writeFeatureDigest(PrintStream digest,
			FeatureModel featureModel, Properties featureProperties) {

		String label = null;
		String provider = null;
		String description = null;
		String license = null;
		String copyright = null;

		if ((featureProperties != null)
				&& featureModel.getLabel().startsWith("%")) { //$NON-NLS-1$
			label = featureProperties.getProperty(featureModel.getLabel()
					.substring(1));
		} else {
			label = featureModel.getLabel();
		}
		if ((featureProperties != null)
				&& (featureModel.getDescriptionModel() != null)
				&& featureModel.getDescriptionModel().getAnnotation()
						.startsWith("%")) { //$NON-NLS-1$
			// System.out.println(featureProperties.getProperty(featureModel.getDescriptionModel().getAnnotation().substring(1)));
			description = featureProperties.getProperty(featureModel
					.getDescriptionModel().getAnnotation().substring(1));
		} else {
			description = featureModel.getDescriptionModel().getAnnotation();
		}
		if ((featureProperties != null)
				&& featureModel.getProvider().startsWith("%")) { //$NON-NLS-1$
			provider = featureProperties.getProperty(featureModel.getProvider()
					.substring(1));
		} else {
			provider = featureModel.getProvider();
		}

		if (((featureProperties != null) && featureModel.getCopyrightModel() != null)
				&& featureModel.getCopyrightModel().getAnnotation().startsWith(
						"%")) { //$NON-NLS-1$
			copyright = featureProperties.getProperty(featureModel
					.getCopyrightModel().getAnnotation().substring(1));
		} else {
			if (featureModel.getCopyrightModel() != null) {
				copyright = featureModel.getCopyrightModel().getAnnotation();
			} else {
				copyright = null;
			}
		}

		if ((featureProperties != null)
				&& (featureModel.getLicenseModel() != null)
				&& featureModel.getLicenseModel().getAnnotation().startsWith(
						"%")) { //$NON-NLS-1$
			license = featureProperties.getProperty(featureModel
					.getLicenseModel().getAnnotation().substring(1));
		} else {
			license = featureModel.getLicenseModel().getAnnotation();
		}

		digest.print("<feature "); //$NON-NLS-1$
		digest.print("label=\"" + label + "\" ");  //$NON-NLS-1$//$NON-NLS-2$
		digest.print("provider-name=\"" + provider + "\" "); //$NON-NLS-1$ //$NON-NLS-2$
		digest.print("id=\"" + featureModel.getFeatureIdentifier() + "\" ");  //$NON-NLS-1$//$NON-NLS-2$
		digest.print("version=\"" + featureModel.getFeatureVersion() + "\" "); //$NON-NLS-1$ //$NON-NLS-2$
		if (featureModel.getOS() != null)
			digest.print("os=\"" + featureModel.getOS() + "\" ");  //$NON-NLS-1$//$NON-NLS-2$
		if (featureModel.getNL() != null)
			digest.print("nl=\"" + featureModel.getNL() + "\" ");  //$NON-NLS-1$//$NON-NLS-2$
		if (featureModel.getWS() != null)
			digest.print("ws=\"" + featureModel.getWS() + "\" "); //$NON-NLS-1$ //$NON-NLS-2$
		if (featureModel.getOSArch() != null)
			digest.print("arch=\"" + featureModel.getOSArch() + "\" ");  //$NON-NLS-1$//$NON-NLS-2$
		if (featureModel.isExclusive())
			digest.print("exclusive=\"" + featureModel.isExclusive() + "\" ");  //$NON-NLS-1$//$NON-NLS-2$

		if (((featureModel.getImportModels() == null) || (featureModel
				.getImportModels().length == 0))
				&& ((featureModel.getDescriptionModel() == null)
						|| (featureModel.getDescriptionModel().getAnnotation() == null) || (featureModel
						.getDescriptionModel().getAnnotation().trim().length() == 0))
				&& ((featureModel.getCopyrightModel() == null)
						|| (featureModel.getCopyrightModel().getAnnotation() == null) || (featureModel
						.getCopyrightModel().getAnnotation().trim().length() == 0))
				&& ((featureModel.getLicenseModel() == null)
						|| (featureModel.getLicenseModel().getAnnotation() == null) || (featureModel
						.getLicenseModel().getAnnotation().trim().length() == 0)) 
				&& ((featureModel.getFeatureIncluded() == null) || (featureModel
						.getFeatureIncluded().length == 0))){
			digest.println("/> "); //$NON-NLS-1$
		} else {
			digest.println("> "); //$NON-NLS-1$
			if (featureModel.getImportModels().length > 0) {

				digest.println("\t<requires> "); //$NON-NLS-1$
				ImportModel[] imports = featureModel.getImportModels();
				for (int j = 0; j < imports.length; j++) {
					digest.print("\t\t<import "); //$NON-NLS-1$
					if (imports[j].isFeatureImport()) {
						digest.print("feature=\""); //$NON-NLS-1$
					} else {
						digest.print("plugin=\""); //$NON-NLS-1$
					}
					digest.print(imports[j].getIdentifier() + "\" "); //$NON-NLS-1$
					digest.print("version=\""); //$NON-NLS-1$
					digest.print(imports[j].getVersion() + "\" "); //$NON-NLS-1$
					digest.print("match=\""); //$NON-NLS-1$
					digest.print(imports[j].getMatchingRuleName() + "\" "); //$NON-NLS-1$
					if (imports[j].isPatch()) {
						digest.print("patch=\"true\" "); //$NON-NLS-1$
					}
					digest.println(" />"); //$NON-NLS-1$
				}

				digest.println("\t</requires>"); //$NON-NLS-1$

			}

			if ((featureModel.getDescriptionModel() != null)
					&& (featureModel.getDescriptionModel().getAnnotation() != null)
					&& (featureModel.getDescriptionModel().getAnnotation()
							.trim().length() != 0)) {
				digest.println("\t<description>"); //$NON-NLS-1$
				digest.println("\t\t" + UpdateManagerUtils.getWritableXMLString(description)); //$NON-NLS-1$
				digest.println("\t</description>"); //$NON-NLS-1$
			}

			if (featureModel.getCopyrightModel() != null) {
				if (featureModel.getCopyrightModel().getAnnotation() != null) {
					// if
					// (featureModel.getDescriptionModel().getAnnotation().length()
					// != 0) {
					digest.println("\t<copyright>"); //$NON-NLS-1$
					digest.println("\t\t" + UpdateManagerUtils.getWritableXMLString(copyright)); //$NON-NLS-1$
					digest.println("\t</copyright>"); //$NON-NLS-1$
					// }
				}
			}

			if ((featureModel.getLicenseModel() != null)
					&& (featureModel.getLicenseModel().getAnnotation() != null)
					&& (featureModel.getDescriptionModel().getAnnotation()
							.trim().length() != 0)) {
				digest.println("\t<license>"); //$NON-NLS-1$
				digest.println("\t\t" + UpdateManagerUtils.getWritableXMLString(license)); //$NON-NLS-1$
				digest.println("\t</license>"); //$NON-NLS-1$
			}

			PluginEntryModel[] plugins = featureModel.getPluginEntryModels();
			if ((plugins != null) && (plugins.length != 0)) {
				for (int i = 0; i < plugins.length; i++) {
					digest.print("\t<plugin "); //$NON-NLS-1$
					digest.print("id=\"" + plugins[i].getPluginIdentifier() //$NON-NLS-1$
							+ "\" "); //$NON-NLS-1$
					digest.print("version=\"" + plugins[i].getPluginVersion() //$NON-NLS-1$
							+ "\" "); //$NON-NLS-1$
					if (plugins[i].getOS() != null)
						digest.print("os=\"" + plugins[i].getOS() + "\" ");  //$NON-NLS-1$//$NON-NLS-2$
					if (plugins[i].getNL() != null)
						digest.print("nl=\"" + plugins[i].getNL() + "\" "); //$NON-NLS-1$ //$NON-NLS-2$
					if (plugins[i].getWS() != null)
						digest.print("ws=\"" + plugins[i].getWS() + "\" ");  //$NON-NLS-1$//$NON-NLS-2$
					if (plugins[i].getOSArch() != null)
						digest
								.print("arch=\"" + plugins[i].getOSArch() //$NON-NLS-1$
										+ "\" "); //$NON-NLS-1$
					if (plugins[i].getDownloadSize() > 0)
						digest.print("download-size=\"" //$NON-NLS-1$
								+ plugins[i].getDownloadSize() + "\" "); //$NON-NLS-1$
					if (plugins[i].getInstallSize() > 0)
						digest.print("install-size=\"" //$NON-NLS-1$
								+ plugins[i].getInstallSize() + "\" "); //$NON-NLS-1$
					if (!plugins[i].isUnpack())
						digest.print("unpack=\"" + plugins[i].isUnpack() //$NON-NLS-1$
								+ "\" "); //$NON-NLS-1$

					digest.println("/> "); //$NON-NLS-1$
				}
			}	
				
			IIncludedFeatureReference[] inlcudedFeatures = featureModel.getFeatureIncluded();
				
			if ((inlcudedFeatures != null) && (inlcudedFeatures.length != 0)) {
				for (int i = 0; i < inlcudedFeatures.length; i++) {
					try {
						digest.print("\t<includes "); //$NON-NLS-1$
						
						digest.print("id=\"" + inlcudedFeatures[i].getVersionedIdentifier().getIdentifier() + "\" "); //$NON-NLS-1$ //$NON-NLS-2$
						digest.print("version=\"" + inlcudedFeatures[i].getVersionedIdentifier().getVersion() + "\" "); //$NON-NLS-1$ //$NON-NLS-2$
						if (inlcudedFeatures[i].getOS() != null)
							digest.print("os=\"" + inlcudedFeatures[i].getOS() + "\" ");  //$NON-NLS-1$//$NON-NLS-2$
						if (inlcudedFeatures[i].getNL() != null)
							digest.print("nl=\"" + inlcudedFeatures[i].getNL() + "\" "); //$NON-NLS-1$ //$NON-NLS-2$
						if (inlcudedFeatures[i].getWS() != null)
							digest.print("ws=\"" + inlcudedFeatures[i].getWS() + "\" ");  //$NON-NLS-1$//$NON-NLS-2$
						if (inlcudedFeatures[i].getOSArch() != null)
							digest.print("arch=\"" + inlcudedFeatures[i].getOSArch() + "\" "); //$NON-NLS-1$ //$NON-NLS-2$											
						if ( (inlcudedFeatures[i] instanceof IncludedFeatureReference) && (((IncludedFeatureReference)inlcudedFeatures[i]).getLabel() != null))
							digest.print("name=\"" + inlcudedFeatures[i].getName() + "\" "); //$NON-NLS-1$ //$NON-NLS-2$									
						if (inlcudedFeatures[i].isOptional())
							digest.print("optional=\"true\""); //$NON-NLS-1$
						digest.print("search-location=\"" + inlcudedFeatures[i].getSearchLocation() + "\" "); //$NON-NLS-1$ //$NON-NLS-2$

						digest.println("/> "); //$NON-NLS-1$
					} catch (CoreException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
			digest.println("</feature>"); //$NON-NLS-1$
		}
	}

	private class CombinedProperties extends Properties {

		private Properties properties1;

		private Properties properties2;

		private Properties properties3;

		public CombinedProperties(Properties properties1,
				Properties properties2, Properties properties3) {
			this.properties1 = properties1;
			this.properties2 = properties2;
			this.properties3 = properties3;
		}

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public String getProperty(String key) {
			String result = null;
			if (properties3 != null && properties3.containsKey(key))
				result = properties3.getProperty(key);
			if (properties2 != null && properties2.containsKey(key))
				result = properties2.getProperty(key);
			if (properties1 != null && properties1.containsKey(key))
				result = properties1.getProperty(key);
			return result;
		}

	}

}
