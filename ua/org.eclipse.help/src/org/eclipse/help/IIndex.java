/*******************************************************************************
 * Copyright (c) 2005, 2015 Intel Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Intel Corporation - initial API and implementation
 *     IBM Corporation - 122967 [Help] Remote help system
 *******************************************************************************/
package org.eclipse.help;

/**
 * IIndex represents metaphor of the book index. It contains index entries,
 * each of them is a keyword with references into related content.
 *
 * @since 3.2
 */
public interface IIndex extends IUAElement {

	/**
	 * Obtains the index entries contained in the index.
	 *
	 * @return Array of IIndexEntry
	 */
	IIndexEntry[] getEntries();
}
