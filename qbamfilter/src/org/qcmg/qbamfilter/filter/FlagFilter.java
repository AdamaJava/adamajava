/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qbamfilter.filter;

import htsjdk.samtools.filter.SamRecordFilter;
import htsjdk.samtools.SAMRecord;

public class FlagFilter  implements SamRecordFilter{
    
    private final SAMFlag flag;
    private final boolean flagValue;
    private final Comparator op;

    public enum SAMFlag{
        ReadPaired,
        ProperPair,
        ReadUnmapped,
        Mateunmapped,
        ReadNegativeStrand,
        MateNegativeStrand,
        FirstOfpair,
        SecondOfpair,
        NotprimaryAlignment,
        ReadFailsVendorQuality,
        DuplicateRead,
        SupplementaryRead;
        
        /**
     * Here we assume all read with valid Flag. Any query regard to Mate read information, 
      * we check whether this read is paired or not. If not paired, it return false. So that 
      * you must check whether this SAMRead is valid or not.
      * eg. flag = 0x0000;
      * query: Flag_ProperPair == true; // it return false even it isn't a Paired read, rather exception
      * query: Flag_ProperPair != true; // it return true even it isn't a Paired read, rather exception
      * eg. flag = 0x0002;
      * query: Flag_properPair == true; // it return false even it is an invalid flag
     */
        public boolean checkFlag(final SAMRecord record){
            boolean readPairedFlag = record.getReadPairedFlag();
            return switch (this) {
                case ReadPaired -> readPairedFlag;
                case ProperPair -> readPairedFlag && record.getProperPairFlag();
                case ReadUnmapped -> readPairedFlag && record.getReadUnmappedFlag();
                case Mateunmapped -> readPairedFlag && record.getMateUnmappedFlag();
                case ReadNegativeStrand -> record.getReadNegativeStrandFlag();
                case MateNegativeStrand -> readPairedFlag && record.getMateNegativeStrandFlag();
                case FirstOfpair -> readPairedFlag && record.getFirstOfPairFlag();
                case SecondOfpair -> readPairedFlag && record.getSecondOfPairFlag();
                case NotprimaryAlignment -> record.isSecondaryAlignment();
                case ReadFailsVendorQuality -> record.getReadFailsVendorQualityCheckFlag();
                case DuplicateRead -> record.getDuplicateReadFlag();
                case SupplementaryRead -> record.getSupplementaryAlignmentFlag();
            };
        }

        /**
         * @param flagName: valid String must belong below 11 type of string ignoring letter case:
         * [ "ReadPaired", "ProperPair","ReadUnmaped","Mateunmapped", "ReadNegativeStrand",
         * "MateNegativeStrand","FirstOfpair","SecondOfpair", "NotprimaryAlignment",
         * "ReadFailsVendorQuality","DuplicateRead"];
         * @return related SAMFlag based on parameter flagName String;
         * @throws Exception if parameter flagName is invalid
         */
        static SAMFlag getFlag(String flagName) throws Exception{
            if(flagName.equalsIgnoreCase("ReadPaired")){
                return  ReadPaired;
            }
            else if(flagName.equalsIgnoreCase("ProperPair")){
                return ProperPair;
            }
            else if(flagName.equalsIgnoreCase("ReadUnmapped")){
                return ReadUnmapped;
            }
            else if(flagName.equalsIgnoreCase("Mateunmapped")){
                return Mateunmapped;
            }
            else if(flagName.equalsIgnoreCase("ReadNegativeStrand")){
                return ReadNegativeStrand;
            }
            else if(flagName.equalsIgnoreCase("MateNegativeStrand")){
                return MateNegativeStrand;
            }
            else if(flagName.equalsIgnoreCase("FirstOfpair")){
                return FirstOfpair;
            }
            else if(flagName.equalsIgnoreCase("SecondOfpair")){
                return SecondOfpair;
            }
            else if(flagName.equalsIgnoreCase("NotprimaryAlignment")){
                return NotprimaryAlignment;
            }
            else if(flagName.equalsIgnoreCase("ReadFailsVendorQuality")){
                return ReadFailsVendorQuality;
            }
            else if(flagName.equalsIgnoreCase("DuplicateRead")){
                return DuplicateRead;
            }
            else if(flagName.equalsIgnoreCase("SupplementaryRead")){
                return SupplementaryRead;
            }
           
            else{
                throw new Exception("invalid flag name: " + flagName  +
                        "in query condition Flag_" + flagName );
            }
        }
    }
    
    /**
     * initialize flag name, comparator and flag value
     * @param flagName : At moment the valid name are [ReadPaired,ProperPair,ReadUnmapped,
     * Mateunmapped,ReadNegativeStrand,MateNegativeStrand,FirstOfpair,
        SecondOfpair,NotprimaryAlignment,ReadFailsVendorQuality,DuplicateRead,SupplementaryRead].
     * @param comp: only valid on Comparator.Equal and Comparator.NotEqual.
     * See details on org.qcmg.qbamfilter.filter::Comparator.
     * @param value:  only valid at ["true","false", "1", "0" ]
     * @throws Exception if the invalid parameter are exists;
     * See usage on method filterOut.
     */
    public FlagFilter( String flagName,  Comparator comp,  String value ) throws Exception {
        //only boolean type value are variable
        if(value.equalsIgnoreCase("true") || value.equals("1")){
            flagValue = true;
        } else if(value.equalsIgnoreCase("false") || value.equals("0")){
            flagValue = false;
        } else{
            throw new Exception("can't accept non-boolean key value for query key name: "
                    + flagName + ". please try 'true', 'false', '1' or '0'");
        }
        
        //get comparator type
        if((comp == Comparator.Equal) || (comp == Comparator.NotEqual)) {
            op = comp;
        } else {
            throw new Exception("invalid flag comparator: " + flagName +
                    ". Please try '==' or '!='");
        }

        flag = SAMFlag.getFlag(flagName);
    }

    /**
     * check the record Flag value. 
     * @param record: a SAMRecord
     * @return true if the Flag is satisfied by the condition
     * Usage example: if you want filter out all reads with matched base greater equal than 35mers. 
     * SAMRecordFilter myfilter = new FlagFilter("ProperPair", Comparator.Equal, "true" );
     * if(myfilter.filterOut(record)){ System.out.println(record.toString);}
     */
    @Override
	public boolean filterOut(final SAMRecord record){
        return op.eval(flagValue, flag.checkFlag(record));
    }
    
    /**
     * It is an inherited method and return false only. 
     */
	@Override @Deprecated
	public boolean filterOut(SAMRecord arg0, SAMRecord arg1) {
		// TODO Auto-generated method stub
		return false;
	}
}
