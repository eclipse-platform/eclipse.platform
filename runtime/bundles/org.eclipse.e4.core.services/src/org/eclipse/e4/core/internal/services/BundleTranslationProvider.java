/*******************************************************************************
 * Copyright (c) 2011, 2018 IBM Corporation and others.
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
 *     Dirk Fauth <dirk.fauth@googlemail.com> - Bug 428427
 ******************************************************************************/
package org.eclipse.e4.core.internal.services;

import java.util.ResourceBundle;
import javax.inject.Inject;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.core.services.translation.ResourceBundleProvider;
import org.eclipse.e4.core.services.translation.TranslationService;
import org.osgi.service.log.Logger;
import org.osgi.service.log.LoggerFactory;

public class BundleTranslationProvider extends TranslationService {

	@Inject
	@Optional
	ResourceBundleProvider provider;

	Logger logger;

	@Override
	public String translate(String key, String contributorURI) {
		ResourceBundleProvider prov = this.provider;
		if (prov == null) {
			return key;
		}

		try {
			ResourceBundle resourceBundle = ResourceBundleHelper.getResourceBundleForUri(
					contributorURI, locale, prov);
			return getResourceString(key, resourceBundle);
		} catch (Exception e) {
			// an error occurred on trying to retrieve the translation for the given key
			// for improved fault tolerance we will log the error and return the key
			Logger log = this.logger;
			if (log != null) {
				log.error("Error retrieving the translation for key={} and contributorURI={}", key, contributorURI,
						e);
			}

			return key;
		}
	}

	@Inject
	@Optional
	void setLoggerFactory(LoggerFactory factory) {
		if (factory != null) {
			this.logger = factory.getLogger(getClass());
		} else {
			this.logger = null;
		}
	}
}
