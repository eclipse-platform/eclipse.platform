/*******************************************************************************
 * Copyright (c) 2005, 2010 IBM Corporation and others.
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
package org.eclipse.team.ui.synchronize;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.team.core.mapping.IMergeContext;
import org.eclipse.team.core.mapping.ISynchronizationContext;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.internal.ui.mapping.CommonMenuManager;
import org.eclipse.team.internal.ui.mapping.MergeAction;
import org.eclipse.team.internal.ui.mapping.MergeIncomingChangesAction;
import org.eclipse.team.internal.ui.mapping.ModelSelectionDropDownAction;
import org.eclipse.team.internal.ui.synchronize.SynchronizePageConfiguration;
import org.eclipse.team.internal.ui.synchronize.actions.OpenInCompareAction;
import org.eclipse.team.internal.ui.synchronize.actions.SyncViewerShowPreferencesAction;
import org.eclipse.team.ui.mapping.SynchronizationActionProvider;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IKeyBindingService;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchSite;

/**
 * Action group that contributes the merge actions to the model
 * synchronize participant. The groups adds the following:
 * <ul>
 * <li>A toolbar action for attempting an auto-merge
 * <li>Context menu merge actions that delegate to the
 * model's merge action handlers.
 * <li>TODO a merge all and overwrite all menu item?
 * </ul>
 * <p>
 * Subclasses can configure the label and icons used for the merge actions
 * by overriding {@link #configureMergeAction(String, Action)} and can
 * configure where in the context menu the actions appear by overriding
 * {@link #addToContextMenu(String, Action, IMenuManager)}.
 *
 * @since 3.2
 **/
public class ModelSynchronizeParticipantActionGroup extends SynchronizePageActionGroup {

	/**
	 * The id of the merge action group that determines where the merge
	 * actions (e.g. merge and overwrite) appear in the context menu or toolbar.
	 */
	public static final String MERGE_ACTION_GROUP = "merge"; //$NON-NLS-1$

	/**
	 * The id of the action group that determines where the other
	 * actions (e.g. mark-as-merged) appear in the context menu.
	 */
	public static final String OTHER_ACTION_GROUP = "other"; //$NON-NLS-1$

	/**
	 * The id used to identify the Merge All action.
	 */
	protected static final String MERGE_ALL_ACTION_ID = "org.eclipse.team.ui.mergeAll"; //$NON-NLS-1$

	/**
	 * Create a merge action group.
	 */
	public ModelSynchronizeParticipantActionGroup() {
	}

	private MergeIncomingChangesAction updateToolbarAction;
	private ModelSelectionDropDownAction modelPicker;
	private SyncViewerShowPreferencesAction showPreferences;
	private OpenInCompareAction openInCompareAction;
	private MergeAction merge;
	private MergeAction overwrite;
	private MergeAction markAsMerged;

	@Override
	public void initialize(ISynchronizePageConfiguration configuration) {
		super.initialize(configuration);

		ModelSynchronizeParticipant participant = ((ModelSynchronizeParticipant)configuration.getParticipant());
		if (participant.isMergingEnabled()) {
			updateToolbarAction = new MergeIncomingChangesAction(configuration);
			configureMergeAction(MERGE_ALL_ACTION_ID, updateToolbarAction);
			appendToGroup(
					ISynchronizePageConfiguration.P_TOOLBAR_MENU,
					MERGE_ACTION_GROUP,
					updateToolbarAction);
			// TODO: Should add a merge all to the context menu as well?
		}
		modelPicker = new ModelSelectionDropDownAction(configuration);
		appendToGroup(
				ISynchronizePageConfiguration.P_TOOLBAR_MENU,
				ISynchronizePageConfiguration.NAVIGATE_GROUP,
				modelPicker);
		ISynchronizePageSite site = configuration.getSite();
		IWorkbenchSite ws = site.getWorkbenchSite();
		if (ws instanceof IViewSite) {
			showPreferences = new SyncViewerShowPreferencesAction(configuration);
			openInCompareAction = new OpenInCompareAction(configuration);
			configuration.setProperty(SynchronizePageConfiguration.P_OPEN_ACTION, new Action() {
				@Override
				public void run() {
					openInCompareAction.run();
				}
			});
		}
	}

	@Override
	public void fillActionBars(IActionBars actionBars) {
		super.fillActionBars(actionBars);
		if (actionBars != null && showPreferences != null) {
			IMenuManager menu = actionBars.getMenuManager();
			appendToGroup(menu, ISynchronizePageConfiguration.PREFERENCES_GROUP, showPreferences);
		}
	}

	@Override
	public void fillContextMenu(IMenuManager menu) {
		super.fillContextMenu(menu);
		if (menu instanceof CommonMenuManager) {
			CommonMenuManager cmm = (CommonMenuManager) menu;
			addMergeActions(cmm);
		}
		Object[] elements = ((IStructuredSelection)getContext().getSelection()).toArray();
		if (elements.length > 0 && openInCompareAction != null) {
			IContributionItem fileGroup = findGroup(menu, ISynchronizePageConfiguration.FILE_GROUP);
			if (fileGroup != null) {
				ModelSynchronizeParticipant msp = ((ModelSynchronizeParticipant)getConfiguration().getParticipant());
				boolean allElementsHaveCompareInput = true;
				for (Object element : elements) {
					if (!msp.hasCompareInputFor(element)) {
						allElementsHaveCompareInput = false;
						break;
					}
				}
				if (allElementsHaveCompareInput) {
					menu.appendToGroup(fileGroup.getId(), openInCompareAction);
				}
			}
		}
	}

	/*
	 * Method to add the merge actions to the context menu. This method
	 * is called by the internal synchronization framework and should not
	 * to be invoked by other clients. Subclasses can configure the
	 * merge actions by overriding {@link #configureMergeAction(String, Action)}
	 * and can control where in the context menu the action appears by
	 * overriding {@link #addToContextMenu(String, Action, IMenuManager)}.
	 * @param cmm the menu manager
	 */
	private void addMergeActions(CommonMenuManager cmm) {
		ModelSynchronizeParticipant participant = ((ModelSynchronizeParticipant)getConfiguration().getParticipant());
		if (participant.isMergingEnabled()) {
			if (!isTwoWayMerge()) {
				if (merge == null) {
					merge = new MergeAction(SynchronizationActionProvider.MERGE_ACTION_ID, cmm, getConfiguration());
					configureMergeAction(SynchronizationActionProvider.MERGE_ACTION_ID, merge);
					registerActionWithWorkbench(merge);

				}
				merge.update();
				addToContextMenu(SynchronizationActionProvider.MERGE_ACTION_ID, merge, cmm);
			}
			if (overwrite == null) {
				overwrite = new MergeAction(SynchronizationActionProvider.OVERWRITE_ACTION_ID, cmm, getConfiguration());
				configureMergeAction(SynchronizationActionProvider.OVERWRITE_ACTION_ID, overwrite);
				registerActionWithWorkbench(overwrite);
			}
			overwrite.update();
			addToContextMenu(SynchronizationActionProvider.OVERWRITE_ACTION_ID, overwrite, cmm);
			if (!isTwoWayMerge()) {
				if (markAsMerged == null) {
					markAsMerged = new MergeAction(SynchronizationActionProvider.MARK_AS_MERGE_ACTION_ID, cmm, getConfiguration());
					configureMergeAction(SynchronizationActionProvider.MARK_AS_MERGE_ACTION_ID, markAsMerged);
				}
				markAsMerged.update();
				addToContextMenu(SynchronizationActionProvider.MARK_AS_MERGE_ACTION_ID, markAsMerged, cmm);
				registerActionWithWorkbench(markAsMerged);
			}
		}
	}

	/**
	 * Register this action with the workbench so that it can participate in keybindings and
	 * retargetable actions.
	 *
	 * @param action the action to register
	 */
	private void registerActionWithWorkbench(IAction action) {
		ISynchronizePageSite site = getConfiguration().getSite();
		String id = action.getId();
		if (id != null) {
			site.getActionBars().setGlobalActionHandler(id, action);
			IKeyBindingService keyBindingService = site.getKeyBindingService();
			if(keyBindingService != null)
				keyBindingService.registerAction(action);
		}
	}

	/**
	 * Configure the merge action to have appropriate label, image, etc.
	 * Subclasses may override but should invoke the overridden
	 * method for unrecognized ids in order to support future additions.
	 * @param mergeActionId the id of the merge action (one of
	 * {@link SynchronizationActionProvider#MERGE_ACTION_ID},
	 * {@link SynchronizationActionProvider#OVERWRITE_ACTION_ID} or
	 * {@link SynchronizationActionProvider#MARK_AS_MERGE_ACTION_ID})
	 * @param action the action for the given id
	 */
	protected void configureMergeAction(String mergeActionId, Action action) {
		if (mergeActionId == null) {
			return;
		}

		switch (mergeActionId) {
		case SynchronizationActionProvider.MERGE_ACTION_ID:
			Utils.initAction(action, "action.merge."); //$NON-NLS-1$
			break;
		case SynchronizationActionProvider.OVERWRITE_ACTION_ID:
			if (isTwoWayMerge()) {
				Utils.initAction(action, "action.replace."); //$NON-NLS-1$
			} else {
				Utils.initAction(action, "action.overwrite."); //$NON-NLS-1$
			}
			break;
		case SynchronizationActionProvider.MARK_AS_MERGE_ACTION_ID:
			Utils.initAction(action, "action.markAsMerged."); //$NON-NLS-1$
			break;
		case MERGE_ALL_ACTION_ID:
			if (isTwoWayMerge()) {
				Utils.initAction(action, "action.replaceAll."); //$NON-NLS-1$
			} else {
				Utils.initAction(action, "action.mergeAll."); //$NON-NLS-1$
			}
			break;
		default:
			break;
		}
	}

	private boolean isTwoWayMerge() {
		ModelSynchronizeParticipant participant = ((ModelSynchronizeParticipant)getConfiguration().getParticipant());
		ISynchronizationContext context = participant.getContext();
		if (context instanceof IMergeContext) {
			IMergeContext mc = (IMergeContext) context;
			return (mc.getMergeType() == ISynchronizationContext.TWO_WAY);
		}
		return false;
	}

	/**
	 * Add the merge action to the context menu manager.
	 * Subclasses may override but should invoke the overridden
	 * method for unrecognized ids in order to support future additions.
	 * @param mergeActionId the id of the merge action (one of
	 * {@link SynchronizationActionProvider#MERGE_ACTION_ID},
	 * {@link SynchronizationActionProvider#OVERWRITE_ACTION_ID} or
	 * {@link SynchronizationActionProvider#MARK_AS_MERGE_ACTION_ID})
	 * @param action the action for the given id
	 * @param manager the context menu manager
	 */
	protected void addToContextMenu(String mergeActionId, Action action, IMenuManager manager) {
		IContributionItem group = null;
		if (mergeActionId != null) {
			switch (mergeActionId) {
			case SynchronizationActionProvider.MERGE_ACTION_ID:
				group = manager.find(MERGE_ACTION_GROUP);
				break;
			case SynchronizationActionProvider.OVERWRITE_ACTION_ID:
				group = manager.find(MERGE_ACTION_GROUP);
				break;
			case SynchronizationActionProvider.MARK_AS_MERGE_ACTION_ID:
				group = manager.find(OTHER_ACTION_GROUP);
				break;
			default:
				break;
			}
			if (group != null) {
				manager.appendToGroup(group.getId(), action);
			} else {
				manager.add(action);
			}
		}
	}

	@Override
	public void dispose() {
		if (modelPicker != null)
			modelPicker.dispose();
		if (merge != null)
			merge.dispose();
		if (overwrite != null)
			overwrite.dispose();
		if (markAsMerged != null)
			markAsMerged.dispose();
		if (updateToolbarAction != null)
			updateToolbarAction.dispose();
		super.dispose();
	}
}
