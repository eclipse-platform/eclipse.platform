/*******************************************************************************
 *  Copyright (c) 2004, 2015 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ui.cheatsheets;

import java.net.URL;

import org.eclipse.help.ui.internal.views.HelpTray;
import org.eclipse.help.ui.internal.views.IHelpPartPage;
import org.eclipse.help.ui.internal.views.ReusableHelpPart;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.TrayDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.internal.cheatsheets.registry.CheatSheetElement;
import org.eclipse.ui.internal.cheatsheets.registry.CheatSheetRegistryReader;
import org.eclipse.ui.internal.cheatsheets.state.DefaultStateManager;
import org.eclipse.ui.internal.cheatsheets.views.CheatSheetHelpPart;
import org.eclipse.ui.internal.cheatsheets.views.CheatSheetView;
import org.eclipse.ui.internal.cheatsheets.views.ViewUtilities;

/**
 * Action for opening a cheat sheet. The cheat sheet can be specified
 * either by a cheat sheet id or by a URL of a cheat sheet content file.
 *
 * @since 3.0
 * @noextend This class is not intended to be subclassed by clients.
 */
public final class OpenCheatSheetAction extends Action {
	private String id;
	private String name;
	private URL url;
	private String xml;
	private String basePath;
	private Shell targetShell;

	/**
	 * Creates an action that opens the cheat sheet with the given id.
	 * The cheat sheet content file is located via the
	 * <code>org.eclipse.ui.cheatsheets.cheatSheetContent</code>
	 * extension point.
	 *
	 * @param id the cheat sheet id
	 * @exception IllegalArgumentException if <code>id</code>
	 * is <code>null</code>
	 */
	public OpenCheatSheetAction(String id) {
		if (id == null) {
			throw new IllegalArgumentException();
		}
		this.id = id;
	}

	/**
	 * Creates an action that opens the cheat sheet with the
	 * given cheat sheet content file.
	 *
	 * @param id the id to give this cheat sheet
	 * @param name the name to give this cheat sheet
	 * @param url URL of the cheat sheet content file
	 * @exception IllegalArgumentException if the parameters
	 * are <code>null</code>
	 */
	public OpenCheatSheetAction(String id, String name, URL url) {
		if (id == null || name == null || url == null) {
			throw new IllegalArgumentException();
		}
		this.id = id;
		this.name = name;
		this.url = url;
	}

	/**
	 * Creates an action that opens a cheat sheet using
	 * XML passed in as a string.
	 *
	 * @param id the id to give this cheat sheet
	 * @param name the name to give this cheat sheet
	 * @param xml the cheatsheet content in xml format
	 * @param baseURL is a URL which is only required if the cheat sheet is
	 * a composite cheat sheet which has tasks which use path parameters in which
	 * case the paths will be relative to baseURL. May be <code>null</code>
	 * if this is not a composite cheat sheet
	 * @exception IllegalArgumentException if the parameters
	 * are <code>null</code>
	 * @since 3.3
	 */
	public OpenCheatSheetAction(String id, String name, String xml, URL baseURL) {
		if (id == null || name == null || xml == null) {
			throw new IllegalArgumentException();
		}
		this.id = id;
		this.name = name;
		this.xml = xml;
		if (baseURL !=null) {
			basePath = baseURL.toExternalForm();
		}
	}


	/*
	 * This action will try to launch the cheat sheet view and populate
	 * it with the content specified either in the URL or the content
	 * file specified in the cheatsheetContent extension point
	 * for the cheat sheet with the id passed to this action.
	 */
	@Override
	public void run() {
		Shell shell = getShell();
		// are we in a dialog that can show a cheat sheet?
		if (shell != null && !shell.isFocusControl() && shell.getData() instanceof TrayDialog) {
			TrayDialog dialog = (TrayDialog)shell.getData();
			HelpTray tray = (HelpTray)dialog.getTray();
			if (tray == null) {
				tray = new HelpTray();
				dialog.openTray(tray);
			}
			ReusableHelpPart helpPart = tray.getHelpPart();
			IHelpPartPage page = helpPart.createPage(CheatSheetHelpPart.ID, null, null);
			page.setVerticalSpacing(0);
			page.setHorizontalMargin(0);
			CheatSheetElement contentElement = CheatSheetRegistryReader.getInstance().findCheatSheet(id);
			helpPart.addPart(CheatSheetHelpPart.ID, new CheatSheetHelpPart(helpPart.getForm().getForm().getBody(), helpPart.getForm().getToolkit(), page.getToolBarManager(), contentElement, new DefaultStateManager()));
			page.addPart(CheatSheetHelpPart.ID, true);
			helpPart.addPage(page);
			helpPart.showPage(CheatSheetHelpPart.ID);
		}
		else {
			CheatSheetView view = ViewUtilities.showCheatSheetView();
			if (view == null) {
				return;
			}
			// Depending on which constructor was used open the cheat sheet view from a
			// URL, an XML string or based on the id
			if(url != null) {
				view.setInput(id, name, url);
			} else if (xml != null) {
				view.setInputFromXml(id, name, xml, basePath);
			} else {
				view.setInput(id);
			}
			IWorkbenchPage page = view.getSite().getWorkbenchWindow().getActivePage();
			page.bringToTop(view);
		}
	}

	/**
	 * Sets the shell in which the cheat sheet is opened. If this is not set,
	 * the active shell of the default display is used.
	 *
	 * @param shell
	 *            The shell in which the cheat sheet is opened.
	 * @since 3.7
	 */
	public void setTargetShell(Shell shell) {
		this.targetShell = shell;
	}

	private Shell getShell() {
		Shell shell = targetShell;
		if (shell == null) {
			shell = Display.getDefault().getActiveShell();
		}
		return shell;
	}
}
