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
package org.eclipse.team.internal.ccvs.ui.tags;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.team.internal.ccvs.core.CVSTag;
import org.eclipse.team.internal.ccvs.ui.*;
import org.eclipse.team.internal.ccvs.ui.wizards.CVSWizardPage;
import org.eclipse.team.internal.ui.PixelConverter;
import org.eclipse.team.internal.ui.SWTUtils;
import org.eclipse.ui.PlatformUI;

/**
 * General tag selection page that allows the selection of a tag
 * for a particular remote folder
 */
public class TagSelectionWizardPage extends CVSWizardPage {

	private CVSTag selectedTag;
	
	// Needed to dynamicaly create refresh buttons
	private Composite composite;
	
	private int includeFlags;
	
	// Fields for allowing the use of the tag from the local workspace
	boolean allowNoTag = false;
	private Button useResourceTagButton;
	private Button selectTagButton;
	private boolean useResourceTag = false;
	private String helpContextId;
	private TagSelectionArea tagArea;
	private TagSource tagSource;
	
	public TagSelectionWizardPage(String pageName, String title, ImageDescriptor titleImage, String description, TagSource tagSource, int includeFlags) {
		super(pageName, title, titleImage, description);
		this.tagSource = tagSource;
		this.includeFlags = includeFlags;
	}

	/**
	 * Set the help context for the tag selection page. 
	 * This method must be invoked before <code>createControl</code>
	 * @param helpContextId the help context id
	 */
	public void setHelpContxtId(String helpContextId) {
		this.helpContextId = helpContextId;
	}
	
	@Override
	public void createControl(Composite parent) {
		
		final PixelConverter converter= SWTUtils.createDialogPixelConverter(parent);
		
		composite= new Composite(parent, SWT.NONE);
		composite.setLayout(SWTUtils.createGridLayout(1, converter, SWTUtils.MARGINS_DEFAULT));
		composite.setLayoutData(SWTUtils.createHVFillGridData());
		setControl(composite);
		
		// set F1 help
		if (helpContextId != null)
			PlatformUI.getWorkbench().getHelpSystem().setHelp(composite, helpContextId);
		
		if (allowNoTag) {
			SelectionListener listener = new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					useResourceTag = useResourceTagButton.getSelection();
					updateEnablement();
				}
			};
			useResourceTag = true;
			useResourceTagButton = createRadioButton(composite, CVSUIMessages.TagSelectionWizardPage_0, 1); 
			selectTagButton = createRadioButton(composite, CVSUIMessages.TagSelectionWizardPage_1, 1); 
			useResourceTagButton.setSelection(useResourceTag);
			selectTagButton.setSelection(!useResourceTag);
			useResourceTagButton.addSelectionListener(listener);
			selectTagButton.addSelectionListener(listener);
		}
		
		createTagArea();
		updateEnablement();
		Dialog.applyDialogFont(parent);	
	}
	
	private void createTagArea() {
		tagArea = new TagSelectionArea(getShell(), tagSource, includeFlags, null);
		tagArea.setRunnableContext(getContainer());
		tagArea.createArea(composite);
		tagArea.addPropertyChangeListener(new IPropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent event) {
				if (event.getProperty().equals(TagSelectionArea.SELECTED_TAG)) {
					selectedTag = tagArea.getSelection();
					updateEnablement();
				} else if (event.getProperty().equals(TagSelectionArea.OPEN_SELECTED_TAG)) {
					if (selectedTag != null)
						gotoNextPage();
				}

			}
		});
		refreshTagArea();
	}

	private void refreshTagArea() {
		if (tagArea != null) {
			tagArea.refresh();
			tagArea.setSelection(selectedTag);
		}
	}
	
	protected void updateEnablement() {
		tagArea.setEnabled(!useResourceTag);
		setPageComplete(useResourceTag || selectedTag != null);
	}
	
	public CVSTag getSelectedTag() {
		if (useResourceTag) 
			return null;
		return selectedTag;
	}
	
	protected void gotoNextPage() {
		TagSelectionWizardPage.this.getContainer().showPage(getNextPage());
	}
	
	public void setAllowNoTag(boolean b) {
		allowNoTag = b;
	}
	
	@Override
	public void setVisible(boolean visible) {
		super.setVisible(visible);

		if (visible && tagArea != null) {
			tagArea.setFocus();
			if (CVSUIPlugin.getPlugin().getPreferenceStore().getBoolean(ICVSUIConstants.PREF_AUTO_REFRESH_TAGS_IN_TAG_SELECTION_DIALOG)) {
				tagArea.refreshTagList();
			}
		}
	}

	/**
	 * Set the tag source used by this wizard page
	 * @param source the tag source
	 */
	public void setTagSource(TagSource source) {
		this.tagSource = source;
		tagArea.setTagSource(tagSource);
		setSelection(null);
		refreshTagArea();
	}

	/**
	 * Set the selection of the page to the given tag
	 * @param selectedTag
	 */
	public void setSelection(CVSTag selectedTag) {
		if (selectedTag == null && (includeFlags & TagSelectionArea.INCLUDE_HEAD_TAG) > 0) {
			this.selectedTag = CVSTag.DEFAULT;
		} else {
			this.selectedTag = selectedTag;
		}
	}
}
