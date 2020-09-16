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
package org.eclipse.team.internal.ui.synchronize;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.team.internal.ui.Policy;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.internal.ui.synchronize.actions.DirectionFilterActionGroup;
import org.eclipse.team.ui.synchronize.ISynchronizePage;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.team.ui.synchronize.ISynchronizePageSite;
import org.eclipse.team.ui.synchronize.SynchronizePageActionGroup;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.part.IShowInSource;
import org.eclipse.ui.part.IShowInTargetList;
import org.eclipse.ui.part.Page;
import org.eclipse.ui.part.ShowInContext;

/**
 * Abstract synchronize page that can filter changes by mode (incoming, outgoing,
 * both or conflicting). It also uses forms to indicate when a model is empty and
 * provide a link to a non-empty mode.
 */
public abstract class AbstractSynchronizePage extends Page implements ISynchronizePage, IAdaptable {

	private ISynchronizePageConfiguration configuration;
	private ISynchronizePageSite site;

	// Parent composite of this view. It is remembered so that we can dispose of its children when
	// the viewer type is switched.
	private Composite composite;
	private ChangesSection changesSection;
	private Viewer changesViewer;

	private AbstractViewerAdvisor viewerAdvisor;

	/*
	 * Contribute actions for changing modes to the page.
	 */
	class ModeFilterActions extends SynchronizePageActionGroup {
		private DirectionFilterActionGroup modes;
		@Override
		public void initialize(ISynchronizePageConfiguration configuration) {
			super.initialize(configuration);
			if (isThreeWay()) {
				modes = new DirectionFilterActionGroup(configuration);
			}
		}
		@Override
		public void fillActionBars(IActionBars actionBars) {
			super.fillActionBars(actionBars);
			if (modes == null) return;
			IToolBarManager manager = actionBars.getToolBarManager();
			IContributionItem group = findGroup(manager, ISynchronizePageConfiguration.MODE_GROUP);
			if (manager != null && group != null) {
				modes.fillToolBar(group.getId(), manager);
			}
			IMenuManager viewMenu = actionBars.getMenuManager();
			group = findGroup(manager, ISynchronizePageConfiguration.MODE_GROUP);
			if (viewMenu != null && group != null) {
				IContributionItem layoutGroup = findGroup(manager, ISynchronizePageConfiguration.LAYOUT_GROUP);
				if (layoutGroup != null) {
					// Put the modes in the layout group to save space
					group = layoutGroup;
				}
				MenuManager modesItem = new MenuManager(Utils.getString("action.modes.label", Policy.getActionBundle())); //$NON-NLS-1$
				viewMenu.appendToGroup(group.getId(), modesItem);
				modes.fillMenu(modesItem);
			}
		}
		private boolean isThreeWay() {
			return ISynchronizePageConfiguration.THREE_WAY.equals(configuration.getComparisonType());
		}
	}

	/**
	 * Create a new instance of the page
	 * @param configuration a synchronize page configuration
	 */
	protected AbstractSynchronizePage(ISynchronizePageConfiguration configuration) {
		this.configuration = configuration;
		configuration.setPage(this);
		configuration.addActionContribution(new ModeFilterActions());
	}

	@Override
	public void createControl(Composite parent) {
		composite = new Composite(parent, SWT.NONE);
		//sc.setContent(composite);
		GridLayout gridLayout= new GridLayout();
		gridLayout.makeColumnsEqualWidth= false;
		gridLayout.marginWidth= 0;
		gridLayout.marginHeight = 0;
		gridLayout.verticalSpacing = 0;
		composite.setLayout(gridLayout);
		GridData data = new GridData(GridData.FILL_BOTH);
		data.grabExcessVerticalSpace = true;
		composite.setLayoutData(data);

		// Create the changes section which, in turn, creates the changes viewer and its configuration
		this.changesSection = createChangesSection(composite);
		createChangesViewer(changesSection.getContainer());
		changesSection.setViewer(changesViewer);
	}

	/**
	 * Create the changes section that will contain the changes viewer.
	 * @return the changes section that will contain the changes viewer
	 */
	protected abstract ChangesSection createChangesSection(Composite parent);

	/**
	 * Return the viewer that will display the changes associated
	 * with the page.
	 *
	 * @param parent the parent of the viewer
	 */
	private void createChangesViewer(Composite parent) {
		viewerAdvisor = createViewerAdvisor(parent);
		changesViewer = viewerAdvisor.getViewer();
		viewerAdvisor.setInitialInput();
	}

	protected abstract AbstractViewerAdvisor createViewerAdvisor(Composite parent);

	public AbstractViewerAdvisor getViewerAdvisor() {
		return viewerAdvisor;
	}

	@Override
	public void setActionBars(IActionBars actionBars) {
		// Delegate menu creation to the advisor
		viewerAdvisor.setActionBars(actionBars);
	}

	@Override
	public Control getControl() {
		return composite;
	}

	@Override
	public void setFocus() {
		changesSection.setFocus();
	}

	@Override
	public void init(ISynchronizePageSite site) {
		this.site = site;
		IDialogSettings settings = getSettings();
		if (settings != null) {
			try {
				int mode = settings.getInt(ISynchronizePageConfiguration.P_MODE);
				if (mode != 0) {
					configuration.setMode(mode);
				}
			} catch (NumberFormatException e) {
				// The mode settings does not exist.
				// Leave the mode as is (assuming the
				// participant initialized it to an
				// appropriate value
			}
		}
	}

	@Override
	public void dispose() {
		changesSection.dispose();
		composite.dispose();
		super.dispose();
	}

	@Override
	public Viewer getViewer() {
		return changesViewer;
	}

	@Override
	public boolean aboutToChangeProperty(
			ISynchronizePageConfiguration configuration, String key,
			Object newValue) {
		if (key.equals(ISynchronizePageConfiguration.P_MODE)) {
			return (internalSetMode(configuration.getMode(), ((Integer)newValue).intValue()));
		}
		return true;
	}

	private boolean internalSetMode(int oldMode, int mode) {
		if(oldMode == mode) return false;
		updateMode(mode);
		IDialogSettings settings = getSettings();
		if (settings != null) {
			settings.put(ISynchronizePageConfiguration.P_MODE, mode);
		}
		return true;
	}

	/*
	 * This method enables "Show In" support for this view
	 *
	 * @see org.eclipse.core.runtime.IAdaptable#getAdapter(java.lang.Class)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public <T> T getAdapter(Class<T> key) {
		if (key.equals(ISelectionProvider.class))
			return (T) changesViewer;
		if (key == IShowInSource.class) {
			return (T) (IShowInSource) () -> {
				StructuredViewer v = (StructuredViewer)changesViewer;
				if (v == null) return null;
				ISelection s = v.getSelection();
				if (s instanceof IStructuredSelection) {
					Object[] resources = Utils.getResources(((IStructuredSelection)s).toArray());
					return new ShowInContext(null, new StructuredSelection(resources));
				}
				return null;
			};
		}
		if (key == IShowInTargetList.class) {
			return (T) (IShowInTargetList) () -> new String[] { IPageLayout.ID_PROJECT_EXPLORER };
		}
		return null;
	}

	/**
	 * Return the page site that was assigned to this page.
	 * @return the page site that was assigned to this page
	 */
	public ISynchronizePageSite getSynchronizePageSite() {
		return site;
	}

	/**
	 * Return the synchronize page configuration that was used to create
	 * this page.
	 * @return Returns the configuration.
	 */
	public ISynchronizePageConfiguration getConfiguration() {
		return configuration;
	}

	/**
	 * Return the settings for the page from the configuration
	 * os <code>null</code> if settings can not be persisted
	 * for the page
	 * @return the persisted page settings
	 */
	protected IDialogSettings getSettings() {
		return configuration.getSite().getPageSettings();
	}

	/**
	 * Callback from the changes section that indicates that the
	 * user has chosen to reset the view contents after an error
	 * has occurred
	 */
	public abstract void reset();

	/**
	 * Change the mode to the given mode. This method is invoked
	 * when the mode in the configuration is changed by a client.
	 * @param mode the mode to be used
	 */
	protected abstract void updateMode(int mode);

	public ChangesSection getChangesSection() {
		return changesSection;
	}
}
