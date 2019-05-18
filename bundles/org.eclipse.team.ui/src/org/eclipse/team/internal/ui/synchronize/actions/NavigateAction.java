/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
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
package org.eclipse.team.internal.ui.synchronize.actions;

import org.eclipse.compare.ICompareNavigator;
import org.eclipse.jface.action.Action;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.internal.ui.synchronize.SynchronizePageConfiguration;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.actions.ActionFactory;

/**
 * Action to navigate the changes shown in the Synchronize View. This
 * will coordinate change browsing between the view and the compare
 * editors.
 *
 * @since 3.0
 */
public class NavigateAction extends Action {
	private final boolean next;
	private ISynchronizePageConfiguration configuration;

	public NavigateAction(ISynchronizePageConfiguration configuration, boolean next) {
		this.configuration = configuration;
		this.next = next;
		IActionBars bars = configuration.getSite().getActionBars();
		if (next) {
			Utils.initAction(this, "action.navigateNext."); //$NON-NLS-1$
			setActionDefinitionId(ActionFactory.NEXT.getCommandId());
			if (bars != null)
				bars.setGlobalActionHandler(ActionFactory.NEXT.getId(), this);
		} else {
			Utils.initAction(this, "action.navigatePrevious."); //$NON-NLS-1$
			setActionDefinitionId(ActionFactory.PREVIOUS.getCommandId());
			if (bars != null)
				bars.setGlobalActionHandler(ActionFactory.PREVIOUS.getId(), this);
		}
	}

	/**
	 * Two types of navigation is supported: navigation that is specific to coordinating between a view
	 * and a compare editor and navigation simply using the configured navigator.
	 */
	@Override
	public void run() {
		ICompareNavigator nav = (ICompareNavigator)configuration.getProperty(SynchronizePageConfiguration.P_NAVIGATOR);
		nav.selectChange(next);
	}
}
