#!/usr/bin/perl -w

##############################################################################
#
#  Program:  torrenttools.pl
#  Author:   John V Pearson
#  Created:  2012-05-18
#
#  Operate on files related to Ion Torrent.
#
#  $Id: torrenttools.pl 4669 2014-07-24 10:48:22Z j.pearson $
#
##############################################################################

use strict;
use warnings;

use Carp qw( carp croak confess );
use Data::Dumper;
use Getopt::Long;
use IO::File;
use Pod::Usage;
use POSIX qw( floor );
use Storable qw(dclone);

use QCMG::IO::qPileupReader;
use QCMG::Util::QLog;

use vars qw( $SVNID $REVISION $CMDLINE $VERSION $VERBOSE );

( $REVISION ) = '$Revision: 4669 $ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id: torrenttools.pl 4669 2014-07-24 10:48:22Z j.pearson $'
    =~ /\$Id:\s+(.*)\s+/;


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
    
    $CMDLINE = join(' ',@ARGV);
    my $mode = shift @ARGV;
    
    my @valid_modes = qw( help man version qpileup1 );


    if ($mode =~ /^$valid_modes[0]$/i or $mode =~ /\?/) {
        pod2usage( -exitval  => 0,
                   -verbose  => 99,
                   -sections => 'SYNOPSIS|COMMANDS' );
    }
    elsif ($mode =~ /^$valid_modes[1]$/i) {
        pod2usage(-exitstatus => 0, -verbose => 2)
    }
    elsif ($mode =~ /^$valid_modes[2]$/i) {
        print "$SVNID\n";
    }
    elsif ($mode =~ /^$valid_modes[3]/i) {
        qpileup1();
    }
    else {
        die "torrenttools [$mode] is unrecognised; valid modes are: ".
            join(' ',@valid_modes) ."\n";
    }
}


sub recode {
   
    # Print help message if no CLI params
    pod2usage( -exitval  => 0,
               -verbose  => 99,
               -sections => 'COMMAND DETAILS/RECODE' )
        unless (scalar @ARGV > 0);

    # Setup defaults for CLI params
    my %params = ( infile      => '',
                   outfile     => '',
                   recodefile  => '',
                   logfile     => '',
                   verbose     => 0 );

    my $results = GetOptions (
           'i|infile=s'           => \$params{infile},        # -i
           'o|outfile=s'          => \$params{outfile},       # -o
           'r|recodefile=s'       => \$params{recodefile},    # -r
           'l|logfile=s'          => \$params{logfile},       # -l
           'v|verbose+'           => \$params{verbose},       # -v
           );

    # It is mandatory to supply infile, outfile and recodefile names
    die "You must specify an input file\n" unless $params{infile};
    die "You must specify an output file\n" unless $params{outfile};
    die "You must specify a recodefile\n" unless $params{recodefile};

    # Set up logging
    qlogfile($params{logfile}) if $params{logfile};
    qlogbegin;
    qlogprint( {l=>'EXEC'}, "CommandLine $CMDLINE\n" );

    my $ra_mafrecs = parse_maf( $params{infile} );
    my $ra_recodes = parse_recodes( $params{recodefile} );

    #print Dumper $ra_recodes;

    # We need to process the Tumor and Matched_Norm IDs in separate loops
    # because for either ID, as soon as we get a match in the recodes,
    # we want to stop further matching on that ID.  This is easier to
    # handle if the 2 IDs are processed independently.

    my $tctr = 0;
    foreach my $mfr (@{ $ra_mafrecs }) {
        foreach my $ra_rec (@{ $ra_recodes }) {
            if ($mfr->Tumor_Sample_Barcode =~ /$ra_rec->[0]/) {
                $tctr++;
                $mfr->Tumor_Sample_Barcode( $ra_rec->[1] . '_TD' );
                # If we get a match, stop checking
                last;
            }
        }
    }
    qlogprint( {l=>'INFO'}, "recoded $tctr Tumor_Sample_Barcode IDs\n" );

    my $nctr = 0;
    foreach my $mfr (@{ $ra_mafrecs }) {
        foreach my $ra_rec (@{ $ra_recodes }) {
            if ($mfr->Matched_Norm_Sample_Barcode =~ /$ra_rec->[0]/) {
                $nctr++;
                $mfr->Matched_Norm_Sample_Barcode( $ra_rec->[1] . '_ND' );
                # If we get a match, stop checking
                last;
            }
        }
    }
    qlogprint( {l=>'INFO'}, "recoded $nctr Matched_Norm_Sample_Barcode IDs\n" );

    write_maf_file( $params{outfile}, $ra_mafrecs );

    qlogend;
}


sub parse_genes {
    my $file = shift;

    my $infh = IO::File->new( $file, 'r' );
    croak "Can't open gene file $file for reading: $!\n"
        unless defined $infh;

    my @genes = ();
    while (my $line = $infh->getline) {
        chomp $line;
        my @fields = split /\s+/, $line;
        push @genes, $fields[0];
    }

    qlogprint( {l=>'INFO'}, 'loaded '.scalar(@genes)." genes from $file\n");
    return @genes;
}



__END__

=head1 NAME

torrenttools.pl - Functions for Ion Torrent


=head1 SYNOPSIS

 qmaftools.pl command [options]


=head1 ABSTRACT

This script operates on files relating to Ion Torrent.
The full documentation for this script is on
the QCMG wiki at http://qcmg-wiki.imb.uq.edu.au/index.php/torrenttools.pl


=head1 COMMANDS

 qpileup1       - pull stats from a torrent qpileup
 version        - print version number and exit immediately
 help           - display usage summary
 man            - display full man page


=head1 COMMAND DETAILS

=head2 QPILEUP1

 -i | --infile        qPileup report file
 -l | --logfile       log file (optional)
 -v | --verbose       print progress and diagnostic messages


=head2 Commandline options

=over

=item B<-l | --logfile>

Optional log file name.  If this option is not specified then logging
goes to STDOUT.

=item B<-v | --verbose>

Print progress and diagnostic messages.  This option can be specified
multiple times on the commandline to enable higher levels of verbosity.

=back


=head1 AUTHOR

=over 2

=item John Pearson, L<mailto:j.pearson@uq.edu.au>

=back


=head1 VERSION

$Id: torrenttools.pl 4669 2014-07-24 10:48:22Z j.pearson $


=head1 COPYRIGHT

Copyright (c) The University of Queensland 2012

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
