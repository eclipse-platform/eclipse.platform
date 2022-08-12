/*******************************************************************************
 *  Copyright (c) 2000, 2010 IBM Corporation and others.
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
 *******************************************************************************/
package org.eclipse.core.resources;

import java.util.Map;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * A builder command names a builder and supplies a table of
 * name-value argument pairs.
 * <p>
 * Changes to a command will only take effect if the modified command is installed
 * into a project description via {@link IProjectDescription#setBuildSpec(ICommand[])}.
 * </p>
 *
 * @see IProjectDescription
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 */
public interface ICommand {

	/**
	 * Returns a table of the arguments for this command, or <code>null</code>
	 * if there are no arguments. The argument names and values are both strings.
	 *
	 * @return a table of command arguments (key type : <code>String</code>
	 *		value type : <code>String</code>), or <code>null</code>
	 * @see #setArguments(Map)
	 */
	Map<String, String> getArguments();

	/**
	 * Returns the name of the builder to run for this command, or
	 * <code>null</code> if the name has not been set.
	 *
	 * @return the name of the builder, or <code>null</code> if not set
	 * @see #setBuilderName(String)
	 */
	String getBuilderName();

	/**
	 * Returns whether this build command responds to the given kind of build.
	 * <p>
	 * By default, build commands respond to all kinds of builds.
	 * </p>
	 *
	 * @param kind One of the <code>*_BUILD</code> constants defined
	 * on <code>IncrementalProjectBuilder</code>
	 * @return <code>true</code> if this build command responds to the specified
	 * kind of build, and <code>false</code> otherwise.
	 * @see #setBuilding(int, boolean)
	 * @since 3.1
	 */
	boolean isBuilding(int kind);

	/**
	 * Returns whether this command allows configuring of what kinds of builds
	 * it responds to.  By default, commands are only configurable
	 * if the corresponding builder defines the {@link #isConfigurable}
	 * attribute in its builder extension declaration. A command that is not
	 * configurable will always respond to all kinds of builds.
	 *
	 * @return <code>true</code> If this command allows configuration of
	 * 		what kinds of builds it responds to, and <code>false</code> otherwise.
	 * @see #setBuilding(int, boolean)
	 * @since 3.1
	 */
	boolean isConfigurable();

	/**
	 * Sets this command's arguments to be the given table of name-values
	 * pairs, or to <code>null</code> if there are no arguments. The argument
	 * names and values are both strings.
	 * <p>
	 * Individual builders specify their argument expectations.
	 * </p>
	 * <p>
	 * Note that modifications to the arguments of a command
	 * being used in a running builder may affect the run of that builder
	 * but will not affect any subsequent runs.  To change a command
	 * permanently you must install the command into the relevant project
	 * build spec using {@link IProjectDescription#setBuildSpec(ICommand[])}.
	 * </p>
	 *
	 * @param args a table of command arguments (keys and values must
	 *   both be of type <code>String</code>), or <code>null</code>
	 * @see #getArguments()
	 */
	void setArguments(Map<String, String> args);

	/**
	 * Sets the name of the builder to run for this command.
	 * <p>
	 * The builder name comes from the extension that plugs in
	 * to the standard <code>org.eclipse.core.resources.builders</code>
	 * extension point.
	 * </p>
	 *
	 * @param builderName the name of the builder
	 * @see #getBuilderName()
	 */
	void setBuilderName(String builderName);

	/**
	 * Specifies whether this build command responds to the provided kind of build.
	 * <p>
	 * When a command is configured to not respond to a given kind of build, the
	 * builder instance will not be called when a build of that kind is initiated.
	 * </p><p>
	 * This method has no effect if this build command does not allow its
	 * build kinds to be configured.
	 * </p><p>
	 * <strong>Note:</strong>
	 * <ul>
	 * <li>A request for INCREMENTAL_BUILD or AUTO_BUILD will result in the builder being called with the FULL_BUILD
	 * kind, if there is no previous delta (e.g. after a clean build).
	 * </li><li>
	 * If INCREMENTAL_BUILD (or AUTO_BUILD) is promoted to FULL_BUILD, the builder will be called,
	 * if the command responds to INCREMENTAL_BUILD (or AUTO_BUILD).
	 * </li><li>
	 * If INCREMENTAL_BUILD is promoted to FULL_BUILD, the builder will be called,
	 * if the command responds to FULL_BUILD.
	 * </li><li>
	 * If AUTO_BUILD is promoted to FULL_BUILD, the builder will be called,
	 * <strong>only if</strong> the command responds to AUTO_BUILD.
	 * </li>
	 * </ul>
	 *
	 * @param kind One of the <code>*_BUILD</code> constants defined
	 * 		on <code>IncrementalProjectBuilder</code>
	 * @param value <code>true</code> if this build command responds to the
	 * 		specified kind of build, and <code>false</code> otherwise.
	 * @see #isBuilding(int)
	 * @see #isConfigurable()
	 * @see IWorkspace#build(int, IProgressMonitor)
	 * @see IProject#build(int, IProgressMonitor)
	 * @see IncrementalProjectBuilder#FULL_BUILD
	 * @see IncrementalProjectBuilder#INCREMENTAL_BUILD
	 * @see IncrementalProjectBuilder#AUTO_BUILD
	 * @see IncrementalProjectBuilder#CLEAN_BUILD
	 * @since 3.1
	 */
	void setBuilding(int kind, boolean value);
}
