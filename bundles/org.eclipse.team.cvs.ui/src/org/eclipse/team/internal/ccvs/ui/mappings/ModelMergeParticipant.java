/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ccvs.ui.mappings;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.runtime.*;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.mapping.ISynchronizationScope;
import org.eclipse.team.core.mapping.ISynchronizationScopeManager;
import org.eclipse.team.core.mapping.provider.MergeContext;
import org.eclipse.team.core.subscribers.SubscriberScopeManager;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.ui.CVSUIMessages;
import org.eclipse.team.internal.ccvs.ui.CVSUIPlugin;
import org.eclipse.team.internal.ccvs.ui.actions.ShowAnnotationAction;
import org.eclipse.team.internal.ccvs.ui.actions.ShowResourceInHistoryAction;
import org.eclipse.team.internal.ccvs.ui.subscriber.CVSActionDelegateWrapper;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.ui.TeamUI;
import org.eclipse.team.ui.synchronize.*;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.PartInitException;

public class ModelMergeParticipant extends CVSModelSynchronizeParticipant {

	public static final String VIEWER_ID = "org.eclipse.team.cvs.ui.mergeSynchronization"; //$NON-NLS-1$
	
	public static final String CONTEXT_MENU_CONTRIBUTION_GROUP_1 = "otherActions1"; //$NON-NLS-1$
	
	public static final String ID = "org.eclipse.team.cvs.ui.modelMergeParticipant"; //$NON-NLS-1$
	
	private final static String CTX_SUBSCRIBER = "mergeSubscriber"; //$NON-NLS-1$
	private final static String CTX_ROOT = "root"; //$NON-NLS-1$
	private final static String CTX_ROOT_PATH = "root_resource"; //$NON-NLS-1$
	private final static String CTX_START_TAG = "start_tag"; //$NON-NLS-1$
	private final static String CTX_START_TAG_TYPE = "start_tag_type"; //$NON-NLS-1$
	private final static String CTX_END_TAG = "end_tag"; //$NON-NLS-1$
	private final static String CTX_END_TAG_TYPE = "end_tag_type"; //$NON-NLS-1$
	
	public class MergeActionGroup extends ModelSynchronizeParticipantActionGroup {
		@Override
		public void initialize(ISynchronizePageConfiguration configuration) {
			super.initialize(configuration);
			if (!configuration.getSite().isModal()) {
				appendToGroup(
					ISynchronizePageConfiguration.P_CONTEXT_MENU, 
					CONTEXT_MENU_CONTRIBUTION_GROUP_1,
					new CVSActionDelegateWrapper(new ShowAnnotationAction(), configuration));
				appendToGroup(
					ISynchronizePageConfiguration.P_CONTEXT_MENU, 
					CONTEXT_MENU_CONTRIBUTION_GROUP_1,
					new CVSActionDelegateWrapper(new ShowResourceInHistoryAction(), configuration));
			}
		}
	}

	private CVSMergeSubscriber subscriber;
	
	public ModelMergeParticipant() {
	}

	public ModelMergeParticipant(MergeSubscriberContext context) {
		super(context);
		subscriber = getSubscriber();
		initialize();
	}
	
	@Override
	protected ModelSynchronizeParticipantActionGroup createMergeActionGroup() {
		return new MergeActionGroup();
	}

	@Override
	protected void initializeConfiguration(ISynchronizePageConfiguration configuration) {
		configuration.setProperty(ISynchronizePageConfiguration.P_VIEWER_ID, VIEWER_ID);
		super.initializeConfiguration(configuration);
		configuration.setSupportedModes(ISynchronizePageConfiguration.INCOMING_MODE | ISynchronizePageConfiguration.CONFLICTING_MODE);
		configuration.setMode(ISynchronizePageConfiguration.INCOMING_MODE);
	}
	
	private void initialize() {
		try {
			ISynchronizeParticipantDescriptor descriptor = TeamUI.getSynchronizeManager().getParticipantDescriptor(ID); 
			setInitializationData(descriptor);
			CVSMergeSubscriber s = getSubscriber();
			setSecondaryId(s.getId().getLocalName());
		} catch (CoreException e) {
			CVSUIPlugin.log(e);
		}
	}

	private CVSMergeSubscriber getSubscriber() {
		return (CVSMergeSubscriber)((MergeSubscriberContext)getContext()).getSubscriber();
	}
	
	@Override
	public void init(String secondaryId, IMemento memento) throws PartInitException {
		if(memento != null) {
			ISynchronizeParticipantDescriptor descriptor = TeamUI.getSynchronizeManager().getParticipantDescriptor(ID); 
			String qualifier = descriptor.getId();
			String localname = secondaryId;
			if(qualifier == null || localname == null) {
				throw new PartInitException(CVSUIMessages.MergeSynchronizeParticipant_8); 
			}
			try {
				subscriber = read(new QualifiedName(qualifier, localname), memento.getChild(CTX_SUBSCRIBER));
			} catch (CVSException e) {
				throw new PartInitException(CVSUIMessages.MergeSynchronizeParticipant_9, e); 
			}
		}
		try {
			super.init(secondaryId, memento);
		} catch (PartInitException e) {
			subscriber.cancel();
			throw e;
		}
	}
	
	@Override
	public void saveState(IMemento memento) {
		super.saveState(memento);
		write(subscriber, memento.createChild(CTX_SUBSCRIBER));
	}
	
	private CVSMergeSubscriber read(QualifiedName id, IMemento memento) throws CVSException {
		CVSTag start = new CVSTag(memento.getString(CTX_START_TAG), memento.getInteger(CTX_START_TAG_TYPE).intValue());
		CVSTag end = new CVSTag(memento.getString(CTX_END_TAG), memento.getInteger(CTX_END_TAG_TYPE).intValue());
		
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
	
	@Override
	protected String getShortTaskName() {
		return CVSUIMessages.Participant_merging;
	}
	
	@Override
	public void dispose() {
		if(TeamUI.getSynchronizeManager().get(getId(), getSecondaryId()) != null) {
			// If the participant is managed by the synchronize manager then we
			// we don't want to flush the synchronizer cache.
			((MergeSubscriberContext)getContext()).setCancelSubscriber(false);
		}
		super.dispose();
	}
	
	@Override
	protected ISynchronizationScopeManager createScopeManager(ResourceMapping[] mappings) {
		return new SubscriberScopeManager(subscriber.getName(), 
				mappings, subscriber, true);
	}
	
	@Override
	protected MergeContext restoreContext(ISynchronizationScopeManager manager) throws CoreException {
		return MergeSubscriberContext.createContext(manager, subscriber);
	}
	
	@Override
	public String getName() {		
		return NLS.bind(CVSUIMessages.CompareParticipant_0, new String[] { getSubscriber().getName(), Utils.getScopeDescription(getContext().getScope()) });  
	}
	
	/*
	 * Returns a merge participant that exist and is configured with the given set of resources, start, and end tags.
	 */
	public static ModelMergeParticipant getMatchingParticipant(ResourceMapping[] mappings, CVSTag startTag, CVSTag endTag) {
		ISynchronizeParticipantReference[] refs = TeamUI.getSynchronizeManager().getSynchronizeParticipants();
		for (ISynchronizeParticipantReference reference : refs) {
			if (reference.getId().equals(ID)) {
				ModelMergeParticipant p;
				try {
					p = (ModelMergeParticipant) reference.getParticipant();
				} catch (TeamException e) {
					continue;
				}
				ISynchronizationScope scope = p.getContext().getScope().asInputScope();
				ResourceMapping[] roots = scope.getMappings();
				if (roots.length == mappings.length) {
					boolean match = true;
					for (ResourceMapping mapping : mappings) {
						if (scope.getTraversals(mapping) == null) {
							// The mapping is not in the scope so the participants don't match
							match = false;
							break;
						}
					}
					if (match && p.getSubscriber().getStartTag().equals(startTag) && p.getSubscriber().getEndTag().equals(endTag)) {
						return p;
					}
				}
			}
		}
		return null;
	}
}
