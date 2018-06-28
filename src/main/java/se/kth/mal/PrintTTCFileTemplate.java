package se.kth.mal;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.PrintWriter;

// We use this to print the default contents of the TTC configuration file (representing an APT).
// This file can then be modified to a different attacker profile, disallowing attacksteps by setting their TTC to "Infinity" or by increasing the required TTC.

public class PrintTTCFileTemplate {

   public PrintTTCFileTemplate() {
      // TODO Auto-generated constructor stub
   }

   public static void writeTTCConfigFile(PrintWriter writer, String malFolder, String malFile) throws Exception {
         BufferedReader in = new BufferedReader(new FileReader(malFolder + "/" + malFile));
         String line = "";
         String assetName = "";
         String attackStepName = "";
         String distribution = "";
         while ((line = in.readLine()) != null) {
            if (line.split(" ").length > 0) {
		    if (line.split(" ")[0].equals("include")) {
			    String includedMalFile = line.split(" ")[1];
			    System.out.print("Including " + malFolder + "/" + includedMalFile + "\n");
			    writeTTCConfigFile(writer, malFolder, includedMalFile);
		    }
		    if (line.matches("(.*)(abstractAsset|asset) (.*)")) {
		       assetName = line.split("sset ")[1].split(" ")[0].replaceAll("\\{", "").trim();
		    }
		    if (line.matches("(\\s*)(\\&|\\|)(.*)")) {
		       String declarations[] = line.split(" \\[");
		       attackStepName = declarations[0].replaceAll("\\||\\&", "").trim();
		       if (declarations.length > 1) {
			  distribution = declarations[1].replaceAll("\\]", "").trim();
			  writer.println(assetName + "." + attackStepName + " = " + distribution);
		       }
		       else {
			  writer.println(assetName + "." + attackStepName + " = Zero");
		       }
		    }
	    }
         }
         in.close();

   }

   public static void writeTTCConfigFile(String configFilePath, String malFolder, String malFile) {

   System.out.print("Attempting to write the TTC Config File " + configFilePath + " based on " + malFolder + "/" + malFile + "\n");
   try {
         PrintWriter writer = new PrintWriter(configFilePath, "UTF-8");
         writeTTCConfigFile(writer, malFolder, malFile);
         writer.close();
      }
      catch (Exception exception) {
         System.out.println(exception.toString());
      }

   }
}
