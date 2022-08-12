/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
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
package org.eclipse.core.internal.events;

import org.eclipse.core.internal.resources.ICoreConstants;
import org.eclipse.core.internal.resources.ResourceInfo;
import org.eclipse.core.internal.watson.IElementComparator;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;

/**
 * Compares two Resources and returns flags describing how
 * they have changed, for use in computing deltas.
 * Implementation note: rather than defining a partial order
 * as specified by IComparator, the compare operation returns
 * a set of flags instead.  The delta computation only cares
 * whether the comparison is zero (equal) or non-zero (not equal).
 */
public class ResourceComparator implements IElementComparator, ICoreConstants {
	/* Singleton instances */
	protected static final ResourceComparator notificationSingleton = new ResourceComparator(true, false);
	protected static final ResourceComparator buildSingleton = new ResourceComparator(false, false);

	/**
	 * Boolean indicating whether or not this comparator is to be used for
	 * a notification. (as opposed to a build) Notifications include extra information
	 * like marker and sync info changes.
	 */
	private boolean notification;

	/**
	 * Boolean indicating whether or not this comparator is to be used for
	 * snapshot. Snapshots care about extra information such as the used bit.
	 */
	private boolean save;

	/**
	 * Returns a comparator which compares resource infos, suitable for computing
	 * save and snapshot deltas.
	 */
	public static ResourceComparator getSaveComparator() {
		return new ResourceComparator(false, true);
	}

	/**
	 * Returns a comparator which compares resource infos, suitable for computing
	 * build deltas.
	 */
	public static ResourceComparator getBuildComparator() {
		return buildSingleton;
	}

	/**
	 * Returns a comparator which compares resource infos, suitable for computing
	 * build deltas.
	 */
	public static ResourceComparator getNotificationComparator() {
		return notificationSingleton;
	}

	/**
	 * Create a comparator which compares resource infos.
	 * @param notification if true, check for marker deltas.
	 * @param save if true, check for all resource changes that snapshot needs
	 */
	private ResourceComparator(boolean notification, boolean save) {
		this.notification = notification;
		this.save = save;
	}

	/**
	 * Compare the ElementInfos for two resources.
	 */
	@Override
	public int compare(Object o1, Object o2) {
		// == handles null, null.
		if (o1 == o2)
			return IResourceDelta.NO_CHANGE;
		int result = 0;
		if (o1 == null)
			return ((ResourceInfo) o2).isSet(M_PHANTOM) ? IResourceDelta.ADDED_PHANTOM : IResourceDelta.ADDED;
		if (o2 == null)
			return ((ResourceInfo) o1).isSet(M_PHANTOM) ? IResourceDelta.REMOVED_PHANTOM : IResourceDelta.REMOVED;
		if (!(o1 instanceof ResourceInfo && o2 instanceof ResourceInfo))
			return IResourceDelta.NO_CHANGE;
		ResourceInfo oldElement = (ResourceInfo) o1;
		ResourceInfo newElement = (ResourceInfo) o2;
		if (!oldElement.isSet(M_PHANTOM) && newElement.isSet(M_PHANTOM))
			return IResourceDelta.REMOVED;
		if (oldElement.isSet(M_PHANTOM) && !newElement.isSet(M_PHANTOM))
			return IResourceDelta.ADDED;
		if (!compareOpen(oldElement, newElement))
			result |= IResourceDelta.OPEN;
		if (!compareContents(oldElement, newElement)) {
			if (oldElement.getType() == IResource.PROJECT)
				result |= IResourceDelta.DESCRIPTION;
			else if (newElement.getType() == IResource.FILE || oldElement.getType() == IResource.FILE)
				result |= IResourceDelta.CONTENT;
		}
		if (!compareType(oldElement, newElement))
			result |= IResourceDelta.TYPE;
		if (!compareNodeIDs(oldElement, newElement)) {
			result |= IResourceDelta.REPLACED;
			// if the node was replaced and the old and new were files, this is also a content change.
			if (oldElement.getType() == IResource.FILE && newElement.getType() == IResource.FILE)
				result |= IResourceDelta.CONTENT;
		}
		if (compareLocal(oldElement, newElement))
			result |= IResourceDelta.LOCAL_CHANGED;
		if (!compareCharsets(oldElement, newElement))
			result |= IResourceDelta.ENCODING;
		if (!compareDerived(oldElement, newElement))
			result |= IResourceDelta.DERIVED_CHANGED;
		if (notification && !compareSync(oldElement, newElement))
			result |= IResourceDelta.SYNC;
		if (notification && !compareMarkers(oldElement, newElement))
			result |= IResourceDelta.MARKERS;
		if (save && !compareUsed(oldElement, newElement))
			result |= IResourceDelta.CHANGED;
		return result == 0 ? 0 : result | IResourceDelta.CHANGED;
	}

	private boolean compareDerived(ResourceInfo oldElement, ResourceInfo newElement) {
		return oldElement.isSet(ICoreConstants.M_DERIVED) == newElement.isSet(ICoreConstants.M_DERIVED);
	}

	private boolean compareCharsets(ResourceInfo oldElement, ResourceInfo newElement) {
		return oldElement.getCharsetGenerationCount() == newElement.getCharsetGenerationCount();
	}

	/**
	 * Compares the contents of the ResourceInfo.
	 */
	private boolean compareContents(ResourceInfo oldElement, ResourceInfo newElement) {
		return oldElement.getContentId() == newElement.getContentId();
	}

	/**
	 * Compares the existence of local files/folders for two linked resources.
	 */
	private boolean compareLocal(ResourceInfo oldElement, ResourceInfo newElement) {
		//only applicable for linked resources
		if (!oldElement.isSet(ICoreConstants.M_LINK) || !newElement.isSet(ICoreConstants.M_LINK))
			return false;
		long oldStamp = oldElement.getModificationStamp();
		long newStamp = newElement.getModificationStamp();
		return (oldStamp == -1 || newStamp == -1) && (oldStamp != newStamp);
	}

	private boolean compareMarkers(ResourceInfo oldElement, ResourceInfo newElement) {
		// If both sets of markers are null then perhaps we added some markers
		// but then deleted them right away before notification. In that case
		// don't signify a marker change in the delta.
		boolean bothNull = oldElement.getMarkers(false) == null && newElement.getMarkers(false) == null;
		return bothNull || oldElement.getMarkerGenerationCount() == newElement.getMarkerGenerationCount();
	}

	/**
	 * Compares the node IDs of the ElementInfos for two resources.
	 */
	private boolean compareNodeIDs(ResourceInfo oldElement, ResourceInfo newElement) {
		return oldElement.getNodeId() == newElement.getNodeId();
	}

	/**
	 * Compares the open state of the ElementInfos for two resources.
	 */
	private boolean compareOpen(ResourceInfo oldElement, ResourceInfo newElement) {
		return oldElement.isSet(M_OPEN) == newElement.isSet(M_OPEN);
	}

	/**
	 * Compares the sync state for two resources.
	 */
	private boolean compareSync(ResourceInfo oldElement, ResourceInfo newElement) {
		return oldElement.getSyncInfoGenerationCount() == newElement.getSyncInfoGenerationCount();
	}

	/**
	 * Compares the type of the ResourceInfo.
	 */
	private boolean compareType(ResourceInfo oldElement, ResourceInfo newElement) {
		return oldElement.getType() == newElement.getType();
	}

	/**
	 * Compares the used state of the ElementInfos for two resources.
	 */
	private boolean compareUsed(ResourceInfo oldElement, ResourceInfo newElement) {
		return oldElement.isSet(M_USED) == newElement.isSet(M_USED);
	}
}
