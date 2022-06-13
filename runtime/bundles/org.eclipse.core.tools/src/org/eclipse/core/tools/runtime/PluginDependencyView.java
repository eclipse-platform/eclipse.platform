/*******************************************************************************
 * Copyright (c) 2002, 2018 IBM Corporation and others.
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

import java.util.HashMap;
import java.util.Map;
import org.eclipse.core.runtime.Platform;
//import org.eclipse.core.runtime.internal.stats.BundleStats;
import org.eclipse.core.tools.*;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.TextViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.osgi.service.resolver.*;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.*;

public class PluginDependencyView extends SpyView implements ISelectionListener {

	private TextViewer viewer;
	private Map<Long, PluginDependencyGraphNode> dependencyGraph = null;

	/** The id by which this view is known in the plug-in registry */
	public static final String VIEW_ID = PluginDependencyView.class.getName();

	@Override
	public void createPartControl(Composite parent) {
		viewer = new TextViewer(parent, SWT.V_SCROLL | SWT.H_SCROLL | SWT.WRAP | SWT.READ_ONLY);
		viewer.setDocument(new Document());

		IActionBars bars = getViewSite().getActionBars();

		final GlobalAction clearOutputAction = new ClearTextAction(viewer.getDocument());
		clearOutputAction.registerAsGlobalAction(bars);

		final GlobalAction selectAllAction = new SelectAllAction(viewer);
		selectAllAction.registerAsGlobalAction(bars);

		// Delete action shortcuts are not captured by the workbench
		// so we need our key binding service to handle Delete keystrokes for us
		this.viewer.getControl().addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.character == SWT.DEL)
					clearOutputAction.run();
			}
		});

		GlobalAction copyAction = new CopyTextSelectionAction(viewer);
		copyAction.registerAsGlobalAction(bars);

		bars.getToolBarManager().add(clearOutputAction);

		bars.updateActionBars();

		// creates a context menu with actions and adds it to the viewer control
		MenuManager menuMgr = new MenuManager();
		menuMgr.add(copyAction);
		menuMgr.add(clearOutputAction);
		Menu menu = menuMgr.createContextMenu(viewer.getControl());
		viewer.getControl().setMenu(menu);

		getViewSite().getPage().addSelectionListener(this);
	}

	@Override
	public void dispose() {
		getViewSite().getPage().removeSelectionListener(this);
		super.dispose();
		dependencyGraph = null;
	}

	/**
	 * Build the table of plug-in dependencies. Iterate over all the plug-ins in the
	 * plug-in registry and the cycle through the list of pre-requisites and create the
	 * parent/child relationships in the nodes.
	 */
	private Map<Long, PluginDependencyGraphNode> getDependencyGraph() {
		if (dependencyGraph != null)
			return dependencyGraph;
		// Build up the dependency graph (see PluginDependencyGraphNode) so
		// we have the information readily available for any plug-in.
		State state = Platform.getPlatformAdmin().getState(false);
		BundleDescription[] plugins = state.getBundles();
		dependencyGraph = new HashMap<>();
		for (BundleDescription descriptor : plugins) {
			PluginDependencyGraphNode node = dependencyGraph.get(Long.valueOf(descriptor.getBundleId()));
			if (node == null) {
				node = new PluginDependencyGraphNode(descriptor);
				dependencyGraph.put(Long.valueOf(descriptor.getBundleId()), node);
			}

			// Cycle through the prerequisites
			BundleSpecification[] requires = descriptor.getRequiredBundles();
			for (BundleSpecification require : requires) {
				BundleDescription childDesc = (BundleDescription) require.getSupplier();
				// if the child doesn't exist then move to the next child
				if (childDesc == null)
					continue;

				// if the child entry is not in the table yet then add it
				PluginDependencyGraphNode childNode = dependencyGraph
						.get(Long.valueOf(childDesc.getBundleId()));
				if (childNode == null) {
					childNode = new PluginDependencyGraphNode(childDesc);
					dependencyGraph.put(Long.valueOf(childDesc.getBundleId()), childNode);
				}

				// Add the child to this node's children and set this node as an ancestor
				// of the child node
				node.addChild(childNode);
				childNode.addAncestor(node);
			}
		}
		return dependencyGraph;
	}

	@Override
	public void selectionChanged(IWorkbenchPart part, ISelection selection) {
		if (!(selection instanceof IStructuredSelection))
			return;
		Object element = ((IStructuredSelection) selection).getFirstElement();
		long id = -1;
		String name = null;
		if (element instanceof BundleDescription) {
			id = ((BundleDescription) element).getBundleId();
			name = ((BundleDescription) element).getSymbolicName();
		}
//		if (element instanceof BundleStats) {
//			id = ((BundleStats) element).getId();
//			name = ((BundleStats) element).getSymbolicName();
//		}
		if (id == -1)
			return;
		PluginDependencyGraphNode node = getDependencyGraph().get(Long.valueOf(id));
		String text = node == null ? NLS.bind(Messages.depend_noInformation, name) : node.toDeepString();
		viewer.getDocument().set(text);
		viewer.refresh();
	}
}
