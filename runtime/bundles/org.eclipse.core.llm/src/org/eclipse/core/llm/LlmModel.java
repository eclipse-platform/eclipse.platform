/*******************************************************************************
 * Copyright (c) 2026 Ericsson
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.core.llm;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import org.eclipse.swt.widgets.Display;

/**
 * Minimal LLM client. An instance is configured with the endpoint URL and the
 * model name and exposes {@link #infer(String)} to query the model.
 * <p>
 * {@link #infer(String)} performs a blocking HTTP request and must never be
 * invoked from the SWT UI thread; doing so throws {@link IllegalStateException}.
 * </p>
 */
public final class LlmModel {

	private final String url;
	private final String model;

	public LlmModel(String url, String model) {
		this.url = url;
		this.model = model;
	}

	public String url() {
		return url;
	}

	public String model() {
		return model;
	}

	/**
	 * Send {@code prompt} to the configured LLM and return the raw response body.
	 *
	 * @throws IllegalStateException if invoked from the SWT UI thread
	 * @throws IOException           if the HTTP call fails
	 */
	public String infer(String prompt) throws IOException, InterruptedException {
		if (Display.getCurrent() != null) {
			throw new IllegalStateException("LlmModel.infer must not be called from the UI thread"); //$NON-NLS-1$
		}
		String body = "{\"model\":\"" + escape(model) + "\",\"prompt\":\"" + escape(prompt) + "\",\"stream\":false}"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		HttpRequest request = HttpRequest.newBuilder(URI.create(url))
				.timeout(Duration.ofMinutes(2))
				.header("Content-Type", "application/json") //$NON-NLS-1$ //$NON-NLS-2$
				.POST(HttpRequest.BodyPublishers.ofString(body))
				.build();
		HttpResponse<String> response = HttpClient.newHttpClient()
				.send(request, HttpResponse.BodyHandlers.ofString());
		if (response.statusCode() / 100 != 2) {
			throw new IOException("LLM request failed: HTTP " + response.statusCode() + " - " + response.body()); //$NON-NLS-1$ //$NON-NLS-2$
		}
		return response.body();
	}

	private static String escape(String s) {
		StringBuilder sb = new StringBuilder(s.length() + 8);
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			switch (c) {
				case '"' -> sb.append("\\\""); //$NON-NLS-1$
				case '\\' -> sb.append("\\\\"); //$NON-NLS-1$
				case '\n' -> sb.append("\\n"); //$NON-NLS-1$
				case '\r' -> sb.append("\\r"); //$NON-NLS-1$
				case '\t' -> sb.append("\\t"); //$NON-NLS-1$
				default -> {
					if (c < 0x20) {
						sb.append(String.format("\\u%04x", (int) c)); //$NON-NLS-1$
					} else {
						sb.append(c);
					}
				}
			}
		}
		return sb.toString();
	}
}
