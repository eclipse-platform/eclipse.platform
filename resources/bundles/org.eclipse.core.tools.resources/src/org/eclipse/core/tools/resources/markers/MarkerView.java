/**********************************************************************
 * Copyright (c) 2003, 2018 Geoff Longman and others.
 *
 *   This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * Geoff Longman - Initial API and implementation
 * IBM - Tightening integration with existing Platform
 * Geoff Longman - added ability to delete the marker selection
 **********************************************************************/
package org.eclipse.core.tools.resources.markers;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.List;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.action.*;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;
import org.eclipse.ui.actions.SelectionProviderAction;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.eclipse.ui.ide.IDE.SharedImages;
import org.eclipse.ui.part.EditorPart;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.progress.IProgressService;
import org.eclipse.ui.views.properties.*;

/**
 * This is a view that allows one to inspect the state of a resource's markers.
 * The view changes when a resource is selected in the workspace.
 */

public class MarkerView extends ViewPart implements ISelectionListener, IResourceChangeListener, IAdaptable {

	public static final String MEMENTO_TAG1 = "MarkerView";
	public static final String MEMENTO_TAG2 = "IResourcePath";
	public static final String NONE_SELECTED = "NONE_SELECTED";
	protected static final IStructuredSelection emptySelection = new StructuredSelection();

	protected int markerDepth = IResource.DEPTH_ZERO;
	protected MarkerExtensionModel model;
	protected ReadOnlyMarkerPropertySource propertySource;
	protected TreeViewer viewer;
	protected Action doubleClickAction;
	protected MarkerDepthAction depthZero;
	protected MarkerDepthAction depthOne;
	protected MarkerDepthAction depthInfinite;
	protected DeleteMarkersAction deleteMarkers;
	protected IResource currentResource;
	protected String errorMsg;
	protected String warningMsg;
	protected ImageDescriptor oneImageDesc;
	protected ImageDescriptor zeroImageDesc;
	protected ImageDescriptor infiniteImageDesc;
	protected PagePartListener pagePartListener;
	protected IWorkbenchPage currentPage;
	protected SelectionProvider selectionProvider;
	protected MarkerViewPropertySheetPage propertyPage;

	static class SelectionProvider implements ISelectionProvider {
		private List<ISelectionChangedListener> listeners = new ArrayList<>();
		private ISelection selection;

		@Override
		public void addSelectionChangedListener(ISelectionChangedListener listener) {
			listeners.add(listener);
		}

		@Override
		public ISelection getSelection() {
			return selection;
		}

		@Override
		public void removeSelectionChangedListener(ISelectionChangedListener listener) {
			listeners.remove(listener);
		}

		@Override
		public synchronized void setSelection(ISelection selection) {
			this.selection = selection;
			SelectionChangedEvent event = new SelectionChangedEvent(this, selection);
			for (ISelectionChangedListener listener : listeners) {
				listener.selectionChanged(event);
			}
		}
	}

	class MarkerViewPropertySheetPage extends PropertySheetPage {
		MarkerView view;

		public MarkerViewPropertySheetPage(MarkerView view) {
			super();
			this.view = view;
		}

		@Override
		public void selectionChanged(IWorkbenchPart part, ISelection selection) {
			if (part == view)
				super.selectionChanged(part, selection);
		}
	}

	class MarkerDepthAction extends Action {

		private int depth;

		public MarkerDepthAction(int depth, String text, ImageDescriptor image) {
			super(text, image);
			this.depth = depth;
		}

		@Override
		public void run() {
			markerDepth = depth;
			updateActionChecks();
			if (currentResource != null)
				viewer.setInput(currentResource);
		}
	}

	class DeleteMarkersAction extends SelectionProviderAction {
		public DeleteMarkersAction(ISelectionProvider provider, String text) {
			super(provider, text);
		}

		@Override
		public void selectionChanged(IStructuredSelection selection) {
			setEnabled(!selection.isEmpty());
		}

		@Override
		public void run() {
			final IStructuredSelection selection = getStructuredSelection();
			if (selection.isEmpty())
				return;

			IRunnableWithProgress op = new WorkspaceModifyOperation() {
				@Override
				public void execute(IProgressMonitor monitor) {
					int count = selection.size();
					int deleted = 1;
					monitor.beginTask("deleting #" + deleted + " of " + count + " markers.", count);
					for (Iterator<?> iter = selection.iterator(); iter.hasNext();) {
						IMarker marker = (IMarker) iter.next();
						try {
							marker.delete();
							monitor.worked(1);
							deleted++;
							monitor.setTaskName("deleting #" + deleted + " of " + count + " markers.");
						} catch (CoreException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					monitor.done();
				}
			};

			IProgressService progressService = PlatformUI.getWorkbench().getProgressService();
			try {
				progressService.busyCursorWhile(op);
			} catch (InvocationTargetException e) {
				IStatus status = null;
				Throwable nested = e.getCause();
				String message = nested.getMessage();
				if (nested instanceof CoreException)
					status = ((CoreException) nested).getStatus();
				ErrorDialog.openError(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), "Error", message, status);
			} catch (InterruptedException e) {
				// Do nothing
			}
		}
	}

	class PagePartListener implements IPartListener, IPageListener {

		@Override
		public void partActivated(IWorkbenchPart part) {
			if (part instanceof IEditorPart)
				viewer.setInput(part);
		}

		@Override
		public void partBroughtToTop(IWorkbenchPart part) {
			if (part instanceof IEditorPart)
				viewer.setInput(part);
		}

		@Override
		public void partClosed(IWorkbenchPart part) {
			// do nothing
		}

		@Override
		public void partDeactivated(IWorkbenchPart part) {
			// do nothing
		}

		@Override
		public void partOpened(IWorkbenchPart part) {
			// do nothing
		}

		@Override
		public void pageActivated(IWorkbenchPage page) {
			if (currentPage != null)
				currentPage.removePartListener(this);
			currentPage = page;
			page.addPartListener(this);
		}

		@Override
		public void pageClosed(IWorkbenchPage page) {
			// do nothing
		}

		@Override
		public void pageOpened(IWorkbenchPage page) {
			// do nothing
		}
	}

	class ViewContentProvider implements IStructuredContentProvider, ITreeContentProvider {
		IStructuredSelection selection;
		IMarker[] markers;
		String[] message = new String[1];

		@Override
		public void inputChanged(Viewer v, Object oldInput, Object newInput) {
			errorMsg = null;
			warningMsg = null;
			markers = null;

			if (newInput instanceof IMarker) {
				findResourceMarkers(((IMarker) newInput).getResource());
			} else if (newInput instanceof IResource) {
				findResourceMarkers((IResource) newInput);
				return;
			} else if (newInput instanceof EditorPart) {
				currentResource = ((EditorPart) newInput).getEditorInput().getAdapter(IResource.class);
				if (currentResource != null) {
					findResourceMarkers(currentResource);
				} else {
					warningMsg = "Could not obtain an IResource from: " + newInput;
					currentResource = null;
				}
			} else {
				// otherwise its a selection
				currentResource = null;
				selection = (IStructuredSelection) newInput;
				if (selection == null || selection.isEmpty()) {
					warningMsg = "No selection found";
				} else if (selection.size() > 1) {
					warningMsg = "Select one resource to view its markers.";
				} else {
					Object selected = selection.getFirstElement();
					if (selected != null) {
						if (selected instanceof IMarker) {
							currentResource = ((IMarker) selected).getResource();
						} else if (selected instanceof IAdaptable) {
							currentResource = ((IAdaptable) selected).getAdapter(IResource.class);
						}
						if (currentResource != null)
							findResourceMarkers(currentResource);
						else
							warningMsg = "Could not obtain an IResource from: " + selected;
					}
				}
			}
			if (newInput != null)
				setPartName("MarkerView" + (currentResource == null ? "" : " " + currentResource.getFullPath().toString()));
		}

		protected void findResourceMarkers(IResource resource) {
			try {
				markers = resource.findMarkers(null, true, markerDepth);
				if (markers.length <= 0)
					warningMsg = "No markers found for: " + resource.getFullPath();
				return;
			} catch (CoreException e) {
				errorMsg = "Exception" + e.getMessage() + " occured obtaining markers for" + resource.getFullPath();
				return;
			}
		}

		@Override
		public void dispose() {
			// do nothing
		}

		@Override
		public Object[] getElements(Object parent) {
			if (parent.equals(viewer.getInput())) {
				if (warningMsg != null) {
					message[0] = warningMsg;
					return message;
				}
				if (errorMsg != null) {
					message[0] = errorMsg;
					return message;
				}
				return getChildren(parent);
			}
			return getChildren(parent);
		}

		@Override
		public Object getParent(Object child) {
			if (child instanceof String || child instanceof IMarker)
				return selection;
			return null;
		}

		@Override
		public Object[] getChildren(Object parent) {
			if (parent == viewer.getInput())
				return markers;
			return new Object[0];
		}

		@Override
		public boolean hasChildren(Object parent) {
			if (parent == viewer.getInput())
				return true;
			return false;
		}
	}

	class ViewLabelProvider extends LabelProvider {

		@Override
		public String getText(Object obj) {
			try {
				if (obj instanceof IMarker) {
					IMarker marker = (IMarker) obj;
					String message = (String) marker.getAttribute(IMarker.MESSAGE);
					String type = marker.getType();
					return message == null ? type : message + " : " + type;
				}
			} catch (CoreException e) {
				Platform.getLog(MarkerView.class).log(e.getStatus());
			}
			return obj.toString();
		}

		@Override
		public Image getImage(Object obj) {
			String imageKey = ISharedImages.IMG_OBJ_ELEMENT;
			if (obj instanceof String) {
				if (warningMsg != null)
					imageKey = ISharedImages.IMG_OBJS_WARN_TSK;
				else if (errorMsg != null)
					imageKey = ISharedImages.IMG_OBJS_ERROR_TSK;
			} else if (obj instanceof IMarker) {
				try {
					IMarker marker = (IMarker) obj;
					if (marker.isSubtypeOf(IMarker.BOOKMARK)) {
						imageKey = SharedImages.IMG_OBJS_BKMRK_TSK;
					} else if (marker.isSubtypeOf(IMarker.PROBLEM)) {
						imageKey = ISharedImages.IMG_OBJS_ERROR_TSK;
						int severity = marker.getAttribute(IMarker.SEVERITY, IMarker.SEVERITY_WARNING);
						switch (severity) {
							case IMarker.SEVERITY_ERROR :
								imageKey = ISharedImages.IMG_OBJS_ERROR_TSK;
								break;
							case IMarker.SEVERITY_INFO :
								imageKey = ISharedImages.IMG_OBJS_INFO_TSK;
								break;
							case IMarker.SEVERITY_WARNING :
								imageKey = ISharedImages.IMG_OBJS_WARN_TSK;
								break;
							default :
								break;
						}
					} else if (marker.isSubtypeOf(IMarker.TASK))
						imageKey = SharedImages.IMG_OBJS_TASK_TSK;
				} catch (CoreException e) {
					Platform.getLog(MarkerView.class).log(e.getStatus());
				}
			}
			return PlatformUI.getWorkbench().getSharedImages().getImage(imageKey);
		}
	}

	/**
	 * The constructor.
	 */
	public MarkerView() {
		zeroImageDesc = ImageDescriptor.createFromURLSupplier(true, () -> MarkerView.class.getResource("/icons/zero.gif"));
		oneImageDesc = ImageDescriptor.createFromURLSupplier(true, () -> MarkerView.class.getResource("/icons/one.gif"));
		infiniteImageDesc = ImageDescriptor.createFromURLSupplier(true, () -> MarkerView.class.getResource("/icons/infinity.gif"));
		model = new MarkerExtensionModel();
		propertySource = new ReadOnlyMarkerPropertySource(this, model);
	}

	/**
	 * create the viewer and initialize it.
	 */
	@Override
	public void createPartControl(Composite parent) {
		IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();

		ISelectionService sel_service = window.getSelectionService();
		sel_service.addSelectionListener(this);

		ResourcesPlugin.getWorkspace().addResourceChangeListener(this);

		viewer = new TreeViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		viewer.setContentProvider(new ViewContentProvider());
		viewer.setLabelProvider(new ViewLabelProvider());
		viewer.addSelectionChangedListener(event -> {
			IStructuredSelection sel = (IStructuredSelection) event.getSelection();
			IStructuredSelection newSelection = emptySelection;
			if (!sel.isEmpty() && sel.size() == 1) {
				Object first = sel.getFirstElement();
				if (first instanceof IMarker) {
					IMarker marker = (IMarker) first;
					propertySource.setSourceMarker(marker);
					newSelection = new StructuredSelection(propertySource);
				}
			}

			getSite().getSelectionProvider().setSelection(newSelection);
		});

		pagePartListener = new PagePartListener();
		currentPage = window.getActivePage();
		window.addPageListener(pagePartListener);

		if (currentResource != null) {
			viewer.setInput(currentResource);
		} else {
			IWorkbenchPart activePart = currentPage == null ? null : currentPage.getActivePart();
			if (activePart instanceof EditorPart)
				viewer.setInput(activePart);
			else
				viewer.setInput(sel_service.getSelection());
		}
		makeActions();
		hookContextMenu();
		contributeToActionBars();
	}

	@Override
	public void dispose() {
		super.dispose();
		IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		ISelectionService sel_service = window.getSelectionService();
		sel_service.removeSelectionListener(this);
		ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);
	}

	void hookContextMenu() {
		MenuManager menuMgr = new MenuManager("#PopupMenu"); //$NON-NLS-1$
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(MarkerView.this::fillContextMenu);
		Menu menu = menuMgr.createContextMenu(viewer.getControl());
		viewer.getControl().setMenu(menu);
		getSite().registerContextMenu(menuMgr, viewer);
	}

	void contributeToActionBars() {
		IActionBars bars = getViewSite().getActionBars();
		fillLocalPullDown(bars.getMenuManager());
		fillLocalToolBar(bars.getToolBarManager());
	}

	void fillLocalPullDown(IMenuManager manager) {
		manager.add(depthZero);
		manager.add(depthOne);
		manager.add(depthInfinite);
	}

	void fillContextMenu(IMenuManager manager) {
		manager.add(depthZero);
		manager.add(depthOne);
		manager.add(depthInfinite);
		manager.add(new Separator());
		manager.add(deleteMarkers);
		manager.add(new Separator());
		// Other plug-ins can contribute there actions here
		manager.add(new Separator("Additions"));
	}

	void fillLocalToolBar(IToolBarManager manager) {
		manager.add(depthZero);
		manager.add(depthOne);
		manager.add(depthInfinite);
	}

	private void makeActions() {
		depthZero = new MarkerDepthAction(IResource.DEPTH_ZERO, "IResource.DEPTH_ZERO", zeroImageDesc);
		depthOne = new MarkerDepthAction(IResource.DEPTH_ONE, "IResource.DEPTH_ONE", oneImageDesc);
		depthInfinite = new MarkerDepthAction(IResource.DEPTH_INFINITE, "IResource.DEPTH_INFINITE", infiniteImageDesc);
		deleteMarkers = new DeleteMarkersAction(viewer, "Delete");
		updateActionChecks();
	}

	void updateActionChecks() {
		switch (markerDepth) {
			case IResource.DEPTH_ZERO :
				depthZero.setChecked(true);
				depthOne.setChecked(false);
				depthInfinite.setChecked(false);
				break;
			case IResource.DEPTH_ONE :
				depthZero.setChecked(false);
				depthOne.setChecked(true);
				depthInfinite.setChecked(false);
				break;
			case IResource.DEPTH_INFINITE :
				depthZero.setChecked(false);
				depthOne.setChecked(false);
				depthInfinite.setChecked(true);
				break;
			default :
				depthZero.setChecked(false);
				depthOne.setChecked(false);
				depthInfinite.setChecked(false);
		}
	}

	/**
	 * Passing the focus request to the viewer's control.
	 */
	@Override
	public void setFocus() {
		viewer.getControl().setFocus();
	}

	/**
	 * Pushes the windows selection into the viewer
	 *
	 * @see org.eclipse.ui.ISelectionListener#selectionChanged(IWorkbenchPart, ISelection)
	 */
	@Override
	public void selectionChanged(IWorkbenchPart part, ISelection selection) {
		if (part instanceof IEditorPart)
			viewer.setInput(part);
		else if (part != this && !(part instanceof PropertySheet) && selection instanceof IStructuredSelection)
			viewer.setInput(selection);
	}

	/**
	 * @see org.eclipse.core.resources.IResourceChangeListener#resourceChanged(IResourceChangeEvent)
	 */
	@Override
	public void resourceChanged(IResourceChangeEvent event) {
		if (currentResource == null)
			return;
		IResourceDelta delta = event.getDelta().findMember(currentResource.getFullPath());
		if (delta == null)
			return;
		// could have been called from a non-UI thread.
		// handle appropriately
		Display.getDefault().asyncExec(() -> viewer.setInput(currentResource));
	}

	/**
	 * @see org.eclipse.core.runtime.IAdaptable#getAdapter(Class)
	 */
	@Override
	public <T> T getAdapter(Class<T> adapter) {
		if (adapter == IPropertySheetPage.class) {
			propertyPage = new MarkerViewPropertySheetPage(this);
			return adapter.cast(propertyPage);
		}
		return super.getAdapter(adapter);
	}

	/**
	 * @see org.eclipse.ui.IViewPart#init(IViewSite, IMemento)
	 */
	@Override
	public void init(IViewSite site, IMemento memento) throws PartInitException {
		super.init(site, memento);
		if (memento != null)
			restoreState(memento);
		selectionProvider = new SelectionProvider();
		site.setSelectionProvider(selectionProvider);
	}

	private void restoreState(IMemento memento) {
		IMemento child = memento.getChild(MEMENTO_TAG1);
		if (child != null) {
			child = child.getChild(MEMENTO_TAG2);
			if (child != null) {
				try {
					String resourcePath = child.getTextData();
					if (resourcePath != null && !NONE_SELECTED.equals(resourcePath)) {
						Path path = new Path(resourcePath);
						IFile file = ResourcesPlugin.getWorkspace().getRoot().getFile(path);
						if (file != null && file.exists()) {
							currentResource = file;
						}
					}
				} catch (Exception e) {
				}
			}
		}
	}

	/**
	 * @see org.eclipse.ui.IViewPart#saveState(IMemento)
	 */
	@Override
	public void saveState(IMemento memento) {
		super.saveState(memento);
		IMemento child = memento.createChild(MEMENTO_TAG1);
		child = child.createChild(MEMENTO_TAG2);
		if (currentResource == null)
			child.putTextData(NONE_SELECTED);
		else
			child.putTextData(currentResource.getFullPath().toString());
	}
}
