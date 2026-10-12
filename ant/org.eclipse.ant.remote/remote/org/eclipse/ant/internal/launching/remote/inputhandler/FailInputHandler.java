/*******************************************************************************
 * Copyright (c) 2005, 2009 IBM Corporation and others.
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

package org.eclipse.ant.internal.launching.remote.inputhandler;

import java.io.IOException;
import java.io.InputStream;

import org.apache.tools.ant.input.DefaultInputHandler;

public class FailInputHandler extends DefaultInputHandler {

	@Override
	protected InputStream getInputStream() {
		// ensure any attempts to read input fail
		return new InputStream() {
			@Override
			public int read() throws IOException {
				throw new IOException();
			}
		};
	}
}
