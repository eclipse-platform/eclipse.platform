/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
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

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.team.internal.ccvs.core.CVSTag;
import org.eclipse.team.internal.ccvs.ui.CVSUIMessages;
import org.eclipse.team.internal.ccvs.ui.IHelpContextIds;
import org.eclipse.team.internal.ccvs.ui.tags.*;
import org.eclipse.team.internal.ui.PixelConverter;
import org.eclipse.team.internal.ui.SWTUtils;
import org.eclipse.ui.PlatformUI;

public class MergeWizardPage extends CVSWizardPage {

	private Text endTagField;
	private Button endTagBrowseButton;
	private TagSource tagSource;
	private Text startTagField;
	private Button startTagBrowseButton;
	private TagRefreshButtonArea tagRefreshArea;
	private CVSTag startTag;
	private CVSTag endTag;
	private Button previewButton;
	private Button noPreviewButton;
	protected boolean preview = true;
	private Button onlyPreviewConflicts;
	private boolean isOnlyPreviewConflicts = false;

	public MergeWizardPage(String pageName, String title, ImageDescriptor titleImage, String description, TagSource tagSource) {
		super(pageName, title, titleImage, description);
		this.tagSource = tagSource;
	}

	@Override
	public void createControl(Composite parent) {
		
		final PixelConverter converter= SWTUtils.createDialogPixelConverter(parent);
		
		final Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(SWTUtils.createGridLayout(1, converter, SWTUtils.MARGINS_DEFAULT));
		// set F1 help
		PlatformUI.getWorkbench().getHelpSystem().setHelp(composite, IHelpContextIds.MERGE_WIZARD_PAGE);
		
		final Composite mainArea = new Composite(composite, SWT.NONE);
		mainArea.setLayoutData(SWTUtils.createHFillGridData());
		mainArea.setLayout(SWTUtils.createGridLayout(2, converter, SWTUtils.MARGINS_NONE));
		
		createEndTagArea(mainArea);
		createStartTagArea(mainArea);
		SWTUtils.equalizeControls(converter, new Button [] { endTagBrowseButton, startTagBrowseButton } );
		
		createPreviewOptionArea(composite, converter);
		createTagRefreshArea(composite);

		Dialog.applyDialogFont(composite);
		setControl(composite);
	}

	private void createPreviewOptionArea(Composite parent, PixelConverter converter) {
		
		final Composite composite= new Composite(parent, SWT.NONE);
		composite.setLayoutData(SWTUtils.createHFillGridData());
		composite.setLayout(SWTUtils.createGridLayout(1, converter, SWTUtils.MARGINS_NONE));
		
		previewButton = SWTUtils.createRadioButton(composite, CVSUIMessages.MergeWizardPage_0);
		if (MergeWizard.isShowModelSync()) {
			onlyPreviewConflicts = SWTUtils.createCheckBox(composite, CVSUIMessages.MergeWizardPage_14);
			GridData data = SWTUtils.createHFillGridData(1);
			data.horizontalIndent = 10;
			onlyPreviewConflicts.setLayoutData(data);
		}
		noPreviewButton = SWTUtils.createRadioButton(composite, CVSUIMessages.MergeWizardPage_1); 
		SelectionAdapter selectionAdapter = new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				preview  = previewButton.getSelection();
				updateEnablements();
			}
		};
		previewButton.setSelection(preview);
		noPreviewButton.setSelection(!preview);
		previewButton.addSelectionListener(selectionAdapter);
		noPreviewButton.addSelectionListener(selectionAdapter);
		if (onlyPreviewConflicts != null) {
			onlyPreviewConflicts.setEnabled(preview);
			onlyPreviewConflicts.setSelection(isOnlyPreviewConflicts);
			onlyPreviewConflicts.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					isOnlyPreviewConflicts = onlyPreviewConflicts.getSelection();
				}
			});
		}
	}
	private void createTagRefreshArea(Composite composite) {
		tagRefreshArea = new TagRefreshButtonArea(getShell(), getTagSource(), null) {
			@Override
			public void refresh(boolean background) {
				super.refresh(background);
				updateStartTag(startTagField.getText());
				updateEndTag(endTagField.getText());
			}
		};
		tagRefreshArea.setRunnableContext(getContainer());
		tagRefreshArea.createArea(composite); 
	}

	private void createEndTagArea(Composite parent) {
		SWTUtils.createLabel(parent, CVSUIMessages.MergeWizardPage_2, 2); 
		
		endTagField = SWTUtils.createText(parent);
		endTagField.addModifyListener(e -> updateEndTag(endTagField.getText()));
		final int endTagIncludeFlags = TagSelectionArea.INCLUDE_VERSIONS | TagSelectionArea.INCLUDE_BRANCHES | TagSelectionArea.INCLUDE_HEAD_TAG;
		TagContentAssistProcessor.createContentAssistant(endTagField, tagSource, endTagIncludeFlags);
		endTagBrowseButton = createPushButton(parent, CVSUIMessages.MergeWizardPage_3); 
		
		endTagBrowseButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				TagSelectionDialog dialog = new TagSelectionDialog(getShell(), getTagSource(), 
						CVSUIMessages.MergeWizardPage_4, 
						CVSUIMessages.MergeWizardPage_5, 
						endTagIncludeFlags,
						false, IHelpContextIds.MERGE_END_PAGE);
				if (dialog.open() == Window.OK) {
					CVSTag selectedTag = dialog.getResult();
					setEndTag(selectedTag);
				}
			}
		});
	}
	
	private void createStartTagArea(Composite parent) {
		
		SWTUtils.createLabel(parent, CVSUIMessages.MergeWizardPage_6, 2); 

		startTagField = SWTUtils.createText(parent);
		startTagField.addModifyListener(e -> updateStartTag(startTagField.getText()));
		TagContentAssistProcessor.createContentAssistant(startTagField, tagSource, TagSelectionArea.INCLUDE_VERSIONS);

		startTagBrowseButton = createPushButton(parent, CVSUIMessages.MergeWizardPage_7); 
		startTagBrowseButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				TagSelectionDialog dialog = new TagSelectionDialog(getShell(), getTagSource(), 
						CVSUIMessages.MergeWizardPage_8, 
						CVSUIMessages.MergeWizardPage_9, 
						TagSelectionDialog.INCLUDE_VERSIONS | TagSelectionDialog.INCLUDE_DATES,
						false, IHelpContextIds.MERGE_START_PAGE);
				if (dialog.open() == Window.OK) {
					CVSTag selectedTag = dialog.getResult();
					setStartTag(selectedTag);
				}
			}
		});   
	}

	protected void updateEndTag(String text) {
		if (endTag == null || !endTag.getName().equals(text)) {
			CVSTag tag = getTagFor(text, false);
			setEndTag(tag);
		}
		updateEnablements();
	}
	
	protected void updateStartTag(String text) {
		if (startTag == null || !startTag.getName().equals(text)) {
			CVSTag tag = getTagFor(text, true);
			setStartTag(tag);
		}
		updateEnablements();
	}

	private CVSTag getTagFor(String text, boolean versionsOnly) {
		if (text.equals(CVSTag.DEFAULT.getName())) {
			if (versionsOnly) return null;
			return CVSTag.DEFAULT;
		}
		if (text.equals(CVSTag.BASE.getName())) {
			if (versionsOnly) return null;
			return CVSTag.BASE;
		}
		CVSTag[] tags;
		if (versionsOnly) {
			tags = tagSource.getTags(new int[] { CVSTag.VERSION, CVSTag.DATE });
		} else {
			tags = tagSource.getTags(new int[] { CVSTag.VERSION, CVSTag.BRANCH, CVSTag.DATE });
		}
		for (CVSTag tag : tags) {
			if (tag.getName().equals(text)) {
				return tag;
			}
		}
		return null;
	}

	protected void setEndTag(CVSTag selectedTag) {
		if (selectedTag == null || endTag == null || !endTag.equals(selectedTag)) {
			endTag = selectedTag;
			if (endTagField != null) {
				String name = endTagField.getText();
				if (endTag != null)
					name = endTag.getName();
				if (!endTagField.getText().equals(name))
					endTagField.setText(name);
				if (startTag == null && endTag != null && endTag.getType() == CVSTag.BRANCH) {
					CVSTag tag = findCommonBaseTag(endTag);
					if (tag != null) {
						setStartTag(tag);
					}
				}
			}
			updateEnablements();           
		}
	}

	protected void setStartTag(CVSTag selectedTag) {
		if (selectedTag == null || startTag != null || endTag == null || !endTag.equals(selectedTag)) {
			startTag = selectedTag;
			if (startTagField != null) {
				String name = startTagField.getText();
				if (startTag != null)
					name = startTag.getName();
				if (!startTagField.getText().equals(name))
					startTagField.setText(name);
			}
			updateEnablements();
		}
	}
	
	private CVSTag findCommonBaseTag(CVSTag tag) {
		CVSTag[] tags = tagSource.getTags(CVSTag.VERSION);
		for (CVSTag potentialMatch : tags) {
			if (potentialMatch.getName().contains(tag.getName())) {
				return potentialMatch;
			}
		}
		return null;
	}

	private void updateEnablements() {
		if (onlyPreviewConflicts != null)
			onlyPreviewConflicts.setEnabled(preview);
		if (endTag == null && endTagField.getText().length() > 0) {
			setErrorMessage(CVSUIMessages.MergeWizardPage_10); 
		} else if (startTag == null && startTagField.getText().length() > 0) {
			setErrorMessage(CVSUIMessages.MergeWizardPage_11); 
		} else if (endTag != null && startTag != null && startTag.equals(endTag)) {
			setErrorMessage(CVSUIMessages.MergeWizardPage_12); 
		} else if (startTag == null && endTag != null && preview) {
			setErrorMessage(CVSUIMessages.MergeWizardPage_13); 
		} else {
			setErrorMessage(null);
		}
		setPageComplete((startTag != null || !preview) && endTag != null && (startTag == null || !startTag.equals(endTag)));
	}

	protected TagSource getTagSource() {
		return tagSource;
	}

	private Button createPushButton(Composite parent, String label) {
		final Button button = new Button(parent, SWT.PUSH);
		button.setText(label);
		button.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		return button;
	}

	public CVSTag getStartTag() {
		return startTag;
	}
	
	public CVSTag getEndTag() {
		return endTag;
	}

	public boolean isPreview() {
		return preview;
	}
	
	public boolean isOnlyPreviewConflicts() {
		return isOnlyPreviewConflicts;
	}
}
