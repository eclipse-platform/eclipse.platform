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
package org.eclipse.tips.examples;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Date utilities.
 *
 */
public class DateUtil {

	/**
	 * Convenience method that creates a date from a dd/mm/yy string.
	 *
	 * @param pYYMMDD
	 *            the date in a dd/mm/yy format, e.g. "01/01/2017"
	 * @return the date
	 * @throws RuntimeException
	 *             if the date is not correct
	 */
	public static Date getDateFromYYMMDD(String pYYMMDD) {
		SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
		try {
			return sdf.parse(pYYMMDD);
		} catch (ParseException e) {
			throw new RuntimeException(e);
		}
	}
}