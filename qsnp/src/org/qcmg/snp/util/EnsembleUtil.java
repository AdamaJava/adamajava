/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.snp.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.transform.Transformers;
import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.ensembl.dao.DAOFactory;
import org.qcmg.ensembl.dao.HibernateDAOFactory;
import org.qcmg.ensembl.model.EnsemblDomainDTO;
import org.qcmg.ensembl.model.SeqRegion;
import org.qcmg.ensembl.model.TranscriptStable;
import org.qcmg.ensembl.model.TranscriptVariation;
import org.qcmg.ensembl.model.VariationFeature;
import org.qcmg.ensembl.util.HibernateUtil;
import org.qcmg.genesymbol.GeneSymbolRecord;
import org.qcmg.pileup.QSnpRecord;

public class EnsembleUtil {
	
	private static final QLogger logger = QLoggerFactory.getLogger(EnsembleUtil.class);
	
	public static List<SeqRegion> seqRegions;
//	public static Map<String, EnsemblDomainDTO> ensemblDomains = new HashMap<String, EnsemblDomainDTO>(600000,0.99f);
	public static Map<String,List<EnsemblDomainDTO>> ensemblDomainList = new HashMap<String, List<EnsemblDomainDTO>>(600000,0.99f);
//	public static List<EnsemblDomainDTO> ensemblDomainList;
	public static DAOFactory factory;
	static {
		factory = DAOFactory.instance(HibernateDAOFactory.class);
		seqRegions = factory.getSeqRegionDAO().findByCoordId(Long.valueOf(2));
		logger.info("Retrieved " + seqRegions.size() + " SeqRegions from db");
		
		populateDomainMap();
		logger.info("Retrieved " + ensemblDomainList.size() + " ensemblDomainList from db");
	}
	
//	public static TranscriptStable getTranscriptStable(Transcript transcript) {
//		TranscriptStable transcriptStable = (TranscriptStable) HibernateUtil.getSession().createCriteria(TranscriptStable.class)
//		.add(Restrictions.eq("transcript", transcript)).uniqueResult();
//		return transcriptStable;
//	}
//	public static GeneStableId getGeneStable(Gene gene) {
//		GeneStableId geneStable = (GeneStableId) HibernateUtil.getSession().createCriteria(GeneStableId.class)
//		.add(Restrictions.eq("gene", gene)).uniqueResult();
//		return geneStable;
//	}
	
	public static void populateDomainMap() {
		 List<EnsemblDomainDTO> domains = HibernateUtil.getSession().createSQLQuery(
//				final List<EnsemblDomainDTO> ensemblDomainList = HibernateUtil.getSession().createSQLQuery(
				
				"SELECT DISTINCT gsi.stable_id as geneStableId, trsi.stable_id as transcriptStableId, " +
				"tsi.stable_id as translationStableId, p.hit_name as hitName, p.seq_start as start, p.seq_end as end, a.logic_name as logicName, i.interpro_ac as interproAc, x.display_label as displayLabel, x.description as description " +
				"FROM homo_sapiens_core_55_37.translation t, homo_sapiens_core_55_37.transcript_stable_id trsi, homo_sapiens_core_55_37.protein_feature p, homo_sapiens_core_55_37.translation_stable_id tsi, homo_sapiens_core_55_37.analysis a, homo_sapiens_core_55_37.interpro i, homo_sapiens_core_55_37.xref x, homo_sapiens_core_55_37.gene_stable_id gsi, homo_sapiens_core_55_37.transcript tr " +
				"WHERE p.translation_id=tsi.translation_id AND p.hit_name != 'Seg'AND p.hit_name!= 'ncoils' " +
				"AND p.analysis_id=a.analysis_id AND i.id=p.hit_name AND x.dbprimary_acc=i.interpro_ac " +
				"AND p.translation_id=t.translation_id AND trsi.transcript_id=t.transcript_id AND gsi.gene_id=tr.gene_id " + 
				"AND tr.transcript_id=t.transcript_id;")
				  .addScalar("geneStableId")
				  .addScalar("transcriptStableId")
				  .addScalar("translationStableId")
				  .addScalar("hitName")
				  .addScalar("start")
				  .addScalar("end")
				  .addScalar("logicName")
				  .addScalar("interproAc")
				  .addScalar("displayLabel")
				  .addScalar("description")
				  .setResultTransformer( Transformers.aliasToBean(EnsemblDomainDTO.class))
				  .list();
		
		for (EnsemblDomainDTO edd : domains) {
			List<EnsemblDomainDTO> tmpList = ensemblDomainList.get(edd.getTranscriptStableId());
			if (null == tmpList) {
				tmpList = new ArrayList<EnsemblDomainDTO>();
				ensemblDomainList.put(edd.getTranscriptStableId(), tmpList);
			}
			tmpList.add(edd);
		}
	}
	
	
	
	public static void annotatePileup(QSnpRecord record, Map<String, String> ensembleToQCMG, Map<String, GeneSymbolRecord> geneSymbols) {
		
		// first of all, get the ensemble chromosome from the map
		String ensemblChr = null;
		for (Map.Entry<String, String> entry : ensembleToQCMG.entrySet()) {
			if (record.getChromosome().equals(entry.getValue())) {
				ensemblChr = entry.getKey();
				break;
			}
		}
		
		if (null == ensemblChr) {
			logger.info("Could not find ensemble chromosome corresponding to " + record.getChromosome());
			return;
		}
		
		// now get the SeqRegion object of interest
		SeqRegion currentChromosome = null;
		for (SeqRegion sr : seqRegions) {
			if (sr.getName().equals(ensemblChr)) {
				currentChromosome = sr;
				break;
			}
		}
		
		if (null == currentChromosome) {
			logger.info("Could not find SeqRegion corresponding to " + ensemblChr);
			return;
		}
		
		List<VariationFeature> features = factory.getVariationFeatureDAO().findBySeqRegionPileup(currentChromosome, record);
		
		
		
		for (VariationFeature vf : features) {
			logger.info("VariationFeature retireved from ensembl: " + vf.getFormattedDisplay());
			
			//
			updatePileupWithVariantFeature(record, vf, geneSymbols);
		}
	}
	
	public static List<EnsemblDomainDTO>getDomains(String transcipt, int position) {
		
		List<EnsemblDomainDTO> tmpResults = ensemblDomainList.get(transcipt);
		if (null != tmpResults) {
			List<EnsemblDomainDTO> results = new ArrayList<EnsemblDomainDTO>();
			for (EnsemblDomainDTO edd : tmpResults) {
				if (edd.getStart() <= position && edd.getEnd() >= position) {
					results.add(edd);
				}
			}
			return results;
		}
		return Collections.emptyList();
	}
	
	
	public static void updatePileupWithVariantFeature(QSnpRecord record, VariationFeature feature, Map<String, GeneSymbolRecord> geneSymbols) {
			
			// get the translationVariants
			Set<TranscriptVariation> variations = feature.getTranscriptVariations();
//				logger.info("more than 1 variation found for feature");
				for (TranscriptVariation variation : variations) {
					String type = variation.getConsequenceType();
					String gene = "-888";
					String aminoAcidChange = "-888";
					String cdsChange = "-888";
					String transcript = null;
					int position = null != variation.getTranslationStart() ? variation.getTranslationStart().intValue() : 0;
					int cdnaStart = null != variation.getStart() ? variation.getStart() : -1;
					String peptideAllele = variation.getPeptideAllele();
					
//					logger.info("transcriptVariation: " + variation.getFormattedString());
					
					// get transcription_stable_id.stable_id
					TranscriptStable ts = variation.getTranscriptStable();
					transcript = ts.getStableId();
//					TranscriptStable ts = getTranscriptStable(variation.getTranscript());
//					transcript = ts.getStableId();
					
					// get gene_stable_id.stable_id
//					GeneStableId geneStableId = getGeneStable(variation.getTranscript().getGene());
//					gene = geneStableId.getStableId();
//					String geneSymbol = geneSymbols.get(gene);
					
					if (null != peptideAllele) {
						if (peptideAllele.contains("/")) {
							String[] params = peptideAllele.split("/");
							aminoAcidChange = params[0] + position + params[1];
						} else if (peptideAllele.matches("[A-Z]")){
							aminoAcidChange = peptideAllele + position + peptideAllele;
						}
					}
					if ( -1 != cdnaStart) {
						String[] params = feature.getAllele().split("/");
						if (params.length < 2) logger.error("Not enough params after splitting of the allele: " + feature.getAllele());
						else cdsChange = cdnaStart + params[0] + ">" + params[1]; 
					} else if (type.contains("INTERGENIC")) {
						transcript = "-888";
					}
					
					String geneSymbol = null;
					GeneSymbolRecord geneSymbolRecord = geneSymbols.get(transcript);
					if (null != geneSymbolRecord) {
						gene = geneSymbolRecord.getGeneId();
						geneSymbol = geneSymbolRecord.getSymbol();
					}
					
					logger.info("aminoAcidChange: " + aminoAcidChange + ", cdsChange: " + cdsChange + ", type: " 
							+ type + ", transcript: " + transcript + " geneStableId: " + gene);
					// set values
					record.addAminoAcidChange(aminoAcidChange);
					record.addCdsChange(cdsChange);
					record.addConsequenceType(type);
					record.addTranscript(transcript);
					record.addGene(gene);
					record.addGeneSymbol(geneSymbol);
					
					if ( ! "-888".equals(aminoAcidChange)) {
						List<EnsemblDomainDTO> domains = getDomains(transcript, position);
						for (EnsemblDomainDTO domain : domains) {
							
							if ("Pfam".equals(domain.getLogicName()))
								record.addDccDomain(domain.getHitName());
							
							record.addGrimmondDomain(domain.getHitName());
							record.addGrimmondDescription(domain.getDisplayLabel());
							record.addGrimmondType(domain.getLogicName());
						}
						
					} else {
						record.addDccDomain("-888");
						record.addGrimmondDomain("-888");
						record.addGrimmondDescription("-888");
						record.addGrimmondType("-888");
					}
				}
				
			}
//		}
//	}
	

}
