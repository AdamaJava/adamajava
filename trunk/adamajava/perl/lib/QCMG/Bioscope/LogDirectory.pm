package QCMG::Bioscope::LogDirectory;

##############################################################################
#
#  Module:   QCMG::Bioscope::LogDirectory.pm
#  Creator:  John V Pearson
#  Created:  2010-08-11
#
#  Parses files from a Bioscope-created log directory
#
###########################################################################

use strict;
use warnings;

use IO::File;
use Data::Dumper;
use Carp qw( carp croak cluck confess );

use QCMG::Bioscope::LogModule;


use vars qw( $SVNID $REVISION );

( $REVISION ) = '$Revision$ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id$'
    =~ /\$Id:\s+(.*)\s+/;


sub new {
    my $class  = shift;
    my %params = @_;

    die "QCMG::Bioscope::LogDirectory->new() requires a directory parameter"
       unless (exists $params{directory} and $params{directory});

    my $self = { directory   => $params{directory},
                 files       => {},
                 modules     => [],
                 verbose     => $params{verbose} || 0 };
    bless $self, $class;

    $self->_initialise();
}


sub verbose {
    my $self = shift;
    return $self->{verbose};
}


sub directory {
    my $self = shift;
    return $self->{directory};
}


sub files {
    my $self = shift;
    return $self->{files};
}


sub file_names {
    my $self = shift;
    return sort keys %{ $self->{files} };
}


sub log_objs {
    my $self = shift;
    return $self->{log_objs};
}


sub _initialise {
    my $self = shift;

    my $this_dir = $self->directory();

    opendir(DIR, $this_dir) || die "Can't opendir [$this_dir]: $!";
    my @everything = readdir(DIR);
    closedir DIR;

    # Store all of the file names
    foreach my $thing (@everything) {
        if (-f "$this_dir/$thing") {
            $self->{files}->{ $thing } = 1;
        }
    }

    foreach my $file ($self->file_names) {

        if ($file =~ /^(\w+)\.(\d+)\.main\.(\d+)\.log$/) {
            my $module    = $1;
            my $counter   = $2;
            my $timestamp = $3;

            # If we have found a main log file then we need to find the
            # matching parameters file and process them as a pair.

            my $param_file_name = join('-',$module, $counter, $timestamp) .
                                  '.parameters';

            # In some cases, catastrophic failure of a module happens
            # before the parameters file is written so in these cases,
            # warn and just skip processing of the phase
            if (! exists $self->{files}->{ $param_file_name }) {
                warn "Cannot find parameters file [$param_file_name] ",
                     "to match main file [$file] - skipping\n";
                next;
            }

            my $blm = QCMG::Bioscope::LogModule->new(
                                              dir       => $self->directory,
                                              type      => $module,
                                              counter   => $counter,
                                              timestamp => $timestamp,
                                              main      => $file,
                                              params    => $param_file_name,
                                              verbose   => $self->verbose );

            push @{ $self->{modules} }, $blm;
        }
    }

    return $self;
}


sub as_xml {
    my $self = shift;
    my $xml = '';

    #$xml .= '<BioscopeLogDirectory>' ."\n";
    #$xml .= _element('Directory',$self->directory);

    $xml .= '<BioscopeLogDirectory ' .
            'dir="' . $self->directory . "\">\n";

    $xml .= "<LogFiles>\n";
    foreach my $file (sort keys %{ $self->{files} }) {
        $xml .= '<LogFile>' . $file . "</LogFile>\n";
    }
    $xml .= "</LogFiles>\n";

    $xml .= "<Modules>\n";
    # Sort by execution start time
    my @sorted_modules = map { $_->[1] }
                         sort { $a->[0] cmp $b->[0] }
                         map { [ substr($_->execution_start,0,19), $_ ] }
                         @{ $self->{modules} };
    foreach my $module (@sorted_modules) {
        $xml .= $module->as_xml();
    }
    $xml .= "</Modules>\n";


    $xml .= "</BioscopeLogDirectory>\n";
    return $xml;
}


sub _element {
    my $elem = shift;
    my $content = shift;
    return '<'.$elem.'>'.$content.'</'.$elem.">\n";
}



1;
__END__


=head1 NAME

QCMG::Bioscope::LogDirectory - Parse a Bioscope log/ directory


=head1 SYNOPSIS

 use QCMG::Bioscope::LogDirectory;


=head1 DESCRIPTION

This module provides an interface for reading and procesing files from a
Bioscope log directory.  A Bioscope run is typically managed as a series
of PLugins that are run sequentially and where each Bioscope Plugin launches
it's own jobs and writes it's own log files.  There are two files that
every module produces - the B<main> and the B<parameters> files.

For example, the main and parameters files from a SOLiD fragment ru that
involved mapping, small-indel detection and BAM creation might have the
following 3 pairs of files:

 Mapping-1-20100803135509252.parameters
 Mapping.1.main.20100803135509252.log
 MaToBAM-1-20100810053310100.parameters
 MaToBAM.1.main.20100810053310100.log
 SmallIndelFragFinding-2-20100804021924357.parameters
 SmallIndelFragFinding.2.main.20100804021924357.log

The main files are named according to the convention:

 MMMMM.CCCCC.main.DDDDD.log

where MMMMM is the module name, CCCCC is an integer counter incremented
each time a module is run or attempted to be run, and DDDDD is a
datestamp.  The matching parameters file uses the same values for MMMMM,
CCCCC and DDDDD but the naming convention is different:

 MMMMM-CCCCC-DDDDD.parameters


=head1 PUBLIC METHODS

=over

=item B<new()>

 my $bld = QCMG::Bioscope::LogDirectory->new(
                directory => '~/S00414_20100804_1_frag/20100814/log' );  

A directory parameter must be supplied to this constructor.

=item B<directory()>
 
 $bld->directory();

Returns the name of the directory processed by this object.

=item B<verbose()>

 $prop->verbose();

Returns the verbose status for this object where 0 sets verbose off 
and any other value (traditionally 1) sets verbose mode on.

=item B<files()>

 $bld->files();

Returns a reference to a has of the names of all the files found in the
log directory.  Note that this reference is to the internal filename
hash so the user shauold not modify this hash and in many cases would be
better served by using B<file_names()> instead..

=item B<file_names()>

 $bld->file_names();

Returns a sorted array of the names of all the files found in the
Bioscope log directory.

=back


=head1 AUTHORS

=over

=item John Pearson L<mailto:j.pearson@uq.edu.au>

=back


=head1 VERSION

$Id$


=head1 COPYRIGHT

Copyright (c) The University of Queensland 2009-2014

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
