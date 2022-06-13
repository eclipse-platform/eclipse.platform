/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
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

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.expressions.Expression;
import org.eclipse.core.expressions.ExpressionConverter;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionDelta;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IRegistryChangeEvent;
import org.eclipse.core.runtime.IRegistryChangeListener;
import org.eclipse.core.runtime.InvalidRegistryObjectException;
import org.eclipse.core.runtime.Platform;

/**
 * This manages the extension point that allows core expression reuse.
 *
 * @since 3.3
 */
public class DefinitionRegistry implements IRegistryChangeListener {
	private Map<String, Expression> cache= null;

	private Map<String, Expression> getCache() {
		if (cache == null) {
			cache= new HashMap<>();
		}
		return cache;
	}

	public DefinitionRegistry() {
		Platform.getExtensionRegistry().addRegistryChangeListener(this, "org.eclipse.core.expressions"); //$NON-NLS-1$
	}

	/**
	 * Get the expression with the id defined by an extension. This class will
	 * cache the expressions when appropriate, so it's OK to always ask the
	 * registry.
	 *
	 * @param id The unique ID of the expression definition
	 * @return the expression
	 * @throws CoreException If the expression cannot be found.
	 */
	public Expression getExpression(String id) throws CoreException {
		Expression cachedExpression= getCache().get(id);
		if (cachedExpression != null) {
			return cachedExpression;
		}

		IExtensionRegistry registry= Platform.getExtensionRegistry();
		IConfigurationElement[] ces= registry.getConfigurationElementsFor("org.eclipse.core.expressions", "definitions"); //$NON-NLS-1$ //$NON-NLS-2$

		Expression foundExpression= null;
		for (IConfigurationElement ce : ces) {
			String cid= ce.getAttribute("id"); //$NON-NLS-1$
			if (cid != null && cid.equals(id)) {
				try {
					foundExpression= getExpression(id, ce);
					break;
				} catch (InvalidRegistryObjectException e) {
					throw new CoreException(new ExpressionStatus(ExpressionStatus.MISSING_EXPRESSION, Messages.format(
						ExpressionMessages.Missing_Expression, id)));
				}
			}
		}
		if (foundExpression == null) {
			throw new CoreException(new ExpressionStatus(ExpressionStatus.MISSING_EXPRESSION, Messages.format(
				ExpressionMessages.Missing_Expression, id)));
		}
		return foundExpression;
	}

	private Expression getExpression(String id, IConfigurationElement element) throws InvalidRegistryObjectException,
		CoreException {
		Expression expr= ExpressionConverter.getDefault().perform(element.getChildren()[0]);
		if (expr != null) {
			getCache().put(id, expr);
		}
		return expr;
	}

	@Override
	public void registryChanged(IRegistryChangeEvent event) {
		IExtensionDelta[] extensionDeltas= event.getExtensionDeltas("org.eclipse.core.expressions", "definitions"); //$NON-NLS-1$//$NON-NLS-2$
		for (IExtensionDelta extensionDelta : extensionDeltas) {
			if (extensionDelta.getKind() == IExtensionDelta.REMOVED) {
				IConfigurationElement[] ces= extensionDelta.getExtension().getConfigurationElements();
				for (IConfigurationElement ce : ces) {
					String id= ce.getAttribute("id"); //$NON-NLS-1$
					if (id != null) {
						getCache().remove(id);
					}
				}
			}
		}
	}
}