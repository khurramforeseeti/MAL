package se.kth.mal.dsltest;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.*;

import se.kth.mal.Master;

public class securiLangDSLTest {
    @Rule
    public TemporaryFolder tmpFolder = new TemporaryFolder();
    
    @Test
    public void test() {
       
        try {
            Master s = new Master("./src/test/resources", 
                                  "securiLang.slng", 
                                  tmpFolder.newFolder("java").getPath(), 
                                  "auto",
                                  tmpFolder.newFolder("json").getPath(), 
                                  false, 0.1, 0.1, "");
        }
        catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
