/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
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

/**
 * A common abstract view for lazy character/binary input streams.
 *
 * @since 3.1
 */
public interface ILazySource {
	/**
	 * @return a boolean indicating whether this stream is character or byte-based
	 */
	boolean isText();

	/**
	 * Rewinds the stream.
	 */
	void rewind();
}
