/*******************************************************************************
 * Copyright (c) 2000, 2012 IBM Corporation and others.
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
package org.eclipse.core.tests.resources;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * Runs the sniff tests for the build. All tests listed here should be
 * automated.
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({ org.eclipse.core.tests.filesystem.AllFileSystemTests.class,
		org.eclipse.core.tests.internal.alias.AllAliasTests.class, org.eclipse.core.tests.internal.builders.AllBuildderTests.class,
		org.eclipse.core.tests.internal.dtree.AllDtreeTests.class,
		org.eclipse.core.tests.internal.localstore.AllLocalStoreTests.class,
		org.eclipse.core.tests.internal.mapping.AllMappingTests.class,
		org.eclipse.core.tests.internal.properties.AllPropertiesTests.class,
		org.eclipse.core.tests.internal.propertytester.AllPropertytesterTests.class,
		org.eclipse.core.tests.internal.utils.AllUtilsTests.class, org.eclipse.core.tests.internal.watson.AllWatsonTests.class,
		org.eclipse.core.tests.resources.AllResourcesTests.class, org.eclipse.core.tests.resources.refresh.AllRefreshTests.class,
		org.eclipse.core.tests.resources.regression.AllRegressionTests.class,
		org.eclipse.core.tests.resources.usecase.AllUsecaseTests.class,
		org.eclipse.core.tests.resources.session.AllSessionTests.class,
		org.eclipse.core.tests.resources.content.AllContentTests.class, org.eclipse.core.tests.internal.events.AllEventsTests.class,
		org.eclipse.core.tests.internal.resources.AllInternalResourcesTests.class })
public class AutomatedResourceTests {
	public static final String PI_RESOURCES_TESTS = "org.eclipse.core.tests.resources"; //$NON-NLS-1$
}
