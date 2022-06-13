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

import org.eclipse.core.expressions.IEvaluationContext;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;

/**
 * An evaluation context that can be used to add a new default variable
 * to a hierarchy of evaluation contexts.
 *
 * @since 3.0
 */
public final class DefaultVariable implements IEvaluationContext {

	private Object fDefaultVariable;
	private IEvaluationContext fParent;
	private IEvaluationContext fManagedPool;

	/**
	 * Constructs a new variable pool for a single default variable.
	 *
	 * @param parent the parent context for the default variable. Must not
	 *  be <code>null</code>.
	 * @param defaultVariable the default variable
	 */
	public DefaultVariable(IEvaluationContext parent, Object defaultVariable) {
		Assert.isNotNull(parent);
		Assert.isNotNull(defaultVariable);
		fParent= parent;
		while (parent instanceof DefaultVariable) {
			parent= parent.getParent();
		}
		fManagedPool= parent;
		fDefaultVariable= defaultVariable;
	}

	@Override
	public IEvaluationContext getParent() {
		return fParent;
	}

	@Override
	public IEvaluationContext getRoot() {
		return fParent.getRoot();
	}

	@Override
	public Object getDefaultVariable() {
		return fDefaultVariable;
	}

	@Override
	public void setAllowPluginActivation(boolean value) {
		fParent.setAllowPluginActivation(value);
	}

	@Override
	public boolean getAllowPluginActivation() {
		return fParent.getAllowPluginActivation();
	}

	@Override
	public void addVariable(String name, Object value) {
		fManagedPool.addVariable(name, value);
	}

	@Override
	public Object removeVariable(String name) {
		return fManagedPool.removeVariable(name);
	}

	@Override
	public Object getVariable(String name) {
		return fManagedPool.getVariable(name);
	}

	@Override
	public Object resolveVariable(String name, Object[] args) throws CoreException {
		return fManagedPool.resolveVariable(name, args);
	}
}
