/*******************************************************************************
 * Copyright (c) 2004, 2005 IBM Corporation and others.
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
package org.eclipse.core.tools.metadata;

import java.io.*;

/**
 * Subclasses must provide a concrete
 * implementation for <code>getStringDumpingStrategy(InputStream)</code> method.
 */
public abstract class MultiStrategyDumper extends AbstractDumper {

	/**
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
	 * @param file the file to be dumped
	 * @return a dump object representing the contents of the dumped file
	 * @see org.eclipse.core.tools.metadata.IDumper#dump(java.io.File)
	 * @see #getStringDumpingStrategy(DataInputStream)
	 * @see IStringDumpingStrategy#dumpStringContents(DataInputStream)
	 * @see #openInputStream(File)
	 */
	@Override
	protected final void dumpContents(PushbackInputStream input, StringBuilder contents) throws IOException, Exception, DumpException {
		DataInputStream dataInput = new DataInputStream(input);
		int c;
		while ((c = input.read()) != -1) {
			input.unread(c);
			IStringDumpingStrategy strategy = getStringDumpingStrategy(dataInput);
			String dumpedContents = strategy.dumpStringContents(dataInput);
			if (dumpedContents == null)
				break;
			contents.append("Format: "); //$NON-NLS-1$
			contents.append(strategy.getFormatDescription());
			contents.append("\n\n"); //$NON-NLS-1$
			contents.append(dumpedContents);
			contents.append("\n"); //$NON-NLS-1$
		}
		if (contents.length() == 0)
			contents.append("No contents\n"); //$NON-NLS-1$
	}

	/**
	 * Returns a <code>IStringDumpingStrategy</code> object. Subclasses must provide
	 * a concrete implementation for this method. The input stream is connected to
	 * the file being dumped. If needed, implementations may consume the input
	 * stream in order to choose a strategy.
	 *
	 * @param input the input stream being read
	 * @return  a <code>IStringDumpingStrategy</code> object
	 * @throws Exception any exceptions occurred during input stream reading must
	 * NOT be caught
	 */
	protected abstract IStringDumpingStrategy getStringDumpingStrategy(DataInputStream input) throws Exception;

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
	@Override
	protected InputStream openInputStream(File file) throws IOException {
		return new FileInputStream(file);
	}

}
