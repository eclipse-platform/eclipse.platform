/*******************************************************************************
 * Copyright (c) 2007, 2017 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.core.mapping;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.resources.mapping.ResourceTraversal;
import org.eclipse.team.core.mapping.ISynchronizationScope;

public class ScopeChangeEvent {

	private final ISynchronizationScope scope;
	private final ResourceMapping[] originalMappings;
	private final ResourceTraversal[] originalTraversals;
	private boolean expanded;
	private boolean contracted;

	public ScopeChangeEvent(ISynchronizationScope scope) {
		this.scope = scope;
		originalMappings = scope.getMappings();
		originalTraversals = scope.getTraversals();
	}

	public boolean hasAdditionalMappings() {
		return scope.getMappings().length > originalMappings.length;
	}

	public ResourceTraversal[] getUncoveredTraversals(CompoundResourceTraversal traversal) {
		CompoundResourceTraversal originals = new CompoundResourceTraversal();
		originals.addTraversals(originalTraversals);
		return originals.getUncoveredTraversals(traversal);
	}

	public void setExpanded(boolean expanded) {
		this.expanded = expanded;
	}

	public boolean isExpanded() {
		return expanded;
	}

	public void setContracted(boolean contracted) {
		this.contracted = contracted;
	}

	public boolean isContracted() {
		return contracted;
	}

	public ResourceMapping[] getChangedMappings() {
		ResourceMapping[] currentMappings = scope.getMappings();
		ResourceMapping[] changedMappings;
		if (currentMappings.length > originalMappings.length) {
			// The number of mappings has increased so we should report the new mappings
			Set<ResourceMapping> originalSet = new HashSet<>();
			List<ResourceMapping> result = new ArrayList<>();
			Collections.addAll(originalSet, originalMappings);
			for (ResourceMapping mapping : currentMappings) {
				if (!originalSet.contains(mapping)) {
					result.add(mapping);
				}
			}
			changedMappings = result.toArray(new ResourceMapping[result.size()]);
		} else if (isContracted()) {
			// The number of mappings may be smaller so report the removed mappings
			Set<ResourceMapping> finalSet = new HashSet<>();
			List<ResourceMapping> result = new ArrayList<>();
			Collections.addAll(finalSet, currentMappings);
			for (ResourceMapping mapping : originalMappings) {
				if (!finalSet.contains(mapping)) {
					result.add(mapping);
				}
			}
			changedMappings = result.toArray(new ResourceMapping[result.size()]);
		} else {
			changedMappings = new ResourceMapping[0];
		}
		return changedMappings;
	}

	public ResourceTraversal[] getChangedTraversals(CompoundResourceTraversal refreshTraversals) {
		ResourceTraversal[] changesTraversals;
		if (isExpanded()) {
			changesTraversals = getUncoveredTraversals(refreshTraversals);
		} else if (isContracted()) {
			CompoundResourceTraversal finalTraversals = new CompoundResourceTraversal();
			finalTraversals.addTraversals(scope.getTraversals());
			changesTraversals = finalTraversals.getUncoveredTraversals(originalTraversals);
		} else {
			changesTraversals = new ResourceTraversal[0];
		}
		return changesTraversals;
	}

	public boolean shouldFireChange() {
		return isExpanded() || isContracted() || hasAdditionalMappings();
	}
}
