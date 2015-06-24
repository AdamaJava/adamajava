#!/usr/bin/env perl

##############################################################################
#
#  Program:  qvisualise.pl
#  Author:   John V Pearson
#  Created:  2011-09-23
#
#  Read various XML report files create by QCMG tools and create HTML
#  files that use the tabbed-pane layout and the Google Chart API for
#  visualisaztion.
#
#  $Id: qvisualise.pl 4526 2014-02-28 07:56:11Z m.anderson $
#
##############################################################################

use strict;
use warnings;

use Carp qw( carp croak verbose );
use Data::Dumper;
use Getopt::Long;
use IO::File;
use Pod::Usage;

use QCMG::Visualise::HtmlReport;
use QCMG::Visualise::LifeScopeDirectory;
use QCMG::Visualise::SolidStatsMappingSummary;
use QCMG::Visualise::SolidStatsReport;
use QCMG::Visualise::TimelordReport;
use QCMG::Visualise::qCoverage;
use QCMG::Visualise::qProfiler;
use QCMG::Visualise::QSigCompare;
use QCMG::Util::QLog;

use vars qw( $SVNID $REVISION $CMDLINE );

( $REVISION ) = '$Revision: 4526 $ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id: qvisualise.pl 4526 2014-02-28 07:56:11Z m.anderson $'
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

    # Each of the XML parsing modes invokes a subroutine, and these
    # subroutines are often almost identical.  While this looks
    # like wasteful code duplication, it is necessary so that each mode
    # has complete independence in terms of processing input parameters
    # and taking action based on the parameters.

    my @valid_modes = qw( help man version qsignature solidstats
                          qcoverage mappingsummary timelord
                          qprofiler qlibcompare );

    if ($mode =~ /^$valid_modes[0]$/i or $mode =~ /\?/) {
        pod2usage( -exitval  => 0,
                   -verbose  => 99,
                   -sections => 'SYNOPSIS|COMMANDS' );

    }
    elsif ($mode =~ /^$valid_modes[1]$/i) {
        pod2usage(-exitstatus => 0, -verbose => 2)
    }
    elsif ($mode =~ /^$valid_modes[2]$/i) {
        print "$SVNID\n";
    }
    elsif ($mode =~ /^$valid_modes[3]/i) {
        qsignature();
    }
    elsif ($mode =~ /^$valid_modes[4]/i) {
        solidstats();
    }
    elsif ($mode =~ /^$valid_modes[5]/i) {
        qcoverage();
    }
    elsif ($mode =~ /^$valid_modes[6]/i) {
        mappingsummary();
    }
    elsif ($mode =~ /^$valid_modes[7]/i) {
        timelord();
    }
    elsif ($mode =~ /^$valid_modes[8]/i) {
        qprofiler();
    }
    elsif ($mode =~ /^$valid_modes[9]/i) {
        qlibcompare();
    }
    else {
        die "qvisualise mode [$mode] is unrecognised; valid modes are: " .
            join(' ',@valid_modes) ."\n";
    }
}


sub mappingsummary {

    pod2usage( -exitval  => 0,
               -verbose  => 99,
               -sections => 'COMMAND DETAILS/MAPPINGSUMMARY' )
        unless (scalar @ARGV > 0);

    # Setup defaults for important variables.

    my %params = ( infile   => '',
                   outfile  => '',
                   logfile  => '',
                   verbose  => 0 );

    my $results = GetOptions (
           'i|infile=s'           => \$params{infile},        # -i
           'o|outfile=s'          => \$params{outfile},       # -o
           'l|logfile=s'          => \$params{logfile},       # -l
           'v|verbose+'           => \$params{verbose},       # -v
           );

    # It is mandatory to supply an infile
    die "You must specify an input file\n" unless $params{infile};

    # Set default for outfile if not set
    $params{outfile} = $params{infile}.'.html' unless $params{outfile};
    
    # Set up logging
    qlogfile($params{logfile}) if $params{logfile};
    qlogbegin();
    qlogprint( {l=>'EXEC'}, "CommandLine $CMDLINE\n");

    # Read the file and make sure it contains appropriate XML
    
    # Slurp XML file into DOM object
    my $doc = undef;
    eval { $doc = XML::LibXML->load_xml( location => $params{infile} ); };
    die $@ if $@;

    # Sequentially try pulling out the various parseable QCMG
    # top-level nodes.  Die if we don't find any.

    if ($doc->hasChildNodes) {
        my @nodes = $doc->childNodes;
        foreach my $node (@nodes) {
            if ($node->nodeName =~ /SolidStatsMappingSummary/) {
                my $obj = QCMG::Visualise::SolidStatsMappingSummary->new(
                               xmlnode => $node,
                               file    => $params{infile},
                               verbose => $params{verbose} );
                $obj->process;
                print_report( $obj, $params{outfile} );
                qlogend();
                exit;
            } 
        } 
        # If we get this far then we found no matches so die
        die "File $params{infile} did not appear to contain SolidStatsMappingSummary XML";
    }
    else {
        die "XML doc appears to have no child nodes ???";
    }
}


sub qcoverage {

    pod2usage( -exitval  => 0,
               -verbose  => 99,
               -sections => 'COMMAND DETAILS/QCOVERAGE' )
        unless (scalar @ARGV > 0);

    # Setup defaults for important variables.

    my %params = ( configfile   => '',
                   infile       => '',
                   outfile      => '',
                   logfile      => '',
                   verbose      => 0 );

    my $results = GetOptions (
           'c|configfile=s'       => \$params{configfile},    # -c
           'i|infile=s'           => \$params{infile},        # -i
           'o|outfile=s'          => \$params{outfile},       # -o
           'l|logfile=s'          => \$params{logfile},       # -l
           'v|verbose+'           => \$params{verbose},       # -v
           );

    # It is mandatory to supply an infile
    die "You must specify an input file\n" unless $params{infile};

    # Set default for outfile if not set
    $params{outfile} = $params{infile}.'.html' unless $params{outfile};
    
    # Set up logging
    qlogfile($params{logfile}) if $params{logfile};
    qlogbegin();
    qlogprint( {l=>'EXEC'}, "CommandLine $CMDLINE\n");

    # Read the file and make sure it contains appropriate XML
    
    # Slurp XML file into DOM object
    my $doc = undef;
    eval { $doc = XML::LibXML->load_xml( location => $params{infile} ); };
    die $@ if $@;

   
    
       
    # Sequentially try pulling out the various parseable QCMG
    # top-level nodes.  Die if we don't find any.
    if ($doc->hasChildNodes) {
        my @nodes = $doc->childNodes;
        foreach my $node (@nodes) {
            if ($node->nodeName =~ /QCoverageStats/) {
                my $obj = undef;
                # New Visuliser
                if ( $params{configfile} ) {
                    $obj = QCMG::Visualise::HtmlReport->new(
                                    xmlnode      => $node,
                                    configfile   => $params{configfile},
                                    file         => $params{infile},
                                    verbose      => $params{verbose} 
                                );
                }                
                # Old Visuliser
                else { 
                    $obj = QCMG::Visualise::qCoverage->new(
                                   xmlnode => $node,
                                   file    => $params{infile},
                                   verbose => $params{verbose} 
                                );
                }
                
                if ($obj) {
                    $obj->process;
                    print_report( $obj, $params{outfile} );
                    qlogend();
                    exit; 
                }
            }
             
        } 
        # If we get this far then we found no matches so die
        die "File $params{infile} does not contain QCoverageStats XML";
    }
    else {
        die "XML doc appears to have no child nodes ???";
    }
}


sub solidstats {

    pod2usage( -exitval  => 0,
               -verbose  => 99,
               -sections => 'COMMAND DETAILS/SOLIDSTATS' )
        unless (scalar @ARGV > 0);

    # Setup defaults for important variables.

    my %params = ( infile   => '',
                   outfile  => '',
                   logfile  => '',
                   verbose  => 0 );

    my $results = GetOptions (
           'i|infile=s'           => \$params{infile},        # -i
           'o|outfile=s'          => \$params{outfile},       # -o
           'l|logfile=s'          => \$params{logfile},       # -l
           'v|verbose+'           => \$params{verbose},       # -v
           );

    # It is mandatory to supply an infile
    die "You must specify an input file\n" unless $params{infile};

    # Set default for outfile if not set
    $params{outfile} = $params{infile}.'.html' unless $params{outfile};
    
    # Set up logging
    qlogfile($params{logfile}) if $params{logfile};
    qlogbegin();
    qlogprint( {l=>'EXEC'}, "CommandLine $CMDLINE\n");

    # Read the file and make sure it contains appropriate XML
    
    # Slurp XML file into DOM object
    my $doc = undef;
    eval { $doc = XML::LibXML->load_xml( location => $params{infile} ); };
    die $@ if $@;

    # Sequentially try pulling out the various parseable QCMG
    # top-level nodes.  Die if we don't find any.

    if ($doc->hasChildNodes) {
        my @nodes = $doc->childNodes;
        foreach my $node (@nodes) {
            if ($node->nodeName eq 'SolidStatsReport') {
                my $obj = QCMG::Visualise::SolidStatsReport->new( 
                               xmlnode => $node,
                               file    => $params{infile},
                               verbose => $params{verbose} );
                $obj->process;
                print_report( $obj, $params{outfile} );
                qlogend();
                exit;
            } 
        } 
        # If we get this far then we found no matches so die
        die "File $params{infile} does not contain SolidStatsReport XML";
    }
    else {
        die "XML doc appears to have no child nodes ???";
    }
}


sub qsignature {

    pod2usage( -exitval  => 0,
               -verbose  => 99,
               -sections => 'COMMAND DETAILS/QSIGNATURE' )
        unless (scalar @ARGV > 0);

    # Setup defaults for important variables.

    my %params = ( infile       => '',
                   outfile      => '',
                   logfile      => '',
                   configfile   => '',
                   metadatafile => '',
                   mode         => 0,
                   greenmax     => 0.025,
                   yellowmax    => 0.04,
                   verbose      => 0 );

    my $results = GetOptions (
    			 'c|configfile=s'       => \$params{configfile},    # -c
           'm|metadatafile=s'     => \$params{metadatafile},  # -m
					 'i|infile=s'           => \$params{infile},        # -i
           'o|outfile=s'          => \$params{outfile},       # -o
           'l|logfile=s'          => \$params{logfile},       # -l
           'd|mode=i'             => \$params{mode},          # -d
           'g|greenmax=f'         => \$params{greenmax},      # -g
           'y|yellowmax=f'        => \$params{yellowmax},     # -y
           'v|verbose+'           => \$params{verbose},       # -v
           );

    # It is mandatory to supply an infile
    die "You must specify an input file\n" unless $params{infile};
    die "greenmax must be less than yellowmax\n"
        if ($params{greenmax} >= $params{yellowmax});

    # Set default for outfile if not set
    $params{outfile} = $params{infile}.'.html' unless $params{outfile};
    
    # Set up logging
    qlogfile($params{logfile}) if $params{logfile};
    qlogbegin();
    qlogprint( {l=>'EXEC'}, "CommandLine $CMDLINE\n");

    # Read the file and make sure it contains appropriate XML
    
    # Slurp XML file into DOM object
    my $doc = undef;
    eval { $doc = XML::LibXML->load_xml( location => $params{infile} ); };
    die $@ if $@;

    # Sequentially try pulling out the various parseable QCMG
    # top-level nodes.  Die if we don't find any.

    if ($doc->hasChildNodes) {
			my @nodes = $doc->childNodes;
			my $obj = undef;
			foreach my $node (@nodes) {
				if ($node->nodeName =~ /QSigCompare/) {
					# Old Visuliser
					print "Old Visuliser\n"; 
					$obj = QCMG::Visualise::QSigCompare->new(
                     xmlnode   => $node,
										 file      => $params{infile},
										 greenmax  => $params{greenmax},
										 yellowmax => $params{yellowmax},
										 mode      => $params{mode},
										 verbose   => $params{verbose}
                  );
        }
				elsif ($node->nodeName =~ /qsignature/) {
					# New Visuliser
          if ( $params{configfile} ) {
              print "New Visuliser\n";
              $obj = QCMG::Visualise::HtmlReport->new(
                              xmlnode       => $node,
                              configfile    => $params{configfile},
                              metadatafile  => $params{metadatafile},
                              file          => $params{infile},
                              mode          => $params{mode},
                              verbose       => $params{verbose} 
                          );
          }                
				}
          
        if ($obj) {
            $obj->process;
            print_report( $obj, $params{outfile} );
            qlogend();
            exit; 
        }							
            #my $obj = QCMG::Visualise::QSigCompare->new(
            #               xmlnode   => $node,
            #               file      => $params{infile},
            #               greenmax  => $params{greenmax},
            #               yellowmax => $params{yellowmax},
            #               mode      => $params{mode},
            #               verbose   => $params{verbose} );
            #$obj->process( $params{mode} );
            #print_report( $obj, $params{outfile} );
            #qlogend();
            #exit;
        #} 
    } 
    # If we get this far then we found no matches so die
    die "File $params{infile} does not contain QSigCompare XML";
  }
  else {
		die "XML doc appears to have no child nodes ???";
  }
}


sub qprofiler {
    
    print "Mode: qprofiler\n";
    
    pod2usage( -exitval  => 0,
               -verbose  => 99,
               -sections => 'COMMAND DETAILS/QPROFILER' )
        unless (scalar @ARGV > 0);

    # Setup defaults for important variables.

    my %params = ( infile    => '',
                   outfile   => '',
                   logfile   => '',
                   mode      => 0,
                   verbose   => 0 );

    my $results = GetOptions (
        'c|configfile=s'       => \$params{configfile},    # -c
        'i|infile=s'           => \$params{infile},        # -i
        'o|outfile=s'          => \$params{outfile},       # -o
        'l|logfile=s'          => \$params{logfile},       # -l
        'd|mode=i'             => \$params{mode},          # -d
        'v|verbose+'           => \$params{verbose},       # -v
        );

    # It is mandatory to supply an infile
    die "You must specify an input file\n" unless $params{infile};

    # Set default for outfile if not set
    $params{outfile} = $params{infile}.'.html' unless $params{outfile};
    
    # Set up logging
    qlogfile($params{logfile}) if $params{logfile};
    qlogbegin();
    qlogprint( {l=>'EXEC'}, "CommandLine $CMDLINE\n");

    # Read the file and make sure it contains appropriate XML
    
    # Slurp XML file into DOM object
    my $doc = undef;
    eval { $doc = XML::LibXML->load_xml( location => $params{infile} ); };
    die $@ if $@;
    
    
    # Sequentially try pulling out the various parseable QCMG
    # top-level nodes.  Die if we don't find any.
    
    #print "Ready\n";
    if ($doc->hasChildNodes) {
        my @nodes = $doc->childNodes;
        foreach my $node (@nodes) {
            #print "Set\n";
            #print "Node Name ".$node->nodeName."\n";
            if ($node->nodeName =~ /qProfiler/ and ref($node) =~ 'XML::LibXML::Element') { #and ref($node) =~ 'XML::LibXML::Element'
                #print "Go -> ".$node->nodeName." \n";
                #print "ref -> ".ref($node)." \n";
                
                
                my $obj = undef;
                # New Visuliser
                if ( $params{configfile} ) {
                    print "New Visuliser\n";
                    $obj = QCMG::Visualise::HtmlReport->new(
                                    xmlnode      => $node,
                                    configfile   => $params{configfile},
                                    file         => $params{infile},
                                    verbose      => $params{verbose} 
                                );
                }                
                # Old Visuliser
                else {
                    print "Old Visuliser\n"; 
                    $obj = QCMG::Visualise::qProfiler->new(
                                   xmlnode => $node,
                                   file    => $params{infile},
                                   mode      => $params{mode},
                                   verbose => $params{verbose} 
                                );
                }
                
                if ($obj) {
                    $obj->process;
                    print_report( $obj, $params{outfile} );
                    qlogend();
                    exit; 
                }
            }
        }
        # If we get this far then we found no matches so die
        die "File $params{infile} does not contain qProfiler XML";
    }
    else {
        die "XML doc appears to have no child nodes ???";
    }
}


sub qlibcompare {
    
    print "Mode: qlibcompare\n";
    
    pod2usage( -exitval  => 0,
               -verbose  => 99,
               -sections => 'COMMAND DETAILS/QLIBCOMPARE' )
        unless (scalar @ARGV > 0);

    # Setup defaults for important variables.

    my %params = ( infile    => '',
                   outfile   => '',
                   logfile   => '',
                   mode      => 0,
                   verbose   => 0 );

    my $results = GetOptions (
        'c|configfile=s'       => \$params{configfile},    # -c
        'i|infile=s'           => \$params{infile},        # -i
        'o|outfile=s'          => \$params{outfile},       # -o
        'l|logfile=s'          => \$params{logfile},       # -l
        'd|mode=i'             => \$params{mode},          # -d
        'v|verbose+'           => \$params{verbose},       # -v
        );

    # It is mandatory to supply an infile
    die "You must specify an input file\n" unless $params{infile};
    die "You must specify a config file file\n" unless $params{configfile};

    # Set default for outfile if not set
    $params{outfile} = $params{infile}.'.html' unless $params{outfile};
    
    # Set up logging
    qlogfile($params{logfile}) if $params{logfile};
    qlogbegin();
    qlogprint( {l=>'EXEC'}, "CommandLine $CMDLINE\n");

    # Read the file and make sure it contains appropriate XML
    
    # Slurp XML file into DOM object
    my $doc = undef;
    eval { $doc = XML::LibXML->load_xml( location => $params{infile} ); };
    die $@ if $@;
    
    
    # Sequentially try pulling out the various parseable QCMG
    # top-level nodes.  Die if we don't find any.
    
    #print "Ready\n";
    if ($doc->hasChildNodes) {
        my @nodes = $doc->childNodes;
        foreach my $node (@nodes) {
            #print "Set\n";
            #print "Node Name ".$node->nodeName."\n";
            if ($node->nodeName =~ /qLibCompare/ and ref($node) =~ 'XML::LibXML::Element') { #and ref($node) =~ 'XML::LibXML::Element'
                #print "Go -> ".$node->nodeName." \n";
                #print "ref -> ".ref($node)." \n";
                
                my $obj = undef;
                $obj = QCMG::Visualise::HtmlReport->new(
                                xmlnode      => $node,
                                configfile   => $params{configfile},
                                file         => $params{infile},
                                verbose      => $params{verbose} 
                            );
                
                if ($obj) {
                    $obj->process;
                    print_report( $obj, $params{outfile} );
                    qlogend();
                    exit; 
                }
            }
        }
        # If we get this far then we found no matches so die
        die "File $params{infile} does not contain qProfiler XML";
    }
    else {
        die "XML doc appears to have no child nodes ???";
    }
}

sub timelord {

    pod2usage( -exitval  => 0,
               -verbose  => 99,
               -sections => 'COMMAND DETAILS/TIMELORD' )
        unless (scalar @ARGV > 0);

    # Setup defaults for important variables.

    my %params = ( infile   => '',
                   outfile  => '',
                   logfile  => '',
                   verbose  => 0 );

    my $results = GetOptions (
           'i|infile=s'           => \$params{infile},        # -i
           'o|outfile=s'          => \$params{outfile},       # -o
           'l|logfile=s'          => \$params{logfile},       # -l
           'v|verbose+'           => \$params{verbose},       # -v
           );

    # It is mandatory to supply an infile
    die "You must specify an input file\n" unless $params{infile};

    # Set default for outfile if not set
    $params{outfile} = $params{infile}.'.html' unless $params{outfile};
    
    # Set up logging
    qlogfile($params{logfile}) if $params{logfile};
    qlogbegin();
    qlogprint( {l=>'EXEC'}, "CommandLine $CMDLINE\n");

    # Read the file and make sure it contains appropriate XML
    
    # Slurp XML file into DOM object
    my $doc = undef;
    eval { $doc = XML::LibXML->load_xml( location => $params{infile} ); };
    die $@ if $@;

    # Sequentially try pulling out the various parseable QCMG
    # top-level nodes.  Die if we don't find any.

    if ($doc->hasChildNodes) {
        my @nodes = $doc->childNodes;
        foreach my $node (@nodes) {
            if ($node->nodeName eq 'TimelordReport') {
                my $obj = QCMG::Visualise::TimelordReport->new( 
                               xmlnode => $node,
                               file    => $params{infile},
                               verbose => $params{verbose} );
                $obj->process;
                print_report( $obj, $params{outfile} );
                qlogend();
                exit;
            } 
        } 
        # If we get this far then we found no matches so die
        die "File $params{infile} does not contain TimelordReport XML";
    }
    else {
        die "XML doc appears to have no child nodes ???";
    }
}


sub print_report {
    my $obj  = shift;
    my $file = shift;

    # Get the detailed output file ready
    my $outfh = IO::File->new( $file, 'w' );
    croak "Can't open output file $file for writing: $!"
        unless defined $outfh;

    print $outfh $obj->html;
    $outfh->close;
}


__END__

=head1 NAME

qvisualise.pl - Create HTML pages from QCMG XML reports


=head1 SYNOPSIS

 qvisualise.pl command [options]


=head1 ABSTRACT

This script will parse XML reports from various QCMG tools including 
qcoverage, qprofiler, qsignature, timelord.pl etc, and create an appropriate 
HTML reports with summary tables and plots rendered using the Google Chart 
Javascript API. It is modelled after samtools so the first commandline
parameter is a command that determines the type of visualisation job and
that is followed by commandline parameters specific to that command.


=head1 COMMANDS

 mappingsummary - process XML report from summarise_solid_stats.pl
 qcoverage      - process XML report from qcoverage
 qprofiler      - process XML report from qprofiler
 qsignature     - process XML report from qsignature
 solidstats     - process XML report from solid_stats_report.pl
 timelord       - process XML report from timelord.pl
 version        - print version number and exit immediately
 help           - display usage summary
 man            - display full man page


=head1 COMMAND DETAILS

=head2 MAPPINGSUMMARY

Process XML report from summarise_solid_stats.pl

 -i | --infile        mapping summary XML report file
 -o | --outfile       HTML file; default = $infile.html
 -l | --logfile       Log file; optional
 -v | --verbose       print progress and diagnostic messages

=head2 QCOVERAGE

Process XML report from qcoverage

 -i | --infile        qcoverage XML report file
 -o | --outfile       HTML file; default = $infile.html
 -c | --configfile    JSON file; optional - used for updated interface
 -l | --logfile       Log file; optional
 -v | --verbose       print progress and diagnostic messages

=head2 QPROFILER

Process XML report from qprofiler

 -i | --infile        qprofiler XML report file
 -o | --outfile       HTML file; default = $infile.html
 -c | --configfile    JSON file; optional - used for updated interface
 -l | --logfile       Log file; optional
 -v | --verbose       print progress and diagnostic messages

=head2 QSIGNATURE

Process XML report from qsignature

 -i | --infile        qsignature XML report file
 -o | --outfile       HTML file; default = $infile.html
 -l | --logfile       Log file; optional
 -v | --verbose       print progress and diagnostic messages

=head2 SOLIDSTATS

Process XML report from solid_stats_report.pl

 -i | --infile        solidstats XML report file
 -o | --outfile       HTML file; default = $infile.html
 -l | --logfile       Log file; optional
 -v | --verbose       print progress and diagnostic messages

=head2 TIMELORD

Process XML report from timelord.pl

 -i | --infile        timelord XML report file
 -o | --outfile       HTML file; default = $infile.html
 -l | --logfile       Log file; optional
 -v | --verbose       print progress and diagnostic messages


=head1 AUTHOR

=over 2

=item John Pearson, L<mailto:j.pearson@uq.edu.au>

=back


=head1 VERSION

$Id: qvisualise.pl 4526 2014-02-28 07:56:11Z m.anderson $


=head1 COPYRIGHT

This software is copyright 2011-2012 by the Queensland Centre for 
Medical Genomics.
All rights reserved.
This License is limited to, and you
may use the Software solely for, your own internal and non-commercial
use for academic and research purposes. Without limiting the foregoing,
you may not use the Software as part of, or in any way in connection with 
the production, marketing, sale or support of any commercial product or
service or for any governmental purposes.
For commercial or governmental use, please contact licensing\@qcmg.org.

In any work or product derived from the use of this Software, proper 
attribution of the authors as the source of the software or data must be 
made.  The following URL should be cited:

  http://bioinformatics.qcmg.org/software/

=cut
