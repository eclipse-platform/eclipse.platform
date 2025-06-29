/*******************************************************************************
 * Copyright (c) 2000, 2018 IBM Corporation and others.
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
package org.eclipse.compare.structuremergeviewer;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.CompareUI;
import org.eclipse.compare.CompareViewerSwitchingPane;
import org.eclipse.compare.ICompareFilter;
import org.eclipse.compare.IContentChangeListener;
import org.eclipse.compare.IContentChangeNotifier;
import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.contentmergeviewer.IDocumentRange;
import org.eclipse.compare.internal.ChangeCompareFilterPropertyAction;
import org.eclipse.compare.internal.CompareMessages;
import org.eclipse.compare.internal.CompareUIPlugin;
import org.eclipse.compare.internal.Utilities;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.text.BadPositionCategoryException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.services.IDisposable;


/**
 * A diff tree viewer that can be configured with a <code>IStructureCreator</code>
 * to retrieve a hierarchical structure from the input object (an <code>ICompareInput</code>)
 * and perform a two-way or three-way compare on it.
 * <p>
 * This class may be instantiated; it is not intended to be subclassed outside
 * this package.
 * </p>
 *
 * @see IStructureCreator
 * @see ICompareInput
 * @noextend This class is not intended to be subclassed by clients.
 */

public class StructureDiffViewer extends DiffTreeViewer {
	private Differencer fDifferencer;
	private boolean fThreeWay= false;

	private final StructureInfo fAncestorStructure = new StructureInfo();
	private final StructureInfo fLeftStructure = new StructureInfo();
	private final StructureInfo fRightStructure = new StructureInfo();

	private IStructureCreator fStructureCreator;
	private IDiffContainer fRoot;
	private IContentChangeListener fContentChangedListener;
	private CompareViewerSwitchingPane fParent;
	private ICompareInputChangeListener fCompareInputChangeListener;

	/*
	 * A set of background tasks for updating the structure
	 */
	private final IRunnableWithProgress diffTask = monitor -> {
		monitor.beginTask(CompareMessages.StructureDiffViewer_0, 100);
		diff(SubMonitor.convert(monitor, 100));
		monitor.done();
	};

	private final IRunnableWithProgress inputChangedTask = monitor -> {
		monitor.beginTask(CompareMessages.StructureDiffViewer_1, 100);
		// TODO: Should we always force
		compareInputChanged((ICompareInput) getInput(), true, SubMonitor.convert(monitor, 100));
		monitor.done();
	};

	/*
	 * A helper class for holding the input and generated structure
	 * for the ancestor, left and right inputs.
	 */
	private class StructureInfo {
		private ITypedElement fInput;
		private IStructureComparator fStructureComparator;
		private final IRunnableWithProgress refreshTask = this::refresh;

		public boolean setInput(ITypedElement newInput, boolean force, IProgressMonitor monitor) {
			boolean changed = false;
			if (force || newInput != fInput) {
				removeDocumentRangeUpdaters();
				if (fInput instanceof IContentChangeNotifier && fContentChangedListener != null) {
					((IContentChangeNotifier)fInput).removeContentChangeListener(fContentChangedListener);
				}
				fInput= newInput;
				if (fInput == null) {
					dispose(); // destroy fStructureComparator
					fStructureComparator= null;
				} else {
					refresh(monitor);
					changed= true;
				}
				if (fInput instanceof IContentChangeNotifier && fContentChangedListener != null) {
					((IContentChangeNotifier)fInput).addContentChangeListener(fContentChangedListener);
				}
			}
			return changed;
		}

		/**
		 * Remove any document range updaters that were registered against the document.
		 */
		private void removeDocumentRangeUpdaters() {
			if (fStructureComparator instanceof IDocumentRange) {
				IDocument doc = ((IDocumentRange) fStructureComparator).getDocument();
				try {
					doc.removePositionCategory(IDocumentRange.RANGE_CATEGORY);
				} catch (BadPositionCategoryException ex) {
					// Ignore
				}
			}
		}

		public IStructureComparator getStructureComparator() {
			return fStructureComparator;
		}

		public void refresh(IProgressMonitor monitor) {
			IStructureComparator oldComparator = fStructureComparator;
			fStructureComparator= createStructure(monitor);
			// Dispose of the old one after in case they are using a shared document
			// (i.e. disposing it after will hold on to a reference to the document
			// so it doesn't get freed and reloaded)
			if (oldComparator instanceof IDisposable disposable) {
				disposable.dispose();
			}
		}

		public Object getInput() {
			return fInput;
		}

		private IStructureComparator createStructure(IProgressMonitor monitor) {
			// Defend against concurrent disposal
			Object input = fInput;
			if (input == null) {
				return null;
			}
			if (fStructureCreator instanceof IStructureCreator2 sc2) {
				try {
					return sc2.createStructure(input, monitor);
				} catch (CoreException e) {
					CompareUIPlugin.log(e);
				}
			}
			return fStructureCreator.getStructure(input);
		}

		public void dispose() {
			if (fStructureComparator != null && fStructureCreator instanceof IStructureCreator2 sc2) {
				sc2.destroy(fStructureComparator);
			}
		}

		public IRunnableWithProgress getRefreshTask() {
			return refreshTask;
		}
	}

	/**
	 * Creates a new viewer for the given SWT tree control with the specified configuration.
	 *
	 * @param tree the tree control
	 * @param configuration the configuration for this viewer
	 */
	public StructureDiffViewer(Tree tree, CompareConfiguration configuration) {
		super(tree, configuration);
		Composite c= tree.getParent();
		if (c instanceof CompareViewerSwitchingPane) {
			fParent= (CompareViewerSwitchingPane) c;
		}
		initialize();
	}

	/**
	 * Creates a new viewer under the given SWT parent with the specified configuration.
	 *
	 * @param parent the SWT control under which to create the viewer
	 * @param configuration the configuration for this viewer
	 */
	public StructureDiffViewer(Composite parent, CompareConfiguration configuration) {
		super(parent, configuration);
		if (parent instanceof CompareViewerSwitchingPane) {
			fParent= (CompareViewerSwitchingPane) parent;
		}
		initialize();
	}

	private void initialize() {

		setAutoExpandLevel(3);

		fContentChangedListener= StructureDiffViewer.this::contentChanged;
		fCompareInputChangeListener = input -> StructureDiffViewer.this.compareInputChanged(input, true);
	}

	/**
	 * Configures the <code>StructureDiffViewer</code> with a structure creator.
	 * The structure creator is used to create a hierarchical structure
	 * for each side of the viewer's input element of type <code>ICompareInput</code>.
	 *
	 * @param structureCreator the new structure creator
	 */
	public void setStructureCreator(IStructureCreator structureCreator) {
		if (fStructureCreator != structureCreator) {
			fStructureCreator= structureCreator;
			Control tree= getControl();
			if (tree != null && !tree.isDisposed()) {
				tree.setData(CompareUI.COMPARE_VIEWER_TITLE, getTitle());
			}
		}
	}

	/**
	 * Returns the structure creator or <code>null</code> if no
	 * structure creator has been set with <code>setStructureCreator</code>.
	 *
	 * @return the structure creator or <code>null</code>
	 */
	public IStructureCreator getStructureCreator() {
		return fStructureCreator;
	}

	/**
	 * Reimplemented to get the descriptive title for this viewer from the <code>IStructureCreator</code>.
	 * @return the viewer's name
	 */
	@Override
	public String getTitle() {
		if (fStructureCreator != null) {
			return fStructureCreator.getName();
		}
		return super.getTitle();
	}

	/**
	 * Overridden because the input of this viewer is not identical to the root of the tree.
	 * The tree's root is a IDiffContainer that was returned from the method <code>diff</code>.
	 *
	 * @return the root of the diff tree produced by method <code>diff</code>
	 */
	@Override
	protected Object getRoot() {
		return fRoot;
	}

	/*
	 * Method declared on StructuredViewer. Overridden to create the comparable
	 * structures from the input object and to feed them through the differencing
	 * engine. Note: for this viewer the value from <code>getInput</code> is not
	 * identical to <code>getRoot</code>.
	 */
	@Override
	protected void inputChanged(Object input, Object oldInput) {
		if (oldInput instanceof ICompareInput old) {
			old.removeCompareInputChangeListener(fCompareInputChangeListener);
		}
		if (input instanceof ICompareInput ci) {
			ci.addCompareInputChangeListener(fCompareInputChangeListener);
			compareInputChanged(ci);
			if (input != oldInput) {
				initialSelection();
			}
		}
	}

	@Override
	protected void initialSelection() {
		expandToLevel(2);
	}

	/* (non Javadoc)
	 * Overridden to unregister all listeners.
	 */
	@Override
	protected void handleDispose(DisposeEvent event) {
		Object input = getInput();
		if (input instanceof ICompareInput ci) {
			ci.removeCompareInputChangeListener(fCompareInputChangeListener);
		}
		compareInputChanged(null);
		fContentChangedListener= null;
		super.handleDispose(event);
	}

	/**
	 * Recreates the comparable structures for the input sides.
	 * @param input this viewer's new input
	 */
	protected void compareInputChanged(ICompareInput input) {
		compareInputChanged(input, false);
	}

	/* package */ void compareInputChanged(final ICompareInput input, final boolean force) {
		if (input == null) {
			// When closing, we don't need a progress monitor to handle the input change
			compareInputChanged(input, force, null);
			return;
		}
		CompareConfiguration cc = getCompareConfiguration();
		// The compare configuration is nulled when the viewer is disposed
		if (cc != null) {
			BusyIndicator.showWhile(Display.getDefault(), () -> {
				try {
					inputChangedTask.run(new NullProgressMonitor());
				} catch (InvocationTargetException e1) {
					CompareUIPlugin.log(e1.getTargetException());
				} catch (InterruptedException e2) {
					// Ignore
				}
			});
		}
	}

	/* package */ void compareInputChanged(ICompareInput input, boolean force, IProgressMonitor monitor) {
		ITypedElement t= null;
		boolean changed= false;

		if (input != null) {
			t= input.getAncestor();
		}
		fThreeWay= (t != null);
		beginWork(monitor, 400);
		try {
			if (fAncestorStructure.setInput(t, force, subMonitor(monitor, 100))) {
				changed = true;
			}

			if (input != null) {
				t= input.getLeft();
			}
			if (fLeftStructure.setInput(t, force, subMonitor(monitor, 100))) {
				changed = true;
			}

			if (input != null) {
				t= input.getRight();
			}
			if (fRightStructure.setInput(t, force, subMonitor(monitor, 100))) {
				changed = true;
			}

			// The compare configuration is nulled when the viewer is disposed
			CompareConfiguration cc = getCompareConfiguration();
			if (changed && cc != null) {
				cc.getContainer().runAsynchronously(diffTask);
			}
		} finally {
			endWork(monitor);
		}
	}

	private void endWork(IProgressMonitor monitor) {
		if (monitor != null) {
			monitor.done();
		}
	}

	private IProgressMonitor subMonitor(IProgressMonitor monitor, int work) {
		if (monitor != null) {
			if (monitor.isCanceled() || getControl().isDisposed()) {
				throw new OperationCanceledException();
			}
			return SubMonitor.convert(monitor, work);
		}
		return null;
	}

	private void beginWork(IProgressMonitor monitor, int totalWork) {
		if (monitor != null) {
			monitor.beginTask(null, totalWork);
		}
	}

	/**
	 * Calls <code>diff</code> whenever the byte contents changes.
	 * @param changed the object that sent out the notification
	 */
	protected void contentChanged(final IContentChangeNotifier changed) {
		if (fStructureCreator == null) {
			return;
		}

		if (changed == null) {
			getCompareConfiguration().getContainer().runAsynchronously(fAncestorStructure.getRefreshTask());
			getCompareConfiguration().getContainer().runAsynchronously(fLeftStructure.getRefreshTask());
			getCompareConfiguration().getContainer().runAsynchronously(fRightStructure.getRefreshTask());
		} else if (changed == fAncestorStructure.getInput()) {
			getCompareConfiguration().getContainer().runAsynchronously(fAncestorStructure.getRefreshTask());
		} else if (changed == fLeftStructure.getInput()) {
			getCompareConfiguration().getContainer().runAsynchronously(fLeftStructure.getRefreshTask());
		} else if (changed == fRightStructure.getInput()) {
			getCompareConfiguration().getContainer().runAsynchronously(fRightStructure.getRefreshTask());
		} else {
			return;
		}
		getCompareConfiguration().getContainer().runAsynchronously(diffTask);
	}

	/**
	 * This method is called from within <code>diff()</code> before the
	 * difference tree is being built. Clients may override this method to
	 * perform their own pre-processing. This default implementation does
	 * nothing.
	 *
	 * @param ancestor the ancestor input to the differencing operation
	 * @param left the left input to the differencing operation
	 * @param right the right input to the differencing operation
	 * @since 2.0
	 * @deprecated Clients should override
	 *             {@link #preDiffHook(IStructureComparator, IStructureComparator, IStructureComparator, IProgressMonitor)}
	 */
	@Deprecated
	protected void preDiffHook(IStructureComparator ancestor, IStructureComparator left, IStructureComparator right) {
		// we do nothing here
	}

	/**
	 * This method is called from within {@link #diff(IProgressMonitor)} before
	 * the difference tree is being built. This method may be called from a
	 * background (non-UI) thread).
	 * <p>
	 * For backwards compatibility, this default implementation calls
	 * {@link #preDiffHook(IStructureComparator, IStructureComparator, IStructureComparator)}
	 * from the UI thread. Clients should override this method even if they
	 * don't perform pre-processing to avoid the call to the UI thread.
	 *
	 * @param ancestor the ancestor input to the differencing operation
	 * @param left the left input to the differencing operation
	 * @param right the right input to the differencing operation
	 * @param monitor a progress monitor or null if progress is not required
	 * @since 3.3
	 */
	protected void preDiffHook(final IStructureComparator ancestor, final IStructureComparator left, final IStructureComparator right, IProgressMonitor monitor) {
		syncExec(() -> preDiffHook(ancestor, left, right));
	}

	/**
	 * Runs the difference engine and refreshes the tree. This method may be called
	 * from a background (non-UI) thread).
	 * @param monitor a progress monitor or <code>null</code> if progress in not required
	 */
	protected void diff(IProgressMonitor monitor) {
		try {
			beginWork(monitor, 150);

			IStructureComparator ancestorComparator = fAncestorStructure.getStructureComparator();
			IStructureComparator leftComparator = fLeftStructure.getStructureComparator();
			IStructureComparator rightComparator = fRightStructure.getStructureComparator();

			preDiffHook(ancestorComparator,
					leftComparator,
					rightComparator,
					subMonitor(monitor, 25));

			String message= null;

			if ((fThreeWay && ancestorComparator == null) || leftComparator == null || rightComparator == null) {
				// could not get structure of one (or more) of the legs
				fRoot= null;
				message= CompareMessages.StructureDiffViewer_StructureError;
			} else {	// calculate difference of the two (or three) structures
				if (fDifferencer == null) {
					fDifferencer= new Differencer() {
						@Override
						protected boolean contentsEqual(Object o1,
								char contributor1, Object o2, char contributor2) {
							return StructureDiffViewer.this.contentsEqual(o1,
									contributor1, o2, contributor2);
						}
						@Override
						protected Object visit(Object data, int result, Object ancestor, Object left, Object right) {
							Object o= super.visit(data, result, ancestor, left, right);
							if (!getCompareConfiguration().isMirrored() && o instanceof DiffNode) {
								((DiffNode)o).swapSides(true);
							}
							return o;
						}
					};
				}

				fRoot= (IDiffContainer) fDifferencer.findDifferences(fThreeWay, subMonitor(monitor, 100), null,
						ancestorComparator, leftComparator, rightComparator);

				if (fRoot == null || fRoot.getChildren().length == 0) {
					message= CompareMessages.StructureDiffViewer_NoStructuralDifferences;
				} else {
					postDiffHook(fDifferencer, fRoot, subMonitor(monitor, 25));
				}
			}

			if (Display.getCurrent() != null) {
				refreshAfterDiff(message);
			} else {
				final String theMessage = message;
				Display.getDefault().asyncExec(() -> refreshAfterDiff(theMessage));
			}
		} finally {
			endWork(monitor);
		}
	}

	private void refreshAfterDiff(String message) {
		if (getControl().isDisposed()) {
			return;
		}
		if (fParent != null) {
			fParent.setTitleArgument(message);
		}

		refresh(getRoot());
		// Setting the auto-expand level doesn't do anything for refreshes
		expandToLevel(3);
	}

	/**
	 * Runs the difference engine and refreshes the tree.
	 */
	protected void diff() {
		try {
			CompareConfiguration compareConfiguration = getCompareConfiguration();
			// A null compare configuration indicates that the viewer was disposed
			if (compareConfiguration != null) {
				compareConfiguration.getContainer().run(true, true, monitor -> {
					try {
						diffTask.run(SubMonitor.convert(monitor, CompareMessages.StructureDiffViewer_2, 100));
					} finally {
						monitor.done();
					}
				});
			}
		} catch (InvocationTargetException e) {
			// Shouldn't happen since the run doesn't throw
			CompareUIPlugin.log(e.getTargetException());
			handleFailedRefresh(e.getTargetException().getMessage());
		} catch (InterruptedException e) {
			// Canceled by user
			handleFailedRefresh(CompareMessages.StructureDiffViewer_3);
		}
	}

	private void handleFailedRefresh(final String message) {
		Runnable runnable = () -> {
			if (getControl().isDisposed()) {
				return;
			}
			refreshAfterDiff(message);
		};
		if (Display.getCurrent() != null) {
			runnable.run();
		} else {
			Display.getDefault().asyncExec(runnable);
		}
	}

	/**
	 * This method is called from within <code>diff()</code> after the
	 * difference tree has been built. Clients may override this method to
	 * perform their own post-processing. This default implementation does
	 * nothing.
	 *
	 * @param differencer the differencer used to perform the differencing
	 * @param root the non-<code>null</code> root node of the difference tree
	 * @since 2.0
	 * @deprecated Subclasses should override
	 *             {@link #postDiffHook(Differencer, IDiffContainer, IProgressMonitor)}
	 *             instead
	 */
	@Deprecated
	protected void postDiffHook(Differencer differencer, IDiffContainer root) {
		// we do nothing here
	}

	/**
	 * This method is called from within {@link #diff(IProgressMonitor)} after
	 * the difference tree has been built. This method may be called from a
	 * background (non-UI) thread).
	 * <p>
	 * For backwards compatibility, this default implementation calls
	 * {@link #postDiffHook(Differencer, IDiffContainer)} from the UI thread.
	 * Clients should override this method even if they don't perform post
	 * processing to avoid the call to the UI thread.
	 *
	 * @param differencer the differencer used to perform the differencing
	 * @param root the non-<code>null</code> root node of the difference tree
	 * @param monitor a progress monitor or <code>null</code> if progress is
	 *            not required
	 * @since 3.3
	 */
	protected void postDiffHook(final Differencer differencer, final IDiffContainer root, IProgressMonitor monitor) {
		syncExec(() -> postDiffHook(differencer, root));
	}

	/*
	 * Performs a byte compare on the given objects.
	 * Called from the difference engine.
	 * Returns <code>null</code> if no structure creator has been set.
	 */
	private boolean contentsEqual(Object o1, char contributor1, Object o2,
			char contributor2) {
		if (fStructureCreator != null) {
			boolean ignoreWhiteSpace = Utilities.getBoolean(
					getCompareConfiguration(),
					CompareConfiguration.IGNORE_WHITESPACE, false);
			ICompareFilter[] compareFilters =
					Utilities.getCompareFilters(getCompareConfiguration());
			String s1, s2;
			if (compareFilters != null && compareFilters.length > 0
					&& fStructureCreator instanceof StructureCreator) {
				return ((StructureCreator) fStructureCreator).contentsEquals(
						o1, contributor1, o2, contributor2, ignoreWhiteSpace,
						compareFilters);
			}

			s1 = fStructureCreator.getContents(o1, ignoreWhiteSpace);
			s2 = fStructureCreator.getContents(o2, ignoreWhiteSpace);
			if (s1 == null || s2 == null) {
				return false;
			}
			return s1.equals(s2);
		}
		return false;
	}

	/**
	 * Tracks property changes of the configuration object.
	 * Clients may override to track their own property changes.
	 * In this case they must call the inherited method.
	 * @param event the property changed event that triggered the call to this method
	 */
	@Override
	protected void propertyChange(PropertyChangeEvent event) {
		String key= event.getProperty();
		if (key.equals(CompareConfiguration.IGNORE_WHITESPACE)) {
			diff();
		} else if (key.equals(ChangeCompareFilterPropertyAction.COMPARE_FILTERS)
				&& getCompareConfiguration().getProperty(
						ChangeCompareFilterPropertyAction.COMPARE_FILTERS_INITIALIZING) == null) {
			diff();
		} else if (key.equals("ANCESTOR_STRUCTURE_REFRESH")) { //$NON-NLS-1$
			fAncestorStructure.refresh(new NullProgressMonitor());
			diff();
		} else if (key.equals("LEFT_STRUCTURE_REFRESH")) { //$NON-NLS-1$
			fLeftStructure.refresh(new NullProgressMonitor());
			diff();
		} else if (key.equals("RIGHT_STRUCTURE_REFRESH")) { //$NON-NLS-1$
			fRightStructure.refresh(new NullProgressMonitor());
			diff();
		} else if (key.equals("ALL_STRUCTURE_REFRESH")) { //$NON-NLS-1$
			fAncestorStructure.refresh(new NullProgressMonitor());
			fLeftStructure.refresh(new NullProgressMonitor());
			fRightStructure.refresh(new NullProgressMonitor());
			diff();
		} else {
			super.propertyChange(event);
		}
	}

	/**
	 * Overridden to call the <code>save</code> method on the structure creator after
	 * nodes have been copied from one side to the other side of an input object.
	 *
	 * @param leftToRight if <code>true</code> the left side is copied to the right side.
	 * If <code>false</code> the right side is copied to the left side
	 */
	@Override
	protected void copySelected(boolean leftToRight) {
		super.copySelected(leftToRight);

		if (fStructureCreator != null) {
			fStructureCreator.save(
							leftToRight ? fRightStructure.getStructureComparator() : fLeftStructure.getStructureComparator(),
							leftToRight ? fRightStructure.getInput() : fLeftStructure.getInput());
		}
	}

	private void syncExec(final Runnable runnable) {
		if (getControl().isDisposed()) {
			return;
		}
		if (Display.getCurrent() != null) {
			runnable.run();
		} else {
			getControl().getDisplay().syncExec(() -> {
				if (!getControl().isDisposed()) {
					runnable.run();
				}
			});
		}
	}
}

