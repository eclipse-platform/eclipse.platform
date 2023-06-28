/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
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
package org.eclipse.debug.ui;


import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorPart;

/**
 * A launch shortcut is capable of launching a selection or active editor in the
 * workbench. The delegate is responsible for interpreting the selection or
 * active editor (if it applies), and launching an application. This may require
 * creating a new launch configuration with default values, or re-using an
 * existing launch configuration.
 * <p>
 * A launch shortcut is defined as an extension of type
 * <code>org.eclipse.debug.ui.launchShortcuts</code>. A shortcut specifies the
 * perspectives in which is should be available from the "Run/Debug" cascade
 * menus.
 * </p>
 * <p>
 * A launch shortcut extension is defined in <code>plugin.xml</code>. Following
 * is an example definition of a launch shortcut extension.
 * </p>
 *
 * <pre>
 * &lt;extension point="org.eclipse.debug.ui.launchShortcuts"&gt;
 *    &lt;shortcut
 *           class="org.eclipse.jdt.internal.debug.ui.launcher.JavaApplicationLaunchShortcut"
 *           description="%JavaLaunchShortcut.description"
 *           helpContextId="org.eclipse.jdt.debug.ui.shortcut_local_java_application"
 *           icon="$nl$/icons/full/etool16/java_app.png"
 *           id="org.eclipse.jdt.debug.ui.localJavaShortcut"
 *           label="%JavaApplicationShortcut.label"
 *           modes="run, debug"&gt;
 *       &lt;contextualLaunch&gt;
 *         &lt;enablement&gt;
 *           &lt;with variable="selection"&gt;
 *             &lt;count value="1"/&gt;
 *              &lt;iterate&gt;
 *               &lt;and&gt;
 *                &lt;adapt type="org.eclipse.jdt.core.IJavaElement"&gt;
 *                	&lt;test property="org.eclipse.jdt.core.isInJavaProject"/&gt;
 *                &lt;/adapt&gt;
 *              	 &lt;or&gt;
 *              	   &lt;test property="org.eclipse.jdt.launching.hasMain"/&gt;
 *              	   &lt;test property="org.eclipse.jdt.launching.isContainer"/&gt;
 *              	   &lt;test property="org.eclipse.jdt.launching.isPackageFragment"/&gt;
 *              	   &lt;test property="org.eclipse.jdt.launching.isPackageFragmentRoot"/&gt;
 *              	 &lt;/or&gt;
 *               &lt;/and&gt;
 *              &lt;/iterate&gt;
 *             &lt;/with&gt;
 *         &lt;/enablement&gt;
 * 		&lt;/contextualLaunch&gt;
 *       &lt;configurationType
 *              id="org.eclipse.jdt.launching.localJavaApplication"&gt;
 *       &lt;/configurationType&gt;
 *       &lt;description
 *              description="%RunJavaLaunchShortcut.description"
 *              mode="run"&gt;
 *       &lt;/description&gt;
 *       &lt;description
 *              description="%DebugJavaLaunchShortcut.description"
 *              mode="debug"&gt;
 *       &lt;/description&gt;
 *    &lt;/shortcut&gt;
 * &lt;/extension&gt;
 * </pre>
 * <p>
 * The attributes are specified as follows:
 * </p>
 * <ul>
 * <li><code>id</code> specifies a unique identifier for this launch
 * shortcut.</li>
 * <li><code>modes</code> specifies a comma separated list of modes this
 * shortcut supports.</li>
 * <li><code>class</code> specifies a fully qualified name of a Java class that
 * implements <code>ILaunchShortcut</code>.</li>
 * <li><code>label</code> specifies a label used to render this shortcut.</li>
 * <li><code>icon</code> specifies a plug-in relative path to an icon used to
 * render this shortcut.</li>
 * <li><code>category</code> specifies the launch configuration type category
 * this shortcut is applicable for. When unspecified, the category is
 * <code>null</code> (default).</li>
 * <li><code>path</code> an optional menu path used to group launch shortcuts in
 * menus. Launch shortcuts are grouped alphabetically based on the
 * <code>path</code> attribute, and then sorted alphabetically within groups
 * based on the <code>label</code> attribute. When unspecified, a shortcut
 * appears in the last group. This attribute was added in the 3.0.1
 * release.</li>
 * <li><code>helpContextId</code> optional attribute specifying the help context
 * identifier to associate with the launch shortcut action in a menu.</li>
 * <li><code>description</code> Provides a human readable description of what
 * the shortcut does (or will do) if the user selects it. A Description provided
 * in this field will apply as the default description for all of the modes
 * listed in the modes attribute.</li>
 * <li><code>perspective</code> has been <b>deprecated</b> in the 3.1 release.
 * The top level Run/Debug/Profile cascade menus now support contextual
 * (selection sensitive) launching, and clients should provide a
 * <code>contextualLaunch</code> element instead.</li>
 * <li><code>contextualLaunch</code> holds all descriptions for adding shortcuts
 * to the selection sensitive Run/Debug/Profile cascade menus. Only objects that
 * provide an <code>org.eclipse.debug.ui.actions.ILaunchable</code> adapter are
 * considered for the cascade menus. The
 * <code>org.eclipse.debug.ui.actions.ILaunchable</code> interface is simply
 * used to tag objects that support contextual launching.</li>
 * <li><code>contextLabel</code> zero or more context menu labels. For shortcuts
 * that pass their filter tests, the specified label will appear in the "Run
 * -&gt;" context menu and be bound to a launch action of the specified mode
 * (e.g. run,debug,profile).</li>
 * <li><code>configurationType</code> allows more that one associated launch
 * configuration type to be specified for this launch shortcut. That way
 * consumers of launch shortcut information can know what kinds of launch
 * configurations your short is associated with/creates</li>
 * <li><code>description</code> allows a mode specific description(s) to be
 * provided for this launch shortcut.</li>
 * </ul>
 * <p>
 * <br>
 * Clients contributing a launch shortcut are intended to implement this
 * interface.
 * </p>
 *
 * @since 2.0
 */
public interface ILaunchShortcut {

	/**
	 * Locates a launchable entity in the given selection and launches
	 * an application in the specified mode. This launch configuration
	 * shortcut is responsible for progress reporting as well
	 * as error handling, in the event that a launchable entity cannot
	 * be found, or launching fails.
	 *
	 * @param selection workbench selection
	 * @param mode one of the launch modes defined by the
	 * 	launch manager
	 * @see org.eclipse.debug.core.ILaunchManager
	 */
	void launch(ISelection selection, String mode);

	/**
	 * Locates a launchable entity in the given active editor, and launches
	 * an application in the specified mode. This launch configuration
	 * shortcut is responsible for progress reporting as well as error
	 * handling, in the event that a launchable entity cannot be found,
	 * or launching fails.
	 *
	 * @param editor the active editor in the workbench
	 * @param mode one of the launch modes defined by the launch
	 * 		manager
	 * @see org.eclipse.debug.core.ILaunchManager
	 */
	void launch(IEditorPart editor, String mode);
}
