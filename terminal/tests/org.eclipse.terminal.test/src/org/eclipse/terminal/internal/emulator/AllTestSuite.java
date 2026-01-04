/*******************************************************************************
 * Copyright (c) 2008, 2018 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * Martin Oberhuber (Wind River) - initial API and implementation
 *******************************************************************************/
package org.eclipse.terminal.internal.emulator;

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

/**
 * Terminal emulator test cases.
 * Runs in emulator package to allow access to default visible items.
 */
@Suite
@SelectClasses({ //
		VT100EmulatorBackendTest.class, //
		VT100EmulatorTest.class, //
})
public class AllTestSuite {

}
