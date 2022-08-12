/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
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
package org.eclipse.team.core.history;

/**
 * ITags can be used to hang additional repository information for IFileRevisions.
 *
 * @see IFileRevision
 * @since 3.2
 */
public interface ITag {

	/**
	 * Returns the name of this tag.
	 * @return String containing the name of the tag
	 */
	public abstract String getName();
}
