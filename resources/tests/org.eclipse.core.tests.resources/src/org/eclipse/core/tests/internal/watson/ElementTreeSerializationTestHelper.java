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

import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.eclipse.core.internal.watson.ElementTreeReader;
import org.eclipse.core.internal.watson.ElementTreeWriter;
import org.eclipse.core.internal.watson.IElementInfoFlattener;
import org.eclipse.core.runtime.IPath;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;

public final class ElementTreeSerializationTestHelper implements ArgumentsProvider {

	private ElementTreeSerializationTestHelper() {
	}

	interface StreamReader {
		Object doRead(ElementTreeReader reader, DataInputStream input) throws IOException;
	}

	interface StreamWriter {
		void doWrite(ElementTreeWriter writer, DataOutputStream output) throws IOException;
	}

	private static class WriterThread implements Runnable {
		private final StreamWriter streamWriter;
		private DataOutputStream dataOutputStream;
		private ElementTreeWriter treeWriter;
		private volatile IOException exceptionDuringRun;

		WriterThread(StreamWriter streamWriter, DataOutputStream stream, ElementTreeWriter treeWriter) {
			this.streamWriter = streamWriter;
			this.dataOutputStream = stream;
			this.treeWriter = treeWriter;
		}

		public IOException getExceptionDuringRun() {
			return exceptionDuringRun;
		}

		@Override
		public void run() {
			try {
				streamWriter.doWrite(treeWriter, dataOutputStream);
				// inform reader, writing is finished:
				dataOutputStream.flush(); // DeltaChainFlatteningTest 30s -> 0s
			} catch (IOException e) {
				exceptionDuringRun = e;
			}
		}
	}

	private static class ReaderThread implements Runnable {
		private final StreamReader streamReader;
		private final DataInputStream dataInputStream;
		private final ElementTreeReader treeReader;
		private Object refried;
		private volatile IOException exceptionDuringRun;

		ReaderThread(StreamReader streamReader, DataInputStream stream, ElementTreeReader reader) {
			this.streamReader = streamReader;
			this.dataInputStream = stream;
			this.treeReader = reader;
		}

		public Object getReconstitutedObject() {
			return refried;
		}

		public IOException getExceptionDuringRun() {
			return exceptionDuringRun;
		}

		@Override
		public void run() {
			try {
				refried = streamReader.doRead(treeReader, dataInputStream);
			} catch (IOException e) {
				exceptionDuringRun = e;
			}
		}
	}

	/*
	 * Arguments for exhaustive test cases across the whole element tree
	 */
	@Override
	public Stream<Arguments> provideArguments(ExtensionContext context) {
		List<Arguments> arguments = new ArrayList<>();
		IPath[] paths = TestUtil.getTreePaths();
		int[] depths = getTreeDepths();

		for (IPath path : paths) {
			for (int depth : depths) {
				arguments.add(Arguments.of(path, depth));
			}
		}
		return arguments.stream();
	}

	/**
	 * Returns all the different possible depth values for this tree. To be
	 * conservative, it is okay if some of the returned depths are not possible.
	 */
	private static int[] getTreeDepths() {
		return new int[] { -1, 0, 1, 2, 3, 4 };
	}

	/**
	 * Write a flattened element tree to a file and read it back.
	 */
	public static Object doFileTest(Path tempDir, StreamReader reading, StreamWriter writing) throws IOException {
		IElementInfoFlattener fac = getFlattener();

		Object newTree = null;
		ElementTreeWriter writer = new ElementTreeWriter(fac);
		ElementTreeReader reader = new ElementTreeReader(fac);

		/* Write the element tree. */
		Files.createDirectories(tempDir);
		Path tempFile = tempDir.resolve("TestFlattening");
		try (OutputStream fos = Files.newOutputStream(tempFile)) {
			DataOutputStream dos = new DataOutputStream(fos);
			writing.doWrite(writer, dos);
		}

		/* Read the element tree. */
		try (InputStream fis = Files.newInputStream(tempFile); DataInputStream dis = new DataInputStream(fis)) {
			newTree = reading.doRead(reader, dis);
		}
		return newTree;
	}

	/** Pipe a flattened element tree from writer to reader threads.
	 */
	public static Object doPipeTest(StreamWriter streamWriter, StreamReader streamReader) throws IOException {
		IElementInfoFlattener fac = getFlattener();
		Object refried = null;
		ElementTreeWriter w = new ElementTreeWriter(fac);
		ElementTreeReader r = new ElementTreeReader(fac);

		/* Pipe the element tree from writer to reader threads. */
		try (PipedOutputStream pout = new PipedOutputStream();
				PipedInputStream pin = new PipedInputStream(pout);
				DataOutputStream oos = new DataOutputStream(pout); // new FileOutputStream(FILE_NAME));
				DataInputStream ois = new DataInputStream(pin)) {
			WriterThread writerThread = new WriterThread(streamWriter, oos, w);
			ReaderThread readerThread = new ReaderThread(streamReader, ois, r);
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
			assertNull(writerThread.getExceptionDuringRun(), "exception occurred during write");
			assertNull(readerThread.getExceptionDuringRun(), "exception occurred during read");
			refried = readerThread.getReconstitutedObject();
		}
		return refried;
	}


	/**
	 * Tests the reading and writing of element deltas
	 */
	private static IElementInfoFlattener getFlattener() {
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

}
