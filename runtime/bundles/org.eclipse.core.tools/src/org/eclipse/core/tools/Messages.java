/**********************************************************************
* Copyright (c) 2005, 2007 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM - Initial API and implementation
 **********************************************************************/
package org.eclipse.core.tools;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.core.tools.messages";//$NON-NLS-1$

	// Stats View
	public static String stats_eventHeader;
	public static String stats_blameHeader;
	public static String stats_contextHeader;
	public static String stats_countHeader;
	public static String stats_timeHeader;

	public static String stats_badStat;
	public static String stats_badColumn;
	public static String stats_pluginid;

	// Resource Spy
	public static String resource_error_unknown_resource_impl;

	public static String resource_file;
	public static String resource_folder;
	public static String resource_project;
	public static String resource_root;
	public static String resource_full_path;
	public static String resource_content_id;
	public static String resource_type;
	public static String resource_node_id;
	public static String resource_local_sync_info;

	public static String resource_persistent_properties;
	public static String resource_error_stored_properties;

	public static String resource_session_properties;

	public static String resource_flags;
	public static String resource_open;
	public static String resource_local_exists;
	public static String resource_phantom;
	public static String resource_used;
	public static String resource_derived;
	public static String resource_team_private;
	public static String resource_hidden;
	public static String resource_markers_snap_dirty;
	public static String resource_sync_info_snap_dirty;
	public static String resource_no_content_description;
	public static String resource_default_content_description;

	public static String resource_content_description;
	public static String resource_error_content_description;
	public static String resource_content_description_from_cache;

	public static String resource_markers;
	public static String resource_error_marker;

	public static String resource_sync_info;

	public static String depend_noInformation;
	public static String depend_noParentPlugins;
	public static String depend_requiredBy;
	public static String depend_noChildrenPlugins;
	public static String depend_requires;
	public static String depend_badPluginId;

	static {
		// load message values from bundle file
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}
}