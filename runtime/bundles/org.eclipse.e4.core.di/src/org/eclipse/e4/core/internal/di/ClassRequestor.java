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

import java.lang.reflect.Field;
import javax.inject.Named;
import org.eclipse.e4.core.di.IInjector;
import org.eclipse.e4.core.di.InjectionException;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.core.di.suppliers.IObjectDescriptor;
import org.eclipse.e4.core.di.suppliers.PrimaryObjectSupplier;

/**
 * This requestor is used to establish a link between the object supplier
 * and the injected object. This pseudo-link is useful if no regular links
 * were created during injection (say, only constructor injection was used)
 * but the injected object needs to be notified on the supplier's disposal.
 */
public class ClassRequestor extends Requestor<Class<?>> {

	@Optional
	@Named("e4.internal.injectionLink")
	final static public String pseudoVariable = null;

	private static IObjectDescriptor[] pseudoVariableDescriptor;

	public ClassRequestor(Class<?> clazz, IInjector injector, PrimaryObjectSupplier primarySupplier, PrimaryObjectSupplier tempSupplier, Object requestingObject, boolean track) {
		super(clazz, injector, primarySupplier, tempSupplier, requestingObject, track);
	}

	@Override
	public Object execute() throws InjectionException {
		clearResolvedArgs();
		return null;
	}

	@Override
	public IObjectDescriptor[] calcDependentObjects() {
		if (pseudoVariableDescriptor == null) {
			Field field = null;
			try {
				field = ClassRequestor.class.getField("pseudoVariable"); //$NON-NLS-1$
			} catch (SecurityException | NoSuchFieldException e) {
				e.printStackTrace(); // tested - not going to happen
				return null;
			}
			pseudoVariableDescriptor = new IObjectDescriptor[] {
					new ObjectDescriptor(field.getGenericType(), field.getAnnotations()) };
		}

		return pseudoVariableDescriptor;
	}

	@Override
	public String toString() {
		StringBuilder tmp = new StringBuilder();
		if (location != null)
			tmp.append(location.getSimpleName());
		tmp.append('.');
		tmp.append(pseudoVariable);
		return tmp.toString();
	}
}
