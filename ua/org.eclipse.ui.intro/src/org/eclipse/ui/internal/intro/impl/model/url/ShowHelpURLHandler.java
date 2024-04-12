/*******************************************************************************
 * Copyright (c) 2004, 2006 IBM Corporation and others.
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


package org.eclipse.ui.internal.intro.impl.model.url;

import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.intro.impl.IntroPlugin;
import org.eclipse.ui.internal.intro.impl.model.AbstractIntroElement;
import org.eclipse.ui.internal.intro.impl.model.AbstractIntroPage;
import org.eclipse.ui.internal.intro.impl.model.IntroModelRoot;
import org.eclipse.ui.internal.intro.impl.model.IntroPartPresentation;
import org.eclipse.ui.internal.intro.impl.presentations.BrowserIntroPartImplementation;
import org.eclipse.ui.internal.intro.impl.util.Log;

public class ShowHelpURLHandler {
	private IntroURL introURL = null;

	ShowHelpURLHandler(IntroURL url) {
		this.introURL = url;
	}



	public boolean showHelpTopic(String href, String embed, String embedTarget) {
		if (href == null)
			return false;

		boolean isEmbedded = (embed != null && embed
			.equals(IntroURL.VALUE_TRUE)) ? true : false;
		if (!isEmbedded)
			// still false, check the embedTarget. If embedTarget is set, then
			// we
			// have embedded by default.
			isEmbedded = (embedTarget != null) ? true : false;

		IntroPartPresentation presentation = IntroPlugin.getDefault()
			.getIntroModelRoot().getPresentation();
		String presentationStyle = presentation.getImplementationKind();

		if (isEmbedded
				&& presentationStyle
					.equals(IntroPartPresentation.BROWSER_IMPL_KIND)) {

			// Embedded is true and we have HTML presentation, show href
			// embedded, either in full page, or in div.
			BrowserIntroPartImplementation impl = (BrowserIntroPartImplementation) presentation
				.getIntroPartImplementation();
			// INTRO: maybe add support for navigation
			href = PlatformUI.getWorkbench().getHelpSystem()
				.resolve(href, true).toExternalForm();

			if (embedTarget == null)
				return impl.getBrowser().setUrl(href);

			// embedded in Div case.
			IntroModelRoot model = IntroPlugin.getDefault().getIntroModelRoot();
			return handleEmbedURLInDiv(href, embedTarget, model
				.getCurrentPage());
		}

		// show href in Help window. SWT presentation is handled here.
		// WorkbenchHelp takes care of error handling.
		PlatformUI.getWorkbench().getHelpSystem().displayHelpResource(href);
		return true;
	}


	/*
	 * Handles the embedded url case. Clone page and insert an IFrame in the
	 * target embed div. Note that only one div per page can be specified as an
	 * embed div. This is because we need to mangle the name for the cloned
	 * page, and the mangled name id pageId_embedTarget.
	 */
	private boolean handleEmbedURLInDiv(String href, String embedTarget,
			AbstractIntroPage currentPage) {

		// re-use a runtime generated page, if found. Create the mangled id for
		// the page and check if page exists first. If not, create one.
		IntroModelRoot model = (IntroModelRoot) currentPage.getParentPage()
			.getParent();
		String currentPageId = null;
		if (currentPage.isIFramePage())
			currentPageId = currentPage.getUnmangledId();
		else
			currentPageId = currentPage.getId();
		String mangledPageId = currentPageId + "_" + "WITH_IFRAME"; //$NON-NLS-1$ //$NON-NLS-2$

		// get current standby state.
		boolean standby = IntroPlugin.isIntroStandby();
		String standbyAsString = standby ? IntroURL.VALUE_TRUE : "false"; //$NON-NLS-1$

		AbstractIntroPage pageWithIFrame = (AbstractIntroPage) model.findChild(
			mangledPageId, AbstractIntroElement.ABSTRACT_PAGE);
		if (pageWithIFrame != null) {
			pageWithIFrame.setIFrameURL(href);
			return introURL.showPage(mangledPageId, standbyAsString);
		}

		// Page never generated, clone and create.
		AbstractIntroPage clonedPage = null;
		try {
			clonedPage = (AbstractIntroPage) currentPage.clone();
		} catch (CloneNotSupportedException ex) {
			// should never be here.
			Log.error("Failed to clone Intro page: " + currentPage.getId(), ex); //$NON-NLS-1$
			return false;
		}

		// embed url as IFrame in target div. We need to find target div in
		// cloned page not in the original page.
		boolean canInjectFrame = clonedPage.injectIFrame(href, embedTarget);
		if (!canInjectFrame)
			// Called method handles error.
			return false;

		clonedPage.setId(mangledPageId);
		model.addChild(clonedPage);
		return introURL.showPage(clonedPage.getId(), standbyAsString);
	}

}
