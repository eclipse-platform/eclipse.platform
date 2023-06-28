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

package org.eclipse.unittest.internal.ui;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import org.eclipse.unittest.internal.UnitTestPlugin;
import org.eclipse.unittest.internal.model.Status;
import org.eclipse.unittest.internal.model.TestCaseElement;
import org.eclipse.unittest.internal.model.TestElement;
import org.eclipse.unittest.internal.model.TestRunSession;
import org.eclipse.unittest.internal.model.TestSuiteElement;
import org.eclipse.unittest.internal.ui.TestRunnerViewPart.TestResultsLayout;
import org.eclipse.unittest.model.ITestCaseElement;
import org.eclipse.unittest.model.ITestElement;
import org.eclipse.unittest.model.ITestElement.Result;
import org.eclipse.unittest.model.ITestRunSession;
import org.eclipse.unittest.model.ITestSuiteElement;

import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.TableItem;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.viewers.ViewerFilter;

import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.part.PageBook;

import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;

/**
 * A Test Viewer implementation
 */
class TestViewer {

	private final class TestSelectionListener implements ISelectionChangedListener {
		@Override
		public void selectionChanged(SelectionChangedEvent event) {
			IStructuredSelection selection = (IStructuredSelection) fSelectionProvider.getSelection();
			TestElement testElement = null;
			if (selection.size() == 1) {
				testElement = (TestElement) selection.getFirstElement();
			}
			fTestRunnerPart.handleTestSelected(testElement);
		}
	}

	private final class TestOpenListener extends SelectionAdapter {
		@Override
		public void widgetDefaultSelected(SelectionEvent e) {
			handleDefaultSelected();
		}
	}

	private final class FailuresOnlyFilter extends ViewerFilter {
		@Override
		public boolean select(Viewer viewer, Object parentElement, Object element) {
			return select(((TestElement) element));
		}

		public boolean select(TestElement testElement) {
			Status status = testElement.getStatus();
			if (status.isErrorOrFailure())
				return true;
			else
				return !fTestRunSession.isRunning() && status == Status.RUNNING; // rerunning
		}
	}

	private final class IgnoredOnlyFilter extends ViewerFilter {
		@Override
		public boolean select(Viewer viewer, Object parentElement, Object element) {
			return select(((TestElement) element));
		}

		public boolean select(TestElement testElement) {
			if (hasIgnoredInTestResult(testElement))
				return true;
			else
				return !fTestRunSession.isRunning() && testElement.getStatus() == Status.RUNNING; // rerunning
		}

		/**
		 * Checks whether a test was skipped i.e. it was ignored (<code>@Ignored</code>)
		 * or had any assumption failure.
		 *
		 * @param testElement the test element (a test suite or a single test case)
		 *
		 * @return <code>true</code> if the test element or any of its children has
		 *         {@link Result#IGNORED} test result
		 */
		private boolean hasIgnoredInTestResult(TestElement testElement) {
			if (testElement instanceof TestSuiteElement) {
				List<TestElement> children = ((TestSuiteElement) testElement).getChildren();
				for (TestElement child : children) {
					boolean hasIgnoredTestResult = hasIgnoredInTestResult(child);
					if (hasIgnoredTestResult) {
						return true;
					}
				}
				return false;
			}

			return testElement.getTestResult(false) == Result.IGNORED;
		}
	}

	private static class ReverseList<E> extends AbstractList<E> {
		private final List<E> fList;

		public ReverseList(List<E> list) {
			fList = list;
		}

		@Override
		public E get(int index) {
			return fList.get(fList.size() - index - 1);
		}

		@Override
		public int size() {
			return fList.size();
		}
	}

	private class ExpandAllAction extends Action {
		public ExpandAllAction() {
			setText(Messages.ExpandAllAction_text);
			setToolTipText(Messages.ExpandAllAction_tooltip);
		}

		@Override
		public void run() {
			fTreeViewer.expandAll();
		}
	}

	private class CollapseAllAction extends Action {
		public CollapseAllAction() {
			setText(Messages.CollapseAllAction_text);
			setToolTipText(Messages.CollapseAllAction_tooltip);
		}

		@Override
		public void run() {
			fTreeViewer.collapseAll();
		}
	}

	/**
	 * Compares two {@link TestElement}s: - {@link TestSuiteElement}s are placed on
	 * top of {@link TestCaseElement}s - TestElements are alphabetically ordered(by
	 * their names)
	 */
	private static final ViewerComparator TEST_ELEMENT_ALPHABETIC_ORDER = new ViewerComparator() {
		@Override
		public int compare(Viewer viewer, Object e1, Object e2) {
			// Show test suites on top of test messages
			int weight1 = (e1 instanceof ITestSuiteElement) ? 0 : 1;
			int weight2 = (e2 instanceof ITestSuiteElement) ? 0 : 1;
			if (weight1 != weight2) {
				return weight1 - weight2;
			}
			// Compare by element names
			return ((TestElement) e1).getTestName().compareTo(((TestElement) e2).getTestName());
		}
	};

	private final FailuresOnlyFilter fFailuresOnlyFilter = new FailuresOnlyFilter();
	private final IgnoredOnlyFilter fIgnoredOnlyFilter = new IgnoredOnlyFilter();

	private final TestRunnerViewPart fTestRunnerPart;
	private final Clipboard fClipboard;

	private PageBook fViewerbook;
	private TreeViewer fTreeViewer;
	private TestSessionTreeContentProvider fTreeContentProvider;
	private TestSessionLabelProvider fTreeLabelProvider;
	private TableViewer fTableViewer;
	private TestSessionLabelProvider fTableLabelProvider;
	private SelectionProviderMediator fSelectionProvider;

	private TestResultsLayout fLayoutMode;
	private boolean fTreeHasFilter;
	private boolean fTableHasFilter;

	private TestRunSession fTestRunSession;

	private boolean fTreeNeedsRefresh;
	private boolean fTableNeedsRefresh;
	private HashSet<ITestElement> fNeedUpdate;
	private ITestCaseElement fAutoScrollTarget;

	private LinkedList<ITestSuiteElement> fAutoClose;
	private HashSet<ITestSuiteElement> fAutoExpand;

	/**
	 * Constructs a Test Viewer object
	 *
	 * @param parent    a parent composite
	 * @param clipboard a {@link Clipboard} instance
	 * @param runner    a Test Runner view part
	 */
	public TestViewer(Composite parent, Clipboard clipboard, TestRunnerViewPart runner) {
		fTestRunnerPart = runner;
		fClipboard = clipboard;

		fLayoutMode = TestRunnerViewPart.TestResultsLayout.HIERARCHICAL;

		createTestViewers(parent);

		registerViewersRefresh();

		initContextMenu();
	}

	private void createTestViewers(Composite parent) {
		fViewerbook = new PageBook(parent, SWT.NULL);

		fTreeViewer = new TreeViewer(fViewerbook, SWT.V_SCROLL | SWT.SINGLE);
		fTreeViewer.setUseHashlookup(true);
		fTreeContentProvider = new TestSessionTreeContentProvider();
		fTreeViewer.setContentProvider(fTreeContentProvider);
		fTreeLabelProvider = new TestSessionLabelProvider(fTestRunnerPart,
				TestRunnerViewPart.TestResultsLayout.HIERARCHICAL);
//		fTreeViewer.setLabelProvider(new ColoringLabelProvider(fTreeLabelProvider));
		fTreeViewer.setLabelProvider(fTreeLabelProvider);

		fTableViewer = new TableViewer(fViewerbook, SWT.V_SCROLL | SWT.H_SCROLL | SWT.SINGLE);
		fTableViewer.setUseHashlookup(true);
		TestSessionTableContentProvider fTableContentProvider = new TestSessionTableContentProvider();
		fTableViewer.setContentProvider(fTableContentProvider);
		fTableLabelProvider = new TestSessionLabelProvider(fTestRunnerPart, TestRunnerViewPart.TestResultsLayout.FLAT);
//		fTableViewer.setLabelProvider(new ColoringLabelProvider(fTableLabelProvider));
		fTableViewer.setLabelProvider(fTableLabelProvider);

		fSelectionProvider = new SelectionProviderMediator(new StructuredViewer[] { fTreeViewer, fTableViewer },
				fTreeViewer);
		fSelectionProvider.addSelectionChangedListener(new TestSelectionListener());
		TestOpenListener testOpenListener = new TestOpenListener();
		fTreeViewer.getTree().addSelectionListener(testOpenListener);
		fTableViewer.getTable().addSelectionListener(testOpenListener);

		fTestRunnerPart.getSite().setSelectionProvider(fSelectionProvider);

		fViewerbook.showPage(fTreeViewer.getTree());
	}

	private void initContextMenu() {
		MenuManager menuMgr = new MenuManager("#PopupMenu"); //$NON-NLS-1$
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(this::handleMenuAboutToShow);
		fTestRunnerPart.getSite().registerContextMenu(menuMgr, fSelectionProvider);
		Menu menu = menuMgr.createContextMenu(fViewerbook);
		fTreeViewer.getTree().setMenu(menu);
		fTableViewer.getTable().setMenu(menu);
	}

	void handleMenuAboutToShow(IMenuManager manager) {
		IStructuredSelection selection = (IStructuredSelection) fSelectionProvider.getSelection();
		if (!selection.isEmpty()) {
			TestElement testElement = (TestElement) selection.getFirstElement();

			if (testElement instanceof TestSuiteElement) {
				TestSuiteElement testSuiteElement = (TestSuiteElement) testElement;
				IAction openTestAction = testSuiteElement.getTestRunSession().getTestViewSupport()
						.getOpenTestAction(fTestRunnerPart.getSite().getShell(), testSuiteElement);
				if (openTestAction != null) {
					manager.add(openTestAction);
				}
				manager.add(new Separator());
				if (!fTestRunnerPart.lastLaunchStillRunning()) {
					addRerunActions(manager, testSuiteElement);
				}
			} else {
				TestCaseElement testCaseElement = (TestCaseElement) testElement;
				IAction openTestAction = testElement.getTestRunSession().getTestViewSupport()
						.getOpenTestAction(fTestRunnerPart.getSite().getShell(), testCaseElement);
				if (openTestAction != null) {
					manager.add(openTestAction);
				}
				manager.add(new Separator());
				addRerunActions(manager, testCaseElement);
			}
			if (fLayoutMode == TestRunnerViewPart.TestResultsLayout.HIERARCHICAL) {
				manager.add(new Separator());
				manager.add(new ExpandAllAction());
				manager.add(new CollapseAllAction());
			}

		}
		if (fTestRunSession != null
				&& fTestRunSession.getCurrentFailureCount() + fTestRunSession.getCurrentErrorCount() > 0) {
			if (fLayoutMode != TestRunnerViewPart.TestResultsLayout.HIERARCHICAL) {
				manager.add(new Separator());
			}
			manager.add(new CopyFailureListAction(fTestRunnerPart, fClipboard));
		}
		manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
		manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS + "-end")); //$NON-NLS-1$
	}

	private void addRerunActions(IMenuManager manager, TestElement testCaseElement) {
		ILaunchConfiguration rerunLaunchConfiguration = testCaseElement.getTestRunSession().getTestViewSupport()
				.getRerunLaunchConfiguration(Collections.singletonList(testCaseElement));
		if (rerunLaunchConfiguration == null) {
			return;
		}
		if (fTestRunnerPart.lastLaunchStillRunning()) {
			manager.add(new RerunAction(rerunLaunchConfiguration, ILaunchManager.RUN_MODE));
		} else {
			try {
				rerunLaunchConfiguration.getType().getSupportedModeCombinations().stream() //
						.filter(modes -> modes.size() == 1) //
						.flatMap(Collection::stream) //
						.forEach(mode -> manager.add(new RerunAction(rerunLaunchConfiguration, mode)));
			} catch (CoreException e) {
				UnitTestPlugin.log(e);
			}
		}
	}

	/**
	 * Returns the tree viewer control
	 *
	 * @return tree viewer control object
	 */
	public Control getTestViewerControl() {
		return fViewerbook;
	}

	/**
	 * Registers a given active test session
	 *
	 * @param testRunSession a test session object
	 */
	public synchronized void registerActiveSession(TestRunSession testRunSession) {
		fTestRunSession = testRunSession;
		registerAutoScrollTarget(null);
		registerViewersRefresh();
	}

	void handleDefaultSelected() {
		IStructuredSelection selection = (IStructuredSelection) fSelectionProvider.getSelection();
		if (selection.size() != 1)
			return;

		TestElement testElement = (TestElement) selection.getFirstElement();
		IAction action;
		if (testElement instanceof ITestSuiteElement) {
			action = testElement.getTestRunSession().getTestViewSupport()
					.getOpenTestAction(fTestRunnerPart.getSite().getShell(), (ITestSuiteElement) testElement);
		} else if (testElement instanceof ITestCaseElement) {
			action = testElement.getTestRunSession().getTestViewSupport()
					.getOpenTestAction(fTestRunnerPart.getSite().getShell(), (ITestCaseElement) testElement);
		} else {
			throw new IllegalStateException(String.valueOf(testElement));
		}

		if (action != null && action.isEnabled())
			action.run();
	}

	/**
	 * Tunes the label providers to show time on the generated labels
	 *
	 * @param showTime <code>true</code> in case a time value is to be shown,
	 *                 otherwise - <code>false</code>
	 */
	public synchronized void setShowTime(boolean showTime) {
		try {
			fViewerbook.setRedraw(false);
			fTreeLabelProvider.setShowTime(showTime);
			fTableLabelProvider.setShowTime(showTime);
		} finally {
			fViewerbook.setRedraw(true);
		}
	}

	/**
	 * It makes sense to display either failed or ignored tests, not both together.
	 *
	 * @param failuresOnly whether to show only failed tests
	 * @param ignoredOnly  whether to show only skipped tests
	 * @param layoutMode   the layout mode
	 */
	public synchronized void setShowFailuresOrIgnoredOnly(boolean failuresOnly, boolean ignoredOnly,
			TestResultsLayout layoutMode) {
		/*
		 * Management of fTreeViewer and fTableViewer
		 * ****************************************** - invisible viewer is updated on
		 * registerViewerUpdate unless its f*NeedsRefresh is true - invisible viewer is
		 * not refreshed upfront - on layout change, new viewer is refreshed if
		 * necessary - filter only applies to "current" layout mode / viewer
		 */
		try {
			fViewerbook.setRedraw(false);

			IStructuredSelection selection = null;
			boolean switchLayout = layoutMode != fLayoutMode;
			if (switchLayout) {
				selection = (IStructuredSelection) fSelectionProvider.getSelection();
				if (layoutMode == TestRunnerViewPart.TestResultsLayout.HIERARCHICAL) {
					if (fTreeNeedsRefresh) {
						clearUpdateAndExpansion();
					}
				} else {
					if (fTableNeedsRefresh) {
						clearUpdateAndExpansion();
					}
				}
				fLayoutMode = layoutMode;
				fViewerbook.showPage(getActiveViewer().getControl());
			}
			// avoid realizing all TableItems, especially in flat mode!
			StructuredViewer viewer = getActiveViewer();
			if (failuresOnly || ignoredOnly) {
				if (getActiveViewerHasFilter()) {
					// For simplicity clear both filters (only one of them is used)
					viewer.removeFilter(fFailuresOnlyFilter);
					viewer.removeFilter(fIgnoredOnlyFilter);
				}
				setActiveViewerHasFilter(true);
				viewer.setInput(null);
				// Set either the failures or the skipped tests filter
				ViewerFilter filter = fFailuresOnlyFilter;
				if (ignoredOnly == true) {
					filter = fIgnoredOnlyFilter;
				}
				viewer.addFilter(filter);
				setActiveViewerNeedsRefresh(true);

			} else {
				if (getActiveViewerHasFilter()) {
					setActiveViewerNeedsRefresh(true);
					setActiveViewerHasFilter(false);
					viewer.setInput(null);
					viewer.removeFilter(fIgnoredOnlyFilter);
					viewer.removeFilter(fFailuresOnlyFilter);
				}
			}
			processChangesInUI();

			if (selection != null) {
				// workaround for https://bugs.eclipse.org/bugs/show_bug.cgi?id=125708
				// (ITreeSelection not adapted if TreePaths changed):
				StructuredSelection flatSelection = new StructuredSelection(selection.toList());
				fSelectionProvider.setSelection(flatSelection, true);
			}

		} finally {
			fViewerbook.setRedraw(true);
		}
	}

	private boolean getActiveViewerHasFilter() {
		if (fLayoutMode == TestRunnerViewPart.TestResultsLayout.HIERARCHICAL)
			return fTreeHasFilter;
		else
			return fTableHasFilter;
	}

	private void setActiveViewerHasFilter(boolean filter) {
		if (fLayoutMode == TestRunnerViewPart.TestResultsLayout.HIERARCHICAL)
			fTreeHasFilter = filter;
		else
			fTableHasFilter = filter;
	}

	private StructuredViewer getActiveViewer() {
		if (fLayoutMode == TestRunnerViewPart.TestResultsLayout.HIERARCHICAL)
			return fTreeViewer;
		else
			return fTableViewer;
	}

	private boolean getActiveViewerNeedsRefresh() {
		if (fLayoutMode == TestRunnerViewPart.TestResultsLayout.HIERARCHICAL)
			return fTreeNeedsRefresh;
		else
			return fTableNeedsRefresh;
	}

	private void setActiveViewerNeedsRefresh(boolean needsRefresh) {
		if (fLayoutMode == TestRunnerViewPart.TestResultsLayout.HIERARCHICAL)
			fTreeNeedsRefresh = needsRefresh;
		else
			fTableNeedsRefresh = needsRefresh;
	}

	/**
	 * To be called periodically by the TestRunnerViewPart (in the UI thread).
	 */
	public void processChangesInUI() {
		if (fTestRunSession == null) {
			registerViewersRefresh();
			fTreeNeedsRefresh = false;
			fTableNeedsRefresh = false;
			fTreeViewer.setInput(null);
			fTableViewer.setInput(null);
			return;
		}

		StructuredViewer viewer = getActiveViewer();
		if (getActiveViewerNeedsRefresh()) {
			clearUpdateAndExpansion();
			setActiveViewerNeedsRefresh(false);
			viewer.setInput(fTestRunnerPart);
		} else {
			Object[] toUpdate;
			synchronized (this) {
				toUpdate = fNeedUpdate.toArray();
				fNeedUpdate.clear();
			}
			if (!fTreeNeedsRefresh && toUpdate.length > 0) {
				if (fTreeHasFilter)
					for (Object element : toUpdate)
						updateElementInTree((TestElement) element);
				else {
					HashSet<Object> toUpdateWithParents = new HashSet<>();
					toUpdateWithParents.addAll(Arrays.asList(toUpdate));
					for (Object element : toUpdate) {
						ITestElement parent = ((ITestElement) element).getParent();
						while (parent != null) {
							toUpdateWithParents.add(parent);
							parent = parent.getParent();
						}
					}
					fTreeViewer.update(toUpdateWithParents.toArray(), null);
				}
			}
			if (!fTableNeedsRefresh && toUpdate.length > 0) {
				if (fTableHasFilter)
					for (Object element : toUpdate)
						updateElementInTable((TestElement) element);
				else
					fTableViewer.update(toUpdate, null);
			}
		}
		autoScrollInUI();
	}

	private void updateElementInTree(final TestElement testElement) {
		if (isShown(testElement)) {
			updateShownElementInTree(testElement);
		} else {
			TestElement current = testElement;
			do {
				if (fTreeViewer.testFindItem(current) != null)
					fTreeViewer.remove(current);
				current = current.getParent();
			} while (!(current instanceof ITestRunSession) && !isShown(current));

			while (current != null && !(current instanceof ITestRunSession)) {
				fTreeViewer.update(current, null);
				current = current.getParent();
			}
		}
	}

	private void updateShownElementInTree(ITestElement testElement) {
		if (testElement == null || testElement instanceof ITestRunSession) // paranoia null check
			return;

		ITestSuiteElement parent = testElement.getParent();
		updateShownElementInTree(parent); // make sure parent is shown and up-to-date

		if (fTreeViewer.testFindItem(testElement) == null) {
			fTreeViewer.add(parent, testElement); // if not yet in tree: add
		} else {
			fTreeViewer.update(testElement, null); // if in tree: update
		}
	}

	private void updateElementInTable(TestElement element) {
		if (isShown(element)) {
			if (fTableViewer.testFindItem(element) == null) {
				TestElement previous = getNextFailure(element, false);
				int insertionIndex = -1;
				if (previous != null) {
					TableItem item = (TableItem) fTableViewer.testFindItem(previous);
					if (item != null)
						insertionIndex = fTableViewer.getTable().indexOf(item);
				}
				fTableViewer.insert(element, insertionIndex);
			} else {
				fTableViewer.update(element, null);
			}
		} else {
			fTableViewer.remove(element);
		}
	}

	private boolean isShown(TestElement current) {
		return fFailuresOnlyFilter.select(current);
	}

	private void autoScrollInUI() {
		if (!fTestRunnerPart.isAutoScroll()) {
			clearAutoExpand();
			fAutoClose.clear();
			return;
		}

		if (fLayoutMode == TestRunnerViewPart.TestResultsLayout.FLAT) {
			if (fAutoScrollTarget != null)
				fTableViewer.reveal(fAutoScrollTarget);
			return;
		}

		synchronized (this) {
			for (ITestSuiteElement suite : fAutoExpand) {
				fTreeViewer.setExpandedState(suite, true);
			}
			clearAutoExpand();
		}

		ITestCaseElement current = fAutoScrollTarget;
		fAutoScrollTarget = null;

		ITestSuiteElement parent = current == null ? null : (ITestSuiteElement) fTreeContentProvider.getParent(current);
		if (fAutoClose.isEmpty() || !fAutoClose.getLast().equals(parent)) {
			// we're in a new branch, so let's close old OK branches:
			for (ListIterator<ITestSuiteElement> iter = fAutoClose.listIterator(fAutoClose.size()); iter
					.hasPrevious();) {
				ITestSuiteElement previousAutoOpened = iter.previous();
				if (previousAutoOpened.equals(parent))
					break;

				if (((TestElement) previousAutoOpened).getStatus() == Status.OK) {
					// auto-opened the element, and all children are OK -> auto close
					iter.remove();
					fTreeViewer.collapseToLevel(previousAutoOpened, AbstractTreeViewer.ALL_LEVELS);
				}
			}

			while (parent != null && !fTestRunSession.equals(parent) && fTreeViewer.getExpandedState(parent) == false) {
				fAutoClose.add(parent); // add to auto-opened elements -> close later if STATUS_OK
				parent = (ITestSuiteElement) fTreeContentProvider.getParent(parent);
			}
		}
		if (current != null)
			fTreeViewer.reveal(current);
	}

	/**
	 * Selects the next failure test element
	 */
	public void selectFirstFailure() {
		ITestElement firstFailure = getNextChildFailure(fTestRunSession, true);
		if (firstFailure != null)
			getActiveViewer().setSelection(new StructuredSelection(firstFailure), true);
	}

	/**
	 * Selects a next failure test element
	 *
	 * @param showNext <code>true</code> if a next failed element is to be shown,
	 *                 otherwise - <code>false</code>
	 */
	public void selectFailure(boolean showNext) {
		IStructuredSelection selection = (IStructuredSelection) getActiveViewer().getSelection();
		TestElement selected = (TestElement) selection.getFirstElement();
		ITestElement next;

		if (selected == null) {
			next = getNextChildFailure(fTestRunSession, showNext);
		} else {
			next = getNextFailure(selected, showNext);
		}

		if (next != null)
			getActiveViewer().setSelection(new StructuredSelection(next), true);
	}

	private TestElement getNextFailure(TestElement selected, boolean showNext) {
		if (selected instanceof TestSuiteElement) {
			TestElement nextChild = getNextChildFailure((TestSuiteElement) selected, showNext);
			if (nextChild != null)
				return nextChild;
		}
		return getNextFailureSibling(selected, showNext);
	}

	private TestElement getNextFailureSibling(TestElement current, boolean showNext) {
		TestSuiteElement parent = current.getParent();
		if (parent == null)
			return null;

		List<TestElement> siblings = getSortedChildren(parent);
		if (!showNext)
			siblings = new ReverseList<>(siblings);

		int nextIndex = siblings.indexOf(current) + 1;
		for (int i = nextIndex; i < siblings.size(); i++) {
			TestElement sibling = siblings.get(i);
			if (sibling.getStatus().isErrorOrFailure()) {
				if (sibling instanceof ITestCaseElement) {
					return sibling;
				} else {
					TestSuiteElement testSuiteElement = (TestSuiteElement) sibling;
					if (testSuiteElement.getChildren().isEmpty()) {
						return testSuiteElement;
					}
					return getNextChildFailure(testSuiteElement, showNext);
				}
			}
		}
		return getNextFailureSibling(parent, showNext);
	}

	private TestElement getNextChildFailure(TestSuiteElement root, boolean showNext) {
		List<TestElement> children = getSortedChildren(root);
		if (!showNext)
			children = new ReverseList<>(children);
		for (TestElement child : children) {
			if (child.getStatus().isErrorOrFailure()) {
				if (child instanceof ITestCaseElement) {
					return child;
				} else {
					TestSuiteElement testSuiteElement = (TestSuiteElement) child;
					if (testSuiteElement.getChildren().isEmpty()) {
						return testSuiteElement;
					}
					return getNextChildFailure(testSuiteElement, showNext);
				}
			}
		}
		return null;
	}

	private List<TestElement> getSortedChildren(TestSuiteElement parent) {
		List<TestElement> siblings = new ArrayList<>(parent.getChildren());
		ViewerComparator comparator = fTreeViewer.getComparator();
		if (comparator != null) {
			siblings.sort((e1, e2) -> comparator.compare(fTreeViewer, e1, e2));
		}
		return siblings;
	}

	/**
	 * Initializes a viewers refresh
	 */
	public synchronized void registerViewersRefresh() {
		fTreeNeedsRefresh = true;
		fTableNeedsRefresh = true;
		clearUpdateAndExpansion();
	}

	private void clearUpdateAndExpansion() {
		fNeedUpdate = new LinkedHashSet<>();
		fAutoClose = new LinkedList<>();
		fAutoExpand = new HashSet<>();
	}

	/**
	 * Registers a test element
	 *
	 * @param testElement the added test
	 */
	public synchronized void registerTestAdded(ITestElement testElement) {
		// TODO: performance: would only need to refresh parent of added element
		fTreeNeedsRefresh = true;
		fTableNeedsRefresh = true;
	}

	/**
	 * Initializes an update for a test element
	 *
	 * @param testElement a test element that needs to be updated
	 */
	public synchronized void registerViewerUpdate(final ITestElement testElement) {
		fNeedUpdate.add(testElement);
	}

	private synchronized void clearAutoExpand() {
		fAutoExpand.clear();
	}

	/**
	 * Registers an auto-scroll target test case element
	 *
	 * @param testCaseElement a test case element
	 */
	public void registerAutoScrollTarget(ITestCaseElement testCaseElement) {
		fAutoScrollTarget = testCaseElement;
	}

	/**
	 * Registers a failed test element for an auto-scroll
	 *
	 * @param testElement a failed test element
	 */
	public synchronized void registerFailedForAutoScroll(ITestElement testElement) {
		ITestSuiteElement parent = (TestSuiteElement) fTreeContentProvider.getParent(testElement);
		if (parent != null)
			fAutoExpand.add(parent);
	}

	/**
	 * Expands the test element tree first level
	 */
	public void expandFirstLevel() {
		fTreeViewer.expandToLevel(2);
	}

	/**
	 * Sets up an alphabetical sort flag
	 *
	 * @param enableAlphabeticalSort <code>true</code> if an alphabetical sort is
	 *                               enabled, otherwise <code>false</code>
	 */
	public void setAlphabeticalSort(boolean enableAlphabeticalSort) {
		fTreeViewer.setComparator(enableAlphabeticalSort ? TEST_ELEMENT_ALPHABETIC_ORDER : null);
		fTreeViewer.refresh();
	}

	/**
	 * Indicates if an alphabetical sort is enabled
	 *
	 * @return <code>true</code> if an alphabetical sort is enabled, otherwise
	 *         <code>false</code>
	 */
	public boolean isAlphabeticalSort() {
		return fTreeViewer.getComparator() == TEST_ELEMENT_ALPHABETIC_ORDER;
	}
}
