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
 *******************************************************************************/
package org.eclipse.team.internal.ccvs.ui.wizards;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.Splitter;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.team.core.diff.*;
import org.eclipse.team.core.mapping.IResourceDiffTree;
import org.eclipse.team.internal.ccvs.ui.CVSUIMessages;
import org.eclipse.team.internal.ccvs.ui.CVSUIPlugin;
import org.eclipse.team.internal.ccvs.ui.IHelpContextIds;
import org.eclipse.team.internal.ccvs.ui.mappings.ModelSynchronizeWizard;
import org.eclipse.team.internal.ui.*;
import org.eclipse.team.ui.synchronize.*;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.PageBook;

/**
 * Page that displays the compare input for sharing
 */
public class SharingWizardSyncPage extends CVSWizardPage implements IDiffChangeListener {
	
	// Constant keys used to store last size for this page
	private static final String PAGE_HEIGHT = "SyncPageHeight"; //$NON-NLS-1$
	private static final String PAGE_WIDTH = "SyncPageWidth"; //$NON-NLS-1$
	
	private ParticipantPageCompareEditorInput input;
	private ISynchronizePageConfiguration configuration;
	private IProject project;
	
	PageBook pageBook;
	private Control syncPage;
	private Control noChangesPage;
	
	private int width;
	private int height;
	private SharingWizardPageActionGroup sharingWizardPageActionGroup;
	private Button fCheckbox;
	
	public SharingWizardSyncPage(String pageName, String title, ImageDescriptor titleImage, String description) {
		super(pageName, title, titleImage, description);
	}

	public void setProject(IProject project) {
		this.project = project;
	}
	
	@Override
	public void createControl(Composite parent) {
		
		final PixelConverter converter= SWTUtils.createDialogPixelConverter(parent);
		
		final Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(SWTUtils.createGridLayout(1, converter, SWTUtils.MARGINS_DEFAULT));
		setControl(composite);
		
		pageBook = new PageBook(composite, SWT.NONE);
		pageBook.setLayoutData(SWTUtils.createHVFillGridData());
		
		syncPage = createSyncPage(pageBook);
		
		noChangesPage = createNoChangesPage(pageBook);
		noChangesPage.setLayoutData(SWTUtils.createHVFillGridData());
		
		updatePage();
		
		PlatformUI.getWorkbench().getHelpSystem().setHelp(composite, IHelpContextIds.SHARING_SYNC_PAGE);
		Dialog.applyDialogFont(parent);	
	}
	
	private IResourceDiffTree getDiffTree() {
		if (configuration == null)
			return null;
		return getParticipant().getContext().getDiffTree();
	}
	
	private Control createSyncPage(PageBook pageBook) {
		Composite composite = createComposite(pageBook, 1, false);
		input = createCompareInput();
		Control c = input.createContents(composite);
		if (c instanceof Splitter) {
			Splitter s = (Splitter) c;
			// hide the content pane by maximizing the outline control
			s.setMaximizedControl(s.getChildren()[0]);
		}
		c.setLayoutData(new GridData(GridData.FILL_BOTH));
		getDiffTree().addDiffChangeListener(this);
		
		fCheckbox= new Button(composite, SWT.CHECK);
		fCheckbox.setLayoutData(SWTUtils.createHFillGridData());
		fCheckbox.setText(CVSUIMessages.SharingWizardSyncPage_12); 
		fCheckbox.setSelection(true);
		
		return composite;
	}
	
	private Control createNoChangesPage(PageBook pageBook) {
		Composite composite = createComposite(pageBook, 1, false);
		createWrappingLabel(composite, NLS.bind(CVSUIMessages.SharingWizardSyncPage_3, new String[] { project.getName() }), 0); 
		return composite;
	}

	/* private */ void showErrors(final IStatus[] status) {
		if (status.length == 0) return;
		getShell().getDisplay().syncExec(() -> {
			String title = CVSUIMessages.SharingWizardSyncPage_8;
			if (status.length == 1) {
				IStatus s = status[0];
				if (s.getException() instanceof CoreException) {
					s = ((CoreException) s.getException()).getStatus();
				}
				ErrorDialog.openError(getShell(), title, null, s);
			} else {
				MultiStatus multi = new MultiStatus(CVSUIPlugin.ID, 0, status, CVSUIMessages.SharingWizardSyncPage_9,
						null);
				ErrorDialog.openError(getShell(), title, null, multi);
			}
		});
	}
	
	private ParticipantPageCompareEditorInput createCompareInput() {	
		ISynchronizeParticipant participant = createParticipant();
		configuration = participant.createPageConfiguration();
		configuration.setProperty(ISynchronizePageConfiguration.P_TOOLBAR_MENU, new String[] {ISynchronizePageConfiguration.NAVIGATE_GROUP, SharingWizardPageActionGroup.ACTION_GROUP});
		sharingWizardPageActionGroup = new SharingWizardPageActionGroup();
		configuration.addActionContribution(sharingWizardPageActionGroup);
		configuration.setRunnableContext(getContainer());
		
		CompareConfiguration cc = new CompareConfiguration();
		cc.setLeftEditable(false);
		cc.setRightEditable(false);
		ParticipantPageCompareEditorInput part = new ParticipantPageCompareEditorInput(cc, configuration, participant) {
			@Override
			protected boolean isOfferToRememberParticipant() {
				return false;
			}
		};
		return part;
	}

	private ISynchronizeParticipant createParticipant() {
		return ModelSynchronizeWizard.createWorkspaceParticipant(Utils.getResourceMappings(new IProject[] { project }), getShell());
	}
	
	@Override
	public void dispose() {
		if (input != null) {
			input.getParticipant().dispose();
		}
	}
	
	@Override
	public void setPreviousPage(IWizardPage page) {
		// There's no going back from this page
		super.setPreviousPage(null);
	}

	private void updatePage() {
		Display.getDefault().syncExec(() -> {
			if (pageBook.isDisposed())
				return;
			if (getDiffTree().isEmpty()) {
				pageBook.showPage(noChangesPage);
			} else {
				pageBook.showPage(syncPage);
			}
		});
	}

	public ModelSynchronizeParticipant getParticipant() {
		return (ModelSynchronizeParticipant)configuration.getParticipant();
	}
	
	@Override
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if (syncPage.isVisible()) {
			initializeSize();
			getShell().setSize(Math.max(width, 300), Math.max(height, 300));
			if(input != null) {
				Viewer viewer = input.getPageConfiguration().getPage().getViewer();
				if(viewer instanceof AbstractTreeViewer && !viewer.getControl().isDisposed()) {
					((AbstractTreeViewer)viewer).expandToLevel(2);
				}
			}
		}
	}
	
	private void initializeSize() {
		IDialogSettings settings = getDialogSettings();
		if (settings != null) {
			try {
				width = settings.getInt(PAGE_WIDTH);
				height = settings.getInt(PAGE_HEIGHT);
			} catch (NumberFormatException e) {
				// Ignore and go on;
			}
		}
		if (width == 0) width = 640;
		if (height == 0) height = 480;
	}

	/**
	 * Save the size of the page so it can be opened with the same size next time
	 */
	public void saveSettings() {
		IDialogSettings settings = getDialogSettings();
		if (settings != null) {
			Point size = getShell().getSize();
			settings.put(PAGE_WIDTH, size.x);
			settings.put(PAGE_HEIGHT, size.y);
		}
	}
	
	public boolean commitChanges() {
		return fCheckbox != null ? fCheckbox.getSelection() && hasOutgoingChanges() : false;
	}
	
	private boolean hasOutgoingChanges() {
		IResourceDiffTree tree = getDiffTree();
		return tree != null && tree.hasMatchingDiffs(ResourcesPlugin.getWorkspace().getRoot().getFullPath(), new FastDiffFilter() {
			@Override
			public boolean select(IDiff diff) {
				if (diff instanceof IThreeWayDiff) {
					IThreeWayDiff twd = (IThreeWayDiff) diff;
					return twd.getDirection() == IThreeWayDiff.OUTGOING || twd.getDirection() == IThreeWayDiff.CONFLICTING;
				}
				return false;
			}
		});
	}

	/**
	 * @return Returns the project.
	 */
	public IProject getProject() {
		return project;
	}

	@Override
	public void diffsChanged(IDiffChangeEvent event, IProgressMonitor monitor) {
		showErrors(event.getErrors());
		updatePage();
	}

	@Override
	public void propertyChanged(IDiffTree tree, int property, IPath[] paths) {
		// Ignore
	}
}
