package se.kth.mal;

import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Asset {
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
   Boolean          abstractAsset               = false;

   public Asset(String name, CompilerModel model, boolean abstractAsset) {
      this.model = model;
      this.name = name;
      nDefenses = 0;
      this.abstractAsset = abstractAsset;
   }

   public AttackStep addAttackStep(Boolean visibility, String attackStepType, String name) {
      AttackStep attackStep = new AttackStep(model, this, visibility, attackStepType, name);
      attackSteps.add(attackStep);
      if (attackStep.isDefense()) {
         nDefenses++;
      }
      return attackStep;
   }

   public String getName() {
      return name;
   }

   public String getCapitalizedName() {
      String line = new String(name);
      return Character.toUpperCase(line.charAt(0)) + line.substring(1);
   }

   public String getDecapitalizedName() {
      String line = new String(name);
      return Character.toLowerCase(line.charAt(0)) + line.substring(1);
   }

   public String getSuperAssetName() {
      return superAssetName;
   }

   public CompilerModel getModel() {
      return model;
   }

   public void setModel(CompilerModel model) {
      this.model = model;
   }

   public String getCategory() {
      return category;
   }

   public void setCategory(String category) {
      this.category = category;
   }

   public Integer getnDefenses() {
      return nDefenses;
   }

   public void setnDefenses(Integer nDefenses) {
      this.nDefenses = nDefenses;
   }

   public List<AttackStep> getAttackSteps() {
      return attackSteps;
   }

   public void setAttackSteps(List<AttackStep> attackSteps) {
      this.attackSteps = attackSteps;
   }

   public List<Asset> getMandatoryChildren() {
      return mandatoryChildren;
   }

   public void setMandatoryChildren(List<Asset> mandatoryChildren) {
      this.mandatoryChildren = mandatoryChildren;
   }

   public List<Asset> getNonMandatoryChildren() {
      return nonMandatoryChildren;
   }

   public void setNonMandatoryChildren(List<Asset> nonMandatoryChildren) {
      this.nonMandatoryChildren = nonMandatoryChildren;
   }

   public Boolean getAlreadyInheritedAttackSteps() {
      return alreadyInheritedAttackSteps;
   }

   public void setAlreadyInheritedAttackSteps(Boolean alreadyInheritedAttackSteps) {
      this.alreadyInheritedAttackSteps = alreadyInheritedAttackSteps;
   }

   public Boolean getHasSpecializations() {
      return hasSpecializations;
   }

   public void setHasSpecializations(Boolean hasSpecializations) {
      this.hasSpecializations = hasSpecializations;
   }

   public Boolean isAbstractAsset() {
      return abstractAsset;
   }

   public void setAbstractAsset(Boolean abstractAsset) {
      this.abstractAsset = abstractAsset;
   }

   public void setName(String name) {
      this.name = name;
   }

   public void setSuperAssetName(String superAssetName) {
      this.superAssetName = superAssetName;
   }

   public List<AttackStep> attackStepsExceptDefensesAndExistence() {
      List<AttackStep> as = new ArrayList<>();
      for (AttackStep attackStep : attackSteps) {
         if (attackStep.isAttackStep()) {
            as.add(attackStep);
         }
      }
      return as;
   }

   public List<AttackStep> defenses() {
      List<AttackStep> as = new ArrayList<>();
      for (AttackStep attackStep : attackSteps) {
         if (attackStep.isDefense()) {
            as.add(attackStep);
         }
      }
      return as;
   }

   public List<AttackStep> defensesExcludingExistenceRequirements() {
      List<AttackStep> as = new ArrayList<>();
      for (AttackStep attackStep : attackSteps) {
         if (attackStep.attackStepType.equals(AttackStep.DEFENSE_TYPE) && !attackStep.hasExistenceRequirements()) {
            as.add(attackStep);
         }
      }
      return as;
   }

   public List<AttackStep> defensesWithExistenceRequirementsOnly() {
      List<AttackStep> as = new ArrayList<>();
      for (AttackStep attackStep : attackSteps) {
         if (attackStep.isExistenceReqDefense()) {
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
      for (Association a : model.getAssociations()) {
         if (a.leftAssetName.equals(this.name)) {
            assetAssociations.add(a);
         }
         if (a.rightAssetName.equals(this.name)) {
            assetAssociations
                  .add(new Association(a.rightAssetName, a.rightRoleName, a.rightMultiplicity, a.rightRelation, a.name, a.leftRelation, a.leftMultiplicity, a.leftRoleName, a.leftAssetName));
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
      while (!currentAsset.superAssetName.isEmpty()) {
         currentAsset = model.getAsset(currentAsset.superAssetName);
         assetAttackSteps.addAll(currentAsset.attackSteps);
      }
      return assetAttackSteps;
   }

   public List<Asset> getNeighborAssets() {
      List<Asset> neighbors = new ArrayList<>();
      for (Association a : model.getAssociations()) {
         if (a.leftAssetName.equals(this.name)) {
            Asset neighbor = model.getAsset(a.rightAssetName);
            neighbors.add(neighbor);
         }
         else if (a.rightAssetName.equals(this.name)) {
            Asset neighbor = model.getAsset(a.leftAssetName);
            neighbors.add(neighbor);
         }
      }
      return neighbors;
   }

   public List<Association> getMandatoryAssociations() {
      List<Association> mandatoryAssociations = new ArrayList<>();
      for (Association a : model.getAssociations()) {
         if (a.leftAssetName.equals(this.name) && (a.rightMultiplicity.equals(Association.ONE) || a.rightMultiplicity.equals(Association.ONE_TO_MANY))) {
            mandatoryAssociations.add(a);
         }
         if (a.rightAssetName.equals(this.name) && (a.leftMultiplicity.equals(Association.ONE) || a.leftMultiplicity.equals(Association.ONE_TO_MANY))) {
            mandatoryAssociations.add(a.inverse());
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
      return null;
   }

   public int nAssociationsAndPublicAttackSteps() {
      int num = getAssociations().size();
      for (AttackStep attackStep : this.attackSteps) {
         if (attackStep.visibility) {
            num++;
         }
      }
      return num;
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
         assertNotNull(String.format("The asset %s is supposed to extend from %s, but asset '%s' cannot be found.", this.name, superAssetName, superAssetName), superAsset);
         superAsset.inheritAttackSteps();
         for (AttackStep superAttackStep : superAsset.attackSteps) {
            if (!attackStepNames.contains(superAttackStep.name)) {
               AttackStep newAttackStep = new AttackStep(model, this, superAttackStep.visibility, superAttackStep.attackStepType, superAttackStep.name);
               newAttackStep.superAttackStepName = superAsset.name + "." + CompilerModel.capitalize(superAttackStep.name);
               this.attackSteps.add(newAttackStep);
            }
            else {
               AttackStep specialization = getAttackStep(superAttackStep.name);
               specialization.superAttackStepName = superAsset.name + "." + CompilerModel.capitalize(superAttackStep.name);
               superAttackStep.hasSpecialization = true;
               specialization.isSpecialization = true;
            }
         }
         alreadyInheritedAttackSteps = true;
      }

   }

   public Set<String> attackStepNames() {
      Set<String> attackStepNames = new HashSet<>();
      for (AttackStep attackStep : attackSteps) {
         attackStepNames.add(attackStep.name);
      }
      return attackStepNames;
   }

}
