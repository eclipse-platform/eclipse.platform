/*******************************************************************************
 * Copyright (c) 2020 Paul Pazderski and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Paul Pazderski - initial API and implementation
 *******************************************************************************/
package org.eclipse.debug.core;

import org.eclipse.debug.core.model.IBinaryStreamMonitor;

/**
 * A stream listener is notified of changes to a binary stream monitor.
 * <p>
 * Clients may implement this interface.
 * </p>
 *
 * @see IBinaryStreamMonitor
 * @see IStreamListener
 * @since 3.16
 */
public interface IBinaryStreamListener {

	/**
	 * Notifies this listener that data has been appended to the given stream
	 * monitor.
	 *
	 * @param data the content appended; not <code>null</code>
	 * @param monitor the stream monitor to which content was appended
	 */
	void streamAppended(byte[] data, IBinaryStreamMonitor monitor);
}
