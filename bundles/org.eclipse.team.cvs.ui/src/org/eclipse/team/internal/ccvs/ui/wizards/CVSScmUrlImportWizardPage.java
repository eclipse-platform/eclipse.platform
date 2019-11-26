/*******************************************************************************
 * Copyright (c) 2011, 2012 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ccvs.ui.wizards;

import java.net.URI;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.team.core.ScmUrlImportDescription;
import org.eclipse.team.internal.ccvs.core.filesystem.CVSURI;
import org.eclipse.team.internal.ccvs.ui.CVSUIMessages;
import org.eclipse.team.internal.ccvs.ui.IHelpContextIds;
import org.eclipse.team.internal.ui.SWTUtils;
import org.eclipse.team.ui.IScmUrlImportWizardPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;

public class CVSScmUrlImportWizardPage extends WizardPage implements IScmUrlImportWizardPage {
	
	private ScmUrlImportDescription[] descriptions;
	private Button useHead;
	private TableViewer bundlesViewer;
	private Label counterLabel;

	private static final String CVS_PAGE_USE_HEAD = "org.eclipse.team.cvs.ui.import.page.head"; //$NON-NLS-1$

	class CVSLabelProvider extends StyledCellLabelProvider implements ILabelProvider {

		@Override
		public Image getImage(Object element) {
			return PlatformUI.getWorkbench().getSharedImages().getImage(IDE.SharedImages.IMG_OBJ_PROJECT);
		}

		@Override
		public String getText(Object element) {
			return getStyledText(element).getString();
		}

		@Override
		public void update(ViewerCell cell) {
			StyledString string = getStyledText(cell.getElement());
			cell.setText(string.getString());
			cell.setStyleRanges(string.getStyleRanges());
			cell.setImage(getImage(cell.getElement()));
			super.update(cell);
		}

		private StyledString getStyledText(Object element) {
			StyledString styledString = new StyledString();
			if (element instanceof ScmUrlImportDescription) {
				ScmUrlImportDescription description = (ScmUrlImportDescription) element;
				String project = description.getProject();
				URI scmUrl = description.getUri();
				String version = getTag(scmUrl);
				String host = getServer(scmUrl);
				styledString.append(project);
				if (version != null) {
					styledString.append(' ');
					styledString.append(version, StyledString.DECORATIONS_STYLER);
				}
				styledString.append(' ');
				styledString.append('[', StyledString.DECORATIONS_STYLER);
				styledString.append(host, StyledString.DECORATIONS_STYLER);
				styledString.append(']', StyledString.DECORATIONS_STYLER);
				return styledString;
			}
			styledString.append(element.toString());
			return styledString;
		}
	}

	/**
	 * Constructs the page.
	 */
	public CVSScmUrlImportWizardPage() {
		super("cvs", CVSUIMessages.CVSScmUrlImportWizardPage_0, null); //$NON-NLS-1$
		setDescription(CVSUIMessages.CVSScmUrlImportWizardPage_1);
	}

	@Override
	public void createControl(Composite parent) {
		Composite comp = SWTUtils.createHVFillComposite(parent, SWTUtils.MARGINS_NONE, 1);
		Composite group = SWTUtils.createHFillComposite(comp, SWTUtils.MARGINS_NONE, 1);

		Button versions = SWTUtils.createRadioButton(group, CVSUIMessages.CVSScmUrlImportWizardPage_3);
		useHead = SWTUtils.createRadioButton(group, CVSUIMessages.CVSScmUrlImportWizardPage_2);
		SelectionListener listener = new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				bundlesViewer.refresh(true);
			}
		};
		versions.addSelectionListener(listener);
		useHead.addSelectionListener(listener);

		Table table = new Table(comp, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL);
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.heightHint = 200;
		gd.widthHint = 225;
		table.setLayoutData(gd);

		bundlesViewer = new TableViewer(table);
		bundlesViewer.setLabelProvider(new CVSLabelProvider());
		bundlesViewer.setContentProvider(new ArrayContentProvider());
		bundlesViewer.setComparator(new ViewerComparator());
		counterLabel = new Label(comp, SWT.NONE);
		counterLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		setControl(comp);
		setPageComplete(true);

		// Initialize versions versus HEAD
		IDialogSettings settings = getWizard().getDialogSettings();
		boolean useHEAD= settings != null && settings.getBoolean(CVS_PAGE_USE_HEAD);
		useHead.setSelection(useHEAD);
		versions.setSelection(!useHEAD);

		if (descriptions != null) {
			bundlesViewer.setInput(descriptions);
			updateCount();
		}

		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, IHelpContextIds.CVS_SCM_URL_IMPORT_PAGE);
	}

	@Override
	public boolean finish() {
		boolean head = false;
		if (getControl() != null) {
			head = useHead.getSelection();
			// store settings
			IDialogSettings settings = getWizard().getDialogSettings();
			if (settings != null) {
				settings.put(CVS_PAGE_USE_HEAD, head);
			}
		} else {
			// use whatever was used last time
			IDialogSettings settings = getWizard().getDialogSettings();
			if (settings != null) {
				head = settings.getBoolean(CVS_PAGE_USE_HEAD);
			}
		}

		if (head) {
			// modify tags on bundle import descriptions
			for (ScmUrlImportDescription description : descriptions) {
				URI scmUri = description.getUri();
				description.setUrl(removeTag(scmUri));
			}
		}
		
		return true;
	}
	
	@Override
	public ScmUrlImportDescription[] getSelection() {
		return descriptions;
	}

	@Override
	public void setSelection(ScmUrlImportDescription[] descriptions) {
		this.descriptions = descriptions;
		// fill viewer
		if (bundlesViewer != null) {
			bundlesViewer.setInput(descriptions);
			updateCount();
		}
	}

	/**
	 * Updates the count of bundles that will be imported
	 */
	private void updateCount() {
		counterLabel.setText(NLS.bind(CVSUIMessages.CVSScmUrlImportWizardPage_4, Integer.valueOf(descriptions.length)));
		counterLabel.getParent().layout();
	}

	private static String getTag(URI scmUri) {
		return CVSURI.fromUri(scmUri).getTag().getName();
	}
	
	/**
	 * Remove tag attributes from the given URI reference. Results in the URI
	 * pointing to HEAD.
	 * 
	 * @param scmUri
	 *            a SCM URI reference to modify
	 * @return Returns the content of the stripped URI as a string.
	 */
	private static String removeTag(URI scmUri) {
		StringBuilder sb = new StringBuilder();
		sb.append(scmUri.getScheme()).append(':');
		String ssp = scmUri.getSchemeSpecificPart();
		int j = ssp.indexOf(';');
		if (j != -1) {
			sb.append(ssp.substring(0, j));
			String[] params = ssp.substring(j).split(";"); //$NON-NLS-1$
			for (String param : params) {
				// PDE way of providing tags
				if (param.startsWith("tag=")) { //$NON-NLS-1$
					// ignore
				} else if (param.startsWith("version=")) { //$NON-NLS-1$
					// ignore
				} else {
					if (param != null && !param.isEmpty()) {
						sb.append(';').append(param);
					}
				}
			}
		} else {
			sb.append(ssp);
		}
		return sb.toString();
	}
	
	private static String getServer(URI scmUri) {
		return CVSURI.fromUri(scmUri).getRepository().getHost();
	}
}
