/*******************************************************************************
 * Copyright (c) 2006, 2007 IBM Corporation and others.
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
package org.eclipse.team.tests.ccvs.core;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.core.client.Command;
import org.eclipse.team.internal.ccvs.core.client.RLog;
import org.eclipse.team.internal.ccvs.core.client.Command.LocalOption;
import org.eclipse.team.internal.ccvs.core.connection.CVSRepositoryLocation;
import org.eclipse.team.internal.ccvs.ui.operations.RemoteLogOperation;
import org.eclipse.ui.IWorkbenchPart;

public class FindCommittersTest extends EclipseTest {

	public class FindCommittersOperation extends RemoteLogOperation {
		private LogEntryCache cache;
		private Set<String> authors;

		public FindCommittersOperation(IWorkbenchPart part,
				ICVSRemoteResource[] remoteResources, CVSTag tag1, CVSTag tag2,
				LogEntryCache cache) {
			super(part, remoteResources, tag1, tag2, cache);
			this.cache = cache;
		}

		@Override
		protected LocalOption[] getLocalOptions(CVSTag tag1, CVSTag tag2) {
			return new Command.LocalOption[] {RLog.NO_TAGS, RLog.ONLY_INCLUDE_CHANGES, RLog.REVISIONS_ON_DEFAULT_BRANCH, new LocalOption("-d" + tag1.asDate() + "<" + tag2.asDate().toString(), null) {
				
			}};
		}

		@Override
		protected void execute(ICVSRepositoryLocation location,
				ICVSRemoteResource[] remoteResources, IProgressMonitor monitor)
				throws CVSException {
			super.execute(location, remoteResources, monitor);
			processAuthors();
		}

		private void processAuthors() {
			authors = getAuthors(cache);
		}
		
		private Set<String> getAuthors(RemoteLogOperation.LogEntryCache logEntryCache) {
			String[] paths = logEntryCache.getCachedFilePaths();
			Set<String> authors = new HashSet<>();
			for (String path : paths) {
				ILogEntry[] entries = logEntryCache.getLogEntries(path);
				for (ILogEntry entry : entries) {
					authors.add(entry.getAuthor());
				}
			}
			return authors;
		}

		public Set<String> getAuthors() {
			return authors;
		}
	}
	
	/**
	 * Constructor for CVSProviderTest
	 */
	public FindCommittersTest() {
		super();
	}

	/**
	 * Constructor for CVSProviderTest
	 */
	public FindCommittersTest(String name) {
		super(name);
	}

	public static Test suite() {
		return new TestSuite(FindCommittersTest.class);
	}
	
	public void testFetchLogs() throws CVSException, InvocationTargetException, InterruptedException {
		CVSRepositoryLocation location = CVSRepositoryLocation.fromString(":pserver:anonymous@dev.eclipse.org:/cvsroot/eclipse");
		ICVSRemoteResource[] members = location.members(null, false, DEFAULT_MONITOR);
		Set<String> authors = fetchLogs(members);
		for (String name : authors) {
			System.out.println(name);
		}
	}

	private Set<String> fetchLogs(
			ICVSRemoteResource[] members) throws InvocationTargetException,
			InterruptedException {
		CVSTag tag1 = new CVSTag(new GregorianCalendar(2001, 10, 7).getTime());
		CVSTag tag2 = new CVSTag(new Date());
		RemoteLogOperation.LogEntryCache logEntryCache = new RemoteLogOperation.LogEntryCache();
		FindCommittersOperation op = new FindCommittersOperation(null, members, tag1, tag2, logEntryCache);
		op.run(DEFAULT_MONITOR);
		return op.getAuthors();
	}
}
