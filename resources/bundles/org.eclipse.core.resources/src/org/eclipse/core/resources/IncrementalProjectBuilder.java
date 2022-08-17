/*******************************************************************************
 * Copyright (c) 2000, 2022 IBM Corporation and others.
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
 * Anton Leherbauer (Wind River) - [198591] Allow Builder to specify scheduling rule
 * Anton Leherbauer (Wind River) - [305858] Allow Builder to return null rule
 * James Blackburn (Broadcom) - [306822] Provide Context for Builder getRule()
 * Broadcom Corporation - build configurations and references
 *******************************************************************************/
package org.eclipse.core.resources;

import java.util.Collection;
import java.util.Map;
import org.eclipse.core.internal.events.InternalBuilder;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.ISchedulingRule;

/**
 * The abstract base class for all incremental project builders. This class
 * provides the infrastructure for defining a builder and fulfills the contract
 * specified by the <code>org.eclipse.core.resources.builders</code> standard
 * extension point.
 * <p>
 * All builders must subclass this class according to the following guidelines:
 * <ul>
 * <li>must re-implement at least <code>build</code></li>
 * <li>may implement other methods</li>
 * <li>must supply a public, no-argument constructor</li>
 * <li>may implement {@link IIncrementalProjectBuilder2}</li>
 * </ul>
 * On creation, the <code>setInitializationData</code> method is called with
 * any parameter data specified in the declaring plug-in's manifest.
 *
 * @see IIncrementalProjectBuilder2
 */
public abstract class IncrementalProjectBuilder extends InternalBuilder implements IExecutableExtension {
	/**
	 * Build kind constant (value 6) indicating a full build request.  A full
	 * build discards all previously built state and builds all resources again.
	 * Resource deltas are not applicable for this kind of build.
	 * <p>
	 * <strong>Note:</strong> If there is no previous delta, a request for {@link #INCREMENTAL_BUILD}
	 * or {@link #AUTO_BUILD} will result in the builder being called with {@link #FULL_BUILD}
	 * build kind.
	 * </p>
	 *
	 * @see IProject#build(int, IProgressMonitor)
	 * @see IProject#build(int, String, Map, IProgressMonitor)
	 * @see IWorkspace#build(int, IProgressMonitor)
	 */
	public static final int FULL_BUILD = 6;
	/**
	 * Build kind constant (value 9) indicating an automatic build request.  When
	 * autobuild is turned on, these builds are triggered automatically whenever
	 * resources change.  Apart from the method by which autobuilds are triggered,
	 * they otherwise operate like an incremental build.
	 *
	 * @see IWorkspaceDescription#setAutoBuilding(boolean)
	 * @see IWorkspace#isAutoBuilding()
	 */
	public static final int AUTO_BUILD = 9;
	/**
	 * Build kind constant (value 10) indicating an incremental build request.
	 * Incremental builds use an {@link IResourceDelta} that describes what
	 * resources have changed since the last build.  The builder calculates
	 * what resources are affected by the delta, and rebuilds the affected resources.
	 *
	 * @see IProject#build(int, IProgressMonitor)
	 * @see IProject#build(int, String, Map, IProgressMonitor)
	 * @see IWorkspace#build(int, IProgressMonitor)
	 */
	public static final int INCREMENTAL_BUILD = 10;
	/**
	 * Build kind constant (value 15) indicating a clean build request.  A clean
	 * build discards any additional state that has  been computed as a result of
	 * previous builds, and returns the project to a clean slate. Resource
	 * deltas are not applicable for this kind of build.
	 *
	 * @see IProject#build(int, IProgressMonitor)
	 * @see IProject#build(int, String, Map, IProgressMonitor)
	 * @see IWorkspace#build(int, IProgressMonitor)
	 * @see #clean(IProgressMonitor)
	 * @since 3.0
	 */
	public static final int CLEAN_BUILD = 15;

	/**
	 * Runs this builder in the specified manner. Subclasses should implement
	 * this method to do the processing they require.
	 * <p>
	 * If the build kind is {@link #INCREMENTAL_BUILD} or
	 * {@link #AUTO_BUILD}, the <code>getDelta</code> method can be
	 * used during the invocation of this method to obtain information about
	 * what changes have occurred since the last invocation of this method. Any
	 * resource delta acquired is valid only for the duration of the invocation
	 * of this method.  A {@link #FULL_BUILD} has no associated build delta.
	 * </p>
	 * <p>
	 * After completing a build, this builder may return a list of projects for
	 * which it requires a resource delta the next time it is run. This
	 * builder's project is implicitly included and need not be specified. The
	 * build mechanism will attempt to maintain and compute deltas relative to
	 * the identified projects when asked the next time this builder is run.
	 * Builders must re-specify the list of interesting projects every time they
	 * are run as this is not carried forward beyond the next build. Projects
	 * mentioned in return value but which do not exist will be ignored and no
	 * delta will be made available for them.
	 * </p>
	 * <p>
	 * This method is long-running; progress and cancellation are provided by
	 * the given progress monitor. All builders should report their progress and
	 * honor cancel requests in a timely manner. Cancelation requests should be
	 * propagated to the caller by throwing
	 * <code>OperationCanceledException</code>.
	 * </p>
	 * <p>
	 * All builders should try to be robust in the face of trouble. In
	 * situations where failing the build by throwing <code>CoreException</code>
	 * is the only option, a builder has a choice of how best to communicate the
	 * problem back to the caller. One option is to use the
	 * {@link IResourceStatus#BUILD_FAILED} status code along with a suitable message;
	 * another is to use a {@link MultiStatus} containing finer-grained problem
	 * diagnoses.
	 * </p>
	 *
	 * @param kind the kind of build being requested. Valid values are
	 * <ul>
	 * <li>{@link #FULL_BUILD} - indicates a full build.</li>
	 * <li>{@link #INCREMENTAL_BUILD}- indicates an incremental build.</li>
	 * <li>{@link #AUTO_BUILD} - indicates an automatically triggered
	 * incremental build (autobuilding on).</li>
	 * </ul>
	 * @param args a table of builder-specific arguments keyed by argument name
	 * (key type: <code>String</code>, value type: <code>String</code>);
	 * <code>null</code> is equivalent to an empty map
	 * @param monitor a progress monitor, or <code>null</code> if progress
	 * reporting and cancellation are not desired
	 * @return the list of projects for which this builder would like deltas the
	 * next time it is run or <code>null</code> if none
	 * @exception CoreException if this build fails.
	 * @see IProject#build(int, String, Map, IProgressMonitor)
	 */
	@Override
	protected abstract IProject[] build(int kind, Map<String, String> args, IProgressMonitor monitor) throws CoreException;

	/**
	 * Clean is an opportunity for a builder to discard any additional state that has
	 * been computed as a result of previous builds. It is recommended that builders
	 * override this method to delete all derived resources created by previous builds,
	 * and to remove all markers of type {@link IMarker#PROBLEM} that
	 * were created by previous invocations of the builder. The platform will
	 * take care of discarding the builder's last built state (there is no need
	 * to call <code>forgetLastBuiltState</code>).
	 * <p>
	 * This method is called as a result of invocations of
	 * <code>IWorkspace.build</code> or <code>IProject.build</code> where
	 * the build kind is {@link #CLEAN_BUILD}.
	 * <p>
	 * This default implementation does nothing. Subclasses may override.
	 * <p>
	 * This method is long-running; progress and cancellation are provided by
	 * the given progress monitor. All builders should report their progress and
	 * honor cancel requests in a timely manner. Cancelation requests should be
	 * propagated to the caller by throwing
	 * <code>OperationCanceledException</code>.
	 * </p>
	 * <p>
	 * An alternative clean method that passes the build arguments is available in
	 * {@link IIncrementalProjectBuilder2#clean(Map, IProgressMonitor)} that can be
	 * implemented instead of overridding this clean method. This method will not be
	 * called if {@link IIncrementalProjectBuilder2} is implemented.
	 * </p>
	 *
	 * @param monitor a progress monitor, or <code>null</code> if progress
	 * reporting and cancellation are not desired
	 * @exception CoreException if this build fails.
	 * @see IWorkspace#build(int, IProgressMonitor)
	 * @see #CLEAN_BUILD
	 * @see IIncrementalProjectBuilder2#clean(Map, IProgressMonitor)
	 * @since 3.0
	 */
	@Override
	protected void clean(IProgressMonitor monitor) throws CoreException {
		//default implementation does nothing
		//thwart compiler warning
	}

	/**
	 * Requests that this builder forget any state it may be retaining regarding
	 * previously built states. Typically this means that the next time the
	 * builder runs, it will have to do a full build since it does not have any
	 * state upon which to base an incremental build.
	 * This supersedes a call to {@link #rememberLastBuiltState()}.
	 */
	@Override
	public final void forgetLastBuiltState() {
		super.forgetLastBuiltState();
	}

	/**
	 * Requests that this builder remember any build invocation specific state.
	 * This means that the next time the builder runs, it will receive a delta
	 * which includes changes reported in the current {@link #getDelta(IProject)}.
	 *<p>
	 * This can be used to indicate that a builder didn't run, even though there
	 * are changes, and the builder wishes that the delta be preserved until its
	 * next invocation.
	 * </p>
	 * This is superseded by a call to {@link #forgetLastBuiltState()}.
	 * @since 3.7
	 */
	@Override
	public final void rememberLastBuiltState() {
		super.rememberLastBuiltState();
	}

	/**
	 * Returns the build command associated with this builder.  The returned
	 * command may or may not be in the build specification for the project
	 * on which this builder operates.
	 * <p>
	 * Any changes made to the returned command will only take effect if
	 * the modified command is installed on a project build spec.
	 * </p>
	 *
	 * @see IProjectDescription#setBuildSpec(ICommand [])
	 * @see IProject#setDescription(IProjectDescription, int, IProgressMonitor)
	 * @since 3.1
	 */
	@Override
	public final ICommand getCommand() {
		return super.getCommand();
	}

	/**
	 * Returns the resource delta recording the changes in the given project
	 * since the last time this builder was run. <code>null</code> is returned
	 * if no such delta is available. An empty delta is returned if no changes
	 * have occurred, or if deltas are not applicable for the current build kind.
	 * If <code>null</code> is returned, clients should assume
	 * that unspecified changes have occurred and take the appropriate action.
	 * <p>
	 * The system reserves the right to trim old state in an effort to conserve
	 * space. As such, callers should be prepared to receive <code>null</code>
	 * even if they previously requested a delta for a particular project by
	 * returning that project from a <code>build</code> call.
	 * </p>
	 * <p>
	 * A non- <code>null</code> delta will only be supplied for the given
	 * project if either the result returned from the previous
	 * <code>build</code> included the project or the project is the one
	 * associated with this builder.
	 * </p>
	 * <p>
	 * If the given project was mentioned in the previous <code>build</code>
	 * and subsequently deleted, a non- <code>null</code> delta containing the
	 * deletion will be returned. If the given project was mentioned in the
	 * previous <code>build</code> and was subsequently created, the returned
	 * value will be <code>null</code>.
	 * </p>
	 * <p>
	 * A valid delta will be returned only when this method is called during a
	 * build. The delta returned will be valid only for the duration of the
	 * enclosing build execution.
	 * </p>
	 * <p>
	 * The delta does not include changes made while this builder is running.
	 * If {@link #getRule(int, Map)} is overridden to return a scheduling rule other than
	 * the workspace root, changes performed in other threads during the build
	 * will not appear in the resource delta.
	 * </p>
	 *
	 * @return the resource delta for the project or <code>null</code>
	 */
	@Override
	public final IResourceDelta getDelta(IProject project) {
		return super.getDelta(project);
	}

	/**
	 * Returns the project for which this builder is defined.
	 *
	 * @return the project
	 */
	@Override
	public final IProject getProject() {
		return super.getProject();
	}

	/**
	 * Returns the build configuration for which this build was invoked.
	 * @return the build configuration
	 * @since 3.7
	 */
	@Override
	public final IBuildConfiguration getBuildConfig() {
		return super.getBuildConfig();
	}

	/**
	 * Returns whether the given project has already been built during this
	 * build iteration.
	 * <p>
	 * When the entire workspace is being built, the projects are built in
	 * linear sequence. This method can be used to determine if another project
	 * precedes this builder's project in that build sequence. If only a single
	 * project is being built, then there is no build order and this method will
	 * always return <code>false</code>.
	 * </p>
	 *
	 * @param project the project to check against in the current build order
	 * @return <code>true</code> if the given project has been built in this
	 * iteration, and <code>false</code> otherwise.
	 * @see #needRebuild()
	 * @since 2.1
	 */
	@Override
	public final boolean hasBeenBuilt(IProject project) {
		return super.hasBeenBuilt(project);
	}

	/**
	 * Returns whether an interrupt request has been made for this build.
	 * Background autobuild is interrupted when another thread tries to modify
	 * the workspace concurrently with the build thread. When this occurs, the
	 * build cycle is flagged as interrupted and the build will be terminated at
	 * the earliest opportunity. This method allows long running builders to
	 * respond to this interruption in a timely manner. Builders are not
	 * required to respond to interruption requests.
	 *
	 * @return <code>true</code> if the build cycle has been interrupted, and
	 * <code>false</code> otherwise.
	 * @since 3.0
	 */
	@Override
	public final boolean isInterrupted() {
		return super.isInterrupted();
	}

	/**
	 * Indicates that this builder made changes that affect a build configuration
	 * that precedes this build configuration in the currently executing build
	 * order, and thus a rebuild will be necessary.
	 * <p>
	 * <b>Note:</b> this method will schedule rebuild for all projects involved in
	 * the current build cycle!
	 * </p>
	 * <ul>
	 * <li>If concrete projects that require rebuild are known, it is better to use
	 * {@link #requestProjectsRebuild(Collection)} instead to avoid overhead.</li>
	 * <li>If only one project should be rebuilt, it is better to use
	 * {@link #requestProjectRebuild(boolean)} where additionally one could avoid
	 * extra work by skipping not yet executed builders.</li>
	 * </ul>
	 * <p>
	 * This is an advanced feature that builders should use with caution. This can
	 * cause workspace builds to iterate until no more builders require rebuilds.
	 * </p>
	 *
	 * @see #hasBeenBuilt(IProject)
	 * @see #requestProjectsRebuild(Collection)
	 * @see #requestProjectRebuild(boolean)
	 * @since 2.1
	 */
	@Override
	public final void needRebuild() {
		super.needRebuild();
	}

	/**
	 * Indicates that this builder generated or detected new input for currently
	 * running project build, and thus a project rebuild will be necessary in the
	 * current build round.
	 * <p>
	 * The builders configured to run after the current one will be still processed
	 * if {@code processOtherBuilders} is set to {@code true}. To force an immediate
	 * rebuild of a project {@code processOtherBuilders} argument should be set to
	 * {@code false}.
	 * </p>
	 * <p>
	 * <b>Note</b> if {@code processOtherBuilders} is set to {@code false}, the
	 * project that is built with current builder will be only rebuilt again, if
	 * this builder is not the first one configured to run.
	 * </p>
	 * <p>
	 * This is an advanced feature that builders should use with caution. This can
	 * cause workspace builds to iterate until no more builders require rebuilds.
	 * </p>
	 *
	 * @param processOtherBuilders to continue building project with other builders
	 *                             and not start project build from beginning
	 *                             immediately
	 *
	 * @see #needRebuild()
	 * @see #requestProjectsRebuild(Collection)
	 * @since 3.17
	 */
	@Override
	public final void requestProjectRebuild(boolean processOtherBuilders) {
		super.requestProjectRebuild(processOtherBuilders);
	}

	/**
	 * Indicates that this builder generated or detected new input for given
	 * projects and thus a rebuild for given projects will be necessary in the next
	 * build round.
	 * <p>
	 * <b>Note</b> if the given collection contains the current project (that is
	 * currently built with current builder), the current project will be not
	 * rebuilt immediately, but scheduled for rebuild on next round. To perform
	 * immediate rebuild of the current project, use
	 * {@link #requestProjectRebuild(boolean)}.
	 * </p>
	 * <p>
	 * This is an advanced feature that builders should use with caution. This can
	 * cause workspace builds to iterate until no more builders require rebuilds.
	 * </p>
	 *
	 * @see #needRebuild()
	 * @see #requestProjectRebuild(boolean)
	 * @since 3.17
	 */
	@Override
	public final void requestProjectsRebuild(Collection<IProject> projects) {
		super.requestProjectsRebuild(projects);
	}

	/**
	 * Sets initialization data for this builder.
	 * <p>
	 * This method is part of the {@link IExecutableExtension} interface.
	 * </p>
	 * <p>
	 * Subclasses are free to extend this method to pick up initialization
	 * parameters from the plug-in plug-in manifest (<code>plugin.xml</code>)
	 * file, but should be sure to invoke this method on their superclass.
	 * </p><p>
	 * For example, the following method looks for a boolean-valued parameter
	 * named "trace":
	 * </p>
	 *
	 * <pre>
	 * public void setInitializationData(IConfigurationElement cfig, String propertyName, Object data) throws CoreException {
	 * 	super.setInitializationData(cfig, propertyName, data);
	 * 	if (data instanceof Hashtable) {
	 * 		Hashtable args = (Hashtable) data;
	 * 		String traceValue = (String) args.get(&quot;trace&quot;);
	 * 		TRACING = (traceValue != null &amp;&amp; traceValue.equals(&quot;true&quot;));
	 * 	}
	 * }
	 * </pre>
	 * @throws CoreException if fails.
	 */
	@Override
	public void setInitializationData(IConfigurationElement config, String propertyName, Object data) throws CoreException {
		//default implementation does nothing
		//thwart compiler warning
	}

	/**
	 * Informs this builder that it is being started by the build management
	 * infrastructure. By the time this method is run, the builder's project is
	 * available and <code>setInitializationData</code> has been called. The
	 * default implementation should be called by all overriding methods.
	 *
	 * @see #setInitializationData(IConfigurationElement, String, Object)
	 */
	@Override
	protected void startupOnInitialize() {
		// reserved for future use
	}

	/**
	 * Returns the scheduling rule that is required for building
	 * the project build configuration for which this builder is defined. The default
	 * is the workspace root rule.
	 * <p>
	 * The scheduling rule determines which resources in the workspace are
	 * protected from being modified by other threads while the builder is running. Up until
	 * Eclipse 3.5, the entire workspace was always locked during a build;
	 * since Eclipse 3.6, builders can allow resources outside their scheduling
	 * rule to be modified.
	 * <p>
	 * <strong>Notes:</strong>
	 * </p>
	 * <ul>
	 * <li>
	 * The rule may be <i>relaxed</i> and in some cases let the builder be scheduled in
	 * parallel of any other operation using a rule based on {@link IResource}). A relaxed
	 * rule is a scheduling rule which does not contain the workspace root rule.
	 * </li>
	 * <li>
	 * The rule returned here may have no effect if the build is invoked within the
	 * scope of another operation that locks the entire workspace.
	 * </li>
	 * <li>
	 * If this method returns any rule other than the workspace root,
	 * resources outside of the rule scope can be modified concurrently with the build.
	 * The delta returned by {@link #getDelta(IProject)} for any project
	 * outside the scope of the builder's rule may not contain changes that occurred
	 * concurrently with the build.
	 * </li>
	 * </ul>
	 * <p>
	 * Subclasses may override this method.
	 * </p>
	 * @noreference This method is not intended to be referenced by clients.
	 *
	 * @param kind the kind of build being requested. Valid values include:
	 * <ul>
	 * <li>{@link #FULL_BUILD} - indicates a full build.</li>
	 * <li>{@link #INCREMENTAL_BUILD} - indicates an incremental build.</li>
	 * <li>{@link #AUTO_BUILD} - indicates an automatically triggered
	 * incremental build (autobuilding on).</li>
	 * <li>{@link #CLEAN_BUILD} - indicates a clean request.</li>
	 * </ul>
	 * @param args a table of builder-specific arguments keyed by argument name
	 * (key type: <code>String</code>, value type: <code>String</code>);
	 * <code>null</code> is equivalent to an empty map.
	 * @return a scheduling rule which is contained in the workspace root rule
	 *   or <code>null</code> to indicate that no protection against resource
	 *   modification during the build is needed.
	 *
	 * @since 3.6
	 */
	public ISchedulingRule getRule(int kind, Map<String, String> args) {
		return ResourcesPlugin.getWorkspace().getRoot();
	}

	/**
	 * Get the context for this invocation of the builder. This is only valid
	 * in the context of a call to
	 * {@link #build(int, Map, IProgressMonitor)}
	 *
	 * <p>
	 * This can be used to discover which build configurations are being built before
	 * and after this build configuration.
	 * </p>
	 *
	 * @return the context for the most recent invocation of the builder
	 * @since 3.7
	 */
	@Override
	public final IBuildContext getContext() {
		return super.getContext();
	}
}
