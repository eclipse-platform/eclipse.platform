/*******************************************************************************
 *  Copyright (c) 2008, 2015 IBM Corporation and others.
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

package org.eclipse.ua.tests.help.webapp;
import java.io.IOException;
import java.io.OutputStream;

import javax.servlet.http.HttpServletRequest;

import org.eclipse.help.webapp.IFilter;

/**
 * Used in FilterExtension test
 */

public class CommentFilter implements IFilter {

	@Override
	public OutputStream filter(HttpServletRequest req, OutputStream out) {
		return new OutFilter(out);
	}

	private class OutFilter extends OutputStream {

		private OutputStream out;
		private boolean preambleWritten = false;

		public OutFilter(OutputStream out) {
			this.out = out;
		}

		@Override
		public void write(int b) throws IOException {
			if (!preambleWritten) {
				preambleWritten = true;
				String comment = "<!-- pre " + getCommentText() + " -->";
				out.write(comment.getBytes());
			}
			out.write(b);
		}

		@Override
		public void close() throws IOException {
			String comment = "<!-- post " + getCommentText() + " -->";
			out.write(comment.getBytes());
			out.close();
			super.close();
		}

	}

	protected String getCommentText() {
		return "comment";
	}

}
