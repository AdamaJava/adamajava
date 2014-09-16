#!/usr/bin/perl -w

##############################################################################
#
#  Program:  pbstools.pl
#  Author:   John V Pearson
#  Created:  2012-06-26
#
#  Tasks associated with PBS.
#
#  $Id: pbstools.pl 4667 2014-07-24 10:09:43Z j.pearson $
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
use QCMG::Util::QLog;

use vars qw( $SVNID $REVISION $CMDLINE );

( $REVISION ) = '$Revision: 4667 $ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id: pbstools.pl 4667 2014-07-24 10:09:43Z j.pearson $'
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

    my @valid_modes = qw( help man version xmljobreports );

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
        xmljobreports();
    }
    else {
        die "pbstools mode [$mode] is unrecognised; valid modes are: " .
            join(' ',@valid_modes) ."\n";
    }
}


sub xmljobreports {
    my $class = shift;

    pod2usage( -exitval  => 0,
               -verbose  => 99,
               -sections => 'COMMAND DETAILS/XMLJOBREPORTS' )
        unless (scalar @ARGV > 0);

    # Setup defaults for important variables.

    my %params = ( infiles  => [],
                   dirs     => [],
                   outfile  => '',
                   logfile  => '',
                   verbose  => 0 );

    my $results = GetOptions (
           'i|infile=s'           =>  $params{infiles},       # -i
           'd|dir=s'              =>  $params{dirs},          # -d
           'o|outfile=s'          => \$params{outfile},       # -o
           'l|logfile=s'          => \$params{logfile},       # -l
           'v|verbose+'           => \$params{verbose},       # -v
           );

    # It is mandatory to supply an infile or directory
    die "You must specify an input file (-i) or directory (-d)\n"
        unless ( scalar( @{ $params{infiles} } ) or
                 scalar( @{ $params{dirs} } ) );
    
    # Set up logging
    qlogfile($params{logfile}) if $params{logfile};
    qlogbegin();
    qlogprint( {l=>'EXEC'}, "CommandLine $CMDLINE\n");

    warn "No output file specified\n" unless $params{outfile};

    # Start compiling the list of files to be processed
    my %infiles = ();

    foreach my $infile (@{ $params{infiles} }) {
        $infiles{ $infile }++;
        warn "File $infile has been seen ",$infiles{$infile}," times\n"
            if ($infiles{ $infile } > 1);
    }
    
    my $ff = QCMG::FileDir::Finder->new( verbose => $params{verbose} );
    foreach my $indir (@{ $params{dirs} }) {
        my @files = $ff->find_file( $indir, '.' );
        foreach my $infile (@files) {
            $infiles{ $infile }++;
            warn "File $infile has been requested ",$infiles{$infile}," times\n"
                if ($infiles{ $infile } > 1);
        }
    }
    
    # The file created by the PBS epilogue may not be real XML - it may 
    # lack a document root element as it is a concatenation of XML <Data/>
    # elements.  This means that we cannot use any of the XML reading
    # routines directly on the files so instead we slurp them, break
    # them up into elements using regex and use XML parsers directly on
    # the <Data/> elements.

    # Only keep one version in rare (and unexplained) case where XML
    # contains two jobs with same ID
    my %recs = ();

    foreach my $infile (sort keys %infiles) {
        qlogprint( "processing file $infile\n" )
            if ($params{verbose} > 1);
        my $text;
        {
            local $/;
            open( my $fh, $infile ) or
                die "unable to open $infile for reading: $!\n";
            $text = <$fh>;
        }

        my $ctr = 0;
        while ($text =~ m/(\<Data>.*?\<\/Data>)/smig) {
            my $xmltxt = $1;
            $ctr++;

            # Slurp XML text into DOM object and get ready for XPath work
            my $doc = undef;
            #qlogprint( "processing record $ctr\n" );
            eval { 
                     $doc = XML::LibXML->load_xml( string => $xmltxt );
                 };
            die "failed while parsing record $ctr ",$@ if $@;
            my $xpc = XML::LibXML::XPathContext->new($doc);

            foreach my $node (@{ $xpc->findnodes('Data/Job') }) {
                my %rec = ();
                $rec{jobId}      = $node->findvalue('Job_Id');
                $rec{jobName}    = $node->findvalue('Job_Name');
                $rec{jobOwner}   = $node->findvalue('Job_Owner');
                $rec{queue}      = $node->findvalue('queue');
                $rec{ctime}      = $node->findvalue('ctime');
                $rec{mtime}      = $node->findvalue('mtime');
                $rec{qtime}      = $node->findvalue('qtime');
                $rec{etime}      = $node->findvalue('etime');
                $rec{startTime}  = $node->findvalue('start_time');
                $rec{execHost}   = $node->findvalue('exec_host');
                $rec{submitArgs} = $node->findvalue('submit_args');

                $rec{resAsked}->{nodect} =
                    $node->findvalue('Resource_List/nodect');
                $rec{resAsked}->{nodes} =
                    $node->findvalue('Resource_List/nodes');
                $rec{resAsked}->{walltime} =
                    $node->findvalue('Resource_List/walltime');
                $rec{resAsked}->{mem} =
                    $node->findvalue('Resource_List/mem');
                $rec{resAsked}->{ncpus} =
                    $node->findvalue('Resource_List/ncpus');

                $rec{resUsed}->{cput} =
                    $node->findvalue('resources_used/cput');
                $rec{resUsed}->{mem} =
                    $node->findvalue('resources_used/mem');
                $rec{resUsed}->{vmem} =
                    $node->findvalue('resources_used/vmem');
                $rec{resUsed}->{walltime} =
                    $node->findvalue('resources_used/walltime');

                $recs{ $rec{jobId} } = \%rec;
            }
        }
        qlogprint( "read $ctr XML records from file $infile\n");
    }

    my @recs = values %recs;
    qlogprint( "found ",scalar(@recs)," PBS job records\n" );

    write_recs( \@recs, $params{outfile} ) if ($params{outfile});

    qlogend();
}


sub write_recs {
    my $ra_recs = shift;
    my $file    = shift;

    # Decide if XML was requested
    my $xmlmode = ($file =~ /\.xml/i) ? 1 : 0;

    # Get the detailed output file ready
    my $outfh = IO::File->new( $file, 'w' );
    croak "Can't open output file $file for writing: $!"
        unless defined $outfh;

    if ($xmlmode) {
        $outfh->print( "<PBSToolsXmlJobReports>\n<Jobs>\n" );
    }
    else {
        $outfh->print( join( "\t", qw( Job_Id Job_Owner Job_Name queue
                                       Queued_At_Time
                                       Start_Time
                                       Queued_For_Time
                                       Requested_Walltime
                                       Requested_Mem
                                       Requested_NodeCt
                                       Requested_Nodes
                                       Requested_NCpus
                                       Used_Walltime
                                       Used_Mem
                                       Used_VMem
                                       Used_CpuTime
                                       Exec_Host Submit_Args ) ), "\n" );
    }

    foreach my $rec (@{ $ra_recs }) {
        my $owner = $rec->{jobOwner};
        $owner =~ s/\@.*$//;
        my $id = $rec->{jobId};
        $id =~ s/\.qcmg-clustermk2\.imb\.uq\.edu\.au//;
        if ($xmlmode) {
            $outfh->print( '<job>',
                           xw( 'id', $id ),
                           xw( 'owner', $owner ),
                           xw( 'name', $rec->{jobName} ),
                           xw( 'queue', $rec->{queue} ),
                           xw( 'queuedAtTime', scalar(localtime($rec->{qtime})) ),
                           xw( 'startTime', scalar(localtime($rec->{startTime})) ),
                           xw( 'queuedForTime', ($rec->{startTime}-$rec->{qtime}) ),
                           xw( 'requestedWallTime', $rec->{resAsked}->{walltime} ),
                           xw( 'requestedMem',   $rec->{resAsked}->{mem} ),
                           xw( 'requestedNodeCt', $rec->{resAsked}->{nodect} ),
                           xw( 'requestedNodes', $rec->{resAsked}->{nodes} ),
                           xw( 'requestedNCpus', $rec->{resAsked}->{ncpus} ),
                           xw( 'usedWallTime', $rec->{resUsed}->{walltime} ),
                           xw( 'usedMem', convert_to_gb( $rec->{resUsed}->{mem} ) ),
                           xw( 'usedVMem', convert_to_gb( $rec->{resUsed}->{vmem} ) ),
                           xw( 'usedCpuTime', $rec->{resUsed}->{cput} ),
                           xw( 'execHost', $rec->{execHost} ),
                           xw( 'submitArgs', $rec->{submitArgs} ),
                           "<\/job>\n" );
        }
        else {
            $outfh->print( join( "\t", $id,
                                       $owner,
                                       $rec->{jobName},
                                       $rec->{queue},
                                       scalar(localtime($rec->{qtime})),
                                       scalar(localtime($rec->{startTime})),
                                       ($rec->{startTime}-$rec->{qtime}),
                                       $rec->{resAsked}->{walltime},
                                       $rec->{resAsked}->{mem},
                                       $rec->{resAsked}->{nodect},
                                       $rec->{resAsked}->{nodes},
                                       $rec->{resAsked}->{ncpus},
                                       $rec->{resUsed}->{walltime},
                                       convert_to_gb( $rec->{resUsed}->{mem} ),
                                       convert_to_gb( $rec->{resUsed}->{vmem} ),
                                       $rec->{resUsed}->{cput},
                                       $rec->{execHost},
                                       $rec->{submitArgs} ),"\n" );
        }
    }

    if ($xmlmode) {
        $outfh->print( "</Jobs>\n</PBSToolsXmlJobReports>\n" );
    }

    $outfh->close;
}


sub convert_to_gb {
    my $mem = shift;
    my $gb  = $mem;
    $gb =~ s/[kgmb]*//g;
    if ($mem =~ /kb$/i) {
        return sprintf( '%.1f', ($gb/1000000) ).'gb';
    }
    elsif ($mem =~ /mb$/i) {
        return sprintf( '%.1f', ($gb/1000) ).'gb';
    }
    elsif ($gb) {
        return $gb.'gb';
    }
    else {
        return $gb;
    }
}


sub xw {
    my $tag = shift;
    my $val = shift;
    return '<'.$tag.'>'.$val.'</'.$tag.'>';
}


__END__

=head1 NAME

pbstools.pl - PBS utility belt script


=head1 SYNOPSIS

 pbstools.pl command [options]


=head1 ABSTRACT

This script is a collection of modes that relate to PBS jobs.


=head1 COMMANDS

 xmljobreports  - process XML file of job reports
 version        - print version number and exit immediately
 help           - display usage summary
 man            - display full man page


=head1 COMMAND DETAILS

=head2 XMLJOBREPORTS

Process epilogue XML reports for each job

 -i | --infile        file of job reports in XML format
 -d | --dir           directory of job reports in XML format
 -o | --outfile       output file in text or XML format; default = text
 -l | --logfile       Log file; optional
 -v | --verbose       print progress and diagnostic messages


=head1 DESCRIPTION

From B<man pbs_job_attributes> and B<man pbs_resources_linux>:

 ctime  The time that the job was created.
 etime  The time that the job became eligible to run, i.e.
        in a queued state while residing in an execution queue.
 mtime  The time that the job was last modified, changed
        state, or changed locations.
 qtime  The time that the job entered the current queue.

 cput   Maximum amount of CPU time used by all processes in
        the job.  Units: time.
 vmem   Maximum amount of virtual memory used by all concurrent
        processes in the job.  Units: size.
 walltime  Maximum amount of real time during which the job
        can be in the running state.  Units: time.
 host   Name  of  host  on  which  job should be run.  This
        resource is provided for use by the site’s scheduling 
        policy.  The allowable values and effect on job
        placement is site dependent.  Units: string.
 nodes  Number and/or type of nodes to be reserved for
        exclusive use by the job.  


=head1 AUTHOR

=over 2

=item John Pearson, L<mailto:j.pearson@uq.edu.au>

=back


=head1 VERSION

$Id: pbstools.pl 4667 2014-07-24 10:09:43Z j.pearson $


=head1 COPYRIGHT

Copyright (c) The University of Queensland 2012

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
