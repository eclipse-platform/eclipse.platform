/*******************************************************************************
 * Copyright (c) 2000, 2016 IBM Corporation and others.
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

import java.util.Arrays;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.activities.WorkbenchActivityHelper;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsoleFactory;

/**
 * @since 3.1
 */
public class OpenConsoleAction extends Action implements IMenuCreator {

	private ConsoleFactoryExtension[] fFactoryExtensions;
	private Menu fMenu;

	public OpenConsoleAction() {
		super(ConsoleMessages.OpenConsoleAction_0, AS_DROP_DOWN_MENU);
		fFactoryExtensions = getSortedFactories();
		setToolTipText(ConsoleMessages.OpenConsoleAction_1);
		setImageDescriptor(ConsolePluginImages.getImageDescriptor(IInternalConsoleConstants.IMG_ELCL_NEW_CON));
		setDisabledImageDescriptor(ConsolePluginImages.getImageDescriptor(IInternalConsoleConstants.IMG_DLCL_NEW_CON));
		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IConsoleHelpContextIds.CONSOLE_OPEN_CONSOLE_ACTION);
	}

	private ConsoleFactoryExtension[] getSortedFactories() {
		ConsoleFactoryExtension[] factoryExtensions = ((ConsoleManager) ConsolePlugin.getDefault().getConsoleManager()).getConsoleFactoryExtensions();
		Arrays.sort(factoryExtensions, (e1, e2) -> {
			if (e1.isNewConsoleExtenson()) {
				return -1;
			}
			if (e2.isNewConsoleExtenson()) {
				return 1;
			}
			String first = e1.getLabel();
			String second = e2.getLabel();
			return first.compareTo(second);
		});
		return factoryExtensions;
	}

	@Override
	public void dispose() {
		fFactoryExtensions = null;
		if (fMenu != null) {
			fMenu.dispose();
			fMenu = null;
		}
	}

	@Override
	public void runWithEvent(Event event) {
		if (event.widget instanceof ToolItem) {
			ToolItem toolItem= (ToolItem) event.widget;
			Control control= toolItem.getParent();
			Menu menu= getMenu(control);

			Rectangle bounds= toolItem.getBounds();
			Point topLeft= new Point(bounds.x, bounds.y + bounds.height);
			menu.setLocation(control.toDisplay(topLeft));
			menu.setVisible(true);
		}
	}

	@Override
	public Menu getMenu(Control parent) {
		if (fMenu != null) {
			fMenu.dispose();
		}

		fMenu= new Menu(parent);
		int accel = 1;
		for (ConsoleFactoryExtension extension : fFactoryExtensions) {
			if (!WorkbenchActivityHelper.filterItem(extension) && extension.isEnabled()) {
				String label = extension.getLabel();
				ImageDescriptor image = extension.getImageDescriptor();
				addActionToMenu(fMenu, new ConsoleFactoryAction(label, image, extension), accel);
				accel++;
				if (extension.isNewConsoleExtenson()) {
					new Separator("new").fill(fMenu, -1); //$NON-NLS-1$
				}
			}
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
	public Menu getMenu(Menu parent) {
		return null;
	}

	private class ConsoleFactoryAction extends Action {

		private ConsoleFactoryExtension fConfig;
		private IConsoleFactory fFactory;

		public ConsoleFactoryAction(String label, ImageDescriptor image, ConsoleFactoryExtension extension) {
			setText(label);
			if (image != null) {
				setImageDescriptor(image);
			}
			fConfig = extension;
		}

		@Override
		public void run() {
			try {
				if (fFactory == null) {
					fFactory = fConfig.createFactory();
				}

				fFactory.openConsole();
			} catch (CoreException e) {
				ConsolePlugin.log(e);
			}
		}

		@Override
		public void runWithEvent(Event event) {
			run();
		}
	}
}
