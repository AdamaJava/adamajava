package QCMG::Organise::ScrapeSchema;
################################################################################
#
#    ScrapeSchema.pm
#
#    Common methods to harvest a QCMG-style slide's meta
#
#    Authors: Stephen Kazakoff
#
#    $Id$
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
sub create_connection {
#
# Checks for config files and opens up a connection to Geneus
#
    my $self = shift;

    my $home = $ENV{'HOME'};

    $ENV{'PGSYSCONFDIR'} = "$home/.geneus";

    if (! -e "$home/.pgpass") {

        if ($self->{'log_fh'} && $self->{'log_io'}) {

            $self->write_log("Invalid `.pgpass` file. Exiting.\n");

            $self->fail();
        }
        else {

            die "Invalid `.pgpass` file. Exiting.\n";
        }
    }

    if (! -e "$home/.geneus/pg_service.conf") {

        if ($self->{'log_fh'} && $self->{'log_io'}) {

            $self->write_log("Invalid `pg_service.conf` file. Exiting.\n");

            $self->fail();
        }
        else {

            die "Invalid `pg_service.conf` file. Exiting.\n";
        }
    }

    my $dbh = DBI->connect(

        'DBI:Pg:service=QCMG',
        undef,
        undef,
        {
            AutoCommit => 0,
            RaiseError => 1,
            PrintError => 0,
        }

    ) or die DBI->errstr;

    return $dbh;
}

################################################################################
sub get_project_list {
#
# Retrives a list of past projects
#
    my $self = shift;

    my $connection = $self->create_connection();

    my $query = qq{
        SELECT
        filename
        FROM
        external_data_file
    };

    my $cursor = $connection->prepare($query);

    my $fetch_datas;

    if ($cursor->execute) {

        $fetch_datas = $cursor->fetchall_arrayref
    }
    else {

        if ($self->{'log_fh'} && $self->{'log_io'}) {

            $self->write_log("Cannot execute query: $query\n");

            $self->fail();
        }
        else {

            die "Cannot execute query: $query\n";
        }
    }

    $cursor->finish;

    $connection->disconnect;

    unless ($fetch_datas) {

        if ($self->{'log_fh'} && $self->{'log_io'}) {

            $self->write_log("Could not retrive from table 'external_data_file'.\n");

            $self->fail();
        }
        else {

            die "Could not retrive from table 'external_data_file'.\n";
        }
    }

    return [ map { @$_ } @$fetch_datas ];
}

################################################################################
sub get_external_data {
#
# Retrieves external data associations
#
    my $self = shift;

    my $connection = $self->create_connection();

    my $query = qq{
        SELECT
        ed.*, efr.runnumber
        FROM
        external_data ed 
        JOIN
        external_data_file edf 
        ON
        ed.fileid=edf.fileid
        JOIN
        external_flowcell_run efr
        ON
        efr.flowcell = ed.flowcell
        WHERE
        filename = '$main::csv';
    };

    my $cursor = $connection->prepare($query);

    my ($field_names, $fetch_datas);

    if ($cursor->execute) {

        $field_names = $cursor->{'NAME'};

        $fetch_datas = $cursor->fetchall_arrayref;
    }
    else {

        $self->write_log("Cannot execute query: $query\n");

        $self->fail();
    }

    $cursor->finish;

    $connection->disconnect;

    unless (@$field_names && @$fetch_datas) {

        $self->write_log("Project does not exist in schema: $main::csv\n");

        $self->fail();
    }

    map {

        my %hash;

        @hash{ @$field_names } = @$_;

        map {

            $hash{$_} = ""
        }
        grep {

            !$hash{$_}
        }
        keys %hash;

        my $slide = join ("_",

            $main::date,
            "EXTERN",
            sprintf("%04d", $hash{'runnumber'}),
            $hash{'flowcell'}
        );

        my $mapset = join (".",

            $slide,
            "lane_" . $hash{'lane'},
            $hash{'index'},
        );

        $self->{'external_data'}{$mapset} = \%hash;
        $self->{'slides'}{$slide}++
    }
    @$fetch_datas;

    my $count = scalar @$fetch_datas;

    $self->write_log("Found: $count sample name and mapset associations.\n");

    return;
}

################################################################################
sub get_all_slide_data {
#
# Searches Geneus for slide metadata
#
    my $self = shift;

    my $query = qq{
        SELECT
        project.parent_project AS "parent_project",
        regexp_replace( project.name, '-.*', '' ) AS "parent_prefix",
        replace( replace( project.name, '-', '_'), '.', '_' ) AS "name",
        sample.name AS "sample_name",
        sample.sample_code AS "sample_code",
        sample.biospecimen_id AS "biospecimen_id",
        sample.tissue_of_origin AS "tissue_of_origin",
        sample.alignment_required AS "alignment_required",
        sample.material AS "material",
        sample.description AS "description",
        regexp_replace( mapset.name, '([^_]*_)\{3\}([^.]*).*', E'\\\\2') AS "flowcell",
        regexp_replace( mapset.name, E'.*\\\\.', '') AS "index",
        regexp_replace( mapset.name, '.*_([1-8]).*', E'\\\\1') AS "lane",
        mapset.name AS "mapset",
        mapset.primary_library AS "primary_library",
        mapset.library_protocol AS "library_protocol",
        mapset.capture_kit AS "capture_kit",
        mapset.sequencing_platform AS "sequencing_platform",
        sample.species_reference_genome AS "species_reference_genome",
        genome.genome_file AS "genome_file",
        coalesce(gfflib.gff_file, gffcapture.gff_file, gffspecies.gff_file) AS "gff_file"
        FROM qcmg.project
        INNER JOIN qcmg.sample on sample.projectid = project.projectid
        INNER JOIN qcmg.mapset on mapset.sampleid = sample.sampleid
        LEFT JOIN qcmg.genome ON genome.species_reference_genome = sample.species_reference_genome
        LEFT JOIN qcmg.gff gfflib ON mapset.library_protocol ~ gfflib.library_protocol
        LEFT JOIN qcmg.gff gffcapture ON mapset.capture_kit = gffcapture.capture_kit
        LEFT JOIN qcmg.gff gffspecies ON sample.species_reference_genome = gffspecies.species_reference_genome
        WHERE mapset.name like ?
    };

    for my $slide (sort keys %{ delete $self->{'slides'} }) {

        $self->write_log("Querying: $slide");

        my $connection = $self->create_connection();

        my $cursor = $connection->prepare($query);

        $cursor->bind_param(1, "$slide.%.%");

        my ($field_names, $fetch_datas);

        if ($cursor->execute) {

            $field_names = $cursor->{'NAME'};

            $fetch_datas = $cursor->fetchall_arrayref;
        }
        else {

            $self->write_log("Cannot execute query: $query\n");

            $self->fail();
        }

        $cursor->finish;

        $connection->disconnect;

        unless (@$field_names && @$fetch_datas) {

            $self->write_log("Slide not in LIMS: " . $self->{'slide'} . "\n");

            $self->fail();
        }

        map {

            my %hash;

            @hash{ @$field_names } = @$_;

            map {

                $hash{$_} = ""
            }
            grep {

                !$hash{$_}
            }
            keys %hash;

            my $mapset = delete $hash{'mapset'};

            $self->{'slides'}{$slide}{$mapset} = \%hash
        }
        @$fetch_datas;

        my $count = scalar @$fetch_datas;

        $self->write_log("Found: $count sample name and mapset associations.\n");
    }
    return;
}

################################################################################
sub get_parent_paths {
#
# Collects parent and path associations from Geneus
#
    my $self = shift;

    my $connection = $self->create_connection();

    my $query = qq{
        SELECT
        prefix,
        parent_path
        FROM project_prefix_map
    };

    my $cursor = $connection->prepare($query);

    my $fetch_datas;

    if ($cursor->execute) {

        $fetch_datas = $cursor->fetchall_arrayref
    }
    else {

        $self->write_log("Cannot execute query: $query\n");

        $self->fail();
    }

    $cursor->finish;

    $connection->disconnect;

    unless ($fetch_datas) {

        $self->write_log("Slide not in LIMS: " . $self->{'Slide data'} . "\n");

        $self->fail();
    }

    $self->{'parent_paths'} = {

        map {

            $_->[0] => $_->[1]
        }
        @$fetch_datas
    };

    my $count = scalar @$fetch_datas;

    $self->write_log("Found: $count prefix and parent path associations.\n");

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
