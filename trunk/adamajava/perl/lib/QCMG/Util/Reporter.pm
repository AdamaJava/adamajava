package QCMG::Util::Reporter;

##############################################################################
#
#  Module:   QCMG::Util::Reporter.pm
#  Creator:  John Pearson
#  Created:  2010-12-15
#
#  This non-OO perl module contains static methods for parsing various
#  reports and producing other reports - often summaries.
#
#  $Id: Reporter.pm 4665 2014-07-24 08:54:04Z j.pearson $
#
##############################################################################

use strict;
use warnings;
use Data::Dumper;
use XML::Simple;
use IO::File;
use Carp qw( croak carp );
use vars qw( $REVISION $SVNID @ISA @EXPORT @EXPORT_OK );

use QCMG::Util::QLog;

( $REVISION ) = '$Revision: 4665 $ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID )    = '$Id: Reporter.pm 4665 2014-07-24 08:54:04Z j.pearson $'
                 =~ /\$Id:\s+(.*)\s+/;

BEGIN {
    use Exporter ();
    @ISA = qw(Exporter);
    # Optionally exported functions
    @EXPORT_OK = qw( solid_stats_report_summary_1 );
}

sub solid_stats_report_summary_1 {
    my $pathname = shift;

    # Slurp file so we can do some pattern matching before passing it
    # all over to be XML parsed.
    local( *FH ) ;
    open( FH, $pathname ) or die "Cannot open file: $pathname\n";
    my $contents = do { local( $/ ) ; <FH> } ;

    # Decide if we're dealing with Bioscope or LifeScope
    if ($contents =~ /\<LifeScopeDirectory/) {
        return _parse_lifescope_solidstats( $pathname, $contents );
    }
    else {
        return _parse_bioscope_solidstats( $pathname, $contents );
    }
}


sub _parse_lifescope_solidstats {
    my $pathname = shift;
    my $contents = shift;

    # Throw away any <LifeScopeChts>... stuff
    while ($contents =~ /\<LifeScopeChts\>/) {
        $contents =~ s/\<LifeScopeChts\>.*\<\/LifeScopeChts\>//sm;
    }

    # DO NOT (!!!) screw with the ForceArray spec here.  It is very
    # finely balanced to handle the normal short form and weird long
    # form summary .tbl files for LifeScope and if you mess with it the
    # summarise_solid_stats.pl script is likely to barf.

    my $xs = XML::Simple->new();
    my $xmlo = $xs->XMLin( $contents, ForceArray => [ 'LifeScopeDirectory',
                                                      'LifeScopeTbls',
                                                      'header',
                                                      'tr' ],
                                      KeyAttr => { } );

    my $filename = $pathname;
    $filename =~ s/.*\///g;  # strip any leading path off the filename

    # summary table XML structure

    my $runid = $xmlo->{runid};
    my @mapping_stats = ();
    my $lt_ctr = 0;
    foreach my $ld (@{ $xmlo->{LifeScopeDirectory} }) {
        foreach my $lt (@{ $ld->{LifeScopeTbls} }) {
            my $tb = $lt->{LifeScopeTbl};

            # We need a filename and it must be *-summary.tbl
            next unless (exists $tb->{file} and
                         $tb->{file} =~ /.*\-summary\.tbl$/);

            # We need to do some special parsing for the weird "long form"
            # tables which we can spot because the parser handles them
            # incorrectly and creates a table with a single header.
            my @cols = @{$tb->{table}->{headers}->{header}};

            if (scalar(@cols) == 1) {
                warn "Parsing \"long form\" summary table tbl ".
                     $tb->{file} ."\n";

                # Do Tags 1 and 2
                foreach my $tag ( qw( Tag1 Tag2 ) ) {
                    my $rh_tag = _parse_long_lifescope_summary_table($tb,$tag);
                    if (defined $rh_tag) {
                        # We never have mapping dates for LifeScope so
                        # use the slide date out of the XML filename.
                        if ($filename =~ /_(20\d{2})(\d{2})\d{2}_/) {
                            $rh_tag->{mapping_start_date} = $1 .'-'. $2;
                        }

                        $rh_tag->{xml_filename} = $filename; 
                        $rh_tag->{xml_pathname} = $pathname; 
                        push @mapping_stats, $rh_tag;
                    }
                }

                $lt_ctr++;  # only count Tables that we actually process
            }
            elsif (scalar(@cols) > 1) {
                my @rows = @{$tb->{table}->{data}->{tr}};

                # A LifeScope stats report can contain multiple rows because
                # if the same library is in more than one lane/barcode, all
                # of the mapsets are considered part of the same "Group" and
                # they get a single report directory so they end up in a
                # single SolidStats XML file.

                foreach my $row (@rows) {
                    my @vals = @{$row->{td}};

                    my %mapping = ();
                    foreach my $col (@cols) {
                        # ForceArray on headers/header means we get an array
                        # of hashes with a single 'header' key that points
                        # to the hash of info we actually want.  Sigh.
                        my $name = $col->{name};
                        if ($name =~ /\((.*?)\;.*/) {
                            $name = $1;
                        }
                        $name =~ s/\s+/\_/g;
                        $mapping{ $name } = { id   => $name,
                                              name => $col->{name},
                                              ctr  => $col->{ctr},
                                              val  => $vals[ $col->{ctr} ] };
                    }

                    #print Dumper $lt;

                    # Do Tags 1 and 2
                    foreach my $tag ( qw( Tag1 Tag2 ) ) {
                        my $rh_tag =
                            _parse_lifescope_summary_table( \%mapping, $tag );
                        if (defined $rh_tag) {
                            # We never have mapping dates for LifeScope so
                            # use the slide date out of the XML filename.
                            if ($filename =~ /_(20\d{2})(\d{2})\d{2}_/) {
                                $rh_tag->{mapping_start_date} = $1 .'-'. $2;
                            }

                            $rh_tag->{xml_filename} = $filename; 
                            $rh_tag->{xml_pathname} = $pathname; 
                            push @mapping_stats, $rh_tag;
                        }
                    }
                }
                $lt_ctr++;  # only count Tables that we actually process
            }

        }
    }

    if (! $lt_ctr) {
        qlogprint {l=>'WARN'}, "No mapping data in LifeScope file $pathname\n";
    }
    return \@mapping_stats;
}

sub _empty_mapping_data_structure {
    # Get an empty data struture ready
    return ( runid            => 0,
             tag              => '',
             bioscope_vers    => 'LifeScope',
             total_tags       => 0,
             mapped_reads     => 0,
             mapped_reads_pc  => 0,
             mapped_reads_mb  => 0,
             mapped_reads_gb  => 0,
             unique_reads_mb  => 0,
             unique_reads_gb  => 0,
             start_points_map => 0,
             start_points_unq => 0,
             input_ma_file    => '',
             source_filename  => '',
             xml_filename     => '',
             xml_pathname     => '',
             start_points_map   => 0,
             start_points_unq   => 0,
             mapped_fulllength_noerror_pc => 0,
             mapping_start_date => '2099-12-31 00:00:00.000  EST'
           );
}


sub _parse_long_lifescope_summary_table {
    my $tb = shift;
    my $tag = shift;

    my @tds = map { $_->{td} }
              @{ $tb->{table}->{data}->{tr} };
    my $text = join( "\n", @tds );

    # Sanity check - ditch if the requested tag is not present
    my $tag_nm = $tag . '-NumMapped';
    my $tag_al = $tag . '-AlignmentLength';
    unless ( $text =~ /$tag_nm/ and $text =~ /$tag_al/ ) {
       qlogprint {l=>'WARN'}, "Could not find data for tag $tag\n";
       return undef;
    }

    # Get an empty data struture ready
    my %data = _empty_mapping_data_structure();

    # Populate the data structure
    my $tag_length=0;
    if ($text =~ /^ReadLength\s+(\d+)x(\d+)$/m) {
        $tag_length = ($tag eq 'Tag1') ? $1 : $2;
    }

    my $avg_mapped_tag_length;
    if ($text =~ /^$tag_al\s+([\-\w]+).*?Avg\s+([\.\d]+)$/ms) {
        $data{tag} = $1;
        $avg_mapped_tag_length = $2;
    }

    if ($text =~ /^NumFragmentsPassingFilters\s+(\d+)$/m) {
        $data{total_tags} = $1;
    }

    if ($text =~ /^$tag_nm\s+(\d+)$/m) {
        $data{mapped_reads} = $1;
    }

    if ($text =~ /^$tag\- % total Mapped\s+([\.\d]+)$/m) {
        $data{mapped_reads_pc} = $1;
    }

    my $bam_tmp = $tb->{table}->{headers}->{header}->[0]->{name};
    if ($bam_tmp =~ /^BamFileName\s+(.*)$/) {
        $data{source_filename} = $1;
    }

    $data{mapped_reads_mb} = $avg_mapped_tag_length * $data{mapped_reads}
                             / 1000000;
    $data{mapped_reads_gb} = sprintf "%.2f", ($data{mapped_reads_mb}/1000);

    return \%data;
}


sub _parse_lifescope_summary_table {
    my $dat = shift;
    my $tag = shift;

    # Sanity check - ditch if the requested tag is not present
    my $tag_nm = $tag . '-NumMapped';
    my $tag_al = $tag . '-AlignmentLength';
    unless ( exists $dat->{$tag_nm} and exists $dat->{$tag_al} ) {
       qlogprint {l=>'WARN'}, "Could not find data for tag $tag\n";
       return undef;
    }

    # Get an empty data strcuture ready
    my %data = _empty_mapping_data_structure();

    # Populate the data structure
    $data{total_tags} = $dat->{ 'NumFragmentsPassingFilters' }->{val} ; 
    $data{mapped_reads} = $dat->{ $tag_nm }->{val};
    $data{mapped_reads_pc} = $dat->{ $tag .'-_%_filtered_mapped' }->{val};
    $data{source_filename} = $dat->{ 'BamFileName' }->{val};

    my $alignment = $dat->{ $tag_al }->{val} ; 
    $alignment =~ s/[\(\)]+//g;  # ditch enclosing parens
    my @fields = split /\;/, $alignment;
    $data{tag} = $fields[0];
    $data{mapped_reads_mb} = $fields[3] * $data{mapped_reads} / 1000000;

    $data{mapped_reads_gb} = sprintf "%.2f", ($data{mapped_reads_mb}/1000);
    $data{unique_reads_gb} = sprintf "%.2f", ($data{unique_reads_mb}/1000);

    return \%data;
}



sub _parse_bioscope_solidstats {
    my $pathname = shift;
    my $contents = shift;

    my $xs = XML::Simple->new();
    my $xmlo = $xs->XMLin( $contents, ForceArray => [ 'MappingStatsReport',
                                                      'BioscopeLogDirectory',
                                                      'BioscopeLogModule' ],
                                      KeyAttr => { } );
    my $filename = $pathname;
    $filename =~ s/.*\///g;  # strip any leading path off the filename

    my $runid = $xmlo->{runid};
    my @results = ();

    # This report is a two-stager: first we have to parse the mapping
    # stats reports, and secondly we have to parse the bioscope logs to
    # try to get an execution start-time for all successful mappings and
    # then try to match up (2) against (1).  Sounds easy doesn't it ...

    # (1) Parse out summary stats from mapping reports

    my @mapping_stats = ();
    foreach my $ma (@{ $xmlo->{MappingStatsReport} }) {
        my $tag = 'F3';  # Default tag is an F3
        if ($ma->{file} =~ /\/\d+\/([^\/]+)\/s_mapping/) {
             $tag = $1;
        }
        my $mi = $ma->{MappingInfo};
        my %data = ( runid            => $runid,
                     tag              => $tag,
                     bioscope_vers    => $mi->{BioscopeVersion},
                     total_tags       => $mi->{TotalTags},
                     mapped_reads     => $mi->{MappedReads},
                     mapped_reads_pc  => $mi->{MappedReadsPercentage},
                     mapped_reads_mb  => $mi->{MappedReadsMegabases},
                     unique_reads_mb  => $mi->{UniqueReadsMegabases},
                     start_points_map => $mi->{StartPointsFromMappedReads},
                     start_points_unq => $mi->{StartPointsFromUniqueReads},
                     input_ma_file    => $mi->{InputMaFile},
                     source_filename  => $ma->{file},
                     xml_filename     => $filename,
                     xml_pathname     => $pathname,
                   );

        # Do some transforms to convert Mb into GB etc 
        $data{mapped_reads_gb} = sprintf "%.2f", ($data{mapped_reads_mb}/1000);
        $data{unique_reads_gb} = sprintf "%.2f", ($data{unique_reads_mb}/1000);

        # Look through the mapping table to find the percent of
        # reads that mapped at full-length with no errors.
        my @mmrs = @{ $ma->{Table} };
        foreach my $mmr (@mmrs) {
            # We want 'MappedReads' table, not 'UniquelyPlacedReads';
            next unless ($mmr->{name} =~ /MappedReads/);
            my @rows = @{ $mmr->{Data}->{Row} };

            # Find the biggest length, i.e. "full-length"
            my $maxlen = 0;
            foreach my $row (@rows) {
                 $maxlen = ( $row->{Cell}->[0] > $maxlen ) ?
                             $row->{Cell}->[0] : $maxlen;
            }

            # Now use 'full-length' to get the actual values
            foreach my $row (@rows) {
                 next unless ( $row->{Cell}->[0] == $maxlen and
                               $row->{Cell}->[1] == 0);
                 $data{mapped_fulllength_length}        = $maxlen;
                 $data{mapped_fulllength_noerror_count} = $row->{Cell}->[2];
                 $data{mapped_fulllength_noerror_pc}    = $row->{Cell}->[3];
            }
        } 

        push @mapping_stats, \%data;
    }


    # (2) Get execution start times for all successful mapping runs

    my @mapping_logs = ();
    foreach my $bld (@{ $xmlo->{BioscopeLogDirectory} }) {
        foreach my $blm (@{ $bld->{Modules}->{BioscopeLogModule} }) {
                
            # We only want to process successful mapping modules
            next unless ($blm->{type} =~ /^Mapping.*/);
            next unless ($blm->{ModuleInfo}->{ExecutionStatus} eq 'SUCCESS');

            my %data = ( timestamp  => $blm->{timestamp},
                         main_file  => $blm->{ModuleInfo}->{MainFile},
                         start_date => $blm->{ModuleInfo}->{ExecutionStart},
                       );

            ## Pull out just the date from the start datetimestamp.
            #if ($data{start_date} =~ /^([\d\-]+) .*/) {
            #    $data{date} = $1;
            #}
            #else {
            #    croak "Could not parse date out of timestamp";
            #}

            # Grab mapping dir so we can match this log against mapping stats
            foreach my $p (@{ $blm->{Parameters}->{Parameter} }) {
                next unless ($p->{name} eq 'mapping.output.dir'); 
                $data{mapping_output_dir} = $p->{value};
                last;  # don't bother to keep looking once we've found it
            }

            push @mapping_logs, \%data;
        }
    }

    # Now match everything up.  This is tricky - there could be multiple
    # successfull mapping runs for each mapping stats so iterate across
    # mapping stats, looking for the latest timestamp mapping log where
    # the mapping.output.dir matches the stem of the input_ma_file !!!

    foreach my $rh_map_stats (@mapping_stats) {
        my @matching_logs = ();
        foreach my $rh_map_logs (@mapping_logs) {
            if ($rh_map_stats->{input_ma_file} =~
                /^$rh_map_logs->{mapping_output_dir}/) {
                push @matching_logs, $rh_map_logs;
            }
        }

        # Now we use a Schwartzian transform to sort by start_date
        my @sorted_logs = map  { $_->[1] }
                          sort { $a->[0] cmp $b->[0] }
                          map  { [ $_->{start_date}, $_ ] } @matching_logs;

        # If there were no matching mapping logs (maybe the bioscope
        # logs were not available) then set a date in the far future
        # otherwise shift off the first (smallest, i.e. first) date
        if (scalar @matching_logs == 0) {
            $rh_map_stats->{ mapping_start_date } =
                '2099-12-31 00:00:00.000  EST'
        }
        else {
            my $rh_map_log = shift @sorted_logs;
            $rh_map_stats->{ mapping_start_date } = $rh_map_log->{ start_date };
        }

        if (scalar @matching_logs > 1) {
            warn 'Multiple bioscope mappings [',
                  scalar(@matching_logs) ,'] found for mapset ',
                  $rh_map_stats->{runid}, ' tag ',
                  $rh_map_stats->{tag}, "\n";
        }

    }

    return \@mapping_stats;
}


1;
__END__


=head1 NAME

QCMG::Util::Reporter - Perl module containing static reporting functions


=head1 SYNOPSIS

 use QCMG::Util::Reporter qw( solid_stats_report_summary_1 );


=head1 DESCRIPTION

This module is not an OO class, rather it contains a collection of
static methods associated with processing QCMG reports.  To use any of
the functions described below, you will need to use the 'import'
notations shown above to make the functions visible in your programs
MAIN namespace.


=head1 FUNCTIONS

=over

=item B<solid_stats_report_summary_1()>

 my $rh_stats = solid_stats_report_summary_1( $fh );


=head1 AUTHORS

=over

=item John Pearson L<mailto:j.pearson@uq.edu.au>

=back


=head1 VERSION

$Id: Reporter.pm 4665 2014-07-24 08:54:04Z j.pearson $


=head1 COPYRIGHT

Copyright (c) The University of Queensland 2010-2012

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
