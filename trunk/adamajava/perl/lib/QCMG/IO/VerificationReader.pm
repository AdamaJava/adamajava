package QCMG::IO::VerificationReader;

###########################################################################
#
#  Module:   QCMG::IO::VerificationReader
#  Creator:  John V Pearson
#  Created:  2012-04-04
#
#  Reads verification summary text file format as specified by QCMG
#  Research Team (Nic and Karin).
#
#  $Id$
#
###########################################################################

use strict;
use warnings;

use Data::Dumper;
use Carp qw( confess );
use vars qw( $SVNID $REVISION @VALID_COLUMNS );

( $REVISION ) = '$Revision$ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id$'
    =~ /\$Id:\s+(.*)\s+/;


BEGIN {
    @VALID_COLUMNS = ( 'PatientID', 'SampleID', 'ChrPos', 'InputType', 
                       'GeneID', 'MutationClass', 'base_change',
                       'AmpliconStart', 'AmpliconEnd', 'AmpliconSize',
                       'SeqDirection', 'PrimerTM', 'PrimerBarcode',
                       'PrimerPlateID', 'PlatePos', 'PlateIDTf',
                       'PlateIDTr', 'IonTorrentRunID',
                       'Experiment_notes', 'ChrPos', 'Patient_ID',
                       'chr', 'Pos', 'Ref', 'A_TD', 'C_TD', 'G_TD',
                       'T_TD', 'N_TD', 'TOTAL_TD',
                       '', '', '', '', '', '', '', '', 'Verification' );

}

sub new {
    my $class = shift;
    my %params = @_;

    confess "VerificationReader:new() requires a filename or zipname parameter" 
        unless ( (exists $params{filename} and defined $params{filename}) or
                 (exists $params{zipname} and defined $params{zipname}) );

    my $self = { filename        => ( $params{filename} ?
                                      $params{filename} : 0),
                 zipname         => ( $params{zipname} ?
                                      $params{zipname} : 0),
                 version         => '2',
                 record_count    => 0,
                 verbose         => ($params{verbose} ?
                                     $params{verbose} : 0),
               };

    bless $self, $class;

    # If there a zipname, we use it in preference to filename.  We only
    # process one so if both are specified, the zipname wins.

    if ( $params{zipname} ) {
        my $fh = IO::Zlib->new( $params{zipname}, 'r' );
        confess 'Unable to open ', $params{zipname}, " for reading: $!"
            unless defined $fh;
        $self->filename( $params{zipname} );
        $self->filehandle( $fh );
    }
    elsif ( $params{filename} ) {
        my $fh = IO::File->new( $params{filename}, 'r' );
        confess 'Unable to open ', $params{filename}, " for reading: $!"
            unless defined $fh;
        $self->filename( $params{filename} );
        $self->filehandle( $fh );
    }

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


sub record_count {
    my $self = shift;
    return $self->{record_count};
}


sub _incr_record_count {
    my $self = shift;
    return $self->{record_count}++;
}


sub next_record {
    my $self = shift;

    # Read lines, checking for and processing any headers
    # and only return once we have a record

    while (1) {
        my $line = $self->filehandle->getline();
        # Catch EOF
        return undef if (! defined $line);
        chomp $line;

        if ($line =~ /^#/) {
            $self->_process_header_line( $line );
            next;
        }

        $self->_incr_record_count;
        my @fields = split /\t/, $line;

        if ($self->verbose) {
            # Print progress messages for every 10K records
            print( $self->record_count, ' verification records processed: ',
                   localtime().'', "\n" )
                if $self->record_count % 1000 == 0;
        }

        return \@fields
    }
}


sub _process_header_line {
    my $self = shift;
    my $line = shift;

    $line =~ s/^\#//;
    chomp $line;
    my @columns = split /\t/, $line;
    my $problems = 0;
    
    my @my_valid_columns = @VALID_COLUMNS;
    foreach my $expected_column (@my_valid_columns) {
        my $actual_column = shift @columns;
        unless ( $expected_column =~ m/$actual_column/i) {
            warn "Column mismatch - ",
                 "expected [$expected_column] found [$actual_column]\n";
            $problems++;
        }
    }
    die "Unable to continue until all column problems have been resolved\n"
       if ($problems > 0);
}


1;

__END__


=head1 NAME

QCMG::IO::VerificationReader - Verification summary file IO


=head1 SYNOPSIS

 use QCMG::IO::VerificationReader;
  
 my $vcr = QCMG::IO::VerificationReader->new( file => 'verif.txt' );


=head1 DESCRIPTION

This module provides an interface for reading 
the verification summary text file format as specified by QCMG
Research Team (Nic and Karin).  This format has 39 columns:

 1.  PatientID
 2.  SampleID
 3.  ChrPos
 4.  InputType
 5.  GeneID
 6.  MutationClass
 7.  base_change
 8.  AmpliconStart
 9.  AmpliconEnd
 10. AmpliconSize
 11. SeqDirection
 12  PrimerTM
 13. PrimerBarcode
 14. PrimerPlateID
 15. PlatePos
 16. PlateIDTf
 17. PlateIDTr
 18. IonTorrentRunID
 19. Experiment_notes
 20. ChrPos
 21. Patient_ID
 22. chr
 23. Pos
 24. Ref
 25. A_TD
 26. C_TD
 27. G_TD
 28. T_TD
 29. N_TD
 30. TOTAL_TD
 31.
 32. 
 33.
 34.
 35.
 36.
 37.
 38.
 39. Verification


=head1 PUBLIC METHODS

=over

=item B<new()>

 my $vcr = QCMG::IO::VerificationReader->new( file => 'verif.txt' );

=item B<debug()>

 $vcr->debug(1);

Flag to force increased warnings.  Defaults to 0 (off);

=item B<next_record()>

 my $ra_fields = $vcr->next_record();

Reads the next record and returns a ref to an array of fields.

=item B<record_count()>

 print $vcr->record_count();

Returns the number of records read from file so far.

=back


=head1 AUTHORS

=over

=item John Pearson, L<mailto:j.pearson@uq.edu.au>

=back


=head1 VERSION

$Id$


=head1 COPYRIGHT

Copyright (c) The University of Queensland 2012-2014

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
