/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
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
package org.eclipse.help.internal.base.util;

/**
 * Utility interface for displaying an error message. Implementation may output
 * messages in different ways (console, pop up window).
 */
public interface IErrorUtil {
	public void displayError(String msg);

	public void displayError(String msg, Thread uiThread);
}
