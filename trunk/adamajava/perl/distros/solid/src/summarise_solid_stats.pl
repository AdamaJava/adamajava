#!/usr/bin/perl -w

##############################################################################
#
#  Program:  summarise_solid_stats.pl
#  Author:   John V Pearson
#  Created:  2010-11-05
#
#  This script recursively parses one or more directories looking for
#  and parsing XML reports created by solid_stats_report.pl.
#
#  $Id: summarise_solid_stats.pl 4670 2014-07-24 10:50:59Z j.pearson $
#
##############################################################################

use strict;
use warnings;

use IO::File;
use Getopt::Long;
use Data::Dumper;
use Pod::Usage;
use XML::Simple;
use XML::LibXML;
use Carp qw( carp croak );

use QCMG::DB::TrackLite;
use QCMG::FileDir::Finder;
use QCMG::Util::Reporter qw( solid_stats_report_summary_1 );
use QCMG::Util::QLog;
use QCMG::SeqResults::Util qw( is_valid_mapset_name is_valid_slide_name
                               is_valid_physdiv_name is_valid_barcode_name );

use vars qw( $SVNID $REVISION $VERSION $VERBOSE %PARAMS );

( $REVISION ) = '$Revision: 4670 $ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id: summarise_solid_stats.pl 4670 2014-07-24 10:50:59Z j.pearson $'
    =~ /\$Id:\s+(.*)\s+/;


###########################################################################
#
# "Cry havoc, and let slip the dogs of war ..."
#

MAIN: {

    # Setup defaults for important variables.

    my @tmp = localtime();
    my $dstring = $tmp[5]+1900 . substr('00'.($tmp[4]+1),-2,2) .
                                 substr('00'.$tmp[3],-2,2);

    my @dirs	= ();
    my $file    = '';  # diagnostic output
    my $outfile = "mapping_stats.$dstring.txt";
    my $logfile = '';
    my $nodb    = '';
       $VERBOSE = 0;
       $VERSION = 0;
    my $help    = 0;
    my $man     = 0;

    # Print usage message if no arguments supplied
    pod2usage(1) unless (scalar @ARGV > 0);

    my $cmdline = join(' ',@ARGV);
    
    # Use GetOptions module to parse commandline options
    my $results = GetOptions (
           'd|dir=s'                  => \@dirs,                  # -d   
           'f|file=s'                 => \$file,                  # -f
           'o|outfile=s'              => \$outfile,               # -o
           'l|logfile=s'              => \$logfile,               # -l
           'n|nodb!'                  => \$nodb,                  # -n
           'v|verbose+'               => \$VERBOSE,               # -v
           'version!'                 => \$VERSION,               # --version
           'h|help|?'                 => \$help,                  # -?
           'man|m'                    => \$man                    # -m
           );

    pod2usage(1) if $help;
    pod2usage(-exitstatus => 0, -verbose => 2) if $man;

    if ($VERSION) {
        print "$SVNID\n";
        exit;
    }

    # Allow for ,-separated lists of dirs
    @dirs = map { split /\,/,$_ } @dirs;

    qlogfile($logfile) if $logfile;
    qlogbegin;
    qlogprint {l=>'EXEC'}, "CmdLine $cmdline\n";
    
    # Warning!
    # Parsing LifeScopt CHT xmls is very expensive because they can be
    # 70Mb or more.  Much better to slurp all XML files and pattern
    # match to distinguish LifeScope/Bioscope runs.  Bioscope can go
    # through full XML parsing but for LifeScope, we will "zero" out
    # <LifeScopeChts>... and parse the rest.

    my $file_count = 0;
    if ($file) {
        # Single file parsing for diagnostic purposes
        my $ra_results = parse_files( [ $file ] );
        $file_count = 1;
    }
    else {
        my @files = ();
        foreach my $dir (@dirs) {
            my $ra_files = parse_directory( $dir );
            push @files, @{ $ra_files } if scalar @{ $ra_files };
        }

        $file_count = scalar @files;

        qlogprint {l=>'TOOL'}, " $file_count solidstats.xml files found\n";
 
        my $ra_results = parse_files( \@files );
        # If output file extension is .xml then output XML
        if ($outfile =~ /\.xml$/i) {
            write_results_to_xml_outfile( $ra_results, $outfile );
        }
        else {
            write_results_to_outfile( $ra_results, $outfile );
        }


        # Skip upload if --nodb option used
        #write_results_to_tracklite( $rh_results ) unless $nodb;
    }

    qlogend;
}


sub parse_directory {
    my $dir = shift;

    my $finder = QCMG::FileDir::Finder->new( verbose => $VERBOSE );
    my $pattern =  'solidstats\.[xX][mM][lL]$';   # case insensitive extension match

    my @files = sort $finder->find_file( $dir, $pattern );
    chomp foreach (@files);

    return \@files;
}


sub parse_files {
    my $ra_files = shift;

    my @mapping_stats = ();
    foreach my $file (@{ $ra_files }) {
        qlogprint("Parsing file $file\n") if $VERBOSE;
        my @new_stats = @{ solid_stats_report_summary_1( $file ) };
        #print Dumper { 'Stats:' => \@new_stats } if ($VERBOSE > 1);
        push @mapping_stats, @new_stats;
    }

    return \@mapping_stats;
}


sub write_results_to_outfile {
    my $ra_results = shift;
    my $outfile    = shift;

    # Write out the Mapping stats table

    my $outfh = IO::File->new( $outfile, 'w' );
    die "Cannot open file $outfile for writing: $!"
        unless defined $outfh;

    $outfh->print( join( "\t", qw( MapSetID Tag TotalTags MappedReads
                                   MappedReadsPercent MappedReadsGbases
                                   UniqueReadsGbases
                                   StartPointsMapped StartPointsUnique
                                   MappedFullLengthNoErrorPC
                                   BioscopeVersion URL 
                                   MapDate ) ), "\n" );

    foreach my $map (@{ $ra_results }) {
        my $url = 'http://grimmond.imb.uq.edu.au/solid_stats_report/' .
                  $map->{xml_filename};
        $outfh->print( join( "\t", $map->{runid},
                                   $map->{tag},
                                   $map->{total_tags},
                                   $map->{mapped_reads},
                                   $map->{mapped_reads_pc},
                                   $map->{mapped_reads_gb},
                                   $map->{unique_reads_gb},
                                   $map->{start_points_map},
                                   $map->{start_points_unq},
                                   $map->{mapped_fulllength_noerror_pc},
                                   $map->{bioscope_vers},
                                   $url,
                                   $map->{mapping_start_date}, ), "\n" );
    }

    # Append summary by month
    my $rh_months = summarise_mapping_by_month( $ra_results );

    $outfh->print( "\n\n", join( "\t", qw( Month MappedReadsGbases
                                           UniqueReadsGbases ) ), "\n" );

    foreach my $month (sort keys %{ $rh_months }) {
        $outfh->print( join( "\t", $month, $rh_months->{$month}->[0],
                                   $rh_months->{$month}->[1] ), "\n" );
    }
}


sub summarise_mapping_by_month {
    my $ra_results = shift;

    my %months = ();
    foreach my $map (@{ $ra_results }) {
        if ($map->{mapping_start_date} =~ /^(\d{4}\-\d{2}).*$/) {
            my $month = $1;
            $months{$month}->[0] += $map->{mapped_reads_gb};
            $months{$month}->[1] += $map->{unique_reads_gb};
        }
        else {
            warn "Cannot pattern match year and date from [",
                 $map->{mapping_start_date}, "] in run ",
                 $map->{runid}, "\n";
        }
    }
    
    return \%months;
}


sub write_results_to_xml_outfile {
    my $ra_results = shift;
    my $outfile    = shift;

    my %params = ( fileid             => 'FileID',
                   runid              => 'RunID',
                   xml_filename       => 'Filename',
                   tag                => 'Tag',
                   total_tags         => 'TotalTags',
                   mapped_reads       => 'MappedReads',
                   mapped_reads_pc    => 'MappedReadsPercent',
                   mapped_reads_gb    => 'MappedReadsGigabase',
                   unique_reads_gb    => 'UniqueReadsGigabase',
                   start_points_map   => 'StartPointsMapped',
                   start_points_unq   => 'StartPointsUnique',
                   mapped_fulllength_noerror_pc
                                      => 'MappedFulllengthNoerrorPercent',
                   bioscope_vers      => 'BioscopeVersion',
                   mapping_start_date => 'MappingStartDate' );

    # Create an XML file from the records

    # Need to create a XML::LibXML::Document and push all of the
    # info in as elements.  Then we call toFH and we're done.

    my $xmldoc  = XML::LibXML::Document->new('1.0','ISO-8859-15');
    my $xmlroot = $xmldoc->createElement('SolidStatsMappingSummary');
    $xmlroot->setAttribute( 'svn_revision', $REVISION );
    $xmlroot->setAttribute( 'start_time', localtime().'' );
    $xmldoc->setDocumentElement( $xmlroot );

    # There are multiple mappings from each solid_stats_report file so
    # in the interests of reduced file size, we'll report the filenames
    # in a single block and reference them from the mapping reports.

    my $ctr = 1;
    my %files = ();
    foreach my $map (@{ $ra_results }) {
        my $pathname = $map->{ xml_pathname };
        if (exists $files{$pathname}) {
            $map->{ fileid } = $files{ $pathname };
        }
        else {
            my $fileid = $ctr++;
            $files{ $pathname } = $fileid;
            $map->{ fileid } = $fileid;
        }
    }

    my $files = XML::LibXML::Element->new( 'ParsedSolidStatsReportFiles' );
    $xmlroot->appendChild( $files );
    foreach my $pathname (sort keys %files) {
        my $el = XML::LibXML::Element->new( 'SolidStatsReportFile' );
        $el->setAttribute( 'fileid', $files{ $pathname } );
        $el->setAttribute( 'filename', $pathname );
        $files->appendChild( $el );
    }

    # Now we report the mappings themselves

    my $maps = XML::LibXML::Element->new( 'MapsetMappings' );
    $xmlroot->appendChild( $maps );

    foreach my $map (@{ $ra_results }) {

        my $mm = XML::LibXML::Element->new( 'MapsetMapping' );
        $maps->appendChild( $mm );

        foreach my $param (sort keys %params) {
            my $el = XML::LibXML::Element->new( $params{$param} );
            $el->appendText( $map->{ $param } );
            $mm->appendChild( $el );
        }

        # Add URL
        my $url = 'http://grimmond.imb.uq.edu.au/solid_stats_report/' .
                  $map->{xml_filename};
        my $el = XML::LibXML::Element->new( 'URL' );
        $el->appendText( $url );
        $mm->appendChild( $el );

        # Add breakdown of slide name
        my $md = XML::LibXML::Element->new( 'MapsetDetails' );
        $mm->appendChild( $md );
        my $mapset_name = $map->{xml_filename};
        $mapset_name =~ s/\.?solidstats.xml$//i;

        warn "no mapset name for ".$map->{filename}."\n"
            unless $mapset_name;

        if (is_valid_mapset_name($mapset_name)) {
            my $mn = XML::LibXML::Element->new( 'MapsetName' );
            $mn->appendText( $mapset_name );
            $md->appendChild( $mn );
            my @tmp = split /\./, $mapset_name;
            warn "invalid slide $tmp[0] in mapset $mapset_name\n"
                unless is_valid_slide_name($tmp[0]);
            warn "invalid physdiv $tmp[1] in mapset $mapset_name\n"
                unless is_valid_physdiv_name($tmp[1]);
            warn "invalid barcode $tmp[2] in mapset $mapset_name\n"
                unless is_valid_barcode_name($tmp[2]);
            my $pd = XML::LibXML::Element->new( 'PhysicalDivision' );
            $pd->appendText( $tmp[1] );
            $md->appendChild( $pd );
            my $bc = XML::LibXML::Element->new( 'Barcode' );
            $bc->appendText( $tmp[2] );
            $md->appendChild( $bc );
            if (is_valid_slide_name($tmp[0])) {
                my $sl = XML::LibXML::Element->new( 'Slide' );
                $sl->appendText( $tmp[0] );
                $md->appendChild( $sl );
                my @fields = split /_/, $tmp[0];
                my $sq = XML::LibXML::Element->new( 'Sequencer' );
                $sq->appendText( $fields[0] );
                $md->appendChild( $sq );
                my $rd = XML::LibXML::Element->new( 'RunDate' );
                $rd->appendText( $fields[1] );
                $md->appendChild( $rd );
                my $sn = XML::LibXML::Element->new( 'SlideNumber' );
                $sn->appendText( $fields[2] );
                $md->appendChild( $sn );
                if (exists $fields[3] and defined $fields[3]) {
                    my $rt = XML::LibXML::Element->new( 'RunType' );
                    $rt->appendText( $fields[3] );
                    $md->appendChild( $rt );
                }
            }
        }
        else {
            warn "invalid mapset name [$mapset_name]\n";
        }
    }

    # Write out the Mapping stats XML file
    open my $outfh, '>', $outfile;
    die "Cannot open XML file $outfile for writing: $!"
        unless defined $outfh;
    binmode $outfh;
    print $outfh $xmldoc->toString(1);
    close $outfh;
}



sub write_results_to_tracklite {
    my $rh_results = shift;

    my $db = QCMG::DB::TrackLite->new;
    $db->connect;

    #my $rh_fields = $db->by_run_sample_number( $PARAMS{run_id} ); 

    #foreach my $key (sort keys %{ $rh_fields }) {
    #    my $rh_field = $rh_fields->{$key};
    #    printf "%-6s",  $rh_field->{ctr};
    #    printf "%-40s", $rh_field->{name};
    #    printf "%s",    (defined $rh_field->{value} ?
    #                     $rh_field->{value} : 'undef');
    #    print "\n";
    #}

}



__END__

=head1 NAME

summarise_solid_stats.pl - Summarise XML files from solid_stats_report.pl


=head1 SYNOPSIS

 summarise_solid_stats.pl [options]


=head1 ABSTRACT

This script will parse all of the XML files in a given directory and
prepare a report showing key mapping and analysis values for each tag
from a run.


=head1 OPTIONS

 -d | --dir           directory containing XML report files
 -f | --file          single file (diagnostic) mode
 -o | --outfile       file for output report (text or XML format)
 -v | --verbose       print diagnostic messages
      --version       print version number
 -? | --help          display help
 -m | --man           display man page


=head1 DESCRIPTION

B<summarise_solid_stats.pl> was designed to support the QCMG
production sequencing team.  It is intended to summarise key data fields
from XML reports created by the solid_stats_report.pl script.  Note that
it will attempt to upload the summary to the QCMG TrackLite database so
this script will exit uncleanly if the database is not present.


=head2 Commandline Options

=over

=item B<-d | --dir>

Root directory of tree where solid_stats_report.pl XML report files are
located.
The directory and all subdirectories will be processed looking for files
that end in I<solidstats.xml>.
QCMG current practice is that solid_stats_reports are generated on barrine 
after mapping and are copied into the patient-specific seq_mapped directories
along with the BAM files.
By recursively processing subdirectories, this script allows the user to
tune the context of the report - it can be per-patient, or per-study or
whole-of-QCMG.

=item B<-f | --file>

In the event of a parsing failure, it can be useful to parse the
problematic file in isolation.  This mode can be paired with higher
levels of B<-v> to get more details about the parse.

=item B<-o | --outfile>

The output file can be in text or XML format.  The text format is a
tab-separated plain text fil ready for import into a spreadsheet.  The
XML format is a summary of key statistics from the individual mapping
reports and it can be used as an input to qvisualise.pl to generate
graphics.  The output format is determined by the name of the output
file - if it ends in .xml then XML is output, otherwise text.

=item B<-l | --logfile>

Log file name (optional).  If no logfile is specified then logging goes
to STDOUT.

=item B<-v | --verbose>

Print progress and diagnostic messages.  This option can be specified
multiple times on the commandline to enable higher levels of verbosity.

=item B<-h | --help>

Display help screen showing available commandline options.

=item B<-m | --man>

Display the full man page (this page).  This is equivalent to doing a
perldoc on the script.

=back

=head2 Example

The following usage example will produce an XML output file containing
details of all mapping for the ICGC pancreatic project:
  
  summarise_solid_stats.pl -d /panfs/seq_results/icgc_pancreatic \
    -o icgc_pancreatic_mapping.xml

The following usage example will produce an XML output file containing
details of all mapping for a single ICGC ovarian donor:
  
  summarise_solid_stats.pl \
    -d /panfs/seq_results/icgc_ovarian/AOCS_001 \
    -o AOCS_001_mapping.xml

This exmaple shows how to generate the monthly mapping report supplied
to Life Technologies showing the throughput of the SOLiD fleet:
  summarise_solid_stats.pl -v \
    -d /mnt/seq_results/icgc_pancreatic/ \
    -d /mnt/seq_results/icgc_ovarian/ \
    -o mapping_stats.20120326.xml

B<N.B.> The spaces between the options (B<-d> etc) and the actual
values are compulsory.  If you do not use the spaces then the value will
be ignored.


=head1 SEE ALSO

=over 2

=item perl

=item QCMG::DB::TrackLite

=back


=head1 AUTHOR

=over 2

=item John Pearson, L<mailto:j.pearson@uq.edu.au>

=back


=head1 VERSION

$Id: summarise_solid_stats.pl 4670 2014-07-24 10:50:59Z j.pearson $


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
