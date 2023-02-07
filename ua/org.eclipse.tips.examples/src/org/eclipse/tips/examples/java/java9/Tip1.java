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
package org.eclipse.tips.examples.java.java9;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.tips.core.IHtmlTip;
import org.eclipse.tips.core.Tip;
import org.eclipse.tips.core.TipAction;
import org.eclipse.tips.core.TipImage;
import org.eclipse.tips.core.TipProvider;
import org.eclipse.tips.examples.DateUtil;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

/**
 * This is an example Tip class.
 *
 */
public class Tip1 extends Tip implements IHtmlTip {

	/**
	 * Tips should be created fast.
	 *
	 * @param pProvider
	 *            the associated {@link TipProvider}
	 */
	public Tip1(String providerId) {
		super(providerId);
	}

	@Override
	public List<TipAction> getActions() {
		Runnable runnable = () -> Display.getDefault()
				.syncExec(() -> MessageDialog.openConfirm(null, getSubject(), "We can start an action from a Tip!"));

		ArrayList<TipAction> actions = new ArrayList<>();
		actions.add(new TipAction("Tip1 Action", "Just a silly action", runnable, null));
		return actions;
	}

	/**
	 * Return the publish date of the tip. The {@link TipProvider} could decide to
	 * server newer tips first.
	 *
	 * @return the date this tip was published which may not be null.
	 */
	@Override
	public Date getCreationDate() {
		return DateUtil.getDateFromYYMMDD("10/01/2018");
	}

	/**
	 * @return the subject which may not be null.
	 */
	@Override
	public String getSubject() {
		return "Java tip 1";
	}

	/**
	 * This method may both return the string representation of an URL or the
	 * descriptive HTML of the tip. If the html of the text is returned then an
	 * effort is made to also inline the image URL. If you supply an URL then
	 * {@link #getImage()} should return null.
	 *
	 * @return the HMTL or URL of the tip which is displayed in a browser widget.
	 * @see #getImage()
	 */
	@Override
	public String getHTML() {
		return """
				<h2>Javatip 1</h2>
				You see this tip because the Tip UI was opened when the java perspective
				was active or because you selected the Java icon below.<br><br>
				More java tips will be displayed here in the near future. For now, select one
				of the other providers by clicking on the icons below.""";
	}

	/**
	 * A getter for the {@link TipImage}. Subclasses may override, the default
	 * implementation returns null.
	 *
	 * @return a TipImage with information about the image or null if no image is
	 *         provided.
	 */
	@Override
	public TipImage getImage() {
		Bundle bundle = FrameworkUtil.getBundle(getClass());
		try {
			return new TipImage(bundle.getEntry("images/java/duke.png")).setAspectRatio(1);
		} catch (IOException e) {
//			getProvider().getManager().log(LogUtil.error(getClass(), e));
		}
		return null;
	}
}