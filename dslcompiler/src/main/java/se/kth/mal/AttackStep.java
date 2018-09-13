package se.kth.mal;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

public class AttackStep {
   boolean                        isSpecialization;
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
   CompilerModel                  model;

   public static final String     DEFENSE_TYPE              = "#";
   public static final String     DEFENSE_E_TYPE            = "E";
   public static final String     DEFENSE_3_TYPE            = "3";
   public static final String     ATTACKSTEP_AND_TYPE       = "&";
   public static final String     ATTACKSTEP_OR_TYPE        = "|";

   public AttackStep(CompilerModel model, Asset asset, Boolean visibility, String attackStepType, String name) {
      this.asset = asset;
      this.visibility = visibility;
      this.attackStepType = attackStepType;
      this.name = name;
      this.model = model;
   }

   public String fullDefaultName() {
      return asset.defaultInstanceName() + "." + name;
   }

   public List<AttackStepPointer> inheritedChildPointers() {
      List<AttackStepPointer> inheritedChildPointers = new ArrayList<>();
      if (!asset.superAssetName.equals("")) {
         if (!superAttackStepName.equals("")) {
            String theSuperAttackStepName = CompilerModel.decapitalize(superAttackStepName.substring(superAttackStepName.lastIndexOf('.') + 1));
            Asset superAsset = model.getAsset(asset.superAssetName);
            AttackStep superAttackStep = superAsset.getAttackStep(theSuperAttackStepName);
            inheritedChildPointers.addAll(superAttackStep.childPointers);
            inheritedChildPointers.addAll(superAttackStep.inheritedChildPointers());
         }
      }
      return inheritedChildPointers;
   }

   public boolean isHiddenAttackStep() {
      return name.startsWith("_");
   }

   public boolean isDisplayableDefense() {
      return attackStepType.equals(DEFENSE_TYPE);
   }

   public String getCapitalizedName() {
      String line = new String(name);
      return Character.toUpperCase(line.charAt(0)) + line.substring(1);
   }

   public boolean isDefense() {
      return attackStepType.equals(DEFENSE_TYPE) || attackStepType.equals(DEFENSE_E_TYPE) || attackStepType.equals(DEFENSE_3_TYPE);
   }

   public boolean isExistenceReqDefense() {
      return attackStepType.equals(DEFENSE_E_TYPE) || attackStepType.equals(DEFENSE_3_TYPE);
   }

   public boolean isAttackStep() {
      return attackStepType.equals(ATTACKSTEP_AND_TYPE) || attackStepType.equals(ATTACKSTEP_OR_TYPE);
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
         String superAttackStepName = CompilerModel.decapitalize(this.superAttackStepName.substring(this.superAttackStepName.lastIndexOf('.') + 1));
         String superAssetName = this.superAttackStepName.substring(0, this.superAttackStepName.lastIndexOf('.'));
         if (!superAssetName.equals("") && !superAttackStepName.equals("")) {
            Asset superAsset = model.getAsset(superAssetName);
            AttackStep superAttackStep = superAsset.getAttackStep(superAttackStepName);
            assertTrue("Can't find the superAttackStep, attacks have to start with a lower case letter.", superAttackStep != null);
            supers.addAll(superAttackStep.getSupersIncludingSelf());
         }
      }
      return supers;
   }

   public AttackStep getBaseAttackStep() {
      AttackStep as = getSuper();
      if (as != null) {
         return as.getBaseAttackStep();
      }
      return this;
   }

   public AttackStep getSuper() {
      if (!superAttackStepName.equals("")) {
         String superAttackStepName = CompilerModel.decapitalize(this.superAttackStepName.substring(this.superAttackStepName.lastIndexOf('.') + 1));
         String superAssetName = this.superAttackStepName.substring(0, this.superAttackStepName.lastIndexOf('.'));
         if (!superAssetName.equals("") && !superAttackStepName.equals("")) {
            Asset superAsset = model.getAsset(superAssetName);
            AttackStep superAttackStep = superAsset.getAttackStep(superAttackStepName);
            return superAttackStep;
         }
      }
      return null;
   }

   public boolean isSpecialization() {
      return isSpecialization;
   }

   public String getSuperAttackStepName() {
      return superAttackStepName;
   }

   public Boolean getHasSpecialization() {
      return hasSpecialization;
   }

   public Asset getAsset() {
      return asset;
   }

   public AttackStep getContainerStep() {
      return containerStep;
   }

   public Boolean getMostImportant() {
      return mostImportant;
   }

   public Boolean getVisibility() {
      return visibility;
   }

   public String getAttackStepType() {
      return attackStepType;
   }

   public String getName() {
      return name;
   }

   public String getDescription() {
      return description;
   }

   public String getTtcFunction() {
      return ttcFunction;
   }

   public List<Float> getTtcParameters() {
      return ttcParameters;
   }

   public List<AttackStepPointer> getChildPointers() {
      return childPointers;
   }

   public List<AttackStepPointer> getParentPointers() {
      return parentPointers;
   }

   public List<String> getExistenceRequirementRoles() {
      return existenceRequirementRoles;
   }

   public CompilerModel getModel() {
      return model;
   }
}
