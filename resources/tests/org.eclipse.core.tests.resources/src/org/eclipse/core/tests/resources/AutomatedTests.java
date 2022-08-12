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
@Suite.SuiteClasses({ org.eclipse.core.tests.filesystem.AllTests.class,
		org.eclipse.core.tests.internal.alias.AllTests.class, org.eclipse.core.tests.internal.builders.AllTests.class,
		org.eclipse.core.tests.internal.dtree.AllTests.class,
		org.eclipse.core.tests.internal.localstore.AllTests.class,
		org.eclipse.core.tests.internal.mapping.AllTests.class,
		org.eclipse.core.tests.internal.properties.AllTests.class,
		org.eclipse.core.tests.internal.propertytester.AllTests.class,
		org.eclipse.core.tests.internal.utils.AllTests.class, org.eclipse.core.tests.internal.watson.AllTests.class,
		org.eclipse.core.tests.resources.AllTests.class, org.eclipse.core.tests.resources.refresh.AllTests.class,
		org.eclipse.core.tests.resources.regression.AllTests.class,
		org.eclipse.core.tests.resources.usecase.AllTests.class,
		org.eclipse.core.tests.resources.session.AllTests.class,
		org.eclipse.core.tests.resources.content.AllTests.class, org.eclipse.core.tests.internal.events.AllTests.class,
		org.eclipse.core.tests.internal.resources.AllTests.class })
public class AutomatedTests {
	public static final String PI_RESOURCES_TESTS = "org.eclipse.core.tests.resources"; //$NON-NLS-1$
}
