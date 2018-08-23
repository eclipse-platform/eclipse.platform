/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
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
package org.eclipse.team.internal.ccvs.ui.tags;

import java.util.Arrays;
import java.util.Vector;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.team.internal.ccvs.core.CVSTag;
import org.eclipse.team.internal.ccvs.ui.CVSUIMessages;
import org.eclipse.team.internal.ccvs.ui.IHelpContextIds;
import org.eclipse.team.internal.ccvs.ui.operations.ITagOperation;
import org.eclipse.team.internal.ui.*;
import org.eclipse.team.internal.ui.dialogs.DetailsDialog;

public class TagAsVersionDialog extends DetailsDialog {

    private static final int TAG_AREA_HEIGHT_HINT = 200;

	private static final int HISTORY_LENGTH = 10;

	private static final String STORE_SECTION = "TagAsVersionDialog"; //$NON-NLS-1$

	private static final String TAG_HISTORY = "tag_history"; //$NON-NLS-1$

	private static IDialogSettings settingsSection;

	private ITagOperation operation;
	
	private Combo tagCombo;
	private Button moveTagButton;
	
	private String tagName = ""; //$NON-NLS-1$
	private boolean moveTag = false;

    private TagSource tagSource;

    private TagSelectionArea tagArea;
	
	public TagAsVersionDialog(Shell parentShell, String title, ITagOperation operation) {
		super(parentShell, title);
		this.tagSource = operation.getTagSource();
		this.operation = operation;
	}
	
	/**
	 * @see DetailsDialog#createMainDialogArea(Composite)
	 */
	protected void createMainDialogArea(Composite parent) {
		
		final int width= convertHorizontalDLUsToPixels(IDialogConstants.MINIMUM_MESSAGE_AREA_WIDTH + 50);
		
		final Label label = SWTUtils.createLabel(parent, CVSUIMessages.TagAction_enterTag); 
		label.setLayoutData(SWTUtils.createGridData(width, SWT.DEFAULT, true, false));

		tagCombo = createDropDownCombo(parent);
		tagName = ""; //$NON-NLS-1$
		tagCombo.setItems(getTagNameHistory());
		tagCombo.setText(tagName);
		tagCombo.addModifyListener(
			new ModifyListener() {
				public void modifyText(ModifyEvent e) {
					tagName = tagCombo.getText();
					updateEnablements();
				}
			}
		);
		
		moveTagButton= SWTUtils.createCheckBox(parent, CVSUIMessages.TagAction_moveTag); 
		moveTagButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				moveTag = moveTagButton.getSelection();
			}
		});

	}

    /* (non-Javadoc)
     * @see org.eclipse.team.internal.ui.dialogs.DetailsDialog#getHelpContextId()
     */
    protected String getHelpContextId() {
        return IHelpContextIds.TAG_AS_VERSION_DIALOG;
    }

	public boolean shouldMoveTag()  {
		return moveTag;
	}
	
	/**
	 * @see DetailsDialog#createDropDownDialogArea(Composite)
	 */
	protected Composite createDropDownDialogArea(Composite parent) {
		
		final PixelConverter converter= SWTUtils.createDialogPixelConverter(parent);
		
		final Composite composite = new Composite(parent, SWT.NONE);
	    composite.setLayout(SWTUtils.createGridLayout(1, converter, SWTUtils.MARGINS_DIALOG));
	    
	    final GridData gridData = new GridData(GridData.FILL_BOTH);
	    gridData.heightHint = TAG_AREA_HEIGHT_HINT;
	    composite.setLayoutData(gridData);
		
		tagArea = new TagSelectionArea(getShell(), tagSource, TagSelectionArea.INCLUDE_VERSIONS, null);
		tagArea.setTagAreaLabel(CVSUIMessages.TagAction_existingVersions);  
		tagArea.setIncludeFilterInputArea(false);
		tagArea.createArea(composite);
		tagArea.addPropertyChangeListener(new IPropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent event) {
                if (event.getProperty().equals(TagSelectionArea.SELECTED_TAG)) {
                    CVSTag tag = tagArea.getSelection();
                    if (tag != null) {
                        tagCombo.setText(tag.getName());
                    }
                } else if (event.getProperty().equals(TagSelectionArea.OPEN_SELECTED_TAG)) {
                    CVSTag tag = tagArea.getSelection();
                    if (tag != null) {
                        tagCombo.setText(tag.getName());
                        okPressed();
                    }
                }
            }
        });
		return composite;
	}
	
	/**
	 * Validates tag name
	 */
	protected void updateEnablements() {
		String message = null;
		if(tagName.length() == 0) {
			message = ""; //$NON-NLS-1$
		} else {		
			IStatus status = CVSTag.validateTagName(tagName);
			if (!status.isOK()) {
				message = status.getMessage();
			}
		}
		setPageComplete(message == null);
		setErrorMessage(message);
		if (tagArea != null) {
		    tagArea.setFilter(tagName);
		}
	}
	
	/**
	 * Returns the tag name entered into this dialog
	 */
	public String getTagName() {
		return tagName;
	}
	
	/**
	 * @return
	 */
	public ITagOperation getOperation() {
		operation.setTag(new CVSTag(tagName, CVSTag.VERSION));
		if (moveTag) {
			operation.moveTag();
		}
		return operation;
	}
	
	/* (non-Javadoc)
     * @see org.eclipse.team.internal.ui.dialogs.DetailsDialog#isMainGrabVertical()
     */
    protected boolean isMainGrabVertical() {
        return false;
    }

	protected Combo createDropDownCombo(Composite parent) {
		Combo combo = new Combo(parent, SWT.DROP_DOWN);
		GridData comboData = new GridData(GridData.FILL_HORIZONTAL);
		comboData.verticalAlignment = GridData.CENTER;
		comboData.grabExcessVerticalSpace = false;
		comboData.widthHint = IDialogConstants.ENTRY_FIELD_WIDTH;
		combo.setLayoutData(comboData);
		return combo;
	}

	protected void okPressed() {
		rememberTagName(tagName);
		super.okPressed();
	}

	protected static String[] getTagNameHistory() {
		IDialogSettings section = getSettingsSection();
		String[] array = section.getArray(TAG_HISTORY);
		return array != null ? array : new String[0];
	}

	private void rememberTagName(String tagName) {
		Object[] tagNameHistory = getTagNameHistory();
		Vector tagNames = new Vector(Arrays.asList(tagNameHistory));
		if (tagNames.contains(tagName)) {
			// The item is in the list. Remove it and add it back at the
			// beginning. If it already was at the beginning this will be a
			// waste of time, but it's not even measurable.
			tagNames.remove(tagName);
		}
		// Most recently used filename goes to the beginning of the list
		tagNames.add(0, tagName);

		// Forget any overflowing items
		while (tagNames.size() > HISTORY_LENGTH) {
			tagNames.remove(HISTORY_LENGTH);
		}
		String[] array = (String[]) tagNames.toArray(new String[tagNames.size()]);
		IDialogSettings section = getSettingsSection();
		section.put(TAG_HISTORY, array);
	}

	private static IDialogSettings getSettingsSection() {
		if (settingsSection != null)
			return settingsSection;

		IDialogSettings settings = TeamUIPlugin.getPlugin().getDialogSettings();
		settingsSection = settings.getSection(STORE_SECTION);
		if (settingsSection != null)
			return settingsSection;

		settingsSection = settings.addNewSection(STORE_SECTION);
		return settingsSection;
	}
}
