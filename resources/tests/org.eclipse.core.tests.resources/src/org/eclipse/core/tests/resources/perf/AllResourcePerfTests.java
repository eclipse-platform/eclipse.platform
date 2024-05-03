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

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

/**
 * @since 3.1
 */
@Suite
@SelectClasses({ //
		BenchCopyFile.class, //
		BenchElementTree.class, //
		// BenchFileStore.class, // very long running
		BenchMiscWorkspace.class, //
		BenchWorkspace.class, //
		BuilderPerformanceTest.class, //
		ConcurrencyPerformanceTest.class, //
		ContentDescriptionPerformanceTest.class, //
		FileSystemPerformanceTest.class, //
		LocalHistoryPerformanceTest.class, //
		MarkerPerformanceTest.class, //
		PropertyManagerPerformanceTest.class, //
		WorkspacePerformanceTest.class, //
})
public class AllResourcePerfTests {
}
