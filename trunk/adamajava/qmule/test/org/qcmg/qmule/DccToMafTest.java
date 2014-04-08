package org.qcmg.qmule;

import java.util.HashMap;
import java.util.Map;

import junit.framework.Assert;

import org.junit.Ignore;
import org.junit.Test;
import org.qcmg.common.dcc.DccConsequence;
import org.qcmg.common.string.StringUtils;

public class DccToMafTest {
	
	
	@Ignore
	public void testRealLifeExample1() {
		// want to test the following dcc record
		/**
		 * APGI_2193_SNP_42944     1       9       21815432        21815432        1       -888    -888    A       A/A     A/G     A>G    
		 *  -999    -999    30      2       2       -888    -999    -999    A:43[39.7],1[40]        
		 *  A:25[39.15],0[0],G:5[40],0[0]   
		 *  NON_SYNONYMOUS_CODING--SPLICE_SITE,NON_SYNONYMOUS_CODING--SPLICE_SITE,5PRIME_UTR--SPLICE_SITE,
		 *  NON_SYNONYMOUS_CODING--SPLICE_SITE,NON_SYNONYMOUS_CODING--SPLICE_SITE,DOWNSTREAM,UPSTREAM 
		 *  I12V,I12V,-888,I12V,I29V,-888,-888      147A>G,147A>G,128A>G,126A>G,85A>G,-888,-888     -888,-888,-888,-888,-888,-888,-888      
		 *  ENSG00000099810|ENSG00000233326|ENSG00000229298 
		 *  ENST00000404796,ENST00000380172,ENST00000355696,ENST00000419385,ENST00000443256|ENST00000427788|ENST00000447235 
		 *  55      -999    CDKN2BAS|-888|-888      -888,TIGR01694,-888,-888,TIGR01694|-888|-888    
		 *  -888,Tigrfam,-888,-888,Tigrfam|-888|-888        
		 *  -888,MeThioAdo_phosphorylase,-888,-888,MeThioAdo_phosphorylase|-888|-888        A/G     chr9:21815432-21815432  --
		 */
		Map<String, String> canonicalMap = new HashMap<String, String>();
		canonicalMap.put("ENSG00000099810", "ENST00000404796");
		canonicalMap.put("ENSG00000233326", "ENST00000427788");
		canonicalMap.put("ENSG00000229298", "ENST00000447235");
		
		String geneString =  "ENSG00000099810|ENSG00000233326|ENSG00000229298";
		String [] genes = geneString.split("\\|");
		Assert.assertEquals(3, genes.length);
		
		String transcriptsString =  "ENST00000404796,ENST00000380172,ENST00000355696,ENST00000419385,ENST00000443256|ENST00000427788|ENST00000447235";
		String [] transcriptIds = transcriptsString.split("\\|");
		Assert.assertEquals(3, transcriptIds.length);
		
		String[] consequenceResults = new String[] {"Splice_Site" , "3'Flank", "5'Flank"};
		
		String consequencesString = "NON_SYNONYMOUS_CODING--SPLICE_SITE,NON_SYNONYMOUS_CODING--SPLICE_SITE,5PRIME_UTR--SPLICE_SITE,NON_SYNONYMOUS_CODING--SPLICE_SITE,NON_SYNONYMOUS_CODING--SPLICE_SITE,DOWNSTREAM,UPSTREAM";
		
		testInputs(canonicalMap, genes, transcriptIds, consequenceResults, consequencesString);
		
	}
	
	@Ignore
	public void testRealLifeExample2() {
		// want to test the following dcc record
		/**
		 * APGI_2158_SNP_61733     1       13      25068851        25068851        1       -888    -888    G       G/G     A/G     G>A     
		 * -999    -999    76      2       2       -888    -999    -999    G:7[40],53[38.71]       G:10[39.9],58[37.33],A:8[40],0[0]       
		 * WITHIN_NON_CODING_GENE,STOP_GAINED      -888,Q201*      -888,707G>A     -888,-888       
		 * ENSG00000205822|ENSG00000102699 ENST00000445572|ENST00000381989 55
      		 * -999    -888|PARP4      -888|-888       -888|-888       -888|-888       G/A     chr13:25068851-25068851 ���
		 */
		Map<String, String> canonicalMap = new HashMap<String, String>();
		canonicalMap.put("ENSG00000205822", "noMatch");
		canonicalMap.put("ENSG00000102699", "ENST00000381989");
		
		String geneString =  "ENSG00000205822|ENSG00000102699";
		String [] genes = geneString.split("\\|");
		Assert.assertEquals(2, genes.length);
		
		String transcriptsString =  "ENST00000445572|ENST00000381989";
		String [] transcriptIds = transcriptsString.split("\\|");
		Assert.assertEquals(2, transcriptIds.length);
		
		String[] consequenceResults = new String[] {null, "Nonsense_Mutation"};
		
		String consequencesString = "WITHIN_NON_CODING_GENE,STOP_GAINED";
		
		testInputs(canonicalMap, genes, transcriptIds, consequenceResults, consequencesString);
		
	}
	
	@Test
	public void testRealLifeExample3() {
		// v70 Ensembl
		// AOCS exome solid data
		// want to test the following dccq record
		/**
		 * AOCS_066_SNP_3124       1       1       115256530       115256530       1       G/T     -1      G       
		 * G/G     G/T     G>T     -999    -999    1.2420510993064712E-22  110     1       2       -888    
		 * rs121913254     -999    G:25[34.12],67[36.06]   G:10[33.2],31[33.35],T:16[39.62],53[38.58]	44      
		 * missense_variant,downstream_gene_variant,downstream_gene_variant,downstream_gene_variant,downstream_gene_variant,downstream_gene_variant,downstream_gene_variant,downstream_gene_variant,downstream_gene_variant        
		 * Q61K;Q61K;Q61K;Q61K;Q61K;Q61K;Q61K;Q61K;Q61K,-888,-888,-888,-888,-888,-888,-888,-888    
		 * 435G>T;435G>T;435G>T;435G>T;435G>T;435G>T;435G>T;435G>T;435G>T,-888,-888,-888,-888,-888,-888,-888,-888  
		 * PF00071;PF08477;PF00025;PF00009;TIGR00231;PR00449;SM00173;SM00175;SM00174       
		 * ENSG00000213281,ENSG00000009307,ENSG00000009307,ENSG00000009307,ENSG00000009307,ENSG00000009307,ENSG00000009307,ENSG00000009307,ENSG00000009307 
		 * ENST00000369535,ENST00000339438,ENST00000438362,ENST00000358528,ENST00000261443,ENST00000530886,ENST00000369530,ENST00000483407,ENST00000534699 
		 * 70      -999    NRAS,CSDE1,CSDE1,CSDE1,CSDE1,CSDE1,CSDE1,CSDE1,CSDE1    PF00071;PF08477;PF00025;PF00009;TIGR00231;PR00449;SM00173;SM00175;SM00174       
		 * pfam;pfam;pfam;pfam;tigrfam;prints;smart;smart;smart    Small_GTPase;MIRO-like;Small_GTPase_ARF/SAR;EF_GTP-bd_dom;Small_GTP-bd_dom;Small_GTPase;Small_GTPase_Ras;Small_GTPase_Rab_type;Small_GTPase_Rho 
		 * chr1:115256530-115256530        PASS    TTCTTTTCCAG
		 */
		Map<String, String> canonicalMap = new HashMap<String, String>();
		canonicalMap.put("ENSG00000205822", "noMatch");
		canonicalMap.put("ENSG00000102699", "ENST00000381989");
		
		String geneString =  "ENSG00000213281,ENSG00000009307,ENSG00000009307,ENSG00000009307,ENSG00000009307,ENSG00000009307,ENSG00000009307,ENSG00000009307,ENSG00000009307";
		String [] genes = geneString.split(",");
		Assert.assertEquals(9, genes.length);
		
		String transcriptsString =  "ENST00000369535,ENST00000339438,ENST00000438362,ENST00000358528,ENST00000261443,ENST00000530886,ENST00000369530,ENST00000483407,ENST00000534699";
		String [] transcriptIds = transcriptsString.split(",");
		Assert.assertEquals(9, transcriptIds.length);
		
		String[] consequenceResults = new String[] {"Nonsense_Mutation", "3'Flank"};
		
		String consequencesString = "missense_variant,downstream_gene_variant,downstream_gene_variant,downstream_gene_variant,downstream_gene_variant,downstream_gene_variant,downstream_gene_variant,downstream_gene_variant,downstream_gene_variant";
		
		testInputs(canonicalMap, genes, transcriptIds, consequenceResults, consequencesString);
		
	}

	private void testInputs(Map<String, String> canonicalMap, String[] genes,
			String[] transcriptIds, String[] consequenceResults,
			String consequencesString) {
		int i = 0, allTranscriptIdCount = 0;
		for (String gene : genes) {
			String[] geneSpecificTranscriptIds =  transcriptIds[i].split(",");
			
			String canonicalTranscripId = canonicalMap.get(gene);
			
			if (null != canonicalTranscripId) {
				int positionInTranscripts = StringUtils.getPositionOfStringInArray(geneSpecificTranscriptIds, canonicalTranscripId, true);
				String [] consequences = consequencesString.split(",");
				if (positionInTranscripts > -1) {
					// we have a matching canonical transcript
					positionInTranscripts += allTranscriptIdCount;
					
					if (consequences.length > positionInTranscripts) {
						Assert.assertEquals(consequenceResults[i], DccConsequence.getMafName(consequences[positionInTranscripts], org.qcmg.common.dcc.MutationType.SNP, -1));
//						maf.setVariantClassification(DccConsequence.getMafName(params[22], type, Integer.parseInt(params[1])));
					} else {
						Assert.fail("consequences.length is <= positionInTranscripts");
					}
				}
				// update transcript count
				allTranscriptIdCount += geneSpecificTranscriptIds.length;
				
			} else {
				// still want to keep the transcript count up to date 
				allTranscriptIdCount += geneSpecificTranscriptIds.length;
//				maf.setVariantClassification(DccConsequence.getMafName(params[22], type, Integer.parseInt(params[1])));
			}
			
			i++;
		}
	}
	
	
	@Test
	public void testMultipleDelimiters() {
		String inputString = "ENST00000438000,ENST00000428930,ENST00000447407,ENST00000419503|ENST00000439302,ENST00000437865,ENST00000422716,ENST00000435585,ENST00000456937|ENST00000416712,ENST00000429121,ENST00000427309";
		
		String [] params = inputString.split("[,|]");
		Assert.assertEquals(12, params.length);
		Assert.assertEquals("ENST00000427309", params[11]);
	}
}
