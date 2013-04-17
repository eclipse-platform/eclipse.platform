/*******************************************************************************
 * Copyright (c) 2000, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.compare.tests;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Test some non-UI areas of the compare plugin.
 */
public class AllTests {

	public static Test suite() {
		TestSuite suite = new TestSuite("Test for org.eclipse.compare.tests"); //$NON-NLS-1$
		// $JUnit-BEGIN$
		suite.addTestSuite(TextMergeViewerTest.class);
		suite.addTestSuite(LineReaderTest.class);
		suite.addTestSuite(StreamMergerTest.class);
		suite.addTestSuite(DocLineComparatorTest.class);
		suite.addTestSuite(FilterTest.class);
		suite.addTestSuite(PatchTest.class);
		suite.addTestSuite(PatchBuilderTest.class);
		suite.addTestSuite(AsyncExecTests.class);
		suite.addTestSuite(DiffTest.class);
		suite.addTestSuite(FileDiffResultTest.class);
		suite.addTestSuite(ContentMergeViewerTest.class);
		suite.addTestSuite(PatchLinesTest.class);
		suite.addTestSuite(PatchUITest.class);
		suite.addTestSuite(RangeDifferencerThreeWayDiffTest.class);
		suite.addTestSuite(CompareUIPluginTest.class);
		suite.addTestSuite(StructureCreatorTest.class);
		// $JUnit-END$
		return suite;
	}
}
