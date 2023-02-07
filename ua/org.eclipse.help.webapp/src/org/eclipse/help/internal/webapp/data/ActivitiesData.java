/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
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
package org.eclipse.help.internal.webapp.data;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.help.internal.base.HelpBasePlugin;
/**
 * Helper for pages in navigation frames. Used enabling/disabling activity
 * filtering
 */
public class ActivitiesData extends RequestData {
	/**
	 * Constructs the data for a request.
	 *
	 * @param context
	 * @param request
	 */
	public ActivitiesData(ServletContext context, HttpServletRequest request,
			HttpServletResponse response) {
		super(context, request, response);
		String changeShowAll = request.getParameter("showAll"); //$NON-NLS-1$
		if (changeShowAll != null) {
			if ("off".equalsIgnoreCase(changeShowAll)) { //$NON-NLS-1$
				HelpBasePlugin.getActivitySupport().setFilteringEnabled(true);
			} else if ("on".equalsIgnoreCase(changeShowAll)) { //$NON-NLS-1$
				HelpBasePlugin.getActivitySupport().setFilteringEnabled(false);
			} else {
				// not supported value
			}
		} else {
			// no change to afilter
		}
		String confirmShowAll = request.getParameter("showconfirm"); //$NON-NLS-1$
		if ("false".equalsIgnoreCase(confirmShowAll)) { //$NON-NLS-1$
			preferences.setDontConfirmShowAll(true);
		}
	}
	/**
	 * @return Checks if filtering is enabled.
	 */
	public boolean isActivityFiltering() {
		return HelpBasePlugin.getActivitySupport().isFilteringEnabled();
	}
	/**
	 * Gives state of show all topics button
	 *
	 * @return "hidden", "off", or "on"
	 */
	public String getButtonState() {
		if (!HelpBasePlugin.getActivitySupport().isUserCanToggleFiltering())
			return "hidden"; //$NON-NLS-1$
		else if (HelpBasePlugin.getActivitySupport().isFilteringEnabled())
			return "off"; //$NON-NLS-1$
		else
			return "on"; //$NON-NLS-1$
	}
}
