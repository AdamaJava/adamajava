#!/usr/bin/perl -w

##############################################################################
#
#  Program:  template.pl
#  Author:   John V Pearson
#  Created:  2010-06-24
#
#  This script is a template framework for a command-line perl script.
#
#  $Id: template.pl 4667 2014-07-24 10:09:43Z j.pearson $
#
##############################################################################

use strict;
use warnings;

use Carp qw( croak );
use IO::File;
use Getopt::Long;
use Data::Dumper;
use Pod::Usage;
use QCMG::Util::QLog;

use vars qw( $SVNID $REVISION %PARAMS );

( $REVISION ) = '$Revision: 4667 $ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id: template.pl 4667 2014-07-24 10:09:43Z j.pearson $'
    =~ /\$Id:\s+(.*)\s+/;


###########################################################################
#
# "Cry havoc, and let slip the dogs of war ..."
#

MAIN: {

    # Setup defaults for important variables.  We put these is a single
    # hash so we can pass them around easily if we need to.

    %PARAMS = ( infiles    => [],
                outfile    => 'default_outfile.txt',
                logfile    => '',
                dir        => '',
                cache_dir  => './',
                chrom      => '',
                start_base => 1,
                verbose    => 0,
                version    => 0,
                help       => '',
                man        => 0 );

    # Print usage message if no arguments supplied
    pod2usage(1) unless (scalar @ARGV > 0);

    # Use GetOptions module to parse commandline options

    my $cmdline = join(' ',@ARGV);
    my $results = GetOptions (
           'i|infile=s'           =>  $PARAMS{infiles},       # -i
           'o|outfile=s'          => \$PARAMS{outfile},       # -o
           'l|logfile=s'          => \$PARAMS{logfile},       # -l
           'd|dir=s'              => \$PARAMS{dir},           # -d
           'a|cache_dir=s'        => \$PARAMS{cache_dir},     # -a
           'c|chrom=s'            => \$PARAMS{chrom},         # -c
           's|start_base=i'       => \$PARAMS{start_base},    # -s, default=1
           'v|verbose+'           => \$PARAMS{verbose},       # -v
           'version!'             => \$PARAMS{version},       # --version
           'h|help|?'             => \$PARAMS{help},          # -?
           'man|m'                => \$PARAMS{man}            # -m
           );

    # Handle version, man and help
    if ($PARAMS{version}) {
        print "$SVNID\n";
        exit;
    }
    pod2usage(1) if $PARAMS{help};
    pod2usage(-exitstatus => 0, -verbose => 2) if $PARAMS{man};

    # Set up logging
    qlogfile($PARAMS{logfile}) if $PARAMS{logfile};
    qlogbegin;
    qlogprint( {l=>'EXEC'}, "CommandLine $cmdline\n");

    # Allow for ,-separated lists of infiles
    my @infiles = map { split /\,/,$_ } @{ $PARAMS{infiles} };
    $PARAMS{infiles} = \@infiles;

    # If $dir is specified, prepend it to all input filenames
    # *UNLESS* they have a leading '/' in which case do nothing
    if ($PARAMS{dir}) { 
        $PARAMS{dir} =~ s/\/$//;  # kill any trailing slash on $dir
        @infiles = map { m/^\// ? $_ : $PARAMS{dir}."/$_" } @infiles;
    }

    # Append '/' to cache_dir if not already present;
    $PARAMS{cache_dir} .= '/' unless ($PARAMS{cache_dir} =~ m/\/$/);

    die( "At least one file must be specified with the -i option\n" )
        unless (@infiles);

    # Print out run params if running in verbose mode
    if ($PARAMS{verbose}) {
        qlogprint( "Run Parameters:\n" );
        foreach my $key (sort keys %PARAMS) {
            if (ref $PARAMS{$key} eq 'ARRAY') {
                my $num_vals = scalar( @{$PARAMS{$key}} );
                if ( $num_vals == 1 ) {
                    qlogprint( "    $key : ", $PARAMS{$key}->[0], "\n" );
                }
                elsif ( $num_vals > 1 ) {
                    qlogprint( "    $key : ", $PARAMS{$key}->[0], "\n" );
                    my $indent = length( "    $key : " );
                    foreach my $ctr (1..($num_vals-1)) {
                         qlogprint( ' 'x$indent, $PARAMS{$key}->[$ctr], "\n" );
                    }
                }
                else {
                    qlogprint( "    $key :\n" );
                }
            }
            else {
                qlogprint( "    $key : ", $PARAMS{$key}, "\n" );
            }
        }
    }

    my $rh_stuff = process_infiles( $PARAMS{infiles} );
    #print Dumper $rh_stuff;

    # Log program exit
    qlogend;
}


sub process_infiles {
    my $ra_infiles = shift;

    my %stuff = ();
    foreach my $infile (@{$ra_infiles}) {

        # Open the file
        my $fh = IO::File->new( $infile, 'r' );
        die( "Cannot open input file $infile for reading: $!\n" )
            unless defined $fh;
        qlogprint( "Reading from file: $infile\n" ) if $PARAMS{verbose};

        # Read all the lines from the file
        while (my $line = $fh->getline) {
            chomp $line;
            # do some stuff
        }

        # Close the file
        $fh->close();
    }

    return \%stuff;  # pass back the file contents
}



__END__

=head1 NAME

template.pl - Perl script for blah blah blah


=head1 SYNOPSIS

 template.pl [options]


=head1 ABSTRACT

This script takes as input the name of a file containing ...


=head1 OPTIONS

 -i | --infile        name of input file(s); Required!
 -o | --outfile       output filename; default=default_output.txt
 -l | --logfile       logfile name
 -c | --chromosome    chromosome for blah
 -s | --start_base    number of Start Base within chromosome; default=1
 -v | --verbose       print progress and diagnostic messages
      --version       print version number
 -? | --help          display help
 -m | --man           display man page


=head1 DESCRIPTION

B<template> was designed to support a project for blah.

B<template> takes as input blah

B<template> produces as output blah


=head2 Commandline Options

=over

=item B<-i | --infile>

Multiple input files are allowed and may be specified 2 ways.  Firstly,
the B<-i> option may be specified multiple times, and secondly each
B<-i> may be followed by a comma-separated list (no spaces!) of
filenames.  You may also mix the 2 modes.

=item B<-d | --dir>

If the B<-d> option is specified, then the directory will be prepended
to every infile specified with B<-i> UNLESS the filename starts with a
"/" character indicating it is a full pathname.  This mechanism lets you
specify a directory to be prepended to most input files but with a
number of files located elsewhere which are specified with full
pathnames.

=item B<-a | --cache_dir>

This script is designed to retrieve blah files from blah server using
blah protocol.  Because this process is time-intensive, a local cache is
kept of all these files so they do not need to be retrieved on
subsequent runs of this script.  The default value is the current 
working directory.

=item B<-l | --logfile>

By default, error and diagnostic messages are output to STDOUT but if
a logfile is specified, the messages will be written to the logfile 
instead.  This can be particularly useful if the script is intended to
be made available via a web wrapper.

=item B<-o | --outfile>

The output file is a tab-separated plain text with unix-style line
endings.  If no name is supplied for the output file then the default
(rs_2_gene.txt) is used.  The output file contains 7 columns of data:

 1.  RS SNP ID
 2.  Alleles
 3.  Chromosome
 4.  Physical position within chromosome
 5.  Associated gene symbol (if any)
 6.  Functional category
 7.  Array platforms on which this SNP appears

=item B<-v | --verbose>

Print progress and diagnostic messages.  This option can be specified
multiple times on the commandline to enable higher levels of verbosity.
The default level is 0 so no diagnostic messages are output.  At level
1, a small number of progress-related messages are written.  These
messages will be printed to STDOUT unless the B<-l> option is used to
specify a logfile.

=item B<-h | --help>

Display help screen showing available commandline options.

=item B<-m | --man>

Display the full man page (this page).  This is equivalent to doing a
perldoc on the script.

=back

=head2 Example

The following usage example will do blah

  template -i blah.fa -c blah -s 15736064

B<N.B.> The spaces between the options (B<-i>, B<-c> etc) and the actual
values are compulsory.  If you do not use the spaces then the value will
be ignored.


=head1 SEE ALSO

=over 2

=item perl

=back


=head1 AUTHOR

=over 2

=item John Pearson, L<mailto:grendeloz@gmail.com>

=back


=head1 VERSION

$Id: template.pl 4667 2014-07-24 10:09:43Z j.pearson $


=head1 COPYRIGHT

Copyright (c) The University of Queensland 20xx

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
