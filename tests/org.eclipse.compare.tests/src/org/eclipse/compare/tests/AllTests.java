/*******************************************************************************
 * Copyright (c) 2000, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.compare.tests;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

/**
 * Test some non-UI areas of the compare plug-in.
 */

@RunWith(Suite.class)
@SuiteClasses({
	TextMergeViewerTest.class,
	LineReaderTest.class,
	StreamMergerTest.class,
	DocLineComparatorTest.class,
	FilterTest.class,
	PatchTest.class,
	PatchBuilderTest.class,
	AsyncExecTests.class,
	DiffTest.class,
	FileDiffResultTest.class,
	ContentMergeViewerTest.class,
	PatchLinesTest.class,
	PatchUITest.class,
	RangeDifferencerThreeWayDiffTest.class,
	CompareUIPluginTest.class,
	StructureCreatorTest.class,
	CompareFileRevisionEditorInputTest.class})
public class AllTests {
	// test suite
}
