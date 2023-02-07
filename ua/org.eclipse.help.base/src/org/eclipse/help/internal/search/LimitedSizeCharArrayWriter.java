/*******************************************************************************
 * Copyright (c) 2009, 2015 IBM Corporation and others.
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

package org.eclipse.help.internal.search;

import java.io.CharArrayWriter;
import java.io.IOException;

public class LimitedSizeCharArrayWriter extends CharArrayWriter {

	private long maxSize;
	private long size = 0;

	public LimitedSizeCharArrayWriter(long maxSize) {
		super();
		this.maxSize = maxSize;
	}

	@Override
	public void write(char[] c, int off, int len) {
		size += len;
		super.write(c, off, len);
	}

	@Override
	public void write(char[] cbuf) throws IOException {
		size += cbuf.length;
		if (size < maxSize) {
			super.write(cbuf);
		}
	}

	@Override
	public void write(String str, int off, int len) {
		size += len;
		if (size < maxSize) {
			super.write(str, off, len);
		}
	}

	@Override
	public void write(int c) {
		size += 1;
		if (size < maxSize) {
			super.write(c);
		}
	}

	@Override
	public void write(String str) throws IOException {
		size += str.length();
		if (size < maxSize) {
			super.write(str);
		}
	}

}
