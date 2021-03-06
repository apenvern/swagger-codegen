package com.wordnik.swagger.codegen;

import io.swagger.parser.SwaggerParser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.wordnik.swagger.models.Swagger;

public class Codegen extends DefaultGenerator {
  
	private final static Logger LOGGER = LoggerFactory.getLogger(Codegen.class);
	
  static Map<String, CodegenConfig> configs = new HashMap<String, CodegenConfig>();
  static String configString;
  static {
    List<CodegenConfig> extensions = getExtensions();
    StringBuilder sb = new StringBuilder();

    for(CodegenConfig config : extensions) {
      if(sb.toString().length() != 0)
        sb.append(", ");
      sb.append(config.getName());
      configs.put(config.getName(), config);
      configString = sb.toString();
    }
  }

  static String debugInfoOptions = "\nThe following additional debug options are available for all codegen targets:" +
    "\n -DdebugSwagger prints the swagger specification as interpreted by the codegen" +
    "\n -DdebugModels prints models passed to the template engine" +
    "\n -DdebugOperations prints operations passed to the template engine" +
    "\n -DdebugSupportingFiles prints additional data passed to the template engine";
  public static void main(String[] args) {

    StringBuilder sb = new StringBuilder();

    Options options = new Options();
    options.addOption("h", "help", false, "shows this message");
    options.addOption("l", "lang", true, "client language to generate.\nAvailable languages include:\n\t[" + configString + "]");
    options.addOption("o", "output", true, "where to write the generated files");
    options.addOption("i", "input-spec", true, "location of the swagger spec, as URL or file");
    options.addOption("t", "template-dir", true, "folder containing the template files");
    options.addOption("d", "debug-info", false, "prints additional info for debugging");
    options.addOption("a", "auth", true, "adds authorization headers when fetching the swagger definitions remotely. Pass in a URL-encoded string of name:header with a comma separating multiple values");
    options.addOption("s", "spe-lang-args", true, "Specific args for the lang.");

    ClientOptInput clientOptInput = new ClientOptInput();
    ClientOpts clientOpts = new ClientOpts();
    Swagger swagger = null;

    CommandLine cmd = null;
    try {
      CommandLineParser parser = new BasicParser();
      CodegenConfig config = null;

      cmd = parser.parse(options, args);
     
      if (cmd.hasOption("d")) {
        usage(options);
        System.out.println(debugInfoOptions);
        return;
      }
      if (cmd.hasOption("a"))
        clientOptInput.setAuth(cmd.getOptionValue("a"));
      if (cmd.hasOption("l"))
        clientOptInput.setConfig(getConfig(cmd.getOptionValue("l")));
      else {
        usage(options);
        return;
      }
      if (cmd.hasOption("o"))
        clientOptInput.getConfig().setOutputDir(cmd.getOptionValue("o"));
      if (cmd.hasOption("h")) {
        if(cmd.hasOption("l")) {
          config = getConfig(String.valueOf(cmd.getOptionValue("l")));
          if(config != null) {
            options.addOption("h", "help", true, config.getHelp());
            usage(options);
            return;
          }
        }
        usage(options);
        return;
      }
      if (cmd.hasOption("i"))
        swagger = new SwaggerParser().read(cmd.getOptionValue("i"), clientOptInput.getAuthorizationValues(), true);
      if (cmd.hasOption("t"))
        clientOpts.getProperties().put("templateDir", String.valueOf(cmd.getOptionValue("t")));
      if (cmd.hasOption("s"))
       	  clientOpts.getProperties().put("spe-lang-args", String.valueOf(cmd.getOptionValue("s")));
    }
    catch (Exception e) {
      usage(options);
      return;
    }
    try{
      clientOptInput
        .opts(clientOpts)
        .swagger(swagger);
      new Codegen().opts(clientOptInput).generate();
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }

  public static List<CodegenConfig> getExtensions() {
    ServiceLoader<CodegenConfig> loader = ServiceLoader.load(CodegenConfig.class);
    List<CodegenConfig> output = new ArrayList<CodegenConfig>();
    Iterator<CodegenConfig> itr = loader.iterator();
    while(itr.hasNext()) {
      output.add(itr.next());
    }
    return output;
  }

  static void usage(Options options) {
    HelpFormatter formatter = new HelpFormatter();
    formatter.printHelp( "Codegen", options );
  }

  public static CodegenConfig getConfig(String name) {
    if(configs.containsKey(name)) {
      return configs.get(name);
    }
    else {
      // see if it's a class
      try {
        System.out.println("loading class " + name);
        Class customClass = Class.forName(name);
        System.out.println("loaded");
        return (CodegenConfig)customClass.newInstance();
      }
      catch (Exception e) {
        throw new RuntimeException("can't load class " + name);
      }
    }
  }
}
