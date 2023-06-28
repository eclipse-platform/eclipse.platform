/*******************************************************************************
 * Copyright (c) 2000, 2013 IBM Corporation and others.
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
 *     rob li <antelope424@gmail.com> - Bug 203907
 *******************************************************************************/
package org.eclipse.debug.internal.ui.launchConfigurations;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationListener;
import org.eclipse.debug.core.ILaunchListener;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.debug.ui.ILaunchGroup;
import org.eclipse.ui.activities.WorkbenchActivityHelper;

/**
 * A history of launches and favorites for a launch group
 */
@SuppressWarnings("deprecation")
public class LaunchHistory implements ILaunchListener, ILaunchConfigurationListener {

	/**
	 * Listing of the complete launch history, which includes favorites in the launched ordering
	 */
	private Vector<ILaunchConfiguration> fCompleteHistory = new Vector<>();

	/**
	 * The launch group this history is provided for
	 */
	private ILaunchGroup fGroup;

	/**
	 * Ordered listing of the favorites of this history
	 */
	private List<ILaunchConfiguration> fFavorites = new ArrayList<>();

	/**
	 * A new saved flag to prevent save participants from serializing unchanged launch histories.
	 * @since 3.3.1
	 */
	private boolean fSaved = true;

	/**
	 * List of instances of this launch history
	 */
	private static List<LaunchHistory> fgLaunchHistoryInstances = new ArrayList<>();

	/**
	 * Creates a new launch history for the given launch group
	 */
	public LaunchHistory(ILaunchGroup group) {
		fGroup = group;
		ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();
		manager.addLaunchListener(this);
		manager.addLaunchConfigurationListener(this);
		fgLaunchHistoryInstances.add(this);
	}

	/**
	 * Disposes this history
	 */
	public void dispose() {
		ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();
		manager.removeLaunchListener(this);
		manager.removeLaunchConfigurationListener(this);
		fgLaunchHistoryInstances.remove(this);
	}

	/**
	 * @see org.eclipse.debug.core.ILaunchListener#launchAdded(org.eclipse.debug.core.ILaunch)
	 */
	@Override
	public void launchAdded(ILaunch launch) {
		ILaunchConfiguration configuration = launch.getLaunchConfiguration();
		if (configuration != null && !configuration.isWorkingCopy() && DebugUIPlugin.doLaunchConfigurationFiltering(configuration) && accepts(configuration)) {
			addHistory(configuration, true);
		}
	}

	/**
	 * Returns if the current history contains the specified <code>ILaunchConfiguration</code>
	 * @param config the configuration to look for
	 * @return true if the current history contains the specified configuration, false otherwise
	 * @since 3.3
	 */
	public synchronized boolean contains(ILaunchConfiguration configuration) {
		return fCompleteHistory.contains(configuration);
	}

	/**
	 * Adds the given configuration to this history
	 *
	 * @param configuration
	 * @param prepend whether the configuration should be added to the beginning of
	 * the history list
	 */
	protected void addHistory(ILaunchConfiguration configuration, boolean prepend) {
		synchronized (this) {
			if(configuration.isWorkingCopy()) {
				return;
			}
			checkFavorites(configuration);
			int index = fCompleteHistory.indexOf(configuration);
			if(index == 0) {
				return;
			}
			if(index < 0) {
				if(prepend) {
					fCompleteHistory.add(0, configuration);
				}
				else {
					fCompleteHistory.add(configuration);
				}
			}
			else {
				fCompleteHistory.add(0, fCompleteHistory.remove(index));
			}
			resizeHistory();
		}
		fireLaunchHistoryChanged();
	}

	/**
	 * Notifies all <code>ILaunchHistoryChangedListener</code>s that the launch history has been modified
	 *
	 * @since 3.3
	 */
	private void fireLaunchHistoryChanged() {
		DebugUIPlugin.getDefault().getLaunchConfigurationManager().fireLaunchHistoryChanged();
		setSaved(false);
	}

	/**
	 * Returns if the launch history requires saving or not
	 * @return true if the history needs to be saved, false otherwise
	 * @since 3.3.1
	 */
	public boolean needsSaving() {
		return !fSaved;
	}

	/**
	 * Allows the dirty flag for this launch history to be set.
	 * It is the clients of this class that must set the saved flag to true
	 * if they have persisted the launch history
	 * @param saved
	 * @since 3.3.1
	 */
	public void setSaved(boolean saved) {
		fSaved = saved;
	}

	/**
	 * @see org.eclipse.debug.core.ILaunchListener#launchChanged(org.eclipse.debug.core.ILaunch)
	 */
	@Override
	public void launchChanged(ILaunch launch) {}

	/**
	 * @see org.eclipse.debug.core.ILaunchListener#launchRemoved(org.eclipse.debug.core.ILaunch)
	 */
	@Override
	public void launchRemoved(ILaunch launch) {}

	/**
	 * Returns the most recently launched configuration in this history, or
	 * <code>null</code> if none.
	 *
	 * @return the most recently launched configuration in this history, or
	 * <code>null</code> if none
	 */
	public synchronized ILaunchConfiguration getRecentLaunch() {
		ILaunchConfiguration[] history = getCompleteLaunchHistory();
		if(history.length > 0) {
			return history[0];
		}
		return null;
	}

	/**
	 * Returns the launch configurations in this history, in most recently
	 * launched order, not including any entries from the favorites listing.
	 *
	 * @return launch history
	 */
	public synchronized ILaunchConfiguration[] getHistory() {
		Vector<ILaunchConfiguration> history = new Vector<>();
		try {
			for (ILaunchConfiguration config : fCompleteHistory) {
				if(config.exists() && !fFavorites.contains(config) &&
						DebugUIPlugin.doLaunchConfigurationFiltering(config) &&
						!WorkbenchActivityHelper.filterItem(new LaunchConfigurationTypeContribution(config.getType()))) {
					history.add(config);
				}
			}
			//size it to the max specified history size
			if(history.size() > getMaxHistorySize()) {
				history.setSize(getMaxHistorySize());
			}
		}
		catch(CoreException ce) {DebugUIPlugin.log(ce);}
		return history.toArray(new ILaunchConfiguration[history.size()]);
	}

	/**
	 * Returns the complete launch history in the order they were last launched, this listing includes all
	 * entries including those from the favorites listing, but not those that have been filtered via
	 * launch configuration filtering or capabilities filtering
	 * @return the list of last launched <code>ILaunchConfiguration</code>s
	 *
	 * @since 3.3
	 */
	public synchronized ILaunchConfiguration[] getCompleteLaunchHistory() {
		ArrayList<ILaunchConfiguration> history = new ArrayList<>();
		try {
			for (ILaunchConfiguration config : fCompleteHistory) {
				if(config.exists() && DebugUIPlugin.doLaunchConfigurationFiltering(config) &&
				!WorkbenchActivityHelper.filterItem(new LaunchConfigurationTypeContribution(config.getType()))) {
					history.add(config);
				}
			}
		}
		catch (CoreException ce) {DebugUIPlugin.log(ce);}
		return history.toArray(new ILaunchConfiguration[history.size()]);
	}

	/**
	 * Returns the favorite launch configurations in this history, in the order
	 * they were created.
	 *
	 * @return launch favorites
	 */
	public synchronized ILaunchConfiguration[] getFavorites() {
		return fFavorites.toArray(new ILaunchConfiguration[fFavorites.size()]);
	}

	/**
	 * Sets this container's favorites.
	 *
	 * @param favorites
	 */
	public synchronized void setFavorites(ILaunchConfiguration[] favorites) {
		fFavorites = new Vector<>(Arrays.asList(favorites));
		setSaved(false);
		fireLaunchHistoryChanged();
	}

	/**
	 * Adds the given configuration to the favorites list.
	 *
	 * @param configuration
	 */
	public synchronized void addFavorite(ILaunchConfiguration configuration) {
		if (!fFavorites.contains(configuration)) {
			fFavorites.add(configuration);
			setSaved(false);
			fireLaunchHistoryChanged();
		}
	}

	/**
	 * Returns the launch group associated with this history
	 *
	 * @return group
	 */
	public ILaunchGroup getLaunchGroup() {
		return fGroup;
	}

	/**
	 * Returns whether the given configuration is included in the group
	 * associated with this launch history.
	 *
	 * @param launch
	 * @return boolean
	 */
	public boolean accepts(ILaunchConfiguration configuration) {
		try {
			if (!LaunchConfigurationManager.isVisible(configuration)) {
				return false;
			}
			if (configuration.getType().supportsMode(getLaunchGroup().getMode())) {
				String launchCategory = null;
				launchCategory = configuration.getCategory();
				String category = getLaunchGroup().getCategory();
				if (launchCategory == null || category == null) {
					return launchCategory == category;
				}
				return category.equals(launchCategory);
			}
		} catch (CoreException e) {
			DebugUIPlugin.log(e);
		}
		return false;
	}

	/**
	 * Notifies all launch histories that the launch history size has changed.
	 */
	public static void launchHistoryChanged() {
		for (LaunchHistory history : fgLaunchHistoryInstances) {
			history.resizeHistory();
		}
	}

	/**
	 * The max history size has changed - remove any histories if current
	 * collection is too long.
	 */
	protected synchronized void resizeHistory() {
		int max = getMaxHistorySize() + fFavorites.size();
		if (fCompleteHistory.size() > max) {
			fCompleteHistory.setSize(max);
		}
	}

	/**
	 * Returns the maximum number of entries allowed in this history
	 *
	 * @return the maximum number of entries allowed in this history
	 */
	protected int getMaxHistorySize() {
		return DebugUIPlugin.getDefault().getPreferenceStore().getInt(IDebugUIConstants.PREF_MAX_HISTORY_SIZE);
	}

	/**
	 * @see org.eclipse.debug.core.ILaunchConfigurationListener#launchConfigurationAdded(org.eclipse.debug.core.ILaunchConfiguration)
	 */
	@Override
	public void launchConfigurationAdded(ILaunchConfiguration configuration) {
		ILaunchConfiguration movedFrom = DebugPlugin.getDefault().getLaunchManager().getMovedFrom(configuration);
		// if this is a move, the launchConfigurationRemoved(...) method will handle updates
		if (movedFrom == null) {
			checkFavorites(configuration);
		}
	}

	/**
	 * This method checks if the specified <code>ILaunchConfiguration</code> is a favorite in this
	 * history's launch group.
	 *
	 * @param configuration
	 * @return true if the configuration is a favorite in this history's launch group, false otherwise
	 * @throws CoreException
	 *
	 * @since 3.4
	 */
	protected boolean isFavorite(ILaunchConfiguration configuration) throws CoreException {
		String groupId = getLaunchGroup().getIdentifier();
		List<String> favoriteGroups = configuration.getAttribute(IDebugUIConstants.ATTR_FAVORITE_GROUPS, (List<String>) null);
		if (favoriteGroups == null) {
			// check deprecated attributes for backwards compatibility
			if (groupId.equals(IDebugUIConstants.ID_DEBUG_LAUNCH_GROUP)) {
				return configuration.getAttribute(IDebugUIConstants.ATTR_DEBUG_FAVORITE, false);
			} else if (groupId.equals(IDebugUIConstants.ID_RUN_LAUNCH_GROUP)) {
				return configuration.getAttribute(IDebugUIConstants.ATTR_RUN_FAVORITE, false);
			}
		}
		else if (favoriteGroups.contains(getLaunchGroup().getIdentifier())) {
			return true;
		}
		return false;
	}

	/**
	 * Adds the given config to the favorites list if it is a favorite, and
	 * returns whether the config was added to the favorites list.
	 *
	 * @param configuration
	 * @return whether added to the favorites list
	 */
	protected boolean checkFavorites(ILaunchConfiguration configuration) {
		// update favorites
		if (configuration.isWorkingCopy()) {
			return false;
		}
		try {
			if (isFavorite(configuration)) {
				addFavorite(configuration);
				return true;
			} else {
				removeFavorite(configuration);
				return false;
			}
		}
		catch (CoreException e) {
		//in the event touching the config  throws an error, remove it
			removeFavorite(configuration);
		}
		return false;
	}

	/**
	 * Removes the given config from the favorites list, if needed.
	 *
	 * @param configuration
	 */
	protected synchronized void removeFavorite(ILaunchConfiguration configuration) {
		fFavorites.remove(configuration);
		setSaved(false);
		fireLaunchHistoryChanged();
	}

	/**
	 * This method removes the specified <code>ILaunchConfiguration</code> from this launch history (if present)
	 * If the launch configuration does not exist in the history nothing is changed. If the configuration does exist
	 * in the history and was removed all history listeners are notified.
	 * @param configuration the configuration to remove
	 *
	 * @since 3.4
	 */
	public synchronized void removeFromHistory(ILaunchConfiguration configuration) {
		try {
			boolean removed = fCompleteHistory.remove(configuration);
			if(isFavorite(configuration)) {
				removed |= fFavorites.remove(configuration);
			}
			if(removed) {
				setSaved(false);
				fireLaunchHistoryChanged();
			}
		}
		catch(CoreException ce) {}
	}

	/**
	 * @see org.eclipse.debug.core.ILaunchConfigurationListener#launchConfigurationChanged(org.eclipse.debug.core.ILaunchConfiguration)
	 */
	@Override
	public void launchConfigurationChanged(ILaunchConfiguration configuration) {
		checkFavorites(configuration);
	}

	/**
	 * @see org.eclipse.debug.core.ILaunchConfigurationListener#launchConfigurationRemoved(org.eclipse.debug.core.ILaunchConfiguration)
	 */
	@Override
	public void launchConfigurationRemoved(ILaunchConfiguration configuration) {
		synchronized (this) {
			ILaunchConfiguration newConfig = DebugPlugin.getDefault().getLaunchManager().getMovedTo(configuration);
			if (newConfig == null) {
				//deleted
				fCompleteHistory.remove(configuration);
				fFavorites.remove(configuration);
			} else {
				// moved/renamed
				int index = fCompleteHistory.indexOf(configuration);
				if (index >= 0) {
					fCompleteHistory.remove(index);
					fCompleteHistory.add(index, newConfig);
				}
				index = fFavorites.indexOf(configuration);
				if (index >= 0) {
					fFavorites.remove(index);
					fFavorites.add(index, newConfig);
				}
				checkFavorites(newConfig);
			}
		}
		fireLaunchHistoryChanged();
	}
}
