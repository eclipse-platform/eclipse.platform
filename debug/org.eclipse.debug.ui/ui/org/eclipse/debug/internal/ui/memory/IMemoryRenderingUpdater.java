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

import org.eclipse.debug.core.model.IMemoryBlockExtension;
import org.eclipse.debug.ui.memory.IMemoryRendering;


/**
 * Represents an object that will manage the update of an IMemoryRendering
 * based on connections.  If the memory block implements this interface or returns
 * an object of this type when getAdapter(...) is called, a rendering would
 * call #supportsManagedUpdate to determine if it should handle and refresh
 * upon a debug event.
 *
 * If the client wants to manage its own update, it would return true when
 * #supportsManagedUpdate is called.  The rendering will not get refreshed
 * upon any debug events.  Instead, the rendering will update when
 * <code>IMemoryBlockConnection.update</code> is called.
 *
 * This interface is EXPERIMENTAL.
 *
 */
public interface IMemoryRenderingUpdater extends IMemoryBlockExtension {

	/**
	 * @return true if the updater will manage the update of a rendering
	 */
	boolean supportsManagedUpdate(IMemoryRendering rendering);

}
