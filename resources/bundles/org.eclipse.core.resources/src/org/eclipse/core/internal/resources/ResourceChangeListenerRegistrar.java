/*******************************************************************************
 * Copyright (c) 2020 Alex Blewitt and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Alex Blewitt - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.internal.resources;

import java.util.Map;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.osgi.service.debug.DebugOptionsListener;
import org.osgi.service.log.Logger;
import org.osgi.service.log.LoggerFactory;

/**
 * Provides automatic registration of {@link IResourceChangeListener} instances
 * with {@link IWorkspace} instances.
 *
 * <p>
 * This allows clients to register their {@link IResourceChangeListener}
 * instances as services, and be called back when changes occur, in the same way
 * that (for example) {@link DebugOptionsListener} is used to receive callbacks.
 * </p>
 */
public final class ResourceChangeListenerRegistrar {

	private final IWorkspace workspace;
	private Logger logger;

	/**
	 * Create an ResourceChangeListenerRegistrar with the given workspace.
	 *
	 * @param workspace the workspace to associate listeners with
	 */
	public ResourceChangeListenerRegistrar(IWorkspace workspace) {
		this.workspace = workspace;
	}

	/**
	 * Register the {@link IResourceChangeListener} with the associated workspace
	 *
	 * @param listener   the listener to register
	 * @param properties the properties, including <code>event.mask</code> if
	 *                   required
	 *                   {@link IWorkspace#addResourceChangeListener(IResourceChangeListener, int)}
	 */
	public void addResourceChangeListener(IResourceChangeListener listener, Map<String, Object> properties) {
		// TODO Add as public API https://bugs.eclipse.org/bugs/show_bug.cgi?id=564985
		Object mask = properties.get(IResourceChangeListener.PROPERTY_EVENT_MASK);
		if (mask instanceof Integer) {
			workspace.addResourceChangeListener(listener, ((Integer) mask).intValue());
		} else {
			Logger local = this.logger;
			if (mask != null && local != null) {
				local.warn("event.mask of IResourceChangeListener service: expected Integer but was {} (from {}): {}", //$NON-NLS-1$
						mask.getClass(), listener.getClass(), mask);
			}
			workspace.addResourceChangeListener(listener);
		}
	}

	/**
	 * Deregister the {@link IResourceChangeListener} from this workspace
	 *
	 * @param listener the listener to deregister
	 */
	public void removeResourceChangeListener(IResourceChangeListener listener) {
		workspace.removeResourceChangeListener(listener);
	}

	/**
	 * Set the logger factory that can be used by this component
	 *
	 * @param factory the factory
	 */
	public void setLoggerFactory(LoggerFactory factory) {
		this.logger = factory.getLogger(ResourceChangeListenerRegistrar.class);
	}

	/**
	 * Unsets the logger generated from the associated logger factory
	 */
	public void unsetLogger() {
		this.logger = null;
	}
}
