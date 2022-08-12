/*******************************************************************************
 * Copyright (c) 2018 Red Hat Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Mickael Istria (Red Hat Inc.)
 *******************************************************************************/
package org.eclipse.core.internal.events;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import org.eclipse.core.internal.resources.ComputeProjectOrder;
import org.eclipse.core.internal.resources.ComputeProjectOrder.Digraph;
import org.eclipse.core.internal.resources.ComputeProjectOrder.Digraph.Edge;
import org.eclipse.core.internal.resources.ComputeProjectOrder.VertexOrder;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.*;

/**
 *
 */
class GraphProcessor<T> {

	final private Digraph<T> graph;
	final private Set<T> toProcess;
	final private Set<T> processing;
	final private Set<T> processed;
	final private VertexOrder<T> sequentialOrder;
	final private JobGroup buildJobGroup;
	final private BiConsumer<T, GraphProcessor<T>> processor;
	final private Function<T, ISchedulingRule> ruleFactory;

	GraphProcessor(Digraph<T> graph1, Class<T> clazz, final BiConsumer<T, GraphProcessor<T>> processor, Function<T, ISchedulingRule> ruleFactory, JobGroup buildJobGroup) {
		this.graph = graph1;
		this.processor = processor;
		this.ruleFactory = ruleFactory;
		this.buildJobGroup = buildJobGroup;
		toProcess = new HashSet<>(graph.vertexMap.keySet());
		processing = new HashSet<>();
		processed = new HashSet<>();
		sequentialOrder = ComputeProjectOrder.computeVertexOrder(graph, clazz);
	}

	private boolean complete() {
		return processed.size() == graph.vertexList.size();
	}

	private boolean allTriggered() {
		return toProcess.isEmpty();
	}

	private void markProcessing(T item) {
		if (!toProcess.remove(item)) {
			throw new IllegalArgumentException();
		}
		processing.add(item);
	}

	void markProcessed(T item) {
		if (!processing.remove(item)) {
			throw new IllegalArgumentException();
		}
		processed.add(item);
	}

	private Set<T> computeReadyVertexes() {
		Set<T> res = new HashSet<>(toProcess);
		for (T item : toProcess) {
			for (Edge<T> edge : graph.getEdges()) {
				if (edge.to == item && !processed.contains(edge.from)) {
					res.remove(item);
				}
			}
		}
		if (res.isEmpty() && !isProcessing()) { // nothing ready, nothing running: a cycle!
			for (T id : sequentialOrder.vertexes) {
				if (!isProcessed(id)) {
					return Collections.singleton(id);
				}
			}
		}
		return res;
	}

	private boolean isProcessing() {
		return !processing.isEmpty();
	}

	private boolean isProcessed(T item) {
		return processed.contains(item);
	}

	public T[] getSequentialOrder() {
		return this.sequentialOrder.vertexes;
	}

	public synchronized void processGraphWithParallelJobs() {
		if (!complete()) {
			if (!allTriggered()) {
				Set<T> readyToBuild = computeReadyVertexes();
				readyToBuild.forEach(this::triggerJob);
			}
		}
	}

	private void triggerJob(T item) {
		synchronized (this) {
			markProcessing(item);
		}
		Job buildJob = new Job(item.toString()) {

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				processor.accept(item, GraphProcessor.this);
				synchronized (GraphProcessor.this) {
					markProcessed(item);
					// do it as part of Job so we're sure following jobs are triggered before this one completes,
					// so we can safely rely on join(family)
					processGraphWithParallelJobs();
				}
				return Status.OK_STATUS;
			}

			@Override
			public boolean belongsTo(Object family) {
				return super.belongsTo(family) || family == GraphProcessor.this;
			}
		};
		if (this.ruleFactory != null) {
			buildJob.setRule(this.ruleFactory.apply(item));
		}
		buildJob.setJobGroup(buildJobGroup);
		buildJob.schedule();
	}

}