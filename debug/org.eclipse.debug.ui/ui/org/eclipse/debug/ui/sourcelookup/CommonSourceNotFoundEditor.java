/*******************************************************************************
 * Copyright (c) 2003, 2018 IBM Corporation and others.
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
package org.eclipse.debug.ui.sourcelookup;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchesListener2;
import org.eclipse.debug.core.model.IDebugElement;
import org.eclipse.debug.core.model.ISourceLocator;
import org.eclipse.debug.core.sourcelookup.AbstractSourceLookupDirector;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.IDebugHelpContextIds;
import org.eclipse.debug.internal.ui.sourcelookup.SourceLookupManager;
import org.eclipse.debug.internal.ui.sourcelookup.SourceLookupUIMessages;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.JFaceColors;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IReusableEditor;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.EditorPart;

/**
 * Default editor displayed when source is not found. Displays a button to modify
 * the source lookup path.
 * <p>
 * This editor's id is <code>IDebugUIConstants.ID_COMMON_SOURCE_NOT_FOUND_EDITOR</code>
 * (value <code>org.eclipse.debug.ui.sourcelookup.CommonSourceNotFoundEditor</code>).
 * </p>
 * <p>
 * This class may be instantiated and subclassed.
 * </p>
 * @see AbstractSourceLookupDirector
 * @see CommonSourceNotFoundEditorInput
 * @since 3.2
 */
public class CommonSourceNotFoundEditor extends EditorPart implements IReusableEditor  {

	/**
	 * Text widgets used for this editor
	 */
	private Text fText;

	/**
	 * Launch listener to handle launch events, or <code>null</code> if none
	 */
	private ILaunchesListener2 fLaunchesListener;

	@Override
	public void doSave(IProgressMonitor monitor) {
	}

	@Override
	public void doSaveAs() {
	}

	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		setSite(site);
		setInput(input);
		initialize();
	}

	@Override
	public boolean isDirty() {
		return false;
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	@Override
	public void createPartControl(Composite parent) {
		GridLayout topLayout = new GridLayout();
		GridData data = new GridData();
		topLayout.numColumns = 1;
		topLayout.verticalSpacing = 10;
		parent.setLayout(topLayout);
		parent.setLayoutData(data);
		parent.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_WHITE));

		fText = new Text(parent,SWT.READ_ONLY|SWT.WRAP);
		data = new GridData(GridData.FILL_HORIZONTAL);
		data.grabExcessHorizontalSpace = true;
		fText.setLayoutData(data);
		fText.setForeground(JFaceColors.getErrorText(fText.getDisplay()));
		fText.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_WHITE));
		if (getEditorInput() != null) {
			setInput(getEditorInput());
		}

		createButtons(parent);

		Dialog.applyDialogFont(parent);

		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, IDebugHelpContextIds.NO_SOURCE_EDITOR);
	}

	/**
	 * Create buttons to be displayed in this editor
	 *
	 * @param parent composite to create the buttons in.
	 */
	protected void createButtons(Composite parent) {
		GridData data;
		Button button = new Button(parent, SWT.PUSH);
		data = new GridData();
		data.grabExcessHorizontalSpace = false;
		data.grabExcessVerticalSpace = false;
		button.setLayoutData(data);
		button.setText(SourceLookupUIMessages.addSourceLocation_addButton2);
		button.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent evt) {
				editSourceLookupPath();
			}
		});
	}

	/**
	 * Edits the source lookup path associated with the active debug context.
	 * After the path is edited, source lookup is performed again and this
	 * editor is closed.
	 */
	protected void editSourceLookupPath(){
		ISourceLocator locator = null;
		ILaunch launch = null;
		IAdaptable selection = DebugUITools.getDebugContext();
		if(selection == null) {
			new MessageDialog(getSite().getShell(),
					SourceLookupUIMessages.CommonSourceNotFoundEditor_0,
					null,
					SourceLookupUIMessages.CommonSourceNotFoundEditor_1,
					MessageDialog.INFORMATION,
					new String[] {IDialogConstants.OK_LABEL}, 0).open();
			return;
		}
		if (selection.getAdapter(ILaunch.class) != null ) {
			launch = selection.getAdapter(ILaunch.class);
			locator = launch.getSourceLocator();
		}
		else if (selection.getAdapter(IDebugElement.class) != null ) {
			launch = selection.getAdapter(IDebugElement.class).getLaunch();
			locator = launch.getSourceLocator();
		}
		else {
			return;  //should not occur
		}
		if (locator == null || !(locator instanceof AbstractSourceLookupDirector)) {
			return;
		}
		final SourceLookupDialog dialog = new SourceLookupDialog(DebugUIPlugin.getShell(),(AbstractSourceLookupDirector) locator);
		if(dialog.open() == Window.OK) {
			IWorkbenchPage page = getEditorSite().getPage();
			SourceLookupManager.getDefault().displaySource(getArtifact(), page, true);
			closeEditor();
		}
	}

	@Override
	public void setFocus() {
		if (fText != null) {
			fText.setFocus();
		}
	}

	@Override
	public void setInput(IEditorInput input) {
		super.setInput(input);
		setPartName(input.getName());
		if (fText != null) {
			fText.setText(getText());
		}
		firePropertyChange(PROP_INPUT);
	}

	/**
	 * Return the text to be displayed in this editor. The text is reset each time
	 * the editor input is set.
	 *
	 * @return the text to be displayed in this editor
	 */
	protected String getText() {
		return getEditorInput().getToolTipText() + "\n"; //$NON-NLS-1$
	}

	/**
	 * Closes this editor.
	 */
	protected void closeEditor()
	{
		final IEditorPart editor = this;
		DebugUIPlugin.getStandardDisplay().syncExec(() -> {
			IWorkbenchWindow activeWorkbenchWindow = DebugUIPlugin.getActiveWorkbenchWindow();
			if (activeWorkbenchWindow != null) {
				IWorkbenchPage activePage = activeWorkbenchWindow.getActivePage();
				if (activePage != null) {
					activePage.closeEditor(editor, false);
				}
			}
		});
	}

	@Override
	public void dispose() {
		if (fLaunchesListener != null) {
			DebugPlugin.getDefault().getLaunchManager().removeLaunchListener(fLaunchesListener);
		}
		super.dispose();
	}

	/**
	 * Returns the artifact this editor was opened for (i.e. the artifact that source
	 * was not found for), or <code>null</code>
	 *
	 * @return artifact with associated source or <code>null</code>
	 */
	protected Object getArtifact() {
		IEditorInput editorInput = getEditorInput();
		if (editorInput instanceof CommonSourceNotFoundEditorInput) {
			CommonSourceNotFoundEditorInput input = (CommonSourceNotFoundEditorInput) editorInput;
			return input.getArtifact();
		}
		return null;
	}

	/**
	 * Initialize this editor.
	 * Called after <code>init(IEditorSite, IEditorInput)</code>. By default, a launch
	 * listener is added to close this editor when the associated launch terminates.
	 * Subclasses may override.
	 */
	protected void initialize()
	{
		fLaunchesListener = new ILaunchesListener2() {
			@Override
			public void launchesTerminated(ILaunch[] launches) {
				Object artifact = getArtifact();
				if (artifact instanceof IDebugElement) {
					IDebugElement element = (IDebugElement)artifact;
					for (ILaunch launch : launches) {
						if (launch.equals(element.getLaunch())) {
							closeEditor();
							return;
						}
					}
				}
			}

			@Override
			public void launchesRemoved(ILaunch[] launches) {
				launchesTerminated(launches);
			}

			@Override
			public void launchesAdded(ILaunch[] launches) {
			}

			@Override
			public void launchesChanged(ILaunch[] launches) {
			}};

		DebugPlugin.getDefault().getLaunchManager().addLaunchListener(fLaunchesListener);
	}
}


