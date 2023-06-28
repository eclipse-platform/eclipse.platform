/*******************************************************************************
 * Copyright (c) 2006, 2018 IBM Corporation and others.
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
package org.eclipse.unittest.internal.launcher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.eclipse.unittest.internal.UnitTestPlugin;
import org.eclipse.unittest.ui.ITestViewSupport;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.Platform;

import org.eclipse.debug.core.ILaunchConfiguration;

/**
 * Test View Support registry
 */
public class TestViewSupportRegistry {
	/**
	 * An identifier of Test View Support extension point
	 */
	public static final String ID_EXTENSION_POINT_TEST_VIEW_SUPPORTS = UnitTestPlugin.PLUGIN_ID + "." //$NON-NLS-1$
			+ "unittestViewSupport"; //$NON-NLS-1$

	/**
	 * Returns an instance of {@link TestViewSupportRegistry} object
	 *
	 * @return a {@link TestViewSupportRegistry} object
	 */
	public static TestViewSupportRegistry getDefault() {
		if (fgRegistry != null)
			return fgRegistry;

		fgRegistry = new TestViewSupportRegistry(
				Platform.getExtensionRegistry().getExtensionPoint(ID_EXTENSION_POINT_TEST_VIEW_SUPPORTS));
		return fgRegistry;
	}

	private static TestViewSupportRegistry fgRegistry;

	private final IExtensionPoint fPoint;
	private List<TestViewSupportExtension> fTestViewSupportExtensions;

	private TestViewSupportRegistry(IExtensionPoint point) {
		fPoint = point;
	}

	/**
	 * Returns all the registered View Support extensions
	 *
	 * @return a {@link List} containing all the registered View Support extensions
	 */
	private List<TestViewSupportExtension> getAllTestViewSupportExtensions() {
		loadTestViewSupportExtensions();
		return fTestViewSupportExtensions;
	}

	/**
	 * Returns all the registered View Support extensions that suit the specified
	 * filter
	 *
	 * @param filter a registry identifier filter
	 * @return an {@link ArrayList} containing the registry kings filtered by
	 *         identifier
	 */
	public List<TestViewSupportExtension> getTestViewSupportExtensions(final String filter) {
		List<TestViewSupportExtension> all = getAllTestViewSupportExtensions();
		if (all == null)
			return Collections.emptyList();

		return all.stream().filter(p -> p.getId().startsWith(filter)).collect(Collectors.toList());
	}

	private void loadTestViewSupportExtensions() {
		if (fTestViewSupportExtensions != null)
			return;

		List<TestViewSupportExtension> items = getConfigurationElements().stream().map(TestViewSupportExtension::new)
				.collect(Collectors.toList());

		fTestViewSupportExtensions = items;
	}

	/*
	 * Returns an {@link Optional <ITestViewSupport>} object instance by its
	 * identifier
	 *
	 * @param id an identifier, can be <code>null</code>
	 *
	 * @return an {@link Optional <ITestViewSupport>} object instance
	 */
	private Optional<ITestViewSupport> findTestViewSupport(String id) {
		return getAllTestViewSupportExtensions().stream().filter(ext -> ext.getId().equals(id)).findFirst().map(t -> {
			try {
				return t.instantiateTestViewSupport();
			} catch (CoreException e) {
				UnitTestPlugin.log(e);
				return null;
			}
		});
	}

	/**
	 * Returns {@link ITestViewSupport} instance from the given launch configuration
	 *
	 * @param launchConfiguration a launch configuration
	 * @return an {@link Optional <ITestViewSupport>} object instance
	 */
	public static Optional<ITestViewSupport> newTestRunnerViewSupport(ILaunchConfiguration launchConfiguration) {
		try {
			return getDefault().findTestViewSupport(launchConfiguration
					.getAttribute(UnitTestLaunchConfigurationConstants.ATTR_UNIT_TEST_VIEW_SUPPORT, (String) null));
		} catch (CoreException e) {
			UnitTestPlugin.log(e);
			return Optional.empty();
		}
	}

	private List<IConfigurationElement> getConfigurationElements() {
		List<IConfigurationElement> items = new ArrayList<>();
		for (IExtension extension : fPoint.getExtensions()) {
			Collections.addAll(items, extension.getConfigurationElements());
		}
		return items;
	}

}
