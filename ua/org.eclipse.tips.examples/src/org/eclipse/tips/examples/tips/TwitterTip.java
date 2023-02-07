/*******************************************************************************
 * Copyright (c) 2018 Remain Software
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
package org.eclipse.tips.examples.tips;

import java.util.Date;

import org.eclipse.tips.core.IUrlTip;
import org.eclipse.tips.core.Tip;
import org.eclipse.tips.core.TipProvider;

/**
 * Specialisation of Tip that receives an URL of a tweet in the constructor. The
 * URL points directly to the tweet:
 * <p>
 * For an example see: <a href=
 * "https://twitter.com/EclipseJavaIDE/status/919915440041840641">https://twitter.com/EclipseJavaIDE/status/919915440041840641</a>
 * </p>
 */
public class TwitterTip extends Tip implements IUrlTip {

	private String fPageUrl;
	private Date fCreationDate;
	private String fSubject;

	/**
	 * Constructor.
	 *
	 * <p>
	 * For an example see: <a href=
	 * "https://twitter.com/EclipseJavaIDE/status/919915440041840641">https://twitter.com/EclipseJavaIDE/status/919915440041840641</a>
	 * </p>
	 *
	 * @param pProvider     the {@link TipProvider} that created this Tip
	 * @param pTweetUrl     the URL of the tweet
	 * @param pCreationDate creation date
	 * @param pSubject      the tips' subject
	 */
	public TwitterTip(String providerId, String pTweetUrl, Date pCreationDate, String pSubject) {
		super(providerId);
		fPageUrl = pTweetUrl;
		fCreationDate = pCreationDate;
		fSubject = pSubject;
	}

	@Override
	public Date getCreationDate() {
		return fCreationDate;
	}

	@Override
	public String getSubject() {
		return fSubject;
	}

	@Override
	public String getURL() {
		return fPageUrl;
	}
}