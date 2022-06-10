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

import org.eclipse.e4.core.di.IBinding;
import org.eclipse.e4.core.di.IInjector;

public class Binding implements IBinding {

	final private Class<?> clazz;
	final private IInjector injector;

	private Class<?> implementationClazz;
	private String qualifierName;

	public Binding(Class<?> clazz, IInjector injector) {
		this.clazz = clazz;
		this.injector = injector;
	}

	private Binding(Binding source) {
		this.clazz = source.clazz;
		this.injector = source.injector;
		this.implementationClazz = source.implementationClazz;
		this.qualifierName = source.qualifierName;
	}

	@Override
	public IBinding named(String name) {
		Binding binding = new Binding(this);
		binding.qualifierName = name;
		injector.addBinding(binding);
		return binding;
	}

	@Override
	public IBinding implementedBy(Class<?> implClazz) {
		Binding binding = new Binding(this);
		binding.implementationClazz = implClazz;
		injector.addBinding(binding);
		return binding;
	}

	public Class<?> getDescribedClass() {
		return clazz;
	}

	public String getQualifierName() {
		return qualifierName;
	}

	public Class<?> getImplementationClass() {
		if (implementationClazz != null)
			return implementationClazz;
		return clazz;
	}
}
