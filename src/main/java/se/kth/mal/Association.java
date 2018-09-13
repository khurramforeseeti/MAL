package se.kth.mal;

import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;

public class Association {
   String                     leftAssetName;
   String                     leftRoleName;
   String                     leftMultiplicity;
   String                     leftRelation;
   String                     name;
   String                     rightRelation;
   String                     rightMultiplicity;
   String                     rightRoleName;
   String                     rightAssetName;

   public static final String ONE          = "1";
   public static final String MANY         = "*";
   public static final String ONE_TO_MANY  = "1-*";
   public static final String ZERO_TO_MANY = "0-*";

   @Override
   public int hashCode() {
      return leftAssetName.hashCode() + leftRoleName.hashCode() + rightAssetName.hashCode() + rightRoleName.hashCode() + name.hashCode();
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj) {
         return true;
      }
      if (obj == null || obj.getClass() != this.getClass()) {
         return false;
      }
      Association other = (Association) obj;
      boolean leftEqualToLeft = (leftAssetName.equals(other.leftAssetName) && rightAssetName.equals(other.rightAssetName) && leftRoleName.equals(other.leftRoleName)
            && rightRoleName.equals(other.rightRoleName));
      boolean leftEqualToRight = (leftAssetName.equals(other.rightAssetName) && rightAssetName.equals(other.leftAssetName) && leftRoleName.equals(other.rightRoleName)
            && rightRoleName.equals(other.leftRoleName));
      return name.equals(other.name) && (leftEqualToLeft || leftEqualToRight);
   }

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

   public String getRightRoleName() {
      return this.rightRoleName;
   }

   public String getLeftAssetName() {
      return leftAssetName;
   }

   public String getLeftRoleName() {
      return leftRoleName;
   }

   public String getLeftMultiplicity() {
      return leftMultiplicity;
   }

   public String getLeftRelation() {
      return leftRelation;
   }

   public String getName() {
      return name;
   }

   public String getRightRelation() {
      return rightRelation;
   }

   public String getRightMultiplicity() {
      return rightMultiplicity;
   }

   public String getRightAssetName() {
      return rightAssetName;
   }

   public void setRightRoleName(String rightRoleName) {
      this.rightRoleName = rightRoleName;
   }

   public Association inverse() {
      return new Association(rightAssetName, rightRoleName, rightMultiplicity, rightRelation, name, leftRelation, leftMultiplicity, leftRoleName, leftAssetName);
   }

   public boolean isMandatoryforLeftAsset() {
      return (rightMultiplicity.equals(ONE) || rightMultiplicity.equalsIgnoreCase(ONE_TO_MANY));
   }

   public boolean isMandatoryforRightAsset() {
      return (leftMultiplicity.equals(ONE) || leftMultiplicity.equalsIgnoreCase(ONE_TO_MANY));
   }

   public boolean isSetforLeftAsset() {
      return (rightMultiplicity.equals(MANY) || rightMultiplicity.equalsIgnoreCase(ONE_TO_MANY));
   }

   public boolean isSetforRightAsset() {
      return (leftMultiplicity.equals(MANY) || leftMultiplicity.equalsIgnoreCase(ONE_TO_MANY));
   }

   public String getAssetName(String roleName) {
      if (leftRoleName.equals(roleName)) {
         return leftAssetName;
      }
      if (rightRoleName.equals(roleName)) {
         return rightAssetName;
      }
      assertTrue(String.format("Role name '%s' not in association.", roleName), false);
      return null;
   }

   public String getTargetRelation(Asset asset) {
      if (this.leftAssetName.equals(asset.name)) {
         return this.rightRelation;
      }
      if (this.rightAssetName.equals(asset.name)) {
         return this.leftRelation;
      }
      return null;
   }

   public String getTargetAssetName(Asset asset) {
      if (this.leftAssetName.equals(asset.name)) {
         return this.rightAssetName;
      }
      if (this.rightAssetName.equals(asset.name)) {
         return this.leftAssetName;
      }
      return null;
   }

   public String getTargetRoleName(Asset asset) {
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
