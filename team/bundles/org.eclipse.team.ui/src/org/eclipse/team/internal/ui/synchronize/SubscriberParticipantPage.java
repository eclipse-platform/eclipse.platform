/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
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

import org.eclipse.swt.widgets.Composite;
import org.eclipse.team.core.synchronize.FastSyncInfoFilter;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.internal.core.subscribers.SubscriberSyncInfoCollector;
import org.eclipse.team.internal.core.subscribers.WorkingSetFilteredSyncInfoCollector;
import org.eclipse.team.internal.ui.synchronize.actions.DefaultSynchronizePageActions;
import org.eclipse.team.internal.ui.synchronize.actions.SubscriberActionContribution;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.team.ui.synchronize.SubscriberParticipant;

/**
 * A synchronize view page that works with participants that are subclasses of
 * {@link SubscriberParticipant}. It shows changes in the tree or table view
 * and supports navigation, opening, and filtering changes.
 * <p>
 * Clients can subclass to extend the label decoration or add action bar
 * contributions. For more extensive modifications, clients should create
 * their own custom page.
 * </p>
 * @since 3.0
 */
public final class SubscriberParticipantPage extends AbstractSynchronizePage {

	private SubscriberParticipant participant;

	private final static int[] INCOMING_MODE_FILTER = new int[] {SyncInfo.CONFLICTING, SyncInfo.INCOMING};
	private final static int[] OUTGOING_MODE_FILTER = new int[] {SyncInfo.CONFLICTING, SyncInfo.OUTGOING};
	private final static int[] BOTH_MODE_FILTER = new int[] {SyncInfo.CONFLICTING, SyncInfo.INCOMING, SyncInfo.OUTGOING};
	private final static int[] CONFLICTING_MODE_FILTER = new int[] {SyncInfo.CONFLICTING};

	/**
	 * Filters out-of-sync resources by working set and mode
	 */
	private WorkingSetFilteredSyncInfoCollector collector;

	/**
	 * Constructs a new SynchronizeView.
	 *
	 * @param configuration
	 *            a synchronize page configuration
	 * @param subscriberCollector
	 *            the subscriber's collector
	 */
	public SubscriberParticipantPage(ISynchronizePageConfiguration configuration, SubscriberSyncInfoCollector subscriberCollector) {
		super(configuration);
		this.participant = (SubscriberParticipant)configuration.getParticipant();
		configuration.setComparisonType(isThreeWay()
						? ISynchronizePageConfiguration.THREE_WAY
						: ISynchronizePageConfiguration.TWO_WAY);
		configuration.addActionContribution(new DefaultSynchronizePageActions());
		configuration.addActionContribution(new SubscriberActionContribution());
		initializeCollector(configuration, subscriberCollector);
	}

	/**
	 * @return Returns the participant.
	 */
	public SubscriberParticipant getParticipant() {
		return participant;
	}

	@Override
	protected AbstractViewerAdvisor createViewerAdvisor(Composite parent) {
		return new TreeViewerAdvisor(parent, getConfiguration());
	}

	/*
	 * This method is invoked from <code>setMode</code> when the mode has changed.
	 * It sets the filter on the collector to show the <code>SyncInfo</code>
	 * appropriate for the mode.
	 * @param mode the new mode (one of <code>INCOMING_MODE_FILTER</code>,
	 * <code>OUTGOING_MODE_FILTER</code>, <code>CONFLICTING_MODE_FILTER</code>
	 * or <code>BOTH_MODE_FILTER</code>)
	 */
	@Override
	protected void updateMode(int mode) {
		if(collector != null && isThreeWay()) {

			int[] modeFilter = BOTH_MODE_FILTER;
			switch(mode) {
			case ISynchronizePageConfiguration.INCOMING_MODE:
				modeFilter = INCOMING_MODE_FILTER; break;
			case ISynchronizePageConfiguration.OUTGOING_MODE:
				modeFilter = OUTGOING_MODE_FILTER; break;
			case ISynchronizePageConfiguration.BOTH_MODE:
				modeFilter = BOTH_MODE_FILTER; break;
			case ISynchronizePageConfiguration.CONFLICTING_MODE:
				modeFilter = CONFLICTING_MODE_FILTER; break;
			}

			collector.setFilter(
					new FastSyncInfoFilter.AndSyncInfoFilter(
							new FastSyncInfoFilter[] {
									new FastSyncInfoFilter.SyncInfoDirectionFilter(modeFilter)
							}));
		}
	}

	private void initializeCollector(ISynchronizePageConfiguration configuration, SubscriberSyncInfoCollector subscriberCollector) {
		SubscriberParticipant participant = getParticipant();
		collector = new WorkingSetFilteredSyncInfoCollector(subscriberCollector, participant.getSubscriber().roots());
		updateMode(configuration.getMode());
		collector.reset();
		configuration.setProperty(ISynchronizePageConfiguration.P_SYNC_INFO_SET, collector.getSyncInfoTree());
		configuration.setProperty(SynchronizePageConfiguration.P_WORKING_SET_SYNC_INFO_SET, collector.getWorkingSetSyncInfoSet());
	}

	protected boolean isThreeWay() {
		return getParticipant().getSubscriber().getResourceComparator().isThreeWay();
	}

	@Override
	public void reset() {
		getParticipant().reset();
	}

	/*
	 * Provide internal access to the collector
	 * @return Returns the collector.
	 */
	public WorkingSetFilteredSyncInfoCollector getCollector() {
		return collector;
	}

	@Override
	public void dispose() {
		super.dispose();
		collector.dispose();
	}

	@Override
	protected ChangesSection createChangesSection(Composite parent) {
		return new SyncInfoSetChangesSection(parent, this, getConfiguration());
	}
}
