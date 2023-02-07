/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
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

package org.eclipse.ui.internal.cheatsheets.actions;

import org.eclipse.swt.widgets.Menu;

/**
 * Interface representing an object that can contribute to a menu
 */

public interface IMenuContributor {
	/**
	 * Contribute zero or more items to a menu
	 * @param menu The menu to add the contributions
	 * @param index The index before cintributions are added
	 * @return the index after contributions are added
	 */
	public int contributeToViewMenu(Menu menu, int index);
}
