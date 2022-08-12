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
 *     James Blackburn (Broadcom Corp.) - ongoing development
 *     Lars Vogel <Lars.Vogel@vogella.com> - Bug 473427
 *******************************************************************************/
package org.eclipse.core.internal.resources;

import java.util.*;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Preferences;

/**
 * This class provides the same interface as <code>WorkspaceDescription</code>
 * but instead of changing/obtaining values from its internal state, it
 * changes/obtains properties in/from the workspace plug-in's preferences.
 *
 * Note: for performance reasons, some frequently called accessor methods are
 * reading a cached value from the super class instead of reading the
 * corresponding property preference store. To keep the cache synchronized with
 * the preference store, a property change listener is used.
 */
public class WorkspacePreferences extends WorkspaceDescription {

	public final static String PROJECT_SEPARATOR = "/"; //$NON-NLS-1$

	private Preferences preferences;

	/**
	 * Helper method that converts a string string array {"string1","
	 * string2",..."stringN"} to a string in the form "string1,string2,...
	 * stringN".
	 */
	public static String convertStringArraytoString(String[] array) {
		if (array == null || array.length == 0)
			return ""; //$NON-NLS-1$
		StringBuilder sb = new StringBuilder();
		for (String element : array) {
			sb.append(element);
			sb.append(PROJECT_SEPARATOR);
		}
		sb.deleteCharAt(sb.length() - 1);
		return sb.toString();
	}

	/**
	 * Helper method that converts a string in the form "string1,string2,...
	 * stringN" to a string array {"string1","string2",..."stringN"}.
	 */
	public static String[] convertStringToStringArray(String string, String separator) {
		List<String> list = new ArrayList<>();
		for (StringTokenizer tokenizer = new StringTokenizer(string, separator); tokenizer.hasMoreTokens();)
			list.add(tokenizer.nextToken());
		return list.toArray(new String[list.size()]);
	}

	/**
	 * Helper method that copies all attributes from a workspace description
	 * object to another.
	 */
	private static void copyFromTo(WorkspaceDescription source, WorkspaceDescription target) {
		target.setAutoBuilding(source.isAutoBuilding());
		target.setBuildOrder(source.getBuildOrder());
		target.setMaxBuildIterations(source.getMaxBuildIterations());
		target.setApplyFileStatePolicy(source.isApplyFileStatePolicy());
		target.setFileStateLongevity(source.getFileStateLongevity());
		target.setMaxFileStates(source.getMaxFileStates());
		target.setMaxFileStateSize(source.getMaxFileStateSize());
		target.setKeepDerivedState(source.isKeepDerivedState());
		target.setSnapshotInterval(source.getSnapshotInterval());
		target.setOperationsPerSnapshot(source.getOperationsPerSnapshot());
		target.setDeltaExpiration(source.getDeltaExpiration());
		target.setMaxConcurrentBuilds(source.getMaxConcurrentBuilds());
	}

	public WorkspacePreferences() {
		super("Workspace"); //$NON-NLS-1$
		this.preferences = ResourcesPlugin.getPlugin().getPluginPreferences();

		final String version = preferences.getString(ICoreConstants.PREF_VERSION_KEY);
		if (!ICoreConstants.PREF_VERSION.equals(version))
			upgradeVersion(version);

		// initialize cached preferences (for better performance)
		super.setAutoBuilding(preferences.getBoolean(ResourcesPlugin.PREF_AUTO_BUILDING));
		super.setSnapshotInterval(preferences.getInt(ResourcesPlugin.PREF_SNAPSHOT_INTERVAL));
		super.setMaxBuildIterations(preferences.getInt(ResourcesPlugin.PREF_MAX_BUILD_ITERATIONS));
		super.setApplyFileStatePolicy(preferences.getBoolean(ResourcesPlugin.PREF_APPLY_FILE_STATE_POLICY));
		super.setMaxFileStates(preferences.getInt(ResourcesPlugin.PREF_MAX_FILE_STATES));
		super.setMaxFileStateSize(preferences.getLong(ResourcesPlugin.PREF_MAX_FILE_STATE_SIZE));
		super.setFileStateLongevity(preferences.getLong(ResourcesPlugin.PREF_FILE_STATE_LONGEVITY));
		super.setOperationsPerSnapshot(preferences.getInt(PreferenceInitializer.PREF_OPERATIONS_PER_SNAPSHOT));
		super.setDeltaExpiration(preferences.getLong(PreferenceInitializer.PREF_DELTA_EXPIRATION));
		super.setMaxConcurrentBuilds(preferences.getInt(ResourcesPlugin.PREF_MAX_CONCURRENT_BUILDS));
		super.setKeepDerivedState(preferences.getBoolean(ResourcesPlugin.PREF_KEEP_DERIVED_STATE));

		// This property listener ensures we are being updated properly when changes
		// are done directly to the preference store.
		preferences.addPropertyChangeListener(event -> synchronizeWithPreferences(event.getProperty()));
	}

	@Override
	public Object clone() {
		// should never be called - throws an exception to avoid using a
		// WorkspacePreferences when using WorkspaceDescription was the real
		// intention (this class offers a different protocol for copying state).
		throw new UnsupportedOperationException("clone() is not supported in " + getClass().getName()); //$NON-NLS-1$
	}

	public void copyFrom(WorkspaceDescription source) {
		copyFromTo(source, this);
	}

	public void copyTo(WorkspaceDescription target) {
		copyFromTo(this, target);
	}

	/**
	 * @see org.eclipse.core.resources.IWorkspaceDescription#getBuildOrder()
	 */
	@Override
	public String[] getBuildOrder() {
		boolean defaultBuildOrder = preferences.getBoolean(ResourcesPlugin.PREF_DEFAULT_BUILD_ORDER);
		if (defaultBuildOrder)
			return null;
		return convertStringToStringArray(preferences.getString(ResourcesPlugin.PREF_BUILD_ORDER), PROJECT_SEPARATOR);
	}

	/**
	 * @see org.eclipse.core.internal.resources.WorkspaceDescription#getBuildOrder(boolean)
	 */
	@Override
	public String[] getBuildOrder(boolean makeCopy) {
		//note that since this is stored in the preference store, we are creating
		//a new copy of the string array on every access anyway
		return getBuildOrder();
	}

	/**
	 * @see org.eclipse.core.resources.IWorkspaceDescription#setAutoBuilding(boolean)
	 */
	@Override
	public void setAutoBuilding(boolean value) {
		preferences.setValue(ResourcesPlugin.PREF_AUTO_BUILDING, value);
	}

	/**
	 * @see org.eclipse.core.resources.IWorkspaceDescription#setBuildOrder(String[])
	 */
	@Override
	public void setBuildOrder(String[] value) {
		preferences.setValue(ResourcesPlugin.PREF_DEFAULT_BUILD_ORDER, value == null);
		preferences.setValue(ResourcesPlugin.PREF_BUILD_ORDER, convertStringArraytoString(value));
	}

	@Override
	public void setDeltaExpiration(long value) {
		preferences.setValue(PreferenceInitializer.PREF_DELTA_EXPIRATION, value);
	}

	/**
	 * @see org.eclipse.core.resources.IWorkspaceDescription#setApplyFileStatePolicy(boolean)
	 */
	@Override
	public void setApplyFileStatePolicy(boolean apply) {
		preferences.setValue(ResourcesPlugin.PREF_APPLY_FILE_STATE_POLICY, apply);
	}

	/**
	 * @see org.eclipse.core.resources.IWorkspaceDescription#setFileStateLongevity(long)
	 */
	@Override
	public void setFileStateLongevity(long time) {
		preferences.setValue(ResourcesPlugin.PREF_FILE_STATE_LONGEVITY, time);
	}

	/**
	 * @see org.eclipse.core.resources.IWorkspaceDescription#setMaxBuildIterations(int)
	 */
	@Override
	public void setMaxBuildIterations(int number) {
		preferences.setValue(ResourcesPlugin.PREF_MAX_BUILD_ITERATIONS, number);
	}

	/**
	 * @see org.eclipse.core.resources.IWorkspaceDescription#setMaxFileStates(int)
	 */
	@Override
	public void setMaxFileStates(int number) {
		preferences.setValue(ResourcesPlugin.PREF_MAX_FILE_STATES, number);
	}

	/**
	 * @see org.eclipse.core.resources.IWorkspaceDescription#setMaxFileStateSize(long)
	 */
	@Override
	public void setMaxFileStateSize(long size) {
		preferences.setValue(ResourcesPlugin.PREF_MAX_FILE_STATE_SIZE, size);
	}

	@Override
	public void setOperationsPerSnapshot(int value) {
		preferences.setValue(PreferenceInitializer.PREF_OPERATIONS_PER_SNAPSHOT, value);
	}

	@Override
	public void setMaxConcurrentBuilds(int n) {
		preferences.setValue(ResourcesPlugin.PREF_MAX_CONCURRENT_BUILDS, n);
	}

	@Override
	public void setKeepDerivedState(boolean keepDerivedState) {
		preferences.setValue(ResourcesPlugin.PREF_KEEP_DERIVED_STATE, keepDerivedState);
	}

	/**
	 * @see org.eclipse.core.resources.IWorkspaceDescription#setSnapshotInterval(long)
	 */
	@Override
	public void setSnapshotInterval(long delay) {
		preferences.setValue(ResourcesPlugin.PREF_SNAPSHOT_INTERVAL, delay);
	}

	protected void synchronizeWithPreferences(String property) {
		// do not use the value in the event - may be a string instead
		// of the expected type. Retrieve it from the preferences store
		// using the type-specific method
		if (property.equals(ResourcesPlugin.PREF_AUTO_BUILDING))
			super.setAutoBuilding(preferences.getBoolean(ResourcesPlugin.PREF_AUTO_BUILDING));
		else if (property.equals(ResourcesPlugin.PREF_SNAPSHOT_INTERVAL))
			super.setSnapshotInterval(preferences.getLong(ResourcesPlugin.PREF_SNAPSHOT_INTERVAL));
		else if (property.equals(ResourcesPlugin.PREF_MAX_BUILD_ITERATIONS))
			super.setMaxBuildIterations(preferences.getInt(ResourcesPlugin.PREF_MAX_BUILD_ITERATIONS));
		else if (property.equals(ResourcesPlugin.PREF_APPLY_FILE_STATE_POLICY))
			super.setApplyFileStatePolicy(preferences.getBoolean(ResourcesPlugin.PREF_APPLY_FILE_STATE_POLICY));
		else if (property.equals(ResourcesPlugin.PREF_MAX_FILE_STATES))
			super.setMaxFileStates(preferences.getInt(ResourcesPlugin.PREF_MAX_FILE_STATES));
		else if (property.equals(ResourcesPlugin.PREF_MAX_FILE_STATE_SIZE))
			super.setMaxFileStateSize(preferences.getLong(ResourcesPlugin.PREF_MAX_FILE_STATE_SIZE));
		else if (property.equals(ResourcesPlugin.PREF_FILE_STATE_LONGEVITY))
			super.setFileStateLongevity(preferences.getLong(ResourcesPlugin.PREF_FILE_STATE_LONGEVITY));
		else if (property.equals(PreferenceInitializer.PREF_OPERATIONS_PER_SNAPSHOT))
			super.setOperationsPerSnapshot(preferences.getInt(PreferenceInitializer.PREF_OPERATIONS_PER_SNAPSHOT));
		else if (property.equals(PreferenceInitializer.PREF_DELTA_EXPIRATION))
			super.setDeltaExpiration(preferences.getLong(PreferenceInitializer.PREF_DELTA_EXPIRATION));
		else if (property.equals(ResourcesPlugin.PREF_MAX_CONCURRENT_BUILDS))
			super.setMaxConcurrentBuilds(preferences.getInt(ResourcesPlugin.PREF_MAX_CONCURRENT_BUILDS));
		else if (property.equals(ResourcesPlugin.PREF_KEEP_DERIVED_STATE)) {
			super.setKeepDerivedState(preferences.getBoolean(ResourcesPlugin.PREF_KEEP_DERIVED_STATE));
		}
	}

	private void upgradeVersion(String oldVersion) {
		if (oldVersion.length() == 0) {
			//only need to convert the build order if we are not using the default order
			if (!preferences.getBoolean(ResourcesPlugin.PREF_DEFAULT_BUILD_ORDER)) {
				String oldOrder = preferences.getString(ResourcesPlugin.PREF_BUILD_ORDER);
				setBuildOrder(convertStringToStringArray(oldOrder, ":")); //$NON-NLS-1$
			}
		}
		preferences.setValue(ICoreConstants.PREF_VERSION_KEY, ICoreConstants.PREF_VERSION);
	}
}