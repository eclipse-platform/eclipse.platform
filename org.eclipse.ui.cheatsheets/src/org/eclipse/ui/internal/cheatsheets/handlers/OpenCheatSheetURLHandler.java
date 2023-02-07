/*******************************************************************************
 * Copyright (c) 2006, 2015 IBM Corporation and others.
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
package org.eclipse.ui.internal.cheatsheets.handlers;

import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.cheatsheets.OpenCheatSheetAction;

/**
 * Opens a cheatsheet given an id, name and a URL to a cheat sheet content file.
 *
 * @since 3.2
 */
public class OpenCheatSheetURLHandler extends AbstractHandler {

	private static final String PARAM_ID_CHEAT_SHEET_ID = "cheatSheetId"; //$NON-NLS-1$

	private static final String PARAM_ID_NAME = "name"; //$NON-NLS-1$

	private static final String PARAM_ID_URL = "url"; //$NON-NLS-1$

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {

		String cheatSheetId = event.getParameter(PARAM_ID_CHEAT_SHEET_ID);
		if (cheatSheetId == null) {
			throw new ExecutionException("missing cheatSheetId parameter"); //$NON-NLS-1$
		}

		String name = event.getParameter(PARAM_ID_NAME);
		if (name == null) {
			throw new ExecutionException("missing name parameter"); //$NON-NLS-1$
		}

		String urlText = event.getParameter(PARAM_ID_URL);
		if (urlText == null) {
			throw new ExecutionException("missing url parameter"); //$NON-NLS-1$
		}

		URL url;
		try {
			url = new URL(urlText);
		} catch (MalformedURLException ex) {
			throw new ExecutionException("malformed url: " + urlText, ex); //$NON-NLS-1$
		}

		OpenCheatSheetAction action = new OpenCheatSheetAction(cheatSheetId,
				name, url);
		action.run();

		return null;
	}

}
