/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
			public boolean select(IDiff node) {
				int status = ((Diff)node).getStatus();
				for (int i = 0; i < states.length; i++) {
					int state = states[i];
					if ((status & mask) == state) {
						return true;
					}
				}
				return false;
			}
		};
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.core.diff.DiffNodeFilter#select(org.eclipse.team.core.diff.IDiffNode, org.eclipse.core.runtime.IProgressMonitor)
	 */
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
