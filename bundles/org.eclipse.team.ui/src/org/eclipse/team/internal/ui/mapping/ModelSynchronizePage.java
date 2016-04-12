/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ui.mapping;

import org.eclipse.core.resources.mapping.IModelProviderDescriptor;
import org.eclipse.core.resources.mapping.ModelProvider;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.team.core.mapping.ISynchronizationContext;
import org.eclipse.team.internal.ui.*;
import org.eclipse.team.internal.ui.synchronize.*;
import org.eclipse.team.internal.ui.synchronize.actions.RefreshActionContribution;
import org.eclipse.team.ui.mapping.ITeamContentProviderManager;
import org.eclipse.team.ui.synchronize.*;

/**
 * A synchronize page for displaying a {@link ModelSynchronizeParticipant}.
 *
 * @since 3.2
 **/
public class ModelSynchronizePage extends AbstractSynchronizePage {

	private ModelSynchronizeParticipant participant;

	/**
	 * Create a page from the given configuration
	 * @param configuration a page configuration
	 */
	public ModelSynchronizePage(ISynchronizePageConfiguration configuration) {
		super(configuration);
		this.participant = (ModelSynchronizeParticipant)configuration.getParticipant();
		configuration.setComparisonType(isThreeWay()
						? ISynchronizePageConfiguration.THREE_WAY
						: ISynchronizePageConfiguration.TWO_WAY);
		configuration.addActionContribution(new RefreshActionContribution());
	}

	private boolean isThreeWay() {
		return getParticipant().getContext().getType() == ISynchronizationContext.THREE_WAY;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ui.synchronize.AbstractSynchronizePage#reset()
	 */
	@Override
	public void reset() {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ui.synchronize.AbstractSynchronizePage#updateMode(int)
	 */
	@Override
	protected void updateMode(int mode) {
		// Nothing to do
	}

	/**
	 * Return the participant of this page.
	 * @return the participant of this page
	 */
	protected ModelSynchronizeParticipant getParticipant() {
		return participant;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ui.synchronize.AbstractSynchronizePage#createViewerAdvisor(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected AbstractViewerAdvisor createViewerAdvisor(Composite parent) {
		CommonViewerAdvisor commonViewerAdvisor = new CommonViewerAdvisor(parent, getConfiguration());
		commonViewerAdvisor.addEmptyTreeListener((DiffTreeChangesSection)getChangesSection());
		updateMode(getConfiguration().getMode());
		return commonViewerAdvisor;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ui.synchronize.AbstractSynchronizePage#createChangesSection()
	 */
	@Override
	protected ChangesSection createChangesSection(Composite parent) {
		return new DiffTreeChangesSection(parent, this, getConfiguration());
	}

	@Override
	public void init(ISynchronizePageSite site) {
		super.init(site);
		IDialogSettings pageSettings = site.getPageSettings();
		if(pageSettings != null) {
			String savedId = pageSettings.get(ModelSynchronizeParticipant.P_VISIBLE_MODEL_PROVIDER);
			if (savedId != null && ! savedId.equals(ModelSynchronizeParticipant.ALL_MODEL_PROVIDERS_VISIBLE)) {
				getConfiguration().setProperty(ModelSynchronizeParticipant.P_VISIBLE_MODEL_PROVIDER, savedId);
			}
			String layout = pageSettings.get(ITeamContentProviderManager.PROP_PAGE_LAYOUT);
			if (layout != null) {
				getConfiguration().setProperty(ITeamContentProviderManager.PROP_PAGE_LAYOUT, layout);
			}
		}
	}

	@Override
	public boolean aboutToChangeProperty(ISynchronizePageConfiguration configuration, String key, Object newValue) {
		if (key.equals(ModelSynchronizeParticipant.P_VISIBLE_MODEL_PROVIDER)) {
			if (!(newValue instanceof String)) {
				return false;
			}
			String currentSetting = (String)configuration.getProperty(ModelSynchronizeParticipant.P_VISIBLE_MODEL_PROVIDER);
			if (currentSetting != null && currentSetting.equals(newValue))
				return false;

			Object input = getViewerInput(configuration,(String) newValue);
			if (input instanceof ModelProvider) {
				ModelProvider provider = (ModelProvider) input;
				configuration.setProperty(
						ISynchronizePageConfiguration.P_PAGE_DESCRIPTION,
						NLS.bind(TeamUIMessages.ShowModelProviderAction_0, new String[] {Utils.getLabel(provider), Utils.shortenText(SynchronizeView.MAX_NAME_LENGTH, configuration.getParticipant().getName()) }));
			} else if (input != null) {
				configuration.setProperty(
						ISynchronizePageConfiguration.P_PAGE_DESCRIPTION,
						Utils.shortenText(SynchronizeView.MAX_NAME_LENGTH, configuration.getParticipant().getName()));
			}
			if (input != null) {
				IDialogSettings pageSettings = configuration.getSite().getPageSettings();
				if(pageSettings != null) {
					pageSettings.put(ModelSynchronizeParticipant.P_VISIBLE_MODEL_PROVIDER, (String) newValue);
				}
				return true;
			}
			return false;
		}
		if (key.equals(ITeamContentProviderManager.PROP_PAGE_LAYOUT)) {
			if (!(newValue instanceof String)) {
				return false;
			}
			String currentSetting = (String)configuration.getProperty(ITeamContentProviderManager.PROP_PAGE_LAYOUT);
			if (currentSetting != null && currentSetting.equals(newValue))
				return false;

			IDialogSettings pageSettings = configuration.getSite().getPageSettings();
			if(pageSettings != null) {
				pageSettings.put(ITeamContentProviderManager.PROP_PAGE_LAYOUT, (String) newValue);
			}
			return true;
		}
		return super.aboutToChangeProperty(configuration, key, newValue);
	}

	/**
	 * Return the input for the viewer.
	 * @param configuration the page configuration
	 * @param providerId the provider id or ModelSynchronizeParticipant.ALL_MODEL_PROVIDERS_VISIBLE
	 * @return the input for the viewer.
	 */
	public static Object getViewerInput(ISynchronizePageConfiguration configuration, String providerId) {
		Object input = null;
		if (!providerId.equals(ModelSynchronizeParticipant.ALL_MODEL_PROVIDERS_VISIBLE)) {
			ModelProvider provider = getModelProvider(providerId);
			if (provider != null) {
				input = provider;
			}
		} else {
			input = (ISynchronizationContext)configuration.getProperty(ITeamContentProviderManager.P_SYNCHRONIZATION_CONTEXT);
		}
		return input;
	}

	private static ModelProvider getModelProvider(String id) {
		try {
			IModelProviderDescriptor desc = ModelProvider.getModelProviderDescriptor((String)id);
			if (desc != null) {
				return desc.getModelProvider();
			}
		} catch (CoreException e) {
			TeamUIPlugin.log(e);
		}
		return null;
	}

}
