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
 *     Philippe Ombredanne - bug 84808
 *******************************************************************************/
package org.eclipse.team.internal.ccvs.ui.wizards;


import java.util.ArrayList;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.wizard.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.team.internal.ccvs.core.ICVSRemoteFolder;
import org.eclipse.team.internal.ccvs.ui.CVSUIPlugin;
import org.eclipse.team.internal.ccvs.ui.operations.RemoteProjectFolder;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.eclipse.ui.views.navigator.ResourceComparator;

/**
 * Common superclass for CVS wizard pages. Provides convenience methods
 * for widget creation.
 */
public abstract class CVSWizardPage extends WizardPage {
	
	protected static final int LABEL_WIDTH_HINT = 400;
	protected static final int LABEL_INDENT_WIDTH = 32;
	protected static final int LIST_HEIGHT_HINT = 100;
	protected static final int SPACER_HEIGHT = 8;

	private ICVSWizard wizard;
	
	/**
	 * CVSWizardPage constructor comment.
	 * @param pageName  the name of the page
	 */
	public CVSWizardPage(String pageName) {
		super(pageName);
	}
	
	/**
	 * CVSWizardPage constructor comment.
	 * @param pageName  the name of the page
	 * @param title  the title of the page
	 * @param titleImage  the image for the page
	 */
	public CVSWizardPage(String pageName, String title, ImageDescriptor titleImage) {
		super(pageName, title, titleImage);
	}
	/**
	 * CVSWizardPage constructor comment.
	 * @param pageName  the name of the page
	 * @param title  the title of the page
	 * @param titleImage  the image for the page
	 * @param description the description of the page
	 */
	public CVSWizardPage(String pageName, String title, ImageDescriptor titleImage, String description) {
		super(pageName, title, titleImage);
		setDescription(description);
	}
	/**
	 * Creates a new checkbox instance and sets the default layout data.
	 *
	 * @param group  the composite in which to create the checkbox
	 * @param label  the string to set into the checkbox
	 * @return the new checkbox
	 */ 
	protected Button createCheckBox(Composite group, String label) {
		Button button = new Button(group, SWT.CHECK | SWT.LEFT);
		button.setText(label);
		GridData data = new GridData();
		data.horizontalSpan = 2;
		button.setLayoutData(data);
		return button;
	}
	/**
	 * Utility method that creates a combo box
	 *
	 * @param parent  the parent for the new label
	 * @return the new widget
	 */
	protected Combo createCombo(Composite parent) {
		Combo combo = new Combo(parent, SWT.READ_ONLY);
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		data.widthHint = IDialogConstants.ENTRY_FIELD_WIDTH;
		combo.setLayoutData(data);
		return combo;
	}
	/**
	 * Creates composite control and sets the default layout data.
	 * @param parent  the parent of the new composite
	 * @param numColumns  the number of columns for the new composite
	 * @param grabExcess <code>true</code> if the composite should take up the remaining horizontal and vertical space
	 *
	 * @return the newly-created composite
	 */
	protected Composite createComposite(Composite parent, int numColumns, boolean grabExcess) {
		final Composite composite = new Composite(parent, SWT.NULL);
		composite.setLayout(new GridLayout(numColumns, false));
		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, grabExcess, grabExcess));
		return composite;
	}
	
	/**
	 * Utility method that creates a label instance
	 * and sets the default layout data.
	 *
	 * @param parent  the parent for the new label
	 * @param text  the text for the new label
	 * @return the new label
	 */
	public static Label createLabel(Composite parent, String text) {
		return createIndentedLabel(parent, text, 0);
	}
	/**
	 * Utility method that creates a label instance indented by the specified
	 * number of pixels and sets the default layout data.
	 *
	 * @param parent  the parent for the new label
	 * @param text  the text for the new label
	 * @param indent  the indent in pixels, or 0 for none
	 * @return the new label
	 */
	public static Label createIndentedLabel(Composite parent, String text, int indent) {
		Label label = new Label(parent, SWT.LEFT);
		label.setText(text);
		GridData data = new GridData();
		data.horizontalSpan = 1;
		data.horizontalAlignment = GridData.FILL;
		data.horizontalIndent = indent;
		label.setLayoutData(data);
		return label;
	}
	/**
	 * Utility method that creates a label instance with word wrap and sets
	 * the default layout data.
	 *
	 * @param parent  the parent for the new label
	 * @param text  the text for the new label
	 * @param indent  the indent in pixels, or 0 for none
	 * @param widthHint  the nominal width of the label
	 * @return the new label
	 */
	protected Label createWrappingLabel(Composite parent, String text, int indent) {
		return createWrappingLabel(parent, text, indent, 1);
	}
	
	protected Label createWrappingLabel(Composite parent, String text, int indent, int horizontalSpan) {
		Label label = new Label(parent, SWT.LEFT | SWT.WRAP);
		label.setText(text);
		GridData data = new GridData(SWT.FILL, SWT.BEGINNING, true, false);
		data.horizontalIndent = indent;
		data.horizontalSpan = horizontalSpan;
		data.widthHint = LABEL_WIDTH_HINT;
		label.setLayoutData(data);
		return label;
	}
	
	/**
	 * Create a text field specific for this application
	 *
	 * @param parent  the parent of the new text field
	 * @return the new text field
	 */
	static public Text createTextField(Composite parent) {
		Text text = new Text(parent, SWT.SINGLE | SWT.BORDER);
		return layoutTextField(text);
	}
	/**
	 * Create a password field specific for this application
	 *
	 * @param parent  the parent of the new text field
	 * @return the new text field
	 */
	static public Text createPasswordField(Composite parent) {
		Text text = new Text(parent, SWT.SINGLE | SWT.BORDER | SWT.PASSWORD);
		return layoutTextField(text);
	}
	/**
	 * Layout a text or password field specific for this application
	 *
	 * @param parent  the parent of the new text field
	 * @return the new text field
	 */
	static public Text layoutTextField(Text text)  {
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		data.verticalAlignment = GridData.CENTER;
		data.grabExcessVerticalSpace = false;
		data.widthHint = IDialogConstants.ENTRY_FIELD_WIDTH;
		text.setLayoutData(data);
		return text;
	}
	
	/**
	 * Utility method to create a radio button
	 * 
	 * @param parent  the parent of the radio button
	 * @param label  the label of the radio button
	 * @param span  the number of columns to span
	 * @return the created radio button
	 */
	protected Button createRadioButton(Composite parent, String label, int span) {
		Button button = new Button(parent, SWT.RADIO);
		button.setText(label);
		GridData data = new GridData();
		data.horizontalSpan = span;
		button.setLayoutData(data);
		return button;
	}

	protected TreeViewer createResourceSelectionTree(Composite composite, int types, int span) {
		TreeViewer tree = new TreeViewer(composite, SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
		tree.setUseHashlookup(true);
		tree.setContentProvider(getResourceProvider(types));
		tree.setLabelProvider(
			new DecoratingLabelProvider(
				new WorkbenchLabelProvider(), 
				PlatformUI.getWorkbench().getDecoratorManager().getLabelDecorator()));
		tree.setComparator(new ResourceComparator(ResourceComparator.NAME));
		
		GridData data = new GridData(GridData.FILL_BOTH | GridData.GRAB_VERTICAL);
		data.heightHint = LIST_HEIGHT_HINT;
		data.horizontalSpan = span;
		tree.getControl().setLayoutData(data);
		return tree;
	}

	/**
	 * Returns a content provider for <code>IResource</code>s that returns 
	 * only children of the given resource type.
	 */
	protected ITreeContentProvider getResourceProvider(final int resourceType) {
		return new WorkbenchContentProvider() {
			@Override
			public Object[] getChildren(Object o) {
				if (o instanceof IContainer) {
					IResource[] members = null;
					try {
						members = ((IContainer)o).members();
					} catch (CoreException e) {
						//just return an empty set of children
						return new Object[0];
					}
	
					//filter out the desired resource types
					ArrayList<IResource> results = new ArrayList<>();
					for (IResource member : members) {
						//And the test bits with the resource types to see if they are what we want
						if ((member.getType() & resourceType) > 0) {
							results.add(member);
						}
					}
					return results.toArray();
				} else {
					return super.getChildren(o);
				}
			}
		};
	}
	
	protected ICVSWizard getCVSWizard() {
		if (wizard != null) {
			return wizard;
		}
		IWizard wizard = getWizard();
		if (wizard instanceof ICVSWizard) {
			// This is the method that is invoked when the next button is pressed
			// Hence, assume that the page s about to be shown
			return ((ICVSWizard)wizard);
		}
		return null;
	}
	
	public void setCVSWizard(ICVSWizard wizard) {
		this.wizard = wizard;
	}
	
	@Override
	public IWizardPage getNextPage() {
		ICVSWizard w = getCVSWizard();
		if (w != null) {
			// This is the method that is invoked when the next button is pressed
			// Hence, assume that the page s about to be shown
			return w.getNextPage(this, true /* about to show */);
		}
		return super.getNextPage();
	}
	
	@Override
	public boolean canFlipToNextPage() {
		ICVSWizard w = getCVSWizard();
		if (w != null) {
			return isPageComplete() && 
				w.getNextPage(this, false /* about to show */) != null;
		}
		return super.canFlipToNextPage();
	}

	/**
	 * Utility method to get a folder name based on preferences.
	 * Returns the folder name or the project name retrieved from the project metafile 
	 * @param the CVS remote folder
	 * @return a project name
	 */
	static protected String getPreferredFolderName(ICVSRemoteFolder folder) {
		if (CVSUIPlugin.getPlugin().isUseProjectNameOnCheckout() && folder instanceof RemoteProjectFolder ) {
			RemoteProjectFolder rpf = (RemoteProjectFolder) folder;
			if (rpf.hasProjectName()) {
				return rpf.getProjectName();
			}
		}
		return folder.getName();
	}
}
