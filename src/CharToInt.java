import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.commons.lang3.StringEscapeUtils;

public class CharToInt {
    
    // TODO use this
    
    public static void fileToInt(File f) {
        //TODO
    }
    
    public static String allToInt(String in) {
        
        int index = in.indexOf("'");
        while (index >= 0) {
            System.out.println(index);
            String bs = in.substring(index);
            int bindex = in.indexOf("'");
            bs = in.substring(1, bindex);
            bs = "" + ((int) StringEscapeUtils.unescapeJava(bs).toCharArray()[0]);
            
            in = in.substring(0, index);
            in = in + in.substring(bindex, in.length());
            
            index = in.indexOf("'", bindex + 1);
        }
                
        return in;
        
    }
    
    public static void main(String[] args) throws IOException {

        BufferedReader cin = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("Enter characters, 'exit' to quit.");
        String s = "";
        while(!s.equals("exit")) {
           s = cin.readLine();
           System.out.println((int) StringEscapeUtils.unescapeJava(s).toCharArray()[0]);
        }
    }

}
