/*******************************************************************************
 * Copyright (c) 2011-2012 IBM Corporation and others.
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

/**
 * Extension to memory site that allows a client to request a rendering container
 * to become visible.
 *
 * @since 3.8
 */
public interface IMemoryRenderingSite2 extends IMemoryRenderingSite {

	/**
	 * Sets whether the identified container should be visible.
	 *
	 * @param id identifier of the container to be affected
	 * @param visible whether the given container should be made visible
	 */
	void setContainerVisible(String id, boolean visible);

}
