/*******************************************************************************
 * Copyright (c) 2004, 2017 IBM Corporation and others.
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
package org.eclipse.ui.internal.intro.impl.swt;

import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

import org.eclipse.core.runtime.IPath;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.ui.forms.FormColors;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.internal.intro.impl.model.IntroModelRoot;
import org.eclipse.ui.internal.intro.impl.util.ImageUtil;
import org.eclipse.ui.internal.intro.impl.util.Log;
import org.osgi.framework.Bundle;

public class SharedStyleManager {

	protected Properties properties;
	protected StyleContext context;

	static class StyleContext {
		IPath path;
		Bundle bundle;
		boolean inTheme;
	}

	SharedStyleManager() {
		// no-op
	}

	/**
	 * Constructor used when shared styles need to be loaded. The bundle is
	 * retrieved from the model root.
	 *
	 * @param modelRoot
	 */
	public SharedStyleManager(IntroModelRoot modelRoot) {
		context = new StyleContext();
		context.bundle = modelRoot.getBundle();
		properties = new Properties();
		String [] sharedStyles = modelRoot.getPresentation()
			.getImplementationStyles();
		if (sharedStyles != null) {
			for (int i=0; i<sharedStyles.length; i++)
			load(properties, sharedStyles[i], context);
		}
	}

	protected void load(Properties properties, String style, StyleContext context) {
		if (style == null)
			return;
		try {
			URL styleURL = new URL(style);
			try (InputStream is = styleURL.openStream()) {
				properties.load(is);
			}
			context.path = IPath.fromOSString(style).removeLastSegments(1);
			String t = (String)properties.get("theme"); //$NON-NLS-1$
			if (t!=null && t.trim().equalsIgnoreCase("true")) //$NON-NLS-1$
				context.inTheme = true;
		} catch (Exception e) {
			Log.error("Could not load SWT style: " + style, e); //$NON-NLS-1$
		}
	}


	/**
	 * Get the property from the shared properties.
	 *
	 * @param key
	 * @return
	 */
	public String getProperty(String key) {
		return doGetProperty(properties, key);
	}

	/*
	 * Utility method to trim properties retrieval.
	 */
	protected String doGetProperty(Properties aProperties, String key) {
		String value = aProperties.getProperty(key);
		if (value != null)
			// trim the properties as trailing balnnks cause problems.
			value = value.trim();
		return value;
	}


	protected RGB getRGB(String key) {
		String value = getProperty(key);
		if (value == null)
			return null;
		return parseRGB(value);
	}

	/**
	 * A utility method that creates RGB object from a value encoded in the
	 * following format: #rrggbb, where r, g and b are hex color values in the
	 * range from 00 to ff.
	 *
	 * @param value
	 * @return
	 */

	public static RGB parseRGB(String value) {
		if (value.charAt(0) == '#') {
			// HEX
			try {
				int r = Integer.parseInt(value.substring(1, 3), 16);
				int g = Integer.parseInt(value.substring(3, 5), 16);
				int b = Integer.parseInt(value.substring(5, 7), 16);
				return new RGB(r, g, b);
			} catch (NumberFormatException e) {
				Log.error("Failed to parser: " + value + " as an integer", e); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
		return null;
	}



	/**
	 * Finds the bundle from which this key was loaded. This is the bundle from
	 * which shared styles where loaded.
	 *
	 * @param key
	 * @return
	 */
	protected Bundle getAssociatedBundle(String key) {
		return context.bundle;
	}

	protected StyleContext getAssociatedContext(String key) {
		return context;
	}



	/**
	 * @return Returns the properties.
	 */
	public Properties getProperties() {
		return properties;
	}


	/**
	 *
	 *
	 * @param toolkit
	 * @param key
	 * @return color. May return null.
	 */
	public Color getColor(FormToolkit toolkit, String key) {
		FormColors colors = toolkit.getColors();
		Color color = colors.getColor(key);
		if (color == null) {
			RGB rgb = getRGB(key);
			if (rgb != null)
				color = colors.createColor(key, rgb);
		}
		return color;
	}



	/**
	 * Retrieve an image from this page's properties, given a key.
	 *
	 * @param key
	 * @param defaultPageKey
	 * @param defaultKey
	 * @return
	 */
	public Image getImage(String key, String defaultPageKey, String defaultKey) {
		String currentKey = key;
		String value = getProperty(currentKey);
		if (value == null && defaultPageKey != null) {
			currentKey = defaultPageKey;
			value = getProperty(defaultPageKey);
		}
		if (value != null) {
			if (ImageUtil.hasImage(currentKey))
				return ImageUtil.getImage(currentKey);
			// try to register the image.
			StyleContext ccontext = getAssociatedContext(currentKey);
			if (ccontext.inTheme) {
				// if 'theme' key is set, load image
				// relative to the file, not relative to the bundle
				ImageUtil.registerImage(currentKey, ccontext.path, value);
			}
			else {
				Bundle bundle = ccontext.bundle;
				if (bundle == null)
					// it means that we are getting a key defined in this page's
					// styles. (ie: not an inherited style).
					bundle = this.context.bundle;
				ImageUtil.registerImage(currentKey, bundle, value);
			}
			Image image = ImageUtil.getImage(currentKey);
			if (image != null)
				return image;
		}
		// try default. We know default is already registered,
		if (defaultKey != null)
			return ImageUtil.getImage(defaultKey);
		return null;
	}


	public boolean useCustomHomePagelayout() {
		String key = "home-page-custom-layout"; //$NON-NLS-1$
		String value = getProperty(key);
		if (value == null)
			value = "true"; //$NON-NLS-1$
		return value.equalsIgnoreCase("true"); //$NON-NLS-1$
	}

}
