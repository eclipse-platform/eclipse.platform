/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
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
package org.eclipse.team.core.mapping;

import java.util.EventListener;

import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.resources.mapping.ResourceTraversal;

/**
 * Listener for synchronization scope changes.
 * <p>
 * Clients may implement this interface.
 * </p>
 *
 * @see ISynchronizationScope
 *
 * @since 3.2
 */
public interface ISynchronizationScopeChangeListener extends EventListener {

	/**
	 * Notification that the scope has changed. The change may be the inclusion
	 * of new resource mappings, new resource traversals or both or the removal
	 * of mappings (in which case the given traversals will cover the set of
	 * resources that are no longer in the scope). This can be due to changes to
	 * the resource mappings used to calculate the traversals of the scope or
	 * because new resources have come under the control of the repository
	 * provider (or other entity) that generated the scope in the first place.
	 * Clients can determine whether a given mappings were removed by querying
	 * the scope for traversals. If the mapping has no traversals, the mapping
	 * represents a removal.
	 * <p>
	 * Clients can use the following rules to interpret the change:
	 * <ol>
	 * <li>If the mappings are not empty, clients should check to see if the
	 * the scope contains traversals for the any of the mappings. If it does,
	 * the given mappings have been added to the scope. If it doesn't the
	 * mappings represent removals. A change event will never include both new
	 * and removed mappings.
	 * <li>If the mappings are added mappings, and the traversals are empty,
	 * then the addition of the mappings did not change the resources covered by
	 * the scope.
	 * <li>If the mappings are added mappings, and the traversals are not
	 * empty, then the additional mappings also caused additional resources to
	 * be included in the scope. The given traversals cover the resources that
	 * have been added to the scope.
	 * <li>If the mappings are removed mappings, and the traversals are not
	 * empty, then the removed mappings also caused resources to be removed from
	 * the scope. The given traversals cover the resources that have been
	 * removed to the scope.
	 * <li>If the mappings are empty and the traversals are not, the traversals
	 * cover resources that have been added to the scope due to a change in the
	 * logical model or the resource under the control of the repository
	 * providers that manages the scope.
	 * </ol>
	 *
	 * @param scope
	 *            the scope that has changed
	 * @param mappings
	 *            the new mappings or removed mappings (may be empty)
	 * @param traversals
	 *            the new traversals or removed traversals (may be empty)
	 */
	void scopeChanged(ISynchronizationScope scope, ResourceMapping[] mappings,
			ResourceTraversal[] traversals);
}
