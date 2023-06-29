/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
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
package org.eclipse.debug.tests.launching;

/**
 * Used by {@link ArgumentParsingTests}.
 */
public class ArgumentsPrinter {
	public static void main(String[] args) {
		for (String arg : args) {
			System.out.println(arg);
		}
	}
}
