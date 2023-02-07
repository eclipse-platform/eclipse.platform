/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
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
package org.eclipse.help.ui;

import org.eclipse.jface.preference.IPreferencePage;

/**
 * Preference pages that are used for editing help search scope settings should
 * implement this interface.
 *
 * @since 3.1
 */
public interface ISearchScopePage extends IPreferencePage {
	/**
	 * Initializes the search scope page.
	 *
	 * @param ed
	 *            the descriptor of the engine associated with this page
	 * @param scopeSetName
	 *            the name of the current scope set that is used to group data
	 *            shown in this page
	 */
	void init(IEngineDescriptor ed, String scopeSetName);
}