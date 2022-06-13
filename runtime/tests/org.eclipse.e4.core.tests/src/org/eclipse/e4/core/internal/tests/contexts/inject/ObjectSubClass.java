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
package org.eclipse.e4.core.internal.tests.contexts.inject;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;

import org.eclipse.e4.core.contexts.IEclipseContext;

/**
 * Test class to check injection mechanism into classes with inheritance
 */
public class ObjectSubClass extends ObjectSuperClass {
	@Inject
	/* package */Integer Integer;

	private Object myObject;

	public int setObjectCalled = 0;
	public int setSubFinalized = 0;
	public int postConstructSetObjectCalled;
	public int postConstructSetOverriddenCalled;
	public int subPostConstructCount;
	public int subPreDestroyCount;
	public int overriddenPreDestroyCount;

	public ObjectSubClass() {
		super();
	}

	@Inject
	public void ObjectViaMethod(Float f) {
		myObject = f;
		setObjectCalled++;
	}

	@Override
	@Inject
	public void OverriddenMethod(Float f) {
		setOverriddenCalled++;
	}

	@Override
	@Inject
	public void contextSet(IEclipseContext context) {
		super.contextSet(context);
		setSubFinalized++;
	}

	public Integer getInteger() {
		return Integer;
	}

	public Object getObjectViaMethod() {
		return myObject;
	}

	public int getFinalizedCount() {
		return setSubFinalized;
	}

	public int getOverriddenCount() {
		return setOverriddenCalled;
	}

	@PostConstruct
	public void subPostConstruct() {
		postConstructSetObjectCalled = setObjectCalled;
		postConstructSetStringCalled = setStringCalled;
		postConstructSetOverriddenCalled = setOverriddenCalled;
		subPostConstructCount++;
	}

	@PreDestroy
	public void subPreDestroy() {
		subPreDestroyCount++;
	}

	@Override
	@PreDestroy
	public void overriddenPreDestroy() {
		overriddenPreDestroyCount++;
	}
}
