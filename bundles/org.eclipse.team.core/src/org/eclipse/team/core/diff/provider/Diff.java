/*******************************************************************************
 * Copyright (c) 2000, 2017 IBM Corporation and others.
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
package org.eclipse.team.core.diff.provider;

import org.eclipse.core.runtime.IPath;
import org.eclipse.team.core.diff.IDiff;
import org.eclipse.team.core.diff.IThreeWayDiff;
import org.eclipse.team.core.diff.ITwoWayDiff;
import org.eclipse.team.internal.core.mapping.SyncInfoToDiffConverter;

/**
 * Abstract implementation of {@link IDiff} that can be subclassed by
 * clients.
 *
 * @see ITwoWayDiff
 * @see IThreeWayDiff
 * @since 3.2
 */
public abstract class Diff implements IDiff {

	/**
	 * Constant (bit mask) that defines the area of the status that is reserved
	 * for use by this abstract class for encoding the kind of the diff.
	 *
	 * @see #getStatus()
	 */
	public static final int KIND_MASK = 0xFF;

	private final IPath path;

	private final int status;

	/**
	 * Create a diff node.
	 *
	 * @param path the path of the diff
	 * @param status the status of the diff. The kind should be encoded in the
	 *            status along with any additional flags required by a subclass.
	 */
	protected Diff(IPath path, int status) {
		this.path = path;
		this.status = status;
	}

	@Override
	public IPath getPath() {
		return path;
	}

	@Override
	public int getKind() {
		return getStatus() & KIND_MASK;
	}

	/**
	 * Return the status of the diff node. The status is a bit field that
	 * contains the kind and any additional status information that subclasses
	 * need to encode. The first byte of the status is reserved for use by this
	 * abstract class as indicated by the <code>KIND_MASK</code>.
	 *
	 * @return the status of the diff node
	 */
	public final int getStatus() {
		return status;
	}

	@Override
	public String toDiffString() {
		int kind = getKind();
		String label = SyncInfoToDiffConverter.diffKindToString(kind);
		return label;
	}

	@Override
	public int hashCode() {
		return getPath().hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this)
			return true;
		if (obj instanceof Diff) {
			Diff other = (Diff) obj;
			return other.getPath().equals(getPath()) && getStatus() == other.getStatus();
		}
		return false;
	}
}
