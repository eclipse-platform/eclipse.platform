/*******************************************************************************
 * Copyright (c) 2009, 2015 IBM Corporation and others.
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
 ******************************************************************************/

package org.eclipse.e4.core.internal.tests.contexts.inject;

/**
 *
 */
public class StringPrintService implements PrintService {
	private StringBuilder buf = new StringBuilder();

	@Override
	public void print(String message) {
		buf.append(message);
	}

	@Override
	public String toString() {
		return buf.toString();
	}

}
