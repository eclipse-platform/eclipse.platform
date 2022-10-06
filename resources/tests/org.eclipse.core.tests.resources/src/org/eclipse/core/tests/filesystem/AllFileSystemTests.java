/*******************************************************************************
 * Copyright (c) 2005, 2012 IBM Corporation and others.
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
 * Martin Oberhuber (Wind River) - [] add SymlinkTest tests
 *******************************************************************************/
package org.eclipse.core.tests.filesystem;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * Class for collecting all test classes that deal with the file system API.
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({ CreateDirectoryTest.class, DeleteTest.class, EFSTest.class, FileCacheTest.class,
		FileStoreTest.class, OpenOutputStreamTest.class, PutInfoTest.class, SymlinkTest.class, URIUtilTest.class })
public class AllFileSystemTests {
}
