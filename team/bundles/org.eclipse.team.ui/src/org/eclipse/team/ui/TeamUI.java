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
package org.eclipse.team.ui;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.team.internal.ui.history.GenericHistoryView;
import org.eclipse.team.internal.ui.registry.TeamContentProviderManager;
import org.eclipse.team.ui.history.IHistoryPageSource;
import org.eclipse.team.ui.history.IHistoryView;
import org.eclipse.team.ui.mapping.ITeamContentProviderManager;
import org.eclipse.team.ui.synchronize.ISynchronizeManager;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;

/**
 * TeamUI contains public API for generic UI-based Team functionality.
 * <p>
 * This class is not intended to be subclassed or instantiated by clients
 */
public class TeamUI {

	/**
	 * Property constant indicating the global ignores list has changed.
	 */
	public static final String GLOBAL_IGNORES_CHANGED = TeamUIPlugin.ID + "global_ignores_changed"; //$NON-NLS-1$

	/**
	 * Property constant indicating the global file types list has changed.
	 * @since 3.1
	 */
	public static final String GLOBAL_FILE_TYPES_CHANGED = TeamUIPlugin.ID + "global_file_types_changed"; //$NON-NLS-1$

	/**
	 * Return the synchronize manager.
	 *
	 * @return the synchronize manager
	 * @since 3.0
	 */
	public static ISynchronizeManager getSynchronizeManager() {
		return TeamUIPlugin.getPlugin().getSynchronizeManager();
	}

	/**
	 * Register for changes made to Team properties.
	 *
	 * @param listener the listener to add
	 */
	public static void addPropertyChangeListener(IPropertyChangeListener listener) {
		TeamUIPlugin.addPropertyChangeListener(listener);
	}

	/**
	 * Remove the listener from Team property change listener list.
	 *
	 * @param listener the listener to remove
	 */
	public static void removePropertyChangeListener(IPropertyChangeListener listener) {
		TeamUIPlugin.removePropertyChangeListener(listener);
	}

	/**
	 * Shows the history view and returns a handle to it. Note that in the case of many
	 * history views, the main history view is the one returned here.
	 *
	 * @return an IHistoryView which is the main history view if it is found or null if it can't be found
	 * @since 3.2
	 */
	public static IHistoryView getHistoryView() {
		try {
			TeamUIPlugin.getActivePage().showView(IHistoryView.VIEW_ID);
			return (IHistoryView) TeamUIPlugin.getActivePage().findView(IHistoryView.VIEW_ID);
		} catch (PartInitException e) {
		}

		return null;
	}

	/**
	 * Shows a history view containing the given input and returns a handle to the view
	 * or <code>null</code> if no history was available for the given input. If an appropriate
	 * instance of a history view is already opened, its input will be changed and the view will
	 * be activated. Otherwise a new view will be opened.
	 * @param page the workbench page containing the history view
	 * @param input the input whose history is to be displayed
	 * @param pageSource the history page source from which to obtain the page or <code>null</code>
	 * 	if the page source should be determined using the Adapter manager.
	 *
	 * @return an IHistoryView which is the main history view if it is found or null if it can't be found
	 * @since 3.3
	 */
	public static IHistoryView showHistoryFor(IWorkbenchPage page, Object input, IHistoryPageSource pageSource) {
		try {
			IHistoryView view = (IHistoryView) page.findView(IHistoryView.VIEW_ID);
			if (view == null) {
				page.showView(IHistoryView.VIEW_ID);
				view = (IHistoryView) TeamUIPlugin.getActivePage().findView(IHistoryView.VIEW_ID);
				if(view != null) {
					return showInputInView(page, input, view, pageSource);
				}
			}
			view = ((GenericHistoryView)view).findAppropriateHistoryViewFor(input, pageSource);
			if (view == null) {
				view = (IHistoryView) page.showView(IHistoryView.VIEW_ID, IHistoryView.VIEW_ID + System.currentTimeMillis(), IWorkbenchPage.VIEW_CREATE);
				return showInputInView(page, input, view, pageSource);
			} else {
				return showInputInView(page, input, view, pageSource);
			}
		} catch (PartInitException e) {
		}

		return null;
	}

	private static IHistoryView showInputInView(IWorkbenchPage page,
			Object input, IHistoryView view, IHistoryPageSource pageSource) {
		page.activate((IWorkbenchPart)view);
		((GenericHistoryView)view).showHistoryPageFor(input, true, false, pageSource);
		return view;
	}

	/**
	 * Return the team content provider manager which gives access to the team
	 * content providers registered with the
	 * <code>org.eclipse.team.ui.teamContentProviders</code> extension point.
	 *
	 * @return the team content provider manager
	 * @since 3.2
	 */
	public static ITeamContentProviderManager getTeamContentProviderManager() {
		return TeamContentProviderManager.getInstance();
	}

	/**
	 * Return a set of wizard pages for the given importer ID. If no wizard page
	 * is registered for the importer then a page will not be created. If an
	 * extension exits, a page will be created, but it's up to the caller to
	 * initialize the page with a set of corresponding descriptions.
	 *
	 * <strong>EXPERIMENTAL</strong>. This class has been added as part of a
	 * work in progress. There is no guarantee that this API will work or that
	 * it will remain the same. Please do not use this API without consulting
	 * with the Team team.
	 *
	 * @param importerId
	 *            the importer ID
	 * @return wizard pages
	 * @throws CoreException
	 *             if an error occurs while trying to create a page extension
	 * @since 3.6
	 * @noreference This method is not intended to be referenced by clients.
	 */
	public static IScmUrlImportWizardPage[] getPages(final String importerId)
			throws CoreException {
		IConfigurationElement[] elements = Platform.getExtensionRegistry()
				.getConfigurationElementsFor(TeamUIPlugin.PLUGIN_ID,
						IScmUrlImportWizardPage.ATT_EXTENSION);
		if (elements.length > 0) {
			Set<IScmUrlImportWizardPage> pages = new HashSet<>();
			for (IConfigurationElement element : elements) {
				String pageImporterId = element.getAttribute(IScmUrlImportWizardPage.ATT_IMPORTER);
				if (importerId.equals(pageImporterId)) {
					Object ext = TeamUIPlugin.createExtension(element, IScmUrlImportWizardPage.ATT_PAGE);
					IScmUrlImportWizardPage page = (IScmUrlImportWizardPage) ext;
					pages.add(page);
				}
			}
			return pages.toArray(new IScmUrlImportWizardPage[pages.size()]);
		}
		return null;
	}
}
