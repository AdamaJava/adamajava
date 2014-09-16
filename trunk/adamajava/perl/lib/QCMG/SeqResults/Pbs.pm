package QCMG::SeqResults::Pbs;

###########################################################################
#
#  Module:   QCMG::SeqResults::Pbs.pm
#  Creator:  John V Pearson
#  Created:  2010-06-02
#
#  Create PBS file stubs.
#
#  $Id: Pbs.pm 4665 2014-07-24 08:54:04Z j.pearson $
#
###########################################################################

use strict;
use warnings;

use IO::File;
use IO::Zlib;
use Data::Dumper;
use vars qw( $VERSION @ISA $TODAY );

( $VERSION ) = '$Revision: 4665 $ ' =~ /\$Revision:\s+([^\s]+)/;

BEGIN {
    # Construct a class static variable
    my @fields = localtime();
    my $year  = 1900 + $fields[5];
    my $month = substr( '00' . ($fields[4]+1), -2, 2);
    my $day   = substr( '00' . $fields[3], -2, 2);
    $TODAY = join('-',$year,$month,$day);
}   


###########################################################################
#                          PUBLIC METHODS                                 #
###########################################################################


sub new {
    my $class  = shift;
    my %params = @_;
    
    my $self = { _today   => $TODAY,
                 _version => $VERSION,
                 template => _set_template() };

    # Set values from input %params or from defaults.  The defaults hash
    # also serves as a list of all values that could possibly be subject
    # to replacement in the template.
    my %defaults = ( job_name  => 'qcmg_pbs',
                     queue     => 'batch',
                     account   => 'sf-QCMG',
                     filename  => 'qcmg_run.pbs',
                     resources => 'walltime=24:00:00',
                     email     => $ENV{QCMG_EMAIL},
                     content   => '',
                     verbose   => 0,
                   );

    foreach my $key (keys %defaults) {
        # Only set from %params if exists
        if (exists $params{$key}) {
            $self->{ $key } = $params{ $key };
        }
        else {
            $self->{ $key } = $defaults{ $key };
        }
    }
    $self->{defaults} = \%defaults;

    bless $self, $class;
}


sub job_name {
    my $self = shift;
    return $self->{job_name} = shift if @_;
    return $self->{job_name};
}


sub queue {
    my $self = shift;
    return $self->{queue} = shift if @_;
    return $self->{queue};
}


sub account {
    my $self = shift;
    return $self->{account} = shift if @_;
    return $self->{account};
}


sub filename {
    my $self = shift;
    return $self->{filename} = shift if @_;
    return $self->{filename};
}


sub resources {
    my $self = shift;
    return $self->{resources} = shift if @_;
    return $self->{resources};
}


sub email {
    my $self = shift;
    return $self->{email} = shift if @_;
    return $self->{email};
}


sub content {
    my $self = shift;
    return $self->{content} = shift if @_;
    return $self->{content};
}


sub verbose {
    my $self = shift;
    return $self->{verbose} = shift if @_;
    return $self->{verbose};
}


sub as_text {
    my $self = shift;

    my $template = $self->_template();

    # Replace all replaceable values
    foreach my $key (keys %{ $self->{defaults} }) {
        my $pattern     = '~QQ_' . uc($key) . '~';
        my $replacement = $self->{$key};
        $template =~ s/$pattern/$replacement/g;
    }

    return $template;
}


sub write_file {
    my $self = shift;
    
    my $outfile = $self->filename;
    my $outfh = IO::File->new( $outfile, 'w' );
    die("Cannot open output file $outfile for writing: $!\n")
         unless defined $outfh;

    # Make sure all of the ~QQ_*~ patterns have been substituted
    my $text = $self->as_text;
    if ($text =~ /(~QQ_[\w\d-]+~)/) {
        die "\nPBS file $outfile cannot be written because there is an ",
            "un-substituted variable [$1] in the file contents:\n\n", $text;
    }

    print $outfh $text;

    $outfh->close;
}


sub _set_template {

    my $template = <<'_EO_TEMPLATE_';

#PBS -N ~QQ_JOB_NAME~
#PBS -q ~QQ_QUEUE~
#PBS -A ~QQ_ACCOUNT~
#PBS -S /bin/bash
#PBS -r n
#PBS -l ~QQ_RESOURCES~
#PBS -m ae
#PBS -M ~QQ_EMAIL~

module load samtools
module load adama

~QQ_CONTENT~
_EO_TEMPLATE_

    return $template;
}


1;
__END__


=head1 NAME

QCMG::SeqResults::Pbs - Create PBS files


=head1 SYNOPSIS

 use QCMG::SeqResults::Pbs;
 my $pbs = QCMG::SeqResults::Pbs->new( %params );
 $pbs->write( '/panfs/imb/home/uqjpear1/tmp/my_run_file.pbs' );


=head1 DESCRIPTION

This module creates PBS files ready to be run on the QCMG clusters
(barrine and qcmg-clustermk2).  It comes ready with default values for
all parameters except for the actual contents of the script.
All the user-supplied values can be specified in the call to B<new()> or
they can be supplied by calling the set accessor methods.


=head1 PUBLIC METHODS

See documentation for the superclass QCMG::Bioscope::Ini::IniFile.


=head1 SEE ALSO

=over

=item QCMG::Bioscope::Ini::IniFile

=back


=head1 AUTHORS

=over

=item John Pearson L<mailto:j.pearson@uq.edu.au>

=back


=head1 VERSION

$Id: Pbs.pm 4665 2014-07-24 08:54:04Z j.pearson $


=head1 COPYRIGHT

Copyright (c) The University of Queensland 2010,2011

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
