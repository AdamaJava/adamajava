#!/usr/bin/env perl

##############################################################################
#
#  Program:  tools_template.pl
#  Author:   John V Pearson
#  Created:  2011-10-18
#
#  Based directly on a cut-down version of qmaftools.pl (included in
#  this sourceforge reposity under distros/seqtools/src.).  It shows a
#  way to structure a "tools" script which has multiple modes each of
#  which has different commandline processing.
#
#  $Id$
#
##############################################################################

use strict;
use warnings;

use Carp qw( carp croak confess );
use Data::Dumper;
use Getopt::Long;
use IO::File;
use Pod::Usage;

use QCMG::Util::QLog;

use vars qw( $SVNID $REVISION $CMDLINE $VERBOSE );


( $REVISION ) = '$Revision$ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id$'
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

    my @valid_modes = qw( help man version 
                          add_context
                          select
                          recode
                          recode_abo_id
                          condense
                          qinspect
                          tofastq
                          sammd
                          xref
                          compare
                          dcc_filter
                          variant_proportion
                          cnv_matrix
                          clinical
                          project_maf
                          study_group
                          re_maf_compare
                          );

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
    elsif ($mode =~ /^select/i) {
        mode_select();
    }
    elsif ($mode =~ /^recode/i) {
        mode_recode();
    }
    elsif ($mode =~ /^condense/i) {
        mode_condense();
    }
    elsif ($mode =~ /^qinspect/i) {
        mode_qinspect();
    }
    elsif ($mode =~ /^tofastq/i) {
        mode_tofastq();
    }
    elsif ($mode =~ /^xref/i) {
        mode_xref();
    }
    elsif ($mode =~ /^sammd/i) {
        mode_sammd();
    }
    elsif ($mode =~ /^compare/i) {
        mode_compare();
    }
    elsif ($mode =~ /^dcc_filter/i) {
        mode_dcc_filter();
    }
    elsif ($mode =~ /^variant_proportion/i) {
        mode_variant_proportion();
    }
    elsif ($mode =~ /^recode_abo_id/i) {
        mode_recode_abo_id();
    }
    elsif ($mode =~ /^add_context/i) {
        mode_add_context();
    }
    elsif ($mode =~ /^clinical/i) {
        mode_clinical();
    }
    elsif ($mode =~ /^cnv_matrix/i) {
        mode_cnv_matrix();
    }
    elsif ($mode =~ /^project_maf/i) {
        mode_project_maf();
    }
    elsif ($mode =~ /^study_group/i) {
        mode_study_group();
    }
    elsif ($mode =~ /^re_maf_compare/i) {
        mode_re_maf_compare();
    }
    else {
        die "qmaftools mode [$mode] is unrecognised or unimplemented; valid ".
            'modes are: '.  join(' ',@valid_modes) ."\n";
    }
}


sub mode_example {

    # Print help message if no CLI params
    pod2usage( -exitval  => 0,
               -verbose  => 99,
               -sections => 'SYNOPSIS|COMMAND DETAILS/EXAMPLE' )
        unless (scalar @ARGV > 0);

    # Setup defaults for CLI params
    my %params = ( directory   => '',
                   copydir     => '',
                   outfile     => '',
                   logfile     => '',
                   verbose     => 0 );

    my $results = GetOptions (
           'd|directory=s'        => \$params{directory},     # -d
           'c|copydir=s'          => \$params{copydir},       # -c
           'o|outfile=s'          => \$params{outfile},       # -o
           'l|logfile=s'          => \$params{logfile},       # -l
           'v|verbose+'           => \$params{verbose},       # -v
           );

    # It is mandatory to supply some input params
    die "You must specify a directory\n" unless $params{directory};
    die "You must specify a copydir\n" unless $params{copydir};
    die "You must specify an output file\n" unless $params{outfile};

    die "directory parameter must be an absolute path\n"
        unless ($params{directory} =~ /^\//);

    # Set up logging
    qlogfile($params{logfile}) if $params{logfile};
    qlogbegin;
    qlogprint( {l=>'EXEC'}, "CommandLine $CMDLINE\n" );
    qlogparams( \%params );

    # Check that the copydir is writable
    die 'Unable to write to copydir [',$params{copydir},"]\n"
        unless (-w $params{copydir});

    # Open the output file
    my $outfh = IO::File->new( $params{outfile}, 'w' );
    die "unable to open file $params{outfile} for writing: $!" unless
        defined $outfh;
    $outfh->print( join(' ',qw( donor qsnp_uuid gatk_uuid )), "\n" );

    qlogend;
}


__END__

=head1 NAME

tools_template.pl - Collect utility routines in a single script


=head1 SYNOPSIS

 tools_template.pl mode [options]


=head1 ABSTRACT

This script is a suggestion on how one might structure a script that
contains multiple modes.  The command-line interface is patterned on samtools
so the first CLI param is the mode which is followed by any options
specific to that mode.


=head1 COMMANDS

 example        - an example mode

 version        - print version number and exit immediately
 help           - display usage summary
 man            - display full man page


=head1 COMMAND DETAILS

=head2 EXAMPLE

 -i | --infile        MAF input file
 -o | --outfile       MAF output file
 -r | --recodefile    Text file containing ID recode pairs
 -l | --logfile       log file (optional)
 -v | --verbose       print progress and diagnostic messages

This example does absolutely nothing useful.  :)


=head1 COMMON COMMANDLINE OPTIONS

Many of the modes in this script use the same set of commandline 
options so common options are documented here rather than having the
descriptions replicated within the documentation for each mode.

=over

=item B<-o | --outfile>

CSV output file.  Patients are always columns ordered either
alphabetically or according to the order established by B<--patfile>.

=item B<-l | --logfile>

Optional log file name.  If this option is not specified then logging
goes to STDOUT.

=item B<-v | --verbose>

Print progress and diagnostic messages.  This option can be specified
multiple times on the commandline to enable higher levels of verbosity.

=item B<--version>

Print the script version and exit immediately.

=item B<-h | --help>

Display help screen showing available commandline options.

=item B<-m | --man>

Display the full man page (this page).  This is equivalent to doing a
perldoc on the script.

=back


=head1 AUTHOR

=over 2

=item John Pearson, L<mailto:grendeloz@gmail.com>

=back


=head1 VERSION

$Id$


=head1 COPYRIGHT

Copyright (c) The University of Queensland 2011-2014
Copyright (c) The QIMR Berghofer Medical Research Institute 2016

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
