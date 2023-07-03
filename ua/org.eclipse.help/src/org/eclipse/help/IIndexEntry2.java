/*******************************************************************************
 * Copyright (c) 2009, 2015 IBM Corporation and others.
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
package org.eclipse.help;

/**
 * IIndexEntry2 is an index entry which may have see elements as children
 *
 * @since 3.5
 */
public interface IIndexEntry2 extends IIndexEntry {

	/**
	 * Obtains see references for this entry
	 *
	 * @return array of ITopic
	 */
	public IIndexSee[] getSees();

}
