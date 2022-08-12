/*******************************************************************************
 * Copyright (c) 2000, 2017 IBM Corporation and others.
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
package org.eclipse.core.tests.internal.watson;

import static org.junit.Assert.fail;

import java.io.*;
import org.eclipse.core.internal.watson.*;
import org.eclipse.core.runtime.IPath;
import org.junit.After;
import org.junit.Before;

public abstract class ElementTreeSerializationTest implements IPathConstants {

	protected ElementTree fTree;

	String root = System.getProperty("java.io.tmpdir");
	protected File tempFile = new File(root + "/temp/TestFlattening");

	class WriterThread implements Runnable {
		DataOutputStream fDataOutputStream;
		ElementTreeWriter fWriter;

		public void setStream(DataOutputStream stream) {
			fDataOutputStream = stream;
		}

		public void setWriter(ElementTreeWriter writer) {
			fWriter = writer;
		}

		@Override
		public void run() {
			try {
				doWrite(fWriter, fDataOutputStream);
				// inform reader, writing is finished:
				fDataOutputStream.flush(); // DeltaChainFlatteningTest 30s -> 0s
			} catch (IOException e) {
				e.printStackTrace();
				fail("Error writing delta");
			}
		}
	}

	class ReaderThread implements Runnable {
		DataInputStream fDataInputStream;
		ElementTreeReader fReader;
		Object fRefried;

		public void setStream(DataInputStream stream) {
			fDataInputStream = stream;
		}

		public void setReader(ElementTreeReader reader) {
			fReader = reader;
		}

		public Object getReconstitutedObject() {
			return fRefried;
		}

		@Override
		public void run() {
			try {
				fRefried = doRead(fReader, fDataInputStream);
			} catch (IOException e) {
				e.printStackTrace();
				fail("Error reading delta");
			}
		}
	}

	WriterThread writerThread = new WriterThread();
	ReaderThread readerThread = new ReaderThread();

	/**
	 * The subtree to write
	 */
	protected IPath fSubtreePath;

	/**
	 * The depth of the tree to write.
	 */
	protected int fDepth;

	/**
	 * Performs exhaustive tests for all subtrees and all depths
	 */
	protected void doExhaustiveTests() {
		IPath[] paths = TestUtil.getTreePaths();
		int[] depths = getTreeDepths();

		for (IPath path : paths) {
			for (int depth : depths) {
				doTest(path, depth);
			}
		}
	}

	/**
	 * Write a flattened element tree to a file and read it back.
	 */
	public Object doFileTest() {
		IElementInfoFlattener fac = getFlattener();

		Object newTree = null;
		ElementTreeWriter writer = new ElementTreeWriter(fac);
		ElementTreeReader reader = new ElementTreeReader(fac);
		FileOutputStream fos = null;
		FileInputStream fis = null;
		DataOutputStream dos = null;
		DataInputStream dis = null;

		/* Write the element tree. */
		try {
			File dir = tempFile.getParentFile();
			dir.mkdirs();
			fos = new FileOutputStream(tempFile);
			dos = new DataOutputStream(fos);
		} catch (IOException e) {
			e.printStackTrace();
			fail("Unable to open ouput file");
		}
		try {
			doWrite(writer, dos);
		} catch (IOException e) {
			e.printStackTrace();
			fail("Error writing tree to file");
		} finally {
			try {
				dos.flush();
				fos.close();
			} catch (IOException e) {
				e.printStackTrace();
				fail("Unable to close output file!");
			}
		}

		/* Read the element tree. */
		try {
			fis = new FileInputStream(tempFile);
			dis = new DataInputStream(fis);
		} catch (IOException e) {
			e.printStackTrace();
			fail("Unable to open input file");
		}
		try {
			newTree = doRead(reader, dis);
		} catch (IOException e) {
			e.printStackTrace();
			fail("Error reading tree from file");
		} finally {
			try {
				fis.close();
			} catch (IOException e) {
				e.printStackTrace();
				fail("Unable to close input file!");
			}
		}
		return newTree;
	}

	/** Pipe a flattened element tree from writer to reader threads.
	 */
	public Object doPipeTest() {
		IElementInfoFlattener fac = getFlattener();
		Object refried = null;
		ElementTreeWriter w = new ElementTreeWriter(fac);
		ElementTreeReader r = new ElementTreeReader(fac);
		PipedOutputStream pout;
		PipedInputStream pin;
		DataOutputStream oos;
		DataInputStream ois;

		/* Pipe the element tree from writer to reader threads. */
		try {
			pout = new PipedOutputStream();
			pin = new PipedInputStream(pout);
			oos = new DataOutputStream(pout); //new FileOutputStream(FILE_NAME));
			ois = new DataInputStream(pin);
			writerThread.setStream(oos);
			readerThread.setStream(ois);
			writerThread.setWriter(w);
			readerThread.setReader(r);
			Thread thread1 = new Thread(writerThread, "testwriter");
			Thread thread2 = new Thread(readerThread, "testreader");
			thread1.start();
			thread2.start();
			while (thread2.isAlive()) {
				try {
					thread2.join();
				} catch (InterruptedException excp) {
				}
			}
			refried = readerThread.getReconstitutedObject();
			ois.close();
			oos.close();
		} catch (IOException e) {
			e.printStackTrace();
			fail("Unable to open stream:");
		}
		return refried;
	}

	/**
	 * Performs the serialization activity for this test
	 */
	public abstract Object doRead(ElementTreeReader reader, DataInputStream input) throws IOException;

	/**
	 * Runs a test for this class at a certain depth and path
	 */
	public abstract void doTest(IPath path, int depth);

	/**
	 * Performs the serialization activity for this test
	 */
	public abstract void doWrite(ElementTreeWriter writer, DataOutputStream output) throws IOException;

	/**
	 * Tests the reading and writing of element deltas
	 */
	public IElementInfoFlattener getFlattener() {
		return new IElementInfoFlattener() {
			@Override
			public void writeElement(IPath path, Object data, DataOutput output) throws IOException {
				if (data == null) {
					output.writeUTF("null");
				} else {
					output.writeUTF((String) data);
				}
			}

			@Override
			public Object readElement(IPath path, DataInput input) throws IOException {
				String data = input.readUTF();
				if ("null".equals(data)) {
					return null;
				}
				return data;
			}
		};
	}

	/**
	 * Returns all the different possible depth values for this tree.
	 * To be conservative, it is okay if some of the returned depths
	 * are not possible.
	 */
	protected int[] getTreeDepths() {
		return new int[] {-1, 0, 1, 2, 3, 4};
	}

	@Before
	public void setUp() throws Exception {
		fTree = TestUtil.createTestElementTree();
		/* default subtree we are interested in */
		fSubtreePath = solution;

		/* default depth is the whole tree */
		fDepth = ElementTreeWriter.D_INFINITE;
	}

	@After
	public void tearDown() throws Exception {
		//ElementTree tests don't use the CoreTest infrastructure
		tempFile.delete();
	}
}
