package inhert;


import java.io.FileInputStream;

import org.qcmg.vcf.VCFFileReader;

public class InputStreamDemo {
	
   public static void main(String[] args) throws Exception {  
	   
	  String f = "/Users/christix/Documents/Eclipse/github/develop.master/adamajava/test.txt";
	   
	  FileInputStream is =new FileInputStream(f); 
	  try(VCFFileReader reader = new VCFFileReader(is)) { } catch(Exception ex) {}  
	  try { System.out.println("0Char : " + (char)is.read()); } finally {}    
            
      try {
         // new input stream created
    	  
//    	InputStream fin = new FileInputStream("/Users/christix/Documents/Eclipse/github/develop.master/adamajava/test.txt");
//      is = new BufferedInputStream(fin);
                  
         // read and print characters one by one
      	for(int i = 0; i < 3; i ++)
      		System.out.println("0Char : "+(char)is.read());
                     
         // mark is set on the input stream
         is.mark(10000);
         
         for(int i = 0; i < 5; i ++)
        	 System.out.println("1Char :  "+(char)is.read());
          
         if(is.markSupported()) {
         
            // reset invoked if mark() is supported
            is.reset();
            for(int i = 0; i < 3; i ++)
            	System.out.println("2Char :   "+(char)is.read());
          }
         
      } catch(Exception e) {
         // if any I/O error occurs
         e.printStackTrace();
      } finally {
         // releases system resources associated with this stream
         if(is!=null)
            is.close();
      }
   }
}