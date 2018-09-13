package se.kth.mal;

import java.util.ArrayList;
import java.util.List;

class Defense {
   String       name;
   List<String> children = new ArrayList<>();
   Asset        enablingAsset;

   public Defense(String name) {
      this.name = name;
   }
}
