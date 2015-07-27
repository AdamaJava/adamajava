package QCMG::IO::LogFileReader;

###########################################################################
#
#  Module:   QCMG::IO::LogFileReader
#  Creator:  John V Pearson
#  Created:  2014-01-08
#
#  Read Log4J (?) formati log files.
#
#  $Id$
#
###########################################################################

use strict;
use warnings;

use Data::Dumper;
use IO::File;
use Carp qw( confess );
use vars qw( $SVNID $REVISION );

use QCMG::Util::QLog;

( $REVISION ) = '$Revision$ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id$'
    =~ /\$Id:\s+(.*)\s+/;


sub new {
    my $class = shift;
    my %params = @_;

    confess "QCMG::IO::LogFileReader:new() requires the filename parameter" 
        unless (exists $params{filename} and defined $params{filename});

    my $self = { filename        => $params{filename},
                 record_ctr      => 0,
                 lines           => [],
                 verbose         => ($params{verbose} ?
                                     $params{verbose} : 0),
               };

    bless $self, $class;

    $self->_parse;

    return $self;
}


sub filename {
    my $self = shift;
    return $self->{filename} = shift if @_;
    return $self->{filename};
}

sub filehandle {
    my $self = shift;
    return $self->{filehandle} = shift if @_;
    return $self->{filehandle};
}

sub verbose {
    my $self = shift;
    return $self->{verbose};
}


sub _incr_record_ctr {
    my $self = shift;
    return $self->{record_ctr}++;
}


sub record_ctr {
    my $self = shift;
    return $self->{record_ctr};
}


sub lines {
    my $self = shift;
    return @{ $self->{lines} };
}


sub _parse {
    my $self = shift;

    my $fh = IO::File->new( $self->filename, 'r' );
    confess 'Unable to open ', $self->filename, " for reading: $!"
        unless defined $fh;
    $self->filehandle( $fh );

    my $total_lines = 0;
    my $unparseable = 0;

    while (my $line = $self->filehandle->getline()) {
        chomp $line;
        next unless $line;
        $total_lines++;
        if ($self->verbose) {
            # Print progress messages for every 1M records
            $self->_incr_record_ctr;
            qlogprint( $self->record_ctr, " records processed\n" )
                if $self->record_ctr % 100000 == 0;
        }
        # 11:19:08.881 [main] INFO org.qcmg.motif.JobQueue - 
        if ($line =~ /([\d\.\:]+)\s+\[(.+)\]\s+([^\s]+)\s+([^\s]+) - (.*)$/) {
            my $time    = $1;
            my $thread  = $2;
            my $label   = $3;
            my $class   = $4;
            my $message = $5;
            push @{ $self->{lines} }, { 'time'  => $time,
                                        thread  => $thread,
                                        label   => $label,
                                        class   => $class,
                                        message => $message };
        }
        else {
            warn "Unable to parse line: $line\n" if $self->verbose;
            $unparseable++;
        }
    }
    qlogprint "$unparseable unparseable lines out of $total_lines\n";
}


1;

__END__


=head1 NAME

QCMG::IO::LogFileReader - log4J file reader


=head1 SYNOPSIS

 use QCMG::IO::LogFileReader;


=head1 DESCRIPTION

This module provides an interface for reading Log4J formatted log files.


=head1 AUTHORS

John Pearson L<mailto:j.pearson@uq.edu.au>


=head1 VERSION

$Id$


=head1 COPYRIGHT

Copyright (c) The University of Queensland 2014
Copyright (c) John Pearson 2014

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
