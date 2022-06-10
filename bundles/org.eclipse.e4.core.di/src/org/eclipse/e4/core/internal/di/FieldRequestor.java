/*******************************************************************************
 * Copyright (c) 2010, 2016 IBM Corporation and others.
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

import java.lang.reflect.Field;
import org.eclipse.e4.core.di.IInjector;
import org.eclipse.e4.core.di.InjectionException;
import org.eclipse.e4.core.di.suppliers.IObjectDescriptor;
import org.eclipse.e4.core.di.suppliers.PrimaryObjectSupplier;

public class FieldRequestor extends Requestor<Field> {

	public FieldRequestor(Field field, IInjector injector, PrimaryObjectSupplier primarySupplier, PrimaryObjectSupplier tempSupplier, Object requestingObject, boolean track) {
		super(field, injector, primarySupplier, tempSupplier, requestingObject, track);
	}

	@Override
	public Object execute() throws InjectionException {
		if (actualArgs == null)
			return null; // optional field
		setField(location, actualArgs[0]);
		clearResolvedArgs();
		return null;
	}

	@Override
	protected IObjectDescriptor[] calcDependentObjects() {
		IObjectDescriptor objectDescriptor = new ObjectDescriptor(location.getGenericType(), location.getAnnotations());
		return new IObjectDescriptor[] {objectDescriptor};
	}

	private boolean setField(Field field, Object value) throws InjectionException {
		Object userObject = getRequestingObject();
		if (userObject == null)
			return false;
		if (!field.isAccessible()) {
			field.setAccessible(true);
		}
		try {
			field.set(userObject, value);
		} catch (IllegalArgumentException | IllegalAccessException e) {
			throw new InjectionException(e);
		}
		return true;
	}

	@Override
	public String toString() {
		StringBuilder tmp = new StringBuilder();
		Object object = getRequestingObject();
		if (object != null)
			tmp.append(object.getClass().getSimpleName());
		tmp.append('.');
		tmp.append(location.getName());
		return tmp.toString();
	}
}
