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
package org.eclipse.ant.internal.ui.preferences;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.ant.core.AntCorePlugin;
import org.eclipse.ant.core.Property;
import org.eclipse.ant.internal.ui.IAntUIHelpContextIds;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.ui.PlatformUI;

/**
 * Preference page for setting global Ant user properties. All properties specified here will be set as user properties on the project for any Ant
 * build
 */
public class AntPropertiesPage implements IAntBlockContainer {

	private final AntPropertiesBlock antPropertiesBlock = new AntPropertiesBlock(this);
	private final AntRuntimePreferencePage preferencePage;

	/**
	 * Creates an instance.
	 */
	public AntPropertiesPage(AntRuntimePreferencePage preferencePage) {
		this.preferencePage = preferencePage;
	}

	/**
	 * Creates the tab item that contains this sub-page.
	 */
	protected TabItem createTabItem(TabFolder folder) {
		TabItem item = new TabItem(folder, SWT.NONE);
		item.setText(AntPreferencesMessages.AntPropertiesPage_title);
		item.setImage(AntObjectLabelProvider.getPropertyImage());
		item.setData(this);
		item.setControl(createContents(folder));
		return item;
	}

	protected Composite createContents(Composite parent) {
		Font font = parent.getFont();

		Composite top = new Composite(parent, SWT.NONE);
		top.setFont(font);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(top, IAntUIHelpContextIds.ANT_PROPERTIES_PAGE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		top.setLayout(layout);
		GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
		top.setLayoutData(gridData);

		antPropertiesBlock.createControl(top, AntPreferencesMessages.AntPropertiesPage__Global_properties__1, AntPreferencesMessages.AntPropertiesPage_Glo_bal_property_files__2);

		return top;
	}

	/**
	 * Sets the contents of the tables on this page.
	 */
	protected void initialize() {
		List<Property> allProperties = AntCorePlugin.getPlugin().getPreferences().getDefaultProperties();
		allProperties.addAll(Arrays.asList(AntCorePlugin.getPlugin().getPreferences().getCustomProperties()));
		antPropertiesBlock.setPropertiesInput(allProperties.toArray(new Property[allProperties.size()]));
		antPropertiesBlock.setPropertyFilesInput(AntCorePlugin.getPlugin().getPreferences().getCustomPropertyFiles(false));
		antPropertiesBlock.update();
	}

	protected void performDefaults() {
		List<Property> defaultProperties = AntCorePlugin.getPlugin().getPreferences().getDefaultProperties();
		antPropertiesBlock.setPropertiesInput(defaultProperties.toArray(new Property[defaultProperties.size()]));
		antPropertiesBlock.setPropertyFilesInput(new String[0]);
		antPropertiesBlock.update();
	}

	/**
	 * Delegates to saving any additional table settings when the page is closed
	 * 
	 * @since 3.5
	 */
	public void saveAdditionalSettings() {
		antPropertiesBlock.saveSettings();
	}

	/**
	 * Returns the specified property files
	 * 
	 * @return String[]
	 */
	protected String[] getPropertyFiles() {
		Object[] elements = antPropertiesBlock.getPropertyFiles();
		String[] files = new String[elements.length];
		for (int i = 0; i < elements.length; i++) {
			files[i] = (String) elements[i];
		}
		return files;
	}

	@Override
	public void setMessage(String message) {
		preferencePage.setMessage(message);
	}

	@Override
	public void setErrorMessage(String message) {
		preferencePage.setErrorMessage(message);
	}

	@Override
	public Button createPushButton(Composite parent, String buttonText) {
		Button button = new Button(parent, SWT.PUSH);
		button.setFont(parent.getFont());
		button.setText(buttonText);
		preferencePage.setButtonLayoutData(button);
		return button;
	}

	@Override
	public void update() {
		// do nothing
	}

	protected List<Property> getProperties() {
		Object[] allProperties = antPropertiesBlock.getProperties();
		List<Property> properties = new ArrayList<>(allProperties.length);
		for (Object prop : allProperties) {
			Property property = (Property) prop;
			if (!property.isDefault()) {
				properties.add(property);
			}
		}
		return properties;
	}
}
