/*****************************************************************
 * Copyright (c) 2009, 2021 Texas Instruments and others
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Patrick Chuong (Texas Instruments) - Initial API and implementation (Bug 238956)
 *     IBM Corporation - ongoing enhancements and bug fixing
 *****************************************************************/
package org.eclipse.debug.internal.ui.views.breakpoints;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.core.commands.operations.IUndoContext;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IBreakpointManagerListener;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.IDebugHelpContextIds;
import org.eclipse.debug.internal.ui.VariablesViewModelPresentation;
import org.eclipse.debug.internal.ui.actions.breakpointGroups.PasteBreakpointsAction;
import org.eclipse.debug.internal.ui.actions.breakpointGroups.RemoveFromWorkingSetAction;
import org.eclipse.debug.internal.ui.actions.breakpoints.OpenBreakpointMarkerAction;
import org.eclipse.debug.internal.ui.actions.breakpoints.ShowTargetBreakpointsAction;
import org.eclipse.debug.internal.ui.actions.breakpoints.SkipAllBreakpointsAction;
import org.eclipse.debug.internal.ui.breakpoints.provisional.IBreakpointContainer;
import org.eclipse.debug.internal.ui.breakpoints.provisional.IBreakpointOrganizer;
import org.eclipse.debug.internal.ui.breakpoints.provisional.IBreakpointUIConstants;
import org.eclipse.debug.internal.ui.elements.adapters.DefaultBreakpointsViewInput;
import org.eclipse.debug.internal.ui.preferences.IDebugPreferenceConstants;
import org.eclipse.debug.internal.ui.viewers.model.VirtualFindAction;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IModelDelta;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IPresentationContext;
import org.eclipse.debug.internal.ui.viewers.model.provisional.ITreeModelViewer;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IViewerInputUpdate;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IViewerUpdate;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IViewerUpdateListener;
import org.eclipse.debug.internal.ui.viewers.model.provisional.ModelDelta;
import org.eclipse.debug.internal.ui.viewers.model.provisional.TreeModelViewer;
import org.eclipse.debug.internal.ui.viewers.model.provisional.VirtualTreeModelViewer;
import org.eclipse.debug.internal.ui.views.DebugUIViewsMessages;
import org.eclipse.debug.internal.ui.views.variables.VariablesView;
import org.eclipse.debug.internal.ui.views.variables.details.AvailableDetailPanesAction;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.IBreakpointOrganizerDelegateExtension;
import org.eclipse.debug.ui.IDebugModelPresentation;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.actions.SelectionListenerAction;
import org.eclipse.ui.operations.RedoActionHandler;
import org.eclipse.ui.operations.UndoActionHandler;

/**
 * This class implements the breakpoints view.
 */
public class BreakpointsView extends VariablesView implements IBreakpointManagerListener {
	private static final String ACTION_GOTO_MARKER				= "GotoMarker";				//$NON-NLS-1$
	private static final String ACTION_SKIP_BREAKPOINTS			= "SkipBreakpoints";		//$NON-NLS-1$
	private static final String ACTION_SHOW_MODEL_BREAKPOINT	= "ShowBreakpointsForModel";//$NON-NLS-1$
	private static final String ACTION_REMOVE_FROM_GROUP 		= "RemoveFromGroup"; 		//$NON-NLS-1$


	private static final String KEY_VALUE						= "value";					//$NON-NLS-1$

	private Clipboard fClipboard;
	private IBreakpointOrganizer[] fOrganizers;

	/**
	 * Flag used to determine whether the viewer input is being set for the
	 * fist time.  If this is the case the view contents are expanded.
	 * (bug 297762)
	 */
	private boolean fFirstInputSet = false;

	private UndoActionHandler fUndoAction;
	private RedoActionHandler fRedoAction;


	@Override
	public void dispose() {
		if (fClipboard != null) {
			fClipboard.dispose();
		}
		DebugPlugin.getDefault().getBreakpointManager().removeBreakpointManagerListener(this);

		fUndoAction.dispose();
		fRedoAction.dispose();

		super.dispose();
	}

	@Override
	protected String getDetailPanePreferenceKey() {
		return IDebugPreferenceConstants.BREAKPOINTS_DETAIL_PANE_ORIENTATION;
	}

	@Override
	protected String getHelpContextId() {
		return IDebugHelpContextIds.BREAKPOINT_VIEW;
	}

	@Override
	protected int getViewerStyle() {
		return SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL | SWT.VIRTUAL | SWT.FULL_SELECTION | SWT.CHECK;
	}

	@Override
	public Viewer createViewer(Composite parent) {
		TreeModelViewer viewer = (TreeModelViewer) super.createViewer(parent);

		initBreakpointOrganizers(getMemento());

		IPresentationContext presentationContext = viewer.getPresentationContext();
		presentationContext.setProperty(IBreakpointUIConstants.PROP_BREAKPOINTS_ORGANIZERS, fOrganizers);
		presentationContext.setProperty(IBreakpointUIConstants.PROP_BREAKPOINTS_ELEMENT_COMPARATOR, new ElementComparator(presentationContext));

		return viewer;
	}

	@Override
	protected IDebugModelPresentation getModelPresentation() {
		if (fModelPresentation == null) {
			fModelPresentation = new VariablesViewModelPresentation() {
				/**
				 * Undo double slashes.
				 */
				@Override
				public String getText(Object element) {
					IDebugModelPresentation lp= getConfiguredPresentation(element);
					if (lp != null) {
						return lp.getText(element);
					}
					return getDefaultText(element);
				}
			};
		}
		return fModelPresentation;
	}

	/**
	 * Returns the tree model viewer.
	 * @return the backin gviewer
	 */
	public TreeModelViewer getTreeModelViewer() {
		return (TreeModelViewer) getViewer();
	}

	@Override
	protected void configureToolBar(IToolBarManager tbm) {
		tbm.add(new Separator(IDebugUIConstants.BREAKPOINT_GROUP));
		tbm.add(getAction(ACTION_SHOW_MODEL_BREAKPOINT));
		tbm.add(getAction(ACTION_GOTO_MARKER));
		tbm.add(getAction(ACTION_SKIP_BREAKPOINTS));
		tbm.add(new Separator(IDebugUIConstants.RENDER_GROUP));
	}

	@Override
	protected void fillContextMenu(IMenuManager menu) {
		updateObjects();
		menu.add(new Separator(IDebugUIConstants.EMPTY_NAVIGATION_GROUP));
		menu.add(new Separator(IDebugUIConstants.NAVIGATION_GROUP));
		menu.add(getAction(ACTION_GOTO_MARKER));
		menu.add(new Separator(IDebugUIConstants.EMPTY_BREAKPOINT_GROUP));
		menu.add(new Separator(IDebugUIConstants.BREAKPOINT_GROUP));
		IAction action = getAction(PASTE_ACTION);
		if (action != null) {
			menu.add(action);
		}
		action = getAction(ACTION_REMOVE_FROM_GROUP);
		if (action != null && action.isEnabled()) {
			menu.add(action);
		}
		menu.add(new Separator(IDebugUIConstants.EMPTY_RENDER_GROUP));
		action = new AvailableDetailPanesAction(this);
		if (isDetailPaneVisible() && action.isEnabled()) {
			menu.add(action);
		}
		menu.add(new Separator(IDebugUIConstants.BREAKPOINT_GROUP_GROUP));

		menu.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
	}

	@Override
	protected void createActions() {
		IAction action = new OpenBreakpointMarkerAction(getViewer());
		setAction(ACTION_GOTO_MARKER, action);
		setAction(DOUBLE_CLICK_ACTION, action);
		setAction(ACTION_SHOW_MODEL_BREAKPOINT, new ShowTargetBreakpointsAction(this));
		SkipAllBreakpointsAction skipAll = new SkipAllBreakpointsAction(this);
		setAction(ACTION_SKIP_BREAKPOINTS, skipAll);
		skipAll.setActionDefinitionId(SkipAllBreakpointsAction.ACTION_DEFINITION_ID);
		DebugPlugin.getDefault().getBreakpointManager().addBreakpointManagerListener(this);

		fClipboard = new Clipboard(getSite().getShell().getDisplay());

		PasteBreakpointsAction paste = new PasteBreakpointsAction(this);
		setAction(PASTE_ACTION, paste);
		paste.setActionDefinitionId(ActionFactory.PASTE.getCommandId());
		//actionBars.setGlobalActionHandler(ActionFactory.PASTE.getId(), paste);
		setGlobalAction(PASTE_ACTION, paste);
		getViewer().addSelectionChangedListener(paste);
		paste.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_TOOL_PASTE));

		SelectionListenerAction remove = new RemoveFromWorkingSetAction(this);
		setAction(ACTION_REMOVE_FROM_GROUP, remove);
		getViewer().addSelectionChangedListener(remove);

		IUndoContext undoContext= DebugUITools.getBreakpointsUndoContext();
		fUndoAction= new UndoActionHandler(getSite(), undoContext);
		fUndoAction.setActionDefinitionId(IWorkbenchCommandConstants.EDIT_UNDO);
		fRedoAction= new RedoActionHandler(getSite(), undoContext);
		fRedoAction.setActionDefinitionId(IWorkbenchCommandConstants.EDIT_REDO);
		//actionBars.setGlobalActionHandler(ActionFactory.UNDO.getId(), fUndoAction);
		//actionBars.setGlobalActionHandler(ActionFactory.REDO.getId(), fRedoAction);
		setGlobalAction(ActionFactory.UNDO.getId(), fUndoAction);
		setGlobalAction(ActionFactory.REDO.getId(), fRedoAction);
		setGlobalAction(FIND_ACTION, new VirtualFindAction(getVariablesViewer()));
	}

	@Override
	protected String getToggleActionLabel() {
		return DebugUIViewsMessages.BreakpointsView_12;
	}

	@Override
	protected String getPresentationContextId() {
		return IDebugUIConstants.ID_BREAKPOINT_VIEW;
	}

	@Override
	protected void contextActivated(ISelection selection) {
		if (!isAvailable() || !isVisible()) {
			return;
		}

		IPresentationContext presentationContext = getTreeModelViewer().getPresentationContext();

		if (selection == null || selection.isEmpty()) {
			Object input = new DefaultBreakpointsViewInput(presentationContext);
			super.contextActivated(new StructuredSelection(input));
		} else {
			super.contextActivated(selection);
		}
		if (isAvailable() && isVisible()) {
			updateAction("ContentAssist"); //$NON-NLS-1$
		}
	}

	@Override
	protected void setViewerInput(Object context) {
		Object current = getViewer().getInput();
		if (current == null && context == null) {
			return;
		}

		if (current != null && current.equals(context)) {
			return;
		}

		showViewer();
		getViewer().setInput(context);

		// Expand all elements when the view is first shown. (bug 297762)
		if (!fFirstInputSet) {
			fFirstInputSet = true;
			expandAllElementsInViewer();
		}
	}

	@Override
	protected void viewerInputUpdateComplete(IViewerInputUpdate update) {
		// handles non-standard debug model
		IStatus status = update.getStatus();
		if ( (status == null || status.isOK()) && update.getElement() != null) {
			setViewerInput(update.getInputElement());
		} else {
			setViewerInput(new DefaultBreakpointsViewInput(getTreeModelViewer().getPresentationContext()));
		}
	}


	/**
	 * Returns whether this view is currently tracking the selection from the debug view.
	 *
	 * @return whether this view is currently tracking the debug view's selection
	 */
	public boolean isTrackingSelection() {
		final TreeModelViewer viewer = getTreeModelViewer();
		if (viewer != null) {
			return Boolean.TRUE.equals(
				viewer.getPresentationContext().getProperty(IBreakpointUIConstants.PROP_BREAKPOINTS_TRACK_SELECTION) );
		}
		return false;
	}

	/**
	 * Sets whether this view should track the selection from the debug view.
	 *
	 * @param trackSelection whether or not this view should track the debug view's selection.
	 */
	public void setTrackSelection(boolean trackSelection) {
		// set the track selection property for non-standard model to track the debug context
		final TreeModelViewer viewer = getTreeModelViewer();
		if (viewer != null) {
			viewer.getPresentationContext().setProperty(
				IBreakpointUIConstants.PROP_BREAKPOINTS_TRACK_SELECTION,
				trackSelection ? Boolean.TRUE : Boolean.FALSE);
		}
	}

	/**
	 * Initializes the persisted breakpoints organizers.
	 * @param memento the memento to read
	 */
	private void initBreakpointOrganizers(IMemento memento) {
		if (memento != null) {
			IMemento node = memento.getChild(IDebugUIConstants.EXTENSION_POINT_BREAKPOINT_ORGANIZERS);
			if (node == null) {
				fOrganizers = null;
			} else {
				String value = node.getString(KEY_VALUE);
				if (value != null) {
					BreakpointOrganizerManager manager = BreakpointOrganizerManager.getDefault();
					List<IBreakpointOrganizer> organizers = new ArrayList<>();
					for (String id : value.split(",")) { //$NON-NLS-1$
						IBreakpointOrganizer organizer = manager.getOrganizer(id);
						if (organizer != null) {
							organizers.add(organizer);
						}
					}
					fOrganizers = organizers.toArray(new IBreakpointOrganizer[organizers.size()]);

					for (IBreakpointOrganizer organizer : fOrganizers) {
						organizer.addPropertyChangeListener(this);
					}
				}
			}
		}
	}

	/**
	 * Initializes drag and drop for the breakpoints viewer
	 * @param viewer the viewer to add drag and drop support to
	 */
	@Override
	protected void initDragAndDrop(TreeModelViewer viewer) {
		int ops = DND.DROP_MOVE | DND.DROP_COPY;
		// drop
		viewer.addDropSupport(ops, new Transfer[] {LocalSelectionTransfer.getTransfer()}, new BreakpointsDropAdapter(viewer, this));
		// Drag
		viewer.addDragSupport(ops, new Transfer[] {LocalSelectionTransfer.getTransfer()}, new BreakpointsDragAdapter(viewer, this));
	}

	@Override
	public void saveViewerState(IMemento memento) {
		StringBuilder buffer = new StringBuilder();
		if (fOrganizers != null) {
			for (int i = 0; i < fOrganizers.length; i++) {
				IBreakpointOrganizer organizer = fOrganizers[i];
				buffer.append(organizer.getIdentifier());
				if (i < (fOrganizers.length - 1)) {
					buffer.append(',');
				}
			}
			IMemento node = memento.createChild(IDebugUIConstants.EXTENSION_POINT_BREAKPOINT_ORGANIZERS);
			node.putString(KEY_VALUE, buffer.toString());
		}
		super.saveViewerState(memento);
	}

	/**
	 * Preserves the selection.
	 *
	 * @param selection the selection
	 */
	public void preserveSelection(IStructuredSelection selection) {
		if (selection instanceof ITreeSelection && !selection.isEmpty()) {
			TreePath path = ((ITreeSelection) selection).getPaths()[0];
			TreeItem item = (TreeItem) ((TreeModelViewer) getViewer()).findItem(path);
			Object toselect = null;
			TreeItem[] siblings = null;
			if (item != null) {
				TreeItem parent = item.getParentItem();
				if (parent != null) {
					siblings = parent.getItems();
				} else {
					siblings = item.getParent().getItems();
				}
				if (siblings.length > 1) {
					for (int i = 0; i < siblings.length; i++) {
						if (item.equals(siblings[i])) {
							if (i + 1 >= siblings.length) {
								toselect = siblings[i - 1].getData();
								break;
							} else {
								toselect = siblings[i + 1].getData();
								break;
							}

						}
					}
				}
			}
			if (toselect != null) {
				getViewer().setSelection(new StructuredSelection(toselect),true);
			}
		}
	}

	/**
	 * Sets the breakpoint organizers for this view.
	 *
	 * @param organizers the organizers, can be <code>null</code>.
	 */
	public void setBreakpointOrganizers(IBreakpointOrganizer[] organizers) {
		fOrganizers = organizers;

		TreeModelViewer viewer = getTreeModelViewer();
		if (viewer != null) {
			// update the presentation context organizer
			viewer.getPresentationContext().setProperty(IBreakpointUIConstants.PROP_BREAKPOINTS_ORGANIZERS, fOrganizers);
		}
	}

	/**
	 * Sets the breakpoint filter for this view.
	 * @param filter the selection to act as a filter
	 */
	public void setFilterSelection(boolean filter) {
		TreeModelViewer viewer = getTreeModelViewer();
		if (viewer != null) {
			// update the presentation context filter
			viewer.getPresentationContext().setProperty(
				IBreakpointUIConstants.PROP_BREAKPOINTS_FILTER_SELECTION, filter ? Boolean.TRUE : Boolean.FALSE);
		}
	}

	@Override
	public void breakpointManagerEnablementChanged(boolean enabled) {
		DebugUIPlugin.getStandardDisplay().asyncExec(() -> {
			IAction action = getAction(ACTION_SKIP_BREAKPOINTS);
			if (action != null) {
				((SkipAllBreakpointsAction) action).updateActionCheckedState();
			}
		});
	}

	/**
	 * Expands all elements in the viewer.
	 */
	public void expandAllElementsInViewer() {
		Display display = getSite().getShell().getDisplay();

		final VirtualTreeModelViewer virtualViewer = new VirtualTreeModelViewer(
			display, 0, ((ITreeModelViewer)getViewer()).getPresentationContext());

		virtualViewer.setAutoExpandLevel(-1);
		virtualViewer.addViewerUpdateListener(new IViewerUpdateListener() {
			@Override
			public void viewerUpdatesComplete() {
				ModelDelta stateDelta = new ModelDelta(virtualViewer.getInput(), IModelDelta.NO_CHANGE);
				virtualViewer.saveElementState(TreePath.EMPTY, stateDelta, IModelDelta.EXPAND);
				ITreeModelViewer treeModelViewer = ((ITreeModelViewer) getViewer());
				if (treeModelViewer != null) {
					((ITreeModelViewer) getViewer()).updateViewer(stateDelta);
				}
				virtualViewer.dispose();
			}
			@Override
			public void viewerUpdatesBegin() {}
			@Override
			public void updateStarted(IViewerUpdate update) {}
			@Override
			public void updateComplete(IViewerUpdate update) {}
		});
		virtualViewer.setInput(getViewer().getInput());
	}


	/**
	 * Returns the breakpoint organizers for this view.
	 *
	 * @return the breakpoint organizers.
	 */
	public IBreakpointOrganizer[] getBreakpointOrganizers() {
		return fOrganizers;
	}

	/**
	 * Returns whether the given selection can be pasted into the given target.
	 * <p>
	 * Scheme:
	 * <ul>
	 * <li>Breakpoints can only be pasted into allowable containers (i..e. like workings sets)</li>
	 * <li>Breakpoints can only be pasted into containers that they do not already reside in</li>
	 * <li>Breakpoints can only be pasted into containers, not other breakpoints</li>
	 * </ul>
	 * </p>
	 *
	 * @param target target of the paste
	 * @param selection the selection to paste
	 * @return whether the given selection can be pasted into the given target
	 *
	 * TODO Remove in favor of using <code>TreeItem</code>s and <code>TreePath</code>s to determine paste targets
	 */
	public boolean canPaste(Object target, ISelection selection) {
		if(!(target instanceof IBreakpointContainer) || !(selection instanceof IStructuredSelection)) {
			return false;
		}
		if(selection == null || selection.isEmpty()) {
			return false;
		}
		IStructuredSelection ss = (IStructuredSelection) selection;
		IBreakpointContainer container = (IBreakpointContainer) target;
		for (Iterator<?> iter = ss.iterator(); iter.hasNext();) {
			IBreakpoint breakpoint = (IBreakpoint)DebugPlugin.getAdapter(iter.next(), IBreakpoint.class);
			if (breakpoint == null || container.contains(breakpoint) || !container.getOrganizer().canAdd(breakpoint, container.getCategory())) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Pastes the selection into the given target
	 *
	 * @param target target of the paste, either a IBreakpointContainer,
	 * or a Breakpoint within a IBreakpointContainer
	 * @param selection breakpoints
	 * @return whether successful
	 *
	 * TODO remove in favor of using <code>TreeItem</code> as paste target
	 */
	public boolean performPaste(Object target, ISelection selection) {
		if (target instanceof IBreakpointContainer && selection instanceof IStructuredSelection) {
			IBreakpointContainer container = (IBreakpointContainer) target;
			for (Object object : (IStructuredSelection) selection) {
				IBreakpoint breakpoint = (IBreakpoint)DebugPlugin.getAdapter(object, IBreakpoint.class);
				if (breakpoint != null) {
					container.getOrganizer().addBreakpoint(breakpoint, container.getCategory());
				}
			}
			return true;
		}
		return false;
	}

	/**
	 * Returns the container from within the specified path that is the container the breakpoint can be removed from
	 * @param path the path to get the container from
	 * @return the first found container that includes the breakpoint that allows removal, or <code>null</code> if none found
	 * @since 3.3
	 */
	public IBreakpointContainer getRemovableContainer(TreePath path) {
		if (path != null) {
			IBreakpoint breakpoint = (IBreakpoint)DebugPlugin.getAdapter(path.getLastSegment(), IBreakpoint.class);
			if (breakpoint != null) {
				IBreakpointContainer container = null;
				for(int i = path.getSegmentCount()-2; i > -1; i--) {
					Object segment = path.getSegment(i);
					if (segment instanceof IBreakpointContainer) {
						container = (IBreakpointContainer) segment;
						if(container.contains(breakpoint) &&
							container.getOrganizer() != null &&
							container.getOrganizer().canRemove(breakpoint, container.getCategory())) {
							return container;
						}
					}
				}
			}
		}
		return null;
	}

	/**
	 * Returns the addable breakpoint container of the specified tree path
	 * @param path the path to get the container for
	 * @return the first found addable container for the specified tree path or <code>null</code> if none found
	 * @since 3.3
	 */
	protected IBreakpointContainer getAddableContainer(TreePath path) {
		if (path != null) {
			Object element = path.getLastSegment();
			if (element instanceof IBreakpointContainer) {
				return (IBreakpointContainer)element;
			}
			IBreakpoint breakpoint = (IBreakpoint)DebugPlugin.getAdapter(element, IBreakpoint.class);
			if (breakpoint != null) {
				IBreakpointContainer container = null;
				for (int i = path.getSegmentCount()-2; i > -1; i--) {
					Object segment = path.getSegment(i);
					if (segment instanceof IBreakpointContainer) {
						container = (IBreakpointContainer) segment;
						if (container.contains(breakpoint) && container.getOrganizer().canAdd(breakpoint, container.getCategory())) {
							return container;
						}
					}
				}
			}
		}
		return null;
	}
	/**
	 * This method is used to determine if there is an addable parent container available for the specified drop target.
	 * <p>
	 * A drop target can be either a <code>IBreakpointContainer</code> or an <code>IBreakpoint</code>. This method always checks the entire hierarchy
	 * of the tree path for the specified target in the event one of the parent element does not support dropping.
	 * </p>
	 * @param path the path
	 * @param breakpoint the breakpoint
	 * @return <code>true</code> if there is a parent container available for the drop target <code>false</code> otherwise
	 */
	private boolean checkAddableParentContainers(TreePath path, IBreakpoint breakpoint) {
		if (path != null) {
			Object element = null;
			for (int i = path.getSegmentCount()-1; i > -1; i--) {
				element = path.getSegment(i);
				if (element instanceof IBreakpointContainer) {
					IBreakpointContainer container = (IBreakpointContainer) element;
					if (container.contains(breakpoint) || !container.getOrganizer().canAdd(breakpoint, container.getCategory())) {
						return false;
					}
				}
			}
		}
		return true;
	}

	/**
	 * Returns if the selected item in the tree can be dragged
	 * <p>
	 * Scheme:
	 * <ul>
	 * <li>breakpoint containers cannot be dragged</li>
	 * <li>breakpoints can be dragged iff the container they reside in supports the removal of breakpoints</li>
	 * </ul>
	 * </p>
	 * @param items the tree paths to check if they can be dragged
	 * @return true if the selected element can be dragged, false otherwise
	 * @since 3.3
	 */
	boolean canDrag(TreePath[] items) {
		if(items == null) {
			return false;
		}
		if (items.length == 0) {
			return false;
		}
		for (TreePath item : items) {
			if (getRemovableContainer(item) == null) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Performs the actual removal of breakpoints from their respective (removable) containers on a successful drag operation
	 * @param paths the tree paths to drag
	 * @since 3.3
	 */
	void performDrag(TreePath[] paths) {
		if (paths == null) {
			return;
		}
		Map<IBreakpointContainer, List<IBreakpoint>> containersToBreakpoints = new HashMap<>();
		for (TreePath path : paths) {
			IBreakpoint breakpoint = (IBreakpoint)DebugPlugin.getAdapter(path.getLastSegment(), IBreakpoint.class);
			if (breakpoint != null) {
				IBreakpointContainer container = getRemovableContainer(path);
				if(container != null) {
					List<IBreakpoint> list = containersToBreakpoints.get(container);
					if (list == null) {
						list = new ArrayList<>();
						containersToBreakpoints.put(container, list);
					}
					list.add(breakpoint);
				}
			}
		}
		for (Entry<IBreakpointContainer, List<IBreakpoint>> entry : containersToBreakpoints.entrySet()) {
			IBreakpointContainer container = entry.getKey();
			List<IBreakpoint> list = entry.getValue();
			IBreakpointOrganizer organizer = container.getOrganizer();
			IBreakpoint[] breakpoints = list.toArray(new IBreakpoint[list.size()]);
			if (organizer instanceof IBreakpointOrganizerDelegateExtension) {
				IBreakpointOrganizerDelegateExtension extension = (IBreakpointOrganizerDelegateExtension) organizer;
				extension.removeBreakpoints(breakpoints, container.getCategory());
			} else {
				for (IBreakpoint breakpoint : breakpoints) {
					organizer.removeBreakpoint(breakpoint, container.getCategory());
				}
			}
		}
	}

	/**
	 * Performs the actual addition of the selected breakpoints to the specified target
	 * @param target the target to add the selection of breakpoints to
	 * @param selection the selection of breakpoints
	 * @return true if the drop occurred, false otherwise
	 * @since 3.3
	 */
	protected boolean performDrop(TreePath target, ITreeSelection selection) {
		if(target == null || selection == null) {
			return false;
		}
		IBreakpointContainer container = getAddableContainer(target);
		if (container == null) {
			return false;
		}

		IBreakpointOrganizer organizer = container.getOrganizer();
		List<IBreakpoint> breakpoints = new ArrayList<>(selection.size());
		for (Iterator<?> iter = selection.iterator(); iter.hasNext();) {
			IBreakpoint breakpoint = (IBreakpoint) DebugPlugin.getAdapter(iter.next(), IBreakpoint.class);
			if (breakpoint != null) {
				breakpoints.add(breakpoint);
			}
		}
		if (organizer instanceof IBreakpointOrganizerDelegateExtension) {
			IBreakpointOrganizerDelegateExtension extension = (IBreakpointOrganizerDelegateExtension) organizer;
			extension.addBreakpoints(
				breakpoints.toArray(new IBreakpoint[breakpoints.size()]),
				container.getCategory());
		} else {
			for (IBreakpoint breakpoint : breakpoints) {
				organizer.addBreakpoint(breakpoint, container.getCategory());
			}
		}
		// TODO expandToLevel(target.getData(), ALL_LEVELS);
		return true;
	}

	/**
	 * Determines if the specified element can be dropped into the specified target
	 * <p>
	 * Scheme:
	 * <ul>
	 * <li>Breakpoints can be dropped into working sets</li>
	 * <li>Breakpoints can be dropped into breakpoints, provided there is a drop-able parent of the target breakpoint</li>
	 * </ul>
	 * </p>
	 * @param target the target for the drop
	 * @param selection the selection to see if we can drop
	 * @return true if the specified element can be dropped into the specified target, false otherwise
	 * @since 3.3
	 */
	boolean canDrop(TreePath target, ITreeSelection selection) {
		if (selection == null  || target == null) {
			return false;
		}
		for (Iterator<?> iter = selection.iterator(); iter.hasNext();) {
			IBreakpoint breakpoint = (IBreakpoint)DebugPlugin.getAdapter(iter.next(), IBreakpoint.class);
			if (breakpoint == null || !checkAddableParentContainers(target, breakpoint)){
				return false;
			}
		}
		return true;
	}
}
