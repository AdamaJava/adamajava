#!/usr/bin/perl -w

##############################################################################
#
#  Program:  rmsk2bed.pl
#  Author:   Matthew Anderson
#  Created:  2011-01-16
#
#  Reads variant files (including MAFs) and creates patient-oriented
#  summaries for use in plotting.
#
#  $Id: rmsk2bed.pl 4669 2014-07-24 10:48:22Z j.pearson $
#
##############################################################################

use strict;
use warnings;

use Carp qw( carp croak confess );
use Data::Dumper;
use Getopt::Long;
use IO::File;
use Pod::Usage;

use QCMG::IO::RmskReader;
use QCMG::Util::QLog;

use vars qw( $SVNID $REVISION $VERSION $VERBOSE );

( $REVISION ) = '$Revision: 4669 $ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id: rmsk2bed.pl 4669 2014-07-24 10:48:22Z j.pearson $' =~ /\$Id:\s+(.*)\s+/;

sub write_bed_header { 
   my $bed_file_handle = shift;

   my $name = 'name="RepeatMasker"';
   my $description = 'description="Repeating Elements by RepeatMasker"';
   my $useScore = 'useScore="1"';
   my $color = 'color="50,50,80"';
   
   local($,) = "\t"; #printing array @bed_header will be tab separated 
   my @bed_header = ("track", $name, $description, $useScore, $color);
   
   print $bed_file_handle @bed_header;
   print $bed_file_handle "\n";
}

sub write_bed_line {
    my $bed_file_handle = shift;
    my $rmask_region_details = shift @_;
            
    my $chrom          = $rmask_region_details->{"chrom"};
    my $chromStart     = $rmask_region_details->{"chromStart"};
    my $chromEnd       = $rmask_region_details->{"chromEnd"};
    my $name           = $rmask_region_details->{"name"};
    my $score          = $rmask_region_details->{"score"};
    my $strand         = $rmask_region_details->{"strand"};
    #my $thickStart     = $rmask_region_details->{"thickStart"};
    #my $thickEnd       = $rmask_region_details->{"thickEnd"};
    #my $itemRgb        = $rmask_region_details->{"itemRgb"};
    #my $blockCount     = $rmask_region_details->{"blockCount"};
    #my $blockSizes     = $rmask_region_details->{"blockSizes"};
    #my $blockStarts    = $rmask_region_details->{"blockStarts"};
    
    my @bed_line = ($chrom, $chromStart, $chromEnd, $name, $score, $strand);
    
    # Write to BED file
    print $bed_file_handle join("\t", @bed_line)."\n";    
}


sub process {
    my $rmsk_file_handle    = shift;
    my $bed_file_handle     = shift;
    my $chromosome_list     = shift @_;
    my %filerterd_chromosomes;
    
    my $count = 0;
    my $written_count = 0;
    my $last_chromosome = "";
    while ( my $rmask_region = $rmsk_file_handle->next_record() ) {
        my %rmask_region_details = (
            "chrom"         => $rmask_region->genoName(),
            "chromStart"    => $rmask_region->genoStart(),
            "chromEnd"      => $rmask_region->genoEnd(),
            "name"          => $rmask_region->repName()." ".$rmask_region->repClass()." ".$rmask_region->repFamily(),
            "score"         => $rmask_region->swScore(),
            "strand"        => $rmask_region->strand()
            #"thickStart"    => "",
            #"thickEnd"      => "",
            #"itemRgb"       => "",
            #"blockCount"    => "",
            #"blockSizes"    => "",
            #"blockStarts"   => ""  
        );
        
        if ( $chromosome_list ){
            my $chromosome = $rmask_region_details{"chrom"};
            if ( $chromosome eq $last_chromosome ) {
                &write_bed_line( $bed_file_handle, \%rmask_region_details);
                $written_count++;
            } elsif ( exists $chromosome_list->{$chromosome} ) {
                &write_bed_line( $bed_file_handle, \%rmask_region_details);
                $written_count++;
                $last_chromosome = $chromosome;
            }else{
                $filerterd_chromosomes{$chromosome}++;
            }
        }else{
            &write_bed_line( $bed_file_handle, \%rmask_region_details);
        }
        
        $count++;
        #last unless $count <= 200000;
    }
    print "\t\tDone\n\n";
    
    print "Lines read: $count \n";
    print "Line written: $written_count \n";
    if ( %filerterd_chromosomes ) {
        print "\nThe following chromosomes and contigs have been filtered out:\n";
        print join("\n", sort keys %filerterd_chromosomes)."\n";
    }     
}


sub get_chromosomes {
    my $chromosomes_file = shift;
    my %chromosomes;
    
    my $chromosomes_file_handle = IO::File->new( $chromosomes_file, 'r' );
    confess 'Unable to open ', $chromosomes_file, "for reading: $!"
        unless defined $chromosomes_file_handle;
    
    while ( defined($_ = $chromosomes_file_handle->getline() ) ) {
        chomp $_;
        my @fields = split "\t", $_;
        my $chromosome_name = $fields[0];
        $chromosomes{$chromosome_name} = "";
        print "$chromosome_name\n";
    }
           
    undef $chromosomes_file_handle;
    return \%chromosomes;
}


MAIN: {
    
    # Setup defaults for important variables.

    my $infile      ='';
    my $outfile     ='';
    my $chromosomes_file = '';
    my $trackline   = 0;
       $VERBOSE     = 0;
       $VERSION     = 0;
    my $help        = 0;
    my $man         = 0;
    my $chromosomes;
    
    # Print usage message if no arguments supplied
    pod2usage(1) unless (scalar @ARGV > 0);
    
    my $results = GetOptions (
        'i|infile=s'        => \$infile,      # -i
        'o|outfile=s'       => \$outfile,       # -o
        'c|chromosomes=s'   => \$chromosomes_file,       # -c
        't|trackline+'     => \$trackline,      # -t
        'v|verbose+'        => \$VERBOSE,       # -v
        'version!'          => \$VERSION,       # --version
        'h|help|?'          => \$help,          # -?
        'man|m'             => \$man            # -m
    );
           
    pod2usage(1) if $help;
    pod2usage(-exitstatus => 0, -verbose => 2) if $man;

    if ($VERSION) {
        print "$SVNID\n";
        exit;
    }
    
    die 'You must supply an infile file name' unless $infile;
    
    # Open Repeat Masker file
    my $infile_handle = QCMG::IO::RmskReader->new( "filename" => $infile );
    # Open output bed file
    my $outfile_handle = IO::File->new( $outfile, 'w' );
    confess 'Unable to open ', $outfile, "for writing: $!"
        unless defined $outfile_handle;
        
    # Create array of valid chromosomes
    if ($chromosomes_file) {
        print "Filtering to only include the following chromosomes and contigs: \n";
        $chromosomes = get_chromosomes ($chromosomes_file);
    }
    
    print "\nProcessing ... \n";
    &write_bed_header ( $outfile_handle ) unless ! $trackline; 
    &process ( $infile_handle, $outfile_handle, \%$chromosomes);
    
    #close file handles
    undef $infile_handle;
    undef $outfile_handle;
}

__END__
