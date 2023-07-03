/*******************************************************************************
 * Copyright (c) 2000, 2013 IBM Corporation and others.
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
package org.eclipse.ui.internal.console;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.ui.console.IConsole;

/**
 * Tests if an IOConsole's type matches the expected value
 *
 * @since 3.1
 */
public class ConsoleTypePropertyTester extends PropertyTester {

	@Override
	public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
		IConsole console = (IConsole) receiver;
		String type = console.getType();
		return type != null ? type.equals(expectedValue) : false;
	}

}
