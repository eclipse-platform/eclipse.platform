/*******************************************************************************
 * Copyright (c) 2009, 2015 IBM Corporation and others.
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

package org.eclipse.ua.tests.help.webextension;

import java.util.Locale;

import org.eclipse.core.runtime.Platform;
import org.eclipse.help.webapp.AbstractButton;
import org.osgi.framework.FrameworkUtil;

public class TitleSearchButton extends AbstractButton {

	@Override
	public String getAction() {
		return "toggleTitleView";
	}

	@Override
	public String getId() {
		return "tsearch";
	}

	@Override
	public String getImageURL() {
		return "/titlesearch/icons/sample3.gif";
	}

	@Override
	public String getTooltip(Locale locale) {
		if ("es".equals(locale.getLanguage())) {
			return "Muestre busque en titulos";
		}
		return "Show Search Topic Title";
	}

	@Override
	public String getJavaScriptURL() {
		return "/titlesearch/script/titlesearch.js";
	}

	@Override
	public boolean isAddedToToolbar(String toolbarName) {
		boolean addButton = Platform.getPreferencesService().getBoolean
		(FrameworkUtil.getBundle(getClass()).getSymbolicName(), "extraButton", false, null);
		return (addButton & toolbarName.equals(AbstractButton.CONTENT_TOOLBAR));
	}

}
