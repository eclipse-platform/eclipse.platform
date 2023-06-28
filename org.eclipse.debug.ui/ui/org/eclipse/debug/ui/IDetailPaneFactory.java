/*******************************************************************************
 * Copyright (c) 2006, 2013 IBM Corporation and others.
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
package org.eclipse.debug.ui;

import java.util.Set;

import org.eclipse.jface.viewers.IStructuredSelection;

/**
 * A detail pane factory creates one or more types of detail panes.
 * <p>
 * Detail pane factories are contributed via the
 * <code>org.eclipse.debug.ui.detailPaneFactories</code> extension point.
 * Following is an example of a detail pane factory extension:
 * </p>
 *
 * <pre>
 * &lt;extension point="org.eclipse.debug.ui.detailPaneFactories"&gt;
 *     &lt;detailFactories
 *           class="org.eclipse.temp.TableDetailPaneFactory"
 *           name="Table Detail Factory"&gt;
 *     &lt;/detailFactories&gt;
 * &lt;/extension&gt;
 * </pre>
 *
 * Clients contributing a detail pane factory are intended to implement this
 * interface.
 *
 * @see IDetailPane
 * @since 3.3
 *
 */
public interface IDetailPaneFactory {

	/**
	 * Returns all possible types detail panes that this factory can
	 * create for the given selection, possibly empty. Detail panes are returned
	 * as a set of detail pane identifiers.
	 *
	 * @param selection The current selection
	 * @return Set of String IDs for possible detail pane types, possibly empty
	 */
	Set<String> getDetailPaneTypes(IStructuredSelection selection);

	/**
	 * Returns the identifier of the default detail pane type to use for the given
	 * selection, or <code>null</code> if this factory has no preference.
	 * A factory can override the platform's default detail pane by returning
	 * a non-<code>null</code> value.
	 *
	 * @param selection The current selection
	 * @return a detail pane type identifier or <code>null</code>
	 */
	String getDefaultDetailPane(IStructuredSelection selection);

	/**
	 * Creates and returns a detail pane corresponding to the given detail pane
	 * type identifier that this factory can produce (according to
	 * <code>getDetailPaneTypes(IStructuredSelection selection)</code>).
	 *
	 * @param paneID The id of the detain pane type to be created
	 * @return detail pane or <code>null</code> if one could not be created
	 */
	IDetailPane createDetailPane(String paneID);

	/**
	 * Returns a name for the detail pane type associated with the given ID
	 * or <code>null</code> if none. Used to
	 * populate the context menu with meaningful names of the pane types.
	 *
	 * @param paneID detail pane type identifier
	 * @return detail pane name or <code>null</code> if none
	 */
	String getDetailPaneName(String paneID);

	/**
	 * Returns a description for the detail pane type associated with the given ID
	 * or <code>null</code> if none.
	 *
	 * @param paneID detail pane type identifier
	 * @return detail pane description or <code>null</code> if none
	 */
	String getDetailPaneDescription(String paneID);

}
