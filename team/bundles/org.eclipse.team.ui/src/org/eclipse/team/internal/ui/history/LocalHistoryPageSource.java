/*******************************************************************************
 * Copyright (c) 2006, 2007 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ui.history;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.ui.history.HistoryPageSource;
import org.eclipse.team.ui.history.IHistoryPageSource;
import org.eclipse.ui.part.Page;

public class LocalHistoryPageSource extends HistoryPageSource {

	private static LocalHistoryPageSource instance;

	public static IFile getFile(Object object) {
		IResource resource = Utils.getResource(object);
		if (resource instanceof IFile) {
			return (IFile) resource;
		}
		return null;
	}

	@Override
	public boolean canShowHistoryFor(Object object) {
		return getFile(object) != null;
	}

	@Override
	public Page createPage(Object object) {
		LocalHistoryPage page = new LocalHistoryPage();
		return page;
	}

	public synchronized static IHistoryPageSource getInstance() {
		if (instance == null)
			instance = new LocalHistoryPageSource();
		return instance;
	}

}
