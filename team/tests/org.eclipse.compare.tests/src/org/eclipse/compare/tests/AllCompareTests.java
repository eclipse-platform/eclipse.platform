/*******************************************************************************
 * Copyright (c) 2000, 2018 IBM Corporation and others.
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
public class AllCompareTests {
	// test suite
}
