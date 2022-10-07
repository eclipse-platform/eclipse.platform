/*******************************************************************************
 * Copyright (c) 2000, 2012 IBM Corporation and others.
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
 * Martin Oberhuber (Wind River) - [105554] added PrefixPoolTest
 *******************************************************************************/
package org.eclipse.core.tests.internal.localstore;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({ BlobStoreTest.class, BucketTreeTests.class, CaseSensitivityTest.class, CopyTest.class,
		DeleteTest.class, FileSystemResourceManagerTest.class, HistoryBucketTest.class, HistoryStoreTest.class,
		LocalSyncTest.class, MoveTest.class, PrefixPoolTest.class, RefreshLocalTest.class,
		SafeChunkyInputOutputStreamTest.class, SafeFileInputOutputStreamTest.class, SymlinkResourceTest.class,
		UnifiedTreeTest.class })
public class AllLocalStoreTests {

}
