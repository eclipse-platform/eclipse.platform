/*******************************************************************************
 * Copyright (c) 2009, 2010 IBM Corporation and others.
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
package org.eclipse.e4.core.internal.tests.contexts.inject;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;

import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.Optional;

/**
 * Test class to check injection mechanism
 */
public class ObjectBasic {

	// Injected directly
	@Inject @Optional
	public String injectedString;
	@Inject
	private Integer injectedInteger;

	// Injected indirectly
	public Double d;
	public Float f;
	public Character c;
	public IEclipseContext context;

	// Test status
	public boolean finalized = false;
	public boolean disposed = false;
	public int setMethodCalled = 0;
	public int setMethodCalled2 = 0;

	public ObjectBasic() {
		// placeholder
	}

	@Inject
	public void objectViaMethod(Double d) {
		setMethodCalled++;
		this.d = d;
	}

	@Inject
	public void arguments(Float f, @Optional Character c) {
		setMethodCalled2++;
		this.f = f;
		this.c = c;
	}

	@PostConstruct
	public void postCreate(IEclipseContext context) {
		this.context = context;
		finalized = true;
	}

	@PreDestroy
	public void dispose(IEclipseContext context) {
		if (this.context != context)
			throw new IllegalArgumentException("Unexpected context");
		this.context = null;
		disposed = true;
	}

	public Integer getInt() {
		return injectedInteger;
	}

}
