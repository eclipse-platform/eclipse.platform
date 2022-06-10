/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
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
package org.eclipse.e4.core.internal.contexts;

import org.eclipse.e4.core.contexts.IEclipseContext;

// TBD make this public and remove #disposed() method from RunAndTrack
/**
 * Objects wanted to be informed when the context is disposed can implement
 * this interface.
 */
public interface IContextDisposalListener {

	/**
	 * Notifies that the context has been disposed
	 * @param context the context being disposed
	 */
	void disposed(IEclipseContext context);
}
