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
 *******************************************************************************/
package org.eclipse.core.tests.internal.alias;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * Class for collecting all test classes that deal with alias support. An alias
 * is a resource in the workspace that has the same file system location as
 * another resource in the workspace. When a resource changes in a way that
 * affects the contents on disk, all aliases need to be updated.
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({ BasicAliasTest.class, SyncAliasTest.class })
public class AllAliasTests {
}
