/*******************************************************************************
 * Copyright (c) 2018, 2021 Remain Software and others
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

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.MessageFormat;
import java.util.Map.Entry;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.osgi.framework.FrameworkUtil;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class Util {

	/**
	 * Parses the passed json or returns a default value.
	 *
	 * @param jsonObject
	 * @param element      the value to return in case the jsonObject does not
	 *                     contain the specified value.
	 * @param defaultValue the value to return in case the jsonObject does not
	 *                     contain the specified value.
	 * @return the returned value
	 */
	public static String getValueOrDefault(JsonObject jsonObject, String element, String defaultValue) {
		if (jsonObject.has(element)) {
			return jsonObject.get(element).getAsString();
		}
		return defaultValue;
	}

	/**
	 * Parses the passed json or returns a default value.
	 *
	 * @param jsonObject
	 * @param element      the value to return in case the jsonObject does not
	 *                     contain the specified value.
	 * @param defaultValue the value to return in case the jsonObject does not
	 *                     contain the specified value.
	 * @return the returned value
	 */
	public static int getValueOrDefault(JsonObject jsonObject, String element, int defaultValue) {
		if (jsonObject.has(element)) {
			return jsonObject.get(element).getAsInt();
		}
		return defaultValue;
	}

	/**
	 * Parses the passed json or returns a default value.
	 *
	 * @param jsonObject
	 * @param element      the value to return in case the jsonObject does not
	 *                     contain the specified value.
	 * @param defaultValue the value to return in case the jsonObject does not
	 *                     contain the specified value.
	 * @return the returned value
	 */
	public static double getValueOrDefault(JsonObject jsonObject, String element, double defaultValue) {
		if (jsonObject.has(element)) {
			return jsonObject.get(element).getAsDouble();
		}
		return defaultValue;
	}

	/**
	 * Replaces all keys in the passed, json object that represent a primitive
	 * value, in the input string. If the passed json object contains a
	 * {@link JsonConstants#T_VARIABLES} object then this is parsed as well.
	 * <p>
	 * Example:
	 *
	 * <pre>
	 * json object: {"first": "Wim", "last": "Jongman", "variables": {"title": "Mr.", "age": 53}}
	 * input: "${title} ${first} ${last} is ${age} years old."
	 * output: "Mr. Wim Jongman is 53 years old"
	 * </pre>
	 *
	 * @param object the input json object
	 * @param input  the string to scan
	 * @return the replaced string
	 */
	public static String replace(JsonObject object, String input) {
		String result = doReplace(object, input);
		JsonObject vars = object.getAsJsonObject(JsonConstants.T_VARIABLES);
		if (vars != null) {
			result = Util.replace(vars, result);
		}
		return result;
	}

	private static String doReplace(JsonObject object, String input) {
		String result = input;
		for (Entry<String, JsonElement> entry : object.entrySet()) {
			JsonElement jsonElement = entry.getValue();
			if (jsonElement.isJsonPrimitive()) {
				String search = "${" + entry.getKey() + "}"; //$NON-NLS-1$ //$NON-NLS-2$
				String replace = jsonElement.getAsString();
				int index = result.indexOf(search);
				while (index > -1) {
					result = result.substring(0, index) + replace + result.substring(index + search.length());
					index = result.indexOf(search);
				}
			}
		}
		return result;
	}

	/**
	 * Checks if the URL is valid.
	 *
	 * @param pUrl
	 * @return A status indicating the result.
	 * @throws IOException
	 */
	public static IStatus isValidUrl(String pUrl) {
		String symbolicName = FrameworkUtil.getBundle(Util.class).getSymbolicName();
		try {
			URL url = new URL(pUrl);
			HttpURLConnection httpCon = (HttpURLConnection) url.openConnection();
			int responseCode = httpCon.getResponseCode();
			if (responseCode != 200) {
				return new Status(IStatus.ERROR, symbolicName, MessageFormat.format("Received response code {0} from {1}.", responseCode + "", pUrl));
			}
			if (httpCon.getContentLength() <= 0) {
				return new Status(IStatus.ERROR, symbolicName,
						MessageFormat.format("Received empty file from {0}.", pUrl));
			}
		} catch (Exception e) {
			return new Status(IStatus.ERROR, symbolicName, MessageFormat.format("Received empty file from {0}.", pUrl),
					e);
		}
		return Status.OK_STATUS;
	}
}
