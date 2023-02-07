/*******************************************************************************
 *  Copyright (c) 2009, 2015 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.ua.tests.help.webextension;

import org.eclipse.core.runtime.Platform;
import org.eclipse.help.webapp.AbstractFrame;
import org.osgi.framework.FrameworkUtil;

public class TestHelpToolbarFrame extends AbstractFrame {

	@Override
	public String getName() {
		return "testHelpToolbarFrame";
	}

	@Override
	public String getURL() {
		return "/titlesearch/jsp/advanced/testHelpToolbarFrame.jsp";
	}

	@Override
	public int getLocation() {
		return AbstractFrame.HELP_TOOLBAR;
	}

	@Override
	public String getSize() {
		return "*";
	}

	@Override
	public String getFrameAttributes() {
		return "\"marginwidth=\"0\" marginheight=\"0\" frameborder=\"0\" scrolling=\"no\""; //$NON-NLS-1$
	}

	@Override
	public boolean isVisible() {
		return Platform.getPreferencesService().getBoolean
		(FrameworkUtil.getBundle(getClass()).getSymbolicName(), "extraFrame", false, null);
	}

}
