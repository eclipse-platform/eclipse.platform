/*******************************************************************************
 * Copyright (c) 2026 Ericsson
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.core.llm.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.core.llm.LlmModel;
import org.eclipse.swt.widgets.Display;
import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpServer;

public class LlmModelTest {

	@Test
	void constructorAndGetters() {
		LlmModel m = new LlmModel("http://example/api", "my-model"); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("http://example/api", m.url()); //$NON-NLS-1$
		assertEquals("my-model", m.model()); //$NON-NLS-1$
	}

	@Test
	void inferSendsExpectedJsonAndReturnsBody() throws Exception {
		AtomicReference<String> captured = new AtomicReference<>();
		HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0); //$NON-NLS-1$
		server.createContext("/", ex -> { //$NON-NLS-1$
			captured.set(new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
			byte[] body = "ok".getBytes(StandardCharsets.UTF_8); //$NON-NLS-1$
			ex.sendResponseHeaders(200, body.length);
			try (OutputStream os = ex.getResponseBody()) {
				os.write(body);
			}
		});
		server.start();
		try {
			String url = "http://127.0.0.1:" + server.getAddress().getPort() + "/"; //$NON-NLS-1$ //$NON-NLS-2$
			LlmModel m = new LlmModel(url, "test-model"); //$NON-NLS-1$
			String reply = m.infer("Hello \"world\"\nline2\ttabbed"); //$NON-NLS-1$
			assertEquals("ok", reply); //$NON-NLS-1$
			String sent = captured.get();
			assertTrue(sent.contains("\"model\":\"test-model\""), sent); //$NON-NLS-1$
			assertTrue(sent.contains("\"prompt\":\"Hello \\\"world\\\"\\nline2\\ttabbed\""), sent); //$NON-NLS-1$
			assertTrue(sent.contains("\"stream\":false"), sent); //$NON-NLS-1$
		} finally {
			server.stop(0);
		}
	}

	@Test
	void inferFailsOnNon2xx() throws Exception {
		HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0); //$NON-NLS-1$
		server.createContext("/", ex -> { //$NON-NLS-1$
			byte[] body = "boom".getBytes(StandardCharsets.UTF_8); //$NON-NLS-1$
			ex.sendResponseHeaders(500, body.length);
			try (OutputStream os = ex.getResponseBody()) {
				os.write(body);
			}
		});
		server.start();
		try {
			LlmModel m = new LlmModel("http://127.0.0.1:" + server.getAddress().getPort() + "/", "x"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			assertThrows(IOException.class, () -> m.infer("q")); //$NON-NLS-1$
		} finally {
			server.stop(0);
		}
	}

	@Test
	void inferOnUiThreadThrows() throws Exception {
		Display display = Display.getDefault();
		AtomicReference<Throwable> caught = new AtomicReference<>();
		display.syncExec(() -> {
			try {
				new LlmModel("http://example/", "m").infer("q"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			} catch (Throwable t) {
				caught.set(t);
			}
		});
		assertTrue(caught.get() instanceof IllegalStateException,
				"expected IllegalStateException, got " + caught.get()); //$NON-NLS-1$
	}
}
