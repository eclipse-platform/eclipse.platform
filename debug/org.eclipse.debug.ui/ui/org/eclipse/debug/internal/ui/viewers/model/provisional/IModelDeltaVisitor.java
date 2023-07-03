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


/**
 * An objects that visits model deltas.
 *
 * @since 3.3
 */
public interface IModelDeltaVisitor {

	/**
	 * Visits the given model delta.
	 *
	 * @param delta the delta to visit
	 * @param depth depth in the delta where 0 == root node
	 * @return <code>true</code> if the model delta's children should
	 *		be visited; <code>false</code> if they should be skipped.
	 */
	boolean visit(IModelDelta delta, int depth);

}
