#!/usr/bin/perl -w

##############################################################################
#
#  Program:  bam_vs_ma.pl
#  Author:   John V Pearson
#  Created:  2010-11-01
#
#  Quick-n-dirty script for looking at tags in Bioscope BAMs and
#  comparing to the matches from the corresponding .ma.
#
#  $Id: bam_vs_ma.pl 4669 2014-07-24 10:48:22Z j.pearson $
#
##############################################################################

use strict;
use warnings;

use IO::File;
use Getopt::Long;
use Data::Dumper;
use Pod::Usage;
use Carp qw( carp croak );

use QCMG::IO::SamReader;
use QCMG::IO::MaReader;

use vars qw( $SVNID $REVISION $VERSION $VERBOSE );

( $REVISION ) = '$Revision: 4669 $ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id: bam_vs_ma.pl 4669 2014-07-24 10:48:22Z j.pearson $'
    =~ /\$Id:\s+(.*)\s+/;

###########################################################################
#
# "Cry havoc, and let slip the dogs of war ..."
#

MAIN: {

    # Setup defaults for important variables.

    my $mafile1        = '';
    my $mafile2        = '';
    my $bamfile        = '';
    my $outfile        = '';
    my $samtoolsbin    = 'samtools';
       $VERBOSE        = 0;
       $VERSION        = 0;
    my $help           = 0;
    my $man            = 0;

    # Print usage message if no arguments supplied
    pod2usage(1) unless (scalar @ARGV > 0);

    # Use GetOptions module to parse commandline options

    my $results = GetOptions (
           'i|mafile1=s'          => \$mafile1,       # -i
           'f|mafile2=s'          => \$mafile2,       # -f
           'b|bamfile=s'          => \$bamfile,       # -b
           'o|outfile=s'          => \$outfile,       # -o
           's|samtoolsbin=s'      => \$samtoolsbin,   # -s
           'v|verbose+'           => \$VERBOSE,       # -v
           'version!'             => \$VERSION,       # --version
           'h|help|?'             => \$help,          # -?
           'man|m'                => \$man            # -m
           );

    pod2usage(1) if $help;
    pod2usage(-exitstatus => 0, -verbose => 2) if $man;

    if ($VERSION) {
        print "$SVNID\n";
        exit;
    }

    die 'You must supply a BAM file name' unless $bamfile;

    print( "\nbam_vs_ma.pl  v$REVISION  [" . localtime() . "]\n",
           "   mafile1       $mafile1\n",
           "   mafile2       $mafile2\n",
           "   bamfile       $bamfile\n",
           "   outfile       $outfile\n",
           "   samtoolsbin   $samtoolsbin\n",
           "   verbose       $VERBOSE\n\n" ) if $VERBOSE;

    if ($mafile2) {
        process_pair( $mafile1, $mafile2, $bamfile, $outfile, $samtoolsbin );
    }
    else {
        process_fragment( $mafile1, $bamfile, $outfile, $samtoolsbin );
    }

    print( "[" . localtime() . "] Finished\n" ) if $VERBOSE;
}


sub process_fragment {
    my $mafile      = shift;
    my $bamfile     = shift;
    my $outfile     = shift;
    my $samtoolsbin = shift;

    my $mr = QCMG::IO::MaReader->new( filename => $mafile,
                                      verbose  => $VERBOSE ); 
    my $sr = QCMG::IO::SamReader->new( filename => $bamfile,
                                       verbose  => $VERBOSE ); 

    my $outfh = IO::File->new( $outfile, 'w' )
        or die "Can't open output file [$outfile] for writing: $!";

    my $header = '#' .
                 join("\t",
                            qw( READID MCOUNT MATCHES ),
                            qw( READID RNAME POS MAPQ NH FLAG FLAG_CHAR )
                     ) . "\n";
    $outfh->print( $header );

    my $current_seq = '';
    my $current_pos = 0;
    my $pos_count   = 0;
    my $neg_count   = 0;
    my $ctr         = 0;

    # Read through MA file
    while (my $ma = $mr->next_record) {
        $outfh->print( join("\t", $ma->id, $ma->match_count,
                                  $sr->current_record->qname ) );

        #print Dumper $sam;
        exit if $ctr++ > 100;

        # Check whether current BAM record matches
        if ($ma->id_notag eq $sr->current_record->qname) {
            my $sam = $sr->next_record_as_record;

            $outfh->print( "\t",
                           join("\t", $sam->qname, $sam->rname, $sam->pos,
                                      $sam->mapq, $sam->tag( 'NH' ),
                                      $sam->flag, $sam->flag_as_chars ) );
        }
        $outfh->print( "\n" );
    }
}

sub process_pair {
    my $mafile1     = shift;
    my $mafile2     = shift;
    my $bamfile     = shift;
    my $outfile     = shift;
    my $samtoolsbin = shift;

    my $mr1 = QCMG::IO::MaReader->new( filename => $mafile1,
                                       verbose  => $VERBOSE ); 
    my $mr2 = QCMG::IO::MaReader->new( filename => $mafile2,
                                       verbose  => $VERBOSE ); 
    my $sr = QCMG::IO::SamReader->new( filename => $bamfile,
                                       verbose  => $VERBOSE ); 

    my $outfh = IO::File->new( $outfile, 'w' )
        or die "Can't open output file [$outfile] for writing: $!";

    my $header = '#' .
                 join("\t",
                            qw( MA1_READID MCOUNT MATCHES ),
                            qw( BAM1_READID RNAME POS MAPQ NH SM ZM FLAG FLAG_CHAR ),
                            qw( BAM2_READID RNAME POS MAPQ NH SM ZM FLAG FLAG_CHAR ),
                            qw( MA2_READID MCOUNT MATCHES ),
                     ) . "\n";
    $outfh->print( $header );

    my $current_seq = '';
    my $current_pos = 0;
    my $pos_count   = 0;
    my $neg_count   = 0;
    my $ctr         = 0;
    my $throwaway   = '';

    # Loop until we explicitly decide to exit
    while (1) {

        # Target ID is the smallest of the 2 MA IDs
        my $id = ($mr1->current_record->id ge $mr2->current_record->id)
                 ? $mr1->current_record->id_notag
                 : $mr2->current_record->id_notag;

        print($ctr,
              '   target:', $id,
              '   MA1:', $mr1->current_record->id,
              '   MA2:', $mr2->current_record->id) if ($VERBOSE > 1 );

        # Check if MA1 has a usable record
        if ($mr1->current_record->id_notag eq $id) {
            $outfh->print( join("\t", $mr1->current_record->id,
                                      $mr1->current_record->match_count,
                                      join(',',$mr1->current_record->matches) ) );
            $throwaway = $mr1->next_record;
        }
        else {
            $outfh->print( "\t\t" );
        }

        # Retrieve any sam records that match the ID
        my @sams;
        while ($sr->current_record->qname eq $id) {
            push @sams, $sr->next_record_as_record;
        }

        # Process any SAM records found
        if (scalar(@sams) > 2) {
           croak "More than 2 SAM records matching [$id] ???";
        }
        elsif (scalar(@sams) > 0) {
            my $sam1 = @sams ? shift(@sams) : '';
            my $sam2 = @sams ? shift(@sams) : '';

            if ($sam1 and ($VERBOSE > 1)) {
                print '   BAM1:', $sam1->qname, ',', $sam1->flag;
            }
            if ($sam2 and ($VERBOSE > 1)) {
                print '   BAM2:', $sam2->qname, ',', $sam2->flag;
            }

            # Work out which (if any) SAM is read1 and which (if any) is read2

            my $read1 = undef;
            my $read2 = undef;

            # If there are 2 sam records, look at flag to work out read1/read2
            if ($sam1 and $sam2) {
                if ( substr($sam1->flag_as_bits,9,1) and
                     substr($sam2->flag_as_bits,8,1) ) {
                    $read1 = $sam1;
                    $read2 = $sam2;
                }
                elsif ( substr($sam1->flag_as_bits,8,1) and
                        substr($sam2->flag_as_bits,9,1) ) {
                    $read2 = $sam1;
                    $read1 = $sam2;
                }
                else {
                    croak "Should be no way to get here - logic failure 2.";
                }
            }
    
            # If there is only one sam records use FLAG to assign read1/read2
            elsif ($sam1) {
                if ( substr($sam1->flag_as_bits,9,1) ) {
                    $read1 = $sam1;
                }
                elsif ( substr($sam1->flag_as_bits,8,1) ) {
                    $read2 = $sam1;
                }
                else {
                    croak "Should be no way to get here - logic failure 3.";
                }
            }

            # Print read1 if present
            if (defined $read1) {
               $outfh->print( "\t",
                               join("\t", $read1->qname,
                                          $read1->rname,
                                          $read1->pos,
                                          $read1->mapq,
                                          $read1->tag( 'NH' ),
                                          $read1->tag( 'SM' ),
                                          $read1->tag( 'ZM' ),
                                          $read1->flag,
                                          $read1->flag_as_chars ) );
            }
            else {
               $outfh->print( "\t\t\t\t\t\t\t\t\t" );
            }

            # Print read2 if present
            if (defined $read2) {
               $outfh->print( "\t",
                               join("\t", $read2->qname,
                                          $read2->rname,
                                          $read2->pos,
                                          $read2->mapq,
                                          $read2->tag( 'NH' ),
                                          $read2->tag( 'SM' ),
                                          $read2->tag( 'ZM' ),
                                          $read2->flag,
                                          $read2->flag_as_chars ) );
            }
            else {
               $outfh->print( "\t\t\t\t\t\t\t\t\t" );
            }
        }
        else {
            # No SAM records found so print out tabs to keep spacing intact
               $outfh->print( "\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t" );
        }
        
        # Finalise diagnostic line printing;
        print "\n";

        # Check if MA2 has a usable record
        if ($mr2->current_record->id_notag eq $id) {
            $outfh->print( "\t",
                           join("\t", $mr2->current_record->id,
                                      $mr2->current_record->match_count,
                                      join(',',$mr2->current_record->matches) ) );
            $throwaway = $mr2->next_record;
        }
        else {
            $outfh->print( "\t\t\t" );
        }

        # Finish off the line
        $outfh->print( "\n" ) if ($VERBOSE > 1);

        $ctr++;
        #last if $ctr++ > 10000;  # DEBUG
    }
}


__END__

=head1 NAME

bam_vs_ma.pl - Perl script for reporting on NH flag


=head1 SYNOPSIS

 bam_vs_ma.pl [options]


=head1 ABSTRACT

This is a very quick-n-dirty script for examining the NH tag assigned by
Bioscope to BAM records and the relationship (if any) to MAPQ and the
raw alignments from the .ma file.


=head1 OPTIONS

 -i | --mafile1       name of MA file
 -f | --mafile2       name of second MA file
 -b | --bamfile       BAM (or SAM) file 
 -o | --outfile       output file
 -s | --samtoolsbin   pathname of the samtools binary
 -v | --verbose       print diagnostic messages
      --version       print version number
 -? | --help          display help
 -m | --man           display man page


=head1 DESCRIPTION

B<bam_vs_ma.pl> relies on samtools for the conversions between SAM and 
BAM so you'll need to have samtools installed to use this script (type
"module load samtools" on qcmg-clustermk1).  This
script is very basic and makes the following assumptions:

 1. The BAM is sorted by read_id (QNAME)
 2. The .ma (match) alignment files correspond to the BAM

=head2 Commandline Options

=over

=item B<-i | --mafile1>

Full pathname to first .ma file to be processed.  In the case of
fragment runs, this will be the only .ma file and for LMP and PE runs,
it should be the F3 tag match file.

=item B<-f | --mafile2>

Full pathname to second .ma file to be processed.  This option should
only be used for LMP runs where it is the R3 tag match file and for PE 
runs where it is the F5-P2 or F5-BC tag match file.

=item B<-b | --bamfile>

Name of the BAM file corresponding to (derived from) the match file(s)
specified with B<-i> and B<-f>.

=item B<-o | --outfile>

Name of the text report file to be written by the script.

=item B<-s | --samtoolsbin>

Full pathname to the samtools executable.  If you do not specify this
value then 'samtools' is used as the default in which case it must be
available via $PATH.

=item B<-v | --verbose>

Print progress and diagnostic messages.  This option can be specified
multiple times on the commandline to enable higher levels of verbosity.
The default level is 0 so no diagnostic messages are output.  At level
1, a small number of progress-related messages are written.  These
messages will be printed to STDOUT.

=item B<-h | --help>

Display help screen showing available commandline options.

=item B<-m | --man>

Display the full man page (this page).  This is equivalent to doing a
perldoc on the script.

=back


=head1 SEE ALSO

=over 2

=item perl

=back


=head1 AUTHOR

=over 2

=item John Pearson, L<mailto:j.pearson@uq.edu.au>

=back


=head1 VERSION

$Id: bam_vs_ma.pl 4669 2014-07-24 10:48:22Z j.pearson $


=head1 COPYRIGHT

Copyright (c) The University of Queensland 2010

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
