/*******************************************************************************
 * Copyright (c) 2009, 2017 IBM Corporation and others.
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
 *     Daniel Kruegler <daniel.kruegler@gmail.com> - Bug 487417
 *     Lars Vogel <Lars.Vogel@vogella.com> - Bug 492963
 *******************************************************************************/
package org.eclipse.e4.core.internal.contexts;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import org.eclipse.e4.core.contexts.IContextFunction;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.contexts.RunAndTrack;
import org.eclipse.e4.core.di.IInjector;
import org.eclipse.e4.core.internal.contexts.ConcurrentNeutralValueMap.Value;
import org.eclipse.e4.core.internal.contexts.osgi.ContextDebugHelper;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

/**
 * This implementation assumes that all contexts are of the class EclipseContext. The external
 * methods of it are exposed via IEclipseContext.
 */
public class EclipseContext implements IEclipseContext {

	/**
	 * A context key identifying the parent context.
	 */
	public static final String PARENT = "parentContext"; //$NON-NLS-1$

	/**
	 * A context key (value "debugString") identifying a value to use in debug statements for a
	 * context. A computed value can be used to embed more complex information in debug statements.
	 */
	public static final String DEBUG_STRING = "debugString"; //$NON-NLS-1$

	/**
	 * String used in {@link #toString()} to name contexts not providing the value
	 * for the {@link #DEBUG_STRING} variable
	 */
	public static final String ANONYMOUS_CONTEXT_NAME = "Anonymous Context"; //$NON-NLS-1$

	static class Scheduled {

		public TrackableComputationExt runnable;
		public ContextChangeEvent event;

		public Scheduled(TrackableComputationExt runnable, ContextChangeEvent event) {
			this.runnable = runnable;
			this.event = event;
		}

		@Override
		public int hashCode() {
			return 31 * (31 + event.hashCode()) + runnable.hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Scheduled other = (Scheduled) obj;
			return Objects.equals(this.event, other.event) && Objects.equals(this.runnable, other.runnable);
		}
	}

	private WeakGroupedListenerList weakListeners = new WeakGroupedListenerList();
	private Map<String, ValueComputation> localValueComputations = new ConcurrentHashMap<>();

	final protected ConcurrentNeutralValueMap<String, Object> localValues = // null values allowed
			new ConcurrentNeutralValueMap<>();

	private Set<String> modifiable;

	private List<Computation> waiting; // list of Computations; null for all non-root entries

	/** Concurrent Collection of {@link #selfRef} */
	private final Collection<WeakReference<EclipseContext>> children = new ConcurrentLinkedDeque<>();
	/** The WeakReference in {@link #getParent()}'s {@link #children} */
	private WeakReference<EclipseContext> selfRef;
	private final StrongIterable<EclipseContext> childIterable = new StrongIterable<>(this.children);

	private Set<IContextDisposalListener> notifyOnDisposal = new HashSet<>();

	static private ThreadLocal<Stack<Computation>> currentComputation = new ThreadLocal<>();

	// I don't think we need to sync referenceQueue access
	private ReferenceQueue<Object> referenceQueue = new ReferenceQueue<>();

	private Map<Reference<?>, TrackableComputationExt> activeComputations = Collections.synchronizedMap(new HashMap<>());
	private Set<TrackableComputationExt> activeRATs = Collections.synchronizedSet(new HashSet<>());

	private final static Object[] nullArgs = new Object[] {null};

	/**
	 * A context key (value "activeChildContext") that identifies another {@link IEclipseContext}
	 * that is a child of the context. The meaning of active is up to the application.
	 */
	public static final String ACTIVE_CHILD = "activeChildContext"; //$NON-NLS-1$

	static private final IEclipseContextDebugger debugAddOn = ContextDebugHelper.getDebugger();

	public EclipseContext(IEclipseContext parent) {
		setParent(parent);
		if (parent == null)
			waiting = Collections.synchronizedList(new ArrayList<>());
		if (debugAddOn != null)
			debugAddOn.notify(this, IEclipseContextDebugger.EventType.CONSTRUCTED, null);
	}

	public Iterable<EclipseContext> getChildren() {
		return childIterable;
	}

	@Override
	public boolean containsKey(String name) {
		trackAccess(name);
		return containsKey(name, false);
	}

	public boolean containsKey(String name, boolean localOnly) {
		if (isSetLocally(name))
			return true;
		if (localOnly)
			return false;
		EclipseContext parent = getParent();
		if (parent != null && parent.containsKey(name, localOnly))
			return true;
		return false;
	}

	@Override
	public void dispose() {
		// dispose of child contexts first
		for (EclipseContext childContext : getChildren()) {
			childContext.dispose();
		}

		ContextChangeEvent event = new ContextChangeEvent(this, ContextChangeEvent.DISPOSE, null, null, null);

		Set<Computation> allComputations = new HashSet<>();
		allComputations.addAll(activeComputations.values());
		allComputations.addAll(activeRATs);
		activeComputations.clear();
		activeRATs.clear();

		Set<Scheduled> scheduled = new LinkedHashSet<>();
		allComputations.addAll(getListeners());
		weakListeners.clear();
		for (Computation computation : allComputations) {
			computation.handleInvalid(event, scheduled);
		}
		processScheduled(scheduled);

		synchronized (notifyOnDisposal) {
			for (IContextDisposalListener listener : notifyOnDisposal) {
				listener.disposed(this);
			}
			notifyOnDisposal.clear();
		}
		localValueComputations.values().removeIf(computation -> {
			computation.dispose();
			return true;
		});

		// if this was the parent's active child, deactivate it
		EclipseContext parent = getParent();
		EclipseContext rootContext = null;
		if (parent != null) {
			rootContext = getRoot();
			if (this == parent.getActiveChild())
				parent.set(ACTIVE_CHILD, null);
		}

		localValues.clear();

		if (parent != null) {
			selfRef.clear(); // remove from parent
			if (rootContext != null) {
				rootContext.cleanup();
			}

			// inform the OSGi layer via EventAdmin about the context disposal
			// used for example to be able to cleanup cached requestors in
			// ExtendedObjectSupplier implementations
			EventAdmin admin = parent.get(EventAdmin.class);
			if (admin != null) {
				Event osgiEvent = new Event(IEclipseContext.TOPIC_DISPOSE, (Map<String, ?>) null);
				admin.postEvent(osgiEvent);
			}
		}

		if (debugAddOn != null) {
			debugAddOn.notify(this, IEclipseContextDebugger.EventType.DISPOSED, null);
		}
	}

	@Override
	public Object get(String name) {
		trackAccess(name);
		return internalGet(this, name, false);
	}

	@Override
	public Object getLocal(String name) {
		trackAccess(name);
		return internalGet(this, name, true);
	}

	public Object internalGet(EclipseContext originatingContext, String name, boolean local) {
		if (this == originatingContext) {
			ValueComputation valueComputation = localValueComputations.get(name);
			if (valueComputation != null) {
				Object result = valueComputation.get();
				if (result != IInjector.NOT_A_VALUE) {
					return result;
				}
			}
		}

		Object result = null;
		// 1. try for local value
		Value<Object> value = localValues.getValue(name);
		if (value.isPresent()) {
			result = value.unwrapped();
			if (result == null)
				return null;
		} else
			result = lookup(name, originatingContext);

		// if we found something, compute the concrete value and return
		if (result != null) {
			if (result instanceof IContextFunction) {
				ValueComputation valueComputation = new ValueComputation(name, originatingContext, ((IContextFunction) result));
				// do calculations before adding listeners
				result = valueComputation.get();
				originatingContext.localValueComputations.put(name, valueComputation);
			}
			if (result != IInjector.NOT_A_VALUE) {
				return result;
			}
		}
		// 3. delegate to parent
		if (!local) {
			IEclipseContext parent = (IEclipseContext) localValues.get(PARENT);
			if (parent != null) {
				return ((EclipseContext) parent).internalGet(originatingContext, name, local);
			}
		}
		return null;
	}

	/**
	 * The given name has been modified or removed in this context. Invalidate all local value
	 * computations and listeners that depend on this name.
	 */
	public void invalidate(String name, int eventType, Object oldValue, Object newValue, Set<Scheduled> scheduled) {
		ContextChangeEvent event = new ContextChangeEvent(this, eventType, null, name, oldValue);

		ValueComputation newComputation = localValueComputations.computeIfPresent(name, (k, computation) -> {
			if (computation.shouldRemove(event)) {
				weakListeners.remove(computation);
				return null; // remove
			}
			return computation; // keep
		});
		if (newComputation != null) {
			newComputation.handleInvalid(event, scheduled);
		}
		Set<Computation> namedComputations = weakListeners.getListeners(name);
		if (namedComputations != null && !namedComputations.isEmpty()) {
			for (Computation listener : namedComputations) {
				listener.handleInvalid(event, scheduled);
			}
		}
		boolean addedOrRemoved = eventType == ContextChangeEvent.ADDED || eventType == ContextChangeEvent.REMOVED;
		// invalidate this name in child contexts
		for (EclipseContext childContext : getChildren()) {
			// unless it is already set in this context (and thus hides the change)
			if (!(addedOrRemoved && childContext.isSetLocally(name))) {
				childContext.invalidate(name, eventType, oldValue, newValue, scheduled);
			}
		}
	}

	protected boolean isLocalEquals(String name, Object newValue) {
		Value<Object> value = localValues.getValue(name);
		if (!value.isPresent())
			return false;
		return (value.unwrapped() == newValue);
	}

	private boolean isSetLocally(String name) {
		return localValues.containsKey(name);
	}

	@Override
	public void remove(String name) {
		if (isSetLocally(name)) {
			Object oldValue = localValues.remove(name);
			Set<Scheduled> scheduled = new LinkedHashSet<>();
			invalidate(name, ContextChangeEvent.REMOVED, oldValue, IInjector.NOT_A_VALUE, scheduled);
			processScheduled(scheduled);
		}
	}

	@Override
	public void runAndTrack(final RunAndTrack runnable) {
		TrackableComputationExt computation = new TrackableComputationExt(runnable, this);
		ContextChangeEvent event = new ContextChangeEvent(this, ContextChangeEvent.INITIAL, null, null, null);
		boolean result = computation.update(event);
		if (result) {
			Reference<Object> ref = computation.getReference();
			if (ref != null)
				activeComputations.put(ref, computation);
			else
				activeRATs.add(computation);
		}
	}

	public void removeRAT(Computation computation) {
		// remove from listeners
		weakListeners.remove(computation);
		activeRATs.remove(computation);
	}

	protected void processScheduled(Set<Scheduled> scheduledList) {
		for (Scheduled scheduled : scheduledList) {
			scheduled.runnable.update(scheduled.event);
		}
	}

	@Override
	public void set(String name, Object value) {
		if (PARENT.equals(name)) {
			setParent((IEclipseContext) value);
			return;
		}
		Value<Object> old = localValues.putAndGetOld(name, value);
		boolean containsKey = old.isPresent();
		Object oldValue = old.unwrapped();
		if (!containsKey || oldValue != value) {
			Set<Scheduled> scheduled = new LinkedHashSet<>();
			invalidate(name, ContextChangeEvent.ADDED, oldValue, value, scheduled);
			processScheduled(scheduled);
		}

		// cleanup unused computation listeners
		Reference<?> ref = referenceQueue.poll();
		if (ref != null) {
			ContextChangeEvent event = new ContextChangeEvent(this, ContextChangeEvent.UNINJECTED, nullArgs, null, null);
			for (; ref != null; ref = referenceQueue.poll()) {
				TrackableComputationExt obsoleteComputation = activeComputations.remove(ref);
				if (obsoleteComputation == null)
					continue;
				obsoleteComputation.update(event);
			}
		}
	}

	@Override
	public void modify(String name, Object value) {
		Set<Scheduled> scheduled = new LinkedHashSet<>();
		if (!internalModify(name, value, scheduled))
			set(name, value);
		processScheduled(scheduled);
	}

	public boolean internalModify(String name, Object value, Set<Scheduled> scheduled) {
		boolean containsKey = localValues.containsKey(name);
		if (containsKey) {
			if (!checkModifiable(name)) {
				String tmp = "Variable " + name + " is not modifiable in the context " + this; //$NON-NLS-1$ //$NON-NLS-2$
				throw new IllegalArgumentException(tmp);
			}
			Object oldValue = localValues.putAndGetOld(name, value).unwrapped();
			if (oldValue != value)
				invalidate(name, ContextChangeEvent.ADDED, oldValue, value, scheduled);
			return true;
		}

		EclipseContext parent = getParent();
		if (parent != null)
			return parent.internalModify(name, value, scheduled);
		return false;
	}

	@Override
	public EclipseContext getParent() {
		return (EclipseContext) localValues.get(PARENT);
	}

	@Override
	public void setParent(IEclipseContext parent) {
		EclipseContext parentContext = (EclipseContext) localValues.get(PARENT);
		if (parent == parentContext)
			return; // no-op
		if (parentContext != null)
			selfRef.clear(); // remove from parent
		Set<Scheduled> scheduled = new LinkedHashSet<>();
		EclipseContext newParent = (EclipseContext) parent;
		handleReparent(newParent, scheduled);
		localValues.put(PARENT, parent);
		if (parent != null) {
			selfRef = new WeakReference<>(this);
			newParent.addChild(selfRef);
		}
		processScheduled(scheduled);
		return;
	}

	/**
	 * Returns a string representation of this context for debugging purposes only.
	 */
	@Override
	public String toString() {
		Object debugString = localValues.get(DEBUG_STRING);
		return debugString instanceof String ? ((String) debugString) : ANONYMOUS_CONTEXT_NAME;
	}

	private void trackAccess(String name) {
		Stack<Computation> current = getCalculatedComputations();
		if (current.isEmpty())
			return;
		Computation computation = current.peek(); // only track in the top-most one
		if (computation == null)
			return;
		addDependency(name, computation);
	}

	public void addDependency(String name, Computation computation) {
		weakListeners.add(name, computation);
	}

	@Override
	public void declareModifiable(String name) {
		if (name == null)
			return;
		if (modifiable == null)
			modifiable = new HashSet<>(3);
		modifiable.add(name);
		localValues.putIfAbsent(name, null);
	}

	private boolean checkModifiable(String name) {
		if (modifiable == null)
			return false;
		return modifiable.contains(name);
	}

	public void removeListenersTo(Object object) {
		if (object == null)
			return;
		ContextChangeEvent event = new ContextChangeEvent(this, ContextChangeEvent.UNINJECTED, new Object[] {object}, null, null);
		Set<Computation> comps = getListeners();
		for (Computation computation : comps) {
			if (computation instanceof TrackableComputationExt)
				((TrackableComputationExt) computation).update(event);
		}
	}

	public Set<Computation> getListeners() {
		return weakListeners.getListeners();
	}

	private void handleReparent(EclipseContext newParent, Set<Scheduled> scheduled) {
		// TBD should we lock waiting list while doing reparent?
		// Add "boolean inReparent" on the root context and process right away?
		processWaiting();
		// 1) everybody who depends on me: I need to collect combined list of names injected
		Set<String> usedNames = new HashSet<>();
		collectDependentNames(usedNames);

		// 2) for each used name:
		for (String name : usedNames) {
			if (localValues.containsKey(name))
				continue; // it is a local value
			Object oldValue = get(name);
			Object newValue = (newParent != null) ? newParent.internalGet(this, name, false) : null;
			if (oldValue != newValue)
				invalidate(name, ContextChangeEvent.ADDED, oldValue, newValue, scheduled);
		}

		invalidateLocalComputations(scheduled);
	}

	protected void invalidateLocalComputations(Set<Scheduled> scheduled) {
		ContextChangeEvent event = new ContextChangeEvent(this, ContextChangeEvent.ADDED, null, null, null);
		localValueComputations.values().removeIf(computation -> {
			weakListeners.remove(computation);
			computation.handleInvalid(event, scheduled);
			return true;
		});

		// We need to cleanup computations recursively see bug 468048
		for (EclipseContext c : getChildren()) {
			c.invalidateLocalComputations(scheduled);
		}
	}

	private void collectDependentNames(Set<String> usedNames) {
		Set<String> names = weakListeners.getNames();
		usedNames.addAll(names);
		for (EclipseContext childContext : getChildren()) {
			childContext.collectDependentNames(usedNames);
		}
	}

	@Override
	public void processWaiting() {
		// traverse to the root node
		EclipseContext parent = getParent();
		if (parent != null) {
			parent.processWaiting();
			return;
		}
		if (waiting == null || waiting.isEmpty())
			return;
		// create update notifications
		Computation[] ls = waiting.toArray(new Computation[waiting.size()]);
		waiting.clear();
		ContextChangeEvent event = new ContextChangeEvent(this, ContextChangeEvent.UPDATE, null, null, null);
		for (Computation element : ls) {
			if (element instanceof TrackableComputationExt)
				((TrackableComputationExt) element).update(event);
		}
	}

	public void addWaiting(Computation cp) {
		// traverse to the root node
		EclipseContext parent = getParent();
		if (parent != null) {
			parent.addWaiting(cp);
			return;
		}
		if (waiting == null) // could happen on re-parent
			waiting = Collections.synchronizedList(new ArrayList<>());
		waiting.add(cp);
	}

	protected EclipseContext getRoot() {
		EclipseContext current = this;
		EclipseContext root;
		do {
			root = current;
			current = current.getParent();
		} while (current != null);
		return root;
	}

	private void addChild(WeakReference<EclipseContext> ref) {
		children.add(ref);
	}

	@Override
	public <T> T get(Class<T> clazz) {
		return clazz.cast(get(clazz.getName()));
	}

	@Override
	public boolean containsKey(Class<?> clazz) {
		return containsKey(clazz.getName());
	}

	@Override
	public <T> void set(Class<T> clazz, T value) {
		set(clazz.getName(), value);
	}

	@Override
	public void remove(Class<?> clazz) {
		remove(clazz.getName());
	}

	@Override
	public <T> T getLocal(Class<T> clazz) {
		return clazz.cast(getLocal(clazz.getName()));
	}

	@Override
	public <T> void modify(Class<T> clazz, T value) {
		modify(clazz.getName(), value);
	}

	@Override
	public void declareModifiable(Class<?> clazz) {
		declareModifiable(clazz.getName());
	}

	@Override
	public IEclipseContext createChild() {
		return new EclipseContext(this); // strategies are not inherited
	}

	@Override
	public IEclipseContext createChild(String name) {
		IEclipseContext result = createChild();
		result.set(DEBUG_STRING, name);
		return result;
	}

	public void notifyOnDisposal(IContextDisposalListener listener) {
		synchronized (notifyOnDisposal) {
			notifyOnDisposal.add(listener);
		}
	}

	@Override
	public IEclipseContext getActiveChild() {
		trackAccess(ACTIVE_CHILD);
		return (EclipseContext) internalGet(this, ACTIVE_CHILD, true);
	}

	@Override
	public IEclipseContext getActiveLeaf() {
		IEclipseContext activeContext = this;
		IEclipseContext child = getActiveChild();
		while (child != null) {
			activeContext = child;
			child = child.getActiveChild();
		}
		return activeContext;
	}

	@Override
	public void activate() {
		EclipseContext parent = getParent();
		if (parent == null)
			return;
		if (this == parent.getActiveChild())
			return;
		parent.set(ACTIVE_CHILD, this);
		if (debugAddOn != null) {
			debugAddOn.notify(this, IEclipseContextDebugger.EventType.ACTIVATED, null);
		}
	}

	@Override
	public void activateBranch() {
		for (IEclipseContext i = this; i != null; i = i.getParent()) {
			i.activate();
		}
	}

	@Override
	public void deactivate() {
		EclipseContext parent = getParent();
		if (parent == null)
			return;
		if (this != parent.getActiveChild())
			return; // this is not an active context; return
		parent.set(ACTIVE_CHILD, null);
		if (debugAddOn != null) {
			debugAddOn.notify(this, IEclipseContextDebugger.EventType.DEACTIVATED, null);
		}
	}

	// This method is for debug only, do not use externally
	public Map<String, Object> localData() {
		Map<String, Object> result = new HashMap<>(localValues.size());
		localValues.forEach((k, v) -> {
			if (!(v instanceof IContextFunction)) {
				result.put(k, v);
			}
		});
		return result;
	}

	// This method is for debug only, do not use externally
	public Map<String, Object> localContextFunction() {
		Map<String, Object> result = new HashMap<>(localValues.size());
		localValues.forEach((k, v) -> {
			if (v instanceof IContextFunction) {
				result.put(k, v);
			}
		});
		return result;
	}

	// This method is for debug only, do not use externally
	public Map<String, Object> cachedCachedContextFunctions() {
		Map<String, Object> result = new HashMap<>(localValueComputations.size());
		for (Map.Entry<String, ValueComputation> entry : localValueComputations.entrySet()) {
			if (entry.getValue() != null) {
				Object r = entry.getValue();
				if (r != IInjector.NOT_A_VALUE) {
					result.put(entry.getKey(), entry.getValue());
				}
			}
		}
		return result;
	}

	// This method is for debug only, do not use externally
	public Set<String> getRawListenerNames() {
		return weakListeners.getNames();
	}

	// This method is for debug only, do not use externally
	public Set<Computation> getListeners(String name) {
		return weakListeners.getListeners(name);
	}

	static public Stack<Computation> getCalculatedComputations() {
		Stack<Computation> current = currentComputation.get();
		if (current == null) {
			current = new Stack<>();
			currentComputation.set(current);
		}
		return current;
	}

	public void pushComputation(Computation comp) {
		Stack<Computation> current = getCalculatedComputations();
		current.push(comp);
	}

	public boolean hasComputation(Computation comp) {
		Stack<Computation> current = getCalculatedComputations();
		boolean hasComputation = current.contains(comp);
		return hasComputation;
	}

	public void popComputation(Computation comp) {
		Stack<Computation> current = getCalculatedComputations();
		Computation ended = current.pop();
		if (ended != comp)
			throw new IllegalArgumentException("Internal error: Invalid nested computation processing"); //$NON-NLS-1$
	}

	/**
	 * This method can be overriden to provide additional source for the requested data.
	 * The override is expected to take care of initiating dynamic updates.
	 */
	protected Object lookup(String name, EclipseContext originatingContext) {
		return null;
	}

	@Override
	public <T> T getActive(Class<T> clazz) {
		return clazz.cast(getActive(clazz.getName()));
	}

	@Override
	public Object getActive(final String name) {
		return getActiveLeaf().get(name);
	}

	public WeakReference<Object> trackedWeakReference(Object object) {
		return new WeakReference<>(object, referenceQueue);
	}

	public void cleanup() {
		for (EclipseContext childContext : getChildren()) {
			childContext.cleanup();
		}
		weakListeners.cleanup();
	}
}
