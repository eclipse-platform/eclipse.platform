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
 *******************************************************************************/
package org.eclipse.team.internal.ccvs.ui.wizards;


import java.text.Collator;  // don't use ICU, pending resolution of issue 
import java.util.*;
import java.util.List;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.team.internal.ccvs.core.client.Command.KSubstOption;
import org.eclipse.team.internal.ccvs.core.util.StringMatcher;
import org.eclipse.team.internal.ccvs.ui.*;
import org.eclipse.team.internal.ccvs.ui.wizards.ModeWizard.ModeChange;
import org.eclipse.team.internal.ui.PixelConverter;
import org.eclipse.team.internal.ui.SWTUtils;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.model.WorkbenchLabelProvider;

//TODO: Filtering the TableViewer is currently slow for large amounts of files. 3.1M5 will feature a framework to help with this, so wait until it is there.
//TODO: Files should be added to the viewer asynchronously, currently bringing up the dialog takes a lot of time for large selections (e.g. jdt.ui)

public class ModeWizardSelectionPage extends WizardPage {
	
	private final static class ModeChangeCellModifier implements ICellModifier {
		
		private final ModeChangeTable fTable;
		
		public ModeChangeCellModifier(ModeChangeTable table) {
			fTable= table;
		}
		
		@Override
		public boolean canModify(Object element, String property) {
			return PROPERTY_MODE.equals(property);
		}
		
		@Override
		public Object getValue(Object element, String property) {
			if (PROPERTY_MODE.equals(property)) {
				final KSubstOption mode= ((ModeChange)element).getNewMode();
				for (int i = 0; i < MODES.length; i++) {
					if (MODES[i].equals(mode)) {
						return Integer.valueOf(i);
					}
				}
			}
			return null;
		}
		
		@Override
		public void modify(Object element, String property, Object value) {
			if (element instanceof Item)
				element= ((Item)element).getData();
			if (PROPERTY_MODE.equals(property)) {
				((ModeChange)element).setNewMode(MODES[((Integer)value).intValue()]);
				fTable.modelChanged(true);
			}
		}
	}
	
	private final static class ModeChangeLabelProvider implements ITableLabelProvider {
		
		private final DecoratingLabelProvider fDecoratingLP;
		private final ModeChangeTable fTable;
		
		public ModeChangeLabelProvider(ModeChangeTable table) {
			fTable= table;
			fDecoratingLP= new DecoratingLabelProvider(new WorkbenchLabelProvider(), PlatformUI.getWorkbench().getDecoratorManager().getLabelDecorator());
			fDecoratingLP.addListener(fTable);
		}
		
		@Override
		public Image getColumnImage(Object element, int columnIndex) {
			if (columnIndex == INDEX_FILE) {
				return fDecoratingLP.getImage(((ModeChange)element).getFile());
			}
			return null;
		}
		
		@Override
		public String getColumnText(Object element, int columnIndex) {
			final ModeChange change= (ModeChange)element;
			switch (columnIndex) {
			case INDEX_FILE: return (change.hasChanged() ? "* " : "") + change.getFile().getName(); //$NON-NLS-1$ //$NON-NLS-2$
			case INDEX_MODE: return change.getNewMode().getLongDisplayText();
			case INDEX_PATH: return change.getFile().getFullPath().toOSString();
			}
			throw new IllegalArgumentException();
		}
		
		@Override
		public void addListener(ILabelProviderListener listener) {
		}
		
		@Override
		public boolean isLabelProperty(Object element, String property) {
			return false;
		}
		
		@Override
		public void dispose() {
			fDecoratingLP.removeListener(fTable);
		}
		
		@Override
		public void removeListener(ILabelProviderListener listener) {        
		}
	}
	
	private final static class TableComparator extends ViewerComparator implements SelectionListener {
		
		private final Collator fCollator;
		private final TableViewer fViewer;
		private final TableColumn fFile, fMode, fPath;
		
		private int fIndex;
		private boolean fAscending;
		
		
		public TableComparator(TableViewer viewer, TableColumn fileColumn, TableColumn modeColumn, TableColumn pathColumn) {
//			TODO: possible issue, TableSorter's Collator not shared with base class.  Might cause problem switching to ICU collation.
			fCollator= Collator.getInstance();
			fViewer= viewer;
			
			fFile= fileColumn;
			fMode= modeColumn;
			fPath= pathColumn;
			
//			Set initial sorting to file column
			fIndex= INDEX_FILE;
			fViewer.getTable().setSortColumn(fFile);
			fViewer.getTable().setSortDirection(SWT.DOWN);
			fAscending= true;
			
			fileColumn.addSelectionListener(this);
			modeColumn.addSelectionListener(this);
			pathColumn.addSelectionListener(this);
		}
		
		@Override
		public int compare(Viewer viewer, Object e1, Object e2) {
			
			final ModeChange mc1= (ModeChange)e1;
			final ModeChange mc2= (ModeChange)e2;
			
			final String s1, s2;
			
			switch (fIndex) {
			
			case INDEX_FILE: 
				s1= mc1.getFile().getName();
				s2= mc2.getFile().getName();
				break;
				
			case INDEX_MODE:
				s1= mc1.getNewMode().getLongDisplayText();
				s2= mc2.getNewMode().getLongDisplayText(); 
				break;
				
			case INDEX_PATH: 
				s1= mc1.getFile().getFullPath().toOSString();
				s2= mc2.getFile().getFullPath().toOSString(); 
				break;
				
			default: 
				throw new IllegalArgumentException();
			}
			
			final int compare= fCollator.compare(s1, s2);
			return fAscending ? compare : -compare;
		}
		
		@Override
		public void widgetSelected(SelectionEvent e) {
			final int index= columnToIndex(e.widget);
			if (index == fIndex) {
				fIndex= index;
				fAscending= !fAscending;
				fViewer.getTable().setSortDirection(fAscending ? SWT.DOWN : SWT.UP);
			} else {
				fIndex= index;
				TableColumn tableCol = null;
				switch(fIndex){
					case INDEX_FILE:
						tableCol = fFile;
					break;
					
					case INDEX_MODE:
						tableCol = fMode;
					break;
					
					case INDEX_PATH:
						tableCol = fPath;
					break;
				}
				fViewer.getTable().setSortColumn(tableCol);
				fViewer.getTable().setSortDirection(fAscending ? SWT.DOWN : SWT.UP);
			}
			fViewer.refresh();
		}
		
		@Override
		public void widgetDefaultSelected(SelectionEvent e) {
			// nop
		}
		
		private int columnToIndex(Object column) {
			if (column == fFile) return INDEX_FILE;
			if (column == fMode) return INDEX_MODE;
			if (column == fPath) return INDEX_PATH;
			throw new IllegalArgumentException();
		}
	}
	
	private static final class ModeChangeTable extends Observable implements ISelectionChangedListener, ILabelProviderListener {
		
		private final List fChanges;
		private final TableViewer fViewer;
		private final Filter fFilter;
		private int fNumberOfChanges;
		
		public ModeChangeTable(Composite composite, PixelConverter converter, List changes) {
			
			fChanges= changes;
			fNumberOfChanges= 0;
			
			/**
			 * Create a table.
			 */
			final Table table = new Table(composite, SWT.V_SCROLL | SWT.BORDER | SWT.MULTI | SWT.FULL_SELECTION);
			table.setLayoutData(SWTUtils.createHVFillGridData());
			table.setLinesVisible(false);
			table.setHeaderVisible(true);
			
			/**
			 * The 'File' column
			 */
			final TableColumn fileColumn = new TableColumn(table, SWT.NONE, INDEX_FILE);
			fileColumn.setWidth(converter.convertWidthInCharsToPixels(LARGE_COLUMN));
			fileColumn.setText(CVSUIMessages.ModeWizardSelectionPage_2); 
			table.setSortColumn(fileColumn);
			table.setSortDirection(SWT.DOWN);
			/**
			 * The 'Mode' column
			 */
			final TableColumn newModeColumn = new TableColumn(table, SWT.NONE, INDEX_MODE);
			newModeColumn.setWidth(converter.convertWidthInCharsToPixels(COLUMN_MIN_WIDTH_IN_CHARS + 6));
			newModeColumn.setText(CVSUIMessages.ModeWizardSelectionPage_3); 
			
			/**
			 * The 'Path' column
			 */
			final TableColumn pathColumn= new TableColumn(table, SWT.NONE, INDEX_PATH);
			pathColumn.setWidth(converter.convertWidthInCharsToPixels(50));
			pathColumn.setText(CVSUIMessages.ModeWizardSelectionPage_4); 
			
			
			fViewer= new TableViewer(table);
			fViewer.setContentProvider(new ModeChangeContentProvider());
			fViewer.setLabelProvider(new ModeChangeLabelProvider(this));
			fViewer.getControl().setLayoutData(SWTUtils.createHVFillGridData());
			
			final CellEditor newModeEditor = new ComboBoxCellEditor(table, COMBO_TEXT, SWT.READ_ONLY);
			
			fViewer.setCellEditors(new CellEditor [] { null, newModeEditor, null });
			fViewer.setColumnProperties(new String [] { PROPERTY_FILE, PROPERTY_MODE, PROPERTY_CHANGED });
			fViewer.setCellModifier(new ModeChangeCellModifier(this));
			
			fViewer.addFilter(fFilter= new Filter());
			
			fViewer.setComparator(new TableComparator(fViewer, fileColumn, newModeColumn, pathColumn));
			
			fViewer.setInput(fChanges);
			
			//TODO: CVSLightweightDecorator.decorate() is lighter than normal decs.
			fViewer.addSelectionChangedListener(this);
			
			fileColumn.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					
				}
			});
			
			fViewer.refresh();
		}
		
		public TableViewer getViewer() {
			return fViewer;
		}
		
		@Override
		public void selectionChanged(SelectionChangedEvent event) {
			setChanged();
			notifyObservers(fViewer.getSelection());
		}
		
		public void modelChanged(boolean updateLabels) {
			fViewer.refresh(updateLabels);
			fNumberOfChanges= 0;
			for (Iterator iter = fChanges.iterator(); iter.hasNext();) {
				ModeChange change = (ModeChange) iter.next();
				if (change.hasChanged())
					++fNumberOfChanges;
			}
			setChanged();
			notifyObservers();
		}
		
		public Filter getFilter() {
			return fFilter;
		}
		
		public IStructuredSelection getSelection() {
			return fViewer.getStructuredSelection();
		}
		
		@Override
		public void labelProviderChanged(LabelProviderChangedEvent event) {
			fViewer.refresh();
		}
		
		public void selectAll() {
			fViewer.setSelection(new StructuredSelection(fChanges));
			fViewer.getControl().setFocus();
		}
		
		public void selectNone() {
			fViewer.setSelection(StructuredSelection.EMPTY);
			fViewer.getControl().setFocus();
		}
		
		public int getNumberOfChanges() {
			return fNumberOfChanges;
		}
	}
	
	private static final class ModeChangeContentProvider implements IStructuredContentProvider {
		
		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		}
		
		@Override
		public Object[] getElements(Object inputElement) {
//			new FetchJob(fViewer, (List)inputElement, fPattern);
//			return new Object[0];
			return ((List)inputElement).toArray();
		}
		
		@Override
		public void dispose() {
		}
	}
	
	private static final class ModeCombo extends SelectionAdapter implements Observer {
		
		private final Combo fCombo;
		private final ModeChangeTable fTable;
		
		public ModeCombo(ModeChangeTable table, Composite parent) {
			fTable= table;
			fCombo= new Combo(parent, SWT.READ_ONLY);
			fCombo.setLayoutData(SWTUtils.createHFillGridData());
			fCombo.setItems(COMBO_TEXT);
			fCombo.addSelectionListener(this);
			fTable.addObserver(this);
		}
		
		@Override
		public void widgetSelected(SelectionEvent e) {
			final KSubstOption mode= MODES[fCombo.getSelectionIndex()];
			final IStructuredSelection selection= fTable.getSelection();
			for (final Iterator iter = selection.iterator(); iter.hasNext();) {
				final ModeChange change = (ModeChange) iter.next();
				change.setNewMode(mode);                
			}
			fTable.modelChanged(true);
		}
		
		@Override
		public void update(Observable o, Object arg) {
			final IStructuredSelection selection= fTable.getViewer().getStructuredSelection();
			
			if (selection.isEmpty()) {
				fCombo.deselectAll();
				fCombo.setEnabled(false);
			} else {
				fCombo.setEnabled(true);
				final KSubstOption option= ((ModeChange)selection.getFirstElement()).getNewMode();
				for (Iterator iter = selection.iterator(); iter.hasNext();) {
					if (option != ((ModeChange)iter.next()).getNewMode()) {
						fCombo.deselectAll();
						return;
					}
				}
				fCombo.setText(option.getLongDisplayText());
			}
		}
	}
	
	private static final class Filter extends ViewerFilter {
		
		private boolean fFilterUnchanged;
		private StringMatcher fMatcher;
		
		public Filter() {
			fFilterUnchanged= false;
			fMatcher= new StringMatcher("*", true, false); //$NON-NLS-1$
		}
		
		@Override
		public boolean select(Viewer viewer, Object parentElement, Object element) {
			final ModeChange change= (ModeChange)element;
			if (fFilterUnchanged && !change.hasChanged())
				return false;
			if (!fMatcher.match(change.getFile().getName()))
				return false;
			return true;
		}
		
		public void setPattern(String pattern) {
			pattern= pattern.trim();
			if (!pattern.endsWith("*")) { //$NON-NLS-1$
				pattern += "*"; //$NON-NLS-1$
			}
			fMatcher= new StringMatcher(pattern, true, false);
		}
		
		public void filterUnchanged(boolean filter) {
			fFilterUnchanged= filter;
		}
	}
	
	private static final class ResetButton extends SelectionAdapter implements Observer {
		
		private final ModeChangeTable fTable;
		private final Button fButton;
		
		public ResetButton(ModeChangeTable table, Composite parent, PixelConverter converter) {
			final int buttonWidth= converter.convertHorizontalDLUsToPixels(IDialogConstants.BUTTON_WIDTH);
			fTable= table;
			fButton= new Button(parent, SWT.NONE);
			fButton.setLayoutData(SWTUtils.createGridData(buttonWidth, SWT.DEFAULT, SWT.FILL, SWT.CENTER, false, false));
			fButton.setText(CVSUIMessages.ModeWizardSelectionPage_8); 
			fButton.setToolTipText(CVSUIMessages.ModeWizardSelectionPage_9); 
			fButton.addSelectionListener(this);
			fTable.addObserver(this);
		}
		
		@Override
		public void widgetSelected(SelectionEvent e) {
			fButton.setEnabled(false);
			final IStructuredSelection selection= fTable.getSelection();
			for (Iterator iter = selection.iterator(); iter.hasNext();) {
				final ModeChange change = (ModeChange) iter.next();
				change.setNewMode(change.getMode());
			}
			fTable.modelChanged(true);
		}
		
		@Override
		public void update(Observable o, Object arg) {
			final IStructuredSelection selection= fTable.getSelection();
			for (final Iterator iter = selection.iterator(); iter.hasNext();) {
				if (((ModeChange)iter.next()).hasChanged()) {
					fButton.setEnabled(true);
					return;
				}
			}
			fButton.setEnabled(false);
		}
	}
	
	private static final class GuessButton extends SelectionAdapter implements Observer {
		
		private final ModeChangeTable fTable;
		private final Button fButton;
		
		public GuessButton(ModeChangeTable table, Composite parent, PixelConverter converter) {
			fTable= table;
			final int buttonWidth= converter.convertHorizontalDLUsToPixels(IDialogConstants.BUTTON_WIDTH);
			fButton= new Button(parent, SWT.NONE);
			fButton.setLayoutData(SWTUtils.createGridData(buttonWidth, SWT.DEFAULT, SWT.FILL, SWT.CENTER, false, false));
			fButton.setText(CVSUIMessages.ModeWizardSelectionPage_10); 
			fButton.setToolTipText(CVSUIMessages.ModeWizardSelectionPage_11); 
			fButton.addSelectionListener(this);
			fTable.addObserver(this);
			
		}
		
		@Override
		public void widgetSelected(SelectionEvent e) {
			final IStructuredSelection selection= fTable.getSelection();
			for (Iterator iter = selection.iterator(); iter.hasNext();) {
				final ModeChange change = (ModeChange) iter.next();
				change.setNewMode(KSubstOption.fromFile(change.getFile()));
			}
			fTable.modelChanged(true);
		}
		
		@Override
		public void update(Observable o, Object arg) {
			fButton.setEnabled(!fTable.getSelection().isEmpty());
		}
	}     
	
	private static final class SelectAllButton extends SelectionAdapter {
		
		private final ModeWizardSelectionPage fPage;
		private final Button fButton;
		
		public SelectAllButton(ModeWizardSelectionPage page, Composite parent, PixelConverter converter) {
			fPage= page;
			final int buttonWidth= converter.convertHorizontalDLUsToPixels(IDialogConstants.BUTTON_WIDTH);
			fButton= new Button(parent, SWT.NONE);
			fButton.setLayoutData(SWTUtils.createGridData(buttonWidth, SWT.DEFAULT, SWT.FILL, SWT.CENTER, false, false));
			fButton.setText(CVSUIMessages.ModeWizardSelectionPage_12); 
			fButton.addSelectionListener(this);
		}
		
		@Override
		public void widgetSelected(SelectionEvent e) {
			fPage.getTable().selectAll();
		}
	}     
	
	private static final class SelectNoneButton extends SelectionAdapter {
		
		private final ModeWizardSelectionPage fPage;
		private final Button fButton;
		
		public SelectNoneButton(ModeWizardSelectionPage page, Composite parent, PixelConverter converter) {
			fPage= page;
			final int buttonWidth= converter.convertHorizontalDLUsToPixels(IDialogConstants.BUTTON_WIDTH);
			fButton= new Button(parent, SWT.NONE);
			fButton.setLayoutData(SWTUtils.createGridData(buttonWidth, SWT.DEFAULT, SWT.FILL, SWT.CENTER, false, false));
			fButton.setText(CVSUIMessages.ModeWizardSelectionPage_13); 
			fButton.addSelectionListener(this);
		}
		
		@Override
		public void widgetSelected(SelectionEvent e) {
			fPage.getTable().selectNone();
		}
	}     
	
	private static final class ShowChangesOnlyCheckbox extends SelectionAdapter {
		
		private final ModeWizardSelectionPage fPage;
		private final Button fCheck;
		
		public ShowChangesOnlyCheckbox(ModeWizardSelectionPage page, Composite parent) {
			fPage= page;
			fCheck= new Button(parent, SWT.CHECK);
			fCheck.setText(CVSUIMessages.ModeWizardSelectionPage_14); 
			fCheck.setLayoutData(SWTUtils.createHFillGridData());
			fCheck.setSelection(false);
			fCheck.addSelectionListener(this);
		}
		
		@Override
		public void widgetSelected(SelectionEvent e) {
			final ModeChangeTable table= fPage.getTable();
			table.getFilter().filterUnchanged(fCheck.getSelection());
			table.modelChanged(true);
		}
	}
	
	private static final class FilterTextBox extends SelectionAdapter implements ModifyListener {
		private final ModeWizardSelectionPage fPage;
		private final Text fTextField;
		
		public FilterTextBox(ModeWizardSelectionPage page, Composite parent, PixelConverter converter) {
			fPage= page;
			fTextField= new Text(parent, SWT.SINGLE | SWT.BORDER);
			fTextField.setLayoutData(SWTUtils.createHFillGridData());
			
			final int buttonWidth= converter.convertHorizontalDLUsToPixels(IDialogConstants.BUTTON_WIDTH);
			final Button button= new Button(parent, SWT.PUSH);
			button.setText(CVSUIMessages.ModeWizardSelectionPage_15); 
			button.setLayoutData(SWTUtils.createGridData(buttonWidth, SWT.DEFAULT, SWT.FILL, SWT.CENTER, false, false));
			button.addSelectionListener(this);
			
			fTextField.addModifyListener(this);
		}
		
		@Override
		public void widgetSelected(SelectionEvent e) {
			fTextField.setText(""); //$NON-NLS-1$
			fTextField.setFocus();
		}
		
		@Override
		public void modifyText(ModifyEvent e) {
			final ModeChangeTable table= fPage.getTable();
			table.getFilter().setPattern(fTextField.getText());
			table.modelChanged(false);
		}
		
		public void setFocus() {
			fTextField.setFocus();
		}
	}
	
	private static final class ChangeCounterLabel implements Observer {
		
		private final Label fLabel;
		private final ModeChangeTable fTable;
		
		ChangeCounterLabel(Composite parent, ModeChangeTable table) {
			fTable= table;
			fTable.addObserver(this);
			fLabel= SWTUtils.createLabel(parent, null);
			fLabel.setFont(JFaceResources.getFontRegistry().getBold(JFaceResources.DIALOG_FONT));
		}
		
		@Override
		public void update(Observable o, Object arg) {
			updateText(fTable.getNumberOfChanges());
		}
		
		/**
		 * @param numberOfChanges
		 */
		private void updateText(int numberOfChanges) {
			fLabel.setText(NLS.bind(CVSUIMessages.ModeWizardSelectionPage_17, new String[] { Integer.toString(numberOfChanges) })); 
		}

	}

	private static final class SelectionCounterLabel implements Observer {
		
		private final Label fLabel;
		private final ModeChangeTable fTable;
		
		public SelectionCounterLabel(Composite parent, ModeChangeTable table) {
			fTable= table;
			fTable.addObserver(this);
			fLabel= new Label(parent, SWT.WRAP | SWT.RIGHT);
			fLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		}
		
		@Override
		public void update(Observable o, Object arg) {
			updateText(fTable.getSelection().size());
		}
		
		/**
		 * @param numberOfChanges
		 */
		private void updateText(int selected) {
			fLabel.setText(NLS.bind(CVSUIMessages.ModeWizardSelectionPage_25, new String[] { Integer.toString(selected) })); 
		}
	}
	
	private final static int LARGE_COLUMN= 50;
	
	protected static final int INDEX_FILE= 0;
	protected static final int INDEX_MODE= 1;
	protected static final int INDEX_PATH= 2;
	
	protected static final String PROPERTY_FILE= "file"; //$NON-NLS-1$
	protected static final String PROPERTY_MODE= "mode"; //$NON-NLS-1$
	protected static final String PROPERTY_CHANGED= "changed"; //$NON-NLS-1$
	
	protected static final KSubstOption [] MODES;
	protected static final String [] COMBO_TEXT;
	
	static final int COLUMN_MIN_WIDTH_IN_CHARS;
	
	static {
		MODES= KSubstOption.getAllKSubstOptions();
		Arrays.sort(MODES, (a, b) -> {
			String aKey = a.getLongDisplayText();
			String bKey = b.getLongDisplayText();
			return aKey.compareTo(bKey);
		});
		COMBO_TEXT= new String[MODES.length];
		int maxLength= 0;
		for (int i = 0; i < MODES.length; i++) {
			COMBO_TEXT[i]= MODES[i].getLongDisplayText();
			if (COMBO_TEXT[i].length() > maxLength) maxLength= COMBO_TEXT[i].length();
		}
		COLUMN_MIN_WIDTH_IN_CHARS= maxLength;
	}
	
	private final List fChanges;
	protected ModeChangeTable fTable;
	
	private CommitCommentArea fCommentArea;
	
	public ModeWizardSelectionPage(List modeChanges) {
		super(CVSUIMessages.ModeWizardSelectionPage_18, CVSUIMessages.ModeWizardSelectionPage_19, CVSUIPlugin.getPlugin().getImageDescriptor(ICVSUIConstants.IMG_WIZBAN_KEYWORD)); // 
		setDescription(CVSUIMessages.ModeWizardSelectionPage_20); 
		fChanges= modeChanges;
	}
	
	@Override
	public void createControl(final Composite parent) {
		
		final PixelConverter converter= SWTUtils.createDialogPixelConverter(parent);
		
		final int horizontalSpace= converter.convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_MARGIN);
		final int verticalSpace= converter.convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_MARGIN);
		
		/**
		 * The main composite with the vertical sash
		 */
		final Composite mainComposite= SWTUtils.createHVFillComposite(parent, SWTUtils.MARGINS_DEFAULT);
		// set F1 help
		PlatformUI.getWorkbench().getHelpSystem().setHelp(mainComposite, IHelpContextIds.KEYWORD_SUBSTITUTION_PAGE);
		
		final SashForm mainSash= new SashForm(mainComposite, SWT.VERTICAL);
		mainSash.setLayoutData(SWTUtils.createHVFillGridData());
		
		/**
		 * The composite with the filter box, the table and the selection and filter controls.
		 */
		final Composite topComposite= SWTUtils.createHVFillComposite(mainSash, SWTUtils.MARGINS_NONE);
		((GridLayout)topComposite.getLayout()).marginBottom= verticalSpace;
		
		final Composite topGroup= SWTUtils.createHVFillGroup(topComposite, CVSUIMessages.ModeWizardSelectionPage_21, SWTUtils.MARGINS_DIALOG); 
		
		final Composite filterComposite= SWTUtils.createHFillComposite(topGroup, SWTUtils.MARGINS_NONE, 2);
		final FilterTextBox filterBox= new FilterTextBox(ModeWizardSelectionPage.this, filterComposite, converter);
		
		fTable= new ModeChangeTable(topGroup, converter, fChanges);
		
		final Composite selectionComposite= SWTUtils.createHFillComposite(topGroup, SWTUtils.MARGINS_NONE, 2);

		new ChangeCounterLabel(selectionComposite, fTable);
		new SelectionCounterLabel(selectionComposite, fTable);
		
		new ShowChangesOnlyCheckbox(ModeWizardSelectionPage.this, selectionComposite);
		
		final Composite buttonComposite= SWTUtils.createHFillComposite(selectionComposite, SWTUtils.MARGINS_NONE, 2);
		buttonComposite.setLayoutData(new GridData());
		new SelectAllButton(ModeWizardSelectionPage.this, buttonComposite, converter);
		new SelectNoneButton(ModeWizardSelectionPage.this, buttonComposite, converter);
		
		/**
		 * The bottom sash which separates the mode controls from the commit comment area
		 */
		final SashForm bottomSash= new SashForm(mainSash, SWT.NONE);
		bottomSash.setLayoutData(SWTUtils.createHFillGridData());
		
		/**
		 * The left composite with the mode controls.
		 */
		final Composite leftComposite= SWTUtils.createHVFillComposite(bottomSash, SWTUtils.MARGINS_NONE, 1);
		((GridLayout)leftComposite.getLayout()).marginRight= horizontalSpace;
		((GridLayout)leftComposite.getLayout()).marginTop= verticalSpace;
		
		final Group leftGroup= SWTUtils.createHVFillGroup(leftComposite, CVSUIMessages.ModeWizardSelectionPage_22, SWTUtils.MARGINS_DIALOG, 3); 
		
		new ModeCombo(fTable, leftGroup);
		new GuessButton(fTable, leftGroup, converter);
		new ResetButton(fTable, leftGroup, converter);
		SWTUtils.createPlaceholder(leftGroup, 1);
		final Label infoLabel= SWTUtils.createLabel(leftGroup, CVSUIMessages.ModeWizardSelectionPage_23, 3); 
		
		fTable.addObserver((o, arg) -> {
			final boolean enabled= !fTable.getSelection().isEmpty();
			leftGroup.setEnabled(enabled);
			infoLabel.setEnabled(enabled);
		});
		
		/**
		 * The right composite with the commit comment area.
		 */
		final Composite rightComposite= SWTUtils.createHVFillComposite(bottomSash, SWTUtils.MARGINS_NONE);
		((GridLayout)rightComposite.getLayout()).marginLeft= horizontalSpace;
		((GridLayout)rightComposite.getLayout()).marginTop= verticalSpace;
		
		final Group rightGroup= SWTUtils.createHVFillGroup(rightComposite, CVSUIMessages.ModeWizardSelectionPage_24, SWTUtils.MARGINS_DIALOG); 
		(fCommentArea= new CommitCommentArea()).createArea(rightGroup);
		
		/**
		 * Set up the page
		 */
		mainSash.setWeights(new int [] { 5, 2 });
		bottomSash.setWeights(new int [] { 3, 2 });
		fTable.modelChanged(true);
		fTable.selectAll();
		filterBox.setFocus();
		setupListeners();
		setControl(mainComposite);
		validatePage();
	}

	private void setupListeners() {
		fCommentArea.addPropertyChangeListener(event -> {
			if (event.getProperty() != null) {
				validatePage();
			}
		});
	}

	protected ModeChangeTable getTable() {
		return fTable;
	}
	
	public List getChanges() {
		final List<ModeChange> changes = new ArrayList<>();
		for (Iterator iter = fChanges.iterator(); iter.hasNext();) {
			final ModeChange change = (ModeChange) iter.next();
			if (change.hasChanged())
				changes.add(change);
		}
		return changes;
	}
	
	public String getComment(Shell shell) {
		return fCommentArea.getCommentWithPrompt(shell);
	}

	private void validatePage() {
		if (fCommentArea.getComment(false).isEmpty()) {
			final IPreferenceStore store = CVSUIPlugin.getPlugin()
					.getPreferenceStore();
			final String allowEmptyComment = store
					.getString(ICVSUIConstants.PREF_ALLOW_EMPTY_COMMIT_COMMENTS);
			if (allowEmptyComment.equals(MessageDialogWithToggle.NEVER)) {
				setPageComplete(false); // then the page is not complete
				return;
			}
		}
		setPageComplete(true);
	}

}
