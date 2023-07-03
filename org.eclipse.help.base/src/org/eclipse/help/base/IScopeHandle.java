/*******************************************************************************
 * Copyright (c) 2010, 2015 IBM Corporation and others.
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

package org.eclipse.help.base;


/**
 * Container class for associating AbstractHelpScopes with an ID
 *
 * @since 3.6
 */
public interface IScopeHandle {

	/**
	 * Get the AbstractHelpScope associated with this handle
	 *
	 * @return AbstractHelpScope
	 */
	public AbstractHelpScope getScope();

	/**
	 * Get the String ID associated with this handle
	 *
	 * @return ID
	 */
	public String getId();
}
