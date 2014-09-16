#!/usr/bin/perl
################################################################################
#
#    qCleanup.pl
#
#    A tool to clean up a mapping job
#
#    Authors: Stephen Kazakoff
#
#    $Id: qCleanup.pl 4714 2014-09-16 02:35:55Z s.kazakoff $
#
################################################################################

use strict;
use warnings;

use Data::Dumper;
use autodie;

$ENV{'PERL5LIB'} = "/share/software/QCMGPerl/lib/";

BEGIN {
    select STDERR; $| = 1;
    select STDOUT; $| = 1;
}

umask 0007;

################################################################################

use Getopt::Long;
use Capture::Tiny ':all';
use XML::Simple;

################################################################################

my $cmd_line = join (" ", @ARGV);
my $version = "0.0.1.0";

my $options = check_params();

# run qCleanup.pbs and wait for it to start
my $host = waitCleanup();

# return the hostname
print $host;


################################################################################
sub waitCleanup {

    my $sample = $options->{'sample'};
    my $seq_mapped = $options->{'seq_mapped'};
    my $seq_results = $options->{'seq_results'};

    my @qsub_args = (
        '-v', "sample=$sample,seq_mapped=$seq_mapped,seq_results=$seq_results",
        '-o', "$seq_mapped/log/$sample.markdups.log",
        '/share/software/QCMGPerl/distros/qOrganise/qCleanup.pbs',
    );

    my ($qsub_stdout, $qsub_stderr, $qsub_exit) = capture {

        system("qsub", @qsub_args)
    };

    die "Could not qsub job: $sample\n" if $qsub_stderr || $qsub_exit != 0;

    my $exec_host;
    my $job_state;

    do {

        my ($qstat_stdout, $qstat_stderr, $qstat_exit) = capture {
    
            system("qstat", "-x", (split('\.', $qsub_stdout))[0])
        };

        next if $qstat_stderr || $qstat_exit != 0;

        my $xml = XMLin($qstat_stdout);

        $exec_host = $xml->{'Job'}{'exec_host'};
        $job_state = $xml->{'Job'}{'job_state'};

        $exec_host =~ s/\/.*//;

        sleep 10;
    }
    while $job_state ne "R" && $job_state ne "C";

    return $exec_host;
}

################################################################################
sub check_params {

    my @standard_options = ( "help|h+", "version|v+" );
    my %options;

    GetOptions( \%options,

        @standard_options,

        "sample|s:s",
        "seq_mapped|m:s",
        "seq_results|r:s",
    );

    print "Version: $version\n\n" if $options{'version'};
    exec "pod2usage $0" if keys %options == 0 || $options{'help'};

    unless (exists $options{'sample'}) {

        print "\n";
        print "You must supply a 'sample' name before continuing.\n";
        print "Please use the '--sample' option.\n";
        print "\n";
        exec "pod2usage $0";
    }

    unless (exists $options{'seq_mapped'}) {

        print "\n";
        print "You must supply a 'seq_mapped' path before continuing.\n";
        print "Please use the '--seq_mapped' option.\n";
        print "\n";
        exec "pod2usage $0";
    }

    unless (exists $options{'seq_results'}) {

        print "\n";
        print "You must supply a 'seq_results' path before continuing.\n";
        print "Please use the '--seq_results' option.\n";
        print "\n";
        exec "pod2usage $0";
    }

    unless (-d $options{'seq_mapped'}) {

        print "\n";
        print "Could not find path: $options{'seq_mapped'}\n";
        print "\n";
        exec "pod2usage $0";
    }

    unless (-d $options{'seq_results'}) {

        print "\n";
        print "Could not find path: $options{'seq_results'}\n";
        print "\n";
        exec "pod2usage $0";
    }

    return \%options;
}

################################################################################
__DATA__

=head1 NAME

    qCleanup.pl


=head1 DESCRIPTION

    A tool to clean up a mapping job. This script qsubs a PBS job of the same
    name onto a node and waits for the job to start. Returns the hostname of the
    job that was started, allowing the caller to transfer files to the minion.

    The minion waits for a special file that signals the completion of the scp
    transfer. The minion then begins the cleanup.


=head1 SYNOPSIS

    qCleanup.pl --sample|s STR --seq_mapped|m PATH --seq_results|r PATH
        [--help|h] [--version|v]


    --sample STR            Specify a 'sample' name.
                            E.g. 140820_NS500239_0013_AH0YHDBGXX.lane_4.ATCACG

    --seq_mapped PATH       Specify the path to the local 'seq_mapped' folder.
                            E.g. /mnt/seq_mapped/140820_NS500239_0013_AH0YHDBGXX

    --seq_results           Specify the path to the relevant 'seq_results' folder.
                            E.g. /mnt/seq_results/smg_core/smgcore_taft/TAFT_0077/seq_mapped


    [--help]                Optional. Displays this basic usage information.
    [--version]             Optional. Displays this scipt's version number.


    For more detailed help, please see the description.

=head1 COPYRIGHT

Copyright (C) 2013 Stephen Kazakoff
Copyright (c) The University of Queensland 2013,2014

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

=cut
