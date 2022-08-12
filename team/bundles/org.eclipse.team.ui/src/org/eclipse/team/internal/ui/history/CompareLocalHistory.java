/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
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

import org.eclipse.team.internal.ui.TeamUIMessages;

public class CompareLocalHistory extends ShowLocalHistory {

	@Override
	protected boolean isCompare() {
		return true;
	}

	@Override
	protected String getPromptTitle() {
		return TeamUIMessages.CompareLocalHistory_0;
	}
}
