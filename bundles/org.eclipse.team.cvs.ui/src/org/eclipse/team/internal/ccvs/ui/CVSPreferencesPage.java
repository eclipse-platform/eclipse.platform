/*******************************************************************************
 * Copyright (c) 2000, 2018 IBM Corporation and others.
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
 *     Philippe Ombredanne - bug 84808
 *     William Mitsuda (wmitsuda@gmail.com) - Bug 153879 [Wizards] configurable size of cvs commit comment history
 *     Brock Janiczak <brockj@tpg.com.au> - Bug 161536 Warn user when committing resources with problem markers
 *******************************************************************************/

package org.eclipse.team.internal.ccvs.ui;

import java.text.Collator;
import java.util.*;
import java.util.List;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.team.internal.ccvs.core.CVSProviderPlugin;
import org.eclipse.team.internal.ccvs.core.client.Command;
import org.eclipse.team.internal.ccvs.core.client.Command.KSubstOption;
import org.eclipse.team.internal.ccvs.core.client.Command.QuietOption;
import org.eclipse.team.internal.ui.SWTUtils;
import org.eclipse.ui.*;
import org.eclipse.ui.preferences.IWorkbenchPreferenceContainer;

/**
 * CVS Preference Page
 * 
 * Allows the configuration of CVS specific options.
 * The currently supported options are:
 *  - Allow loading of CVS administration directory (CVSROOT)
 * 
 * There are currently a couple of deficiencies:
 *  1. The Repository view is not refreshed when the show CVSROOT option is changed
 *  2. There is no help associated with the page
 */
public class CVSPreferencesPage extends PreferencePage implements IWorkbenchPreferencePage {
	
	public static class PerspectiveDescriptorComparator implements Comparator {
		@Override
		public int compare(Object o1, Object o2) {
			if (o1 instanceof IPerspectiveDescriptor && o2 instanceof IPerspectiveDescriptor) {
				String id1= ((IPerspectiveDescriptor)o1).getLabel();
				String id2= ((IPerspectiveDescriptor)o2).getLabel();
				return Collator.getInstance().compare(id1, id2);
			}
			return 0;
		}
	}
	
	
	private abstract class Field {
		protected final String fKey;
		public Field(String key) { fFields.add(this); fKey= key; }
		public abstract void initializeValue(IPreferenceStore store);
		public abstract void performOk(IPreferenceStore store);
		public void performDefaults(IPreferenceStore store) {
			store.setToDefault(fKey);
			initializeValue(store);
		}
	}
	
	private class Checkbox extends Field {
		
		private final Button fCheckbox;
		
		public Checkbox(Composite composite, String key, String label, String helpID) {
			super(key);
			fCheckbox= new Button(composite, SWT.CHECK);
			fCheckbox.setText(label);
			PlatformUI.getWorkbench().getHelpSystem().setHelp(fCheckbox, helpID);
		}
		
		@Override
		public void initializeValue(IPreferenceStore store) {
			fCheckbox.setSelection(store.getBoolean(fKey));
		}
		
		@Override
		public void performOk(IPreferenceStore store) {
			store.setValue(fKey, fCheckbox.getSelection());
		}
	}

	private abstract class ComboBox extends Field {
		protected final Combo fCombo;
		private final String [] fLabels;
		private final List fValues;
		
		public ComboBox(Composite composite, String key, String text, String helpID, String [] labels, Object [] values) {
			super(key);
			fLabels= labels;
			fValues= Arrays.asList(values);
			
			final Label label= SWTUtils.createLabel(composite, text);
			fCombo= new Combo(composite, SWT.READ_ONLY);
			fCombo.setLayoutData(SWTUtils.createHFillGridData());
			fCombo.setItems(labels);
			
			if (((GridLayout)composite.getLayout()).numColumns > 1) {
				label.setLayoutData(SWTUtils.createGridData(SWT.DEFAULT, SWT.DEFAULT, false, false));
			}
			
			PlatformUI.getWorkbench().getHelpSystem().setHelp(fCombo, helpID);
		}
		
		public Combo getCombo() {
			return fCombo;
		}
		
		@Override
		public void initializeValue(IPreferenceStore store) {
			final Object value= getValue(store, fKey);
			final int index= fValues.indexOf(value); 
			if (index >= 0 && index < fLabels.length)
				fCombo.select(index);
			else 
				fCombo.select(0);
		}
		
		@Override
		public void performOk(IPreferenceStore store) {
			saveValue(store, fKey, fValues.get(fCombo.getSelectionIndex()));
		}
		
		protected abstract void saveValue(IPreferenceStore store, String key, Object object);
		protected abstract Object getValue(IPreferenceStore store, String key);
	}
	
	private class IntegerComboBox extends ComboBox {
		public IntegerComboBox(Composite composite, String key, String label, String helpID, String[] labels, Integer [] values) {
			super(composite, key, label, helpID, labels, values);
		}

		@Override
		protected void saveValue(IPreferenceStore store, String key, Object object) {
			store.setValue(key, ((Integer)object).intValue());			
		}
		
		@Override
		protected Object getValue(IPreferenceStore store, String key) {
			return Integer.valueOf(store.getInt(key));			
		}
	}
	
	private class StringComboBox extends ComboBox {
		
		public StringComboBox(Composite composite, String key, String label, String helpID, String [] labels, String [] values) {
			super(composite, key, label, helpID, labels, values);
		}

		@Override
		protected Object getValue(IPreferenceStore store, String key) {
			return store.getString(key);
		}
		
		@Override
		protected void saveValue(IPreferenceStore store, String key, Object object) {
			store.setValue(key, (String)object);
		}
	}

	private abstract class RadioButtons extends Field {
		protected final Button [] fButtons;
		private final String [] fLabels;
		private final List fValues;
		private final Group fGroup;
		
		public RadioButtons(Composite composite, String key, String label, String helpID, String [] labels, Object [] values) {
			super(key);
			fLabels= labels;
			fValues= Arrays.asList(values);
			
			fGroup= SWTUtils.createHFillGroup(composite, label, SWTUtils.MARGINS_DEFAULT, labels.length);
			
			
			fButtons= new Button [labels.length];
			for (int i = 0; i < fLabels.length; i++) {
				fButtons[i]= new Button(fGroup, SWT.RADIO);
				fButtons[i].setLayoutData(SWTUtils.createGridData(SWT.DEFAULT, SWT.DEFAULT, false, false));
				fButtons[i].setText(labels[i]);
			}
			SWTUtils.equalizeControls(SWTUtils.createDialogPixelConverter(composite), fButtons, 0, fButtons.length - 2);
			PlatformUI.getWorkbench().getHelpSystem().setHelp(fGroup, helpID);
		}
		
		@Override
		public void initializeValue(IPreferenceStore store) {
			final Object value= loadValue(store, fKey);
			final int index= fValues.indexOf(value);
			for (int i = 0; i < fButtons.length; i++) {
				Button b = fButtons[i];
				b.setSelection(index == i);
			}
		}

		@Override
		public void performOk(IPreferenceStore store) {
			for (int i = 0; i < fButtons.length; ++i) {
				if (fButtons[i].getSelection()) {
					saveValue(store, fKey, fValues.get(i));
					return;
				} 
			}
		}
		
		protected abstract Object loadValue(IPreferenceStore store, String key);

		protected abstract void saveValue(IPreferenceStore store, String key, Object value);
	}

	private class IntegerRadioButtons extends RadioButtons {

		public IntegerRadioButtons(Composite composite, String key, String label, String helpID, String[] labels, Integer [] values) {
			super(composite, key, label, helpID, labels, values);
		}

		@Override
		protected Object loadValue(IPreferenceStore store, String key) {
			return Integer.valueOf(store.getInt(key));
		}

		@Override
		protected void saveValue(IPreferenceStore store, String key, Object value) {
			store.setValue(key, ((Integer)value).intValue());
		}
	}
	
	private class StringRadioButtons extends RadioButtons {

		public StringRadioButtons(Composite composite, String key, String label, String helpID, String[] labels, String [] values) {
			super(composite, key, label, helpID, labels, values);
		}

		@Override
		protected Object loadValue(IPreferenceStore store, String key) {
			return store.getString(key);
		}

		@Override
		protected void saveValue(IPreferenceStore store, String key, Object value) {
			store.setValue(key, (String)value);
		}
	}
	
	private abstract class TextField extends Field implements ModifyListener {
		protected final Text fText;
		
		public TextField(Composite composite, String key, String text, String helpID) {
			super(key);
			
			final Label label= new Label(composite, SWT.WRAP);
			label.setText(text);
			label.setLayoutData(SWTUtils.createGridData(SWT.DEFAULT, SWT.DEFAULT, false, false));
			
			fText= SWTUtils.createText(composite);
			fText.addModifyListener(this);
			
			if (helpID != null)
				PlatformUI.getWorkbench().getHelpSystem().setHelp(fText, helpID);
		}
		
		@Override
		public void initializeValue(IPreferenceStore store) {
			final String value= store.getString(fKey);
			fText.setText(value);
		}
		
		@Override
		public void performOk(IPreferenceStore store) {
			store.setValue(fKey, fText.getText());
		}
		
		@Override
		public void modifyText(ModifyEvent e) {
			modifyText(fText);
		}
		
		protected abstract void modifyText(Text text);
	}
	
	private final String [] KSUBST_VALUES;
	private final String [] KSUBST_LABELS;
	
	private final String [] COMPRESSION_LABELS;
	private final Integer [] COMPRESSION_VALUES;
	
	protected final ArrayList fFields;
	private final String [] PERSPECTIVE_VALUES;
	private final String [] PERSPECTIVE_LABELS;
	private final String [] YES_NO_PROMPT;
	
	public CVSPreferencesPage() {
		fFields= new ArrayList();
		
		final KSubstOption[] options = KSubstOption.getAllKSubstOptions();
		final ArrayList<KSubstOption> KSUBST_MODES= new ArrayList<>();
		for (KSubstOption option : options) {
			if (!option.isBinary()) {
				KSUBST_MODES.add(option);
			}
		}
		Collections.sort(KSUBST_MODES, (a, b) -> {
			final String aKey = a.getLongDisplayText();
			final String bKey = b.getLongDisplayText();
			return aKey.compareTo(bKey);
		});
		
		KSUBST_LABELS= new String[KSUBST_MODES.size()];
		KSUBST_VALUES= new String[KSUBST_MODES.size()];
		int index= 0;
		for (Iterator iter = KSUBST_MODES.iterator(); iter.hasNext();) {
			KSubstOption mod = (KSubstOption) iter.next();
			KSUBST_LABELS[index]= mod.getLongDisplayText();
			final String mode= mod.toMode().trim();
			KSUBST_VALUES[index]= mode.length() != 0 ? mode : "-kkv";    //$NON-NLS-1$
			++index;
		}	
	
		COMPRESSION_LABELS = new String[] { CVSUIMessages.CVSPreferencesPage_0,
				CVSUIMessages.CVSPreferencesPage_1,
				CVSUIMessages.CVSPreferencesPage_2,
				CVSUIMessages.CVSPreferencesPage_3,
				CVSUIMessages.CVSPreferencesPage_4,
				CVSUIMessages.CVSPreferencesPage_5,
				/* CVSUIMessages.CVSPreferencesPage_6,  // Disallow 6 through 9 due to server bug (see bug 15724)
				CVSUIMessages.CVSPreferencesPage_7,
				CVSUIMessages.CVSPreferencesPage_8,
				CVSUIMessages.CVSPreferencesPage_9 */};      
		COMPRESSION_VALUES= new Integer [COMPRESSION_LABELS.length];
		for (int i = 0; i < COMPRESSION_VALUES.length; i++) {
			COMPRESSION_VALUES[i]= Integer.valueOf(i);
		}
		
		final IPerspectiveDescriptor [] perspectives= PlatformUI.getWorkbench().getPerspectiveRegistry().getPerspectives();
		PERSPECTIVE_VALUES= new String[perspectives.length + 1];
		PERSPECTIVE_LABELS= new String [perspectives.length + 1];
		Arrays.sort(perspectives, new PerspectiveDescriptorComparator());
		PERSPECTIVE_VALUES[0]= ICVSUIConstants.OPTION_NO_PERSPECTIVE;
		PERSPECTIVE_LABELS[0]= CVSUIMessages.CVSPreferencesPage_10; 
		for (int i = 0; i < perspectives.length; i++) {
			PERSPECTIVE_VALUES[i + 1]= perspectives[i].getId();
			PERSPECTIVE_LABELS[i + 1]= perspectives[i].getLabel();
		}
		
		YES_NO_PROMPT= new String [] { CVSUIMessages.CVSPreferencesPage_11, CVSUIMessages.CVSPreferencesPage_12, CVSUIMessages.CVSPreferencesPage_13 }; //  
	}

	@Override
	protected Control createContents(Composite parent) {
		
		// create a tab folder for the page
		final TabFolder tabFolder = new TabFolder(parent, SWT.NONE);
		tabFolder.setLayoutData(SWTUtils.createHFillGridData());

		createGeneralTab(tabFolder);
		createFilesFoldersTab(tabFolder);
		createConnectionTab(tabFolder);
		createPromptingTab(tabFolder);
		
		initializeValues();
		
		Dialog.applyDialogFont(parent);
		
		PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(), IHelpContextIds.GENERAL_PREFERENCE_PAGE);
		return tabFolder;
	}

	private Composite createGeneralTab(final TabFolder tabFolder) {
		final Composite composite = SWTUtils.createHFillComposite(tabFolder, SWTUtils.MARGINS_DEFAULT);
		final TabItem tab= new TabItem(tabFolder, SWT.NONE);
		tab.setText(CVSUIMessages.CVSPreferencesPage_14); 
		tab.setControl(composite);
		new Checkbox(composite, ICVSUIConstants.PREF_DETERMINE_SERVER_VERSION,  CVSUIMessages.CVSPreferencesPage_15, IHelpContextIds.PREF_DETERMINE_SERVER_VERSION);
		new Checkbox(composite, ICVSUIConstants.PREF_CONFIRM_MOVE_TAG, CVSUIMessages.CVSPreferencesPage_16, IHelpContextIds.PREF_CONFIRM_MOVE_TAG);
		new Checkbox(composite, ICVSUIConstants.PREF_DEBUG_PROTOCOL, CVSUIMessages.CVSPreferencesPage_17, IHelpContextIds.PREF_DEBUG_PROTOCOL);
		new Checkbox(composite, ICVSUIConstants.PREF_AUTO_REFRESH_TAGS_IN_TAG_SELECTION_DIALOG, CVSUIMessages.CVSPreferencesPage_18, IHelpContextIds.PREF_AUTOREFRESH_TAG);
		new Checkbox(composite, ICVSUIConstants.PREF_AUTO_SHARE_ON_IMPORT, CVSUIMessages.CVSPreferencesPage_44, null);
		new Checkbox(composite, ICVSUIConstants.PREF_USE_PROJECT_NAME_ON_CHECKOUT, CVSUIMessages.CVSPreferencesPage_45, null);
		
		final Composite textComposite= SWTUtils.createHFillComposite(composite, SWTUtils.MARGINS_NONE, 2);
		new TextField(
				textComposite, 
				ICVSUIConstants.PREF_COMMIT_FILES_DISPLAY_THRESHOLD, 
				CVSUIMessages.CVSPreferencesPage_20, 
				null) {
			@Override
			protected void modifyText(Text text) {
				// Parse the timeout value
				try {
					final int x = Integer.parseInt(text.getText());
					if (x >= 0) {
						setErrorMessage(null);
						setValid(true);
					} else {
						setErrorMessage(CVSUIMessages.CVSPreferencesPage_21); 
						setValid(false);
					}
				} catch (NumberFormatException ex) {
					setErrorMessage(CVSUIMessages.CVSPreferencesPage_22); 
					setValid(false);
				}
			}
		};
		new TextField(
				textComposite, 
				ICVSUIConstants.PREF_COMMIT_COMMENTS_MAX_HISTORY, 
				CVSUIMessages.CVSPreferencesPage_47, 
				null) {
			@Override
			protected void modifyText(Text text) {
				try {
					final int x = Integer.parseInt(text.getText());
					if (x > 0) {
						setErrorMessage(null);
						setValid(true);
					} else {
						setErrorMessage(CVSUIMessages.CVSPreferencesPage_48); 
						setValid(false);
					}
				} catch (NumberFormatException ex) {
					setErrorMessage(CVSUIMessages.CVSPreferencesPage_49); 
					setValid(false);
				}
			}
		};
		
		return composite;
	}
	
	private Composite createConnectionTab(final TabFolder tabFolder) {
		final Composite composite = SWTUtils.createHFillComposite(tabFolder, SWTUtils.MARGINS_DEFAULT);
		final TabItem tab= new TabItem(tabFolder, SWT.NONE);
		tab.setText(CVSUIMessages.CVSPreferencesPage_19); 
		tab.setControl(composite);
		
		final Composite textComposite= SWTUtils.createHFillComposite(composite, SWTUtils.MARGINS_NONE, 2);
		new TextField(
				textComposite, 
				ICVSUIConstants.PREF_TIMEOUT, 
				CVSUIMessages.CVSPreferencesPage_23,  
				IHelpContextIds.PREF_COMMS_TIMEOUT) {
			@Override
			protected void modifyText(Text text) {
				// Parse the timeout value
				try {
					final int x = Integer.parseInt(text.getText());
					if (x >= 0) {
						setErrorMessage(null);
						setValid(true);
					} else {
						setErrorMessage(CVSUIMessages.CVSPreferencesPage_24); 
						setValid(false);
					}
				} catch (NumberFormatException ex) {
					setErrorMessage(CVSUIMessages.CVSPreferencesPage_25); 
					setValid(false);
				}
			}
		};
		
		final ComboBox quietnessCombo = new IntegerComboBox(
				textComposite, 
				ICVSUIConstants.PREF_QUIETNESS, 
				CVSUIMessages.CVSPreferencesPage_26,  
				IHelpContextIds.PREF_QUIET,
				new String [] { CVSUIMessages.CVSPreferencesPage_27, CVSUIMessages.CVSPreferencesPage_28, CVSUIMessages.CVSPreferencesPage_29 }, //  
				new Integer [] { Integer.valueOf(0), Integer.valueOf(1), Integer.valueOf(2)});
		
		quietnessCombo.getCombo().addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (getQuietnessOptionFor(quietnessCombo.getCombo().getSelectionIndex()).equals(Command.SILENT)) {
					MessageDialog.openWarning(getShell(), CVSUIMessages.CVSPreferencesPage_30, CVSUIMessages.CVSPreferencesPage_31);  // 
				}
			}
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				widgetSelected(e);
			}
		});

		new IntegerComboBox(
				textComposite, 
				ICVSUIConstants.PREF_COMPRESSION_LEVEL, 
				CVSUIMessages.CVSPreferencesPage_32,  
				IHelpContextIds.PREF_COMPRESSION, 
				COMPRESSION_LABELS, COMPRESSION_VALUES);
		
		SWTUtils.createPreferenceLink((IWorkbenchPreferenceContainer) getContainer(), composite, CVSUIMessages.CVSPreferencesPage_52, CVSUIMessages.CVSPreferencesPage_53);
		
		SWTUtils.createPreferenceLink((IWorkbenchPreferenceContainer) getContainer(), composite, CVSUIMessages.CVSPreferencesPage_54, CVSUIMessages.CVSPreferencesPage_55);

		return composite;
	}

	
	private Composite createFilesFoldersTab(final TabFolder tabFolder) {
		final Composite composite = SWTUtils.createHFillComposite(tabFolder, SWTUtils.MARGINS_DEFAULT);
		final TabItem tab= new TabItem(tabFolder, SWT.NONE);
		tab.setText(CVSUIMessages.CVSPreferencesPage_33); 
		tab.setControl(composite);
		new Checkbox(composite, ICVSUIConstants.PREF_REPOSITORIES_ARE_BINARY, CVSUIMessages.CVSPreferencesPage_34, IHelpContextIds.PREF_TREAT_NEW_FILE_AS_BINARY); 
		new Checkbox(composite, ICVSUIConstants.PREF_USE_PLATFORM_LINEEND, CVSUIMessages.CVSPreferencesPage_35, IHelpContextIds.PREF_LINEEND); 
		new Checkbox(composite, ICVSUIConstants.PREF_PRUNE_EMPTY_DIRECTORIES, CVSUIMessages.CVSPreferencesPage_36, IHelpContextIds.PREF_PRUNE); 
		new Checkbox(composite, ICVSUIConstants.PREF_REPLACE_UNMANAGED, CVSUIMessages.CVSPreferencesPage_37, IHelpContextIds.PREF_REPLACE_DELETE_UNMANAGED); 
		SWTUtils.createPlaceholder(composite, 1);
		final Composite bottom= SWTUtils.createHFillComposite(composite, SWTUtils.MARGINS_NONE, 2);
		new StringComboBox(
				bottom, 
				ICVSUIConstants.PREF_TEXT_KSUBST, 
				CVSUIMessages.CVSPreferencesPage_38,  
				IHelpContextIds.PREF_KEYWORDMODE, 
				KSUBST_LABELS, KSUBST_VALUES);
		
		return composite;
	}
	
	private Composite createPromptingTab(final TabFolder tabFolder) {

		final Composite composite = SWTUtils.createHFillComposite(tabFolder, SWTUtils.MARGINS_DEFAULT, 1);
		final TabItem tab= new TabItem(tabFolder, SWT.NONE);
		tab.setText(CVSUIMessages.CVSPreferencesPage_39); 
		tab.setControl(composite);
		
		new StringRadioButtons(
				composite,
				ICVSUIConstants.PREF_ALLOW_EMPTY_COMMIT_COMMENTS,
				CVSUIMessages.CVSPreferencesPage_40, 
				IHelpContextIds.PREF_ALLOW_EMPTY_COMMIT_COMMENTS,
				YES_NO_PROMPT,
				new String [] { MessageDialogWithToggle.ALWAYS, MessageDialogWithToggle.NEVER, MessageDialogWithToggle.PROMPT });
		
		new IntegerRadioButtons(composite, 
				ICVSUIConstants.PREF_SAVE_DIRTY_EDITORS, 
				CVSUIMessages.CVSPreferencesPage_41,  
				IHelpContextIds.PREF_SAVE_DIRTY_EDITORS, 
				YES_NO_PROMPT,
				new Integer [] { Integer.valueOf(ICVSUIConstants.OPTION_AUTOMATIC),	Integer.valueOf(ICVSUIConstants.OPTION_NEVER), 	Integer.valueOf(ICVSUIConstants.OPTION_PROMPT)});
		
		new StringRadioButtons(
				composite, 
				ICVSUIConstants.PREF_INCLUDE_CHANGE_SETS_IN_COMMIT, 
				CVSUIMessages.CVSPreferencesPage_46, 
				IHelpContextIds.PREF_INCLUDE_CHANGE_SETS_IN_COMMIT,
				YES_NO_PROMPT,
				new String [] { MessageDialogWithToggle.ALWAYS, MessageDialogWithToggle.NEVER,	MessageDialogWithToggle.PROMPT }
				);
		
		new StringRadioButtons(
				composite, 
				ICVSUIConstants.PREF_ALLOW_COMMIT_WITH_WARNINGS, 
				CVSUIMessages.CVSPreferencesPage_50, 
				IHelpContextIds.PREF_INCLUDE_CHANGE_SETS_IN_COMMIT,
				YES_NO_PROMPT,
				new String [] { MessageDialogWithToggle.ALWAYS, MessageDialogWithToggle.NEVER,	MessageDialogWithToggle.PROMPT }
				);

		new StringRadioButtons(
				composite, 
				ICVSUIConstants.PREF_ALLOW_COMMIT_WITH_ERRORS, 
				CVSUIMessages.CVSPreferencesPage_51, 
				IHelpContextIds.PREF_INCLUDE_CHANGE_SETS_IN_COMMIT,
				YES_NO_PROMPT,
				new String [] { MessageDialogWithToggle.ALWAYS, MessageDialogWithToggle.NEVER,	MessageDialogWithToggle.PROMPT }
				);
		SWTUtils.createPlaceholder(composite, 1);
	
		return composite;
	}
	
	/**
	 * Initializes states of the controls from the preference store.
	 */
	private void initializeValues() {
		final IPreferenceStore store = getPreferenceStore();
		for (Iterator iter = fFields.iterator(); iter.hasNext();) {
			((Field)iter.next()).initializeValue(store);
		}
	}

	@Override
	public void init(IWorkbench workbench) {
	}

	@Override
	public boolean performOk() {

		final IPreferenceStore store = getPreferenceStore();
		for (Iterator iter = fFields.iterator(); iter.hasNext();) {
			((Field) iter.next()).performOk(store);
		}

		CVSProviderPlugin.getPlugin().setReplaceUnmanaged(store.getBoolean(ICVSUIConstants.PREF_REPLACE_UNMANAGED));
		CVSProviderPlugin.getPlugin().setPruneEmptyDirectories(store.getBoolean(ICVSUIConstants.PREF_PRUNE_EMPTY_DIRECTORIES));
		CVSProviderPlugin.getPlugin().setTimeout(store.getInt(ICVSUIConstants.PREF_TIMEOUT));
		CVSProviderPlugin.getPlugin().setQuietness(getQuietnessOptionFor(store.getInt(ICVSUIConstants.PREF_QUIETNESS)));
		CVSProviderPlugin.getPlugin().setCompressionLevel(store.getInt(ICVSUIConstants.PREF_COMPRESSION_LEVEL));
		CVSProviderPlugin.getPlugin().setDebugProtocol(store.getBoolean(ICVSUIConstants.PREF_DEBUG_PROTOCOL));
		CVSProviderPlugin.getPlugin().setRepositoriesAreBinary(store.getBoolean(ICVSUIConstants.PREF_REPOSITORIES_ARE_BINARY));
		KSubstOption oldKSubst = CVSProviderPlugin.getPlugin().getDefaultTextKSubstOption();
		KSubstOption newKSubst = KSubstOption.fromMode(store.getString(ICVSUIConstants.PREF_TEXT_KSUBST));
		CVSProviderPlugin.getPlugin().setDefaultTextKSubstOption(newKSubst);
		CVSProviderPlugin.getPlugin().setUsePlatformLineend(store.getBoolean(ICVSUIConstants.PREF_USE_PLATFORM_LINEEND));
		CVSProviderPlugin.getPlugin().setDetermineVersionEnabled(store.getBoolean(ICVSUIConstants.PREF_DETERMINE_SERVER_VERSION));
		CVSProviderPlugin.getPlugin().setAutoshareOnImport(store.getBoolean(ICVSUIConstants.PREF_AUTO_SHARE_ON_IMPORT));
		
		// changing the default keyword substitution mode for text files may affect
		// information displayed in the decorators
		if (! oldKSubst.equals(newKSubst)) {
			CVSUIPlugin.broadcastPropertyChange(new PropertyChangeEvent(this, CVSUIPlugin.P_DECORATORS_CHANGED, null, null));
		}
		
		CVSUIPlugin.getPlugin().savePluginPreferences();
		return true;
	}

	@Override
	protected void performDefaults() {
		super.performDefaults();
		final IPreferenceStore store = getPreferenceStore();
		for (Iterator iter = fFields.iterator(); iter.hasNext();) {
			((Field) iter.next()).performDefaults(store);
		}
	}

	/**
	 * Returns preference store that belongs to the our plugin.
	 * This is important because we want to store
	 * our preferences separately from the desktop.
	 *
	 * @return the preference store for this plugin
	 */
	@Override
	protected IPreferenceStore doGetPreferenceStore() {
		return CVSUIPlugin.getPlugin().getPreferenceStore();
	}
	
	protected static QuietOption getQuietnessOptionFor(int option) {
		switch (option) {
			case 0: return Command.VERBOSE;
			case 1: return Command.PARTLY_QUIET;
			case 2: return Command.SILENT;
		}
		return null;
	}
}
