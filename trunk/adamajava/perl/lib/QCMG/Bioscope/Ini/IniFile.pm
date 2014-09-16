package QCMG::Bioscope::Ini::IniFile;

###########################################################################
#
#  Module:   QCMG::Bioscope::Ini::IniFile.pm
#  Creator:  John V Pearson
#  Created:  2010-08-23
#
#  This is the superclass that is extended by all of the modules that
#  create ini files - RunPbsFile.pm etc.  The advantage of using this
#  superclass is that is contains the logic to handle *ALL* of the 
#  user-settable values that are used in all of the different run types.
#  This allows all run-types to share critical logic for options that many
#  file types share, for example execution_server and run_date.
#
###########################################################################

use strict;
use warnings;

use IO::File;
use IO::Zlib;
use Data::Dumper;
use Carp qw( confess );
use vars qw( $VERSION $TODAY );

#use QCMG::Bioscope::Ini::Parameter;
#use QCMG::Bioscope::Ini::ParameterCollection;
#use QCMG::Bioscope::Ini::TextBlock;

our $AUTOLOAD;  # it's a package global

#  The $VALID_AUTOLOAD_METHODS hashref is very important to the
#  operation of this class as it contains the names of all the methods
#  that we will try to handle via AUTOLOAD.  We are using AUTOLOAD
#  because with a little planning, it lets us avoid defining and
#  maintaining a lot of basically identical accessor methods.

our $VALID_AUTOLOAD_METHODS = {
        execution_server      => '~BS_EXEC_SRVR~',
        bioscope_install_dir  => '~BS_INSTALL_DIR~',
        panasas_mount         => '~BS_PANASAS~',
        pbs_queue             => '~BS_PBS_QUEUE~',
        pbs_exe_queue         => '~BS_PBS_EXE_QUEUE~',
        ini_root_dir          => '~BS_INI_ROOT_DIR~',
        run_name              => '~BS_RUN_NAME~',
        run_date              => '~BS_RUN_DATE~',
        email                 => '~BS_EMAIL~',
        barcode               => '~BS_BARCODE~',
        physical_division     => '~BS_PHYS_DIV~',
        f3_csfasta            => '~BS_F3_CSFASTA~',
        f3_qual               => '~BS_F3_QUAL~',
        f3_read_length        => '~BS_F3_READ_LENGTH~',
        f3_primary_dir        => '~BS_F3_PRIMARY_DIR~',
        r3_csfasta            => '~BS_R3_CSFASTA~',
        r3_qual               => '~BS_R3_QUAL~',
        r3_read_length        => '~BS_R3_READ_LENGTH~',
        r3_primary_dir        => '~BS_R3_PRIMARY_DIR~',
        f5_csfasta            => '~BS_F5_CSFASTA~',
        f5_qual               => '~BS_F5_QUAL~',
        f5_read_length        => '~BS_F5_READ_LENGTH~',
        f5_primary_dir        => '~BS_F5_PRIMARY_DIR~',
        primary_lib_id        => '~BS_PRIMARY_LIB_ID~',
    };


BEGIN {
    # Construct a class static variable for use with ~BS_TODAY~
    my @fields = localtime();
    my $year  = 1900 + $fields[5];
    my $month = substr( '00' . ($fields[4]+1), -2, 2);
    my $day   = substr( '00' . $fields[3], -2, 2);
    $TODAY = join('-',$year,$month,$day);
}


###########################################################################
#                             PUBLIC METHODS                              #
###########################################################################


sub new {
    my $class = shift;
    my %params = @_;

    # We are intentionally not setting any values in the constructor
    # because we want to force each subclass to call the setters from
    # within their new() method since the setters also do the
    # template substitutions.

    my $self = { execution_server              => '',
                 bioscope_install_dir          => '',
                 panasas_mount                 => '',
                 pbs_queue                     => '',
                 pbs_exe_queue                 => '',
                 ini_root_dir                  => '',
                 run_name                      => '',
                 run_date                      => '',
                 email                         => '',
                 barcode                       => '',
                 physical_division             => '',
                 f3_csfasta                    => '',
                 f3_qual                       => '',
                 f3_read_length                => '',
                 f3_primary_dir                => '',
                 r3_csfasta                    => '',
                 r3_qual                       => '',
                 r3_read_length                => '',
                 r3_primary_dir                => '',
                 f5_csfasta                    => '',
                 f5_qual                       => '',
                 f5_read_length                => '',
                 f5_primary_dir                => '',
                 primary_lib_id                => '',
                 _today                        => $TODAY,
                 _contents                     => '',
                 _filename                     => '',
                 _run_type                     => '',
                 _version                      => '',
               };

    bless $self, $class;

    # Setup template text with common header
    my $template = $self->_header() . $params{template};
    $template =~ s/~BS_I_FILENAME~/$params{filename}/g;
    $template =~ s/~BS_I_VERSION~/$params{version}/g;
    $template =~ s/~BS_I_TODAY~/$TODAY/g;

	# add shell command to top of run.pbs to avoid shell-dependent issues
	if($params{filename} eq 'run.pbs') {
		$template = qq{#!/bin/bash\n}.$template;
	}

    # Set internal values
    $self->contents( $template );
    $self->filename( $params{filename} );
    $self->run_type( $params{run_type} );
    $self->version( $params{version} );

    return $self;
}


sub AUTOLOAD {
    my $self  = shift;
    my $value = shift || undef;

    my $type       = ref($self) or confess "$self is not an object";
    my $invocation = $AUTOLOAD;
    my $method     = undef;

    if ($invocation =~ m/^.*::([^:]*)$/) {
        $method = $1;
    }
    else {
        die "QCMG::Bioscope::Ini::IniFile AUTOLOAD problem with [$invocation]";
    }

    # We don't need to report on missing DESTROY methods.
    return undef if ($method eq 'DESTROY');

    unless (exists $VALID_AUTOLOAD_METHODS->{$method}) {
        die "QCMG::Bioscope::Ini::IniFile can't access method [$method]";
    }

    # If this is a setter call then do the set and the substitution
    if (defined $value) {
        $self->{$method} = $value;
        my $target = $VALID_AUTOLOAD_METHODS->{$method};
        $self->{_contents} =~ s/$target/$value/g;
        return $value;
    }
    else {
        # Return current value if no new value is supplied;
        return $self->{$method};
    }

}  


sub _header {
    my $self = shift;

    my $header = << '_EO_HEADER_';
##########################################################################
#
#  File:          ~BS_I_FILENAME~
#  Generated on:  ~BS_I_TODAY~
#  Generated by:  v~BS_I_VERSION~
#
#  This file was auto-generated and should not be hand edited without a
#  good reason.  There certainly are occasions where editing is acceptable
#  but the edited file MUST go into subversion.  If you are not sure about 
#  whether you should be editing the ini files or regenerating from
#  scratch, please see John Pearson.
#
##########################################################################
_EO_HEADER_

    return $header;
}


sub contents {
    my $self = shift;
    return $self->{_contents} = shift if @_;
    return $self->{_contents};
}


sub filename {
    my $self = shift;
    return $self->{_filename} = shift if @_;
    return $self->{_filename};
}


sub run_type {
    my $self = shift;
    return $self->{_run_type} = shift if @_;
    return $self->{_run_type};
}

sub verbose {
    my $self = shift;
    return $self->{_verbose} = shift if @_;
    return $self->{_verbose};
}


sub version {
    my $self = shift;
    return $self->{_version} = shift if @_;
    return $self->{_version};
}


###########################################################################
#                         NON-AUTOLOAD ACCESSORS                          #
###########################################################################


#  There will be some accessors that need additional logic beyond the
#  generic functionality provided by AUTOLOAD so they simply get their
#  own specific accessors defined here.  Note that these accessors are
#  free to call AUTOLOAD accessors as needed.


sub execution_server {
    my $self   = shift;
    my $server = shift;

    # Return current value if no new value is supplied;
    return $self->{execution_server} unless defined $server;

    my @valid_servers = ( 'barrine', 'qcmg-clustermk2' );

    # Check that the supplied value is valid.
    my $valid = 0;
    foreach my $valid_server (@valid_servers) {
       if ($valid_server =~ /^$server$/i) {
           $valid = 1;
           last;
       }
    }

    # If we didn't find a valid server then it's time to die
    die "The only valid values for execution server are: ",
        join(', ',@valid_servers), "\n" unless $valid;

    # If the server was valid, set variables.  Note logic to avoid deep
    # recursion when setting execution_server.  Also note that we will
    # not reset any params that have already been set by the user.
    if ($server =~ /barrine/i) {
        $self->{execution_server} = 'barrine';
        $self->panasas_mount( '/panfs/' ) unless $self->panasas_mount;
        $self->pbs_queue( 'batch' ) unless $self->pbs_queue;
        $self->pbs_exe_queue( 'batch' ) unless $self->pbs_exe_queue;
        $self->bioscope_install_dir( '/sw/bioscope/1.2.1/bioscope' )
             unless $self->bioscope_install_dir;
    }
    elsif ($server =~ /qcmg-clustermk2/i) {
        $self->{execution_server} = 'qcmg-clustermk2';
        $self->panasas_mount( '/panfs' ) unless $self->panasas_mount;
        $self->pbs_queue( 'batch' ) unless $self->pbs_queue;
        $self->pbs_exe_queue( 'batch' ) unless $self->pbs_exe_queue;
        $self->bioscope_install_dir( '/share/software/Bioscope/bioscope' )
             unless $self->bioscope_install_dir;
    }

    # Do substitution
    $self->{_contents} =~ s/~BS_EXEC_SRVR~/$server/g;

    return $server;
}


sub write {
    my $self = shift;
    my $directory = shift;

    my $filename = join( '/', $directory, $self->filename );

    # For safety's sake, we do not do any overwriting of files
    #die "File [$filename] already exists and will not be overwritten"
    #    if (-f $filename);

    # If there are any un-substituted ~BS_*~ variables left then there
    # is a logic error somewhere than needs to be fixed
    my $contents = $self->contents;
    if ($contents =~ /(~BS_[\w\d-]+~)/) {
        die "\nFile $filename cannot be written because there is an ",
            "un-substituted Bioscope variable [$1] in the file contents:\n\n",
            $contents;

    }

    # Now we are ready to write out the file
    my $fh = IO::File->new( $filename, 'w' );
    die 'Unable to open file [', $filename, "] for writing: $!"
        unless defined $fh;
    $fh->print( $contents );
    $fh->close;
}



1;
__END__


=head1 NAME

QCMG::Bioscope::Ini::IniFile - Superclass for Bioscope Ini Files


=head1 SYNOPSIS

 use QCMG::Bioscope::Ini::IniFile;
 @ISA = qw( QCMG::Bioscope::Ini::IniFile );

 sub new {
     my $class = shift;
     my %params = @_;
     my $self = $class->SUPER::new( %params );
     return $self;
 }


=head1 DESCRIPTION

This module is the superclass for all of the classes that generate
Bioscope ini files, for example RunPbsFile.  It should B<never> be used
directly by a programmer unless they are subclassing it to create a
module for a new ini files.
It handles all user-settable values that are required by any of the ini
files.  This ensures that the same logic is used by all ini files that
share a common variable.
All the user-supplied values can be specified in the call to B<new()> or
they can be supplied by calling the accessor methods.
The following table shows that strings that should be used in subclasses
as placeholders for values to be substituted in.  It also shows that
accessor method used to set the value that will be substituted in.

 ~BS_TODAY~              not user settable!
 ~BS_EXEC_SRVR~          execution_server()
 ~BS_PBS_QUEUE~          pbs_queue() (also see execution_server())
 ~BS_PBS_EXE_QUEUE~      pbs_exe_queue() (also see execution_server())
 ~BS_PANASAS~            panasas_mount() (also see execution_server())
 ~BS_INSTALL_DIR~        bioscope_install_dir() (also see execution_server())
 ~BS_INI_ROOT_DIR~       ini_root_dir()
 ~BS_RUN_NAME~           run_name()
 ~BS_RUN_DATE~           run_date()
 ~BS_EMAIL~              email()
 ~BS_F3_PRIMARY_DIR~     f3_primary_dir()
 ~BS_F3_CSFASTA~         f3_csfasta()
 ~BS_F3_QUAL~            f3_qual()
 ~BS_F3_READ_LENGTH~     f3_read_length()
 ~BS_R3_PRIMARY_DIR~     r3_primary_dir()
 ~BS_R3_CSFASTA~         r3_csfasta()
 ~BS_R3_QUAL~            r3_qual()
 ~BS_R3_READ_LENGTH~     r3_read_length()
 ~BS_F5_PRIMARY_DIR~     f5_primary_dir()
 ~BS_F5_CSFASTA~         f5_csfasta()
 ~BS_F5_QUAL~            f5_qual()
 ~BS_F5_READ_LENGTH~     f5_read_length()
 ~BS_PRIMARY_LIB_ID~     primary_lib_id()


=head1 PUBLIC METHODS

=over

=item B<new()>

 my $ini = QCMG::Bioscope::Ini::IniFile->new(
                execution_server => 'barrine',
                ini_root_dir     => '/panfs/imb/home/uqjpear1/bioscope/',
                run_name         => 'S0436_20100517_2_Frag',
                run_date         => '20100804',
                );


=item B<execution_server()>
 
 $ini->execution_server( 'barrine' );

Sets the server on which this run is to be executed.  This is important
because the mount points for some filesystems and the locations of the
bioscope executable can vary from server to server.  The only currently
valid values for this method are 'barrine' and 'qcmg-custermk1'.

=item B<pbs_queue()>
 
 $ini->pbs_queue( 'batch' );

This is the queue that the run.pbs job goes to.  Note that this is
different from the queue where the bioscope jobs themselves are sent
which can be set using B<pbs_exe_queue()>.  This queue distinction is
useful on qcmg-clustermk2 where the bioscope queues have 4 x 8-core
nodes so if the run.pbs job goes onto that queue, it will block on of
the 4 mapping jobs because mapping jobs require all 8-cores, i.e. they
cannot share a node.  In this case, you send the bioscope jobs to queue
bioscope or bioscope2 (pbs_exe_queue) and you send the run.pbs job
(pbs_queue) to batch.

=item B<verbose()>

 $ma->verbose();

Returns the verbose status for this object where 0 sets verbose off 
and any other value (traditionally 1) sets verbose mode on.

=back


=head1 AUTHORS

=over

=item John Pearson L<mailto:j.pearson@uq.edu.au>

=back


=head1 VERSION

$Id: IniFile.pm 4660 2014-07-23 12:18:43Z j.pearson $


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
