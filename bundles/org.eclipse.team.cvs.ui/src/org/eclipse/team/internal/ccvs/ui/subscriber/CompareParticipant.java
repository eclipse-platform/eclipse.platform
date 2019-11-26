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
package org.eclipse.team.internal.ccvs.ui.subscriber;

import java.util.Arrays;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.IPreferenceChangeListener;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.PreferenceChangeEvent;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.subscribers.Subscriber;
import org.eclipse.team.core.synchronize.*;
import org.eclipse.team.internal.ccvs.core.CVSCompareSubscriber;
import org.eclipse.team.internal.ccvs.core.CVSTag;
import org.eclipse.team.internal.ccvs.ui.*;
import org.eclipse.team.internal.ccvs.ui.actions.ShowAnnotationAction;
import org.eclipse.team.internal.ccvs.ui.actions.ShowResourceInHistoryAction;
import org.eclipse.team.internal.core.subscribers.ActiveChangeSetManager;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.ui.TeamUI;
import org.eclipse.team.ui.synchronize.*;

public class CompareParticipant extends CVSParticipant implements IPreferenceChangeListener {
	
	public static final String CONTEXT_MENU_CONTRIBUTION_GROUP = "context_group_1"; //$NON-NLS-1$
	public static final String NON_MODAL_CONTEXT_MENU_CONTRIBUTION_GROUP = "context_group_2"; //$NON-NLS-1$

	/**
	 * Actions for the compare particpant's toolbar
	 */
	public class CompareParticipantActionContribution extends SynchronizePageActionGroup {
		@Override
		public void initialize(ISynchronizePageConfiguration configuration) {
			super.initialize(configuration);
			
			appendToGroup(
					ISynchronizePageConfiguration.P_CONTEXT_MENU, 
					CONTEXT_MENU_CONTRIBUTION_GROUP,
					new CompareRevertAction(configuration));
			
			if (!configuration.getSite().isModal()) {
				ShowAnnotationAction showAnnotationAction = new ShowAnnotationAction();
				appendToGroup(
						ISynchronizePageConfiguration.P_CONTEXT_MENU, 
						NON_MODAL_CONTEXT_MENU_CONTRIBUTION_GROUP,
						new CVSActionDelegateWrapper(showAnnotationAction, configuration));
				appendToGroup(
						ISynchronizePageConfiguration.P_CONTEXT_MENU, 
						NON_MODAL_CONTEXT_MENU_CONTRIBUTION_GROUP,
						new CVSActionDelegateWrapper(new ShowResourceInHistoryAction(), configuration));
			}
		}
	}
	
	private SyncInfoFilter contentComparison = new SyncInfoFilter() {
		private SyncInfoFilter contentCompare = new SyncInfoFilter.ContentComparisonSyncInfoFilter();
		@Override
		public boolean select(SyncInfo info, IProgressMonitor monitor) {
			// Want to select infos whose contents do not match
			return !contentCompare.select(info, monitor);
		}
	};
	
	private SyncInfoFilter createSyncInfoFilter() {
		final SyncInfoFilter regexFilter = createRegexFilter();
		if (isConsiderContents() && regexFilter != null) {
			return new SyncInfoFilter() {
				@Override
				public boolean select(SyncInfo info, IProgressMonitor monitor) {
					return contentComparison.select(info, monitor)
							&& !regexFilter.select(info, monitor);
				}
			};
		} else if (isConsiderContents()) {
			return new SyncInfoFilter() {
				@Override
				public boolean select(SyncInfo info, IProgressMonitor monitor) {
					return contentComparison.select(info, monitor);
				}
			};
		} else if (regexFilter != null) {
			return new SyncInfoFilter() {
				@Override
				public boolean select(SyncInfo info, IProgressMonitor monitor) {
					// want to select infos which contain at least one unmatched difference
					return !regexFilter.select(info, monitor);
				}
			};
		}
		return null;
	}

	private boolean isConsiderContents() {
		return CVSUIPlugin.getPlugin().getPreferenceStore().getBoolean(ICVSUIConstants.PREF_CONSIDER_CONTENTS);
	}

	private SyncInfoFilter createRegexFilter() {
		if (isConsiderContents()) {
			String pattern = CVSUIPlugin.getPlugin().getPreferenceStore().getString(ICVSUIConstants.PREF_SYNCVIEW_REGEX_FILTER_PATTERN);
			if (pattern != null && !pattern.isEmpty()) {
				return new RegexSyncInfoFilter(pattern);
			}
		}
		return null;
	}

	public CompareParticipant(CVSCompareSubscriber subscriber) {
		setSubscriber(subscriber);
	}

	@Override
	protected void setSubscriber(Subscriber subscriber) {
		super.setSubscriber(subscriber);
		setSyncInfoFilter(createSyncInfoFilter());
		try {
			ISynchronizeParticipantDescriptor descriptor = TeamUI.getSynchronizeManager().getParticipantDescriptor(CVSCompareSubscriber.ID);
			setInitializationData(descriptor);
			CVSCompareSubscriber s = getCVSCompareSubscriber();
			setSecondaryId(s.getId().getLocalName());
		} catch (CoreException e) {
			CVSUIPlugin.log(e);
		}
		((IEclipsePreferences) CVSUIPlugin.getPlugin().getInstancePreferences().node("")).addPreferenceChangeListener(this); //$NON-NLS-1$
	}
	
	@Override
	public String getName() {
		return NLS.bind(CVSUIMessages.CompareParticipant_0, new String[] { getSubscriber().getName(), Utils.convertSelection(getSubscriber().roots()) }); 
	}
	
	/*
	 * Returns a merge participant that exist and is configured with the given set of resources, start, and end tags.
	 */
	public static CompareParticipant getMatchingParticipant(IResource[] resources, CVSTag tag) {
		ISynchronizeParticipantReference[] refs = TeamUI.getSynchronizeManager().getSynchronizeParticipants();
		for (ISynchronizeParticipantReference reference : refs) {
			if (reference.getId().equals(CVSCompareSubscriber.ID)) {
				try {
					CompareParticipant p = (CompareParticipant) reference.getParticipant();
					if (p.matches(resources, tag)) {
						return p;
					}
				} catch (TeamException e) {
					continue;
				}
			}
		}
		return null;
	}
	
	/**
	 * Return whether this compare subscriber matches persisly the 
	 * provided list of resources and the single tag.
	 * @param resources the resources
	 * @param tag the tag
	 * @return whether this compare subscriber matches persisly the 
	 * provided list of resources and the single tag
	 */
	protected boolean matches(IResource[] resources, CVSTag tag) {
		CVSTag existingTag = getCVSCompareSubscriber().getTag();
		// The tag can be null if the compare participant has a different tag for each root
		if (existingTag != null) {
			IResource[] roots = getResources();
			Arrays.sort(resources, Utils.resourceComparator);
			Arrays.sort(roots, Utils.resourceComparator);
			if (Arrays.equals(resources, roots) && existingTag.equals(tag)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Return the subscriber as an instance of CVSCompareSubscriber.
	 * @return the subscriber as an instance of CVSCompareSubscriber
	 */
	public CVSCompareSubscriber getCVSCompareSubscriber() {
		return (CVSCompareSubscriber)getSubscriber();
	}

	@Override
	protected void initializeConfiguration(ISynchronizePageConfiguration configuration) {
		super.initializeConfiguration(configuration);
		configuration.addMenuGroup(
				ISynchronizePageConfiguration.P_CONTEXT_MENU, 
				CONTEXT_MENU_CONTRIBUTION_GROUP);
		configuration.addMenuGroup(
				ISynchronizePageConfiguration.P_CONTEXT_MENU, 
				NON_MODAL_CONTEXT_MENU_CONTRIBUTION_GROUP);
		configuration.addActionContribution(new CompareParticipantActionContribution());
	}
	
	@Override
	public void dispose() {
		super.dispose();
		((IEclipsePreferences) CVSUIPlugin.getPlugin().getInstancePreferences().node("")).removePreferenceChangeListener(this); //$NON-NLS-1$
		getCVSCompareSubscriber().dispose();
	}

	@Override
	public void preferenceChange(PreferenceChangeEvent event) {
		if (event.getKey().equals(ICVSUIConstants.PREF_CONSIDER_CONTENTS) || event.getKey().equals(ICVSUIConstants.PREF_SYNCVIEW_REGEX_FILTER_PATTERN)) {
			SyncInfoFilter filter = createSyncInfoFilter();
			if (filter != null) {
				setSyncInfoFilter(filter);
			} else {
				setSyncInfoFilter(new FastSyncInfoFilter());
			}
		}
	}
	
	@Override
	protected String getLongTaskName() {
		return getName();
	}
	
	@Override
	protected String getShortTaskName() {
		return CVSUIMessages.Participant_comparing; 
	}
	
	@Override
	protected CVSChangeSetCapability createChangeSetCapability() {
		return new CVSChangeSetCapability() {
			@Override
			public ActiveChangeSetManager getActiveChangeSetManager() {
				return CVSUIPlugin.getPlugin().getChangeSetManager();
			}
			@Override
			public boolean enableActiveChangeSetsFor(ISynchronizePageConfiguration configuration) {
				return super.enableActiveChangeSetsFor(configuration) ||
					configuration.getComparisonType() == ISynchronizePageConfiguration.TWO_WAY;
			}
		};
	}
}
