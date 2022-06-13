/*******************************************************************************
 * Copyright (c) 2009, 2010 IBM Corporation and others.
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
package org.eclipse.e4.core.di;

/**
 * The exception indicates a error that occurred while performing dependency
 * injection. Use {@link #getCause()} to obtain underlying exception, if any.
 *
 * @since 1.7
 */
public class InjectionException extends RuntimeException {

	private static final long serialVersionUID = 3098545573510654907L;

	/**
	 * Constructs a new injection exception.
	 */
	public InjectionException() {
		super();
	}

	/**
	 * Constructs a new injection exception with the specified cause.
	 * @param e original exception
	 */
	public InjectionException(Throwable e) {
		super(e);
	}

	/**
	 * Constructs a new injection exception with the specified message.
	 * @param msg the error message
	 */
	public InjectionException(String msg) {
		super(msg);
	}

	/**
	 * Constructs a new injection exception with the specified cause and message.
	 * @param msg  the error message
	 * @param e original exception
	 */
	public InjectionException(String msg, Throwable e) {
		super(msg, e);
	}

}
