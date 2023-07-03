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
package org.eclipse.tips.json;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.text.ParseException;
import java.util.ArrayList;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.tips.core.Tip;
import org.eclipse.tips.core.TipImage;
import org.eclipse.tips.core.TipProvider;
import org.eclipse.tips.core.internal.LogUtil;
import org.eclipse.tips.json.internal.JsonConstants;
import org.eclipse.tips.json.internal.JsonHTMLTip;
import org.eclipse.tips.json.internal.JsonUrlTip;
import org.eclipse.tips.json.internal.Messages;
import org.eclipse.tips.json.internal.Util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;

/**
 * A special TipProvider that gets instantiated from a JSon file.
 *
 */
@SuppressWarnings("restriction")
public abstract class JsonTipProvider extends TipProvider {

	private static final String SPACE = " "; //$NON-NLS-1$
	private URL fJsonUrl;
	private String fDescription;
	private String fImage;
	private JsonObject fJsonObject;

	/**
	 * A method to set the a url containing a JSon file that describes this tip
	 * provider.
	 *
	 * @param jsonUrl the uRL to the Json file describing the provider and tips
	 * @throws MalformedURLException in case of an incorrect URL
	 */
	public void setJsonUrl(String jsonUrl) throws MalformedURLException {
		fJsonUrl = new URL(jsonUrl);
	}

	/**
	 *
	 * {@inheritDoc}
	 *
	 * <p>
	 * <b>Implementation Details</b><br>
	 * The implementation of this method in this provider will parse the json file
	 * which was set with {@link #setJsonUrl(String)}.
	 *
	 */
	@Override
	public synchronized IStatus loadNewTips(IProgressMonitor monitor) {
		SubMonitor subMonitor = SubMonitor.convert(monitor, getDescription() + SPACE + Messages.JsonTipProvider_1, 3);
		ArrayList<Tip> result = new ArrayList<>();
		try {
			fJsonObject = loadJsonObject();
			subMonitor.worked(1);
			if (fJsonObject == null) {
				return new Status(IStatus.INFO, "org.eclipse.tips.json",
						MessageFormat.format("Could not parse json for {0}. Cache invalidated.", getID()), null);
			}
			JsonObject provider = fJsonObject.getAsJsonObject(JsonConstants.P_PROVIDER);
			fDescription = Util.getValueOrDefault(provider, JsonConstants.P_DESCRIPTION, "not set"); //$NON-NLS-1$
			fImage = Util.getValueOrDefault(provider, JsonConstants.P_IMAGE, null);
			setExpression(Util.getValueOrDefault(provider, JsonConstants.P_EXPRESSION, null));
			JsonArray tips = provider.getAsJsonArray(JsonConstants.P_TIPS);
			subMonitor.worked(1);
			tips.forEach(parm -> result.add(createJsonTip(parm)));
			subMonitor.worked(1);
		} catch (Exception e) {
			Status status = new Status(IStatus.ERROR, "org.eclipse.tips.json", e.getMessage(), e); //$NON-NLS-1$
			getManager().log(status);
			return status;
		}
		getManager().log(LogUtil.info(MessageFormat.format(Messages.JsonTipProvider_4, result.size() + ""))); //$NON-NLS-1$
		setTips(result);
		return Status.OK_STATUS;
	}

	private JsonObject loadJsonObject() throws IOException {
		// RFC 8259: https://tools.ietf.org/html/rfc8259#section-8.1
		// Json MUST be encoded as UTF-8, unless in a closed system.
		try (InputStream stream = fJsonUrl.openStream();
				InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
			JsonReader jreader = new JsonReader(reader);
			jreader.setLenient(true);
			Object result = JsonParser.parseReader(jreader);
			if (result instanceof JsonObject) {
				return (JsonObject) result;
			} else {
				return null;
			}
		}
	}

	@Override
	public TipImage getImage() {
		if (fImage == null) {
			return null;
		}
		return new TipImage(fImage);
	}

	@Override
	public String getDescription() {
		return fDescription;
	}

	private Tip createJsonTip(JsonElement parm) {
		JsonObject json = (JsonObject) parm;
		replaceVariables(json);
		try {
			if (json.get(JsonConstants.T_URL) != null) {
				return new JsonUrlTip(getID(), json);
			} else {
				return new JsonHTMLTip(getID(), json);
			}
		} catch (ParseException e) {
			getManager().log(LogUtil.error(getClass(), e));
			throw new RuntimeException(e);
		}
	}

	private void replaceVariables(JsonObject pJson) {
		String url = Util.getValueOrDefault(pJson, JsonConstants.T_URL, null);
		String html = Util.getValueOrDefault(pJson, JsonConstants.T_HTML, null);
		JsonObject vars = fJsonObject.getAsJsonObject(JsonConstants.P_PROVIDER)
				.getAsJsonObject(JsonConstants.T_VARIABLES);
		if (vars != null) {
			if (url != null) {
				url = Util.replace(vars, url);
				pJson.remove(JsonConstants.T_URL);
				pJson.addProperty(JsonConstants.T_URL, url);
			}
			if (html != null) {
				html = Util.replace(vars, html);
				pJson.remove(JsonConstants.T_HTML);
				pJson.addProperty(JsonConstants.T_HTML, html);
			}
		}
	}

	@Override
	public void dispose() {
	}
}