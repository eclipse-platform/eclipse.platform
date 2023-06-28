/*******************************************************************************
 * Copyright (c) 2004, 2013 IBM Corporation and others.
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
package org.eclipse.debug.internal.ui.memory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.Platform;
import org.eclipse.debug.core.model.IMemoryBlock;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.debug.ui.memory.AbstractMemoryRenderingBindingsProvider;
import org.eclipse.debug.ui.memory.IMemoryRendering;
import org.eclipse.debug.ui.memory.IMemoryRenderingManager;
import org.eclipse.debug.ui.memory.IMemoryRenderingType;

/**
 * The memory rendering manager.
 *
 * @see org.eclipse.debug.ui.memory.IMemoryRenderingManager
 * @since 3.1
 */
public class MemoryRenderingManager extends AbstractMemoryRenderingBindingsProvider implements IMemoryRenderingManager {

	// map of rendering type ids to valid rendering types
	private Map<String, MemoryRenderingType> fRenderingTypes = new HashMap<>();

	// list of renderingBindings
	private List<RenderingBindings> fBindings = new ArrayList<>();

	// singleton manager
	private static MemoryRenderingManager fgDefault;

	// elements in the memory renderings extension point
	public static final String ELEMENT_MEMORY_RENDERING_TYPE = "renderingType"; //$NON-NLS-1$
	public static final String ELEMENT_RENDERING_BINDINGS = "renderingBindings"; //$NON-NLS-1$

	/**
	 * Returns the memory rendering manager.
	 *
	 * @return the memory rendering manager
	 */
	public static IMemoryRenderingManager getDefault() {
		if (fgDefault == null) {
			fgDefault = new MemoryRenderingManager();
		}
		return fgDefault;
	}

	/**
	 * Construts a new memory rendering manager. Only to be called by
	 * DebugUIPlugin.
	 */
	private MemoryRenderingManager() {
		initializeRenderings();
	}

	public IMemoryRendering createRendering(String id) throws CoreException {
		IMemoryRenderingType type = getRenderingType(id);
		if (type != null) {
			return type.createRendering();
		}
		return null;
	}

	@Override
	public IMemoryRenderingType[] getRenderingTypes() {
		Collection<MemoryRenderingType> types = fRenderingTypes.values();
		return types.toArray(new IMemoryRenderingType[types.size()]);
	}

	@Override
	public IMemoryRenderingType getRenderingType(String id) {
		return fRenderingTypes.get(id);
	}

	@Override
	public IMemoryRenderingType[] getDefaultRenderingTypes(IMemoryBlock block) {
		List<IMemoryRenderingType> allTypes = new ArrayList<>();
		Iterator<RenderingBindings> iterator = fBindings.iterator();
		while (iterator.hasNext()) {
			RenderingBindings binding = iterator.next();
			IMemoryRenderingType[] renderingTypes = binding.getDefaultRenderingTypes(block);
			for (IMemoryRenderingType type : renderingTypes) {
				if (!allTypes.contains(type)) {
					allTypes.add(type);
				}
			}
		}
		return allTypes.toArray(new IMemoryRenderingType[allTypes.size()]);
	}

	@Override
	public IMemoryRenderingType getPrimaryRenderingType(IMemoryBlock block) {
		for (RenderingBindings binding : fBindings) {
			IMemoryRenderingType renderingType = binding.getPrimaryRenderingType(block);
			if (renderingType != null) {
				return renderingType;
			}
		}
		return null;
	}

	@Override
	public IMemoryRenderingType[] getRenderingTypes(IMemoryBlock block) {
		List<IMemoryRenderingType> allTypes = new ArrayList<>();
		for (RenderingBindings binding : fBindings) {
			IMemoryRenderingType[] renderingTypes = binding.getRenderingTypes(block);
			for (IMemoryRenderingType type : renderingTypes) {
				if (!allTypes.contains(type)) {
					allTypes.add(type);
				}
			}
		}
		return allTypes.toArray(new IMemoryRenderingType[allTypes.size()]);
	}

	/**
	 * Processes memory rendering contributions.
	 */
	private void initializeRenderings() {
		IExtensionPoint extensionPoint = Platform.getExtensionRegistry().getExtensionPoint(DebugUIPlugin.getUniqueIdentifier(), IDebugUIConstants.EXTENSION_POINT_MEMORY_RENDERINGS);
		IConfigurationElement[] configurationElements = extensionPoint.getConfigurationElements();
		for (IConfigurationElement element : configurationElements) {
			String name = element.getName();
			if (name.equals(ELEMENT_MEMORY_RENDERING_TYPE)) {
				MemoryRenderingType type = new MemoryRenderingType(element);
				try {
					type.validate();
					fRenderingTypes.put(type.getId(), type);
				} catch (CoreException e) {
					DebugUIPlugin.log(e);
				}
			} else if (name.equals(ELEMENT_RENDERING_BINDINGS)) {
				RenderingBindings bindings = new RenderingBindings(element);
				try {
					bindings.validate();
					fBindings.add(bindings);
					bindings.addListener(this::fireBindingsChanged);
				} catch (CoreException e) {
					DebugUIPlugin.log(e);
				}
			}
		}
	}
}
