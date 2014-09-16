package QCMG::Organise::IngestSlide;
################################################################################
#
#    IngestSlide.pm
#
#    Common methods to ingest a QCMG-style slide into LiveArc
#
#    Authors: Stephen Kazakoff
#
#    $Id: IngestSlide.pm 4663 2014-07-24 06:39:00Z j.pearson $
#
################################################################################
# # #
# #       SET DEFAULTS
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
sub ingest_datafiles {
#
# Ingests a hash of checked files
#
    my $self = shift;

    my $list = $self->{'checksums'};

    for (natkeysort { $_ } keys %$list) {

        my $prefix;
        my $extension;

        my $BAM_prefixes = $self->{'BAM_prefixes'}{$_};
        my $FASTQ_prefixes = $self->{'FASTQ_prefixes'}{$_};

        if ($BAM_prefixes) {

            $prefix = $BAM_prefixes;
            $extension = ".bam";
        }

        if ($FASTQ_prefixes) {

            $prefix = $FASTQ_prefixes;
            $extension = ".fastq.gz";
        }

        $self->ingest_file(

            $_,

            $prefix ? $prefix . $extension : basename($_),

            [
                $list->{$_}{'crc'},
                $list->{$_}{'size'},
            ],
        );
    }

    return;
}

################################################################################
sub ingest_folder {
#
# Performs directory ingestion into LiveArc
#
    my $self = shift;

    my $folder = shift;

    $self->write_log("--- Ingesting $folder ---");

    my $base = basename($folder);

    my $slide = join ("_",

        (split ("_", $self->{'slide'}))[0..3]
    );

    my $folder_name = ($base =~ /^$slide/ ? $base : "$slide\_bwa_$base");

    my $asset_import = join (" ",

        $self->{'LA_execute'} .
        "import",
        "-namespace " . $self->{'namespace'},
        "-archive 1",
        "-name " . $folder_name,
        $folder,
    );

    my $res = $self->exec_LA_command($asset_import);

    $res =~ s/[^ ]* *//;

    $self->write_log("Successfully $res into LiveArc.");

    $self->write_log("--- Done ---\n");

    return;
}

################################################################################
sub ingest_file {
#
# Performs file ingestion into LiveArc
#
    my $self = shift;

    my ($file, $name, $meta) = @_;

    my $base = basename($file);

    $self->write_log("--- Ingesting $base ---");

    $base = $name if $name;

    my $ns = $self->{'namespace'};

    my $asset_exists = $self->{'LA_execute'} . "asset.exists :id path=$ns/$base";
    my $asset_get = $self->{'LA_execute'} . "asset.get :id path=$ns/$base";
    my $asset_create = $self->{'LA_execute'} . "asset.create :namespace $ns :name $base";

    my ($id, $vs);

    my $res1 = $self->exec_LA_command($asset_exists);

    if ((split ('"', $res1))[-1] eq "true") {

        $self->write_log("Found: $ns/$base");

        my $res2 = $self->exec_LA_command($asset_get);

        my %meta = map {

            s/[^:]+://;

            my @fields = split;

            shift @fields => join (" ", @fields)
        }
        split ("\n", $res2);

        $id = (split ('"', $meta{'asset'}))[1];
        $vs = (split ('"', $meta{'asset'}))[3];

        $self->write_log("Found an asset with id: $id, and version: $vs");
    }
    else {

        $self->write_log("Creating asset: $ns/$base");

        my $res3 = $self->exec_LA_command($asset_create);

        $id = (split ('"', $res3))[-1];

        $self->write_log("Asset with id: $id, is now at version: 1");
    }

    my $metadata = $meta ? join (" ",
        "",
        ":meta \\<",
            ":checksums \\<",
                ":source orig",
                ":checksum \\<",
                    ":chksum " . $meta->[0],
                    ":filesize " . $meta->[1],
                    ":filename $base",
                "\\>",
            "\\>",
        "\\>",
    ) : "";

    my $res4 = $self->exec_LA_command(

        $self->{'LA_execute'} . join (" ",
            "asset.set",
            ":id $id",
            ":in file:$file",
        ) . $metadata
    );

    $res4 = (split ('"', $res4))[-1];

    $self->write_log("Asset with id: $id, is now at version: $res4");

    $self->write_log("--- Done ---\n");

    return;
}

################################################################################
sub exec_LA_command {
#
# Runs a LiveArc command; re-attempt every 15 minutes and fail after 10 days
#
    my $self = shift;

    my $cmd = shift;

    my @results = (undef, undef, 1);

    my $counter = 1;

    my $sleep_time = 15;
    my $fail_after = 10;

    my $time = $sleep_time;

    do {

        @results = capture { system ($cmd) };

        if ($results[2] != 0) {

            $self->write_log("Error: Could not execute:\n$cmd");
            $self->write_log("Will re-attempt execution in $sleep_time minutes.\n");

            $counter++;   
 
            sleep ($sleep_time * 60);
        }

        $time += $sleep_time;
    }
    until $results[2] == 0 || $time >= ($fail_after * 24 * 60);

    if ($results[2] != 0) {

        $self->write_log("Error: Could not execute:\n$cmd");
        $self->write_log("Failed after $counter retries.");

        $self->fail();
    }
    else {

        (my $log = $cmd) =~ s/.*--app //;

        $log =~ s/ :(meta .*)/\n$1/;

        $self->write_log($log);
    }

    chomp $results[0];

    return $results[0];
}

################################################################################
sub create_namespace {
#
# Make sure LiveArc is ready to accept ingestion
#
    my $self = shift;

    my $ns = $self->{'namespace'};

    $self->write_log("--- Creating Namespace $ns ---");

    my @dirs = File::Spec->splitdir($ns);

    for (1 .. $#dirs) {

        my $dir = File::Spec->catdir(@dirs[1 .. $_]);

        my $dir_exists = $self->{'LA_execute'} . "asset.namespace.exists :namespace $dir";
        my $dir_create = $self->{'LA_execute'} . "asset.namespace.create :namespace $dir";

        my $res1 = $self->exec_LA_command($dir_exists);

        if ((split ('"', $res1))[-1] eq "true") {
    
            $self->write_log("Found: $dir");
        }
        else {

            $self->exec_LA_command($dir_create);

            $self->write_log("Created namespace: $dir");
        }
    }

    $self->{'ingest_prepared'} = 1;

    $self->write_log("--- Done ---\n");

    return;
}

################################################################################
sub calculate_checksums {
#
# Calculates CRC and file sizes
#
    my $self = shift;
 
    $self->write_log("--- Calculating checksums ---");
 
    $self->{'checksums'} = { 

        map {
 
            my $name = basename($_);
 
            my ($crc, $size) = map {

                split
            }
            capture {

                system ("cksum", $_)
            };
 
            $self->write_log("Stats: $name => $crc, $size");
 
            $_ => {
                'crc' => $crc,
                'size' => $size,
            }
        }
        @{ $self->{ $self->{'file_type'} } }
    };
 
    $self->write_log("--- Done ---\n");
 
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
