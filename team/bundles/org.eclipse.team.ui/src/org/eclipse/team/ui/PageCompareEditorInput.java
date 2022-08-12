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
package org.eclipse.team.ui;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.CompareEditorInput;
import org.eclipse.compare.CompareViewerPane;
import org.eclipse.compare.IContentChangeListener;
import org.eclipse.compare.IContentChangeNotifier;
import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.Splitter;
import org.eclipse.compare.structuremergeviewer.ICompareInput;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.OpenEvent;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.internal.ui.synchronize.LocalResourceTypedElement;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.IPage;
import org.eclipse.ui.progress.IProgressService;

/**
 * Abstract class for hosting a page based structure input view for the purposes
 * of feeding compare viewers.
 * <p>
 * This class is not intended to be subclassed by clients outside of the Team framework.
 *
 * @since 3.3
 */
public abstract class PageCompareEditorInput extends CompareEditorInput implements IContentChangeListener {

	private CompareViewerPane pagePane;
	private ICompareInput hookedInput;

	/**
	 * Create a page compare editor input.
	 * @param configuration the compare configuration
	 */
	protected PageCompareEditorInput(CompareConfiguration configuration) {
		super(configuration);
	}

	@Override
	protected CompareViewerPane createStructureInputPane(Composite parent) {
		pagePane = new CompareViewerPane(parent, SWT.BORDER | SWT.FLAT) {
			@Override
			public void selectionChanged(SelectionChangedEvent ev) {
				ISelection selection = ev.getSelection();
				StructuredSelection newSelection = convertSelection(selection, false);
				SelectionChangedEvent newEv = new SelectionChangedEvent(pagePane, newSelection);
				super.selectionChanged(newEv);
			}
			private StructuredSelection convertSelection(ISelection selection, boolean prepare) {
				ICompareInput ci = asCompareInput(selection);
				StructuredSelection newSelection;
				if (ci != null) {
					if (prepare)
						prepareCompareInput(ci);
					newSelection = new StructuredSelection(ci);
				} else {
					newSelection = StructuredSelection.EMPTY;
				}
				return newSelection;
			}
			@Override
			public ISelection getSelection() {
				return convertSelection(getSelectionProvider().getSelection(), false);
			}
			@Override
			public Object getInput() {
				return PageCompareEditorInput.this.getCompareResult();
			}
			@Override
			public void open(OpenEvent event) {
				ISelection selection = event.getSelection();
				StructuredSelection newSelection = convertSelection(selection, true);
				super.open(new OpenEvent((Viewer)event.getSource(), newSelection));
			}
			@Override
			public void doubleClick(DoubleClickEvent event) {
				ISelection selection = event.getSelection();
				StructuredSelection newSelection = convertSelection(selection, true);
				super.doubleClick(new DoubleClickEvent((Viewer)event.getSource(), newSelection));
			}

			@Override
			public void setInput(Object input) {
				super.setInput(input);
				Composite c = getParent();
				if (c instanceof Splitter)
					((Splitter)c).setVisible(this, true);
				layout(true);
			}
		};
		ToolBarManager toolBarManager = CompareViewerPane.getToolBarManager(pagePane);
		IPage page = createPage(pagePane, toolBarManager);
		pagePane.setContent(page.getControl());
		if (parent instanceof Splitter)
			((Splitter)parent).setVisible(pagePane, false);
		hookupListeners();
		return pagePane;
	}

	/**
	 * Create the page for this part and return the top level control
	 * for the page.
	 * @param parent the parent composite
	 * @param toolBarManager the toolbar manager for the page
	 * @return the top-level control for the page
	 */
	protected abstract IPage createPage(CompareViewerPane parent, IToolBarManager toolBarManager);

	/**
	 * Return the selection provider for the page. This method is
	 * called after the page is created in order to register a
	 * selection listener on the page.
	 * @return the selection provider for the page
	 */
	protected abstract ISelectionProvider getSelectionProvider();

	/**
	 * Set the title of the page's page to the given text. The title
	 * will appear in the header of the pane containing the page.
	 * @param title the page's title
	 */
	protected void setPageDescription(String title) {
		pagePane.setText(title);
	}

	@Override
	protected void handleDispose() {
		super.handleDispose();
		cleanupListeners();
		unhookContentChangeListener();
	}

	private void hookupListeners() {
		ISelectionProvider selectionProvider = getSelectionProvider();
		if (selectionProvider != null)
			selectionProvider.addSelectionChangedListener(pagePane);
		if (selectionProvider instanceof StructuredViewer) {
			StructuredViewer sv = (StructuredViewer) selectionProvider;
			sv.addOpenListener(pagePane);
			sv.addDoubleClickListener(pagePane);
		}
	}

	private void cleanupListeners() {
		ISelectionProvider selectionProvider = getSelectionProvider();
		if (selectionProvider != null)
			selectionProvider.removeSelectionChangedListener(pagePane);
		if (selectionProvider instanceof StructuredViewer) {
			StructuredViewer sv = (StructuredViewer) selectionProvider;
			sv.removeOpenListener(pagePane);
			sv.removeDoubleClickListener(pagePane);
		}
	}

	private void hookContentChangeListener(ICompareInput node) {
		if (hookedInput == node)
			return;
		unhookContentChangeListener();
		hookedInput = node;
		ITypedElement left = node.getLeft();
		if(left instanceof IContentChangeNotifier) {
			((IContentChangeNotifier)left).addContentChangeListener(this);
		}
		ITypedElement right = node.getRight();
		if(right instanceof IContentChangeNotifier) {
			((IContentChangeNotifier)right).addContentChangeListener(this);
		}
	}

	private void unhookContentChangeListener() {
		if (hookedInput != null) {
			ITypedElement left = hookedInput.getLeft();
			if(left instanceof IContentChangeNotifier) {
				((IContentChangeNotifier)left).addContentChangeListener(this);
			}
			ITypedElement right = hookedInput.getRight();
			if(right instanceof IContentChangeNotifier) {
				((IContentChangeNotifier)right).addContentChangeListener(this);
			}
		}
	}

	/**
	 * Return a compare input that represents the selection.
	 * This input is used to feed the structure and content
	 * viewers. By default, a compare input is returned if the selection is
	 * of size 1 and the selected element implements <code>ICompareInput</code>.
	 * Subclasses may override.
	 * @param selection the selection
	 * @return a compare input representing the selection
	 */
	protected ICompareInput asCompareInput(ISelection selection) {
		if (selection != null && selection instanceof IStructuredSelection) {
			IStructuredSelection ss= (IStructuredSelection) selection;
			if (ss.size() == 1) {
				Object o = ss.getFirstElement();
				if(o instanceof ICompareInput) {
					return (ICompareInput)o;
				}
			}
		}
		return null;
	}

	/**
	 * Convenience method that calls {@link #prepareInput(ICompareInput, CompareConfiguration, IProgressMonitor)}
	 * with a progress monitor.
	 * @param input the compare input to be prepared
	 */
	protected final void prepareCompareInput(final ICompareInput input) {
		if (input == null)
			return;
		// Don't allow the use of shared documents with PageSaveableParts
		Object left = input.getLeft();
		if (left instanceof LocalResourceTypedElement) {
			LocalResourceTypedElement lrte = (LocalResourceTypedElement) left;
			lrte.enableSharedDocument(false);
		}
		IProgressService manager = PlatformUI.getWorkbench().getProgressService();
		try {
			// TODO: we need a better progress story here (i.e. support for cancellation) bug 127075
			manager.busyCursorWhile(monitor -> {
				prepareInput(input, getCompareConfiguration(), monitor);
				hookContentChangeListener(input);
			});
		} catch (InvocationTargetException e) {
			Utils.handle(e);
		} catch (InterruptedException e) {
			// Ignore
		}
	}

	@Override
	public void contentChanged(IContentChangeNotifier source) {
		setDirty(true);
	}

	@Override
	public boolean canRunAsJob() {
		return true;
	}

	/**
	 * Prepare the compare input for display in a content viewer. This method is
	 * called from {@link #prepareCompareInput(ICompareInput)} and may be called
	 * from a non-UI thread. This method should not be called by others.
	 * @param input the input
	 * @param configuration the compare configuration
	 * @param monitor a progress monitor
	 * @throws InvocationTargetException if an error occurs
	 */
	protected abstract void prepareInput(ICompareInput input, CompareConfiguration configuration, IProgressMonitor monitor) throws InvocationTargetException;


}
