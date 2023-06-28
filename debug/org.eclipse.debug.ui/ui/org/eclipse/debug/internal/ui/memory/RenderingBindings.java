/*******************************************************************************
 * Copyright (c) 2004, 2013 IBM Corporation and others.
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
package org.eclipse.debug.internal.ui.memory;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.expressions.EvaluationResult;
import org.eclipse.core.expressions.Expression;
import org.eclipse.core.expressions.ExpressionConverter;
import org.eclipse.core.expressions.ExpressionTagNames;
import org.eclipse.core.expressions.IEvaluationContext;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.model.IMemoryBlock;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.debug.ui.memory.AbstractMemoryRenderingBindingsProvider;
import org.eclipse.debug.ui.memory.IMemoryRenderingBindingsProvider;
import org.eclipse.debug.ui.memory.IMemoryRenderingManager;
import org.eclipse.debug.ui.memory.IMemoryRenderingType;

/**
 * Represents a renderingBindings element of a memoryRenderings contribution.
 *
 * @since 3.1
 */
class RenderingBindings extends AbstractMemoryRenderingBindingsProvider implements IMemoryRenderingBindingsProvider {

	// element
	protected IConfigurationElement fConfigurationElement;

	// cached rendering ids
	private IMemoryRenderingType[] fAllTypes;
	private IMemoryRenderingType[] fRenderingTypes;
	private IMemoryRenderingType[] fDefaultTypes;

	// rendering type provider, or null (optional)
	private IMemoryRenderingBindingsProvider fProvider;

	// optional exprssion
	private Expression fExpression;

	// element attribute
	public static final String ATTR_RENDERING_IDS = "renderingIds"; //$NON-NLS-1$
	public static final String ATTR_DEFAULT_IDS = "defaultIds"; //$NON-NLS-1$
	public static final String ATTR_PRIMARY = "primaryId"; //$NON-NLS-1$
	public static final String ATTR_PROVIDER = "class"; //$NON-NLS-1$

	// empty bindings
	private static final IMemoryRenderingType[] EMPTY = new IMemoryRenderingType[0];

	/**
	 * Constructs a bindings element from the given contribution.
	 *
	 * @param element contribution
	 */
	RenderingBindings(IConfigurationElement element) {
		fConfigurationElement = element;
	}

	/**
	 * Returns the non-default bindings specified by this contribution.
	 *
	 * @return the non-default bindings specified by this contribution
	 */
	private IMemoryRenderingType[] getBindings() {
		if (fRenderingTypes == null) {
			String ids = fConfigurationElement.getAttribute(ATTR_RENDERING_IDS);
			List<IMemoryRenderingType> list = new ArrayList<>();
			IMemoryRenderingManager manager = getManager();
			if (ids != null) {
				String[] strings = ids.split(","); //$NON-NLS-1$
				for (String s : strings) {
					String id = s.trim();
					IMemoryRenderingType type = manager.getRenderingType(id);
					if (type != null) {
						list.add(type);
					}
				}
			}
			// remove any default bindings, in case of duplicate specification
			IMemoryRenderingType[] defaultBindings = getDefaultBindings();
			for (IMemoryRenderingType defaultBinding : defaultBindings) {
				list.remove(defaultBinding);
			}
			fRenderingTypes = list.toArray(new IMemoryRenderingType[list.size()]);
		}
		return fRenderingTypes;
	}

	/**
	 * Returns the default bindings specified by this contribution.
	 *
	 * @return the default bindings specified by this contribution
	 */
	private IMemoryRenderingType[] getDefaultBindings() {
		if (fDefaultTypes == null) {
			String ids = fConfigurationElement.getAttribute(ATTR_DEFAULT_IDS);
			List<IMemoryRenderingType> list = new ArrayList<>();
			IMemoryRenderingManager manager = getManager();
			if (ids != null) {
				String[] strings = ids.split(","); //$NON-NLS-1$
				for (String s : strings) {
					String id = s.trim();
					IMemoryRenderingType type = manager.getRenderingType(id);
					if (type != null) {
						list.add(type);
					}
				}
			}
			// the primary is also considered a default rendering
			String primaryId = getPrimaryId();
			if (primaryId != null) {
				IMemoryRenderingType type = manager.getRenderingType(primaryId);
				if (type != null) {
					list.add(type);
				}
			}
			fDefaultTypes = list.toArray(new IMemoryRenderingType[list.size()]);
		}
		return fDefaultTypes;
	}

	/**
	 * Returns the primary id, or <code>null</code> if none.
	 *
	 * @return the primary id, or <code>null</code> if none
	 */
	private String getPrimaryId() {
		return fConfigurationElement.getAttribute(ATTR_PRIMARY);
	}

	/**
	 * Returns the provider for this binding or <code>null</code> of none.
	 *
	 * @return the provider for this binding or <code>null</code> of none
	 */
	protected IMemoryRenderingBindingsProvider getProvider(IMemoryBlock memoryBlock) {
		if (isBound(memoryBlock)) {
			if (fProvider == null) {
				String name = fConfigurationElement.getAttribute(ATTR_PROVIDER);
				if (name != null) {
					try {
						fProvider = (IMemoryRenderingBindingsProvider) fConfigurationElement.createExecutableExtension(ATTR_PROVIDER);
					} catch (CoreException e) {
						DebugUIPlugin.log(e);
					}
				}

				if (fProvider != null) {
					fProvider.addListener(this::fireBindingsChanged);
				}
			}
		}
		return fProvider;
	}

	/**
	 * Returns whether this binding is applies to the given memory block.
	 *
	 * @param block memory block
	 * @return whether this binding is applies to the given memory block
	 */
	private boolean isBound(IMemoryBlock block) {
		Expression expression = getExpression();
		if (expression != null) {
			IEvaluationContext context = DebugUIPlugin.createEvaluationContext(block);
			try {
				return expression.evaluate(context) == EvaluationResult.TRUE;
			} catch (CoreException e) {
				DebugUIPlugin.log(e);
				return false;
			}
		}
		return true;
	}

	/**
	 * Validates this contribution.
	 *
	 * @exception CoreException if invalid
	 */
	void validate() throws CoreException {
		if (fConfigurationElement.getAttribute(ATTR_PROVIDER) != null) {
			if (fConfigurationElement.getAttribute(ATTR_RENDERING_IDS) != null || fConfigurationElement.getAttribute(ATTR_DEFAULT_IDS) != null || fConfigurationElement.getAttribute(ATTR_PRIMARY) != null) {
				Status status = new Status(IStatus.ERROR, DebugUIPlugin.getUniqueIdentifier(), IDebugUIConstants.INTERNAL_ERROR, "<renderingBindings> element cannot specify other attributes when " + ATTR_PROVIDER + " is present", null); //$NON-NLS-1$ //$NON-NLS-2$
				throw new CoreException(status);
			}
		}
	}

	/**
	 * Returns this binding's enablement expression, or <code>null</code> if
	 * none.
	 *
	 * @return enablement expression, or <code>null</code> if none
	 */
	private Expression getExpression() {
		if (fExpression == null) {
			IConfigurationElement[] elements = fConfigurationElement.getChildren(ExpressionTagNames.ENABLEMENT);
			IConfigurationElement enablement = elements.length > 0 ? elements[0] : null;
			if (enablement != null) {
				try {
					fExpression = ExpressionConverter.getDefault().perform(enablement);
				} catch (CoreException e) {
					DebugUIPlugin.log(e);
				}
			}
		}
		return fExpression;
	}

	@Override
	public IMemoryRenderingType[] getRenderingTypes(IMemoryBlock block) {
		if (isBound(block)) {
			IMemoryRenderingBindingsProvider provider = getProvider(block);
			if (provider == null) {
				if (fAllTypes == null) {
					IMemoryRenderingType[] defaultBindings = getDefaultBindings();
					IMemoryRenderingType[] bindings = getBindings();
					fAllTypes = new IMemoryRenderingType[defaultBindings.length + bindings.length];
					for (int i = 0; i < defaultBindings.length; i++) {
						fAllTypes[i] = defaultBindings[i];
					}
					for (int i = 0, j = defaultBindings.length; i < bindings.length; i++, j++) {
						fAllTypes[j] = bindings[i];
					}
				}
				return fAllTypes;
			}
			return provider.getRenderingTypes(block);
		}
		return EMPTY;
	}

	@Override
	public IMemoryRenderingType[] getDefaultRenderingTypes(IMemoryBlock block) {
		if (isBound(block)) {
			IMemoryRenderingBindingsProvider provider = getProvider(block);
			if (provider == null) {
				return getDefaultBindings();
			}
			return provider.getDefaultRenderingTypes(block);
		}
		return EMPTY;
	}

	@Override
	public IMemoryRenderingType getPrimaryRenderingType(IMemoryBlock block) {
		if (isBound(block)) {
			IMemoryRenderingBindingsProvider provider = getProvider(block);
			if (provider == null) {
				String primaryId = getPrimaryId();
				if (primaryId != null) {
					return getManager().getRenderingType(primaryId);
				}
			} else {
				return provider.getPrimaryRenderingType(block);
			}
		}
		return null;
	}

	private IMemoryRenderingManager getManager() {
		return DebugUITools.getMemoryRenderingManager();
	}

}
