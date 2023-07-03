/*******************************************************************************
 * Copyright (c) 2004, 2017 IBM Corporation and others.
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
package org.eclipse.ui.internal.intro.impl.model;

import java.util.List;
import java.util.Vector;

import org.eclipse.ui.internal.intro.impl.model.url.IntroURLParser;

/*
 * Intro History Model.
 */
public class History {
	// History of Intro Pages and real URL visited by Intro Browser. All
	// elements are all of type HistoryObject.
	private Vector<HistoryObject> history = new Vector<>();
	private int navigationLocation = 0;


	// Model class for history objects. A history object may be a url or an
	// Intro page. A url is a regular URL navigated to from a fully qualified
	// link. An intro page may be an IFrame page. IFrame pages are not created
	// for every Help topic navigated in an embedded IFrame. Instead the same
	// IFrame is stored in history as a different object with the IFrameURL set.
	// This way the model actually creates one page for every embedded Help
	// Topic target but the navigation history updates the IFrame accordingly.
	static class HistoryObject {
		AbstractIntroPage page;
		String iframeUrl;
		String url;

		HistoryObject(Object location) {
			if (location instanceof String)
				this.url = (String) location;

			if (location instanceof AbstractIntroPage) {
				this.page = (AbstractIntroPage) location;
				// will be set to null if the page is not an IFrame page.
				this.iframeUrl = this.page.getIFrameURL();
			}
		}

		/**
		 * returns the history page. If iframe page, updated to correct url.
		 *
		 * @return
		 */
		AbstractIntroPage getPage() {
			if (page.isIFramePage())
				// when page was stored, the IFrame url was also stored. Make
				// sure to return the same state. The page is the same, only the
				// IFrame url changes.
				page.setIFrameURL(getIFrameUrl());
			return page;
		}

		String getPageId() {
			return page.getId();
		}

		String getIFrameUrl() {
			return iframeUrl;
		}

		String getUrl() {
			return url;
		}

		boolean isURL() {
			return (url != null) ? true : false;
		}

		boolean isIntroPage() {
			return (page != null) ? true : false;
		}

		boolean isIFramePage() {
			return (iframeUrl != null) ? true : false;
		}

	}


	/**
	 * Updates the UI navigation history with either a real URL, or a page ID.
	 *
	 * @param pageId
	 */
	public void updateHistory(String location) {
		// quick exit.
		if (!history.isEmpty() && isSameLocation(location))
			// resetting the same location is useless.
			return;
		doUpdateHistory(location);
	}

	/**
	 * Updates the UI navigation history with either a real URL, or a page ID.
	 *
	 * @param page
	 */
	public void updateHistory(AbstractIntroPage page) {
		// quick exit.
		if (!history.isEmpty() && isSameLocation(page))
			// resetting the same location is useless.
			return;
		doUpdateHistory(page);
	}

	private void doUpdateHistory(Object location) {
		// we got here due to an intro URL listener or an SWT Form hyperlink
		// listener. location may be a url or an IntroPage.
		if (navigationLocation == getHistoryEndPosition())
			// we are at the end of the vector, just push.
			pushToHistory(location);
		else
			// we already navigated. add item at current location, and clear
			// rest of history. (Same as browser behavior.)
			trimHistory(location);
	}


	private boolean isSameLocation(Object location) {
		HistoryObject currentLocation = getCurrentLocation();
		if (location instanceof String && currentLocation.isURL())
			return currentLocation.getUrl().equals(location);

		if (location instanceof AbstractIntroPage
				&& currentLocation.isIntroPage()) {

			AbstractIntroPage locationPage = (AbstractIntroPage) location;
			// be carefull here with calling getPage on historyOvject.
			if (!currentLocation.getPageId().equals(locationPage.getId()))
				return false;

			// both pages have same ids, they are either both regular pages or
			// both are Iframe pages. check if they have the same IFrame urls
			if (currentLocation.isIFramePage() && locationPage.isIFramePage())
				return currentLocation.getIFrameUrl().equals(
					locationPage.getIFrameURL());

			// both pages are not IFrame pages, and they have same id.
			return true;
		}

		return false;
	}




	private void pushToHistory(Object location) {
		history.add(new HistoryObject(location));
		// point the nav location to the end of the vector.
		navigationLocation = getHistoryEndPosition();
	}

	 public void removeLastHistory() {
		history.remove(getHistoryEndPosition());
		// point the nav location to the end of the vector.
		navigationLocation = getHistoryEndPosition();
	}

	private void trimHistory(Object location) {
		List<HistoryObject> newHistory = history.subList(0, navigationLocation + 1);
		history = new Vector<>(newHistory);
		history.add(new HistoryObject(location));
		// point the nav location to the end of the vector.
		navigationLocation = getHistoryEndPosition();
	}

	/**
	 * Return the position of the last element in the navigation history. If
	 * vector is empty, return 0.
	 *
	 * @param vector
	 * @return
	 */
	private int getHistoryEndPosition() {
		if (history.isEmpty())
			return 0;
		return history.size() - 1;
	}

	public void navigateHistoryBackward() {
		if (badNavigationLocation(navigationLocation - 1))
			// do nothing. We are at the begining.
			return;
		--navigationLocation;
	}

	/**
	 * Navigate forward in the history.
	 *
	 * @return
	 */
	public void navigateHistoryForward() {
		if (badNavigationLocation(navigationLocation + 1))
			// do nothing. We are at the begining.
			return;
		++navigationLocation;
	}


	private boolean badNavigationLocation(int navigationLocation) {
		if (navigationLocation < 0 || navigationLocation >= history.size())
			// bad nav location.
			return true;
		return false;
	}


	/**
	 * Returns true if the current location in the navigation history represents
	 * a URL. False if the location is an Intro Page id.
	 *
	 * @return Returns the locationIsURL.
	 */
	private HistoryObject getCurrentLocation() {
		return history.elementAt(navigationLocation);
	}

	public boolean canNavigateForward() {
		return navigationLocation != getHistoryEndPosition() ? true : false;
	}

	public boolean canNavigateBackward() {
		return navigationLocation == 0 ? false : true;
	}

	public boolean currentLocationIsUrl() {
		if (history.isEmpty()) {
			return false;
		}
		return getCurrentLocation().isURL();
	}

	public String getCurrentLocationAsUrl() {
		return getCurrentLocation().getUrl();
	}

	public AbstractIntroPage getCurrentLocationAsPage() {
		return getCurrentLocation().getPage();
	}

	public static boolean isURL(String aString) {
		IntroURLParser parser = new IntroURLParser(aString);
		if (parser.hasProtocol())
			return true;
		return false;
	}


	public void clear() {
		history.clear();
		navigationLocation = 0;
	}




}
