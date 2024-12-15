/*******************************************************************************
 * Copyright (c) 2009, 2017 IBM Corporation and others.
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
package org.eclipse.update.internal.configurator;

import java.util.ArrayList;

import org.eclipse.core.runtime.IBundleGroup;
import org.eclipse.core.runtime.IBundleGroupProvider;
import org.eclipse.update.configurator.IPlatformConfiguration;
import org.eclipse.update.configurator.IPlatformConfiguration.IFeatureEntry;
import org.eclipse.update.configurator.IPlatformConfigurationFactory;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * Declarative services component that provides an implementation of
 * {@link IBundleGroupProvider}. This allows the bundle group provider to be
 * made available in the service registry before this bundle has started.
 */
@Component(service = IBundleGroupProvider.class)
@SuppressWarnings("deprecation")
public class BundleGroupComponent implements IBundleGroupProvider {


	private IPlatformConfigurationFactory factory;

	@Activate
	public BundleGroupComponent(@Reference IPlatformConfigurationFactory factory) {
		this.factory = factory;
	}

	@Override
	public IBundleGroup[] getBundleGroups() {
		IPlatformConfiguration configuration = factory.getCurrentPlatformConfiguration();
		if (configuration == null) {
			return new IBundleGroup[0];
		}
		IPlatformConfiguration.IFeatureEntry[] features = configuration.getConfiguredFeatureEntries();
		ArrayList<IBundleGroup> bundleGroups = new ArrayList<>(features.length);
		for (IFeatureEntry feature : features) {
			if (feature instanceof FeatureEntry && ((FeatureEntry) feature).hasBranding())
				bundleGroups.add((IBundleGroup) feature);
		}
		return bundleGroups.toArray(new IBundleGroup[bundleGroups.size()]);
	}

	@Override
	public String getName() {
		return Messages.BundleGroupProvider;
	}

}
