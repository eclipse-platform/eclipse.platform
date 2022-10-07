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
 *     Broadcom Corp. - build configurations
 *******************************************************************************/
package org.eclipse.core.tests.internal.resources;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * The suite method for this class contains test suites for all automated tests
 * in this test package.
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({ ModelObjectReaderWriterTest.class, ProjectPreferencesTest.class,
		ResourceInfoTest.class,
		WorkspaceConcurrencyTest.class, WorkspacePreferencesTest.class, ProjectReferencesTest.class,
		ProjectDynamicReferencesTest.class, ProjectBuildConfigsTest.class, Bug544975Test.class, })
public class AllInternalResourcesTests {
}
