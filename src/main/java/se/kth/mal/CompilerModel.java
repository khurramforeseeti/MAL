package se.kth.mal;

import static org.junit.Assert.assertNotNull;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

public class CompilerModel {

   private List<Asset>              assets       = new ArrayList<>();
   private List<Association>        associations = new ArrayList<>();
   private Map<Association, String> links        = new HashMap<>();

   public CompilerModel(String securiLangFolder, String securiLangFile) throws FileNotFoundException, IOException {

      String fileWithIncludesPath = includeIncludes(securiLangFolder, securiLangFile);

      InputStream is = System.in;
      if (securiLangFile != null) {
         System.out.println("Reading from " + fileWithIncludesPath);
         is = new FileInputStream(fileWithIncludesPath);
      }
      CharStream input = CharStreams.fromStream(is);

      sLangLexer lexer = new sLangLexer(input);
      CommonTokenStream tokens = new CommonTokenStream(lexer);
      sLangParser parser = new sLangParser(tokens);
      ParseTree tree = parser.compilationUnit(); // parse

      ParseTreeWalker walker = new ParseTreeWalker(); // create standard
      // walker
      sLangListener extractor = new SecuriLangListener(this);
      walker.walk(extractor, tree); // initiate walk of tree with listener
      update();
   }

   public List<Asset> getAssets() {
      return assets;
   }

   public List<Association> getAssociations() {
      return associations;
   }

   public Map<Association, String> getLinks() {
      return links;
   }

   public String includeIncludes(String securiLangFolder, String securiLangFile) {
      String filePath = securiLangFolder + "/" + securiLangFile;

      String outPath = filePath + "inc";
      try {
         FileWriter fileWriter = new FileWriter(outPath);
         BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
         appendFileToBufferedWriter(securiLangFolder, securiLangFile, bufferedWriter);
         bufferedWriter.close();
      }
      catch (FileNotFoundException ex) {
         System.out.println("Unable to open file '" + filePath + "'");
      }
      catch (IOException ex) {
         System.out.println("Error reading file '" + filePath + "'");
      }
      return outPath;
   }

   public void appendFileToBufferedWriter(String securiLangFolder, String securiLangFile, BufferedWriter bufferedWriter) throws java.io.FileNotFoundException, java.io.IOException {
      String line = null;
      String filePath = securiLangFolder + "/" + securiLangFile;

      FileReader fileReader = new FileReader(filePath);
      BufferedReader bufferedReader = new BufferedReader(fileReader);
      while ((line = bufferedReader.readLine()) != null) {
         if (line.contains("include") && !line.contains("//")) {
            if (line.split(" ")[0].equals("include")) {
               String includeFileName = line.split(" ")[1];
               appendFileToBufferedWriter(securiLangFolder, includeFileName, bufferedWriter);
            }
         }
         else {
            bufferedWriter.write(line + "\n");
         }
      }
      bufferedReader.close();
   }

   public Asset addAsset(String name, String superAssetName, boolean abstractAsset) {
      Asset a = new Asset(name, this, abstractAsset);
      if (superAssetName != "") {
         a.superAssetName = superAssetName;
      }
      assets.add(a);
      return a;
   }

   public Association addAssociation(String leftAssetName, String leftRoleName, String leftMultiplicity, String leftRelation, String name, String rightRelation, String rightMultiplicity,
         String rightRoleName, String rightAssetName) {
      Association a = new Association(leftAssetName, leftRoleName, leftMultiplicity, leftRelation, name, rightRelation, rightMultiplicity, rightRoleName, rightAssetName);
      associations.add(a);
      if (!links.containsKey(a)) {
         links.put(a, String.format("%s_%s", a.leftRoleName, a.rightRoleName));
      }
      return a;
   }

   public AttackStepPointer addStepPointer() {
      return new AttackStepPointer();
   }

   public Asset getAsset(String name) {
      for (Asset asset : this.assets) {
         if (asset.name.equals(name)) {
            return asset;
         }
      }
      return null;
   }

   public List<Association> getAssociations(Asset asset) {
      List<Association> assetAssociations = new ArrayList<>();
      for (Association a : associations) {
         if (a.leftAssetName.equals(asset.name)) {
            assetAssociations.add(a);
         }
         if (a.rightAssetName.equals(asset.name)) {
            assetAssociations
                  .add(new Association(a.rightAssetName, a.rightRoleName, a.rightMultiplicity, a.rightRelation, a.name, a.leftRelation, a.leftMultiplicity, a.leftRoleName, a.leftAssetName));
         }
      }
      return assetAssociations;
   }

   public Association getAssociation(String assetName_1, String associationName, String assetName_2) {
      for (Association a : associations) {
         if (a.name.equals(associationName)
               && ((a.leftAssetName.equals(assetName_1) && a.rightAssetName.equals(assetName_2)) || (a.rightAssetName.equals(assetName_1) && a.leftAssetName.equals(assetName_2)))) {
            return a;
         }
      }
      return null;
   }

   public Association getConnectedAssociation(String leftAssetName, String rightRoleName) {
      for (Association association : this.associations) {
         if (association.leftAssetName.equals(leftAssetName) && association.rightRoleName.equals(rightRoleName)) {
            return association;
         }
         if (association.rightAssetName.equals(leftAssetName) && association.leftRoleName.equals(rightRoleName)) {
            return association;
         }
      }
      // Does the association exist for the super asset of leftAsset?
      Asset leftAsset = getAsset(leftAssetName);
      if (!leftAsset.superAssetName.equals("")) {
         Association association = getConnectedAssociation(leftAsset.superAssetName, rightRoleName);
         if (association != null) {
            return association;
         }
      }

      return null;
   }

   public String getConnectedAssetName(String leftAssetName, String rightRoleName) {
      if (rightRoleName.equals("this")) {
         return leftAssetName;
      }
      Association association = getConnectedAssociation(leftAssetName, rightRoleName);
      if (association.rightRoleName.equals(rightRoleName)) {
         return association.rightAssetName;
      }
      if (association.leftRoleName.equals(rightRoleName)) {
         return association.leftAssetName;
      }
      return null;
   }

   // SecuriLangListener reads strings from the .slang file into Model
   // variables, but because the model is not yet complete, they cannot always
   // be written to the proper place (for instance, children classes may not yet
   // have been created when the first reference is encountered). Therefore,
   // Model.update() makes a second pass through the model to place all
   // information in the right place.

   public void update() {
      for (Asset asset : assets) {
         asset.inheritAttackSteps();
         // Store whether assets have specializations or not.
         if (!asset.superAssetName.equals("")) {
            Asset sAsset = getAsset(asset.superAssetName);
            sAsset.hasSpecializations = true;
         }
      }

      for (Asset parentAsset : assets) {
         for (AttackStep parentAttackStep : parentAsset.attackSteps) {
            for (AttackStepPointer childPointer : parentAttackStep.childPointers) {
               AttackStepPointer parentPointer = addStepPointer();

               parentPointer.attackStep = parentAttackStep;
               parentPointer.attackStepName = parentAttackStep.name;

               if (childPointer.roleName.equals("this")) {
                  childPointer.multiplicity = Association.ONE;
                  parentPointer.multiplicity = Association.ONE;
               }
               else {
                  childPointer.association = getConnectedAssociation(parentAsset.name, childPointer.roleName);
                  assertNotNull(String.format("Can't find the association that connects %s to the role %s (%s.%s -> %s.%s) . Perhaps the role name is incorrect?", parentAsset.name,
                        childPointer.roleName, parentAsset.name, parentAttackStep.name, childPointer.roleName, childPointer.attackStepName, childPointer.association), childPointer.association);
                  parentPointer.association = childPointer.association;
                  if (childPointer.association.rightRoleName.equals(childPointer.roleName)) {
                     childPointer.multiplicity = childPointer.association.rightMultiplicity;
                     parentPointer.multiplicity = childPointer.association.leftMultiplicity;
                     parentPointer.roleName = childPointer.association.leftRoleName;
                  }
                  if (childPointer.association.leftRoleName.equals(childPointer.roleName)) {
                     childPointer.multiplicity = childPointer.association.leftMultiplicity;
                     parentPointer.multiplicity = childPointer.association.rightMultiplicity;
                     parentPointer.roleName = childPointer.association.rightRoleName;
                  }
               }

               assertNotNull(String.format("%s could not find the multiplicity of %s.%s", parentAsset.name, childPointer.roleName, childPointer.attackStepName), childPointer.multiplicity);
               String childAssetName = getConnectedAssetName(parentAsset.name, childPointer.roleName);
               assertNotNull(childAssetName);
               Asset childAsset = getAsset(childAssetName);

               assertNotNull(String.format("Did not find %s. Its attack step %s.%s was supposed to be reached from %s.%s", childAssetName, childPointer.roleName, childPointer.attackStepName,
                     parentAsset.name, parentAttackStep.name), childAsset);
               childPointer.attackStep = childAsset.getAttackStep(childPointer.attackStepName);

               assertNotNull(String.format("Did not find the attack step %s in the asset %s, but %s.%s thinks so.", childPointer.attackStepName, childAsset.name, parentPointer.attackStep.asset.name,
                     parentPointer.attackStepName), childPointer.attackStep);
               childPointer.attackStep.parentPointers.add(parentPointer);
            }
         }

         System.out.println("looking into mandatory and nonmandatory for " + parentAsset.name);
         Asset childAsset;
         for (Association association : getAssociations(parentAsset)) {
            childAsset = getAsset(association.getTargetAssetName(parentAsset));
            assertNotNull(String.format("The association %s [%s]<--%s--> [%s] %s from %s can't find its counterpart.", association.leftAssetName, association.leftRoleName, association.name,
                  association.rightRoleName, association.rightAssetName, parentAsset.name), childAsset);
            if (parentAsset.name.equals("MyNetwork")) {
               System.out.println("Child Asset = " + childAsset.name);
            }

            if (!parentAsset.nonMandatoryChildren.contains(childAsset)) {
               if (parentAsset.name.equals("MyNetwork")) {
                  System.out.println("target relation = " + association.getTargetRelation(parentAsset));
               }
               if (association.getTargetRelation(parentAsset).equals("-)")) {
                  System.out.println("found nonmandatory");
                  parentAsset.nonMandatoryChildren.add(childAsset);
               }
               if (association.getTargetRelation(parentAsset).equals("->")) {
                  parentAsset.mandatoryChildren.add(childAsset);
               }
            }
         }
      }

   }

   class Distribution {
      String       distributionName;
      List<String> parameters = new ArrayList<>();

      public Distribution(String distributionName, List<String> parameters) {
         this.distributionName = distributionName;
         this.parameters = parameters;
      }
   }

   public static String capitalize(String line) {
      return Character.toUpperCase(line.charAt(0)) + line.substring(1);
   }

   public static String decapitalize(String line) {
      return Character.toLowerCase(line.charAt(0)) + line.substring(1);
   }
}
