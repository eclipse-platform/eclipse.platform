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
package org.eclipse.team.internal.ccvs.ui.subscriber;

import java.util.*;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.*;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.subscribers.Subscriber;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.ui.CVSUIMessages;
import org.eclipse.team.internal.ccvs.ui.CVSUIPlugin;
import org.eclipse.team.internal.ccvs.ui.actions.ShowAnnotationAction;
import org.eclipse.team.internal.ccvs.ui.actions.ShowResourceInHistoryAction;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.ui.TeamUI;
import org.eclipse.team.ui.synchronize.*;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.PartInitException;

public class MergeSynchronizeParticipant extends CVSParticipant {
	
	/**
	 * The id of a workspace action group to which additions actions can 
	 * be added.
	 */
	public static final String TOOLBAR_CONTRIBUTION_GROUP = "toolbar_group"; //$NON-NLS-1$
	
	public static final String CONTEXT_MENU_CONTRIBUTION_GROUP = "context_group_1"; //$NON-NLS-1$
	public static final String NON_MODAL_CONTEXT_MENU_CONTRIBUTION_GROUP = "context_group_2"; //$NON-NLS-1$

	private final static String CTX_ROOT = "root"; //$NON-NLS-1$
	private final static String CTX_ROOT_PATH = "root_resource"; //$NON-NLS-1$
	private final static String CTX_START_TAG = "start_tag"; //$NON-NLS-1$
	private final static String CTX_START_TAG_TYPE = "start_tag_type"; //$NON-NLS-1$
	private final static String CTX_END_TAG = "end_tag"; //$NON-NLS-1$
	private final static String CTX_END_TAG_TYPE = "end_tag_type"; //$NON-NLS-1$
	
	/**
	 * Actions for the merge particpant's toolbar
	 */
	public class MergeParticipantActionContribution extends SynchronizePageActionGroup {
		private MergeUpdateAction updateAction;
		@Override
		public void initialize(ISynchronizePageConfiguration configuration) {
			super.initialize(configuration);
			
			updateAction = new MergeUpdateAction(
					configuration, 
					getVisibleRootsSelectionProvider(), 
					"WorkspaceToolbarUpdateAction."); //$NON-NLS-1$
			updateAction.setPromptBeforeUpdate(true);
			appendToGroup(
					ISynchronizePageConfiguration.P_TOOLBAR_MENU,
					TOOLBAR_CONTRIBUTION_GROUP,
					updateAction);
			
			appendToGroup(
					ISynchronizePageConfiguration.P_CONTEXT_MENU, 
					CONTEXT_MENU_CONTRIBUTION_GROUP,
					new MergeUpdateAction(configuration));
			appendToGroup(
					ISynchronizePageConfiguration.P_CONTEXT_MENU, 
					CONTEXT_MENU_CONTRIBUTION_GROUP,
					new ConfirmMergedAction(configuration));
			
			if (!configuration.getSite().isModal()) {
				appendToGroup(
					ISynchronizePageConfiguration.P_CONTEXT_MENU, 
					NON_MODAL_CONTEXT_MENU_CONTRIBUTION_GROUP,
					new CVSActionDelegateWrapper(new ShowAnnotationAction(), configuration));
				appendToGroup(
					ISynchronizePageConfiguration.P_CONTEXT_MENU, 
					NON_MODAL_CONTEXT_MENU_CONTRIBUTION_GROUP,
					new CVSActionDelegateWrapper(new ShowResourceInHistoryAction(), configuration));
			}

		}
	}
	
	public MergeSynchronizeParticipant() {
		super();
	}
	
	public MergeSynchronizeParticipant(CVSMergeSubscriber subscriber) {
		setSubscriber(subscriber);
	}
	
	@Override
	public  void setSubscriber(Subscriber subscriber) {
		super.setSubscriber(subscriber);
		try {
			ISynchronizeParticipantDescriptor descriptor = TeamUI.getSynchronizeManager().getParticipantDescriptor(CVSMergeSubscriber.ID); 
			setInitializationData(descriptor);
			CVSMergeSubscriber s = (CVSMergeSubscriber)getSubscriber();
			setSecondaryId(s.getId().getLocalName());
		} catch (CoreException e) {
			CVSUIPlugin.log(e);
		}
	}
	
	@Override
	public void init(String secondayId, IMemento memento) throws PartInitException {
		super.init(secondayId, memento);
		if(memento != null) {
			ISynchronizeParticipantDescriptor descriptor = TeamUI.getSynchronizeManager().getParticipantDescriptor(CVSMergeSubscriber.ID); 
			String qualifier = descriptor.getId();
			String localname = secondayId;
			if(qualifier == null || localname == null) {
				throw new PartInitException(CVSUIMessages.MergeSynchronizeParticipant_8); 
			}
			try {
				setSubscriber(read(new QualifiedName(qualifier, localname), memento));
			} catch (CVSException e) {
				throw new PartInitException(CVSUIMessages.MergeSynchronizeParticipant_9, e); 
			}
		}
	}
	
	@Override
	public void saveState(IMemento memento) {
		super.saveState(memento);
		CVSMergeSubscriber s = (CVSMergeSubscriber)getSubscriber();
		write(s, memento);
	}
		
	@Override
	public void dispose() {
		super.dispose();
		if(TeamUI.getSynchronizeManager().get(getId(), getSecondaryId()) == null) {
			// If the participant isn't managed by the synchronize manager then we
			// must ensure that the state cached in the synchronizer is flushed.
			flushStateCache();
		}
	}
	
	@Override
	public String getName() {		
		return NLS.bind(CVSUIMessages.CompareParticipant_0, new String[] { ((CVSMergeSubscriber)getSubscriber()).getName(), Utils.convertSelection(getSubscriber().roots()) });  
	}
	
	/*
	 * Returns the start tag for this merge participant. The start tag is actually stored with the subscriber.
	 */
	protected CVSTag getStartTag() {
		return ((CVSMergeSubscriber)getSubscriber()).getStartTag();
	}
	
	/*
	 * Returns the end tag for this merge participant. The end tag is actually stored with the subscriber.
	 */
	protected CVSTag getEndTag() {
		return ((CVSMergeSubscriber)getSubscriber()).getEndTag();
	}
	
	/*
	 * Returns a merge participant that exist and is configured with the given set of resources, start, and end tags.
	 */
	public static MergeSynchronizeParticipant getMatchingParticipant(IResource[] resources, CVSTag startTag, CVSTag endTag) {
		ISynchronizeParticipantReference[] refs = TeamUI.getSynchronizeManager().getSynchronizeParticipants();
		for (ISynchronizeParticipantReference reference : refs) {
			if (reference.getId().equals(CVSMergeSubscriber.ID)) {
				MergeSynchronizeParticipant p;
				try {
					p = (MergeSynchronizeParticipant) reference.getParticipant();
				} catch (TeamException e) {
					continue;
				}
				IResource[] roots = p.getResources();
				Arrays.sort(resources, Utils.resourceComparator);
				Arrays.sort(roots, Utils.resourceComparator);
				if (Arrays.equals(resources, roots) && p.getStartTag().equals(startTag) && p.getEndTag().equals(endTag)) {
					return p;
				}
			}
		}
		return null;
	}
	
	private void write(CVSMergeSubscriber s, IMemento memento) {
		// start and end tags
		CVSTag start = s.getStartTag();
		CVSTag end = s.getEndTag();
		memento.putString(CTX_START_TAG, start.getName());
		memento.putInteger(CTX_START_TAG_TYPE, start.getType());
		memento.putString(CTX_END_TAG, end.getName());
		memento.putInteger(CTX_END_TAG_TYPE, end.getType());
		
		// resource roots
		IResource[] roots = s.roots();
		for (IResource resource : roots) {
			IMemento rootNode = memento.createChild(CTX_ROOT);
			rootNode.putString(CTX_ROOT_PATH, resource.getFullPath().toString());
		}
	}
	
	private CVSMergeSubscriber read(QualifiedName id, IMemento memento) throws CVSException {
		CVSTag start = new CVSTag(memento.getString(CTX_START_TAG), memento.getInteger(CTX_START_TAG_TYPE).intValue()); // 
		CVSTag end = new CVSTag(memento.getString(CTX_END_TAG), memento.getInteger(CTX_END_TAG_TYPE).intValue()); // 
		
		IMemento[] rootNodes = memento.getChildren(CTX_ROOT);
		if(rootNodes == null || rootNodes.length == 0) {
			throw new CVSException(NLS.bind(CVSUIMessages.MergeSynchronizeParticipant_10, new String[] { id.toString() })); 
		}
		
		List<IResource> resources = new ArrayList<>();
		for (IMemento rootNode : rootNodes) {
			IPath path = new Path(rootNode.getString(CTX_ROOT_PATH)); 
			IResource resource = ResourcesPlugin.getWorkspace().getRoot().findMember(path, true /* include phantoms */);
			if(resource != null) {
				resources.add(resource);
			} else {
				// log that a resource previously in the merge set is no longer in the workspace
				CVSProviderPlugin.log(IStatus.INFO, NLS.bind(CVSUIMessages.MergeSynchronizeParticipant_11, new String[] { path.toString() }), null); 
			}
		}
		if(resources.isEmpty()) {
			throw new CVSException(NLS.bind(CVSUIMessages.MergeSynchronizeParticipant_12, new String[] { id.toString() })); 
		}
		IResource[] roots = resources.toArray(new IResource[resources.size()]);
		return new CVSMergeSubscriber(id, roots, start, end);
	}
	
	private void flushStateCache() {
		((CVSMergeSubscriber)getSubscriber()).cancel();
	}

	@Override
	protected void initializeConfiguration(ISynchronizePageConfiguration configuration) {
		super.initializeConfiguration(configuration);
		configuration.addMenuGroup(ISynchronizePageConfiguration.P_TOOLBAR_MENU, TOOLBAR_CONTRIBUTION_GROUP);
		configuration.addMenuGroup(
				ISynchronizePageConfiguration.P_CONTEXT_MENU, 
				CONTEXT_MENU_CONTRIBUTION_GROUP);
		configuration.addMenuGroup(
				ISynchronizePageConfiguration.P_CONTEXT_MENU, 
				NON_MODAL_CONTEXT_MENU_CONTRIBUTION_GROUP);
		configuration.setSupportedModes(ISynchronizePageConfiguration.INCOMING_MODE | ISynchronizePageConfiguration.CONFLICTING_MODE);
		configuration.setMode(ISynchronizePageConfiguration.INCOMING_MODE);
		configuration.addActionContribution(new MergeParticipantActionContribution());
	}
	
	@Override
	protected String getLongTaskName() {
		return getName();
	}
	
	@Override
	protected String getShortTaskName() {
		return CVSUIMessages.Participant_merging; 
	}
	
	@Override
	protected CVSChangeSetCapability createChangeSetCapability() {
		// See bug 84561 for a description of the problems with Merge Change Sets
		return null;
	}
}
