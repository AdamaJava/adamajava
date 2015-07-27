package QCMG::Bioscope::Ini::GlobalIniFile;

###########################################################################
#
#  Module:   QCMG::Bioscope::Ini::GlobalIniFile.pm
#  Creator:  John V Pearson
#  Created:  2010-08-27
#
#  Create global.ini file.
#
#  $Id$
#
###########################################################################

use strict;
use warnings;

use IO::File;
use IO::Zlib;
use Data::Dumper;
use vars qw( $VERSION @ISA );

use QCMG::Bioscope::Ini::IniFile;
use QCMG::Bioscope::Ini::Parameter;
use QCMG::Bioscope::Ini::ParameterCollection;
use QCMG::Bioscope::Ini::TextBlock;

@ISA = qw( QCMG::Bioscope::Ini::IniFile );

( $VERSION ) = '$Revision$ ' =~ /\$Revision:\s+([^\s]+)/;


###########################################################################
#                          PUBLIC METHODS                                 #
###########################################################################


sub new {
    my $class  = shift;
    my %params = @_;

    # Choose which sort of global.ini is required
    my $template = '';
    if ($params{run_type} =~ /fragment/i) {
        $template = _set_template_fragment();
    }
    elsif ($params{run_type} =~ /matepair/i) {
        $template = _set_template_matepair();
    }
    elsif ($params{run_type} =~ /pairedendBC/i) {
        $template = _set_template_pairedend();
    }
    else {
        die 'QCMG::Bioscope::Ini::GlobalIniFile - does not currently ',
            'understand runtype [', $params{run_type}, ']';
    }

    my $self = $class->SUPER::new( %params,
                                   filename => 'global.ini',
                                   template => $template,
                                   version  => $VERSION ); 

    $self->panasas_mount( $params{panasas_mount} );
    $self->bioscope_install_dir( $params{bioscope_install_dir} );
    $self->pbs_queue( $params{pbs_queue} );
    $self->pbs_exe_queue( $params{pbs_exe_queue} );
    $self->execution_server( $params{execution_server} );
    $self->run_name( $params{run_name} );
    $self->run_date( $params{run_date} );
    $self->barcode( $params{barcode} );
    $self->physical_division( $params{physical_division} );
    $self->f3_read_length( $params{f3_read_length} );
    $self->f3_primary_dir( $params{f3_primary_dir} );
    $self->f3_csfasta( $params{f3_csfasta} );
    $self->f3_qual( $params{f3_qual} );
    $self->primary_lib_id( $params{primary_lib_id} );

    if ($params{run_type} =~ /matepair/i) {
        $self->r3_read_length( $params{r3_read_length} );
        $self->r3_primary_dir( $params{r3_primary_dir} );
        $self->r3_csfasta( $params{r3_csfasta} );
        $self->r3_qual( $params{r3_qual} );
    }

    if ($params{run_type} =~ /pairedend/i) {
        $self->f5_read_length( $params{f5_read_length} );
        $self->f5_primary_dir( $params{f5_primary_dir} );
        $self->f5_csfasta( $params{f5_csfasta} );
        $self->f5_qual( $params{f5_qual} );
    }

    return $self;
}


sub _set_template_fragment {
    return _template_standard_start() .
           _template_common_f3() .
           _pc_common_end();
}


sub _set_template_matepair {
    return _template_standard_start() .
           _template_common_f3() .
           _template_common_r3() .
           _pc_common_end();
}


sub _set_template_pairedend {
    return _template_standard_start() .
           _template_common_f3() .
           _template_common_f5() .
           _pc_common_end();
}


sub _template_standard_start {

    my $template = <<'_EO_TEMPLATE_';

## QCMG ##

qcmg.slide          = ~BS_RUN_NAME~
qcmg.physdiv        = ~BS_PHYS_DIV~
qcmg.barcode        = ~BS_BARCODE~
qcmg.mapset         = ${qcmg.slide}.${qcmg.physdiv}.${qcmg.barcode}

qcmg.library	    = ~BS_PRIMARY_LIB_ID~

run.name            = ${qcmg.slide}
qcmg.date           = ~BS_RUN_DATE~
qcmg.panfs.dir      = ~BS_PANASAS~
queue.name          = ~BS_PBS_EXE_QUEUE~
qcmg.run.dir        = ${qcmg.mapset}
_EO_TEMPLATE_

    return $template;
}


sub _template_common_f3 {

    my $template = <<'_EO_TEMPLATE_';

qcmg.f3.primary.dir = ~BS_F3_PRIMARY_DIR~
qcmg.f3.csfasta     = ~BS_F3_CSFASTA~
qcmg.f3.qual        = ~BS_F3_QUAL~
qcmg.f3.read.length = ~BS_F3_READ_LENGTH~
_EO_TEMPLATE_

    return $template;
}


sub _template_common_r3 {

    my $template = <<'_EO_TEMPLATE_';

qcmg.r3.primary.dir = ~BS_R3_PRIMARY_DIR~
qcmg.r3.csfasta     = ~BS_R3_CSFASTA~
qcmg.r3.qual        = ~BS_R3_QUAL~
qcmg.r3.read.length = ~BS_R3_READ_LENGTH~
_EO_TEMPLATE_

    return $template;
}


sub _template_common_f5 {

    my $template = <<'_EO_TEMPLATE_';

qcmg.f5.primary.dir = ~BS_F5_PRIMARY_DIR~
qcmg.f5.csfasta     = ~BS_F5_CSFASTA~
qcmg.f5.qual        = ~BS_F5_QUAL~
qcmg.f5.read.length = ~BS_F5_READ_LENGTH~
_EO_TEMPLATE_

    return $template;
}


sub _pc_common_end {
    my $pc = QCMG::Bioscope::Ini::ParameterCollection->new(
                  name => 'common.end',
                  lines_pre => 1 );

    $pc->add_textblock( name  => 'A',
                        value => "\n##  QCMG Standard Parameters  ##\n" );
    $pc->add_object( _pc_qcmg_standard_params() );

    $pc->add_textblock( name  => 'B',
                        value => "\n##  QCMG Standard Input Parameters  ##\n" );
    $pc->add_object( _pc_common_input_params() );

    $pc->add_textblock( name  => 'C',
                        value => "\n##  QCMG Standard Output Parameters  ##\n" );
    $pc->add_object( _pc_common_output_params() );
           
    return $pc->as_text( $pc->name_length );
}


sub _pc_qcmg_standard_params {
    my $pc = QCMG::Bioscope::Ini::ParameterCollection->new(
                  name      => 'qcmg.standard.params',
                  lines_pre => 1 );

    $pc->add_parameter( name  => 'qcmg.seq.raw',
                        value => '${qcmg.panfs.dir}/seq_raw' );
    $pc->add_parameter( name  => 'qcmg.seq.mapped',
                        value => '${qcmg.panfs.dir}/seq_mapped' );
    $pc->add_parameter( name  => 'qcmg.reference.dir',
                        value => '${qcmg.panfs.dir}/share/genomes' );
    $pc->add_parameter( name  => 'qcmg.reference.seq',
                        value => '${qcmg.reference.dir}/GRCh37_ICGC_standard_v2/GRCh37_ICGC_standard_v2.fa' );
    $pc->add_parameter( name  => 'qcmg.reference.cmap',
                        value => '${qcmg.reference.dir}/GRCh37_ICGC_standard_v2_split/GRCh37_ICGC_standard_v2.fa.cmap' );
    $pc->add_parameter( name  => 'qcmg.scratch.dir',
                        value => '/scratch/${qcmg.run.dir}/${qcmg.date}' );
    return $pc;
}


sub _pc_common_input_params {
    my $pc = QCMG::Bioscope::Ini::ParameterCollection->new(
                  name      => 'B.common.input.params',
                  lines_pre => 1 );

    $pc->add_parameter( name    => 'qcmg.base.dir',
                        value   => '${qcmg.seq.raw}/${run.name}',
                        comment => 'Directory that holds unmapped data' );
    $pc->add_parameter( name    => 'reference.dir',
                        value   => '${qcmg.reference.dir}',
                        comment => 'Absolute folder location of reference' );
    $pc->add_parameter( name    => 'reference',
                        value   => '${qcmg.reference.seq}',
                        comment => 'Pathname of reference FASTA file' );
    return $pc;
}

sub _pc_common_output_params {
    my $pc = QCMG::Bioscope::Ini::ParameterCollection->new(
                   name      => 'common.output.params',
                   lines_pre => 1 );
    
    $pc->add_parameter( name    => 'output.dir',
                        value   => '${qcmg.seq.mapped}/${qcmg.mapset}/${qcmg.date}',
                        comment => 'Directory for output' );
    $pc->add_parameter( name    => 'tmp.dir',
                        value   => '${output.dir}/tmp',
                        comment => 'Directory for writing temporary files' );
    $pc->add_parameter( name    => 'intermediate.dir',
                        value   => '${output.dir}/intermediate',
                        comment => 'Directory for writing intermediate results' );
    $pc->add_parameter( name    => 'log.dir',
                        value   => '${output.dir}/log',
                        comment => 'Directory for log files' );
    $pc->add_parameter( name    => 'bioscope.log.dir',
                        value   => '${log.dir}',
                        comment => 'Directory for log files' );
    $pc->add_parameter( name    => 'scratch.dir',
                        value   => '${qcmg.scratch.dir}',
                        comment => 'Scratch directory - local to nodes to save on IO' );
    return $pc;
}




1;
__END__


=head1 NAME

QCMG::Bioscope::Ini::GlobalIniFile - Create global.ini file


=head1 SYNOPSIS

 use QCMG::Bioscope::Ini::GlobalIniFile;
 my $ini = QCMG::Bioscope::Ini::GlobalIniFile->new( %params );
 $ini->write( '/panfs/imb/home/uqjpear1/tmp' );


=head1 DESCRIPTION

This module creates the global.ini file required to initiate a Bioscope
run.  It requires multiple values to be supplied by the user.
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
