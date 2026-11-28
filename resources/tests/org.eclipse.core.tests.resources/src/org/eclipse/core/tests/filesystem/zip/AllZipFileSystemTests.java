/*******************************************************************************
 * Copyright (c) 2024 Vector Informatik GmbH and others.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors: Vector Informatik GmbH - initial API and implementation
 *******************************************************************************/

package org.eclipse.core.tests.filesystem.zip;

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

/**
 * Class for collecting all test classes that deal with the zip file system API.
 */
@Suite
@SelectClasses({ CloseTest.class, CopyTest.class, CreateTest.class, DeleteTest.class, MoveTest.class,
		RenameTest.class, SetupTest.class, OpenTest.class })
public class AllZipFileSystemTests {
}

