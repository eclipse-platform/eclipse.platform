/*******************************************************************************
 * Copyright (c) 2009, 2011 IBM Corporation and others.
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
package org.eclipse.debug.tests;

import org.eclipse.debug.tests.viewer.model.ColumnPresentationTests;
import org.eclipse.debug.tests.viewer.model.JFaceViewerCheckTests;
import org.eclipse.debug.tests.viewer.model.JFaceViewerContentTests;
import org.eclipse.debug.tests.viewer.model.JFaceViewerDeltaTests;
import org.eclipse.debug.tests.viewer.model.JFaceViewerFilterTests;
import org.eclipse.debug.tests.viewer.model.JFaceViewerLazyTests;
import org.eclipse.debug.tests.viewer.model.JFaceViewerSelectionTests;
import org.eclipse.debug.tests.viewer.model.JFaceViewerStateTests;
import org.eclipse.debug.tests.viewer.model.JFaceViewerTopIndexTests;
import org.eclipse.debug.tests.viewer.model.JFaceViewerUpdateTests;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * Tests to run locally.  They require a user terminal to execute correctly
 * and have frequent issues when run on build machine.
 *
 * @since 3.7
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
		JFaceViewerCheckTests.class, JFaceViewerContentTests.class,
		JFaceViewerDeltaTests.class, JFaceViewerSelectionTests.class,
		JFaceViewerStateTests.class, JFaceViewerUpdateTests.class,
		JFaceViewerLazyTests.class, JFaceViewerTopIndexTests.class,
		JFaceViewerFilterTests.class, ColumnPresentationTests.class
})
public class LocalSuite {

}
