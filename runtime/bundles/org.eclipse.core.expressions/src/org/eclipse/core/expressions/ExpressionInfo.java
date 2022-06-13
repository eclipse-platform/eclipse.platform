/*******************************************************************************
 * Copyright (c) 2000, 2013 IBM Corporation and others.
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
package org.eclipse.core.expressions;

import java.util.ArrayList;
import java.util.List;

/**
 * A status object describing information about an expression tree.
 * This information can for example be used to decide whether an
 * expression tree has to be reevaluated if the value of some
 * variables changes.
 * <p>
 * This class is not intended to be extended by clients.
 * </p>
 *
 * @since 3.2
 *
 * @noextend This class is not intended to be subclassed by clients.
 */
public class ExpressionInfo {

	private boolean fHasDefaultVariableAccess;
	private boolean fHasSystemPropertyAccess;

	// Although we are using this as sets we use lists since
	// they are faster for smaller numbers of elements
	private List<String> fAccessedVariableNames;
	private List<Class<?>> fMisbehavingExpressionTypes;
	private List<String> fAccessedPropertyNames;

	/**
	 * Returns <code>true</code> if the default variable is accessed
	 * by the expression tree.
	 *
	 * @return whether the default variable is accessed or not
	 */
	public boolean hasDefaultVariableAccess() {
		return fHasDefaultVariableAccess;
	}

	/**
	 * Marks the default variable as accessed.
	 */
	public void markDefaultVariableAccessed() {
		fHasDefaultVariableAccess= true;
	}

	/**
	 * Returns <code>true</code> if the system property is accessed
	 * by the expression tree.
	 *
	 * @return whether the system property is accessed or not
	 */
	public boolean hasSystemPropertyAccess() {
		return fHasSystemPropertyAccess;
	}

	/**
	 * Marks the system property as accessed.
	 */
	public void markSystemPropertyAccessed() {
		fHasSystemPropertyAccess= true;
	}

	/**
	 * Returns the set of accessed variables.
	 *
	 * @return the set of accessed variables
	 */
	public String[] getAccessedVariableNames() {
		if (fAccessedVariableNames == null) {
			return new String[0];
		}
		return fAccessedVariableNames.toArray(new String[fAccessedVariableNames.size()]);
	}

	/**
	 * Marks the given variable as accessed.
	 *
	 * @param name the accessed variable
	 */
	public void addVariableNameAccess(String name) {
		if (fAccessedVariableNames == null) {
			fAccessedVariableNames= new ArrayList<>(5);
			fAccessedVariableNames.add(name);
		} else if (!fAccessedVariableNames.contains(name)) {
			fAccessedVariableNames.add(name);
		}
	}

	/**
	 * Returns the set of accessed {@link PropertyTester} properties.
	 *
	 * @return the fully qualified names of accessed properties, or an empty array
	 *
	 * @see #hasSystemPropertyAccess() for system properties
	 * @since 3.4
	 */
	public String[] getAccessedPropertyNames() {
		if (fAccessedPropertyNames == null) {
			return new String[0];
		}
		return fAccessedPropertyNames.toArray(new String[fAccessedPropertyNames.size()]);
	}

	/**
	 * Marks the given property (the fully qualified name of a
	 * {@link PropertyTester} property) as accessed.
	 *
	 * @param name
	 *            the fully qualified property name
	 *
	 * @see #markSystemPropertyAccessed() for system properties
	 * @since 3.4
	 */
	public void addAccessedPropertyName(String name) {
		if (fAccessedPropertyNames == null) {
			fAccessedPropertyNames= new ArrayList<>(5);
			fAccessedPropertyNames.add(name);
		} else if (!fAccessedPropertyNames.contains(name))
		{
			fAccessedPropertyNames.add(name);
		}
	}

	/**
	 * Returns the set of expression types which don't reimplement the
	 * new {@link Expression#collectExpressionInfo(ExpressionInfo)}
	 * method. If one expression in the expression tree didn't implement the
	 * method, then no optimizations can be done. Returns <code>null</code> if
	 * all expressions implement the method.
	 *
	 * @return the set of expression types which don't implement the
	 *  <code>computeReevaluationInfo</code> method, or <code>null</code> if all do
	 */
	public Class<?>[] getMisbehavingExpressionTypes() {
		if (fMisbehavingExpressionTypes == null) {
			return null;
		}
		return fMisbehavingExpressionTypes.toArray(new Class[fMisbehavingExpressionTypes.size()]);
	}

	/**
	 * Adds the given class to the list of misbehaving classes.
	 *
	 * @param clazz the class to add.
	 */
	public void addMisBehavingExpressionType(Class<?> clazz) {
		if (fMisbehavingExpressionTypes == null) {
			fMisbehavingExpressionTypes= new ArrayList<>(2);
			fMisbehavingExpressionTypes.add(clazz);
		} else if (!fMisbehavingExpressionTypes.contains(clazz)) {
			fMisbehavingExpressionTypes.add(clazz);
		}
	}

	/**
	 * Merges this reevaluation information with the given info.
	 *
	 * @param other the information to merge with
	 */
	public void merge(ExpressionInfo other) {
		mergeDefaultVariableAccess(other);
		mergeSystemPropertyAccess(other);

		mergeAccessedVariableNames(other);
		mergeAccessedPropertyNames(other);
		mergeMisbehavingExpressionTypes(other);
	}

	/**
	 * Merges this reevaluation information with the given info
	 * ignoring the default variable access.
	 *
	 * @param other the information to merge with
	 */
	public void mergeExceptDefaultVariable(ExpressionInfo other) {
		mergeSystemPropertyAccess(other);

		mergeAccessedVariableNames(other);
		mergeAccessedPropertyNames(other);
		mergeMisbehavingExpressionTypes(other);
	}

	/**
	 * Merges only the default variable access.
	 *
	 * @param other the information to merge with
	 */
	private void mergeDefaultVariableAccess(ExpressionInfo other) {
		fHasDefaultVariableAccess= fHasDefaultVariableAccess || other.fHasDefaultVariableAccess;
	}

	/**
	 * Merges only the system property access.
	 *
	 * @param other the information to merge with
	 */
	private void mergeSystemPropertyAccess(ExpressionInfo other) {
		fHasSystemPropertyAccess= fHasSystemPropertyAccess || other.fHasSystemPropertyAccess;
	}

	/**
	 * Merges only the accessed variable names.
	 *
	 * @param other the information to merge with
	 */
	private void mergeAccessedVariableNames(ExpressionInfo other) {
		if (fAccessedVariableNames == null) {
			fAccessedVariableNames= other.fAccessedVariableNames; //TODO: shares the two lists! Can propagate further additions up into sibling branches.
		} else if (other.fAccessedVariableNames != null) {
			for (String variableName : other.fAccessedVariableNames) {
				if (!fAccessedVariableNames.contains(variableName)) {
					fAccessedVariableNames.add(variableName);
				}
			}
		}
	}

	/**
	 * Merges only the accessed property names.
	 *
	 * @param other the information to merge with
	 * @see #mergeSystemPropertyAccess(ExpressionInfo) for system properties
	 * @since 3.4
	 */
	private void mergeAccessedPropertyNames(ExpressionInfo other) {
		if (fAccessedPropertyNames == null) {
			fAccessedPropertyNames= other.fAccessedPropertyNames; //TODO: shares the two lists!
		} else if (other.fAccessedPropertyNames != null) {
			for (String variableName : other.fAccessedPropertyNames) {
				if (!fAccessedPropertyNames.contains(variableName)) {
					fAccessedPropertyNames.add(variableName);
				}
			}
		}
	}

	/**
	 * Merges only the misbehaving expression types.
	 *
	 * @param other the information to merge with
	 */
	private void mergeMisbehavingExpressionTypes(ExpressionInfo other) {
		if (fMisbehavingExpressionTypes == null) {
			fMisbehavingExpressionTypes= other.fMisbehavingExpressionTypes; //TODO: shares the two lists!
		} else if (other.fMisbehavingExpressionTypes != null) {
			for (Class<?> clazz : other.fMisbehavingExpressionTypes) {
				if (!fMisbehavingExpressionTypes.contains(clazz)) {
					fMisbehavingExpressionTypes.add(clazz);
				}
			}
		}
	}
}