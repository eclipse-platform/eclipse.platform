/*******************************************************************************
 * Copyright (c) 2026 Vogella GmbH and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Vogella GmbH - initial API and implementation
 *******************************************************************************/
package org.eclipse.ui.internal.buildmonitor;

import java.util.HashMap;
import java.util.Map;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.Platform;

/**
 * Maps a builder class name to the name its extension declares, so the tree can
 * show "Java Builder" rather than the implementation class.
 */
final class BuilderLabels {

	private static Map<String, String> namesByClass;

	private BuilderLabels() {
		// Do not instantiate.
	}

	/** The declared name of the builder, or the class name when there is none. */
	static synchronized String of(String builderClass) {
		if (namesByClass == null) {
			namesByClass = read();
		}
		return namesByClass.getOrDefault(builderClass, builderClass);
	}

	private static Map<String, String> read() {
		Map<String, String> names = new HashMap<>();
		IExtensionPoint point = Platform.getExtensionRegistry()
				.getExtensionPoint(ResourcesPlugin.PI_RESOURCES, ResourcesPlugin.PT_BUILDERS);
		if (point == null) {
			return names;
		}
		for (IExtension extension : point.getExtensions()) {
			String name = extension.getLabel();
			if (name == null || name.isBlank()) {
				continue;
			}
			for (IConfigurationElement element : extension.getConfigurationElements()) {
				collectRunClasses(element, name, names);
			}
		}
		return names;
	}

	private static void collectRunClasses(IConfigurationElement element, String name, Map<String, String> names) {
		if ("run".equals(element.getName())) { //$NON-NLS-1$
			String className = element.getAttribute("class"); //$NON-NLS-1$
			if (className != null) {
				names.putIfAbsent(className, name);
			}
			return;
		}
		for (IConfigurationElement child : element.getChildren()) {
			collectRunClasses(child, name, names);
		}
	}
}
