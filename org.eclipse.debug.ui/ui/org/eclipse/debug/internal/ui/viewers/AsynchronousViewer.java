/*******************************************************************************
 * Copyright (c) 2005, 2013 IBM Corporation and others.
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
package org.eclipse.debug.internal.ui.viewers;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IModelChangedListener;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IModelProxy;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IModelSelectionPolicy;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IModelSelectionPolicyFactory;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IPresentationContext;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IStatusMonitor;
import org.eclipse.debug.internal.ui.viewers.model.provisional.PresentationContext;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.progress.WorkbenchJob;

/**
 * A viewer that retrieves labels and content asynchronously via adapters and supports
 * duplicate elements in the viewer. Retrieving content and labels asynchronously allows
 * for arbitrary latency without blocking the UI thread.
 * <p>
 * This viewer uses adapters to retrieve labels and content rather than
 * a label provider and content provider. As such, the label provider for this viewer
 * is <code>null</code> by default. The content provider returned by this viewer is
 * non-<code>null</code> to conform to the viewer specification, but performs no
 * useful function.
 * </p>
 * <p>
 * The selection in this viewer is also set asynchronously. When the selection is set,
 * the viewer attempts to perform the selection. If the elements in the specified selection
 * are not yet in the viewer, the portion of the selection that could not be honored
 * becomes a pending selection. As more elements are added to viewer, the pending selection
 * is attempted to be set.
 * </p>
 * @since 3.2
 */
public abstract class AsynchronousViewer extends StructuredViewer implements Listener {

	/**
	 * Model of elements for this viewer
	 */
	private AsynchronousModel fModel;

	/**
	 * Cache of images used for elements in this tree viewer. Label updates
	 * use the method <code>getImage(...)</code> to cache images for
	 * image descriptors. The images are disposed when this viewer is disposed.
	 */
	private Map<ImageDescriptor, Image> fImageCache = new HashMap<>();

	/**
	 * Cache of the fonts used for elements in this tree viewer. Label updates
	 * use the method <code>getFont(...)</code> to cache fonts for
	 * FontData objects. The fonts are disposed with the viewer.
	 */
	private Map<FontData, Font> fFontCache = new HashMap<>();

	/**
	 * Cache of the colors used for elements in this tree viewer. Label updates
	 * use the method <code>getColor(...)</code> to cache colors for
	 * RGB values. The colors are disposed with the viewer.
	 */
	private Map<RGB, Color> fColorCache = new HashMap<>();

	/**
	 * The context in which this viewer is being used - i.e. what part it is contained
	 * in any any preference settings associated with it.
	 */
	private IPresentationContext fContext;

	private ISelection fPendingSelection;

	private ISelection fCurrentSelection;

	/**
	 * Array used to store indices of the path to an item in the viewer being mapped
	 * by a 'set data' callback. Indices are bottom up. For example when 'set data' for
	 * the 3rd child of the 4th child of the 2nd root element were being asked for,
	 * the first 3 indices would look like: [3, 4, 2, ....]. We re-use an array to avoid
	 * creating a new one all the time. The array grows as needed to accommodate deep
	 * elements.
	 */
	private int[] fSetDataIndicies = new int[5];

	/**
	 * The update policy for this viewer.
	 */
	private AbstractUpdatePolicy fUpdatePolicy;

	protected static final String OLD_LABEL = "old_label"; //$NON-NLS-1$
	protected static final String OLD_IMAGE = "old_image"; //$NON-NLS-1$

	/**
	 * Creates a new viewer
	 */
	protected AsynchronousViewer() {
		setContentProvider(new NullContentProvider());
		setUseHashlookup(true);
	}

	/**
	 * Hash lookup is required, don't let subclasses change behavior.
	 * @param enable if hash lookup should be used in the viewer
	 */
	@Override
	public final void setUseHashlookup(boolean enable) {
		Assert.isTrue(enable);
		super.setUseHashlookup(enable);
	}

	@Override
	protected void hookControl(Control control) {
		super.hookControl(control);
		control.addListener(SWT.SetData, this);
	}

	/**
	 * Clients must call this methods when this viewer is no longer needed
	 * so it can perform cleanup.
	 */
	public synchronized void dispose() {
		for (Image image : fImageCache.values()) {
			image.dispose();
		}
		fImageCache.clear();
		for (Font font : fFontCache.values()) {
			font.dispose();
		}
		fFontCache.clear();
		for (Color color : fColorCache.values()) {
			color.dispose();
		}
		fColorCache.clear();

		if (fModel != null) {
			fModel.dispose();
		}
		if (fUpdatePolicy != null) {
			fUpdatePolicy.dispose();
		}
		if (fContext != null) {
			((PresentationContext)fContext).dispose();
		}
	}

	/**
	 * Updates all occurrences of the given element in this viewer.
	 *
	 * @param element element to update
	 */
	public void update(Object element) {
		ModelNode[] nodes = getModel().getNodes(element);
		if (nodes != null) {
			for (ModelNode node : nodes) {
				updateLabel(node);
			}
		}
	}

	/**
	 * Updates the label for a specific element (node) in the model.
	 *
	 * @param node node to update
	 */
	protected void updateLabel(ModelNode node) {
		// the input is not displayed
		if (!node.getElement().equals(getInput())) {
			getModel().updateLabel(node);
		}
	}

	/**
	 * Returns the presentation context to be used in update requests.
	 * Clients may override this method if required to provide special
	 * implementations of contexts.
	 *
	 * @return presentation context
	 */
	public IPresentationContext getPresentationContext() {
		return fContext;
	}

	@Override
	protected synchronized void unmapAllElements() {
		super.unmapAllElements();
		AsynchronousModel model = getModel();
		if (model != null) {
			model.dispose();
		}
	}

	@Override
	protected synchronized void inputChanged(Object input, Object oldInput) {
		fPendingSelection = null;
		if (fCurrentSelection != null) {
			updateSelection(new StructuredSelection());
			fCurrentSelection = null;
		}
		if (fUpdatePolicy == null) {
			fUpdatePolicy = createUpdatePolicy();
			fUpdatePolicy.init(this);
		}
		if (fModel != null) {
			fModel.dispose();
		}
		fModel = createModel();
		fModel.init(input);
		if (input != null) {
			mapElement(fModel.getRootNode(), getControl());
			getControl().setData(fModel.getRootNode().getElement());
		} else {
			unmapAllElements();
			getControl().setData(null);
		}
		refresh();
	}

	/**
	 * Creates a new empty model for this viewer that
	 * is *not* initialized.
	 *
	 * @return a new model
	 */
	protected abstract AsynchronousModel createModel();

	/**
	 * Creates and returns this viewers update policy.
	 * @return update policy
	 */
	public abstract AbstractUpdatePolicy createUpdatePolicy();

	Image[] getImages(ImageDescriptor[] descriptors) {
		if (descriptors == null || descriptors.length == 0) {
			String[] columns = getPresentationContext().getColumns();
			if (columns == null) {
				return new Image[1];
			} else {
				return new Image[columns.length];
			}
		}
		Image[] images = new Image[descriptors.length];
		for (int i = 0; i < images.length; i++) {
			images[i] = getImage(descriptors[i]);
		}
		return images;
	}

	/**
	 * Returns an image for the given image descriptor or <code>null</code>. Adds the image
	 * to a cache of images if it does not already exist. The cache is cleared when this viewer
	 * is disposed.
	 *
	 * @param descriptor image descriptor or <code>null</code>
	 * @return image or <code>null</code>
	 */
	protected Image getImage(ImageDescriptor descriptor) {
		if (descriptor == null) {
			return null;
		}
		Image image = fImageCache.get(descriptor);
		if (image == null) {
			image = new Image(getControl().getDisplay(), descriptor.getImageData());
			fImageCache.put(descriptor, image);
		}
		return image;
	}

	protected Font[] getFonts(FontData[] fontDatas) {
		if (fontDatas == null || fontDatas.length == 0) {
			String[] columns = getPresentationContext().getColumns();
			if (columns == null) {
				return new Font[1];
			} else {
				return new Font[columns.length];
			}
		}

		Font[] fonts = new Font[fontDatas.length];
		for (int i = 0; i < fonts.length; i++) {
			fonts[i] = getFont(fontDatas[i]);
		}
		return fonts;
	}

	/**
	 * Returns a font for the given font data or <code>null</code>. Adds the font to this viewer's font
	 * cache which is disposed when this viewer is disposed.
	 *
	 * @param fontData font data or <code>null</code>
	 * @return font font or <code>null</code>
	 */
	protected Font getFont(FontData fontData) {
		if (fontData == null) {
			return null;
		}
		Font font = fFontCache.get(fontData);
		if (font == null) {
			font = new Font(getControl().getDisplay(), fontData);
			fFontCache.put(fontData, font);
		}
		return font;
	}

	protected Color[] getColors(RGB[] rgb) {
		if (rgb == null || rgb.length == 0) {
			String[] columns = getPresentationContext().getColumns();
			if (columns == null) {
				return new Color[1];
			} else {
				return new Color[columns.length];
			}
		}
		Color[] colors = new Color[rgb.length];
		for (int i = 0; i < colors.length; i++) {
			colors[i] = getColor(rgb[i]);
		}
		return colors;
	}
	/**
	 * Returns a color for the given RGB or <code>null</code>. Adds the color to this viewer's color
	 * cache which is disposed when this viewer is disposed.
	 *
	 * @param rgb RGB or <code>null</code>
	 * @return color or <code>null</code>
	 */
	protected Color getColor(RGB rgb) {
		if (rgb == null) {
			return null;
		}
		Color color = fColorCache.get(rgb);
		if (color == null) {
			color = new Color(getControl().getDisplay(), rgb);
			fColorCache.put(rgb, color);
		}
		return color;
	}

	/**
	 * Sets the context for this viewer.
	 *
	 * @param context the presentation context
	 */
	public void setContext(IPresentationContext context) {
		fContext = context;
	}

	@Override
	protected Widget doFindItem(Object element) {
		// this viewer maps model nodes to widgets, so the element is a ModelNode
		AsynchronousModel model = getModel();
		if (model != null) {
			if (element.equals(model.getRootNode())) {
				return doFindInputItem(element);
			}
			Widget[] widgets = findItems(element);
			if (widgets.length > 0) {
				return widgets[0];
			}
		}
		return null;
	}

	@Override
	protected Widget doFindInputItem(Object element) {
		if (element instanceof ModelNode) {
			ModelNode node = (ModelNode) element;
			if (node.getElement().equals(getInput())) {
				return getControl();
			}
		}
		return null;
	}

	@Override
	protected void doUpdateItem(Widget item, Object element, boolean fullMap) {
	}

	@Override
	protected void internalRefresh(Object element) {
		// get the nodes in the model
		AsynchronousModel model = getModel();
		if (model != null) {
			ModelNode[] nodes = model.getNodes(element);
			if (nodes != null) {
				for (ModelNode node : nodes) {
					// get the widget for the node
					Widget item = findItem(node);
					if (item != null) {
						internalRefresh(node);
					}
				}
			}
		}
	}

	/**
	 * Refreshes a specific occurrence of an element (a node).
	 *
	 * @param node node to update
	 *
	 * Subclasses should override and call super
	 */
	protected void internalRefresh(ModelNode node) {
		updateLabel(node);
	}

	@Override
	public synchronized void setSelection(ISelection selection, boolean reveal) {
		setSelection(selection, reveal, false);
	}

	/**
	 * Sets the selection in this viewer.
	 *
	 * @param selection new selection
	 * @param reveal whether to reveal the selection
	 * @param force whether to force the selection change without consulting the model
	 *  selection policy
	 */
	public synchronized void setSelection(ISelection selection, final boolean reveal, boolean force) {
		Control control = getControl();
		if (control == null || control.isDisposed()) {
			return;
		}
		if (!acceptsSelection(selection)) {
			selection = getEmptySelection();
		}
		if (!force && !overrideSelection(fCurrentSelection, selection)) {
			return;
		}

		fPendingSelection = selection;

		if (getControl().getDisplay().getThread() == Thread.currentThread()) {
			attemptSelection(reveal);
		} else {
			WorkbenchJob job = new WorkbenchJob("attemptSelection") { //$NON-NLS-1$
				@Override
				public IStatus runInUIThread(IProgressMonitor monitor) {
					attemptSelection(reveal);
					return Status.OK_STATUS;
				}

			};
			job.setSystem(true);
			job.schedule();
		}
	}


	/**
	 * Returns whether the candidate selection should override the current
	 * selection.
	 *
	 * @param current the current selection
	 * @param candidate the new selection
	 * @return if the selection should be overridden
	 */
	protected boolean overrideSelection(ISelection current, ISelection candidate) {
		IModelSelectionPolicy selectionPolicy = getSelectionPolicy(current);
		if (selectionPolicy == null) {
			return true;
		}
		if (selectionPolicy.contains(candidate, getPresentationContext())) {
			return selectionPolicy.overrides(current, candidate, getPresentationContext());
		}
		return !selectionPolicy.isSticky(current, getPresentationContext());
	}

	@Override
	public ISelection getSelection() {
		Control control = getControl();
		if (control == null || control.isDisposed() || fCurrentSelection == null) {
			return StructuredSelection.EMPTY;
		}
		return fCurrentSelection;
	}

	@Override
	protected void handleSelect(SelectionEvent event) {
		// handle case where an earlier selection listener disposed the control.
		Control control = getControl();
		if (control != null && !control.isDisposed()) {
			updateSelection(newSelectionFromWidget());
		}
	}

	@Override
	protected void handlePostSelect(SelectionEvent e) {
		SelectionChangedEvent event = new SelectionChangedEvent(this, newSelectionFromWidget());
		firePostSelectionChanged(event);
	}

	/**
	 * Creates and returns a new selection from this viewer, based on the selected
	 * elements in the widget.
	 *
	 * @return a new selection
	 */
	protected abstract ISelection newSelectionFromWidget();

	/**
	 * Returns the selection policy associated with the given selection
	 * or <code>null</code> if none.
	 *
	 * @param selection or <code>null</code>
	 * @return selection policy or <code>null</code>
	 */
	protected IModelSelectionPolicy getSelectionPolicy(ISelection selection) {
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection ss = (IStructuredSelection) selection;
			Object element = ss.getFirstElement();
			if (element instanceof IAdaptable) {
				IAdaptable adaptable = (IAdaptable) element;
				IModelSelectionPolicyFactory factory =  adaptable.getAdapter(IModelSelectionPolicyFactory.class);
				if (factory != null) {
					return factory.createModelSelectionPolicyAdapter(adaptable, getPresentationContext());
				}
			}
		}
		return null;
	}

	@Override
	final protected void setSelectionToWidget(ISelection selection, final boolean reveal) {
		// NOT USED
		throw new IllegalArgumentException("This method should not be called"); //$NON-NLS-1$
	}

	@Override
	final protected void setSelectionToWidget(List l, boolean reveal) {
		// NOT USED
		throw new IllegalArgumentException("This method should not be called"); //$NON-NLS-1$
	}

	/**
	 * Attempts to update any pending selection.
	 *
	 * @param reveal whether to reveal the selection
	 */
	protected void attemptSelection(boolean reveal) {
		ISelection currentSelection = null;
		synchronized (this) {
			if (fPendingSelection != null) {
				ISelection remaining = doAttemptSelectionToWidget(fPendingSelection, reveal);
				if (remaining.isEmpty()) {
					remaining = null;
				}
				if (!fPendingSelection.equals(remaining)) {
					fPendingSelection = remaining;
					currentSelection = newSelectionFromWidget();
					if (isSuppressEqualSelections() && currentSelection.equals(fCurrentSelection)) {
						return;
					}
				}
			}
		}
		if (currentSelection != null) {
			updateSelection(currentSelection);
			firePostSelectionChanged(new SelectionChangedEvent(this, currentSelection));
		}
	}

	/**
	 * Controls whether selection change notification is sent even when
	 * successive selections are equal.
	 *
	 * TODO: what we really want is to fire selection change on ACTIVATE model
	 * change, even when selection is the same.
	 *
	 * @return whether to suppress change notification for equal successive
	 *         selections
	 */
	protected boolean isSuppressEqualSelections() {
		return true;
	}

	/**
	 * Attempts to selection the specified selection and returns a selection
	 * representing the portion of the selection that could not be honored
	 * and still needs to be selected.
	 *
	 * @param selection selection to attempt
	 * @param reveal whether to reveal the selection
	 * @return remaining selection
	 */
	protected abstract ISelection doAttemptSelectionToWidget(ISelection selection, boolean reveal);

	/**
	 * Returns whether this viewer supports the given selection.
	 *
	 * @param selection a selection
	 * @return whether this viewer supports the given selection
	 */
	protected abstract boolean acceptsSelection(ISelection selection);

	/**
	 * Returns an empty selection supported by this viewer.
	 *
	 * @return an empty selection supported by this viewer
	 */
	protected abstract ISelection getEmptySelection();

	/**
	 * A content provider that does nothing.
	 */
	private static class NullContentProvider implements IStructuredContentProvider {
		@Override
		public void dispose() {
		}

		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		}

		@Override
		public Object[] getElements(Object inputElement) {
			return null;
		}
	}

	/**
	 * Notification that a presentation update has failed.
	 * Subclasses may override as required. The default implementation
	 * does nothing.
	 *
	 * @param monitor monitor for the presentation request that failed
	 * @param status status of update
	 */
	protected void handlePresentationFailure(IStatusMonitor monitor, IStatus status) {
	}

	@Override
	protected synchronized void preservingSelection(Runnable updateCode) {
		if (fPendingSelection == null || fPendingSelection.isEmpty()) {
			ISelection oldSelection = null;
			try {
				// preserve selection
				oldSelection = fCurrentSelection;
				// perform the update
				updateCode.run();
			} finally {
				// restore selection
				if (oldSelection == null) {
					oldSelection = new StructuredSelection();
				}
				if (getControl().getDisplay().getThread() == Thread.currentThread()) {
					if (!oldSelection.equals(newSelectionFromWidget())) {
						restoreSelection(oldSelection);
					}
				} else {
					WorkbenchJob job = new WorkbenchJob("attemptSelection") { //$NON-NLS-1$
						@Override
						public IStatus runInUIThread(IProgressMonitor monitor) {
							synchronized (AsynchronousViewer.this) {
								if (!getControl().isDisposed()) {
									if (fPendingSelection == null || fPendingSelection.isEmpty()) {
										ISelection tempSelection = fCurrentSelection;
										if (tempSelection == null) {
											tempSelection = new StructuredSelection();
										}
										if (!tempSelection.equals(newSelectionFromWidget())) {
											restoreSelection(tempSelection);
										}
									}
								}
							}
							return Status.OK_STATUS;
						}

					};
					job.setSystem(true);
					job.schedule();
				}
			}
		} else {
			updateCode.run();
		}
	}

	protected synchronized void restoreSelection(ISelection oldSelection) {
		ISelection remaining = doAttemptSelectionToWidget(oldSelection, false);
		// send out notification if old and new differ
		fCurrentSelection = newSelectionFromWidget();
		if (!selectionExists(fCurrentSelection)) {
			if (selectionExists(oldSelection)) {
				// old selection exists in the model, but not widget
				fCurrentSelection = oldSelection;
			} else {
				fCurrentSelection = getEmptySelection();
			}
		}
		if (!fCurrentSelection.equals(oldSelection)) {
			handleInvalidSelection(oldSelection, fCurrentSelection);
			// if the remaining selection still exists in the model, make it pending
			if (selectionExists(remaining)) {
				setSelection(remaining);
			}
		}
	}

	/**
	 * Returns whether the selection exists in the model
	 * @param selection the selection context
	 * @return <code>true</code> if the selecton exists in the model <code>false</code> otherwise
	 */
	protected boolean selectionExists(ISelection selection) {
		if (selection.isEmpty()) {
			return false;
		}
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection ss = (IStructuredSelection) selection;
			Iterator<?> iterator = ss.iterator();
			while (iterator.hasNext()) {
				Object element = iterator.next();
				if (getModel().getNodes(element) == null) {
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * Sets the color attributes of the given widget.
	 *
	 * @param widget the widget to update
	 * @param foreground foreground color of the widget or <code>null</code> if default
	 * @param background background color of the widget or <code>null</code> if default
	 */
	protected abstract void setColors(Widget widget, RGB foreground[], RGB background[]);

	/**
	 * Sets the label attributes of the given widget.
	 *
	 * @param widget the widget to update
	 * @param text label text
	 * @param image label image or <code>null</code>
	 */
	protected abstract void setLabels(Widget widget, String[] text, ImageDescriptor[] image);

	/**
	 * Sets the font attributes of the given widget.
	 *
	 * @param widget widget to update
	 * @param font font of the widget or <code>null</code> if default.
	 */
	protected abstract void setFonts(Widget widget, FontData[] font);

	@Override
	protected synchronized void updateSelection(ISelection selection) {
		fCurrentSelection = selection;
		super.updateSelection(selection);
	}



	/**
	 * Notification the given model proxy has been added to this viewer's model.
	 *
	 * @param proxy the model proxy that has been added
	 */
	protected void modelProxyAdded(IModelProxy proxy) {
		if (fUpdatePolicy instanceof IModelChangedListener) {
			proxy.addModelChangedListener((IModelChangedListener)fUpdatePolicy);
		}
	}

	/**
	 * Notification the given model proxy has been removed from this viewer's model.
	 *
	 * @param proxy the model proxy that has been removed
	 */
	protected void modelProxyRemoved(IModelProxy proxy) {
		if (fUpdatePolicy instanceof IModelChangedListener) {
			proxy.removeModelChangedListener((IModelChangedListener)fUpdatePolicy);
		}
	}

	/**
	 * Returns this viewer's model
	 *
	 * @return model
	 */
	protected AsynchronousModel getModel() {
		return fModel;
	}

	/**
	 * A node in the model has been updated
	 *
	 * @param node the model node that has been changed
	 */
	protected void nodeChanged(ModelNode node) {
		Widget widget = findItem(node);
		if (widget != null) {
			clear(widget);
			attemptPendingUpdates();
		}
	}

	/**
	 * @return if there are any more pending updates in the viewer
	 */
	public synchronized boolean hasPendingUpdates() {
		return getModel().hasPendingUpdates();
	}

	/**
	 * Notification from the model that the update for the given request
	 * has completed.
	 *
	 * @param monitor the monitor
	 */
	protected void updateComplete(IStatusMonitor monitor) {
	}

	/**
	 * Clears the given widget
	 *
	 * @param item the widget
	 */
	protected abstract void clear(Widget item);

	/**
	 * Clears the children of the widget.
	 *
	 * @param item the widget to clear children from
	 */
	protected abstract void clearChildren(Widget item);

	/**
	 * Clears the child at the given index.
	 *
	 * @param parent the parent widget
	 * @param childIndex the index of the child widget to clear
	 */
	protected abstract void clearChild(Widget parent, int childIndex);

	/**
	 * Returns the child widget at the given index for the given parent or
	 * <code>null</code>
	 *
	 * @param parent the parent widget
	 * @param index the index of the child in the parent widget
	 * @return the widget at the given index in the parent or <code>null</code>
	 */
	protected abstract Widget getChildWidget(Widget parent, int index);

	/**
	 * Sets the item count for a parent widget
	 *
	 * @param parent the parent widget
	 * @param itemCount the new item count to set
	 */
	protected abstract void setItemCount(Widget parent, int itemCount);

	/**
	 * Attempt pending updates. Subclasses may override but should call super.
	 */
	protected void attemptPendingUpdates() {
		attemptSelection(false);
	}

	/**
	 * Notification a node's children have changed.
	 * Updates the child count for the parent's widget
	 * and clears children to be updated.
	 *
	 * @param parentNode the parent model node
	 */
	protected void nodeChildrenChanged(ModelNode parentNode) {
		Widget widget = findItem(parentNode);
		if (widget != null && !widget.isDisposed()) {
			int childCount = parentNode.getChildCount();
			setItemCount(widget, childCount);
			clearChildren(widget);
			attemptPendingUpdates();
		}
	}

	/**
	 * Notification children have been added to the end
	 * of the given parent.
	 *
	 * @param parentNode the parent model node
	 */
	protected void nodeChildrenAdded(ModelNode parentNode) {
		Widget widget = findItem(parentNode);
		if (widget != null && !widget.isDisposed()) {
			int childCount = parentNode.getChildCount();
			setItemCount(widget, childCount);
			attemptPendingUpdates();
		}
	}

	/**
	 * Notification children have been added to the end
	 * of the given parent.
	 *
	 * @param parentNode the parent model node
	 * @param index the index of the child that was removed
	 */
	protected void nodeChildRemoved(ModelNode parentNode, int index) {
		Widget widget = findItem(parentNode);
		if (widget != null && !widget.isDisposed()) {
			Widget childWidget = getChildWidget(widget, index);
			int childCount = parentNode.getChildCount();
			// if the child widget exists, dispose it so item state remains, otherwise update child count
			if (childWidget == null) {
				setItemCount(widget, childCount);
			} else {
				childWidget.dispose();
			}
			for (int i = index; i < childCount; i ++) {
				clearChild(widget, i);
			}
			attemptPendingUpdates();
		}
	}

	/**
	 * Unmaps the node from its widget and all of its children nodes from
	 * their widgets.
	 *
	 * @param node the model node
	 */
	protected void unmapNode(ModelNode node) {
		unmapElement(node);
		ModelNode[] childrenNodes = node.getChildrenNodes();
		if (childrenNodes != null) {
			for (ModelNode childNode : childrenNodes) {
				unmapNode(childNode);
			}
		}
	}

	/**
	 * Returns the node corresponding to the given widget or <code>null</code>
	 * @param widget widget for which a node is requested
	 * @return node or <code>null</code>
	 */
	protected ModelNode findNode(Widget widget) {
		ModelNode[] nodes = getModel().getNodes(widget.getData());
		if (nodes != null) {
			for (ModelNode node : nodes) {
				Widget item = findItem(node);
				if (widget == item) {
					return node;
				}
			}
		}
		return null;
	}

	/**
	 * Returns the item for the node or <code>null</code>
	 * @param node the model node
	 * @return the widget or <code>null</code>
	 */
	protected Widget findItem(ModelNode node) {
		return findItem((Object)node);
	}

	/*
	 * A virtual item has been exposed in the control, map its data.
	 */
	@Override
	public void handleEvent(final Event event) {
		update((Item)event.item, event.index);
	}

	/**
	 * Update the given item.
	 *
	 * @param item item to update
	 * @param index index of item in parent's children
	 */
	protected void update(Item item, int index) {
		restoreLabels(item);
		int level = 0;

		Widget parentItem = getParentWidget(item);
		if (DebugUIPlugin.DEBUG_VIEWER) {
			DebugUIPlugin.trace("SET DATA [" + index + "]: " + parentItem);  //$NON-NLS-1$//$NON-NLS-2$
		}
		ModelNode node = null;
		// first, see if the parent element is in the model
		// and look directly for the child
		if (parentItem != null) {
			ModelNode[] nodes = getModel().getNodes(parentItem.getData());
			if (nodes != null) {
				for (ModelNode parentNode : nodes) {
					Widget parentWidget = findItem(parentNode);
					if (parentWidget == parentItem) {
						ModelNode[] childrenNodes = parentNode.getChildrenNodes();
						if (childrenNodes != null && index < childrenNodes.length) {
							node = childrenNodes[index];
						}
					}
				}
			}
		}

		// otherwise, build a path to the model node
		if (node == null) {
			setNodeIndex(index, level);
			while (parentItem instanceof Item) {
				level++;
				Widget parent = getParentWidget(parentItem);
				int pindex = indexOf(parent, parentItem);
				if (pindex < 0) {
					return;
				}
				setNodeIndex(pindex, level);
				parentItem = parent;
			}

			node = getModel().getRootNode();
			if (node == null) {
				if (DebugUIPlugin.DEBUG_VIEWER) {
					DebugUIPlugin.trace("\tFAILED - root model node is null"); //$NON-NLS-1$
				}
				return;
			}
			for (int i = level; i >= 0; i--) {
				ModelNode[] childrenNodes = node.getChildrenNodes();
				if (childrenNodes == null) {
					if (DebugUIPlugin.DEBUG_VIEWER) {
						DebugUIPlugin.trace("\tFAILED - no children nodes for " + node); //$NON-NLS-1$
					}
					return;
				}
				int pindex = getNodeIndex(i);
				if (pindex < childrenNodes.length) {
					node = childrenNodes[pindex];
				} else {
					if (DebugUIPlugin.DEBUG_VIEWER) {
						DebugUIPlugin.trace("\tFAILED - no children nodes for " + node); //$NON-NLS-1$
					}
					return;
				}
			}
		}


		// map the node to the element and refresh it
		if (node != null) {
			mapElement(node, item);
			item.setData(node.getElement());
			if (DebugUIPlugin.DEBUG_VIEWER) {
				DebugUIPlugin.trace("\titem mapped: " + node); //$NON-NLS-1$
			}
			internalRefresh(node);
		} else {
			if (DebugUIPlugin.DEBUG_VIEWER) {
				DebugUIPlugin.trace("\tFAILED - unable to find corresponding node"); //$NON-NLS-1$
			}
		}
	}

	/**
	 * Sets the index of a child node being mapped at the given expansion level
	 * in the tree.
	 *
	 * @param nodeIndex the index of the node
	 * @param level the expansion level
	 */
	private void setNodeIndex(int nodeIndex, int level) {
		if (level > (fSetDataIndicies.length - 1)) {
			// grow the array
			int[] next = new int[level+5];
			System.arraycopy(fSetDataIndicies, 0, next, 0, fSetDataIndicies.length);
			fSetDataIndicies = next;
		}
		fSetDataIndicies[level] = nodeIndex;
	}

	/**
	 * Returns the index of a child node being mapped at the given expansion level in
	 * the tree.
	 *
	 * @param level the expansion level
	 * @return the child index
	 */
	private int getNodeIndex(int level) {
		return fSetDataIndicies[level];
	}

	protected abstract int indexOf(Widget parent, Widget child);

	protected abstract void restoreLabels(Item item);

	/**
	 * Returns the parent widget for the given widget or <code>null</code>
	 *
	 * @param widget the widget to get the parent from
	 * @return parent widget or <code>null</code>
	 */
	protected abstract Widget getParentWidget(Widget widget);

	/**
	 * Updates the children of the given node.
	 *
	 * @param parent
	 *            node of which to update children
	 */
	protected void updateChildren(ModelNode parent) {
		getModel().updateChildren(parent);
	}
}
