/*******************************************************************************
 * Copyright (c) 2000, 2017 IBM Corporation and others.
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
package org.eclipse.team.internal.ui.preferences;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.team.core.IFileContentManager;
import org.eclipse.team.core.IStringMapping;
import org.eclipse.team.core.Team;
import org.eclipse.team.internal.ui.IHelpContextIds;
import org.eclipse.team.internal.ui.PixelConverter;
import org.eclipse.team.internal.ui.SWTUtils;
import org.eclipse.team.internal.ui.TeamUIMessages;
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.team.internal.ui.preferences.FileTypeTable.Item;
import org.eclipse.team.ui.TeamUI;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.PlatformUI;
/**
 * This preference page displays all patterns which determine whether a resource
 * is to be treated as a text file or not. The page allows the user to add or
 * remove entries from this table, and change their values from Text to Binary.
 */
public class TextPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

	private static final class ExtensionValidator implements IInputValidator {
		@Override
		public String isValid(String newText) {
			if (newText.trim().length() == 0)
				return ""; //$NON-NLS-1$
			if (!isComplete(newText)){
				return TeamUIMessages.TextPreferencePage_ExtensionNotCompleted;
			}
			newText = strip(newText);
			if (newText.indexOf('*') >= 0)
				return TeamUIMessages.TextPreferencePage_2;
			if (newText.indexOf('.') >= 0)
				return TeamUIMessages.TextPreferencePage_3;
			return null;
		}

		/**
		 * @param newText
		 * @return the text
		 */
		public String strip(String newText) {
			newText= newText.trim();
			if (newText.startsWith("*")) //$NON-NLS-1$
				newText= newText.substring(1);
			if (newText.startsWith(".")) //$NON-NLS-1$
				newText= newText.substring(1);
			return newText;
		}

		public boolean isComplete(String text){
			//Allowed formats of extension are:
			// extension
			// .extension
			// *.extension
			if (text.equals("*") || text.equals("*.") || text.equals(".")){  //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
				return false;
			}
			return true;
		}
	}

	private static final class FilenameValidator implements IInputValidator {
		@Override
		public String isValid(String newText) {
			if (newText.trim().length() == 0)
				return ""; //$NON-NLS-1$
			if (newText.indexOf('*') >= 0)
				return TeamUIMessages.TextPreferencePage_5;
			return null;
		}
	}

	// The input for the table viewer
	private final List<Item> fItems;

	// Widgets
	private Button fRemoveButton;
	private Button fChangeButton;

	protected FileTypeTable fTable;

	public TextPreferencePage() {
		fItems= new ArrayList<>();
		initializeItems();
	}

	private void initializeItems() {

		fItems.clear();

		final IFileContentManager manager= Team.getFileContentManager();

		final IStringMapping [] extensionInfoArray= manager.getExtensionMappings();
		final IStringMapping [] nameInfoArray= manager.getNameMappings();

		Set fPluginNames= makeSetOfStrings(manager.getDefaultNameMappings());
		Set fPluginExtensions= makeSetOfStrings(manager.getDefaultExtensionMappings());

		for (IStringMapping info : extensionInfoArray) {
			final FileTypeTable.Extension extension= new FileTypeTable.Extension(info.getString(), fPluginExtensions.contains(info.getString()));
			extension.mode= info.getType();
			fItems.add(extension);
		}
		for (IStringMapping info : nameInfoArray) {
			final FileTypeTable.Name name= new FileTypeTable.Name(info.getString(), fPluginNames.contains(info.getString()));
			name.mode= info.getType();
			fItems.add(name);
		}

	}

	private static Set<String> makeSetOfStrings(IStringMapping [] mappings) {
		final Set<String> set= new HashSet<>(mappings.length);
		for (IStringMapping mapping : mappings) {
			set.add(mapping.getString());
		}
		return set;
	}

	@Override
	public void init(IWorkbench workbench) {
	}

	@Override
	protected Control createContents(Composite parent) {

		initializeDialogUnits(parent);

		final PixelConverter converter= SWTUtils.createDialogPixelConverter(parent);

		final Composite composite= SWTUtils.createHVFillComposite(parent, SWTUtils.MARGINS_NONE, 2);

		fTable= new FileTypeTable(composite, fItems, false);

		fTable.getViewer().getControl().addListener(SWT.Selection, e -> handleSelection());

		fTable.getViewer().addDoubleClickListener(event -> {
			final ISelection selection = event.getSelection();
			if (selection == null || !(selection instanceof IStructuredSelection)) {
				return;
			}
			fTable.getViewer().editElement(((IStructuredSelection)selection).getFirstElement(), 1);
		});

		final Composite buttonsComposite = new Composite(composite, SWT.NONE);
		buttonsComposite.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		buttonsComposite.setLayout(SWTUtils.createGridLayout(1, converter, SWTUtils.MARGINS_NONE));

		final Button addExtensionButton = new Button(buttonsComposite, SWT.PUSH);
		addExtensionButton.setText(TeamUIMessages.TextPreferencePage_add);
		final Button addNameButton = new Button(buttonsComposite, SWT.PUSH);
		addNameButton.setText(TeamUIMessages.TextPreferencePage_0);
		fChangeButton = new Button(buttonsComposite, SWT.PUSH);
		fChangeButton.setText(TeamUIMessages.TextPreferencePage_change);
		fRemoveButton= new Button(buttonsComposite, SWT.PUSH);
		fRemoveButton.setText(TeamUIMessages.TextPreferencePage_remove);

		final var label = new Label(composite, SWT.WRAP);
		label.setText(TeamUIMessages.TextPreferencePage_1);
		label.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 2, 1));

		/**
		 * Calculate and set the button size
		 */
		final int buttonWidth= SWTUtils.calculateControlSize(converter, new Button [] { addExtensionButton, addNameButton, fChangeButton, fRemoveButton });
		addExtensionButton.setLayoutData(SWTUtils.createGridData(buttonWidth, SWT.DEFAULT, SWT.FILL, SWT.FILL, false, false));
		addNameButton.setLayoutData(SWTUtils.createGridData(buttonWidth, SWT.DEFAULT, SWT.FILL, SWT.FILL, false, false));
		fChangeButton.setLayoutData(SWTUtils.createGridData(buttonWidth, SWT.DEFAULT, SWT.FILL, SWT.FILL, false, false));
		fRemoveButton.setLayoutData(SWTUtils.createGridData(buttonWidth, SWT.DEFAULT, SWT.FILL, SWT.FILL, false, false));

		addExtensionButton.addListener(SWT.Selection, e -> addExtension());
		addNameButton.addListener(SWT.Selection, e -> addName());

		fChangeButton.setEnabled(false);
		fChangeButton.addListener(SWT.Selection, e -> changePattern());

		fRemoveButton.setEnabled(false);
		fRemoveButton.addListener(SWT.Selection, e -> removePattern());

		// set F1 help
		PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(), IHelpContextIds.FILE_TYPE_PREFERENCE_PAGE);

		return composite;
	}

	@Override
	protected void performDefaults() {
		super.performDefaults();
		initializeItems();
		if (fTable != null)
			fTable.getViewer().refresh();
	}

	/**
	 * Do anything necessary because the OK button has been pressed.
	 *
	 * @return whether it is okay to close the preference page
	 */
	@Override
	public boolean performOk() {
		final ArrayList<String> extensionsList= new ArrayList<>();
		final ArrayList<Integer> extensionsModesList= new ArrayList<>();

		final ArrayList<String> namesList= new ArrayList<>();
		final ArrayList<Integer> namesModesList= new ArrayList<>();

		for (Object element : fItems) {
			final FileTypeTable.Item item= (FileTypeTable.Item) element;

			if (item instanceof FileTypeTable.Extension) {
				extensionsList.add(item.name);
				extensionsModesList.add(Integer.valueOf(item.mode));
			} else if (item instanceof FileTypeTable.Name) {
				namesList.add(item.name);
				namesModesList.add(Integer.valueOf(item.mode));
			}
		}

		final String [] extensions= extensionsList.toArray(new String [extensionsList.size()]);
		final String [] names= namesList.toArray(new String [namesList.size()]);

		final int [] extensionsModes= integerListToIntArray(extensionsModesList);
		final int [] namesModes= integerListToIntArray(namesModesList);

		Team.getFileContentManager().setExtensionMappings(extensions, extensionsModes);
		Team.getFileContentManager().setNameMappings(names, namesModes);

		TeamUIPlugin.broadcastPropertyChange(new PropertyChangeEvent(this, TeamUI.GLOBAL_FILE_TYPES_CHANGED, null, null));

		return true;
	}

	private static int [] integerListToIntArray(List integers) {
		final int [] array= new int [integers.size()];
		int index= 0;
		for (Object element : integers)
			array[index++]= ((Integer)element).intValue();
		return array;
	}

	/**
	 * Add a new item to the table with the default type of Text.
	 */
	void addExtension() {
		final ExtensionValidator validator = new ExtensionValidator();
		final InputDialog dialog = new InputDialog(getShell(), TeamUIMessages.TextPreferencePage_enterExtensionShort, TeamUIMessages.TextPreferencePage_enterExtensionLong, null, validator); //
		if (dialog.open() != Window.OK)
			return;

		final String extension = validator.strip(dialog.getValue());

		// Check if the item already exists
		final Iterator it = fItems.iterator();
		while (it.hasNext()) {
			final FileTypeTable.Item item= (FileTypeTable.Item)it.next();
			if (item instanceof FileTypeTable.Extension && item.name.equals(extension)) {
				MessageDialog.openWarning(getShell(), TeamUIMessages.TextPreferencePage_extensionExistsShort, TeamUIMessages.TextPreferencePage_extensionExistsLong); //
				return;
			}
		}
		final FileTypeTable.Item item= new FileTypeTable.Extension(extension, false);
		fItems.add(item);
		fTable.getViewer().refresh();
	}

	/**
	 * Add a new item to the table with the default type of Text.
	 */
	void addName() {
		final InputDialog dialog = new InputDialog(getShell(), TeamUIMessages.TextPreferencePage_6, TeamUIMessages.TextPreferencePage_7, null, new FilenameValidator()); //
		if (dialog.open() != Window.OK)
			return;

		final String name = dialog.getValue();

		// Check if the item already exists
		final Iterator it = fItems.iterator();
		while (it.hasNext()) {
			final FileTypeTable.Item item= (FileTypeTable.Item)it.next();
			if (item instanceof FileTypeTable.Name && item.name.equals(name)) {
				MessageDialog.openWarning(getShell(), TeamUIMessages.TextPreferencePage_extensionExistsShort, TeamUIMessages.TextPreferencePage_extensionExistsLong); //
				return;
			}
		}
		final FileTypeTable.Item item= new FileTypeTable.Name(name, false);
		fItems.add(item);
		fTable.getViewer().refresh();
	}

	/**
	 * Remove the selected items from the table
	 */
	void removePattern() {
		final IStructuredSelection selection = fTable.getSelection();
		if (selection == null)
			return;

		for (Object element : selection) {
			final FileTypeTable.Item item= (FileTypeTable.Item)element;
			if (item.contributed)
				continue;
			fItems.remove(item);
		}
		fTable.getViewer().refresh();
		handleSelection();
	}
	/**
	 * Toggle the selected items' content types
	 */
	void changePattern() {
		final IStructuredSelection selection = fTable.getSelection();
		if (selection == null)
			return;

		for (Object element : selection) {
			final FileTypeTable.Item item= (FileTypeTable.Item)element;
			item.mode= item.mode == Team.TEXT ? Team.BINARY : Team.TEXT;
			fTable.getViewer().refresh(item);
		}
	}

	/**
	 * The table viewer selection has changed. Update the remove and change button enablement.
	 */
	void handleSelection() {
		final boolean empty = fTable.getSelection().isEmpty();
		FileTypeTable.Item selectedItem = (Item) fTable.getSelection().getFirstElement();

		fRemoveButton.setEnabled(!empty && !selectedItem.contributed);
		fChangeButton.setEnabled(!empty);
	}
}
