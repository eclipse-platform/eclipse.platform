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
import junit.framework.AssertionFailedError;

public class RemoteAssertionFailedError extends AssertionFailedError {
	/**
	 * All serializable objects should have a stable serialVersionUID
	 */
	private static final long serialVersionUID = 1L;

	private Object stackText;
	private String message;

	public RemoteAssertionFailedError(String message, String stackText) {
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
