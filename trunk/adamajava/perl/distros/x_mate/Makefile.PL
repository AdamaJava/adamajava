###########################################################################
#
#  Script:   Makefile.PL
#  Author:   Qinying Xu
#  Created:  2010-04-19
#
#  This file uses ExtUtils::MakeMaker to create a sensible Makefile
#  for the ngstools project.  For more details on how to influence the
#  contents of the Makefile that is written, read the POD (perldoc) for 
#  ExtUtils::MakeMaker.
#
#  $Id: Makefile.PL,v 1.1 2010/04/20 06:38:38 Christina Xu Exp $
#
###########################################################################

use ExtUtils::MakeMaker;

display_license();

my $version = '$Revision: 1.5 $ ' =~ /\$Revision:\s+([^\s]+)/;

WriteMakefile(
    NAME         => 'x_mate',
    VERSION      => $version,
    ABSTRACT     => 'SOLiD short read alignment',
    AUTHOR       => 'QCMG Bioinformatics Group',
    EXE_FILES    => [ 'src/XMate.pl',
                      'src/MuMRescueLite.py',
		      'src/assess_junctions_for_directionality.pl',
		      'src/check_matching_stats_XMATE.pl',
		      'src/chrom_sizes.pl',
		      'src/clean_up_XMATE_output_directories.pl',
		      'src/combine_donor_and_acceptor_sequences.pl',
		      'src/concatenate_sequences.pl',
		      'src/create_non_redundant_intron_coordinates.pl',
		      'src/filter_bedGraphs.pl',
		      'src/make_index.pl',
		      'src/restart_from_rescue.pl',
		      'src/write_sam_conversion_config.pl' ],
    PREREQ_PM    => { 'Parallel::ForkManager' => 0,
		      'Object::InsideOut' => 0 },
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

=cut
_EOL_
}


