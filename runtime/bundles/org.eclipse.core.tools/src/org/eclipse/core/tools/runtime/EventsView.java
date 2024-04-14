/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM - Initial API and implementation
 *******************************************************************************/

package org.eclipse.core.tools.runtime;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.eclipse.core.runtime.PerformanceStats;
import org.eclipse.core.tools.CopyStructuredSelectionAction;
import org.eclipse.core.tools.Messages;
import org.eclipse.core.tools.TableSelectionProviderDecorator;
import org.eclipse.core.tools.TableWithTotalView;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ColumnLayoutData;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IActionBars;

/**
 * Stats View
 *
 * This spy utility will display globally available statistical
 * information on various plug-ins.  The statistics available are those that
 * are available through the PluginStats class of
 * org.eclipse.core.resource/src/org/eclipse/core/internal/utils
 *
 * Currently available information includes:
 * 	the id of the statistic
 * 	the number of notification this plug-in received
 * 	the amount of time spent receiving these notifications
 * 	the number of build requests on this plug-in
 * 	the amount of time spent building this plug-in
 *
 * Build statistics will only be displayed if the tracing/debug option
 * "build/invoking" in the plug-in org.eclipse.core.resources is set to
 * true.
 */

public class EventsView extends TableWithTotalView {
	static class EventsViewContentProvider implements ITreeContentProvider {
		@Override
		public void dispose() {
			// do nothing
		}

		@Override
		public Object[] getChildren(Object parentElement) {
			return new Object[0];
		}

		@Override
		public Object[] getElements(Object input) {
			return PerformanceStats.getAllStats();
		}

		@Override
		public Object getParent(Object element) {
			return null;
		}

		@Override
		public boolean hasChildren(Object element) {
			return false;
		}

		@Override
		public void inputChanged(Viewer aViewer, Object oldInput, Object newInput) {
			// do nothing
		}
	}

	/**
	 * Class to display the labels for the stats view table.
	 */
	static class EventsViewLabelProvider extends LabelProvider implements ITableLabelProvider, IColorProvider {

		@Override
		public Image getColumnImage(Object arg0, int arg1) {
			return null;
		}

		@Override
		public String getColumnText(Object element, int columnIndex) {
			if (!(element instanceof PerformanceStats)) {
				return Messages.stats_badStat;
			}
			PerformanceStats stats = (PerformanceStats) element;
			switch (columnIndex) {
				case COLUMN_EVENT :
					return stats.getEvent();
				case COLUMN_BLAME :
					return stats.getBlameString();
				case COLUMN_CONTEXT :
					return stats.getContext();
				case COLUMN_COUNT :
					return Integer.toString(stats.getRunCount());
				case COLUMN_TIME :
					return Long.toString(stats.getRunningTime());
			}
			return Messages.stats_badColumn;
		}

		@Override
		public Color getForeground(Object element) {
			if (!(element instanceof PerformanceStats)) {
				return null;
			}
			PerformanceStats stats = (PerformanceStats) element;
			if (stats.isFailure())
				return Display.getDefault().getSystemColor(SWT.COLOR_RED);
			return null;
		}

		@Override
		public Color getBackground(Object element) {
			return null;
		}
	}

	class StatsListener extends PerformanceStats.PerformanceListener {
		private void asyncExec(Runnable runnable) {
			@SuppressWarnings("synthetic-access")
			final Control control = viewer.getControl();
			if (control == null || control.isDisposed())
				return;
			final Display display = control.getDisplay();
			if (display.isDisposed())
				return;
			display.asyncExec(runnable);
		}

		@Override
		public void eventsOccurred(final PerformanceStats[] event) {
			asyncExec(() -> {
				if (!getViewer().getControl().isDisposed())
					getViewer().refresh();
			});
		}

		@Override
		public void eventFailed(final PerformanceStats event, final long duration) {
			asyncExec(() -> {
				String msg = "Performance event failure: " + event.getEvent() + " blame: " + event.getBlameString() + " context: " + event.getContext() + " duration: " + duration; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
				getViewSite().getActionBars().getStatusLineManager().setErrorMessage(msg);
				//					MessageDialog.openError(getSite().getShell(), "Performance failure", msg);
			});
		}
	}

	// Table of Column Indices
	public final static int COLUMN_EVENT = 0;
	public final static int COLUMN_BLAME = 1;
	public final static int COLUMN_CONTEXT = 2;
	public final static int COLUMN_COUNT = 3;
	public final static int COLUMN_TIME = 4;

	private final String columnHeaders[] = {Messages.stats_eventHeader, //
			Messages.stats_blameHeader, //
			Messages.stats_contextHeader, //
			Messages.stats_countHeader, //
			Messages.stats_timeHeader, //
	};
	private final ColumnLayoutData columnLayouts[] = {new ColumnWeightData(80), // event
			new ColumnWeightData(180), // blame
			new ColumnWeightData(40), // context
			new ColumnPixelData(65), // count
			new ColumnPixelData(65)}; // total time
	private CopyStructuredSelectionAction copyAction;
	private Action resetAction;
	private final StatsListener statsListener = new StatsListener();

	@Override
	protected String[] computeTotalLine(Iterator<PerformanceStats> iter) {
		String[] totals = new String[getColumnHeaders().length];
		int count = 0;
		int events = 0;
		long time = 0;
		if (!iter.hasNext()) {
			Object[] elements = ((ITreeContentProvider) viewer.getContentProvider()).getElements(viewer.getInput());
			@SuppressWarnings({ "rawtypes", "unchecked" })
			List<PerformanceStats> list = elements == null ? (List) Arrays.asList(elements) : Collections.emptyList();
			iter = list.iterator();
		}
		while (iter.hasNext()) {
			PerformanceStats element = iter.next();
			events += element.getRunCount();
			time += element.getRunningTime();
			count++;
		}
		totals[0] = "Total: " + count; //$NON-NLS-1$
		totals[2] = "" + events; //$NON-NLS-1$
		totals[3] = "" + time; //$NON-NLS-1$
		return totals;
	}

	@Override
	protected void createActions() {
		resetAction = new Action("Reset") { //$NON-NLS-1$
			@Override
			public void run() {
				PerformanceStats.clear();
				getViewer().setInput("");
				updateTotals();
			}
		};
		resetAction.setToolTipText("Reset all event statistics"); //$NON-NLS-1$
		resetAction.setImageDescriptor(ImageDescriptor.createFromURLSupplier(true, () -> EventsView.class.getResource("/icons/clear.gif")));
		// Add copy selection action

		IActionBars bars = getViewSite().getActionBars();
		copyAction = new CopyStructuredSelectionAction(new TableSelectionProviderDecorator(viewer));
		copyAction.registerAsGlobalAction(bars);
		bars.updateActionBars();
	}

	@Override
	protected void createContextMenu() {
		// creates a context menu with actions and adds it to the viewer control
		MenuManager menuMgr = new MenuManager();
		menuMgr.add(resetAction);
		menuMgr.add(copyAction);
		Menu menu = menuMgr.createContextMenu(viewer.getControl());
		viewer.getControl().setMenu(menu);
	}

	@Override
	public void createPartControl(Composite parent) {
		super.createPartControl(parent);
		PerformanceStats.addListener(statsListener);
		viewer.setInput(""); //$NON-NLS-1$
	}

	@Override
	protected void createToolbar() {
		IToolBarManager mgr = getViewSite().getActionBars().getToolBarManager();
		mgr.add(resetAction);
	}

	@Override
	public void dispose() {
		super.dispose();
		PerformanceStats.removeListener(statsListener);
	}

	@Override
	protected String[] getColumnHeaders() {
		return columnHeaders;
	}

	@Override
	protected ColumnLayoutData[] getColumnLayout() {
		return columnLayouts;
	}

	@Override
	protected ITreeContentProvider getContentProvider() {
		return new EventsViewContentProvider();
	}

	@Override
	protected ITableLabelProvider getLabelProvider() {
		return new EventsViewLabelProvider();
	}

	@Override
	protected ViewerComparator getSorter(int column) {
		return new EventsSorter(column);
	}

	@Override
	protected String getStatusLineMessage(Object element) {
		if (!(element instanceof PerformanceStats))
			return ""; //$NON-NLS-1$
		return ((PerformanceStats) element).getBlameString();
	}

	protected TableViewer getViewer() {
		return viewer;
	}
}
