/*******************************************************************************
 * Copyright (c) 2006, 2016 IBM Corporation and others.
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
package org.eclipse.help.ui.internal.dynamic;

import org.eclipse.help.internal.dynamic.FilterResolver.Extension;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.activities.IActivityManager;
import org.eclipse.ui.activities.IWorkbenchActivitySupport;
import org.eclipse.ui.activities.WorkbenchActivityHelper;

/*
 * An extension to the FilterResolver to handle UI-related filters, namely
 * activities and activity categories.
 */
public class FilterResolverExtension implements Extension {

	private static final String NAME_ACTIVITY = "activity"; //$NON-NLS-1$
	private static final String NAME_CATEGORY = "category"; //$NON-NLS-1$

	@Override
	public boolean isHandled(String name) {
		return (name.equals(NAME_ACTIVITY) || name.equals(NAME_CATEGORY));
	}

	@Override
	public boolean isFiltered(String name, String value) {
		if (name.equals(NAME_ACTIVITY)) {
			return filterByActivity(value);
		}
		else if (name.equals(NAME_CATEGORY)) {
			return filterByCategory(value);
		}
		return false;
	}

	/*
	 * Evaluates the "category" filter.
	 */
	private static boolean filterByCategory(String categoryId) {
		try {
			IWorkbenchActivitySupport workbenchActivitySupport = PlatformUI.getWorkbench().getActivitySupport();
			IActivityManager activityManager = workbenchActivitySupport.getActivityManager();
			if (activityManager.getCategory(categoryId).isDefined()) {
				return !WorkbenchActivityHelper.isEnabled(activityManager, categoryId);
			}
			return true;
		}
		catch (Exception e) {
			// no workbench available (standalone mode)
			return false;
		}
	}

	/*
	 * Evaluates the "activity" filter.
	 */
	private static boolean filterByActivity(String activityId) {
		try {
			IWorkbenchActivitySupport workbenchActivitySupport = PlatformUI.getWorkbench().getActivitySupport();
			IActivityManager activityManager = workbenchActivitySupport.getActivityManager();
			if (activityManager.getActivity(activityId).isDefined()) {
				return !activityManager.getActivity(activityId).isEnabled();
			}
			return true;
		}
		catch (Exception e) {
			// no workbench available (standalone mode)
			return false;
		}
	}
}
