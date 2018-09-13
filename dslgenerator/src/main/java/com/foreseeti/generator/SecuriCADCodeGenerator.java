package com.foreseeti.generator;

import static org.junit.Assert.assertTrue;

//Iff any attack step (internal or external) points to an inherited attack step in an asset:
//Create a specialization of that attack step, in the specialized asset.
//Override the setExpectedParents() method, including the pointing attack step as parent in the specialization.
//(In the generalized attack step, the pointing attack step should not be included).
//Don't instantiate the attack step in the generalized class but instead in the constructor.

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import se.kth.mal.Asset;
import se.kth.mal.Association;
import se.kth.mal.AttackStep;
import se.kth.mal.AttackStepPointer;
import se.kth.mal.CompilerModel;

// The JavaWriter produces executable Java code for securiCAD simulator.
public class SecuriCADCodeGenerator {

   protected PrintWriter   writer;
   protected String        securiLangFolder;
   protected String        securiLangFile;
   protected String        testCasesFolder;
   protected String        jsonString = "";
   protected CompilerModel model;
   protected String        packageName;
   protected String        javaFolder;

   protected Integer       associationIndex;

   protected String package2path(String packageName) throws IllegalArgumentException {
      if (!packageName.matches("\\w+(\\.\\w+)*$")) {
         throw (new IllegalArgumentException(String.format("'%s' is not a valid package name", packageName)));
      }

      return packageName.replace('.', File.separatorChar);
   }

   public SecuriCADCodeGenerator(String securiLangFile, String testCasesFolder, String javaFolder, String packageName) throws IllegalArgumentException {
      this.securiLangFile = securiLangFile;
      this.testCasesFolder = testCasesFolder;
      this.packageName = packageName;
      this.javaFolder = javaFolder;

      if (securiLangFile == null || securiLangFile.equals("")) {
         throw new IllegalArgumentException("Missing MAL file path");
      }
      if (javaFolder == null || javaFolder.equals("")) {
         throw new IllegalArgumentException("Missing java Output FolderPath file path");
      }

      File malFile = new File(securiLangFile);
      if (!malFile.exists() || !malFile.isFile()) {
         throw new IllegalArgumentException("Bad MAL file path " + malFile.getAbsolutePath());
      }

      File outPut = new File(javaFolder);
      if (!outPut.exists() || !outPut.isDirectory()) {
         throw new IllegalArgumentException("Bad output folder path");
      }

      if (testCasesFolder != null && !testCasesFolder.equals("")) {
         File testCaseOut = new File(testCasesFolder);
         if (!testCaseOut.exists() || !testCaseOut.isDirectory()) {
            throw new IllegalArgumentException("Bad test cases output folder path");
         }
      }
      this.securiLangFolder = new File(malFile.getAbsolutePath()).getParentFile().getAbsolutePath();

   }

   public void generate() throws IOException, IllegalArgumentException {
      String packagePath = package2path(packageName);
      model = new CompilerModel(securiLangFolder, securiLangFile);
      writeJava(javaFolder, packageName, packagePath);
   }

   private String capitalize(final String line) {
      return Character.toUpperCase(line.charAt(0)) + line.substring(1);
   }

   private String decapitalize(final String line) {
      return Character.toLowerCase(line.charAt(0)) + line.substring(1);
   }

   private String readFile(String filePath) throws IOException {
      String contents;
      try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
         StringBuilder sb = new StringBuilder();
         String line = br.readLine();

         while (line != null) {
            sb.append(line);
            sb.append(System.lineSeparator());
            line = br.readLine();
         }
         contents = sb.toString();
      }
      return contents;
   }

   protected List<String> getImportList() {
      List<String> importsList = new ArrayList<String>();
      importsList.add("com.foreseeti.simulator.*");
      importsList.add("com.foreseeti.simulator.ConcreteSample");
      importsList.add("com.foreseeti.corelib.BaseSample");
      importsList.add("com.foreseeti.corelib.FClass");
      importsList.add("com.foreseeti.corelib.FAnnotations.TypeDescription");
      importsList.add("com.foreseeti.corelib.FAnnotations.TypeName");
      importsList.add("com.foreseeti.corelib.DefaultValue");
      importsList.add("com.foreseeti.simulator.Defense");
      importsList.add("com.foreseeti.simulator.AttackStep");
      importsList.add("com.google.common.collect.ImmutableSet");
      importsList.add("com.foreseeti.simulator.MultiParentAsset");
      importsList.add("com.foreseeti.simulator.BaseLangLink");
      importsList.add("com.foreseeti.corelib.AssociationManager");
      importsList.add("com.foreseeti.corelib.util.FProbSet");
      importsList.add("com.foreseeti.corelib.util.FProb");
      importsList.add("com.foreseeti.corelib.FAnnotations.*");
      importsList.add("java.util.ArrayList");
      importsList.add("java.util.HashSet");
      importsList.add("java.util.List");
      importsList.add("java.util.Set");
      importsList.add("com.foreseeti.corelib.BaseSample");
      importsList.add("com.foreseeti.simulator.Asset");
      importsList.add("com.foreseeti.simulator.AttackStep");
      importsList.add("com.foreseeti.simulator.AttackStepMax");
      importsList.add("com.foreseeti.simulator.AttackStepMin");
      importsList.add("com.foreseeti.simulator.Defense;");
      return importsList;
   }

   protected void writeJava(String outputFolder, String packageName, String packagePath) throws IOException, UnsupportedEncodingException {

      // Create the path unless it already exists
      String path = outputFolder + "/" + packagePath + "/";
      (new File(path)).mkdirs();

      for (Asset asset : model.getAssets()) {
         System.out.print("Writing the Java class corresponding to asset " + asset.getName() + "\n");
         String sourceCodeFile = path + asset.getName() + ".java";
         writer = new PrintWriter(sourceCodeFile, "UTF-8");
         printPackage(packageName);
         printImports();
         printAssetClassHeaders(asset);
         printDefaultClassMembers(asset);
         printAssociations(asset);
         printStepAssignments(asset);
         printConstructor(asset);
         printStepDefinitions(asset);
         printConnectionHelpers(asset);
         printDefaultOverriddenMethods(asset);
         printLocalAttackStepSpecialization(asset);
         writer.println("}");
         writer.close();
      }

      System.out.println("writing lang link file AutoLangLink.java");

      String sourceCodeFile = path + "AutoLangLink.java";
      writer = new PrintWriter(sourceCodeFile, "UTF-8");
      writer.println("package auto;");
      writer.println("import com.foreseeti.corelib.Link;");
      writer.println("public enum AutoLangLink implements Link {");

      String coma = "";
      for (Association association : model.getLinks().keySet()) {
         String link_text = model.getLinks().get(association);
         writer.print(coma);
         writer.println(String.format("%s(\"%s\")", link_text, association.getName()));
         coma = ",";
      }
      writer.println(";");
      writer.println("private final String name;");
      writer.println("AutoLangLink(String name) {");
      writer.println("this.name = name;");
      writer.println("}");
      writer.println("@Override");
      writer.println("public String getName() {");
      writer.println("return name;");
      writer.println("}");
      writer.println("}");
      writer.close();

      sourceCodeFile = path + "Attacker.java";
      writer = new PrintWriter(sourceCodeFile, "UTF-8");
      createDefaultAttacker();
      writer.close();
   }

   void printDefaultClassMembers(Asset asset) {
      if (asset.getSuperAssetName().isEmpty()) {
         writer.println("protected ImmutableSet<AttackStep> attackSteps;");
         writer.println("protected ImmutableSet<Defense> defenses;");
      }
   }

   void printDefaultOverriddenMethods(Asset asset) {
      writer.println("@Override");
      writer.println("public ImmutableSet<AttackStep> getAttackSteps() {");
      writer.println("return attackSteps;");
      writer.println("}");
      writer.println("@Override");
      writer.println("public ImmutableSet<Defense> getDefenses() {");
      writer.println("return defenses;");
      writer.println("}");
      writer.println("@Override");
      writer.println("public String getDescription() {");
      writer.println("return \"\";");
      writer.println("}");
   }

   void printPackage(String packageName) {
      writer.println("package " + packageName + ";\n");
   }

   void printImports() {
      String imports = "import com.foreseeti.simulator.*; " + "import com.foreseeti.simulator.ConcreteSample;" + "import com.foreseeti.corelib.BaseSample;\n" + "import com.foreseeti.corelib.FClass;\n"
            + "import com.foreseeti.corelib.FAnnotations.TypeDescription;\n" + "import com.foreseeti.corelib.FAnnotations.TypeName;\n" + "import com.foreseeti.corelib.DefaultValue;\n"
            + "import com.foreseeti.simulator.Defense;\n" + "import com.foreseeti.simulator.AttackStep;\n" + "import com.google.common.collect.ImmutableSet;\n"
            + "import com.foreseeti.simulator.MultiParentAsset;\n" + "import com.foreseeti.simulator.BaseLangLink;\n" + "import com.foreseeti.corelib.AssociationManager;\n"
            + "import com.foreseeti.corelib.util.FProbSet;\n" + "import com.foreseeti.corelib.util.FProb;\n" + "import com.foreseeti.corelib.FAnnotations.*;\n" + "import java.util.ArrayList;\n"
            + "import java.util.HashSet;\n" + "import java.util.List;\n" + "import java.util.Set;\n" + "import com.foreseeti.corelib.BaseSample;\n" + "import static org.junit.Assert.assertTrue;\n"
            + "import com.foreseeti.simulator.Asset;\n" + "import com.foreseeti.simulator.AttackStep;\n" + "import com.foreseeti.simulator.AttackStepMax;\n"
            + "import com.foreseeti.simulator.AttackStepMin;\n" + "import com.foreseeti.simulator.Defense;";
      writer.println(imports);
   }

   void printAssetClassHeaders(Asset asset) {
      String mandatory = "";
      if (!asset.getMandatoryChildren().isEmpty()) {
         mandatory += ", mandatoryChildren = {";
         String coma = "";
         for (Asset manAsset : asset.getMandatoryChildren()) {
            mandatory += coma + capitalize(manAsset.getName()) + ".class";
            coma = ",";
         }
         mandatory += "}";
      }

      String nonmandatory = "";
      if (!asset.getNonMandatoryChildren().isEmpty()) {
         nonmandatory += ", nonMandatoryChildren = {";
         String coma = "";
         for (Asset nonmanAsset : asset.getNonMandatoryChildren()) {
            nonmandatory += coma + capitalize(nonmanAsset.getName()) + ".class";
            coma = ",";
         }
         nonmandatory += "}";
      }

      String abs = "";
      if (asset.isAbstractAsset()) {
         abs = "abstract";
      }
      else {
         writer.print(String.format("@DisplayClass(category = Category.%s  %s  %s)\n", asset.getCategory(), mandatory, nonmandatory));
         writer.print(String.format("@TypeName(name = \"%s\")\n", asset.getName()));
      }

      writer.print(String.format("public %s class %s", abs, asset.getName()));
      if (asset.getSuperAssetName() != "") {
         writer.print(" extends " + asset.getSuperAssetName());
      }
      else {
         writer.print(" extends MultiParentAsset");
      }
      writer.println(" {");
   }

   void printAssociations(Asset asset) {
      int number = 1;
      for (Association association : model.getAssociations(asset)) {
         String association_annotations = String.format("@Association(index = %d, name = \"%s\")", number, association.getRightRoleName());
         writer.println(association_annotations);
         if (association.getRightMultiplicity().equals("*") || association.getRightMultiplicity().equals("1-*")) {
            writer.println("   public FProbSet<" + association.getRightAssetName() + "> " + association.getRightRoleName() + " = new FProbSet<>();");
         }
         else {
            if (association.getRightMultiplicity().equals("1") || association.getRightMultiplicity().equals("0-1")) {
               writer.println("   public FProb<" + association.getRightAssetName() + "> " + association.getRightRoleName() + ";");
            }
         }
         number++;
      }
      number = 1;
      writer.println("@Override");
      writer.println("public void registerAssociations() {");
      if (asset.getSuperAssetName() != null && !asset.getSuperAssetName().equals("")) {
         writer.println("super.registerAssociations();");
      }
      for (Association association : model.getAssociations(asset)) {
         String oppositeClass = association.getTargetAssetName(asset);
         String oppositeRole = association.getTargetRoleName(asset);
         String linkText = model.getLinks().get(association);
         if (association.getRightMultiplicity().equals("*")) {
            writer.println(
                  String.format("   AssociationManager.addSupportedAssociationMultiple(this.getClass(), \"%s\", %s.class, AutoLangLink.%s);", oppositeRole, capitalize(oppositeClass), linkText));
         }
         else if (association.getRightMultiplicity().equals("1-*")) {
            writer.println(String.format("   AssociationManager.addSupportedAssociationMultiple(this.getClass(), \"%s\", %s.class, 1, AssociationManager.NO_LIMIT, AutoLangLink.%s);", oppositeRole,
                  capitalize(oppositeClass), linkText));
         }
         else if (association.getRightMultiplicity().equals("0-1")) {
            writer.println(String.format("  AssociationManager.addSupportedAssociationSingle(this.getClass(), \"%s\", %s.class, AutoLangLink.%s);", oppositeRole, capitalize(oppositeClass), linkText));
         }
         else if (association.getRightMultiplicity().equals("1")) {
            writer.println(
                  String.format("  AssociationManager.addSupportedAssociationMandatorySingle(this.getClass(), \"%s\", %s.class, AutoLangLink.%s);", oppositeRole, capitalize(oppositeClass), linkText));
         }

         number++;
      }
      writer.println("}");

   }

   protected void createDefaultAttacker() {
      writer.println("package auto;");
      writer.println("import java.util.Set;");
      writer.println("import com.foreseeti.corelib.AssociationManager;");
      writer.println("import com.foreseeti.corelib.DefaultValue;");
      writer.println("import com.foreseeti.corelib.FAnnotations.Category;");
      writer.println("import com.foreseeti.corelib.FAnnotations.DisplayClass;");
      writer.println("import com.foreseeti.corelib.FAnnotations.TypeName;");
      writer.println("import com.foreseeti.corelib.FClass;");
      writer.println("import com.foreseeti.corelib.util.FProb;");
      writer.println("import com.foreseeti.corelib.util.FProbSet;");
      writer.println("import com.foreseeti.simulator.AbstractAttacker;");
      writer.println("import com.foreseeti.simulator.AttackStep;");
      writer.println("import com.foreseeti.simulator.BaseLangLink;");
      writer.println("import com.foreseeti.simulator.Defense;");
      writer.println("import com.google.common.collect.ImmutableSet;");

      writer.println("@DisplayClass(supportCapexOpex = false, category = Category.Attacker)");
      writer.println("@TypeName(name = \"Attacker\")");
      writer.println("public class Attacker extends AbstractAttacker {");

      writer.println("  public Attacker() {");
      writer.println("    this(DefaultValue.False);");
      writer.println("  }");

      writer.println("  public Attacker(DefaultValue val) {");
      writer.println("    firstSteps = new FProbSet<>();");
      writer.println("    fillElementMap();");
      writer.println("  }");

      writer.println("  public Attacker(Attacker other) {");
      writer.println("    super(other);");
      writer.println("    firstSteps = new FProbSet<>();");
      writer.println("    entryPoint = new EntryPoint(other.entryPoint);");
      writer.println("    fillElementMap();");
      writer.println("  }");

      writer.println("  @Override");
      writer.println("  public String getConnectionValidationErrors(String sourceFieldName, FClass target, String targetFieldName) {");
      writer.println("    if (Attacker.class.isAssignableFrom(target.getClass())) {");
      writer.println("      return \"Attacker can not be connected to other Attackers\";");
      writer.println("    }");
      writer.println("    return getConnectionValidationErrors(target.getClass());");
      writer.println("  }");

      writer.println("  @Override");
      writer.println("  protected void registerAssociations() {");
      writer.println("    AssociationManager.addSupportedAssociationMultiple(this.getClass(),getName(1),AttackStep.class,0,AssociationManager.NO_LIMIT,BaseLangLink.Attacker_AttackStep);");

      writer.println("  }");

      writer.println("  @Override");
      writer.println("  public ImmutableSet<AttackStep> getAttackSteps() {");
      writer.println("    return ImmutableSet.copyOf(new AttackStep[] {entryPoint});");
      writer.println("  }");

      writer.println("  @Override");
      writer.println("  public ImmutableSet<Defense> getDefenses() {");
      writer.println("    return ImmutableSet.copyOf(new Defense[] {});");
      writer.println("  }");

      writer.println("  @TypeName(name = \"EntryPoint\")");
      writer.println("  public class EntryPoint extends AbstractAttacker.EntryPoint {");
      writer.println("    public EntryPoint() {}");

      writer.println("    public EntryPoint(AbstractAttacker.EntryPoint other) {");
      writer.println("      super(other);");
      writer.println("    }");

      writer.println("    @Override");
      writer.println("    public FClass getContainerFClass() {");
      writer.println("      return Attacker.this;");
      writer.println("    }");

      writer.println("    @Override");
      writer.println("    public Set<AttackStep> getAttackStepChildren() {");
      writer.println("      return FClass.toSampleSet(((Attacker) getContainerFClass()).firstSteps, null);");
      writer.println("    }");
      writer.println("  }");

      writer.println("  @Override");
      writer.println("  public boolean areAssociationsPublic() {");
      writer.println("    return false;");
      writer.println("  }");

      writer.println("  @Override");
      writer.println("  public boolean areModelElementsPublic() {");
      writer.println("    return false;");
      writer.println("  }");

      writer.println("  @Override");
      writer.println("  public boolean isAttacker() {");
      writer.println("    return true;");
      writer.println("  }");

      writer.println("}");

   }

   void printStepAssignments(Asset asset) {
      int num = 0;
      for (AttackStep attackStep : asset.getAttackSteps()) {
         if (!attackStep.getSuperAttackStepName().equals("") && !attackStep.isDefense()) {
            num++;
         }
      }
      for (AttackStep attackStep : asset.getAttackSteps()) {
         if (attackStep.getSuperAttackStepName().equals("")) {
            String display = "";
            boolean def = attackStep.isDefense();
            if ((!attackStep.isHiddenAttackStep() && !def) || (def && attackStep.isDisplayableDefense())) {
               display = "@Display";
            }
            if (!def) {
               writer.println(String.format("@Association(index = %d, name = \"%s\")", ++num, attackStep.getName()));
            }
            writer.print(String.format("%s   public %s %s ; \n", display, capitalize(attackStep.getName()), attackStep.getName()));
         }
      }
      writer.println("");
   }

   void printConstructor(Asset asset) {
      printDefaultConstructors(asset);
      printInitAttackStepsWithDefault(asset);
   }

   protected void generateGetConnectionErrors() {
      writer.println("public String getConnectionValidationErrors(String sourceFieldName, FClass target, String targetFieldName) {");
      writer.println("	   return null");
      writer.println("}");
   }

   protected void printDefaultConstructors(Asset asset) {
      List<AttackStep> defensesExcludingExistenceRequirements = asset.defensesExcludingExistenceRequirements();
      String immutable_attacks = "";
      String immutable_defenses = "";
      String coma = "";
      if (defensesExcludingExistenceRequirements.isEmpty()) {
         writer.println(String.format("public %s(DefaultValue val) {this();}", capitalize(asset.getName())));
         writer.println(String.format("public %s(){initAttackStepsWithDefault();initAttackStepAndDefenseLists();}", capitalize(asset.getName())));
      }
      else {
         writer.println(String.format("public %s(){this(DefaultValue.False);}", capitalize(asset.getName())));
         writer.println(String.format("public %s(DefaultValue val) {", capitalize(asset.getName())));
         String booleanParams = "";
         writer.println("this(");
         for (AttackStep defense : defensesExcludingExistenceRequirements) {
            writer.print(String.format("%s val.get()", coma));
            booleanParams += String.format("%s boolean is%s", coma, capitalize(defense.getName()));
            coma = ",";
         }
         writer.println(");");
         writer.println("}");
         writer.println(String.format("public %s(%s){", capitalize(asset.getName()), booleanParams));
         for (AttackStep defense : defensesExcludingExistenceRequirements) {
            writer.println(String.format("%s = new %s(is%s);", defense.getName(), capitalize(defense.getName()), capitalize(defense.getName())));
         }
         for (AttackStep defense : asset.defensesWithExistenceRequirementsOnly()) {
            writer.println(String.format("%s = new %s(false);", defense.getName(), capitalize(defense.getName())));
         }
         writer.println("initAttackStepsWithDefault();");
         writer.println("initAttackStepAndDefenseLists();");
         writer.println("}");
      }
      writer.println(String.format("public %s(%s other) {", capitalize(asset.getName()), capitalize(asset.getName())));
      writer.println("super(other);");
      for (AttackStep defense : asset.defensesExcludingExistenceRequirements()) {
         writer.println(String.format("%s = new %s(other.%s);", defense.getName(), capitalize(defense.getName()), defense.getName()));
      }
      for (AttackStep attackStep : asset.attackStepsExceptDefensesAndExistence()) {
         writer.println(String.format("%s = new %s(other.%s);", attackStep.getName(), capitalize(attackStep.getName()), attackStep.getName()));
      }
      writer.println("initAttackStepAndDefenseLists();");
      writer.println("}");
      writer.println("");
      writer.println("private void initAttackStepAndDefenseLists() {");
      coma = "";
      for (AttackStep defense : asset.defenses()) {
         immutable_defenses += String.format("%s %s", coma, defense.getName());
         coma = ",";
      }
      writer.println(String.format("defenses = ImmutableSet.of(%s);", immutable_defenses));
      coma = "";
      for (AttackStep attackStep : asset.attackStepsExceptDefensesAndExistence()) {
         immutable_attacks += String.format("%s %s", coma, attackStep.getName());
         coma = ",";
      }
      for (AttackStep defense : asset.defenses()) {
         immutable_attacks += String.format("%s %s.disable", coma, defense.getName());
         coma = ",";
      }
      writer.println(String.format("attackSteps = ImmutableSet.of(%s);", immutable_attacks));
      writer.println("fillElementMap();\n");
      writer.println("}");
   }

   protected void printInitAttackStepsWithDefault(Asset asset) {
      writer.println("protected void initAttackStepsWithDefault() {");
      for (AttackStep attackStep : asset.attackStepsExceptDefensesAndExistence()) {
         writer.println(String.format("%s = new %s();", attackStep.getName(), capitalize(attackStep.getName())));
      }
      writer.println("}");
   }

   protected void printLocalAttackStepSpecialization(Asset asset) {
      writer.println("public class LocalAttackStepMin extends AttackStepMin {");
      writer.println("@Override");
      writer.println("public FClass getContainerFClass() {");
      writer.println(String.format("return %s.this;", capitalize(asset.getName())));
      writer.println("}");
      writer.println("LocalAttackStepMin() {}");
      writer.println("LocalAttackStepMin(LocalAttackStepMin other) {");
      writer.println("super(other);");
      writer.println("}");
      writer.println("}");

      writer.println("public class LocalAttackStepMax extends AttackStepMax {");

      writer.println("@Override");
      writer.println("public FClass getContainerFClass() {");
      writer.println(String.format("return %s.this;", capitalize(asset.getName())));
      writer.println("}");

      writer.println("LocalAttackStepMax() {}");

      writer.println("LocalAttackStepMax(LocalAttackStepMax other) {");
      writer.println("super(other);");
      writer.println("}");
      writer.println("      public double defaultLocalTtc(BaseSample sample, AttackStep caller)  {return 0.00001157407;}");
      writer.println("}");
   }

   protected String sprintConstructorWithoutDefenseAttributes(Asset asset, boolean hasName, String constructorString) {
      if (!asset.defensesExcludingExistenceRequirements().isEmpty()) {
         constructorString += "   public " + capitalize(asset.getName()) + "(";
         if (hasName) {
            constructorString += "String name";
         }
         constructorString += ") {\n";
         constructorString += "      this(";
         if (hasName) {
            constructorString += "name, ";
         }

         for (AttackStep defense : asset.defensesExcludingExistenceRequirements()) {
            constructorString += "false, ";
         }
         constructorString = constructorString.substring(0, constructorString.length() - 2);
         constructorString += ");\n";
         constructorString += "      assetClassName = \"" + asset.getName() + "\";\n   }\n\n";
      }
      return constructorString;
   }

   protected String sprintConstructorWithDefenseAttributes(Asset asset, boolean hasName, String constructorString) {
      constructorString += "   public " + capitalize(asset.getName()) + "(";
      if (hasName) {
         constructorString += "String name";
         if (!asset.defensesExcludingExistenceRequirements().isEmpty()) {
            constructorString += ", ";
         }
      }
      constructorString = sprintDefenseAttributes(asset, constructorString);
      constructorString += ") {\n";
      constructorString = sprintSuperCall(asset, hasName, constructorString);
      constructorString = sprintStepCreation(asset, constructorString);
      constructorString += "      assetClassName = \"" + asset.getName() + "\";\n   }\n\n";
      return constructorString;
   }

   protected String sprintSuperCall(Asset asset, boolean hasName, String constructorString) {
      constructorString += "      super(";
      Asset superAsset = null;
      if (!asset.getSuperAssetName().equals("")) {
         superAsset = model.getAsset(asset.getSuperAssetName());
      }
      if (hasName) {
         constructorString += "name";
         if (!asset.getSuperAssetName().equals("")) {
            if (!superAsset.defensesExcludingExistenceRequirements().isEmpty()) {
               constructorString += ", ";
            }
         }
      }
      if (!asset.getSuperAssetName().equals("")) {
         for (AttackStep defense : superAsset.defensesExcludingExistenceRequirements()) {
            constructorString += defense.getName() + "State, ";
         }
         if (!superAsset.defensesExcludingExistenceRequirements().isEmpty()) {
            constructorString = constructorString.substring(0, constructorString.length() - 2);
         }
      }

      constructorString += ");\n";
      return constructorString;
   }

   protected String sprintDefenseAttributes(Asset asset, String constructorString) {
      String attributesString = "";
      if (!asset.defensesExcludingExistenceRequirements().isEmpty()) {
         for (AttackStep defense : asset.defensesExcludingExistenceRequirements()) {
            attributesString += "Boolean " + defense.getName() + "State, ";
         }
         if (attributesString.length() > 0) {
            attributesString = attributesString.substring(0, attributesString.length() - 2);
         }
      }
      return constructorString + attributesString;
   }

   protected String sprintStepCreation(Asset asset, String constructorString) {
      for (AttackStep defense : asset.defenses()) {
         constructorString += "      if (" + defense.getName() + " != null) {\n";
         constructorString += "         AttackStep.allAttackSteps.remove(" + defense.getName() + ".disable);\n";
         constructorString += "      }\n";
         constructorString += "      Defense.allDefenses.remove(" + defense.getName() + ");\n";
         constructorString += "      " + defense.getName() + " = new " + capitalize(defense.getName()) + "(this.name";
         if (!defense.hasExistenceRequirements()) {
            constructorString += ", " + defense.getName() + "State";
         }
         constructorString += ");\n";
      }
      for (AttackStep attackStep : asset.getAttackSteps()) {
         if (!asset.defenses().contains(attackStep)) {
            constructorString += "      AttackStep.allAttackSteps.remove(" + attackStep.getName() + ");\n";
            constructorString += "      " + attackStep.getName() + " = new " + capitalize(attackStep.getName()) + "(this.name);\n";
         }
      }
      return constructorString;
   }

   void printStepDefinitions(Asset asset) {
      for (AttackStep attackStep : asset.getAttackSteps()) {
         if (attackStep.isDefense()) {
            printDefenseDefinition(attackStep, asset);
         }
         else {
            printAttackStepDefinition(attackStep);
         }
      }
   }

   void printDefenseDefinition(AttackStep attackStep, Asset asset) {
      printDefenseSignature(attackStep);
      printDefenseConstructor(attackStep);
      printExistenceRequirements(attackStep);
      // A new Disable is created for each specialization, and then only the
      // most specialized is used. Not so pretty.
      printDisableDeclaration(attackStep, asset);
      printContainerFClassMethod(asset);
      writer.println("}\n");
   }

   protected void printContainerFClassMethod(Asset asset) {
      writer.println("@Override");
      writer.println("public FClass getContainerFClass() {");
      writer.println(String.format("return %s.this;", capitalize(asset.getName())));
      writer.println("}");
   }

   protected void printInfluenceDefense(AttackStep attackStep) {
      writer.println("@Override");
      writer.println("public Defense getInfluencingDefense() {");
      writer.println(String.format("return %s.this;", capitalize(attackStep.getName())));
      writer.println("}");
   }

   protected void printDisableDeclaration(AttackStep attackStep, Asset asset) {
      boolean max = attackStep.getAttackStepType().equals("&");
      writer.print("   public class Disable extends ");
      if (!attackStep.getSuperAttackStepName().equals("")) {
         writer.print(attackStep.getSuperAttackStepName() + ".Disable");
      }
      else {
         if (max) {
            writer.print("AttackStepMax");
         }
         else {
            writer.print("AttackStepMin");
         }

      }
      writer.println(" {");
      printUpdateChildren(attackStep);
      printContainerFClassMethod(asset);
      printInfluenceDefense(attackStep);
      writer.println("   }");
   }

   protected void printDefenseConstructor(AttackStep attackStep) {
      writer.print("   public " + capitalize(attackStep.getName()) + "(Boolean enabled){");
      writer.print("      super(enabled);");
      writer.println("      disable = new Disable();");
      writer.println("   }\n");
      AttackStep baseAttackStep = attackStep.getBaseAttackStep();
      if (baseAttackStep == null) {
         baseAttackStep = attackStep;
      }
      writer.print(String.format("   public %s(%s.%s other) {", capitalize(baseAttackStep.getName()), capitalize(baseAttackStep.getAsset().getName()), capitalize(baseAttackStep.getName())));
      writer.println("super(other);");
      writer.println("disable = new Disable();");
      writer.println("   }\n");

   }

   protected void printDefenseSignature(AttackStep attackStep) {
      writer.println(String.format("@TypeName(name = \"%s\")", capitalize(attackStep.getName())));
      writer.println(String.format("@TypeDescription(text = \"%s\")", capitalize(attackStep.getName())));
      writer.print("   public class " + capitalize(attackStep.getName()) + " extends ");
      if (!attackStep.getSuperAttackStepName().equals("")) {
         writer.println(attackStep.getSuperAttackStepName() + " {");
      }
      else {
         writer.println("Defense {");
      }
   }

   void printExistenceRequirements(AttackStep attackStep) {
      if (!attackStep.getExistenceRequirementRoles().isEmpty()) {
         writer.println("   @Override");
         writer.println("   public boolean isEnabled(ConcreteSample sample) {");
         // The below should be the role name, not the asset name.
         // Furthermore, it should check for empty set rather than == null for
         // multiplicity associations
         Association association = model.getConnectedAssociation(attackStep.getAsset().getName(), attackStep.getExistenceRequirementRoles().get(0));
         assertTrue("Did not find the association from the asset " + attackStep.getAsset().getName() + " to the role " + attackStep.getExistenceRequirementRoles().get(0), association != null);
         String multiplicity = association.targetMultiplicityIncludingInheritance(attackStep.getAsset());
         if (multiplicity.equals("1") || multiplicity.equals("0-1")) {
            if (attackStep.getAttackStepType().equals("E")) {
               writer.println("      return " + attackStep.getExistenceRequirementRoles().get(0) + " == null;");
            }
            if (attackStep.getAttackStepType().equals("3")) {
               writer.println("      return " + attackStep.getExistenceRequirementRoles().get(0) + " != null;");
            }
         }
         else {
            if (attackStep.getAttackStepType().equals("E")) {
               writer.println("      return " + attackStep.getExistenceRequirementRoles().get(0) + ".isEmpty();");
            }
            if (attackStep.getAttackStepType().equals("3")) {
               writer.println("      return !" + attackStep.getExistenceRequirementRoles().get(0) + ".isEmpty();");
            }
         }
         writer.println("   }");
      }

   }

   void printAttackStepDefinition(AttackStep attackStep) {
      writer.print("   public class " + capitalize(attackStep.getName()) + " extends ");
      String attackStepTypeString = "";
      if (!attackStep.getSuperAttackStepName().equals("")) {
         attackStepTypeString = attackStep.getSuperAttackStepName();
      }
      else {
         if (attackStep.getAttackStepType().equals("&")) {
            attackStepTypeString = "LocalAttackStepMax";
         }
         if (attackStep.getAttackStepType().equals("|")) {
            attackStepTypeString = "LocalAttackStepMin";
         }
         if (attackStep.getAttackStepType().equals("t")) {
            attackStepTypeString = "CPT_AttackStep";
         }
      }
      AttackStep baseAttackStep = attackStep.getBaseAttackStep();
      if (baseAttackStep == null) {
         baseAttackStep = attackStep;
      }
      assert (!attackStepTypeString.equals(""));

      writer.println(attackStepTypeString + " {");
      writer.println(String.format("   public %s(%s.%s other) {", capitalize(attackStep.getName()), capitalize(baseAttackStep.getAsset().getName()), capitalize(baseAttackStep.getName())));
      writer.println("      super(other);");
      writer.println("   }");
      writer.println(String.format("   public %s() {", capitalize(attackStep.getName())));
      writer.println("   }");
      printSetExpectedParents(attackStep);
      printUpdateChildren(attackStep);

      writer.println("   }\n");
   }

   void printUpdateChildren(AttackStep attackStep) {
      if (!attackStep.childPointers.isEmpty()) {
         writer.println("      @Override");
         writer.println("      public Set<AttackStep> getAttackStepChildren()  {");
         if (!attackStep.isSpecialization()) {
            writer.println("Set<AttackStep> set = new HashSet<>(super.getAttackStepChildren());");
         }
         else {
            writer.println("Set<AttackStep> set = new HashSet<>();");
         }
         String subClassAndAttackStepName;

         for (AttackStepPointer childPointer : attackStep.childPointers) {
            if (childPointer.getSubClassName().equals("")) {
               subClassAndAttackStepName = childPointer.getAttackStep().getName();
            }
            else {
               subClassAndAttackStepName = childPointer.getSubClassName() + "." + childPointer.getAttackStep().getName();
            }
            if (childPointer.getMultiplicity().equals("0-1") || childPointer.getMultiplicity().equals("1")) {
               if (childPointer.getRoleName().equals("this")) {
                  writer.println("         if (" + subClassAndAttackStepName + " != null) {");
                  writer.println("            set.add(" + subClassAndAttackStepName + ");");
                  writer.println("         }");
               }
               else {
                  writer.println("         if (" + childPointer.getRoleName() + "(null) != null) {");
                  writer.println("            set.add(" + childPointer.getRoleName() + "(null)." + subClassAndAttackStepName + ");");
                  writer.println("         }");
               }
            }
            if (childPointer.getMultiplicity().equals("1-*") || childPointer.getMultiplicity().equals("*")) {
               writer.println("         for (" + childPointer.getAttackStep().getAsset().getName() + " " + decapitalize(childPointer.getAttackStep().getAsset().getName()) + " : "
                     + childPointer.getRoleName() + "(null)) {");
               writer.println("            set.add(" + decapitalize(childPointer.getAttackStep().getAsset().getName()) + "." + subClassAndAttackStepName + ");\n         }");
            }
         }
         writer.println("return set;");
         writer.println("      }\n");
      }
   }

   void printSetExpectedParents(AttackStep attackStep) {
      if (!attackStep.getParentPointers().isEmpty()) {
         writer.println("      @Override");
         writer.println("      protected void setExpectedParents(ConcreteSample sample) {");
         // When an attack step is overridden, the inheriting parents must still
         // be
         // able to reach it as specified in the super class.

         if (!attackStep.getSuperAttackStepName().equals("")) {
            writer.println("         super.setExpectedParents(sample);");
         }
         if (attackStep.getExistenceRequirementRoles().size() > 0) {
            writer.println("         if (" + attackStep.getExistenceRequirementRoles().get(0) + " != null) {");
         }

         for (AttackStepPointer parentPointer : attackStep.getParentPointers()) {
            String disableString = "";
            if (parentPointer.getAttackStep().isDefense()) {
               disableString = ".disable";
            }
            String parentRoleName = parentPointer.getRoleName();
            String parentAssetNameAccordingToAttackStep = parentPointer.getAttackStep().getAsset().getName();
            String parentAssetNameAccordingToAssociation = "";
            if (parentPointer.getAssociation() != null) {
               parentAssetNameAccordingToAssociation = parentPointer.getAssociation().getAssetName(parentRoleName);
            }
            String parentShortStepName = parentPointer.getAttackStep().getName() + disableString;
            String parentString = "";
            if (parentPointer.getAttackStep().getAsset().superAssets().contains(attackStep.getAsset())) {
               parentString = parentShortStepName;
            }
            else {
               parentString = parentRoleName + "." + parentShortStepName;
            }
            String mainExpressionString = "";
            if (parentPointer.getMultiplicity().equals("1")) {
               if (!parentRoleName.isEmpty()) {
                  mainExpressionString += "         if (" + parentRoleName + "(sample) != null) {\n";
                  mainExpressionString += "            sample.addExpectedParent(this, " + parentRoleName + "(sample)." + parentShortStepName + ");\n";
                  mainExpressionString += "         }\n";
                  mainExpressionString += "         else {\n";
                  mainExpressionString += "            System.out.println(\"Error in \" + name + \": Exactly one " + parentRoleName + " must be connected to each " + attackStep.getAsset().getName()
                        + "\");\n";
                  mainExpressionString += "         }\n";
               }
               else {
                  mainExpressionString += "         sample.addExpectedParent(this," + parentString + ");\n";
               }
            }

            if (parentPointer.getMultiplicity().equals("0-1")) {
               mainExpressionString += "         if (" + parentRoleName + "(sample) != null) {\n";
               if (parentAssetNameAccordingToAssociation.equals(parentAssetNameAccordingToAttackStep)) {
                  if (!parentRoleName.isEmpty())
                     mainExpressionString += "            sample.addExpectedParent(this," + parentRoleName + "(sample)." + parentShortStepName + ");\n";
                  else
                     mainExpressionString += "            sample.addExpectedParent(this," + parentString + ");\n";
               }
               else {
                  mainExpressionString += "            if (" + decapitalize(parentRoleName) + "(sample) instanceof " + parentAssetNameAccordingToAttackStep + ") {\n";
                  mainExpressionString += "               sample.addExpectedParent(this,((" + parentAssetNameAccordingToAttackStep + ")" + decapitalize(parentRoleName) + "(sample))."
                        + parentShortStepName + ");\n";
                  mainExpressionString += "            }\n";

               }
               mainExpressionString += "         }\n";
            }
            if (parentPointer.getMultiplicity().equals("*")) {
               mainExpressionString = loopString(parentAssetNameAccordingToAttackStep, parentAssetNameAccordingToAssociation, parentRoleName, parentShortStepName, mainExpressionString);

            }
            if (parentPointer.getMultiplicity().equals("1-*")) {
               mainExpressionString += "         if (" + parentRoleName + " != null) {\n";
               mainExpressionString = loopString(parentAssetNameAccordingToAttackStep, parentAssetNameAccordingToAssociation, parentRoleName, parentShortStepName, mainExpressionString);
               mainExpressionString += "         }\n";
               mainExpressionString += "         else {\n";
               mainExpressionString += "            throw new NullPointerException(\"At least one " + parentRoleName + " must be connected to each " + attackStep.getAsset().getName() + "\");\n";
               mainExpressionString += "         }\n";
            }
            writer.println(mainExpressionString);
         }
         if (attackStep.getExistenceRequirementRoles().size() > 0) {
            writer.println("         }");
         }
         writer.println("      }\n");
      }
   }

   protected String loopString(String parentAssetNameAccordingToAttackStep, String parentAssetNameAccordingToAssociation, String parentRoleName, String parentShortStepName,
         String mainExpressionString) {
      if (parentAssetNameAccordingToAssociation.equals(parentAssetNameAccordingToAttackStep)) {
         mainExpressionString += "         for (" + parentAssetNameAccordingToAttackStep + " " + decapitalize(parentAssetNameAccordingToAttackStep) + " : " + parentRoleName + "(sample)) {\n";
         mainExpressionString += "            sample.addExpectedParent(this, " + decapitalize(parentAssetNameAccordingToAttackStep) + "." + parentShortStepName + ");\n         }\n";
      }
      else {
         mainExpressionString += "         for (" + parentAssetNameAccordingToAssociation + " " + decapitalize(parentAssetNameAccordingToAssociation) + " : " + parentRoleName + "(sample)) {\n";
         mainExpressionString += "            if (" + decapitalize(parentAssetNameAccordingToAssociation) + " instanceof " + parentAssetNameAccordingToAttackStep + ") {\n";
         mainExpressionString += "            sample.addExpectedParent(this, ((" + parentAssetNameAccordingToAttackStep + ")" + decapitalize(parentAssetNameAccordingToAssociation) + ")."
               + parentShortStepName + ");\n            }\n         }\n";
      }
      return mainExpressionString;
   }

   void printConnectionHelpers(Asset asset) {

      for (Association association : asset.getAssociations()) {

         if (association.getRightMultiplicity().equals("*") || association.getRightMultiplicity().equals("1-*")) {
            writer.println(String.format("public Set<%s> %s(BaseSample sample) { return toSampleSet(%s, sample); } \n", capitalize(association.getRightAssetName()), association.getRightRoleName(),
                  association.getRightRoleName()));
         }
         else {
            writer.println(String.format("public %s %s(BaseSample sample) { return toSample(%s, sample); } \n", capitalize(association.getRightAssetName()), association.getRightRoleName(),
                  association.getRightRoleName()));
         }
      }
   }

   void printGetAssociatedAssetClassName(Asset asset) {
      writer.println("   @Override");
      writer.println("   public String getAssociatedAssetClassName(String roleName) {");
      for (Association association : asset.getAssociations()) {
         writer.println("      if (roleName.equals(\"" + association.getTargetRoleName(asset) + "\")) {");
         if (association.targetMultiplicity(asset).equals("*") || association.targetMultiplicity(asset).equals("1-*")) {
            writer.println("         for (Object o: " + association.getTargetRoleName(asset) + ") {");
            writer.println("            return o.getClass().getName();");
            writer.println("         }");
         }
         else {
            writer.println("         return " + association.getTargetRoleName(asset) + ".getClass().getName();");
         }
         writer.println("      }");
      }
      writer.println("      return null;");
      writer.println("   }");
   }

   void printGetAssociatedAssets(Asset asset) {
      writer.println("   @Override");
      writer.println("   public Set<Asset> getAssociatedAssets(String roleName) {");
      writer.println("      AnySet<Asset> assets = new AnySet<>();");
      for (Association association : asset.getAssociationsIncludingInherited()) {
         writer.println(
               "      if (roleName.equals(\"" + association.getTargetRoleNameIncludingInheritance(asset) + "\")  && " + association.getTargetRoleNameIncludingInheritance(asset) + " != null) {");
         if (association.targetMultiplicityIncludingInheritance(asset).equals("*") || association.targetMultiplicityIncludingInheritance(asset).equals("1-*")) {
            writer.println("         assets.addAll(" + association.getTargetRoleNameIncludingInheritance(asset) + ");");
         }
         else {
            writer.println("         assets.add(" + association.getTargetRoleNameIncludingInheritance(asset) + ");");
         }
         writer.println("         return assets;");
         writer.println("      }");
      }
      writer.println("      assertTrue(\"The asset \" + this.toString() + \" does not feature the role name \" + roleName + \".\", false);");
      writer.println("      return null;");
      writer.println("   }");
   }

   void printGetAllAssociatedAssets(Asset asset) {
      writer.println("   @Override");
      writer.println("   public Set<Asset> getAllAssociatedAssets() {");
      writer.println("      AnySet<Asset> assets = new AnySet<>();");
      for (Association association : asset.getAssociationsIncludingInherited()) {
         if (association.targetMultiplicityIncludingInheritance(asset).equals("*") || association.targetMultiplicityIncludingInheritance(asset).equals("1-*")) {
            writer.println("      assets.addAll(" + association.getTargetRoleNameIncludingInheritance(asset) + ");");
         }
         else {
            writer.println("      if (" + association.getTargetRoleNameIncludingInheritance(asset) + " != null) {");
            writer.println("         assets.add(" + association.getTargetRoleNameIncludingInheritance(asset) + ");");
            writer.println("      }");
         }
      }
      writer.println("      return assets;");
      writer.println("   }");
   }

   private void writeToJsonStringln(String s) {
      jsonString = jsonString + s + "\n";
   }

   private void backtrackJsonString() {
      jsonString = jsonString.substring(0, jsonString.length() - 2) + "\n";
   }

   public Set<String> listFilesForFolder(final File folder) {
      Set<String> testClassNames = new HashSet<>();
      for (final File fileEntry : folder.listFiles()) {
         if (fileEntry.isDirectory()) {
            listFilesForFolder(fileEntry);
         }
         else {
            if (fileEntry.getName().indexOf(".csv") != -1) {
               testClassNames.add(fileEntry.getName().split("\\.")[0]);
            }
         }
      }
      return testClassNames;
   }

   private void writeTestCases(String securiLangFolderPath) {

      final File folder = new File(securiLangFolderPath + "/testCases/");
      Set<String> testClassNames = listFilesForFolder(folder);
      for (String testClassName : testClassNames) {
         Set<TestingCase> testingCases = new HashSet<>();
         readTestCaseFile(securiLangFolderPath, testClassName, testingCases);
         writeTests(securiLangFolderPath, testClassName, testingCases);
      }
      writeTestRunner(securiLangFolderPath);
   }

   protected void readTestCaseFile(String securiLangFolderPath, String fileName, Set<TestingCase> testingCases) {
      try {
         String testFile = readFile(securiLangFolderPath + "/testCases/" + fileName + ".csv");
         String[] rows = testFile.split("\n");
         int nRows = rows.length;
         int nCols = rows[0].split(";").length;
         String[][] matrix = new String[nRows][nCols];
         for (int iRow = 0; iRow < nRows; iRow++) {
            String[] words = rows[iRow].split(";");
            for (int iCol = 0; iCol < nCols; iCol++) {
               matrix[iRow][iCol] = words[iCol];
            }
            List<String> newValues = new ArrayList<>();
            for (int iWord = 2; iWord < words.length; iWord++) {
               newValues.add(words[iWord]);
            }
         }

         for (int iCol = 3; iCol < nCols; iCol++) {
            TestingCase testingCase = new TestingCase();
            for (int iRow = 0; iRow < nRows; iRow++) {
               AttackStepName attackPoint = new AttackStepName();
               attackPoint.assetName = matrix[iRow][1].toLowerCase();
               testingCase.assetNames.add(matrix[iRow][1]);
               attackPoint.attackStepName = matrix[iRow][2];
               if (matrix[iRow][0].equals("attackStep")) {
                  if (matrix[iRow][iCol].equals("entryPoint")) {
                     testingCase.attackPointNames.add(attackPoint);
                  }
                  if (matrix[iRow][iCol].equals("infinity")) {
                     testingCase.attackStepStates.put(attackPoint, " == AttackStep.infinity");
                  }
                  if (matrix[iRow][iCol].equals("lessThanADay")) {
                     testingCase.attackStepStates.put(attackPoint, " < 1");
                  }
               }
               if (matrix[iRow][0].equals("defenseStep")) {
                  testingCase.defenseStepStates.put(attackPoint, matrix[iRow][iCol]);
               }
            }
            testingCases.add(testingCase);
         }
      }
      catch (IOException e) {
         // TODO Auto-generated catch block
         e.printStackTrace();
      }
   }

   protected void writeTests(String javaFolderPath, String testCaseName, Set<TestingCase> testingCases) {

      // Doing it wrong:

      String testCodeFile = javaFolderPath + "/autoLang/test/test" + testCaseName + ".java";
      try {
         writer = new PrintWriter(testCodeFile, "UTF-8");
         writer.println("import static org.junit.Assert.assertTrue;");

         writer.println("import org.junit.Test;");
         writer.println("");
         writer.println("public class test" + testCaseName + " {");
         writer.println("");
         Integer iCase = 0;
         for (TestingCase testingCase : testingCases) {
            iCase++;
            writer.println("   @Test");
            writer.println("   public void test" + iCase.toString() + "() {");
            for (String assetName : testingCase.assetNames) {
               writer.println("      " + assetName + " " + assetName.toLowerCase() + " = new " + assetName + "();");
            }
            for (Map.Entry<AttackStepName, String> entry : testingCase.defenseStepStates.entrySet()) {
               AttackStepName defenseStepName = entry.getKey();
               String defenseStepState = entry.getValue();
               writer.println("      " + defenseStepName.assetName.toLowerCase() + "." + defenseStepName.attackStepName + ".defaultValue = " + defenseStepState + ";");
            }
            writer.println("");
            writer.println("      Attacker attacker = new Attacker();");
            for (AttackStepName attackPointName : testingCase.attackPointNames) {
               writer.println("      attacker.addAttackPoint(" + attackPointName.assetName.toLowerCase() + "." + attackPointName.attackStepName + ");");
            }
            writer.println("      attacker.attack();");
            writer.println("");

            for (Map.Entry<AttackStepName, String> entry : testingCase.attackStepStates.entrySet()) {
               AttackStepName attackStepName = entry.getKey();
               String attackStepState = entry.getValue();
               writer.println("      assertTrue(" + attackStepName.assetName.toLowerCase() + "." + attackStepName.attackStepName + ".ttc" + attackStepState + ");");
            }
            writer.println("   }");
         }
         writer.println("");
         writer.println("}");
         writer.close();

      }
      catch (FileNotFoundException e) {
         System.out.println("FileNotFoundException");
         e.printStackTrace();
      }
      catch (UnsupportedEncodingException e) {
         System.out.println("UnsupportedEncodingException");
         e.printStackTrace();
      }

   }

   protected void writeTestRunner(String javaFolderPath) {

      String testCodeFile = javaFolderPath + "/autoLang/test/TestRunner.java";
      try {
         writer = new PrintWriter(testCodeFile, "UTF-8");
         writer.println("import org.junit.runner.JUnitCore;\nimport org.junit.runner.Result;\nimport org.junit.runner.notification.Failure;\n");
         writer.print("public class TestRunner {\n   public static void main(String[] args) {\n      Result result = JUnitCore.runClasses(");
         // writer.print("testNetwork.class");
         writer.println(
               ");\n\n      for (Failure failure : result.getFailures()) {\n         System.out.println(failure.toString());\n      }\n\n      System.out.println(\"Executed \" + result.getRunCount() + \" cases.\");");
         writer.println(
               "       if (result.wasSuccessful()) {\n         System.out.println(\"All were successful.\");\n      }\n      else {\n         System.out.println(\"Some failed.\");\n      }\n\n   }\n}");
         writer.close();

      }
      catch (FileNotFoundException e) {
         System.out.println("FileNotFoundException");
         e.printStackTrace();
      }
      catch (UnsupportedEncodingException e) {
         System.out.println("UnsupportedEncodingException");
         e.printStackTrace();
      }

   }

   class AttackStepName {
      String assetName;
      String attackStepName;

      String attackPointName() {
         return assetName + "." + attackStepName;
      }
   }

   class TestingCase {
      Set<String>                 assetNames        = new HashSet<>();
      Set<AttackStepName>         attackPointNames  = new HashSet<>();
      Map<AttackStepName, String> defenseStepStates = new HashMap<>();
      Map<AttackStepName, String> attackStepStates  = new HashMap<>();
   }

}
