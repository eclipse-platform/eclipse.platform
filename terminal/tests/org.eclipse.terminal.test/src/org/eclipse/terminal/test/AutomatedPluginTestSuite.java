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

package org.eclipse.terminal.test;

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

/**
 * Master Test Suite to run all Terminal plug-in tests.
 */
@Suite
@SelectClasses({ //
		org.eclipse.terminal.internal.connector.TerminalConnectorPluginTest.class, //
		org.eclipse.terminal.internal.connector.TerminalConnectorFactoryTest.class, //
})
public class AutomatedPluginTestSuite {

}
