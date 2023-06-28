/*******************************************************************************
 * Copyright (c) 2004, 2006 IBM Corporation and others.
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

import org.eclipse.ui.IWorkbenchPartSite;

/**
 * A workbench site that hosts memory renderings and provides
 * synchronization services for memory renderings.
 * <p>
 * A rendering site has an optional synchronization provider at any one time. If a
 * rendering provides synchronization information it should set itself as the synchronization
 * provider for its memory rendering site when it is activated.
 * </p>
 * <p>
 * Clients hosting memory rendering may implement this interface.
 * </p>
 * @since 3.1
 */
public interface IMemoryRenderingSite {

	/**
	 * Returns the workbench part site hosting memory renderings for this rendering site.
	 *
	 * @return the view site hosting memory renderings for this rendering site
	 */
	IWorkbenchPartSite getSite();

	/**
	 * Returns the syncrhonization service for this rendering site
	 * or <code>null</code> if none.
	 *
	 * @return the syncrhonization service for this rendering site or <code>null</code>
	 */
	IMemoryRenderingSynchronizationService getSynchronizationService();

	/**
	 * Returns all the memory rendering containers within this rendering site.
	 *
	 * @return all the memory rendering containers within this rendering site
	 */
	IMemoryRenderingContainer[] getMemoryRenderingContainers();

	/**
	 * Returns the rendering container with the given id or <code>null</code>
	 * if none.
	 *
	 * @param id identifier of the container being requested
	 * @return the rendering container with the given id or <code>null</code>
	 * if none
	 */
	IMemoryRenderingContainer getContainer(String id);


}
