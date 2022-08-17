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
import java.util.ArrayList;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.CompareEditorInput;
import org.eclipse.compare.CompareUI;
import org.eclipse.compare.CompareViewerPane;
import org.eclipse.compare.CompareViewerSwitchingPane;
import org.eclipse.compare.IContentChangeListener;
import org.eclipse.compare.IContentChangeNotifier;
import org.eclipse.compare.IPropertyChangeNotifier;
import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.Splitter;
import org.eclipse.compare.contentmergeviewer.IFlushable;
import org.eclipse.compare.internal.CompareEditor;
import org.eclipse.compare.internal.CompareEditorInputNavigator;
import org.eclipse.compare.structuremergeviewer.ICompareInput;
import org.eclipse.core.runtime.Adapters;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.internal.ui.TeamUIMessages;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.internal.ui.synchronize.LocalResourceTypedElement;
import org.eclipse.team.internal.ui.synchronize.SynchronizePageConfiguration;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.IProgressService;

/**
 * Abstract class for hosting a page based structure input view for the purposes
 * of feeding compare viewers.
 *
 * @since 3.2
 * @noextend This class is not intended to be subclassed by clients outside of the Team framework.
 * @deprecated Clients should use a subclass of {@link CompareEditorInput}
 *      and {@link CompareUI#openCompareDialog(org.eclipse.compare.CompareEditorInput)}
 */
@Deprecated
public abstract class PageSaveablePart extends SaveablePartAdapter implements IContentChangeListener{

	private CompareConfiguration cc;
	Shell shell;

	// Tracking of dirty state
	private boolean fDirty= false;
	private ArrayList<Object> fDirtyViewers= new ArrayList<>();
	private IPropertyChangeListener fDirtyStateListener;

	//	 SWT controls
	private CompareViewerSwitchingPane fContentPane;
	private CompareViewerPane fEditionPane;
	private CompareViewerSwitchingPane fStructuredComparePane;
	private Control control;

	// Configuration options
	private boolean showContentPanes = true;

	/**
	 * Create a saveable part.
	 * @param shell the shell for the part
	 * @param compareConfiguration the compare configuration
	 */
	protected PageSaveablePart(Shell shell, CompareConfiguration compareConfiguration){
		this.shell = shell;
		this.cc = compareConfiguration;

		fDirtyStateListener= e -> {
			String propertyName= e.getProperty();
			if (CompareEditorInput.DIRTY_STATE.equals(propertyName)) {
				boolean changed= false;
				Object newValue= e.getNewValue();
				if (newValue instanceof Boolean)
					changed= ((Boolean)newValue).booleanValue();
				setDirty(e.getSource(), changed);
			}
		};
	}

	@Override
	public boolean isDirty() {
		return fDirty || fDirtyViewers.size() > 0;
	}

	@Override
	public void createPartControl(Composite parent) {
		Composite composite = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		layout.verticalSpacing = 0;
		GridData data = new GridData(GridData.FILL_BOTH);
		data.grabExcessHorizontalSpace = true;
		composite.setLayout(layout);
		composite.setLayoutData(data);

		shell = parent.getShell();

		Splitter vsplitter = new Splitter(composite, SWT.VERTICAL);
		vsplitter.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.GRAB_HORIZONTAL | GridData.VERTICAL_ALIGN_FILL | GridData.GRAB_VERTICAL));
		// we need two panes: the left for the elements, the right one for the structured diff
		Splitter hsplitter = new Splitter(vsplitter, SWT.HORIZONTAL);
		fEditionPane = new CompareViewerPane(hsplitter, SWT.BORDER | SWT.FLAT);
		fStructuredComparePane = new CompareViewerSwitchingPane(hsplitter, SWT.BORDER | SWT.FLAT, true) {
			@Override
			protected Viewer getViewer(Viewer oldViewer, Object input) {
				if (input instanceof ICompareInput)
					return findStructureViewer(this, oldViewer, (ICompareInput)input);
				return null;
			}
		};
		fStructuredComparePane.addSelectionChangedListener(e -> feedInput2(e.getSelection()));
		fEditionPane.setText(TeamUIMessages.ParticipantPageSaveablePart_0);
		fContentPane = new CompareViewerSwitchingPane(vsplitter, SWT.BORDER | SWT.FLAT) {
			@Override
			protected Viewer getViewer(Viewer oldViewer, Object input) {
				if (!(input instanceof ICompareInput))
					return null;
				Viewer newViewer= findContentViewer(this, oldViewer, (ICompareInput)input);
				boolean isNewViewer= newViewer != oldViewer;
				if (isNewViewer && newViewer instanceof IPropertyChangeNotifier) {
					final IPropertyChangeNotifier dsp= (IPropertyChangeNotifier) newViewer;
					dsp.addPropertyChangeListener(fDirtyStateListener);
					Control c= newViewer.getControl();
					c.addDisposeListener(
						e -> dsp.removePropertyChangeListener(fDirtyStateListener)
					);
					hookContentChangeListener((ICompareInput)input);
				}
				return newViewer;
			}
		};
		vsplitter.setWeights(new int[]{30, 70});

		control = composite;

		ToolBarManager toolBarManager = CompareViewerPane.getToolBarManager(fEditionPane);
		Control c = createPage(fEditionPane, toolBarManager);
		fEditionPane.setContent(c);

		if(! showContentPanes) {
			hsplitter.setMaximizedControl(fEditionPane);
		}

		getSelectionProvider().addSelectionChangedListener(event -> {
			ICompareInput input = getCompareInput(event.getSelection());
			if (input != null)
				prepareCompareInput(input);
			setInput(input);
		});
	}

	/**
	 * Return the selection provider for the page. This method is
	 * called after the page is created in order to register a
	 * selection listener on the page.
	 * @return the selection provider for the page
	 */
	protected abstract ISelectionProvider getSelectionProvider();

	/**
	 * Create the page for this part and return the top level control
	 * for the page.
	 * @param parent the parent composite
	 * @param toolBarManager the toolbar manager for the page
	 * @return the top-level control for the page
	 */
	protected abstract Control createPage(Composite parent, ToolBarManager toolBarManager);

	/**
	 * Set the title of the page's page to the given text. The title
	 * will appear in the header of the pane containing the page.
	 * @param title the page's title
	 */
	protected void setPageDescription(String title) {
		fEditionPane.setText(title);
	}

	/**
	 * Set the saveable part's dirty state to the given state.
	 * @param dirty the dirty state
	 */
	protected void setDirty(boolean dirty) {
		boolean confirmSave= true;
		Object o= cc.getProperty(CompareEditor.CONFIRM_SAVE_PROPERTY);
		if (o instanceof Boolean)
			confirmSave= ((Boolean)o).booleanValue();

		if (!confirmSave) {
			fDirty= dirty;
			if (!fDirty)
				fDirtyViewers.clear();
		}
	}

	private void setDirty(Object source, boolean dirty) {
		Assert.isNotNull(source);
		if (dirty)
			fDirtyViewers.add(source);
		else
			fDirtyViewers.remove(source);
	}

	/**
	 * Feeds input from the page into the content and structured viewers.
	 * @param input the input
	 */
	private void setInput(Object input) {
		CompareViewerPane pane = fContentPane;
		if (pane != null && !pane.isDisposed())
			fContentPane.setInput(input);
		if (fStructuredComparePane != null && !fStructuredComparePane.isDisposed())
			fStructuredComparePane.setInput(input);
	}

	/*
	 * Feeds selection from structure viewer to content viewer.
	 */
	private void feedInput2(ISelection sel) {
		ICompareInput input = getCompareInput(sel);
		prepareCompareInput(input);
		if (input != null)
			fContentPane.setInput(input);
	}

	/**
	 * Convenience method that calls {@link #prepareInput(ICompareInput, CompareConfiguration, IProgressMonitor)}
	 * with a progress monitor.
	 * @param input the compare input to be prepared
	 */
	protected void prepareCompareInput(final ICompareInput input) {
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

	private void hookContentChangeListener(ICompareInput node) {
		// TODO: there is no unhook which may lead to a leak
		ITypedElement left = node.getLeft();
		if(left instanceof IContentChangeNotifier) {
			((IContentChangeNotifier)left).addContentChangeListener(this);
		}
		ITypedElement right = node.getRight();
		if(right instanceof IContentChangeNotifier) {
			((IContentChangeNotifier)right).addContentChangeListener(this);
		}
	}

	/**
	 * Return the parent shell of this part.
	 * @return the parent shell of this part
	 */
	protected Shell getShell() {
		return shell;
	}

	/**
	 * This method is internal to the framework and should not be called by clients
	 * outside of the framework.
	 */
	protected void setNavigator(ISynchronizePageConfiguration configuration) {
			configuration.setProperty(SynchronizePageConfiguration.P_NAVIGATOR, new CompareEditorInputNavigator(
				new Object[] {
					configuration.getProperty(SynchronizePageConfiguration.P_ADVISOR),
					fStructuredComparePane,
					fContentPane
				}
			));
	}

	/*
	 * Find a viewer that can provide a structure view for the given compare input.
	 * Return <code>null</code> if a suitable viewer could not be found.
	 * @param parent the parent composite for the viewer
	 * @param oldViewer the viewer that is currently a child of the parent
	 * @param input the compare input to be viewed
	 * @return a viewer capable of displaying a structure view of the input or
	 * <code>null</code> if such a viewer is not available.
	 */
	private Viewer findStructureViewer(Composite parent, Viewer oldViewer, ICompareInput input) {
		return CompareUI.findStructureViewer(oldViewer, input, parent, cc);
	}

	/*
	 * Find a viewer that can provide a content compare view for the given compare input.
	 * Return <code>null</code> if a suitable viewer could not be found.
	 * @param parent the parent composite for the viewer
	 * @param oldViewer the viewer that is currently a child of the parent
	 * @param input the compare input to be viewed
	 * @return a viewer capable of displaying a content compare view of the input or
	 * <code>null</code> if such a viewer is not available.
	 */
	private Viewer findContentViewer(Composite parent, Viewer oldViewer, ICompareInput input) {
		return CompareUI.findContentViewer(oldViewer, input, parent, cc);
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
	protected ICompareInput getCompareInput(ISelection selection) {
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
	 * Set whether the file contents panes should be shown. If they are not,
	 * only the page will be shown.
	 *
	 * @param showContentPanes whether to show contents pane
	 */
	public void setShowContentPanes(boolean showContentPanes) {
		this.showContentPanes = showContentPanes;
	}

	/**
	 * Returns the primary control for this part.
	 *
	 * @return the primary control for this part.
	 */
	public Control getControl() {
		return control;
	}

	/**
	 * Return the compare configuration.
	 * @return the compare configuration
	 */
	private CompareConfiguration getCompareConfiguration() {
		return cc;
	}

	/**
	 * This method flushes the content in any viewers. Subclasses should
	 * override if they need to perform additional processing when a save is
	 * performed.
	 *
	 * @param monitor
	 *            a progress monitor
	 */
	@Override
	public void doSave(IProgressMonitor monitor) {
		flushViewers(monitor);
	}

	private void flushViewers(IProgressMonitor monitor) {
		for (Object element : fDirtyViewers) {
			IFlushable flushable = Adapters.adapt(element, IFlushable.class);
			if (flushable != null)
				flushable.flush(monitor);
		}
	}

}
