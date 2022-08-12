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
package org.eclipse.team.internal.core.mapping;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.diff.IDiff;
import org.eclipse.team.core.diff.IThreeWayDiff;
import org.eclipse.team.core.diff.ITwoWayDiff;
import org.eclipse.team.core.diff.provider.Diff;
import org.eclipse.team.core.diff.provider.ThreeWayDiff;
import org.eclipse.team.core.history.IFileRevision;
import org.eclipse.team.core.mapping.IResourceDiff;
import org.eclipse.team.core.mapping.provider.ResourceDiff;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.core.variants.IResourceVariant;
import org.eclipse.team.core.variants.IResourceVariantComparator;
import org.eclipse.team.internal.core.Messages;
import org.eclipse.team.internal.core.history.LocalFileRevision;

/**
 * Covert a SyncInfo into a IDiff
 */
public class SyncInfoToDiffConverter {

	private static class PrecalculatedSyncInfo extends SyncInfo {
		public int kind;
		public PrecalculatedSyncInfo(int kind, IResource local, IResourceVariant base, IResourceVariant remote, IResourceVariantComparator comparator) {
			super(local, base, remote, comparator);
			this.kind = kind;
		}

		@Override
		protected int calculateKind() throws TeamException {
			return kind;
		}
	}

	private static SyncInfoToDiffConverter instance;


	public static String diffKindToString(int kind) {
		String label = ""; //$NON-NLS-1$
		if(kind==IDiff.NO_CHANGE) {
			label = Messages.RemoteSyncElement_insync;
		} else {
			switch(kind) {
				case IDiff.CHANGE: label = Messages.RemoteSyncElement_change ; break;
				case IDiff.ADD: label = Messages.RemoteSyncElement_addition; break;
				case IDiff.REMOVE: label = Messages.RemoteSyncElement_deletion; break;
			}
		}
		return label;
	}

	public static String diffDirectionToString(int direction) {
		switch(direction) {
			case IThreeWayDiff.CONFLICTING: return Messages.RemoteSyncElement_conflicting;
			case IThreeWayDiff.OUTGOING: return Messages.RemoteSyncElement_outgoing;
			case IThreeWayDiff.INCOMING: return Messages.RemoteSyncElement_incoming;
		}
		return ""; //$NON-NLS-1$
	}

	public static String diffStatusToString(int status) {
		int kind = status & Diff.KIND_MASK;
		String label = diffKindToString(kind);
		int direction = status & ThreeWayDiff.DIRECTION_MASK;
		if (direction != 0)
			label = NLS.bind(Messages.concatStrings, new String[] { diffDirectionToString(direction), label });
		return label;
	}

	public static int asDiffFlags(int syncInfoFlags) {
		if (syncInfoFlags == SyncInfo.IN_SYNC)
			return IDiff.NO_CHANGE;
		int kind = SyncInfo.getChange(syncInfoFlags);
		int diffFlags = 0;
		switch (kind) {
		case SyncInfo.ADDITION:
			diffFlags = IDiff.ADD;
			break;
		case SyncInfo.DELETION:
			diffFlags = IDiff.REMOVE;
			break;
		case SyncInfo.CHANGE:
			diffFlags = IDiff.CHANGE;
			break;
		}
		int direction = SyncInfo.getDirection(syncInfoFlags);
		switch (direction) {
		case SyncInfo.INCOMING:
			diffFlags |= IThreeWayDiff.INCOMING;
			break;
		case SyncInfo.OUTGOING:
			diffFlags |= IThreeWayDiff.OUTGOING;
			break;
		case SyncInfo.CONFLICTING:
			diffFlags |= IThreeWayDiff.CONFLICTING;
			break;
		}
		return diffFlags;
	}

	private static int asSyncInfoKind(IThreeWayDiff diff) {
		int kind = diff.getKind();
		if (diff.getKind() == IDiff.NO_CHANGE)
			return SyncInfo.IN_SYNC;
		int syncKind = 0;
		switch (kind) {
		case IDiff.ADD:
			syncKind = SyncInfo.ADDITION;
			break;
		case IDiff.REMOVE:
			syncKind = SyncInfo.DELETION;
			break;
		case IDiff.CHANGE:
			syncKind = SyncInfo.CHANGE;
			break;
		}
		int direction = diff.getDirection();
		switch (direction) {
		case IThreeWayDiff.INCOMING:
			syncKind |= SyncInfo.INCOMING;
			break;
		case IThreeWayDiff.OUTGOING:
			syncKind |= SyncInfo.OUTGOING;
			break;
		case IThreeWayDiff.CONFLICTING:
			syncKind |= SyncInfo.CONFLICTING;
			break;
		}
		return syncKind;
	}

	public IDiff getDeltaFor(SyncInfo info) {
		if (info.getComparator().isThreeWay()) {
			ITwoWayDiff local = getLocalDelta(info);
			ITwoWayDiff remote = getRemoteDelta(info);
			return new ThreeWayDiff(local, remote);
		} else {
			if (info.getKind() != SyncInfo.IN_SYNC) {
				IResourceVariant remote = info.getRemote();
				IResource local = info.getLocal();
				int kind;
				if (remote == null) {
					kind = IDiff.REMOVE;
				} else if (!local.exists()) {
					kind = IDiff.ADD;
				} else {
					kind = IDiff.CHANGE;
				}
				if (local.getType() == IResource.FILE) {
					IFileRevision after = asFileState(remote);
					IFileRevision before = getFileRevisionFor((IFile)local);
					return new ResourceDiff(info.getLocal(), kind, 0, before, after);
				}
				// For folders, we don't need file states
				return new ResourceDiff(info.getLocal(), kind);
			}
			return null;
		}
	}

	private ITwoWayDiff getRemoteDelta(SyncInfo info) {
		int direction = SyncInfo.getDirection(info.getKind());
		if (direction == SyncInfo.INCOMING || direction == SyncInfo.CONFLICTING) {
			IResourceVariant ancestor = info.getBase();
			IResourceVariant remote = info.getRemote();
			int kind;
			if (ancestor == null) {
				kind = IDiff.ADD;
			} else if (remote == null) {
				kind = IDiff.REMOVE;
			} else {
				kind = IDiff.CHANGE;
			}
			// For folders, we don't need file states
			if (info.getLocal().getType() == IResource.FILE) {
				IFileRevision before = asFileState(ancestor);
				IFileRevision after = asFileState(remote);
				return new ResourceDiff(info.getLocal(), kind, 0, before, after);
			}

			return new ResourceDiff(info.getLocal(), kind);
		}
		return null;
	}

	private IFileRevision asFileState(final IResourceVariant variant) {
		if (variant == null)
			return null;
		return asFileRevision(variant);
	}

	private IFileRevision getFileRevisionFor(final IFile file) {
		return new LocalFileRevision(file);
	}

	protected ResourceVariantFileRevision asFileRevision(final IResourceVariant variant) {
		return new ResourceVariantFileRevision(variant);
	}

	private ITwoWayDiff getLocalDelta(SyncInfo info) {
		int direction = SyncInfo.getDirection(info.getKind());
		if (direction == SyncInfo.OUTGOING || direction == SyncInfo.CONFLICTING) {
			IResourceVariant ancestor = info.getBase();
			IResource local = info.getLocal();
			int kind;
			if (ancestor == null) {
				kind = IDiff.ADD;
			} else if (!local.exists()) {
				kind = IDiff.REMOVE;
			} else {
				kind = IDiff.CHANGE;
			}
			if (local.getType() == IResource.FILE) {
				IFileRevision before = asFileState(ancestor);
				IFileRevision after = getFileRevisionFor((IFile)local);
				return new ResourceDiff(info.getLocal(), kind, 0, before, after);
			}
			// For folders, we don't need file states
			return new ResourceDiff(info.getLocal(), kind);

		}
		return null;
	}

	public static IResourceVariant getRemoteVariant(IThreeWayDiff twd) {
		IFileRevision revision = getRemote(twd);
		if (revision != null)
			return asResourceVariant(revision);
		return null;
	}

	public static IResourceVariant getBaseVariant(IThreeWayDiff twd) {
		IResourceDiff diff = (IResourceDiff)twd.getRemoteChange();
		if (diff != null)
			return asResourceVariant(diff.getBeforeState());
		diff = (IResourceDiff)twd.getLocalChange();
		if (diff != null)
			return asResourceVariant(diff.getBeforeState());
		return null;
	}

	public SyncInfo asSyncInfo(IDiff diff, IResourceVariantComparator comparator) {
		if (diff instanceof ResourceDiff) {
			ResourceDiff rd = (ResourceDiff) diff;
			IResource local = rd.getResource();
			IFileRevision afterState = rd.getAfterState();
			IResourceVariant remote = asResourceVariant(afterState);
			int kind;
			if (remote == null) {
				kind = SyncInfo.DELETION;
			} else if (!local.exists()) {
				kind = SyncInfo.ADDITION;
			} else {
				kind = SyncInfo.CHANGE;
			}
			SyncInfo info = createSyncInfo(comparator, kind, local, null, remote);
			return info;
		} else if (diff instanceof IThreeWayDiff) {
			IThreeWayDiff twd = (IThreeWayDiff) diff;
			IResource local = getLocal(twd);
			if (local != null) {
				IResourceVariant remote = getRemoteVariant(twd);
				IResourceVariant base = getBaseVariant(twd);
				int kind = asSyncInfoKind(twd);
				SyncInfo info = createSyncInfo(comparator, kind, local, base, remote);
				return info;
			}
		}
		return null;
	}

	protected SyncInfo createSyncInfo(IResourceVariantComparator comparator, int kind, IResource local, IResourceVariant base, IResourceVariant remote) {
		PrecalculatedSyncInfo info = new PrecalculatedSyncInfo(kind, local, base, remote, comparator);
		try {
			info.init();
		} catch (TeamException e) {
			// Ignore
		}
		return info;
	}

	private static IResource getLocal(IThreeWayDiff twd) {
		IResourceDiff diff = (IResourceDiff)twd.getRemoteChange();
		if (diff != null)
			return diff.getResource();
		diff = (IResourceDiff)twd.getLocalChange();
		if (diff != null)
			return diff.getResource();
		return null;
	}

	public static IResourceVariant asResourceVariant(IFileRevision revision) {
		if (revision == null)
			return null;
		if (revision instanceof ResourceVariantFileRevision) {
			ResourceVariantFileRevision rvfr = (ResourceVariantFileRevision) revision;
			return rvfr.getVariant();
		}
		if (revision instanceof IAdaptable) {
			IAdaptable adaptable = (IAdaptable) revision;
			Object o = adaptable.getAdapter(IResourceVariant.class);
			if (o instanceof IResourceVariant) {
				return (IResourceVariant) o;
			}
		}
		return null;
	}

	public static IFileRevision getRemote(IDiff diff) {
		if (diff instanceof IResourceDiff) {
			IResourceDiff rd = (IResourceDiff) diff;
			return rd.getAfterState();
		}
		if (diff instanceof IThreeWayDiff) {
			IThreeWayDiff twd = (IThreeWayDiff) diff;
			return getRemote(twd);
		}
		return null;
	}

	public static IFileRevision getRemote(IThreeWayDiff twd) {
		IResourceDiff rd = (IResourceDiff)twd.getRemoteChange();
		if (rd != null)
			return rd.getAfterState();
		rd = (IResourceDiff)twd.getLocalChange();
		if (rd != null)
			return rd.getBeforeState();
		return null;
	}

	public static SyncInfoToDiffConverter getDefault() {
		if (instance == null)
			instance = new SyncInfoToDiffConverter();
		return instance;
	}
}
