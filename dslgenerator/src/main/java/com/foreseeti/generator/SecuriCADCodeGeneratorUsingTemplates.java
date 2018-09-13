package com.foreseeti.generator;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;

import se.kth.mal.Asset;
import se.kth.mal.CompilerModel;

public class SecuriCADCodeGeneratorUsingTemplates extends SecuriCADCodeGenerator {
   public SecuriCADCodeGeneratorUsingTemplates(String malFilePath, String testCasesFolder, String javaFolder, String packageName) throws IllegalArgumentException {
      super(malFilePath, testCasesFolder, javaFolder, packageName);
   }

   protected void writeJava(String outputFolder, String packageName, String packagePath) throws IOException, UnsupportedEncodingException {
      // Create the path unless it already exists
      String path = outputFolder + "/" + packagePath + "/";
      (new File(path)).mkdirs();
      for (Asset asset : model.getAssets()) {
         System.out.print("Writing the Java class corresponding to asset " + asset.getName() + "\n");
         String sourceCodeFile = path + asset.getName() + ".java";
         writer = new PrintWriter(sourceCodeFile, "UTF-8");
         VelocityEngine ve = new VelocityEngine();
         ve.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath");
         ve.setProperty("classpath.resource.loader.class", ClasspathResourceLoader.class.getName());
         ve.setProperty("file.resource.loader.path", this.getClass().getResource("/").getPath());
         ve.init();
         Template template = ve.getTemplate("Asset.vm");
         VelocityContext context = new VelocityContext();
         context.put("model", model);
         context.put("packageName", packageName);
         context.put("importList", getImportList());
         context.put("asset", asset);
         context.put("CompilerModel", CompilerModel.class);
         template.merge(context, writer);
         writer.close();

      }
      System.out.print("Writing AutoLangLink ");
      String sourceCodeFile = path + "AutoLangLink.java";
      writer = new PrintWriter(sourceCodeFile, "UTF-8");
      VelocityEngine ve = new VelocityEngine();
      ve.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath");
      ve.setProperty("classpath.resource.loader.class", ClasspathResourceLoader.class.getName());
      ve.setProperty("file.resource.loader.path", this.getClass().getResource("/").getPath());
      ve.init();
      Template template = ve.getTemplate("AutoLangLink.vm");
      VelocityContext context = new VelocityContext();
      context.put("model", model);
      context.put("packageName", packageName);
      context.put("CompilerModel", CompilerModel.class);
      template.merge(context, writer);
      writer.close();

      System.out.print("Writing Attacker ");
      sourceCodeFile = path + "Attacker.java";
      writer = new PrintWriter(sourceCodeFile, "UTF-8");
      ve = new VelocityEngine();
      ve.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath");
      ve.setProperty("classpath.resource.loader.class", ClasspathResourceLoader.class.getName());
      ve.setProperty("file.resource.loader.path", this.getClass().getResource("/").getPath());
      ve.init();
      template = ve.getTemplate("Attacker.vm");
      context = new VelocityContext();
      context.put("model", model);
      context.put("importList", getImportList());
      context.put("packageName", packageName);
      context.put("CompilerModel", CompilerModel.class);
      template.merge(context, writer);
      writer.close();

   }
}
