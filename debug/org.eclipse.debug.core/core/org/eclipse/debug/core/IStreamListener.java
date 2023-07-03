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
package org.eclipse.debug.core;


import org.eclipse.debug.core.model.IStreamMonitor;

/**
 * A stream listener is notified of changes
 * to a stream monitor.
 * <p>
 * Clients may implement this interface.
 * </p>
 * @see IStreamMonitor
 */
public interface IStreamListener {
	/**
	 * Notifies this listener that text has been appended to
	 * the given stream monitor.
	 *
	 * @param text the appended text
	 * @param monitor the stream monitor to which text was appended
	 */
	void streamAppended(String text, IStreamMonitor monitor);
}
