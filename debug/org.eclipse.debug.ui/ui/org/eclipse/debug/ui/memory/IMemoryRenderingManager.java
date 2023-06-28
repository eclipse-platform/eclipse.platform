/*******************************************************************************
 * Copyright (c) 2004, 2008 IBM Corporation and others.
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



/**
 * Manager for memory renderings. Provides facilities for creating
 * renderings and retrieving memory rendering bindings.
 * @since 3.1
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 */
public interface IMemoryRenderingManager extends IMemoryRenderingBindingsProvider {


	/**
	 * Returns all contributed memory rendering types.
	 *
	 * @return all contributed memory rendering types
	 */
	IMemoryRenderingType[] getRenderingTypes();

	/**
	 * Returns the memory rendering type with the given identifier, or
	 * <code>null</code> if none.  The memory rendering manager will
	 * search through rendering types that are contributed via explicit
	 * rendering bindings.  (i.e. rendering types contributed via the
	 * memoryRenderings extension point). This method will not return
	 * rendering types that are contributed by a memory binding provider.
	 *
	 * @param id memory rendering type identifier
	 * @return the memory rendering type with the given identifier, or
	 * <code>null</code> if none
	 */
	IMemoryRenderingType getRenderingType(String id);

}


