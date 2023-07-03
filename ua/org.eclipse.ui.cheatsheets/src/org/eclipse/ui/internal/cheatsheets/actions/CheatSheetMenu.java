/*******************************************************************************
 * Copyright (c) 2002, 2020 IBM Corporation and others.
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
package org.eclipse.ui.internal.cheatsheets.actions;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.eclipse.jface.action.ContributionItem;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.cheatsheets.OpenCheatSheetAction;
import org.eclipse.ui.internal.cheatsheets.CheatSheetHistory;
import org.eclipse.ui.internal.cheatsheets.CheatSheetPlugin;
import org.eclipse.ui.internal.cheatsheets.ICheatSheetResource;
import org.eclipse.ui.internal.cheatsheets.Messages;
import org.eclipse.ui.internal.cheatsheets.registry.CheatSheetCollectionElement;
import org.eclipse.ui.internal.cheatsheets.registry.CheatSheetElement;
import org.eclipse.ui.internal.cheatsheets.registry.CheatSheetRegistryReader;
import org.eclipse.ui.internal.cheatsheets.views.CheatSheetView;

/**
 * A menu for cheatsheet selection.
 * <p>
 * A <code>CheatSheetMenu</code> is used to populate a menu with
 * cheatsheet items.  If the user selects one of these items
 * an action is performed to launch the selected cheatsheet.
 * </p><p>
 * The visible cheatsheet items within the menu are dynamic and reflect the
 * available set. The available set consists of a limited combination of
 * the most recently used cheatsheet list and the currently available
 * cheatsheet.
 * </p>
 */
public class CheatSheetMenu extends ContributionItem {
	private static final int MAX_CHEATSHEET_ITEMS = 5;
	private static CheatSheetRegistryReader reg;

	private boolean showActive = false;

	private IMenuContributor menuContributor;

	private Comparator<CheatSheetElement> comparator = new Comparator<>() {
		private Collator collator = Collator.getInstance();

		@Override
		public int compare(CheatSheetElement ob1, CheatSheetElement ob2) {
			if(ob1 == null || ob2 == null) {
				return -1;
			}
			return collator.compare(ob1.getLabel(null), ob2.getLabel(null));
		}
	};

	/**
	 * Constructs a new instance of <code>CheatSheetMenu</code>.
	 */
	public CheatSheetMenu() {
		super("LaunchCheatSheetMenu"); //$NON-NLS-1$

		if (reg == null)
			reg = CheatSheetRegistryReader.getInstance();

		showActive(true);
	}

	/*
	 * Creates a menu item for a cheatsheet.
	 */
	private void createMenuItem(Menu menu, int index, final CheatSheetElement element, boolean bCheck) {

		MenuItem mi = new MenuItem(menu, bCheck ? SWT.RADIO : SWT.PUSH, index);
		mi.setText(element.getLabel(null));
		String key;
		if (element.isComposite()) {
			key = ICheatSheetResource.COMPOSITE_OBJ;
		} else {
			key = ICheatSheetResource.CHEATSHEET_OBJ;
		}
		mi.setImage(CheatSheetPlugin.getPlugin().getImageRegistry().get(key));
		mi.setSelection(bCheck);
		mi.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				run(element, e);
			}
		});
	}

	/*
	 * Creates a menu item for "Other...".
	 */
	private void createOtherItem(Menu menu, int index) {
		MenuItem mi = new MenuItem(menu, SWT.PUSH, index);
		mi.setText(Messages.CHEAT_SHEET_OTHER_MENU);
		mi.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				runOther(e);
			}
		});
	}

	@Override
	public void fill(Menu menu, int index) {
		// Get the checked cheatsheet.
		String checkID = null;
		if (showActive) {
			checkID = getActiveCheatSheetID();
		}

		// Collect and sort cheatsheet items.
		ArrayList<CheatSheetElement> cheatsheets = getCheatSheetItems();
		cheatsheets.sort(comparator);

		// Add cheatsheet shortcuts
		for (int i = 0; i < cheatsheets.size(); i++) {
			CheatSheetElement element = cheatsheets.get(i);
			if (element != null) {
				createMenuItem(menu, index++, element, element.getID().equals(checkID));
			}
		}

		// Add others item..
		if (cheatsheets.size() > 0) {
			new MenuItem(menu, SWT.SEPARATOR, index++);
		}
		createOtherItem(menu, index++);
		if (menuContributor != null) {
			menuContributor.contributeToViewMenu(menu, index);
		}
	}

	/**
	 * Method getActiveCheatSheetID returns the id of the active
	 * cheatsheet or null.
	 *
	 * @return String
	 */
	private String getActiveCheatSheetID() {
		//get the active cheatsheet view, if opened
		IWorkbenchPage page = getActiveWorkbenchPage();

		if( page != null ) {
			CheatSheetView view = (CheatSheetView) page.findView(ICheatSheetResource.CHEAT_SHEET_VIEW_ID);
			if (view != null) {
				CheatSheetElement content = view.getContent();
				if (content != null) {
					return content.getID();
				}
			}
		}

		return null;
	}

	/**
	 * Method getActiveWorkbenchPage returns the active
	 * workbench page or null.
	 *
	 * @return IWorkbenchPage
	 */
	private IWorkbenchPage getActiveWorkbenchPage() {
		IWorkbench workbench = PlatformUI.getWorkbench();
		IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();

		//get the active cheatsheet view, if opened
		return window.getActivePage();
	}

	/**
	 * Returns the available list of cheatsheets to display
	 * in the menu.
	 * <p>
	 * By default, the list contains the most recently used cheatsheets
	 * and then random cheatsheets until there are 5 present in the list.
	 * </p><p>
	 * Care should be taken to keep this list to a minimum (7 +/- 2 items
	 * is a good guideline to follow).
	 * </p>
	 *
	 * @return an <code>ArrayList</code> of cheatsheet items <code>CheatSheetElement</code>
	 */
	protected ArrayList<CheatSheetElement> getCheatSheetItems() {
		ArrayList<CheatSheetElement> list = new ArrayList<>(MAX_CHEATSHEET_ITEMS);
		int emptySlots = MAX_CHEATSHEET_ITEMS;

		// Add cheatsheets from MRU list
		if (emptySlots > 0) {
			ArrayList<CheatSheetElement> mru = new ArrayList<>(MAX_CHEATSHEET_ITEMS);
			int count = getCheatSheetMru(mru, 0, MAX_CHEATSHEET_ITEMS);
			for (int i = 0; i < count && emptySlots > 0; i++) {
				if (!list.contains(mru.get(i))) {
					list.add(mru.get(i));
					emptySlots--;
				}
			}
		}

		// Add random cheatsheets until the list is filled.
		CheatSheetCollectionElement cheatSheetsCollection = reg.getCheatSheets();
		emptySlots = addCheatSheets(list, cheatSheetsCollection, emptySlots);

		return list;
	}

	/**
	 * Method addCheatSheets fills a list with cheatsheet elements until there
	 * are no more empty slots.
	 *
	 * @param list - the list to file
	 * @param cheatSheetsCollection - the collection to get the elements from
	 * @param emptySlots - number of empty slots remaining
	 * @return int - number of empty slots remaining
	 */
	private int addCheatSheets(ArrayList<CheatSheetElement> list, CheatSheetCollectionElement cheatSheetsCollection,
			int emptySlots) {
		Object[] cheatSheets = cheatSheetsCollection.getCheatSheets();
		for (int i = 0; i < cheatSheets.length && emptySlots > 0; i++) {
			if (!list.contains(cheatSheets[i])) {
				list.add((CheatSheetElement) cheatSheets[i]);
				emptySlots--;
			}
		}

		Object[] cheatSheetsFromCollection = cheatSheetsCollection.getChildren();
		for (int nX = 0; nX < cheatSheetsFromCollection.length && emptySlots > 0; nX++) {
			CheatSheetCollectionElement collection = (CheatSheetCollectionElement) cheatSheetsFromCollection[nX];
			emptySlots = addCheatSheets(list, collection, emptySlots);
		}

		return emptySlots;
	}

	/*
	 * Gets the most recently used (MRU) shortcut cheatsheets
	 * (<code>CheatSheetElement</code> items)
	 * <p>
	 * The list is formed from the global cheatsheet history.
	 * </p>
	 * @param dest destination list to contain the items
	 * @param destStart index in destination list to start copying items at
	 * @param count number of items to copy from history
	 * @return the number of items actually copied
	 */
	private int getCheatSheetMru(List<CheatSheetElement> dest, int destStart, int count) {
		CheatSheetHistory history = CheatSheetPlugin.getPlugin().getCheatSheetHistory();
		return history.copyItems(dest, destStart, count);
	}

	/**
	 * Returns whether the menu item representing the active cheatsheet
	 * will have a check mark.
	 *
	 * @return <code>true</code> if a check mark is shown, <code>false</code> otherwise
	 */
	protected boolean getShowActive() {
		return showActive;
	}

	@Override
	public boolean isDynamic() {
		return true;
	}

	@Override
	public boolean isVisible() {
		return getActiveWorkbenchPage() != null;
	}

	/**
	 * Runs an action to launch the cheatsheet.
	 *
	 * @param element the selected cheatsheet
	 * @param event SelectionEvent - the event send along with the selection callback
	 */
	protected void run(CheatSheetElement element, SelectionEvent event) {
		new OpenCheatSheetAction(element.getID()).run();
	}

	/*
	 * Show the "other" dialog, select a cheatsheet, and launch it. Pass on the selection
	 * event should the meny need it.
	 */
	private void runOther(SelectionEvent event) {
		new CheatSheetCategoryBasedSelectionAction().run();
	}

	/**
	 * Sets the showActive flag.  If <code>showActive == true</code> then the
	 * active cheatsheet is highlighted with a check mark.
	 *
	 * @param b the new showActive flag
	 */
	protected void showActive(boolean b) {
		showActive = b;
	}

	/**
	 * Sets the menuContributor
	 * @param menuContributor an object which may add contributions to
	 * the menu.
	 */
	public void setMenuContributor(IMenuContributor menuContributor) {
		this.menuContributor = menuContributor;
	}

}
