/*******************************************************************************
 * Copyright (c) 2009, 2015 IBM Corporation and others.
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

package org.eclipse.ua.tests.doc.internal.linkchecker;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;

import javax.servlet.http.HttpServletRequest;

import org.eclipse.help.internal.webapp.servlet.FilterUtils;
import org.eclipse.help.webapp.IFilter;
import org.eclipse.ua.tests.doc.internal.dialogs.SelectTocDialog;

public class OnLoadFilter implements IFilter {

	private static long uniqueId = 0;

	private class OutFilter extends OutputStream {

		private OutputStream out;

		private int state = 0;

		private String pathPrefix;

		public void updateState(int b) throws IOException {
			if (state == 0 && b == '<') {
				state = 1;
			} else if (state == 1 && (b == 'b' || b == 'B')) {
				state = 2;
			} else if (state == 2 && (b == 'o' || b == 'O')) {
				state = 3;
			} else if (state == 3 && (b == 'd' || b == 'D')) {
				state = 4;
			} else if (state == 4 && (b == 'y' || b == 'Y')) {
				state = 20;
				out.write(b);
				if (linkProvider.hasNext()) {
					String location = linkProvider.next();
					String onload = getOnloadText(pathPrefix + location, testKind);
					out.write(onload.getBytes());
					//System.out.println("Onload = " + onload);
				} else {
					linkProvider = null;
					String announceComplete = getCompletionText(testKind);
					out.write(announceComplete.getBytes());
					//System.out.println("announceComplete = " + announceComplete);
				}
			} else if (state == 20 && b == '>') {
				state = 21;
			} else if (state == 1 && (b == 'h' || b == 'H')) {
				state = 11;
			} else if (state == 11 && (b == 'e' || b == 'E')) {
				state = 12;
			} else if (state == 12 && (b == 'a' || b == 'A')) {
				state = 13;
			} else if (state == 13 && (b == 'm' || b == 'M')) {
				state = 14;
			} else if (state == 14 && (b == 'e' || b == 'E')) {
				state = 15;
			} else if (state == 15 && (b == 's' || b == 'S')) {
				state = 16;
			} else if (state == 16 && (b == 'e' || b == 'E')) {
				state = 17;
			} else if (state == 17 && (b == 't' || b == 'T')) {
				state = 20;
			} else if (state > 0 && state < 20) {
				state = 0;
			}
		}

		private String getOnloadText(String location, int testKind) {
			String onload = " onload = \"";
			if (testKind == SelectTocDialog.FOLLOW_LINKS) {
				onload += "ua_test_doc_record_links();";
			}
			onload += " window.location = '";
			String uniqParam = "?uniq=" + ++uniqueId;
			int anchor = location.indexOf('#');
			if (anchor == -1) {
				onload = onload + location + uniqParam + "'";
			} else {
				onload = onload + location.substring(0, anchor) + uniqParam
						+ location.substring(anchor) + "'";
			}
			onload += '"';
			return onload;
		}

		private String getCompletionText(int testKind) {
			if (testKind == SelectTocDialog.FOLLOW_LINKS) {
				return " onload = \"ua_test_doc_check_links();\" ";
			} else {
				return " onload = \"ua_test_doc_complete();\" ";
			}
		}

		public OutFilter(OutputStream out, String prefix) {
			this.out = out;
			this.pathPrefix = prefix;
		}

		@Override
		public void write(int b) throws IOException {
			updateState(b);
			if (state != 20) {
				out.write(b);
			}
		}

		@Override
		public void close() throws IOException {
			out.close();
			super.close();
		}
	}

	private int testKind;

	public OnLoadFilter(int testKind) {
		this.testKind = testKind;
	}

	private static Iterator<String> linkProvider;

	protected String getCommentText() {
		return "comment";
	}

	@Override
	public OutputStream filter(HttpServletRequest req, OutputStream out) {
		if (linkProvider == null) {
			return out;
		}
		String pathPrefix = FilterUtils.getRelativePathPrefix(req);
		if (pathPrefix.length() >= 4) {
			return new OutFilter(out, pathPrefix.substring(0, pathPrefix.length() - 4));
		}
		return new OutFilter(out, "PLUGINS_ROOT");
	}

	public static void setLinkProvider(Iterator<String> provider) {
		linkProvider = provider;
	}

}
