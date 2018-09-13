package se.kth.mal;

public class AttackStepPointer {
   String      roleName       = "";
   String      subClassName   = "";
   String      attackStepName = "";
   String      multiplicity;
   AttackStep  attackStep;
   Association association;

   public String getRoleName() {
      return roleName;
   }

   public String getSubClassName() {
      return subClassName;
   }

   public String getAttackStepName() {
      return attackStepName;
   }

   public String getMultiplicity() {
      return multiplicity;
   }

   public AttackStep getAttackStep() {
      return attackStep;
   }

   public Association getAssociation() {
      return association;
   }
}
