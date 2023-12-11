/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
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
package org.eclipse.core.tests.internal.localstore;

import static org.eclipse.core.tests.harness.FileSystemHelper.getRandomLocation;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createRandomString;
import static org.eclipse.core.tests.resources.ResourceTestUtil.removeFromFileSystem;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.core.internal.localstore.ILocalStoreConstants;
import org.eclipse.core.internal.localstore.SafeChunkyInputStream;
import org.eclipse.core.internal.localstore.SafeChunkyOutputStream;
import org.eclipse.core.internal.resources.Workspace;
import org.eclipse.core.tests.resources.WorkspaceTestRule;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class SafeChunkyInputOutputStreamTest {

	@Rule
	public WorkspaceTestRule workspaceRule = new WorkspaceTestRule();

	private File temp;

	private List<SafeChunkyOutputStream> streams;

	private File target;

	private byte[] merge(byte[] b1, byte[] b2) {
		byte[] result = new byte[b1.length + b2.length];
		System.arraycopy(b1, 0, result, 0, b1.length);
		System.arraycopy(b2, 0, result, b1.length, b2.length);
		return result;
	}

	@Before
	public void setUp() throws Exception {
		streams = new ArrayList<>();
		temp = getRandomLocation().append("temp").toFile();
		temp.mkdirs();
		assertTrue("could not create temp directory", temp.isDirectory());
		target = new File(temp, "target");
	}

	@After
	public void tearDown() throws Exception {
		for (SafeChunkyOutputStream stream : streams) {
			try {
				stream.close();
			} catch (Exception e) {
				// ignore
			}
		}
		removeFromFileSystem(temp.getParentFile());
	}

	@Test
	public void testBufferLimit() throws Exception {
		Workspace.clear(target); // make sure there was nothing here before
		assertTrue(!target.exists());

		// use only one chunk but bigger than the buffer
		int bufferSize = 10024;
		byte[] chunk = getBigContents(bufferSize);
		try (SafeChunkyOutputStream output = new SafeChunkyOutputStream(target)) {
			output.write(chunk);
			output.succeed();
		}

		// read chunks
		try (SafeChunkyInputStream input = new SafeChunkyInputStream(target)) {
			byte[] read = new byte[chunk.length];
			assertEquals(chunk.length, input.read(read));
			assertThat(read, is(chunk));
		}
		Workspace.clear(target); // make sure there was nothing here before
	}

	/**
	 * The returned array will have at least the specified size.
	 */
	private byte[] getBigContents(int size) {
		StringBuilder sb = new StringBuilder();
		while (sb.length() < size) {
			sb.append(createRandomString());
		}
		return sb.toString().getBytes();
	}

	public void testFailure() throws Exception {
		Workspace.clear(target); // make sure there was nothing here before
		assertFalse(target.exists());

		// misc
		byte[] fakeEnd = new byte[ILocalStoreConstants.END_CHUNK.length];
		System.arraycopy(ILocalStoreConstants.END_CHUNK, 0, fakeEnd, 0, ILocalStoreConstants.END_CHUNK.length);
		fakeEnd[fakeEnd.length - 1] = 86;

		// write chunks
		byte[] chunk1 = createRandomString().getBytes();
		byte[] chunk2 = createRandomString().getBytes();
		byte[] chunk3 = createRandomString().getBytes();
		byte[] chunk4 = createRandomString().getBytes();
		byte[] chunk5 = createRandomString().getBytes();
		byte[] chunk6 = createRandomString().getBytes();
		SafeChunkyOutputStream output = new SafeChunkyOutputStream(target);
		try {
			output.write(chunk1);
			output.succeed();
			doNothing(output);
			output = new SafeChunkyOutputStream(target);
			// fake failure
			output.write(chunk2);
			output.write(ILocalStoreConstants.BEGIN_CHUNK); // another begin
			output.succeed();
			//
			doNothing(output);
			output = new SafeChunkyOutputStream(target);
			output.write(chunk3);
			output.succeed();
			doNothing(output);
			output = new SafeChunkyOutputStream(target);
			// fake failure
			output.write(chunk4);
			output.write(ILocalStoreConstants.END_CHUNK); // another end
			output.succeed();
			doNothing(output);
			//
			output = new SafeChunkyOutputStream(target);
			output.write(chunk5);
			output.succeed();
			// fake failure
			output.write(fakeEnd);
			output.write(chunk6);
			output.succeed();
		} finally {
			output.close();
		}

		// read chunks
		try (SafeChunkyInputStream input = new SafeChunkyInputStream(target)) {
			byte[] read1 = new byte[chunk1.length];
			// byte[] read2 = new byte[chunk2.length];
			byte[] read3 = new byte[chunk3.length];
			byte[] read4 = new byte[chunk4.length];
			byte[] read5 = new byte[chunk5.length];
			byte[] read6 = new byte[fakeEnd.length + chunk6.length];
			assertEquals(chunk1.length, input.read(read1));
			// assert("3.1", input.read(read2) == chunk2.length);
			assertEquals(chunk3.length, input.read(read3));
			assertEquals(chunk4.length, input.read(read4));
			assertEquals(chunk5.length, input.read(read5));
			assertEquals((fakeEnd.length + chunk6.length), input.read(read6));
			assertThat(read1, is(chunk1));
			// assert("3.7", compare(chunk2, read2));
			assertThat(read3, is(chunk3));
			assertThat(read4, is(chunk4));
			assertThat(read5, is(chunk5));
			byte[] expected = merge(fakeEnd, chunk6);
			assertThat(read6, is(expected));
		}
	}

	/**
	 * This method is used to trick the java compiler to avoid reporting
	 * a warning that the stream was not closed. In this test we are intentionally
	 * not closing the stream to test recovery from failure.
	 */
	private void doNothing(SafeChunkyOutputStream output) {
		streams.add(output);
	}

	@Test
	public void testAlmostEmpty() throws Exception {
		Workspace.clear(target); // make sure there was nothing here before
		assertTrue(!target.exists());

		// open the file but don't write anything.
		try (SafeChunkyOutputStream output = new SafeChunkyOutputStream(target)) {
			output.close();
		}

		try (DataInputStream input = new DataInputStream(new SafeChunkyInputStream(target))) {
			assertThrows(EOFException.class, () -> input.readUTF());
		}
	}

	@Test
	public void testSimple() throws Exception {
		Workspace.clear(target); // make sure there was nothing here before
		assertFalse(target.exists());

		// write chunks
		byte[] chunk1 = createRandomString().getBytes();
		byte[] chunk2 = createRandomString().getBytes();
		byte[] chunk3 = createRandomString().getBytes();
		byte[] chunk4 = createRandomString().getBytes();
		byte[] chunk5 = createRandomString().getBytes();
		try (SafeChunkyOutputStream output = new SafeChunkyOutputStream(target)) {
			output.write(chunk1);
			output.succeed();
			output.write(chunk2);
			output.succeed();
			output.write(chunk3);
			output.succeed();
			output.write(chunk4);
			output.succeed();
			output.write(chunk5);
			output.succeed();
		}

		// read chunks
		try (SafeChunkyInputStream input = new SafeChunkyInputStream(target)) {
			byte[] read1 = new byte[chunk1.length];
			byte[] read2 = new byte[chunk2.length];
			byte[] read3 = new byte[chunk3.length];
			byte[] read4 = new byte[chunk4.length];
			byte[] read5 = new byte[chunk5.length];
			assertEquals(chunk1.length, input.read(read1));
			assertEquals(chunk2.length, input.read(read2));
			assertEquals(chunk3.length, input.read(read3));
			assertEquals(chunk4.length, input.read(read4));
			assertEquals(chunk5.length, input.read(read5));
			assertThat(read1, is(chunk1));
			assertThat(read2, is(chunk2));
			assertThat(read3, is(chunk3));
			assertThat(read4, is(chunk4));
			assertThat(read5, is(chunk5));
		} finally {
			Workspace.clear(target); // make sure there was nothing here before
		}
	}

}
