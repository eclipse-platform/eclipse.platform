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

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IPath;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.core.diff.IDiff;
import org.eclipse.team.core.diff.IThreeWayDiff;
import org.eclipse.team.core.diff.ITwoWayDiff;
import org.eclipse.team.internal.core.Messages;
import org.eclipse.team.internal.core.mapping.SyncInfoToDiffConverter;

/**
 * Implementation of {@link IThreeWayDiff}.
 *
 * @since 3.2
 * @noextend This class is not intended to be subclassed by clients.
 */
public class ThreeWayDiff extends Diff implements IThreeWayDiff {

	private final ITwoWayDiff localChange;
	private final ITwoWayDiff remoteChange;

	/**
	 * Create a three-way delta from the two changes. At least one change
	 * must be provided (i.e. either change may be <code>null</code>
	 * but at least one must be non-<code>null</code>).
	 * @param localChange the local change in the model object or <code>null</code> if there is no local change
	 * @param remoteChange the remote change in the model object or <code>null</code> if there is no local change
	 */
	public ThreeWayDiff(ITwoWayDiff localChange, ITwoWayDiff remoteChange) {
		super(calculatePath(localChange, remoteChange), calculateKind(localChange, remoteChange) | calculateDirection(localChange, remoteChange));
		this.localChange = localChange;
		this.remoteChange = remoteChange;
	}

	private static IPath calculatePath(ITwoWayDiff localChange, ITwoWayDiff remoteChange) {
		if (localChange != null && remoteChange != null)
			Assert.isTrue(localChange.getPath().equals(remoteChange.getPath()));
		if (localChange != null)
			return localChange.getPath();
		if (remoteChange != null)
			return remoteChange.getPath();
		Assert.isLegal(false, "Either or local or remote change must be supplied"); //$NON-NLS-1$
		return null; // Will never be reached
	}

	private static int calculateDirection(ITwoWayDiff localChange, ITwoWayDiff remoteChange) {
		int direction = 0;
		if (localChange != null && localChange.getKind() != NO_CHANGE) {
			direction |= OUTGOING;
		}
		if (remoteChange != null && remoteChange.getKind() != NO_CHANGE) {
			direction |= INCOMING;
		}
		return direction;
	}

	private static int calculateKind(ITwoWayDiff localChange, ITwoWayDiff remoteChange) {
		int localKind = NO_CHANGE;
		if (localChange != null)
			localKind = localChange.getKind();
		int remoteKind = NO_CHANGE;
		if (remoteChange != null)
			remoteKind = remoteChange.getKind();
		if (localKind == NO_CHANGE || localKind == remoteKind)
			return remoteKind;
		if (remoteKind == NO_CHANGE)
			return localKind;
		return CHANGE;
	}

	@Override
	public ITwoWayDiff getLocalChange() {
		return localChange;
	}

	@Override
	public ITwoWayDiff getRemoteChange() {
		return remoteChange;
	}

	@Override
	public int getDirection() {
		return getStatus() & CONFLICTING;
	}

	@Override
	public String toDiffString() {
		int kind = getKind();
		String label = ""; //$NON-NLS-1$
		if(kind==IDiff.NO_CHANGE) {
			label = super.toDiffString();
		} else {
			label = SyncInfoToDiffConverter.diffDirectionToString(getDirection());
			label = NLS.bind(Messages.concatStrings, new String[] { label, super.toDiffString() });
		}
		return label;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this)
			return true;
		if (super.equals(obj)) {
			if (obj instanceof ThreeWayDiff) {
				ThreeWayDiff other = (ThreeWayDiff) obj;
				return changesEqual(getLocalChange(), other.getLocalChange())
					&& changesEqual(getRemoteChange(), other.getRemoteChange());
			}
		}
		return false;
	}

	private boolean changesEqual(ITwoWayDiff diff, ITwoWayDiff diff2) {
		if (diff == null)
			return diff2 == null;
		if (diff2 == null)
			return false;
		return diff.equals(diff2);
	}

}
