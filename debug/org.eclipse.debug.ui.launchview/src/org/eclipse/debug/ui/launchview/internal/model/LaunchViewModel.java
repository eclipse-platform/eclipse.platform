/*******************************************************************************
 * Copyright (c) 2017, 2019 SSI Schaefer IT Solutions GmbH and others.
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
 *******************************************************************************/
package org.eclipse.debug.ui.launchview.internal.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.eclipse.debug.ui.launchview.services.ILaunchObjectProvider;
import org.eclipse.ui.IWorkbench;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

@Component(immediate = true)
public class LaunchViewModel implements ILaunchModel {

	public Set<ILaunchObjectProvider> providers = new TreeSet<>((a, b) -> {
		int x = Integer.compare(b.getPriority(), a.getPriority());
		if (x == 0) {
			x = a.getClass().getName().compareTo(b.getClass().getName());
		}
		return x;
	});
	private static LaunchViewModel service;

	private final List<Runnable> updateListeners = new ArrayList<>();
	private final Runnable providerUpdateListener = this::fireUpdate;

	public Set<ILaunchObjectProvider> getProviders() {
		return providers;
	}

	@Override
	public LaunchObjectContainerModel getModel() {
		LaunchObjectContainerModel root = new LaunchObjectContainerModel();

		// find all objects from services, sorted by prio (highest prio first).
		Set<LaunchObjectModel> allObjects = providers.stream().map(ILaunchObjectProvider::getLaunchObjects).flatMap(o -> o.stream().map(LaunchObjectModel::new)).collect(Collectors.toCollection(TreeSet::new));

		// create favorite container
		LaunchObjectFavoriteContainerModel favorites = new LaunchObjectFavoriteContainerModel();
		root.addChild(favorites);

		// create all required type containers
		allObjects.stream().map(o -> o.getObject().getType()).distinct().map(LaunchObjectContainerModel::new).forEach(root::addChild);

		// create all nodes
		allObjects.stream().forEach(m -> {
			LaunchObjectContainerModel container = root.getContainerFor(m);
			if (container == null) {
				return;
			}
			container.addChild(m);
			if (m.getObject() != null && m.getObject().isFavorite()) {
				favorites.addChild(m);
			}
		});

		// this is the root :)
		return root;
	}

	@Reference(service = ILaunchObjectProvider.class, cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY)
	public void addLaunchObjectProvider(ILaunchObjectProvider svc) {
		providers.add(svc);
		svc.addUpdateListener(providerUpdateListener);

		fireUpdate();
	}

	public void removeLaunchObjectProvider(ILaunchObjectProvider svc) {
		providers.remove(svc);
		svc.removeUpdateListener(providerUpdateListener);

		fireUpdate();
	}

	@Reference(service = IWorkbench.class, cardinality = ReferenceCardinality.MANDATORY, policy = ReferencePolicy.STATIC, unbind = "unsetWorkbench")
	public void setWorkbench(@SuppressWarnings("unused") IWorkbench svc) {
		// this reference is just a marker to control startup order.
		// this is required, otherwise this service activates so early, that the
		// prompt for the workspace location is no longer shown (as the location
		// is accessed indirectly before the prompt, which initializes it to a
		// default).
	}

	public void unsetWorkbench(@SuppressWarnings("unused") IWorkbench svc) {
	}

	@Activate
	public void activate() {
		service = this;
	}

	@Deactivate
	public void deactivate() {
		service = null;
	}

	public void addUpdateListener(Runnable r) {
		updateListeners.add(r);
	}

	public void removeUpdateListener(Runnable r) {
		updateListeners.remove(r);
	}

	private void fireUpdate() {
		updateListeners.forEach(Runnable::run);
	}

	public static LaunchViewModel getService() {
		return service;
	}

}
