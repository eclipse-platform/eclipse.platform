/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
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
package org.eclipse.compare.examples.xml;

import java.util.*;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.window.Window;

import org.eclipse.ui.*;

/**
 * The XMLComparePreferencePage is the page used to set ID Mappings for XML Compare
 */
public class XMLComparePreferencePage extends PreferencePage implements IWorkbenchPreferencePage, Listener {

	private Table fIdMapsTable;
	private Button fAddIdMapButton;
	private Button fRenameIdMapButton;
	private Button fRemoveIdMapButton;
	private Button fEditIdMapButton;

	private Table fMappingsTable;
	private Button fNewMappingsButton;
	private Button fEditMappingsButton;
	private Button fRemoveMappingsButton;

	private Table fOrderedTable;
	private Button fNewOrderedButton;
	private Button fEditOrderedButton;
	private Button fRemoveOrderedButton;

	private HashMap fIdMapsInternal;
	private HashMap fIdMaps;// HashMap ( idname -> HashMap (signature -> id) )
	private HashMap fIdExtensionToName;
	
	//fOrderedElements contains signature of xml element whose children must be compared in ordered fashion
	private HashMap fOrderedElements;// HashMap ( idname -> ArrayList (signature) )
	private HashMap fOrderedElementsInternal;
	
	protected static char[] invalidCharacters;
	protected static final char SIGN_SEPARATOR = XMLStructureCreator.SIGN_SEPARATOR;

	public static String IDTYPE_ATTRIBUTE= XMLCompareMessages.XMLComparePreference_idtype_attribute; 
	public static String IDTYPE_CHILDBODY= XMLCompareMessages.XMLComparePreference_idtype_child_body; 

	
	static {
		invalidCharacters = new char[] {XMLPlugin.IDMAP_SEPARATOR,XMLPlugin.IDMAP_FIELDS_SEPARATOR,XMLStructureCreator.SIGN_ENCLOSING};
	}

	
	public XMLComparePreferencePage() {
		super();
		
		fIdMaps = new HashMap();
		XMLPlugin plugin= XMLPlugin.getDefault();
		HashMap PluginIdMaps = plugin.getIdMaps();
		Set keySet = PluginIdMaps.keySet();
		for (Iterator iter = keySet.iterator(); iter.hasNext(); ) {
			String key = (String) iter.next();
			fIdMaps.put(key, ((HashMap)PluginIdMaps.get(key)).clone() );
		}
		fIdMapsInternal = plugin.getIdMapsInternal();
		
		fIdExtensionToName= new HashMap();
		HashMap PluginIdExtensionToName= plugin.getIdExtensionToName();
		keySet= PluginIdExtensionToName.keySet();
		for (Iterator iter= keySet.iterator(); iter.hasNext(); ) {
			String key= (String) iter.next();
			fIdExtensionToName.put(key, PluginIdExtensionToName.get(key));
		}
		
		fOrderedElements= new HashMap();
		HashMap PluginOrderedElements= plugin.getOrderedElements();
		keySet= PluginOrderedElements.keySet();
		for (Iterator iter= keySet.iterator(); iter.hasNext();) {
			String key= (String) iter.next();
			fOrderedElements.put(key, ((ArrayList)PluginOrderedElements.get(key)).clone());
		}
		
		fOrderedElementsInternal= plugin.getOrderedElementsInternal();
	}

	@Override
	protected Control createContents(Composite ancestor) {
		Composite parent= new Composite(ancestor, SWT.NULL);
		GridLayout layout= new GridLayout();
		layout.numColumns= 2;
		layout.marginHeight= 0;
		layout.marginWidth= 0;
		parent.setLayout(layout);				
		
		//layout the top table & its buttons
		Label label = new Label(parent, SWT.LEFT);
		label.setText(XMLCompareMessages.XMLComparePreference_topTableLabel); 
		GridData data = new GridData();
		data.horizontalAlignment = GridData.FILL;
		data.horizontalSpan = 2;
		label.setLayoutData(data);
	
		fIdMapsTable = new Table(parent, SWT.SINGLE | SWT.BORDER | SWT.FULL_SELECTION);
		fIdMapsTable.setHeaderVisible(true);	
		data = new GridData(GridData.FILL_BOTH);
		data.heightHint = fIdMapsTable.getItemHeight()*4;
		fIdMapsTable.setLayoutData(data);
		fIdMapsTable.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				selectionChanged();
			}
		});

		String column2Text= XMLCompareMessages.XMLComparePreference_topTableColumn2; 
		String column3Text= XMLCompareMessages.XMLComparePreference_topTableColumn3; 
		ColumnLayoutData columnLayouts[]= {
			new ColumnWeightData(1),
			new ColumnPixelData(convertWidthInCharsToPixels(column2Text.length()+2), true),
			new ColumnPixelData(convertWidthInCharsToPixels(column3Text.length()+5), true)};
		TableLayout tablelayout = new TableLayout();
		fIdMapsTable.setLayout(tablelayout);
		for (int i=0; i<3; i++)
			tablelayout.addColumnData(columnLayouts[i]);
		TableColumn column = new TableColumn(fIdMapsTable, SWT.NONE);
		column.setText(XMLCompareMessages.XMLComparePreference_topTableColumn1); 
		column = new TableColumn(fIdMapsTable, SWT.NONE);
		column.setText(column2Text);
		column = new TableColumn(fIdMapsTable, SWT.NONE);
		column.setText(column3Text);
		
		fillIdMapsTable();

		Composite buttons= new Composite(parent, SWT.NULL);
		buttons.setLayout(new GridLayout());
		data = new GridData();
		data.verticalAlignment = GridData.FILL;
		data.horizontalAlignment = GridData.FILL;
		buttons.setLayoutData(data);

		fAddIdMapButton = new Button(buttons, SWT.PUSH);
		fAddIdMapButton.setText(XMLCompareMessages.XMLComparePreference_topAdd); 
		fAddIdMapButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				addIdMap(fAddIdMapButton.getShell());
			}
		});
		data = new GridData();
		data.horizontalAlignment = GridData.FILL;
		//data.heightHint = convertVerticalDLUsToPixels(IDialogConstants.BUTTON_HEIGHT);
		int widthHint = convertHorizontalDLUsToPixels(IDialogConstants.BUTTON_WIDTH);
		data.widthHint = Math.max(widthHint, fAddIdMapButton.computeSize(SWT.DEFAULT, SWT.DEFAULT, true).x);
		fAddIdMapButton.setLayoutData(data);
	
		fRenameIdMapButton = new Button(buttons, SWT.PUSH);
		fRenameIdMapButton.setText(XMLCompareMessages.XMLComparePreference_topRename); 
		fRenameIdMapButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				renameIdMap(fRenameIdMapButton.getShell());
			}
		});
		data = new GridData();
		data.horizontalAlignment = GridData.FILL;
		//data.heightHint = convertVerticalDLUsToPixels(IDialogConstants.BUTTON_HEIGHT);
		widthHint = convertHorizontalDLUsToPixels(IDialogConstants.BUTTON_WIDTH);
		data.widthHint = Math.max(widthHint, fAddIdMapButton.computeSize(SWT.DEFAULT, SWT.DEFAULT, true).x);
		fRenameIdMapButton.setLayoutData(data);

		fRemoveIdMapButton = new Button(buttons, SWT.PUSH);
		fRemoveIdMapButton.setText(XMLCompareMessages.XMLComparePreference_topRemove); 
		fRemoveIdMapButton.addSelectionListener(new SelectionAdapter() {
		@Override
		public void widgetSelected(SelectionEvent e) {
				removeIdMap(fRemoveIdMapButton.getShell());
			}
		});
		data = new GridData();
		data.horizontalAlignment = GridData.FILL;
		//data.heightHint = convertVerticalDLUsToPixels(IDialogConstants.BUTTON_HEIGHT);
			widthHint = convertHorizontalDLUsToPixels(IDialogConstants.BUTTON_WIDTH);
		data.widthHint = Math.max(widthHint, fRemoveIdMapButton.computeSize(SWT.DEFAULT, SWT.DEFAULT, true).x);
		fRemoveIdMapButton.setLayoutData(data);

		createSpacer(buttons);

		fEditIdMapButton = new Button(buttons, SWT.PUSH);
		fEditIdMapButton.setText(XMLCompareMessages.XMLComparePreference_topEdit); 
		fEditIdMapButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				editIdMap(fEditIdMapButton.getShell());
			}
		});
		data = new GridData();
		data.horizontalAlignment = GridData.FILL;
		//data.heightHint = convertVerticalDLUsToPixels(IDialogConstants.BUTTON_HEIGHT);
		widthHint = convertHorizontalDLUsToPixels(IDialogConstants.BUTTON_WIDTH);
		data.widthHint = Math.max(widthHint, fEditIdMapButton.computeSize(SWT.DEFAULT, SWT.DEFAULT, true).x);
		fEditIdMapButton.setLayoutData(data);
	
		//Spacer
		label = new Label(parent, SWT.LEFT);
		data = new GridData();
		data.horizontalAlignment = GridData.FILL;
		data.horizontalSpan = 2;
		label.setLayoutData(data);

		//layout the middle table & its buttons
		label = new Label(parent, SWT.LEFT);
		label.setText(XMLCompareMessages.XMLComparePreference_middleTableLabel); 
		data = new GridData();
		data.horizontalAlignment = GridData.FILL;
		data.horizontalSpan = 2;
		label.setLayoutData(data);

		fMappingsTable = new Table(parent, SWT.SINGLE | SWT.BORDER | SWT.FULL_SELECTION);
		fMappingsTable.setHeaderVisible(true);	
		data = new GridData(GridData.FILL_BOTH);
		data.heightHint = fMappingsTable.getItemHeight()*4;
		data.widthHint= convertWidthInCharsToPixels(70);
		fMappingsTable.setLayoutData(data);
		
		column3Text= XMLCompareMessages.XMLComparePreference_middleTableColumn3; 
		String column4Text= XMLCompareMessages.XMLComparePreference_middleTableColumn4; 
		columnLayouts= new ColumnLayoutData[] {
			new ColumnWeightData(10),
			new ColumnWeightData(18),
			new ColumnPixelData(convertWidthInCharsToPixels(column3Text.length()+1), true),
			new ColumnPixelData(convertWidthInCharsToPixels(column4Text.length()+3), true)};
		tablelayout = new TableLayout();
		fMappingsTable.setLayout(tablelayout);
		for (int i=0; i<4; i++)
			tablelayout.addColumnData(columnLayouts[i]);
		column = new TableColumn(fMappingsTable, SWT.NONE);
		column.setText(XMLCompareMessages.XMLComparePreference_middleTableColumn1); 
		column = new TableColumn(fMappingsTable, SWT.NONE);
		column.setText(XMLCompareMessages.XMLComparePreference_middleTableColumn2); 
		column = new TableColumn(fMappingsTable, SWT.NONE);
		column.setText(column3Text);
		column = new TableColumn(fMappingsTable, SWT.NONE);
		column.setText(column4Text);
	
		buttons= new Composite(parent, SWT.NULL);
		buttons.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));
		layout= new GridLayout();
		layout.marginHeight= 0;
		layout.marginWidth= 0;
		buttons.setLayout(layout);
		
		fNewMappingsButton= new Button(buttons, SWT.PUSH);
		fNewMappingsButton.setLayoutData(getButtonGridData(fNewMappingsButton));
		fNewMappingsButton.setText(XMLCompareMessages.XMLComparePreference_middleNew); 
		fNewMappingsButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				addMapping(fAddIdMapButton.getShell());
			}
		});

		fEditMappingsButton= new Button(buttons, SWT.PUSH);
		fEditMappingsButton.setLayoutData(getButtonGridData(fEditMappingsButton));
		fEditMappingsButton.setText(XMLCompareMessages.XMLComparePreference_middleEdit); 
		fEditMappingsButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				editMapping(fEditMappingsButton.getShell());
			}
		});

		fRemoveMappingsButton= new Button(buttons, SWT.PUSH);
		fRemoveMappingsButton.setLayoutData(getButtonGridData(fRemoveMappingsButton));
		fRemoveMappingsButton.setText(XMLCompareMessages.XMLComparePreference_middleRemove); 
		fRemoveMappingsButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				removeMapping(fRemoveMappingsButton.getShell());
			}
		});

		createSpacer(buttons);

		//layout the botton table & its buttons
		label = new Label(parent, SWT.LEFT);
		label.setText(XMLCompareMessages.XMLComparePreference_bottomTableLabel); 
		data = new GridData();
		data.horizontalAlignment = GridData.FILL;
		data.horizontalSpan = 2;
		label.setLayoutData(data);

		fOrderedTable = new Table(parent, SWT.SINGLE | SWT.BORDER | SWT.FULL_SELECTION);
		fOrderedTable.setHeaderVisible(true);	
		data = new GridData(GridData.FILL_BOTH);
		data.heightHint = fOrderedTable.getItemHeight()*2;
		data.widthHint= convertWidthInCharsToPixels(70);
		fOrderedTable.setLayoutData(data);
		
		columnLayouts= new ColumnLayoutData[] {
			new ColumnWeightData(1),
			new ColumnWeightData(1)};
		tablelayout = new TableLayout();
		fOrderedTable.setLayout(tablelayout);
		for (int i=0; i<2; i++)
			tablelayout.addColumnData(columnLayouts[i]);
		column = new TableColumn(fOrderedTable, SWT.NONE);
		column.setText(XMLCompareMessages.XMLComparePreference_bottomTableColumn1); 
		column = new TableColumn(fOrderedTable, SWT.NONE);
		column.setText(XMLCompareMessages.XMLComparePreference_bottomTableColumn2); 
	
		buttons= new Composite(parent, SWT.NULL);
		buttons.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));
		layout= new GridLayout();
		layout.marginHeight= 0;
		layout.marginWidth= 0;
		buttons.setLayout(layout);
		
		fNewOrderedButton= new Button(buttons, SWT.PUSH);
		fNewOrderedButton.setLayoutData(getButtonGridData(fNewOrderedButton));
		fNewOrderedButton.setText(XMLCompareMessages.XMLComparePreference_bottomNew); 
		fNewOrderedButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				addOrdered(fNewOrderedButton.getShell());
			}
		});

		fEditOrderedButton= new Button(buttons, SWT.PUSH);
		fEditOrderedButton.setLayoutData(getButtonGridData(fEditOrderedButton));
		fEditOrderedButton.setText(XMLCompareMessages.XMLComparePreference_bottomEdit); 
		fEditOrderedButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				editOrdered(fEditOrderedButton.getShell());
			}
		});

		fRemoveOrderedButton= new Button(buttons, SWT.PUSH);
		fRemoveOrderedButton.setLayoutData(getButtonGridData(fRemoveOrderedButton));
		fRemoveOrderedButton.setText(XMLCompareMessages.XMLComparePreference_bottomRemove); 
		fRemoveOrderedButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				removeOrdered(fRemoveOrderedButton.getShell());
			}
		});

		createSpacer(buttons);



		fIdMapsTable.setSelection(0);
		fIdMapsTable.setFocus();
		selectionChanged();
		
		return parent;
	}
	
	protected void createSpacer(Composite parent) {
		Label spacer= new Label(parent, SWT.NONE);
		GridData data= new GridData();
		data.horizontalAlignment= GridData.FILL;
		data.verticalAlignment= GridData.BEGINNING;
		data.heightHint= 4;		
		spacer.setLayoutData(data);
	}
	
	private static GridData getButtonGridData(Button button) {
		GridData data= new GridData(GridData.FILL_HORIZONTAL);
		data.widthHint= SWTUtil.getButtonWidthHint(button);
		//data.heightHint= SWTUtil.getButtonHeigthHint(button);
		return data;
	}
	
	@Override
	public void init(IWorkbench workbench) {
		noDefaultAndApplyButton();
	}

	@Override
	public void handleEvent(Event event) {
		// empty implementation
	}

	private void addIdMap(Shell shell) {
		IdMap idmap = new IdMap(false);
		XMLCompareAddIdMapDialog dialog= new XMLCompareAddIdMapDialog(shell,idmap,fIdMaps,fIdMapsInternal,fIdExtensionToName,false);
		if (dialog.open() == Window.OK) {
			if (!fIdMaps.containsKey(idmap.getName())) {
				fIdMaps.put(idmap.getName(),new HashMap());
				if (!idmap.getExtension().isEmpty())
					fIdExtensionToName.put(idmap.getExtension(),idmap.getName());
				newIdMapsTableItem(idmap,true);
			}
		}
	}

	private void renameIdMap(Shell shell) {
		TableItem[] itemsIdMaps = fIdMapsTable.getSelection();
		if (itemsIdMaps.length > 0) {
			IdMap idmap = (IdMap) itemsIdMaps[0].getData();
			String old_name = idmap.getName();
			String old_extension= idmap.getExtension();
			HashMap idmapHS = (HashMap) fIdMaps.get(old_name);
			XMLCompareAddIdMapDialog dialog= new XMLCompareAddIdMapDialog(shell,idmap,fIdMaps,fIdMapsInternal,fIdExtensionToName,true);
			if (dialog.open() == Window.OK) {
				fIdMaps.remove(old_name);
				fIdExtensionToName.remove(old_extension);
				fIdMaps.put(idmap.getName(),idmapHS);
				if (!idmap.getExtension().isEmpty())
					fIdExtensionToName.put(idmap.getExtension(),idmap.getName());
				fIdMapsTable.remove(fIdMapsTable.indexOf(itemsIdMaps[0]));
				newIdMapsTableItem(idmap,true);
			}
		}
	}

	private void removeIdMap(Shell shell) {
		TableItem[] itemsIdMap = fIdMapsTable.getSelection();
		if (itemsIdMap.length > 0) {
//			fIdMaps.remove(itemsIdMap[0].getText());
			String IdMapName= ((IdMap)itemsIdMap[0].getData()).getName();
			fIdMaps.remove( IdMapName );
			fOrderedElements.remove( IdMapName );
			//All the corresponding ID Mappings must be removed as well
			TableItem[] itemsMappings = fMappingsTable.getItems();
			for (TableItem itemsMapping : itemsMappings) {
				itemsMapping.dispose();
			}
			//All the corresponding Ordered entries must be removed as well
			TableItem[] itemsOrdered= fOrderedTable.getItems();
			for (TableItem itemsOrd : itemsOrdered) {
				itemsOrd.dispose();
			}
			//Remove extension
			if (!itemsIdMap[0].getText(2).equals("")) { //$NON-NLS-1$
				fIdExtensionToName.remove(itemsIdMap[0].getText(2));
			}
			itemsIdMap[0].dispose();  //Table is single selection
		}
	}
	
	private void editIdMap(Shell shell) {
		TableItem[] items = fIdMapsTable.getSelection();
		if (items.length > 0) {
			IdMap idmap = (IdMap) items[0].getData();
			XMLCompareEditCopyIdMapDialog dialog= new XMLCompareEditCopyIdMapDialog(shell,idmap,fIdMaps,fIdMapsInternal);
			if (dialog.open() == Window.OK) {
				String new_idmapName = dialog.getResult();
				if (!fIdMaps.containsKey(new_idmapName)) {
					//copy over id mappings
					Vector newMappings = new Vector();
					IdMap newIdMap = new IdMap(new_idmapName, false, newMappings);
					HashMap newIdmapHM = new HashMap();
					fIdMaps.put(newIdMap.getName(),newIdmapHM);
					Vector Mappings = idmap.getMappings();
					for (Enumeration enumeration= Mappings.elements(); enumeration.hasMoreElements(); ) {
						Mapping mapping = (Mapping) enumeration.nextElement();
						Mapping newMapping = new Mapping(mapping.getElement(), mapping.getSignature(), mapping.getIdAttribute());
						newMappings.add(newMapping);
						newIdmapHM.put(newMapping.getKey(), newMapping.getIdAttribute());
					}
					//copy over ordered entries
					ArrayList orderedAL= idmap.getOrdered();
					if (orderedAL != null && orderedAL.size() > 0) {
						ArrayList newOrderedAL= new ArrayList();
						newIdMap.setOrdered(newOrderedAL);
						ArrayList idmapOrdered= new ArrayList();
						fOrderedElements.put(newIdMap.getName(),idmapOrdered);
						for (Iterator iter= orderedAL.iterator(); iter.hasNext();) {
							Mapping ordered= (Mapping) iter.next();
							Mapping newOrdered= new Mapping(ordered.getElement(), ordered.getSignature());
							newOrderedAL.add(newOrdered);
							idmapOrdered.add(newOrdered.getKey());
						}
					}
					
					newIdMapsTableItem(newIdMap,true);
					selectionChanged();
				}
			}
		}
	}

	private void addMapping(Shell shell) {
		TableItem[] items = fIdMapsTable.getSelection();
		if (items.length > 0) {
			IdMap idmap = (IdMap) items[0].getData();
			Mapping mapping = new Mapping();
			HashMap idmapHM = (HashMap) fIdMaps.get(idmap.getName());
			XMLCompareEditMappingDialog dialog= new XMLCompareEditMappingDialog(shell,mapping,idmapHM,false);
			if (dialog.open() == Window.OK) {
				String idmapHMKey = mapping.getKey();
				if (idmapHM == null)
					idmapHM= new HashMap();
				if (!idmapHM.containsKey(idmapHMKey)) {
					idmapHM.put(idmapHMKey, mapping.getIdAttribute());
					newMappingsTableItem(mapping, true);
					Vector mappings = idmap.getMappings();
					mappings.add(mapping);
				}
			}
		}
	}

	private void editMapping(Shell shell) {
		TableItem[] itemsIdMaps = fIdMapsTable.getSelection();		
		TableItem[] itemsMappings = fMappingsTable.getSelection();
		if (itemsMappings.length > 0) {
			IdMap idmap = (IdMap) itemsIdMaps[0].getData();
			HashMap idmapHM = (HashMap) fIdMaps.get(idmap.getName());
			Mapping mapping = (Mapping)itemsMappings[0].getData();
			String idmapHMKey = mapping.getKey();
			idmapHM.remove(idmapHMKey);
			XMLCompareEditMappingDialog dialog= new XMLCompareEditMappingDialog(shell,mapping,null,true);
			if (dialog.open() == Window.OK) {
				idmapHMKey = mapping.getKey();
				idmapHM.put(idmapHMKey, mapping.getIdAttribute());
				fMappingsTable.remove(fMappingsTable.indexOf(itemsMappings[0]));
				newMappingsTableItem(mapping, true);
			}
		}
	}

	private void removeMapping(Shell shell) {
		TableItem[] itemsIdMaps = fIdMapsTable.getSelection();
		TableItem[] itemsMappings = fMappingsTable.getSelection();
		
		if (itemsMappings.length > 0 && itemsIdMaps.length > 0) {
			Mapping mapping = (Mapping)itemsMappings[0].getData();
			IdMap idmap = (IdMap) itemsIdMaps[0].getData();
			HashMap idmapHS = (HashMap) fIdMaps.get( idmap.getName() );
			idmapHS.remove(mapping.getKey());
			Vector mappings= idmap.getMappings();
			mappings.remove(mapping);
			itemsMappings[0].dispose();  //Table is single selection
		}
	}

	private void addOrdered(Shell shell) {
		TableItem[] items = fIdMapsTable.getSelection();
		if (items.length > 0) {
//			Set orderedSet= fOrderedElements.keySet();
//			for (Iterator iter= orderedSet.iterator(); iter.hasNext(); ) {
//				String IdMapName= (String) iter.next();
//				ArrayList ordered= (ArrayList) fOrderedElements.get(IdMapName);
//				for (Iterator iter2= ordered.iterator(); iter2.hasNext(); ) {
//					System.out.println(IdMapName + ": " + iter2.next()); //$NON-NLS-1$
//				}
//			}
			IdMap idmap = (IdMap) items[0].getData();
			Mapping mapping = new Mapping();
			ArrayList idmapAL= (ArrayList) fOrderedElements.get(idmap.getName());
			if (idmapAL == null)
				idmapAL= new ArrayList();
			XMLCompareEditOrderedDialog dialog= new XMLCompareEditOrderedDialog(shell,mapping,idmapAL,false);
			if (dialog.open() == Window.OK) {
				String idmapALKey = mapping.getKey();
				if (!idmapAL.contains(idmapALKey)) {
					idmapAL.add(idmapALKey);
					newOrderedTableItem(mapping, true);
					ArrayList ordered= idmap.getOrdered();
					if (ordered == null) {
						ordered= new ArrayList();
						ordered.add(mapping);
						idmap.setOrdered(ordered);
					} else {
						ordered.add(mapping);
					}
					if (!fOrderedElements.containsKey(idmap.getName()))
						fOrderedElements.put(idmap.getName(), idmapAL);
				}
			}
		}
	}
	
	private void editOrdered(Shell shell) {
		TableItem[] itemsIdMaps = fIdMapsTable.getSelection();		
		TableItem[] itemsOrdered = fOrderedTable.getSelection();
		if (itemsOrdered.length > 0) {
			IdMap idmap = (IdMap) itemsIdMaps[0].getData();
			ArrayList idmapAL = (ArrayList) fOrderedElements.get(idmap.getName());
			Mapping mapping = (Mapping)itemsOrdered[0].getData();
			String idmapALKey = mapping.getKey();
			idmapAL.remove(idmapALKey);
			XMLCompareEditOrderedDialog dialog= new XMLCompareEditOrderedDialog(shell,mapping,null,true);
			if (dialog.open() == Window.OK) {
				idmapALKey = mapping.getKey();
				idmapAL.add(idmapALKey);
				fOrderedTable.remove(fOrderedTable.indexOf(itemsOrdered[0]));
				newOrderedTableItem(mapping, true);
			}
		}
		
	}
	
	private void removeOrdered(Shell shell) {
		TableItem[] itemsIdMaps = fIdMapsTable.getSelection();
		TableItem[] itemsOrdered = fOrderedTable.getSelection();
		if (itemsOrdered.length > 0 && itemsIdMaps.length > 0) {
			Mapping mapping = (Mapping)itemsOrdered[0].getData();
			IdMap idmap = (IdMap) itemsIdMaps[0].getData();
			ArrayList idmapAL = (ArrayList) fOrderedElements.get( idmap.getName() );
			idmapAL.remove(mapping.getKey());
			if (idmapAL.size() <= 0)
				fOrderedElements.remove(idmap.getName());
			ArrayList ordered= idmap.getOrdered();
			ordered.remove(mapping);
			if (ordered.size() <= 0)
				idmap.setOrdered(null);
			itemsOrdered[0].dispose();  //Table is single selection
		}		
	}

	protected TableItem newIdMapsTableItem(IdMap idmap, boolean selected) {
		//find index where to insert table entry
		TableItem[] items = fIdMapsTable.getItems();
		int i= 0;
		while (i<items.length && idmap.getName().compareToIgnoreCase(items[i].getText(0)) > 0)
			i++;
		TableItem item = new TableItem(fIdMapsTable, SWT.NULL, i);
		String[] values = new String[] {idmap.getName(), (idmap.isInternal())?XMLCompareMessages.XMLComparePreference_topTableColumn2internal:XMLCompareMessages.XMLComparePreference_topTableColumn2user,idmap.getExtension()}; 
		item.setText(values);
		item.setData(idmap);
		if (selected) {
			fIdMapsTable.setSelection(i);
			fIdMapsTable.setFocus();
			selectionChanged();
		}
		return item;
	}
	
	protected TableItem newMappingsTableItem(Mapping mapping, boolean selected) {
		TableItem[] items = fMappingsTable.getItems();
		int i= 0;
		while (i<items.length && mapping.getElement().compareToIgnoreCase(items[i].getText(0)) > 0)
			i++;
		TableItem item = new TableItem(fMappingsTable, SWT.NULL, i);
		String idtext = mapping.getIdAttribute();
		String idtype;
		if (idtext.charAt(0)==XMLStructureCreator.ID_TYPE_BODY) {
			idtext = idtext.substring(1,idtext.length());
			idtype = IDTYPE_CHILDBODY;
		} else
			idtype = IDTYPE_ATTRIBUTE;
		
		String[] values = new String[] {mapping.getElement(), mapping.getSignature(), idtext, idtype};
		item.setText(values);
		item.setData(mapping);
		if (selected)
			fMappingsTable.setSelection(i);
	
		return item;
	}

	protected TableItem newOrderedTableItem(Mapping mapping, boolean selected) {
		TableItem[] items = fOrderedTable.getItems();
		int i= 0;
		while (i<items.length && mapping.getElement().compareToIgnoreCase(items[i].getText(0)) > 0)
			i++;

		TableItem item = new TableItem(fOrderedTable, SWT.NULL, i);
		
		String[] values = new String[] {mapping.getElement(), mapping.getSignature()};
		item.setText(values);
		item.setData(mapping);
		if (selected)
			fOrderedTable.setSelection(i);
	
		return item;
	}

	
	protected void fillIdMapsTable() {
		//fill user idmaps from plugin.xml
		fillIdMaps(true);
		
		//fill user idmaps from Preference Store
		fillIdMaps(false);
				
		//add user idmaps that have ordered entries but no id mappings
		//they do not appear in the preference store with name IDMAP_PREFERENCE_NAME
		Set OrderedKeys= fOrderedElements.keySet();
		Set IdMapKeys= fIdMaps.keySet();
		for (Iterator iter_orderedElements= OrderedKeys.iterator(); iter_orderedElements.hasNext();) {
			String IdMapName= (String) iter_orderedElements.next();
			if (!IdMapKeys.contains(IdMapName)) {
				IdMap idmap= new IdMap(IdMapName, false);
				ArrayList idmapOrdered= (ArrayList) fOrderedElements.get(IdMapName);
				setOrdered(idmap, idmapOrdered);
				newIdMapsTableItem(idmap, false);
			}
		}
	}

	private void fillIdMaps(boolean internal) {
		HashMap IdMaps= (internal)?fIdMapsInternal:fIdMaps;
		HashMap OrderedElements= (internal)?fOrderedElementsInternal:fOrderedElements;
		Set IdMapKeys = IdMaps.keySet();
		for (Iterator iter_internal = IdMapKeys.iterator(); iter_internal.hasNext(); ) {
			String IdMapName = (String) iter_internal.next();
			Vector Mappings = new Vector();
			IdMap idmap = new IdMap(IdMapName, internal, Mappings);
			//create mappings of internal idmaps
			HashMap idmapHM = (HashMap) IdMaps.get(IdMapName);
			Set idmapKeys = idmapHM.keySet();
			for (Iterator iter_idmap = idmapKeys.iterator(); iter_idmap.hasNext(); ) {
				Mapping mapping = new Mapping();
				String signature = (String) iter_idmap.next();
				int end_of_signature = signature.lastIndexOf(SIGN_SEPARATOR,signature.length()-2);
				if (end_of_signature < XMLStructureCreator.ROOT_ID.length() + 1)
					mapping.setSignature(""); //$NON-NLS-1$
				else
					mapping.setSignature(signature.substring(XMLStructureCreator.ROOT_ID.length() + 1,end_of_signature));
				mapping.setElement(signature.substring(end_of_signature+1,signature.length()-1));
				mapping.setIdAttribute((String)idmapHM.get(signature));
				Mappings.add(mapping);
			}
			//create ordered mappings
			ArrayList idmapOrdered= (ArrayList) OrderedElements.get(IdMapName);
			if (idmapOrdered != null) {
				setOrdered(idmap, idmapOrdered);
			}
			//set extension
			if (fIdExtensionToName.containsValue(IdMapName)) {
				Set keySet= fIdExtensionToName.keySet();
				String extension= new String();
				for (Iterator iter= keySet.iterator(); iter.hasNext(); ) {
					extension= (String)iter.next();
					if ( ((String)fIdExtensionToName.get(extension)).equals(IdMapName) )
						break;
				}
				idmap.setExtension(extension);
			}
			newIdMapsTableItem(idmap, false);
		}
	}

	protected static void setOrdered(IdMap idmap, ArrayList idmapOrdered) {
		ArrayList Ordered= new ArrayList();
		for (Iterator iter_ordered= idmapOrdered.iterator(); iter_ordered.hasNext();) {
			Mapping mapping= new Mapping();
			String signature= (String) iter_ordered.next();
			int end_of_signature = signature.lastIndexOf(SIGN_SEPARATOR,signature.length()-2);
			if (end_of_signature < XMLStructureCreator.ROOT_ID.length() + 1)
				mapping.setSignature(""); //$NON-NLS-1$
			else
				mapping.setSignature(signature.substring(XMLStructureCreator.ROOT_ID.length() + 1,end_of_signature));
			mapping.setElement(signature.substring(end_of_signature+1,signature.length()-1));
			Ordered.add(mapping);
		}				
		idmap.setOrdered(Ordered);
	}

	@Override
	public boolean performOk() {
		XMLPlugin plugin= XMLPlugin.getDefault();
		if (!plugin.getIdMaps().equals(fIdMaps)
			|| !plugin.getIdExtensionToName().equals(fIdExtensionToName)
			|| !plugin.getOrderedElements().equals(fOrderedElements) )
			plugin.setIdMaps(fIdMaps,fIdExtensionToName,fOrderedElements,true);
			//XMLPlugin.getDefault().setIdMaps(fIdMaps,fIdExtensionToName,null);
		return super.performOk();
	}	
	
	@Override
	public boolean performCancel() {
		fIdMaps = (HashMap) XMLPlugin.getDefault().getIdMaps().clone();
		return super.performCancel();
	}
	
	protected void selectionChanged() {
		TableItem[] items = fIdMapsTable.getSelection();
		if (items.length > 0) {
			//Refresh Mappings Table
			fMappingsTable.removeAll();
			Vector Mappings = ((IdMap)items[0].getData()).getMappings();
			for (Enumeration enumeration = Mappings.elements(); enumeration.hasMoreElements(); ) {
				newMappingsTableItem((Mapping)enumeration.nextElement(), false);
			}
			//Refresh Ordered Table
			fOrderedTable.removeAll();
			ArrayList Ordered= ((IdMap)items[0].getData()).getOrdered();
			if (Ordered != null) {
				for (Iterator iter_ordered= Ordered.iterator(); iter_ordered.hasNext();) {
					newOrderedTableItem((Mapping)iter_ordered.next(), false);
				}
			}
		}
		updateEnabledState();
	}

	/**
	 * Updates the state (enabled, not enabled) of the buttons
	 */
	private void updateEnabledState() {
		TableItem[] itemsIdMaps = fIdMapsTable.getSelection();
		if (itemsIdMaps.length > 0) {
			IdMap idmap = (IdMap) itemsIdMaps[0].getData();
			if (idmap.isInternal()) {
				fRenameIdMapButton.setEnabled(false);
				fRemoveIdMapButton.setEnabled(false);
				fEditIdMapButton.setEnabled(true);
				
				fNewMappingsButton.setEnabled(false);
				fEditMappingsButton.setEnabled(false);
				fRemoveMappingsButton.setEnabled(false);
				
				fNewOrderedButton.setEnabled(false);
				fEditOrderedButton.setEnabled(false);
				fRemoveOrderedButton.setEnabled(false);
			} else {
				fRenameIdMapButton.setEnabled(true);
				fRemoveIdMapButton.setEnabled(true);
				fEditIdMapButton.setEnabled(false);
				
				fNewMappingsButton.setEnabled(true);
				fEditMappingsButton.setEnabled(true);
				fRemoveMappingsButton.setEnabled(true);
				
				fNewOrderedButton.setEnabled(true);
				fEditOrderedButton.setEnabled(true);
				fRemoveOrderedButton.setEnabled(true);
			}
		}
	}

	static protected boolean containsInvalidCharacters(String text) {
		for (int i=0; i<invalidCharacters.length; i++) {
			if (text.indexOf(invalidCharacters[i]) > -1)
				return true;
		}
		return false;
	}
}
