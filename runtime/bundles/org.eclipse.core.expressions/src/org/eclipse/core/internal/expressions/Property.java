/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
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
package org.eclipse.core.internal.expressions;

import org.eclipse.core.expressions.IPropertyTester;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

public class Property {

	private final Class<?> fType;
	private final String fNamespace;
	private final String fName;

	private IPropertyTester fTester;

	/* package */ Property(Class<?> type, String namespace, String name) {
		Assert.isNotNull(type);
		Assert.isNotNull(namespace);
		Assert.isNotNull(name);

		fType= type;
		fNamespace= namespace;
		fName= name;
	}

	/* package */ void setPropertyTester(IPropertyTester tester) {
		Assert.isNotNull(tester);
		fTester= tester;
	}

	public boolean isInstantiated() {
		return fTester.isInstantiated();
	}

	public boolean isDeclaringPluginActive() {
		return fTester.isDeclaringPluginActive();
	}

	public boolean isValidCacheEntry(boolean forcePluginActivation) {
		if (forcePluginActivation) {
			return isInstantiated() && isDeclaringPluginActive();
		} else {
			return 	(isInstantiated() && isDeclaringPluginActive()) ||
					(!isInstantiated() && !isDeclaringPluginActive());
		}
	}

	public boolean test(Object receiver, Object[] args, Object expectedValue) throws CoreException {
		try {
			return fTester.test(receiver, fName, args, expectedValue);
		} catch (Exception e) {
			String message = "Error evaluating " + this; //$NON-NLS-1$
			throw new CoreException(new Status(IStatus.ERROR, Property.class, message, e));
		}
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Property))
			return false;
		Property other= (Property)obj;
		return fType.equals(other.fType) && fNamespace.equals(other.fNamespace) && fName.equals(other.fName);
	}

	@Override
	public int hashCode() {
		return (fType.hashCode() << 16) | fNamespace.hashCode() << 8 | fName.hashCode();
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Property ["); //$NON-NLS-1$
		builder.append(fNamespace);
		builder.append("."); //$NON-NLS-1$
		builder.append(fName);
		builder.append(", type="); //$NON-NLS-1$
		builder.append(fType);
		if (fTester != null) {
			builder.append(", tester="); //$NON-NLS-1$
			builder.append(fTester);
		}
		builder.append("]"); //$NON-NLS-1$
		return builder.toString();
	}
}
