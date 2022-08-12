/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
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
package org.eclipse.core.internal.utils;

/**
 * Utility methods for manipulating bit masks
 */
public class BitMask {
	/**
	 * Returns true if all of the bits indicated by the mask are set.
	 */
	public static boolean isSet(int flags, int mask) {
		return (flags & mask) == mask;
	}
}
