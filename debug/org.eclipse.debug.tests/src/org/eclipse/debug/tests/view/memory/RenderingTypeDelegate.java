/*******************************************************************************
 *  Copyright (c) 2000, 2009 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.debug.tests.view.memory;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.ui.memory.IMemoryRendering;
import org.eclipse.debug.ui.memory.IMemoryRenderingTypeDelegate;

/**
 * Test memory rendering type delegate.
 *
 * @since 3.1
 */
public class RenderingTypeDelegate implements IMemoryRenderingTypeDelegate {

	/**
	 * @see org.eclipse.debug.ui.memory.IMemoryRenderingTypeDelegate#createRendering(java.lang.String)
	 */
	@Override
	public IMemoryRendering createRendering(String id) throws CoreException {
		return null;
	}
}
