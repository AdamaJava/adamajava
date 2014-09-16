package QCMG::Bioscope::LogModule;

##############################################################################
#
#  Module:   QCMG::Bioscope::LogModule.pm
#  Creator:  John V Pearson
#  Created:  2010-08-11
#
#  Parses a main/parameter file pair from a Bioscope-created log directory
#
###########################################################################

use strict;
use warnings;

use IO::File;
use Data::Dumper;
use Carp qw( carp croak cluck confess );
use File::Find;
use POSIX;

use vars qw( $SVNID $REVISION );

( $REVISION ) = '$Revision: 4660 $ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id: LogModule.pm 4660 2014-07-23 12:18:43Z j.pearson $'
    =~ /\$Id:\s+(.*)\s+/;


sub new {
    my $class  = shift;
    my %params = @_;
    
    die "QCMG::Bioscope::LogModule->new() requires dir, main and param",
        'parameters '
        unless ( (exists $params{dir} and $params{dir}) and
                 (exists $params{main} and $params{main}) and
                 (exists $params{params} and $params{params}) );

    my $self = { dir                => $params{dir},
                 type               => $params{type},
                 counter            => $params{counter},
                 timestamp          => $params{timestamp},
                 main_file          => $params{main},
                 param_file         => $params{params},
                 execution_start    => '',
                 execution_end      => '',
                 execution_duration => '',
                 bioscope_version   => '',
                 status             => 'SUCCESS',
                 params             => {},
                 subjobs            => [],
                 verbose            => $params{verbose} || 0 };
    bless $self, $class;

    $self->_process_main_file();
    $self->_process_param_file();

    return $self;
}


sub dir {
    my $self = shift;
    return $self->{dir};
}


sub main_file {
    my $self = shift;
    return $self->{main_file};
}


sub param_file {
    my $self = shift;
    return $self->{param_file};
}


sub verbose {
    my $self = shift;
    return $self->{verbose};
}


sub execution_start {
    my $self = shift;
    return $self->{execution_start} = shift if @_;
    return $self->{execution_start};
}

sub execution_end {
    my $self = shift;
    return $self->{execution_end} = shift if @_;
    return $self->{execution_end};
}

sub execution_duration {
    my $self = shift;
    return $self->{execution_duration} = shift if @_;
    return $self->{execution_duration};
}

sub bioscope_version {
    my $self = shift;
    return $self->{bioscope_version} = shift if @_;
    return $self->{bioscope_version};
}

sub status {
    my $self = shift;
    return $self->{status} = shift if @_;
    return $self->{status};
}


sub _process_main_file {
    my $self = shift;

    my $filename = $self->dir . $self->main_file;
    my $infh = IO::File->new( $filename, 'r' );
    die "Cannot open main log file $filename for reading: $!"
        unless defined $infh;

    while (my $line = $infh->getline) {
        if ($line =~ /PluginRunner:/) {
            chomp $line;
            if ($line =~ /START of PluginRunner.*date=(.*?)\s*$/) {
                $self->execution_start( $1 );
            }
            elsif ($line =~ /END of PluginRunner.*date=(.*?)\s*$/) {
                $self->execution_end( $1 );
            }
            elsif ($line =~ /END of PluginRunner.*DURATION=(.*?)\s*$/) {
                $self->execution_duration( $1 );
            }
            elsif ($line =~ /Bioscope version: (.*?)\s*$/) {
                $self->bioscope_version( $1 );
            }
            elsif ($line =~ /SYSTEM INFO[ >]+(.*?)\s*$/) {
                push @{ $self->{system} }, $1;
            }
            elsif ($line =~ /FATAL(.*Exception:)(.*)/) {
                $self->status('FATAL: ' . $2);
            }
        }
        elsif ($line =~ /^#PBS\s+\-o\s+\'(.*?)\'/) {
            push @{ $self->{subjobs} }, $1;
        }
    }
}


sub _process_param_file {
    my $self = shift;

    my $filename = $self->dir . $self->param_file;
    my $infh = IO::File->new( $filename, 'r' );
    die "Cannot open params log file $filename for reading: $!"
        unless defined $infh;

    my %params = ();
    while (my $line = $infh->getline) {
        next if ($line =~ /^#/);
        chomp $line;
        my ($key,$value) = split( /=/, $line, 2);
        $key =~ s/^\s+//;
        $key =~ s/\s+$//;
        $value =~ s/^\s+//;
        $value =~ s/\s+$//;
        $params{ $key } = $value;
    }

    $self->{params} = \%params;
}


sub as_xml {
    my $self = shift;
    my $xml = '';

    $xml .= '<BioscopeLogModule type="' . $self->{type} . '"' .
                           ' counter="' . $self->{counter} . '"' .
                         ' timestamp="' . $self->{timestamp} . '"' .  ">\n";

    $xml .= "<ModuleInfo>\n";
    $xml .= _element('ExecutionStatus',$self->status);
    $xml .= _element('Directory',$self->dir);
    $xml .= _element('MainFile',$self->main_file);
    $xml .= _element('ParametersFile',$self->param_file);
    $xml .= _element('BioscopeVersion',$self->{bioscope_version});
    $xml .= _element('ExecutionStart',$self->{execution_start});
    $xml .= _element('ExecutionEnd',$self->{execution_end});
    $xml .= _element('ExecutionDuration',$self->{execution_duration});
    $xml .= '<System>';
    $xml .= join('; ', @{ $self->{system} });
    $xml .= "</System>\n";
    $xml .= "</ModuleInfo>\n";

    $xml .= "<Parameters>\n";
    foreach my $key (sort keys %{ $self->{params} }) {
        $xml .= '<Parameter name="' . $key . '" value="' .
                $self->{params}->{$key} . "\"/>\n";
    }
    $xml .= "</Parameters>\n";

    $xml .= "<SubJobs>\n";
    foreach my $job (@{ $self->{subjobs} }) {
        $xml .= '<SubJob>' . $job . "</SubJob>\n";
    }
    $xml .= "</SubJobs>\n";

    $xml .= "</BioscopeLogModule>\n";
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

QCMG::Bioscope::LogModule - Bioscope Plugin logs


=head1 SYNOPSIS

 use QCMG::Bioscope::LogModule;


=head1 DESCRIPTION

This module provides an interface for reading Bioscope log files that
relate to a particular instance of a Bioscope plugin and particularly to
making available the information held in the main and parameters files.

This module will not usually be directly invoked by the user who instead
should use module QCMG::Bioscope::LogDirectory to access all of the
Bioscope modules from a particular run.


=head1 PUBLIC METHODS

=over

=item B<new()>

 my $blm = QCMG::Bioscope::LogModule->new(
                dir   => '~/S00414_20100804_1_frag/20100814/log',
                main  => 'Mapping.1.main.20100602000012675.log',
                param => 'Mapping-1-20100602000012675.parameters' );


The dir, main and param parameters must be supplied to this constructor.

=item B<dir()>
 
 $blm->dir();

Returns the name of the log directory where this module is located.

=item B<main_file()>
 
 $blm->mail_file();

Returns the name of the main log file being processed.

=item B<param_file()>
 
 $blm->param_file();

Returns the name of the parameters  log file being processed.

=item B<verbose()>

 $blm->verbose();

Returns the verbose status for this object where 0 sets verbose off 
and any other value (traditionally 1) sets verbose mode on.

=back


=head1 AUTHORS

=over

=item John Pearson L<mailto:j.pearson@uq.edu.au>

=back


=head1 VERSION

$Id: LogModule.pm 4660 2014-07-23 12:18:43Z j.pearson $


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
