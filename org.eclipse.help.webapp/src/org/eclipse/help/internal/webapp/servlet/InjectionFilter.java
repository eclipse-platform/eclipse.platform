/*******************************************************************************
 * Copyright (c) 2000, 2019 IBM Corporation and others.
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

package org.eclipse.help.internal.webapp.servlet;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.help.internal.base.HelpBasePlugin;
import org.eclipse.help.internal.util.ProductPreferences;
import org.eclipse.help.internal.webapp.data.CssUtil;
import org.eclipse.help.internal.webapp.data.UrlUtil;
import org.eclipse.help.webapp.IFilter;

/**
 * This class inserts a CSSs for narrow and disabled CSSs when called from the
 * dynamic help view.
 */
public class InjectionFilter implements IFilter {
	private static final String disabledBook3 = "\n<script type=\"text/javascript\" src=\""; //$NON-NLS-1$
	private static final String disabledBook4 = "livehelp.js\"> </script>"; //$NON-NLS-1$

	private static final String TOPIC_CSS = "topic_css"; //$NON-NLS-1$
	private static final String NAV_CSS   = "nav_css"; //$NON-NLS-1$
	private static final String NARROW_CSS = "narrow_css"; //$NON-NLS-1$
	private static final String DISABLED_CSS = "disabled_css"; //$NON-NLS-1$
	private static final String REMOTE_CSS = "remote_css"; //$NON-NLS-1$
	private boolean isRemote;

	public InjectionFilter( boolean isRemote ) {
		this.isRemote = isRemote;
	}

	@Override
	public OutputStream filter(HttpServletRequest req, OutputStream out) {
		boolean isUnfiltered = ProductPreferences.useEnablementFilters();

		boolean addNarrow = false;
		boolean addDisabled = false;
		boolean needsLiveHelp = false;

		String uri = req.getRequestURI();
		boolean isNav = "/nav".equals(req.getServletPath()); //$NON-NLS-1$
		if (uri == null || !uri.endsWith("html") && !uri.endsWith("htm") && !isNav) { //$NON-NLS-1$ //$NON-NLS-2$
			return out;
		}
		if (UrlUtil.isBot(req)) {
			return out;
		}
		String pathInfo = req.getPathInfo();
		if (pathInfo == null) {
			return out;
		}

		List<String> cssIncludes = new ArrayList<>();
		if (isNav) {
			CssUtil.addCssFiles(NAV_CSS, cssIncludes);
		} else {
			CssUtil.addCssFiles(TOPIC_CSS, cssIncludes);
		}
		if(isRemote){
			CssUtil.addCssFiles(REMOTE_CSS, cssIncludes);
		}

		boolean enabled = isUnfiltered || isNav
			|| HelpBasePlugin.getActivitySupport().isRoleEnabled(pathInfo)
			|| isRemote;
		if ("/ntopic".equals(req.getServletPath())) { //$NON-NLS-1$
			addNarrow = true;
			CssUtil.addCssFiles(NARROW_CSS, cssIncludes);
		}
		if (!enabled) {
			addDisabled = true;
			CssUtil.addCssFiles(DISABLED_CSS, cssIncludes);
		}

		needsLiveHelp = !enabled && HelpBasePlugin.getActivitySupport().getDocumentMessageUsesLiveHelp(addNarrow);

		if (cssIncludes.isEmpty() && !addDisabled)
			return out;

		IPath path = new Path(pathInfo);
		int upLevels = path.segmentCount() - 1;
		String relativePath = FilterUtils.getRelativePathPrefix(req);
		StringBuilder script = new StringBuilder();
		StringBuilder disabledContent = new StringBuilder();
		script.append(CssUtil.createCssIncludes(cssIncludes, FilterUtils.getRelativePathPrefix(req)));
		if (addDisabled) {
			if (needsLiveHelp) {
				script.append(disabledBook3);
				script.append(relativePath);
				script.append("content/org.eclipse.help/"); //$NON-NLS-1$
				script.append(disabledBook4);
			}
			appendDisabled(disabledContent, upLevels, addNarrow, relativePath);
		}
		return new FilterHTMLHeadAndBodyOutputStream(out,
					script.toString().getBytes(StandardCharsets.US_ASCII), addDisabled ? disabledContent.toString() : null);
	}


	private void appendDisabled(StringBuilder buff, int nsteps, boolean narrow, String relativePath) {
		String message = HelpBasePlugin.getActivitySupport().getDocumentMessage(narrow);
		if (message==null)
			return;
		buff.append("<div id=\"help-disabledTopic\">"); //$NON-NLS-1$
		buff.append("<img src=\""); //$NON-NLS-1$
		buff.append(relativePath);
		buff.append("content/org.eclipse.help.webapp/"); //$NON-NLS-1$
		buff.append("advanced/images/e_show_all.svg\" border=\"0\" align=\"bottom\">&nbsp;"); //$NON-NLS-1$
		buff.append(message);
		buff.append("<br><hr></div>"); //$NON-NLS-1$
	}
}
