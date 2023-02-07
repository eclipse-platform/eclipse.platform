/*******************************************************************************
 * Copyright (c) 2018, 2023 Remain Software
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     wim.jongman@remainsoftware.com - initial API and implementation
 *******************************************************************************/
package org.eclipse.tips.examples.eclipsetips;

import java.util.Date;

import org.eclipse.tips.core.IHtmlTip;
import org.eclipse.tips.core.Tip;
import org.eclipse.tips.core.TipImage;
import org.eclipse.tips.examples.DateUtil;

public class TwitterTip extends Tip implements IHtmlTip {

	public TwitterTip(String providerId) {
		super(providerId);
	}

	@Override
	public Date getCreationDate() {
		return DateUtil.getDateFromYYMMDD("10/01/2018");
	}

	@Override
	public String getSubject() {
		return "CTRL+1 Quick Assists";
	}

	@Override
	public String getHTML() {
		return """
				<html>
				<head><style></style></head>
				<body>
					<div>
						<blockquote class="twitter-tweet" data-lang="en">
						<p lang="en" dir="ltr">The &#39;Extract class...&#39; refactoring
						(from Alt+Shift+T) 	extracts a group of fields into a separate class
						and replaces all occurrences to fit the new structure. See example.
						<a href="https://twitter.com/hashtag/EclipseTips?src=hash&amp;ref_src=twsrc%5Etfw">#EclipseTips</a>
						<a href="https://t.co/tEI7ic7C1g">pic.twitter.com/tEI7ic7C1g</a></p>
						&mdash; Eclipse Java IDE (@EclipseJavaIDE)
						<a href="https://twitter.com/EclipseJavaIDE/status/949238007051235328?ref_src=twsrc%5Etfw">January 5, 2018</a>
						</blockquote>
						<script src="https://platform.twitter.com/widgets.js" charset="utf-8"></script>
					</div>
				</body>
				</html>""";
	}

	@Override
	public TipImage getImage() {
		return null;
	}
}