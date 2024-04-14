/*******************************************************************************
 * Copyright (c) 2017, 2022 SSI Schaefer IT Solutions GmbH and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     SSI Schaefer IT Solutions GmbH
 *     IBM Corporation - bug fixes
 *******************************************************************************/
package org.eclipse.debug.ui.launchview.internal.view;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchMode;
import org.eclipse.debug.ui.launchview.internal.LaunchViewBundleInfo;
import org.eclipse.debug.ui.launchview.internal.LaunchViewMessages;
import org.eclipse.debug.ui.launchview.internal.model.LaunchObjectContainerModel;
import org.eclipse.debug.ui.launchview.internal.model.LaunchObjectFavoriteContainerModel;
import org.eclipse.debug.ui.launchview.internal.model.LaunchObjectModel;
import org.eclipse.debug.ui.launchview.internal.model.LaunchViewModel;
import org.eclipse.debug.ui.launchview.services.ILaunchObject;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.di.Focus;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.model.application.ui.menu.MDirectMenuItem;
import org.eclipse.e4.ui.model.application.ui.menu.MMenu;
import org.eclipse.e4.ui.model.application.ui.menu.MMenuFactory;
import org.eclipse.e4.ui.model.application.ui.menu.MPopupMenu;
import org.eclipse.e4.ui.services.EMenuService;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.dialogs.FilteredTree;
import org.eclipse.ui.dialogs.PatternFilter;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.inject.Inject;

public class LaunchViewImpl implements Supplier<Set<ILaunchObject>> {

	private static final String CONTEXT_MENU_ID = "LaunchViewContextMenu"; //$NON-NLS-1$

	private LaunchViewModel model;
	private final Runnable reset = LaunchViewImpl.this::queueReset;
	private final Job resetJob;
	private FilteredTree tree;

	@Inject
	EMenuService menuService;

	public LaunchViewImpl() {
		resetJob = new Job(LaunchViewMessages.LaunchView_Reset) {

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				reset();
				return Status.OK_STATUS;
			}
		};

		resetJob.setSystem(true);
	}

	@PostConstruct
	public void createView(Composite parent, MPart part) {
		model = LaunchViewModel.getService();
		model.addUpdateListener(reset);

		tree = new FilteredTree(parent, SWT.BORDER | SWT.MULTI, new PatternFilter() {

			@Override
			public void setPattern(String pattern) {
				if (pattern != null && !pattern.isEmpty() && pattern.indexOf("*") != 0 && pattern.indexOf("?") != 0 //$NON-NLS-1$ //$NON-NLS-2$
						&& pattern.indexOf(".") != 0) { //$NON-NLS-1$
					super.setPattern("*" + pattern); //$NON-NLS-1$
				} else {
					super.setPattern(pattern);
				}
			}

			@Override
			protected boolean isLeafMatch(Viewer viewer, Object element) {
				if (!(element instanceof LaunchObjectModel) || element instanceof LaunchObjectContainerModel) {
					return false;
				}
				String txt = ((LaunchObjectModel) element).getLabel().toString();
				return wordMatches(txt);
			}
		}, true, true);
		tree.getViewer().setContentProvider(new LaunchViewContentProvider());
		tree.getViewer().setLabelProvider(new DelegatingStyledCellLabelProvider(new LaunchViewLabelProvider()));
		tree.getViewer().getTree().setLayout(new GridLayout());
		tree.getViewer().getTree().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		createMenus(part);

		tree.getViewer().addDoubleClickListener(e -> {
			ITreeSelection selection = tree.getViewer().getStructuredSelection();
			if (selection.isEmpty()) {
				return;
			}

			for (Object selected : selection.toList()) {
				if (selected instanceof LaunchObjectContainerModel) {
					tree.getViewer().setExpandedState(selected, !tree.getViewer().getExpandedState(selected));
					return; // only supported for single element double-click
				}
			}

			new LaunchAction(DebugPlugin.getDefault().getLaunchManager().getLaunchMode("run"), LaunchViewImpl.this).run(); //$NON-NLS-1$
		});
		tree.getViewer().setComparator(new LaunchViewComparator());

		reset();
	}

	@Focus
	public void onFocus() {
		tree.getViewer().getControl().setFocus();
	}

	private void createMenus(MPart part) {
		part.getMenus().clear(); // clear persisted state

		// View menu
		MMenu viewMenu = MMenuFactory.INSTANCE.createMenu();
		viewMenu.setElementId("menu:" + part.getElementId()); //$NON-NLS-1$
		viewMenu.getTags().add("ViewMenu"); //$NON-NLS-1$

		MDirectMenuItem refresh = MMenuFactory.INSTANCE.createDirectMenuItem();
		refresh.setLabel(LaunchViewMessages.LaunchView_Refresh);
		refresh.setIconURI("platform:/plugin/" + LaunchViewBundleInfo.PLUGIN_ID + "/icons/refresh.png"); //$NON-NLS-1$ //$NON-NLS-2$
		refresh.setObject(new RefreshHandler());

		MDirectMenuItem terminateAll = MMenuFactory.INSTANCE.createDirectMenuItem();
		terminateAll.setLabel(LaunchViewMessages.LaunchView_TerminateAll);
		terminateAll.setIconURI("platform:/plugin/" + LaunchViewBundleInfo.PLUGIN_ID + "/icons/terminate_all_co.png"); //$NON-NLS-1$ //$NON-NLS-2$
		terminateAll.setObject(new TerminateAllHandler());

		viewMenu.getChildren().add(refresh);
		viewMenu.getChildren().add(MMenuFactory.INSTANCE.createMenuSeparator());
		viewMenu.getChildren().add(terminateAll);

		// contributions from providers
		model.getProviders().forEach(p -> p.contributeViewMenu(this, viewMenu));

		part.getMenus().add(viewMenu);

		// Context menu
		MPopupMenu ctxMenu = MMenuFactory.INSTANCE.createPopupMenu();
		ctxMenu.setElementId(CONTEXT_MENU_ID);

		// one menu item for each mode that launches all selected
		for (ILaunchMode mode : getPreSortedLaunchModes()) {
			ctxMenu.getChildren().add(new LaunchAction(mode, this).asMMenuItem());
		}

		ctxMenu.getChildren().add(MMenuFactory.INSTANCE.createMenuSeparator());
		ctxMenu.getChildren().add(new RelaunchAction(this).asMMenuItem());
		ctxMenu.getChildren().add(new TerminateAction(this).asMMenuItem());
		ctxMenu.getChildren().add(MMenuFactory.INSTANCE.createMenuSeparator());
		ctxMenu.getChildren().add(new EditAction(this).asMMenuItem());

		// contributions from providers
		model.getProviders().forEach(p -> p.contributeContextMenu(this, ctxMenu));

		part.getMenus().add(ctxMenu);

		menuService.registerContextMenu(tree.getViewer().getControl(), CONTEXT_MENU_ID);
	}

	private List<ILaunchMode> getPreSortedLaunchModes() {
		List<ILaunchMode> modes = new ArrayList<>();

		ILaunchMode run = null;
		ILaunchMode debug = null;
		ILaunchMode profile = null;
		ILaunchMode coverage = null;

		ILaunchMode[] launchModes = DebugPlugin.getDefault().getLaunchManager().getLaunchModes();
		List<ILaunchMode> others = new ArrayList<>();

		for (ILaunchMode m : launchModes) {
			switch (m.getIdentifier()) {
				case "run": //$NON-NLS-1$
					run = m;
					break;
				case "debug": //$NON-NLS-1$
					debug = m;
					break;
				case "profile": //$NON-NLS-1$
					profile = m;
					break;
				case "coverage": //$NON-NLS-1$
					coverage = m;
					break;
				default:
					others.add(m);
			}
		}

		if (run != null) {
			modes.add(run);
		}
		if (debug != null) {
			modes.add(debug);
		}
		if (coverage != null) {
			modes.add(coverage);
		}
		if (profile != null) {
			modes.add(profile);
		}
		modes.addAll(others);
		return modes;
	}

	private void queueReset() {
		resetJob.cancel();
		resetJob.schedule(100);
	}

	@Override
	public Set<ILaunchObject> get() {
		ISelection selection = tree.getViewer().getSelection();
		IStructuredSelection structuredSelection = (IStructuredSelection) selection;
		if (structuredSelection.isEmpty()) {
			return Collections.emptySet();
		}

		Set<LaunchObjectModel> elements = new TreeSet<>();
		// expand selection if containers are selected
		for (Object selected : structuredSelection.toList()) {
			if (selected instanceof LaunchObjectContainerModel) {
				// skip container nodes (Bug 577581)
			} else if (selected instanceof LaunchObjectModel) {
				elements.add((LaunchObjectModel) selected);
			}
		}

		return elements.stream().map(LaunchObjectModel::getObject).filter(Objects::nonNull).collect(Collectors.toCollection(TreeSet::new));
	}

	private synchronized void reset() {
		if (tree == null || tree.isDisposed()) {
			return;
		}
		tree.getDisplay().syncExec(() -> {
			tree.getViewer().getTree().setRedraw(false);
			try {
				TreePath[] exp = tree.getViewer().getExpandedTreePaths();
				tree.getViewer().setInput(model.getModel());
				tree.getViewer().setExpandedTreePaths(exp);
			} finally {
				tree.getViewer().getTree().setRedraw(true);
			}
		});
	}

	@PreDestroy
	public void destroy() {
		model.removeUpdateListener(reset);
	}

	private final class RefreshHandler {

		@Execute
		public void handle() {
			reset();
		}
	}

	private final class TerminateAllHandler {

		@Execute
		public void handle() {
			LaunchObjectContainerModel root = (LaunchObjectContainerModel) tree.getViewer().getInput();
			if (root == null) {
				return;
			}

			for (LaunchObjectModel container : root.getChildren()) {
				if (container instanceof LaunchObjectFavoriteContainerModel) {
					continue;
				}

				if (container instanceof LaunchObjectContainerModel) {
					for (LaunchObjectModel m : ((LaunchObjectContainerModel) container).getChildren()) {
						if (m.getObject() == null) {
							continue;
						}

						if (m.getObject().canTerminate()) {
							m.getObject().terminate();
						}
					}
				}
			}
		}
	}

}