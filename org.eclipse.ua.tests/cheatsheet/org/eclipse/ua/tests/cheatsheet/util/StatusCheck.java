/*******************************************************************************
 * Copyright (c) 2006, 2015 IBM Corporation and others.
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

package org.eclipse.ua.tests.cheatsheet.util;

import org.junit.Assert;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;


/**
 * Utilities for checking status
 */
public class StatusCheck {

	public static void assertStatusContains(IStatus status, String text) {
		if (!status.getMessage().contains(text)) {
			Assert.fail("Expected status message to contain '" + text + "' actual message is '"
					+ status.getMessage() + "'");
		}
	}

	public static void assertMultiStatusContains(IStatus status, String text) {
		Assert.assertTrue(status instanceof MultiStatus);
		IStatus[] children = status.getChildren();
		for (IStatus element : children) {
			if (element.getMessage().contains(text)) {
				return;
			}
		}
		if (!status.getMessage().contains(text)) {
			Assert.fail("Expected status message to contain '" + text + "' status.toString = '"
					+ status.toString() + "'");
		}
	}
}
