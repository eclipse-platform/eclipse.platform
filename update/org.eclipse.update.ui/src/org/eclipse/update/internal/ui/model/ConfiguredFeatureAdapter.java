/*
 * Copyright (c) 2002 IBM Corp.  All rights reserved.
 * This file is made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 */
package org.eclipse.update.internal.ui.model;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.update.configuration.*;
import org.eclipse.update.core.*;

/**
 * @version 	1.0
 * @author
 */
public class ConfiguredFeatureAdapter
	extends SimpleFeatureAdapter
	implements IConfiguredFeatureAdapter {
	private IConfiguredSiteAdapter adapter;
	private boolean configured;

	public ConfiguredFeatureAdapter(
		IConfiguredSiteAdapter adapter,
		IFeature feature,
		boolean configured,
		boolean optional) {
		super(feature, optional);
		this.adapter = adapter;
		this.configured = configured;
	}

	public IConfiguredSite getConfigurationSite() {
		return adapter.getConfigurationSite();
	}
	public IInstallConfiguration getInstallConfiguration() {
		return adapter.getInstallConfiguration();
	}
	public boolean isConfigured() {
		return configured;
	}
	public IFeatureAdapter[] getIncludedFeatures() {
		try {
			IFeatureReference[] included =
				getFeature().getIncludedFeatureReferences();
			ConfiguredFeatureAdapter[] result =
				new ConfiguredFeatureAdapter[included.length];
			for (int i = 0; i < included.length; i++) {
				IFeatureReference fref = included[i];
				IFeature feature;
				try {
					feature = fref.getFeature();
				} catch (CoreException e) {
					feature = new MissingFeature(fref);
				}

				result[i] =
					new ConfiguredFeatureAdapter(adapter, feature, configured, fref.isOptional());
				result[i].setIncluded(true);
			}
			return result;
		} catch (CoreException e) {
			return new IFeatureAdapter[0];
		}
	}
}