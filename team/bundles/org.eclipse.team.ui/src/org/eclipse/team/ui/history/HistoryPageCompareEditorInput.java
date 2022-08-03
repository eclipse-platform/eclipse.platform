/*******************************************************************************
 * Copyright (c) 2006, 2017 IBM Corporation and others.
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
package org.eclipse.team.ui.history;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.CompareViewerPane;
import org.eclipse.compare.structuremergeviewer.ICompareInput;
import org.eclipse.core.runtime.Adapters;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.team.internal.ui.TeamUIMessages;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.internal.ui.history.DialogHistoryPageSite;
import org.eclipse.team.ui.PageCompareEditorInput;
import org.eclipse.ui.part.IPage;
import org.eclipse.ui.part.Page;

/**
 * Displays a history page combined with the compare/merge infrastructure. This only works properly if the
 * history page adapts to an {@link IHistoryCompareAdapter}.
 *
 * @since 3.3
 */
public class HistoryPageCompareEditorInput extends PageCompareEditorInput {

	private IHistoryPage historyPage;
	private DialogHistoryPageSite site;
	private final Object object;
	private final IHistoryPageSource pageSource;
	private final IPropertyChangeListener changeListener = this::handlePropertyChange;
	private boolean isReplace;

	/**
	 * Create a history page compare editor input for the given page and object.
	 * @param configuration the compare configuration
	 * @param pageSource the page source
	 * @param object the object whose history is to be displayed
	 */
	public HistoryPageCompareEditorInput(CompareConfiguration configuration, IHistoryPageSource pageSource, Object object) {
		super(configuration);
		this.pageSource = pageSource;
		this.object = object;
	}

	@Override
	protected Object prepareInput(IProgressMonitor monitor)
			throws InvocationTargetException, InterruptedException {
		return object;
	}

	@Override
	protected void handleDispose() {
		super.handleDispose();
		if (historyPage != null) {
			historyPage.removePropertyChangeListener(changeListener);
			historyPage.dispose();
		}
	}

	@Override
	protected IPage createPage(CompareViewerPane parent, IToolBarManager toolBarManager) {
		site = new DialogHistoryPageSite(parent.getShell());
		historyPage = (IHistoryPage)pageSource.createPage(object);
		historyPage.setSite(site);
		site.setToolBarManager(toolBarManager);
		((Page) historyPage).createControl(parent);
		historyPage.setInput(object);
		String description = historyPage.getDescription();
		if (description == null)
			description = ""; //$NON-NLS-1$
		setPageDescription(description);
		if (getTitle() == null)
			setTitle(historyPage.getName());
		historyPage.addPropertyChangeListener(changeListener);
		return (IPage)historyPage;
	}

	@Override
	protected ICompareInput asCompareInput(ISelection selection) {
		ICompareInput compareInput = super.asCompareInput(selection);
		if (compareInput != null)
			return compareInput;
		IHistoryCompareAdapter compareAdapter = Adapters.adapt(historyPage, IHistoryCompareAdapter.class);
		if (compareAdapter != null){
			if (selection instanceof IStructuredSelection) {
				IStructuredSelection ss= (IStructuredSelection) selection;
				if (ss.size() == 1) {
					Object o = ss.getFirstElement();
					return compareAdapter.getCompareInput(o);
				}
			}
		}
		return null;
	}

	@Override
	protected ISelectionProvider getSelectionProvider() {
		return site.getSelectionProvider();
	}

	@Override
	protected void prepareInput(ICompareInput input,
			CompareConfiguration configuration, IProgressMonitor monitor)
			throws InvocationTargetException {
		IHistoryCompareAdapter compareAdapter = Adapters.adapt(historyPage, IHistoryCompareAdapter.class);
		if (compareAdapter != null){
			compareAdapter.prepareInput(input, configuration, monitor);
		}
	}

	/**
	 * Return the history page for this input or <code>null</code> if the
	 * page hasn't been created yet.
	 * @return the history page for this input
	 */
	public final IHistoryPage getHistoryPage() {
		return historyPage;
	}

	/**
	 * Handle a property change event from the history page.
	 * @param event the change event
	 */
	protected void handlePropertyChange(PropertyChangeEvent event) {
		if (event.getSource() == historyPage) {
			if (event.getProperty().equals(IHistoryPage.P_NAME)) {
				Display.getDefault().asyncExec(() -> setTitle(historyPage.getName()));
			} else if (event.getProperty().equals(IHistoryPage.P_DESCRIPTION)) {
				Display.getDefault().asyncExec(() -> setPageDescription(historyPage.getDescription()));
			}
		}
	}

	@Override
	public boolean isEditionSelectionDialog() {
		return isReplaceDialog();
	}

	/**
	 * Return whether this compare editor input is being used in a replace
	 * dialog.
	 * @return whether this compare editor input is being used in a replace
	 * dialog
	 */
	protected boolean isReplaceDialog() {
		return isReplace;
	}

	/**
	 * Set whether this compare editor input is being used in a replace
	 * dialog.
	 * @param isReplace whether this compare editor input is being used in a replace
	 * dialog
	 */
	public void setReplace(boolean isReplace) {
		this.isReplace = isReplace;
	}

	@Override
	public String getOKButtonLabel() {
		if (isReplaceDialog())
			return TeamUIMessages.HistoryPageCompareEditorInput_0;
		return super.getOKButtonLabel();
	}

	@Override
	public boolean okPressed() {
		if (!isReplaceDialog())
			return super.okPressed();
		try {
			Object o = getSelectedEdition();
			performReplace(((ICompareInput)o).getRight());
		} catch (CoreException e) {
			Utils.handle(e);
			return false;
		}
		return true;
	}

	/**
	 * A replace has been requested. This method will be
	 * invoked if {@link #isReplaceDialog()} is <code>true</code>
	 * and the user has clicked the "Replace" button.
	 * By default, this method does nothing.
	 * Subclasses may override.
	 * @param selectedObject the selected object
	 * @throws CoreException if an error occurs performing the replace
	 */
	protected void performReplace(Object selectedObject) throws CoreException {
		// By default, do nothing
	}

}
