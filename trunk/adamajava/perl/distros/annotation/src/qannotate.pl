#!/usr/bin/perl -w

##############################################################################
#
#  Program:  qannotate.pl
#  Author:   John V Pearson
#  Created:  2012-11-01
#
#  A perl program to do annotation of variants using the locally installed
#  version of the EnsEMBL databases and perl API plus whateer other fles
#  turn out to be necessary.
#
#  $Id: qannotate.pl 4667 2014-07-24 10:09:43Z j.pearson $
#
##############################################################################

use strict;
use warnings;

use Carp qw( carp croak confess );
use Clone qw( clone );
use Data::Dumper;
use Getopt::Long;
use IO::File;
use Pod::Usage;

use QCMG::Annotate::Pindel;
use QCMG::Annotate::Strelka;
use QCMG::Annotate::Qsnp;
use QCMG::Annotate::Qsnp2;
use QCMG::Annotate::SmallIndelTool;
use QCMG::Annotate::Gff;
use QCMG::Annotate::GATKindel;
use QCMG::Annotate::QsnpCompoundMutations;
use QCMG::Annotate::Util qw( load_ensembl_API_modules
                             initialise_dcc2_file
                             transcript_to_domain_lookup 
                             transcript_to_geneid_and_symbol_lookups
                             dcc2rec_from_dcc1rec
                             dccqrec_from_dcc1rec 
                             annotation_defaults
                             read_and_filter_dcc1_records
                             mutation_snp
                             mutation_indel );
use QCMG::IO::DccSnpReader;
use QCMG::IO::DccSnpWriter;
use QCMG::IO::EnsemblDomainsReader;
use QCMG::IO::EnsemblTranscriptMapReader;
use QCMG::Util::QLog;

use vars qw( $SVNID $REVISION $CMDLINE );

( $REVISION ) = '$Revision: 4667 $ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id: qannotate.pl 4667 2014-07-24 10:09:43Z j.pearson $'
    =~ /\$Id:\s+(.*)\s+/;

# Setup global data structures


###########################################################################
#
# "Cry havoc, and let slip the dogs of war ..."
#

MAIN: {

    # Print usage message if no arguments supplied
    pod2usage( -exitval  => 0,
               -verbose  => 99,
               -sections => 'SYNOPSIS|COMMANDS' )
        unless (scalar @ARGV > 0);

    # This is a list of global defaults.  Every instance of a
    # QCMG::Annotate::* object will get passed these in the constructor
    # so they can be set once for the whole qannotate.pl script but
    # individual modules can override or ignore as required.
 
    my %globals = ( ensver   => '70',
                    organism => 'human',
                    repeats  => '/panfs/share/repeatmasker/hg19.repeatmasked.indel_filtering.gff3',
                    release  => '14',
                    verbose  => 0 );

    $CMDLINE = join(' ',@ARGV);
    my $mode = shift @ARGV;

    # We call this the "Grand Central Station" model - the main point of
    # the MAIN{} block is just to pick which of the mode-specific subs
    # will be called.  Each of the subs is responsible for doing its own
    # command line processing and logging.  Increasingly, we are trying
    # to encapsulate the logic for each mode in a separate module.

    my @valid_modes = qw( help man version qsnp qsnpold smallindeltool
                          pindel gff qsnpcompoundmutations strelka );
            
    if ($mode =~ /^help$/i or $mode =~ /\?/) {
        pod2usage( -exitval  => 0,
                   -verbose  => 99,
                   -sections => 'SYNOPSIS|COMMANDS' );
                
    }           
    elsif ($mode =~ /^man$/i) {
        pod2usage(-exitstatus => 0, -verbose => 2)
    }       
    elsif ($mode =~ /^version$/i) {
        print "$SVNID\n";
    }
    elsif ($mode =~ /^qsnp$/i) {
        my $annotator = QCMG::Annotate::Qsnp2->new( %globals );
        $annotator->execute;
    }
    elsif ($mode =~ /^qsnpold$/i) {
        # This is deprecated but left here for legacy code comparisons
        my $annotator = QCMG::Annotate::Qsnp->new( %globals );
        $annotator->execute;
    } 
    elsif ($mode =~ /^smallindeltool$/i) {
        #smallindeltool();
        my $annotator = QCMG::Annotate::SmallIndelTool->new( %globals );
        $annotator->execute;
    } 
    elsif ($mode =~ /^pindel$/i) {
        my $annotator = QCMG::Annotate::Pindel->new( %globals );
        $annotator->execute;
    } 
    elsif ($mode =~ /^strelka$/i) {
        my $annotator = QCMG::Annotate::Strelka->new( %globals );
        $annotator->execute;
    } 
    elsif ($mode =~ /^GATKindel$/i) {
        my $annotator = QCMG::Annotate::GATKindel->new( %globals );
        $annotator->execute;
    } 
    elsif ($mode =~ /^gff$/i) {
        my $annotator = QCMG::Annotate::Gff->new( %globals );
        $annotator->execute;
    }
    elsif ($mode =~ /^qsnpcompoundmutations$/i) {
        my $annotator = QCMG::Annotate::QsnpCompoundMutations->new( %globals );
        $annotator->execute;
    }
    else {
        die "qannotate mode [$mode] is unrecognised; valid modes are: ".
            join(' ',@valid_modes) ."\n";
    }
}



__END__

=head1 NAME

qannotate.pl - Perl script for annotating variants


=head1 SYNOPSIS

 qannotate.pl command [options]


=head1 ABSTRACT

This script will gradually take over all variant annotation tasks at
QCMG.  This will require the rolling in of numerous extant perl and
python scripts. Sic transit gloria mundi.


=head1 COMMANDS

 qsnp                  - process qsnp DCC1 files for mouse/human
 smallindeltool        - process Bioscope small indel tool DCC1 files
 pindel                - process pindel output, create DCC1 file
 strelka               - process strelka VCF files, create DCC1 file
 gatkindel             - process GATK indel VCF files, create DCC1 file
 gff                   - process a GFF file and annotate the DCC1 QCMGflag field with feature info
 qsnpcompoundmutations - process a DCC1 file and collapse adjacent, same-codon SNPs
 version               - print version number and exit immediately
 help                  - display usage summary
 man                   - display full man page


=head1 AUTHORS

=over 2

=item John Pearson, L<mailto:j.pearson@uq.edu.au>

=item Karin Kassahn

=item Lynn Fink, L<mailto:l.fink@uq.edu.au>

=back


=head1 VERSION

$Id: qannotate.pl 4667 2014-07-24 10:09:43Z j.pearson $


=head1 COPYRIGHT

Copyright (c) The University of Queensland 2012,2013

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
