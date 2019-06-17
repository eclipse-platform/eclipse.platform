/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
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
 *     Brock Janiczak (brockj@tpg.com.au) - Bug 158456 Preview fonts and colours in the CVS decorator preference page
 *******************************************************************************/
package org.eclipse.team.internal.ccvs.ui;

import java.util.*;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.resource.*;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.team.internal.ccvs.core.CVSException;
import org.eclipse.team.internal.ccvs.core.client.Command;
import org.eclipse.team.internal.ccvs.core.connection.CVSRepositoryLocation;
import org.eclipse.team.internal.ui.SWTUtils;
import org.eclipse.ui.*;
import org.eclipse.ui.dialogs.ListSelectionDialog;
import org.eclipse.ui.ide.IDE.SharedImages;
import org.eclipse.ui.preferences.IWorkbenchPreferenceContainer;

public class CVSDecoratorPreferencesPage extends PreferencePage implements IWorkbenchPreferencePage {

	private static class PreviewFile {
		
		public final String name, tag, mode;
		public final int type;
		public final boolean added, dirty, hasRemote, ignored, newResource;
		public Collection children;
		
		public PreviewFile(String name, int type, boolean added, boolean newResource, boolean dirty, boolean ignored, boolean hasRemote, String mode, String tag)  {
			this.name= name;
			this.type= type;
			this.added= added;
			this.ignored= ignored;
			this.dirty= dirty;
			this.hasRemote= hasRemote;
			this.newResource= newResource;
			this.mode= mode != null ? mode : Command.KSUBST_TEXT.getShortDisplayText();
			this.tag= tag != null ? tag : ""; //$NON-NLS-1$
			this.children= Collections.EMPTY_LIST;
		}
		
		public void configureDecoration(CVSDecoration decoration) {
			decoration.setResourceType(type);
			decoration.setAdded(added);
			decoration.setDirty(dirty);
			decoration.setNewResource(newResource);
			decoration.setIgnored(ignored);
			decoration.setHasRemote(hasRemote);
			decoration.setTag(tag);
			decoration.setKeywordSubstitution(mode);
		}
	}
	
	private static class FormatEditor extends SelectionAdapter {
		private final Text fText;
		private final Map fBindings;
		private final String fKey;
		
		public FormatEditor(Composite composite, String title, String buttonText, Map bindings, String key) {
			
			fKey= key;
			fBindings= bindings;
			
			final Label label= SWTUtils.createLabel(composite, title);
			label.setLayoutData(SWTUtils.createGridData(SWT.DEFAULT, SWT.DEFAULT, false, false));
			
			fText= SWTUtils.createText(composite);
			
			final Button button = new Button(composite, SWT.NONE);
			button.setText(buttonText);
			button.setLayoutData(new GridData());

			button.addSelectionListener(this);
		}
		
		public void addModifyListener(ModifyListener listener) {
			fText.addModifyListener(listener);
		}

		public String getText() {
			return fText.getText();
		}
		
		@Override
		public void widgetSelected(SelectionEvent e) {
		
			final ILabelProvider labelProvider = new LabelProvider() {
				@Override
				public String getText(Object element) {
					return ((Map.Entry)element).getKey() + " - " + ((Map.Entry)element).getValue(); //$NON-NLS-1$
				}
			};
			
			final IStructuredContentProvider contentsProvider = new IStructuredContentProvider() {
				@Override
				public Object[] getElements(Object inputElement) {
					return ((Collection)inputElement).toArray();
				}
				@Override
				public void dispose() {}
				@Override
				public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {}
			};
			
			final ListSelectionDialog dialog= new ListSelectionDialog(
					fText.getShell(),
					fBindings.entrySet(),
					contentsProvider,
					labelProvider,
					CVSUIMessages.CVSDecoratorPreferencesPage_0); 
			dialog.setHelpAvailable(false);
			dialog.setTitle(CVSUIMessages.CVSDecoratorPreferencesPage_1);  
			if (dialog.open() != Window.OK)
				return;
		
			Object[] result = dialog.getResult();
			
			for (Object r : result) {
				fText.insert("{" + ((Map.Entry) r).getKey() + "}"); //$NON-NLS-1$ //$NON-NLS-2$
			}		
		}

		public void performOk(IPreferenceStore store) {
			store.setValue(fKey, fText.getText());
		}
		
		public void performDefaults(IPreferenceStore store) {
			store.setToDefault(fKey);
			fText.setText(store.getDefaultString(fKey));
		}

		public void initializeValue(IPreferenceStore store) {
			fText.setText(
					CVSDecoration.updateOldDirtyFlag(store.getString(fKey)));
		}
	}
	
	private abstract class Tab extends Observable {
		
		public abstract void initializeValues(IPreferenceStore store);
		public abstract void performDefaults(IPreferenceStore store);
		public abstract void performOk(IPreferenceStore store);
		public abstract void setPreferences(Preferences preferences);

	}

	private class IconDecoratorTab extends Tab implements SelectionListener {
		
		private final Button fDirty, fHasRemote, fAdded, fNewResource;
		
		public IconDecoratorTab(TabFolder parent) {

			final Composite composite= SWTUtils.createHFillComposite(parent, SWTUtils.MARGINS_DEFAULT);
			
			fDirty= SWTUtils.createCheckBox(composite, CVSUIMessages.CVSDecoratorPreferencesPage_2); 
			fDirty.addSelectionListener(this);
			
			fHasRemote= SWTUtils.createCheckBox(composite, CVSUIMessages.CVSDecoratorPreferencesPage_3); 
			fHasRemote.addSelectionListener(this);
			
			fAdded= SWTUtils.createCheckBox(composite, CVSUIMessages.CVSDecoratorPreferencesPage_4); 
			fAdded.addSelectionListener(this);
			
			fNewResource= SWTUtils.createCheckBox(composite, CVSUIMessages.CVSDecoratorPreferencesPage_5); 
			fNewResource.addSelectionListener(this); 
			
			final TabItem item= new TabItem(parent, SWT.NONE);
			item.setText(CVSUIMessages.CVSDecoratorPreferencesPage_6);		 
			item.setControl(composite);	
		}
		
		@Override
		public void widgetSelected(SelectionEvent e) {
			setChanged();
			notifyObservers();
		}
		
		@Override
		public void initializeValues(IPreferenceStore store) {
			fDirty.setSelection(store.getBoolean(ICVSUIConstants.PREF_SHOW_DIRTY_DECORATION));
			fAdded.setSelection(store.getBoolean(ICVSUIConstants.PREF_SHOW_ADDED_DECORATION));
			fHasRemote.setSelection(store.getBoolean(ICVSUIConstants.PREF_SHOW_HASREMOTE_DECORATION));
			fNewResource.setSelection(store.getBoolean(ICVSUIConstants.PREF_SHOW_NEWRESOURCE_DECORATION));
		}
		
		@Override
		public void performOk(IPreferenceStore store) {
			store.setValue(ICVSUIConstants.PREF_SHOW_DIRTY_DECORATION, fDirty.getSelection());
			store.setValue(ICVSUIConstants.PREF_SHOW_ADDED_DECORATION, fAdded.getSelection());
			store.setValue(ICVSUIConstants.PREF_SHOW_HASREMOTE_DECORATION, fHasRemote.getSelection());
			store.setValue(ICVSUIConstants.PREF_SHOW_NEWRESOURCE_DECORATION, fNewResource.getSelection());
		}
		
		@Override
		public void performDefaults(IPreferenceStore store) {
			fDirty.setSelection(store.getDefaultBoolean(ICVSUIConstants.PREF_SHOW_DIRTY_DECORATION));
			fAdded.setSelection(store.getDefaultBoolean(ICVSUIConstants.PREF_SHOW_ADDED_DECORATION));
			fHasRemote.setSelection(store.getDefaultBoolean(ICVSUIConstants.PREF_SHOW_HASREMOTE_DECORATION));
			fNewResource.setSelection(store.getDefaultBoolean(ICVSUIConstants.PREF_SHOW_NEWRESOURCE_DECORATION));
		}
		
		@Override
		public void setPreferences(Preferences prefs) {
			prefs.setDefault(ICVSUIConstants.PREF_SHOW_DIRTY_DECORATION, fDirty.getSelection());
			prefs.setDefault(ICVSUIConstants.PREF_SHOW_ADDED_DECORATION, fAdded.getSelection());
			prefs.setDefault(ICVSUIConstants.PREF_SHOW_HASREMOTE_DECORATION, fHasRemote.getSelection());
			prefs.setDefault(ICVSUIConstants.PREF_SHOW_NEWRESOURCE_DECORATION, fNewResource.getSelection());
		}

		@Override
		public void widgetDefaultSelected(SelectionEvent e) {
		}
	}	
	
	private class TextDecoratorTab extends Tab implements ModifyListener {
		
		private final FormatEditor fFileDecoration;
		private final FormatEditor fFolderDecoration;	
		private final FormatEditor fProjectDecoration;
		
		private final Text dirtyFlag;
		private final Text addedFlag;
		
		public TextDecoratorTab(TabFolder parent) {
			
			final Composite composite= SWTUtils.createHFillComposite(parent, SWTUtils.MARGINS_DEFAULT, 3);
			Dialog.applyDialogFont(composite);
			
			fFileDecoration= new FormatEditor(composite, CVSUIMessages.CVSDecoratorPreferencesPage_7, CVSUIMessages.CVSDecoratorPreferencesPage_8, BINDINGS, ICVSUIConstants.PREF_FILETEXT_DECORATION);   // 
			fFolderDecoration= new FormatEditor(composite, CVSUIMessages.CVSDecoratorPreferencesPage_9, CVSUIMessages.CVSDecoratorPreferencesPage_10, FOLDER_BINDINGS, ICVSUIConstants.PREF_FOLDERTEXT_DECORATION);   // 
			fProjectDecoration= new FormatEditor(composite, CVSUIMessages.CVSDecoratorPreferencesPage_11, CVSUIMessages.CVSDecoratorPreferencesPage_12, FOLDER_BINDINGS, ICVSUIConstants.PREF_PROJECTTEXT_DECORATION); // 
			
			fFileDecoration.addModifyListener(this);
			fFolderDecoration.addModifyListener(this);
			fProjectDecoration.addModifyListener(this);

			SWTUtils.createPlaceholder(composite, 1, 3); 
			final Label dirtyLabel= SWTUtils.createLabel(composite, CVSUIMessages.CVSDecoratorPreferencesPage_13, 1); 
			dirtyLabel.setLayoutData(new GridData());
			
			dirtyFlag = new Text(composite, SWT.BORDER);
			dirtyFlag.setLayoutData(SWTUtils.createHFillGridData(1));
			dirtyFlag.addModifyListener(this);
			SWTUtils.createPlaceholder(composite, 1, 1);
			

			final Label addedLabel= SWTUtils.createLabel(composite, CVSUIMessages.CVSDecoratorPreferencesPage_14, 1); 
			addedLabel.setLayoutData(new GridData());

			addedFlag = new Text(composite, SWT.BORDER);
			addedFlag.setLayoutData(SWTUtils.createHFillGridData(1));
			addedFlag.addModifyListener(this);
			SWTUtils.createPlaceholder(composite, 1, 1);
			
			SWTUtils.createPlaceholder(composite, 1, 3);

			final TabItem item= new TabItem(parent, SWT.NONE);
			item.setText(CVSUIMessages.CVSDecoratorPreferencesPage_15);		 
			item.setControl(composite);	
		}
		
		@Override
		public void initializeValues(IPreferenceStore store) {
			fFileDecoration.initializeValue(store);
			fFolderDecoration.initializeValue(store);
			fProjectDecoration.initializeValue(store);
			addedFlag.setText(store.getString(ICVSUIConstants.PREF_ADDED_FLAG));
			dirtyFlag.setText(store.getString(ICVSUIConstants.PREF_DIRTY_FLAG));
		}
		
		@Override
		public void performOk(IPreferenceStore store) {
			fFileDecoration.performOk(store);
			fFolderDecoration.performOk(store);
			fProjectDecoration.performOk(store);
			store.setValue(ICVSUIConstants.PREF_ADDED_FLAG, addedFlag.getText());
			store.setValue(ICVSUIConstants.PREF_DIRTY_FLAG, dirtyFlag.getText());
		}
		
		@Override
		public void performDefaults(IPreferenceStore store) {
			fFileDecoration.performDefaults(store);
			fFolderDecoration.performDefaults(store);
			fProjectDecoration.performDefaults(store);
			
			addedFlag.setText(store.getDefaultString(ICVSUIConstants.PREF_ADDED_FLAG));
			dirtyFlag.setText(store.getDefaultString(ICVSUIConstants.PREF_DIRTY_FLAG));
		}

		public String getFileTextFormat() {
			return fFileDecoration.getText();
		}

		public String getFolderTextFormat() {
			return fFolderDecoration.getText();
		}

		public String getProjectTextFormat() {
			return fProjectDecoration.getText();
		}

		@Override
		public void modifyText(ModifyEvent e) {
			setChanged();
			notifyObservers();
		}

		@Override
		public void setPreferences(Preferences prefs) {
			prefs.setDefault(ICVSUIConstants.PREF_CALCULATE_DIRTY, true);
			prefs.setDefault(ICVSUIConstants.PREF_DIRTY_FLAG, dirtyFlag.getText());
			prefs.setDefault(ICVSUIConstants.PREF_ADDED_FLAG, addedFlag.getText());
		}
	}
	
	private class GeneralTab extends Tab implements SelectionListener {
		private final Button fShowDirty;
		private final Button fUseFontDecorations;

		public GeneralTab(TabFolder parent) {
			final Composite composite= SWTUtils.createHFillComposite(parent, SWTUtils.MARGINS_DEFAULT);
			Dialog.applyDialogFont(composite);
			
			SWTUtils.createPreferenceLink((IWorkbenchPreferenceContainer) getContainer(), composite, CVSUIMessages.CVSDecoratorPreferencesPage_36, CVSUIMessages.CVSDecoratorPreferencesPage_37); 
			
			fShowDirty= SWTUtils.createCheckBox(composite, CVSUIMessages.CVSDecoratorPreferencesPage_16); 
			SWTUtils.createLabel(composite, CVSUIMessages.CVSDecoratorPreferencesPage_17); 
			
			fUseFontDecorations= SWTUtils.createCheckBox(composite, CVSUIMessages.CVSDecoratorPreferencesPage_18); 

			SWTUtils.createPreferenceLink((IWorkbenchPreferenceContainer) getContainer(), composite, CVSUIMessages.CVSDecoratorPreferencesPage_19, CVSUIMessages.CVSDecoratorPreferencesPage_20); 

			fShowDirty.addSelectionListener(this);
			fUseFontDecorations.addSelectionListener(this);

			final TabItem item= new TabItem(parent, SWT.NONE);
			item.setText(CVSUIMessages.CVSDecoratorPreferencesPage_21);		 
			item.setControl(composite);	
		}

		@Override
		public void widgetSelected(SelectionEvent e) {
			setChanged();
			notifyObservers();
		}
		
		@Override
		public void widgetDefaultSelected(SelectionEvent e) {
		}

		@Override
		public void initializeValues(IPreferenceStore store) {
			fShowDirty.setSelection(store.getBoolean(ICVSUIConstants.PREF_CALCULATE_DIRTY));
			fUseFontDecorations.setSelection(store.getBoolean(ICVSUIConstants.PREF_USE_FONT_DECORATORS));
		}

		@Override
		public void performDefaults(IPreferenceStore store) {
			fShowDirty.setSelection(store.getDefaultBoolean(ICVSUIConstants.PREF_CALCULATE_DIRTY));
			fUseFontDecorations.setSelection(store.getDefaultBoolean(ICVSUIConstants.PREF_USE_FONT_DECORATORS));
		}
		
		@Override
		public void performOk(IPreferenceStore store) {
			store.setValue(ICVSUIConstants.PREF_CALCULATE_DIRTY, fShowDirty.getSelection());
			store.setValue(ICVSUIConstants.PREF_USE_FONT_DECORATORS, fUseFontDecorations.getSelection());
		}
		
		@Override
		public void setPreferences(Preferences preferences) {
			preferences.setValue(ICVSUIConstants.PREF_CALCULATE_DIRTY, fShowDirty.getSelection());
			preferences.setValue(ICVSUIConstants.PREF_USE_FONT_DECORATORS, fUseFontDecorations.getSelection());
		}
		
		public boolean isFontDecorationEnabled() {
			return this.fUseFontDecorations.getEnabled();
		}
		
	}
	
	public class Preview extends LabelProvider implements Observer, ITreeContentProvider {
		
		private final ResourceManager fImageCache;
		private final TreeViewer fViewer; 
		
		public Preview(Composite composite) {
			SWTUtils.createLabel(composite, CVSUIMessages.CVSDecoratorPreferencesPage_39);
			fImageCache= new LocalResourceManager(JFaceResources.getResources());
			fViewer = new TreeViewer(composite);
			fViewer.getControl().setLayoutData(SWTUtils.createHVFillGridData());
			fViewer.setContentProvider(this);
			fViewer.setLabelProvider(this);
			fViewer.setInput(ROOT);
			fViewer.expandAll();
		}
		
		public void refresh() {
			fViewer.refresh(true);
			setColorsAndFonts();
		}
		
		private void setColorsAndFonts() {
			TreeItem[] items = fViewer.getTree().getItems();
			setColorsAndFonts(items);
		}
		
		private void setColorsAndFonts(TreeItem[] items) {
			for (TreeItem item : items) {
				if (fGeneralTab.isFontDecorationEnabled()) {
					Color backGroundColor = getBackground(item.getData());
					item.setBackground(backGroundColor);
					Color foreGroundColor = getForeground(item.getData());
					item.setForeground(foreGroundColor);
					Font font = getFont(item.getData());
					item.setFont(font);
				} else {
					item.setBackground(null);
					item.setForeground(null);
					item.setFont(null);
				}
				setColorsAndFonts(item.getItems());
			}
		}
		
		@Override
		public void update(Observable o, Object arg) {
			refresh();
		}
		
		@Override
		public Object[] getChildren(Object parentElement) {
			return ((PreviewFile)parentElement).children.toArray();
		}

		@Override
		public Object getParent(Object element) {
			return null;
		}

		@Override
		public boolean hasChildren(Object element) {
			return !((PreviewFile)element).children.isEmpty();
		}

		@Override
		public Object[] getElements(Object inputElement) {
			return ((Collection)inputElement).toArray();
		}

		@Override
		public void dispose() {
			fImageCache.dispose();
		}

		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		}

		public Color getBackground(Object element) {
			return getDecoration(element).getBackgroundColor();
		}
		
		public Color getForeground(Object element) {
			return getDecoration(element).getForegroundColor();
		}
			
		public Font getFont(Object element) {
			return getDecoration(element).getFont();
		}
		
		@Override
		public String getText(Object element) {
			final CVSDecoration decoration = getDecoration(element);
			final StringBuilder buffer = new StringBuilder();
			final String prefix = decoration.getPrefix();
			if (prefix != null)
				buffer.append(prefix);
			buffer.append(((PreviewFile)element).name);
			final String suffix = decoration.getSuffix();
			if (suffix != null)
				buffer.append(suffix);
			return buffer.toString();
		}
		
		public CVSDecoration getDecoration(Object element) {
			final CVSDecoration decoration = buildDecoration((PreviewFile)element);
			((PreviewFile)element).configureDecoration(decoration);
			decoration.compute();
			return decoration;
		}
		
		@Override
		public Image getImage(Object element) {
			final String s;
			switch (((PreviewFile)element).type) {
			case IResource.PROJECT:
				s= SharedImages.IMG_OBJ_PROJECT; break;
			case IResource.FOLDER:
				s= ISharedImages.IMG_OBJ_FOLDER; break;
			default:
				s= ISharedImages.IMG_OBJ_FILE; break;
			}
			final Image baseImage= PlatformUI.getWorkbench().getSharedImages().getImage(s);
			final ImageDescriptor overlay = getDecoration(element).getOverlay();
			if (overlay == null)
				return baseImage;
			try {
				return fImageCache.createImage(new DecorationOverlayIcon(baseImage, overlay, IDecoration.BOTTOM_RIGHT));
			} catch (DeviceResourceException e) {
				CVSUIPlugin.log(new Status(IStatus.ERROR, CVSUIPlugin.ID, 0, "Error creating decorator image", e)); //$NON-NLS-1$
			}
			return null;
		}
	}
	
	private static class ThemeListener implements IPropertyChangeListener {

		private final Preview fPreview;
		
		ThemeListener(Preview preview) {
			fPreview= preview;
		}
		@Override
		public void propertyChange(PropertyChangeEvent event) {
			fPreview.refresh();
		}
	}
	
	protected static final Collection ROOT;
	protected static final Map BINDINGS;
	protected static final Map FOLDER_BINDINGS;

	static {
		BINDINGS= new HashMap();
		BINDINGS.put(CVSDecoratorConfiguration.RESOURCE_NAME, CVSUIMessages.CVSDecoratorPreferencesPage_22);  
		BINDINGS.put(CVSDecoratorConfiguration.RESOURCE_TAG, CVSUIMessages.CVSDecoratorPreferencesPage_23);  
		BINDINGS.put(CVSDecoratorConfiguration.FILE_KEYWORD, CVSUIMessages.CVSDecoratorPreferencesPage_24);  
		BINDINGS.put(CVSDecoratorConfiguration.FILE_REVISION, CVSUIMessages.CVSDecoratorPreferencesPage_25);  
		BINDINGS.put(CVSDecoratorConfiguration.NEW_DIRTY_FLAG, CVSUIMessages.CVSDecoratorPreferencesPage_26);  
		BINDINGS.put(CVSDecoratorConfiguration.ADDED_FLAG, CVSUIMessages.CVSDecoratorPreferencesPage_27); 
		
		FOLDER_BINDINGS= new HashMap();
		FOLDER_BINDINGS.put(CVSDecoratorConfiguration.RESOURCE_NAME, CVSUIMessages.CVSDecoratorPreferencesPage_28);  
		FOLDER_BINDINGS.put(CVSDecoratorConfiguration.RESOURCE_TAG, CVSUIMessages.CVSDecoratorPreferencesPage_29);  
		FOLDER_BINDINGS.put(CVSDecoratorConfiguration.REMOTELOCATION_HOST, CVSUIMessages.CVSDecoratorPreferencesPage_30);  
		FOLDER_BINDINGS.put(CVSDecoratorConfiguration.REMOTELOCATION_METHOD, CVSUIMessages.CVSDecoratorPreferencesPage_31);  
		FOLDER_BINDINGS.put(CVSDecoratorConfiguration.REMOTELOCATION_USER, CVSUIMessages.CVSDecoratorPreferencesPage_32);  
		FOLDER_BINDINGS.put(CVSDecoratorConfiguration.REMOTELOCATION_ROOT, CVSUIMessages.CVSDecoratorPreferencesPage_33);  
		FOLDER_BINDINGS.put(CVSDecoratorConfiguration.REMOTELOCATION_REPOSITORY, CVSUIMessages.CVSDecoratorPreferencesPage_34);  
		FOLDER_BINDINGS.put(CVSDecoratorConfiguration.REMOTELOCATION_LABEL, CVSUIMessages.CVSDecoratorPreferencesPage_38);  
		FOLDER_BINDINGS.put(CVSDecoratorConfiguration.NEW_DIRTY_FLAG, CVSUIMessages.CVSDecoratorPreferencesPage_35); 
		
		final PreviewFile project= new PreviewFile("Project", IResource.PROJECT, false, false, false, false, true, null, "v1_0"); //$NON-NLS-1$ //$NON-NLS-2$
		final ArrayList children= new ArrayList();
		children.add(new PreviewFile("Folder", IResource.FOLDER, false, false, false, false, true, null, null)); //$NON-NLS-1$
		children.add(new PreviewFile("ignored.txt", IResource.FILE, false, false, false, true, false, null, null)); //$NON-NLS-1$
		children.add(new PreviewFile("dirty.cpp", IResource.FILE, false, false, true, false, true, null, null)); //$NON-NLS-1$
		children.add(new PreviewFile("added.java", IResource.FILE, true, false, true, false, false, null, null)); //$NON-NLS-1$
		children.add(new PreviewFile("todo.txt", IResource.FILE, false, true, true, false, false, null, null)); //$NON-NLS-1$
		children.add(new PreviewFile("bugs.txt", IResource.FILE, false, false, true, false, true, null, null)); //$NON-NLS-1$
		children.add(new PreviewFile("archive.zip", IResource.FILE, false, false, true, false, true, Command.KSUBST_BINARY.getShortDisplayText(), null)); //$NON-NLS-1$
		project.children= children;
		ROOT= Collections.singleton(project);
	}

		
	private TextDecoratorTab fTextTab;
	private Tab fIconTab;
	private GeneralTab fGeneralTab;

	private Preview fPreview;
	private ThemeListener fThemeListener;
	
	@Override
	protected Control createContents(Composite parent) {
		
		final Composite composite= SWTUtils.createHVFillComposite(parent, SWTUtils.MARGINS_NONE);
		
		final Composite folderComposite= SWTUtils.createHFillComposite(composite, SWTUtils.MARGINS_NONE);
				
		// create a tab folder for the page
		final TabFolder tabFolder = new TabFolder(folderComposite, SWT.NONE);
		tabFolder.setLayoutData(SWTUtils.createHFillGridData());
		
		// text decoration options
		fGeneralTab= new GeneralTab(tabFolder);
		fTextTab= new TextDecoratorTab(tabFolder);
		fIconTab= new IconDecoratorTab(tabFolder);

		fPreview= new Preview(composite);
		
		fTextTab.addObserver(fPreview);
		fIconTab.addObserver(fPreview);
		fGeneralTab.addObserver(fPreview);

		initializeValues();
		PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(), IHelpContextIds.DECORATORS_PREFERENCE_PAGE);
		Dialog.applyDialogFont(parent);
		
		PlatformUI.getWorkbench().getThemeManager().addPropertyChangeListener(fThemeListener= new ThemeListener(fPreview));
		
		return tabFolder;
	}
	
	@Override
	public void dispose() {
		if (fThemeListener != null)
			PlatformUI.getWorkbench().getThemeManager().removePropertyChangeListener(fThemeListener);
		if (fPreview != null)
			fPreview.dispose();
	}
	
	/**
	 * Initializes states of the controls from the preference store.
	 */
	private void initializeValues() {
		final IPreferenceStore store = getPreferenceStore();
		fTextTab.initializeValues(store);
		fIconTab.initializeValues(store);
		fGeneralTab.initializeValues(store);
		fPreview.refresh();
		setValid(true);
	}

	@Override
	public void init(IWorkbench workbench) {
	}

	/**
	 * OK was clicked. Store the CVS preferences.
	 *
	 * @return whether it is okay to close the preference page
	 */
	@Override
	public boolean performOk() {
		final IPreferenceStore store = getPreferenceStore();
		fTextTab.performOk(store);
		fIconTab.performOk(store);
		fGeneralTab.performOk(store);
		if (store.needsSaving()) {
			CVSUIPlugin.broadcastPropertyChange(new PropertyChangeEvent(this, CVSUIPlugin.P_DECORATORS_CHANGED, null, null));
			CVSUIPlugin.getPlugin().savePluginPreferences();
		}
		return true;
	}

	/**
	 * Defaults was clicked. Restore the CVS preferences to
	 * their default values
	 */
	@Override
	protected void performDefaults() {
		final IPreferenceStore store = getPreferenceStore();
		
		fTextTab.performDefaults(store);
		fIconTab.performDefaults(store);
		fGeneralTab.performDefaults(store);
		fPreview.refresh();
		super.performDefaults();
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
	
	
	public CVSDecoration buildDecoration(PreviewFile file) {
		final Preferences prefs = new Preferences();
		
		fIconTab.setPreferences(prefs);
		fTextTab.setPreferences(prefs);
		fGeneralTab.setPreferences(prefs);
		
		final CVSDecoration decoration= new CVSDecoration(prefs, fTextTab.getFileTextFormat(), fTextTab.getFolderTextFormat(), fTextTab.getProjectTextFormat());
		
		decoration.setKeywordSubstitution(Command.KSUBST_TEXT.getShortDisplayText());
		decoration.setRevision("1.45");  //$NON-NLS-1$
		try {
			decoration.setLocation(CVSRepositoryLocation.fromString(":pserver:alize@cvs.site.org:/home/cvsroot"));  //$NON-NLS-1$
		} catch (CVSException e) {
			// continue without a location, since the location is hard coded an exception should never occur
		}
		return decoration;
	}
}
