/*******************************************************************************
 * Copyright (c) 2019, 2020 Torbjörn Svensson and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Torbjörn Svensson <azoff@svenskalinuxforeningen.se> - initial API and implementation
 *******************************************************************************/
package org.eclipse.update.configurator.tests;

import org.eclipse.update.internal.configurator.tests.FeatureEntryTests;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * Tests for integration and nightly builds.
 *
 * @since 0.1
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({ FeatureEntryTests.class })
public class AutomatedConfiguratorSuite {

}
