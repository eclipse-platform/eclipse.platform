/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.update.internal.ui;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.window.ApplicationWindow;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.update.internal.ui.views.ConfigurationView;

/**
 * Configuration Manager window.
 */
public class ConfigurationManagerWindow
	extends ApplicationWindow
	{
	private ConfigurationView view;
	private GlobalAction propertiesAction;
	private IAction propertiesActionHandler;
	
	class GlobalAction extends Action implements IPropertyChangeListener {
		private IAction handler;

		public GlobalAction() {
		}

		public void setActionHandler(IAction action) {
			if (handler != null) {
				handler.removePropertyChangeListener(this);
				handler = null;
			}
			if (action != null) {
				this.handler = action;
				action.addPropertyChangeListener(this);
			}
			if (handler != null) {
				setEnabled(handler.isEnabled());
				setChecked(handler.isChecked());
			}
		}

		public void propertyChange(PropertyChangeEvent event) {
			if (event.getProperty().equals(Action.ENABLED)) {
				Boolean bool = (Boolean) event.getNewValue();
				setEnabled(bool.booleanValue());
			} else if (event.getProperty().equals(Action.CHECKED)) {
				Boolean bool = (Boolean) event.getNewValue();
				setChecked(bool.booleanValue());
			}
		}

		public void run() {
			if (handler != null)
				handler.run();
		}
	}

	/**
	 * @param parentShell
	 */
	public ConfigurationManagerWindow(Shell parentShell) {
		super(parentShell);
		setShellStyle(getShellStyle() | SWT.APPLICATION_MODAL);
		// Setup window.
		addMenuBar();
		addActions();
		addToolBar(SWT.FLAT);
//		addStatusLine();
	}

	private void addActions() {
		IMenuManager menuBar = getMenuBarManager();
		IMenuManager fileMenu = new MenuManager(UpdateUIMessages.ConfigurationManagerWindow_fileMenu); 
		menuBar.add(fileMenu);

		propertiesAction = new GlobalAction();
		propertiesAction.setText(UpdateUIMessages.ConfigurationManagerWindow_properties); 
		propertiesAction.setEnabled(false);

		fileMenu.add(propertiesAction);
		fileMenu.add(new Separator());

		Action closeAction = new Action() {
			public void run() {
				close();
			}
		};
		closeAction.setText(UpdateUIMessages.ConfigurationManagerWindow_close); 
		fileMenu.add(closeAction);
	}
	
	private void hookGlobalActions() {
		if(propertiesActionHandler!=null)
			propertiesAction.setActionHandler(propertiesActionHandler);
	}
	
	protected Control createContents(Composite parent) {
		view = new ConfigurationView(this);
		Composite container = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.marginWidth = layout.marginHeight = 0;
		layout.verticalSpacing = 0;
		container.setLayout(layout);

		GridData gd;
		Label separator = new Label(container, SWT.SEPARATOR | SWT.HORIZONTAL);
		gd = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		gd.heightHint = 1;
		separator.setLayoutData(gd);

		view.createPartControl(container);
		Control viewControl = view.getControl();
		gd = new GridData(GridData.FILL_BOTH);
		viewControl.setLayoutData(gd);
		hookGlobalActions();

		updateActionBars();

		UpdateLabelProvider provider = UpdateUI.getDefault().getLabelProvider();
		getShell().setImage(provider.get(UpdateUIImages.DESC_CONFIGS_VIEW, 0));
		
		return container;
	}

	private void updateActionBars() {
		getMenuBarManager().updateAll(false);
		getToolBarManager().update(false);
//		getStatusLineManager().update(false);
	}
	/* (non-Javadoc)
	 * @see org.eclipse.jface.window.Window#close()
	 */
	public boolean close() {
		if (view != null)
			view.dispose();
		return super.close();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.window.Window#create()
	 */
	public void create() {
		super.create();
		// set the title
		getShell().setText(UpdateUIMessages.ConfigurationManagerAction_title); 
		getShell().setSize(800, 600);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.window.Window#open()
	 */
	public int open() {
		// update action bars
		updateActionBars();
		return super.open();
	}
	
	public void setPropertiesActionHandler(IAction handler){
		propertiesActionHandler=handler;
	}
}
