/*******************************************************************************
 *  Copyright (c) 2019 ArSysOp and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *      Alexander Fedorov <alexander.fedorov@arsysop.ru> - initial API and implementation
 *******************************************************************************/
package org.eclipse.e4.core.internal.tests.about;

import java.util.Collection;

import org.eclipse.core.runtime.IBundleGroup;
import org.eclipse.core.runtime.IBundleGroupProvider;
import org.osgi.framework.Bundle;

/**
 *
 * Helper mock class to enable "plain JUnit" testing
 *
 */
final class BundleGroupFactory {

	private final String identifier;
	private final String version;
	private String name;
	private String description;
	private String providerName;

	private BundleGroupFactory(String identifier, String version) {
		this.identifier = identifier;
		this.version = version;
	}

	static BundleGroupFactory identify(String identifier, String version) {
		return new BundleGroupFactory(identifier, version);
	}

	static IBundleGroupProvider provider(String name, Collection<BundleGroupFactory> groups) {
		return new IBundleGroupProvider() {

			@Override
			public String getName() {
				return name;
			}

			@Override
			public IBundleGroup[] getBundleGroups() {
				return groups.stream().map(BundleGroupFactory::create).toArray(IBundleGroup[]::new);
			}
		};
	}

	BundleGroupFactory describe(String name, String description, String providerName) {
		this.name = name;
		this.description = description;
		this.providerName = providerName;
		return this;
	}

	IBundleGroup create() {
		return new IBundleGroup() {

			@Override
			public String getVersion() {
				return version;
			}

			@Override
			public String getProviderName() {
				return providerName;
			}

			@Override
			public String getProperty(String key) {
				return null;
			}

			@Override
			public String getName() {
				return name;
			}

			@Override
			public String getIdentifier() {
				return identifier;
			}

			@Override
			public String getDescription() {
				return description;
			}

			@Override
			public Bundle[] getBundles() {
				return new Bundle[0];
			}
		};

	}



}
