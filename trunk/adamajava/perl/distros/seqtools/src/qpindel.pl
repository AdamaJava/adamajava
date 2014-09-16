#!/usr/bin/perl -w

##############################################################################
#
#  Program:  qpindel.pl
#  Author:   John V Pearson
#  Created:  2012-08-08
#
#  Tasks associated with running and post-processing data from the
#  pindel variant caller.
#
#  $Id: qpindel.pl 4669 2014-07-24 10:48:22Z j.pearson $
#
##############################################################################

use strict;
use warnings;

use Carp qw( carp croak verbose );
use Data::Dumper;
use Getopt::Long;
use IO::File;
use Pod::Usage;
use XML::LibXML;

use QCMG::FileDir::Finder;
use QCMG::IO::INIFile;
use QCMG::IO::PindelReader;
use QCMG::Util::QLog;
use QCMG::Util::FileManipulator;

use vars qw( $SVNID $REVISION $CMDLINE );

( $REVISION ) = '$Revision: 4669 $ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id: qpindel.pl 4669 2014-07-24 10:48:22Z j.pearson $'
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

    my @valid_modes = qw( help man version filter pipeline test );

    if ($mode =~ /^help$/i or $mode =~ /\?/) {
        pod2usage( -exitval  => 0,
                   -verbose  => 99,
                   -sections => 'SYNOPSIS|COMMANDS' );

    }
    elsif ($mode =~ /^man$/i) {
        pod2usage( -exitval  => 0,
                   -verbose  => 2 );

    }
    elsif ($mode =~ /^version$/i) {
        print "$SVNID\n";
    }
    elsif ($mode =~ /^filter$/i) {
        filter();
    }
    elsif ($mode =~ /^pipeline$/i) {
        pipeline();
    }
    elsif ($mode =~ /^test$/i) {
        test();
    }
    else {
        die "qpindel mode [$mode] is unrecognised; valid modes are: " .
            join(' ',@valid_modes) ."\n";
    }
}


sub filter {
    my $class = shift;

    pod2usage( -exitval  => 0,
               -verbose  => 99,
               -sections => 'COMMAND DETAILS/FILTER' )
        unless (scalar @ARGV > 0);

    # Setup defaults for important variables.

    my %params = ( infiles  => [],
                   dir      => '',
                   stem     => '',
                   outfile  => '',
                   logfile  => '',
                   min1     => 0,
                   max1     => 1000000,
                   min2     => 0,
                   max2     => 1000000,
                   verbose  => 0 );

    my $results = GetOptions (
           'i|infile=s'           =>  $params{infiles},       # -i
           'd|dir=s'              => \$params{dir},           # -d
           's|stem=s'             => \$params{stem},          # -s
           'o|outfile=s'          => \$params{outfile},       # -o
             'min1=i'             => \$params{min1},
             'max1=i'             => \$params{max1},
             'min2=i'             => \$params{min2},
             'max2=i'             => \$params{max2},
           'l|logfile=s'          => \$params{logfile},       # -l
           'v|verbose+'           => \$params{verbose},       # -v
           );

    # Set up logging
    qlogfile($params{logfile}) if $params{logfile};
    qlogbegin();
    qlogprint( {l=>'EXEC'}, "CommandLine $CMDLINE\n");

    # It is mandatory to supply an infile or directory
    die "You must specify an input file (-i) or directory (-d) and stem (-s)\n"
        unless ( ($params{dir} and $params{stem}) or
                 scalar( @{ $params{infiles} } ) );

    die "No output file specified\n" unless $params{outfile};

    my @passed_recs = _do_filter( $params{infiles},
                                  $params{dir},
                                  $params{stem},
                                  $params{outfile},
                                  $params{min1},
                                  $params{max1},
                                  $params{min2},
                                  $params{max2},
                                  $params{verbose} );

    qlogend();
}


sub _do_filter {
    my $ra_infiles = shift;
    my $dir        = shift;
    my $stem       = shift;
    my $outfile    = shift;
    my $min1       = shift;
    my $max1       = shift;
    my $min2       = shift;
    my $max2       = shift;
    my $verbose    = shift;

    # Start compiling the list of files to be processed
    my @infiles = @{ $ra_infiles };
    my %infiles = ();

    if (scalar(@infiles)) {
        foreach my $infile (@infiles) {
            $infiles{ $infile }++;
            warn "File $infile has been seen ",$infiles{$infile}," times\n"
                if ($infiles{ $infile } > 1);
        }
    }
    else {
        foreach my $ext (qw( _D _INV _SI _TD)) {
            my $infile = $dir .'/'. $stem . $ext;
            if (-r $infile) {
                $infiles{ $infile }++;
                qlogprint( "found file: $infile\n" ) if $verbose;
            }
            else {
                qlogprint( "could not find file: $infile\n" );
            };
        }
    }

    # Get the detailed output file ready
    my $outfh = IO::File->new( $outfile, 'w' );
    croak "Can't open output file $outfile for writing: $!"
        unless defined $outfh;
    qlogprint( "writing to file: $outfile\n" ) if $verbose;

    my @recs = ();
    foreach my $infile (sort keys %infiles) {
        qlogprint( "processing file $infile\n" ) if $verbose;

        my $prr = QCMG::IO::PindelReader->new( filename => $infile,
                                               verbose  => $verbose );
        my $passed_ctr = 0;
        while (my $rec = $prr->next_record()) {

            # Should we be looking at File9UpstreamReadsCount or
            # File9UpstreamUniqueReadsCount ?  The following example
            # shows why it makes a difference:
            #
            # 412095  I 1 NT 1 "C"    ChrID chrX  BP 152864477    152864478
            # BP_range  152864477    152864481   Supports 5  4   + 4 3   - 1 1
            # S1 10   SUM_MS   300 2   NumSupSamples 2 1
            # APGI_2353_HiSeq_genome_ND 1 0 0 0
            # APGI_2353_HiSeq_genome_XD 3 3 1 1
            # 
            # If we look at the last 2 lines we see that the ND has 1
            # upstream read but 0 unique upstream reads so depending on
            # which count you chose it does or doesn't have reads in the
            # normal with evidence for the variant - this can cause us
            # to mislabel a germline as somatic - so maybe we should use
            # the read count, not unique read count.  BUT, the XD file
            # shows 3 upstream and 3 downstread reads but only 1 of each
            # is unique so there is a lot more evidence in the
            # non-unique reads (duplicates?).  Overall, if we use reads
            # count we actually get more somatic calls made because more
            # TD samples have enough evidence to trigger calling.

            ### What do we do ? ###

            # Check that the reads counts are within specified limits
            my $f1reads = $rec->file(0)->{UpstreamUniqueReadsCount} +
                          $rec->file(0)->{DownstreamUniqueReadsCount};
            my $f2reads = $rec->file(1)->{UpstreamUniqueReadsCount} +
                          $rec->file(1)->{DownstreamUniqueReadsCount};

            if (($f1reads >= $min1 and $f1reads <= $max1) and 
                ($f2reads >= $min2 and $f2reads <= $max2)) {
                $outfh->print( $rec->to_text() );
                push @recs, $rec;
                $passed_ctr++;
            }
        }

        qlogprint( "$passed_ctr of ". $prr->record_ctr() .
                   " records passed filter from file $infile\n");
    }
    $outfh->close;

    return \@recs;
}


sub _do_filter_2 {
    my $ra_infiles   = shift;
    my $dir          = shift;
    my $stem         = shift;
    my $outfile      = shift;
    my $ra_limits    = shift;
    my $tumour_label = shift;
    my $normal_label = shift;
    my $verbose      = shift;

    # Start compiling the list of files to be processed
    my @infiles = @{ $ra_infiles };
    my %infiles = ();

    # If a list of files was passed in, process them otherwise assume we
    # will be doing the standard files and predict their names based
    # on the directory and stem supplied and the standard postfixes.

    if (scalar(@infiles)) {
        foreach my $infile (@infiles) {
            $infiles{ $infile }++;
            warn "File $infile has been seen ",$infiles{$infile}," times\n"
                if ($infiles{ $infile } > 1);
        }
    }
    else {
        foreach my $ext (qw( _D _INV _SI _TD)) {
            my $infile = $dir .'/'. $stem . $ext;
            if (-r $infile) {
                $infiles{ $infile }++;
                qlogprint( "found file: $infile\n" ) if $verbose;
            }
            else {
                qlogprint( "could not find file: $infile\n" );
            };
        }
    }

    # Get the detailed output file ready
    my $outfh = IO::File->new( $outfile, 'w' );
    croak "Can't open output file $outfile for writing: $!"
        unless defined $outfh;
    qlogprint( "writing to file: $outfile\n" ) if $verbose;

    my %legal_comps = ( '<' => 1,
                        '>' => 1, 
                        '=' => 1, 
                        '<=' => 1, 
                        '>=' => 1 );
    my %legal_types = ( 'TUMOUR' => 1,
                        'TUMOUR_UNIQUE' => 1,
                        'NORMAL' => 1,
                        'NORMAL_UNIQUE' => 1 );

    # Test data
#    my @limits = ( 'TUMOUR_UNIQUE > 4',
#                   'TUMOUR_UNIQUE < 1000',
#                   'NORMAL_UNIQUE < 1',
#                   'NORMAL < 2' );
#    my $tumour_label = 'APGI_2353_HiSeq_genome_TD';
#    my $normal_label = 'APGI_2353_HiSeq_genome_ND';
    
    my @recs = ();
    my $fctr = 0;
    foreach my $infile (sort keys %infiles) {
        qlogprint( "processing file $infile\n" ) if $verbose;

        my $prr = QCMG::IO::PindelReader->new( filename => $infile,
                                               verbose  => $verbose );
        my $passed_ctr = 0;
        my $tumour_label_index = undef;
        my $normal_label_index = undef;
        while (my $rec = $prr->next_record()) {

            # Using first record, we need to set the indexes for tumour
            # and normal files based on the labels.

            if (! defined $tumour_label_index and ! defined $normal_label_index) {
                my @files = $rec->files;
                foreach my $ctr (0..$#files) {
                    $tumour_label_index = $ctr
                        if ($files[$ctr]->{SampleName} eq $tumour_label);
                    $normal_label_index = $ctr
                        if ($files[$ctr]->{SampleName} eq $normal_label);
                }

#                print Dumper $tumour_label, $normal_label,
#                             $rec->{file_info},
#                             $tumour_label_index, $normal_label_index;

                # If we get here and we don't have indexes for tumour
                # and normal then we have a problem and cannot continue

                die "Unable to match tumour label from INI file to pindel file\n"
                    unless (defined $tumour_label_index);
                die "Unable to match normal label from INI file to pindel file\n"
                    unless (defined $normal_label_index);
            }

            # Should we be looking at File9UpstreamReadsCount or
            # File9UpstreamUniqueReadsCount ?  The following example
            # shows why it makes a difference:
            #
            # 412095  I 1 NT 1 "C"    ChrID chrX  BP 152864477    152864478
            # BP_range  152864477    152864481   Supports 5  4   + 4 3   - 1 1
            # S1 10   SUM_MS   300 2   NumSupSamples 2 1
            # APGI_2353_HiSeq_genome_ND 1 0 0 0
            # APGI_2353_HiSeq_genome_XD 3 3 1 1
            # 
            # If we look at the last 2 lines we see that the ND has 1
            # upstream read but 0 unique upstream reads so depending on
            # which count you chose it does or doesn't have reads in the
            # normal with evidence for the variant - this can cause us
            # to mislabel a germline as somatic - so maybe we should use
            # the read count, not unique read count.  BUT, the XD file
            # shows 3 upstream and 3 downstread reads but only 1 of each
            # is unique so there is a lot more evidence in the
            # non-unique reads (duplicates?).  Overall, if we use reads
            # count we actually get more somatic calls made because more
            # TD samples have enough evidence to trigger calling.

            ### What do we do ? ###

            my $failed = 0;
            foreach my $limit (@{ $ra_limits }) {
                if ($limit =~ /^\s*([A-Z_]+)\s*([<>=]{1,2})\s*(\d+)\s*$/) {
                    my $type=$1;
                    my $comp=$2;
                    my $value=$3;

                    # Make sure that all the values are legit
                    die "Unable to parse limit: [$limit]\n"
                       unless (defined $type and
                               defined $comp and
                               defined $value and
                               exists $legal_types{ $type } and
                               exists $legal_comps{ $comp });

                    # Work out which type of count the limit applies to
                    # and set our read_count appropriately.
                    my $read_count = undef;
                    if ($type eq 'TUMOUR_UNIQUE') {
                        $read_count = $rec->file( $tumour_label_index )->{UpstreamUniqueReadsCount} +
                                      $rec->file( $tumour_label_index )->{DownstreamUniqueReadsCount};
                    }
                    elsif ($type eq 'TUMOUR') {
                        $read_count = $rec->file( $tumour_label_index )->{UpstreamReadsCount} +
                                      $rec->file( $tumour_label_index )->{DownstreamReadsCount};
                    }
                    elsif ($type eq 'NORMAL_UNIQUE') {
                        $read_count = $rec->file( $normal_label_index )->{UpstreamUniqueReadsCount} +
                                      $rec->file( $normal_label_index )->{DownstreamUniqueReadsCount};
                    }
                    elsif ($type eq 'NORMAL') {
                        $read_count = $rec->file( $normal_label_index )->{UpstreamReadsCount} +
                                      $rec->file( $normal_label_index )->{DownstreamReadsCount};
                    }

                    # Now we build the expression and evaluate it
                    my $expression = "$read_count $comp $value";
                    my $success = eval $expression; warn $@ if $@;
                    $failed = 1 unless $success;
                    #print Dumper "expr: $expression  success: $success  failed: $failed"; 
                }
                else {
                    die "Unable to process limit: [$limit]\n";
                }
            }

            $fctr++;
            if ($verbose and $fctr % 100000 == 0) {
                 warn "processed $fctr records (".scalar(@recs)." passed)\n";
            }

            # If we didn't fail any criteria then this record is a keeper
            if (! $failed) {
                push @recs, $rec;
                $passed_ctr++;
                $outfh->print( $rec->to_text );
            }
        }

        qlogprint( "$passed_ctr of ". $prr->record_ctr() .
                   " records passed filter from file $infile\n");
    }
    $outfh->close;

    qlogprint( scalar(@recs). " records passed filters\n");

    return \@recs;
}


sub pipeline {
    my $class = shift;

    pod2usage( -exitval  => 0,
               -verbose  => 99,
               -sections => 'COMMAND DETAILS/PIPELINE' )
        unless (scalar @ARGV > 0);

    # Setup defaults for important variables.

    my %params = ( config   => '',
                   logfile  => '',
                   verbose  => 0 );

    my $results = GetOptions (
           'c|config=s'           => \$params{config},        # -c
           'l|logfile=s'          => \$params{logfile},       # -l
           'v|verbose+'           => \$params{verbose},       # -v
           );

    # Set up logging
    qlogfile($params{logfile}) if $params{logfile};
    qlogbegin();
    qlogprint( {l=>'EXEC'}, "CommandLine $CMDLINE\n");

    # It is mandatory to supply an infile or directory
    die "You must specify a config file (-c)\n" unless $params{config};

    my $ini = QCMG::IO::INIFile->new(
                    file    => $params{config},
                    verbose => $params{verbose} );

    die "The INI file must contain a [Filter] section" 
        unless $ini->section('filter');

    if ($params{verbose}) {
        qlogprint( "Parsed contents of INI file ",$params{config},":\n" );
        qlogprint( "  $_\n" ) foreach split( /\n/, $ini->to_text );
    }

    # Derive any files/binaries that will be required later
    my $filtfile = $ini->param('qpindel','tmpdir') .'/'.
                   $ini->param('qpindel','output') .'_filtered.txt';

    # Check that required binaries are all available before we bother to
    # do any processing
    my $p2vbin = defined $ini->param('qpindel','p2vbin') ?
                 $ini->param('qpindel','p2vbin') :
                 '/panfs/share/software/pindel024s/pindel2vcf';
    die "pindel2vcf binary not executable: $p2vbin\n" unless (-e $p2vbin);

    my $c2abin = defined $ini->param('qpindel','c2abin') ?
                 $ini->param('qpindel','c2abin') :
                 '/share/software/annovar/convert2annovar.pl';
    die "convert2annovar.pl binary not executable: $c2abin\n" unless (-e $c2abin);

    my $annbin = defined $ini->param('qpindel','annbin') ?
                 $ini->param('qpindel','annbin') :
                 '/share/software/annovar/annotate_variation.pl';
    die "annotate_variation.pl binary not executable: $annbin\n" unless (-e $annbin);

    my @filters = map { $ini->param( 'filter', $_ ) }
                  sort keys %{ $ini->section( 'filter' ) };

    # Filter pindel output
    my @passed_recs = _do_filter_2( [],
                                    $ini->param('qpindel','indir'),
                                    $ini->param('pindel','output'),
                                    $filtfile,
                                    \@filters,
                                    $ini->param('tumour','label'),
                                    $ini->param('normal','label'),
                                    $params{verbose} || 0 );

    # Run pindel2vcf to create a VCF file
    my $vcffile = $filtfile .'.vcf';
    _do_pindel2vcf( $filtfile,
                    $vcffile,
                    $p2vbin,
                    $ini->param('pindel','reference'),
                    $ini->param('pindel','reference_label'),
                    $ini->param('pindel','reference_date'),
                    $params{verbose} || 0 );

    # Run convert2annovar.pl to convert VCF to annovar-ready format
    my $annfile = $vcffile .'.annovar';
    _do_vcf2annovar( $vcffile,
                     $annfile,
                     $c2abin,
                     $params{verbose} || 0 );

    # Run annotate_variation.pl to annotate VCF
    _do_annovar_ensgene( $annfile,
                         $annbin,
                         $params{verbose} || 0 );

    my $fm = QCMG::Util::FileManipulator->new( verbose => $params{verbose} );

    # Run annotate_variation.pl to annotate VCF
    my $exonfile = $annfile .'.exonic_variant_function';
    my $exonfile_rerun = $exonfile . '_rerunnable';
    $fm->move_columns_from_front_to_back( infile  => $exonfile,
                                          outfile => $exonfile_rerun,
                                          count   => 3 );


    qlogend();
}


sub _do_annovar_ensgene {
     my $infile          = shift;
     my $binary          = shift;
     my $verbose         = shift;

     my $cmdline = "$binary " .
                   "-geneanno -dbtype ensgene ".
                   "$infile " .
                   "-buildver hg19 /share/software/annovar/hg19";

     qlogprint( "ensgene hg19 annovar cmdline: $cmdline\n" ) if $verbose;

     if (system($cmdline) != 0) {
         # You can check all the failure possibilities by inspecting $? like this:
         if ($? == -1) {
             qlogprint "failed to execute annotate_variation.pl: $!\n";
         }
         elsif ($? & 127) {
             qlogprint( sprintf( "annotate_variation.pl died with signal %d, %s coredump\n",
                                ($? & 127), ($? & 128) ? 'with' : 'without' ));
         }
         else {
             qlogprint( sprintf "annotate_variation.pl exited with value %d\n", $? >> 8 );
         }
     }
}


sub _do_vcf2annovar {
     my $infile          = shift;
     my $outfile         = shift;
     my $binary          = shift;
     my $verbose         = shift;

     my $cmdline = "$binary " .
                   "-format vcf4 --includeinfo ".
                   "-outfile $outfile $infile";

     qlogprint( "convert2annovar cmdline: $cmdline\n" ) if $verbose;

     if (system($cmdline) != 0) {
         # You can check all the failure possibilities by inspecting $? like this:
         if ($? == -1) {
             qlogprint "failed to execute convert2annovar: $!\n";
         }
         elsif ($? & 127) {
             qlogprint( sprintf( "convert2annovar died with signal %d, %s coredump\n",
                                ($? & 127), ($? & 128) ? 'with' : 'without' ));
         }
         else {
             qlogprint( sprintf "convert2annovar exited with value %d\n", $? >> 8 );
         }
     }
}


sub _do_pindel2vcf {
     my $infile          = shift;
     my $outfile         = shift;
     my $binary          = shift;
     my $reference       = shift;
     my $reference_label = shift;
     my $reference_date  = shift;
     my $verbose         = shift;

     # This step takes over 90% of the pipeline execution time so once
     # it has been run once, it can safely be skipped during debugging
     # by uncommenting the following statement:
     #return;

     my $cmdline = "$binary " .
                   "-r $reference ".
                   "-R $reference_label ".
                   "-d $reference_date ".
                   "-p $infile ".
                   "-v $infile.vcf";

     qlogprint( "pindel2vcf cmdline: $cmdline\n" ) if $verbose;

     if (system($cmdline) != 0) {
         # You can check all the failure possibilities by inspecting $? like this:
         if ($? == -1) {
             qlogprint "failed to execute pindel2vcf: $!\n";
         }
         elsif ($? & 127) {
             qlogprint( sprintf( "pindel2vcf died with signal %d, %s coredump\n",
                                ($? & 127), ($? & 128) ? 'with' : 'without' ));
         }
         else {
             qlogprint( sprintf "pindel2vcf exited with value %d\n", $? >> 8 );
         }
     }
}


sub test {
    my $class = shift;

    pod2usage( -exitval  => 0,
               -verbose  => 99,
               -sections => 'COMMAND DETAILS/FILTER' )
        unless (scalar @ARGV > 0);

    # Setup defaults for important variables.

    my %params = ( infiles  => [],
                   logfile  => '',
                   verbose  => 0 );

    my $results = GetOptions (
           'i|infile=s'           =>  $params{infiles},       # -i
           'l|logfile=s'          => \$params{logfile},       # -l
           'v|verbose+'           => \$params{verbose},       # -v
           );

    # Set up logging
    qlogfile($params{logfile}) if $params{logfile};
    qlogbegin();
    qlogprint( {l=>'EXEC'}, "CommandLine $CMDLINE\n");

    my $pr = QCMG::IO::PindelReader->new(
                  filename => $params{infiles}->[0],
                  verbose  => $params{verbose } );

    $pr->next_record foreach (0..9999);

    qlogend();
}


__END__

=head1 NAME

qpindel.pl - pindel utility belt script


=head1 SYNOPSIS

 qpindel.pl command [options]


=head1 ABSTRACT

This script is a collection of modes that relate to the pindel variant
calling software.  Documentation for this script has been transferred to
the QCMG wiki pages under qpindel.pl.


=head1 COMMANDS

 filter         - filter pindel output files
 pipeline       - run filter and annovar pipeline from config file
 version        - print version number and exit immediately
 help           - display usage summary
 man            - display full man page


=head1 COMMAND DETAILS

=head2 FILTER

Filter records out of pindel output files.

 -i | --infile        pindel output file
 -d | --dir           directory of pindel output files
 -s | --stem          stem of pindel file names (used with -d)
      --min1          minimum supporting reads for variant in file 1
      --max1          maximum supporting reads for variant in file 1
      --min2          minimum supporting reads for variant in file 2
      --max2          maximum supporting reads for variant in file 2
 -o | --outfile       output file in text or XML format; default = text
 -l | --logfile       Log file; optional
 -v | --verbose       print progress and diagnostic messages

=head2 PIPELINE

Filter pindel calls and run through annovar

 -c | --config        INI_style configuration file; mandatory
 -l | --logfile       Log file; optional
 -v | --verbose       print progress and diagnostic messages


=head1 AUTHOR

=over 2

=item John Pearson, L<mailto:j.pearson@uq.edu.au>

=back


=head1 VERSION

$Id: qpindel.pl 4669 2014-07-24 10:48:22Z j.pearson $


=head1 COPYRIGHT

Copyright (c) The University of Queensland 2012-2013

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
