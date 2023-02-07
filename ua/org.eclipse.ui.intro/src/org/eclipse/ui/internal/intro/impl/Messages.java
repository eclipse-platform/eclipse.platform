/*******************************************************************************
 * Copyright (c) 2004, 2014 IBM Corporation and others.
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
package org.eclipse.ui.internal.intro.impl;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String INTRO_RESOURCE_BUNDLE = "org.eclipse.ui.internal.intro.impl.Messages"; //$NON-NLS-1$

	static {
		initializeMessages(INTRO_RESOURCE_BUNDLE, Messages.class);
	}

	public static String Browser_homeButton_tooltip;
	public static String Browser_forwardButton_tooltip;
	public static String Browser_backwardButton_tooltip;
	public static String Browser_invalidConfig;
	public static String Browser_magnify_tooltip;
	public static String Browser_reduce_tooltip;

	// Dialogs
	// --------
	public static String MessageDialog_errorTitle;
	public static String MessageDialog_warningTitle;
	public static String MessageDialog_infoTitle;

	public static String CustomizableIntroPart_configNotFound;
	public static String StandbyPart_returnToIntro;
	public static String StandbyPart_returnTo;
	public static String EmptyStandbyContentPart_text;
	public static String StandbyPart_failedToCreate;
	public static String StandbyPart_nonDefined;
	public static String StandbyPart_canNotRestore;

	public static String IntroURL_failedToDecode;
	public static String IntroURL_badCommand;
	public static String HyperlinkAdapter_urlIs;
	public static String HTML_embeddedLink;
	public static String StaticHTML_welcome;

	public static String IntroLaunchBar_close_label;
	public static String IntroLaunchBar_close_tooltip;
	public static String IntroLaunchBar_restore_tooltip;
	public static String IntroLaunchBar_welcome;

	// Misc
	// -------
	public static String IntroPart_openExternal_tooltip;
	public static String IntroPart_showContentButton_tooltip;

	//Always Welcome Checkbox
	public static String AlwaysWelcomeCheckbox_Text;

	//Eclipse RSS Viewer
	public static String RSS_Subscribe;
	public static String RSS_Loading;
	public static String RSS_No_news_please_visit;
	public static String RSS_Reading;
	public static String RSS_No_news;
	public static String RSS_Malformed_feed;

}
