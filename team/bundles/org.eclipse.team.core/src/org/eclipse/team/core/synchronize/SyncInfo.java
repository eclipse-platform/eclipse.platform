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
package org.eclipse.team.core.synchronize;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.variants.IResourceVariant;
import org.eclipse.team.core.variants.IResourceVariantComparator;
import org.eclipse.team.internal.core.Messages;

/**
 * Describes the synchronization of a <b>local</b> resource relative to a
 * <b>remote</b> resource variant. There are two types of comparison: two-way
 * and three-way. The {@link IResourceVariantComparator} is used to decide which
 * comparison type to use.
 * <p>
 * For two-way comparisons, a <code>SyncInfo</code> node has a change type. This
 * will be one of <code>IN-SYNC</code>, <code>ADDITION</code>,
 * <code>DELETION</code> or <code>CHANGE</code> determined in the following
 * manner.
 * </p>
 * <ul>
 * <li>A resource is considered an <code>ADDITION</code> if it exists locally
 * and there is no remote.
 * <li>A resource is considered an <code>DELETION</code> if it does not exists
 * locally and there is remote.
 * <li>A resource is considered a <code>CHANGE</code> if both the local and
 * remote exist but the comparator indicates that they differ. The comparator
 * may be comparing contents or timestamps or some other resource state.
 * <li>A resource is considered <code>IN_SYNC</code> in all other cases.
 * </ul>
 * <p>
 * For three-way comparisons, the sync info node has a direction as well as a
 * change type. The direction is one of <code>INCOMING</code>,
 * <code>OUTGOING</code> or <code>CONFLICTING</code>. The comparison of the
 * local and remote resources with a <b>base</b> resource is used to determine
 * the direction of the change.
 * </p>
 * <ul>
 * <li>Differences between the base and local resources are classified as
 * <b>outgoing changes</b>; if there is a difference, the local resource is
 * considered the <b>outgoing resource</b>.
 * <li>Differences between the base and remote resources are classified as
 * <b>incoming changes</b>; if there is a difference, the remote resource is
 * considered the <b>incoming resource</b>.
 * <li>If there are both incoming and outgoing changes, the resource is
 * considered a <b>conflicting change</b>.
 * </ul>
 * Again, the comparison of resources is done using the variant comparator
 * provided when the sync info was created.
 *
 * @since 3.0
 */
public class SyncInfo implements IAdaptable {

	/*====================================================================
	 * Constants defining synchronization types:
	 *====================================================================*/

	/**
	 * Sync constant (value 0) indicating element is in sync.
	 */
	public static final int IN_SYNC = 0;

	/**
	 * Sync constant (value 1) indicating that one side was added.
	 */
	public static final int ADDITION = 1;

	/**
	 * Sync constant (value 2) indicating that one side was deleted.
	 */
	public static final int DELETION = 2;

	/**
	 * Sync constant (value 3) indicating that one side was changed.
	 */
	public static final int CHANGE = 3;

	/**
	 * Bit mask for extracting the change type.
	 */
	public static final int CHANGE_MASK = CHANGE;

	/*====================================================================
	 * Constants defining synchronization direction:
	 *====================================================================*/

	/**
	 * Sync constant (value 4) indicating a change to the local resource.
	 */
	public static final int OUTGOING = 4;

	/**
	 * Sync constant (value 8) indicating a change to the remote resource.
	 */
	public static final int INCOMING = 8;

	/**
	 * Sync constant (value 12) indicating a change to both the remote and local resources.
	 */
	public static final int CONFLICTING = 12;

	/**
	 * Bit mask for extracting the synchronization direction.
	 */
	public static final int DIRECTION_MASK = CONFLICTING;

	/*====================================================================
	 * Constants defining synchronization conflict types:
	 *====================================================================*/

	/**
	 * Sync constant (value 16) indication that both the local and remote resources have changed
	 * relative to the base but their contents are the same.
	 */
	public static final int PSEUDO_CONFLICT = 16;

	/**
	 * Sync constant (value 32) indicating that both the local and remote resources have changed
	 * relative to the base but their content changes do not conflict (e.g. source file changes on different
	 * lines). These conflicts could be merged automatically.
	 */
	public static final int AUTOMERGE_CONFLICT = 32;

	/**
	 * Sync constant (value 64) indicating that both the local and remote resources have changed relative
	 * to the base and their content changes conflict (e.g. local and remote resource have changes on
	 * same lines). These conflicts can only be correctly resolved by the user.
	 */
	public static final int MANUAL_CONFLICT = 64;

	/*====================================================================
	 * Members:
	 *====================================================================*/
	private IResource local;
	private IResourceVariant base;
	private IResourceVariant remote;
	private IResourceVariantComparator comparator;

	private int syncKind;

	/**
	 * Construct a sync info object.
	 * @param local the local resource. Must be non-null but may not exist.
	 * @param base the base resource variant or <code>null</code>
	 * @param remote the remote resource variant or <code>null</code>
	 * @param comparator the comparator used to determine if resources differ
	 */
	public SyncInfo(IResource local, IResourceVariant base, IResourceVariant remote, IResourceVariantComparator comparator) {
		Assert.isNotNull(local);
		Assert.isNotNull(comparator);
		this.local = local;
		this.base = base;
		this.remote = remote;
		this.comparator = comparator;
	}

	/**
	 * Returns the state of the local resource. Note that the
	 * resource may or may not exist.
	 *
	 * @return a resource
	 */
	public IResource getLocal() {
		return local;
	}

	/**
	 * Returns the content identifier for the local resource or <code>null</code> if
	 * it doesn't have one. For example, in CVS this would be the revision number.
	 *
	 * @return String that could be displayed to the user to identify this resource.
	 */
	public String getLocalContentIdentifier() {
		return null;
	}

	/**
	 * Returns the author of the revision corresponding to the local resource or <code>null</code>
	 * if it doesn't have one. For example if the local file is shared in CVS this would be the
	 * revision author.
	 *
	 * @param monitor the progress monitor
	 * @return the author of the revision associated with the local file or <code>null</code>
	 * @since 3.6
	 */
	public String getLocalAuthor(IProgressMonitor monitor) {
		return null;
	}

	/**
	 * Returns the remote resource handle for the base resource,
	 * or <code>null</code> if the base resource does not exist.
	 * <p>
	 * [Note: The type of the common resource may be different from the types
	 * of the local and remote resources.
	 * ]
	 * </p>
	 * @return a remote base resource handle, or <code>null</code>
	 */
	public IResourceVariant getBase() {
		return base;
	}

	/**
	 * Returns the handle for the remote resource,
	 * or <code>null</code> if the remote resource does not exist.
	 * <p>
	 * [Note: The type of the remote resource may be different from the types
	 * of the local and common resources.
	 * ]
	 * </p>
	 * @return a remote resource handle, or <code>null</code>
	 */
	public IResourceVariant getRemote() {
		return remote;
	}

	/**
	 * Returns the comparator that is used to determine the
	 * kind of this sync node.
	 *
	 * @return the comparator that is used to determine the
	 * kind of this sync node.
	 */
	public IResourceVariantComparator getComparator() {
		return comparator;
	}

	/**
	 * Returns the kind of synchronization for this node.
	 *
	 * @return the kind of synchronization for this node.
	 */
	public int getKind() {
		return syncKind;
	}

	/**
	 * Helper method that returns whether the given kind represents
	 * an in-sync resource.
	 *
	 * @param kind the kind of a <code>SyncInfo</code>
	 * @return whether the kind is <code>IN_SYNC</code>.
	 */
	static public boolean isInSync(int kind) {
		return kind == IN_SYNC;
	}

	/**
	 * Helper method to return the direction portion
	 * of the given kind. The resulting value
	 * can be compared directly with the direction constants.
	 *
	 * @param kind the kind of a <code>SyncInfo</code>
	 * @return the direction portion of the kind
	 */
	static public int getDirection(int kind) {
		return kind & DIRECTION_MASK;
	}

	/**
	 * Helper method to return the change portion
	 * of the given kind. The resulting value
	 * can be compared directly with the change
	 * type constants.
	 *
	 * @param kind the kind of a <code>SyncInfo</code>
	 * @return the change portion of the kind
	 */
	static public int getChange(int kind) {
		return kind & CHANGE_MASK;
	}

	@Override
	public boolean equals(Object other) {
		if(other == this) return true;
		if(other instanceof SyncInfo) {
			return equalNodes(this, (SyncInfo)other);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return getLocal().hashCode();
	}

	private boolean equalNodes(SyncInfo node1, SyncInfo node2) {
			if(node1 == null || node2 == null) {
				return false;
			}

			// First, ensure the local resources are equals
			IResource local1 = null;
			if (node1.getLocal() != null)
				local1 = node1.getLocal();
			IResource local2 = null;
			if (node2.getLocal() != null)
				local2 = node2.getLocal();
			if (!equalObjects(local1, local2)) return false;

			// Next, ensure the base resources are equal
			IResourceVariant base1 = null;
			if (node1.getBase() != null)
				base1 = node1.getBase();
			IResourceVariant base2 = null;
			if (node2.getBase() != null)
				base2 = node2.getBase();
			if (!equalObjects(base1, base2)) return false;

			// Finally, ensure the remote resources are equal
			IResourceVariant remote1 = null;
			if (node1.getRemote() != null)
				remote1 = node1.getRemote();
			IResourceVariant remote2 = null;
			if (node2.getRemote() != null)
					remote2 = node2.getRemote();
			if (!equalObjects(remote1, remote2)) return false;

			return true;
		}

	private boolean equalObjects(Object o1, Object o2) {
		if (o1 == null && o2 == null) return true;
		if (o1 == null || o2 == null) return false;
		return o1.equals(o2);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T getAdapter(Class<T> adapter) {
		if (adapter == IResource.class) {
			return (T) getLocal();
		}
		return null;
	}

	@Override
	public String toString() {
		return getLocal().getName() + " " + kindToString(getKind()); //$NON-NLS-1$
	}

	/**
	 * A helper method that returns a displayable (i.e. externalized)
	 * string describing the provided sync kind.
	 *
	 * @param kind the sync kind obtained from a <code>SyncInfo</code>
	 * @return a displayable string that describes the kind
	 */
	public static String kindToString(int kind) {
		String label = ""; //$NON-NLS-1$
		if(kind==IN_SYNC) {
			label = Messages.RemoteSyncElement_insync;
		} else {
			switch(kind & DIRECTION_MASK) {
				case CONFLICTING: label = Messages.RemoteSyncElement_conflicting; break;
				case OUTGOING: label = Messages.RemoteSyncElement_outgoing; break;
				case INCOMING: label = Messages.RemoteSyncElement_incoming; break;
			}
			switch(kind & CHANGE_MASK) {
				case CHANGE: label = NLS.bind(Messages.concatStrings, new String[] { label, Messages.RemoteSyncElement_change }); break; //
				case ADDITION: label = NLS.bind(Messages.concatStrings, new String[] { label, Messages.RemoteSyncElement_addition }); break; //
				case DELETION: label = NLS.bind(Messages.concatStrings, new String[] { label, Messages.RemoteSyncElement_deletion }); break; //
			}
			if((kind & MANUAL_CONFLICT) != 0) {
				label = NLS.bind(Messages.concatStrings, new String[] { label, Messages.RemoteSyncElement_manual }); //
			}
			if((kind & AUTOMERGE_CONFLICT) != 0) {
				label = NLS.bind(Messages.concatStrings, new String[] { label, Messages.RemoteSyncElement_auto }); //
			}
		}
		return NLS.bind(Messages.RemoteSyncElement_delimit, new String[] { label });
	}

	/**
	 * Method that is invoked after instance creation to initialize the sync kind.
	 * This method should only be invoked by the creator of the <code>SyncInfo</code>
	 * instance. It is not done from the constructor in order to allow subclasses
	 * to calculate the sync kind from any additional state variables they may have.
	 *
	 * @throws TeamException if there were problems calculating the sync state.
	 */
	public final void init() throws TeamException {
		syncKind = calculateKind();
	}

	/**
	 * Method that is invoked from the <code>init()</code> method to calculate
	 * the sync kind for this instance of <code>SyncInfo</code>. The result is
	 * assigned to an instance variable and is available using <code>getKind()</code>.
	 * Subclasses should not invoke this method but may override it in order to customize
	 * the sync kind calculation algorithm.
	 *
	 * @return the sync kind of this <code>SyncInfo</code>
	 * @throws TeamException if there were problems calculating the sync state.
	 */
	protected int calculateKind() throws TeamException {
		int description = IN_SYNC;

		boolean localExists = local.exists();

		if (comparator.isThreeWay()) {
			if (base == null) {
				if (remote == null) {
					if (!localExists) {
						description = IN_SYNC;
					} else {
						description = OUTGOING | ADDITION;
					}
				} else {
					if (!localExists) {
						description = INCOMING | ADDITION;
					} else {
						description = CONFLICTING | ADDITION;
						if (comparator.compare(local, remote)) {
							description |= PSEUDO_CONFLICT;
						}
					}
				}
			} else {
				if (!localExists) {
					if (remote == null) {
						description = CONFLICTING | DELETION | PSEUDO_CONFLICT;
					} else {
						if (comparator.compare(base, remote))
							description = OUTGOING | DELETION;
						else
							description = CONFLICTING | CHANGE;
					}
				} else {
					if (remote == null) {
						if (comparator.compare(local, base))
							description = INCOMING | DELETION;
						else
							description = CONFLICTING | CHANGE;
					} else {
						boolean ay = comparator.compare(local, base);
						boolean am = comparator.compare(base, remote);
						if (ay && am) {
							// in-sync
						} else if (ay && !am) {
							description = INCOMING | CHANGE;
						} else if (!ay && am) {
							description = OUTGOING | CHANGE;
						} else {
							if(! comparator.compare(local, remote)) {
								description = CONFLICTING | CHANGE;
							}
						}
					}
				}
			}
		} else { // two compare without access to base contents
			if (remote == null) {
				if (!localExists) {
					Assert.isTrue(false);
					// shouldn't happen
				} else {
					description= DELETION;
				}
			} else {
				if (!localExists) {
					description= ADDITION;
				} else {
					if (! comparator.compare(local, remote))
						description= CHANGE;
				}
			}
		}
		return description;
	}
}
