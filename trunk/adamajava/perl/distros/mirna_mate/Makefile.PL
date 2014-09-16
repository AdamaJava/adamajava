###########################################################################
#
#  Script:   Makefile.PL
#  Author:   John V Pearson
#  Created:  2010-04-08
#
#  This file uses ExtUtils::MakeMaker to create a sensible Makefile
#  for the ngstools project.  For more details on how to influence the
#  contents of the Makefile that is written, read the POD (perldoc) for 
#  ExtUtils::MakeMaker.
#
#  $Id: Makefile.PL,v 1.1 2008/05/20 06:38:38 jpearson Exp $
#
###########################################################################

use ExtUtils::MakeMaker;

display_license();

my $version = '$Revision: 1.5 $ ' =~ /\$Revision:\s+([^\s]+)/;

WriteMakefile(
    NAME         => 'mirna_mate',
    VERSION      => $version,
    ABSTRACT     => 'SOLiD transcriptome alignment',
    AUTHOR       => 'QCMG Bioinformatics Group',
    EXE_FILES    => [	'src/mirna_mate.pl',
			'src/editing.pl',
			'src/f2m.pl',
			'src/mask_schemas_mapreads.pl',
			'src/library_construction/00_filter_species.pl',
			'src/library_construction/01_make_one_line_fasta.pl',
			'src/library_construction/02_change_U_to_T_and_remove_description.pl',
			'src/library_construction/03_pad_mature_miRNAs.pl',
			'src/library_construction/04_make_index.pl',
			'src/library_construction/05_concatenate_sequences.pl',
			'src/library_construction/06_split_by_length.pl',
			'src/library_construction/07_convert_to_colourspace_with_initial_T.pl',
			'src/library_construction/08_match_miRs.pl',
			'src/library_construction/09_make_miRNA_clusters.pl',
			'src/library_construction/10_create_new_fasta.pl',
			'src/library_construction/11_final_cluster_by_family.pl',
			'src/library_construction/12_reannotate_index_file.pl'
		],
    PREREQ_PM    => { 'Parallel::ForkManager' => 0.7.5, 'Path::Class'=>0.16 },
);

sub display_license {

    print <<_EOL_

This software is copyright 2010 by the Queensland Centre for Medical
Genomics. All rights reserved.  This License is limited to, and you
may use the Software solely for, your own internal and non-commercial
use for academic and research purposes. Without limiting the foregoing,
you may not use the Software as part of, or in any way in connection with 
the production, marketing, sale or support of any commercial product or
service or for any governmental purposes.  For commercial or governmental 
use, please contact licensing\@qcmg.org.

In any work or product derived from the use of this Software, proper 
attribution of the authors as the source of the software or data must be 
made.  The following URL should be cited:

  http://bioinformatics.qcmg.org/software/


_EOL_
}
