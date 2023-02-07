/*******************************************************************************
 * Copyright (c) 2003, 2019 IBM Corporation and others.
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
package org.eclipse.ui.internal.cheatsheets.views;

import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.internal.cheatsheets.CheatSheetStopWatch;
import org.eclipse.ui.internal.cheatsheets.ICheatSheetResource;
import org.eclipse.ui.internal.cheatsheets.Messages;
import org.eclipse.ui.internal.cheatsheets.actions.CheatSheetMenu;
import org.eclipse.ui.internal.cheatsheets.registry.CheatSheetElement;
import org.eclipse.ui.internal.cheatsheets.registry.CheatSheetRegistryReader;
import org.eclipse.ui.part.ViewPart;

public class CheatSheetView extends ViewPart {

	private boolean actionBarContributed = false;
	private CheatSheetExpandRestoreAction expandRestoreAction;
	private Action copyAction;
	private CheatSheetViewer viewer;
	private IMemento memento;
	private static final String CHEAT_SHEET_VIEW_HELP_ID = "org.eclipse.ui.cheatsheets.cheatSheetView"; //$NON-NLS-1$

	private void contributeToActionBars() {
		IActionBars bars = getViewSite().getActionBars();
		IMenuManager menuManager = bars.getMenuManager();
		IToolBarManager tbmanager = bars.getToolBarManager();

		expandRestoreAction = new CheatSheetExpandRestoreAction(Messages.COLLAPSE_ALL_BUT_CURRENT_TOOLTIP, false, viewer);

		copyAction = new Action("copy") { //$NON-NLS-1$
			@Override
			public void run() {
				viewer.copy();
			}
		};
		copyAction.setEnabled(false);
		tbmanager.add(expandRestoreAction);
		bars.setGlobalActionHandler(ActionFactory.COPY.getId(), copyAction);

		viewer.setExpandRestoreAction(expandRestoreAction);
		viewer.setCopyAction(copyAction);

		CheatSheetMenu cheatsheetMenuMenuItem = new CheatSheetMenu();
		menuManager.add(cheatsheetMenuMenuItem);

		cheatsheetMenuMenuItem.setMenuContributor(viewer);
	}


	/**
	 * Creates the SWT controls for this workbench part.
	 * <p>
	 * Clients should not call this method (the workbench calls this method at
	 * appropriate times).
	 * </p>
	 * <p>
	 * For implementors this is a multi-step process:
	 * </p>
	 * <ol>
	 *   <li>Create one or more controls within the parent.</li>
	 *   <li>Set the parent layout as needed.</li>
	 *   <li>Register any global actions with the <code>IActionService</code>.</li>
	 *   <li>Register any popup menus with the <code>IActionService</code>.</li>
	 *   <li>Register a selection provider with the <code>ISelectionService</code>
	 *     (optional). </li>
	 * </ol>
	 *
	 * @param parent the parent control
	 */
	@Override
	public void createPartControl(Composite parent) {
		CheatSheetStopWatch.startStopWatch("CheatSheetView.createPartControl"); //$NON-NLS-1$

		viewer = new CheatSheetViewer(false);
		viewer.createPartControl(parent);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, CHEAT_SHEET_VIEW_HELP_ID);

		if (!actionBarContributed) {
			contributeToActionBars();
			actionBarContributed = true;
		}
		CheatSheetStopWatch.printLapTime("CheatSheetView.createPartControl", "Time in CheatSheetView.createPartControl() before restoreState: "); //$NON-NLS-1$ //$NON-NLS-2$
		if (memento != null) {
			restoreState(memento);
		}

		CheatSheetStopWatch.printTotalTime("CheatSheetView.createPartControl", "Time in CheatSheetView.createPartControl(): "); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Override
	public void dispose() {
		super.dispose();
	}

	public CheatSheetElement getContent() {
		if(viewer != null) {
			return viewer.getContent();
		}
		return null;
	}

	public String getCheatSheetID() {
		if(viewer != null) {
			return viewer.getCheatSheetID();
		}
		return null;
	}

	/*
	 * Returns the CheatSheetViewer contained in this view.
	 */
	public CheatSheetViewer getCheatSheetViewer() {
		return viewer;
	}

	@Override
	public void init(IViewSite site, IMemento memento) throws PartInitException {
		init(site);
		this.memento = memento;
	}

	/**
	 * Restore the view state
	 */
	private void restoreState(IMemento memento) {
		IMemento contentMemento = memento.getChild(ICheatSheetResource.MEMENTO);
		if (contentMemento != null) {
			String id = contentMemento.getString(ICheatSheetResource.MEMENTO_ID);
			String name = contentMemento.getString(ICheatSheetResource.MEMENTO_NAME);

			// Using an if/else if here because at a point in time there was a different
			// attribute used. As a result an if/else could cause setInput(null) to be
			// invoked but this would throw an IllegalArgumentException.
			if(name != null) {
				try {
				URL fileURL = new URL(contentMemento.getString(ICheatSheetResource.MEMENTO_URL));
				setInput(id, name, fileURL);
				} catch (MalformedURLException mue) {
				}
			} else if (id != null) {
				setInput(id);
			}

		}
	}

	@Override
	public void saveState(IMemento memento) {
		if(viewer != null) {
			CheatSheetElement element = viewer.getContent();

			if(element == null) {
				// Currently no cheat sheet is being displayed so just return
				return;
			}

			IMemento contentMemento = memento.createChild(ICheatSheetResource.MEMENTO);

			CheatSheetElement tempElement = CheatSheetRegistryReader.getInstance().findCheatSheet(element.getID());
			if(tempElement != null) {
				contentMemento.putString(ICheatSheetResource.MEMENTO_ID, element.getID());
			} else {
				contentMemento.putString(ICheatSheetResource.MEMENTO_ID, element.getID());
				contentMemento.putString(ICheatSheetResource.MEMENTO_NAME, element.getLabel(null));
				contentMemento.putString(ICheatSheetResource.MEMENTO_URL, element.getHref());
			}

			// Make sure the current cheat sheet is saved
			viewer.saveCurrentSheet();
		}
	}

	/**
	 * Passing the focus request to the viewer's control.
	 */
	@Override
	public void setFocus() {
		if(viewer != null) {
			viewer.setFocus();
		}
	}

	public void setInput(String id) {
		CheatSheetStopWatch.startStopWatch("CheatSheetView.setInput"); //$NON-NLS-1$

		if(viewer != null) {
			viewer.setInput(id);
		}

		CheatSheetStopWatch.printTotalTime("CheatSheetView.setInput", "Time in CheatSheetView.setInput(String id): "); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public void setInput(String id, String name, URL url) {
		if(viewer != null) {
			viewer.setInput(id, name, url);
		}
	}

	public void setInputFromXml(String id, String name, String xml, String basePath) {
		if(viewer != null) {
			viewer.setInputFromXml(id, name, xml, basePath);
		}
	}
}
