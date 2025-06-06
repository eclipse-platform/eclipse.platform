/*******************************************************************************
 * Copyright (c) 2002, 2005 Object Factory Inc.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *		Object Factory Inc. - Initial implementation
 *		IBM Corporation - serial version id
 *******************************************************************************/
package org.eclipse.ant.internal.ui.dtd;

/**
 * @author Bob Foster
 */
public class ParseError extends Exception {

	private static final long serialVersionUID = 1L;

	/**
	 * Constructor for ParseError.
	 *
	 * @param msg
	 *            Message
	 */
	public ParseError(String msg) {
		super(msg);
	}

	/**
	 * Constructor for ParseError.
	 *
	 * @param msg
	 *            Message
	 * @param cause
	 *            Cause
	 */
	public ParseError(String msg, Throwable cause) {
		super(msg, cause);
	}
}
