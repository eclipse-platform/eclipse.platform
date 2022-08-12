/*******************************************************************************
 * Copyright (c) 2009, 2017 IBM Corporation and others.
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
package org.eclipse.team.internal.ui.synchronize.patch;

import org.eclipse.compare.structuremergeviewer.ICompareInput;
import org.eclipse.core.resources.mapping.ModelProvider;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.team.core.mapping.provider.SynchronizationContext;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.internal.ui.mapping.FuzzFactorAction;
import org.eclipse.team.internal.ui.mapping.GererateRejFileAction;
import org.eclipse.team.internal.ui.mapping.IgnoreLeadingPathSegmentsAction;
import org.eclipse.team.internal.ui.mapping.ReversePatchAction;
import org.eclipse.team.internal.ui.synchronize.IRefreshable;
import org.eclipse.team.ui.TeamUI;
import org.eclipse.team.ui.mapping.ISynchronizationCompareAdapter;
import org.eclipse.team.ui.mapping.SynchronizationActionProvider;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.team.ui.synchronize.ISynchronizeParticipantDescriptor;
import org.eclipse.team.ui.synchronize.ModelSynchronizeParticipant;
import org.eclipse.team.ui.synchronize.ModelSynchronizeParticipantActionGroup;
import org.eclipse.ui.IActionBars;

public class ApplyPatchModelSynchronizeParticipant extends
		ModelSynchronizeParticipant {

	public static final String ID = "org.eclipse.team.ui.applyPatchModelParticipant"; //$NON-NLS-1$

	public ApplyPatchModelSynchronizeParticipant(SynchronizationContext context) {
		super(context);
		init();
	}

	private void init() {
		try {
			ISynchronizeParticipantDescriptor descriptor = TeamUI
					.getSynchronizeManager().getParticipantDescriptor(ID);
			setInitializationData(descriptor);
			setSecondaryId(Long.toString(System.currentTimeMillis()));
		} catch (CoreException e) {
			// ignore
		}
	}

	@Override
	protected void initializeConfiguration(
			final ISynchronizePageConfiguration configuration) {
		super.initializeConfiguration(configuration);
		configuration
				.setSupportedModes(ISynchronizePageConfiguration.INCOMING_MODE
						| ISynchronizePageConfiguration.CONFLICTING_MODE);
		configuration.setMode(ISynchronizePageConfiguration.INCOMING_MODE);
	}

	@Override
	protected ModelSynchronizeParticipantActionGroup createMergeActionGroup() {
		return new ApplyPatchModelSynchronizeParticipantActionGroup();
	}

	public class ApplyPatchModelSynchronizeParticipantActionGroup extends
			ModelSynchronizeParticipantActionGroup {
		@Override
		protected void addToContextMenu(String mergeActionId, Action action,
				IMenuManager manager) {
			if (mergeActionId == SynchronizationActionProvider.OVERWRITE_ACTION_ID) {
				// omit this action
				return;
			}
			super.addToContextMenu(mergeActionId, action, manager);
		}

		@Override
		public void fillActionBars(IActionBars actionBars) {
			if (actionBars != null) {
				IMenuManager menu = actionBars.getMenuManager();
				ReversePatchAction reversePatchAction = new ReversePatchAction(
						getConfiguration());
				appendToGroup(menu,
						ISynchronizePageConfiguration.PREFERENCES_GROUP,
						reversePatchAction);
				Utils.initAction(reversePatchAction, "action.reversePatch."); //$NON-NLS-1$
				FuzzFactorAction fuzzFactor = new FuzzFactorAction(
						getConfiguration());
				appendToGroup(menu,
						ISynchronizePageConfiguration.PREFERENCES_GROUP,
						fuzzFactor);
				Utils.initAction(fuzzFactor, "action.fuzzFactor."); //$NON-NLS-1$
				IgnoreLeadingPathSegmentsAction ignoreAction = new IgnoreLeadingPathSegmentsAction(
						getConfiguration());
				appendToGroup(menu,
						ISynchronizePageConfiguration.PREFERENCES_GROUP,
						ignoreAction);
				Utils.initAction(ignoreAction,
						"action.ignoreLeadingPathSegments."); //$NON-NLS-1$
				GererateRejFileAction generateAction = new GererateRejFileAction(
						getConfiguration());
				appendToGroup(menu,
						ISynchronizePageConfiguration.PREFERENCES_GROUP,
						generateAction);
				Utils.initAction(generateAction, "action.generateRejFile."); //$NON-NLS-1$
				appendToGroup(menu,
						ISynchronizePageConfiguration.PREFERENCES_GROUP,
						new Separator());
			}
			super.fillActionBars(actionBars);
		}
	}

	@Override
	public ModelProvider[] getEnabledModelProviders() {
		ModelProvider[] enabledProviders = super.getEnabledModelProviders();
		// add Patch model provider if it's not there
		for (ModelProvider provider : enabledProviders) {
			if (provider.getId().equals(PatchModelProvider.ID))
				return enabledProviders;
		}
		ModelProvider[] extended = new ModelProvider[enabledProviders.length + 1];
		System.arraycopy(enabledProviders, 0, extended, 0, enabledProviders.length);
		PatchModelProvider provider = PatchModelProvider.getProvider();
		if (provider == null)
			return enabledProviders;
		extended[extended.length - 1] = provider;
		return extended;
	}

	@Override
	public ICompareInput asCompareInput(Object object) {
		// consult adapter first
		ISynchronizationCompareAdapter adapter = Utils
				.getCompareAdapter(object);
		if (adapter != null)
			return adapter.asCompareInput(getContext(), object);
		if (object instanceof ICompareInput) {
			return (ICompareInput) object;
		}
		return null;
	}

	@Override
	public <T> T getAdapter(Class<T> adapter) {
		if (adapter == IRefreshable.class) {
			return null;
		}
		return super.getAdapter(adapter);
	}
}
