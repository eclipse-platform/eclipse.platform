/*******************************************************************************
 * Copyright (c) 2006, 2017 IBM Corporation and others.
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
package org.eclipse.team.core.diff;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.core.diff.provider.Diff;

/**
 * A specialized <code>DiffNodeFilter</code> that does not require a progress monitor.
 * This enables these filters to be used when determining menu enablement or other
 * operations that must be short running.
 *
 * @see IDiff
 * @see IDiffTree
 * @see DiffFilter
 * @since 3.2
 */
public abstract class FastDiffFilter extends DiffFilter {

	public static final FastDiffFilter getStateFilter(final int[] states, final int mask) {
		return new FastDiffFilter() {
			@Override
			public boolean select(IDiff node) {
				int status = ((Diff)node).getStatus();
				for (int state : states) {
					if ((status & mask) == state) {
						return true;
					}
				}
				return false;
			}
		};
	}

	@Override
	public final boolean select(IDiff diff, IProgressMonitor monitor) {
		return select(diff);
	}

	/**
	 * Return <code>true</code> if the provided <code>IDiffNode</code> matches the filter.
	 *
	 * @param diff the <code>IDiffNode</code> to be tested
	 * @return <code>true</code> if the <code>IDiffNode</code> matches the filter
	 */
	public abstract boolean select(IDiff diff);
}
