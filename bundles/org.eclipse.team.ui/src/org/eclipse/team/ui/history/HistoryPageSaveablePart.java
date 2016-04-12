/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.ui.history;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.IContentChangeNotifier;
import org.eclipse.compare.structuremergeviewer.ICompareInput;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.*;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.internal.ui.history.DialogHistoryPageSite;
import org.eclipse.team.ui.PageSaveablePart;
import org.eclipse.team.ui.SaveablePartDialog;
import org.eclipse.ui.part.Page;

/**
 * Displays a history page combined with the compare/merge infrastructure. This only works properly if the
 * history page adapts to an {@link IHistoryCompareAdapter}.
 *
 * @deprecated use {@link HistoryPageCompareEditorInput}
 * @since 3.2
 */
@Deprecated
public class HistoryPageSaveablePart extends PageSaveablePart {

	private IHistoryPage historyPage;
	private DialogHistoryPageSite site;
	private final Object object;
	private final IHistoryPageSource pageSource;

	/**
	 * Show the history for the object in a dialog. The history will only be
	 * shown if an {@link IHistoryPageSource} can be found for the object.
	 * @param shell the parent sell
	 * @param object the object
	 * @return whether the object had an {@link IHistoryPageSource} available or not
	 */
	public static boolean showHistoryInDialog(Shell shell, Object object) {
		IHistoryPageSource pageSource = HistoryPageSource.getHistoryPageSource(object);
		if (pageSource != null && pageSource.canShowHistoryFor(object)) {
			CompareConfiguration cc = new CompareConfiguration();
			cc.setLeftEditable(isFile(object));
			cc.setRightEditable(false);
			HistoryPageSaveablePart input = new HistoryPageSaveablePart(shell, cc, pageSource, object);
			try {
				SaveablePartDialog cd = new SaveablePartDialog(shell, input);
				cd.setBlockOnOpen(true);
				cd.open();
			} finally {
				input.dispose();
			}
			return true;
		}
		return false;
	}

	private static boolean isFile(Object object) {
		IResource resource = Utils.getResource(object);
		return (resource != null && resource.getType() == IResource.FILE);
	}

	/**
	 * Create a history page part for the given page and object.
	 * @param shell the parent shell
	 * @param configuration the compare configuration
	 * @param pageSource the page source
	 * @param object the object whose history is to be displayed
	 */
	public HistoryPageSaveablePart(Shell shell, CompareConfiguration configuration, IHistoryPageSource pageSource, Object object) {
		super(shell,configuration);
		this.pageSource = pageSource;
		this.object = object;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.PageSaveablePart#getTitle()
	 */
	@Override
	public String getTitle() {
		return historyPage.getName();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.PageSaveablePart#getTitleImage()
	 */
	@Override
	public Image getTitleImage() {
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.compare.IContentChangeListener#contentChanged(org.eclipse.compare.IContentChangeNotifier)
	 */
	@Override
	public void contentChanged(IContentChangeNotifier source) {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.PageSaveablePart#createPage(org.eclipse.swt.widgets.Composite, org.eclipse.jface.action.ToolBarManager)
	 */
	@Override
	protected Control createPage(Composite parent, ToolBarManager toolBarManager) {
		site = new DialogHistoryPageSite(getShell());
		historyPage = (IHistoryPage)pageSource.createPage(object);
		historyPage.setSite(site);
		site.setToolBarManager(toolBarManager);
		((Page) historyPage).createControl(parent);
		historyPage.setInput(object);
		String description = historyPage.getDescription();
		if (description == null)
			description = ""; //$NON-NLS-1$
		setPageDescription(description);
		return ((Page) historyPage).getControl();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.PageSaveablePart#getPageSelectionProvider()
	 */
	@Override
	protected final ISelectionProvider getSelectionProvider() {
		return site.getSelectionProvider();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.PageSaveablePart#getCompareInput(org.eclipse.jface.viewers.ISelection)
	 */
	@Override
	protected ICompareInput getCompareInput(ISelection selection) {
		ICompareInput compareInput = super.getCompareInput(selection);
		if (compareInput != null)
			return compareInput;
		IHistoryCompareAdapter compareAdapter = (IHistoryCompareAdapter) Utils.getAdapter(historyPage, IHistoryCompareAdapter.class);
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

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.PageSaveablePart#prepareInput(org.eclipse.compare.structuremergeviewer.ICompareInput, org.eclipse.compare.CompareConfiguration, org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	protected void prepareInput(ICompareInput input, CompareConfiguration configuration, IProgressMonitor monitor) throws InvocationTargetException {
		IHistoryCompareAdapter compareAdapter = (IHistoryCompareAdapter) Utils.getAdapter(historyPage, IHistoryCompareAdapter.class);
		if (compareAdapter != null){
			compareAdapter.prepareInput(input, configuration, monitor);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.SaveablePartAdapter#dispose()
	 */
	@Override
	public void dispose() {
		super.dispose();
		if (historyPage != null)
			historyPage.dispose();
	}
}
