/*******************************************************************************
 * Copyright (c) 2008, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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