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
package org.eclipse.core.internal.content;

import java.io.IOException;
import org.eclipse.core.runtime.Assert;

/**
 * A wrapper for IOExceptions, throw by LazyInputStream/Reader.
 * Its purpose is to allow one to differentiate
 * between IOExceptions thrown by the base stream/reader from those
 * thrown by streams/readers built on top of LazyInputStream/Reader.
 *
 * @see LazyInputStream
 * @see LazyReader
 */
/* package */class LowLevelIOException extends IOException {

	/**
	 * All serializable objects should have a stable serialVersionUID
	 */
	private static final long serialVersionUID = 1L;

	private IOException actual;

	public LowLevelIOException(IOException actual) {
		// ensure we don't wrap more than once
		Assert.isLegal(!(actual instanceof LowLevelIOException));
		this.actual = actual;
	}

	public IOException getActualException() {
		return actual;
	}
}
