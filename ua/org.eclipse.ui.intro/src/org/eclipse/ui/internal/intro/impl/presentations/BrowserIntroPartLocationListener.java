/*******************************************************************************
 * Copyright (c) 2003, 2016 IBM Corporation and others.
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
package org.eclipse.ui.internal.intro.impl.presentations;

import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.LocationEvent;
import org.eclipse.swt.browser.LocationListener;
import org.eclipse.ui.internal.intro.impl.model.AbstractIntroPage;
import org.eclipse.ui.internal.intro.impl.model.IntroModelRoot;
import org.eclipse.ui.internal.intro.impl.model.url.IntroURL;
import org.eclipse.ui.internal.intro.impl.model.url.IntroURLParser;

/**
 * A Location Listener that knows how to intercept OOBE action URLs. It also
 * knows how to update UI navigation hisutory.
 */
public class BrowserIntroPartLocationListener implements LocationListener {

	private final BrowserIntroPartImplementation implementation;

	/**
	 * Takes the implementation as an input.
	 */
	public BrowserIntroPartLocationListener(
			BrowserIntroPartImplementation implementation) {
		this.implementation = implementation;
	}

	@Override
	public void changed(LocationEvent event) {
		String url = event.location;
		if (url == null)
			return;

		// guard against unnecessary History updates.
		Browser browser = (Browser) event.getSource();
		if (browser.getData("navigation") != null //$NON-NLS-1$
				&& browser.getData("navigation").equals("true")) //$NON-NLS-1$ //$NON-NLS-2$
			return;

		IntroModelRoot model = implementation.getModel();
		IntroURLParser parser = new IntroURLParser(url);
		if (!parser.hasProtocol() || parser.getHost() == null
				|| parser.getHost().isEmpty())
			// This will filter out two navigation events fired by the browser
			// on a setText. (about:blank and
			// res://C:\WINDOWS\System32\shdoclc.dll/navcancl.htm on windows,
			// and file:/// on Linux)
			return;

		if (model.isDynamic()) {
			// Update the history even with real URLs. Note that if we have
			// multiple embedded URL navigations due to frames, the
			// frameNavigation flag filters them. This flag is set here, and is
			// cleared by a progress listener, when all the frame navigation is
			// completed. We need to update history only in dynamic case. In
			// static case, the browser keeps the history.
			if (browser.getData("frameNavigation") != null) { //$NON-NLS-1$
				// this is at least the second frame navigation, remove last
				// history since it was added just as a filler.
				if (!event.top && browser.getData("tempUrl") != null //$NON-NLS-1$
						&& browser.getData("tempUrl").equals("true")) { //$NON-NLS-1$ //$NON-NLS-2$
					implementation.getHistory().removeLastHistory();
					flagRemovedTempUrl();
				}
			}

			if (event.top) {
				// we are navigating to a regular fully qualified URL. Event.top
				// is true.
				flagStartOfFrameNavigation();
				implementation.updateHistory(url);
			}

			if (browser.getData("frameNavigation") == null //$NON-NLS-1$
					&& !event.top) {
				// a new url navigation that is not in a top frame. It can
				// be a navigation url due to frames, it can be due to a true
				// single Frame navigation (when you click on a link inside a
				// Frame) or it is an embedded Help System topic navigation.
				AbstractIntroPage currentPage = model.getCurrentPage();
				if (currentPage.isIFramePage()) {
					// it is an embedded Help System topic navigation. we are
					// navigating to an injected iframe since event.top is
					// false. Set the iframe url of the current iframe page, and
					// add it
					// to history.
					currentPage.setIFrameURL(url);
					implementation.updateHistory(currentPage);
				} else {
					flagStartOfFrameNavigation();
					flagStoredTempUrl();
					implementation.updateHistory(url);
				}
			}

		}
		return;
	}

	/**
	 * Intercept any LocationEvents on the browser. If the event location is a
	 * valid IntroURL, cancel the event and execute the intro action that is
	 * embedded in the URL
	 */
	@Override
	public void changing(LocationEvent event) {
		String url = event.location;
		if (url == null)
			return;

		IntroModelRoot model = implementation.getModel();
		IntroURLParser parser = new IntroURLParser(url);
		if (parser.hasIntroUrl()) {
			// stop URL first.
			event.doit = false;
			// execute the action embedded in the IntroURL
			IntroURL introURL = parser.getIntroURL();
			introURL.execute();

			// In the case of dynamic Intro, guard against extra Frame
			// navigations. This can happen in the case of intro injected
			// IFrames or Frames included through intro xml content.
			// INTRO: user defined iframes in Intro pages are not properly
			// supported here, only Help system injected iframes.
			if (model.isDynamic()) {
				if ((introURL.getParameter(IntroURL.KEY_EMBED_TARGET) != null)
						&& introURL.getAction().equals(IntroURL.SHOW_PAGE))
					flagStartOfNavigation();
			}
			return;
		}


	}




	public void flagStartOfFrameNavigation() {
		if (implementation.getBrowser().getData("frameNavigation") == null) //$NON-NLS-1$
			implementation.getBrowser().setData("frameNavigation", "true"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public void flagEndOfFrameNavigation() {
		implementation.getBrowser().setData("frameNavigation", null); //$NON-NLS-1$
	}


	public void flagStartOfNavigation() {
		if (implementation.getBrowser().getData("navigation") == null) //$NON-NLS-1$
			implementation.getBrowser().setData("navigation", "true"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public void flagEndOfNavigation() {
		implementation.getBrowser().setData("navigation", null); //$NON-NLS-1$
	}


	public void flagStoredTempUrl() {
		if (implementation.getBrowser().getData("tempUrl") == null) //$NON-NLS-1$
			implementation.getBrowser().setData("tempUrl", "true"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public void flagRemovedTempUrl() {
		implementation.getBrowser().setData("tempUrl", null); //$NON-NLS-1$
	}


}
