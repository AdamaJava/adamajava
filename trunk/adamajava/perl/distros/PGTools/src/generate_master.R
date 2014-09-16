

require( plyr )

hprd_ids <- read.delim( 
  'HPRD_ID_MAPPINGS.txt', 
  header=F, 
  col.names=c( 'hprd_id', 'geneSymbol', 'nucleotide_accession', 'protein_accession','entrezgene_id','omim_id','swissprot_id', 'main_name' ), 
  colClasses="character"
) 

genetic_diseases <- read.delim( 
  'GENETIC_DISEASES.txt', 
  header=F, 
  col.names=c( 'hprd_id', 'geneSymbol', 'refseq_id', 'disease_name', 'reference_id' ),
  colClasses="character"
)

tissue_expressions <- read.delim(
  'TISSUE_EXPRESSIONS.txt',
  header=F,
  col.names=c( 'hprd_id','refseq_id','geneSymbol','expression_term','status','reference_id' ),
  colClasses="character"
)

protien_architecture <- read.delim(
  'PROTEIN_ARCHITECTURE.txt',
  header=F,
  col.names=c( 'hprd_id','isoform_id','refseq_id','geneSymbol','architecture_name','architecture_type','start_site','end_site','reference_type','reference_id' ),
  colClasses="character"
)

gene_ontology <- read.delim(
  'GENE_ONTOLOGY.txt',
  header=F,
  col.names=c( 'hprd_id','isoform_id','refseq_id','geneSymbol','isoform_specifity_status','molecular_function_term','molecular_function_reference_id','biological_process_term','biological_process_reference_id','cellular_component_term','cellular_component_reference_id' ),
  colClasses="character"
)

protein_interactions <- read.delim(
  'BINARY_PROTEIN_PROTEIN_INTERACTIONS.txt',
  header=F,
  col.names=c( 'geneSymbol','hprd_id','interactor_1_refseq_id','geneSymbol_2','hprd_id_2','interactor_2_refseq_id','experiment_type','reference_id' ),
  colClasses="character"
)

non_protein_interactions <- read.delim(
  'BINARY_PROTEIN_NONPROTEIN_INTERACTIONS.txt',
  header=F,
  col.names=c( 'geneSymbol', 'hprd_id', 'interactor_refseq_id', 'non_protein_interactor_name','experiment_type','reference_id' ),
  colClasses="character"
)

post_translation <- read.delim(
  'POST_TRANSLATIONAL_MODIFICATIONS.txt',
  header=F,
  col.names=c( 'hprd_id','geneSymbol','substrate_isoform_id','substrate_refseq_id','site','residue','enzyme_name','enzyme_hprd_id','modification_type','experiment_type','reference_id' ),
  colClasses="character"
)

ensg_ids <- read.csv( 'id_to_ensg.csv' )

common_cols = c( 'geneSymbol.x', 'nucleotide_accession', 'protein_accession', 'swissprot_id', 'entrezgene_id' )


remove_common_cols <- function( d ) {
  for( i in common_cols ) {
    d[[ i ]] <- NULL
  }

  return( d )
}

ensgs <- ddply(
  merge( hprd_ids, ensg_ids, by='entrezgene_id', all.x=T ),
  c('hprd_id', 'geneSymbol', 'nucleotide_accession', 'protein_accession', 'swissprot_id', 'entrezgene_id' ),
  function( x ) data.frame(
    ensg_id=paste( x$ensg_id, collapse=';' )
  )
)

genetic_diseases <- ddply( 
  merge( hprd_ids, genetic_diseases, by='hprd_id', all.x=T ), 
  c('hprd_id', 'geneSymbol.x', 'nucleotide_accession', 'protein_accession', 'swissprot_id', 'entrezgene_id' ),
  function( x ) data.frame( disease_name=paste( x$disease_name, collapse="\n\n" ) )
)

tissue_expressions <- remove_common_cols( ddply(
  merge( hprd_ids, tissue_expressions, by='hprd_id', all.x=T ),
  c('hprd_id', 'geneSymbol.x', 'nucleotide_accession', 'protein_accession', 'swissprot_id', 'entrezgene_id' ),
  function( x ) data.frame( expression_term=paste( x$expression_term, collapse="\n\n" ) )
) )

protien_architecture <- remove_common_cols( ddply(
  merge( hprd_ids, protien_architecture, by='hprd_id', all.x=T ),
  c('hprd_id', 'geneSymbol.x', 'nucleotide_accession', 'protein_accession', 'swissprot_id', 'entrezgene_id' ),
  function( x ) data.frame( architecture_name=paste( x$architecture_name, collapse="\n\n" ), architecture_type=paste( x$architecture_type, collapse="\n\n" ) )
) )

gene_ontology <- remove_common_cols( ddply(
  merge( hprd_ids, gene_ontology, by='hprd_id', all.x=T ),
  c('hprd_id', 'geneSymbol.x', 'nucleotide_accession', 'protein_accession', 'swissprot_id', 'entrezgene_id' ),
  function( x ) data.frame( 
    molecular_function_term=paste( x$molecular_function_term, collapse='\n\n' ), 
    biological_process_term=paste( x$biological_process_term, collapse='\n\n' ), 
    cellular_component_term=paste( x$cellular_component_term, collapse='\n\n' ) 
  )
) )

protien_interactions <- remove_common_cols( ddply(
  merge( hprd_ids, protein_interactions, by='hprd_id', all.x=T ),
  c('hprd_id', 'geneSymbol.x', 'nucleotide_accession', 'protein_accession', 'swissprot_id', 'entrezgene_id'  ),
  function( x ) data.frame( 
    geneSymbol_2=paste( x$geneSymbol_2, collapse="\n\n" ), 
    hprd_id_2=paste( x$hprd_id_2, collapse="\n\n" ) 
  )
) )

non_protein_interactions <- remove_common_cols( ddply(
  merge( hprd_ids, non_protein_interactions, by='hprd_id', all.x=T ),
  c('hprd_id', 'geneSymbol.x', 'nucleotide_accession', 'protein_accession', 'swissprot_id', 'entrezgene_id' ),
  function( x ) data.frame( protein_interactor_name=paste( x$non_protein_interactor_name, collapse='\n\n' ) )
) )


post_translation <- remove_common_cols( ddply(
  merge( hprd_ids, post_translation, by='hprd_id', all.x=T ),
  c('hprd_id', 'geneSymbol.x', 'nucleotide_accession', 'protein_accession', 'swissprot_id', 'entrezgene_id' ),
  function( x ) data.frame(
    site=paste( x$site, collapse='\n\n' ),
    residue=paste( x$residue, collapse='\n\n' ),
    enzyme_name=paste( x$enzyme_name, collapse='\n\n' ),
    enzyme_hprd_id=paste( x$enzyme_hprd_id, collapse='\n\n' ),
    modification_type=paste( x$modification_type, collapse='\n\n' ) 
  )
) )


output = merge( genetic_diseases, tissue_expressions, by='hprd_id', all.x=T )
output = merge( output, protien_architecture, by='hprd_id', all.x=T )
output = merge( output, gene_ontology, by='hprd_id', all.x=T )
output = merge( output, protien_interactions, by='hprd_id', all.x=T )
output = merge( output, non_protein_interactions, by='hprd_id', all.x=T )
output = merge( output, post_translation, by='hprd_id', all.x=T )
output = merge( output, ensgs, by='hprd_id', all.x=T )


write.csv( output, file='master.csv' )



