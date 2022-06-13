/*******************************************************************************
 * Copyright (c) 2004, 2015 IBM Corporation and others.
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
package org.eclipse.core.tests.session;

import java.io.PrintStream;
import java.io.PrintWriter;

public class RemoteTestException extends Exception {
	/**
	 * All serializable objects should have a stable serialVersionUID
	 */
	private static final long serialVersionUID = 1L;

	private String stackText;
	private String message;

	public RemoteTestException(String message, String stackText) {
		this.message = message;
		this.stackText = stackText;
	}

	@Override
	public void printStackTrace(PrintWriter stream) {
		stream.print(stackText);
	}

	@Override
	public void printStackTrace(PrintStream stream) {
		stream.print(stackText);
	}

	@Override
	public String getMessage() {
		return message;
	}
}
