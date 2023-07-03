/*******************************************************************************
 * Copyright (c) 2005, 2011 IBM Corporation and others.
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
 * A model changed listener is notified of changes in a model. A model
 * is represented by an {@link IModelProxy}.
 *
 * @since 3.2
 * @see IModelProxy
 * @see IModelDelta
 */
public interface IModelChangedListener {

	/**
	 * Notification a model has changed as described by the given delta.
	 *
	 * @param delta model delta
	 * @param proxy proxy that created the delta
	 */
	void modelChanged(IModelDelta delta, IModelProxy proxy);

}
