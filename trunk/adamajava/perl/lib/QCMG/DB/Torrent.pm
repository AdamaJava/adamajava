package QCMG::DB::Torrent;

##############################################################################
#
#  Module:   QCMG::DB::Torrent.pm
#  Creator:  John V Pearson/Lynn Fink
#  Created:  2011-06-21
#
#  This class is an interface to the Torrent Suite API
#
#  $Id: Torrent.pm 1394 2011-12-01 01:23:29Z l.fink $
#
##############################################################################

=pod

=head1 NAME

QCMG::DB::Torrent -- Common functions for querying the Torrent API 

=head1 SYNOPSIS

 my $gl = QCMG::DB::Torrent->new();
 $gl->connect();

 # To query a different database or as a different user:
 my $gl = QCMG::DB::Torrent->new(
               server   => 'http://10.160.72.27/rundb/api/v1/',
               user     => 'ionadmin',
               password => 'ionadmin' );
 $gl->connect();

=head1 DESCRIPTION

This class contains methods for connecting to and querying the 
Torrent servers using the Torrent Suite API (REST).

To avoid the need to supply a username and password on the
commandline or hard-code them into source, the new() method includes
logic to read a configuration file that contains the username and
password.  The configuration file must be called B<ts_credentials.cfg>
and it must be placed in a directory called B<.torrent> under your home
directory (taken from the B<$QCMG_HOME> environment variable).  The
permissions on the B<$QCMG_HOME/.geneus> directory must be 0700 and the
configuration file can have any permissions set.  The configuration file
is a simple plain-text file that contains variable=value pairs.  For
example:

 # Configuration file for QCMG::DB::Torrent perl module.
 #
 # This configuration allows the connection parameters to be stored
 # in a secure file without being passed on the command line.
 # N.B.  The permissions on the ~/.torrent subdirectory must be 0700.
 #
 # User credentials
 user     = myname
 password = mypassword

=head1 REQUIREMENTS

 Carp
 Data::Dumper
 LWP
 JSON
 HTTP::Request

=cut

use strict;
use warnings;
use lib qw(/usr/local/mediaflux/perl5/lib/perl5/);
use Data::Dumper;
use MIME::Base64;
use HTTP::Request;
use IO::File;
use LWP;
use Carp qw( carp croak );
use POSIX 'strftime';                   # for printing timestamp
#use lib qw(/panfs/home/lfink/projects/torrent/JSON-2.53/lib/);	# babyjesus
use JSON;

use vars qw( $SVNID $REVISION );

( $REVISION ) = '$Revision: 1394 $ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id: Torrent.pm 1394 2011-12-01 01:23:29Z l.fink $'
    =~ /\$Id:\s+(.*)\s+/;

################################################################################

=pod

=head1 METHODS

B<new()> (constructor)

Create a new instance of this class.

Parameters:
 can be run with no parameters; default server, user, password are set

  OR

 server		server URL (iontorrent1)
 user		username
 password	password
 verbose 	level of verbosity

Returns:
 a new instance of this class.

=cut
sub new {
    my $class  = shift;
    my %params = @_;
    
    my $self = { lwp	=> LWP::UserAgent->new };

    # If the user has a ~/.torrent/ts_credentials.cfg file then we should
    # read it for user-settable parameters.

	# Need to query both servers simultaneously

    # Need to have desired server first (if set in %params)
    #my $server	  = $params{'server'};
	#print STDERR "User requested server: $server\n";
    #my $rh_config = _read_ts_credentials($server);   
    my $rh_config = _read_ts_credentials();   

    # Set values from input %params or from defaults.
	# set iontorrent1 as default for now...
    my %defaults = ( server   => 'http://10.160.72.27/rundb/api/v1/',
                     user      => 'apiuser',
                     password  => 'qcmg4567',
                     email     => $ENV{QCMG_EMAIL},
                     verbose   => 0 );

    # This block will set values from parameters, then from config file
    # and lastly from defaults.  Note that only keys from %defaults are
    # even checked so this is how we control what can be specified in
    # the config file - if it's not in %defaults, it can't be set!

    foreach my $key (keys %defaults) {
        if (exists $params{$key} and defined $params{$key} and $params{$key}) {
            $self->{ $key } = $params{ $key };
        }
        elsif (exists $rh_config->{$key} and defined $rh_config->{$key}) {
            $self->{ $key } = $rh_config->{ $key };
        }
        else {
            $self->{ $key } = $defaults{ $key };
        }
    }

    # FYI
    $self->{_params}   = \%params;
    $self->{_config}   = $rh_config;
    $self->{_defaults} = \%defaults;

	#print STDERR "SERVER:	$self->{'server'}\n";
	#print STDERR "USER:	$self->{'user'}\n";
	#print STDERR "PASSW:	$self->{'password'}\n";

    bless $self, $class;
}

################################################################################

=pod

B<server()>

Get/set base URL of Torrent REST API

Parameters:
 scalar: server

Returns:
 scalar: server

=cut
sub server {
    my $self = shift;
    return $self->{server} = shift if @_;
    return ($self->{server});
}




################################################################################

=pod

B<user()>

Get/set username used to log in to Torrent

Parameters:
 scalar: username

Returns:
 scalar: username

=cut
sub user {
    my $self = shift;
    return $self->{user} = shift if @_;
    return $self->{user};
}


################################################################################

=pod

B<password()>

Get/set password used to log in to Torrent

Parameters:
 scalar: password

Returns:
 scalar: password

=cut
sub password {
    my $self = shift;
    return $self->{password} = shift if @_;
    return $self->{password};
}


################################################################################

=pod

B<verbose()>

Get/set verbosity level

Parameters:
 scalar: integer defining level of verbose messages

Returns:
 scalar: integer defining level of verbose messages

=cut
sub verbose {
    my $self = shift;
    return $self->{verbose} = shift if @_;
    return $self->{verbose};
}


################################################################################

=pod

B<connect()>

Connects to Torrent API ready to process queries.  You must call this
routine before running any queries.  You should call it again any time
you change the server, user or password.

Parameters:
 none

Returns:
 none

=cut
sub connect {
    my $self = shift;

    my $lwp = $self->_lwp;
    my $bit64 = encode_base64( $self->user . ':' . $self->password);
    $lwp->default_header('Authorization' => "Basic $bit64");
}


################################################################################

=pod

B<query()>

Basic method for making a query against Torrent

Parameters:
 query: scalar string

Returns:
 data structure with JSON objects

This is the basic abstracted query method that is used by all of the
more specific queries.  Takes a query string which is then directly
appended to the base server string to create the URL that is submitted
to Torrent.  Returns the data structure as parsed from the XML reply.

=cut

sub query {
    my $self  = shift;
    my $query = shift || '';

    my $lwp = $self->_lwp;
    my $url = $self->server . $query;

    my $response = $lwp->get( $url );

    # Some error modes return HTML instead of XML and this borks the
    # JSON parse so we'll have to manually handle these cases.

    my $data = $response->content;
	#print Dumper $response->content;

    if ($data !~ /^<!DOCTYPE/m) {
        #print Dumper $data;
        my $json	= JSON->new();
        #$data		= $json->allow_nonref->utf8->relaxed->escape_slash->loose->allow_singlequote->allow_barekey->decode($data);
   	    $data		= $json->allow_nonref->utf8->relaxed->decode($data);
        #my $json = JSON->new->allow_nonref;
        #my $perl_scalar = $json->decode( $data );
        #$data = $json->pretty->encode( $perl_scalar ); # pretty-printing
    }
    else {
        #print Dumper $data;
        warn "Torrent server could not process URL: $url";
        return undef;
    }

	return $data;
}

################################################################################
#
#  This is generic method to be used by any of the higher level queries
#  where a list of all items of a particular type (e.g. projects or
#  samples) is requested.  These lists often appear as multiple pages so
#  this routine includes logic to keep querying for the "next page" of
#  results until all results are found.  It returns a $result object
#  where {data} points to a list of URIs and the number of queries that
#  were required.

sub _list_items {
    my $self = shift;
    my $base = shift;

    my $qctr = 0;
    my @uris = ();
    my $result;
    my $query = $base . 's/';

    while (1) {
        $result = $self->query( $query );
        $qctr++;
        if ($result->status == 0) {
            #print Dumper $result;
            #die;
		my $data	= $result->data;
            	#print STDERR $data->toString(1);

		my $rootel = $data->getDocumentElement();
		#my $elname = $rootel->getName();

		my @kids = $rootel->childNodes();
		foreach my $child (@kids) {
        		my $elname	= $child->getName();	# sample, project, etc
        		my @atts	= $child->getAttributes();

			my $uri		= $child->getAttribute("uri");

			$qctr++;
			push @uris, $uri;

        		#foreach my $at (@atts) {
                	#	my $na = $at->getName(); # uri, limsid, etc
                	#	my $va = $at->getValue(); # values for uri, etc
                	#}
        	}

            # Keep redefining and repeating the query until there's no 
            # 'next-page' item in which case we 'last' out of the while(1).
	    # NOT NECESSARY NOW - HAVE INCREASED THE MAX RESULTS TO 1E6 (LF/CRL)

		last;
        }
        else {
            croak 'ERROR [',$result->status,'] : ', $result->status_msg;
        }
    }

    # Add 2 _list_items specific fields to $result - query count and url list
    $result->{_query_count} = $qctr;
    $result->{_uri_list}    = \@uris;

    return $result;
}
=cut

################################################################################

=pod

B<slide_chiptype()>

 Get chip type from slide by name (ie, R_2012_01_12_14_51_07_user_T01-105)

Parameters:
 slide: scalar string

Returns:
 scalar: chip type

=cut
sub slide_chiptype {
	my $self	= shift;
	my $slide	= shift;

	my $query	= 'experiment?format=json&expName='.$slide;

	my $data	= $self->query($query);	

	my $ob		= shift @{$data->{'objects'}};

	my $chiptype	= $ob->{'log'}->{'chiptype'};
	# "chiptype" : "318B"

	return $chiptype;
}

################################################################################

=pod

B<slide_runtype()>

 Get run type from slide by name (ie, R_2012_01_12_14_51_07_user_T01-105)

Parameters:
 slide: scalar string

Returns:
 scalar: run type

=cut
sub slide_runtype {
	my $self	= shift;
	my $slide	= shift;

	my $query	= 'experiment?format=json&expName='.$slide;

	#print STDERR "$query\n$slide\n";

	my $data	= $self->query($query);	

	my $ob		= shift @{$data->{'objects'}};

	# "notes" : "Project_ID=icgc_pancreas;Patient_ID=APGI_1992;Primary_Library_ID=Library-20110923_D;RunType=Frag",
	my $notes               = $ob->{'notes'};

	#print STDERR "Notes: $notes\n";

	my $runtype = 'Frag';	# default
	if($notes =~ /RunType/) {
		$notes          =~ /RunType=(\w+)/;
		$runtype	= $1;
	}

	#print STDERR "Run type: $runtype\n";

	return $runtype;
}

################################################################################

=pod

B<slide_projectid()>

 Get project ID from slide by name (ie, R_2012_01_12_14_51_07_user_T01-105)

Parameters:
 slide: scalar string

Returns:
 scalar: project ID 

=cut
sub slide_projectid {
	my $self	= shift;
	my $slide	= shift;

	my $query	= 'experiment?format=json&expName='.$slide;

	#print STDERR "$query\n$slide\n";

	my $data	= $self->query($query);	

	my $ob		= shift @{$data->{'objects'}};

	# "notes" : "Project_ID=icgc_pancreas;Patient_ID=APGI_1992;Primary_Library_ID=Library-20110923_D;RunType=Frag",
	my $notes               = $ob->{'notes'};

	#print STDERR "Notes: $notes\n";

	my $projectid = 'Frag';	# default
	if($notes =~ /Project_ID/) {
		$notes          =~ /Project_ID=(\w+)/;
		$projectid	= $1;
	}

	return $projectid;
}

################################################################################

=pod

B<slide_patientid()>

 Get patient ID from slide by name (ie, R_2012_01_12_14_51_07_user_T01-105)

Parameters:
 slide: scalar string

Returns:
 scalar: patient ID 

=cut
sub slide_patientid {
	my $self	= shift;
	my $slide	= shift;

	my $query	= 'experiment?format=json&expName='.$slide;

	#print STDERR "$query\n$slide\n";

	my $data	= $self->query($query);	

	my $ob		= shift @{$data->{'objects'}};

	# "notes" : "Project_ID=icgc_pancreas;Patient_ID=APGI_1992;Primary_Library_ID=Library-20110923_D;RunType=Frag",
	my $notes               = $ob->{'notes'};

	#print STDERR "Notes: $notes\n";

	my $patientid = 'Frag';	# default
	if($notes =~ /Project_ID/) {
		$notes          =~ /Patient_ID=(\w+)/;
		$patientid	= $1;
	}

	return $patientid;
}

################################################################################

=pod

B<slide2reformatted_name()>

 Translate slide name (R_2012_01_12_14_51_07_user_T01-105) to reformatted name
  (T01_20120112_105_Frag)

Parameters:
 slide: scalar string

Returns:
 scalar: slide name

=cut
sub slide2reformatted_name {
	my $self	= shift;
	my $slide	= shift;

	my $runtype	= $self->get_run_type($slide);

	$slide		=~ /R\_(\d{4})\_(\d{2})\_(\d{2})\_.+?\_(T\d+)\-(\d+)/;
	my $date	= join "", $1, $2, $3;
	my $serial	= $4;	# machine "serial number"
	my $flowcell	= $5;	# not really flow cell, but will go in that pos

	# add run type
	my $name	= $serial."_".$date."_".$flowcell."_".$runtype;

	return $name;
}

################################################################################

=pod

B<reformatted_name2slide()>

 Translate reformatted name to slide name
  (T01_20120112_105_Frag) -> (R_2012_01_12_14_51_07_user_T01-105)

Parameters:
 slide: scalar string

Returns:
 scalar: slide name

=cut
sub reformatted_name2slide {
	my $self	= shift;
	my $slide	= shift;

	# T01_20120112_105_Frag
	# ---          ---
	$slide		=~ /(T\d+)\_\d+\_(\d+)\_/;

	my $num		= $1."-".$2;

	#print STDERR "$num\n";

	# search for an experiment matching this number
	my $query	= "experiment?format=json&expName__contains=$num";

	my $data	= $self->query($query);	
	#print STDERR "$query\n";

	#print Dumper $data;

	my $ob		= shift @{$data->{'objects'}};

	my $name	= $ob->{'expName'};

	return $name;
}

################################################################################

=pod

B<put()>

Basic method for writing to Torrent

Parameters:

Returns:
 0 if successful, 1 if failed

This is the method that will attempt to write data back to Torrent.

=cut

sub put {
    my $self	= shift;
    my $query	= shift;

	if(! $query) {
		print STDERR "Torrent query is empty\n";
		exit(1);
	}

    my $lwp = $self->_lwp;
    my $url = $self->server . $query;

	my $rv = 1;	# default to failed update

	# PUT modified xml

	# $json_text   = $json->encode( $perl_scalar );

	my $request = new HTTP::Request 'PUT', $url;
	$request->header('content-type' => 'application/xml');
	my $response = $lwp->request( $request );
	if ( not $response->is_success ) {
     		print STDERR $response->error_as_HTML();
		exit($rv);
	}
	else {
		$rv = 0;
	}

	return($rv);
}

sub _lwp {
    my $self = shift;
    return $self->{lwp};
}

### Non-OO Functions ######################################################


# Parse the ~/.torrent/ts_credentials.cfg file if it exists.  This
# routine will fail gracefully if the file is missing but will throw
# fatal errors if $QCMG_HOME environment variable is not set or the
# ~/.torrent directory exists but is not chmod'd to 700.
#
# If the user has a ~/.torrent/ts_credentials.cfg file then we should
# read it for user-settable parameters. The first line that matches the
# requested server should be used:
#   iontorrent1:::ionadmin:::<testpwd>
#   iontorrent2:::ionadmin:::<prodpwd>

sub _read_ts_credentials {
    my $server	= shift @_;	

    die 'FATAL: Set environment variable $QCMG_HOME to use Perl Torrent API'
        unless defined $ENV{QCMG_HOME};

    my %config = ();

    # Exit unless we found ~/.torrent directory
    my $tsdir = $ENV{QCMG_HOME};
    $tsdir .= '/' unless ($tsdir =~ /\/$/);
    $tsdir .= '.torrent';
    return \%config unless -d $tsdir;

    # Check for 0700 permissions on ~/.torrent
    my $mode = (stat($tsdir))[2];
    my $tsdir_perm = sprintf("%04o", $mode & 07777);
    die "FATAL: ~/.geneus permissions must be 0700, not $tsdir_perm\n"
        unless $tsdir_perm eq '0700';

    # Exit unless we found ~/.torrent/ts_credentials.cfg
    my $tscred = $tsdir . '/ts_credentials.cfg';
    return \%config unless -f $tscred;

    # Read config values out of ~/.torrent/ts_credentials.cfg
    my $fh = IO::File->new($tscred, 'r');
    while (my $line = $fh->getline) {
	#print STDERR "CRED LINE: $line\n";
        chomp $line;
        $line =~ s/^\s+//g;     # drop leading spaces
        $line =~ s/\s+$//g;     # drop trailing spaces
        next if $line =~ /^#/;  # skip comments
        next unless $line =~ /^.+=.+$/;  # skip lines not in A=B pattern

        my ($key, $value) = split /\s*=\s*/, $line;
        $config{ $key } = $value;

	#$config{'server'}	= 'http://'.$server.'/rundb/api/v1/';
	$config{'server'}	= 'http://10.160.72.27/rundb/api/v1/';
	#$config{'user'}		= $user;
	#$config{'password'}	= $password;
    }

    return \%config;
}


# This routine can take either a limsid or a URI and extract and the limsid

sub getid {
    my $input = shift;
    #print "getid - in: $input";
    if ($input =~ /^(http:\/\/.*\/)([^\/]+)$/) {
        $input = $2;
    }
    #print ", out: $input\n";
    return $input;
} 

################################################################################
=pod

B<timestamp()> 
 Generate a timestamp in the format: 2003-02-14-16:37:46
                                     030214
                                     030214163746

Parameters:
 FORMAT 
   - ISO8601         - Default timestamp, ISO 8601 format
   - YYYYMMDD        - timestamp in YYYYMMDD format 
   - YYYYMMDDhhmmss  - timestamp in YYYYMMDDhhmmss format 
   - YYYY-MM-DD 

Returns:
 scalar - timestamp string

=cut
sub timestamp {
        my $self = shift @_;

        my $options = {};

        # read params from @_ and put in %options
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                $options->{$_[$i]} = $_[($i + 1)];
        }

        # default = ISO 8601 format: [2011-02-19 23:59:99Z]
        my $stamp = lc strftime("[%Y-%m-%d %H:%M:%S]", localtime); 

        # return date in YYMMDD format
        if($options->{'FORMAT'} eq 'YYYYMMDD') {
                $stamp = uc strftime("%Y%m%d", localtime);
        }
        elsif($options->{'FORMAT'} =~ /yyyymmddhh/i) {
                $stamp = uc strftime("%Y%m%d%H%M%S", localtime);
        }
        elsif($options->{'FORMAT'} =~ /yyyy-mm-dd/i) {
                $stamp = uc strftime("%Y-%m-%d", localtime);
        }

        return($stamp);
}


1;

__END__

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
