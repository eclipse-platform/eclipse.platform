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
 * Used to save and restore viewer selection/expansion state. A memento
 * provider adapter should be available from a viewer input element
 * in order to support viewer state save/restore.
 * <p>
 * Note: provider methods are called in the Display thread of the viewer.
 * To avoid blocking the UI, long running operations should be performed
 * asynchronously.
 * </p>
 *
 * @since 3.3
 */
public interface IElementMementoProvider {

	/**
	 * Creates and stores a mementos for the elements specified in the requests.
	 * A request should be cancelled if a memento is not supported for the
	 * specified element or context.
	 *
	 * @param requests Specifies elements and provides memento stores.
	 *  The requests array is guaranteed to have at least one element, and for
	 *  all requests to have the same presentation context.
	 */
	void encodeElements(IElementMementoRequest[] requests);

	/**
	 * Determines whether mementos represent associated elements specified in the requests.
	 *
	 * @param requests Specifies each element and previously created memento.
	 *  The requests array is guaranteed to have at least one element, and for
	 *  all requests to have the same presentation context.
	 */
	void compareElements(IElementCompareRequest[] requests);

}
