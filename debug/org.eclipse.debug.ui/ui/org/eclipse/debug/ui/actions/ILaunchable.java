/*******************************************************************************
 * Copyright (c) 2000, 2012 IBM Corporation and others.
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
package org.eclipse.debug.ui.actions;

/**
 * Interface used to tag objects as launchable. Objects that provide an adapter
 * of this type will be considered by the contextual launch support.
 * <p>
 * The debug platform uses the {@link ILaunchable} interface as a tag for
 * objects that can be launched via the context menu 'Run As / Debug As' actions
 * and run/debug tool bar buttons. The platform checks if an {@link ILaunchable}
 * adapter is available from the selection/active editor to determine if it
 * should be considered for launching. However, the platform never actually
 * retrieves the adapter or calls any methods on it (the interface is, in fact,
 * empty).
 * </p>
 * <p>
 * The debug platform performs the following test:
 * </p>
 * <ul>
 * <li>
 * <code>Platform.getAdapterManager().hasAdapter(X, ILaunchable.class.getName());</code>
 * </li>
 * </ul>
 * <p>
 * Thus objects that can be launched need to register {@link ILaunchable}
 * adapters, but don't have to provide implementations. There is also no harm in
 * implementing the interface or providing the adapter. For example, JDT
 * contributes an adapter as follows. Although no adapter is actually provided
 * the platform will answer <code>true</code> to <code>hasAdapter(...)</code>.
 * </p>
 *
 * <pre>
 * &lt;extension point="org.eclipse.core.runtime.adapters"&gt;
 *    &lt;factory
 *       class=""
 *       adaptableType="org.eclipse.jdt.core.IJavaElement"&gt;
 *       &lt;adapter type="org.eclipse.debug.ui.actions.ILaunchable"/&gt;
 *   &lt;/factory&gt;
 * &lt;/extension&gt;
 * </pre>
 * <p>
 * Clients may contribute an adapter of this type for launchable objects via the
 * <code>org.eclipse.core.runtime.adapters</code> extension point. A factory and
 * implementation of this interface are not actually required.
 * </p>
 * <p>
 * Clients that need to check for presence of launchable adapter using standard
 * expression mechanism, should use a property tester as follows:
 * </p>
 *
 * <pre>
 * &lt;with variable="selection"&gt;
 *    &lt;iterator operator="and" ifEmpty="false"&gt;
 *       &lt;test property="org.eclipse.debug.core.launchable" value="debug"/&gt;
 *    &lt;/iterator&gt;
 * &lt;/with&gt;
 * </pre>
 *
 * @see org.eclipse.debug.ui.actions.ContextualLaunchAction
 * @since 3.0
 */
public interface ILaunchable {

}
