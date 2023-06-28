/*******************************************************************************
 * Copyright (c) 2006, 2011 IBM Corporation and others.
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
 *     Wind River - Pawel Piech - Need a clarification on usage of IElement*Provider interfaces with update arrays (Bug 213609)
 *******************************************************************************/
package org.eclipse.debug.internal.ui.viewers.model.provisional;

/**
 * Provides context sensitive labels. Can be registered as an adapter for an element,
 * or implemented directly.
 * <p>
 * Note: provider methods are called in the Display thread of the viewer.
 * To avoid blocking the UI, long running operations should be performed
 * asynchronously.
 * </p>
 *
 * @since 3.3
 */
public interface IElementLabelProvider {

	/**
	 * Updates the specified labels.
	 *
	 * @param updates Each update specifies the element and context for which a label is requested and
	 *  stores label attributes.  The update array is guaranteed to have at least one element, and for
	 *  all updates to have the same presentation context.
	 */
	void update(ILabelUpdate[] updates);
}
