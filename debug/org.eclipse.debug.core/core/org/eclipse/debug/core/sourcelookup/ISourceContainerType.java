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
package org.eclipse.debug.core.sourcelookup;



/**
 * A source container type represents a kind of container of source code. For
 * example, a source container type may be a project or a directory. A specific
 * project or directory is represented by an instance of a source container
 * type, which is called a source container (<code>ISourceContainer</code>).
 * <p>
 * A source container type is contributed via the
 * <code>sourceContainerTypes</code> extension point, providing a delegate to
 * the work specific to the contributed type. Following is an example
 * contribution.
 * </p>
 *
 * <pre>
 * &lt;extension point=&quot;org.eclipse.debug.core.sourceContainerTypes&quot;&gt;
 * 	&lt;sourceContainerType
 * 		name=&quot;Project&quot;
 * 		class=&quot;org.eclipse.debug.internal.core.sourcelookup.containers.ProjectSourceContainerType&quot;
 * 		id=&quot;org.eclipse.debug.core.containerType.project&quot;
 * 		description=&quot;A project in the workspace&quot;&gt;
 * 	&lt;/sourceContainerType&gt;
 * &lt;/extension&gt;
 * </pre>
 * <p>
 * Clients contributing a source container type implement
 * {@link org.eclipse.debug.core.sourcelookup.ISourceContainerTypeDelegate}.
 * </p>
 *
 * @see org.eclipse.debug.core.sourcelookup.ISourceContainer
 * @see org.eclipse.debug.core.sourcelookup.ISourceContainerTypeDelegate
 * @since 3.0
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 */
public interface ISourceContainerType extends ISourceContainerTypeDelegate {

	/**
	 * Returns the name of this source container type that can be used for
	 * presentation purposes. For example, <code>Working Set</code> or
	 * <code>Project</code>.  The value returned is
	 * identical to the name specified in plugin.xml by the <code>name</code>
	 * attribute.
	 *
	 * @return the name of this source container type
	 */
	String getName();

	/**
	 * Returns the unique identifier associated with this source container type.
	 * The value returned is identical to the identifier specified in plugin.xml by
	 * the <code>id</code> attribute.
	 *
	 * @return the unique identifier associated with this source container type
	 */
	String getId();

	/**
	 * Returns a short description of this source container type that can be used
	 * for presentation purposes, or <code>null</code> if none.
	 *
	 * @return a short description of this source container type, or <code>null</code>
	 */
	String getDescription();

}
