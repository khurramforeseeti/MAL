package se.kth.mal;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.FileReader;
import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.BufferedWriter;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import se.kth.mal.sLangLexer;
import se.kth.mal.sLangListener;
import se.kth.mal.sLangParser;

public class CompilerModel {

   public List<Asset>       assets       = new ArrayList<>();
   public List<Association> associations = new ArrayList<>();

   public CompilerModel(String securiLangFolder, String securiLangFile) throws FileNotFoundException, IOException {
      
      
      String filePath = securiLangFolder + "/" + securiLangFile;
      
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

   public String includeIncludes(String securiLangFolder, String securiLangFile) {
            String filePath = securiLangFolder + "/" + securiLangFile;
      
      String outPath = filePath + "inc";
      try {
         FileWriter fileWriter = new FileWriter(outPath);
         BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
         appendFileToBufferedWriter(securiLangFolder, securiLangFile, bufferedWriter);
         bufferedWriter.close();
      }
      catch(FileNotFoundException ex) {
         System.out.println("Unable to open file '" + filePath + "'");
      }
      catch(IOException ex) {
         System.out.println("Error reading file '" + filePath + "'");
      }
      return outPath;
   }

   public void appendFileToBufferedWriter(String securiLangFolder, String securiLangFile, BufferedWriter bufferedWriter) throws java.io.FileNotFoundException, java.io.IOException { 
      String line = null;
            String filePath = securiLangFolder + "/" + securiLangFile;
      
      FileReader fileReader = new FileReader(filePath);
      BufferedReader bufferedReader = new BufferedReader(fileReader);
      while((line = bufferedReader.readLine()) != null) {
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

   public Asset addAsset(String name, String superAssetName) {
      Asset a = new Asset(name, this);
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
            assetAssociations.add(
                  new CompilerModel.Association(a.rightAssetName, a.rightRoleName, a.rightMultiplicity, a.rightRelation, a.name, a.leftRelation, a.leftMultiplicity, a.leftRoleName, a.leftAssetName));
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

	public void printModel() {
		
      for (CompilerModel.Asset asset : assets) {
			//System.out.print("Asset " + asset.name + ":\n");
         for (CompilerModel.AttackStep attackStep : asset.attackSteps) {
				//System.out.print("   AttackStep " + attackStep.name + ":\n");
            for (CompilerModel.AttackStepPointer childPointer : attackStep.childPointers) {
					//System.out.print("      ChildPointer to " + childPointer.roleName + "." + childPointer.attackStepName + "\n");
				}
            for (CompilerModel.AttackStepPointer parentPointer : attackStep.parentPointers) {
					//System.out.print("      ParentPointer to " + parentPointer.roleName + "." + parentPointer.attackStepName + "\n");
				}
			}
		}
	}

   // SecuriLangListener reads strings from the .slang file into Model
   // variables, but because the model is not yet complete, they cannot always
   // be written to the proper place (for instance, children classes may not yet
   // have been created when the first reference is encountered). Therefore,
   // Model.update() makes a second pass through the model to place all
   // information in the right place.

   public void update() {
      for (CompilerModel.Asset asset : assets) {
         asset.inheritAttackSteps();
         // Store whether assets have specializations or not.
         if (!asset.superAssetName.equals("")) {
            Asset sAsset = getAsset(asset.superAssetName);
            sAsset.hasSpecializations = true;
         }
      }

      for (CompilerModel.Asset parentAsset : assets) {
         for (CompilerModel.AttackStep parentAttackStep : parentAsset.attackSteps) {
            for (CompilerModel.AttackStepPointer childPointer : parentAttackStep.childPointers) {
					CompilerModel.AttackStepPointer parentPointer = addStepPointer();

               parentPointer.attackStep = parentAttackStep;
               parentPointer.attackStepName = parentAttackStep.name;

               if (childPointer.roleName.equals("this")) {
                  childPointer.multiplicity = "1";
                  parentPointer.multiplicity = "1";
               }
               else {
                  childPointer.association = getConnectedAssociation(parentAsset.name, childPointer.roleName);
                  assertNotNull("Can't find the association that connects " + parentAsset.name + " to the role " + childPointer.roleName + " (" + parentAsset.name + "." + parentAttackStep.name
                        + " -> " + childPointer.roleName + "." + childPointer.attackStepName + ") . Perhaps the role name is incorrect?", childPointer.association);
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
               assertNotNull(parentAsset.name + " could not find the multiplicity of " + childPointer.roleName + "." + childPointer.attackStepName + ".", childPointer.multiplicity);
               String childAssetName = getConnectedAssetName(parentAsset.name, childPointer.roleName);
               assertNotNull(childAssetName);
               CompilerModel.Asset childAsset = getAsset(childAssetName);
               assertNotNull("Did not find " + childAssetName + ". Its attack step " + childPointer.roleName + "." + childPointer.attackStepName + " was supposed to be reached from "
                     + parentAsset.name + "." + parentAttackStep.name, childAsset);
               childPointer.attackStep = childAsset.getAttackStep(childPointer.attackStepName);
               assertNotNull("Did not find the attack step " + childPointer.attackStepName + " in the asset " + childAsset.name + ", but " + parentPointer.attackStep.asset.name + "."
                     + parentPointer.attackStepName + " thinks so.", childPointer.attackStep);
               childPointer.attackStep.parentPointers.add(parentPointer);
            }
         }
         CompilerModel.Asset childAsset;
         for (CompilerModel.Association association : getAssociations(parentAsset)) {
            childAsset = getAsset(association.getTargetAssetName(parentAsset));
            assertNotNull("The association " + association.leftAssetName + " [" + association.leftRoleName + "] <-- " + association.name + " --> [" + association.rightRoleName + "] "
                  + association.rightAssetName + " from " + parentAsset.name + " can't find its counterpart.", childAsset);
            if (!parentAsset.nonMandatoryChildren.contains(childAsset)) {
               if (association.getTargetRelation(parentAsset).equals("-)>")) {
                  parentAsset.nonMandatoryChildren.add(childAsset);
               }
               if (association.getTargetRelation(parentAsset).equals("->>")) {
                  parentAsset.mandatoryChildren.add(childAsset);
               }
            }
         }
      }

   }

   class Asset {
      CompilerModel    model;
      String           name;
      String           category                    = "NoCategoryName";
      String           superAssetName              = "";
      Integer          nDefenses                   = 0;
      List<AttackStep> attackSteps                 = new ArrayList<>();
      List<Asset>      mandatoryChildren           = new ArrayList<>();
      List<Asset>      nonMandatoryChildren        = new ArrayList<>();
      Boolean          alreadyInheritedAttackSteps = false;
      Boolean          hasSpecializations          = false;

      public Asset(String name, CompilerModel model) {
         this.model = model;
         this.name = name;
         nDefenses = 0;
      }

      public AttackStep addAttackStep(Boolean visibility, String attackStepType, String name) {
         AttackStep attackStep = new AttackStep(this, visibility, attackStepType, name);
         attackSteps.add(attackStep);
         if (attackStepType.equals("#")) {
            nDefenses++;
         }
         return attackStep;
      }

      public List<AttackStep> attackStepsExceptDefensesAndExistence() {
         List<AttackStep> as = new ArrayList<>();
         for (AttackStep attackStep : attackSteps) {
            if (attackStep.attackStepType.equals("&") || attackStep.attackStepType.equals("|")) {
               as.add(attackStep);
            }
         }
         return as;
      }

      public List<AttackStep> defenses() {
         List<AttackStep> as = new ArrayList<>();
         for (AttackStep attackStep : attackSteps) {
            if (attackStep.attackStepType.equals("#") || attackStep.attackStepType.equals("E") || attackStep.attackStepType.equals("3")) {
               as.add(attackStep);
            }
         }
         return as;
      }

      public List<AttackStep> defensesExcludingExistenceRequirements() {
         List<AttackStep> as = new ArrayList<>();
         for (AttackStep attackStep : attackSteps) {
            if (attackStep.attackStepType.equals("#") && !attackStep.hasExistenceRequirements()) {
               as.add(attackStep);
            }
         }
         return as;
      }

      public AttackStepPointer addStepPointer() {
         return new AttackStepPointer();
      }

      public List<Association> getAssociations() {
         List<Association> assetAssociations = new ArrayList<>();
         for (Association a : associations) {
            if (a.leftAssetName.equals(this.name)) {
               assetAssociations.add(a);
            }
            else {
               if (a.rightAssetName.equals(this.name)) {
                  assetAssociations.add(new CompilerModel.Association(a.rightAssetName, a.rightRoleName, a.rightMultiplicity, a.rightRelation, a.name, a.leftRelation, a.leftMultiplicity,
                        a.leftRoleName, a.leftAssetName));
               }
            }
         }
         return assetAssociations;
      }

      public List<Association> getAssociationsIncludingInherited() {
         List<Association> assetAssociations = new ArrayList<>();
         Asset currentAsset = this;
         assetAssociations.addAll(currentAsset.getAssociations());
         while (!currentAsset.superAssetName.equals("")) {
            currentAsset = model.getAsset(currentAsset.superAssetName);
            assetAssociations.addAll(currentAsset.getAssociations());
         }
         return assetAssociations;
      }

      public List<AttackStep> getAttackStepsIncludingInherited() {
         List<AttackStep> assetAttackSteps = new ArrayList<>();
         Asset currentAsset = this;
         assetAttackSteps.addAll(currentAsset.attackSteps);
         while (!currentAsset.superAssetName.equals("")) {
            currentAsset = model.getAsset(currentAsset.superAssetName);
            assetAttackSteps.addAll(currentAsset.attackSteps);
         }
         return assetAttackSteps;
      }

      public List<Asset> getNeighborAssets() {
         List<Asset> neighbors = new ArrayList<>();
         for (Association a : associations) {
            if (a.leftAssetName.equals(this.name)) {
               Asset neighbor = model.getAsset(a.rightAssetName);
               neighbors.add(neighbor);
            }
            else {
               if (a.rightAssetName.equals(this.name)) {
                  Asset neighbor = model.getAsset(a.leftAssetName);
                  neighbors.add(neighbor);
               }
            }
         }
         return neighbors;
      }

      public List<Association> getMandatoryAssociations() {
         List<Association> mandatoryAssociations = new ArrayList<>();
         for (Association a : associations) {
            if (a.leftAssetName.equals(this.name) && (a.rightMultiplicity.equals("1") || a.rightMultiplicity.equals("1-*"))) {
               mandatoryAssociations.add(a);
            }
            else {
               if (a.rightAssetName.equals(this.name) && (a.leftMultiplicity.equals("1") || a.leftMultiplicity.equals("1-*"))) {
                  mandatoryAssociations.add(a.inverse());
               }
            }
         }
         return mandatoryAssociations;
      }

      public AttackStep getAttackStep(String name) {
         for (AttackStep attackStep : this.attackSteps) {
            if (attackStep.name.equals(name)) {
               return attackStep;
            }
         }
         // if (!superAssetName.equals("")) {
         // Asset superAsset = model.getAsset(superAssetName);
         // if (superAsset != null) {
         // return superAsset.getAttackStep(name);
         // }
         // }
         return null;
      }

      public int nAssociationsAndPublicAttackSteps() {
         int n = getAssociations().size();
         for (AttackStep attackStep : this.attackSteps) {
            if (attackStep.visibility) {
               n++;
            }
         }
         return n;
      }

      public int nInheritedAssociations() {
         Asset superAsset = model.getAsset(superAssetName);
         if (superAsset == null) {
            return 0;
         }
         else {
            return superAsset.nInheritedAssociations() + superAsset.nAssociationsAndPublicAttackSteps();
         }
      }

      public String defaultInstanceName() {
         return name.substring(0, 1).toLowerCase() + name.substring(1);
      }

      public Set<Asset> superAssets() {
         Set<Asset> sA = new HashSet<>();
         sA.add(this);
         if (superAssetName == "") {
            return sA;
         }
         else {
            Asset superAsset = model.getAsset(superAssetName);
            sA.addAll(superAsset.superAssets());
            return sA;
         }
      }

      public void inheritAttackSteps() {
         if (superAssetName != "" && !alreadyInheritedAttackSteps) {
            Set<String> attackStepNames = attackStepNames();
            Asset superAsset = model.getAsset(superAssetName);
	    
            assertNotNull("The asset " + this.name + " is supposed to extend from " + superAssetName + ", but that asset cannot be found.", superAsset);
           
	    superAsset.inheritAttackSteps();
            for (CompilerModel.AttackStep superAttackStep : superAsset.attackSteps) {
               if (!attackStepNames.contains(superAttackStep.name)) {
                  AttackStep newAttackStep = new AttackStep(this, superAttackStep.visibility, superAttackStep.attackStepType, superAttackStep.name);
                  newAttackStep.superAttackStepName = superAsset.name + "." + capitalize(superAttackStep.name);
                  this.attackSteps.add(newAttackStep);
               }
               else {
                  CompilerModel.AttackStep specialization = getAttackStep(superAttackStep.name);
                  specialization.superAttackStepName = superAsset.name + "." + capitalize(superAttackStep.name);
                  superAttackStep.hasSpecialization = true;
                  specialization.isSpecialization = true;
               }
            }
            alreadyInheritedAttackSteps = true;
         }

      }

      public Set<String> attackStepNames() {
         Set<String> attackStepNames = new HashSet<>();
         for (CompilerModel.AttackStep attackStep : attackSteps) {
            attackStepNames.add(attackStep.name);
         }
         return attackStepNames;
      }

   }

   class AttackStepPointer {
      public String      roleName       = "";
      public String      subClassName   = "";
      public String      attackStepName = "";
      public String      multiplicity;
      public AttackStep  attackStep;
      public Association association;
   }

   class AttackStep {
      public boolean                 isSpecialization;
      String                         superAttackStepName       = "";
      Boolean                        hasSpecialization         = false;
      Asset                          asset;
      AttackStep                     containerStep;
      Boolean                        mostImportant             = false;
      Boolean                        visibility;
      String                         attackStepType;
      String                         name;
      String                         description;
      String                         ttcFunction               = "Default";
      List<Float>                    ttcParameters             = new ArrayList<>();
      public List<AttackStepPointer> childPointers             = new ArrayList<>();
      List<AttackStepPointer>        parentPointers            = new ArrayList<>();
      List<String>                   existenceRequirementRoles = new ArrayList<>();

      public AttackStep(Asset asset, Boolean visibility, String attackStepType, String name) {
         this.asset = asset;
         this.visibility = visibility;
         this.attackStepType = attackStepType;
         this.name = name;
      }

      public String fullDefaultName() {
         return asset.defaultInstanceName() + "." + name;
      }

      public List<AttackStepPointer> inheritedChildPointers() {
         List<AttackStepPointer> inheritedChildPointers = new ArrayList<>();
         if (!asset.superAssetName.equals("")) {
            if (!superAttackStepName.equals("")) {
               String theSuperAttackStepName = decapitalize(superAttackStepName.substring(superAttackStepName.lastIndexOf('.') + 1));
               Asset superAsset = getAsset(asset.superAssetName);
               AttackStep superAttackStep = superAsset.getAttackStep(theSuperAttackStepName);
               inheritedChildPointers.addAll(superAttackStep.childPointers);
               inheritedChildPointers.addAll(superAttackStep.inheritedChildPointers());
            }
         }
         return inheritedChildPointers;
      }

      public boolean hasExistenceRequirements() {
         for (AttackStep step : getSupersIncludingSelf()) {
            if (!step.existenceRequirementRoles.isEmpty()) {
               return true;
            }
         }
         return false;
      }

      public List<AttackStep> getSupersIncludingSelf() {
         List<AttackStep> supers = new ArrayList<>();
         supers.add(this);
         if (!superAttackStepName.equals("")) {
            String superAttackStepName = decapitalize(this.superAttackStepName.substring(this.superAttackStepName.lastIndexOf('.') + 1));
            String superAssetName = this.superAttackStepName.substring(0, this.superAttackStepName.lastIndexOf('.'));
            if (!superAssetName.equals("") && !superAttackStepName.equals("")) {
               Asset superAsset = getAsset(superAssetName);
               AttackStep superAttackStep = superAsset.getAttackStep(superAttackStepName);
               assertTrue("Can't find the superAttackStep, attacks have to start with a lower case letter.", superAttackStep!=null);
               supers.addAll(superAttackStep.getSupersIncludingSelf());
            }
         }
         return supers;
      }

      public AttackStep getSuper() {
         if (!superAttackStepName.equals("")) {
            String superAttackStepName = decapitalize(this.superAttackStepName.substring(this.superAttackStepName.lastIndexOf('.') + 1));
            String superAssetName = this.superAttackStepName.substring(0, this.superAttackStepName.lastIndexOf('.'));
            if (!superAssetName.equals("") && !superAttackStepName.equals("")) {
               Asset superAsset = getAsset(superAssetName);
               AttackStep superAttackStep = superAsset.getAttackStep(superAttackStepName);
               return superAttackStep;
            }
         }
         return null;
      }
   }

   class Defense {
      String       name;
      List<String> children = new ArrayList<>();
      Asset        enablingAsset;

      public Defense(String name) {
         this.name = name;
      }
   }

   class Association {
      String leftAssetName;
      String leftRoleName;
      String leftMultiplicity;
      String leftRelation;
      String name;
      String rightRelation;
      String rightMultiplicity;
      String rightRoleName;
      String rightAssetName;

      public Association(String leftAssetName, String leftRoleName, String leftMultiplicity, String leftRelation, String name, String rightRelation, String rightMultiplicity, String rightRoleName,
            String rightAssetName) {
         this.leftAssetName = leftAssetName;
         this.leftRoleName = leftRoleName;
         this.leftMultiplicity = leftMultiplicity;
         this.leftRelation = leftRelation;
         this.name = name;
         this.rightRelation = rightRelation;
         this.rightMultiplicity = rightMultiplicity;
         this.rightRoleName = rightRoleName;
         this.rightAssetName = rightAssetName;
      }

      public Association inverse() {
         return new Association(rightAssetName, rightRoleName, rightMultiplicity, rightRelation, name, leftRelation, leftMultiplicity, leftRoleName, leftAssetName);
      }

      public boolean isMandatoryforLeftAsset() {
         return (rightMultiplicity.equals("1") || rightMultiplicity.equalsIgnoreCase("1-*"));
      }

      public boolean isMandatoryforRightAsset() {
         return (leftMultiplicity.equals("1") || leftMultiplicity.equalsIgnoreCase("1-*"));
      }

      public boolean isSetforLeftAsset() {
         return (rightMultiplicity.equals("*") || rightMultiplicity.equalsIgnoreCase("1-*"));
      }

      public boolean isSetforRightAsset() {
         return (leftMultiplicity.equals("*") || leftMultiplicity.equalsIgnoreCase("1-*"));
      }

      String getAssetName(String roleName) {
         if (leftRoleName.equals(roleName)) {
            return leftAssetName;
         }
         if (rightRoleName.equals(roleName)) {
            return rightAssetName;
         }
         assertTrue("Role name not in association.", false);
         return null;
      }

      String getTargetRelation(Asset asset) {
         if (this.leftAssetName.equals(asset.name)) {
            return this.rightRelation;
         }
         if (this.rightAssetName.equals(asset.name)) {
            return this.leftRelation;
         }
         return null;
      }

      String getTargetAssetName(Asset asset) {
         if (this.leftAssetName.equals(asset.name)) {
            return this.rightAssetName;
         }
         if (this.rightAssetName.equals(asset.name)) {
            return this.leftAssetName;
         }
         return null;
      }

      String getTargetRoleName(Asset asset) {
         if (this.leftAssetName.equals(asset.name)) {
            return this.rightRoleName;
         }
         if (this.rightAssetName.equals(asset.name)) {
            return this.leftRoleName;
         }
         return null;
      }

      public Object getTargetRoleNameIncludingInheritance(Asset asset) {
         Set<String> superAssetNames = new HashSet<>();
         superAssetNames.add(asset.name);
         for (Asset superAsset : asset.superAssets()) {
            superAssetNames.add(superAsset.name);
         }
         if (superAssetNames.contains(this.leftAssetName)) {
            return this.rightRoleName;
         }
         if (superAssetNames.contains(this.rightAssetName)) {
            return this.leftRoleName;
         }
         return null;
      }

      public String getSourceRoleName(Asset asset) {
         if (this.leftAssetName.equals(asset.name)) {
            return this.leftRoleName;
         }
         if (this.rightAssetName.equals(asset.name)) {
            return this.rightRoleName;
         }
         return null;
      }

      public String targetMultiplicity(Asset asset) {
         if (this.leftAssetName.equals(asset.name)) {
            return this.rightMultiplicity;
         }
         if (this.rightAssetName.equals(asset.name)) {
            return this.leftMultiplicity;
         }
         return null;
      }

      public String targetMultiplicityIncludingInheritance(Asset asset) {
         Set<String> superAssetNames = new HashSet<>();
         superAssetNames.add(asset.name);
         for (Asset superAsset : asset.superAssets()) {
            superAssetNames.add(superAsset.name);
         }
         if (superAssetNames.contains(this.leftAssetName)) {
            return this.rightMultiplicity;
         }
         if (superAssetNames.contains(this.rightAssetName)) {
            return this.leftMultiplicity;
         }
         return null;
      }

      public Object sourceMultiplicity(Asset asset) {
         if (this.leftAssetName.equals(asset.name)) {
            return this.leftMultiplicity;
         }
         if (this.rightAssetName.equals(asset.name)) {
            return this.rightMultiplicity;
         }
         return null;
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

   private String capitalize(final String line) {
      return Character.toUpperCase(line.charAt(0)) + line.substring(1);
   }

   private String decapitalize(final String line) {
      return Character.toLowerCase(line.charAt(0)) + line.substring(1);
   }

}
