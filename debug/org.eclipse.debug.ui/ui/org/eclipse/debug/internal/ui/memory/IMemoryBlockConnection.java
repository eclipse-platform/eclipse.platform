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
package org.eclipse.debug.internal.ui.memory;

/**
 * Represents a connection to a memory block.  An IMemoryRenderingUpdater can call
 * a connection to update instead of relying on a rendering to listen
 * for CHANGE / SUSPEND debug event to trigger an update.
 *
 * This interface is EXPERIMENTAL.
 *
 */
public interface IMemoryBlockConnection {

	/**
	 * Update the content of a memory block in a connection.
	 */
	void update();
}
