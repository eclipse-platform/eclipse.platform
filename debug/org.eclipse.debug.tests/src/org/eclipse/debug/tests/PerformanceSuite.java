/*******************************************************************************
 * Copyright (c) 2009 Wind River Systems and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.debug.tests;

import org.eclipse.debug.tests.viewer.model.JFaceViewerPerformanceTests;
import org.eclipse.debug.tests.viewer.model.VirtualViewerPerformanceTests;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * Tests for release builds.
 *
 * @since 3.6
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
		JFaceViewerPerformanceTests.class,
		VirtualViewerPerformanceTests.class })
public class PerformanceSuite {
}
