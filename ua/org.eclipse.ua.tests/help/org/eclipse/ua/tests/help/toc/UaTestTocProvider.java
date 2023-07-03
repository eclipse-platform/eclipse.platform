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

package org.eclipse.ua.tests.help.toc;

import org.eclipse.help.AbstractTocProvider;
import org.eclipse.help.IToc;
import org.eclipse.help.ITocContribution;
import org.eclipse.ua.tests.help.other.UserToc;
import org.eclipse.ua.tests.help.other.UserTopic;

public class UaTestTocProvider extends AbstractTocProvider {

	UserToc toc;
	TocContribution contribution;
	private ITocContribution[] contributions;

	private class TocContribution implements ITocContribution {

		@Override
		public String getCategoryId() {
			return null;
		}

		@Override
		public String getContributorId() {
			return "org.eclipse.ua.tests";
		}

		@Override
		public String[] getExtraDocuments() {
			return new String[0];
		}

		@Override
		public String getId() {
			return "generatedToc";
		}

		@Override
		public String getLinkTo() {
			return "PLUGINS_ROOT/org.eclipse.ua.tests/data/help/toc/root.xml#generatedContent";
		}

		@Override
		public String getLocale() {
			return null;
		}

		@Override
		public IToc getToc() {
			return toc;
		}

		@Override
		public boolean isPrimary() {
			return false;
		}

	}

	public UaTestTocProvider() {
		toc = new UserToc("Generated Toc", null, true);
		UserTopic parentTopic = new UserTopic("Generated Parent",
				"generated/Generated+Parent/Parent+page+with+searchable+word+egrology+.html", true);
		for (int i = 1; i <= 4; i++) {
			UserTopic childTopic = new UserTopic("Generated Child " + i,
					"generated/Generated+Child" + i +
					"/Child+topic+" + i + ".html", true);
			parentTopic.addTopic(childTopic);
		}
		toc.addTopic(parentTopic);
		contribution = new TocContribution();
		contributions = new ITocContribution[] { contribution };
	}

	@Override
	public ITocContribution[] getTocContributions(String locale) {
		return contributions;
	}

}
