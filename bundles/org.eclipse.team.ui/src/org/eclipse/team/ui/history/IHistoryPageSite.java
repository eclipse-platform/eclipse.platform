/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.ui.history;

import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.part.IPageSite;

/**
 * A site which provides access to the context in which this page
 * is being displayed. Instances of this interface serve a similar purpose
 * to <code>IWorkbenchSite</code> instances but is provided as a separate
 * objects to allow clients to access the different site types
 * (view, editor, dialog) using a common interface. This interface also provides
 * access to the part for the site because this is required by some UI
 * components. Clients should not need to access the part.
 * <p>
 * Clients can determine the type of workbench site by doing <code>instanceof</code>
 * checks on the object returned by <code>getWorkbenchSite</code>. Similar
 * <code>instanceof</code> checks can be done with the part.
 * <p>
 * This interface is not intended to be implemented by clients.
 *
 * @since 3.2
 */
public interface IHistoryPageSite {

	/**
	 * Return the workbench page site for the page or <code>null</code> if a
	 * workbench page site is not available (e.g. if the page is being shown in
	 * a dialog).
	 *
	 * @return the workbench page site for the page or <code>null</code>
	 */
	IPageSite getWorkbenchPageSite();

	/**
	 * Return the workbench part for the page or <code>null</code> if a
	 * workbench part is not available (e.g. if the page is being shown in a
	 * dialog).
	 *
	 * @return the workbench part for the page or <code>null</code>
	 */
	IWorkbenchPart getPart();

	/**
	 * Returns the shell for this site.
	 * @return the shell for this site
	 */
	Shell getShell();

	/**
	 * Get the selection provider that gives access to the selection
	 * of the synchronize page associated with this page site.
	 * @return the selection provider for the page
	 */
	ISelectionProvider getSelectionProvider();

	/**
	 * Sets the selection provider for this workbench site.
	 * @param provider the selection provider, or <code>null</code> to clear it
	 */
	void setSelectionProvider(ISelectionProvider provider);

	/**
	 * Give the page focus.
	 */
	void setFocus();

	/**
	 * Returns the toolbar manager for this site or <code>null</code>
	 * it there is no toolbar. For sites which have a corresponding
	 * workbench site, the toolbar manager will come from the action
	 * bars.
	 *
	 * @return toolbar manager
	 */
	IToolBarManager getToolBarManager();

	/**
	 * Returns whether this site is modal.
	 * @return true if the site is modal, false otherwise
	 */
	boolean isModal();

}
