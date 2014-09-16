package QCMG::Organise::Common;
################################################################################
#
#    Common.pm
#
#    Common methods to handle project logging
#
#    Authors: Stephen Kazakoff
#
#    $Id: Common.pm 4672 2014-07-26 07:04:40Z s.kazakoff $
#
################################################################################
# # #
# #       LOAD MODULES AND GET OPTIONS
#
use strict;
use warnings;
################################################################################
################################################################################
$ENV{'PERL5LIB'} = "/share/software/QCMGPerl/lib/";

BEGIN {
    select STDERR; $| = 1;
    select STDOUT; $| = 1;
}

umask 0007;

use Getopt::Long;
use Data::Dumper;

use Bio::SeqIO;
use Bio::DB::Sam;
use Capture::Tiny ':all';
use Cwd 'abs_path';
use DBI;
use File::Basename;
use File::chdir;
use File::Copy;
use File::Find::Rule;
use File::Spec;
use IO::Handle;
use IO::Tee;
use File::Path 'make_path';
use File::Slurp qw(edit_file read_file);
use File::Temp;
use Math::Round;
use POSIX;
use Sort::Key::Natural qw(natkeysort natsort);


################################################################################
# # #
# #       MAIN
#
################################################################################
sub new {
#
# Define a constructor
#
    my ($class, %options) = @_;

    my $self = {};

    bless $self, $class;

    map {
        $self->{$_} = delete $options{$_}
    }
    keys %options;

    $self->{'default_email'} = 'QCMG-InfoTeam@imb.uq.edu.au';

    $self->load_defaults();

    $self->create_log();

    return $self;
}

################################################################################
sub load_defaults {
#
# Set some defaults
#
    my $self = shift;

    my $LA_java = "/usr/bin/java";
    my $LA_cred = $ENV{'HOME'} . "/.mediaflux/mf_credentials.cfg";
    my $LA_term = "/panfs/share/software/mediaflux/aterm.jar";
    my $LA_base = "$LA_java -Dmf.result=shell -Dmf.cfg=$LA_cred -Xmx1g -jar $LA_term";

    my $genome_dir = "/panfs/share/genomes";

    my $config = {

        'LA_execute' => "$LA_base --app exec ",

        'default_email' => 'QCMG-InfoTeam@imb.uq.edu.au',

        'default_genome' => join ("/",

            $genome_dir,
            "GRCh37_ICGC_standard_v2",
            "GRCh37_ICGC_standard_v2.fa",
        ),

        'default_chromosomes' => join ("/",

            $genome_dir,
            "GRCh37_ICGC_standard_v2_split",
        ),

        'norRNA_genome' => join ("/",

            $genome_dir,
            "GRCh37_ICGC_standard_v2",
            "GRCh37_ICGC_standard_v70_norRNA",
        ),

        'default_GTF' => join ("/",

            $genome_dir,
            "GRCh37ensemblv70",
            "GRCh37_ICGC_standard_v70.gtf",
        ),

        'default_GC' => join ("/",

            $genome_dir,
            "GRCh37ensemblv70",
            "GRCh37_ICGC_standard_v70.gc.txt",
        ),
    };

    map {

        $self->{$_} = $config->{$_}
    }
    keys %$config;

    return;
}

################################################################################
sub create_log {
#
# Renames an existing logfile (if present), creates a log and writes a header
#
    my $self = shift;

    my $logfile = $self->{'log_file'};
    my $logextn = $self->{'log_extn'};

    my $date = date_spec();

    my ($name, $dir, $suffix) = fileparse($logfile, $logextn);

    if (
        File::Find::Rule->maxdepth(1)->file->name($name . $suffix)->in($dir)
    ) {
        my @ext = split (/\./, $suffix);

        my $type = pop @ext;

        my $newname = $name . join (".", @ext, "") . $date->[2] . ".$type";

        my $rv = rename ($logfile, $dir . $newname);

        if ($rv != 1) {

            die join ("\n",

                "Error: Could not rename the file:",
                $logfile,
                "Please check file permissions and try again.",
            ) . "\n";
        }
    }

    $self->{'log_fh'} = IO::File->new($logfile, "w");
    $self->{'log_io'} = IO::Handle->new();

    $self->{'log_io'}->autoflush(1);

    my %sysinfo = (

       'nodename' => capture { system ("uname", "-n") },
       'userid' => capture { system ("whoami") },
       'kernel_name' => capture { system ("uname", "-s") },
       'architecture' => capture { system ("arch") },
       'kernel_version' => capture { system ("uname", "-r") },
       'perl' => capture { system ("which", "perl") },
       'java' => capture { system ("which", "java") },
       'java_version' => (split ('"', capture { system ("java -version 2>&1") }))[1],
    );
  
    map { chomp; $_ } values %sysinfo;

    my $msg = join ("\n",
        "EXECLOG: Start-time " . $date->[1],
        "EXECLOG: Host " . $sysinfo{'nodename'},
        "EXECLOG: Run-by " . $sysinfo{'userid'},
        "EXECLOG: OS-name " . $sysinfo{'kernel_name'},
        "EXECLOG: OS-arch " . $sysinfo{'architecture'},
        "EXECLOG: OS-version " . $sysinfo{'kernel_version'},
        "EXECLOG: Tool-name " . $0,
        "EXECLOG: Tool-version " . $main::version,
        "EXECLOG: PWD " . getcwd,
        "EXECLOG: Command-line $0 $main::cmd_line",
        "EXECLOG: Perl-version " . $],
        "EXECLOG: Perl-home " . $sysinfo{'perl'},
        "EXECLOG: Java-version " . $sysinfo{'java_version'},
        "EXECLOG: Java-home " . $sysinfo{'java'},
        "TOOLLOG: Log_file " . $logfile,
        "TOOLLOG: Slide_folder " . abs_path($dir),
        "TOOLLOG: Default_email " . $self->{'default_email'},
        "TOOLLOG: Slide_name " . $name
    );

    $self->{'start_time'} = $date;

    $self->write_log("$msg\n\n");

    return;
}

################################################################################
sub execute_PBS {
#
# Executes a list of PBS scripts
#
    my $self = shift;

    return if ($main::run_mode || !$self->{'PBS_jobs'});

    $self->write_log("--- Executing PBS ---");

    for my $job (sort @{ $self->{'PBS_jobs'} }) {

        my $dependency = $self->get_dependencies($job);

        my ($stdout, $stderr, $exit) = capture {

            if ($dependency) {

                system ("qsub", "-W depend=afterok:$dependency", $job);
            }
            else {

                system ("qsub", $job);
            };
        };

        chomp $stdout;

        $self->write_log("Job executed: $stdout");

        $self->{'job_ids'}{$job} = $stdout;

        if ($exit != 0) {

            $self->write_log("Could not 'qsub' file:\n    $job\n$!\n");

            if ($dependency) {

                $self->write_log("Using dependency:\n    $dependency\n");
            }

            $self->write_log($stderr);

            $self->fail();
        }
    }

    delete $self->{'PBS_jobs'};

    $self->write_log("--- Done ---\n");

    return;
}

################################################################################
sub unique_string {
#
# Returns a unique string
#
    my $self = shift;

    my $string = strftime(

        "%Y%m%d%H%M%S", localtime

    ) . join ("",

        map {

            int ( rand (9) )

        } 1 .. 3
    );

    sleep 1;

    return $string;
}

################################################################################
sub date_spec {
#
# Returns a hash of three common date specs
#
    my $self = shift;

    my $time = time;
    my @localtime = localtime ($time);

    my @timestamps = (

        $time,
        strftime("[%Y-%m-%d %H:%M:%S]", @localtime),
        strftime("%Y%m%d%H%M%S", @localtime),
    );

    return \@timestamps;
}

################################################################################
sub fail {
#
# Kills qOrganise in the event of an error
#
    my $self = shift;

    die join ("\n",

        "Died. Please see the log file for more details:",
        $self->{'log_file'},
    ) . "\n";
}

################################################################################
sub write_log {
#
# Writes logging info
#
    my $self = shift;

    my $content = shift;

    my $fh = $self->{'log_fh'};
    my $io = $self->{'log_io'};

    if ($io->fdopen($fh, "w") && !$self->{'fh_closed'}) {

        $io->print("$content\n");
    }

    return;
}

################################################################################
sub log_die {
#
# Writes logging info to a file and dies
#
    my $self = shift;

    my $content = shift;

    my $fh = $self->{'log_fh'};
    my $io = $self->{'log_io'};

    if ($io->fdopen($fh, "w") && !$self->{'fh_closed'}) {

        $io->print("$content\n");
    }

    die "$content\n";

    return;
}

################################################################################
sub DESTROY {
#
# Define the destructor
#
    my $self = shift;

    my $date = date_spec();

    my $start = $self->{'start_time'}[0];

    my $elapsed_time = $date->[0] - $start;

    my $time = sprintf ("%d days, %d hours, %d minutes and %d seconds",

        int ($elapsed_time / (24 * 60 * 60)),
        ($elapsed_time / (60 * 60)) % 24,
        ($elapsed_time / 60) % 60,
        ($elapsed_time % 60),
    );

    $self->write_log("EXECLOG: Elapsed_time " . $time);
    $self->write_log("EXECLOG: Stop_time " . $date->[1]);
    $self->write_log("EXECLOG: Exit_status $?");

    $self->{'fh_closed'}++;

    $self->{'log_io'}->close;

    if ($? == 0 && $self->{'ingest_prepared'}) {

        $self->ingest_file($self->{'log_file'});
    }

    return;
}

################################################################################
1;

=head1 COPYRIGHT

Copyright (c) The University of Queensland 2014

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
