/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.core.diff.provider;

import org.eclipse.core.runtime.IPath;
import org.eclipse.team.core.diff.ITwoWayDiff;

/**
 * Implementation of {@link ITwoWayDiff}. By default, this implementation
 * returns <code>null</code> for the {@link #getFromPath() } and
 * {@link #getToPath() }. Subclasses that support move diffs
 * should override these methods.
 * <p>
 * This class may be subclassed by clients.
 *
 * @since 3.2
 */
public class TwoWayDiff extends Diff implements ITwoWayDiff {

	/**
	 * Constant (bit mask) that defines the area of the status that is reserved
	 * for use by this abstract class for encoding the flags of the diff.
	 * However, subclasses may include their own bits in the flag
	 * as long as they do not overlap with the bits in the <code>FLAG_MASK</code>
	 *
	 * @see Diff#getStatus()
	 */
	protected static final int FLAG_MASK = 0xFF00;

	/**
	 * Create a two-way diff
	 * @param path the path of the model object that has changed
	 * @param kind the kind of change
	 * @param flags additional flags that describe the change
	 */
	public TwoWayDiff(IPath path, int kind, int flags) {
		super(path, (kind & KIND_MASK) | (flags & ~KIND_MASK));
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.core.synchronize.ITwoWayDelta#getFlags()
	 */
	public int getFlags() {
		return getStatus() & ~KIND_MASK;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.core.delta.ITwoWayDelta#getMovedToPath()
	 */
	public IPath getToPath() {
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.core.delta.ITwoWayDelta#getMovedFromPath()
	 */
	public IPath getFromPath() {
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.core.diff.provider.Diff#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		if (obj == this)
			return true;
		if (super.equals(obj)) {
			if (obj instanceof TwoWayDiff) {
				TwoWayDiff other = (TwoWayDiff) obj;
				return pathsEqual(getFromPath(), other.getFromPath()) && pathsEqual(getToPath(), other.getToPath());
			}
		}
		return false;
	}

	private boolean pathsEqual(IPath path1, IPath path2) {
		if (path1 == null)
			return path2 == null;
		if (path2 == null)
			return false;
		return path1.equals(path2);
	}

}
