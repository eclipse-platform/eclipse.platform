/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
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
package org.eclipse.help.internal.webapp.data;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.help.IHelpResource;
import org.eclipse.help.internal.base.BaseHelpSystem;
import org.eclipse.help.internal.base.BookmarkManager;

/**
 * This class manages bookmarks.
 */
public class BookmarksData extends RequestData {
	public static final int NONE = 0;
	public static final int ADD = 1;
	public static final int REMOVE = 2;
	public static final int REMOVE_ALL = 3;

	public BookmarksData(ServletContext context, HttpServletRequest request,
			HttpServletResponse response) {
		super(context, request, response);

		switch (getOperation()) {
			case ADD :
				addBookmark();
				break;
			case REMOVE :
				removeBookmark();
				break;
			case REMOVE_ALL :
				removeAllBookmarks();
				break;
			default :
				break;
		}
	}

	public void addBookmark() {
		String bookmarkURL = request.getParameter("bookmark"); //$NON-NLS-1$
		if (bookmarkURL != null && bookmarkURL.length() > 0
				&& !bookmarkURL.equals("about:blank")) { //$NON-NLS-1$
			String title = request.getParameter("title"); //$NON-NLS-1$
			if (title == null) {
				return;
			}
			BookmarkManager manager = BaseHelpSystem.getBookmarkManager();
			manager.addBookmark(bookmarkURL, title);
		}
	}

	public void removeBookmark() {
		String bookmarkURL = request.getParameter("bookmark"); //$NON-NLS-1$
		if (bookmarkURL != null && bookmarkURL.length() > 0
				&& !bookmarkURL.equals("about:blank")) { //$NON-NLS-1$
			String title = request.getParameter("title"); //$NON-NLS-1$
			if (title == null) {
				return;
			}
			BookmarkManager manager = BaseHelpSystem.getBookmarkManager();
			manager.removeBookmark(bookmarkURL, title);
		}
	}

	public void removeAllBookmarks() {
		BookmarkManager manager = BaseHelpSystem.getBookmarkManager();
		manager.removeAllBookmarks();
	}

	public Topic[] getBookmarks() {
		// sanity test for infocenter, but this could not work anyway...
		if (BaseHelpSystem.getMode() != BaseHelpSystem.MODE_INFOCENTER) {
			BookmarkManager manager = BaseHelpSystem.getBookmarkManager();
			IHelpResource [] bookmarks = manager.getBookmarks();
			Topic [] topics = new Topic[bookmarks.length];
			for (int i=0; i<bookmarks.length; i++) {
				IHelpResource bookmark = bookmarks[i];
				topics[i] = new Topic(bookmark.getLabel(), bookmark.getHref());
			}
			return topics;
		}
		return new Topic[0];
	}

	private int getOperation() {
		String op = request.getParameter("operation"); //$NON-NLS-1$
		if ("add".equals(op)) //$NON-NLS-1$
			return ADD;
		else if ("remove".equals(op)) //$NON-NLS-1$
			return REMOVE;
		else if ("removeAll".equals(op)) //$NON-NLS-1$
			return REMOVE_ALL;
		else
			return NONE;
	}
}
