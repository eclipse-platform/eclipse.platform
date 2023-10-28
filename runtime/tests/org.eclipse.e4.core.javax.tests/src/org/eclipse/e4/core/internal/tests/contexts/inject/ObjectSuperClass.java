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

/**
 * Test class to check injection mechanism into classes with inheritance
 */
public class ObjectSuperClass {

	protected IEclipseContext context;
	@Inject
	private String String;
	private String myString;

	public int postConstructSetStringCalled;
	public int setFinalizedCalled = 0;
	public int setStringCalled = 0;
	public int superPostConstructCount;
	public int superPreDestroyCount;
	public int setOverriddenCalled = 0;

	public ObjectSuperClass() {
		// placeholder
	}

	@Inject
	public void contextSet(IEclipseContext context) {
		this.context = context;
		setFinalizedCalled++;
	}

	public IEclipseContext getContext() {
		return context;
	}

	public int getFinalizedCalled() {
		return setFinalizedCalled;
	}

	public String getString() {
		return String;
	}

	public String getStringViaMethod() {
		return myString;
	}

	@Inject
	public void OverriddenMethod(Float f) {
		setOverriddenCalled++;
	}

	@Inject
	public void StringViaMethod(String string) {
		myString = string;
		setStringCalled++;
	}

	@PostConstruct
	public void superPostConstruct() {
		// record setter invocation counts at time of post construct invocation
		postConstructSetStringCalled = setStringCalled;
		superPostConstructCount++;
	}

	@PreDestroy
	public void superPreDestroy() {
		superPreDestroyCount++;
	}

	@PreDestroy()
	public void overriddenPreDestroy() {
		//
	}

}
