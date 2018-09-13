package com.foreseeti.generator;

import java.io.IOException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class Master {

   public Master(String malFilePath, String testsOutFolderPath, String javaOutFolderPath, String packageName) {
      try {
         SecuriCADCodeGeneratorUsingTemplates generator = new SecuriCADCodeGeneratorUsingTemplates(malFilePath, testsOutFolderPath, javaOutFolderPath, packageName);
         generator.generate();
      }
      catch (IOException e) {
         e.printStackTrace();
      }
      System.out.println("Complete");
   }

   public static void main(String[] args) throws Exception {

      Options options = new Options();

      Option input = new Option("i", "input", true, "input mal file path");
      input.setRequired(true);
      options.addOption(input);

      Option output = new Option("o", "output", true, "output folder path for generated code");
      output.setRequired(true);
      options.addOption(output);

      Option tests = new Option("t", "tests", true, "output folder path for generated test code");
      tests.setRequired(false);
      options.addOption(tests);

      Option packageName = new Option("p", "package", true, "package name of generated code");
      packageName.setRequired(true);
      options.addOption(packageName);
      CommandLineParser parser = new DefaultParser();
      HelpFormatter formatter = new HelpFormatter();
      CommandLine cmd = null;

      try {
         cmd = parser.parse(options, args);
         Master master = new Master(cmd.getOptionValue("input").trim(), cmd.getOptionValue("tests"), cmd.getOptionValue("output").trim(), cmd.getOptionValue("package").trim());
      }
      catch (ParseException e) {
         System.err.println(e.getMessage());
         formatter.printHelp("utility-name", options);
      }

   }
}
