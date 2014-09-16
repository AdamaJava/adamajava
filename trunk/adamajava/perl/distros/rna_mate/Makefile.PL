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
    NAME         => 'rna_mate',
    VERSION      => $version,
    ABSTRACT     => 'SOLiD transcriptome alignment',
    AUTHOR       => 'QCMG Bioinformatics Group',
    EXE_FILES    => [ 'src/rna_mate.pl',
                      'src/chr_wig.pl',
                      'src/chr_start.pl',
                      'src/chr_rescueSOLiD.py' ],
    PREREQ_PM    => { 'Parallel::ForkManager' => 0 },
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
