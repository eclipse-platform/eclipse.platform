/*******************************************************************************
 * Copyright (c) 2011, 2015 IBM Corporation and others.
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

package org.eclipse.help.internal.base.remote;

import java.io.IOException;
import java.io.InputStream;

/**
 * This class is used to identify that an input stream comes from a remote server so appropriate css can
 * be generated.
 */

public class RemoteHelpInputStream extends InputStream{

	private InputStream is;

	public RemoteHelpInputStream(){
		is=null;
	}
	public RemoteHelpInputStream(InputStream is) {
		this.is=is;
	}

	public InputStream getInputStream(){
		return this.is;
	}

	@Override
	public int read() throws IOException {
		return is.read();
	}

	@Override
	public int available() throws IOException {
		return is.available();
	}

	@Override
	public void close() throws IOException {
		is.close();
	}

	@Override
	public synchronized void mark(int arg0) {
		is.mark(arg0);
	}

	@Override
	public boolean markSupported() {
		return is.markSupported();
	}

	@Override
	public int read(byte[] b) throws IOException {
		return is.read(b);
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		return is.read(b, off, len);
	}

	@Override
	public synchronized void reset() throws IOException {
		is.reset();
	}

	@Override
	public long skip(long n) throws IOException {
		return is.skip(n);
	}



}
