/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
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
 *     James Blackburn (Broadcom Corp.) - ongoing development
 *     Lars Vogel <Lars.Vogel@vogella.com> - Bug 473427
 *******************************************************************************/
package org.eclipse.core.internal.resources;

import java.util.*;
import org.eclipse.core.internal.utils.Policy;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.*;

public class MarkerTypeDefinitionCache {
	static class MarkerTypeDefinition {
		boolean isPersistent = false;
		Set<String> superTypes;

		MarkerTypeDefinition(IExtension ext) {
			IConfigurationElement[] elements = ext.getConfigurationElements();
			for (IConfigurationElement element : elements) {
				// supertype
				final String elementName = element.getName();
				if (elementName.equalsIgnoreCase("super")) { //$NON-NLS-1$
					String aType = element.getAttribute("type"); //$NON-NLS-1$
					if (aType != null) {
						if (superTypes == null)
							superTypes = new HashSet<>(8);
						//note that all marker type names will be in the intern table
						//already because there is invariably a constant to describe
						//the type name
						superTypes.add(aType.intern());
					}
				}
				// persistent
				if (elementName.equalsIgnoreCase("persistent")) { //$NON-NLS-1$
					String bool = element.getAttribute("value"); //$NON-NLS-1$
					if (bool != null)
						this.isPersistent = Boolean.parseBoolean(bool);
				}
				// XXX: legacy code for support of <transient> tag. remove later.
				if (elementName.equalsIgnoreCase("transient")) { //$NON-NLS-1$
					String bool = element.getAttribute("value"); //$NON-NLS-1$
					if (bool != null)
						this.isPersistent = !Boolean.parseBoolean(bool);
				}
			}
		}
	}

	/**
	 * The marker type definitions.  Maps String (markerId) -&gt; MarkerTypeDefinition
	 */
	protected HashMap<String, MarkerTypeDefinition> definitions;

	/** Constructs a new type cache.
	 */
	public MarkerTypeDefinitionCache() {
		loadDefinitions();
		HashSet<String> toCompute = new HashSet<>(definitions.keySet());
		for (String markerId : definitions.keySet()) {
			if (toCompute.contains(markerId))
				computeSuperTypes(markerId, toCompute);
		}
	}

	/**
	 * Computes the transitive set of super types of the given marker type.
	 * @param markerId The type to compute super types for
	 * @param toCompute The set of types that have not yet had their
	 * supertypes computed.
	 * @return The transitive set of super types for this marker, or null
	 * if this marker is not defined or has no super types.
	 */
	private Set<String> computeSuperTypes(String markerId, Set<String> toCompute) {
		MarkerTypeDefinition def = definitions.get(markerId);
		if (def == null || def.superTypes == null) {
			//nothing to do if there are no supertypes
			toCompute.remove(markerId);
			return null;
		}
		Set<String> transitiveSuperTypes = new HashSet<>(def.superTypes);
		for (String superId : def.superTypes) {
			Set<String> toAdd = null;
			if (toCompute.contains(superId)) {
				//this type's super types have not been compute yet. Do recursive call
				toAdd = computeSuperTypes(superId, toCompute);
			} else {
				// we have already computed this super type's super types (or it doesn't exist)
				MarkerTypeDefinition parentDef = definitions.get(superId);
				if (parentDef != null)
					toAdd = parentDef.superTypes;
			}
			if (toAdd != null)
				transitiveSuperTypes.addAll(toAdd);
		}
		def.superTypes = transitiveSuperTypes;
		toCompute.remove(markerId);
		return transitiveSuperTypes;
	}

	/**
	 * Returns true if the given marker type is defined to be persistent.
	 */
	public boolean isPersistent(String type) {
		MarkerTypeDefinition def = definitions.get(type);
		return def != null && def.isPersistent;
	}

	/**
	 * Returns true if the given target class has the specified type as a super type.
	 */
	public boolean isSubtype(String type, String superType) {
		//types are considered super types of themselves
		if (type.equals(superType))
			return true;
		MarkerTypeDefinition def = definitions.get(type);
		return def != null && def.superTypes != null && def.superTypes.contains(superType);
	}

	private void loadDefinitions() {
		IExtensionPoint point = Platform.getExtensionRegistry().getExtensionPoint(ResourcesPlugin.PI_RESOURCES, ResourcesPlugin.PT_MARKERS);
		IExtension[] types = point.getExtensions();
		definitions = new HashMap<>(types.length);
		for (IExtension type : types) {
			String markerId = type.getUniqueIdentifier();
			if (markerId != null)
				definitions.put(markerId.intern(), new MarkerTypeDefinition(type));
			else
				Policy.log(IStatus.WARNING, "Missing marker id from plugin: " + type.getContributor().getName(), null); //$NON-NLS-1$
		}
	}
}
