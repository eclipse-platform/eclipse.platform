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
 *     IBM - Initial API and implementation
 *******************************************************************************/
package org.eclipse.core.tools.resources;

import java.util.*;
import org.eclipse.core.internal.resources.*;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.core.tools.*;
import org.eclipse.jface.util.SafeRunnable;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.osgi.util.NLS;

/**
 * Constructs a tree made of <code>TreeContentProviderNode</code>, representing
 * several details about resources.
 * Details include:
 * <ul>
 *   <li>resource's full path;</li>
 *   <li>content ID;</li>
 *   <li>node ID;</li>
 *   <li>resource type;</li>
 *   <li>local sync info;</li>
 *   <li>resource info flags;</li>
 *   <li>markers' type and attributes;</li>
 *   <li>sync info;</li>
 *   <li>session properties;</li>
 *   <li>persistent properties.</li>
 * </ul>
 *
 * @see org.eclipse.core.tools.TreeContentProviderNode
 */
public class ResourceContentProvider extends AbstractTreeContentProvider {

	/**
	 * The maximum number of bytes to be shown for sync info
	 */
	private final static int MAX_SYNC_INFO_BYTES = 8;

	/**
	 * Collects resource info. Calls all other <code>extract...</code> methods.
	 *
	 * @param selectedResource the resource object from where to extract information
	 */
	protected void extractInfo(IResource selectedResource) {

		if (!(selectedResource instanceof Resource)) {
			String message = NLS.bind(Messages.resource_error_unknown_resource_impl, selectedResource.getClass().getName());
			getRootNode().addChild(message);
			return;
		}

		Resource resource = (Resource) selectedResource;

		ResourceInfo info = resource.getResourceInfo(true, false);
		// Resource#getResourceInfo may return null when the resource
		// does not exist anymore. In this case, we just ignore it.
		if (info == null)
			return;

		extractBasicInfo(resource, info);
		extractFlags(info);
		extractContentDescription(resource);
		extractMarkersInfo(resource, info);
		extractSyncInfo(info);
		extractPersistentProperties(resource);
		extractSessionProperties(resource);

	}

	/**
	 * Collects persistent properties information.
	 *
	 * @param resource the resource object from where to extract information
	 */
	protected void extractPersistentProperties(IResource resource) {
		try {
			Map<QualifiedName, String> properties = ((Workspace) ResourcesPlugin.getWorkspace()).getPropertyManager().getProperties(resource);
			if (properties.isEmpty())
				return;
			// creates a node for persistent properties and populates it
			TreeContentProviderNode propertiesRootNode = createNode(Messages.resource_persistent_properties);
			getRootNode().addChild(propertiesRootNode);
			for (Map.Entry<QualifiedName, String> entry : properties.entrySet()) {
				propertiesRootNode.addChild(createNode(entry.getKey().toString(), entry.getValue()));
			}
		} catch (CoreException ce) {
			getRootNode().addChild(createNode(NLS.bind(Messages.resource_error_stored_properties, ce.toString())));
		}
	}

	/**
	 * Collects session properties information.
	 *
	 * @param resource the resource object from where to extract information
	 */
	protected void extractSessionProperties(IResource resource) {

		// retrieves all session properties for the selected resource
		Map<QualifiedName, Object> properties = org.eclipse.core.internal.resources.SpySupport.getSessionProperties(resource);
		if (properties == null || properties.size() == 0)
			return;

		// creates a node for session properties and populates it
		TreeContentProviderNode propertiesRootNode = createNode(Messages.resource_session_properties);
		getRootNode().addChild(propertiesRootNode);

		for (Map.Entry<QualifiedName, Object> entry : properties.entrySet()) {
			propertiesRootNode.addChild(createNode(entry.getKey().toString(), entry.getValue()));
		}

	}

	/**
	 * Extracts basic resource info.
	 *
	 * @param resource the resource object from where to extract information
	 * @param info the resource info object from where to extract information
	 */
	protected void extractBasicInfo(IResource resource, ResourceInfo info) {

		// extracts information from IResource
		IPath resourcePath = resource.getFullPath();
		long id = info.getContentId();

		String type = null;
		switch (resource.getType()) {
			case IResource.FILE :
				type = Messages.resource_file;
				break;
			case IResource.FOLDER :
				type = Messages.resource_folder;
				break;
			case IResource.PROJECT :
				type = Messages.resource_project;
				break;
			case IResource.ROOT :
				type = Messages.resource_root;
				break;
		}

		// extracts information from ResourceInfo
		long localSyncInfo = info.getLocalSyncInfo();
		long nodeId = info.getNodeId();

		// creates a root node for each basic information
		getRootNode().addChild(createNode(Messages.resource_full_path, resourcePath));
		getRootNode().addChild(createNode(Messages.resource_content_id, Long.toString(id)));
		getRootNode().addChild(createNode(Messages.resource_type, type));
		getRootNode().addChild(createNode(Messages.resource_node_id, Long.toString(nodeId)));
		getRootNode().addChild(createNode(Messages.resource_local_sync_info, Long.toString(localSyncInfo)));
	}

	/**
	 * Extracts flags from the resource info object, building a 'Flags'
	 * subtree in the resource details' tree.
	 *
	 * @param info the resource info object from where to extract information
	 */

	protected void extractFlags(ResourceInfo info) {
		// extract flags'values for the resource info object
		boolean isOpen = info.isSet(ICoreConstants.M_OPEN);
		boolean localExists = info.isSet(ICoreConstants.M_LOCAL_EXISTS);
		boolean isPhantom = info.isSet(ICoreConstants.M_PHANTOM);
		boolean isUsed = info.isSet(ICoreConstants.M_USED);
		boolean isDerived = info.isSet(ICoreConstants.M_DERIVED);
		boolean isTeamPrivateMember = info.isSet(ICoreConstants.M_TEAM_PRIVATE_MEMBER);
		boolean isMarkersSnapDirty = info.isSet(ICoreConstants.M_MARKERS_SNAP_DIRTY);
		boolean isSyncInfoSnapDirty = info.isSet(ICoreConstants.M_SYNCINFO_SNAP_DIRTY);
		boolean noContentDescription = info.isSet(ICoreConstants.M_NO_CONTENT_DESCRIPTION);
		boolean defaultContentDescription = info.isSet(ICoreConstants.M_DEFAULT_CONTENT_DESCRIPTION);
		boolean isHidden = info.isSet(ICoreConstants.M_HIDDEN);

		// creates a node for flags
		TreeContentProviderNode flagsParentNode = createNode(Messages.resource_flags);
		getRootNode().addChild(flagsParentNode);

		// creates a child node in "Flags" node for each basic information
		flagsParentNode.addChild(createNode(Messages.resource_open, Boolean.valueOf(isOpen)));
		flagsParentNode.addChild(createNode(Messages.resource_local_exists, Boolean.valueOf(localExists)));
		flagsParentNode.addChild(createNode(Messages.resource_phantom, Boolean.valueOf(isPhantom)));
		flagsParentNode.addChild(createNode(Messages.resource_used, Boolean.valueOf(isUsed)));
		flagsParentNode.addChild(createNode(Messages.resource_derived, Boolean.valueOf(isDerived)));
		flagsParentNode.addChild(createNode(Messages.resource_team_private, Boolean.valueOf(isTeamPrivateMember)));
		flagsParentNode.addChild(createNode(Messages.resource_markers_snap_dirty, Boolean.valueOf(isMarkersSnapDirty)));
		flagsParentNode.addChild(createNode(Messages.resource_sync_info_snap_dirty, Boolean.valueOf(isSyncInfoSnapDirty)));
		flagsParentNode.addChild(createNode(Messages.resource_no_content_description, Boolean.valueOf(noContentDescription)));
		flagsParentNode.addChild(createNode(Messages.resource_default_content_description, Boolean.valueOf(defaultContentDescription)));
		flagsParentNode.addChild(createNode(Messages.resource_hidden, Boolean.valueOf(isHidden)));

	}

	protected void extractContentDescription(Resource resource) {
		if (resource.getType() != IResource.FILE)
			return;
		File file = (File) resource;
		// creates a node for flags
		try {
			boolean cached = org.eclipse.core.internal.resources.SpySupport.isContentDescriptionCached(file);
			String description = file.getContentDescription() + " (" + NLS.bind(Messages.resource_content_description_from_cache, Boolean.toString(cached)) + ")"; //$NON-NLS-1$//$NON-NLS-2$
			TreeContentProviderNode contentDescriptionNode = createNode(Messages.resource_content_description, description);
			getRootNode().addChild(contentDescriptionNode);
		} catch (CoreException ce) {
			// in the case of a CoreException, just present it as node
			getRootNode().addChild(createNode(NLS.bind(Messages.resource_error_content_description, ce.toString())));
		}

	}

	/**
	 * Extracts from resource info object all information regarding markers,
	 * building a 'Markers' subtree in the resource details tree.
	 *
	 * @param resource the resource object from where to extract information
	 * @param info the resource info object from where to extract information
	 */
	protected void extractMarkersInfo(IResource resource, ResourceInfo info) {
		MarkerSet markerSet = info.getMarkers();
		if (markerSet == null)
			return;

		IMarkerSetElement[] markerSetElements = markerSet.elements();
		if (markerSetElements.length == 0)
			return;

		// creates a root node for all markers
		TreeContentProviderNode markersParentNode = createNode(Messages.resource_markers);
		getRootNode().addChild(markersParentNode);

		int markerSetSize = markerSetElements.length;
		for (int i = 0; i < markerSetSize; i++) {
			try {
				long markerID = markerSetElements[i].getId();
				IMarker marker = resource.getMarker(markerID);

				// creates a marker node
				String id = " [id = " + marker.getId() + "]"; //$NON-NLS-1$//$NON-NLS-2$
				TreeContentProviderNode markerNode = createNode(marker.getType() + id);
				markersParentNode.addChild(markerNode);
				extractMarkerAttributes(markerNode, marker);

			} catch (CoreException ce) {
				// in the case of a CoreException, just present it as node
				markersParentNode.addChild(createNode(NLS.bind(Messages.resource_error_marker, ce.toString())));
			}
		}
	}

	/**
	 * Retrieves all attributes from a marker, adding them as sub-nodes in the
	 * marker's node.
	 *
	 * @param markerNode the marker's node
	 * @param marker the marker object
	 * @throws CoreException if this method fails. Reasons include:
	 * <ul>
	 * <li>This marker does not exist.</li>
	 * </ul>
	 */
	protected void extractMarkerAttributes(TreeContentProviderNode markerNode, IMarker marker) throws CoreException {

		Map<String, Object> attributes = marker.getAttributes();
		if (attributes == null || attributes.size() == 0)
			return;

		// create a node (under markerNode) for each attribute found
		for (Map.Entry<String, Object> mapEntry : attributes.entrySet()) {
			String attributeName = mapEntry.getKey();
			Object attributeValue = mapEntry.getValue();
			// adds each attribute as a subnode under
			// this marker's node
			markerNode.addChild(createNode(attributeName, attributeValue));
		}
		// attributes will be ordered by attribute name
		markerNode.sort();
	}

	/**
	 * Extracts from the resource info object all information related to VCM,
	 * building a 'Sync Info' subtree in the resource details tree.
	 *
	 * @param info the resource info object from where to extract information
	 */
	protected void extractSyncInfo(ResourceInfo info) {
		Map<QualifiedName, Object> syncInfo = info.getSyncInfo(true);
		if (syncInfo == null || syncInfo.isEmpty())
			return;

		// creates a root node for all sync info
		TreeContentProviderNode syncInfoParentNode = createNode(Messages.resource_sync_info);
		getRootNode().addChild(syncInfoParentNode);

		for (Map.Entry<QualifiedName, Object> entry : syncInfo.entrySet()) {
			// creates a sync info node
			String name = entry.getKey().toString();
			String value = ByteUtil.byteArrayToString((byte[]) entry.getValue(), MAX_SYNC_INFO_BYTES);
			syncInfoParentNode.addChild(createNode(name, value));
		}

	}

	/**
	 * Reconstructs this content provider data model upon the provided input object.
	 *
	 * @param input the new input object - must not be null
	 */
	@Override
	protected void rebuild(Viewer viewer, final Object input) {
		SafeRunner.run(new SafeRunnable() {
			@Override
			public void run() throws Exception {
				extractInfo((IResource) input);
			}
		});
	}

	/**
	 * Returns true if the input is a resource.
	 *
	 * @param input an input object
	 * @see org.eclipse.core.tools.AbstractTreeContentProvider#acceptInput(java.lang.Object)
	 */
	@Override
	protected boolean acceptInput(Object input) {
		return input instanceof IResource;
	}

}
