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
package org.eclipse.tips.json.internal;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.eclipse.tips.core.IUrlTip;
import org.eclipse.tips.core.Tip;
import org.eclipse.tips.json.IJsonTip;

import com.google.gson.JsonObject;

/**
 * Internal implementation for a Json generated {@link IUrlTip}.
 */
public class JsonUrlTip extends Tip implements IUrlTip, IJsonTip {

	private String fSubject;
	private Date fDate;
	private String fUrl;
	private JsonObject fJsonObject;

	/**
	 * Creates the tip out of the passed Json object.
	 *
	 * @param jsonObject the json object
	 * @param provider   the provider
	 * @throws ParseException when the json object could not be parsed.
	 */
	public JsonUrlTip(String providerId, JsonObject jsonObject) throws ParseException {
		super(providerId);
		fJsonObject = jsonObject;
		fSubject = Util.getValueOrDefault(jsonObject, JsonConstants.T_SUBJECT, "Not set"); //$NON-NLS-1$
		fDate = getDate(jsonObject);
		fUrl = Util.getValueOrDefault(jsonObject, JsonConstants.T_URL, null);
	}

	private static Date getDate(JsonObject jsonObject) throws ParseException {
		String date = Util.getValueOrDefault(jsonObject, JsonConstants.T_DATE, "1970-01-01"); //$NON-NLS-1$
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd"); //$NON-NLS-1$
		return sdf.parse(date);
	}

	@Override
	public Date getCreationDate() {
		return fDate;
	}

	@Override
	public String getURL() {
		return Util.replace(fJsonObject, fUrl);
	}

	@Override
	public String getSubject() {
		return fSubject;
	}

	@Override
	public String getJsonObject() {
		return fJsonObject.getAsString();
	}
}