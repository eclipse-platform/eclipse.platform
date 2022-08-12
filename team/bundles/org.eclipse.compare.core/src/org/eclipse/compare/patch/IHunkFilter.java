/*******************************************************************************
 * Copyright (c) 2008, 2011 IBM Corporation and others.
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
package org.eclipse.compare.patch;

/**
 * Filter that is used to determine if a hunk should be applied or not
 *
 * @since org.eclipse.compare.core 3.5
 */
public interface IHunkFilter {

	/**
	 * Returns true if the given hunk should be applied
	 *
	 * @param hunk
	 *            the hunk
	 * @return true if the given hunk should be applied
	 */
	public boolean select(IHunk hunk);

}