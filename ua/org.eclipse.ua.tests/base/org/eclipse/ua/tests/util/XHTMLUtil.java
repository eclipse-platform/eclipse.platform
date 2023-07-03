/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
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
package org.eclipse.ua.tests.util;

/**
 * A utility class for working with XHTML.
 */
public class XHTMLUtil {

	/*
	 * Some of the XHTML content is environment-specific. This means it changes
	 * depending on the test machine, location on filesystem, etc. This content
	 * is not important for this test so just strip it out before comparing the
	 * serializations.
	 */
	public static String removeEnvironmentSpecificContent(String xhtml) {
		/*
		 * Remove the space between the last attribute value of a tag and
		 * the closing part "/>". The Sun 1.5 JDK's XML DOM serializer will
		 * always remove it, and 1.4 will always add it, so need to pick one
		 * and adapt all documents to it.
		 *
		 * For example:
		 * <myElement myAttribute="myValue" />
		 * becomes:
		 * <myElement myAttribute="myValue"/>
		 */
		xhtml = xhtml.replaceAll(" />", "/>");

		/*
		 * The base tag is added before showing in browser. It contains an
		 * absolute path in filesystem.
		 */
		xhtml = xhtml.replaceAll("<base href=\".*\"/>", "");

		/*
		 * The order of the params for the meta tag comes out differently on
		 * different platforms. I'm not sure why, and why just this tag. We
		 * don't care about this one for our tests anyway, so just strip it.
		 */
		xhtml = xhtml.replaceAll("<meta .*/>", "");
		return xhtml;
	}
}
