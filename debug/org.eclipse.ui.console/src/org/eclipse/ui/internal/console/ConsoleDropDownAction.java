/*******************************************************************************
 * Copyright (c) 2000, 2013 IBM Corporation and others.
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
package org.eclipse.ui.internal.console;


import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleConstants;
import org.eclipse.ui.console.IConsoleListener;
import org.eclipse.ui.console.IConsoleView;
import org.eclipse.ui.progress.UIJob;
import org.eclipse.ui.texteditor.IUpdate;

/**
 * Drop down action in the console to select the console to display.
 */
class ConsoleDropDownAction extends Action implements IMenuCreator, IConsoleListener, IUpdate {

	private IConsoleView fView;
	private Menu fMenu;

	@Override
	public void update() {
		IConsole[] consoles = ConsolePlugin.getDefault().getConsoleManager().getConsoles();
		setEnabled(consoles.length > 1);
	}

	public ConsoleDropDownAction(IConsoleView view) {
		super(ConsoleMessages.ConsoleDropDownAction_0, AS_DROP_DOWN_MENU);
		fView= view;
		setToolTipText(ConsoleMessages.ConsoleDropDownAction_1);
		setImageDescriptor(ConsolePluginImages.getImageDescriptor(IConsoleConstants.IMG_VIEW_CONSOLE));
		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IConsoleHelpContextIds.CONSOLE_DISPLAY_CONSOLE_ACTION);
		ConsolePlugin.getDefault().getConsoleManager().addConsoleListener(this);
		update();
	}

	@Override
	public void dispose() {
		if (fMenu != null) {
			fMenu.dispose();
		}

		fView= null;
		ConsolePlugin.getDefault().getConsoleManager().removeConsoleListener(this);
	}

	@Override
	public Menu getMenu(Menu parent) {
		return null;
	}

	@Override
	public Menu getMenu(Control parent) {
		if (fMenu != null) {
			fMenu.dispose();
		}

		fMenu= new Menu(parent);
		IConsole[] consoles= ConsolePlugin.getDefault().getConsoleManager().getConsoles();
		IConsole current = fView.getConsole();
		for (int i = 0; i < consoles.length; i++) {
			IConsole console = consoles[i];
			Action action = new ShowConsoleAction(fView, console);
			action.setChecked(console.equals(current));
			addActionToMenu(fMenu, action, i + 1);
		}
		return fMenu;
	}

	private void addActionToMenu(Menu parent, Action action, int accelerator) {
		if (accelerator < 10) {
			StringBuilder label= new StringBuilder();
			//add the numerical accelerator
			label.append('&');
			label.append(accelerator);
			label.append(' ');
			label.append(action.getText());
			action.setText(label.toString());
		}
		ActionContributionItem item= new ActionContributionItem(action);
		item.fill(parent, -1);
	}

	@Override
	public void run() {
		ConsoleView consoleView = (ConsoleView) fView;
		boolean pinned = consoleView.isPinned();
		try {
			if (pinned) {
				consoleView.setPinned(false);
			}
			IConsole[] consoles = ConsolePlugin.getDefault().getConsoleManager().getConsoles();
			IConsole current = fView.getConsole();
			int idx = 0;
			for (int i = 0; i < consoles.length; i++) {
				idx = i;
				if(consoles[i] == current) {
					break;
				}
			}
			int next = idx+1;
			if(next >= consoles.length) {
				next = 0;
			}
			fView.display(consoles[next]);
		}
		finally {
			if (pinned) {
				consoleView.setPinned(true);
			}
		}
	}

	@Override
	public void runWithEvent(Event event) {
		// Show menu on drop-down button, run action otherwise
		if (event.detail == SWT.ARROW && event.widget instanceof ToolItem) {
			ToolItem toolItem = (ToolItem) event.widget;
			Control control = toolItem.getParent();
			Menu menu = getMenu(control);

			Rectangle bounds = toolItem.getBounds();
			Point topLeft = new Point(bounds.x, bounds.y + bounds.height);
			menu.setLocation(control.toDisplay(topLeft));
			menu.setVisible(true);
		} else {
			run();
		}
	}

	@Override
	public void consolesAdded(IConsole[] consoles) {
		UIJob job = new UIJob(ConsoleMessages.UpdatingConsoleState) {
			@Override
			public IStatus runInUIThread(IProgressMonitor monitor) {
				update();
				return Status.OK_STATUS;
			}
		};
		job.setSystem(true);
		job.setPriority(Job.INTERACTIVE);
		job.schedule();
	}

	/*
	 * Dispose the menu when a launch is removed, such that the actions in this menu
	 * do not hang on to associated resources.
	 */
	@Override
	public void consolesRemoved(IConsole[] consoles) {
		UIJob job = new UIJob(ConsoleMessages.UpdatingConsoleState) {
			@Override
			public IStatus runInUIThread(IProgressMonitor monitor) {
				if (fMenu != null) {
					fMenu.dispose();
				}
				update();
				return Status.OK_STATUS;
			}
		};
		job.setSystem(true);
		job.setPriority(Job.INTERACTIVE);
		job.schedule();
	}
}
