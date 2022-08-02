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
package org.eclipse.team.ui.synchronize;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.ui.PartInitException;

/**
 * Interface for {@link org.eclipse.ui.part.Page} subclasses that can appear in
 * the synchronize view {@link ISynchronizeView} and other views, editors or
 * dialogs that display synchronization information. It is not a requirement
 * that pages that appear in the synchronize view implement this interface.
 *
 * @since 3.0
 */
public interface ISynchronizePage {

	/**
	 * Initialize this page with workbench part that contains the page.
	 * This method will be called after the <code>Page#init(IPageSite)</code>
	 * but before <code>Page#createControl(Composite)</code>
	 *
	 * @param site the workbench part for the view containing the page
	 * @throws PartInitException if an error occurs
	 */
	public void init(ISynchronizePageSite site) throws PartInitException;

	/**
	 * Returns the viewer associated with this page or <code>null</code> if the page
	 * doesn't have a viewer.
	 *
	 * @return the viewer associated with this page or <code>null</code> if the page
	 * doesn't have a viewer.
	 */
	public Viewer getViewer();

	/**
	 * Callback that is invoked from the synchronize configuration whenever a
	 * property's value is about to be changed. The page can react to the change
	 * before change events are fired or veto the change.
	 *
	 * @param configuration the synchronize page configuration
	 * @param key the property key
	 * @param newValue new value for the key
	 * @return <code>true</code> if the property change should continue to be
	 * fired to other listeners or <code>false</code> to veto the property change
	 * notification.
	 */
	public boolean aboutToChangeProperty(ISynchronizePageConfiguration configuration, String key, Object newValue);

}
