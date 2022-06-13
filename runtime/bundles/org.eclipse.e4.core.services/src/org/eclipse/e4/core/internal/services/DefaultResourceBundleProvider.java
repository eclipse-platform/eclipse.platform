/*******************************************************************************
 * Copyright (c) 2014, 2016 Dirk Fauth and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Dirk Fauth <dirk.fauth@googlemail.com> - initial API and implementation
 ******************************************************************************/
package org.eclipse.e4.core.internal.services;

import java.util.ResourceBundle;
import org.eclipse.e4.core.services.translation.ResourceBundleProvider;
import org.eclipse.osgi.service.localization.BundleLocalization;
import org.osgi.framework.Bundle;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * Default implementation of {@link ResourceBundleProvider} that simply delegates to the
 * {@link BundleLocalization} for retrieving the {@link ResourceBundle}.
 */
@Component
public class DefaultResourceBundleProvider implements ResourceBundleProvider {

	private BundleLocalization localization;

	@Override
	public ResourceBundle getResourceBundle(Bundle bundle, String locale) {
		if (localization != null)
			return localization.getLocalization(bundle, locale);

		return null;
	}

	/**
	 * Method called by DS to set the {@link BundleLocalization} to this
	 * {@link ResourceBundleProvider}.
	 *
	 * @param localization
	 *            The {@link BundleLocalization} that should be set to this
	 *            {@link ResourceBundleProvider}
	 */
	@Reference
	void setBundleLocalization(BundleLocalization localization) {
		this.localization = localization;
	}

}
