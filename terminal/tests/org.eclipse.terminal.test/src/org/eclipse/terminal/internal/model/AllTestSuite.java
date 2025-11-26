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
package org.eclipse.terminal.internal.model;

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

/**
 * Internal Terminal Model test cases.
 * Runs in internal model package to allow access to default visible items.
 */
@Suite
@SelectClasses({ //
		SnapshotChangesTest.class, //
		SynchronizedTerminalTextDataTest.class, //
		TerminalTextDataFastScrollTest.class, //
		TerminalTextDataFastScrollMaxHeightTest.class, //
		TerminalTextDataPerformanceTest.class, //
		TerminalTextDataSnapshotTest.class, //
		TerminalTextDataSnapshotWindowTest.class, //
		TerminalTextDataStoreTest.class, //
		TerminalTextDataTest.class, //
		TerminalTextDataWindowTest.class, //
})
public class AllTestSuite {

}
