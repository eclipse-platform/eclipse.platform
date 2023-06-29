/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
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
package org.eclipse.debug.core.model;

import java.io.IOException;

/**
 * Extension to a streams proxy that allows closing of the output stream
 * connected to the standard input stream of a proxy's process.
 * <p>
 * Clients should implement this interface, in addition to
 * <code>IStreamsProxy</code>, if interested closing the standard
 * input stream.
 * </p>
 * @since 3.1
 */
public interface IStreamsProxy2 extends IStreamsProxy {

	/**
	 * Closes the output stream connected to the standard input stream
	 * of this proxy's process.
	 *
	 * @throws IOException if unable to close the stream
	 */
	void closeInputStream() throws IOException;
}
