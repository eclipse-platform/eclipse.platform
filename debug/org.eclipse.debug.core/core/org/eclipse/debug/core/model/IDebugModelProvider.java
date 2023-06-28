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
package org.eclipse.debug.core.model;

/**
 * A debug model provider provides debug model identifiers.
 * This interface is used as an adapter to determine what
 * debug models are associated with an adaptable object.
 * Generally, when debugging one language, only one debug
 * model is associated with a debug element. However,
 * a debug model that provides cross language debugging
 * may represent several debug models.
 * <p>
 * Clients are intended to implement this interface.
 * </p>
 * @since 3.0
 */
public interface IDebugModelProvider {

	/**
	 * Returns a collection of debug model identifiers.
	 *
	 * @return a collection of debug model identifiers
	 */
	String[] getModelIdentifiers();

}
