/*******************************************************************************
 * Copyright (c) 2004, 2006 IBM Corporation and others.
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
package org.eclipse.debug.ui.memory;

import org.eclipse.core.runtime.CoreException;

/**
 * Represents a type of memory rendering contributed via the <code>memoryRenderings</code>
 * extension point.
 * <p>
 * Clients contributing a rendering usually
 * implement {@link org.eclipse.debug.ui.memory.IMemoryRenderingTypeDelegate}
 * and {@link org.eclipse.debug.ui.memory.IMemoryRendering}. Clients providing
 * dynamic rendering bindings via an
 * {@link org.eclipse.debug.ui.memory.IMemoryRenderingBindingsProvider}
 * may implement this interface.
 * </p>
 * @since 3.1
 */
public interface IMemoryRenderingType {

	/**
	 * Returns a label for this type of memory rendering.
	 *
	 * @return a label for this type of memory rendering
	 */
	String getLabel();

	/**
	 * Returns the unique identifier for this rendering type.
	 *
	 * @return the unique identifier for this rendering type
	 */
	String getId();

	/**
	 * Creates and returns a new rendering of this type or <code>null</code>
	 * if none.
	 *
	 * @return a new rendering of this type
	 * @exception CoreException if an exception occurs creating
	 *  the rendering
	 */
	IMemoryRendering createRendering() throws CoreException;

}
