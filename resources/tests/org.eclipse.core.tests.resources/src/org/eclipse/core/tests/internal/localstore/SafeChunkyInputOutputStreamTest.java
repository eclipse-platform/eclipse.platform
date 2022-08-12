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

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.core.internal.localstore.*;
import org.eclipse.core.internal.resources.Workspace;
import org.eclipse.core.runtime.IPath;

public class SafeChunkyInputOutputStreamTest extends LocalStoreTest {
	protected File temp;

	private List<SafeChunkyOutputStream> streams;

	private File target;

	protected boolean compare(byte[] source, byte[] target1) {
		assertEquals(source.length, target1.length);
		for (int i = 0; i < target1.length; i++) {
			assertEquals(source[i], target1[i]);
		}
		return true;
	}

	protected byte[] merge(byte[] b1, byte[] b2) {
		byte[] result = new byte[b1.length + b2.length];
		System.arraycopy(b1, 0, result, 0, b1.length);
		System.arraycopy(b2, 0, result, b1.length, b2.length);
		return result;
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		streams = new ArrayList<>();

		IPath location = getRandomLocation();
		temp = location.append("temp").toFile();
		temp.mkdirs();
		assertTrue("could not create temp directory", temp.isDirectory());
		target = new File(temp, "target");
	}

	@Override
	protected void tearDown() throws Exception {
		for (SafeChunkyOutputStream stream : streams) {
			try {
				stream.close();
			} catch (Exception e) {
				// ignore
			}
		}
		ensureDoesNotExistInFileSystem(temp.getParentFile());
		super.tearDown();
	}

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
			assertTrue(compare(chunk, read));
		}
		Workspace.clear(target); // make sure there was nothing here before
	}

	public void testFailure() throws Exception {
		Workspace.clear(target); // make sure there was nothing here before
		assertTrue(!target.exists());

		// misc
		byte[] fakeEnd = new byte[ILocalStoreConstants.END_CHUNK.length];
		System.arraycopy(ILocalStoreConstants.END_CHUNK, 0, fakeEnd, 0, ILocalStoreConstants.END_CHUNK.length);
		fakeEnd[fakeEnd.length - 1] = 86;

		// write chunks
		byte[] chunk1 = getRandomString().getBytes();
		byte[] chunk2 = getRandomString().getBytes();
		byte[] chunk3 = getRandomString().getBytes();
		byte[] chunk4 = getRandomString().getBytes();
		byte[] chunk5 = getRandomString().getBytes();
		byte[] chunk6 = getRandomString().getBytes();
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
			assertTrue(compare(chunk1, read1));
			// assert("3.7", compare(chunk2, read2));
			assertTrue(compare(chunk3, read3));
			assertTrue(compare(chunk4, read4));
			assertTrue(compare(chunk5, read5));
			byte[] expected = merge(fakeEnd, chunk6);
			assertTrue(compare(expected, read6));
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

	public void testAlmostEmpty() throws Exception {
		Workspace.clear(target); // make sure there was nothing here before
		assertTrue(!target.exists());

		// open the file but don't write anything.
		try (SafeChunkyOutputStream output = new SafeChunkyOutputStream(target)) {
			output.close();
		}

		try (DataInputStream input = new DataInputStream(new SafeChunkyInputStream(target))) {
			input.readUTF();
			fail("Should throw EOFException");
		} catch (EOFException e) {
			// should hit here
		}
	}

	public void testSimple() throws Exception {
		Workspace.clear(target); // make sure there was nothing here before
		assertTrue(!target.exists());

		// write chunks
		byte[] chunk1 = getRandomString().getBytes();
		byte[] chunk2 = getRandomString().getBytes();
		byte[] chunk3 = getRandomString().getBytes();
		byte[] chunk4 = getRandomString().getBytes();
		byte[] chunk5 = getRandomString().getBytes();
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
			assertTrue(compare(chunk1, read1));
			assertTrue(compare(chunk2, read2));
			assertTrue(compare(chunk3, read3));
			assertTrue(compare(chunk4, read4));
			assertTrue(compare(chunk5, read5));
		} finally {
			Workspace.clear(target); // make sure there was nothing here before
		}
	}
}
