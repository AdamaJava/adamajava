package QCMG::IO::SamHeader;

###########################################################################
#
#  Module:   QCMG::IO::SamHeader
#  Creator:  John V Pearson
#  Created:  2014-05-16
#
#  Parses SAM/BAM file header
#
#  $Id: SamHeader.pm 4643 2014-06-16 07:16:11Z j.pearson $
#
###########################################################################

use strict;
use warnings;
use IO::File;
use Data::Dumper;
use Carp qw( carp croak );

use QCMG::IO::SamRecord;
use QCMG::Util::QLog;

use vars qw( $SVNID $REVISION $VERSION $VERBOSE );

( $REVISION ) = '$Revision: 4643 $ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id: SamHeader.pm 4643 2014-06-16 07:16:11Z j.pearson $'
    =~ /\$Id:\s+(.*)\s+/;


sub new {
    my $class = shift;
    my %params = @_;

    croak "SamHeader:new() requires a block of header text" 
        unless (exists $params{header} and defined $params{header});

    my $self = { original_text   => $params{header},
                 HD              => [],
                 SQ              => [],
                 RG              => [],
                 PG              => [],
                 CO              => [],
                 verbose         => ($params{verbose} ?
                                     $params{verbose} : 0),
               };

    bless $self, $class;

    $self->_initialise;

    return $self;
}


sub HD {
    my $self = shift;
    return $self->{HD};
}

sub SQ {
    my $self = shift;
    return $self->{SQ};
}

sub RG {
    my $self = shift;
    return $self->{RG};
}

sub PG {
    my $self = shift;
    return $self->{PG};
}

sub CO {
    my $self = shift;
    return $self->{CO};
}

sub add_CO {
    my $self = shift;
    my $line = shift;
    push @{ $self->{CO} }, $line;
}


sub add_PG {
    my $self = shift;
    my $line = shift;
    push @{ $self->{PG} }, $line;
}


sub replace_RG_array {
    my $self      = shift;
    my $array_ref = shift;
    $self->{RG} = $array_ref;
}


sub replace_CO_array {
    my $self      = shift;
    my $array_ref = shift;
    $self->{CO} = $array_ref;
}


sub verbose {
    my $self = shift;
    return $self->{verbose};
}


sub _initialise {
    my $self = shift;

    my @lines = split /\n/, $self->{original_text};

    foreach my $line (@lines) {
        chomp $line;

        if ($line =~ /^\@HD/) {
            push @{ $self->{HD} }, $line;
        }
        elsif ($line =~ /^\@SQ/) {
            push @{ $self->{SQ} }, $line;
        }
        elsif ($line =~ /^\@RG/) {
            push @{ $self->{RG} }, $line;
        }
        elsif ($line =~ /^\@PG/) {
            push @{ $self->{PG} }, $line;
        }
        elsif ($line =~ /^\@CO/) {
            push @{ $self->{CO} }, $line;
        }
        else {
            die "cannot parse BAM header line [$line]\n";
        }
    }
}


sub as_text {
    my $self = shift;

    my $text = '';
    $text .= "$_\n" foreach @{ $self->{HD} };
    $text .= "$_\n" foreach @{ $self->{SQ} };
    $text .= "$_\n" foreach @{ $self->{RG} };
    $text .= "$_\n" foreach @{ $self->{PG} };
    $text .= "$_\n" foreach @{ $self->{CO} };

    return $text;
}




1;

__END__


=head1 NAME

QCMG::IO::SamHeader - SAM/BAM file header


=head1 SYNOPSIS

 use QCMG::IO::SamHeader;

 my $bam = QCMG::IO::SamReader->new( filename => $infile );
 my $head = QCMG::IO::SamHeader->new( header = $bam->headers_text );


=head1 DESCRIPTION

This module provides an interface for parsing SAM/BAM file headers.


=head1 PUBLIC METHODS

=over 2

=item B<new()>

 my $head = QCMG::IO::SamHeader->new( header => $header,
                                      verbose  => 1 );

The B<new()> method takes 1 mandatory and 1 optional parameters.  The
mandatory param is I<header> which is the text string to be parsed as a
SAM/BAM file header and the optional param is I<verbose> which defaults
to 0 and indicates the level of verbosity in reporting.

=item B<verbose()>

 $bam->verbose( 2 );
 my $verb = $bam->verbose;

Accessor for verbosity level of progress reporting.

=back


=head1 AUTHORS

John Pearson L<mailto:john.pearson@qimrberghofer.edu.au>


=head1 VERSION

$Id: SamHeader.pm 4643 2014-06-16 07:16:11Z j.pearson $


=head1 COPYRIGHT

This software is copyright 2014 by the QIMR Berghofer Medical Research
Institute. All rights reserved.  This License is limited to, and you
may use the Software solely for, your own internal and non-commercial
use for academic and research purposes. Without limiting the foregoing,
you may not use the Software as part of, or in any way in connection with 
the production, marketing, sale or support of any commercial product or
service or for any governmental purposes.

In any work or product derived from the use of this Software, proper 
attribution of the authors as the source of the software or data must be 
made.  The following URL should be cited:

  http://sourceforge.net/admamjava/

=cut
