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

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.history.IFileHistoryProvider;
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.team.ui.TeamUI;
import org.eclipse.ui.SubActionBars;


/**
 * <p>Interface for pages that appear in the team history view.</p>
 * <p><em>This interface is not intended to be implemented by clients.
 * Clients can instead subclass {@link HistoryPage}.</em></p>
 *
 * <h3>Lifecycle</h3>
 * <p>
 * Once an Object requests to have its history shown in the History View (through an action or DND), the History View
 * will first try to see if any of the open History Views (there can be multiple Views if some of them are pinned) currently
 * show the requested Object's history. It does this by calling {@link HistoryPage#getInput()} on each IHistoryPage (which is supposed
 * to extend {@link HistoryPage} and comparing the input with the passed in Object. If a History View is found that already contains the Object,
 * it is brought to the front and its history is refreshed (by calling {@link #refresh()}).
 * If no History View already contains the requested Object an <code>IHistoryPage</code> might need to be created if the current <code>IHistoryPage</code>
 * being shown in the History View doesn't know how to display the Object or if there are no <code>IHistoryPage</code>s being shown in the History View.
 * </p>
 * <p>
 * The History View uses an {@link IHistoryPageSource#createPage(Object)} to create the <code>IHistoryPage</code>. If the History View can
 * determine what the Repository Provider is for the dropped Object (i.e. the Object is an instance of {@link IResource}), then it will try
 * to get the {@link IFileHistoryProvider} for the Repository Provider by calling {@link RepositoryProvider#getFileHistoryProvider()}. If no
 * <code>IFileHistoryProvider</code> is returned, the History View will try to adapt the Repository Provider to a <code>IFileHistoryProvider</code>.
 * If the Object whose history is being requested is not an {@link IResource}, it will not be possible to retrieve the Repository Provider from it.
 * In these instances the History View will try to adapt the Object to an {@link IHistoryPageSource}.
 * </p>
 * <p>
 * Once the <code>IHistoryPage</code> is created, {@link IHistoryPage#setInput(Object)} is called; this is handled by {@link HistoryPage} which clients
 * should subclass for their own <code>IHistoryPage</code> implementations. <code>HistoryPage</code> will in turn call {@link HistoryPage#inputSet()} -
 * which clients can use for setting up their <code>IHistoryPage</code>. The old page in the History View (along with its {@link SubActionBars} is disposed  -
 * {@link HistoryPage#dispose()} gets called; interested clients can supply a <code>dispose()</code> method in their subclass. Finally, the new page is shown
 * and its SubActionBars are activated.
 * </p>
 *
 *
 * @see TeamUI#getHistoryView()
 * @since 3.2
 */
public interface IHistoryPage {

	/**
	 * Property name constant (value <code>"org.eclipse.team.ui.name"</code>)
	 * for the page's name.
	 * @since 3.3
	 */
	public static final String P_NAME = TeamUIPlugin.ID + ".name"; //$NON-NLS-1$

	/**
	 * Property name constant (value <code>"org.eclipse.team.ui.description"</code>)
	 * for an page's description.
	 * @since 3.3
	 */
	public static final String P_DESCRIPTION = TeamUIPlugin.ID + ".description"; //$NON-NLS-1$

	/**
	 * Fetches and populates the history page for the given Object. Clients
	 * should provide an implementation for their individual pages.
	 *
	 * @param object the object for which history is being requested for
	 * @return true if the page was able to display the history for the object, false otherwise
	 * @since 3.2
	 */
	public boolean setInput(Object object);

	/**
	 * Returns the object whose history is currently being displayed in the history page.
	 * @return object	the object being displayed in the history page or <code>null</code>
	 * if no input has been set;
	 * @since 3.2
	 */
	public Object getInput();

	/**
	 * Returns true if this history page can show a history for the given object, false if it cannot
	 * @param object the object that is to have history shown
	 * @return boolean
	 * @since 3.2
	 */
	public boolean isValidInput(Object object);

	/**
	 * Requests a refresh of the information presented by the history page.
	 * @since 3.2
	 */
	public void refresh();

	/**
	 * Returns the name of the object whose history the page is showing
	 * @return String containing the name of the object
	 * @since 3.2
	 */
	public String getName();

	/**
	 * Returns a one line description of the object whose history is
	 * being displayed. For example, for files, this may be the
	 * workspace path of the file. The description may be displayed to
	 * the user as tooltip text or by some other means.
	 * @return a one line description of the object whose history is
	 * being displayed or <code>null</code>
	 * @since 3.2
	 */
	public String getDescription();

	/**
	 * Set the site for the page - this needs to be replaced with a proper
	 * {@link IHistoryPageSite} in order to allow history pages to be displayed in
	 * both views and dialogs.
	 * @param site the history page site
	 * @since 3.2
	 */
	public void setSite(IHistoryPageSite site);

	/**
	 * Returns the {@link IHistoryPageSite} set for this page.
	 * @return the history page site for this page
	 * @since 3.2
	 */
	public IHistoryPageSite getHistoryPageSite();

	/**
	 * Called to allow IHistoryPage a chance to dispose of any widgets created
	 * for its page implementation
	 * @since 3.2
	 */
	public void dispose();

	/**
	 * Returns the {@link IHistoryView} instance that contains this history page or <em>null</em> if
	 * the history view instance cannot be determined.
	 * @return IHistoryView	the history view that contains this history page or <em>null</em> if
	 * the history view instance cannot be determined.
	 * @since 3.3
	 */
	public IHistoryView getHistoryView();

	/**
	 * Adds a listener for changes to properties of this page.
	 * Has no effect if an identical listener is already
	 * registered.
	 * <p>
	 * The changes supported by the page are as follows:
	 * </p>
	 * <ul>
	 * <li><code>P_NAME</code>- indicates the name
	 * of the page has changed</li>
	 * <li><code>P_DESCRIPTION</code>- indicates the
	 * description of the page has changed</li>
	 * </ul>
	 * <p>
	 * Clients may define additional properties as required.
	 * </p>
	 * @param listener a property change listener
	 * @since 3.3
	 */
	public void addPropertyChangeListener(IPropertyChangeListener listener);

	/**
	 * Removes the given property listener from this page.
	 * Has no effect if an identical listener is not already registered.
	 *
	 * @param listener a property listener
	 * @since 3.3
	 */
	public void removePropertyChangeListener(IPropertyChangeListener listener);
}
