/*******************************************************************************
 * Copyright (c) 2005, 2015 IBM Corporation and others.
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
package org.eclipse.core.internal.preferences.legacy;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;
import org.eclipse.core.internal.preferences.exchange.IProductPreferencesService;
import org.eclipse.core.internal.runtime.InternalPlatform;
import org.eclipse.core.runtime.*;
import org.osgi.framework.Bundle;

public class ProductPreferencesService implements IProductPreferencesService {

	private static final IPath NL_DIR = new Path("$nl$"); //$NON-NLS-1$

	// declared in org.eclipse.ui.branding.IProductConstants
	public static final String PRODUCT_KEY = "preferenceCustomization"; //$NON-NLS-1$
	private static final String LEGACY_PRODUCT_CUSTOMIZATION_FILENAME = "plugin_customization.ini"; //$NON-NLS-1$
	private static final String PROPERTIES_FILE_EXTENSION = "properties"; //$NON-NLS-1$

	private boolean initialized = false;
	private String customizationValue = null; // it won't change during the product run time
	private Bundle customizationBundle = null;
	private String productID = null;

	private void initValues() {
		if (initialized)
			return;
		initialized = true;

		IProduct product = Platform.getProduct();
		if (product == null) {
			if (InternalPlatform.DEBUG_PLUGIN_PREFERENCES)
				InternalPlatform.message("Product not available to set product default preference overrides."); //$NON-NLS-1$
			return;
		}
		productID = product.getId();
		if (productID == null) {
			if (InternalPlatform.DEBUG_PLUGIN_PREFERENCES)
				InternalPlatform.message("Product ID not available to apply product-level preference defaults."); //$NON-NLS-1$
			return;
		}
		customizationBundle = product.getDefiningBundle();
		if (customizationBundle == null) {
			if (InternalPlatform.DEBUG_PLUGIN_PREFERENCES)
				InternalPlatform.message("Bundle not available to apply product-level preference defaults for product id: " + productID); //$NON-NLS-1$
			return;
		}
		customizationValue = product.getProperty(PRODUCT_KEY);
		if (customizationValue == null) {
			if (InternalPlatform.DEBUG_PLUGIN_PREFERENCES)
				InternalPlatform.message("Product : " + productID + " does not define preference customization file. Using legacy file: plugin_customization.ini"); //$NON-NLS-1$//$NON-NLS-2$
			customizationValue = LEGACY_PRODUCT_CUSTOMIZATION_FILENAME;
		}
	}

	@Override
	public Properties getProductCustomization() {
		initValues();
		URL url = null;
		if (customizationValue != null) {
			// try to convert the key to a URL
			try {
				url = new URL(customizationValue);
			} catch (MalformedURLException e) {
				// didn't work so treat it as a filename
				url = FileLocator.find(customizationBundle, new Path(customizationValue), null);
			}
		}

		if (url == null) {
			if (InternalPlatform.DEBUG_PLUGIN_PREFERENCES)
				InternalPlatform.message("Product preference customization file: " + customizationValue + " not found for bundle: " + productID); //$NON-NLS-1$//$NON-NLS-2$
		}

		return loadProperties(url);
	}

	@Override
	public Properties getProductTranslation() {
		initValues();
		URL transURL = null;

		if (customizationValue != null)
			transURL = FileLocator.find(customizationBundle, NL_DIR.append(customizationValue).removeFileExtension().addFileExtension(PROPERTIES_FILE_EXTENSION), null);

		if (transURL == null && InternalPlatform.DEBUG_PLUGIN_PREFERENCES)
			InternalPlatform.message("No preference translations found for product/file: " + customizationBundle.getSymbolicName() + '/' + customizationValue); //$NON-NLS-1$

		return loadProperties(transURL);
	}

	private Properties loadProperties(URL url) {
		Properties result = new Properties();
		if (url == null)
			return result;
		InputStream input = null;
		try {
			input = url.openStream();
			result.load(input);
		} catch (IOException e) {
			if (InternalPlatform.DEBUG_PLUGIN_PREFERENCES) {
				InternalPlatform.message("Problem opening stream to preference customization file: " + url); //$NON-NLS-1$
				e.printStackTrace();
			}
		} finally {
			if (input != null)
				try {
					input.close();
				} catch (IOException e) {
					// ignore
				}
		}
		return result;
	}

}
