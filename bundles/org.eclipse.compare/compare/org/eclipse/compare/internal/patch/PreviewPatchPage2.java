/*******************************************************************************
 * Copyright (c) 2005, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Alex Blewitt <alex.blewitt@gmail.com> - replace new Boolean with Boolean.valueOf - https://bugs.eclipse.org/470344
 *******************************************************************************/
package org.eclipse.compare.internal.patch;

import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;
import java.util.regex.Pattern;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.CompareUI;
import org.eclipse.compare.internal.ComparePreferencePage;
import org.eclipse.compare.internal.CompareUIPlugin;
import org.eclipse.compare.internal.ICompareUIConstants;
import org.eclipse.compare.internal.core.patch.FilePatch2;
import org.eclipse.compare.internal.core.patch.Hunk;
import org.eclipse.compare.patch.IHunk;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.events.ExpansionAdapter;
import org.eclipse.ui.forms.events.ExpansionEvent;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;


public class PreviewPatchPage2 extends WizardPage {

	protected final static String PREVIEWPATCHPAGE_NAME= "PreviewPatchPage";  //$NON-NLS-1$

	private static final String EXPAND_PATCH_OPTIONS = "expandPatchOptions"; //$NON-NLS-1$
	private static final String GENERATE_REJECTS = "generateRejects"; //$NON-NLS-1$
	
	final WorkspacePatcher fPatcher;
	private final CompareConfiguration fConfiguration;
	private PatchCompareEditorInput fInput;
	
	private Combo fStripPrefixSegments;
	private Text fFuzzField;
	private Label addedRemovedLines;
	
	private Action fExcludeAction;
	private Action fIncludeAction;
	private Action fIgnoreWhiteSpace;
	private Action fReversePatch;
	private Action fMoveAction;
	
	protected boolean pageRecalculate= true;

	private IDialogSettings settings;
	private ExpandableComposite patchOptions;
	private Button generateRejects;
	private FormToolkit fToolkit;
		
	public PreviewPatchPage2(WorkspacePatcher patcher, CompareConfiguration configuration) {
		super(PREVIEWPATCHPAGE_NAME, PatchMessages.PreviewPatchPage_title, null);
		setDescription(PatchMessages.PreviewPatchPage2_8);
		Assert.isNotNull(patcher);
		Assert.isNotNull(configuration);
		this.fPatcher = patcher;
		this.fConfiguration = configuration;
		this.fConfiguration.addPropertyChangeListener(new IPropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent event) {
				if (event.getProperty().equals(CompareConfiguration.IGNORE_WHITESPACE)){
					rebuildTree();
				}
			}
		});
	}

	public void createControl(Composite parent) {
		fToolkit = new FormToolkit(parent.getDisplay());
		fToolkit.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
		
		final Form form = fToolkit.createForm(parent);
		Composite composite = form.getBody();
		composite.setLayout(new GridLayout());
		composite.setLayoutData(new GridData(GridData.FILL_BOTH));
		initializeDialogUnits(parent);
		
		fInput = new PatchCompareEditorInput(getPatcher(), getCompareConfiguration()) {
			protected void fillContextMenu(IMenuManager manager) {
				if (isShowAll()) {
					manager.add(fIncludeAction);
				}
				manager.add(fExcludeAction);
				manager.add(new Separator());
				manager.add(fMoveAction);
			}
		};
		
		buildPatchOptionsGroup(form);
		
		// Initialize the input
		try {
			fInput.run(null);
		} catch (InterruptedException e) {//ignore
		} catch (InvocationTargetException e) {//ignore
		}
	
		Label label = new Label(composite, SWT.NONE);
		label.setText(PatchMessages.PreviewPatchPage2_9);
		Control c = fInput.createContents(composite);
		initializeActions();
		fInput.contributeDiffViewerToolbarItems(getContributedActions(), getPatcher().isWorkspacePatch());
		fInput.getViewer().addSelectionChangedListener(new ISelectionChangedListener(){
			public void selectionChanged(SelectionChangedEvent event) {
				ISelection s = event.getSelection();
				if (s != null && !s.isEmpty()) {
					if (s instanceof IStructuredSelection) {
						IStructuredSelection ss = (IStructuredSelection) s;
						updateActions(ss);
					}
				}
			}});

		c.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		addedRemovedLines = new Label(composite, SWT.NONE);
		addedRemovedLines.setLayoutData(new GridData(GridData.FILL_HORIZONTAL
				| GridData.VERTICAL_ALIGN_BEGINNING));
			
		setControl(composite);
		
		restoreWidgetValues();
		
		Dialog.applyDialogFont(composite);
	}
	
	private void updateActions(IStructuredSelection ss) {
		fExcludeAction.setEnabled(false);
		fIncludeAction.setEnabled(false);
		for (Iterator it = ss.iterator(); it.hasNext();) {
			Object element = it.next();
			if (element instanceof PatchDiffNode) {
				if (((PatchDiffNode) element).isEnabled()) {
					fExcludeAction.setEnabled(true);
				} else {
					fIncludeAction.setEnabled(true);
				}
			}
		}
	}
	
	/**
	 * Makes sure that at least one hunk is checked off in the tree before
	 * allowing the patch to be applied.
	 */
	private void updateEnablements() {
		boolean atLeastOneIsEnabled = false;
		if (fInput != null)
			atLeastOneIsEnabled = fInput.hasResultToApply();
		setPageComplete(atLeastOneIsEnabled);
	}
	
	private Action[] getContributedActions() {
		return new Action[]{ fIgnoreWhiteSpace };
	}

	private void initializeActions() {
		
		fMoveAction = new Action(PatchMessages.PreviewPatchPage2_RetargetAction, null) {
			public void run() {
				Shell shell = getShell();
				ISelection selection = fInput.getViewer().getSelection();
				PatchDiffNode node = null;
				if (selection instanceof IStructuredSelection) {
					IStructuredSelection ss = (IStructuredSelection) selection;
					if (ss.getFirstElement() instanceof PatchDiffNode) {
						node = (PatchDiffNode) ss.getFirstElement();
					}
				}
				if (node == null)
					return;
				final RetargetPatchElementDialog dialog = new RetargetPatchElementDialog(shell, fPatcher, node);
				int returnCode = dialog.open();
				if (returnCode == Window.OK) {
					// TODO: This could be a problem. We should only rebuild the affected nodes
					rebuildTree();
				}
			}
		};
		fMoveAction .setToolTipText(PatchMessages.PreviewPatchPage2_RetargetTooltip);
		fMoveAction.setEnabled(true);
		fInput.getViewer().addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				IStructuredSelection sel= (IStructuredSelection) event.getSelection();
				Object obj= sel.getFirstElement();
				boolean enable = false;
				if (obj instanceof PatchProjectDiffNode) {
					enable = true;
				} else if (obj instanceof PatchFileDiffNode) {
					PatchFileDiffNode node = (PatchFileDiffNode) obj;
					enable = node.getDiffResult().getDiffProblem();
				} else if (obj instanceof HunkDiffNode) {
					enable = true;
				}
				fMoveAction.setEnabled(enable);
			}
		});
		
		fExcludeAction = new Action(PatchMessages.PreviewPatchPage2_0) {
			public void run() {
				ISelection selection = fInput.getViewer().getSelection();
				if (selection instanceof TreeSelection){
					TreeSelection treeSelection = (TreeSelection) selection;
					Iterator iter = treeSelection.iterator();
					while (iter.hasNext()){
						Object obj = iter.next();
						if (obj instanceof PatchDiffNode){
							PatchDiffNode node = ((PatchDiffNode) obj);
							node.setEnabled(false);
							// TODO: This may require a rebuild if matched hunks are shown
						} 
					}
					updateActions(treeSelection);
				}
				fInput.getViewer().refresh();
			}
		};
		fExcludeAction.setEnabled(true);
		
		fIncludeAction = new Action(PatchMessages.PreviewPatchPage2_1) {
			public void run() {
				ISelection selection = fInput.getViewer().getSelection();
				if (selection instanceof TreeSelection){
					TreeSelection treeSelection = (TreeSelection) selection;
					Iterator iter = treeSelection.iterator();
					while (iter.hasNext()){
						Object obj = iter.next();
						if (obj instanceof PatchDiffNode){
							PatchDiffNode node = ((PatchDiffNode) obj);
							node.setEnabled(true);
							// TODO: This may require a rebuild if matched hunks are shown
						} 
					}
					updateActions(treeSelection);
				}
				fInput.getViewer().refresh();
			}
		};
		fIncludeAction.setEnabled(true);
		
		fIgnoreWhiteSpace = new Action(PatchMessages.PreviewPatchPage2_IgnoreWSAction, CompareUIPlugin.getImageDescriptor(ICompareUIConstants.IGNORE_WHITESPACE_ENABLED)){
			public void run(){
				try {
					getContainer().run(false, true, new IRunnableWithProgress() {
						public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
							monitor.beginTask(PatchMessages.PreviewPatchPage2_IgnoreWhitespace, IProgressMonitor.UNKNOWN);
							if (isChecked() != getPatcher().isIgnoreWhitespace()) {
								if (promptToRebuild(PatchMessages.PreviewPatchPage2_2)) {
									if (getPatcher().setIgnoreWhitespace(isChecked())){
										getCompareConfiguration().setProperty(CompareConfiguration.IGNORE_WHITESPACE, Boolean.valueOf(isChecked()));
									}
								} else {
									fIgnoreWhiteSpace.setChecked(!isChecked());
								}
							}
							monitor.done();
						}
					});
				} catch (InvocationTargetException e) { //ignore
				} catch (InterruptedException e) { //ignore
				}
			}
		};
		fIgnoreWhiteSpace.setChecked(false);
		fIgnoreWhiteSpace.setToolTipText(PatchMessages.PreviewPatchPage2_IgnoreWSTooltip);
		fIgnoreWhiteSpace.setDisabledImageDescriptor(CompareUIPlugin.getImageDescriptor(ICompareUIConstants.IGNORE_WHITESPACE_DISABLED));
		
		fReversePatch = new Action(PatchMessages.PreviewPatchPage_ReversePatch_text){
			public void run(){
				try {
					getContainer().run(true, true, new IRunnableWithProgress() {
						public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
							monitor.beginTask(PatchMessages.PreviewPatchPage2_CalculateReverse, IProgressMonitor.UNKNOWN);
							if (isChecked() != getPatcher().isReversed()) {
								if (promptToRebuild(PatchMessages.PreviewPatchPage2_3)) {
									if (getPatcher().setReversed(isChecked())){
										rebuildTree();
									}
								} else {
									fReversePatch.setChecked(!isChecked());
								}
							}
							monitor.done();
						}
					});
				} catch (InvocationTargetException e) { //ignore
				} catch (InterruptedException e) { //ignore
				}
				
			}
			
		};
		fReversePatch.setChecked(false);
		fReversePatch.setToolTipText(PatchMessages.PreviewPatchPage_ReversePatch_text);
	}
	
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		//Need to handle input and rebuild tree only when becoming visible
		if (visible){
			fillSegmentCombo();
			if (getPatcher().isGitPatch()) {
				int ignore = getPatcher().calculateStripGitPrefixSegments();
				fStripPrefixSegments.select(ignore);
				getPatcher().setStripPrefixSegments(ignore);
			}
			// TODO: We should only do this if the tree needs to be rebuilt
			rebuildTree();
			updateEnablements();
			addedRemovedLines.setText(countLines());
			// expand the first tree item i.e. change
			getCompareConfiguration().getContainer().getNavigator().selectChange(true);
			getContainer().updateButtons();
			getShell().getDefaultButton().setFocus();
		}
	}
	
	private boolean promptToRebuild(final String promptToConfirm){
		final Control ctrl = getControl();
		final boolean[] result = new boolean[] { false };
		if (ctrl != null && !ctrl.isDisposed()){
			Runnable runnable = new Runnable() {
				public void run() {
					if (!ctrl.isDisposed()) {
						// flush any viewers before prompting
						try {
							fInput.saveChanges(null);
						} catch (CoreException e) {
							CompareUIPlugin.log(e);
						}
						result[0] = fInput.confirmRebuild(promptToConfirm);
					}
				}
			};
			if (Display.getCurrent() == null)
				ctrl.getDisplay().syncExec(runnable);
			else
				runnable.run();
		}
		return result[0];
	}
	
	private void rebuildTree(){
		final Control ctrl = getControl();
		if (ctrl != null && !ctrl.isDisposed()){
			Runnable runnable = new Runnable() {
				public void run() {
					if (!ctrl.isDisposed()) {
						fInput.buildTree();
						updateEnablements();
					}
				}
			};
			if (Display.getCurrent() == null)
				ctrl.getDisplay().syncExec(runnable);
			else
				runnable.run();
		}
	}

	private void fillSegmentCombo() {
		if (getPatcher().isWorkspacePatch()) {
			fStripPrefixSegments.setEnabled(false);
		} else {
			fStripPrefixSegments.setEnabled(true);
			int length = 99;
			if (fStripPrefixSegments != null && pageRecalculate) {
				if (fStripPrefixSegments.getItemCount() > 1)
					fStripPrefixSegments.remove(1,
							fStripPrefixSegments.getItemCount() - 1);
				length = getPatcher().calculatePrefixSegmentCount();
				if (length != 99) {
					for (int k = 1; k < length; k++)
						fStripPrefixSegments.add(Integer.toString(k));
					fStripPrefixSegments.select(0);
					getPatcher().setStripPrefixSegments(0);
					pageRecalculate = false;
				}
			}
		}
	}

	/*
	 *	Create the group for setting various patch options
	 */
	private void buildPatchOptionsGroup(final Form form) {
		Composite parent = form.getBody();
			
		patchOptions = fToolkit.createExpandableComposite(parent, ExpandableComposite.TWISTIE | ExpandableComposite.CLIENT_INDENT);
		patchOptions.setText(PatchMessages.PreviewPatchPage_PatchOptions_title);
		patchOptions.setFont(JFaceResources.getFontRegistry().getBold(JFaceResources.DIALOG_FONT));
		patchOptions.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false, 3, 1));
		patchOptions.addExpansionListener(new ExpansionAdapter() {
			public void expansionStateChanged(ExpansionEvent e) {
				form.layout();
			}
		});

		Composite c = new Composite(patchOptions, SWT.NONE);
		patchOptions.setClient(c);
		patchOptions.setExpanded(true);
		GridLayout gl= new GridLayout(); gl.numColumns= 3;
		c.setLayout(gl);
		c.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL|GridData.GRAB_HORIZONTAL));

		// 1st row
		createStripSegmentCombo(c);
		createShowMatchedToggle(c);
		createFuzzFactorChooser(c);

		// 2nd row
		createReversePatchToggle(c);
		createShowRemovedToggle(c);
		createGenerateRejectsToggle(c);

		// register listeners
		final WorkspacePatcher patcher= getPatcher();
		if (fStripPrefixSegments!=null)
			fStripPrefixSegments.addSelectionListener(
				new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					if (patcher.getStripPrefixSegments() != getStripPrefixSegments()) {
						if (promptToRebuild(PatchMessages.PreviewPatchPage2_4)) {
							if (patcher.setStripPrefixSegments(getStripPrefixSegments()))
								rebuildTree();
							}
						}
					}
				}
			);
	

		fFuzzField.addModifyListener(
			new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				if (patcher.getFuzz() != getFuzzFactor()) {
					if (promptToRebuild(PatchMessages.PreviewPatchPage2_5)) {
						if (patcher.setFuzz(getFuzzFactor()))
							rebuildTree();
					} else {
						fFuzzField.setText(Integer.toString(patcher.getFuzz()));
					}
				}
			}
		});
	}

	private void createFuzzFactorChooser(Composite parent) {
		final WorkspacePatcher patcher= getPatcher();
		Composite pair= new Composite(parent, SWT.NONE);
		GridLayout gl= new GridLayout(); gl.numColumns= 3; gl.marginHeight= gl.marginWidth= 0;
		pair.setLayout(gl);
		GridData gd= new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		pair.setLayoutData(gd);

		Label l= new Label(pair, SWT.NONE);
		l.setText(PatchMessages.PreviewPatchPage_FuzzFactor_text);
		l.setToolTipText(PatchMessages.PreviewPatchPage_FuzzFactor_tooltip);
		gd= new GridData(GridData.VERTICAL_ALIGN_CENTER|GridData.HORIZONTAL_ALIGN_BEGINNING|GridData.GRAB_HORIZONTAL);
		l.setLayoutData(gd);

		fFuzzField= new Text(pair, SWT.BORDER);
		fFuzzField.setText("0"); //$NON-NLS-1$
		gd= new GridData(GridData.VERTICAL_ALIGN_CENTER | GridData.HORIZONTAL_ALIGN_END);
		gd.widthHint= 30;
		fFuzzField.setLayoutData(gd);

		Button b= new Button(pair, SWT.PUSH);
		b.setText(PatchMessages.PreviewPatchPage_GuessFuzz_text);
			b.addSelectionListener(new SelectionAdapter() {
					public void widgetSelected(SelectionEvent e) {
						if (promptToRebuild(PatchMessages.PreviewPatchPage2_6)) {
							// Reset the fuzz. We don't use HunkResult.MAXIMUM_FUZZ_FACTOR on purpose here,
							// in order to refresh the tree the result of the calculation needs to be different
							// than the fuzz set in the configuration (see fFuzzField modify listener).
							patcher.setFuzz(-1); 
							int fuzz= guessFuzzFactor(patcher);
							if (fuzz>=0)
								fFuzzField.setText(Integer.toString(fuzz));
						}
					}
				}
			);
		gd= new GridData(GridData.VERTICAL_ALIGN_CENTER);
		int widthHint = convertHorizontalDLUsToPixels(IDialogConstants.BUTTON_WIDTH);
		Point minSize = b.computeSize(SWT.DEFAULT, SWT.DEFAULT, true);
		gd.widthHint = Math.max(widthHint, minSize.x);		
		b.setLayoutData(gd);
	}

	private void createGenerateRejectsToggle(Composite pair) {
		generateRejects = new Button(pair, SWT.CHECK);
		generateRejects.setText(PatchMessages.HunkMergePage_GenerateRejectFile);
		GridData gd = new GridData(GridData.VERTICAL_ALIGN_CENTER
				| GridData.HORIZONTAL_ALIGN_BEGINNING
				| GridData.GRAB_HORIZONTAL);
		generateRejects.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				getPatcher().setGenerateRejectFile(
						generateRejects.getSelection());
			}
		});
		generateRejects.setSelection(false);
		generateRejects.setLayoutData(gd);
	}
	
	private void createShowRemovedToggle(Composite pair) {
		final Button showRemoved = new Button(pair, SWT.CHECK);
		showRemoved.setText(PatchMessages.PreviewPatchPage2_7);
		GridData gd = new GridData(GridData.VERTICAL_ALIGN_CENTER
				| GridData.HORIZONTAL_ALIGN_BEGINNING
				| GridData.GRAB_HORIZONTAL);
		showRemoved.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				fInput.setShowAll(showRemoved.getSelection());
				fInput.updateTree();
			}
		});
		showRemoved.setSelection(fInput.isShowAll());
		showRemoved.setLayoutData(gd);
	}
	
	private void createReversePatchToggle(Composite pair) {
		final Button reversePatch = new Button(pair, SWT.CHECK);
		reversePatch.setText(PatchMessages.PreviewPatchPage_ReversePatch_text);
		GridData gd = new GridData(GridData.VERTICAL_ALIGN_CENTER
				| GridData.HORIZONTAL_ALIGN_BEGINNING
				| GridData.GRAB_HORIZONTAL);
		reversePatch.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (fReversePatch != null) {
					fReversePatch.setChecked(reversePatch.getSelection());
					fReversePatch.run();
					if (fReversePatch.isChecked() != reversePatch.getSelection()) {
						reversePatch.setSelection(fReversePatch.isChecked());
					}
				}
			}
		});
		reversePatch.setSelection(getPatcher().isReversed());
		reversePatch.setLayoutData(gd);
	}

	private void createStripSegmentCombo(Composite parent) {
		final WorkspacePatcher patcher= getPatcher();
		
		Composite pair= new Composite(parent, SWT.NONE);
		GridLayout gl= new GridLayout(); gl.numColumns= 2; gl.marginHeight= gl.marginWidth= 0;
		pair.setLayout(gl);
		GridData gd= new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		pair.setLayoutData(gd);

		Label l= new Label(pair, SWT.NONE);
		l.setText(PatchMessages.PreviewPatchPage_IgnoreSegments_text);
		gd= new GridData(GridData.VERTICAL_ALIGN_CENTER|GridData.HORIZONTAL_ALIGN_BEGINNING);
		l.setLayoutData(gd);

		fStripPrefixSegments= new Combo(pair, SWT.DROP_DOWN|SWT.READ_ONLY|SWT.SIMPLE);
		int prefixCnt= patcher.getStripPrefixSegments();
		String prefix= Integer.toString(prefixCnt);
		fStripPrefixSegments.add(prefix);
		fStripPrefixSegments.setText(prefix);
		gd= new GridData(GridData.VERTICAL_ALIGN_CENTER|GridData.HORIZONTAL_ALIGN_BEGINNING | GridData.GRAB_HORIZONTAL);
		fStripPrefixSegments.setLayoutData(gd);
	}
	
	private void createShowMatchedToggle(Composite parent) {
		final Button showMatched = new Button(parent, SWT.CHECK);
		showMatched.setText(PatchMessages.PreviewPatchPage2_ShowMatched);
		GridData gd = new GridData(GridData.VERTICAL_ALIGN_CENTER
				| GridData.HORIZONTAL_ALIGN_BEGINNING
				| GridData.GRAB_HORIZONTAL);
		showMatched.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				fInput.setShowMatched(showMatched.getSelection());
				rebuildTree();
			}
		});
		showMatched.setSelection(fInput.isShowMatched());
		showMatched.setLayoutData(gd);
	}
	
	public int getFuzzFactor() {
		int fuzzFactor= 0;
		if (fFuzzField!=null) {
			String s= fFuzzField.getText();
			try {
				fuzzFactor= Integer.parseInt(s);
			} catch (NumberFormatException ex) {
				// silently ignored
			}
		}
		return fuzzFactor;
	}
	
	public int getStripPrefixSegments() {
		int stripPrefixSegments= 0;
		if (fStripPrefixSegments!=null) {
			String s= fStripPrefixSegments.getText();
			try {
				stripPrefixSegments= Integer.parseInt(s);
			} catch (NumberFormatException ex) {
				// silently ignored
			}
		}
		return stripPrefixSegments;
	}
	
	private int guessFuzzFactor(final WorkspacePatcher patcher) {
		final int[] result= new int[] { -1 };
		try {
			PlatformUI.getWorkbench().getProgressService().run(true, true,
					new IRunnableWithProgress() {
						public void run(IProgressMonitor monitor) {
							result[0]= patcher.guessFuzzFactor(monitor);
						}
				}
			);
		} catch (InvocationTargetException ex) {
			// NeedWork
		} catch (InterruptedException ex) {
			// NeedWork
		}
		return result[0];
	}
	
	public void ensureContentsSaved() {
		try {
			fInput.saveChanges(new NullProgressMonitor());
		} catch (CoreException e) {
			//ignore
		}
	}

	public WorkspacePatcher getPatcher() {
		return fPatcher;
	}

	public CompareConfiguration getCompareConfiguration() {
		return fConfiguration;
	}
	
	private void restoreWidgetValues() {
		IDialogSettings dialogSettings = CompareUI.getPlugin().getDialogSettings();
		settings = dialogSettings.getSection(PREVIEWPATCHPAGE_NAME);
		if (settings == null) {
			settings = dialogSettings.addNewSection(PREVIEWPATCHPAGE_NAME);
		}
		if (settings != null) {
			if (settings.get(EXPAND_PATCH_OPTIONS) != null)
				patchOptions.setExpanded(settings.getBoolean(EXPAND_PATCH_OPTIONS));
			if (settings.get(GENERATE_REJECTS) != null) {
				generateRejects.setSelection(settings.getBoolean(GENERATE_REJECTS));
				getPatcher().setGenerateRejectFile(generateRejects.getSelection());
			}
		}
	}			
	
	void saveWidgetValues() {
		settings.put(EXPAND_PATCH_OPTIONS, patchOptions.isExpanded());
		settings.put(GENERATE_REJECTS, generateRejects.getSelection());
	}
	
	private String countLines() {
		int added = 0, removed = 0;
		
		IPreferenceStore store = CompareUIPlugin.getDefault().getPreferenceStore();
		String addedLinesRegex = store.getString(ComparePreferencePage.ADDED_LINES_REGEX);
		String removedLinesRegex = store.getString(ComparePreferencePage.REMOVED_LINES_REGEX);
		
		if ((addedLinesRegex == null || "".equals(addedLinesRegex)) //$NON-NLS-1$
				&& (removedLinesRegex == null || "".equals(removedLinesRegex))) { //$NON-NLS-1$
			
			fPatcher.countLines();
			FilePatch2[] fileDiffs = fPatcher.getDiffs();
			for (int i = 0; i < fileDiffs.length; i++) {
				added += fileDiffs[i].getAddedLines();
				removed += fileDiffs[i].getRemovedLines();
			}
			
		} else {

			Pattern addedPattern = Pattern.compile(addedLinesRegex);
			Pattern removedPattern = Pattern.compile(removedLinesRegex);

			FilePatch2[] fileDiffs = fPatcher.getDiffs();
			for (int i = 0; i < fileDiffs.length; i++) {
				IHunk[] hunks = fileDiffs[i].getHunks();
				for (int j = 0; j < hunks.length; j++) {
					String[] lines = ((Hunk) hunks[j]).getLines();
					for (int k = 0; k < lines.length; k++) {
						String line = lines[k];
						if (addedPattern.matcher(line).find())
							added++;
						if (removedPattern.matcher(line).find())
							removed++;
					}
				}
			}
		}
		
		return NLS.bind(PatchMessages.PreviewPatchPage2_AddedRemovedLines,
				new String[] { added + "", removed + "" }); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public void dispose() {
		fToolkit.dispose();
		super.dispose();
	}
}
