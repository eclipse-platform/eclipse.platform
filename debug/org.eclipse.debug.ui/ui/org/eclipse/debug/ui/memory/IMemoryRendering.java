/*******************************************************************************
 * Copyright (c) 2004, 2011 IBM Corporation and others.
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
package org.eclipse.debug.ui.memory;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.debug.core.model.IMemoryBlock;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/**
 * An arbitrary rendering of a memory block. A memory rendering is contributed
 * via the <code>memoryRenderings</code> extension point.
 * <p>
 * Following is an example definition of a memory renderings extension.
 * </p>
 *
 * <pre>
 * &lt;extension point="org.eclipse.debug.ui.memoryRenderings"&gt;
 *   &lt;memoryRenderingType
 *      id="com.example.asciiRendering"
 *      name="ASCII"
 *      class="com.example.RenderingFactory"&gt;
 *   &lt;/memoryRenderingType&gt;
 * &lt;/extension&gt;
 * </pre>
 *
 * The attributes are specified as follows:
 * <ul>
 * <li><code>id</code> specifies a unique identifier for a type of memory
 * rendering</li>
 * <li><code>name</code> specifies a human readable label for a rendering
 * type</li>
 * <li><code>class</code> specifies the fully qualified name of the Java class
 * that implements <code>IMemoryRenderingTypeDelegate</code>. Renderings are
 * created via this factory.</li>
 * </ul>
 * <p>
 * A rendering provides an image and label. To support dynamic labels and
 * images, property change notification is used with the following property
 * constants defined in <code>IBasicPropertyConstants</code>:
 * </p>
 * <ul>
 * <li><code>P_TEXT</code> - indicates a label change</li>
 * <li><code>P_IMAGE</code> - indicates a image change</li>
 * </ul>
 * <p>
 * Renderings needing to synchronize with other renderings are intended to use
 * property change notifications via its synchronization service. For example,
 * when a rendering becomes visible, it can register for property change events
 * with its rendering site's synchronization service, and when it becomes hidden
 * it can unregister. When a rendering is activated, it should set itself as the
 * synchrnoization provider in its rendering site and fire property change
 * events to communicate information to interested listeners.
 * </p>
 * <p>
 * Clients contributing a memory rendering type are intended to implement this
 * interface and <code>IMemoryRenderingTypeDelegate</code>. The factory will be
 * used to create instances of <code>IMemoryRendering</code>.
 * </p>
 *
 * @since 3.1
 */
public interface IMemoryRendering extends IAdaptable{

	/**
	 * Initializes this rendering to be hosted in the given container, displaying
	 * the given memory block. This method is called before this rendering's control
	 * has been created.
	 *
	 * @param container container hosting this rendering
	 * @param block the memory block to render
	 */
	void init(IMemoryRenderingContainer container, IMemoryBlock block);

	/**
	 * Creates the top level control for this rendering under the given parent composite.
	 * This method is called after this rendering's <code>init</code> method has been
	 * called.
	 * <p>
	 * Implementors are responsible for ensuring that
	 * the created control can be accessed via <code>getControl</code>
	 * </p>
	 * @param parent the parent composite
	 * @return the new top level control
	 */
	Control createControl(Composite parent);

	/**
	 * Returns the top level control for this rendering.
	 * <p>
	 * May return <code>null</code> if the control
	 * has not been created yet.
	 * </p>
	 * @return the top level control or <code>null</code>
	 */
	Control getControl();

	/**
	 * Disposes this rendering.
	 */
	void dispose();

	/**
	 * Notification this rendering has become the active rendering. Only one
	 * rendering can be active at once. Generally, the active rendering is
	 * visible and has focus.
	 */
	void activated();

	/**
	 * Notification this rendering is no longer the active rendering.
	 */
	void deactivated();

	/**
	 * Notification this rendering has become visible in its container.
	 * Note that a rendering does not have to be active to be visible.
	 */
	void becomesVisible();

	/**
	 * Notification this rendering has become hidden in its container.
	 */
	void becomesHidden();

	/**
	 * Returns the memory block displayed by this rendering.
	 *
	 * @return the memory block displayed by this rendering
	 */
	IMemoryBlock getMemoryBlock();

	/**
	 * Returns the identifier associated with this rendering's type.
	 *
	 * @return the identifier associated with this rendering's type
	 * @see IMemoryRenderingType
	 */
	String getRenderingId();

	/**
	 * Adds a listener for property changes to this rendering.
	 * Has no effect if an identical listener is already registered.
	 *
	 * @param listener a property change listener
	 */
	void addPropertyChangeListener(IPropertyChangeListener listener);

	/**
	 * Removes the given property change listener from this rendering.
	 * Has no effect if the identical listener is not registered.
	 *
	 * @param listener a property change listener
	 */
	void removePropertyChangeListener(IPropertyChangeListener listener);

	/**
	 * Returns an image for this rendering. Clients should not dispose
	 * this image. This rendering will dispose the image if required when
	 * this rendering is disposed.
	 *
	 * @return an image for this rendering
	 */
	Image getImage();

	/**
	 * Returns a label for this rendering.
	 *
	 * @return a label for this rendering
	 */
	String getLabel();
}
