/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ccvs.ui.repo;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.action.*;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.internal.ccvs.ui.CVSUIMessages;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.actions.ActionGroup;

public class RepositoriesSortingActionGroup extends ActionGroup {
	// Sorting actions
	private Action labelSortingAction;
	private Action locationSortingAction;
	private Action hostSortingAction;
	/**
	 * Action for switching between ascending and descending sorting order
	 */
	private Action reverseSortingOrderAction;

	/**
	 * Current comparator set.
	 */
	private RepositoryComparator comparator;
	private IPropertyChangeListener comparatorUpdater;

	/**
	 * Indicates if comparator was changed
	 */
	public static final String CHANGE_COMPARATOR = "changeComparator"; //$NON-NLS-1$

	private static final String REPOSITORIES_SORTING_ACTION_GROUP = "repositoriesSortingActionGroup"; //$NON-NLS-1$

	public RepositoriesSortingActionGroup(Shell shell,
			IPropertyChangeListener comparatorUpdater) {
		Assert.isNotNull(shell);

		this.comparatorUpdater = comparatorUpdater;
		labelSortingAction = new Action(
				CVSUIMessages.RepositoriesSortingActionGroup_label,
				Action.AS_RADIO_BUTTON) {
			@Override
			public void run() {
				if (labelSortingAction.isChecked())
					setComparatorOrder(RepositoryComparator.ORDER_BY_LABEL);
			}
		};
		locationSortingAction = new Action(
				CVSUIMessages.RepositoriesSortingActionGroup_location,
				Action.AS_RADIO_BUTTON) {
			@Override
			public void run() {
				if (locationSortingAction.isChecked())
					setComparatorOrder(RepositoryComparator.ORDER_BY_LOCATION);
			}
		};
		hostSortingAction = new Action(
				CVSUIMessages.RepositoriesSortingActionGroup_host,
				Action.AS_RADIO_BUTTON) {
			@Override
			public void run() {
				if (hostSortingAction.isChecked())
					setComparatorOrder(RepositoryComparator.ORDER_BY_HOST);
			}
		};
		reverseSortingOrderAction = new Action(
				CVSUIMessages.RepositoriesSortingActionGroup_descending,
				Action.AS_CHECK_BOX) {
			@Override
			public void run() {
				reverseSortingOrder();
			}
		};
	}

	/* package */void setComparatorOrder(int orderBy) {
		comparator.setOrder(orderBy);
		firePropertyChange(comparator);
	}

	private void reverseSortingOrder() {
		this.comparator.setAscending(!comparator.isAscending());
		firePropertyChange(comparator);
	}

	private void firePropertyChange(RepositoryComparator newComparator) {
		// Update viewer
		if (comparatorUpdater != null) {
			comparatorUpdater
					.propertyChange(new PropertyChangeEvent(this,
							CHANGE_COMPARATOR, null /* oldComparator */,
							newComparator));
		}
	}

	@Override
	public void fillActionBars(IActionBars actionBars) {
		IMenuManager menuManager = actionBars.getMenuManager();

		if (menuManager.find(IWorkbenchActionConstants.MB_ADDITIONS) != null)
			menuManager.insertAfter(IWorkbenchActionConstants.MB_ADDITIONS,
					new Separator(REPOSITORIES_SORTING_ACTION_GROUP));
		else
			menuManager.add(new Separator(REPOSITORIES_SORTING_ACTION_GROUP));

		IMenuManager sortSubmenu = new MenuManager(
				CVSUIMessages.RepositoriesSortingActionGroup_sortBy);
		menuManager.appendToGroup(REPOSITORIES_SORTING_ACTION_GROUP,
				sortSubmenu);
		sortSubmenu.add(labelSortingAction);
		sortSubmenu.add(locationSortingAction);
		sortSubmenu.add(hostSortingAction);
		sortSubmenu.add(new Separator());
		sortSubmenu.add(reverseSortingOrderAction);
	}

	public void setSelectedComparator(RepositoryComparator selectedComparator) {
		comparator = selectedComparator;

		labelSortingAction
				.setChecked(selectedComparator.getOrderBy() == RepositoryComparator.ORDER_BY_LABEL);
		locationSortingAction
				.setChecked(selectedComparator.getOrderBy() == RepositoryComparator.ORDER_BY_LOCATION);
		hostSortingAction
				.setChecked(selectedComparator.getOrderBy() == RepositoryComparator.ORDER_BY_HOST);

		reverseSortingOrderAction.setChecked(!selectedComparator.isAscending());

		firePropertyChange(comparator);
	}
}
