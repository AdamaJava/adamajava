package org.qcmg.picard;

import java.io.*;
import java.util.*;


public class Faidx {
    public static class SamSequenceRecord{
        String name=null;
        int length=0;
        long begin;
        int linelen=-1;
    }
    public final List<SamSequenceRecord> dict = new ArrayList<>();
    public Faidx(final File fn) throws IOException {
        long offset=0;
        FileReader rz = new FileReader(fn);
        int c;
        SamSequenceRecord ssr = null;
        while((c=rz.read())!=-1) {
            if (c == '>') {
                offset++;
                StringBuilder sb=new StringBuilder();
                ssr = new SamSequenceRecord();
                boolean ws=false;
                while((c=rz.read())!=-1 )
                    {
                    offset++;
                    if( c=='\n') break;
                    if(!ws && Character.isWhitespace(c)) ws=true;
                    if(!ws) sb.append((char)c);
                    }
                ssr.name=sb.toString();
                ssr.begin=offset;
                dict.add(ssr);
            } else{
                int n=0;
                do  {
                    offset++;
                    if( c == '\n') break;
                    n++;
                    c = rz.read();
                   } while(c!=-1);
                ssr.length+=n;
                if(ssr.linelen==-1){
                    ssr.linelen = n;
                    }
                }
            }
        }
    public void outputIdx(String fo) throws IOException {
    	 try(BufferedWriter out = new BufferedWriter(new FileWriter(fo));) {   
    		 for(SamSequenceRecord ssr: dict){
    			 	out.write(ssr.name+"\t"+ssr.length+"\t"+ssr.begin+"\t"+ssr.linelen+"\t"+(ssr.linelen+1)+"\n");
    	        }
          }    	
    }
    public void outputDict(String fo) throws IOException {
   	 try(BufferedWriter out = new BufferedWriter(new FileWriter(fo));) {   
   		 for(SamSequenceRecord ssr: dict){
   			 	out.write("@SQ\tSN:" + ssr.name+"\tLN:"+ssr.length+"\n");
   	        }
         }    	
   }
    public void print(PrintStream out){    	
        for(SamSequenceRecord ssr: dict){
            out.println(ssr.name+"\t"+ssr.length+"\t"+ssr.begin+"\t"+ssr.linelen+"\t"+(ssr.linelen+1));
        }
    }
}