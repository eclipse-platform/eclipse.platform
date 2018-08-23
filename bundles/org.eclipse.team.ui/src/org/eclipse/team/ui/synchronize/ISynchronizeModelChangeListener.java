/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
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
package org.eclipse.team.ui.synchronize;

/**
 * Listener that gets informed when the model created by the model provider is created or updated.
 * <p>
 * Clients may implement this interface.
 * </p>
 * @since 3.0
 */
public interface ISynchronizeModelChangeListener {
	/**
	 * Called whenever the input model shown in a diff node viewer is updated.
	 *
	 * @param root the root <code>DiffNode</code> of the model.
	 */
	public void modelChanged(ISynchronizeModelElement root);
}
