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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.eclipse.core.runtime.PerformanceStats;
import org.eclipse.core.runtime.PerformanceStats.PerformanceListener;
import org.eclipse.core.runtime.ServiceCaller;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.osgi.service.debug.DebugOptions;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.part.ViewPart;

/**
 * Shows how much time each builder spends on each project. Builder tracing is
 * switched on while the view is open and the previous debug options are put back
 * when it closes.
 */
public class BuildMonitorView extends ViewPart {

	/** Performance event fired once per builder run by org.eclipse.core.resources. */
	private static final String EVENT_BUILDERS = "org.eclipse.core.resources/perf/builders"; //$NON-NLS-1$

	private static final String OPTION_PERF = "org.eclipse.core.runtime/perf"; //$NON-NLS-1$
	private static final String OPTION_PERF_SUCCESS = "org.eclipse.core.runtime/perf/success"; //$NON-NLS-1$

	/**
	 * The options this view turns on, global flag first. A builder threshold of 0
	 * reports every run to listeners without writing any of them to the log.
	 */
	private static final Map<String, String> TRACING_OPTIONS = new LinkedHashMap<>();
	static {
		TRACING_OPTIONS.put(OPTION_PERF, "true"); //$NON-NLS-1$
		TRACING_OPTIONS.put(OPTION_PERF_SUCCESS, "true"); //$NON-NLS-1$
		TRACING_OPTIONS.put(EVENT_BUILDERS, "0"); //$NON-NLS-1$
	}

	private final Map<String, BuilderTiming> timings = new LinkedHashMap<>();
	private final Map<String, String> replacedOptions = new LinkedHashMap<>();
	private boolean debugWasEnabled;

	private TableViewer viewer;

	private final PerformanceListener performanceListener = new PerformanceListener() {
		@Override
		public void eventFailed(PerformanceStats event, long duration) {
			// unlike eventsOccurred, these events carry the project as their context
			if (EVENT_BUILDERS.equals(event.getEvent())) {
				record(event.getBlameString(), event.getContext(), duration);
			}
		}
	};

	@Override
	public void createPartControl(Composite parent) {
		viewer = new TableViewer(parent, SWT.FULL_SELECTION | SWT.H_SCROLL | SWT.V_SCROLL);
		Table table = viewer.getTable();
		table.setHeaderVisible(true);
		table.setLinesVisible(true);

		addColumn(Messages.BuildMonitorView_builder_column, 260, BuilderTiming::getBuilder);
		addColumn(Messages.BuildMonitorView_project_column, 160, BuilderTiming::getProject);
		addColumn(Messages.BuildMonitorView_runs_column, 60, t -> Integer.toString(t.getRuns()));
		addColumn(Messages.BuildMonitorView_total_time_column, 90, t -> Long.toString(t.getTotalTime()));
		addColumn(Messages.BuildMonitorView_slowest_run_column, 90, t -> Long.toString(t.getSlowestRun()));

		viewer.setContentProvider(ArrayContentProvider.getInstance());
		viewer.setComparator(new ViewerComparator() {
			@Override
			public int compare(Viewer v, Object e1, Object e2) {
				return Long.compare(((BuilderTiming) e2).getTotalTime(), ((BuilderTiming) e1).getTotalTime());
			}
		});
		viewer.setInput(new BuilderTiming[0]);

		Action clear = new Action(Messages.BuildMonitorView_clear_action) {
			@Override
			public void run() {
				synchronized (timings) {
					timings.clear();
				}
				refresh();
			}
		};
		clear.setToolTipText(Messages.BuildMonitorView_clear_action_tooltip);
		getViewSite().getActionBars().getToolBarManager().add(clear);

		if (enableTracing()) {
			PerformanceStats.addListener(performanceListener);
			setContentDescription(Messages.BuildMonitorView_tracing_enabled);
		} else {
			setContentDescription(Messages.BuildMonitorView_tracing_unavailable);
		}
	}

	@Override
	public void dispose() {
		PerformanceStats.removeListener(performanceListener);
		disableTracing();
		super.dispose();
	}

	@Override
	public void setFocus() {
		viewer.getControl().setFocus();
	}

	private void addColumn(String header, int width, Function<BuilderTiming, String> text) {
		TableViewerColumn column = new TableViewerColumn(viewer, SWT.NONE);
		column.getColumn().setText(header);
		column.getColumn().setWidth(width);
		column.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				return text.apply((BuilderTiming) element);
			}
		});
	}

	/**
	 * Turns builder tracing on, remembering the previous state so that
	 * {@link #disableTracing()} can put it back. Returns whether the debug options
	 * service was available.
	 */
	private boolean enableTracing() {
		return ServiceCaller.callOnce(getClass(), DebugOptions.class, options -> {
			debugWasEnabled = options.isDebugEnabled();
			for (String option : TRACING_OPTIONS.keySet()) {
				replacedOptions.put(option, options.getOption(option));
			}
			options.setDebugEnabled(true);
			TRACING_OPTIONS.forEach(options::setOption);
		});
	}

	private void disableTracing() {
		ServiceCaller.callOnce(getClass(), DebugOptions.class, options -> {
			if (!debugWasEnabled) {
				// disabling debug discards all options, including the ones set above
				options.setDebugEnabled(false);
				return;
			}
			replacedOptions.forEach((option, value) -> {
				if (value == null) {
					options.removeOption(option);
				} else {
					options.setOption(option, value);
				}
			});
		});
	}

	/**
	 * Called from the performance stats job, not the user interface thread.
	 */
	private void record(String builder, String project, long duration) {
		String key = builder + '/' + project;
		synchronized (timings) {
			timings.computeIfAbsent(key, k -> new BuilderTiming(builder, project)).addRun(duration);
		}
		refresh();
	}

	private void refresh() {
		Table table = viewer.getTable();
		if (table.isDisposed()) {
			return;
		}
		table.getDisplay().asyncExec(() -> {
			if (table.isDisposed()) {
				return;
			}
			List<BuilderTiming> rows;
			synchronized (timings) {
				rows = new ArrayList<>(timings.values());
			}
			viewer.setInput(rows.toArray(new BuilderTiming[0]));
		});
	}
}
