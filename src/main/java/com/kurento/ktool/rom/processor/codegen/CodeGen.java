package com.kurento.ktool.rom.processor.codegen;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import com.google.gson.JsonObject;
import com.kurento.ktool.rom.processor.codegen.function.CamelToUnderscore;
import com.kurento.ktool.rom.processor.codegen.function.CppObjectType;
import com.kurento.ktool.rom.processor.codegen.function.EscapeString;
import com.kurento.ktool.rom.processor.codegen.function.IsFirstConstructorParam;
import com.kurento.ktool.rom.processor.codegen.function.JavaObjectType;
import com.kurento.ktool.rom.processor.codegen.function.JsNamespace;
import com.kurento.ktool.rom.processor.codegen.function.JsonCppTypeData;
import com.kurento.ktool.rom.processor.codegen.function.PackageToFolder;
import com.kurento.ktool.rom.processor.codegen.function.RemoteClassDependencies;
import com.kurento.ktool.rom.processor.codegen.function.SphinxLinks;
import com.kurento.ktool.rom.processor.model.Model;
import com.kurento.ktool.rom.processor.model.Type;

import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;

public class CodeGen {

	private final Path templatesFolder;
	private final Path outputFolder;
	private final Configuration cfg;

	private final boolean listGeneratedFiles;
	private final boolean verbose;
	private final boolean overwrite;
	private final JsonObject config;

	public CodeGen(Path templatesFolder, Path outputFolder, boolean verbose,
			boolean listGeneratedFiles, boolean overwrite, JsonObject config)
			throws IOException {

		this.verbose = verbose;
		this.listGeneratedFiles = listGeneratedFiles;
		this.overwrite = overwrite;
		this.templatesFolder = templatesFolder;
		this.outputFolder = outputFolder;
		this.config = config;

		cfg = new Configuration();

		// Specify the data source where the template files come from. Here I
		// set a
		// plain directory for it, but non-file-system are possible too:
		cfg.setTemplateLoader(new PathTemplateLoader(templatesFolder));

		// Specify how templates will see the data-model. This is an advanced
		// topic...
		// for now just use this:
		cfg.setObjectWrapper(new DefaultObjectWrapper());

		// Set your preferred charset template files are stored in. UTF-8 is
		// a good choice in most applications:
		cfg.setDefaultEncoding("UTF-8");

		// Sets how errors will appear. Here we assume we are developing HTML
		// pages.
		// For production systems TemplateExceptionHandler.RETHROW_HANDLER is
		// better.
		cfg.setTemplateExceptionHandler(TemplateExceptionHandler.DEBUG_HANDLER);

		// At least in new projects, specify that you want the fixes that aren't
		// 100% backward compatible too (these are very low-risk changes as far
		// as the
		// 1st and 2nd version number remains):
		// cfg.setIncompatibleImprovements(new Version(2, 3, 19)); // FreeMarker
		// 2.3.19

	}

	public void generateCode(Model model) throws IOException, TemplateException {

		try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(
				templatesFolder, "*.ftl")) {

			for (Path path : directoryStream) {
				String name = path.getFileName().toString();
				String modelType = name.split("_")[0];

				generateCode(name, model, modelType);
			}
		}
	}

	private void generateCode(String templateName, Model model, String modelType)
			throws TemplateException, IOException {

		Template temp = cfg.getTemplate(templateName);

		List<? extends Type> types;
		if (modelType.equals("remoteClass")) {
			types = model.getRemoteClasses();
		} else if (modelType.equals("complexType")) {
			types = model.getComplexTypes();
		} else if (modelType.equals("event")) {
			types = model.getEvents();
		} else if (modelType.equals("model")) {
			types = null;
		} else {
			throw new RuntimeException(
					"Unknown model element: '"
							+ modelType
							+ "'. It should be 'remoteClass' or 'complexType' or 'event'");
		}

		Map<String, Object> root = new HashMap<String, Object>();
		root.put("getJavaObjectType", new JavaObjectType());
		root.put("getCppObjectType", new CppObjectType());
		root.put("getJsonCppTypeData", new JsonCppTypeData());
		root.put("escapeString", new EscapeString());
		root.put("camelToUnderscore", new CamelToUnderscore());
		root.put("remoteClassDependencies", new RemoteClassDependencies());
		root.put("isFirstConstructorParam", new IsFirstConstructorParam());
		root.put("sphinxLinks", new SphinxLinks());
		root.put("getJsNamespace", new JsNamespace());
		root.put("packageToFolder", new PackageToFolder());

		root.put("model", model);
		if (this.config != null) {
			JsonObjectAsMap mapper = new JsonObjectAsMap();
			root.put("config", mapper.createMapFromJsonObject(config));
		} else {
			root.put("config", Collections.emptyMap());
		}

		if (types == null) {
			generateFile(temp, root);
		} else {

			for (Type type : types) {

				if (modelType.equals("remoteClass")) {
					root.put("remoteClass", type);
				} else if (modelType.equals("complexType")) {
					root.put("complexType", type);
				} else if (modelType.equals("event")) {
					root.put("event", type);
				}

				generateFile(temp, root);
			}
		}
	}

	private void generateFile(Template temp, Map<String, Object> root)
			throws TemplateException, IOException {

		StringWriter out = new StringWriter();
		temp.process(root, out);
		String tempOutput = out.toString();

		if (tempOutput.isEmpty()) {
			System.out.println("No file generation because applying template '"
					+ temp.getName() + "' is empty");
			return;
		}

		StringTokenizer st = new StringTokenizer(tempOutput);

		String fileName = st.nextToken();

		File outputFile = new File(outputFolder.toFile(), fileName);

		if (!outputFile.getParentFile().exists()) {
			outputFile.getParentFile().mkdirs();
		}

		String sourceCode = tempOutput.substring(fileName.length() + 1,
				tempOutput.length());

		boolean generateFile = !outputFile.exists();
		if (outputFile.exists() && overwrite) {
			generateFile = true;
			String oldContent = readFile(outputFile);

			if (oldContent.equals(sourceCode)) {
				generateFile = false;
			}
		}

		if (generateFile) {
			Writer writer = new FileWriter(outputFile);
			writer.write(sourceCode);
			writer.close();
		}

		if (verbose) {
			System.out.println("File: " + fileName);
			System.out.println();
			System.out.println(sourceCode);
			System.out.println("---------------------------------------");
		}

		if (listGeneratedFiles) {
			System.out.print("Processed file:\t" + fileName);
			if (!generateFile) {
				System.out.println("\t(not generated)");
			} else {
				System.out.println();
			}
		}
	}

	public static String readFile(File file) throws IOException {
		int len;
		char[] chr = new char[1024];
		final StringBuffer buffer = new StringBuffer();
		final FileReader reader = new FileReader(file);
		try {
			while ((len = reader.read(chr)) > 0) {
				buffer.append(chr, 0, len);
			}
		} finally {
			reader.close();
		}
		return buffer.toString();
	}
}
