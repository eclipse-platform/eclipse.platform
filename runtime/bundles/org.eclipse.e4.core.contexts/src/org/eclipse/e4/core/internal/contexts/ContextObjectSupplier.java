/*******************************************************************************
 * Copyright (c) 2009, 2015 IBM Corporation and others.
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
package org.eclipse.e4.core.internal.contexts;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Objects;
import java.util.Stack;
import javax.inject.Named;
import org.eclipse.e4.core.contexts.Active;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.contexts.RunAndTrack;
import org.eclipse.e4.core.di.IInjector;
import org.eclipse.e4.core.di.suppliers.IObjectDescriptor;
import org.eclipse.e4.core.di.suppliers.IRequestor;
import org.eclipse.e4.core.di.suppliers.PrimaryObjectSupplier;
import org.eclipse.e4.core.internal.di.Requestor;

public class ContextObjectSupplier extends PrimaryObjectSupplier {

	final static protected String ECLIPSE_CONTEXT_NAME = IEclipseContext.class.getName();

	public static class ContextInjectionListener extends RunAndTrackExt {

		final private Object[] result;
		final private String[] keys;
		final private boolean[] active;
		final private IRequestor requestor;
		final private IEclipseContext context;

		public ContextInjectionListener(IEclipseContext context, Object[] result, String[] keys, boolean[] active, IRequestor requestor, boolean group) {
			super(group);
			this.result = result;
			this.keys = keys;
			this.active = active;
			this.requestor = requestor;
			this.context = context;
		}

		@Override
		public Reference<Object> getReference() {
			if (requestor instanceof Requestor)
				return ((Requestor<?>) requestor).getReference();
			return super.getReference();
		}

		@Override
		public boolean update(IEclipseContext eventsContext, int eventType, Object[] extraArguments) {
			if (eventType == ContextChangeEvent.INITIAL) {
				// needs to be done inside runnable to establish dependencies
				for (int i = 0; i < keys.length; i++) {
					if (keys[i] == null)
						continue;
					IEclipseContext targetContext = (active[i]) ? context.getActiveLeaf() : context;
					if (ECLIPSE_CONTEXT_NAME.equals(keys[i])) {
						result[i] = targetContext;
						IEclipseContext parent = targetContext.getParent(); // creates pseudo-link
						if (parent == null)
							targetContext.get(ECLIPSE_CONTEXT_NAME); // pseudo-link in case there is no parent
					} else if (targetContext.containsKey(keys[i]))
						result[i] = targetContext.get(keys[i]);
				}
				return true;
			}

			switch (eventType) {
			case ContextChangeEvent.DISPOSE:
				if (eventsContext == context) {
					ContextObjectSupplier originatingSupplier = eventsContext.getLocal(ContextObjectSupplier.class);
					requestor.disposed(originatingSupplier);
					return false;
				}	break;
			case ContextChangeEvent.UNINJECTED:
				if (eventsContext == context) {
					ContextObjectSupplier originatingSupplier = eventsContext.getLocal(ContextObjectSupplier.class);
					return requestor.uninject(extraArguments[0], originatingSupplier);
				}	break;
			default:
				if (!requestor.isValid())
					return false; // remove this listener
				requestor.resolveArguments(false);
				requestor.execute();
				break;
			}
			return true;
		}

		@Override
		public boolean changed(IEclipseContext eventsContext) {
			return true;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int hashRresult = 1;
			hashRresult = prime * hashRresult + Objects.hashCode(context);
			return prime * hashRresult + Objects.hashCode(requestor);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			ContextInjectionListener other = (ContextInjectionListener) obj;
			return Objects.equals(this.context, other.context) && Objects.equals(this.requestor, other.requestor);
		}

		@Override
		public String toString() {
			return requestor.toString();
		}

	}

	final private IEclipseContext context;

	public ContextObjectSupplier(IEclipseContext context, IInjector injector) {
		this.context = context;
	}

	public IEclipseContext getContext() {
		return context;
	}

	@Override
	public void get(IObjectDescriptor[] descriptors, Object[] actualArgs, final IRequestor requestor, boolean initial, boolean track, boolean group) {
		final String[] keys = new String[descriptors.length];
		final boolean[] active = new boolean[descriptors.length];

		for (int i = 0; i < descriptors.length; i++) {
			String key = getKey(descriptors[i]);
			if ((actualArgs[i] == IInjector.NOT_A_VALUE))
				keys[i] = key;
			else if (ECLIPSE_CONTEXT_NAME.equals(key)) // allow provider to override IEclipseContext
				keys[i] = ECLIPSE_CONTEXT_NAME;
			else
				keys[i] = null;
			if (descriptors[i] == null)
				active[i] = false;
			else
				active[i] = (descriptors[i].hasQualifier(Active.class));
		}

		if (requestor != null && track) { // only track if requested
			if (initial) {
				RunAndTrack trackable = new ContextInjectionListener(context, actualArgs, keys, active, requestor, group);
				context.runAndTrack(trackable);
			} else { // we do track if this is done inside a computation, but don't create another runnable
				fillArgs(actualArgs, keys, active);
			}
		} else if (descriptors.length > 0) {
			pauseRecording();
			try {
				fillArgs(actualArgs, keys, active);
			} finally {
				resumeRecording();
			}
		}
	}

	private void fillArgs(Object[] actualArgs, String[] keys, boolean[] active) {
		for (int i = 0; i < keys.length; i++) {
			if (keys[i] == null)
				continue;
			IEclipseContext targetContext = (active[i]) ? context.getActiveLeaf() : context;
			if (ECLIPSE_CONTEXT_NAME.equals(keys[i]))
				actualArgs[i] = targetContext;
			else if (targetContext.containsKey(keys[i]))
				actualArgs[i] = targetContext.get(keys[i]);
		}
	}

	private String getKey(IObjectDescriptor descriptor) {
		if (descriptor.hasQualifier(Named.class)) {
			Named namedAnnotation = descriptor.getQualifier(Named.class);
			return namedAnnotation.value();
		}
		Type elementType = descriptor.getDesiredType();
		return typeToString(elementType);
	}

	private String typeToString(Type type) {
		if (type == null)
			return null;
		if (type instanceof Class<?>)
			return ((Class<?>) type).getName();
		if (type instanceof ParameterizedType) {
			Type rawType = ((ParameterizedType) type).getRawType();
			return typeToString(rawType);
		}
		return null;
	}

	@Override
	synchronized public void pauseRecording() {
		Stack<Computation> current = EclipseContext.getCalculatedComputations();
		current.push(null);
	}

	@Override
	synchronized public void resumeRecording() {
		Stack<Computation> current = EclipseContext.getCalculatedComputations();
		Computation plug = current.pop();
		if (plug != null)
			throw new IllegalArgumentException("Internal error in nested computation processing"); //$NON-NLS-1$
	}

	static public ContextObjectSupplier getObjectSupplier(IEclipseContext context, IInjector injector) {
		if (context == null)
			return null;
		// don't track this dependency if we are called in RaT
		ContextObjectSupplier supplier = (ContextObjectSupplier) ((EclipseContext) context).internalGet((EclipseContext) context, ContextObjectSupplier.class.getName(), true);
		if (supplier != null)
			return supplier;
		ContextObjectSupplier objectSupplier = new ContextObjectSupplier(context, injector);
		context.set(ContextObjectSupplier.class, objectSupplier);
		return objectSupplier;
	}

	@Override
	public WeakReference<Object> makeReference(Object object) {
		if (context instanceof EclipseContext) {
			return ((EclipseContext) context).trackedWeakReference(object);
		}
		return super.makeReference(object);
	}

}
