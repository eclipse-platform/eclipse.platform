/*******************************************************************************
 * Copyright (c) 2000, 2017 IBM Corporation and others.
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
package org.eclipse.team.examples.localhistory;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.structuremergeviewer.IDiffElement;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.ILabelDecorator;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.graphics.Image;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.subscribers.Subscriber;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.core.variants.IResourceVariant;
import org.eclipse.team.ui.TeamUI;
import org.eclipse.team.ui.synchronize.ISynchronizeModelElement;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.team.ui.synchronize.ISynchronizeParticipantDescriptor;
import org.eclipse.team.ui.synchronize.SubscriberParticipant;
import org.eclipse.team.ui.synchronize.SynchronizeModelAction;
import org.eclipse.team.ui.synchronize.SynchronizeModelOperation;
import org.eclipse.team.ui.synchronize.SynchronizePageActionGroup;

public class LocalHistoryParticipant extends SubscriberParticipant {

	public static final String ID = "org.eclipse.team.synchronize.example"; //$NON-NLS-1$
	public static final String CONTEXT_MENU_CONTRIBUTION_GROUP = "context_group_1"; //$NON-NLS-1$

	private class LocalHistoryActionContribution extends SynchronizePageActionGroup {
		@Override
		public void initialize(ISynchronizePageConfiguration configuration) {
			super.initialize(configuration);
			appendToGroup(
					ISynchronizePageConfiguration.P_CONTEXT_MENU, CONTEXT_MENU_CONTRIBUTION_GROUP,
					new SynchronizeModelAction("Revert to latest in local history", configuration) { //$NON-NLS-1$
						@Override
						protected SynchronizeModelOperation getSubscriberOperation(ISynchronizePageConfiguration configuration, IDiffElement[] elements) {
							return new RevertAllOperation(configuration, elements);
						}
					});
		}
	}

	private class LocalHistoryDecorator extends LabelProvider implements ILabelDecorator {
		@Override
		public String decorateText(String text, Object element) {
			if(element instanceof ISynchronizeModelElement) {
				ISynchronizeModelElement node = (ISynchronizeModelElement)element;
				if(node instanceof IAdaptable) {
					SyncInfo info = ((IAdaptable)node).getAdapter(SyncInfo.class);
					if(info != null) {
						LocalHistoryVariant state = (LocalHistoryVariant)info.getRemote();
						return text+ " ("+ state.getContentIdentifier() + ")";
					}
				}
			}
			return text;
		}

		@Override
		public Image decorateImage(Image image, Object element) {
			return null;
		}
	}

	public LocalHistoryParticipant() {
		setSubscriber(new LocalHistorySubscriber());
	}

	@Override
	protected void setSubscriber(Subscriber subscriber) {
		super.setSubscriber(subscriber);
		try {
			ISynchronizeParticipantDescriptor descriptor = TeamUI.getSynchronizeManager().getParticipantDescriptor(ID);
			setInitializationData(descriptor);
			setSecondaryId(Long.toString(System.currentTimeMillis()));
		} catch (CoreException e) {
			// ignore
		}
	}

	@Override
	protected void initializeConfiguration(ISynchronizePageConfiguration configuration) {
		super.initializeConfiguration(configuration);
		configuration.addMenuGroup(
				ISynchronizePageConfiguration.P_CONTEXT_MENU,
				CONTEXT_MENU_CONTRIBUTION_GROUP);
		configuration.addActionContribution(new LocalHistoryActionContribution());
		configuration.addLabelDecorator(new LocalHistoryDecorator());
	}

	protected static SyncInfo getSyncInfo(ISynchronizeModelElement element) {
		if (element instanceof IAdaptable) {
			return ((IAdaptable)element).getAdapter(SyncInfo.class);
		}
		return null;
	}

	@Override
	public void prepareCompareInput(ISynchronizeModelElement element,
			CompareConfiguration config, IProgressMonitor monitor)
			throws TeamException {
		super.prepareCompareInput(element, config, monitor);

		SyncInfo sync = getSyncInfo(element);
		final IResourceVariant remote = sync.getRemote();
		if (remote != null) {
			config.setRightLabel(NLS.bind("Local History ({0})",
					new String[] { remote.getContentIdentifier() }));
		} else {
			config.setRightLabel("Local History");
		}
	}
}
