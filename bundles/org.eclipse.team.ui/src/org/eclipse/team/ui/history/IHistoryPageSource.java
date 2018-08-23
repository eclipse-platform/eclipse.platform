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
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.team.ui.history;

import org.eclipse.team.core.history.IFileHistoryProvider;
import org.eclipse.ui.part.Page;

/**
 * Interface to an object which is capable of supplying a history page for display
 * by the history view or other views, dialogs or editors that wish to display
 * the history of an object.
 *
 * This interface is not intended to be implemented by clients.
 * Clients can instead subclass {@link HistoryPageSource}.
 *
 * @see IFileHistoryProvider
 * @since 3.2
 */
public interface IHistoryPageSource {

	/**
	 * Returns true if this history page source can show a history for the given object, false if it cannot
	 * @param object the object that is to have history shown
	 * @return boolean
	 */
	public boolean canShowHistoryFor(Object object);

	/**
	 * Called by the history view to create the page for this IFileHistoryProvider. The
	 * page must implement {@link IHistoryPage}.
	 * @param object the object whose history is to be shown
	 *
	 * @see IHistoryPage
	 * @return a Page that implements IHistoryPage (should return either an IPage, IPageBookViewPage or an IHistoryPage
	 */
	public Page createPage(Object object);
}
