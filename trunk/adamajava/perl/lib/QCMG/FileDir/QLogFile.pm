package QCMG::FileDir::QLogFile;

##############################################################################
#
#  Module:   QCMG::FileDir::QLogFile.pm
#  Creator:  John V Pearson
#  Created:  2010-09-16
#
#  This class implements a parser and data structure for files in the
#  QCMG standard log file format as written by QCMG::Util::QLog.pm.
#
#  $Id$
#
##############################################################################

use strict;
use warnings;
use Data::Dumper;

use QCMG::FileDir::DirectoryObject;
use QCMG::FileDir::FileObject;
use QCMG::Util::QLog;

use vars qw( $SVNID $REVISION );

( $REVISION ) = '$Revision$ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id$'
    =~ /\$Id:\s+(.*)\s+/;


sub new {
    my $class  = shift;
    my %params = @_;

    die "You must supply a file parameter to a new QLogFile"
       unless (exists $params{file} and $params{file});

    my $self = { file        => $params{file},
                 linectr     => 0,
                 lines       => [],
                 parsed      => [],
                 unparsed    => [],
                 verbose     => ($params{verbose} ? $params{verbose} : 0) };
    bless $self, $class;

    $self->_parse_log_file;

    return $self;
}


sub verbose {
    my $self = shift;
    return $self->{verbose};
}


sub file {
    my $self = shift;
    return $self->{file};
}


sub linectr {
    my $self = shift;
    return $self->{linectr};
}


sub _parse_log_file {
    my $self = shift;

    my $pathname = $self->file;
    local( *FH ) ;
    open( FH, $pathname ) or die "Cannot open file: $pathname\n";
    my $contents = do { local( $/ ) ; <FH> };
    my @lines = split("\n", $contents);

    $self->{linectr} = scalar @lines;
    $self->{lines}   = \@lines;
    my @parsed       = ();
    my @unparsed     = ();

    foreach my $ctr (0..$#lines) {
        my $line = $lines[$ctr];
        # For execution speed, this switch statement should be organised
        # from most common to least common patterns so we waste the
        # minimum possible amount of time in useless regexes.

        #if ($line =~ /^([^\s]+)\s\[([^\s]+)\]\s([^\s]+)\s([^\s])\s\-\s(.*)$/) {
        if ($line =~ /^([^\s]+)\s+\[([^\s]+)\]\s+([^\s]+)\s+([^\s]+)\s+\-\s+(.*)$/) {
            push @parsed, { 'timestamp' => $1,
                            'thread'    => $2,
                            'loglevel'  => $3,
                            'class'     => $4,
                            'message'   => $5 };
        }
        else {
            qlogprint {l=>'WARN'}, "unable to parse log file line $line\n"
                if $self->verbose;
            push @unparsed, [$ctr,$line];
        }
    }

    qlogprint {l=>'WARN'}, scalar(@unparsed),
                           " lines could not be parsed from $pathname\n"
        if scalar(@unparsed);

    $self->{parsed}   = \@parsed;
    $self->{unparsed} = \@unparsed;
}


sub lines_by_loglevel {
    my $self    = shift;
    my $pattern = shift;

    my @lines = ();
    foreach my $rh_line (@{ $self->{parsed} }) {
        push @lines, $rh_line if ($rh_line->{'loglevel'} =~ /$pattern/i);
    }

    return \@lines;
}


sub attributes_from_exec_lines {
    my $self = shift;

    my $ra_execs = $self->lines_by_loglevel('EXEC');
    my %attribs = ();

    # Extract info from the EXEC lines
    foreach my $rh_line (@{ $ra_execs }) {
        if ($rh_line->{message} =~ /([^\s]+)\s+(.*)$/) {
            $attribs{$1} = $2;
        }
        else {
            qlogprint {l=>'WARN'}, 'unable to parse EXEC line ',
                                   $rh_line->{message},"\n";
        }
    } 

    return \%attribs;
}


1;
__END__


=head1 NAME

QCMG::FileDir::QLogFile - parser for QLog format report files


=head1 SYNOPSIS

 use QCMG::FileDir::QLogFile;

 my $file = QCMG::FileDir::QLogFile->new( file => 'mylog.log' );


=head1 DESCRIPTION

This module provides a parser and data structure for accessing the
contents of log files in the QCMG standard format as written by
QCMG::Util::QLog.pm.


=head1 PUBLIC METHODS

=over

=item B<new()>

 my $file = QCMG::FileDir::QLogFile->new( file => 'mylog.log',
                                          verbose => 1 );

=item B<linecount()>

=item B<lines_by_loglevel>

 my $ra_lines = $file->lines_by_loglevel( 'INFO' );

This is the easiest way to retrieve blocks of lines from a QLog file for
further parsing.  Typical loglevel values inlcude EXEC, TOOL, INFO, WARN
and FATAL.

=item B<attributes_from_exec_lines>

=item B<verbose()>

 $file->verbose();

Returns the verbose status for this object where 0 sets verbose off 
and any other value (traditionally 1) sets verbose mode on.

=back


=head1 AUTHORS

=over

=item John Pearson L<mailto:j.pearson@uq.edu.au>

=back


=head1 VERSION

$Id$


=head1 COPYRIGHT

Copyright (c) The University of Queensland 2013-2014

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
