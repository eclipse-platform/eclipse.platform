/*******************************************************************************
 * Copyright (c) 2006, 2013 IBM Corporation and others.
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
package org.eclipse.debug.internal.core.commands;

import java.util.Arrays;

import org.eclipse.debug.core.commands.IDebugCommandRequest;

/**
 * @since 3.3
 */
public class DebugCommandRequest extends Request implements IDebugCommandRequest {

	private Object[] fElements;

	public DebugCommandRequest(Object[] elements) {
		fElements = elements;
	}

	@Override
	public Object[] getElements() {
		return fElements;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + " on " + Arrays.toString(fElements); //$NON-NLS-1$
	}

}
