/*******************************************************************************
 * Copyright (c) 2026 Vogella GmbH and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Vogella GmbH - initial API and implementation
 *******************************************************************************/
package org.eclipse.ui.internal.buildmonitor;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.PerformanceStats;
import org.eclipse.core.runtime.ServiceCaller;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.commands.ActionHandler;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.osgi.service.debug.DebugOptions;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.part.ViewPart;

/**
 * Shows what each build cost, grouped either by project or by builder. Builder
 * tracing is switched on while the view is open and the previous debug options
 * are put back when it closes.
 */
public class BuildMonitorView extends ViewPart {

	/** Performance event recorded once per builder run by org.eclipse.core.resources. */
	private static final String EVENT_BUILDERS = "org.eclipse.core.resources/perf/builders"; //$NON-NLS-1$

	private static final String OPTION_PERF = "org.eclipse.core.runtime/perf"; //$NON-NLS-1$
	private static final String OPTION_PERF_SUCCESS = "org.eclipse.core.runtime/perf/success"; //$NON-NLS-1$

	/**
	 * The options this view turns on, global flag first. A builder threshold of 0
	 * records every run without writing any of them to the performance log, and
	 * perf/success is off because only the per-project counters are read.
	 */
	private static final List<String> OPTION_ORDER = List.of(OPTION_PERF, OPTION_PERF_SUCCESS, EVENT_BUILDERS);
	private static final Map<String, String> OPTION_VALUES = Map.of(OPTION_PERF, "true", //$NON-NLS-1$
			OPTION_PERF_SUCCESS, "false", //$NON-NLS-1$
			EVENT_BUILDERS, "0"); //$NON-NLS-1$

	/**
	 * Bounds on what is kept. One build of a large workspace is thousands of rows
	 * and one of a single project is a handful, so the row budget is what actually
	 * bounds the memory and the build count only keeps the top level readable.
	 */
	private static final int MAX_BUILDS = 25;
	private static final int MAX_ROWS = 50_000;

	private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss"); //$NON-NLS-1$

	private static final int COLUMN_NAME = 0;
	private static final int COLUMN_RUNS = 1;
	private static final int COLUMN_TIME = 2;
	private static final int COLUMN_SHARE = 3;
	private static final int COLUMN_WALL = 4;

	private final List<BuildRun> builds = new ArrayList<>();
	private final Map<String, String> replacedOptions = new LinkedHashMap<>();
	private boolean tracing;
	private boolean debugWasEnabled;
	private boolean groupedByBuilder;

	private TreeViewer viewer;
	private TreeColumn nameColumn;
	private final BuildComparator comparator = new BuildComparator();

	/** Counters at the moment the running build started, null while none runs. */
	private Map<Key, long[]> buildStartCounters;
	private long buildStartNanos;
	private String buildLabel;
	private LocalTime buildStarted;
	private int buildDepth;

	private final IResourceChangeListener buildListener = event -> {
		if (event.getType() == IResourceChangeEvent.PRE_BUILD) {
			buildStarted(event);
		} else {
			buildFinished();
		}
	};

	@Override
	public void createPartControl(Composite parent) {
		viewer = new TreeViewer(parent, SWT.FULL_SELECTION | SWT.H_SCROLL | SWT.V_SCROLL);
		Tree tree = viewer.getTree();
		tree.setHeaderVisible(true);
		tree.setLinesVisible(true);

		nameColumn = addColumn(columnHeader(), null, 420, SWT.LEFT, COLUMN_NAME, this::name);
		addColumn(Messages.BuildMonitorView_runs_column, Messages.BuildMonitorView_runs_column_tooltip, 70, SWT.RIGHT,
				COLUMN_RUNS, this::runs);
		addColumn(Messages.BuildMonitorView_time_column, Messages.BuildMonitorView_time_column_tooltip, 130, SWT.RIGHT,
				COLUMN_TIME, this::time);
		addColumn(Messages.BuildMonitorView_share_column, Messages.BuildMonitorView_share_column_tooltip, 80, SWT.RIGHT,
				COLUMN_SHARE, this::share);
		addColumn(Messages.BuildMonitorView_wall_column, Messages.BuildMonitorView_wall_column_tooltip, 160, SWT.RIGHT,
				COLUMN_WALL, this::wall);

		viewer.setContentProvider(new BuildContentProvider());
		viewer.setComparator(comparator);
		viewer.setInput(builds);

		createContextMenu();
		createToolBar();

		tracing = enableTracing();
		if (tracing) {
			ResourcesPlugin.getWorkspace().addResourceChangeListener(buildListener,
					IResourceChangeEvent.PRE_BUILD | IResourceChangeEvent.POST_BUILD);
		}
		updateDescription();
	}

	private void createToolBar() {
		IToolBarManager toolBar = getViewSite().getActionBars().getToolBarManager();

		Action byBuilder = new Action(Messages.BuildMonitorView_by_builder_action, IAction.AS_CHECK_BOX) {
			@Override
			public void run() {
				groupedByBuilder = isChecked();
				nameColumn.setText(columnHeader());
				withoutRedraw(viewer::refresh);
			}
		};
		byBuilder.setToolTipText(Messages.BuildMonitorView_by_builder_action_tooltip);
		toolBar.add(byBuilder);
		// also in the view menu, where Eclipse puts a grouping switch
		getViewSite().getActionBars().getMenuManager().add(byBuilder);
		getViewSite().getActionBars().getMenuManager().add(copyAction());
		toolBar.add(new Separator());
		toolBar.add(action(Messages.BuildMonitorView_expand_all_action, Messages.BuildMonitorView_expand_all_action,
				ISharedImages.IMG_ELCL_EXPANDALL, () -> withoutRedraw(viewer::expandAll)));
		toolBar.add(action(Messages.BuildMonitorView_collapse_all_action, Messages.BuildMonitorView_collapse_all_action,
				ISharedImages.IMG_ELCL_COLLAPSEALL, () -> withoutRedraw(viewer::collapseAll)));
		toolBar.add(new Separator());
		toolBar.add(action(Messages.BuildMonitorView_clear_action, Messages.BuildMonitorView_clear_action_tooltip,
				ISharedImages.IMG_ETOOL_CLEAR, () -> {
					builds.clear();
					viewer.refresh();
					updateDescription();
				}));
	}

	@Override
	public void dispose() {
		ResourcesPlugin.getWorkspace().removeResourceChangeListener(buildListener);
		disableTracing();
		super.dispose();
	}

	@Override
	public void setFocus() {
		viewer.getControl().setFocus();
	}

	private void createContextMenu() {
		Action copy = copyAction();
		MenuManager menuManager = new MenuManager();
		menuManager.add(copy);
		Menu menu = menuManager.createContextMenu(viewer.getTree());
		viewer.getTree().setMenu(menu);

		// Ctrl+C while the tree has focus. The retargetable action alone is not
		// enough, the copy command is answered by another handler otherwise.
		getViewSite().getActionBars().setGlobalActionHandler(ActionFactory.COPY.getId(), copy);
		getViewSite().getActionBars().updateActionBars();
		IHandlerService handlers = getSite().getService(IHandlerService.class);
		if (handlers != null) {
			handlers.activateHandler(IWorkbenchCommandConstants.EDIT_COPY, new ActionHandler(copy));
		}
	}

	private Action copyAction() {
		Action copy = new Action(Messages.BuildMonitorView_copy_action) {
			@Override
			public void run() {
				copyToClipboard();
			}
		};
		copy.setToolTipText(Messages.BuildMonitorView_copy_action_tooltip);
		return copy;
	}

	/** Copies the selected rows, or everything when nothing is selected. */
	private void copyToClipboard() {
		List<Object> rows = new ArrayList<>();
		if (viewer.getSelection() instanceof IStructuredSelection selection && !selection.isEmpty()) {
			selection.forEach(rows::add);
		} else {
			for (BuildRun build : builds) {
				rows.add(build);
				for (GroupEntry group : build.getGroups(groupedByBuilder)) {
					rows.add(group);
					rows.addAll(group.getChildren());
				}
			}
		}
		if (rows.isEmpty()) {
			return;
		}
		StringBuilder text = new StringBuilder(Messages.BuildMonitorView_copy_header);
		rows.forEach(row -> appendRow(text, row));
		Clipboard clipboard = new Clipboard(viewer.getTree().getDisplay());
		try {
			clipboard.setContents(new Object[] { text.toString() },
					new Transfer[] { TextTransfer.getInstance() });
		} finally {
			clipboard.dispose();
		}
	}

	/**
	 * One tab separated line per row. Every row carries its build, project and
	 * builder, so a line stays meaningful once it is out of the tree.
	 */
	private void appendRow(StringBuilder text, Object element) {
		String build = ""; //$NON-NLS-1$
		String started = ""; //$NON-NLS-1$
		String project = ""; //$NON-NLS-1$
		String builder = ""; //$NON-NLS-1$
		if (element instanceof BuildRun run) {
			build = run.getLabel();
			started = TIME.format(run.getStarted());
		} else if (element instanceof GroupEntry group) {
			if (group.isGroupedByBuilder()) {
				builder = group.getName();
			} else {
				project = group.getName();
			}
		} else if (element instanceof BuilderEntry leaf) {
			project = leaf.getProject();
			builder = BuilderLabels.of(leaf.getBuilder());
		}
		text.append(System.lineSeparator());
		text.append(clean(build)).append('\t').append(started).append('\t');
		text.append(clean(project)).append('\t').append(clean(builder)).append('\t');
		text.append(runsOf(element)).append('\t').append(timeOf(element)).append('\t');
		text.append(String.format("%.1f", Double.valueOf(shareOf(element)))).append('\t'); //$NON-NLS-1$
		text.append(element instanceof BuildRun run ? Long.toString(run.getWallTime()) : ""); //$NON-NLS-1$
	}

	private static String clean(String value) {
		return value.replace('\t', ' ').replace('\n', ' ');
	}

	private String columnHeader() {
		return groupedByBuilder ? Messages.BuildMonitorView_builder_column : Messages.BuildMonitorView_project_column;
	}

	private static Action action(String text, String tooltip, String sharedImage, Runnable body) {
		Action action = new Action(text) {
			@Override
			public void run() {
				body.run();
			}
		};
		action.setToolTipText(tooltip);
		action.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(sharedImage));
		return action;
	}

	/**
	 * Expanding a build of a large workspace creates thousands of rows, so the tree
	 * is only painted once the whole operation is done.
	 */
	private void withoutRedraw(Runnable body) {
		Tree tree = viewer.getTree();
		tree.setRedraw(false);
		try {
			BusyIndicator.showWhile(tree.getDisplay(), body);
		} finally {
			tree.setRedraw(true);
		}
	}

	private TreeColumn addColumn(String header, String tooltip, int width, int alignment, int index,
			Function<Object, String> text) {
		TreeViewerColumn column = new TreeViewerColumn(viewer, alignment);
		TreeColumn treeColumn = column.getColumn();
		treeColumn.setText(header);
		treeColumn.setToolTipText(tooltip);
		treeColumn.setWidth(width);
		treeColumn.addListener(SWT.Selection, e -> sortBy(index, treeColumn));
		column.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				return text.apply(element);
			}
		});
		return treeColumn;
	}

	/** Clicking the sorted column flips the direction, another column starts at descending. */
	private void sortBy(int column, TreeColumn treeColumn) {
		if (comparator.column == column) {
			comparator.ascending = !comparator.ascending;
		} else {
			comparator.column = column;
			comparator.ascending = column == COLUMN_NAME;
		}
		Tree tree = viewer.getTree();
		tree.setSortColumn(treeColumn);
		tree.setSortDirection(comparator.ascending ? SWT.UP : SWT.DOWN);
		withoutRedraw(viewer::refresh);
	}

	private String name(Object element) {
		if (element instanceof BuildRun build) {
			String pattern = build.getProjects() == 1 ? Messages.BuildMonitorView_build_label_one
					: Messages.BuildMonitorView_build_label;
			return NLS.bind(pattern, new Object[] { build.getLabel(), TIME.format(build.getStarted()),
					Integer.valueOf(build.getProjects()) });
		}
		if (element instanceof GroupEntry group) {
			return group.getName();
		}
		return ((BuilderEntry) element).getLabel(groupedByBuilder);
	}

	private String runs(Object element) {
		return Integer.toString(runsOf(element));
	}

	private String time(Object element) {
		return Long.toString(timeOf(element));
	}

	/**
	 * On a build, how much of it was builders at all; below it, the row's share of
	 * that build's builder time.
	 */
	private String share(Object element) {
		return String.format("%.1f%%", Double.valueOf(shareOf(element))); //$NON-NLS-1$
	}

	private static double shareOf(Object element) {
		if (element instanceof BuildRun build) {
			return build.getShare();
		}
		if (element instanceof GroupEntry group) {
			return group.getShare();
		}
		return ((BuilderEntry) element).getShare();
	}

	/** Only a build has a wall time; below it the builder times are the whole story. */
	private String wall(Object element) {
		return element instanceof BuildRun build ? Long.toString(build.getWallTime()) : ""; //$NON-NLS-1$
	}

	private static int runsOf(Object element) {
		if (element instanceof BuildRun build) {
			return build.getRuns();
		}
		if (element instanceof GroupEntry group) {
			return group.getRuns();
		}
		return ((BuilderEntry) element).getRuns();
	}

	private static long timeOf(Object element) {
		if (element instanceof BuildRun build) {
			return build.getTime();
		}
		if (element instanceof GroupEntry group) {
			return group.getTime();
		}
		return ((BuilderEntry) element).getTime();
	}

	private void buildStarted(IResourceChangeEvent event) {
		if (buildDepth++ > 0) {
			// a builder that builds another project nests, and the outer build owns the time
			return;
		}
		buildStartCounters = counters();
		buildStartNanos = System.nanoTime();
		buildStarted = LocalTime.now();
		buildLabel = label(event);
	}

	private void buildFinished() {
		if (buildDepth > 0) {
			buildDepth--;
		}
		if (buildDepth > 0 || buildStartCounters == null) {
			return;
		}
		long wallTime = (System.nanoTime() - buildStartNanos) / 1_000_000L;
		List<BuilderEntry> leaves = since(buildStartCounters);
		buildStartCounters = null;
		if (leaves.isEmpty()) {
			// a build that ran no builder at all is noise rather than a measurement
			return;
		}
		BuildRun build = new BuildRun(buildLabel, buildStarted, wallTime, leaves);
		Tree tree = viewer.getTree();
		if (tree.isDisposed()) {
			return;
		}
		tree.getDisplay().asyncExec(() -> add(build));
	}

	private void add(BuildRun build) {
		if (viewer.getTree().isDisposed()) {
			return;
		}
		builds.add(0, build);
		trim();
		viewer.refresh();
		viewer.expandToLevel(build, 1);
		viewer.reveal(build);
		updateDescription();
	}

	/** Drops the oldest builds until both bounds hold, always keeping the newest. */
	private void trim() {
		int rows = 0;
		for (BuildRun build : builds) {
			rows += build.getLeafCount();
		}
		while (builds.size() > 1 && (builds.size() > MAX_BUILDS || rows > MAX_ROWS)) {
			rows -= builds.remove(builds.size() - 1).getLeafCount();
		}
	}

	/**
	 * The accumulated builder counters, keyed by builder and project.
	 * {@link PerformanceStats} updates these while the builder returns, so they are
	 * complete by the time the build ends, unlike the batched listener callbacks.
	 */
	private static Map<Key, long[]> counters() {
		Map<Key, long[]> counters = new HashMap<>();
		for (PerformanceStats stats : PerformanceStats.getAllStats()) {
			if (EVENT_BUILDERS.equals(stats.getEvent()) && stats.getContext() != null) {
				counters.put(new Key(stats.getBlameString(), stats.getContext()),
						new long[] { stats.getRunCount(), stats.getRunningTime() });
			}
		}
		return counters;
	}

	/** What the counters gained since the given snapshot. */
	private static List<BuilderEntry> since(Map<Key, long[]> before) {
		List<BuilderEntry> leaves = new ArrayList<>();
		counters().forEach((key, now) -> {
			long[] then = before.getOrDefault(key, new long[] { 0, 0 });
			int runs = (int) (now[0] - then[0]);
			if (runs > 0) {
				leaves.add(new BuilderEntry(key.builder(), key.project(), runs, Math.max(now[1] - then[1], 0)));
			}
		});
		return leaves;
	}

	/**
	 * A builder on a project. The two strings belong to the shared
	 * {@link PerformanceStats} objects, so every recorded build refers to the same
	 * names rather than to copies of them.
	 */
	private record Key(String builder, String project) {
	}

	private static String label(IResourceChangeEvent event) {
		String kind = switch (event.getBuildKind()) {
			case IncrementalProjectBuilder.FULL_BUILD -> Messages.BuildMonitorView_kind_full;
			case IncrementalProjectBuilder.CLEAN_BUILD -> Messages.BuildMonitorView_kind_clean;
			case IncrementalProjectBuilder.AUTO_BUILD -> Messages.BuildMonitorView_kind_auto;
			default -> Messages.BuildMonitorView_kind_incremental;
		};
		if (event.getSource() instanceof IProject project) {
			return NLS.bind(Messages.BuildMonitorView_scope_project, kind, project.getName());
		}
		return NLS.bind(Messages.BuildMonitorView_scope_workspace, kind);
	}

	private void updateDescription() {
		if (!tracing) {
			setContentDescription(Messages.BuildMonitorView_tracing_unavailable);
		} else if (builds.isEmpty()) {
			setContentDescription(Messages.BuildMonitorView_waiting);
		} else {
			setContentDescription(NLS.bind(Messages.BuildMonitorView_recorded, Integer.valueOf(builds.size())));
		}
	}

	/**
	 * Turns builder tracing on, remembering the previous state so that
	 * {@link #disableTracing()} can put it back. Returns whether the debug options
	 * service was available.
	 */
	private boolean enableTracing() {
		return ServiceCaller.callOnce(getClass(), DebugOptions.class, options -> {
			debugWasEnabled = options.isDebugEnabled();
			for (String option : OPTION_ORDER) {
				replacedOptions.put(option, options.getOption(option));
			}
			options.setDebugEnabled(true);
			for (String option : OPTION_ORDER) {
				options.setOption(option, OPTION_VALUES.get(option));
			}
		});
	}

	private void disableTracing() {
		if (!tracing) {
			// nothing was changed, so there is nothing to put back
			return;
		}
		tracing = false;
		ServiceCaller.callOnce(getClass(), DebugOptions.class, options -> {
			// Put the options back before disabling. Equinox stashes whatever is set when
			// debugging goes off and hands it out again the next time it goes on, so
			// leaving ours in place would resurrect them.
			replacedOptions.forEach((option, value) -> {
				if (value == null) {
					options.removeOption(option);
				} else {
					options.setOption(option, value);
				}
			});
			if (!debugWasEnabled) {
				options.setDebugEnabled(false);
			}
		});
	}

	/**
	 * Sorts by the column whose header was clicked. Without a chosen column the
	 * builds stay newest first and everything below them slowest first.
	 */
	private final class BuildComparator extends ViewerComparator {

		private int column = -1;
		private boolean ascending;

		@Override
		public int compare(Viewer v, Object e1, Object e2) {
			if (column < 0) {
				return e1 instanceof BuildRun ? 0 : Long.compare(timeOf(e2), timeOf(e1));
			}
			int result = switch (column) {
				case COLUMN_RUNS -> Integer.compare(runsOf(e1), runsOf(e2));
				case COLUMN_TIME -> Long.compare(timeOf(e1), timeOf(e2));
				case COLUMN_SHARE -> Double.compare(shareOf(e1), shareOf(e2));
				case COLUMN_WALL -> Long.compare(wallOf(e1), wallOf(e2));
				default -> name(e1).compareToIgnoreCase(name(e2));
			};
			return ascending ? result : -result;
		}

		private long wallOf(Object element) {
			return element instanceof BuildRun build ? build.getWallTime() : 0;
		}
	}

	private final class BuildContentProvider implements ITreeContentProvider {

		@Override
		public Object[] getElements(Object input) {
			return ((List<?>) input).toArray();
		}

		@Override
		public Object[] getChildren(Object element) {
			if (element instanceof BuildRun build) {
				return build.getGroups(groupedByBuilder).toArray();
			}
			if (element instanceof GroupEntry group) {
				return group.getChildren().toArray();
			}
			return new Object[0];
		}

		@Override
		public boolean hasChildren(Object element) {
			return element instanceof BuildRun || element instanceof GroupEntry;
		}

		@Override
		public Object getParent(Object element) {
			return null;
		}
	}
}
