/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
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

import java.text.ParseException;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.team.internal.ccvs.core.CVSException;
import org.eclipse.team.internal.ccvs.core.CVSProviderPlugin;
import org.eclipse.team.internal.ccvs.core.ICVSRepositoryLocation;
import org.eclipse.team.internal.ccvs.core.client.Session;
import org.eclipse.team.internal.ccvs.core.resources.CVSWorkspaceRoot;
import org.eclipse.team.internal.ccvs.core.syncinfo.ResourceSyncInfo;
import org.eclipse.team.internal.ccvs.core.util.*;
import org.eclipse.team.internal.ccvs.core.util.CVSDateFormatter;
import org.eclipse.team.internal.ccvs.core.util.Util;
import org.eclipse.team.tests.ccvs.core.CVSTestSetup;
import org.eclipse.team.tests.ccvs.core.EclipseTest;
import org.eclipse.team.tests.ccvs.core.TestConnection;

public class ResourceSyncBytesTest extends EclipseTest {

	public ResourceSyncBytesTest() {
		super();
	}

	public ResourceSyncBytesTest(String name) {
		super(name);
	}

	public static Test suite() {
		TestSuite suite = new TestSuite(ResourceSyncBytesTest.class);
		return new CVSTestSetup(suite);
	}
	
	/**
	 * Convert the input to bytes and get the bytes in the given slot delimited by slash (/).
	 * Only retieve the bytes in the given slot and not the rest.
	 * @param input
	 * @param slot
	 * @return
	 */
	private byte[] getBytesForSlot(String input, int slot) {
		return getBytesForSlot(input, slot, false /* include rest */);
	}
	
	/**
	 * Convert the input to bytes and get the bytes in the given slot delimited by slash (/).
	 * @param input
	 * @param slot
	 * @return
	 */
	private byte[] getBytesForSlot(String input, int slot, boolean includeRest) {
		byte[] result = Util.getBytesForSlot(input.getBytes(), (byte) '/', slot, includeRest);
		return result;
	}
	
	private void assertEqualBytes(String expected, byte[] actual) {
		assertEquals(expected, new String(actual));
	}
	
	public void testUtilGetBytesForSlot() {
		// test success cases
		String input = "zero/one/two";
		assertEqualBytes("zero", getBytesForSlot(input, 0));
		assertEqualBytes("one", getBytesForSlot(input, 1));
		assertEqualBytes("two", getBytesForSlot(input, 2));
		assertEqualBytes("one/two", getBytesForSlot(input, 1, true /* include rest */));
		assertEqualBytes("", getBytesForSlot("///", 0));
		assertEqualBytes("", getBytesForSlot("///", 1));
		assertEqualBytes("", getBytesForSlot("///", 2));
		assertEqualBytes("/", getBytesForSlot("///", 2, true /* include rest */));
		
		// test failure cases
		input = "zero/one/two";
		assertNull(getBytesForSlot(input, 3));
		assertNull(getBytesForSlot(input, 4));
		assertNull(getBytesForSlot(input, -1));
	}

	public void testSendEntry() throws CVSException, ParseException {
		ICVSRepositoryLocation location = KnownRepositories.getInstance().getRepository(":test:user:password@host:/path");
		// disable version detemrination to reduce traffic
		CVSProviderPlugin.getPlugin().setDetermineVersionEnabled(false);
		// create and open a session
		Session session = new Session(location, CVSWorkspaceRoot.getCVSFolderFor(ResourcesPlugin.getWorkspace().getRoot()));
		session.open(DEFAULT_MONITOR, false /* read-only */);
		
		// test a normal entry line
		byte[] entryLine = "/plugin.xml/1.27/Tue Mar  4 19:47:36 2003/-ko/".getBytes();
		session.sendEntry(entryLine, ResourceSyncInfo.getTimestampToServer(entryLine, CVSDateFormatter.entryLineToDate("Tue Mar  4 19:47:36 2003")));
		assertEquals("Entry /plugin.xml/1.27//-ko/", TestConnection.getLastLine());
		
		// test a server merged with conflict entry line
		entryLine = "/newfile.txt/1.10/Result of merge+Thu Mar 20 16:36:56 2003//".getBytes();
		session.sendEntry(entryLine, ResourceSyncInfo.getTimestampToServer(entryLine, CVSDateFormatter.entryLineToDate("Thu Mar 20 16:36:56 2003")));
		assertEquals("Entry /newfile.txt/1.10/+=//", TestConnection.getLastLine());
		
		// test a server merged entry line
		entryLine = "/newfile.txt/1.10/Result of merge+Thu Mar 20 16:36:56 2003//".getBytes();
		session.sendEntry(entryLine, ResourceSyncInfo.getTimestampToServer(entryLine, CVSDateFormatter.entryLineToDate("Thu Mar 20 16:37:56 2003")));
		assertEquals("Entry /newfile.txt/1.10/+modified//", TestConnection.getLastLine());
		
		// test added entry line
		entryLine = "/plugin.xml/0/dummy timestamp/-ko/".getBytes();
		session.sendEntry(entryLine, ResourceSyncInfo.getTimestampToServer(entryLine, CVSDateFormatter.entryLineToDate("Tue Mar  4 19:47:36 2003")));
		assertEquals("Entry /plugin.xml/0//-ko/", TestConnection.getLastLine());
		
		// test empty timestamp entry line
		entryLine = "/plugin.xml/1.1//-ko/".getBytes();
		session.sendEntry(entryLine, ResourceSyncInfo.getTimestampToServer(entryLine, CVSDateFormatter.entryLineToDate("Tue Mar  4 19:47:36 2003")));
		assertEquals("Entry /plugin.xml/1.1//-ko/", TestConnection.getLastLine());
		
	}

}
