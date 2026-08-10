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

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.StringTokenizer;

import org.eclipse.core.runtime.IBundleGroup;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.Platform;
import org.osgi.framework.Bundle;

/**
 * A bundle group backed by an installed feature. The feature identity comes
 * from platform.xml, everything else is read from the feature and its branding
 * bundle on demand.
 */
public class FeatureBundleGroup implements IBundleGroup, IBundleGroupConstants, IProductConstants {

	private static final String FEATURE_XML = "feature.xml"; //$NON-NLS-1$
	private static final String FEATURE_PROPERTIES = "feature"; //$NON-NLS-1$
	private static final String KEY_PREFIX = "%"; //$NON-NLS-1$
	private static final String KEY_DOUBLE_PREFIX = "%%"; //$NON-NLS-1$

	private final String id;
	private final String version;
	private final String pluginIdentifier;
	private final String pluginVersion;
	private final URI featureLocation;

	private AboutInfo branding;
	private FeatureManifest manifest;
	private boolean fullyParsed;
	private ResourceBundle resourceBundle;
	private boolean resourceBundleLoaded;

	FeatureBundleGroup(String id, String version, String pluginIdentifier, String pluginVersion, URI featureLocation) {
		this.id = id;
		this.version = version;
		this.pluginIdentifier = pluginIdentifier;
		this.pluginVersion = pluginVersion;
		this.featureLocation = featureLocation;
	}

	/**
	 * A feature contributes a bundle group only when its branding bundle is
	 * installed.
	 */
	boolean hasBranding() {
		String bundleId = getFeaturePluginIdentifier();
		return bundleId != null && Platform.getBundle(bundleId) != null;
	}

	private String getFeaturePluginIdentifier() {
		return pluginIdentifier != null && !pluginIdentifier.isEmpty() ? pluginIdentifier : id;
	}

	@Override
	public String getIdentifier() {
		return id;
	}

	@Override
	public String getVersion() {
		return version;
	}

	@Override
	public String getName() {
		return getBranding().getProductName();
	}

	@Override
	public String getProviderName() {
		return getBranding().getProviderName();
	}

	@Override
	public String getDescription() {
		FeatureManifest parsed = fullParse();
		return parsed == null ? null : getResourceString(parsed.description);
	}

	@Override
	public Bundle[] getBundles() {
		FeatureManifest parsed = fullParse();
		if (parsed == null) {
			return new Bundle[0];
		}
		List<Bundle> bundles = new ArrayList<>(parsed.pluginIds.size());
		for (String pluginId : parsed.pluginIds) {
			Bundle bundle = Platform.getBundle(pluginId);
			if (bundle != null) {
				bundles.add(bundle);
			}
		}
		return bundles.toArray(new Bundle[bundles.size()]);
	}

	@Override
	public String getProperty(String key) {
		if (key == null) {
			return null;
		}
		AboutInfo info = getBranding();

		// IBundleGroupConstants
		if (key.equals(FEATURE_IMAGE)) {
			return toExternalForm(info.getFeatureImageURL());
		} else if (key.equals(TIPS_AND_TRICKS_HREF)) {
			return info.getTipsAndTricksHref();
		} else if (key.equals(IBundleGroupConstants.WELCOME_PAGE)) { // same value is used by product and bundle group
			return toExternalForm(info.getWelcomePageURL());
		} else if (key.equals(WELCOME_PERSPECTIVE)) {
			return info.getWelcomePerspectiveId();
		} else if (key.equals(BRANDING_BUNDLE_ID)) {
			return pluginIdentifier;
		} else if (key.equals(BRANDING_BUNDLE_VERSION)) {
			return pluginVersion;
		} else if (key.equals(APP_NAME)) {
			return info.getAppName();
		} else if (key.equals(ABOUT_TEXT)) {
			return info.getAboutText();
		} else if (key.equals(ABOUT_IMAGE)) {
			return toExternalForm(info.getAboutImageURL());
		} else if (key.equals(WINDOW_IMAGE)) {
			return toExternalForm(info.getWindowImageURL());
		} else if (key.equals(WINDOW_IMAGES)) {
			URL[] urls = info.getWindowImagesURLs();
			if (urls == null) {
				return null;
			}
			StringBuilder windowImagesURLs = new StringBuilder();
			for (int i = 0; i < urls.length; i++) {
				windowImagesURLs.append(urls[i].toExternalForm());
				if (i != urls.length - 1) {
					windowImagesURLs.append(',');
				}
			}
			return windowImagesURLs.toString();
		} else if (key.equals(LICENSE_HREF)) {
			return getLicenseURL();
		}

		return null;
	}

	private static String toExternalForm(URL url) {
		return url == null ? null : url.toExternalForm();
	}

	private synchronized AboutInfo getBranding() {
		if (branding == null) {
			branding = AboutInfo.readFeatureInfo(id, version, getFeaturePluginIdentifier());
		}
		return branding;
	}

	private String getLicenseURL() {
		FeatureManifest parsed = fullParse();
		if (parsed == null || parsed.licenseURL == null) {
			return null;
		}
		String resolved = getResourceString(parsed.licenseURL);
		if (resolved.startsWith("http://") || resolved.startsWith("https://")) { //$NON-NLS-1$ //$NON-NLS-2$
			return resolved;
		}
		return featureLocation.resolve(resolved).toString();
	}

	private synchronized FeatureManifest fullParse() {
		if (!fullyParsed) {
			fullyParsed = true;
			try {
				manifest = FeatureXmlParser.parse(featureLocation.resolve(FEATURE_XML).toURL(), false);
			} catch (MalformedURLException | IllegalArgumentException e) {
				ILog.of(FeatureBundleGroup.class).error("Cannot locate the feature.xml of " + id, e); //$NON-NLS-1$
			}
		}
		return manifest;
	}

	/*
	 * Resolves a %key reference against the feature.properties of this feature.
	 */
	private String getResourceString(String value) {
		if (value == null) {
			return null;
		}
		String s = value.trim();
		if (s.isEmpty() || !s.startsWith(KEY_PREFIX)) {
			return value;
		}
		if (s.startsWith(KEY_DOUBLE_PREFIX)) {
			return s.substring(1);
		}

		int index = s.indexOf(' ');
		String key = index == -1 ? s : s.substring(0, index);
		String defaultValue = index == -1 ? s : s.substring(index + 1);

		ResourceBundle bundle = getResourceBundle();
		if (bundle == null) {
			return defaultValue;
		}
		try {
			return bundle.getString(key.substring(1));
		} catch (MissingResourceException e) {
			return defaultValue;
		}
	}

	private synchronized ResourceBundle getResourceBundle() {
		if (!resourceBundleLoaded) {
			resourceBundleLoaded = true;
			try {
				ClassLoader loader = new URLClassLoader(new URL[] { featureLocation.toURL() }, null);
				resourceBundle = ResourceBundle.getBundle(FEATURE_PROPERTIES, getDefaultLocale(), loader);
			} catch (MissingResourceException | MalformedURLException | IllegalArgumentException e) {
				// features are not required to be translated
			}
		}
		return resourceBundle;
	}

	private static Locale getDefaultLocale() {
		String nl = Platform.getNL();
		if (nl == null) {
			return Locale.getDefault();
		}
		StringTokenizer locales = new StringTokenizer(nl, "_"); //$NON-NLS-1$
		switch (locales.countTokens()) {
		case 1:
			return Locale.of(locales.nextToken());
		case 2:
			return Locale.of(locales.nextToken(), locales.nextToken());
		case 3:
			return Locale.of(locales.nextToken(), locales.nextToken(), locales.nextToken());
		default:
			return Locale.getDefault();
		}
	}
}
