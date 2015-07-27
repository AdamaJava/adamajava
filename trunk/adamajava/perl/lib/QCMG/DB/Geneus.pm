package QCMG::DB::Geneus;

##############################################################################
#
#  Module:   QCMG::DB::Geneus.pm
#  Creator:  John V Pearson
#  Created:  2011-06-21
#
#  This class is a portal to information held in the Genologic Geneus
#  LIMS database.
#
#  $Id$
#
##############################################################################

=pod

=head1 NAME

QCMG::DB::Geneus -- Common functions for querying the Geneus LIMS

=head1 SYNOPSIS

 my $gl = QCMG::DB::Geneus->new();
 $gl->connect();

 # To query a different database or as a different user:
 my $gl = QCMG::DB::Geneus->new(
               server   => 'http://qcmg-gltest:8080/api/v1/',
               user     => 'anotherusername',
               password => 'newpasswd' );
 $gl->connect();

=head1 DESCRIPTION

This class contains methods for connecting to and querying the 
QCMG Genologics Geneus LIMS using the REST API.

To avoid the need to supply a username and password on the
commandline or hard-code them into source, the new() method includes
logic to read a configuration file that contains the username and
password.  The configuration file must be called B<gf_credentials.cfg>
and it must be placed in a directory called B<.geneus> under your home
directory (taken from the B<$QCMG_HOME> environment variable).  The
permissions on the B<$QCMG_HOME/.geneus> directory must be 0700 and the
configuration file can have any permissions set.  The configuration file
is a simple plain-text file that contains variable=value pairs.  For
example:

 # Configuration file for QCMG::DB::Geneus perl module.
 #
 # This configuration allows the connection parameters to be stored
 # in a secure file without being passed on the command line.
 # N.B.  The permissions on the ~/.geneus subdirectory must be 0700.
 #
 # User credentials
 user     = myname
 password = mypassword

All of the querys return an object of type QCMG::DB::GeneusResult
however there is no guarantee that the result is from a single query
against Geneus.  For example, Geneus returns results in batches of 500
so for any query with more than 500 expected return values (e.g.
B<samples()> then the query will have internal logic to continue
querying the LIMS until all results were found.  In these cases, the
status etc from GeneusResult will relate to the most recent query.  A
subtle but important point that may be particularly important when
debugging failed queries.

=head1 REQUIREMENTS

 Carp
 Data::Dumper
 LWP
 MIME::Base64
 XML::LibXML
 HTTP::Request
 QCMG::DB::GeneusResult

=cut

# This BEGIN block must occur before the "use" block.  The SOLiD
# sequencers don't have all the module we depend on for this module and
# rather than pollute/break the local perl install, we have the extra 
# modules installed in a QCMG-only area which we need to have added to
# @INC. We also need to manipulate the libs for qcmg-clustermk2.

BEGIN {
    if ($ENV{HOSTNAME} =~ /solid\d+\.sequencer/i) {
        use lib qw(/usr/local/mediaflux/perl5/site_perl/5.8.8/);
    }
    elsif ($ENV{HOSTNAME} =~ /qcmg-cluster/i) {
        use lib qw(/panfs/home/lfink/devel/QCMGPerl/lib/);
        use lib qw(/panfs/home/lfink/lib/lib/perl5/x86_64-linux-thread-multi/);
    }
    else {
	# put info/settings here for torrent
       #warn "Warning: this module has only beenon sequencers and qcmg-clustermk2 (hostname = $ENV{HOSTNAME})";
    }
}

use strict;
#use warnings;
use lib qw(/usr/local/mediaflux/perl5/lib/perl5/);
use XML::LibXML;	# must be 1.88+
use Data::Dumper;
use MIME::Base64;
use HTTP::Request;
use IO::File;
use LWP;
use Carp qw( carp croak );
use POSIX 'strftime';                   # for printing timestamp

use QCMG::DB::GeneusResult;

use vars qw( $SVNID $REVISION );

( $REVISION ) = '$Revision$ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id$'
    =~ /\$Id:\s+(.*)\s+/;

################################################################################

=pod

=head1 METHODS

B<new()> (constructor)

Create a new instance of this class.

Parameters:
 can be run with no parameters; default server, user, password are set

  OR

 server		server URL
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

    $self->{'libxml'}	= XML::LibXML->new();

    # If the user has a ~/.geneus/gl_credentials.cfg file then we should
    # read it for user-settable parameters. The first line that matches the
    # requested server should be used:
    #   qcmg-gltest:::apiuser:::<testpwd>
    #   qcmg-glprod:::apiuser:::<prodpwd>
    # Need to have desired server first (if set in %params)
    my $server	  = $params{'server'};
	#print STDERR "User requested server: $server\n";
    my $rh_config = _read_gl_credentials($server);   

    # Set values from input %params or from defaults.
    my %defaults = ( server    => 'http://qcmg-gltest.imb.uq.edu.au:8080/api/v1/',
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


#my $url = 'http://qcmg-gltest:8080/api/v1/processes/?Slide=S0014_20110510_1_Frag';
#my $url = 'http://qcmg-gltest:8080/api/v1/processes/?type=SOLiD Sequencing Run&Slide=S0014_20110510_1_Frag';
#my $url = 'http://qcmg-gltest:8080/api/v1/processes/?type=SOLiD Sequencing Run&udf.Slide=S0014_20110510_1_Frag';
#my $url = 'http://qcmg-gltest:8080/api/v1/processes/?type=SOLiD Sequencing Run&udf.udfname[field]=S0014_20110510_1_Frag';


################################################################################

=pod

B<server()>

Get/set base URL of Geneus REST API

Parameters:
 scalar: server

Returns:
 scalar: server

=cut

sub server {
    my $self = shift;
    return $self->{server} = shift if @_;
    return $self->{server};
}


################################################################################

=pod

B<user()>

Get/set username used to log in to Geneus

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

Get/set password used to log in to Geneus

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

Connects to Geneus API ready to process queries.  You must call this
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

Basic method for making a query against Geneus

Parameters:
 query: scalar string

Returns:
 QCMG::DB::GeneusResult object

This is the basic abstracted query method that is used by all of the
more specific queries.  Takes a query string which is then directly
appended to the base server string to create the URL that is submitted
to Geneus.  Returns the data structure as parsed from the XML reply.

=cut

sub query {
    my $self  = shift;
    my $query = shift || '';

    my $libxml = $self->_libxml;
    my $lwp = $self->_lwp;
    my $url = $self->server . $query;

	print STDERR "URL: $url\n" if($self->{'verbose'});

    my $response = $lwp->get( $url );
	#print Dumper $response;

    # Some error modes return HTML instead of XML and this borks the
    # XML::Simple parse so we'll have to manually handle these cases.

    my $data = undef;
	#print Dumper $response->content;
    if ($response->content !~ /^<html>/) {
       $data = $libxml->load_xml( string => $response->content );
    }

    my $result = QCMG::DB::GeneusResult->new( 
                     url     => $url,
                     reply   => $response->content,
                     msg     => $response->{'_msg'},
                     data    => $data,
                     verbose => $self->verbose );

    return $result;
}

################################################################################

=pod

B<put()>

Basic method for writing to Geneus

Parameters:
 XML::LibXML object with XML to send to Geneus 

Returns:
 0 if successful, 1 if failed

This is the method that will attempt to write XML back to Geneus. It will
validate against the XML schema to prevent obvious accidents from happening. It
returns a simple bool indicated whether the put succeeded or failed.

=cut

sub put {
    my $self	= shift;
    my $xml	= shift;
    my $query	= shift;

	if(! $query) {
		print STDERR "Geneus query is empty; please make sure the Geneus entity exists\n";
		exit(1);
	}

    my $libxml = $self->_libxml;
    my $lwp = $self->_lwp;
    my $url = $self->server . $query;

	my $rv = 1;	# default to failed update

	# PUT modified xml
	my $request = new HTTP::Request 'PUT', $url;
	$request->header('content-type' => 'application/xml');
	$request->content( $xml->toString() );
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

################################################################################

=pod

B<set_raw_ingest_status()>

Set the exit status of ingest.pl (raw ingest) on the slide artifact using the
artifact's parent process ID

Parameters:
 XML::LibXML object with artifact XML
 error code and message from ingest.pl (string)

Returns:
 edited XML::LibXML object

=cut

sub set_raw_ingest_status {
	my $self	= shift;
	my $uri		= shift;
	my $status	= shift;
	my $is_error	= shift;	# 1 = error, 0 = success

	#print STDERR "URI: $uri\n";

	my $result 	= $self->query( $uri );
	my $xml		= $result->data;
	#print "ORIGINAL\n" . $xml->toString(1) . "\n";
	
	# check for existing artifact-flag
	#my $is_set	= 'n';
	#my $nodes	= $xml->findnodes( '//artifact-flag' );
	#foreach my $node (@{$nodes}) {
		#print STDERR "artifact flag is set\n";
	#	$is_set 	= 'y';
	#	$xml		= undef;
	#}

	# choose which attribute to set based on whether the status is an error or if they ingest was successful
	my $name	= 'Ingest successful';
	my $typeid	= 1;
	if($is_error == 1) {
		$name		= 'External Program Error';
		$typeid		= -1;
	}

	#print STDERR "SETTING ARTIFACT-FLAG: $name $typeid\n";

	# if flag is not already set, create and set a new one
	#if($is_set eq 'n') {
		# Add artifact-flag node
		my($docnode)	= $xml->getDocumentElement();
		my $flag	= $xml->createElement( 'artifact-flag' );
		$docnode->appendChild( $flag );
		
		# 2011-07-25
		my $date	= $self->timestamp(FORMAT => 'YYYY-MM-DD');

		# Set artifact-flag data, attrs & add children
		$flag->setAttribute( 'name' => $name );
		$flag->setAttribute( 'typeID' => $typeid );
		$flag->appendTextChild( 'last-modified-date'  => $date );
		$flag->appendTextChild( 'note' => $status );
		my $assignee	= $xml->createElement('assignee');
		# assign to Dave Miller
		$assignee->setAttribute( 'uri' => $self->server() . 'researchers/2' );
		my $creator	= $xml->createElement('creator');
		# set apiuser as creator of falg
		$creator->setAttribute( 'uri' => $self->server() . 'researchers/53' );
		$flag->appendChild( $assignee );
		$flag->appendChild( $creator );
		#print STDERR "RESULT: ", $xml->toString(1), "\n";
	
		# set flag in LIMS
		$self->put($xml, $uri);
	#}

	return($xml);
}

################################################################################

=pod

B<unset_raw_ingest_status()>

Unset the exit status of ingest.pl (raw ingest) on the slide artifact

Parameters:
 XML::LibXML object with artifact XML

Returns:

=cut

sub unset_raw_ingest_status {
	my $self	= shift;
	my $uri		= shift;
	my $status	= shift;

	#print STDERR "URI: $uri\n";

	my $result 	= $self->query( $uri );
	my $xml		= $result->data;
	#print "ORIGINAL\n" . $xml->toString(1) . "\n";
	
	# check for existing artifact-flag
	my $nodes	= $xml->findnodes( '//artifact-flag' );
	foreach my $node (@{$nodes}) {
		#print STDERR "artifact flag is set\n";

 		$node->unbindNode;

		$self->put($xml, $uri);
	}

	return();
}

################################################################################

=pod

B<projects()>,
B<samples()>,
B<researchers()>,
B<processtypes()>

Retrieve URIs for all items of a given type in LIMS

Parameters:
 none:

Returns:
 QCMG::DB::GeneusResult object

These queries are special cases and need to be treated with some care.
They provide a list of the URI's for all items in the database of a
particular type, e.g. samples.  The problem is that the LIMS will not
provide all of this information in a single query so multiple "pages"
from multiple queries are required to make any one of these queries
work.  This means that the B<data()> method called on the
QCMG::DB::GeneusResults object from one of these queries only contains
the XML and data structure from B<the most recent query>.

There are 2 additional QCMG::DB::GeneusResults methods which are used
for these queries: B<query_count()> and B<uri_list()>.  The first tells
you how many sub-queries were required to assemble the whole URI list.
This is useful because if the query_count() is "1" then the data()
and reply() methods return valid values because there was only one query
- otherwise those methods return information from the I<last> query.
The B<uri_list()> method returns a ref to an array of all of the URIs
found across all of the queries.

=cut
sub projects {
    my $self = shift;
    return $self->_list_items( 'project' );
}

sub samples {
    my $self = shift;
    return $self->_list_items( 'sample' );
}

sub researchers {
    my $self = shift;
    return $self->_list_items( 'researcher' );
}

sub processtypes {
    my $self = shift;
    return $self->_list_items( 'processtype' );
}



################################################################################


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


################################################################################

=pod

B<sample()>

Retrieve data about a single sample

Parameters:
 none:

Returns:
 QCMG::DB::GeneusResult object

=cut
sub sample {
    my $self   = shift;
    my $limsid = getid( shift );

    my $result = $self->query( "samples/$limsid" );
    return $result if ($result->status == 0);
    croak 'FATAL [',$result->status,'] : ', $result->status_msg;
}


################################################################################

=pod

B<resultfile_by_slide()>

Retrieve artifact ResultFile for a nominated slide
 (for 

Parameters:
 slide: scalar string

Returns:
 QCMG::DB::GeneusResult object

=cut

sub resultfile_by_slide {
    my $self  = shift;
    my $slide = shift;
    return $self->query( "artifacts?type=ResultFile&name=$slide" );
}

################################################################################

=pod

B<processes_by_slide()>

Retrieve all processes for a nominated slide

Parameters:
 slide: scalar string

Returns:
 QCMG::DB::GeneusResult object

=cut

sub processes_by_slide {
    my $self  = shift;
    my $slide = shift;
    return $self->query( "processes/?type=SOLiD Sequencing Run&udf.Slide=$slide" );
}


################################################################################

=pod

B<project_by_patient()>

Retrieve project for a patient using the patient ID (APGI-1992)

Parameters:
 slide: scalar string

Returns:
 QCMG::DB::GeneusResult object

=cut

sub project_by_patient {
    my $self	= shift;
    my $id	= shift;

	# replace _ with - if used (APGI_1992 -> APGI-1992)
	$id		=~ s/\_/\-/;

	# can't seem to search by project name so get all projects and filter
	# them here (CRL is putting feature request in for this 110720)
	my $res		= $self->projects();
	my $data	= $res->data;

# <project uri="http://qcmg-gltest.imb.uq.edu.au:8080/api/v1/projects/BIA4848" limsid="BIA4848">
#  <name>APGI-1959</name>
# </project>

	# get requested patient
	my $limsid;
	my $nodes = $data->findnodes("//project[name/text() = '$id']");
	foreach my $node (@{$nodes}) {
		#my $uri		= $node->getAttribute("uri");
		$limsid	= $node->getAttribute("limsid");
	}
	$res		= $self->query("projects/$limsid");
	$data		= $res->data;

	#print STDERR $data->toString(1);

# <prj:project xmlns:udf="http://genologics.com/ri/userdefined" xmlns:ri="http://genologics.com/ri" xmlns:file="http://genologics.com/ri/file" xmlns:prj="http://genologics.com/ri/project" uri="http://qcmg-gltest.imb.uq.edu.au:8080/api/v1/projects/BIA4857" limsid="BIA4857">
#   <name>APGI-1992</name>
#   <open-date>2009-12-03</open-date>
#   <researcher uri="http://qcmg-gltest.imb.uq.edu.au:8080/api/v1/researchers/65"/>
#   <udf:type name="ICGC Pancreatic Cancer">
#     <udf:field type="String" name="Ion Torrent Verification Index Name">MID-2</udf:field>
#     <udf:field type="String" name="Ion Torrent Verification Index Sequence">ACGCTCGACA</udf:field>
#   </udf:type>
#   <permissions uri="http://qcmg-gltest.imb.uq.edu.au:8080/api/v1/permissions/projects/BIA4857"/>
# </prj:project>

	my %project	= ();

	$nodes = $data->findnodes( '//name/text()' );
	foreach my $node (@{$nodes}) {
		$project{'name'}	= $node->toString(1);
	}

	$nodes = $data->findnodes( '//open-date/text()' );
	foreach my $node (@{$nodes}) {
		$project{'open-date'}	= $node->toString(1);
	}

	$nodes = $data->findnodes( '//researcher' );
	foreach my $node (@{$nodes}) {
		$project{'researcher'}	= $node->getAttribute("uri");
	}

	$nodes = $data->findnodes( '//udf:type' );
	foreach my $node (@{$nodes}) {
		$project{'type'}	= $node->getAttribute("name");
	}

	$nodes = $data->findnodes( '//udf:field[@name="Ion Torrent Verification Index Name"]/text()' );
	foreach my $node (@{$nodes}) {
		my $val		= $node->toString(1);
		$project{'Ion Torrent Verification Index Name'}	= $val;
	}

	$nodes = $data->findnodes( '//udf:field[@name="Ion Torrent Verification Index Sequence"]/text()' );
	foreach my $node (@{$nodes}) {
		my $val		= $node->toString(1);
		$project{'Ion Torrent Verification Index Sequence'}	= $val;
	}

	#print Dumper %project;

	return($data, \%project);
}


################################################################################

=pod

B<run_by_slide()>

Retrieve all run processes for a nominated slide

Parameters:
 slide: scalar string

Returns:
 QCMG::DB::GeneusResult object

=cut

sub run_by_slide {
    my $self  = shift;
    my $slide = shift;

    #return $self->query( "processes/?Slide=$slide" . '&type=SOLiD Sequencing Run' );

	# http://qcmg-gltest:8080/api/v1/artifacts?type=ResultFile&udf.Slide=S0449_20100603_1_Frag
	# $slide = 'S0449_20100603_1_Frag';
	my $res		= $self->query( "artifacts?type=ResultFile&udf.Slide=$slide" );
	my $data	= $res->data;

	my($artifactnode) = $data->findnodes( '//artifact[1]' );
	# TODO: check that $artifactnode is not empty
	my $limsid = $artifactnode->getAttribute( 'limsid' );

	$res		= $self->query( "artifacts/$limsid" );
	$data		= $res->data;

	my (@barcodes, $pd, $f5pri, $f3pri, $r3pri, $bcpri, $prilib, $mtype, $stype, $etype,
		$pid, $sample, $agroup, $qcflag, $type, $parent, $otype, $name );

# <name>S0449_20100603_1_Frag.nopd.nobc</name>
# <type>ResultFile</type>
# <output-type>MapSet</output-type>
# <parent-process
#         uri="http://qcmg-gltest.imb.uq.edu.au:8080/api/v1/processes/ING-DKM-110707-24-48483"
#         limsid="ING-DKM-110707-24-48483"/>
# <qc-flag>UNKNOWN</qc-flag>
# <sample uri="http://qcmg-gltest.imb.uq.edu.au:8080/api/v1/samples/BIA5053A3" limsid="BIA5053A3"/>
# <reagent-label name="nobc"/>
# <udf:field type="String" name="F3 Primary Reads">primary.20100610005836273</udf:field>
# <udf:field type="String" name="F5 Primary Reads"></udf:field>
# <udf:field type="String" name="R3 Primary Reads"></udf:field>
# <udf:field type="String" name="Physical Division">nopd</udf:field>
# <udf:field type="String" name="Barcode">nobc</udf:field>
# <udf:field type="String" name="Slide">S0449_20100603_1_Frag</udf:field>
# <udf:field type="String" name="Primary Library">Library_20100423_C</udf:field>
# <udf:field type="String" name="Experiment Type">genome</udf:field>
# <udf:field type="String" name="Source Type">Xenograft (X)</udf:field>
# <udf:field type="String" name="Material Type">DNA (D)</udf:field>
# <udf:field type="String" name="Patient ID">APGI-2125</udf:field>
# <artifact-group name="Exome - pancreatic project" uri="http://qcmg-gltest.imb.uq.edu.au:8080/api/v1/artifactgroups/64"/>
# 
# <udf:field type="String" name="F3 Primary Reads">primary.20111128102900535</udf:field>
# <udf:field type="String" name="F5 Primary Reads">primary.20111120060105754</udf:field>
# <udf:field type="String" name="BC Primary Reads">primary.20111114205737794</udf:field>

	# find udf nodes
	my ($barcode);
	my $nodes = $data->findnodes( '//udf:field[@name="Barcode"]/text()' );
	foreach my $node (@{$nodes}) {
		push @barcodes, $node->toString(1);
	}

	$nodes = $data->findnodes( '//udf:field[@name="Experiment Type"]/text()' );
	foreach my $node (@{$nodes}) {
		$etype = $node->toString(1);
	}

	$nodes = $data->findnodes( '//udf:field[@name="Slide"]/text()' );
	foreach my $node (@{$nodes}) {
		$slide = $node->toString(1);
	}

	$nodes = $data->findnodes( '//udf:field[@name="Primary Library"]/text()' );
	foreach my $node (@{$nodes}) {
		$prilib = $node->toString(1);
	}

	$nodes = $data->findnodes( '//udf:field[@name="F3 Primary Reads"]/text()' );
	foreach my $node (@{$nodes}) {
		$f3pri = $node->toString(1);
	}

	$nodes = $data->findnodes( '//udf:field[@name="F5 Primary Reads"]/text()' );
	foreach my $node (@{$nodes}) {
		$f5pri = $node->toString(1);
	}

	$nodes = $data->findnodes( '//udf:field[@name="R3 Primary Reads"]/text()' );
	foreach my $node (@{$nodes}) {
		$r3pri = $node->toString(1);
	}

	$nodes = $data->findnodes( '//udf:field[@name="BC Primary Reads"]/text()' );
	foreach my $node (@{$nodes}) {
		$bcpri = $node->toString(1);
	}

	$nodes = $data->findnodes( '//udf:field[@name="Patient ID"]/text()' );
	foreach my $node (@{$nodes}) {
		$pid = $node->toString(1);
	}

	$nodes = $data->findnodes( '//udf:field[@name="Physical Division"]/text()' );
	foreach my $node (@{$nodes}) {
		$pd = $node->toString(1);
	}

	$nodes = $data->findnodes( '//udf:field[@name="Material Type"]/text()' );
	foreach my $node (@{$nodes}) {
		$mtype = $node->toString(1);
	}

	$nodes = $data->findnodes( '//udf:field[@name="Source Type"]/text()' );
	foreach my $node (@{$nodes}) {
		$stype = $node->toString(1);
	}

	$nodes = $data->findnodes( '//sample' );
	foreach my $node (@{$nodes}) {
		$sample = $node->getAttribute("limsid");
	}

	$nodes = $data->findnodes( '//type/text()' );
	foreach my $node (@{$nodes}) {
		$type = $node->toString(1);
	}

	$nodes = $data->findnodes( '//artifact-group' );
	foreach my $node (@{$nodes}) {
		$agroup = $node->getAttribute("name");
	}

	$nodes = $data->findnodes( '//qc-flag/text()' );
	foreach my $node (@{$nodes}) {
		$qcflag = $node->toString(1);
	}

	$nodes = $data->findnodes( '//parent-process' );
	foreach my $node (@{$nodes}) {
		$parent = $node->getAttribute("limsid");
	}

	$nodes = $data->findnodes( '//output-type/text()' );
	foreach my $node (@{$nodes}) {
		$otype = $node->toString(1);
	}

	$nodes = $data->findnodes( '//name/text()' );
	foreach my $node (@{$nodes}) {
		$name = $node->toString(1);
	}

	my %slide	= (
				'Physical Division'	=> $pd,
				'F5 Primary Reads'	=> $f5pri,
				'F3 Primary Reads'	=> $f3pri,
				'R3 Primary Reads'	=> $r3pri,
				'BC Primary Reads'	=> $bcpri,
				'Primary Library'	=> $prilib,
				'barcodes'		=> \@barcodes,
				'artifact-group'	=> $agroup,
				'output-type'		=> $otype,
				'sample'		=> $sample,
				'qc-flag'		=> $qcflag,
				'parent-process'	=> $parent,
				'Experiment Type'	=> $etype,
				'Material Type'		=> $mtype,
				'Source Type'		=> $stype,
				'Patient ID'		=> $pid,
				'type'			=> $type,
				'name'			=> $name,
				'slide'			=> $slide
			);

	#print Dumper %slide;

	return($data, \%slide);
}

################################################################################

=pod

B<torrent_run_by_slide()>

Retrieve all run processes for a nominated Ion Torrent slide

Parameters:
 slide: scalar string

Returns:
 QCMG::DB::GeneusResult object

=cut

sub torrent_run_by_slide {
    my $self  = shift;
    my $slide = shift;

    #return $self->query( "processes/?Slide=$slide" . '&type=SOLiD Sequencing Run' );

	# http://qcmg-gltest:8080/api/v1/artifacts?type=ResultFile&udf.Slide=S0449_20100603_1_Frag
	# $slide = 'S0449_20100603_1_Frag';
	# http://qcmg-glprod.imb.uq.edu.au:8080/api/v1/artifacts?type=ResultFile&udf.Slide=T00001_20110924_49
	my $res		= $self->query( "artifacts?type=ResultFile&udf.Slide=$slide" );
	my $data	= $res->data;
	print Dumper $data;

=cut
<art:artifacts>
<artifact limsid="BIA4857A5PG15" uri="http://qcmg-glprod.imb.uq.edu.au:8080/api/v1/artifacts/BIA4857A5PG15"/>
</art:artifacts>
=cut

	my $artifactnodes = $data->findnodes( '//artifact' );

	my %slide;
	foreach my $artifactnode (@{$artifactnodes}) {
		my $limsid = $artifactnode->getAttribute( 'limsid' );
		#print STDERR "LIMS ID: $limsid\n";

		$res		= $self->query( "artifacts/$limsid" );
		$data		= $res->data;

		#print Dumper $data->toString(1);

		my ($barcode, $pd, $sampleuri, $reagentlabel, $prilib, $mtype, $stype, $etype,
			$pid, $sample, $agroup, $qcflag, $type, $parent, $otype,
			$name , $nodes);

=cut
<art:artifact xmlns:udf="http://genologics.com/ri/userdefined" xmlns:file="http://genologics.com/ri/file" xmlns:art="http://genologics.com/ri/artifact" uri="http://qcmg-gltest.imb.uq.edu.au:8080/api/v1/artifacts/92-57218?state=26213" limsid="92-57218">
  <name>T00001_20120419_162.nopd.IonXpress_009</name>
  <type>ResultFile</type>
  <output-type>MapSet</output-type>
  <parent-process uri="http://qcmg-gltest.imb.uq.edu.au:8080/api/v1/processes/PGM-API-120427-24-52147" limsid="PGM-API-120427-24-52147"/>
  <qc-flag>UNKNOWN</qc-flag>
  <sample uri="http://qcmg-gltest.imb.uq.edu.au:8080/api/v1/samples/BIA4927A1" limsid="BIA4927A1"/>
  <reagent-label name="IonXpress_009"/>
  <udf:field type="String" name="Physical Division">nopd</udf:field>
  <udf:field type="String" name="Barcode">IonXpress_009</udf:field>
  <udf:field type="String" name="Slide">T00001_20120419_162</udf:field>
  <udf:field type="String" name="Primary Library">Library_20120306_I</udf:field>
  <udf:field type="String" name="Source Type">Normal (N)</udf:field>
  <udf:field type="String" name="Material Type">DNA (D)</udf:field>
  <udf:field type="String" name="Patient ID">APGI-2219</udf:field>
  <artifact-gro
=cut

		# find udf nodes
		$nodes = $data->findnodes( '//name/text()' );
		foreach my $node (@{$nodes}) {
			$name = $node->toString(1);
			#print STDERR "NAME: $name\n";
		}

		$nodes = $data->findnodes( '//type/text()' );
		foreach my $node (@{$nodes}) {
			$type = $node->toString(1);
			#print STDERR "TYPE: $type\n";
		}

		$nodes = $data->findnodes( '//output-type/text()' );
		foreach my $node (@{$nodes}) {
			$otype = $node->toString(1);
			#print STDERR "OTYPE: $otype\n";
		}

		$nodes = $data->findnodes( '//parent-process' );
		foreach my $node (@{$nodes}) {
			$parent = $node->getAttribute("limsid");
			#print STDERR "PARENT: $parent\n";
		}

		$nodes = $data->findnodes( '//qc-flag/text()' );
		foreach my $node (@{$nodes}) {
			$qcflag = $node->toString(1);
			#print STDERR "QCFLAG: $qcflag\n";
		}

		$nodes = $data->findnodes( '//sample' );
		foreach my $node (@{$nodes}) {
			$sample = $node->getAttribute("limsid");
			#print STDERR "SAMPLE: $sample\n";
		}

		$nodes = $data->findnodes( '//reagent-label' );
		foreach my $node (@{$nodes}) {
			$reagentlabel = $node->getAttribute("name");
			#print STDERR "REAGENTLABEL: $reagentlabel\n";
		}

		$nodes = $data->findnodes( '//udf:field[@name="Physical Division"]/text()' );
		foreach my $node (@{$nodes}) {
			$pd = $node->toString(1);
		}

		$nodes = $data->findnodes( '//udf:field[@name="Barcode"]/text()' );
		foreach my $node (@{$nodes}) {
			$barcode = $node->toString(1);
			#print STDERR "BARCODE: $barcode\n";
		}
	
		#$nodes = $data->findnodes( '//udf:field[@name="Experiment Type"]/text()' );
		#foreach my $node (@{$nodes}) {
		#	$etype = $node->toString(1);
		#	print STDERR "ETYPE: $etype\n";
		#}
	
		$nodes = $data->findnodes( '//udf:field[@name="Slide"]/text()' );
		foreach my $node (@{$nodes}) {
			$slide = $node->toString(1);
			#print STDERR "SLIDE: $slide\n";
		}
	
		$nodes = $data->findnodes( '//udf:field[@name="Primary Library"]/text()' );
		foreach my $node (@{$nodes}) {
			$prilib = $node->toString(1);
			#print STDERR "PRI LIB: $prilib\n";
		}
	
		$nodes = $data->findnodes( '//udf:field[@name="Source Type"]/text()' );
		foreach my $node (@{$nodes}) {
			$stype = $node->toString(1);
		}

		$nodes = $data->findnodes( '//udf:field[@name="Material Type"]/text()' );
		foreach my $node (@{$nodes}) {
			$mtype = $node->toString(1);
		}

		$nodes = $data->findnodes( '//udf:field[@name="Patient ID"]/text()' );
		foreach my $node (@{$nodes}) {
			$pid = $node->toString(1);
			#print STDERR "PID: $pid\n";
		}
	
		$nodes = $data->findnodes( '//artifact-group' );
		foreach my $node (@{$nodes}) {
			$agroup = $node->getAttribute("name");
		}

		%{$slide{$limsid}}	= (
				'Physical Division'	=> $pd,
				'Primary Library'	=> $prilib,
				'barcode'		=> $barcode,
				'output-type'		=> $otype,
				'qc-flag'		=> $qcflag,
				'parent-process'	=> $parent,
				'Experiment Type'	=> $etype,
				'Material Type'		=> $mtype,
				'Source Type'		=> $stype,
				'Patient ID'		=> $pid,
				'type'			=> $type,
				'name'			=> $name,
				'slide'			=> $slide,
				'artifact-group'	=> $agroup
			);
	}

=cut
my ($data, $slide)	= $gl->torrent_run_by_slide($slidename);
foreach my $field (keys %{$slide}) {
	foreach $key (keys %{$slide->{$field}}) {
			print "$key: ".$slide->{$field}->{$key}."\n";
	}
}
=cut

	return($data, \%slide);
}


sub _lwp {
    my $self = shift;
    return $self->{lwp};
}

sub _libxml {
    my $self = shift;
    return $self->{libxml};
}


################################################################################

=pod

B<run_by_process()>

Retrieve a run process using the process ID (more rigorous than using slide
name)

Parameters:
 slide: scalar string

Returns:
 QCMG::DB::GeneusResult object

=cut

sub run_by_process {
    my $self		= shift;
    my $process_id	= shift;

    #return $self->query( "processes/?Slide=$slide" . '&type=SOLiD Sequencing Run' );

	# http://qcmg-gltest:8080/api/v1/artifacts?type=ResultFile&udf.Slide=S0449_20100603_1_Frag
	# $slide = 'S0449_20100603_1_Frag';
	my $res		= $self->query( "processes/$process_id" );
	my $data	= $res->data;

	#print $data->toString(1);

	my ($pnode)	= $data->find( '//output' );
	my @mapsets	= ();
	foreach my $node (@{$pnode}) {
		my $mapseturi = $node->getAttribute("uri");
		push @mapsets, $mapseturi;
	}

	$pnode		= $data->find( '//input' );
	my $parenturi;
	foreach my $node (@{$pnode}) {
		$parenturi = $node->getAttribute("post-process-uri");
		$parenturi =~ s/.+?\/(artifacts\/.+)/$1/;
		$parenturi = $1;
	}

	my %slide;	# hash of hashes
	foreach (@mapsets) {
		my $uri		= $_;
	
		$uri		=~ s/.+?\/(artifacts\/.+)/$1/;
		$uri		= $1;
		$res		= $self->query($uri);
		$data		= $res->data;
	
		#print $data->toString(1);
	
		my (@barcodes, $pd, $f5pri, $f3pri, $r3pri, $bcpri, $prilib, $mtype, $stype, $etype,
			$pid, $sample, $agroup, $qcflag, $type, $parent, $otype, $name,
			$slide, $barcode );
	
		# find udf nodes
		my $nodes = $data->findnodes( '//udf:field[@name="Barcode"]/text()' );
		foreach my $node (@{$nodes}) {
			$barcode =  $node->toString(1);
		}

		$nodes = $data->findnodes( '//udf:field[@name="Experiment Type"]/text()' );
		foreach my $node (@{$nodes}) {
			$etype = $node->toString(1);
		}
	
		$nodes = $data->findnodes( '//udf:field[@name="Slide"]/text()' );
		foreach my $node (@{$nodes}) {
			$slide = $node->toString(1);
		}
	
		$nodes = $data->findnodes( '//udf:field[@name="Primary Library"]/text()' );
		foreach my $node (@{$nodes}) {
			$prilib = $node->toString(1);
		}
	
		$nodes = $data->findnodes( '//udf:field[@name="F3 Primary Reads"]/text()' );
		foreach my $node (@{$nodes}) {
			$f3pri = $node->toString(1);
		}
	
		$nodes = $data->findnodes( '//udf:field[@name="F5 Primary Reads"]/text()' );
		foreach my $node (@{$nodes}) {
			$f5pri = $node->toString(1);
		}
	
		$nodes = $data->findnodes( '//udf:field[@name="R3 Primary Reads"]/text()' );
		foreach my $node (@{$nodes}) {
			$r3pri = $node->toString(1);
		}

		$nodes = $data->findnodes( '//udf:field[@name="BC Primary Reads"]/text()' );
		foreach my $node (@{$nodes}) {
			$bcpri = $node->toString(1);
		}
	
		$nodes = $data->findnodes( '//udf:field[@name="Patient ID"]/text()' );
		foreach my $node (@{$nodes}) {
			$pid = $node->toString(1);
		}
	
		$nodes = $data->findnodes( '//udf:field[@name="Physical Division"]/text()' );
		foreach my $node (@{$nodes}) {
			$pd = $node->toString(1);
		}
	
		$nodes = $data->findnodes( '//udf:field[@name="Material Type"]/text()' );
		foreach my $node (@{$nodes}) {
			$mtype = $node->toString(1);
		}
	
		$nodes = $data->findnodes( '//udf:field[@name="Source Type"]/text()' );
		foreach my $node (@{$nodes}) {
			$stype = $node->toString(1);
		}
	
		$nodes = $data->findnodes( '//sample' );
		foreach my $node (@{$nodes}) {
			$sample = $node->getAttribute("limsid");
		}
	
		$nodes = $data->findnodes( '//type/text()' );
		foreach my $node (@{$nodes}) {
			$type = $node->toString(1);
		}
	
		$nodes = $data->findnodes( '//artifact-group' );
		foreach my $node (@{$nodes}) {
			$agroup = $node->getAttribute("name");
		}
	
		$nodes = $data->findnodes( '//qc-flag/text()' );
		foreach my $node (@{$nodes}) {
			$qcflag = $node->toString(1);
		}
	
		$nodes = $data->findnodes( '//parent-process' );
		foreach my $node (@{$nodes}) {
			$parent = $node->getAttribute("limsid");
		}

		$nodes = $data->findnodes( '//output-type/text()' );
		foreach my $node (@{$nodes}) {
			$otype = $node->toString(1);
		}
	
		$nodes = $data->findnodes( '//name/text()' );
		foreach my $node (@{$nodes}) {
			$name = $node->toString(1);
		}
	
		my %mapset	= (
					'Physical Division'	=> $pd,
					'F5 Primary Reads'	=> $f5pri,
					'F3 Primary Reads'	=> $f3pri,
					'R3 Primary Reads'	=> $r3pri,
					'BC Primary Reads'	=> $bcpri,
					'Primary Library'	=> $prilib,
					'barcode'		=> $barcode,
					'artifact-group'	=> $agroup,
					'output-type'		=> $otype,
					'sample'		=> $sample,
					'qc-flag'		=> $qcflag,
					'parent-process'	=> $parent,
					'parent-uri'		=> $parenturi,
					'Experiment Type'	=> $etype,
					'Material Type'		=> $mtype,
					'Source Type'		=> $stype,
					'Patient ID'		=> $pid,
					'type'			=> $type,
					'name'			=> $name,
					'slide'			=> $slide,
					'uri'			=> $uri
				);

		$slide{$name} = \%mapset;
	
		#print Dumper $slide{$name};
	}

	return($data, \%slide);
}
################################################################################

=pod

B<torrent_run_by_process()>

Retrieve a run process using the process ID (more rigorous than using slide
name)

Parameters:
 slide: scalar string

Returns:
 QCMG::DB::GeneusResult object

=cut

sub torrent_run_by_process {
    my $self		= shift;
    my $process_id	= shift;

    #return $self->query( "processes/?Slide=$slide" . '&type=SOLiD Sequencing Run' );

	# http://qcmg-gltest:8080/api/v1/artifacts?type=ResultFile&udf.Slide=S0449_20100603_1_Frag
	# $slide = 'S0449_20100603_1_Frag';
	my $res		= $self->query( "processes/$process_id" );
	my $data	= $res->data;

	#print $data->toString(1);
=cut
  <type uri="http://qcmg-glprod.imb.uq.edu.au:8080/api/v1/processtypes/382">PGM Sequencing Run</type>
  <date-run>2012-04-19</date-run>
  <technician uri="http://qcmg-glprod.imb.uq.edu.au:8080/api/v1/researchers/203">
    <first-name>Tim</first-name>
    <last-name>Bruxner</last-name>
  </technician>
  <input-output-map>
    <input post-process-uri="http://qcmg-glprod.imb.uq.edu.au:8080/api/v1/artifacts/2-55826?state=24480" uri="http://qcmg-glprod.imb.uq.edu.au:8080/api/v1/artifacts/2-55826?state=24476" limsid="2-55826">
      <parent-process uri="http://qcmg-glprod.imb.uq.edu.au:8080/api/v1/processes/ION-ANC-120419-24-51766" limsid="ION-ANC-120419-24-51766"/>
    </input>
    <output uri="http://qcmg-glprod.imb.uq.edu.au:8080/api/v1/artifacts/92-55830?state=24479" output-generation-type="PerAllInputs" output-type="ResultFile" limsid="92-55830"/>
  </input-output-map>
  <udf:type name="Multiplex Fragment Sequencing">
    <udf:field type="String" name="Barcode Set">IonXpress</udf:field>
  </udf:type>
  <udf:field type="Numeric" name="FLOWS">520</udf:field>
  <udf:field type="String" name="Sequencing Kit Lot Number">01501-11</udf:field>
  <udf:field type="String" name="Sequencing Kit">Ion Sequencing 200 Kit (p/n #4471258)</udf:field>
  <udf:field type="String" name="Chip Type">318</udf:field>
  <udf:field type="String" name="Reference Library">ICGC_HG19</udf:field>
  <udf:field type="String" name="Project">targetseq_109</udf:field>
  <udf:field type="String" name="Run Type">TARS</udf:field>
  <udf:field type="String" name="Plan Name">Library_20120413_E</udf:field>
  <udf:field type="String" name="Plan Short ID">KQ1KO</udf:field>
  <udf:field type="String" name="Variant Frequency">Somatic</udf:field>
  <protocol-name>Multiplex Sequencing 200bp</protocol-name>
  <instrument uri="http://qcmg-glprod.imb.uq.edu.au:8080/api/v1/instruments/257"/>
  <process-parameter name="ionrunplan.py"/>
=cut
	
	my ($parent_uri, $output_uri, $flows, $lotnum, $kit, $chiptype,
		$reflib, $project, $runtype, $planname, $planshortid,
		$varfreq, $protocol, $instrument_uri, $process_param,
		$run_type, $name, $parent_process );

	# find udf nodes
	my $nodes = $data->findnodes( '//udf:field[@name="FLOWS"]/text()' );
	foreach my $node (@{$nodes}) {
		$flows =  $node->toString(1);
	}

	$nodes = $data->findnodes( '//output' );
	foreach my $node (@{$nodes}) {
		$output_uri = $node->getAttribute("uri");
	}

	$nodes = $data->findnodes( '//udf:field[@name="Sequencing Kit Lot Number"]/text()' );
	foreach my $node (@{$nodes}) {
		$lotnum =  $node->toString(1);
	}

	$nodes = $data->findnodes( '//udf:field[@name="Sequencing Kit"]/text()' );
	foreach my $node (@{$nodes}) {
		$kit =  $node->toString(1);
	}

	$nodes = $data->findnodes( '//udf:field[@name="Chip Type"]/text()' );
	foreach my $node (@{$nodes}) {
		$chiptype =  $node->toString(1);
	}

	$nodes = $data->findnodes( '//udf:field[@name="Reference Library"]/text()' );
	foreach my $node (@{$nodes}) {
		$reflib =  $node->toString(1);
	}

	$nodes = $data->findnodes( '//udf:field[@name="Project"]/text()' );
	foreach my $node (@{$nodes}) {
		$project = $node->toString(1);
	}

	$nodes = $data->findnodes( '//udf:field[@name="Run Type"]/text()' );
	foreach my $node (@{$nodes}) {
		$runtype = $node->toString(1);
	}

	$nodes = $data->findnodes( '//udf:field[@name="Plan Name"]/text()' );
	foreach my $node (@{$nodes}) {
		$planname = $node->toString(1);
	}

	$nodes = $data->findnodes( '//udf:field[@name="Plan Short ID"]/text()' );
	foreach my $node (@{$nodes}) {
		$planshortid = $node->toString(1);
	}

	$nodes = $data->findnodes( '//udf:field[@name="Variant Frequency"]/text()' );
	foreach my $node (@{$nodes}) {
		$varfreq = $node->toString(1);
	}

	$nodes = $data->findnodes( '//instrument' );
	foreach my $node (@{$nodes}) {
		$instrument_uri = $node->getAttribute("uri");
	}

	$nodes = $data->findnodes( '//parent-process' );
	foreach my $node (@{$nodes}) {
		$parent_process = $node->getAttribute("uri");
	}

	$nodes = $data->findnodes( '//process-parameter' );
	foreach my $node (@{$nodes}) {
		$process_param = $node->getAttribute("name");
	}

	$nodes = $data->findnodes( '//protocol-name/text()' );
	foreach my $node (@{$nodes}) {
		$protocol = $node->toString(1);
	}

	#  http://qcmg-glprod.imb.uq.edu.au:8080/api/v1/artifacts/92-55830?state=24479
	$output_uri	=~ s/.+?\/(artifacts\/.+)/$1/;
	$output_uri	= $1;
	$res		= $self->query($output_uri);
	$data		= $res->data;

	#print $data->toString(1);

	my $nodes = $data->findnodes( '//name/text()' );
	foreach my $node (@{$nodes}) {
		$name =  $node->toString(1);
	}

	my %hash	= (
				'output_uri'		=> $output_uri,
				'FLOWS'			=> $flows,
				'Sequencing Kit Lot Number'	=> $lotnum,
				'Sequencing Kit'	=> $kit,
				'Chip Type'		=> $chiptype,
				'Reference Library'	=> $reflib,
				'Project'		=> $project,
				'Run Type'		=> $runtype,
				'Plan Name'		=> $planname,
				'Plan Short ID'		=> $planshortid,
				'Variant Frequency'	=> $varfreq,
				'protocol-name'		=> $protocol,
				'slide'			=> $name,
				'process-parameter'	=> $process_param,
				'parent-process_uri'	=> $parent_process
				#'uri'			=> $uri
			);

	my %slide;
	$slide{$name}	= \%hash;
	#print Dumper $slide{$name};

	return($data, \%slide);
}

################################################################################

=pod

B<dual_index_not_sequenced()>

Determine if this slide had two indexes but only one was sequenced

Parameters:
 slide: scalar string

Returns:
 scalar "true" or null (when not defined)

=cut
sub dual_index_not_sequenced {
	my $self  = shift;
	my $slide = shift;

	# http://qcmg-glprod.imb.uq.edu.au:8080/api/v1/processes?type=3.%20Cluster%20Generation%20(Illumina%20SBS)%203.0&udf.Flow%20Cell%20ID=C1RCWACXX
	#                                                                                                                                     _________
	# only flow cell changes in query

	#print STDERR "In dual_index_not_sequenced()\n";

	# slide    = 130423_7001407_0103_AC1RCWACXX
	# flowcell =                      C1RCWACXX
	$slide		=~ /^\d{6}\_.+?\_\d{4}\_[A,B](.+)$/;
	my $flowcell	= $1;
	#print STDERR "SLIDE:\t\t$slide\nFLOWCELL:\t$flowcell\n";
	my $res		= $self->query("processes?type=3.%20Cluster%20Generation%20(Illumina%20SBS)%203.0&udf.Flow%20Cell%20ID=$flowcell");
	#print Dumper $res;
	#print STDERR "processes?type=3.%20Cluster%20Generation%20(Illumina%20SBS)%203.0&udf.Flow%20Cell%20ID=$flowcell\n";
	my $data	= $res->data;

	# possible values: 'true', 'false'
	my $flag	= '';

	#print $data->toString(1);
	# <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
	# <prc:processes xmlns:prc="http://genologics.com/ri/process">
  	#  <process uri="http://qcmg-glprod.imb.uq.edu.au:8080/api/v1/processes/SBS-SIX-130423-24-71866" limsid="SBS-SIX-130423-24-71866"/>
	# </prc:processes>

	# This happens sometimes, not sure why; return error here instead of
	# whole script failing
	#<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
	#<prc:processes xmlns:prc="http://genologics.com/ri/process"/>
	if($data->toString(1) =~ /<prc:processes.+process"\/>/) {
		return('ERROR: '.$data->toString(1));
	}

	my $nodes	= $data->findnodes( '//process' );
	my $process_uri	= '';
	# http://qcmg-glprod.imb.uq.edu.au:8080/api/v1/processes/SBS-SIX-130423-24-71866
	foreach my $node (@{$nodes}) {
		$process_uri = $node->getAttribute("uri");
	}
	$process_uri		=~ /.+?\/(processes\/.+)/;
	$process_uri 		= $1;

	$res		= $self->query( $process_uri );
	$data	= $res->data;

	#print $data->toString(1);

	# <udf:field type="Boolean" name="Only first index of dual index will be sequenced">true</udf:field>
	$nodes = $data->findnodes( '//udf:field[@name="Only first index of dual index will be sequenced"]/text()' );
	foreach my $node (@{$nodes}) {
		$flag = $node->toString(1);
	}

	#print STDERR "FLAG: $flag\n";

	return($flag);
}

################################################################################
=pod

B<torrent_slide2chiptype()>

Get the chip type of a slide using the slidename

Parameters:
 slidename (T0001_blah_blah)

Returns:
 scalar chip type string

=cut
sub torrent_slide2chiptype {
    	my $self	= shift;
	my $slidename	= shift;

	my ($data, $slide)	= $self->torrent_run_by_slide($slidename);
	my $parent_process;
	foreach my $field (keys %{$slide}) {
		foreach my $key (keys %{$slide->{$field}}) {
			if($key eq 'parent-process') {
				$parent_process = $slide->{$field}->{$key};
				last;
			}
		}
	}
	
	my ($data, $slide)	= $self->torrent_run_by_process($parent_process);
	foreach my $mapset (keys %{$slide}) {
		foreach my $key (keys %{$slide->{$mapset}}) {
			if($key eq 'parent-process_uri') {
				$parent_process = $slide->{$mapset}->{$key};
				last;
			}
		}
	}
	
	$parent_process		=~ s/^.+\/([\w\-]+)$/$1/;
	my ($data, $slide)	= $self->torrent_run_by_process($parent_process);
	my $chip_type;
	foreach my $mapset (keys %{$slide}) {
		foreach my $key (keys %{$slide->{$mapset}}) {
			if($key eq 'Chip Type') {
				$chip_type = $slide->{$mapset}->{$key}, "\n";
				last;
			}
		}
	}

	return($chip_type);
}

################################################################################
=pod

B<torrent_slide2projecttype()>

Get the project type of a slide using the slidename

Parameters:
 slidename (T0001_blah_blah)

Returns:
 scalar chip type string

=cut
sub torrent_slide2projecttype {
    	my $self	= shift;
	my $slidename	= shift;

	my ($data, $slide)	= $self->torrent_run_by_slide($slidename);
	my $parent_process;
	foreach my $field (keys %{$slide}) {
		foreach my $key (keys %{$slide->{$field}}) {
			if($key eq 'parent-process') {
				$parent_process = $slide->{$field}->{$key};
				last;
			}
		}
	}
	
	my ($data, $slide)	= $self->torrent_run_by_process($parent_process);
	foreach my $mapset (keys %{$slide}) {
		foreach my $key (keys %{$slide->{$mapset}}) {
			if($key eq 'parent-process_uri') {
				$parent_process = $slide->{$mapset}->{$key};
				last;
			}
		}
	}
	
	$parent_process		=~ s/^.+\/([\w\-]+)$/$1/;
	my ($data, $slide)	= $self->torrent_run_by_process($parent_process);
	my $project_type;
	foreach my $mapset (keys %{$slide}) {
		foreach my $key (keys %{$slide->{$mapset}}) {
			if($key eq 'Project') {
				$project_type = $slide->{$mapset}->{$key}, "\n";
				last;
			}
		}
	}

	return($project_type);
}

################################################################################
=pod

B<torrent_slide2refgenome()>

Get the reference genome used for a slide using the slidename

Parameters:
 slidename (T0001_blah_blah)

Returns:
 scalar chip type string

=cut
sub torrent_slide2refgenome {
    	my $self	= shift;
	my $slidename	= shift;

	my ($data, $slide)	= $self->torrent_run_by_slide($slidename);
	my $parent_process;
	foreach my $field (keys %{$slide}) {
		foreach my $key (keys %{$slide->{$field}}) {
			if($key eq 'parent-process') {
				$parent_process = $slide->{$field}->{$key};
				last;
			}
		}
	}
	
	my ($data, $slide)	= $self->torrent_run_by_process($parent_process);
	foreach my $mapset (keys %{$slide}) {
		foreach my $key (keys %{$slide->{$mapset}}) {
			if($key eq 'parent-process_uri') {
				$parent_process = $slide->{$mapset}->{$key};
				last;
			}
		}
	}
	
	$parent_process		=~ s/^.+\/([\w\-]+)$/$1/;
	my ($data, $slide)	= $self->torrent_run_by_process($parent_process);
	my $refgenome;
	foreach my $mapset (keys %{$slide}) {
		foreach my $key (keys %{$slide->{$mapset}}) {
			if($key eq 'Reference Library') {
				$refgenome = $slide->{$mapset}->{$key}, "\n";
				last;
			}
		}
	}

	return($refgenome);
}

### Non-OO Functions ######################################################


# Parse the ~/.geneus/gl_credentials.cfg file if it exists.  This
# routine will fail gracefully if the file is missing but will throw
# fatal errors if $QCMG_HOME environment variable is not set or the
# ~/.geneus directory exists but is not chmod'd to 700.
#
# If the user has a ~/.geneus/gl_credentials.cfg file then we should
# read it for user-settable parameters. The first line that matches the
# requested server should be used:
#   qcmg-gltest:::apiuser:::<testpwd>
#   qcmg-glprod:::apiuser:::<prodpwd>

sub _read_gl_credentials {
    # full URL: http://qcmg-gltest.imb.uq.edu.au:8080/api/v1/
    my $server	= shift @_;	

	#print STDERR "SERVER: $server\n";

    $server	= 'qcmg-gltest' if(! $server);
	#print STDERR "SERVER: $server\n";

    die 'FATAL: Set environment variable $QCMG_HOME to use Perl Geneus API'
        unless defined $ENV{QCMG_HOME};

    my %config = ();

    # Exit unless we found ~/.geneus directory
    my $gldir = $ENV{QCMG_HOME};
    $gldir .= '/' unless ($gldir =~ /\/$/);
    $gldir .= '.geneus';
    return \%config unless -d $gldir;

    # Check for 0700 permissions on ~/.geneus
    my $mode = (stat($gldir))[2];
    my $gldir_perm = sprintf("%04o", $mode & 07777);
    die "FATAL: ~/.geneus permissions must be 0700, not $gldir_perm\n"
        unless $gldir_perm eq '0700';

    # Exit unless we found ~/.geneus/gl_credentials.cfg
    my $glcred = $gldir . '/gl_credentials.cfg';
    return \%config unless -f $glcred;

    # Read config values out of ~/.geneus/gl_credentials.cfg
    #my $fh = IO::File->new($glcred, 'r');
    #while (my $line = $fh->getline) {
    # For some reason, the above code wasn't parsing the credentials file
    # correctly so the below code was added. Weerd
    local($/) = undef;
    open(FH, $glcred);
    my $fc = <FH>;
    close(FH);
    my @lines	= split /\n/, $fc;
    foreach (@lines) {
	my $line	= $_;
	#print STDERR "CRED LINE: $line\n";
        chomp $line;
        $line =~ s/^\s+//g;     # drop leading spaces
        $line =~ s/\s+$//g;     # drop trailing spaces
        next if $line =~ /^#/;  # skip comments
        #next unless $line =~ /^.+=.+$/;  # skip lines not in A=B pattern
	next unless $line =~ /\:\:\:/;

	$line	=~ /(.+?)\:\:\:(.+?)\:\:\:(.+)/;
        #my ($key, $value) = split /\s*=\s*/, $line;
        #$config{ $key } = $value;
	my $glserver	= $1;
	my $user	= $2;
	my $password	= $3;

	#print STDERR "$glserver, $user, $password\n";

	# if user has requested a specific server, get the credentials line for
	# that server
	#print STDERR "SERVER: $server / GLSERVER: $glserver\n";
	if($server =~ /gl/ && $server !~ /$glserver/) {
		#print STDERR "Skipping, no match\n";
		next;
	}

	#$config{'server'}	= 'http://'.$server.'.imb.uq.edu.au:8080/api/v1/';
	$config{'server'}	= 'http://'.$server.':8080/api/v1/';
	$config{'user'}		= $user;
	$config{'password'}	= $password;

	#print Dumper %config;
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
