package QCMG::Bioscope::Ini::RunPbsFile;

###########################################################################
#
#  Module:   QCMG::Bioscope::Ini::RunPbsFile.pm
#  Creator:  John V Pearson
#  Created:  2010-08-23
#
#  Create run.pbs file.
#
#  $Id: RunPbsFile.pm 4660 2014-07-23 12:18:43Z j.pearson $
#
###########################################################################

use strict;
use warnings;

use IO::File;
use IO::Zlib;
use Data::Dumper;
use vars qw( $VERSION @ISA );

use QCMG::Bioscope::Ini::IniFile;

@ISA = qw( QCMG::Bioscope::Ini::IniFile );

( $VERSION ) = '$Revision: 4660 $ ' =~ /\$Revision:\s+([^\s]+)/;


###########################################################################
#                          PUBLIC METHODS                                 #
###########################################################################


sub new {
    my $class  = shift;
    my %params = @_;
    
    my $self = $class->SUPER::new( %params,
                                   filename => 'run.pbs',
                                   template => _set_template(),
                                   version  => $VERSION );

    # These accessors must be called before execution_server as it
    # will provide defaults for these params if not supplied by user
    $self->bioscope_install_dir( $params{bioscope_install_dir} );
    $self->panasas_mount( $params{panasas_mount} );
    $self->pbs_queue( $params{pbs_queue} );
    $self->pbs_exe_queue( $params{pbs_exe_queue} );

    $self->execution_server( $params{execution_server} );
    $self->ini_root_dir( $params{ini_root_dir} );
    $self->run_name( $params{run_name} );
    $self->run_date( $params{run_date} );
    $self->barcode( $params{barcode} );
    $self->physical_division( $params{physical_division} );
    $self->email( $params{email} );

    return $self;
}


sub _set_template {

    my $template = <<'_EO_TEMPLATE_';

#PBS -N bs_run_pbs
#PBS -q batch
#PBS -r n
#PBS -j oe
#PBS -l ncpus=1,mem=4gb,walltime=168:00:00
#PBS -m ae
#PBS -M QCMG-InfoTeam@imb.uq.edu.au

RUN_NAME=~BS_RUN_NAME~.~BS_PHYS_DIV~.~BS_BARCODE~
RUN_DATE=~BS_RUN_DATE~

export BIOSCOPEROOT=/share/software/Bioscope/bioscope
. $BIOSCOPEROOT/etc/profile.d/bioscope_profile.sh

check_mf_result ()
{
        case $1 in
        0)
                ;;
        1)
                echo "`date` - LiveArc responded that an error has occured"
                date
                ;;
        2)
                echo "`date` - Authentication issue with LiveArc"
                date
                ;;
        esac
}

# This assumes the following:
#   the ~/.mediaflux/mf_credentials.cfg file exists and works
#   the /QCMG/${SLIDE_NAME} namespace exists
#   the /QCMG/${SLIDE_NAME}/${SLIDE_NAME}_mapped namespace exists
#   the /QCMG/${SLIDE_NAME}/${SLIDE_NAME}_mapped/${RUN_NAME}_seq_mapped asset exists
#   the usual QCMG_* environment variables are set

# To ensure the run.pbs can be used regardless of LiveArc status,
# The exit status of this script will be the exit status of the bioscope
# job, itself. 

#set environment
export JOBID=`echo $PBS_JOBID |awk -F . '{print $1}'`
export CRED_FILE=$HOME/.mediaflux/mf_credentials.cfg
export PARENT_NS=/QCMG_solid
export SLIDE_NAME=`echo $RUN_NAME |awk -F . '{print $1}'`
export ATERM=/panfs/share/software/mediaflux/aterm.jar
export ASSET_PATH=${PARENT_NS}/${SLIDE_NAME}/${SLIDE_NAME}_mapped
export LIVEARC_RUN="java -Dmf.cfg=$CRED_FILE -jar $ATERM --app exec"

#Check to ensure the seq_mapped asset exists

#source /usr/share/modules/init/bash
#module load java/1.6.0_22-sun

echo "`date` - checking LiveArc for ${ASSET_PATH}/${RUN_NAME}_seq_mapped"
CHECK_ASSET=`$LIVEARC_RUN asset.exists :id path=${ASSET_PATH}/${RUN_NAME}_seq_mapped`
check_mf_result $?
echo $CHECK_ASSET |grep \"true\"\$
RESULT=$?
if [ $RESULT = 0 ] ; then
        ASSET_EXISTS="true"
	echo "`date` - ${ASSET_PATH}/${RUN_NAME}_seq_mapped exists and will be updated"
else
        ASSET_EXISTS="false"
	echo "`date` - ${ASSET_PATH}/${RUN_NAME}_seq_mapped does not exist and will not will be updated"
fi
echo $ASSET_EXISTS
# Update seq_mapped asset metadata
if [ $ASSET_EXISTS = "true" ] ; then
        SET_META=`$LIVEARC_RUN asset.set :id path=${ASSET_PATH}/${RUN_NAME}_seq_mapped :meta -action merge \< :mappinginfo \< :job_id "$JOBID" :map_start "now" :map_end "now" :status RUNNING :notes " " \> \>`
        check_mf_result $?
fi
#run bioscope
echo "`date` - running bioscope"
cd /panfs/seq_raw/$SLIDE_NAME/_bioscope_ini/${RUN_NAME}/$RUN_DATE
bioscope.sh -l bioscope.log bioscope.plan
RUNRESULT=$?
chmod -R g+r /panfs/seq_raw/$SLIDE_NAME/_bioscope_ini
#
##update LiveArc with new metadata
if [ $ASSET_EXISTS = "true" ] ; then
        export PERL5LIB=/share/software/QCMGPerl/lib:$PERL5LIB
        export PERLHOME=/share/software/QCMGPerl/
	# for Class::Accessor
	module load perl
	module load perl/5.15.8
	echo "`date` - updating ${ASSET_PATH}/${RUN_NAME}_seq_mapped asset"
        if [ $RUNRESULT = 0 ] ;then
                # update metadata with a smile
                SET_META=`$LIVEARC_RUN asset.set :id path=${ASSET_PATH}/${RUN_NAME}_seq_mapped :meta -action merge \< :mappinginfo \< :map_end "now" :status SUCCEEDED \> \>`
                check_mf_result $?
		echo "`date` - running $PERLHOME/distros/automation/ingest_mapped.pl to ingest results"
                if [ $PARENT_NS = "/test" ] ; then
                        $PERLHOME/distros/automation/ingest_mapped.pl -i ${QCMG_PANFS}/seq_mapped/${RUN_NAME} -log /panfs/automation_log/${RUN_NAME}.ingest_mapped.log -loglevel DEBUG
                else
                        $PERLHOME/distros/automation/ingest_mapped.pl -i ${QCMG_PANFS}/seq_mapped/${RUN_NAME} -log /panfs/automation_log/${RUN_NAME}.ingest_mapped.log
                fi
        else
                # update metadata with a frown
                SET_META=`$LIVEARC_RUN asset.set :id path=${ASSET_PATH}/${RUN_NAME}_seq_mapped :meta -action merge \< :mappinginfo \< :map_end "now" :status FAILED \> \>`
                check_mf_result $?
        fi
fi
echo "`date` - run.pbs is complete."
exit $RUNRESULT

_EO_TEMPLATE_

    return $template;
}


1;
__END__


=head1 NAME

QCMG::Bioscope::Ini::RunPbsFile - Create run.pbs file


=head1 SYNOPSIS

 use QCMG::Bioscope::Ini::RunPbsFile;
 my $ini = QCMG::Bioscope::Ini::RunPbsFile->new( %params );
 $ini->write( '/panfs/imb/home/uqjpear1/tmp' );


=head1 DESCRIPTION

This module creates the run.pbs.plan file required to initiate a Bioscope
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

$Id: RunPbsFile.pm 4660 2014-07-23 12:18:43Z j.pearson $


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
