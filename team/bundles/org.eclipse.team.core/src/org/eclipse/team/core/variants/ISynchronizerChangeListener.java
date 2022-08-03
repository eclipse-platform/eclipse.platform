/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
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
package org.eclipse.team.core.variants;

import org.eclipse.core.resources.IResource;

/**
 * Listener that can receive notification from a <code>ThreeWaySynchronizer</code>
 * when the synchronization state of one or more resources has changed.
 * @see ThreeWaySynchronizer
 * @since 3.0
 */
public interface ISynchronizerChangeListener {

	/**
	 * Notification of synchronization state changes for the given resources.
	 * Clients must query the <code>ThreeWaySynchronizer</code> that generated
	 * this event to determine the new synchronization state.
	 * @param resources the resources whose synchronization state has changed
	 */
	public void syncStateChanged(IResource[] resources);
}
