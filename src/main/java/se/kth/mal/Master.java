package se.kth.mal;

import java.io.IOException;

import se.kth.mal.CompilerWriter;
import se.kth.mal.PrintTTCFileTemplate;

public class Master {

   public Master(String inFolderPath, String inFileName, String javaFolderPath, String packageName, String jsonFolderPath, boolean generateTestCases,
         double probabilityOfNonMandatoryNeighbor, double probabilityOfDoubleNeighbor, String assetName) {
      try {
         CompilerWriter compilerWriter = new CompilerWriter(inFolderPath, inFileName, inFolderPath, javaFolderPath, packageName, jsonFolderPath);
      }
      catch (IOException e) {
         // TODO Auto-generated catch block
         e.printStackTrace();
      }
      System.out.println("Complete");
   }

   //
   // args[0] - Full path to the .mal language specification file
   // args[1] - Path to where the resulting Java code is places (root of the Java package)
   // args[2] - Target Java package name for the generated code on the form "com.foo.bar"
   // args[3] - Json folder path
   public static void main(String[] args) throws Exception {
      String inFolderPath = args[0].substring(0, args[0].lastIndexOf('/'));
      String inFileName = args[0].substring(args[0].lastIndexOf('/') + 1);
      Master master = new Master(inFolderPath, inFileName, args[1], args[2], args[3], false, 0.1, 0.1, "");
   }
}
