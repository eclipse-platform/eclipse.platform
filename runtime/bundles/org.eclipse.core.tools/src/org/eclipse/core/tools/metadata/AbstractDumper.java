/*******************************************************************************
 * Copyright (c) 2002, 2007 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM - Initial API and implementation
 *******************************************************************************/
package org.eclipse.core.tools.metadata;

import java.io.*;

/**
 * An abstract implementation for dumpers that generate bare text dumps by
 * sequentially reading input streams.
 */
public abstract class AbstractDumper implements IDumper {

	/**
	 * Reads a given file and produces a dump object. Provides a template implementation for
	 * <code>IDumper.dump(java.io.File)</code>.
	 * <p>Subclasses must implement
	 * <code>getStringDumpingStrategy(InputStream)</code> in order to select the
	 * real dumping behaviour. This method will call
	 * <code>IStringDumpingStrategy#dumpStringContents(DataInputStream)</code> on
	 * the returned strategy. If, after calling that method,  there are still bytes
	 * to be read in the input stream,
	 * <code>getStringDumpingStrategy(InputStream)</code> will be called again in
	 * order to select another strategy to read the remaining contents, and so on.
	 * </p>
	 *
	 * <p>Subclasses can also select which type of low-level InputStream will be
	 * used to read the file to be dumped by overriding
	 * <code>openInputStream(File)</code>.</p>
	 *
	 * @param file the file to be dumped
	 * @return a dump object representing the contents of the dumped file
	 * @see org.eclipse.core.tools.metadata.IDumper#dump(java.io.File)
	 * @see IStringDumpingStrategy#dumpStringContents(DataInputStream)
	 * @see #openInputStream(File)
	 */
	@Override
	public IDump dump(File file) {
		MeteredInputStream meteredInput = null;
		Dump dump = new Dump();
		dump.setFile(file);
		StringBuilder contents = new StringBuilder(40);
		try {
			//uses a metered input stream in order to count the number of bytes read
			meteredInput = new MeteredInputStream(openInputStream(file));
			dumpContents(meteredInput, contents);
		} catch (PartialDumpException pde) {
			// ensure we remember any partial contents
			if (pde.getPartialContents() != null)
				contents.append(pde.getPartialContents());
			dump.setFailureReason(pde);
		} catch (Exception e) {
			dump.setFailureReason(e);
		}
		dump.setContents(contents.toString());
		if (meteredInput != null)
			dump.setOffset(meteredInput.getOffset());
		return dump;
	}

	/**
	 * Does the actual  work. Subclasses must implement this method to define
	 * dumping behavior. The results are added to the <code>contents</code>
	 * string buffer.
	 *
	 * @param input
	 * @param contents
	 * @throws IOException
	 * @throws Exception
	 * @throws DumpException
	 */
	protected abstract void dumpContents(PushbackInputStream input, StringBuilder contents)
			throws IOException, Exception, DumpException;

	/**
	 * Opens an input stream connected to the file object provided. Provides an
	 * opportunity for subclasses to select a different input stream class. By
	 * default, returns a <code>FileInputStream</code> object.
	 *
	 * @param file the file to be opened
	 * @return an input stream connected to the file provided as
	 * argument
	 * @throws IOException if an exception happens while opening the inpuut stream
	 */
	protected InputStream openInputStream(File file) throws IOException {
		return new FileInputStream(file);
	}

}
