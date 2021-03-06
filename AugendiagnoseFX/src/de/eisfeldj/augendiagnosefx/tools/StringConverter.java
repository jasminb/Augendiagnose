package de.eisfeldj.augendiagnosefx.tools;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Properties;
import java.util.TreeSet;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Tool to convert Strings from Android app to Windows application.
 */
public final class StringConverter {
	/**
	 * The global Strings file.
	 */
	private static final File[] XML_FILES_GLOBAL = {new File("../AugendiagnoseIdea/augendiagnoseLib/src/main/res/values/strings.xml"),
			new File("../AugendiagnoseIdea/augendiagnoseLib/src/main/res/values/strings_menu.xml"),
			new File("../AugendiagnoseIdea/augendiagnoseLib/src/main/res/values/strings_settings.xml")};

	/**
	 * The German String file.
	 */
	private static final File[] XML_FILES_DE = {new File("../AugendiagnoseIdea/augendiagnoseLib/src/main/res/values-de/strings.xml"),
			new File("../AugendiagnoseIdea/augendiagnoseLib/src/main/res/values-de/strings_menu.xml"),
			new File("../AugendiagnoseIdea/augendiagnoseLib/src/main/res/values-de/strings_settings.xml")};

	/**
	 * The Spanish String file.
	 */
	private static final File[] XML_FILES_ES = {new File("../AugendiagnoseIdea/augendiagnoseLib/src/main/res/values-es/strings.xml"),
			new File("../AugendiagnoseIdea/augendiagnoseLib/src/main/res/values-es/strings_menu.xml"),
			new File("../AugendiagnoseIdea/augendiagnoseLib/src/main/res/values-es/strings_settings.xml")};

	/**
	 * The global Properties file.
	 */
	private static final File PROP_FILE_GLOBAL = new File("resources/bundles/Strings.properties");

	/**
	 * The German Properties file.
	 */
	private static final File PROP_FILE_DE = new File("resources/bundles/Strings_de.properties");

	/**
	 * The Spanish Properties file.
	 */
	private static final File PROP_FILE_ES = new File("resources/bundles/Strings_es.properties");

	/**
	 * The class containing resource constants.
	 */
	private static final File RESOURCE_CLASS = new File("src/de/eisfeldj/augendiagnosefx/util/ResourceConstants.java");

	/**
	 * Suffix for file while writing.
	 */
	private static final String FILE_SUFFIX = ".tmp";

	/**
	 * An instance of this class.
	 */
	private static StringConverter mInstance;

	/**
	 * The SAX parser factory used.
	 */
	private SAXParserFactory mFactory = SAXParserFactory.newInstance();

	/**
	 * Private constructor.
	 */
	private StringConverter() {
		// do nothing.
	}

	/**
	 * Main method.
	 *
	 * @param args
	 *            The command line arguments.
	 */
	public static void main(final String[] args) {
		mInstance = new StringConverter();

		mInstance.process();
	}

	/**
	 * Process the resource files and update valus from XML files, as well as ResourceConstants.
	 */
	private void process() {
		File tmpPropFile = new File(PROP_FILE_GLOBAL.getPath() + FILE_SUFFIX);
		File tmpPropFileDe = new File(PROP_FILE_DE.getPath() + FILE_SUFFIX);
		File tmpPropFileEs = new File(PROP_FILE_ES.getPath() + FILE_SUFFIX);

		PROP_FILE_GLOBAL.renameTo(tmpPropFile);
		PROP_FILE_DE.renameTo(tmpPropFileDe);
		PROP_FILE_ES.renameTo(tmpPropFileEs);

		try (InputStream reader = new FileInputStream(tmpPropFile);
				Writer writer = new FilteredFileWriter(PROP_FILE_GLOBAL);
				InputStream readerDe = new FileInputStream(tmpPropFileDe);
				Writer writerDe = new FilteredFileWriter(PROP_FILE_DE);
				InputStream readerEs = new FileInputStream(tmpPropFileEs);
				Writer writerEs = new FilteredFileWriter(PROP_FILE_ES)) {

			AlphabeticProperties props = new AlphabeticProperties();
			props.load(reader);

			AlphabeticProperties propsDe = new AlphabeticProperties();
			propsDe.load(readerDe);

			AlphabeticProperties propsEs = new AlphabeticProperties();
			propsEs.load(readerEs);

			Enumeration<?> e = props.propertyNames();

			while (e.hasMoreElements()) {
				String key = (String) e.nextElement();
				for (File xmlFile : XML_FILES_GLOBAL) {
					String xmlValue = getValueFromXml(xmlFile, key);
					if (xmlValue != null) {
						props.setProperty(key, xmlValue);
					}
				}
				for (File xmlFile : XML_FILES_DE) {
					String xmlValueDe = getValueFromXml(xmlFile, key);
					if (xmlValueDe != null) {
						propsDe.setProperty(key, xmlValueDe);
					}
				}
				for (File xmlFile : XML_FILES_ES) {
					String xmlValueEs = getValueFromXml(xmlFile, key);
					if (xmlValueEs != null) {
						propsEs.setProperty(key, xmlValueEs);
					}
				}
			}

			props.store(writer, "String resources");
			propsDe.store(writerDe, "String resources DE");
			propsEs.store(writerEs, "String resources ES");

			createResourceConstants(props);
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		tmpPropFile.delete();
		tmpPropFileDe.delete();
		tmpPropFileEs.delete();
	}

	/**
	 * Create the resource constants class.
	 *
	 * @param props
	 *            The properties from which to create resource constants.
	 */
	private void createResourceConstants(final Properties props) {
		String header = "package de.eisfeldj.augendiagnosefx.util;\n\n"
				+ "/**\n"
				+ " * Constants for resource strings.\n"
				+ " */\n"
				+ "public final class ResourceConstants {\n\n"
				+ "	/**\n"
				+ "	 * Private default constructor.\n"
				+ "	 */\n"
				+ "	private ResourceConstants() {\n"
				+ "		// do nothing.\n"
				+ "	}\n\n"
				+ "	// JAVADOC:OFF\n";
		String footer = "	// JAVADOC:ON\n}";

		File tmpResourceClass = new File(RESOURCE_CLASS.getAbsolutePath() + FILE_SUFFIX);
		RESOURCE_CLASS.renameTo(tmpResourceClass);

		try (PrintWriter writer = new PrintWriter(RESOURCE_CLASS)) {
			writer.print(header);

			Enumeration<?> e = props.keys();

			while (e.hasMoreElements()) {
				String key = (String) e.nextElement();

				writer.println("	public static final String " + key.toUpperCase() + " = \"" + key + "\";");
			}

			writer.println(footer);
		}
		catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		tmpResourceClass.delete();
	}

	/**
	 * Get a String value from an android String file.
	 *
	 * @param file
	 *            The file.
	 * @param key
	 *            The key.
	 * @return The String value.
	 */
	private String getValueFromXml(final File file, final String key) {
		String result = null;

		SAXParser parser;
		try {
			parser = mFactory.newSAXParser();
			SaxHandler handler = new SaxHandler(key);
			parser.parse(file, handler);
			result = handler.getValue();
		}
		catch (ParserConfigurationException | SAXException | IOException e) {
			e.printStackTrace();
		}

		return result;
	}

	/**
	 * Convert the non-ISO characters of an input String into its unicode representation.
	 *
	 * @param input
	 *            The input String
	 * @return The converted String.
	 */
	private static String convertToUnicode(final String input) {
		StringBuffer result = new StringBuffer();
		for (char character : input.toCharArray()) {
			String hexString = Integer.toHexString(character | 0x10000).substring(1); // MAGIC_NUMBER
			if (hexString.startsWith("00")) {
				result.append(character);
			}
			else {
				result.append("\\u" + hexString);
			}
		}

		return result.toString();
	}

	/**
	 * SAX handler to parse the strings.xml file.
	 */
	private static final class SaxHandler extends DefaultHandler {
		/**
		 * Storage for the resource key.
		 */
		private String mKey = null;
		/**
		 * Storage for the resource value.
		 */
		private String mValue = null;

		/**
		 * The String to be looked for.
		 */
		private String mSearchKey = null;

		/**
		 * Constructor passing a search key.
		 *
		 * @param searchKey
		 *            The String to be looked for.
		 */
		private SaxHandler(final String searchKey) {
			this.mSearchKey = searchKey;
		}

		@Override
		public void startElement(final String uri, final String localName, final String qName,
				final Attributes attributes) throws SAXException {
			if ("string".equals(qName)) {
				mKey = attributes.getValue("name");
			}
		}

		@Override
		public void characters(final char[] ch, final int start, final int length) throws SAXException {
			if (mKey != null && mKey.equals(mSearchKey)) {
				mValue = new String(ch, start, length);
			}
		}

		@Override
		public void endElement(final String uri, final String localName,
				final String qName) throws SAXException {
			if ("string".equals(qName)) {
				mKey = null;
			}
		}

		/**
		 * Getter for the found value.
		 *
		 * @return The found value.
		 */
		public String getValue() {
			return mValue;
		}
	}

	/**
	 * Variant of properties that stores values in alphabetical order.
	 */
	private static class AlphabeticProperties extends Properties {
		/**
		 * The serial version UID.
		 */
		private static final long serialVersionUID = 1L;

		@Override
		public synchronized Enumeration<Object> keys() {
			return Collections.enumeration(new TreeSet<>(super.keySet()));
		}
	}

	/**
	 * Utility writer to remove the date string from the Properties output and to convert non-iso Strings to unicode.
	 */
	private static class FilteredFileWriter extends BufferedWriter {
		/**
		 * Constructor.
		 *
		 * @param file
		 *            The file to be written.
		 * @throws IOException
		 *             thrown if there are issues writing the file.
		 */
		@SuppressWarnings("resource")
		protected FilteredFileWriter(final File file) throws IOException {
			super(new OutputStreamWriter(new FileOutputStream(file), Charset.forName("ISO8859-1").newEncoder()));
		}

		@Override
		public void write(final String str) throws IOException {
			if (!str.startsWith("#") || !str.matches("#.*[0-9][0-9]:[0-9][0-9]:[0-9][0-9].*")) {
				super.write(convertToUnicode(str));
			}
		}
	}

}
