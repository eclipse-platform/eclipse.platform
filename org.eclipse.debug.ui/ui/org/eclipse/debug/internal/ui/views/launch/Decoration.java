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
package org.eclipse.debug.internal.ui.views.launch;

import org.eclipse.debug.core.model.IThread;

/**
 * A decoration in an editor, created by the debugger.
 */
public abstract class Decoration {

	/**
	 * Removes this decoration
	 */
	public abstract void remove();

	/**
	 * Returns the thread this decoration decorates.
	 *
	 * @return thread associated with this decoration
	 */
	public abstract IThread getThread();

}
