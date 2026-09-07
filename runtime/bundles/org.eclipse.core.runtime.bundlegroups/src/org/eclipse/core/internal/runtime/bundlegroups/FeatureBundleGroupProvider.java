/*******************************************************************************
 * Copyright (c) 2026 Lars Vogel and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Lars Vogel <Lars.Vogel@vogella.com> - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.internal.runtime.bundlegroups;

import java.io.File;
import java.io.FileFilter;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.internal.runtime.bundlegroups.platformxml.Configuration;
import org.eclipse.core.internal.runtime.bundlegroups.platformxml.ConfigurationParser;
import org.eclipse.core.internal.runtime.bundlegroups.platformxml.Feature;
import org.eclipse.core.internal.runtime.bundlegroups.platformxml.Site;
import org.eclipse.core.runtime.IBundleGroup;
import org.eclipse.core.runtime.IBundleGroupProvider;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.Platform;
import org.eclipse.osgi.service.datalocation.Location;
import org.osgi.service.component.annotations.Component;

/**
 * Contributes the installed features as bundle groups. The feature list comes
 * from the platform.xml written at provisioning time, so no plug-in has to be
 * inspected to build it.
 */
@Component(service = IBundleGroupProvider.class)
public class FeatureBundleGroupProvider implements IBundleGroupProvider {

	private static final String PLATFORM_XML = "org.eclipse.update/platform.xml"; //$NON-NLS-1$
	private static final String FEATURES = "features"; //$NON-NLS-1$
	private static final String FEATURE_XML = "feature.xml"; //$NON-NLS-1$

	private List<FeatureBundleGroup> cachedFeatures;
	private long cachedTimestamp = -1;

	@Override
	public String getName() {
		return Messages.BundleGroupProvider;
	}

	@Override
	public synchronized IBundleGroup[] getBundleGroups() {
		File platformXml = findPlatformXml();
		long timestamp = platformXml == null ? 0 : platformXml.lastModified();
		if (cachedFeatures == null || timestamp != cachedTimestamp) {
			cachedFeatures = readFeatures(platformXml);
			cachedTimestamp = timestamp;
		}
		// the branding bundle of a feature can be installed after the feature was read
		return cachedFeatures.stream().filter(FeatureBundleGroup::hasBranding).toArray(IBundleGroup[]::new);
	}

	private static List<FeatureBundleGroup> readFeatures(File platformXml) {
		URL installArea = getInstallArea();
		List<FeatureBundleGroup> features = new ArrayList<>();
		Configuration configuration = platformXml == null ? null
				: ConfigurationParser.parse(platformXml, installArea);
		if (configuration == null) {
			collectFromInstallArea(installArea, features);
		} else {
			for (Site site : configuration.getSites()) {
				collectFromSite(site, features);
			}
		}
		return features;
	}

	private static void collectFromSite(Site site, List<FeatureBundleGroup> features) {
		URI siteLocation = toURI(site.getUrl());
		if (siteLocation == null) {
			return;
		}
		for (Feature feature : site.getFeatures()) {
			if (feature.getId() == null) {
				continue;
			}
			String featureURL = feature.getUrl();
			if (featureURL == null) {
				featureURL = FEATURES + "/" + feature.getId() + "_" + feature.getVersion() + "/"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			}
			features.add(new FeatureBundleGroup(feature.getId(), feature.getVersion(), feature.getPluginIdentifier(),
					feature.getPluginVersion(), siteLocation.resolve(featureURL)));
		}
	}

	/*
	 * Without a platform.xml the installed features can only be found by looking at
	 * the features directory of the install area.
	 */
	private static void collectFromInstallArea(URL installArea, List<FeatureBundleGroup> features) {
		if (installArea == null || !"file".equals(installArea.getProtocol())) { //$NON-NLS-1$
			return;
		}
		File featuresDir = new File(installArea.getFile().replace('/', File.separatorChar), FEATURES);
		File[] dirs = featuresDir
				.listFiles((FileFilter) f -> f.isDirectory() && new File(f, FEATURE_XML).exists());
		if (dirs == null) {
			return;
		}
		for (File dir : dirs) {
			try {
				FeatureManifest manifest = FeatureXmlParser.parse(new File(dir, FEATURE_XML).toURI().toURL(), true);
				if (manifest != null) {
					features.add(new FeatureBundleGroup(manifest.id, manifest.version, manifest.pluginIdentifier,
							manifest.version, dir.toURI()));
				}
			} catch (MalformedURLException e) {
				ILog.of(FeatureBundleGroupProvider.class).error("Cannot read the feature in " + dir, e); //$NON-NLS-1$
			}
		}
	}

	/*
	 * The platform.xml of the configuration area, falling back to the one of a
	 * shared install.
	 */
	private static File findPlatformXml() {
		Location configuration = Platform.getConfigurationLocation();
		if (configuration == null) {
			return null;
		}
		File file = toPlatformXml(configuration);
		if (file != null && file.exists()) {
			return file;
		}
		Location parent = configuration.getParentLocation();
		File parentFile = parent == null ? null : toPlatformXml(parent);
		return parentFile != null && parentFile.exists() ? parentFile : null;
	}

	private static File toPlatformXml(Location location) {
		URL url = location.getURL();
		if (url == null || !"file".equals(url.getProtocol())) { //$NON-NLS-1$
			return null;
		}
		return new File(url.getFile().replace('/', File.separatorChar), PLATFORM_XML);
	}

	private static URL getInstallArea() {
		Location install = Platform.getInstallLocation();
		return install == null ? null : install.getURL();
	}

	private static URI toURI(String encodedURI) {
		if (encodedURI == null) {
			return null;
		}
		try {
			return new URI(encodedURI);
		} catch (URISyntaxException e) {
			ILog.of(FeatureBundleGroupProvider.class).error("Cannot resolve the site " + encodedURI, e); //$NON-NLS-1$
			return null;
		}
	}
}
