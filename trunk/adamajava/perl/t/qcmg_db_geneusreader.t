###########################################################################
#
#  File:     qcmg_db_geneusreader.t
#  Creator:  Matthew Anderson
#  Created:  2012-08-24
#
#	Test file for module QCMG::DB:GeneusReader.pm 
#
#  $Id
#
###########################################################################

use Test::More tests =>93;

BEGIN { 
	use_ok( 'QCMG::DB::GeneusReader' ) ;
};

my $geneus = QCMG::DB::GeneusReader->new();
isnt( $geneus, undef, 'Creation of class' );

# Valid exmple querry values.
$sample_1				= "ICGC-ABMJ-20110302-15-TD";
$sample_2				= "ICGC-DBLG-20100225-12-TD";
$APGI_donor_1 			= "APGI_1992";
$APGI_donor_2 			= "APGI_2058";
$SMGRES_donor_1 		= "SLJS_Q679";
$SMGRES_donor_2 		= "GEMM_0204";
$primary_library_1		= "Library_20110617_R";
$primary_library_2		= "Library_20120216_K";
$mapset_LMP_SOLID		= "S0433_20120424_2_LMP.nopd.nobc";
$mapset_LMP_XL5500		= "S88006_20120503_2_LMP.lane_5.nobc";
$mapset_NOBC_TORRENT	= "T00001_20120109_102.nopd.nobc";
$mapset_NOBC_HISEQ		= "120529_SN7001243_0092_AD1321ACXX.lane_1.nobc";
$mapset_FRAG_SOLID		= "S0417_20110818_2_FragPEBC.nopd.bcB16_13";
$mapset_FRAG_XL5500		= "S17002_20120323_2_FragPEBC.lane_2.bcB16_05";
$mapset_FRAG_TORRENT	= "T00001_20120725_247.nopd.IonXpress_005";
$mapset_FRAG_HISEQ		= "120712_SN7001243_0096_AC0VM0ACXX.lane_6.ACAGTG";
$slide_NOBC_SOLID		= "S0433_20100415_1_Frag";
$slide_LMP_XL5500		= "S17001_20120525_1_LMP";
$slide_NOBC_TORRENT		= "T00001_20120107_97";
$slide_NOBC_HISEQ		= "120529_SN7001243_0092_AD1321ACXX";
$slide_FRAG_SOLID		= "S0428_20120626_2_FragPEBC";
$slide_FRAG_XL5500		= "S88006_20111024_1_FragPEBC";
$slide_FRAG_TORRENT		= "T00001_20120621_216";
$slide_FRAG_HISEQ		= "120718_SN7001238_0067_AD12UNACXX";

#
# resource_metadata()
# If resource is NOT found 0 is returned, otherwise 1 if successfull
#

## The following should NOT be successful
is( $geneus->resource_metadata("sample", 		"invalid value" ) 		, 0 , "Metadata should not found for sample " );
is( $geneus->resource_metadata("donor", 		"invalid value" ) 		, 0 , "Metadata should not found for donor " );
is( $geneus->resource_metadata("primary_library", "invalid value") 		, 0 , "Metadata should not found for primary_library " );
is( $geneus->resource_metadata("mapset", 		"invalid value"	) 		, 0 , "Metadata should not found for mapset " );
is( $geneus->resource_metadata("slide", 		"invalid value"	) 		, 0 , "Metadata should not found for slide " );
is( $geneus->resource_metadata("invalid value", $mapset_LMP_SOLID ) 	, 0 , "Metadata should not found for invalid type " );

## The following should be successful 
isnt( $geneus->resource_metadata("sample", $sample_1)		, 0 , "Metadata found for sample $sample_1" );
isnt( $geneus->resource_metadata("sample", $sample_2)		, 0 , "Metadata found for sample $sample_2" );

isnt( $geneus->resource_metadata("donor", $APGI_donor_1)	, 0 , "Metadata found for donor $APGI_donor_1" );
isnt( $geneus->resource_metadata("donor", $APGI_donor_2)	, 0 , "Metadata found for donor $APGI_donor_2" );
isnt( $geneus->resource_metadata("donor", $SMGRES_donor_1) 	, 0 , "Metadata found for donor $SMGRES_donor_1" );
isnt( $geneus->resource_metadata("donor", $SMGRES_donor_2) 	, 0 , "Metadata found for donor $SMGRES_donor_2" );
  
isnt( $geneus->resource_metadata("primary_library", $primary_library_1 )	, 0 , "Metadata found for primary_library $primary_library_1 " );
isnt( $geneus->resource_metadata("primary_library", $primary_library_2 )	, 0 , "Metadata found for primary_library $primary_library_2 " );

isnt( $geneus->resource_metadata("mapset", $mapset_LMP_SOLID	) , 0 , "Metadata found for mapset $mapset_LMP_SOLID " );
isnt( $geneus->resource_metadata("mapset", $mapset_LMP_XL5500	) , 0 , "Metadata found for mapset $mapset_LMP_XL5500 " );
isnt( $geneus->resource_metadata("mapset", $mapset_NOBC_TORRENT	) , 0 , "Metadata found for mapset $mapset_NOBC_TORRENT " );
isnt( $geneus->resource_metadata("mapset", $mapset_NOBC_HISEQ	) , 0 , "Metadata found for mapset $mapset_NOBC_HISEQ " );
isnt( $geneus->resource_metadata("mapset", $mapset_FRAG_SOLID	) , 0 , "Metadata found for mapset $mapset_FRAG_SOLID " );
isnt( $geneus->resource_metadata("mapset", $mapset_FRAG_XL5500	) , 0 , "Metadata found for mapset $mapset_FRAG_XL5500 " );
isnt( $geneus->resource_metadata("mapset", $mapset_FRAG_TORRENT	) , 0 , "Metadata found for mapset $mapset_FRAG_TORRENT	" );
isnt( $geneus->resource_metadata("mapset", $mapset_FRAG_HISEQ	) , 0 , "Metadata found for mapset $mapset_FRAG_HISEQ " );
 
isnt( $geneus->resource_metadata("slide",	$slide_NOBC_SOLID	) , 0 , "Metadata found for slide $slide_NOBC_SOLID " );
isnt( $geneus->resource_metadata("slide",	$slide_LMP_XL5500	) , 0 , "Metadata found for slide $slide_LMP_XL5500 " );
isnt( $geneus->resource_metadata("slide",	$slide_NOBC_TORRENT	) , 0 , "Metadata found for slide $slide_NOBC_TORRENT " );
isnt( $geneus->resource_metadata("slide",	$slide_NOBC_HISEQ	) , 0 , "Metadata found for slide $slide_NOBC_HISEQ " );
isnt( $geneus->resource_metadata("slide",	$slide_FRAG_SOLID	) , 0 , "Metadata found for slide $slide_FRAG_SOLID	" );
isnt( $geneus->resource_metadata("slide",	$slide_FRAG_XL5500	) , 0 , "Metadata found for slide $slide_FRAG_XL5500 " );
isnt( $geneus->resource_metadata("slide",	$slide_FRAG_TORRENT	) , 0 , "Metadata found for slide $slide_FRAG_TORRENT " );
isnt( $geneus->resource_metadata("slide",	$slide_FRAG_HISEQ	) , 0 , "Metadata found for slide $slide_FRAG_HISEQ	" );

##
## Testing that the required fields are returned in the svn array of hashes. 
##

$geneus->resource_metadata("slide",	$slide_NOBC_SOLID);
my $metadata = $geneus->fetch_metadata();
isnt( $metadata , undef, 'Metadata returned');
$resource = shift @$metadata; # first row
ok( exists $resource->{'Project'},    				'Project' );                   
ok( exists $resource->{'Donor'},    				'Donor' );                     
ok( exists $resource->{'Donor LIMS ID'},    		'Donor LIMS ID' );             
ok( exists $resource->{'Sample'},    				'Sample' );                    
ok( exists $resource->{'Sample LIMS ID'},    		'Sample LIMS ID' );            
ok( exists $resource->{'Sample Code'},    			'Sample Code' );               
ok( exists $resource->{'Material'},    				'Material' );                  
ok( exists $resource->{'Mapset'},    				'Mapset' );                    
ok( exists $resource->{'Mapset LIMS ID'},    		'Mapset LIMS ID' );            
ok( exists $resource->{'Primary Library'},    		'Primary Library' ); 
ok( exists $resource->{'Library Type'},    			'Library Type' );
ok( exists $resource->{'Library Protocol'},			'Library Protocol' );    
ok( exists $resource->{'Capture Kit'},    			'Capture Kit' );               
ok( exists $resource->{'GFF File'},    				'GFF File' );                  
ok( exists $resource->{'Sequencing Type'},    		'Sequencing Type' );
ok( exists $resource->{'Sequencing Platform'},  	'Sequencing Platform' );         
ok( exists $resource->{'Aligner'},    				'Aligner' );                   
ok( exists $resource->{'Species Reference Genome'}, 'Species Reference Genome' );  
ok( exists $resource->{'Reference Genome File'},    'Reference Genome File' );		
ok( exists $resource->{'Alignment Required'},   	'Alignment Required'); 		
ok( exists $resource->{'Failed QC'},    			'Failed QC' );                 
ok( exists $resource->{'isize Min'},    			'isize Min' );					
ok( exists $resource->{'isize Max'},   				'isize Max' ); 				
ok( exists $resource->{'isize Manually Reviewed'},	'isize Manually Reviewed' );
ok( exists $resource->{'Expression Array'},			'Expression Array' );
ok( exists $resource->{'SNP Array'},				'SNP Array' );
ok( exists $resource->{'Methylation Array'},		'Methylation Array' );


# Testing all metadata
$geneus->all_resources_metadata();
my $metadata = $geneus->fetch_metadata();
isnt( $metadata , undef, 'Metadata returned');
$resource = shift @$metadata; # first row
ok( exists $resource->{'Project'},    				'Project' );                   
ok( exists $resource->{'Donor'},    				'Donor' );                     
ok( exists $resource->{'Donor LIMS ID'},    		'Donor LIMS ID' );             
ok( exists $resource->{'Sample'},    				'Sample' );                    
ok( exists $resource->{'Sample LIMS ID'},    		'Sample LIMS ID' );            
ok( exists $resource->{'Sample Code'},    			'Sample Code' );               
ok( exists $resource->{'Material'},    				'Material' );                  
ok( exists $resource->{'Mapset'},    				'Mapset' );                    
ok( exists $resource->{'Mapset LIMS ID'},    		'Mapset LIMS ID' );            
ok( exists $resource->{'Primary Library'},    		'Primary Library' ); 
ok( exists $resource->{'Library Type'},    			'Library Type' );
ok( exists $resource->{'Library Protocol'},			'Library Protocol' );    
ok( exists $resource->{'Capture Kit'},    			'Capture Kit' );               
ok( exists $resource->{'GFF File'},    				'GFF File' );                  
ok( exists $resource->{'Sequencing Type'},    		'Sequencing Type' );
ok( exists $resource->{'Sequencing Platform'},  	'Sequencing Platform' );         
ok( exists $resource->{'Aligner'},    				'Aligner' );                   
ok( exists $resource->{'Species Reference Genome'}, 'Species Reference Genome' );  
ok( exists $resource->{'Reference Genome File'},    'Reference Genome File' );		
ok( exists $resource->{'Alignment Required'},   	'Alignment Required'); 		
ok( exists $resource->{'Failed QC'},    			'Failed QC' );                 
ok( exists $resource->{'isize Min'},    			'isize Min' );					
ok( exists $resource->{'isize Max'},   				'isize Max' ); 				
ok( exists $resource->{'isize Manually Reviewed'},	'isize Manually Reviewed' );
ok( exists $resource->{'Expression Array'},			'Expression Array' );
ok( exists $resource->{'SNP Array'},				'SNP Array' );
ok( exists $resource->{'Methylation Array'},		'Methylation Array' );


# Testing SOLiD Primaries
my $metadata = $geneus->solid_primaries($slide_NOBC_SOLID);
$resource = shift @$metadata; # first row
ok( exists $resource->{'slide_name'},			'slide_name' );
ok( exists $resource->{'f3_primary_reads'},		'f3_primary_reads' );
ok( exists $resource->{'f5_primary_reads'},		'f5_primary_reads' );
ok( exists $resource->{'r3_primary_reads'},		'r3_primary_reads' );
ok( exists $resource->{'bc_primary_reads'},		'bc_primary_reads' );

