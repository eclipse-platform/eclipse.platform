/*******************************************************************************
 * Copyright (c) 2026 Vogella GmbH and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Lars Vogel - initial implementation
 *******************************************************************************/
package org.eclipse.team.tests.core;

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

/**
 * Runs every test of this bundle. The Maven build can only run a single suite
 * per bundle, while test.xml keeps the core and UI suites apart.
 */
@Suite
@SelectClasses({ //
		AllTeamTests.class, //
		AllTeamUITests.class, //
})
public class AllTests {
}
