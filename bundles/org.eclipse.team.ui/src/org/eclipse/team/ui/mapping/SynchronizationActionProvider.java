/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.ui.mapping;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.core.commands.IHandler;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.team.core.mapping.ISynchronizationContext;
import org.eclipse.team.internal.ui.mapping.CommonMenuManager;
import org.eclipse.team.internal.ui.synchronize.actions.OpenWithActionGroup;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.ui.*;
import org.eclipse.ui.actions.ActionContext;
import org.eclipse.ui.navigator.*;

/**
 * An action group that can be used by models to contribute actions
 * to a team synchronization viewer. Subclasses should override the
 * {@link #initialize()} method in order to register handlers for the
 * following merge actions if they want custom merge behavior:
 * <ul>
 * <li>{@link #MERGE_ACTION_ID}
 * <li>{@link #OVERWRITE_ACTION_ID}
 * <li>{@link #OVERWRITE_ACTION_ID}
 * </ul>
 * They may also add other actions to the context menu or register action handlers
 * in the {@link #fillActionBars(IActionBars)} method.
 * <p>
 * This class may be subclasses by clients.
 *
 * @see MergeActionHandler
 * @since 3.2
 */
public class SynchronizationActionProvider extends CommonActionProvider {

	/**
	 * Action id constant for the merge action.
	 * @see #registerHandler(String, IHandler)
	 */
	public static final String MERGE_ACTION_ID = "org.eclipse.team.ui.mergeAction"; //$NON-NLS-1$

	/**
	 * Action id constant for the merge action.
	 * @see #registerHandler(String, IHandler)
	 */
	public static final String OVERWRITE_ACTION_ID = "org.eclipse.team.ui.overwriteAction"; //$NON-NLS-1$

	/**
	 * Action id constant for the mark-as-merge action.
	 * @see #registerHandler(String, IHandler)
	 */
	public static final String MARK_AS_MERGE_ACTION_ID = "org.eclipse.team.ui.markAsMergeAction"; //$NON-NLS-1$

	private Map handlers = new HashMap();
	private OpenWithActionGroup openWithActions;

	@Override
	public void init(ICommonActionExtensionSite site) {
		super.init(site);
		initialize();
	}

	/**
	 * Method called during action provider initialization.
	 * It is invoked from the {@link #init(ICommonActionExtensionSite)}
	 * after after the configuration has been recorded. Subclasses
	 * may override. Subclasses that want to provide there own merge actions
	 * handlers can register them in this method.
	 * @see #registerHandler(String, IHandler)
	 * @see MergeActionHandler
	 */
	protected void initialize() {
		initializeOpenActions();
	}

	/**
	 * Method called from {@link #initialize()} to initialize the Open/Open With
	 * actions. This method will add an Open item and Open With menu for single
	 * selections that adapt to IResource. Subclasses may override. They may
	 * still call this method, in which case they only need to handle providing
	 * open for non-files. Otherwise, if they do not call this method, they must
	 * provide all non-compare related open items.
	 *
	 */
	protected void initializeOpenActions() {
		ICommonViewerSite cvs = getActionSite().getViewSite();
		ISynchronizePageConfiguration configuration = getSynchronizePageConfiguration();
		if (cvs instanceof ICommonViewerWorkbenchSite && configuration != null) {
			ICommonViewerWorkbenchSite cvws = (ICommonViewerWorkbenchSite) cvs;
			final IWorkbenchPartSite wps = cvws.getSite();
			if (wps instanceof IViewSite) {
				openWithActions = new OpenWithActionGroup(configuration, false);
			}
		}
	}

	/**
	 * Return the configuration from the synchronize page that contains
	 * the common viewer.
	 * @return the configuration from the synchronize page that contains
	 * the common viewer
	 */
	protected final ISynchronizePageConfiguration getSynchronizePageConfiguration() {
		return (ISynchronizePageConfiguration)getExtensionStateModel().getProperty(ITeamContentProviderManager.P_SYNCHRONIZATION_PAGE_CONFIGURATION);
	}

	/**
	 * Return the extension state model for the content provider associated with
	 * action provider.
	 * @return the extension state model for the content provider associated with
	 * action provider
	 */
	protected final IExtensionStateModel getExtensionStateModel() {
		return getActionSite().getExtensionStateModel();
	}

	/**
	 * Return the synchronization context to which the actions of this provider
	 * apply.
	 * @return the synchronization context to which the actions of this provider
	 * apply
	 */
	protected final ISynchronizationContext getSynchronizationContext() {
		return (ISynchronizationContext)getExtensionStateModel().getProperty(ITeamContentProviderManager.P_SYNCHRONIZATION_CONTEXT);
	}

	/**
	 * Register the handler as the handler for the given action id when
	 * a merge action is performed on elements that match this groups
	 * enablement.
	 * @param actionId the id of the merge action
	 * @param handler the handler for elements of the model that provided this group
	 */
	protected void registerHandler(String actionId, IHandler handler) {
		handlers.put(actionId, handler);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.actions.ActionGroup#fillContextMenu(org.eclipse.jface.action.IMenuManager)
	 */
	@Override
	public void fillContextMenu(IMenuManager menu) {
		super.fillContextMenu(menu);
		if (menu instanceof CommonMenuManager) {
			CommonMenuManager manager = (CommonMenuManager) menu;
			for (Iterator iter = handlers.keySet().iterator(); iter.hasNext();) {
				String actionId = (String) iter.next();
				manager.registerHandler(actionId, (IHandler)handlers.get(actionId));
			}
		}
        final IContributionItem fileGroup = menu.find(ISynchronizePageConfiguration.FILE_GROUP);
		if (openWithActions != null && fileGroup != null) {
			openWithActions.fillContextMenu(menu, fileGroup.getId());
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.actions.ActionGroup#fillActionBars(org.eclipse.ui.IActionBars)
	 */
	@Override
	public void fillActionBars(IActionBars actionBars) {
		super.fillActionBars(actionBars);
		if (openWithActions != null) openWithActions.fillActionBars(actionBars);
	}

	@Override
	public void updateActionBars() {
		super.updateActionBars();
		if (openWithActions != null) openWithActions.updateActionBars();
	}

	@Override
	public void setContext(ActionContext context) {
		super.setContext(context);
		if (openWithActions != null) openWithActions.setContext(context);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.actions.ActionGroup#dispose()
	 */
	@Override
	public void dispose() {
		super.dispose();
		if (openWithActions != null) openWithActions.dispose();
		for (Iterator iter = handlers.values().iterator(); iter.hasNext();) {
			IHandler handler = (IHandler) iter.next();
			if (handler instanceof MergeActionHandler) {
				MergeActionHandler mah = (MergeActionHandler) handler;
				mah.dispose();
			}
		}
	}

	/**
	 * Return the extension site for this action provider.
	 * This method just calls {@link #getActionSite()}.
	 * @return the extension site for this action provider
	 */
	public ICommonActionExtensionSite getExtensionSite() {
		return getActionSite();
	}

}
