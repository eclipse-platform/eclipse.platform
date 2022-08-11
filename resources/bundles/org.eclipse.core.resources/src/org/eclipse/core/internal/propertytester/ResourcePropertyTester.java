/*******************************************************************************
 * Copyright (c) 2005, 2015 IBM Corporation and others.
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
package org.eclipse.core.internal.propertytester;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.QualifiedName;

/**
 * A property tester for various properties of resources.
 *
 * @since 3.2
 */
public class ResourcePropertyTester extends PropertyTester {
	/**
	 * A property indicating the file extension (value <code>"extension"</code>).
	 * "*" and "?" wild cards are supported.
	 */
	protected static final String EXTENSION = "extension"; //$NON-NLS-1$

	/**
	 * A property indicating the file name (value <code>"name"</code>). "*"
	 * and "?" wild cards are supported.
	 */
	protected static final String NAME = "name"; //$NON-NLS-1$

	/**
	 * A property indicating the file path (value <code>"path"</code>). "*"
	 * and "?" wild cards are supported.
	 */
	protected static final String PATH = "path"; //$NON-NLS-1$

	/**
	 * A property indicating a persistent property on the selected resource
	 * (value <code>"persistentProperty"</code>). If two arguments are given,
	 * this treats the first as the property name, and the second as the expected
	 * property value. If only one argument (or just the expected value) is
	 * given, this treats it as the property name, and simply tests for existence of
	 * the property on the resource.
	 */
	protected static final String PERSISTENT_PROPERTY = "persistentProperty"; //$NON-NLS-1$

	/**
	 * A property indicating the project nature (value
	 * <code>"projectNature"</code>).
	 */
	protected static final String PROJECT_NATURE = "projectNature"; //$NON-NLS-1$

	/**
	 * A property indicating a persistent property on the selected resource's
	 * project. (value <code>"projectPersistentProperty"</code>). If two
	 * arguments are given, this treats the first as the property name, and the
	 * second as the expected property value. If only one argument (or just the
	 * expected value) is given, this treats it as the property name, and simply
	 * tests for existence of the property on the resource.
	 */
	protected static final String PROJECT_PERSISTENT_PROPERTY = "projectPersistentProperty"; //$NON-NLS-1$

	/**
	 * A property indicating a session property on the selected resource's
	 * project. (value <code>"projectSessionProperty"</code>). If two
	 * arguments are given, this treats the first as the property name, and the
	 * second as the expected property value. If only one argument (or just the
	 * expected value) is given, this treats it as the property name, and simply
	 * tests for existence of the property on the resource.
	 */
	protected static final String PROJECT_SESSION_PROPERTY = "projectSessionProperty"; //$NON-NLS-1$

	/**
	 * A property indicating whether the file is read only (value
	 * <code>"readOnly"</code>).
	 */
	protected static final String READ_ONLY = "readOnly"; //$NON-NLS-1$

	/**
	 * A property indicating a session property on the selected resource (value
	 * <code>"sessionProperty"</code>). If two arguments are given, this
	 * treats the first as the property name, and the second as the expected
	 * property value. If only one argument (or just the expected value) is
	 * given, this treats it as the property name, and simply tests for existence of
	 * the property on the resource.
	 */
	protected static final String SESSION_PROPERTY = "sessionProperty"; //$NON-NLS-1$

	@Override
	public boolean test(Object receiver, String method, Object[] args, Object expectedValue) {
		if (!(receiver instanceof IResource))
			return false;
		IResource res = (IResource) receiver;
		if (method.equals(NAME)) {
			return new StringMatcher(toString(expectedValue)).match(res.getName());
		} else if (method.equals(PATH)) {
			return new StringMatcher(toString(expectedValue)).match(res.getFullPath().toString());
		} else if (method.equals(EXTENSION)) {
			return new StringMatcher(toString(expectedValue)).match(res.getFileExtension());
		} else if (method.equals(READ_ONLY)) {
			ResourceAttributes attr = res.getResourceAttributes();
			return (attr != null && attr.isReadOnly()) == toBoolean(expectedValue);
		} else if (method.equals(PROJECT_NATURE)) {
			try {
				IProject proj = res.getProject();
				return proj != null && proj.isAccessible() && proj.hasNature(toString(expectedValue));
			} catch (CoreException e) {
				return false;
			}
		} else if (method.equals(PERSISTENT_PROPERTY)) {
			return testProperty(res, true, args, expectedValue);
		} else if (method.equals(PROJECT_PERSISTENT_PROPERTY)) {
			return testProperty(res.getProject(), true, args, expectedValue);
		} else if (method.equals(SESSION_PROPERTY)) {
			return testProperty(res, false, args, expectedValue);
		} else if (method.equals(PROJECT_SESSION_PROPERTY)) {
			return testProperty(res.getProject(), false, args, expectedValue);
		}
		return false;
	}

	/**
	 * Tests whether a session or persistent property on the resource or its
	 * project matches the given value.
	 *
	 * @param resource
	 *            the resource to check
	 * @param persistentFlag
	 *            <code>true</code> for a persistent property,
	 *            <code>false</code> for a session property
	 * @param args
	 *            additional arguments to evaluate the property.
	 *            If of length 0, this treats the expectedValue as the property name
	 *            and does a simple check for existence of the property.
	 *            If of length 1, this treats the first argument as the property name
	 *            and does a simple check for existence of the property.
	 *            If of length 2, this treats the first argument as the property name,
	 *            the second argument as the expected value, and checks for equality
	 *            with the actual property value.
	 * @param expectedValue
	 *            used only if args is of length 0 (see Javadoc for args parameter)
	 * @return whether there is a match
	 */
	protected boolean testProperty(IResource resource, boolean persistentFlag, Object[] args, Object expectedValue) {
		//the project of IWorkspaceRoot is null
		if (resource == null)
			return false;
		String propertyName;
		String expectedVal;
		switch (args.length) {
			case 0:
				propertyName = toString(expectedValue);
				expectedVal = null;
				break;
			case 1:
				propertyName = toString(args[0]);
				expectedVal = null;
				break;
			default:
				propertyName = toString(args[0]);
				expectedVal = toString(args[1]);
				break;
		}
		try {
			QualifiedName key = toQualifedName(propertyName);
			Object actualVal = persistentFlag ? resource.getPersistentProperty(key) : resource.getSessionProperty(key);
			if (actualVal == null)
				return false;
			return expectedVal == null || expectedVal.equals(actualVal.toString());
		} catch (CoreException e) {
			//if the resource is not accessible, fall through and return false below
		}
		return false;
	}

	/**
	 * Converts the given expected value to a boolean.
	 *
	 * @param expectedValue
	 *            the expected value (may be <code>null</code>).
	 * @return <code>false</code> if the expected value equals Boolean.FALSE,
	 *         <code>true</code> otherwise
	 */
	protected boolean toBoolean(Object expectedValue) {
		if (expectedValue instanceof Boolean) {
			return ((Boolean) expectedValue).booleanValue();
		}
		return true;
	}

	/**
	 * Converts the given name to a qualified name.
	 *
	 * @param name the name
	 * @return the qualified name
	 */
	protected QualifiedName toQualifedName(String name) {
		QualifiedName key;
		int dot = name.lastIndexOf('.');
		if (dot != -1) {
			key = new QualifiedName(name.substring(0, dot), name.substring(dot + 1));
		} else {
			key = new QualifiedName(null, name);
		}
		return key;
	}

	/**
	 * Converts the given expected value to a <code>String</code>.
	 *
	 * @param expectedValue
	 *            the expected value (may be <code>null</code>).
	 * @return the empty string if the expected value is <code>null</code>,
	 *         otherwise the <code>toString()</code> representation of the
	 *         expected value
	 */
	protected String toString(Object expectedValue) {
		return expectedValue == null ? "" : expectedValue.toString(); //$NON-NLS-1$
	}
}
