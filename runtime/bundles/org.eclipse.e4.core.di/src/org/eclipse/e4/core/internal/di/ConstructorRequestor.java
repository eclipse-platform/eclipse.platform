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

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import org.eclipse.e4.core.di.IInjector;
import org.eclipse.e4.core.di.InjectionException;
import org.eclipse.e4.core.di.suppliers.IObjectDescriptor;
import org.eclipse.e4.core.di.suppliers.PrimaryObjectSupplier;

public class ConstructorRequestor extends Requestor<Constructor<?>> {

	public ConstructorRequestor(Constructor<?> constructor, IInjector injector, PrimaryObjectSupplier primarySupplier, PrimaryObjectSupplier tempSupplier) {
		super(constructor, injector, primarySupplier, tempSupplier, null, false /* do not track */);
	}

	@Override
	public Object execute() throws InjectionException {
		Object result = null;
		if (!location.isAccessible()) {
			location.setAccessible(true);
		}
		boolean pausedRecording = false;
		if ((primarySupplier != null)) {
			primarySupplier.pauseRecording();
			pausedRecording = true;
		}
		try {
			result = location.newInstance(actualArgs);
		} catch (IllegalArgumentException | IllegalAccessException e) {
			throw new InjectionException(e);
		} catch (InstantiationException e) {
			throw new InjectionException("Unable to instantiate " + location, e); //$NON-NLS-1$
		} catch (InvocationTargetException e) {
			Throwable originalException = e.getCause();
			// Errors such as ThreadDeath or OutOfMemoryError should not be trapped
			// http://bugs.eclipse.org/bugs/show_bug.cgi?id=457687
			if (originalException instanceof Error) {
				throw (Error) originalException;
			}
			throw new InjectionException((originalException != null) ? originalException : e);
		} finally {
			if (pausedRecording)
				primarySupplier.resumeRecording();
			clearResolvedArgs();
		}
		return result;
	}

	@Override
	public IObjectDescriptor[] calcDependentObjects() {
		Annotation[][] annotations = location.getParameterAnnotations();
		Type[] logicalParams = location.getGenericParameterTypes();
		// JDK bug: different methods see / don't see generated args for nested classes
		// http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=5087240
		Class<?>[] compilerParams = location.getParameterTypes();
		if (compilerParams.length > logicalParams.length) {
			Type[] tmp = new Type[compilerParams.length];
			System.arraycopy(compilerParams, 0, tmp, 0, compilerParams.length - logicalParams.length);
			System.arraycopy(logicalParams, 0, tmp, compilerParams.length - logicalParams.length, logicalParams.length);
			logicalParams = tmp;
		}
		if (logicalParams.length == 0) {
			return EMPTY_DESCRIPTORS;
		}
		IObjectDescriptor[] descriptors = new IObjectDescriptor[logicalParams.length];
		for (int i = 0; i < logicalParams.length; i++) {
			descriptors[i] = new ObjectDescriptor(logicalParams[i], annotations[i]);
		}
		return descriptors;
	}

	@Override
	public Class<?> getRequestingObjectClass() {
		return location.getDeclaringClass();
	}

	@Override
	public String toString() {
		StringBuilder tmp = new StringBuilder();
		Object object = getRequestingObject();
		if (object != null)
			tmp.append(object.getClass().getSimpleName());
		else
			tmp.append(location.getDeclaringClass().getSimpleName());
		tmp.append('(');
		tmp.append(')');
		return tmp.toString();
	}
}
