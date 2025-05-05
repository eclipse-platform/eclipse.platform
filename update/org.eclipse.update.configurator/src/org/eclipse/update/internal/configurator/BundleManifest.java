/*******************************************************************************
 * Copyright (c) 2000, 2017 IBM Corporation and others.
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
 *     James D Miles (IBM Corp.) - bug 182666, trim spaces from version id
 *******************************************************************************/
package org.eclipse.update.internal.configurator;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
/**
 * Parses MANIFEST.MF
 */
public class BundleManifest implements IConfigurationConstants {
	private File manifestFile;
	private PluginEntry pluginEntry;
	private IOException exception;
	private String bundleURL;
	/**
	 * Constructor for local file
	 */
	public BundleManifest(File manifest) {
		super();
		manifestFile = manifest;
		if (manifest.exists() && !manifest.isDirectory()) {
			try (FileInputStream fos = new FileInputStream(manifest)){
				parse(fos);
			} catch (IOException ioe) {
			}
		}
	}
		/**
		 * Constructor for local file
		 */
		public BundleManifest(InputStream input, String bundleUrl) {
			super();
			bundleURL = bundleUrl;
			if (input != null) {
				parse(input);
			}
		}
	/**
	 * Parses manifest, creates PluginEntry if manifest is valid, stores
	 * exception if any occurs
	 *
	 * @param in
	 *            InputStream
	 */
	private void parse(InputStream in) {
		try {
			Manifest m = new Manifest(in);
			Attributes a = m.getMainAttributes();
			// plugin id
			String symbolicName = a.getValue(Constants.BUNDLE_SYMBOLICNAME);
			if (symbolicName == null) {
				// In Eclipse manifest must have Bundle-SymbolicName attribute
				return;
			}
			String id;
			try {
				ManifestElement[] elements = ManifestElement.parseHeader(
						Constants.BUNDLE_SYMBOLICNAME, symbolicName);
				id = elements[0].getValue();
			} catch (BundleException be) {
				throw new IOException(be.getMessage());
			}
			// plugin version
			String version = a.getValue(Constants.BUNDLE_VERSION);
			if (version == null) {
				Utils.log(NLS.bind(Messages.BundleManifest_noVersion, Constants.BUNDLE_VERSION, id));
				return;
			}
			version = version.trim();
			String hostPlugin = a.getValue(Constants.FRAGMENT_HOST);
			pluginEntry = new PluginEntry();
			pluginEntry.setVersionedIdentifier(new VersionedIdentifier(id,
					version));
			pluginEntry.isFragment(hostPlugin != null
					&& hostPlugin.length() > 0);
			// Set URL
			if(bundleURL!=null){
				pluginEntry.setURL(bundleURL);
			}else{
				File pluginDir = manifestFile.getParentFile();
				if (pluginDir != null) {
					pluginDir = pluginDir.getParentFile();
				}
				if (pluginDir != null){
					pluginEntry.setURL(PLUGINS + "/" + pluginDir.getName() + "/"); //$NON-NLS-1$ //$NON-NLS-2$
				}
			}
			//
		} catch (IOException ioe) {
			exception = ioe;
		}
	}
	public boolean exists() {
		return exception != null || pluginEntry != null;
	}
	/**
	 * Obtains PluginEntry from a manifest.
	 *
	 * @return PluginEntry or null if valid manifest does not exist
	 * @throws IOException
	 *             if exception during parsing
	 */
	public PluginEntry getPluginEntry() throws IOException {
		if (exception != null) {
			throw exception;
		}
		return pluginEntry;
	}
}
