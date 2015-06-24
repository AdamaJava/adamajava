#!/usr/bin/env perl

##############################################################################
#
#  Program:  qvariantdb.pl
#  Author:   John V Pearson
#  Created:  2013-07-19
#
#  A perl program to parse variants subdirectories to create a database
#  of calls plus create reports.
#
#  $Id: qvariantdb.pl 4667 2014-07-24 10:09:43Z j.pearson $
#
##############################################################################

use strict;
use warnings;

use Carp qw( carp croak confess );
use Data::Dumper;
use Getopt::Long;
use IO::File;
use Pod::Usage;

use QCMG::FileDir::QLogFile;
use QCMG::FileDir::QSnpDirParser;
use QCMG::Util::QLog;
use QCMG::QBamMaker::SeqFinalDirectory;

use vars qw( $SVNID $REVISION $CMDLINE %PARAMS );

( $REVISION ) = '$Revision: 4667 $ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id: qvariantdb.pl 4667 2014-07-24 10:09:43Z j.pearson $'
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

    # Each of the modes invokes a subroutine, and these subroutines 
    # are often almost identical.  While this looks like wasteful code 
    # duplication, it is necessary so that each mode has complete 
    # independence in terms of processing input parameters and taking
    # action based on the parameters.

    my @valid_modes = qw( help man version qsnpdir qsnpdup gatkdir );

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
        qsnpdir();
    }
    elsif ($mode =~ /^$valid_modes[4]/i) {
        qsnpdup();
    }
    elsif ($mode =~ /^$valid_modes[3]/i) {
        gatkdir();
    }
    else {
        die "qvariantdb mode [$mode] is unrecognised; valid modes are: " .
            join(' ',@valid_modes) ."\n";
    }
}


sub gatkdir {
    my $class = shift;

    pod2usage( -exitval  => 0,
               -verbose  => 99,
               -sections => 'COMMAND DETAILS/GATKDIR' )
        unless (scalar @ARGV > 0);

    # Setup defaults for important variables.

    my %params = ( dir      => '',
                   outfile  => '',
                   logfile  => '',
                   verbose  => 0 );

    my $results = GetOptions (
           'd|dir=s'              => \$params{dir},           # -d
           'o|outfile=s'          => \$params{outfile},       # -o
           'l|logfile=s'          => \$params{logfile},       # -l
           'v|verbose+'           => \$params{verbose},       # -v
           );

    # It is mandatory to supply an outfile and directory
    die "You must specify a directory (-d) and an outfile (-o)\n"
        unless ( $params{dir} and $params{outfile} );
    
    # Set up logging
    qlogfile($params{logfile}) if $params{logfile};
    qlogbegin();
    qlogprint( {l=>'EXEC'}, "CommandLine $CMDLINE\n");
    qlogparams( \%params ) if $params{verbose};

    # Get the detailed output file ready
    my $outfh = IO::File->new( $params{outfile}, 'w' );
    croak 'Cannot open output file ',$params{outfile}," for writing: $!"
        unless defined $outfh;

    my $fact = QCMG::FileDir::GatkDirParser->new( verbose => $params{verbose} );

    # Find all variants/qSNP directories
    my $ra_qsnps = $fact->parse( $params{dir} );
    $outfh->print( $fact->completion_report );
    $outfh->close;

    qlogend();
}


sub qsnpdir {
    my $class = shift;

    pod2usage( -exitval  => 0,
               -verbose  => 99,
               -sections => 'COMMAND DETAILS/QSNPDIR' )
        unless (scalar @ARGV > 0);

    # Setup defaults for important variables.

    my %params = ( dir      => '',
                   outfile  => '',
                   logfile  => '',
                   verbose  => 0 );

    my $results = GetOptions (
           'd|dir=s'              => \$params{dir},           # -d
           'o|outfile=s'          => \$params{outfile},       # -o
           'l|logfile=s'          => \$params{logfile},       # -l
           'v|verbose+'           => \$params{verbose},       # -v
           );

    # It is mandatory to supply an outfile and directory
    die "You must specify a directory (-d) and an outfile (-o)\n"
        unless ( $params{dir} and $params{outfile} );
    
    # Set up logging
    qlogfile($params{logfile}) if $params{logfile};
    qlogbegin();
    qlogprint( {l=>'EXEC'}, "CommandLine $CMDLINE\n");
    qlogparams( \%params ) if $params{verbose};

    # Get the detailed output file ready
    my $outfh = IO::File->new( $params{outfile}, 'w' );
    croak 'Cannot open output file ',$params{outfile}," for writing: $!"
        unless defined $outfh;

    my $fact = QCMG::FileDir::QSnpDirParser->new( verbose => $params{verbose} );

    # Find all variants/qSNP directories
    my $ra_qsnps = $fact->parse( $params{dir} );
    $outfh->print( $fact->completion_report );
    $outfh->close;

    qlogend();
}


sub qsnpdup {
    my $class = shift;

    pod2usage( -exitval  => 0,
               -verbose  => 99,
               -sections => 'COMMAND DETAILS/QSNPDUP' )
        unless (scalar @ARGV > 0);

    # Setup defaults for important variables.

    my %params = ( dir      => '',
                   outfile  => '',
                   logfile  => '',
                   verbose  => 0 );

    my $results = GetOptions (
           'd|dir=s'              => \$params{dir},           # -d
           'o|outfile=s'          => \$params{outfile},       # -o
           'l|logfile=s'          => \$params{logfile},       # -l
           'v|verbose+'           => \$params{verbose},       # -v
           );

    # It is mandatory to supply an outfile and directory
    die "You must specify a directory (-d) and an outfile (-o)\n"
        unless ( $params{dir} and $params{outfile} );
    
    # Set up logging
    qlogfile($params{logfile}) if $params{logfile};
    qlogbegin();
    qlogprint( {l=>'EXEC'}, "CommandLine $CMDLINE\n");

    # Get the detailed output file ready
    my $outfh = IO::File->new( $params{outfile}, 'w' );
    croak 'Cannot open output file ',$params{outfile}," for writing: $!"
        unless defined $outfh;

    my $fact = QCMG::FileDir::QSnpDirParser->new( verbose => $params{verbose} );

    # Find all variants/qSNP directories
    my $ra_qsnps = $fact->parse( $params{dir} );
    $outfh->print( $fact->duplicate_report );
    $outfh->close;

    qlogend();
}


sub dbdump {
    my $class = shift;

    pod2usage( -exitval  => 0,
               -verbose  => 99,
               -sections => 'COMMAND DETAILS/DBDUMP' )
        unless (scalar @ARGV > 0);

    # Setup defaults for important variables.

    my %params = ( indir    => '',
                   outdir   => '',
                   logfile  => '',
                   verbose  => 0 );

    my $results = GetOptions (
           'i|indir=s'            => \$params{indir},         # -i
           'o|outdir=s'           => \$params{outdir},        # -o
           'l|logfile=s'          => \$params{logfile},       # -l
           'v|verbose+'           => \$params{verbose},       # -v
           );

    # It is mandatory to supply both in and out dirs
    die "You must specify an indir (-i) and an outdir (-o)\n"
        unless ( $params{indir} and $params{outdir} );
    
    # Set up logging
    qlogfile($params{logfile}) if $params{logfile};
    qlogbegin();
    qlogprint( {l=>'EXEC'}, "CommandLine $CMDLINE\n");
    qlogparams( \%params ) if $params{verbose};

    my $qsnp = QCMG::FileDir::QsnpDirParser->new( verbose => $params{verbose} );
    my $ra_qsnps = $qsnp->parse( $params{indir} );

    my $gatk = QCMG::FileDir::GatkDirParser->new( verbose => $params{verbose} );
    my $ra_gatks = $gatk->parse( $params{indir} );

    # Now we need to write out our TSV data dumps
    # ...

    qlogend();
}


__END__

=head1 NAME

qvariantdb.pl - report on QCMG variant calling


=head1 SYNOPSIS

 qvariantdb.pl command [options]


=head1 ABSTRACT

This script outputs reports on QCMG variant calling.


=head1 COMMANDS

 qsnpdir        - process qSNP call directories
 qsnpdup        - find duplicat qSNP runs
 gatkdir        - process GATK call directories
 version        - print version number and exit immediately
 help           - display usage summary
 man            - display full man page


=head1 COMMAND DETAILS

=head2 QSNPDIR

Process all qSNP call directories below a specifed directory.

 -d | --dir           root directory of search space
 -o | --outfile       output file in text format
 -l | --logfile       Log file; optional
 -v | --verbose       print progress and diagnostic messages

=head2 QSNPDUP

Find qSNP call directories that were run on the same pair of BAMs.

 -d | --dir           root directory of search space
 -o | --outfile       output file in text format
 -l | --logfile       Log file; optional
 -v | --verbose       print progress and diagnostic messages

=head2 GATKDIR

Process all GATK call directories below a specifed directory.

 -d | --dir           root directory of search space
 -o | --outfile       output file in text format
 -l | --logfile       Log file; optional
 -v | --verbose       print progress and diagnostic messages


=head1 DESCRIPTION

I'll fill some stuff in here one day - JP.


=head1 AUTHOR

=over 2

=item John Pearson, L<mailto:j.pearson@uq.edu.au>

=back


=head1 VERSION

$Id: qvariantdb.pl 4667 2014-07-24 10:09:43Z j.pearson $


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
