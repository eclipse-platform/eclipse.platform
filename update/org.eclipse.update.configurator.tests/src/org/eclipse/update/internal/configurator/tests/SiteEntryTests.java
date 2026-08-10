/*******************************************************************************
 * Copyright (c) 2026 Lars Vogel and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     Lars Vogel <Lars.Vogel@vogella.com> - initial API and implementation
 *******************************************************************************/
package org.eclipse.update.internal.configurator.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import org.eclipse.update.internal.configurator.Configuration;
import org.eclipse.update.internal.configurator.FeatureEntry;
import org.eclipse.update.internal.configurator.PluginEntry;
import org.eclipse.update.internal.configurator.SiteEntry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@SuppressWarnings("restriction")
public class SiteEntryTests {

	@TempDir
	Path siteRoot;

	@Test
	public void testLoadFromDiskDetectsFeaturesWithoutScanningPlugins() throws Exception {
		createFeature("test.feature", "1.0.0");
		Path pluginJar = createPluginJar("test.plugin", "1.0.0");

		SiteEntry site = createSite();
		site.loadFromDisk(0);

		FeatureEntry[] features = site.getFeatureEntries();
		assertEquals(1, features.length);
		assertEquals("test.feature", features[0].getFeatureIdentifier());

		// the jar is untouched so far, deleting it now leaves an eager scan nothing to have found
		Files.delete(pluginJar);
		assertEquals(0, site.getAllPluginEntries().length);
	}

	@Test
	public void testPluginsAreDetectedOnFirstAccess() throws Exception {
		createFeature("test.feature", "1.0.0");
		createPluginJar("test.plugin", "1.0.0");

		SiteEntry site = createSite();
		site.loadFromDisk(0);

		PluginEntry[] plugins = site.getAllPluginEntries();
		assertEquals(1, plugins.length);
		assertEquals("test.plugin", plugins[0].getPluginIdentifier());
		assertEquals("1.0.0", plugins[0].getPluginVersion());
	}

	private SiteEntry createSite() throws Exception {
		SiteEntry site = new SiteEntry(siteRoot.toUri().toURL());
		site.setConfig(new Configuration());
		return site;
	}

	private void createFeature(String id, String version) throws Exception {
		Path featureDir = Files.createDirectories(siteRoot.resolve("features").resolve(id + "_" + version));
		Files.writeString(featureDir.resolve("feature.xml"), "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
				+ "<feature id=\"" + id + "\" version=\"" + version + "\"/>\n");
	}

	private Path createPluginJar(String id, String version) throws Exception {
		Path pluginsDir = Files.createDirectories(siteRoot.resolve("plugins"));
		Path jar = pluginsDir.resolve(id + "_" + version + ".jar");
		Manifest manifest = new Manifest();
		Attributes attributes = manifest.getMainAttributes();
		attributes.put(Attributes.Name.MANIFEST_VERSION, "1.0");
		attributes.putValue("Bundle-ManifestVersion", "2");
		attributes.putValue("Bundle-SymbolicName", id);
		attributes.putValue("Bundle-Version", version);
		try (OutputStream out = Files.newOutputStream(jar); JarOutputStream jarOut = new JarOutputStream(out, manifest)) {
			// the manifest written by the stream is all the detection needs
		}
		return jar;
	}
}
