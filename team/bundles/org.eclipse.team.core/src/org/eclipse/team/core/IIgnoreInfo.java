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
package org.eclipse.team.core;

/**
 * An ignore info specifies both the pattern and the enabled state of a globally
 * ignored pattern.
 *
 * @since 2.0
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface IIgnoreInfo {
	/**
	 * Return the string specifying the pattern of this ignore. The string
	 * may include the wildcard characters '*' and '?'. If you wish to
	 * include either of these characters verbatim (i.e. you do not wish
	 * them to expand to wildcards), you must escape them with a backslash '\'.
	 * <p>
	 * If you are using string literals in Java to represent the patterns, don't
	 * forget escape characters are represented by "\\".
	 *
	 * @return the pattern represented by this ignore info
	 */
	public String getPattern();
	/**
	 * Return whether or not this ignore info is enabled. A disabled ignore
	 * info remains in the global ignore list, but no attempt is made to match
	 * with it to determine resource ignore state.
	 *
	 * @return whether the ignore info is enabled
	 */
	public boolean getEnabled();
}
