/*******************************************************************************
 *  Copyright (c) 2002, 2019 IBM Corporation and others.
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
package org.eclipse.ui.internal.cheatsheets.dialogs;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.TrayDialog;
import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.activities.ITriggerPoint;
import org.eclipse.ui.activities.WorkbenchActivityHelper;
import org.eclipse.ui.cheatsheets.OpenCheatSheetAction;
import org.eclipse.ui.forms.widgets.ScrolledFormText;
import org.eclipse.ui.internal.cheatsheets.CheatSheetPlugin;
import org.eclipse.ui.internal.cheatsheets.ICheatSheetResource;
import org.eclipse.ui.internal.cheatsheets.Messages;
import org.eclipse.ui.internal.cheatsheets.data.ParserStatusUtility;
import org.eclipse.ui.internal.cheatsheets.registry.CheatSheetCollectionElement;
import org.eclipse.ui.internal.cheatsheets.registry.CheatSheetCollectionSorter;
import org.eclipse.ui.internal.cheatsheets.registry.CheatSheetElement;
import org.eclipse.ui.internal.cheatsheets.state.DefaultStateManager;
import org.eclipse.ui.internal.cheatsheets.views.CheatSheetView;
import org.eclipse.ui.internal.cheatsheets.views.ViewUtilities;
import org.eclipse.ui.model.BaseWorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchAdapter;
import org.osgi.framework.FrameworkUtil;

/**
 * Dialog to allow the user to select a cheat sheet from a list.
 */
public class CheatSheetCategoryBasedSelectionDialog extends TrayDialog //extends SelectionDialog
		implements ISelectionChangedListener {
	private static final String CHEAT_SHEET_SELECTION_HELP_ID = "org.eclipse.ui.cheatsheets.cheatSheetSelection"; //$NON-NLS-1$

	private IDialogSettings settings;

	private CheatSheetCollectionElement cheatsheetCategories;

	private CheatSheetElement currentSelection;

	private TreeViewer treeViewer;

	private ScrolledFormText desc;

	private Button showAllButton;

	private Button selectRegisteredRadio;

	private Button selectFileRadio;

	private Button selectUrlRadio;

	private Combo selectFileCombo;

	private Combo selectUrlCombo;

	private ActivityViewerFilter activityViewerFilter = new ActivityViewerFilter();

	private boolean okButtonState;

	// id constants

	private static final String DIALOG_SETTINGS_SECTION = "CheatSheetCategoryBasedSelectionDialog"; //$NON-NLS-1$

	private final static String STORE_EXPANDED_CATEGORIES_ID = "CheatSheetCategoryBasedSelectionDialog.STORE_EXPANDED_CATEGORIES_ID"; //$NON-NLS-1$

	private final static String STORE_SELECTED_CHEATSHEET_ID = "CheatSheetCategoryBasedSelectionDialog.STORE_SELECTED_CHEATSHEET_ID"; //$NON-NLS-1$

	private final static String STORE_RADIO_SETTING = "CheatSheetCategoryBasedSelectionDialog.STORE_RADIO_SELECTION"; //$NON-NLS-1$

	private final static String STORE_CHEATSHEET_FILENAME = "CheatSheetCategoryBasedSelectionDialog.STORE_CHEATSHEET_FILENAME"; //$NON-NLS-1$

	private final static String STORE_CHEATSHEET_URL = "CheatSheetCategoryBasedSelectionDialog.STORE_CHEATSHEET_URL"; //$NON-NLS-1$

	private final static String STORE_URL_MRU = "CheatSheetCategoryBasedSelectionDialog.STORE_URL_MRU"; //$NON-NLS-1$
	private final static String STORE_FILE_MRU = "CheatSheetCategoryBasedSelectionDialog.STORE_FILE_MRU"; //$NON-NLS-1$

	private static final int MOST_RECENT_LENGTH = 3;
	private static final int RADIO_REGISTERED = 1;
	private static final int RADIO_FILE = 2;
	private static final int RADIO_URL = 3;

	private Button browseFileButton;

	private String title;

	private IStatus status = Status.OK_STATUS;


	List<String> mostRecentFiles = new ArrayList<>();
	List<String> mostRecentUrls = new ArrayList<>();

	private static class ActivityViewerFilter extends ViewerFilter {
		private boolean hasEncounteredFilteredItem = false;

		@Override
		public boolean select(Viewer viewer, Object parentElement,
				Object element) {
			if (WorkbenchActivityHelper.filterItem(element)) {
				setHasEncounteredFilteredItem(true);
				return false;
			}
			return true;
		}

		/**
		 * @return returns whether the filter has filtered an item
		 */
		public boolean getHasEncounteredFilteredItem() {
			return hasEncounteredFilteredItem;
		}

		/**
		 * @param sets
		 *            whether the filter has filtered an item
		 */
		public void setHasEncounteredFilteredItem(
				boolean hasEncounteredFilteredItem) {
			this.hasEncounteredFilteredItem = hasEncounteredFilteredItem;
		}
	}

	private class CheatsheetLabelProvider extends LabelProvider {
		@Override
		public String getText(Object obj) {
			if (obj instanceof WorkbenchAdapter) {
				return ((WorkbenchAdapter) obj).getLabel(null);
			}
			return super.getText(obj);
		}

		@Override
		public Image getImage(Object obj) {
			if (obj instanceof CheatSheetElement) {
				CheatSheetElement element = (CheatSheetElement)obj;
				if (element.isComposite()) {
					return CheatSheetPlugin.getPlugin().getImageRegistry().get(
							ICheatSheetResource.COMPOSITE_OBJ);
				}
				return CheatSheetPlugin.getPlugin().getImageRegistry().get(
						ICheatSheetResource.CHEATSHEET_OBJ);
			}
			return PlatformUI.getWorkbench().getSharedImages().getImage(
					ISharedImages.IMG_OBJ_FOLDER);
		}
	}

	/**
	 * Creates an instance of this dialog to display the a list of cheat sheets.
	 *
	 * @param shell
	 *            the parent shell
	 */
	public CheatSheetCategoryBasedSelectionDialog(Shell shell,
			CheatSheetCollectionElement cheatsheetCategories) {
		super(shell);

		this.cheatsheetCategories = cheatsheetCategories;

		this.title = Messages.CHEAT_SHEET_SELECTION_DIALOG_TITLE;

		setShellStyle(getShellStyle() | SWT.RESIZE);
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		if (title != null) {
			newShell.setText(title);
		}
		newShell.setImage(CheatSheetPlugin.getPlugin().getImage(ICheatSheetResource.CHEATSHEET_VIEW));
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		super.createButtonsForButtonBar(parent);

		enableOKButton(okButtonState);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		initializeDialogUnits(parent);

		IDialogSettings workbenchSettings = PlatformUI
				.getDialogSettingsProvider(FrameworkUtil.getBundle(CheatSheetCategoryBasedSelectionDialog.class))
				.getDialogSettings();
		IDialogSettings dialogSettings = workbenchSettings
				.getSection(DIALOG_SETTINGS_SECTION);
		if (dialogSettings == null)
			dialogSettings = workbenchSettings
					.addNewSection(DIALOG_SETTINGS_SECTION);

		setDialogSettings(dialogSettings);

		// top level group
		Composite outerContainer = (Composite) super.createDialogArea(parent);

		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, CHEAT_SHEET_SELECTION_HELP_ID);
		GridLayout gridLayout = new GridLayout();
		gridLayout.marginWidth = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_MARGIN);
		gridLayout.marginHeight = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_MARGIN);
		gridLayout.horizontalSpacing = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_SPACING);
		gridLayout.verticalSpacing = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_SPACING);
		outerContainer.setLayout(gridLayout);
		outerContainer.setLayoutData(new GridData(GridData.FILL_BOTH));

		// Create label
		createMessageArea(outerContainer);

		// Create radio button
		selectRegisteredRadio = new Button(outerContainer, SWT.RADIO);
		selectRegisteredRadio.setText(Messages.SELECTION_DIALOG_OPEN_REGISTERED);

		SashForm sform = new SashForm(outerContainer, SWT.VERTICAL);
		GridData data = new GridData(GridData.FILL_BOTH);
		data.heightHint = 300;
		sform.setLayoutData(data);

		// category tree pane
		treeViewer = new TreeViewer(sform, SWT.SINGLE | SWT.H_SCROLL
				| SWT.V_SCROLL | SWT.BORDER);
		treeViewer.getTree().setLayoutData(data);
		treeViewer.setContentProvider(getCheatSheetProvider());
		treeViewer.setLabelProvider(new CheatsheetLabelProvider());
		treeViewer.setComparator(CheatSheetCollectionSorter.INSTANCE);
		treeViewer.addFilter(activityViewerFilter);
		treeViewer.addSelectionChangedListener(this);
		treeViewer.setInput(cheatsheetCategories);

		desc = new ScrolledFormText(sform, true);
		desc.setEnabled(false);

		sform.setWeights(new int[] {10, 2});

		if (activityViewerFilter.getHasEncounteredFilteredItem())
			createShowAllButton(outerContainer);

		// Add double-click listener
		treeViewer.addDoubleClickListener(event -> {
			IStructuredSelection selection = (IStructuredSelection) event.getSelection();
			Object obj = selection.getFirstElement();
			if (obj instanceof CheatSheetCollectionElement) {
				boolean state = treeViewer.getExpandedState(obj);
				treeViewer.setExpandedState(obj, !state);
			} else {
				okPressed();
			}
		});

		// Create radio button for select from file
		selectFileRadio = new Button(outerContainer, SWT.RADIO);
		selectFileRadio.setText(Messages.SELECTION_DIALOG_OPEN_FROM_FILE);

		Composite selectFileComposite = new Composite(outerContainer, SWT.NULL);
		GridLayout selectFileLayout = new GridLayout();
		selectFileLayout.marginWidth = 0;
		selectFileLayout.marginHeight = 0;
		selectFileLayout.horizontalSpacing = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_SPACING);
		selectFileLayout.verticalSpacing = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_SPACING);
		selectFileLayout.numColumns = 2;
		selectFileComposite.setLayout(selectFileLayout);
		GridData sfCompositeData = new GridData(GridData.FILL_HORIZONTAL);
		sfCompositeData.widthHint = 300;
		selectFileComposite.setLayoutData(sfCompositeData);
		selectFileCombo = new Combo(selectFileComposite, SWT.BORDER);
		GridData sfTextData = new GridData(GridData.FILL_HORIZONTAL);
		selectFileCombo.setLayoutData(sfTextData);
		browseFileButton = new Button(selectFileComposite, SWT.NULL);
		browseFileButton.setText(Messages.SELECTION_DIALOG_FILEPICKER_BROWSE);
		setButtonLayoutData(browseFileButton);

		// Create radio button for select from URL
		selectUrlRadio = new Button(outerContainer, SWT.RADIO);
		selectUrlRadio.setText(Messages.SELECTION_DIALOG_OPEN_FROM_URL);
		selectUrlCombo = new Combo(outerContainer, SWT.BORDER);
		GridData suTextData = new GridData(GridData.FILL_HORIZONTAL);
		selectUrlCombo.setLayoutData(suTextData);

		restoreWidgetValues();
		restoreFileSettings();

		if (!treeViewer.getSelection().isEmpty())
			// we only set focus if a selection was restored
			treeViewer.getTree().setFocus();

		Dialog.applyDialogFont(outerContainer);
		selectFileCombo.addModifyListener(new FileAndUrlListener());
		browseFileButton.addSelectionListener(new BrowseListener());
		selectRegisteredRadio.addSelectionListener(new RadioSelectionListener());
		selectUrlRadio.addSelectionListener(new RadioSelectionListener());
		selectUrlCombo.addModifyListener(new FileAndUrlListener());
		checkRadioButtons();
		return outerContainer;
	}

	private class RadioSelectionListener implements SelectionListener {

		@Override
		public void widgetSelected(SelectionEvent e) {
			checkRadioButtons();
		}

		@Override
		public void widgetDefaultSelected(SelectionEvent e) {
			// do nothing
		}
	}

	private class BrowseListener implements SelectionListener {

		@Override
		public void widgetSelected(SelectionEvent e) {
			// Launch a file dialog to select a cheatsheet file
			FileDialog fileDlg = new FileDialog(getShell());
			fileDlg.setFilterExtensions(new String[]{"*.xml"}); //$NON-NLS-1$
			fileDlg.setText(Messages.SELECTION_DIALOG_FILEPICKER_TITLE);
			fileDlg.open();
			String filename = fileDlg.getFileName();
			if (filename != null) {
				IPath folderPath = new Path(fileDlg.getFilterPath());
				IPath filePath = folderPath.append(filename);
				selectFileCombo.setText(filePath.toOSString());
				checkRadioButtons();
			}
		}

		@Override
		public void widgetDefaultSelected(SelectionEvent e) {
			// do nothing
		}
	}

	private class FileAndUrlListener implements ModifyListener {

		@Override
		public void modifyText(ModifyEvent e) {
			setOkButton();
		}
	}

	/*
	 * Check the state of the Radio buttons and disable those parts of the UI that don't apply
	 */
	private void checkRadioButtons() {
		selectFileCombo.setEnabled(selectFileRadio.getSelection());
		browseFileButton.setEnabled(selectFileRadio.getSelection());
		if (showAllButton != null) {
			showAllButton.setEnabled(selectRegisteredRadio.getSelection());
		}
		treeViewer.getTree().setEnabled(selectRegisteredRadio.getSelection());
		selectUrlCombo.setEnabled(selectUrlRadio.getSelection());
		setOkButton();
	}

	/**
	 * Create a show all button in the parent.
	 *
	 * @param parent
	 *            the parent <code>Composite</code>.
	 */
	private void createShowAllButton(Composite parent) {
		showAllButton = new Button(parent, SWT.CHECK);
		showAllButton
				.setText(Messages.CheatSheetCategoryBasedSelectionDialog_showAll);
		showAllButton.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				if (showAllButton.getSelection()) {
					treeViewer.resetFilters();
				} else {
					treeViewer.addFilter(activityViewerFilter);
				}
			}
		});
	}

	/**
	 * Method enableOKButton enables/diables the OK button for the dialog and
	 * saves the state, allowing the enabling/disabling to occur even if the
	 * button has not been created yet.
	 *
	 * @param value
	 */
	private void enableOKButton(boolean value) {
		Button button = getButton(IDialogConstants.OK_ID);

		okButtonState = value;
		if (button != null) {
			button.setEnabled(value);
		}
	}

	/**
	 * Expands the cheatsheet categories in this page's category viewer that
	 * were expanded last time this page was used. If a category that was
	 * previously expanded no longer exists then it is ignored.
	 */
	protected CheatSheetCollectionElement expandPreviouslyExpandedCategories() {
		String[] expandedCategoryPaths = settings
				.getArray(STORE_EXPANDED_CATEGORIES_ID);
		List<CheatSheetCollectionElement> categoriesToExpand = new ArrayList<>(expandedCategoryPaths.length);

		for (String expandedCategoryPath : expandedCategoryPaths) {
			CheatSheetCollectionElement category = cheatsheetCategories
					.findChildCollection(new Path(expandedCategoryPath));
			if (category != null) // ie.- it still exists
				categoriesToExpand.add(category);
		}

		if (!categoriesToExpand.isEmpty())
			treeViewer.setExpandedElements(categoriesToExpand.toArray());
		return categoriesToExpand.isEmpty() ? null
				: (CheatSheetCollectionElement) categoriesToExpand
						.get(categoriesToExpand.size() - 1);
	}

	/**
	 * Returns the content provider for this page.
	 */
	protected IContentProvider getCheatSheetProvider() {
		// want to get the cheatsheets of the collection element
		return new BaseWorkbenchContentProvider() {
			@Override
			public Object[] getChildren(Object o) {
				Object[] cheatsheets;
				Object[] subCategories;
				if (o instanceof CheatSheetCollectionElement) {
					cheatsheets = ((CheatSheetCollectionElement) o)
							.getCheatSheets();
					subCategories = ((CheatSheetCollectionElement) o).getChildren();
				} else {
					cheatsheets = new Object[0];
					subCategories = new Object[0];
				}

				if (cheatsheets.length == 0) {
					return subCategories;
				} else if (subCategories.length == 0) {
					return cheatsheets;
				} else {
					Object[] result = new Object[cheatsheets.length + subCategories.length];
					System.arraycopy(subCategories, 0, result, 0, subCategories.length);
					System.arraycopy(cheatsheets, 0, result, subCategories.length, cheatsheets.length);
					return result;
				}
			}
		};
	}

	/**
	 * Returns the single selected object contained in the passed
	 * selectionEvent, or <code>null</code> if the selectionEvent contains
	 * either 0 or 2+ selected objects.
	 */
	protected Object getSingleSelection(IStructuredSelection selection) {
		return selection.size() == 1 ? selection.getFirstElement() : null;
	}

	/**
	 * The user selected either new cheatsheet category(s) or cheatsheet
	 * element(s). Proceed accordingly.
	 *
	 * @param newSelection
	 *            ISelection
	 */
	@Override
	public void selectionChanged(SelectionChangedEvent selectionEvent) {
		Object obj = getSingleSelection(selectionEvent.getStructuredSelection());
		if (obj instanceof CheatSheetCollectionElement) {
			currentSelection = null;
		} else {
			currentSelection = (CheatSheetElement) obj;
		}

		String description;
		if (currentSelection != null) {
			description = currentSelection.getDescription();
		} else {
			description = ""; //$NON-NLS-1$
		}
		desc.getFormText().setText(description, false, false);
		desc.reflow(true);
		setOkButton();
	}

	private void setOkButton() {
		if (selectRegisteredRadio.getSelection()) {
			enableOKButton(currentSelection != null);
		} else if (selectFileRadio.getSelection() ){
			enableOKButton(selectFileCombo.getText().length() > 0);
		} else {
			enableOKButton(selectUrlCombo.getText().length() > 0);
		}
	}

	@Override
	protected void okPressed() {
		/*
		 * Prevent the cheat sheet from opening inside this dialog's tray
		 * because it is about to close.
		 */
		getShell().setVisible(false);

		if (selectFileRadio.getSelection()) {
			setResultFromFile();
		} else if (selectRegisteredRadio.getSelection() ){
			setResultFromTree();
		} else {
			setResultFromUrl();
		}

		// save our selection state
		saveWidgetValues();

		super.okPressed();
	}

	private void setResultFromTree() {
		if (currentSelection != null) {
			ITriggerPoint triggerPoint = PlatformUI.getWorkbench()
					.getActivitySupport().getTriggerPointManager()
					.getTriggerPoint(ICheatSheetResource.TRIGGER_POINT_ID);
			if (WorkbenchActivityHelper.allowUseOf(triggerPoint,
					currentSelection)) {
				OpenCheatSheetAction openCheatSheetAction = new OpenCheatSheetAction(currentSelection.getID());
				openCheatSheetAction.setTargetShell(getParentShell());
				openCheatSheetAction.run();
			}
		}
	}

	private void setResultFromFile() {
		// Use the filename without extension as the id of this cheatsheet
		IPath filePath = new Path(selectFileCombo.getText());
		String id = filePath.lastSegment();
		int extensionIndex = id.indexOf('.');
		if (extensionIndex > 0) {
			id = id.substring(0, extensionIndex);
		}
		// Use the id as the name
		URL url = null;
		boolean opened = false;

		try {
			File contentFile = new File(selectFileCombo.getText());
			url = contentFile.toURI().toURL();
			OpenCheatSheetAction openCheatSheetAction = new OpenCheatSheetAction(id, id, url);
			openCheatSheetAction.setTargetShell(getParentShell());
			openCheatSheetAction.run();
			opened = true;
		} catch (MalformedURLException e) {
			opened = false;
		}
		if (!opened) {
			String message = NLS.bind(Messages.ERROR_OPENING_FILE, (new Object[] {selectFileCombo.getText()}));
			status = new Status(IStatus.ERROR, ICheatSheetResource.CHEAT_SHEET_PLUGIN_ID, ParserStatusUtility.PARSER_ERROR, message, null);
			CheatSheetView view = ViewUtilities.showCheatSheetView();
			view.getCheatSheetViewer().showError(message);
		}
	}

	private void setResultFromUrl() {
		// Use the filename without extension as the id of this cheatsheet
		IPath filePath = new Path(selectUrlCombo.getText());
		String id = filePath.lastSegment();
		if (id == null) {
			id = ""; //$NON-NLS-1$
		}
		int extensionIndex = id.indexOf('.');
		if (extensionIndex > 0) {
			id = id.substring(0, extensionIndex);
		}
		// Use the id as the name
		URL url = null;
		boolean opened = false;
		CheatSheetView view = ViewUtilities.showCheatSheetView();
		if (view == null) {
			return;
		}
		try {
			url = new URL(selectUrlCombo.getText());
			view.getCheatSheetViewer().setInput(id, id, url, new DefaultStateManager(), true);
			opened = true;
		} catch (MalformedURLException e) {
			opened = false;
		}
		if (!opened) {
			String message = NLS.bind(Messages.ERROR_OPENING_FILE, (new Object[] {selectUrlCombo.getText()}));
			status = new Status(IStatus.ERROR, ICheatSheetResource.CHEAT_SHEET_PLUGIN_ID, ParserStatusUtility.PARSER_ERROR, message, null);
			view.getCheatSheetViewer().showError(message);
		}
	}

	/**
	 * Set's widgets to the values that they held last time this page was
	 * opened
	 */
	protected void restoreWidgetValues() {
		String[] expandedCategoryPaths = settings
				.getArray(STORE_EXPANDED_CATEGORIES_ID);
		if (expandedCategoryPaths == null)
			return; // no stored values

		CheatSheetCollectionElement category = expandPreviouslyExpandedCategories();
		if (category != null)
			selectPreviouslySelectedCheatSheet(category);
	}

	/**
	 * Restores the state of the radio button and file name fields
	 */
	private void restoreFileSettings() {
		int radioSetting = RADIO_REGISTERED;
		try {
			radioSetting = settings.getInt(STORE_RADIO_SETTING);
		}
		catch(NumberFormatException n) {
		}
		selectFileRadio.setSelection(radioSetting == RADIO_FILE);
		selectRegisteredRadio.setSelection(radioSetting == RADIO_REGISTERED);
		selectUrlRadio.setSelection(radioSetting == RADIO_URL);
		String fileName = settings.get(STORE_CHEATSHEET_FILENAME);
		if (fileName != null) {
			selectFileCombo.setText(fileName);
		}
		String url = settings.get(STORE_CHEATSHEET_URL);
		if (url != null) {
			selectUrlCombo.setText(url);
		}
		loadMRU(mostRecentUrls, STORE_URL_MRU, selectUrlCombo);
		loadMRU(mostRecentFiles, STORE_FILE_MRU, selectFileCombo);
	}

	private void loadMRU(List<String> mostRecentList, String key, Combo combo) {
		for (int i = 0; i < MOST_RECENT_LENGTH; i++) {
			String name = settings.get(key + i);
			if (name != null) {
				mostRecentList.add(name);
				combo.add(name);
			}
		}
	}

	private void saveMRU(List<String> mostRecentList, String key, String selection) {
		if (selection.length() > 0 && !mostRecentList.contains(selection)) {
			mostRecentList.add(0, selection);
		}
		for (int i = 0; i < MOST_RECENT_LENGTH && i < mostRecentList.size(); i++) {
			String name = mostRecentList.get(i);
			if (name.length() > 0) {
				settings.put(key + i, name);
			}
		}
	}

	/**
	 * Store the current values of self's widgets so that they can be restored
	 * in the next instance of self
	 *
	 */
	public void saveWidgetValues() {
		storeExpandedCategories();
		storeSelectedCheatSheet();
		storeFileSettings();
	}

	/**
	 * Selects the cheatsheet category and cheatsheet in this page that were
	 * selected last time this page was used. If a category or cheatsheet that
	 * was previously selected no longer exists then it is ignored.
	 */
	protected void selectPreviouslySelectedCheatSheet(
			CheatSheetCollectionElement category) {
		String cheatsheetId = settings.get(STORE_SELECTED_CHEATSHEET_ID);
		if (cheatsheetId == null)
			return;
		CheatSheetElement cheatsheet = category.findCheatSheet(cheatsheetId,
				false);
		if (cheatsheet == null)
			return; // cheatsheet no longer exists, or has moved

		treeViewer.setSelection(new StructuredSelection(cheatsheet));
	}

	/**
	 * Set the dialog store to use for widget value storage and retrieval
	 *
	 * @param settings
	 *            IDialogSettings
	 */
	public void setDialogSettings(IDialogSettings settings) {
		this.settings = settings;
	}

	/**
	 * Stores the collection of currently-expanded categories in this page's
	 * dialog store, in order to recreate this page's state in the next instance
	 * of this page.
	 */
	protected void storeExpandedCategories() {
		Object[] expandedElements = treeViewer.getExpandedElements();
		String[] expandedElementPaths = new String[expandedElements.length];
		for (int i = 0; i < expandedElements.length; ++i) {
			expandedElementPaths[i] = ((CheatSheetCollectionElement) expandedElements[i])
					.getPath().toString();
		}
		settings.put(STORE_EXPANDED_CATEGORIES_ID, expandedElementPaths);
	}

	/**
	 * Stores the currently-selected category and cheatsheet in this page's
	 * dialog store, in order to recreate this page's state in the next instance
	 * of this page.
	 */
	protected void storeSelectedCheatSheet() {
		CheatSheetElement element = null;

		Object el = getSingleSelection(treeViewer.getStructuredSelection());
		if (el == null)
			return;

		if (el instanceof CheatSheetElement) {
			element = (CheatSheetElement) el;
		} else
			return;

		settings.put(STORE_SELECTED_CHEATSHEET_ID, element.getID());
	}

	/**
	 * Stores the state of the radio button and file name fields
	 */
	private void storeFileSettings() {
		int radioSetting = 0;
		if (selectRegisteredRadio.getSelection()) {
			radioSetting = 1;
		}
		if (selectFileRadio.getSelection()) {
			radioSetting = 2;
		}
		if (selectUrlRadio.getSelection()) {
			radioSetting = 3;
		}
		settings.put(STORE_RADIO_SETTING, radioSetting);
		settings.put(STORE_CHEATSHEET_FILENAME, selectFileCombo.getText());
		settings.put(STORE_CHEATSHEET_URL, selectUrlCombo.getText());

		saveMRU(mostRecentUrls, STORE_URL_MRU, selectUrlCombo.getText());
		saveMRU(mostRecentFiles, STORE_FILE_MRU, selectFileCombo.getText());
	}

	@Override
	protected IDialogSettings getDialogBoundsSettings() {
		IDialogSettings settings = PlatformUI
				.getDialogSettingsProvider(FrameworkUtil.getBundle(CheatSheetCategoryBasedSelectionDialog.class))
				.getDialogSettings();
		IDialogSettings section = settings.getSection(DIALOG_SETTINGS_SECTION);
		if (section == null) {
			section = settings.addNewSection(DIALOG_SETTINGS_SECTION);
		}
		return section;
	}

	private Label createMessageArea(Composite composite) {
		Label label = new Label(composite, SWT.NONE);
		label.setText(Messages.CHEAT_SHEET_SELECTION_DIALOG_MSG);
		label.setFont(composite.getFont());
		return label;
	}

	public IStatus getStatus() {
		return status ;
	}
}
