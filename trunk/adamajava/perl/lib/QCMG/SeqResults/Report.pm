package QCMG::SeqResults::Report;

###########################################################################
#
#  Module:   QCMG::SeqResults::Report.pm
#  Creator:  John V Pearson
#  Created:  2011-05-16
#
#  Superclass for reports.
#
#  $Id$
#
###########################################################################

use strict;
use warnings;

use Data::Dumper;
use File::Find;
use Getopt::Long;
use Pod::Usage;
use vars qw( $SVNID $REVISION );

use QCMG::SeqResults::Util qw( qmail );
use QCMG::Util::QLog;


( $REVISION ) = '$Revision$ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id$'
    =~ /\$Id:\s+(.*)\s+/;


sub new {
    my $class  = shift;
    my %params = @_;

    # Allow for ,-separated lists of emails
    my @emails = ();
    if (defined $params{emails}) {
        push @emails, map { split /\,/,$_ } @{ $params{emails} };
    }

    # Create the object
    my $self = { dir     => $params{dir} || '',
                 emails  => \@emails,
                 outfile => $params{outfile} || '',
                 verbose => $params{verbose} || 0,
                 };

    bless $self, $class;
}


sub dir {
    my $self = shift;
    return $self->{dir};
}


sub email {
    my $self = shift;
    return $self->{emails};
}


sub emails {
    my $self = shift;
    return @{ $self->{emails} };
}


sub outfile {
    my $self = shift;
    return $self->{outfile};
}


sub verbose {
    my $self = shift;
    return $self->{verbose};
}


sub execute {
    my $self = shift;
    die "Report subclasses must implement execute()\n";
}

sub report_text {
    my $self = shift;
    die "Report subclasses must implement report_text()\n";
}

sub report_xml_string {
    my $self = shift;
    die "Report subclasses must implement report_xml_string()\n";
}

sub report_xml_object {
    my $self = shift;
    die "Report subclasses must implement report_xml_object()\n";
}


sub output_report {
    my $self = shift;

    # File and email reporting are independent
    if ($self->outfile) {
        if ($self->outfile =~ /\.xml$/) {
            open OUT, '>'.$self->outfile ||
                die 'Unable to open ',$self->outfile," for writing $!";
            qlogprint "writing XML report to ". $self->outfile ."\n";
            print OUT $self->report_xml_string;
            close OUT;
        }
        else {
            open OUT, '>'.$self->outfile ||
                die 'Unable to open ',$self->outfile," for writing $!";
            qlogprint "writing text report to ". $self->outfile ."\n";
            print OUT $self->report_text;
            close OUT;
        }
    }
    else {
        # If no output file specified then dump text report to STDOUT
        print $self->report_text;
    }

    # Email text report to any addresses specified
    if ($self->email and scalar($self->emails)) {
        qlogprint "emailing reports to ". scalar($self->emails) .
                  " recipients\n";
        # qmail copes with $self->email being an arrayref  of addresses
        qmail( To      => $self->email,
               From    => $ENV{QCMG_EMAIL},
               Subject => 'Timelord report',
               Message => $self->report_text );
    }

}



1;
__END__


=head1 NAME

QCMG::SeqResults::Report - Superclass for all Report modules


=head1 SYNOPSIS

 use QCMG::SeqResults::Report;
 my $pbs = QCMG::SeqResults::Pbs->new( %params );
 $pbs->write( '/panfs/imb/home/uqjpear1/tmp/my_run_file.pbs' );


=head1 DESCRIPTION

This module should never be directly used by a programmer unless they
are writing a Reports module that will extend this class.

There are some common behaviours that all Report modules must share and
those will be enforced by this module:

 1. Report modules parse the commandline @ARGV
 2. Report modules have a new method that accepts all parameters that
    could be specified on the commandline.  Parameters to new() are
    always overridden by CLI parameters which allows the programmer to
    set sensible defaults that can be overridden by the user.
 3. Report modules must use QLog.
 4. Report modules must accept a filename to direct output to and if the
    extension is .xml then the output is an XML document otherwise it is
    in plain text.
 5. Report modules must accept one or more email addresses in which case
    the final report (in plain text format) is emailed to each of the
    specified recipients.

=head2 Methods that subclasses must implement

Every Report subclass must implement the following methods.  The
superclass has all of these methods but they are simply "die" statements
so if your subclass doesn't implement these methods and someone calls
one, it will be fatal.  Fair warning given.

=over

=item B<execute()>

=item B<report_text()>

=item B<report_xml_string()>

=item B<report_xml_object()>

=back


=head1 PUBLIC METHODS

=over

=item B<new()>

=back


=head1 SEE ALSO

=over

=item QCMG::SeqResults::ReportCheckBam

=back


=head1 AUTHORS

=over

=item John Pearson L<mailto:j.pearson@uq.edu.au>

=back


=head1 VERSION

$Id$


=head1 COPYRIGHT

Copyright (c) The University of Queensland 2011,2012

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
