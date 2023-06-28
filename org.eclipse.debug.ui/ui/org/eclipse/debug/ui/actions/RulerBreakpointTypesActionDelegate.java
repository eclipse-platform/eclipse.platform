/*******************************************************************************
 * Copyright (c) 2005, 2013 IBM Corporation and others.
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
 *     Wind River Systems - adapted action to use for breakpoint types
 ******************************************************************************/
package org.eclipse.debug.ui.actions;

import java.util.Set;

import org.eclipse.debug.internal.ui.actions.ToggleBreakpointsTargetManager;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.text.source.IVerticalRulerInfo;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.events.MenuAdapter;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.ITextEditorExtension;

/**
 * Breakpoint ruler pop-up action that creates a sub-menu to select the
 * currently active breakpoint type. This action delegate can be contributed to
 * an editor with the <code>editorActions</code> extension point. The breakpoint
 * types are calculated based on the toggle breakpoint target factories
 * contributed through the <code>toggleBreakpointsTargetFactories</code>
 * extension point.
 * <p>
 * Following is example plug-in XML used to contribute this action to an
 * editor's vertical ruler context menu. It uses the <code>popupMenus</code>
 * extension point, by referencing the ruler's context menu identifier in the
 * <code>targetID</code> attribute.
 * </p>
 *
 * <pre>
 * &lt;extension point="org.eclipse.ui.popupMenus"&gt;
 *   &lt;viewerContribution
 *     targetID="example.rulerContextMenuId"
 *     id="example.RulerPopupActions"&gt;
 *       &lt;action
 *         label="Toggle Breakpoint"
 *         class="org.eclipse.debug.ui.actions.RulerBreakpointTypesActionDelegate"
 *         menubarPath="additions"
 *         id="example.rulerContextMenu.breakpointTypesAction"&gt;
 *       &lt;/action&gt;
 *   &lt;/viewerContribution&gt;
 * </pre>
 * <p>
 * Clients may refer to this class as an action delegate in plug-in XML. This
 * class is not intended to be subclassed.
 * </p>
 *
 * @see IToggleBreakpointsTargetManager
 * @see IToggleBreakpointsTargetFactory
 * @noextend This class is not intended to be subclassed by clients.
 * @since 3.5
 *
 * @deprecated Should use BreakpointTypesContribution instead.
 */
@Deprecated
public class RulerBreakpointTypesActionDelegate implements IEditorActionDelegate, IMenuListener, IMenuCreator {
	private ITextEditor fEditor = null;
	private IAction fCallerAction = null;
	private IVerticalRulerInfo fRulerInfo;
	private ISelection fSelection;

	/**
	 * The menu created by this action
	 */
	private Menu fMenu;

	private class SelectTargetAction extends Action {
		private final Set<String> fPossibleIDs;
		private final String fID;

		SelectTargetAction(String name, Set<String> possibleIDs, String ID) {
			super(name, AS_RADIO_BUTTON);
			fID = ID;
			fPossibleIDs = possibleIDs;
		}

		@Override
		public void run() {
			if (isChecked()) {
				ToggleBreakpointsTargetManager.getDefault().setPreferredTarget(fPossibleIDs, fID);
			}
		}
	}


	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		// In the editor we're not using the selection.
	}

	@Override
	public void run(IAction action) {
		// Do nothing, this is a pull-down menu.
	}

	@Override
	public void setActiveEditor(IAction callerAction, IEditorPart targetEditor) {
		// Clean up old editor data.
		if (fCallerAction != null) {
			fCallerAction.setMenuCreator(null);
		}
		if (fEditor instanceof ITextEditorExtension) {
			((ITextEditorExtension) fEditor).removeRulerContextMenuListener(this);
		}
		fRulerInfo = null;

		// Set up new editor data.
		fCallerAction = callerAction;
		fCallerAction.setMenuCreator(this);

		fEditor= targetEditor == null ? null : targetEditor.getAdapter(ITextEditor.class);

		if (fEditor != null) {
			if (fEditor instanceof ITextEditorExtension) {
				((ITextEditorExtension) fEditor).addRulerContextMenuListener(this);
			}

			fRulerInfo= fEditor.getAdapter(IVerticalRulerInfo.class);
		}

	}

	@Override
	public void dispose() {
		if (fCallerAction != null) {
			fCallerAction.setMenuCreator(null);
		}
		if (fEditor instanceof ITextEditorExtension) {
			((ITextEditorExtension) fEditor).removeRulerContextMenuListener(this);
		}
		fRulerInfo = null;
	}

	@Override
	public void menuAboutToShow(IMenuManager manager) {
		fSelection = StructuredSelection.EMPTY;
		if (fEditor != null && fRulerInfo != null) {

			IDocumentProvider provider = fEditor.getDocumentProvider();
			if (provider != null) {
				IDocument document =  provider.getDocument(fEditor.getEditorInput());
				int line = fRulerInfo.getLineOfLastMouseButtonActivity();
				if (line > -1) {
					try {
						IRegion region = document.getLineInformation(line);
						fSelection = new TextSelection(document, region.getOffset(), 0);
					} catch (BadLocationException e) {}
				}
			}
			ToggleBreakpointsTargetManager toggleTargetManager = ToggleBreakpointsTargetManager.getDefault();
			Set<String> enabledIDs = toggleTargetManager.getEnabledToggleBreakpointsTargetIDs(fEditor, fSelection);
			fCallerAction.setEnabled(enabledIDs.size() > 0);
		} else {
			fCallerAction.setEnabled(false);
		}

	}

	/**
	 * Sets this action's drop-down menu, disposing the previous menu.
	 *
	 * @param menu the new menu
	 */
	private void setMenu(Menu menu) {
		if (fMenu != null) {
			fMenu.dispose();
		}
		fMenu = menu;
	}

	@Override
	public Menu getMenu(Menu parent) {
		setMenu(new Menu(parent));
		fillMenu(fMenu);
		initMenu();
		return fMenu;
	}

	@Override
	public Menu getMenu(Control parent) {
		setMenu(new Menu(parent));
		fillMenu(fMenu);
		initMenu();
		return fMenu;
	}

	/**
	 * Fills the drop-down menu with enabled toggle breakpoint targets
	 *
	 * @param menu the menu to fill
	 */
	private void fillMenu(Menu menu) {
		ToggleBreakpointsTargetManager manager = ToggleBreakpointsTargetManager.getDefault();
		Set<String> enabledIDs = manager.getEnabledToggleBreakpointsTargetIDs(fEditor, fSelection);
		String preferredId = manager.getPreferredToggleBreakpointsTargetID(fEditor, fSelection);
		for (String id : enabledIDs) {
			SelectTargetAction action= new SelectTargetAction(manager.getToggleBreakpointsTargetName(id), enabledIDs, id);
			if (id.equals(preferredId)){
				action.setChecked(true);
			}
			ActionContributionItem item= new ActionContributionItem(action);
			item.fill(menu, -1);
		}
	}

	/**
	 * Creates the menu for the action
	 */
	private void initMenu() {
		// Add listener to re-populate the menu each time
		// it is shown because of dynamic history list
		fMenu.addMenuListener(new MenuAdapter() {
			@Override
			public void menuShown(MenuEvent e) {
				Menu m = (Menu)e.widget;
				for (MenuItem item : m.getItems()) {
					item.dispose();
				}
				fillMenu(m);
			}
		});
	}

}
