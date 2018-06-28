package se.kth.mal;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import se.kth.mal.CompilerModel.Asset;
import se.kth.mal.CompilerModel.Association;
import se.kth.mal.CompilerModel.AttackStep;

public class TestCaseGenerator {

   CompilerModel model;
   String        javaFolderPath;
   // PrintWriter writer;
   // String testCaseName;
   Random        ran = new Random();

   public TestCaseGenerator(String securiLangFolder, String securiLangFile, String javaFolderPath) throws IOException {

      this.model = new CompilerModel(securiLangFolder, securiLangFile);
      this.javaFolderPath = javaFolderPath;
      // TestModel testModel = generateSpecificTestModel();
      // TestModel testModel = generateUnitTest("EncryptedAuthenticatedData");
      List<String> assetNames = new ArrayList<>();
      assetNames.add("EncryptedData");
      TestModel testModel = generateMinimalSyntacticallyCorrectModel(assetNames);
      printTestCase(testModel, 1, 10);
   }

   public TestModel generateUnitTest(String assetName) {
      TestModel testModel = new TestModel();
      testModel.addInstance(model.getAsset(capitalize(assetName)), decapitalize(assetName));
      return testModel;
   }

   public TestModel generateMinimalSyntacticallyCorrectModel(List<String> assetNames) {
      TestModel testModel = new TestModel();
      List<Instance> instances = new ArrayList<>();

      for (String assetName : assetNames) {
         instances.add(testModel.addInstance(model.getAsset(capitalize(assetName)), decapitalize(assetName)));
      }

      List<Instance> newInstances = new ArrayList<>();
      for (Instance instance : instances) {
         List<Association> associations = instance.asset.getAssociationsIncludingInherited();
         for (Association association : associations) {
            String targetMultiplicity = (String) association.targetMultiplicityIncludingInheritance(instance.asset);
            // Find any required associations
            if (targetMultiplicity.equals("1") || targetMultiplicity.equals("1-*")) {
               String targetAssetName = association.getTargetAssetName(instance.asset);
               List<Instance> existingInstanceOfTargetType = instances.stream().filter(i -> i.asset.name.equals(targetAssetName)).collect(Collectors.toList());
               // Link to existing instances, if available...
               if (existingInstanceOfTargetType.size() > 0) {
                  testModel.addLink(instance, existingInstanceOfTargetType.get(0), association.name);
               }
               // ...otherwise, create new instances and link
               else {
                  Instance newInstance = testModel.addInstance(model.getAsset(capitalize(targetAssetName)), decapitalize(targetAssetName));
                  newInstances.add(newInstance);
                  testModel.addLink(instance, newInstance, association.name);
               }
            }
         }
      }
      return testModel;
   }

   public TestModel generateSpecificTestModel() {
      TestModel testModel = new TestModel();
      Integer n = 2;
      List<Instance> dataflows = testModel.addInstances(model.getAsset("Dataflow"), "dataflow", n);
      List<Instance> encryptedAuthenticatedData = testModel.addInstances(model.getAsset("EncryptedAuthenticatedData"), "encryptedAuthenticatedData", n);
      Instance information = testModel.addInstance(model.getAsset("Information"), "information");
      for (Integer i = 0; i < n; i++) {
         testModel.addLink(dataflows.get(i), encryptedAuthenticatedData.get(i), "Transmission");
         testModel.addLink(information, encryptedAuthenticatedData.get(i), "Representation");
      }
      return testModel;
   }

   public void printTestCase(TestModel testModel, Integer nAttackPoints, Integer maxAssertions) {

      String testCaseName = createTestCaseName(testModel);
      PrintWriter writer = createTestCaseFile(testCaseName);
      writer.print(intro(testCaseName));
      for (Instance instance : testModel.instances) {
         writer.print(instance.declaration());
      }
      for (Link link : testModel.links) {
         writer.print(link.declaration());
      }
      writer.print(attackPoint(testModel, nAttackPoints));

      writer.print(assertions(testModel, maxAssertions));

      writer.print(outro());
      writer.close();
      System.out.println("Created Test" + testCaseName + ".java");
   }

   protected String intro(String testCaseName) {
      String string = "";
      string += "import org.junit.Test;\n";
      string += "import core.*;\n";
      string += "import auto.*;\n";
      string += "public class Test" + testCaseName + " extends AbstractTest {\n";
      string += "   @Test\n";
      string += "   public void test" + testCaseName + "() {\n";
      return string;
   }

   protected String createTestCaseName(TestModel testModel) {
      String testCaseNameBase = "";
      for (Instance instance : testModel.instances) {
         testCaseNameBase += capitalize(instance.name.substring(0, 3));
      }
      testCaseNameBase = testCaseNameBase.substring(0, Math.min(15, testCaseNameBase.length()));
      Integer iTest = 1;
      String testCaseName = testCaseNameBase + iTest.toString();
      String testCodeFileName = javaFolderPath + "/test/Test" + testCaseName + ".java";
      File testCodeFile = new File(testCodeFileName);
      while (testCodeFile.exists()) {
         iTest++;
         testCaseName = testCaseNameBase + iTest.toString();
         testCodeFileName = javaFolderPath + "/test/Test" + testCaseName + ".java";
         testCodeFile = new File(testCodeFileName);
      }
      return testCaseName;
   }

   protected PrintWriter createTestCaseFile(String testCaseName) {
      String testCaseFileName = javaFolderPath + "/test/Test" + testCaseName + ".java";
      PrintWriter writer = null;

      try {
         writer = new PrintWriter(testCaseFileName, "UTF-8");
      }
      catch (FileNotFoundException e) {
         System.out.println("FileNotFoundException");
         e.printStackTrace();
      }
      catch (UnsupportedEncodingException e) {
         System.out.println("UnsupportedEncodingException");
         e.printStackTrace();
      }
      return writer;
   }

   protected String attackPoint(TestModel testModel, Integer nAttackPoints) {
      String s = "";
      s += "\n      Attacker attacker = new Attacker();\n";
      for (Integer i = 0; i < nAttackPoints; i++) {
         Instance instance = testModel.instances.get(ran.nextInt(testModel.instances.size()));
         List<AttackStep> aSteps = instance.asset.attackStepsExceptDefensesAndExistence();
         int iRandomAttackStep = ran.nextInt(aSteps.size());
         AttackStep randomAttackPoint = aSteps.get(iRandomAttackStep);
         s += "      attacker.addAttackPoint(" + instance.name + "." + randomAttackPoint.name + ");\n";
      }
      s += "      attacker.attack();\n\n";
      return s;
   }

   protected String assertions(TestModel testModel, Integer maxAssertions) {
      String string = "";
      List<String> assertionStrings = new ArrayList<>();
      for (Instance instance : testModel.instances) {
         for (AttackStep attackStep : instance.asset.attackStepsExceptDefensesAndExistence()) {
            assertionStrings.add("      " + instance.name + "." + attackStep.name + ".assertUncompromised();\n");
         }
      }
      Collections.shuffle(assertionStrings);
      for (Integer i = 0; i < Math.min(maxAssertions, assertionStrings.size()); i++) {
         string += assertionStrings.get(i);
      }
      return string;
   }

   protected String outro() {
      String string = "";
      string += " }\n";
      string += "}\n";
      return string;
   }

   class TestModel {
      List<Instance> instances = new ArrayList<>();
      List<Link>     links     = new ArrayList<>();

      public Instance addInstance(Asset asset, String instanceName) {
         Instance instance = new Instance(asset, instanceName);
         instances.add(instance);
         return instance;
      }

      public List<Instance> addInstances(Asset asset, String instanceNameBase, Integer nInstances) {
         List<Instance> newInstances = new ArrayList<>();
         for (Integer iInstance = 0; iInstance < nInstances; iInstance++) {
            Instance instance = new Instance(asset, instanceNameBase + iInstance.toString());
            instances.add(instance);
            newInstances.add(instance);
         }
         return newInstances;
      }

      public Link addLink(Instance source, Instance target, String name) {
         Link link = new Link(source, target, name);
         links.add(link);
         return link;
      }

   }

   class Link {
      Instance    source;
      Instance    target;
      Association association;

      public Link(Instance source, Instance target, String associationName) {
         this.source = source;
         this.target = target;
         association = model.getAssociation(source.asset.name, associationName, target.asset.name);
         assertTrue(association != null);
      }

      public String declaration() {
         String string = "";
         String targetRoleName = association.getTargetRoleName(source.asset);
         string += "      " + source.name + ".add" + capitalize(targetRoleName) + "(" + target.name + ");\n";
         return string;
      }
   }

   class Instance {
      Asset  asset;
      String name;

      public Instance(Asset asset, String instanceName) {
         this.asset = asset;
         this.name = instanceName;
      }

      public String declaration() {
         String declarationString = "";
         declarationString += "      " + asset.name + " " + name + " = new " + asset.name + "(";
         for (AttackStep defense : asset.defenses()) {
            if (ran.nextBoolean()) {
               declarationString += "true, ";
            }
            else {
               declarationString += "false, ";
            }
         }
         if (!asset.defenses().isEmpty()) {
            declarationString = backtrack(declarationString);
         }
         declarationString += ");\n";
         return declarationString;
      }
   }

   private String backtrack(String s) {
      return s.substring(0, s.length() - 2);
   }

   private String capitalize(final String line) {
      return Character.toUpperCase(line.charAt(0)) + line.substring(1);
   }

   private String decapitalize(final String line) {
      return Character.toLowerCase(line.charAt(0)) + line.substring(1);
   }

   public static void main(String[] args) throws Exception {
      String securiLangFolder = args[0].substring(0, args[0].lastIndexOf('/'));
      String securiLangFile = args[0].substring(args[0].lastIndexOf('/') + 1);
      String javaFolderPath = args[1];
      List<String> assetStringList = new ArrayList<>();
      assetStringList.add("EncryptedAuthenticatedData");
      assetStringList.add("Dataflow");
      TestCaseGenerator testCaseGenerator = new TestCaseGenerator(securiLangFolder, securiLangFile, javaFolderPath);
   }

}
