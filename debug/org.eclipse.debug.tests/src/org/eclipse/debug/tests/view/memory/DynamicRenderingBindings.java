/*******************************************************************************
 *  Copyright (c) 2000, 2013 IBM Corporation and others.
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

import org.eclipse.debug.core.model.IMemoryBlock;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.memory.AbstractMemoryRenderingBindingsProvider;
import org.eclipse.debug.ui.memory.IMemoryRenderingType;

/**
 * Contributed dynamic rendernig bindings.
 * @since 3.1
 */
public class DynamicRenderingBindings extends AbstractMemoryRenderingBindingsProvider {

	//
	private static DynamicRenderingBindings fgSingleton = null;

	// id of rendering type bound by this provider
	private String fId = "rendering_type_1"; //$NON-NLS-1$

	/**
	 * Constructor
	 */
	public DynamicRenderingBindings() {
		fgSingleton = this;
	}

	/**
	 * @see org.eclipse.debug.ui.memory.IMemoryRenderingBindingsProvider#getRenderingTypes(org.eclipse.debug.core.model.IMemoryBlock)
	 */
	@Override
	public IMemoryRenderingType[] getRenderingTypes(IMemoryBlock block) {
		return new IMemoryRenderingType[]{getPrimaryRenderingType(block)};
	}

	/**
	 * @see org.eclipse.debug.ui.memory.IMemoryRenderingBindingsProvider#getDefaultRenderingTypes(org.eclipse.debug.core.model.IMemoryBlock)
	 */
	@Override
	public IMemoryRenderingType[] getDefaultRenderingTypes(IMemoryBlock block) {
		return new IMemoryRenderingType[]{getPrimaryRenderingType(block)};
	}

	/**
	 * @see org.eclipse.debug.ui.memory.IMemoryRenderingBindingsProvider#getPrimaryRenderingType(org.eclipse.debug.core.model.IMemoryBlock)
	 */
	@Override
	public IMemoryRenderingType getPrimaryRenderingType(IMemoryBlock block) {
		return DebugUITools.getMemoryRenderingManager().getRenderingType(fId);
	}

	/**
	 * Sets the current rendering bound to this provider, and notifies
	 * listeners of the change.
	 *
	 * @param id rendering id
	 */
	public static void setBinding(String id) {
		fgSingleton.fId = id;
		fgSingleton.fireBindingsChanged();
	}

}
