/*******************************************************************************
 * Copyright (c) 2000, 2020 IBM Corporation and others.
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
import org.eclipse.ant.core.AntCorePreferences;
import org.eclipse.ant.core.IAntClasspathEntry;
import org.eclipse.ant.core.Property;
import org.eclipse.ant.core.Task;
import org.eclipse.ant.core.Type;
import org.eclipse.ant.internal.ui.AntUIPlugin;
import org.eclipse.ant.internal.ui.IAntUIHelpContextIds;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.TabFolderLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.PlatformUI;
import org.osgi.framework.FrameworkUtil;

/**
 * Ant preference page to set the classpath, tasks, types and properties.
 */
public class AntRuntimePreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

	private AntClasspathPage classpathPage;
	private AntTasksPage tasksPage;
	private AntTypesPage typesPage;
	private AntPropertiesPage propertiesPage;

	/**
	 * Creates the preference page
	 */
	public AntRuntimePreferencePage() {
		setDescription(AntPreferencesMessages.AntPreferencePage_description);
		setPreferenceStore(AntUIPlugin.getDefault().getPreferenceStore());
	}

	@Override
	public void init(IWorkbench workbench) {
		// do nothing
	}

	@Override
	protected Control createContents(Composite parent) {
		PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(), IAntUIHelpContextIds.ANT_RUNTIME_PREFERENCE_PAGE);
		initializeDialogUnits(parent);

		TabFolder folder = new TabFolder(parent, SWT.NONE);
		folder.setLayout(new TabFolderLayout());
		folder.setLayoutData(new GridData(GridData.FILL_BOTH));
		folder.setFont(parent.getFont());

		classpathPage = new AntClasspathPage(this);
		classpathPage.createTabItem(folder);

		tasksPage = new AntTasksPage(this);
		tasksPage.createTabItem(folder);

		typesPage = new AntTypesPage(this);
		typesPage.createTabItem(folder);

		propertiesPage = new AntPropertiesPage(this);
		propertiesPage.createTabItem(folder);

		tasksPage.initialize();
		typesPage.initialize();
		classpathPage.initialize();
		propertiesPage.initialize();

		return folder;
	}

	@Override
	protected void performDefaults() {
		super.performDefaults();

		AntCorePreferences prefs = AntCorePlugin.getPlugin().getPreferences();
		tasksPage.setInput(prefs.getDefaultTasks());
		typesPage.setInput(prefs.getDefaultTypes());
		classpathPage.performDefaults();
		propertiesPage.performDefaults();
	}

	@Override
	public boolean performOk() {
		AntCorePreferences prefs = AntCorePlugin.getPlugin().getPreferences();
		IDialogSettings settings = PlatformUI.getDialogSettingsProvider(FrameworkUtil.getBundle(AntRuntimePreferencePage.class)).getDialogSettings();

		prefs.setAntHomeClasspathEntries(classpathPage.getAntHomeEntries());

		prefs.setAdditionalClasspathEntries(classpathPage.getAdditionalEntries());

		String antHome = classpathPage.getAntHome();
		prefs.setAntHome(antHome);

		List<?> contents = tasksPage.getContents(false);
		if (contents != null) {
			Task[] tasks = contents.toArray(new Task[contents.size()]);
			prefs.setCustomTasks(tasks);
		}

		tasksPage.saveColumnSettings(settings);

		contents = typesPage.getContents(false);
		if (contents != null) {
			Type[] types = contents.toArray(new Type[contents.size()]);
			prefs.setCustomTypes(types);
		}

		typesPage.saveColumnSettings(settings);

		contents = propertiesPage.getProperties();
		if (contents != null) {
			Property[] properties = contents.toArray(new Property[contents.size()]);
			prefs.setCustomProperties(properties);
		}

		String[] files = propertiesPage.getPropertyFiles();
		prefs.setCustomPropertyFiles(files);

		propertiesPage.saveAdditionalSettings();

		prefs.updatePluginPreferences();

		return super.performOk();
	}

	@Override
	protected GridData setButtonLayoutData(Button button) {
		return super.setButtonLayoutData(button);
	}

	protected List<IAntClasspathEntry> getLibraryEntries() {
		List<IAntClasspathEntry> urls = new ArrayList<>();
		urls.addAll(Arrays.asList(classpathPage.getAntHomeEntries()));
		urls.addAll(Arrays.asList(classpathPage.getAdditionalEntries()));
		urls.addAll(Arrays.asList(classpathPage.getContributedEntries()));
		return urls;
	}
}
