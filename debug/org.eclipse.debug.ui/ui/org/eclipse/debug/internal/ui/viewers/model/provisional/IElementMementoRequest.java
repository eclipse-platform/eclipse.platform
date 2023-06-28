/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
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
package org.eclipse.debug.internal.ui.viewers.model.provisional;

import org.eclipse.ui.IMemento;

/**
 * Request to store a memento for an element in a specific context.
 * <p>
 * Clients are not intended to implement this interface.
 * </p>
 * @since 3.3
 */
public interface IElementMementoRequest extends IViewerUpdate {

	/**
	 * Returns the memento used to persist the element.
	 *
	 * @return memento
	 */
	IMemento getMemento();
}
