/*******************************************************************************
 * Copyright (c) 2008, 2016 IBM Corporation and others.
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

package org.eclipse.ua.tests.help.preferences;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Observable;
import java.util.Observer;

import org.eclipse.help.IHelpResource;
import org.eclipse.help.internal.base.BookmarkManager;
import org.eclipse.help.internal.base.BookmarkManager.BookmarkEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/*
 * Test the BookmarkManager
 */
public class BookmarksTest {

	private static final String ECLIPSE = "eclipse";
	private static final String HTTP_ECLIPSE = "http://www.eclipse.org";
	private static final String HELP = "help";
	private static final String HTTP_HELP = "http://help.eclipse.org";
	private static final String BUGZILLA = "bugzilla";
	private static final String HTTP_BUGZILLA = "https://bugs.eclipse.org/bugs/";

	private static class BookmarkObserver implements Observer {

		public Object o;
		public Object arg;
		public int eventCount = 0;

		@Override
		public void update(Observable o, Object arg) {
			++eventCount;
			this.o = o;
			this.arg = arg;
		}

		public BookmarkManager.BookmarkEvent getEvent() {
			return (BookmarkEvent) arg;
		}

	}

	private BookmarkManager manager;
	private BookmarkObserver observer;

	@BeforeEach
	public void setUp() throws Exception {
		manager = new BookmarkManager();
		manager.removeAllBookmarks();
		observer = new BookmarkObserver();
		manager.addObserver(observer);
	}

	@AfterEach
	public void tearDown() throws Exception {
		manager = null;
		observer = null;
	}

	@Test
	public void testRemoveAll() {
		manager.removeAllBookmarks();
		assertEquals(1, observer.eventCount);
		assertEquals(manager, observer.o);
		assertEquals(BookmarkManager.REMOVE_ALL, observer.getEvent().getType());
		assertNull(observer.getEvent().getBookmark());
		IHelpResource[] bookmarks = manager.getBookmarks();
		assertThat(bookmarks).isEmpty();
		assertEquals(1, observer.eventCount);
	}

	@Test
	public void testAddBookmarks() {
		manager.addBookmark(HTTP_ECLIPSE, ECLIPSE);
		assertEquals(1, observer.eventCount);
		assertTrue(observer.arg instanceof BookmarkManager.BookmarkEvent);
		BookmarkManager.BookmarkEvent event = observer.getEvent();
		assertEquals(event.getType(), BookmarkManager.ADD);
		assertEquals(ECLIPSE, event.getBookmark().getLabel());
		assertEquals(HTTP_ECLIPSE, event.getBookmark().getHref());
		manager.addBookmark(HTTP_BUGZILLA, BUGZILLA);
		BookmarkManager manager2 = new BookmarkManager();
		IHelpResource[] bookmarks = manager2.getBookmarks();
		assertThat(bookmarks).hasSize(2);
		assertEquals(ECLIPSE, bookmarks[0].getLabel());
		assertEquals(BUGZILLA, bookmarks[1].getLabel());
		assertEquals(HTTP_ECLIPSE, bookmarks[0].getHref());
		assertEquals(HTTP_BUGZILLA, bookmarks[1].getHref());
		assertEquals(2, observer.eventCount);
		assertEquals(manager, observer.o);
	}

	@Test
	public void testRemoveBookmarks() {
		manager.addBookmark(HTTP_ECLIPSE, ECLIPSE);
		assertEquals(1, observer.eventCount);
		manager.addBookmark(HTTP_BUGZILLA, BUGZILLA);
		manager.addBookmark(HTTP_HELP, HELP);
		assertEquals(3, observer.eventCount);
		manager.removeBookmark(HTTP_ECLIPSE, BUGZILLA);  // No such bookmark
		assertEquals(observer.eventCount, 3);
		manager.removeBookmark(HTTP_ECLIPSE, ECLIPSE);
		assertEquals(4, observer.eventCount);
		BookmarkManager.BookmarkEvent event = observer.getEvent();
		assertEquals(event.getType(), BookmarkManager.REMOVE);
		assertEquals(ECLIPSE, event.getBookmark().getLabel());
		assertEquals(HTTP_ECLIPSE, event.getBookmark().getHref());
		assertEquals(manager, observer.o);
		manager.removeBookmark(new BookmarkManager.Bookmark(BUGZILLA, HTTP_BUGZILLA));
		assertEquals(5, observer.eventCount);
		event = observer.getEvent();
		assertEquals(event.getType(), BookmarkManager.REMOVE);
		assertEquals(BUGZILLA, event.getBookmark().getLabel());
		assertEquals(HTTP_BUGZILLA, event.getBookmark().getHref());
		assertThat(manager.getBookmarks()).hasSize(1);
	}
}
