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
 *     Brock Janiczak <brockj@tpg.com.au> - Bug 166333 [Wizards] Show diff in CVS commit dialog
 *     Brock Janiczak <brockj@tpg.com.au> - Bug 190674 Conflicting resources message lost when typing in commit wizard
 *******************************************************************************/
package org.eclipse.team.internal.ccvs.ui.wizards;

import java.util.Arrays;

import org.eclipse.compare.*;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.mapping.ResourceTraversal;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.*;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.wizard.*;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.team.core.diff.*;
import org.eclipse.team.internal.ccvs.ui.*;
import org.eclipse.team.internal.ccvs.ui.IHelpContextIds;
import org.eclipse.team.internal.ccvs.ui.mappings.ChangeSetComparator;
import org.eclipse.team.internal.core.subscribers.ActiveChangeSet;
import org.eclipse.team.internal.core.subscribers.ChangeSet;
import org.eclipse.team.internal.ui.*;
import org.eclipse.team.ui.synchronize.*;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.PageBook;

/**
 * This wizard page shows a preview of the commit operation and allows entering
 * a commit comment.
 */
public class CommitWizardCommitPage extends WizardPage implements IPropertyChangeListener {
	
	public static final String SHOW_COMPARE = "ShowCompare"; //$NON-NLS-1$
	private static final String H_WEIGHT_1 = "HWeight1"; //$NON-NLS-1$
	private static final String H_WEIGHT_2 = "HWeight2"; //$NON-NLS-1$
	private static final String V_WEIGHT_1 = "VWeight1"; //$NON-NLS-1$
	private static final String V_WEIGHT_2 = "VWeight2"; //$NON-NLS-1$
	

	private final CommitCommentArea fCommentArea;
	
	private ISynchronizePageConfiguration fConfiguration;
	
	protected final CommitWizard fWizard;
	
	private ParticipantPagePane fPagePane;
	private PageBook bottomChild;

	private boolean fHasConflicts;

	private boolean fIsEmpty;
	
	private SashForm horizontalSash;
	private SashForm verticalSash;
	private Splitter placeholder;
	private boolean showCompare;
	
	public CommitWizardCommitPage(IResource [] resources, CommitWizard wizard) {
		
		super(CVSUIMessages.CommitWizardCommitPage_0); 
		setTitle(CVSUIMessages.CommitWizardCommitPage_0); 
		setDescription(CVSUIMessages.CommitWizardCommitPage_2); 
		
		fWizard= wizard;
		fCommentArea= new CommitCommentArea();
		fCommentArea.setProposedComment(getProposedComment(resources));
		if (resources.length > 0)
			fCommentArea.setProject(resources[0].getProject());
		fWizard.getDiffTree().addDiffChangeListener(new IDiffChangeListener() {
			@Override
			public void propertyChanged(IDiffTree tree, int property, IPath[] paths) {
				// ignore property changes
			}
			@Override
			public void diffsChanged(IDiffChangeEvent event, IProgressMonitor monitor) {
				Utils.syncExec((Runnable) () -> updateEnablements(), CommitWizardCommitPage.this.getControl());
			}
		});
	}
	
	@Override
	public void createControl(Composite parent) {
		initializeDialogUnits(parent);
		Dialog.applyDialogFont(parent);
		final PixelConverter converter= new PixelConverter(parent);
		
		final Composite composite= new Composite(parent, SWT.NONE);
		composite.setLayout(SWTUtils.createGridLayout(1, converter, SWTUtils.MARGINS_DEFAULT));
		// set F1 help
		PlatformUI.getWorkbench().getHelpSystem().setHelp(composite, IHelpContextIds.COMMIT_COMMENT_PAGE);
		
		
		horizontalSash = new SashForm(composite, SWT.HORIZONTAL);
		horizontalSash.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		verticalSash = new SashForm(horizontalSash, SWT.VERTICAL);
		verticalSash.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		createCommentArea(verticalSash, converter);

		placeholder = new Splitter(horizontalSash, SWT.VERTICAL /*any*/);
		placeholder.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		createChangesArea(verticalSash, converter);

		IDialogSettings section = getDialogSettings().getSection(CommitWizard.COMMIT_WIZARD_DIALOG_SETTINGS);
		showCompare = section == null ? false : section.getBoolean(SHOW_COMPARE);
		int vWeight1 = 50;
		int vWeight2 = 50;
		if (section != null) {
			try {
				vWeight1 = section.getInt(V_WEIGHT_1);
				vWeight2 = section.getInt(V_WEIGHT_2);
			} catch (NumberFormatException e) {
			}
		}
		
		int hWeight1 = 35;
		int hWeight2 = 65;
		if (section != null) {
			try {
				hWeight1 = section.getInt(H_WEIGHT_1);
				hWeight2 = section.getInt(H_WEIGHT_2);
			} catch (NumberFormatException e) {
			}
		}

		if (!showCompare) {
			horizontalSash.setMaximizedControl(verticalSash);
		}
		
		verticalSash.setWeights(new int[] {vWeight1, vWeight2});
		horizontalSash.setWeights(new int[] {hWeight1, hWeight2});
		
		//fSashForm.setWeights(weights);
		Dialog.applyDialogFont(parent);
		setControl(composite);
		
		fCommentArea.setFocus();
		
		validatePage(false);
	}
	
	private void createCommentArea(Composite parent, PixelConverter converter) {
		Composite c = new Composite(parent, SWT.NONE);
		c.setLayout(GridLayoutFactory.fillDefaults().margins(0, 0).create());
		
		fCommentArea.createArea(c);
		GridData gd = SWTUtils.createGridData(SWT.DEFAULT, SWT.DEFAULT, SWT.FILL, SWT.FILL, true, true);
		fCommentArea.getComposite().setLayoutData(gd);
		fCommentArea.addPropertyChangeListener(this);
		
		createPlaceholder(c);
	}
	
	private void createChangesArea(Composite parent, PixelConverter converter) {
		
		ISynchronizeParticipant participant= fWizard.getParticipant();
		int size = fWizard.getDiffTree().getAffectedResources().length;
		if (size > getFileDisplayThreshold()) {
			// Create a page book to allow eventual inclusion of changes
			bottomChild = new PageBook(parent, SWT.NONE);
			bottomChild.setLayoutData(SWTUtils.createGridData(SWT.DEFAULT, SWT.DEFAULT, SWT.FILL, SWT.FILL, true, false));
			// Create composite for showing the reason for not showing the changes and a button to show them
			Composite changeDesc = new Composite(bottomChild, SWT.NONE);
			changeDesc.setLayout(SWTUtils.createGridLayout(1, converter, SWTUtils.MARGINS_NONE));
			SWTUtils.createLabel(changeDesc, NLS.bind(CVSUIMessages.CommitWizardCommitPage_1, new String[] { Integer.toString(size), Integer.toString(getFileDisplayThreshold()) })); 
			Button showChanges = new Button(changeDesc, SWT.PUSH);
			showChanges.setText(CVSUIMessages.CommitWizardCommitPage_5); 
			showChanges.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					showChangesPane();
				}
			});
			showChanges.setLayoutData(new GridData());
			bottomChild.showPage(changeDesc);
			// Hide compare pane
			horizontalSash.setMaximizedControl(verticalSash);
		} else {
			final Composite composite= new Composite(parent, SWT.NONE);
			composite.setLayout(SWTUtils.createGridLayout(1, converter, SWTUtils.MARGINS_NONE));
			composite.setLayoutData(SWTUtils.createGridData(SWT.DEFAULT, SWT.DEFAULT, SWT.FILL, SWT.FILL, true, true));
			
			Control c = createChangesPage(composite, participant);
			c.setLayoutData(SWTUtils.createHVFillGridData());
		}
	}

	protected void showChangesPane() {
		Control c = createChangesPage(bottomChild, fWizard.getParticipant());
		bottomChild.setLayoutData(SWTUtils.createGridData(SWT.DEFAULT, SWT.DEFAULT, SWT.FILL, SWT.FILL, true, true));
		bottomChild.showPage(c);
		// Restore compare pane. It has been hidden when file display threshold was reached.
		if (showCompare) {
			horizontalSash.setMaximizedControl(null);
		}
		Dialog.applyDialogFont(getControl());
		((Composite)getControl()).layout();
	}

	private Control createChangesPage(final Composite composite, ISynchronizeParticipant participant) {
		fConfiguration= participant.createPageConfiguration();
		CompareConfiguration cc = new CompareConfiguration();
		cc.setLeftEditable(false);
		cc.setRightEditable(false);
		ParticipantPageCompareEditorInput input = new CommitWizardParticipantPageCompareEditorInput(cc, fConfiguration, participant);
		Control control = input.createContents(composite);
		control.setLayoutData(new GridData(GridData.FILL_BOTH));
		return control;
	}
	
	private class CommitWizardParticipantPageCompareEditorInput extends
			ParticipantPageCompareEditorInput {
		public CommitWizardParticipantPageCompareEditorInput(
				CompareConfiguration cc,
				ISynchronizePageConfiguration configuration,
				ISynchronizeParticipant participant) {
			super(cc, configuration, participant);
		}

		@Override
		protected boolean isOfferToRememberParticipant() {
			return false;
		}

		@Override
		protected CompareViewerSwitchingPane createContentViewerSwitchingPane(
				Splitter parent, int style, CompareEditorInput cei) {
			return super.createContentViewerSwitchingPane(placeholder, style, cei);
		}

		@Override
		protected void setPageDescription(String title) {
			super.setPageDescription(TeamUIMessages.ParticipantPageSaveablePart_0);
		}
	}
	
	private int getFileDisplayThreshold() {
		return CVSUIPlugin.getPlugin().getPreferenceStore().getInt(ICVSUIConstants.PREF_COMMIT_FILES_DISPLAY_THRESHOLD);
	}

	@Override
	public void dispose() {
		super.dispose();
		// Disposing of the page pane will dispose of the page and the configuration
		if (fPagePane != null)
			fPagePane.dispose();
	}
	
	private void createPlaceholder(final Composite composite) {
		final Composite placeholder= new Composite(composite, SWT.NONE);
		placeholder.setLayoutData(new GridData(SWT.DEFAULT, convertHorizontalDLUsToPixels(IDialogConstants.VERTICAL_SPACING) /3));
	}
	
	public String getComment(Shell shell) {
		return fCommentArea.getCommentWithPrompt(shell);
	}
	
	@Override
	public boolean isPageComplete() {
		/* if empty comment is not allowed (see bug 114678) */
		final IPreferenceStore store = CVSUIPlugin.getPlugin()
				.getPreferenceStore();
		final String allowEmptyComment = store
				.getString(ICVSUIConstants.PREF_ALLOW_EMPTY_COMMIT_COMMENTS);
		if (allowEmptyComment.equals(MessageDialogWithToggle.NEVER)) {
			/* but is empty */
			final String comment = fCommentArea.getComment(false);
			if (comment.isEmpty()) {
				return false; // then the page is not complete
			}
		}
		return super.isPageComplete();
	}
	
	@Override
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		expand();
		if (visible && fConfiguration != null) {
			final Viewer viewer= fConfiguration.getPage().getViewer();
			viewer.refresh();
		}
		updateEnablements();
		setFocus();
	}

	protected void expand() {
		if (fConfiguration != null) {
			final Viewer viewer= fConfiguration.getPage().getViewer();
			if (viewer instanceof TreeViewer) {
				try {
					viewer.getControl().setRedraw(false);
					((TreeViewer)viewer).expandAll();
				} finally {
					viewer.getControl().setRedraw(true);
				}
			}
		}
	}
	
	/*
	 * Expand the sync elements and update the page enablement
	 */
	protected void updateForModelChange() {
		Control control = getControl();
		if (control == null || control.isDisposed()) return;
		expand();
		updateEnablements();
	}
	
	public void updateEnablements() {
		if (fConfiguration != null) {
			fHasConflicts = false;
			fIsEmpty = false;
			
			if (fWizard.hasConflicts()) {
				fHasConflicts = true;
			}
			if (!fWizard.hasOutgoingChanges()) {
				fIsEmpty = true;
			}
		}
		
		validatePage(false);
	}

	boolean validatePage(boolean setMessage) {
		if (fCommentArea != null && fCommentArea.getComment(false).length() == 0) {
			final IPreferenceStore store= CVSUIPlugin.getPlugin().getPreferenceStore();
			final String value= store.getString(ICVSUIConstants.PREF_ALLOW_EMPTY_COMMIT_COMMENTS);
			if (MessageDialogWithToggle.NEVER.equals(value)) {
				setPageComplete(false);
				if (setMessage)
					setErrorMessage(CVSUIMessages.CommitWizardCommitPage_3); 
				return false;
			}
		}
		
		if (fHasConflicts) {
			setErrorMessage(CVSUIMessages.CommitWizardCommitPage_4); 
			setPageComplete(false);
			return false;
		}
		if (fIsEmpty) {
			// No need for a message as it should be obvious that there are no resources to commit
			setErrorMessage(null);
			setPageComplete(false);
			return false;
		}
		
		setPageComplete(true);
		setErrorMessage(null);
		return true;
	}
	
	public void setFocus() {
		fCommentArea.setFocus();
		validatePage(true);
	}
	
	@Override
	protected IWizardContainer getContainer() {
		return super.getContainer();
	}

	ResourceTraversal[] getTraversalsToCommit() {
		return fWizard.getParticipant().getContext().getScope().getTraversals();
	}

	@Override
	public void propertyChange(PropertyChangeEvent event) {
		
		if (event.getProperty().equals(CommitCommentArea.OK_REQUESTED)) {
			final IWizardContainer container= getContainer();
			if (container instanceof WizardDialog) {
				final WizardDialog dialog= (WizardDialog)container;
				if (getWizard().canFinish()) {
					try {
						getWizard().performFinish();
					} finally {
						dialog.close();
					}
				}
			}
		}
		if (event.getProperty().equals(CommitCommentArea.COMMENT_MODIFIED)) {
			validatePage(true); 
		}
	}
	
	/*
	 * Get a proposed comment by looking at the active change sets
	 */
	private String getProposedComment(IResource[] resourcesToCommit) {
		StringBuilder comment = new StringBuilder();
		ChangeSet[] sets = CVSUIPlugin.getPlugin().getChangeSetManager().getSets();
		Arrays.sort(sets, new ChangeSetComparator());
		int numMatchedSets = 0;
		for (ChangeSet set : sets) {
			if (isUserSet(set) && containsOne(set, resourcesToCommit)) {
				if(numMatchedSets > 0) comment.append(System.getProperty("line.separator")); //$NON-NLS-1$
				comment.append(set.getComment());
				numMatchedSets++;
			}
		}
		return comment.toString();
	}
	
	private boolean isUserSet(ChangeSet set) {
		if (set instanceof ActiveChangeSet) {
			ActiveChangeSet acs = (ActiveChangeSet) set;
			return acs.isUserCreated();
		}
		return false;
	}

	private boolean containsOne(ChangeSet set, IResource[] resourcesToCommit) {
		for (IResource resource : resourcesToCommit) {
			if (set.contains(resource)) {
				return true;
			}
			if (set instanceof ActiveChangeSet) {
				ActiveChangeSet acs = (ActiveChangeSet) set;
				if (acs.getDiffTree().members(resource).length > 0)
					return true;
			}
		}
		return false;
	}
	
	public void finish() {
		int[] hWeights = horizontalSash.getWeights();
		int[] vWeights = verticalSash.getWeights();
		IDialogSettings section = getDialogSettings().getSection(CommitWizard.COMMIT_WIZARD_DIALOG_SETTINGS);
		if (section == null)
			section= getDialogSettings().addNewSection(CommitWizard.COMMIT_WIZARD_DIALOG_SETTINGS);
		if (showCompare) {
			section.put(H_WEIGHT_1, hWeights[0]);
			section.put(H_WEIGHT_2, hWeights[1]);
		}
		section.put(V_WEIGHT_1, vWeights[0]);
		section.put(V_WEIGHT_2, vWeights[1]);
		section.put(SHOW_COMPARE, showCompare);
	}

	public void showComparePane(boolean showCompare) {
		this.showCompare = showCompare;
		if (showCompare) {
			horizontalSash.setMaximizedControl(null);
		} else {
			horizontalSash.setMaximizedControl(verticalSash);
		}
		
	}
	
}    

