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
import java.util.List;
import java.util.Map;
//import org.eclipse.core.runtime.internal.stats.BundleStats;
import org.eclipse.core.tools.ClearTextAction;
import org.eclipse.core.tools.CopyTextSelectionAction;
import org.eclipse.core.tools.GlobalAction;
import org.eclipse.core.tools.Messages;
import org.eclipse.core.tools.SelectAllAction;
import org.eclipse.core.tools.SpyView;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.TextViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;

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
		this.viewer.getControl().addKeyListener(KeyListener.keyPressedAdapter(e -> {
			if (e.character == SWT.DEL) {
				clearOutputAction.run();
			}
		}));

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
		if (dependencyGraph != null) {
			return dependencyGraph;
		}
		// Build up the dependency graph (see PluginDependencyGraphNode) so
		// we have the information readily available for any plug-in.

		BundleContext bundleContext = FrameworkUtil.getBundle(PluginDependencyView.class).getBundleContext();
		Bundle[] plugins = bundleContext.getBundles();
		dependencyGraph = new HashMap<>();
		for (Bundle bundle : plugins) {
			PluginDependencyGraphNode node = dependencyGraph.computeIfAbsent(bundle.getBundleId(),
					i -> new PluginDependencyGraphNode(bundle));

			// Cycle through the prerequisites
			BundleWiring wiring = bundle.adapt(BundleWiring.class);
			if (wiring == null) {
				continue;
			}
			List<BundleWire> requiredWires = wiring.getRequiredWires(null);
			for (BundleWire requiredWire : requiredWires) {
				Bundle capabilityProvider = requiredWire.getCapability().getRevision().getBundle();
				// if the child entry is not in the table yet then add it
				PluginDependencyGraphNode childNode = dependencyGraph.computeIfAbsent(capabilityProvider.getBundleId(),
						i -> new PluginDependencyGraphNode(capabilityProvider));

				// Add the child to this node's children and set this node as an
				// ancestor of the child node
				node.addChild(childNode);
				childNode.addAncestor(node);
			}
		}
		return dependencyGraph;
	}

	@Override
	public void selectionChanged(IWorkbenchPart part, ISelection selection) {
		if (!(selection instanceof IStructuredSelection structuredSelection)) {
			return;
		}
		if (structuredSelection.getFirstElement() instanceof Bundle bundle) {
			PluginDependencyGraphNode node = getDependencyGraph().get(bundle.getBundleId());
			String text = node == null ? NLS.bind(Messages.depend_noInformation, bundle.getSymbolicName()) : node.toDeepString();
			viewer.getDocument().set(text);
			viewer.refresh();
		}
	}
}
