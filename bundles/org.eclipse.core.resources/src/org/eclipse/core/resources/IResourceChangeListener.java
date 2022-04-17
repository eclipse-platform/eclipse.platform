/*******************************************************************************
 *  Copyright (c) 2000, 2017 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Lars Vogel <Lars.Vogel@vogella.com> - Bug 527657
 *******************************************************************************/
package org.eclipse.core.resources;

import java.util.*;

/**
 * A resource change listener is notified of changes to resources in the
 * workspace. These changes arise from direct manipulation of resources, or
 * indirectly through re-synchronization with the local file system.
 * <p>
 * Clients may implement this interface.
 * </p>
 *
 * There are two ways to register a listener:
 * <ol>
 * <li>One could direct registration with
 * IWorkspace#addResourceChangeListener(IResourceChangeListener, int) users
 * should note that they are responsible to remove the listener if no longer
 * needed to prevent memory leaks.</li>
 * <li>One could register an OSGi Service making and it will automatically be
 * picked up leveraging the <a href=
 * "https://enroute.osgi.org/FAQ/400-patterns.html#whiteboard-pattern">Whiteboard
 * Pattern</a>. Services registered with an {@link #PROPERTY_EVENT_MASK}
 * property can be used to receive a sub-set of the events, by registering the
 * value with the
 * {@link IWorkspace#addResourceChangeListener(IResourceChangeListener, int)}
 * method. This allows (for example) {@link IResourceChangeEvent#POST_CHANGE}
 * events to be received by setting <code>event.mask=1</code> in the service
 * registration.</li>
 * </ol>
 * <p>
 * For example the services can be registered with Declarative Services, which
 * allows a bundle to not require that the Workspace bundle be started prior to
 * accessing the resources, as until the IWorkspace is available the bundle will
 * not need any callbacks. This will also save potential NPEs when the
 * {@link IWorkspace} shuts down, because the OSGi runtime will handle the
 * deregistration of services automatically:
 * </p>
 *
 * <pre>
&lt;?xml version="1.0" encoding="UTF-8"?&gt;
&lt;scr:component xmlns:scr="http://www.osgi.org/xmlns/scr/v1.4.0" immediate="true" name="ExampleResourceListener"&gt;
   &lt;implementation class="org.example.ExampleResourceListener"/&gt;
   &lt;service&gt;
      &lt;provide interface="org.eclipse.core.resources.IResourceChangeListener"/&gt;
   &lt;/service&gt;
   &lt;!-- 1 == IResourceChangeEvent.POST_CHANGE --&gt;
   &lt;property name="event.mask" type="Integer" value="1"/&gt;
&lt;/scr:component&gt;
 * </pre>
 *
 * <p>
 * If you choose to register it with the core OSGi API (e.g. in an activator)
 * you can use the following pattern:
 * </p>
 *
 * <pre>
 * bundleContext.registerService(IResourceChangeListener.class, myListener, IResourceChangeListener.getMaskProperties(
 * 		IResourceChangeEvent.POST_CHANGE | IResourceChangeEvent.PRE_CLOSE | IResourceChangeEvent.PRE_DELETE));
 * </pre>
 *
 *
 *
 * @see IResourceDelta
 */

@FunctionalInterface
public interface IResourceChangeListener extends EventListener {

	/**
	 * @since 3.17
	 */
	String PROPERTY_EVENT_MASK = "event.mask"; //$NON-NLS-1$

	/**
	 * Notifies this listener that some resource changes
	 * are happening, or have already happened.
	 * <p>
	 * The supplied event gives details. This event object (and the
	 * resource delta within it) is valid only for the duration of
	 * the invocation of this method.
	 * </p>
	 * <p>
	 * Note: This method is called by the platform; it is not intended
	 * to be called directly by clients.
	 * <p>
	 * Note that during resource change event notification, further changes
	 * to resources may be disallowed.
	 * </p>
	 *
	 * @param event the resource change event
	 * @see IResourceDelta
	 */
	void resourceChanged(IResourceChangeEvent event);

	/**
	 * Creates a {@link Dictionary} suitable to be used when registering a
	 * {@link IResourceChangeListener} as an OSGi service.
	 *
	 * @param mask see
	 *             {@link IWorkspace#addResourceChangeListener(IResourceChangeListener, int)}
	 * @return a new {@link Dictionary} representing the OSGi service properties for
	 *         the given mask
	 * @since 3.17
	 */
	static Dictionary<String, ?> getMaskProperties(int mask) {
		Dictionary<String, Object> properties = new Hashtable<>();
		properties.put(PROPERTY_EVENT_MASK, mask);
		return properties;
	}
}
