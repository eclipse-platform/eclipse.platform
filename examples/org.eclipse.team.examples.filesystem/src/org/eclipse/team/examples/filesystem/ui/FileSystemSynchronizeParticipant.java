/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
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
package org.eclipse.team.examples.filesystem.ui;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.viewers.ILabelDecorator;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.mapping.ISynchronizationScopeManager;
import org.eclipse.team.core.mapping.provider.MergeContext;
import org.eclipse.team.core.mapping.provider.SynchronizationContext;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.core.variants.IResourceVariant;
import org.eclipse.team.examples.filesystem.FileSystemPlugin;
import org.eclipse.team.examples.filesystem.subscriber.FileSystemMergeContext;
import org.eclipse.team.examples.filesystem.subscriber.FileSystemSubscriber;
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.team.ui.TeamUI;
import org.eclipse.team.ui.mapping.SynchronizationActionProvider;
import org.eclipse.team.ui.synchronize.ISynchronizeModelElement;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.team.ui.synchronize.ModelSynchronizeParticipant;
import org.eclipse.team.ui.synchronize.ModelSynchronizeParticipantActionGroup;


/**
 * This is an example synchronize participant for the file system provider. It will allow
 * showing synchronization state for local resources mapped to a remote file system
 * location.
 *
 * @since 3.0
 */
public class FileSystemSynchronizeParticipant extends ModelSynchronizeParticipant {

	/**
	 * The participant id for the org.eclipse.team.ui.synchronizeParticipant extension point.
	 */
	public static final String ID = "org.eclipse.team.examples.filesystem.participant"; //$NON-NLS-1$

	/**
	 * The viewer id for the org.eclipse.ui.navigator.viewer extension point.
	 */
	public static final String VIEWER_ID = "org.eclipse.team.examples.filesystem.syncViewer"; //$NON-NLS-1$

	/**
	 * Custom menu groups included in the viewer definition in the plugin.xml.
	 */
	public static final String CONTEXT_MENU_PUT_GROUP_1 = "put"; //$NON-NLS-1$
	public static final String CONTEXT_MENU_OVERWRITE_GROUP_1 = "overwrite"; //$NON-NLS-1$

	/**
	 * A custom label decorator that will show the remote mapped path for each
	 * file.
	 */
	public static class FileSystemParticipantLabelDecorator extends LabelProvider implements ILabelDecorator {
		@Override
		public Image decorateImage(Image image, Object element) {
			return image;
		}

		@Override
		public String decorateText(String text, Object element) {
			try {
				if (element instanceof ISynchronizeModelElement) {
					IResource resource = ((ISynchronizeModelElement) element).getResource();
					if (resource != null && resource.getType() == IResource.FILE) {
						SyncInfo info = FileSystemSubscriber.getInstance().getSyncInfo(resource);
						IResourceVariant variant = info.getRemote();
						if (variant != null) {
							return text + " (" + variant.getContentIdentifier() + ")";
						}
					}
				}
			} catch (TeamException e) {
				FileSystemPlugin.log(e);
			}
			return null;
		}
	}

	/**
	 * Action group that contributes the get an put menus to the context menu
	 * in the synchronize view
	 */
	public class FileSystemParticipantActionGroup extends ModelSynchronizeParticipantActionGroup {
		@Override
		public void initialize(ISynchronizePageConfiguration configuration) {
			super.initialize(configuration);
			appendToGroup(
					ISynchronizePageConfiguration.P_CONTEXT_MENU,
					CONTEXT_MENU_PUT_GROUP_1,
					new ModelPutAction("Put", configuration));
		}

		@Override
		protected void configureMergeAction(String mergeActionId, Action action) {
			if (mergeActionId == SynchronizationActionProvider.MERGE_ACTION_ID) {
				// Custom label for overwrite
				action.setText("Get");
			} else if (mergeActionId == SynchronizationActionProvider.MARK_AS_MERGE_ACTION_ID) {
				// Custom label for mark-as-merged
				action.setText("Ignore Remote");
			} else {
				super.configureMergeAction(mergeActionId, action);
			}
		}

		@Override
		protected void addToContextMenu(String mergeActionId, Action action, IMenuManager manager) {
			IContributionItem group = null;
			if (mergeActionId == SynchronizationActionProvider.MERGE_ACTION_ID) {
				// This could be left out since this is the default group but it is here for illustration
				group = manager.find(MERGE_ACTION_GROUP);
			} else if (mergeActionId == SynchronizationActionProvider.OVERWRITE_ACTION_ID) {
				// This is a custom group for the overwrite command
				group = manager.find(CONTEXT_MENU_OVERWRITE_GROUP_1);
			} else if (mergeActionId == SynchronizationActionProvider.MARK_AS_MERGE_ACTION_ID) {
				// This could be left out since this is the default group but it is here for illustration
				group = manager.find(OTHER_ACTION_GROUP);
			} else {
				super.addToContextMenu(mergeActionId, action, manager);
				return;
			}
			if (group != null) {
				manager.appendToGroup(group.getId(), action);
			} else {
				manager.add(action);
			}
		}

	}

	/**
	 * Create a file system participant. This method is invoked by the
	 * Synchronize view when a persisted participant is being restored.
	 * Participants that are persisted must override the
	 * {@link #restoreContext(ISynchronizationScopeManager)} method to recreate
	 * the context and may also need to override the
	 * {@link #createScopeManager(ResourceMapping[])} method if they require a
	 * custom scope manager.
	 */
	public FileSystemSynchronizeParticipant() {
		super();
	}

	/**
	 * Create the participant for the given context. This method is used
	 * by the file system plugin to create a participant and then add it to
	 * the sync view (or show it is some other container).
	 * @param context the synchronization context
	 */
	public FileSystemSynchronizeParticipant(SynchronizationContext context) {
		super(context);
		try {
			setInitializationData(TeamUI.getSynchronizeManager().getParticipantDescriptor(ID));
		} catch (CoreException e) {
			TeamUIPlugin.log(e);
		}
		setSecondaryId(Long.toString(System.currentTimeMillis()));
	}

	@Override
	protected void initializeConfiguration(ISynchronizePageConfiguration configuration) {
		super.initializeConfiguration(configuration);
		configuration.setProperty(ISynchronizePageConfiguration.P_VIEWER_ID, VIEWER_ID);

		// Add the label decorator
		configuration.addLabelDecorator(new FileSystemParticipantLabelDecorator());
	}

	@Override
	protected ModelSynchronizeParticipantActionGroup createMergeActionGroup() {
		return new FileSystemParticipantActionGroup();
	}

	@Override
	protected MergeContext restoreContext(ISynchronizationScopeManager manager) {
		return new FileSystemMergeContext(manager);
	}

	@Override
	protected ISynchronizationScopeManager createScopeManager(ResourceMapping[] mappings) {
		return FileSystemOperation.createScopeManager(getName(), mappings);
	}
}
