/*******************************************************************************
 * Copyright (c) 2005, 2008 IBM Corporation and others.
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
package org.eclipse.core.internal.expressions;

import org.eclipse.osgi.util.NLS;


/**
 * Helper class to format message strings.
 *
 * @since 3.1
 */
public class Messages {

	public static String format(String message, Object object) {
		return NLS.bind(message, object);
	}

	public static String format(String message, Object[] objects) {
		return NLS.bind(message, objects);
	}

	private Messages() {
		// Not for instantiation
	}
}