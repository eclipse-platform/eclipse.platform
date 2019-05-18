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

package org.eclipse.team.internal.ccvs.ui.wizards;

import java.util.*;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.team.core.Team;
import org.eclipse.team.internal.ccvs.core.CVSException;
import org.eclipse.team.internal.ccvs.core.ICVSFile;
import org.eclipse.team.internal.ccvs.core.client.Command;
import org.eclipse.team.internal.ccvs.core.client.Command.KSubstOption;
import org.eclipse.team.internal.ccvs.core.resources.CVSWorkspaceRoot;
import org.eclipse.team.internal.ccvs.core.syncinfo.ResourceSyncInfo;
import org.eclipse.team.internal.ccvs.ui.CVSUIMessages;
import org.eclipse.team.internal.ccvs.ui.IHelpContextIds;
import org.eclipse.team.internal.ui.SWTUtils;
import org.eclipse.team.internal.ui.preferences.FileTypeTable;
import org.eclipse.team.internal.ui.preferences.FileTypeTable.Item;
import org.eclipse.ui.PlatformUI;

/**                
 * 
 */
public class CommitWizardFileTypePage extends WizardPage {

	private final Collection fExtensions;
	private final Collection<Item> fNames;
	
	public CommitWizardFileTypePage(Collection unknownExtensions, Collection unknownNames) {
		super(CVSUIMessages.CommitWizardFileTypePage_0); 
		setTitle(CVSUIMessages.CommitWizardFileTypePage_0); 
		setDescription(CVSUIMessages.CommitWizardFileTypePage_2); 
		
		fNames = new ArrayList<>();
		for (final Iterator iter = unknownNames.iterator(); iter.hasNext();) {
			final String name = (String) iter.next();
			fNames.add(new FileTypeTable.Name(name, false));
		}
		
		fExtensions= new ArrayList();
		for (final Iterator iter = unknownExtensions.iterator(); iter.hasNext();) {
			final String extension= (String)iter.next();
			fExtensions.add(new FileTypeTable.Extension(extension, false));
		}
	}
	
	@Override
	public void createControl(Composite parent) {
		
		initializeDialogUnits(parent);
		
		final Composite composite= new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout(1, false));
		// set F1 help
		PlatformUI.getWorkbench().getHelpSystem().setHelp(composite, IHelpContextIds.COMMIT_FILE_TYPES_PAGE);
		
		final Label label= new Label(composite, SWT.WRAP);
		label.setText(CVSUIMessages.CommitWizardFileTypePage_3); 
		label.setLayoutData(SWTUtils.createHFillGridData());
		Dialog.applyDialogFont(composite);
		
		final List items= new ArrayList();
		items.addAll(fNames);
		items.addAll(fExtensions);
		
		new FileTypeTable(composite, items, true);

		setControl(composite);
		
	}

	public void getModesForExtensions(Map toSave, Map notToSave) {
		getModes(fExtensions, toSave, notToSave);
	}

	public void getModesForNames(Map toSave, Map notToSave) {
		getModes(fNames, toSave, notToSave);
	}

	private static void getModes(Collection items, Map toSave, Map notToSave) {
		for (final Iterator iter = items.iterator(); iter.hasNext();) {
			final FileTypeTable.Item item= (FileTypeTable.Item)iter.next();
			final Map destination= item.save ? toSave : notToSave;
			destination.put(item.name, Integer.valueOf(item.mode));
		}
	}

	public KSubstOption getOption(IFile file) throws CVSException {
		final String extension= file.getFileExtension();
		if (extension != null) {
			for (Iterator iter = fExtensions.iterator(); iter.hasNext();) {
				final FileTypeTable.Item element = (FileTypeTable.Item) iter.next();
				if (element.name.equals(extension)) {
					return element.mode == Team.TEXT ? KSubstOption.getDefaultTextMode() : Command.KSUBST_BINARY;
				}
			}
		} else {
			final String name= file.getName();
			for (Iterator iter = fNames.iterator(); iter.hasNext();) {
				FileTypeTable.Item item = (FileTypeTable.Item) iter.next();
				if (item.name.equals(name)) {
					return item.mode == Team.TEXT ? KSubstOption.getDefaultTextMode() : Command.KSUBST_BINARY;
				}
			}
		}
		final ICVSFile cvsFile = CVSWorkspaceRoot.getCVSFileFor(file);
		ResourceSyncInfo fileInfo = cvsFile.getSyncInfo();
		return fileInfo != null ? fileInfo.getKeywordMode() : KSubstOption.fromFile(file);
	}
	
	public static void saveExtensionMappings(Map modesToPersist) {
		
		final String [] extensions= new String [modesToPersist.size()];
		final int [] modes= new int[modesToPersist.size()];
		
		int index= 0;
		for (Iterator iter= modesToPersist.keySet().iterator(); iter.hasNext();) {
			extensions[index]= (String) iter.next();
			modes[index]= ((Integer)modesToPersist.get(extensions[index])).intValue();
			++index;
		}
		Team.getFileContentManager().addExtensionMappings(extensions, modes);
	}
	
	public static void saveNameMappings(Map modesToPersist) {
		
		final String [] names= new String [modesToPersist.size()];
		final int [] modes= new int[modesToPersist.size()];
		
		int index= 0;
		for (Iterator iter= modesToPersist.keySet().iterator(); iter.hasNext();) {
			names[index]= (String) iter.next();
			modes[index]= ((Integer)modesToPersist.get(names[index])).intValue();
			++index;
		}
		Team.getFileContentManager().addNameMappings(names, modes);
	}
}
