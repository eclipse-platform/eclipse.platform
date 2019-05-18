/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
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
package org.eclipse.team.tests.ccvs.core.cvsresources;
import java.util.Date;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.team.internal.ccvs.core.CVSException;
import org.eclipse.team.internal.ccvs.core.connection.CVSRepositoryLocation;
import org.eclipse.team.internal.ccvs.core.syncinfo.FolderSyncInfo;
import org.eclipse.team.internal.ccvs.core.syncinfo.ResourceSyncInfo;
import org.eclipse.team.tests.ccvs.core.CVSTestSetup;
import org.eclipse.team.tests.ccvs.core.EclipseTest;

public class ResourceSyncInfoTest extends EclipseTest {

	public ResourceSyncInfoTest() {
		super();
	}
	
	public ResourceSyncInfoTest(String name) {
		super(name);
	}
	
	public static Test suite() {
		TestSuite suite = new TestSuite(ResourceSyncInfoTest.class);
		return new CVSTestSetup(suite);
	}
		
	public void testEntryLineParsing() {
		
		// testing malformed entry lines first
		try {
			new ResourceSyncInfo("//////", null);			
			fail();
		} catch(CVSException e) {
			// Error expected
		}
		try {
			new ResourceSyncInfo("//1.1///", null);			
			fail();
		} catch(CVSException e) {
			// Error expected
		}
		try {
			new ResourceSyncInfo("/file.txt////", null);			
			fail();
		} catch(CVSException e) {
			// Error expected
		}
		try {
			new ResourceSyncInfo("/file.txt//////////", null);			
			fail();
		} catch(CVSException e) {
			// Error expected
		}
	}
	
	public void testEntryLineConstructor() throws CVSException {		
		ResourceSyncInfo info;
		info = new ResourceSyncInfo("/file.java/-1.1/Mon Feb 25 21:44:02 2002/-k/", null);
		assertTrue(info.isDeleted());
		
		info = new ResourceSyncInfo("/file.java/0/something/-k/", null);
		assertTrue(info.isAdded());
		
		info = new ResourceSyncInfo("/file.java/1.0/Mon Feb 25 21:44:02 2002/-k/Tv1", null);
		assertTrue(info.getTag() != null);
		
		Date timestamp = new Date(123000);
		info = new ResourceSyncInfo("/file.java/1.0/Mon Feb 25 21:44:02 2002/-k/Tv1", timestamp);
		assertTrue(info.getTimeStamp().equals(timestamp));
		
		info = new ResourceSyncInfo("/file.java/0/Mon Feb 25 21:44:02 2002/-k/", timestamp);
		assertTrue(info.getTimeStamp().equals(timestamp));
		
		info = new ResourceSyncInfo("D/file.java////", null);
		assertTrue(info.isDirectory());
	}
	
	public void testConstructor() throws CVSException {
		ResourceSyncInfo info;
		
		info = new ResourceSyncInfo("folder");
		assertTrue(info.isDirectory());
		
		info = new ResourceSyncInfo("/file.java/-2.34/Mon Feb 25 21:44:02 2002/-k/Tv1", null);
		assertTrue(info.isDeleted());
		assertTrue(info.getRevision().equals("2.34"));
		
		info = new ResourceSyncInfo("/file.java/0/Mon Feb 25 21:44:02 2002/-k/Tv1", null);
		assertTrue(info.isAdded());
	}
	
	public void testMergeTimestamps() throws CVSException {
		ResourceSyncInfo info, info2;
		Date timestamp = new Date(123000);
		Date timestamp2 = new Date(654000);
				
		info = new ResourceSyncInfo("/file.java/1.1//-kb/", timestamp);
		assertTrue(!info.isMerged());
		assertTrue(!info.isNeedsMerge(timestamp));		
		
		// test merged entry lines the server and ensure that their entry line format is compatible
		info = new ResourceSyncInfo("/file.java/1.1/+=/-kb/", timestamp);
		String entryLine = info.getEntryLine();
		info2 = new ResourceSyncInfo(entryLine, null);
		assertTrue(info.isMerged() && info2.isMerged());
		assertTrue(info.isNeedsMerge(timestamp) && info2.isNeedsMerge(timestamp));
		assertTrue(!info.isNeedsMerge(timestamp2) && !info2.isNeedsMerge(timestamp2));
		assertTrue(info.getTimeStamp().equals(timestamp) && info2.getTimeStamp().equals(timestamp));		

		info = new ResourceSyncInfo("/file.java/1.1/+modified/-kb/", null);
		entryLine = info.getEntryLine();
		info2 = new ResourceSyncInfo(entryLine, null);	
		assertTrue(info.isMerged() && info2.isMerged());
		assertTrue(!info.isNeedsMerge(timestamp) && !info2.isNeedsMerge(timestamp));
		assertTrue(!info.isNeedsMerge(timestamp2) && !info2.isNeedsMerge(timestamp2));
		assertTrue(info.getTimeStamp()==null && info2.getTimeStamp()==null);
	}
	
	public void testTimestampCompatibility() throws CVSException, CoreException {
		String entryLine1 = "/a.bin/1.1/Mon Feb  9 21:44:02 2002/-kb/";
		String entryLine2 = "/a.bin/1.1/Mon Feb 9 21:44:02 2002/-kb/";
		String entryLine3 = "/a.bin/1.1/Mon Feb 09 21:44:02 2002/-kb/";		
		ResourceSyncInfo info1 = new ResourceSyncInfo(entryLine1, null);
		ResourceSyncInfo info2 = new ResourceSyncInfo(entryLine2, null);
		ResourceSyncInfo info3 = new ResourceSyncInfo(entryLine3, null);
		Date date1 = info1.getTimeStamp();
		Date date2 = info2.getTimeStamp();
		Date date3 = info3.getTimeStamp();
		assertTrue(date1.equals(date2));
		assertTrue(date1.equals(date3));
		assertTrue(date2.equals(date3));
	}
	
	public void testRevisionComparison() {
		assertTrue(ResourceSyncInfo.isLaterRevision("1.9", "1.8"));
		assertTrue( ! ResourceSyncInfo.isLaterRevision("1.8", "1.8"));
		assertTrue( ! ResourceSyncInfo.isLaterRevision("1.8", "1.9"));
		
		assertTrue(ResourceSyncInfo.isLaterRevision("1.8.1.2", "1.8"));
		assertTrue( ! ResourceSyncInfo.isLaterRevision("1.8", "1.8.1.2"));
		assertTrue( ! ResourceSyncInfo.isLaterRevision("1.8.1.2", "1.7"));
		
		assertTrue( ! ResourceSyncInfo.isLaterRevision("0", "1.1"));
		assertTrue(ResourceSyncInfo.isLaterRevision("1.1", "0"));
	}
	
	public void testRevisionOnBranchComparison() throws CVSException {
		ResourceSyncInfo syncInfo1 = new ResourceSyncInfo("/name/1.5/dummy timestamp//", null);
		ResourceSyncInfo syncInfo2 = new ResourceSyncInfo("/name/1.4/dummy timestamp//", null);
		
		ResourceSyncInfo syncInfo3 = new ResourceSyncInfo("/name/1.4.1.2/dummy timestamp//Nb1", null);
		ResourceSyncInfo syncInfo4 = new ResourceSyncInfo("/name/1.4/dummy timestamp//Nb1", null);
		
		ResourceSyncInfo syncInfo5 = new ResourceSyncInfo("/name/1.4.1.2/dummy timestamp//Tv1", null);
		
		assertTrue(ResourceSyncInfo.isLaterRevisionOnSameBranch(syncInfo1.getBytes(), syncInfo2.getBytes()));
		assertTrue( ! ResourceSyncInfo.isLaterRevisionOnSameBranch(syncInfo2.getBytes(), syncInfo1.getBytes()));
		assertTrue( ! ResourceSyncInfo.isLaterRevisionOnSameBranch(syncInfo1.getBytes(), syncInfo1.getBytes()));
		
		assertTrue(ResourceSyncInfo.isLaterRevisionOnSameBranch(syncInfo3.getBytes(), syncInfo4.getBytes()));
		assertTrue( ! ResourceSyncInfo.isLaterRevisionOnSameBranch(syncInfo4.getBytes(), syncInfo3.getBytes()));
		assertTrue( ! ResourceSyncInfo.isLaterRevisionOnSameBranch(syncInfo4.getBytes(), syncInfo4.getBytes()));
		
		assertTrue( ! ResourceSyncInfo.isLaterRevisionOnSameBranch(syncInfo5.getBytes(), syncInfo4.getBytes()));
		assertTrue( ! ResourceSyncInfo.isLaterRevisionOnSameBranch(syncInfo4.getBytes(), syncInfo5.getBytes()));
		assertTrue( ! ResourceSyncInfo.isLaterRevisionOnSameBranch(syncInfo5.getBytes(), syncInfo5.getBytes()));
	}
	
	public void testRepositoryLocationFormats() throws CVSException {
		assertPathCorrect(CVSRepositoryLocation.fromString(":pserver:user@host:/home/path"), "/home/path");
		assertPathCorrect(CVSRepositoryLocation.fromString(":pserver:user:password@host:/home/path"), "/home/path");
		assertPathCorrect(CVSRepositoryLocation.fromString(":pserver:host:/home/path"), "/home/path");
		assertPathCorrect(CVSRepositoryLocation.fromString(":pserver:user@host:1234/home/path"), "/home/path");
		assertPathCorrect(CVSRepositoryLocation.fromString(":pserver:user:password@host:1234/home/path"), "/home/path");
		assertPathCorrect(CVSRepositoryLocation.fromString(":pserver:host:1234/home/path"), "/home/path");
		assertPathCorrect(CVSRepositoryLocation.fromString(":pserver:user@host/home/path"), "/home/path");
		assertPathCorrect(CVSRepositoryLocation.fromString(":pserver:user:password@host/home/path"), "/home/path");
		assertPathCorrect(CVSRepositoryLocation.fromString(":pserver:host/home/path"), "/home/path");
		assertPathCorrect(CVSRepositoryLocation.fromString(":pserver:user@domain:password@host/home/path"), "/home/path");
	}

	private void assertPathCorrect(CVSRepositoryLocation location, String string) throws CVSException {
		assertEquals(location.getRootDirectory(), string);
		FolderSyncInfo info = new FolderSyncInfo("childPath", location.getLocation(), null, false);
		assertEquals(info.getRemoteLocation(), string + '/' + "childPath");
		
	}
}
