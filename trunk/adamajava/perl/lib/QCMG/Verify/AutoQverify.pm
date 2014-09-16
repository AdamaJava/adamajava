package QCMG::Verify::AutoQverify;

###########################################################################
#
#  Module:   QCMG::Verify::AutoQverify
#  Creator:  John V Pearson
#  Created:  2013-08-01
#
#  This a factory class for running qverify against a directory that
#  contains variant calls in MAF format and where those MAFs have
#  been processed through qbasepileup.
#  
#  The run method of this class takes the absolute pathname to a MAF file
#  sitting within QCMG's /mnt/seq_results directory structure and, based 
#  on the pathname, collects the information required to appropriately call
#  the java qbasepileup tool, runs qbasepileup, and puts the output and
#  log files back into the same directory as the MAF file.
#
#  $Id: AutoQbasepileup.pm 4151 2013-07-29 06:06:23Z j.pearson $
#
###########################################################################

use strict;
use warnings;

use Carp qw( croak carp confess );
use Data::Dumper;
use IO::File;

use QCMG::FileDir::Finder;
use QCMG::FileDir::QSnpDirRecord;
use QCMG::FileDir::GatkDirRecord;
use QCMG::Run::Qbasepileup;
use QCMG::Util::QLog;
use QCMG::Verify::AutoNames;
use QCMG::QBamMaker::SeqFinalDirectory;

use vars qw( $SVNID $REVISION %BAM_CATEGORY %LIMITS );

( $REVISION ) = '$Revision: 4151 $ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id: AutoQbasepileup.pm 4151 2013-07-29 06:06:23Z j.pearson $'
    =~ /\$Id:\s+(.*)\s+/;


sub new {
    my $class = shift;
    my %params = @_;

    # Note that (unusually) we have set the default verbose level to 1
    my $self = { verbose => ($params{verbose} ? $params{verbose} : 1) };

    bless $self, $class;
}


sub run {
    my $self = shift;
    my $maf  = shift;

    # Parse info out of the MAF pathname
    my $names = QCMG::Verify::AutoNames->new( verbose => $self->verbose );
    my $rh_mafinfo = $names->params_from_maf( $maf );

    # We need to know the index BAMs so we are going to have to process
    # the variant directory to get our hands on the appropriate log
    # files etc.

    if ($rh_mafinfo->{tool} eq 'qSNP') {
        # Parse the whole dir so we are ready to rock
        my $dir = QCMG::FileDir::QSnpDirRecord->new(
                      dir     => $rh_mafinfo->{variant_dir},
                      verbose => $self->verbose );
        my $rh_dirinfo = $dir->completion_report;

        #print Dumper $rh_mafinfo;
        $self->_run_qverify( $rh_dirinfo, $rh_mafinfo );
    }
    elsif ($rh_mafinfo->{tool} eq 'GATK') {
        # Parse the whole dir so we are ready to rock
        my $dir = QCMG::FileDir::GatkDirRecord->new(
                      dir     => $rh_mafinfo->{variant_dir},
                      verbose => $self->verbose );
        my $rh_dirinfo = $dir->completion_report;

        #print Dumper $rh_mafinfo;
        $self->_run_qverify( $rh_dirinfo, $rh_mafinfo );
    }
    else {
        die 'Cannot call qverify on variant calls from ',
            $rh_mafinfo->{tool},"\n";
    }
}


sub _run_qverify {
    my $self       = shift;
    my $rh_dirinfo = shift;
    my $rh_mafinfo = shift;

    # Get the run parameters set up 
    my %params = ();
    $params{maf}         = $rh_mafinfo->{variant_dir} .'/'. $rh_mafinfo->{maf_file};
    $params{indexbams}   = [ $rh_dirinfo->{qsnplog_normal_bam},
                             $rh_dirinfo->{qsnplog_tumour_bam} ];
    $params{qbasepileup} = $rh_mafinfo->{variant_dir} .'/'. $rh_mafinfo->{qbp_outfile};
    $params{bamlist}     = $rh_mafinfo->{variant_dir} .'/'. $rh_mafinfo->{bamlist_file};
    my $maf_verif        = $rh_mafinfo->{variant_dir} .'/'. $rh_mafinfo->{maf_verif_file};
    $params{outfile}     = $maf_verif . '.report';
    $params{logfile}     = $maf_verif . '.log';
    $params{verbose}     = $self->verbose;

    my $vf = QCMG::Verify::QVerify->new( %params );
    $vf->write_verified_maf( $maf_verif );
}


sub verbose {
    my $self = shift;
    return $self->{verbose} = shift if @_;
    return $self->{verbose};
}


1;

__END__

=head1 NAME

QCMG::Verify::AutoQverify - run qverify on a given MAF file


=head1 SYNOPSIS

 use QCMG::Verify::AutoQverify;


=head1 ABSTRACT

This a factory class for running qverify against MAF files in an
automated fashion.
The run method of this class takes the absolute pathname to a MAF file
sitting within QCMG's /mnt/seq_results directory structure and based on 
the pathname, it collects all the information required to appropriately 
call the java qbasepileup tool and then runs qbasepileup, putting the 
results back into the same directory as the MAF file.


=head1 DESCRIPTION

=head2 Public Methods

=over

=item B<new()>

 my $fact = QCMG::Verify::AutoQverify->new( verbose => 1 );
            
The new method takes a single optional parameter - verbose.  Because the
logging is automated, the default verbose level has been set to 1 so
that logs are more detailed.

=item B<run>
 
 $fact->run( '/absolute/path/to/my_maf.maf' );

MAF file containing the variant positions to be tested.

=item B<verbose>

Print progress and diagnostic messages.  Higher numbers enable 
higher levels of verbosity.

=back


=head1 AUTHOR

=over 2

=item John Pearson, L<mailto:j.pearson@uq.edu.au>

=back


=head1 VERSION

$Id: AutoQbasepileup.pm 4151 2013-07-29 06:06:23Z j.pearson $


=head1 COPYRIGHT

Copyright (c) The University of Queensland 2013

Permission is hereby granted, free of charge, to any person obtaining a
copy of this software and associated documentation files (the "Software"),
to deal in the Software without restriction, including without limitation
the rights to use, copy, modify, merge, publish, distribute, sublicense,
and/or sell copies of the Software, and to permit persons to whom the
Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included
in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
DEALINGS IN THE SOFTWARE.
