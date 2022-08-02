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

import org.eclipse.team.core.history.IFileHistory;

/**
 * This interface provides a way for clients to request the History View
 * to display the history for a given object.
 *
 * <p>
 * This interface is not intended to be implemented by clients.
 *
 * @since 3.2
 * @see IFileHistory
 * @see IHistoryPage
 */
public interface IHistoryView {

	/**
	 * The id for this view.
	 * @since 3.3
	 */
	public static final String VIEW_ID = "org.eclipse.team.ui.GenericHistoryView"; //$NON-NLS-1$

	/**
	 * Returns the history page that is currently being displayed by the
	 * history view.
	 * TODO: Need to explain what page this is. Is it the visible page?
	 * @return the history page
	 */
	public IHistoryPage getHistoryPage();

	/**
	 * Shows the history for the passed in object. This method is equivalent to
	 * {@link #showHistoryFor(Object, boolean)} with <code>force</code>
	 * set to <code>false</code>.
	 *
	 * @param object the input whose history is to be displayed
	 * @return returns the history page that the passed in object is being shown
	 *         in or null if no appropriate page can be found.
	 */
	public IHistoryPage showHistoryFor(Object object);

	/**
	 * Shows the history for the given object. If force is <code>false</code>,
	 * the history may be displayed in another instance of {@link IHistoryView}.
	 * For example, if the target view is pinned, the history will be shown in another
	 * view instance. If force is <code>true</code>, the history will be shown in this view
	 * instance regardless of the view state.
	 *
	 * @param object the input whose history is to be displayed
	 * @param force whether this view should show the input even if it is pinned
	 * @return returns the history page that the passed in object is being shown
	 *         in or <code>null</code> if no appropriate page can be found.
	 * @since 3.3
	 */
	public IHistoryPage showHistoryFor(Object object, boolean force);

}
