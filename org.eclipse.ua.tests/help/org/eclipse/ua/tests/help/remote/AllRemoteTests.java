/*******************************************************************************
 * Copyright (c) 2009, 2012 IBM Corporation and others.
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
package org.eclipse.ua.tests.help.remote;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/*
 * Tests help keyword index functionality.
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
	RemotePreferenceTest.class,
	TocServletTest.class,
	SearchServletTest.class,
	IndexServletTest.class,
	ContentServletTest.class,
	ContextServletTest.class,
	LoadTocUsingRemoteHelp.class,
	SearchUsingRemoteHelp.class,
	LoadIndexUsingRemoteHelp.class,
	GetContentUsingRemoteHelp.class,
	GetContextUsingRemoteHelp.class,
	TocManagerTest.class,
	SearchIndexCreation.class,
	ParallelSearchUsingRemote.class,
	ParallelSearchServletTest.class
})
public class AllRemoteTests {
}
