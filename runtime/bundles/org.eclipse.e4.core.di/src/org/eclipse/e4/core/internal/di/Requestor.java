/*******************************************************************************
 * Copyright (c) 2010, 2015 IBM Corporation and others.
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
package org.eclipse.e4.core.internal.di;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.lang.reflect.AnnotatedElement;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;
import org.eclipse.e4.core.di.IInjector;
import org.eclipse.e4.core.di.annotations.GroupUpdates;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.core.di.suppliers.IObjectDescriptor;
import org.eclipse.e4.core.di.suppliers.IRequestor;
import org.eclipse.e4.core.di.suppliers.PrimaryObjectSupplier;

/**
 * @noextend This class is not intended to be subclassed by clients.
 */
abstract public class Requestor<L extends AnnotatedElement> implements IRequestor {

	private static final Map<AnnotatedElement, IObjectDescriptor[]> descriptorCache = Collections
			.synchronizedMap(new WeakHashMap<>());
	protected static final IObjectDescriptor[] EMPTY_DESCRIPTORS = new IObjectDescriptor[0];

	/** The request location; may be null */
	final protected L location;
	final private WeakReference<Object> objectRef;
	final protected boolean track;
	final private boolean groupUpdates;
	final private boolean isOptional;
	// since objectRef is weak, must maintain object hashCode for stability
	final private int objectHashcode;

	final private IInjector injector;
	final protected PrimaryObjectSupplier primarySupplier;
	private PrimaryObjectSupplier tempSupplier;

	protected Object[] actualArgs;

	private IObjectDescriptor[] objectDescriptors;

	/* @NonNull */
	protected abstract IObjectDescriptor[] calcDependentObjects();

	public Requestor(L location, IInjector injector, PrimaryObjectSupplier primarySupplier, PrimaryObjectSupplier tempSupplier, Object requestingObject, boolean track) {
		this.location = location;
		this.injector = injector;
		this.primarySupplier = primarySupplier;
		this.tempSupplier = tempSupplier;
		if (requestingObject != null) {
			if (primarySupplier != null)
				objectRef = primarySupplier.makeReference(requestingObject);
			else
				objectRef = new WeakReference<>(requestingObject);
			objectHashcode = requestingObject.hashCode();
		} else {
			objectRef = null;
			objectHashcode = 0;
		}
		this.track = track;
		groupUpdates = (location == null) ? false : location.isAnnotationPresent(GroupUpdates.class);
		isOptional = (location == null) ? false : location.isAnnotationPresent(Optional.class);
	}

	/**
	 * Return the injection location described by this requestor
	 * @return the request location; may be {@code null}
	 */
	public L getLocation() {
		return location;
	}

	public IInjector getInjector() {
		return injector;
	}

	public PrimaryObjectSupplier getPrimarySupplier() {
		return primarySupplier;
	}

	public PrimaryObjectSupplier getTempSupplier() {
		return tempSupplier;
	}

	public void clearTempSupplier() {
		tempSupplier = null; // don't keep temporary suppliers in memory after initial processing
	}

	@Override
	public Object getRequestingObject() {
		if (objectRef == null)
			return null;
		return objectRef.get();
	}

	@Override
	public Class<?> getRequestingObjectClass() {
		Object object = getRequestingObject();
		if (object == null)
			return null;
		return object.getClass();
	}

	public Reference<Object> getReference() {
		return objectRef;
	}

	/**
	 * Determines if the requestor wants to be called whenever one of the dependent object changes.
	 */
	public boolean shouldTrack() {
		return track;
	}

	public boolean shouldGroupUpdates() {
		return groupUpdates;
	}

	public boolean isOptional() {
		return isOptional;
	}

	/**
	 * If actual arguments are resolved for this requestor
	 */
	public boolean isResolved() {
		return (actualArgs != null);
	}

	public void setResolvedArgs(Object[] actualArgs) {
		this.actualArgs = actualArgs;
	}

	@Override
	public boolean isValid() {
		return (getRequestingObject() != null);
	}

	@Override
	public void resolveArguments(boolean initial) {
		((InjectorImpl) injector).resolveArguments(this, initial);
	}

	@Override
	public void disposed(PrimaryObjectSupplier objectSupplier) {
		((InjectorImpl) injector).disposed(objectSupplier);
		// Clear the objectRef if the supplier is disposed see 430041
		if (objectSupplier == primarySupplier) {
			if (objectRef != null) {
				objectRef.clear();
			}
		}
	}

	@Override
	public boolean uninject(Object object, PrimaryObjectSupplier objectSupplier) {
		Object originatingObject = getRequestingObject();
		if (originatingObject == null)
			return false;
		if (originatingObject != object)
			return true;
		injector.uninject(object, objectSupplier);
		return false;
	}

	public synchronized IObjectDescriptor[] getDependentObjects() {
		if (objectDescriptors == null) {
			objectDescriptors = descriptorCache.computeIfAbsent(location, l -> calcDependentObjects());
		}
		return objectDescriptors;
	}

	/**
	 * Don't hold on to the resolved results as it will prevent
	 * them from being garbage collected.
	 */
	protected void clearResolvedArgs() {
		if (actualArgs == null)
			return;
		Arrays.fill(actualArgs, null);
		actualArgs = null;
		return;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Objects.hashCode(location);
		result = prime * result + Boolean.hashCode(groupUpdates);
		result = prime * result + Objects.hashCode(injector);
		result = prime * result + Boolean.hashCode(isOptional);
		result = prime * result + Objects.hashCode(primarySupplier);
		return prime * result + objectHashcode;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Requestor<?> other = (Requestor<?>) obj;
		return Objects.equals(this.location, other.location) && this.groupUpdates == other.groupUpdates
				&& Objects.equals(this.injector, other.injector) && this.isOptional == other.isOptional
				&& Objects.equals(this.primarySupplier, other.primarySupplier)
				&& Objects.equals(this.getRequestingObject(), other.getRequestingObject());
	}

}
