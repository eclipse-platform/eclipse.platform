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

import static org.assertj.core.api.Assertions.assertThat;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;

/**
 * Utilities for checking status
 */
public class StatusCheck {

	public static void assertStatusContains(IStatus status, String text) {
		assertThat(status.getMessage()).contains(text);
	}

	public static void assertMultiStatusContains(IStatus status, String text) {
		assertThat(status).isInstanceOf(MultiStatus.class);
		IStatus[] children = status.getChildren();
		for (IStatus element : children) {
			if (element.getMessage().contains(text)) {
				return;
			}
		}
		assertThat(status.getMessage()).contains(text);
	}
}
