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
 *     IBM - Initial API and implementation
 *******************************************************************************/
package org.eclipse.core.tools;

import org.eclipse.ui.part.ViewPart;

/**
 * A common base class for all Spy Plug-in views
 */
public abstract class SpyView extends ViewPart {
	/**
	 * SpyView constructor comment.
	 */
	public SpyView() {
		super();
	}

	/**
	 * Asks this part to take focus within the workbench. Does nothing.
	 */
	@Override
	public void setFocus() {
		// do nothing
	}
}
