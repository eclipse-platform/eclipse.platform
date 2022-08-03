/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
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
package org.eclipse.team.internal.ui.synchronize;

import org.eclipse.team.core.diff.IDiff;
import org.eclipse.team.internal.core.subscribers.ActiveChangeSet;
import org.eclipse.team.internal.core.subscribers.ActiveChangeSetManager;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.team.ui.synchronize.SynchronizePageActionGroup;

/**
 * A change set capability is used by a SubscriberSynchronizePage
 * to determine what, if any change set capabilities should be enabled
 * for the pages of the participant.
 * @since 3.1
 */
public abstract class ChangeSetCapability {

	/**
	 * Return whether the associated participant supports
	 * the display of checked-in change sets. The default is
	 * unsupported (<code>false</code>). If subclasses support
	 * checked-in change sets, they must override the
	 * <code>createCheckedInChangeSetCollector</code>
	 * method to return an appropriate values.
	 * @return whether the associated participant supports
	 * the display of checked-in change sets
	 */
	public boolean supportsCheckedInChangeSets() {
		return false;
	}

	/**
	 * Return whether the associated participant supports
	 * the use of active change sets. The default is unsupported
	 * (<code>false</code>). If a subclass overrides this method in
	 * order to support active change sets, they must also override the methods
	 * <code>getActiveChangeSetManager</code>,
	 * <code>createChangeSet</code> and <code>editChangeSet</code>.
	 * @return whether the associated participant supports
	 * the use of active change sets
	 */
	public boolean supportsActiveChangeSets() {
		return false;
	}

	/**
	 * Return the change set collector that manages the active change
	 * set for the participant associated with this capability. A <code>null</code>
	 * is returned if active change sets are not supported. The default is to
	 * return <code>null</code>.  This method must be
	 * overridden by subclasses that support active change sets.
	 * @return the change set collector that manages the active change
	 * set for the participant associated with this capability or
	 * <code>null</code> if active change sets are not supported.
	 */
	public ActiveChangeSetManager getActiveChangeSetManager() {
		return null;
	}

	/**
	 * Create a change set from the given manager that contains the given sync info.
	 * This method is invoked from the UI thread. A <code>null</code>
	 * is returned if active change sets are not supported. The default is to
	 * return <code>null</code>.  This method must be
	 * overridden by subclasses that support active change sets.
	 * @param configuration the configuration of the page displaying the change sets
	 * @param diffs the sync info to be added to the change set
	 * @return the created set.
	 */
	public ActiveChangeSet createChangeSet(ISynchronizePageConfiguration configuration, IDiff[] diffs) {
		return null;
	}

	/**
	 * Edit the title and comment of the given change set. This method must be
	 * overridden by subclasses that support active change sets.
	 * This method is invoked from the UI thread.
	 * @param configuration the configuration of the page displaying the change sets
	 * @param set the set to be edited
	 */
	public void editChangeSet(ISynchronizePageConfiguration configuration, ActiveChangeSet set) {
		// Default is to do nothing
	}

	/**
	 * Return a collector that can be used to group a set of checked-in changes
	 * into a set of checked-in change sets.  This method must be
	 * overridden by subclasses that support checked-in change sets.
	 * @param configuration the configuration for the page that will be displaying the change sets
	 * @return a change set collector
	 */
	public SyncInfoSetChangeSetCollector createSyncInfoSetChangeSetCollector(ISynchronizePageConfiguration configuration) {
		return null;
	}

	/**
	 * Return an action group for contributing context menu items
	 * to the synchronize page while change sets are enabled.
	 * Return <code>null</code> if no custom actions are required.
	 * Note that only context menus can be contributed since the view menu
	 * and toolbar menu are fixed. This method can be overridden by subclasses
	 * who wish to support custom change set actions.
	 * @return an action group for contributing context menu items
	 * to the synchronize page while change sets are enabled or <code>null</code>
	 */
	public SynchronizePageActionGroup getActionGroup() {
		return null;
	}

	/**
	 * Returns whether checked-in change sets should be enabled for the given state
	 * in the configuration. The default is to enable for three-way incoming mode and
	 * two-way.
	 * @param configuration the configuration for a synchronize page
	 * @return whether checked-in change sets should be enabled for the given state
	 * in the configuration
	 */
	public boolean enableCheckedInChangeSetsFor(ISynchronizePageConfiguration configuration) {
		return supportsCheckedInChangeSets() &&
			(configuration.getMode() == ISynchronizePageConfiguration.INCOMING_MODE ||
					configuration.getComparisonType() == ISynchronizePageConfiguration.TWO_WAY);
	}

	/**
	 * Returns whether active change sets should be enabled for the given state
	 * in the configuration. The default is to enable for three-way outgoing mode.
	 * @param configuration the configuration for a synchronize page
	 * @return whether active change sets should be enabled for the given state
	 * in the configuration
	 */
	public boolean enableActiveChangeSetsFor(ISynchronizePageConfiguration configuration) {
		return supportsActiveChangeSets() &&
			configuration.getMode() == ISynchronizePageConfiguration.OUTGOING_MODE;
	}

	/**
	 * Return whether change sets should be enabled by default on pages
	 * that display the participant.
	 * @return whether change sets should be enabled by default on pages
	 * that display the participant
	 */
	public boolean enableChangeSetsByDefault() {
		return false;
	}

}
