/*******************************************************************************
 *  Copyright (c) 2004, 2012 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.tests.resources.perf;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * @since 3.1
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({ BenchFileStore.class, BenchWorkspace.class, BenchMiscWorkspace.class,
		BuilderPerformanceTest.class, MarkerPerformanceTest.class, LocalHistoryPerformanceTest.class,
		WorkspacePerformanceTest.class, PropertyManagerPerformanceTest.class, FileSystemPerformanceTest.class })
public class AllResourcePerfTests {
	// these tests are flawed - see bug 57137
	// ContentDescriptionPerformanceTest.class
}
