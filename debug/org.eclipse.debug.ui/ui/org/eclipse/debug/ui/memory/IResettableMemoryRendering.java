/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
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

package org.eclipse.debug.ui.memory;

import org.eclipse.debug.core.DebugException;

/**
 * A memory rendering that can be reset.  Reset behavior is rendering
 * specific.  Typically, reset means that the rendering would position
 * itself back to the base address of its memory block.  However, clients
 * may define its reset behavior that is suitable for its rendering.
 * <p>
 * Clients may implement this interface.
 * </p>
 * @since 3.2
 *
 */
public interface IResettableMemoryRendering extends IMemoryRendering {

	/**
	 * Reset this memory rendering.
	 *
	 * @throws DebugException when there is a problem resetting this memory rendering.
	 */
	void resetRendering() throws DebugException;

}
