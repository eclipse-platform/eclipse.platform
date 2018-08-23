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
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.team.internal.ccvs.ui;

import org.eclipse.team.ui.history.HistoryPageSource;
import org.eclipse.ui.part.Page;

public class CVSHistoryPageSource extends HistoryPageSource {

	public Page createPage(Object object) {
		CVSHistoryPage page = new CVSHistoryPage(object);
		return page;
	}

	public boolean canShowHistoryFor(Object object) {
		return CVSHistoryPage.getCVSFile(object) != null;
	}

}
