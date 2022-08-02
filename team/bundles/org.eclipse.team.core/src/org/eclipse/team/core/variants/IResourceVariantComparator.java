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
package org.eclipse.team.core.variants;

import org.eclipse.core.resources.IResource;

/**
 * An <code>IResourceVariantComparator</code> is provided by a <code>Subscriber</code>
 * and used by a <code>SyncInfo</code> to calculate the sync
 * state of the workspace resources. Subscribers should provide a criteria
 * best suited for their environment. For example, an FTP subscriber could choose to use file
 * size or file timestamps as comparison criteria whereas a CVS workspace subscriber would
 * use file revision numbers.
 *
 * @see org.eclipse.team.core.synchronize.SyncInfo
 * @see org.eclipse.team.core.subscribers.Subscriber
 * @since 3.0
 */
public interface IResourceVariantComparator {

	/**
	 * Returns <code>true</code> if the local resource
	 * matches the remote resource based on this criteria and <code>false</code>
	 * otherwise. Comparing should be fast and based on cached information.
	 *
	 * @param local the local resource to be compared
	 * @param remote the remote resources to be compared
	 * @return <code>true</code> if local and remote are equal based on this criteria and <code>false</code>
	 * otherwise.
	 */
	public boolean compare(IResource local, IResourceVariant remote);

	/**
	 * Returns <code>true</code> if the base resource
	 * matches the remote resource based on this criteria and <code>false</code>
	 * otherwise. Comparing should be fast and based on cached information.
	 *
	 * @param base the base resource to be compared
	 * @param remote the remote resources to be compared
	 * @return <code>true</code> if base and remote are equal based on this criteria and <code>false</code>
	 * otherwise.
	 */
	public boolean compare(IResourceVariant base, IResourceVariant remote);

	/**
	 * Answers <code>true</code> if the base tree is maintained by this comparator's
	 * subscriber. If the base tree is not considered than the subscriber can
	 * be considered as not supported three-way comparisons. Instead
	 * comparisons are made between the local and remote only without
	 * consideration for the base.
	 * @return whether this comparator is three-way or two-way
	 */
	public boolean isThreeWay();
}
