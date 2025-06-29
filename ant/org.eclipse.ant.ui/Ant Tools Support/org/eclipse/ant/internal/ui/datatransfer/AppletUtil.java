/*******************************************************************************
 * Copyright (c) 2004, 2013 Richard Hoefter and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Richard Hoefter (richard.hoefter@web.de) - initial API and implementation
 *     IBM Corporation - nlsing and incorporating into Eclipse
 *******************************************************************************/

package org.eclipse.ant.internal.ui.datatransfer;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.ant.internal.core.IAntCoreConstants;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.launching.AbstractJavaLaunchConfigurationDelegate;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;

/**
 * Helper class to access private method <code>org.eclipse.jdt.internal.launching.JavaAppletLaunchConfigurationDelegate}.buildHTMLFile()</code>.
 *
 * <p>
 * Source was copied and slightly modified.
 */
public class AppletUtil {
	private AppletUtil() {
	}

	// create instance to access helper method
	private static AbstractJavaLaunchConfigurationDelegate fgDelegate = new AbstractJavaLaunchConfigurationDelegate() {
		@Override
		public void launch(ILaunchConfiguration configuration, String mode, ILaunch launch, IProgressMonitor monitor) throws CoreException {
			// do nothing
		}
	};

	/**
	 * Using the specified launch configuration, build an HTML file that specifies the applet to launch.
	 *
	 * @return the created HTML file
	 */
	public static String buildHTMLFile(ILaunchConfiguration configuration) throws CoreException {
		String name = getMainTypeName(configuration);
		StringBuilder b = new StringBuilder();
		b.append("<!--" + BuildFileCreator.WARNING + " -->" + ExportUtil.NEWLINE); //$NON-NLS-1$ //$NON-NLS-2$
		b.append("<html>" + ExportUtil.NEWLINE); //$NON-NLS-1$
		b.append("    <body>" + ExportUtil.NEWLINE); //$NON-NLS-1$
		b.append("        <applet code="); //$NON-NLS-1$
		b.append(getQuotedString(name + ".class")); //$NON-NLS-1$
		String appletName = configuration.getAttribute(IJavaLaunchConfigurationConstants.ATTR_APPLET_NAME, IAntCoreConstants.EMPTY_STRING);
		if (appletName.length() != 0) {
			b.append(" name=\"" + appletName + "\""); //$NON-NLS-1$ //$NON-NLS-2$
		}
		b.append(" width=\""); //$NON-NLS-1$
		b.append(Integer.toString(configuration.getAttribute(IJavaLaunchConfigurationConstants.ATTR_APPLET_WIDTH, 200)));
		b.append("\" height=\""); //$NON-NLS-1$
		b.append(Integer.toString(configuration.getAttribute(IJavaLaunchConfigurationConstants.ATTR_APPLET_HEIGHT, 200)));
		b.append("\">" + ExportUtil.NEWLINE); //$NON-NLS-1$
		Map<String, String> parameters = configuration.getAttribute(IJavaLaunchConfigurationConstants.ATTR_APPLET_PARAMETERS, new HashMap<>());
		if (parameters.size() != 0) {
			Iterator<Entry<String, String>> iterator = parameters.entrySet().iterator();
			while (iterator.hasNext()) {
				Entry<String, String> next = iterator.next();
				b.append("            <param name="); //$NON-NLS-1$
				b.append(getQuotedString(next.getKey()));
				b.append(" value="); //$NON-NLS-1$
				b.append(getQuotedString(next.getValue()));
				b.append(">" + ExportUtil.NEWLINE); //$NON-NLS-1$
			}
		}
		b.append("        </applet>" + ExportUtil.NEWLINE); //$NON-NLS-1$
		b.append("    </body>" + ExportUtil.NEWLINE); //$NON-NLS-1$
		b.append("</html>" + ExportUtil.NEWLINE); //$NON-NLS-1$
		return b.toString();
	}

	private static String getQuotedString(String string) {
		if (string.indexOf('"') == -1) {
			return '"' + string + '"';
		}
		return '\'' + string + '\'';
	}

	/**
	 * Returns the main type name specified by the given launch configuration, or <code>null</code> if none.
	 *
	 * @param configuration
	 *            launch configuration
	 * @return the main type name specified by the given launch configuration, or <code>null</code> if none
	 * @exception CoreException
	 *                if unable to retrieve the attribute
	 */
	public static String getMainTypeName(ILaunchConfiguration configuration) throws CoreException {
		return fgDelegate.getMainTypeName(configuration);
	}
}