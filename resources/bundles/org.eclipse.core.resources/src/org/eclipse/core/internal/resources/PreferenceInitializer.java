/*******************************************************************************
 * Copyright (c) 2004, 2014 IBM Corporation and others.
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
 *     James Blackburn (Broadcom Corp.) - ongoing development
 *     Ingo Mohr - Issue #166 - Add Preference to Turn Off Warning-Check for Project Specific Encoding
 *******************************************************************************/
package org.eclipse.core.internal.resources;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.preferences.*;

/**
 * @since 3.1
 */
public class PreferenceInitializer extends AbstractPreferenceInitializer {

	// internal preference keys
	public static final String PREF_OPERATIONS_PER_SNAPSHOT = "snapshots.operations"; //$NON-NLS-1$
	public static final String PREF_DELTA_EXPIRATION = "delta.expiration"; //$NON-NLS-1$

	// DEFAULTS
	public static final boolean PREF_AUTO_REFRESH_DEFAULT = false;
	public static final boolean PREF_LIGHTWEIGHT_AUTO_REFRESH_DEFAULT = true;
	public static final boolean PREF_DISABLE_LINKING_DEFAULT = false;
	public static final String PREF_ENCODING_DEFAULT = ""; //$NON-NLS-1$
	public static final boolean PREF_AUTO_BUILDING_DEFAULT = true;
	public static final String PREF_BUILD_ORDER_DEFAULT = ""; //$NON-NLS-1$
	public static final int PREF_MAX_BUILD_ITERATIONS_DEFAULT = 10;
	public static final boolean PREF_DEFAULT_BUILD_ORDER_DEFAULT = true;
	public final static long PREF_SNAPSHOT_INTERVAL_DEFAULT = 5 * 60 * 1000l; // 5 min
	public static final int PREF_OPERATIONS_PER_SNAPSHOT_DEFAULT = 100;
	public static final boolean PREF_APPLY_FILE_STATE_POLICY_DEFAULT = true;
	public static final long PREF_FILE_STATE_LONGEVITY_DEFAULT = 7 * 24 * 3600 * 1000l; // 7 days
	public static final long PREF_MAX_FILE_STATE_SIZE_DEFAULT = 1024 * 1024l; // 1 MB
	public static final boolean PREF_KEEP_DERIVED_STATE_DEFAULT = false;
	public static final int PREF_MAX_FILE_STATES_DEFAULT = 50;
	public static final long PREF_DELTA_EXPIRATION_DEFAULT = 30 * 24 * 3600 * 1000l; // 30 days
	/**
	 * Default setting for {@value ResourcesPlugin#PREF_MISSING_NATURE_MARKER_SEVERITY}.
	 *
	 * @since 3.12
	 */
	public static final int PREF_MISSING_NATURE_MARKER_SEVERITY_DEFAULT = IMarker.SEVERITY_WARNING;
	/**
	 * Default setting for
	 * {@value ResourcesPlugin#PREF_MISSING_ENCODING_MARKER_SEVERITY}.
	 *
	 * @since 3.18
	 */
	public static final int PREF_MISSING_ENCODING_MARKER_SEVERITY_DEFAULT = IMarker.SEVERITY_WARNING;

	/**
	 * @since 3.13
	 */
	public static final int PREF_MAX_CONCURRENT_BUILDS_DEFAULT = 1;

	public PreferenceInitializer() {
		super();
	}

	@Override
	public void initializeDefaultPreferences() {
		IEclipsePreferences node = DefaultScope.INSTANCE.getNode(ResourcesPlugin.PI_RESOURCES);
		// auto-refresh default
		node.putBoolean(ResourcesPlugin.PREF_AUTO_REFRESH, PREF_AUTO_REFRESH_DEFAULT);
		node.putBoolean(ResourcesPlugin.PREF_LIGHTWEIGHT_AUTO_REFRESH, PREF_LIGHTWEIGHT_AUTO_REFRESH_DEFAULT);

		// linked resources default
		node.putBoolean(ResourcesPlugin.PREF_DISABLE_LINKING, PREF_DISABLE_LINKING_DEFAULT);

		// build manager defaults
		node.putBoolean(ResourcesPlugin.PREF_AUTO_BUILDING, PREF_AUTO_BUILDING_DEFAULT);
		node.put(ResourcesPlugin.PREF_BUILD_ORDER, PREF_BUILD_ORDER_DEFAULT);
		node.putInt(ResourcesPlugin.PREF_MAX_BUILD_ITERATIONS, PREF_MAX_BUILD_ITERATIONS_DEFAULT);
		node.putBoolean(ResourcesPlugin.PREF_DEFAULT_BUILD_ORDER, PREF_DEFAULT_BUILD_ORDER_DEFAULT);
		node.putInt(ResourcesPlugin.PREF_MISSING_NATURE_MARKER_SEVERITY, PREF_MISSING_NATURE_MARKER_SEVERITY_DEFAULT);
		node.putInt(ResourcesPlugin.PREF_MISSING_ENCODING_MARKER_SEVERITY,
				PREF_MISSING_ENCODING_MARKER_SEVERITY_DEFAULT);


		// history store defaults
		node.putBoolean(ResourcesPlugin.PREF_APPLY_FILE_STATE_POLICY, PREF_APPLY_FILE_STATE_POLICY_DEFAULT);
		node.putLong(ResourcesPlugin.PREF_FILE_STATE_LONGEVITY, PREF_FILE_STATE_LONGEVITY_DEFAULT);
		node.putLong(ResourcesPlugin.PREF_MAX_FILE_STATE_SIZE, PREF_MAX_FILE_STATE_SIZE_DEFAULT);
		node.putInt(ResourcesPlugin.PREF_MAX_FILE_STATES, PREF_MAX_FILE_STATES_DEFAULT);
		node.putBoolean(ResourcesPlugin.PREF_KEEP_DERIVED_STATE, PREF_KEEP_DERIVED_STATE_DEFAULT);

		// save manager defaults
		node.putLong(ResourcesPlugin.PREF_SNAPSHOT_INTERVAL, PREF_SNAPSHOT_INTERVAL_DEFAULT);
		node.putInt(PREF_OPERATIONS_PER_SNAPSHOT, PREF_OPERATIONS_PER_SNAPSHOT_DEFAULT);
		node.putLong(PREF_DELTA_EXPIRATION, PREF_DELTA_EXPIRATION_DEFAULT);

		// encoding defaults
		node.put(ResourcesPlugin.PREF_ENCODING, PREF_ENCODING_DEFAULT);

		// parallel builds defaults
		node.putInt(ResourcesPlugin.PREF_MAX_CONCURRENT_BUILDS, PREF_MAX_CONCURRENT_BUILDS_DEFAULT);
	}

}
