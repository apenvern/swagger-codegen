package com.wordnik.swagger.codegen.languages;

import com.wordnik.swagger.codegen.*;
import com.wordnik.swagger.models.properties.*;

import java.util.*;
import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JavaClientCodegen extends DefaultCodegen implements CodegenConfig {
	
	private final static Logger LOGGER = LoggerFactory.getLogger(Codegen.class);
	
  protected String invokerPackage = "io.swagger.client";
  protected String groupId = "io.swagger";
  protected String artifactId = "swagger-client";
  protected String artifactVersion = "1.0.0";
  protected String sourceFolder = "src/main/java";

  public CodegenType getTag() {
    return CodegenType.CLIENT;
  }

  public String getName() {
    return "java";
  }

  public String getHelp() {
    return "Generates a Java client library.";
  }

	public void processOpts() {
		System.out.println("Call on processOpts ...");
		if (additionalProperties.containsKey("spe-lang-args")) {
			String[] javaSpeArgs = ((String) additionalProperties
					.get("spe-lang-args")).split(";");
			for (String arg : javaSpeArgs) {
				if (arg != null && !arg.isEmpty()) {
					String[] option = arg.split("=");
					if ("groupId".equals(option[0])) {
						groupId = option[1];
						invokerPackage = option[1]+".client";
					} else if ("artifactId".equals(option[0])) {
						artifactId = option[1];
					} else if ("artifactVersion".equals(option[0])) {
						artifactVersion = option[1];
					}
				}
			}
		}
	    System.out.println("Add specific maven conf groupId : "+groupId);
	    additionalProperties.put("invokerPackage", invokerPackage);
	    additionalProperties.put("groupId", groupId);
	    additionalProperties.put("artifactId", artifactId);
	    additionalProperties.put("artifactVersion", artifactVersion);
	    apiPackage = invokerPackage+".api";
	    modelPackage = invokerPackage+".model";

	    supportingFiles.add(new SupportingFile("pom.mustache", "", "pom.xml"));
	    supportingFiles.add(new SupportingFile("apiInvoker.mustache", 
	      (sourceFolder + File.separator + invokerPackage).replace(".", java.io.File.separator), "ApiInvoker.java"));
	    supportingFiles.add(new SupportingFile("JsonUtil.mustache", 
	      (sourceFolder + File.separator + invokerPackage).replace(".", java.io.File.separator), "JsonUtil.java"));
	    supportingFiles.add(new SupportingFile("apiException.mustache", 
	      (sourceFolder + File.separator + invokerPackage).replace(".", java.io.File.separator), "ApiException.java"));
	}
  
  public JavaClientCodegen() {
    super();
    outputFolder = "generated-code/java";
    modelTemplateFiles.put("model.mustache", ".java");
    apiTemplateFiles.put("api.mustache", ".java");
    templateDir = "Java";

    reservedWords = new HashSet<String> (
      Arrays.asList(
        "abstract", "continue", "for", "new", "switch", "assert", 
        "default", "if", "package", "synchronized", "boolean", "do", "goto", "private", 
        "this", "break", "double", "implements", "protected", "throw", "byte", "else", 
        "import", "public", "throws", "case", "enum", "instanceof", "return", "transient", 
        "catch", "extends", "int", "short", "try", "char", "final", "interface", "static", 
        "void", "class", "finally", "long", "strictfp", "volatile", "const", "float", 
        "native", "super", "while")
    );

    languageSpecificPrimitives = new HashSet<String>(
      Arrays.asList(
        "String",
        "boolean",
        "Boolean",
        "Double",
        "Integer",
        "Long",
        "Float",
        "Object")
      );
    instantiationTypes.put("array", "ArrayList");
    instantiationTypes.put("map", "HashMap");
  }

  @Override
  public String escapeReservedWord(String name) {
    return "_" + name;
  }

  @Override
  public String apiFileFolder() {
    return outputFolder + "/" + sourceFolder + "/" + apiPackage().replace('.', File.separatorChar);
  }

  public String modelFileFolder() {
    return outputFolder + "/" + sourceFolder + "/" + modelPackage().replace('.', File.separatorChar);
  }

  @Override
  public String getTypeDeclaration(Property p) {
    if(p instanceof ArrayProperty) {
      ArrayProperty ap = (ArrayProperty) p;
      Property inner = ap.getItems();
      return getSwaggerType(p) + "<" + getTypeDeclaration(inner) + ">";
    }
    else if (p instanceof MapProperty) {
      MapProperty mp = (MapProperty) p;
      Property inner = mp.getAdditionalProperties();

      return getSwaggerType(p) + "<String, " + getTypeDeclaration(inner) + ">";
    }
    return super.getTypeDeclaration(p);
  }

  @Override
  public String getSwaggerType(Property p) {
    String swaggerType = super.getSwaggerType(p);
    String type = null;
    if(typeMapping.containsKey(swaggerType)) {
      type = typeMapping.get(swaggerType);
      if(languageSpecificPrimitives.contains(type))
        return toModelName(type);
    }
    else
      type = swaggerType;
    return toModelName(type);
  }
}