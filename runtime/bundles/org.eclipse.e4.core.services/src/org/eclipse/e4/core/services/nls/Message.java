/*******************************************************************************
 * Copyright (c) 2013, 2014 Tom Schindl <tom.schindl@bestsolution.at> and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Tom Schindl <tom.schindl@bestsolution.at> - initial API and implementation
 *     Dirk Fauth <dirk.fauth@googlemail.com - Bug 440444, 440445
 *     Lars Vogel <Lars.Vogel@gmail.com> - Bug 440444, 440445
 *     Markus Keller <markus_keller@ch.ibm.com> - Bug 440445
 *******************************************************************************/
package org.eclipse.e4.core.services.nls;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Annotation for message classes to control
 * <ul>
 * <li>contribution uri to point to resource bundles in a different location</li>
 * <li>caching behavior</li>
 * </ul>
 *
 * <p>
 * <b>ResourceBundle location</b>
 * </p>
 *
 * <p>
 * Via the <code>contributionURI</code> parameter it is possible to specify the location of the
 * resource bundle files. It supports the following location patterns:
 * </p>
 * <ul>
 * <li>
 * <code>platform:/[plugin|fragment]/[Bundle-SymbolicName]</code><br>
 * Load the OSGi resource bundle out of the bundle/fragment named [Bundle-SymbolicName].<br>
 * For example:<br>
 * <code>@Message(contributionURI="platform:/plugin/com.example.e4.translation.extension")</code><br>
 * will load the OSGi resource bundle that is configured in the <code>MANIFEST.MF</code> of the
 * <code>com.example.e4.translation.extension</code> plug-in.</li>
 * <li>
 * <code>platform:/[plugin|fragment]/[Bundle-SymbolicName]/[Path]/[Basename]</code><br>
 * Load the resource bundle specified by [Path] and [Basename] out of the bundle/fragment named
 * [Bundle-SymbolicName].<br>
 * For example:<br>
 * <code>@Message(contributionURI="platform:/plugin/com.example.e4.translation/resources/another")</code>
 * <br>
 * will load the resource bundle that is located in the folder <i>resources/another</i> in the
 * <code>com.example.e4.translation</code> plug-in.</li>
 * <li>
 * <code>bundleclass://[plugin|fragment]/[Bundle-SymbolicName]/[Fully-Qualified-Classname]</code><br>
 * Instantiate the class-based resource bundle specified by [Fully-Qualified-Classname] out of the
 * bundle/fragment named [Bundle-SymbolicName]. Note that the class needs to be a subtype of
 * <code>ResourceBundle</code>.<br>
 * For example:<br>
 * <code>@Message(contributionURI="bundleclass://com.example.e4.translation/com.example.e4.translation.resources.MockBundle")</code>
 * <br>
 * will load the class-based resource bundle <code>MockBundle</code> in package
 * <code>com.example.e4.translation.resources</code> in the <code>com.example.e4.translation</code>
 * plug-in.</li>
 * </ul>
 *
 * <p>
 * <b>Note:</b><br>
 * If the resource bundle files are located in the same package as the messages class having the
 * same base name, or the OSGI resource bundle should be used (by default located in
 * <i>OSGI-INF/l10n</i> with base name <i>bundle</i>) it is not necessary to specify the
 * <code>contributionURI</code> parameter.
 * </p>
 *
 * <p>
 * <b>Caching behavior</b>
 * </p>
 *
 * <p>
 * Via the <code>referenceType</code> parameter it is possible to specify the caching behavior of
 * message class instances.
 * </p>
 * <ul>
 * <li>
 * <code>ReferenceType.NONE</code><br>
 * The message instance is not cached. Each requestor gets its own instance.</li>
 * <li>
 * <code>ReferenceType.WEAK</code><br>
 * The message instance is cached as a weak reference. If every requestor was garbage collected, the
 * message instance is also discarded at the next garbage collection cycle.</li>
 * <li>
 * <code>ReferenceType.SOFT</code><br>
 * The message instance is cached as a soft reference. If every requestor was garbage collected, the
 * message instance is not immediately discarded with the next garbage collection cycle, but will be
 * retained for a while in memory. <b>This is the default configuration!</b></li>
 * </ul>
 *
 * <p>
 * <b>Examples:</b>
 * </p>
 *
 * <p>
 * <b>Loading through a dedicated class</b>
 * </p>
 *
 * <pre>
 * &#064;Message(contributionURI = &quot;bundleclass://mybundle/my.ResourceBundleClass&quot;)
 * public class ResourceBundleClassMessages {
 * 	public String message_1;
 * }
 * </pre>
 *
 * <p>
 * <b>No caching</b>
 * </p>
 *
 * <pre>
 * &#064;Message(referenceType = ReferenceType.NONE)
 * public class ResourceBundleClassMessages {
 * 	public String message_1;
 * }
 * </pre>
 *
 * <p>
 * <b>Loading through a dedicated class with weak reference type</b>
 * </p>
 *
 * <pre>
 * &#064;Message(contributionURI = &quot;bundleclass://mybundle/my.ResourceBundleClass&quot;, referenceType = ReferenceType.WEAK)
 * public class ResourceBundleClassMessages {
 * 	public String message_1;
 * }
 * </pre>
 *
 * @since 1.2
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface Message {
	/**
	 * Caching behaviors.
	 *
	 * @see Message
	 */
	public enum ReferenceType {
		NONE, SOFT, WEAK
	}

	/**
	 * @return optional caching behavior
	 * @see Message
	 */
	ReferenceType referenceType() default ReferenceType.SOFT;

	/**
	 * @deprecated Use contributionURI instead
	 */
	@Deprecated
	String contributorURI() default "";

	/**
	 * @return optional parameter to point to resource bundles in a different location
	 * @see Message
	 * @since 2.0
	 */
	String contributionURI() default "";
}
