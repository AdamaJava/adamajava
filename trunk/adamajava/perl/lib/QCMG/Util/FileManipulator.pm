package QCMG::Util::FileManipulator;

##############################################################################
#
#  Module:   QCMG::Util::FileManipulator.pm
#  Creator:  John Pearson
#  Created:  2012-10-19
#
#  Factory for methods that operate on text files.
#
#  $Id: FileManipulator.pm 4665 2014-07-24 08:54:04Z j.pearson $
#
##############################################################################

use strict;
use warnings;
use Data::Dumper;
use IO::File;
use Carp qw( croak carp );
use POSIX;

use QCMG::Util::QLog;

use vars qw( $SVNID $REVISION );

( $REVISION ) = '$Revision: 4665 $ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id: FileManipulator.pm 4665 2014-07-24 08:54:04Z j.pearson $'
    =~ /\$Id:\s+(.*)\s+/;


sub new {
    my $class  = shift;
    my %params = @_;

    my $self = { records_processed    => 0,
                 verbose              => ($params{verbose} ?
                                          $params{verbose} : 0),
               };

    bless $self, $class;
}


sub records_processed {
    my $self = shift;
    return $self->{'records_processed'};
}


sub verbose {
    my $self = shift;
    return $self->{'verbose'} = shift if @_;
    return $self->{'verbose'};
}


sub move_columns_from_front_to_back {
    my $self   = shift;
    my %params = @_;

    die "Must supply infile, outfile, count params" unless
        (exists $params{infile} and exists $params{outfile} and
         exists $params{count});

    my $infile = $params{infile};
    my $infh = IO::File->new( $infile, 'r');
    die "Cannot open file for reading [$infile]: $!\n"
        unless (defined $infh);

    my $outfile = $params{outfile};
    my $outfh = IO::File->new( $outfile, 'w');
    die "Cannot open file for writing [$outfile]: $!\n"
        unless (defined $outfh);

    qlogprint( 'Moving '. $params{count} .
               ' columns from front to back on file '.
               $params{infile} ."\n") if $self->verbose;

    my $column_total = 0;
    my $column_move  = $params{count};

    my $linectr = 0;
    while( my $line = $infh->getline ) {
        $linectr++;
        # Comments and blank lines get written straight to output
        if ($line =~ /^#/ or $line =~ /^\s*$/) {
            $outfh->print( $line );
            next;
        }

        # We will need to reterminate the line after columns are shifted
        # so we better strip the terminator off now.
        chomp $line;

        # We will require that all lines have an indentical number of
        # column otherwise our column moving will scramble the file so
        # we will use the first data line (ideally column headers) to
        # determine the required column count.

        if ($column_total == 0) {
            my @fields = split /\t/, $line;
            $column_total = scalar(@fields);
            # Make sure we have enough columns to do the move!
            die "First data line has only $column_total fields but ".
                "$column_move were specified to be moved\n"
                unless ($column_total > $column_move);
        }

        # Do the move
        my @fields = split /\t/, $line;
        die "Line $linectr has ". scalar(@fields) .
            " fields but is supposed to have $column_total\n"
            unless (scalar(@fields) == $column_total);
        # Use slices to create new line
        my @new_fields = ( @fields[ $column_move .. ($column_total-1) ],
                           @fields[ 0 .. ($column_move-1) ] );

        $outfh->print( join("\t",@new_fields) . "\n" );
    }

    $outfh->close;
    $infh->close;

    return $self->{'records_processed'} = $linectr;
}


sub add_lines_at_top {
    my $self   = shift;
    my %params = @_;

    die "Must supply infile, outfile, lines params" unless
        (exists $params{infile} and exists $params{outfile} and
         exists $params{lines});

    my $infile = $params{infile};
    my $infh = IO::File->new( $infile, 'r');
    die "Cannot open file for reading [$infile]: $!\n"
        unless (defined $infh);

    my $outfile = $params{outfile};
    my $outfh = IO::File->new( $outfile, 'w');
    die "Cannot open file for writing [$outfile]: $!\n"
        unless (defined $outfh);

    qlogprint( 'Adding '. scalar(@{$params{lines}}) .
               ' lines to top of file '.
               $params{infile} ."\n") if $self->verbose;

    # Print lines to be placed at top of file
    $outfh->print( $_ ) foreach @{ $params{lines} };

    # Copy the file from infile to outfile
    while( my $line = $infh->getline ) {
        $outfh->print( $line );
    }

    $infh->close;
    $outfh->close;

    return 0;
}


sub drop_columns {
    my $self   = shift;
    my %params = @_;

    die "Must supply infile, outfile, lines params" unless
        (exists $params{infile} and exists $params{outfile} and
         exists $params{columns});

    my $infile = $params{infile};
    my $infh = IO::File->new( $infile, 'r');
    die "Cannot open file for reading [$infile]: $!\n"
        unless (defined $infh);

    my $outfile = $params{outfile};
    my $outfh = IO::File->new( $outfile, 'w');
    die "Cannot open file for writing [$outfile]: $!\n"
        unless (defined $outfh);

    qlogprint( 'Dropping columns '. join(',',@{$params{columns}}) .
               ' from file '.
               $params{infile} ."\n") if $self->verbose;

    # Create hash of columns to be dropped
    my %drops = ();
    $drops{ $_ } = 1 foreach @{$params{columns}};

    # Copy the file from infile to outfile
    while( my $line = $infh->getline ) {
        chomp $line;
        my @fields = split /\t/, $line;
        # Create slice index without dropped columns
        my @keepers = grep { ! exists $drops{$_} } 
                      ( 0 .. $#fields );
        $outfh->print( join("\t",@fields[ @keepers ]), "\n" );
    }

    $infh->close;
    $outfh->close;

    return 0;
}


1;
__END__


=head1 NAME

QCMG::Util::FileManipulator - Perl module for manipulating text files


=head1 SYNOPSIS

 use QCMG::Util::FileManipulator;


=head1 DESCRIPTION

This module in a factory for operating on text files.  Most methods will
take input and output filenames and some parameters to drive the
transformation.


=head1 PUBLIC METHODS

=over

=item B<new()>

 my $fm = QCMG::Util::FileManipulator->new( verbose => 0 );

=item B<move_columns_from_front_to_back()>

 $fm->move_columns_from_front_to_back( infile  => 'infile.txt',
                                       outfile => 'outfile.txt',
                                       count   => 3 );

Moves a specified number of columns from the front of a tab-separated
file to the back.  Comment (starts with # char) and blank lines are
moved without modification.  Every line must have an identical number
of columns and that number is determined by the field count in the 
first line processed.  Note that if there are column headers but they
are in a line starting with '#' then they will be treated as comments so
they will appear in the file BUT they will not have been moved so the
headers will no longer match up with the data.

=item B<add_lines_at_top()>

 $fm->add_lines_at_top( infile  => 'infile.txt',
                        outfile => 'outfile.txt',
                        lines   => [ $line1, $line2 ] );

Add the specified lines at the top of the file.  Note that this does
exactly what it says so if you already have a header line, the new lines
will go before the header line.  Also note that the user is responsible
for adding newline chars to any lines prior to passing them to this
method.  Newlines are not added.

=item B<drop_columns()>

 $fm->drop_columns( infile  => 'infile.txt',
                    outfile => 'outfile.txt',
                    columns => [ 0,2,4,3 ] );

Takes a list of columns to be dropped from a tab-seperated text file and
writes an outfile with those columns missing.  The column list does not
have to be contiguous or in order.  This method uses hashes and slices to
do the dropping so it may be a bit slow if used on huge files.

=item B<verbose()>

 $fm->verbose();
 $fm->verbose( 1 );

Returns the verbose status for this object where 0 sets verbose off 
and any other value (traditionally 1) sets verbose mode on.

=back


=head1 AUTHORS

=over

=item John Pearson L<mailto:j.pearson@uq.edu.au>

=back


=head1 VERSION

$Id: FileManipulator.pm 4665 2014-07-24 08:54:04Z j.pearson $


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
