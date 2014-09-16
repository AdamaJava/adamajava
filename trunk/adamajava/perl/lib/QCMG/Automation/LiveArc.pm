package QCMG::Automation::LiveArc;

# $Id: LiveArc.pm 4660 2014-07-23 12:18:43Z j.pearson $

=pod

=head1 NAME

QCMG::Automation::LiveArc -- Common functions for ingesting raw sequencing files 

=head1 SYNOPSIS

Most common use:
 my $mf = QCMG::Automation::LiveArc->new();

To query a different database:
 my $mf = QCMG::Automation::LiveArc->new();

=head1 DESCRIPTION

This class contains methods for checking out raw sequencing files and copying
them to appropriate places via Mediaflux/LiveArc

=head1 REQUIREMENTS

 Exporter

=cut

use strict;
use Data::Dumper;

use QCMG::Automation::Common;
our @ISA = qw(QCMG::Automation::Common);	# inherit Common methods

=pod

=head1 VARIABLES AND TAGS

 Module version
  $VERSION

=cut

################################################################################

=pod

=head1 METHODS

B<new()> (constructor)

Create a new instance of this class.

Parameters:
  RUN_FOLDER => directory

  Optional:
	any of the variables specified in ini files (LA_HOST, etc.)...

Returns:
 a new instance of this class.

=cut

sub new {
        my $that  = shift;

	#print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

        my $class = ref($that) || $that;
        my $self = bless $that->SUPER::new(), $class;

	my $options = {};
	$self->{options} = $options;
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                #defined($_[($i + 1)]) || warn "Odd number of params: $this_sub : $!";
                $options->{uc($_[$i])} = $_[($i + 1)];
        }
	$self->{'VERBOSE'}		= $options->{'VERBOSE'};
	#$self->{'VERBOSE'}		= 1; 

	$self->{'LA_BASE'}  = $self->LA_JAVA;
	$self->{'LA_BASE'} .= qq{ -Dmf.result=shell -Dmf.cfg=};
	$self->{'LA_BASE'} .= $self->LA_CRED_FILE;
	$self->{'LA_BASE'} .= qq{ -Xmx1g -jar };
	$self->{'LA_BASE'} .= $self->LA_ATERM;

	#$self->{'LA_EXECUTE'}  = $self->LA_JAVA;
	#$self->{'LA_EXECUTE'} .= qq{ -Dmf.result=shell -Dmf.cfg=};
	#$self->{'LA_EXECUTE'} .= $self->LA_CRED_FILE;
	#$self->{'LA_EXECUTE'} .= qq{ -jar };
	#$self->{'LA_EXECUTE'} .= $self->LA_ATERM;
	$self->{'LA_EXECUTE'} = $self->{'LA_BASE'}.qq{ --app exec };

	#$self->{'LA_IMPORT'}  = $self->LA_JAVA;
	#$self->{'LA_IMPORT'} .= qq{ -Dmf.result=shell -Dmf.cfg=};
	#$self->{'LA_IMPORT'} .= $self->LA_CRED_FILE;
	#$self->{'LA_IMPORT'} .= qq{ -jar };
	#$self->{'LA_IMPORT'} .= $self->LA_ATERM;
	$self->{'LA_IMPORT'}	= $self->{'LA_BASE'}.qq{ --app exec import };


	# this works on barrine:
	#java -Dmf.result=shell -Dmf.cfg=/home/uqlfink/.mediaflux/mf_credentials.cfg -jar /panfs/imb/qcmg/software/aterm.jar --app exec execute asset.get :id path=/QCMG/test_lmp/test_lmp_seq_raw

	#print STDERR "EXECUTE: $self->{'LA_EXECUTE'}\n" if($self->{'VERBOSE'});
	#print STDERR "IMPORT:  $self->{'LA_IMPORT'}\n" if($self->{'VERBOSE'});

	return $self;
}

################################################################################
=pod

B<namespace_exists()> 
 Query the mediaflux server to see if a namespace already exists. 

Parameters:
 NAMESPACE - string with full namespace to check
 CMD       - just return command

Returns:
 scalar: 1 = if does not exist; 0 = if does exist; 2 if unexpected result is found

=cut
sub namespace_exists {
        my $self = shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n" if($self->{'VERBOSE'});

	print STDERR "Checking namespace existence in LiveArc...\n" if($self->{'VERBOSE'});

        # parse params
        my $options = {};

        # read params from @_ and put in %options
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                #defined($_[($i + 1)]) || warn "Odd number of params: $this_sub : $!";
                $options->{$_[$i]} = $_[($i + 1)];
        }

	$options->{'NAMESPACE'} = $self->LA_NAMESPACE if(!  $options->{'NAMESPACE'});

	my $status = 2;

	# check run namespace first => see if run has been added
	# COMMAND: asset.namespace.exists :namespace "/QCMG/S0449_20100924_2_FragPEBC"
	# RESULT:  :exists -namespace "/QCMG/S0449_20100924_2_FragPEBC" "true"
	# RESULT:  :exists -namespace "/QCMG/S0449_20100924_2_FragPEBC" "false"

	my $cmd	= qq{$self->{'LA_EXECUTE'} asset.namespace.exists :namespace "$options->{'NAMESPACE'}"};#"

	if($options->{'CMD'}) {
		return($cmd);
	}

	my $r;
	my $num_try	= 0;
	until($status == 0 || $status == 1 || $num_try > 3) {
		$r	= `$cmd`;

		print STDERR "$cmd\nrv: $r\n$?\n" if($self->{'VERBOSE'});
	
		if($r =~ /.+?false/) {
			$status = 1;
		}
		elsif($r =~ /.+?true/) {
			$status = 0;
		}
		else {
			# LA returned unexpected output; consider as failed command
			# get return value of la command and look up error code
			print STDERR $self->LACOMMAND_EXIT->{$?}."\n";
			#exit($?);
		}

		sleep(240) if($status == 2);
		$num_try++;
	}

	#print STDERR "STATUS: $status\n";
		
	return($status);
}

################################################################################
=pod

B<asset_exists()> 
 Query the mediaflux server to see if an asset exists. 

Parameters:
 ASSET     - asset name
 NAMESPACE - namespace of asset 
  or
 ID        - asset ID

 CMD       - return command line only

Returns:
 scalar: ID, if it exists. void, otherwise

=cut
sub asset_exists {
        my $self = shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

	print STDERR "Checking asset existence in LiveArc...\n" if($self->{'VERBOSE'});

        # parse params
        my $options = {};

        # read params from @_ and put in %options
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                #defined($_[($i + 1)]) || warn "Odd number of params: $this_sub : $!";
                $options->{$_[$i]} = $_[($i + 1)];
        }

	$options->{'NAMESPACE'} = $self->LA_NAMESPACE if(!  $options->{'NAMESPACE'});

	$options->{'NAMESPACE'} =~ s/\/$//;

	my $cmd;
	if($options->{'ID'}) {
		$cmd = qq{$self->{'LA_EXECUTE'} asset.exists :id $options->{'ID'} };
	}
	elsif($options->{'ASSET'}) {
		$cmd = qq{$self->{'LA_EXECUTE'} asset.exists :id path=$options->{'NAMESPACE'}/$options->{'ASSET'} };
	}

	print STDERR "$cmd\n" if($self->{'VERBOSE'});

	if($options->{'CMD'}) {
		return($cmd);
	}

=cut
	# ping LiveArc to see if it is alive; if so, continue; otherwise, take
	# some naps and keep trying
	my $num_try	= 0;
	my $ping_rv	= 2; # 0 = good; 1 = connection refused
	until($ping_rv == 0 || $num_try > 3) {

		my $ping	= $self->{'LA_EXECUTE'}." ping";
		my $ping_rv	= `$ping`;

		sleep(240) if($ping_rv ne 0 || $ping_rv =~ /transport/);
		$num_try++;
	}
=cut

	my $r	= `$cmd`;

	my $id;
	if($options->{'ID'}) {
  		# :exists -id "40468" "true"
		$r =~ /\-id \"(\d+)\"\s\"(\w+)\"/;

		if($2 eq 'true') {
			$id = $1;
		}
	}
	elsif($options->{'ASSET'}) {
		# :exists -id "path=test/S0449_20100603_1_Frag/S0449_20100603_1_Frag_seq_raw" "true"
		$r =~ /\-id \"(.+?)\"\s\"(\w+)\"/;

		if($2 eq 'true') {
			my $meta = $self->get_asset(NAMESPACE => $options->{'NAMESPACE'}, ASSET => $options->{'ASSET'});
			$id = $meta->{'ID'};
		}
	}

	return($id);
}

################################################################################
=pod

B<create_namespace()> 
 Create a namespace in Mediaflux/LiveArc

Parameters:
 NAMESPACE - string with full namespace to create
 CMD       - just return command

Returns:
 scalar: 1 = if does not exist; 0 = if does exist; 2 if unexpected result is found

=cut
sub create_namespace {
        my $self = shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

	print STDERR "Creating namespace in LiveArc...\n" if($self->{'VERBOSE'});

        # parse params
        my $options = {};

        # read params from @_ and put in %options
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                #defined($_[($i + 1)]) || warn "Odd number of params: $this_sub : $!";
                $options->{$_[$i]} = $_[($i + 1)];
        }

	$options->{'NAMESPACE'} = $self->LA_NAMESPACE if(!  $options->{'NAMESPACE'});

	my $status = 2;

	my $cmd = qq{$self->{'LA_EXECUTE'} asset.namespace.create :namespace $options->{'NAMESPACE'}};

	if($options->{'CMD'}) {
		return($cmd);
	}

	my $r;
	my $num_try	= 0;
	until($status == 0 || $status == 1 || $num_try > 3) {
		$r	= `$cmd`;
	
		print STDERR "(attempt $num_try) $cmd\nrv: $r\n" if($self->{'VERBOSE'});
	
		if($r =~ /.+?false/) {
			$status = 1;
		}
		else {
			$status = 0;
		}

		sleep(240) if($status == 2);
		$num_try++;
	}
		
	return($status);
}

################################################################################
=pod

B<create_asset()> 
 Create Mediaflux/LiveArc asset

Parameters:
 NAMESPACE - namespace to create asset in
 ASSET     - string with full asset name to create
 CMD       - just return command

Returns:
 scalar: asset ID

=cut
sub create_asset {
        my $self = shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

	print STDERR "Creating asset in LiveArc...\n" if($self->{'VERBOSE'});

        # parse params
        my $options = {};

        # read params from @_ and put in %options
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                #defined($_[($i + 1)]) || warn "Odd number of params: $this_sub : $!";
                $options->{$_[$i]} = $_[($i + 1)];
        }

	$options->{'NAMESPACE'} = $self->LA_NAMESPACE if(!  $options->{'NAMESPACE'});

	my $status = 2;

	# COMMAND: asset.exists :id "path=/QCMG/S0449_20100924_2_FragPEBC/S0449_20100924_2_FragPEBC_seq_raw"
	# RESULT:  :exists -id "path=/QCMG/S0449_20100924_2_FragPEBC/S0449_20100924_2_FragPEBC_seq_raw" "true"
	# RESULT:  :exists -id "path=/QCMG/S0449_20100924_2_FragPEBC/S0449_20100924_2_FragPEBC_seq_raw" "false"

	# asset.create :namespace /QCMG/test_lmp :name test_lmp_seq_raw_test
    	# :id "167172" (if successful)
	# OR
	# error: ...

	my $cmd = qq{$self->{'LA_EXECUTE'} asset.create :namespace $options->{'NAMESPACE'} :name $options->{'ASSET'}};
	print STDERR "$cmd\n" if($self->{'VERBOSE'});

	if($options->{'CMD'}) {
		return($cmd);
	}

	#my $r;
	my $num_try	= 0;
	#until($r =~ /\:id/ || $
	my $r	= `$cmd`;
	print STDERR "rv: $r\n" if($self->{'VERBOSE'});

	$r =~ /:id\s+\"(\d+)\"/;
	my $id = $1;

	# return asset ID if successful, will be empty otherwise
	return($id);
}

################################################################################
=pod

B<get_asset()> 
 Get data about an asset

Parameters:
 NAMESPACE - namespace 
 ASSET     - asset name
 ID	   - asset ID
 CMD       - just return command

Returns:
 ref to hash of metadata types as keys, and their values as the hash values
 NOTE: the more complex attributes will need to be parsed by the user (until we
       decide if we only want certain bits of information and code for them)

=cut
sub get_asset {
        my $self = shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

	print STDERR "Getting asset data in Mediaflux...\n" if($self->{'VERBOSE'});

        # parse params
        my $options = {};

        # read params from @_ and put in %options
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                #defined($_[($i + 1)]) || warn "Odd number of params: $this_sub : $!";
                $options->{$_[$i]} = $_[($i + 1)];
        }

	$options->{'NAMESPACE'} = $self->LA_NAMESPACE if(!  $options->{'NAMESPACE'});

	my $status = 1;

	$options->{'NAMESPACE'} =~ s/\/$//;

	my $cmd = qq{$self->{'LA_EXECUTE'} asset.get :id };
	if(! $options->{'ID'}) {
		$cmd .= qq{ "path=$options->{'NAMESPACE'}/$options->{'ASSET'}"};#"
	}
	else {
		# use ID if provided
		$cmd .= qq{ $options->{'ID'}};
	}

	print STDERR "$cmd\n" if($self->{'VERBOSE'});

	if($options->{'CMD'}) {
		return($cmd);
	}

	my $r	= `$cmd`;
	print STDERR "rv: $r\n" if($self->{'VERBOSE'});

	my %meta;
	$r	=~ /-id\s+\"(\d+)\"/;
	$meta{'ID'}		= $1;
	#print STDERR "ID $1\n";
	$r	=~ /-version\s+\"(\d+)\"/;
	$meta{'VERSION'}	= $1;
	#print STDERR "VERSION $1\n";
	$r	=~ /-vid\s+\"(\d+)\"/;
	$meta{'VID'}		= $1;
	#print STDERR "VID $1\n";
	$r	=~ /:type\s+\"(.+?)\"/;
	$meta{'TYPE'}		= $1;
	#print STDERR "TYPE $1\n";
	$r	=~ /:namespace\s+\"(.+?)\"/;
	$meta{'NAMESPACE'}	= $1;
	#print STDERR "NAMESPACE $1\n";
	$r	=~ /:name\s+\"(.+?)\"/;
	$meta{'NAME'}		= $1;
	#print STDERR "NAME $1\n";
	$r	=~ /:path\s+\"(.+?)\"/;
	$meta{'PATH'}		= $1;
	#print STDERR "PATH $1\n";
	$r	=~ /:creator\s+(\-id.+?:domain.+?:user.+?)\"/s;
	$meta{'CREATOR'}	= $1;
	#print STDERR "CREATOR $1\n";
	$r	=~ /:ctime\s+(.+)/;
	$meta{'CTIME'}		= $1;
	#print STDERR "CTIME $1\n";
	$r	=~ /:mtime\s+(.+)/;
	$meta{'MTIME'}		= $1;
	#print STDERR "MTIME $1\n";
	$r	=~ /:stime\s+\"(\d+)\"/;
	$meta{'STIME'}		= $1;
	#print STDERR "STIME $1\n";
	$r	=~ /:versioned\s+(.+)/;
	$meta{'VERSIONED'}	= $1;
	#print STDERR "VERSIONED $1\n";
	$r	=~ /:access\s+(:access.+?:modify.+?:destroy.+?:access-content.+?:modify-content.+?)/s;
	$meta{'ACCESS'}		= $1;
	#print STDERR "ACCESS $1\n";
	$r	=~ /:meta\s+(.+?)\n\s+:content\s+\-id/s;
	$meta{'META'}		= $1;
	#print STDERR "META $1\n";
	$r	=~ /:content\s+(.+)/s;
	$meta{'CONTENT'}	= $1;
	#print STDERR "CONTENT $1\n";

	# return ref to hash; keys = field, values = values
	return(\%meta);
}


################################################################################
=pod

B<list_assets()> 
 List all Mediaflux/LiveArc assets

Parameters:
 NAMESPACE - namespace to search in
 CMD       - just return command

Returns:

=cut
sub list_assets {
        my $self = shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

	print STDERR "Querying asset in Mediaflux...\n" if($self->{'VERBOSE'});

        # parse params
        my $options = {};

        # read params from @_ and put in %options
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                #defined($_[($i + 1)]) || warn "Odd number of params: $this_sub : $!";
                $options->{$_[$i]} = $_[($i + 1)];
        }

	$options->{'NAMESPACE'} = $self->LA_NAMESPACE if(!  $options->{'NAMESPACE'});

	#print STDERR "NS: ".$options->{'NAMESPACE'}."\n";

	$options->{'NAMESPACE'} =~ s/\/$//;

	#print STDERR "NS: ".$options->{'NAMESPACE'}."\n";

	my $status = 1;

	# get all assets with their id and path; find the one we want
	# (asset.query in simpler form doesn't seem to work)
	# THIS ONLY RETURNS ASSETS THAT EXIST, NOT EMPTY NAMESPACES
	my $cmd = qq{$self->{'LA_EXECUTE'} asset.query :where "namespace>=$options->{'NAMESPACE'}" :action get-path :size 1000000};

	print STDERR "$cmd\n" if($self->{'VERBOSE'});

	if($options->{'CMD'}) {
		return($cmd);
	}

	my @r	= `$cmd`;

	#    :path -id "167092" "/QCMG/S0417_20100831_1_FragBC/:S0417_20100831_1_FragBC_seq_raw"
	#    :path -id "167089" "/QCMG/S0413_20100831_1_FragBC/:S0413_20100831_1_FragBC_seq_raw"
	#    ...
	my %assets = ();
	foreach (@r) {
		chomp;

		if(/$options->{'ASSET'}/) {
			$status = 0;
			#print STDERR "Asset found: $_\n";

			#  :path -id "50754" -version "0" "/QCMG_torrent/T01_20120212_124_FragBC/:T01_20120212_124_FragBC.nopd.nobc.wells"
			/-id\s+\"(\d+)\"\s+\-version\s+\"\d+\"\s+\"(.+?)\"/;

			#print STDERR "ID: $1, PATH: $2\n";

			$assets{$2} = $1;
		}
	}

	#/QCMG/S0413_20100831_1_FragBC/:S0413_20100831_1_FragBC_seq_raw  167089
	# key = namespace:asset; value = id
	return(\%assets);
}

################################################################################
=pod

B<list_contents()> 
 List the contents of an archive

Parameters:
 ID	   - ID of archive asset
 CMD       - just return command

Returns:

=cut
sub list_contents {
        my $self = shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

	print STDERR "Querying asset in Mediaflux...\n" if($self->{'VERBOSE'});

        # parse params
        my $options = {};

        # read params from @_ and put in %options
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                #defined($_[($i + 1)]) || warn "Odd number of params: $this_sub : $!";
                $options->{$_[$i]} = $_[($i + 1)];
        }


	my $status = 1;

	# get all assets contents with their index and name
	my $cmd = qq{$self->{'LA_EXECUTE'} asset.archive.content.list :id $options->{'ID'} :size 10000};

	print STDERR "$cmd\n" if($self->{'VERBOSE'});

	if($options->{'CMD'}) {
		return($cmd);
	}

	my @r	= `$cmd`;

	my %assets = ();
	foreach (@r) {
		chomp;

		# > asset.archive.content.list :id  70216
    		# :entry -idx "1" -size "0" "plugin_out/variantCaller_out/drmaa_stdout.txt.~1~"
    		# :entry -idx "2" -size "32813" "plugin_out/variantCaller_out/variantCaller.html"
    		# :entry -idx "3" -size "2629" "plugin_out/variantCaller_out/variantCaller_block.html"
    		# :entry -idx "4" -size "74051" "plugin_out/variantCaller_out/IonXpress_011/variants.xls"
    		# :entry -idx "5" -size "937" "plugin_out/variantCaller_out/IonXpress_011/filelinks.xls"
		/-idx\s+\"(\d+)\"\s+\-size\s+\"\d+\"\s+\"(.+?)\"/;

		#print STDERR "ID: $1, NAME: $2\n";

		$assets{$2} = $1;
	}

	# key = name; value = index
	return(\%assets);
}

################################################################################
=pod

B<delete_asset()> 
 Delete Mediaflux/LiveArc asset

Parameters:
 NAMESPACE - namespace of asset
 ASSET     - string with full asset name to remove 
 ID	   - asset ID

Returns:

=cut
sub delete_asset {
        my $self = shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

	print STDERR "Deleting asset in LiveArc...\n" if($self->{'VERBOSE'});

        # parse params
        my $options = {};

        # read params from @_ and put in %options
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                #defined($_[($i + 1)]) || warn "Odd number of params: $this_sub : $!";
                $options->{$_[$i]} = $_[($i + 1)];
        }

	$options->{'NAMESPACE'} = $self->LA_NAMESPACE if(!  $options->{'NAMESPACE'});

	my $status = 1;

	# COMMAND: asset.exists :id "path=/QCMG/S0449_20100924_2_FragPEBC/S0449_20100924_2_FragPEBC_seq_raw"
	# RESULT:  :exists -id "path=/QCMG/S0449_20100924_2_FragPEBC/S0449_20100924_2_FragPEBC_seq_raw" "true"
	# RESULT:  :exists -id "path=/QCMG/S0449_20100924_2_FragPEBC/S0449_20100924_2_FragPEBC_seq_raw" "false"

	my $asset = join "/", $options->{'NAMESPACE'}, $options->{'ASSET'};
	$asset =~ s/\/\//\//g;	# remove double path characters // -> /

	# asset.destroy :id path=/QCMG/test_lmp/test_lmp_seq_raw_test2
	#my $cmd = qq{$self->{'LA_EXECUTE'} asset.destroy :id path=$asset};
	my $cmd = qq{$self->{'LA_EXECUTE'} asset.destroy :id };
	if(! $options->{'ID'}) {
		$cmd .= qq{ path=$asset};
	}
	else {
		$cmd .= $options->{'ID'};
	}

	if($options->{'CMD'}) {
		return($cmd);
	}

	my $r	= `$cmd`;
	print STDERR "DELETE: $cmd\nrv: $r\n" if($self->{'VERBOSE'});

	if($r =~ /error/) {
		$status = 1;
	}
	elsif(! $r) {
		$status = 0;
	}
	else {
		# LA returned unexpected output; consider as failed command
		# get return value of la command and look up error code
		print STDERR $self->LACOMMAND_EXIT->{$?}."\n";
		#exit($?);
	}
		
	return($status);
}

################################################################################
=pod

B<delete_namespace()> 
 Delete Mediaflux/LiveArc namespace

Parameters:
 NAMESPACE - namespace of asset
 #ID	   - asset ID
 CMD       - just return command 

Returns:

=cut
sub delete_namespace {
        my $self = shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

	print STDERR "Deleting namespace in LiveArc...\n" if($self->{'VERBOSE'});

        # parse params
        my $options = {};

        # read params from @_ and put in %options
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                #defined($_[($i + 1)]) || warn "Odd number of params: $this_sub : $!";
                $options->{$_[$i]} = $_[($i + 1)];
        }

	$options->{'NAMESPACE'} = $self->LA_NAMESPACE if(!  $options->{'NAMESPACE'});

	my $status = 1;

	my $cmd = qq{$self->{'LA_EXECUTE'} asset.namespace.destroy :namespace $options->{'NAMESPACE'}};

	if($options->{'CMD'}) {
		return($cmd);
	}

	my $r	= `$cmd`;
	print STDERR "DELETE: $cmd\nrv: $r\n" if($self->{'VERBOSE'});

	if($r =~ /error/) {
		$status = 1;
	}
	elsif(! $r) {
		$status = 0;
	}
	else {
		# LA returned unexpected output; consider as failed command
		# get return value of la command and look up error code
		print STDERR $self->LACOMMAND_EXIT->{$?}."\n";
		#exit($?);
	}
		
	return($status);
}

################################################################################
=pod

B<extract_asset()> 
 Retrieve and decompress an asset

Parameters:
 ID    - id of asset
 ASSET - namespace/name of asset
 PATH  - place to decompress asset to
 RAW   - asset is a raw file (1); do not try to decompress it (default = 0, is compressed)
 CMD   - return command only, do not execute it (1)

Returns:
 scalar: 1 if error, 0 if success

=cut
sub extract_asset {
        my $self = shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

	print STDERR "Extracting asset in LiveArc...\n" if($self->{'VERBOSE'});

        # parse params
        my $options = {};

        # read params from @_ and put in %options
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                #defined($_[($i + 1)]) || warn "Odd number of params: $this_sub : $!";
                $options->{$_[$i]} = $_[($i + 1)];
        }

	#$options->{'ASSET'} = $self->LA_ASSET if(!  $options->{'ASSET'});

	my $status = 1;

	#asset.get :id path=/QCMG/138000411_20091013_2_1005_D_frag/138000411_20091013_2_1005_D_frag_seq_raw 
	#   :out -decompress true /panfs/imb/home/uqlfink/
	my $cmd	= qq{$self->{'LA_EXECUTE'} asset.get :id };
	if($options->{'ASSET'}) {
		$cmd	.= qq{ path=$options->{'ASSET'}};
	}
	elsif($options->{'ID'}) {
		$cmd	.= qq{ $options->{'ID'} };
	}

	# decide whether to decompress or not
	if($options->{'RAW'} == 1) {
		$cmd	.= qq{ :out $options->{'PATH'}};
	}
	else {
		$cmd	.= qq{ :out -decompress true $options->{'PATH'}};
	}

	if(! $options->{'CMD'}) {
		my $r	= `$cmd`;
		print STDERR "EXTRACT: $cmd\nrv: $r\n" if($self->{'VERBOSE'});
	
		if($r =~ /error/) {
			$status = 1;
		}
		elsif(! $r) {
			$status = 0;
		}
		else {
			# LA returned unexpected output; consider as failed command
			# get return value of la command and look up error code
			print STDERR $self->LACOMMAND_EXIT->{$?}."\n";
			#exit($?);
		}
			
		return($status);
	}
	else {
		return($cmd);
	}
}

################################################################################
=pod

B<extract_content()> 
 Retrieve a file from an archived directory

Parameters:
 ID    - id of asset
 IDX   - index of asset file
 PATH  - place to decompress asset to (asset will come out underneath the full path from the original archive
 CMD   - return command only, do not execute it (1)

Returns:
 scalar: 1 if error, 0 if success

=cut
sub extract_content {
        my $self = shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

	print STDERR "Extracting content in LiveArc...\n" if($self->{'VERBOSE'});

        # parse params
        my $options = {};

        # read params from @_ and put in %options
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                #defined($_[($i + 1)]) || warn "Odd number of params: $this_sub : $!";
                $options->{$_[$i]} = $_[($i + 1)];
        }

	#$options->{'ASSET'} = $self->LA_ASSET if(!  $options->{'ASSET'});

	my $status = 1;

	my $cmd	= qq{$self->{'LA_EXECUTE'} asset.archive.content.extract };
	$cmd	.= qq{ :id  $options->{'ID'} };
	$cmd	.= qq{ :idx $options->{'IDX'} };
	$cmd	.= qq{ :out $options->{'PATH'} };
	$cmd	.= qq{ -decompress 1 };

	if(! $options->{'CMD'}) {
		my $r	= `$cmd`;
		print STDERR "EXTRACT: $cmd\nrv: $r\n" if($self->{'VERBOSE'});
	
		if($r =~ /error/) {
			$status = 1;
		}
		elsif(! $r) {
			$status = 0;
		}
		else {
			# LA returned unexpected output; consider as failed command
			# get return value of la command and look up error code
			print STDERR $self->LACOMMAND_EXIT->{$?}."\n";
			#exit($?);
		}
			
		return($status);
	}
	else {
		return($cmd);
	}
}

################################################################################
=pod

B<update_asset_file()> 
 Update an asset by uploading a new file, which increments the version
  number of the asset (only works for files, not directories!)

Parameters:
 NAMESPACE - full namespace of asset
 ASSET - name of asset
 ID   - asset ID to update/reimport
 FILE - /path/and/file of new version of asset
 CMD  - just return command line string, don't run command

Returns:
 scalar - status of command (0 success, 1 failed) or command line string

=cut
sub update_asset_file {
        my $self = shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

	print STDERR "Updating asset in LiveArc...\n" if($self->{'VERBOSE'});

        # parse params
        my $options = {};

        # read params from @_ and put in %options
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                #defined($_[($i + 1)]) || warn "Odd number of params: $this_sub : $!";
                $options->{$_[$i]} = $_[($i + 1)];
        }

	my $status = 1;

#> asset.set :id 40101 :in file:/Users/l.fink/0_projects/qcmg/ingest_tools/S0449_20100603_1_Frag_run_definition.txt
#    :version "2"

	#print STDERR "Updating asset\n";

	my $cmd;
	if($options->{'ID'}) {
		$cmd	= qq{$self->{'LA_EXECUTE'} asset.set :id $options->{'ID'} :in file:$options->{'FILE'}};
	}
	else {
		$cmd	= qq{$self->{'LA_EXECUTE'} asset.set :id path=$options->{'NAMESPACE'}/$options->{'ASSET'} :in file:$options->{'FILE'}};
	}

	print STDERR "update_asset: $cmd\n" if($self->{'VERBOSE'});

	if(! $options->{'CMD'}) {
		my $r	= `$cmd`;
		print STDERR "UPDATE:    $cmd\nUPDATE rv: $r\n" if($self->{'VERBOSE'});
	
		if($r =~ /version/) {
			$status = 0;
		}
		elsif(! $r) {
			$status = 0;
		}
			
		return($status);
	}
	else {
		return($cmd);
	}
}

################################################################################
=pod

B<laimport_file()> 
 Create an asset by uploading a new file

Parameters:
 NAMESPACE - full namespace of asset
 ASSET - name of asset
 FILE - /path/and/file of new version of asset
 CMD  - just return command line string, don't run command

Returns:
 scalar - id of asset

=cut
sub laimport_file {
        my $self = shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

	print STDERR "Updating asset in LiveArc...\n" if($self->{'VERBOSE'});

        # parse params
        my $options = {};

        # read params from @_ and put in %options
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                #defined($_[($i + 1)]) || warn "Odd number of params: $this_sub : $!";
                $options->{$_[$i]} = $_[($i + 1)];
        }

	my $status = 1;

	# asset.create :namespace /test/ :name upload_test :in file:/Volumes/QCMG/ingest/S0449_20100603_1_Frag/ingest.log

	my $cmd = qq{$self->{'LA_EXECUTE'} asset.create :namespace $options->{'NAMESPACE'} :name $options->{'ASSET'} :in file:$options->{'FILE'}};

	print STDERR "laimport_file: $cmd\n" if($self->{'VERBOSE'});

	my $id = 0;
	if(! $options->{'CMD'}) {
		my $r	= `$cmd`;
		print STDERR "IMPORT FILE:    $cmd\nIMPORT FILE rv: $r\n" if($self->{'VERBOSE'});
	
		if($r =~ /id/) {
			$r =~ /\:id\s+\"(\d+)\"/;
			$id = $1;
		}
			
		return($id);
	}
	else {
		return($cmd);
	}
}

################################################################################
=pod

B<update_asset()> 
 Update an asset with metadata

Parameters:
 NAMESPACE - full namespace of asset
 ASSET     - name of asset
 ID        - asset ID to update/reimport
 DATA      - data string
 CMD       - just return command line string, don't run command

Returns:
 scalar - status of command or command line string

=cut
sub update_asset {
        my $self = shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

	print STDERR "Updating asset in LiveArc...\n" if($self->{'VERBOSE'});

        # parse params
        my $options = {};

        # read params from @_ and put in %options
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                #defined($_[($i + 1)]) || warn "Odd number of params: $this_sub : $!";
                $options->{$_[$i]} = $_[($i + 1)];
        }

	my $status = 1;

#> asset.set :id 40101 :in file:/Users/l.fink/0_projects/qcmg/ingest_tools/S0449_20100603_1_Frag_run_definition.txt
#    :version "2"

	#print STDERR "Updating asset\n";

	$options->{'DATA'} =~ s/>/\\>/g;
	$options->{'DATA'} =~ s/</\\</g;
	$options->{'DATA'} =~ s/"/\\"/g;
	$options->{'DATA'} =~ s/'/\\'/g;

	my $cmd;
	if($options->{'ID'}) {
		$cmd	= qq{$self->{'LA_EXECUTE'} asset.set :id $options->{'ID'} $options->{'DATA'}};
	}
	else {
		$cmd	= qq{$self->{'LA_EXECUTE'} asset.set :id path=$options->{'NAMESPACE'}/$options->{'ASSET'} $options->{'DATA'}};
	}

	print STDERR "update_asset: $cmd\n" if($self->{'VERBOSE'});

	if(! $options->{'CMD'}) {
		my $r	= `$cmd`;
		print STDERR "UPDATE:    $cmd\nUPDATE rv: $r\n" if($self->{'VERBOSE'});
	
		if($r =~ /error/) {
			$status = 1;
		}
		elsif($r =~ /version/) {
			$status = 0;
		}
		elsif(! $r) {
			$status = 0;
		}
		else {
			# LA returned unexpected output; consider as failed command
			# get return value of la command and look up error code
			#exit($self->EXIT_LIVEARC_ERROR());
		}
			
		return($status);
	}
	else {
		return($cmd);
	}
}

################################################################################
=pod

B<laimport()> 
 Import and archive namespace and assets

Parameters:
 NAMESPACE (namespace + run name)
 ASSET
 RUN_FOLDER or DIR
 METADATA (optional)
 CMD - return command only, do not execute it (1)

Returns:
 scalar: 0 if file(s) imported; 1 if no files imported

=cut
sub laimport {
        my $self = shift @_;

	# has to be named "laimport" to avoid clashing with existing funciton
	# "import"...

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

	#print STDERR "Importing to Mediaflux/LiveArc and archiving...\n";

        # parse params
        my $options = {};

        # read params from @_ and put in %options
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                #defined($_[($i + 1)]) || warn "Odd number of params: $this_sub : $!";
                $options->{$_[$i]} = $_[($i + 1)];
        }

	$options->{'RUN_FOLDER'}	= $options->{'DIR'}     if($options->{'DIR'});

	$options->{'NAMESPACE'}	 	= $self->LA_NAMESPACE   if(!  $options->{'NAMESPACE'});
	$options->{'RUN_FOLDER'} 	= $self->{'RUN_FOLDER'} if(!  $options->{'RUN_FOLDER'});

	#print STDERR "RF: $options->{'RUN_FOLDER'}\n";

	my $status	= 1;

	# concatenate namespace and asset name with a / ; make sure there is only
	# one /
	#my $assetns = $options->{'NAMESPACE'}."/".$options->{'ASSET'};
	#$assetns	=~ s/\/\//\//g;

	# import -namespace $MFLUX_NAMESPACE/$RUN_NAME -archive 1 -name $ASSET_NAME $RUN_FOLDER $MFLUX_METADATA

	# import -archive 1 -mode live -verbose true name="testingest/S0449_20100603_1_Frag_seq_raw"  /Volumes/QCMG/ingest/S0449_20100603_1_Frag/

	# --app import -namespace $MFLUX_NAMESPACE/$RUN_NAME -archive 1 -name $ASSET_NAME $RUN_FOLDER $MFLUX_METADATA


	my $cmd	 = $self->{'LA_IMPORT'};
	$cmd	.= qq{ -namespace };
	$cmd	.= $options->{'NAMESPACE'}.qq{ -archive 1 -name }.$options->{'ASSET'}." ".$options->{'RUN_FOLDER'};
 
	print STDERR "$cmd\n" if($self->{'VERBOSE'});

	if($options->{'METADATA'}) {
		$cmd	.= qq{ $options->{'METADATA'} };
	}
	elsif($self->{'METADATA'}) {
		$cmd	.= qq{ $self->{'METADATA'} };
	}

	if(! $options->{'CMD'}) {

		my $r	= `$cmd`;
	
		print STDERR "IMPORT rv: $r\n" if($self->{'VERBOSE'});
	
		# RETURN VALUES 
		# live: imported 0 file(s)
		# live: imported 1 file(s)
		# :version "2"
	
		$r =~ /imported\s+(\d+)\s+/;
		my $numfiles = $1;
	
		if($numfiles > 0) {
			$status = 0;
		}
		elsif($r =~ /version/) {
			$status = 0;
		}
			
		return($status);
	}
	else {
		return($cmd);
	}
}

################################################################################
=pod

B<laimport_profile()> 
 Import and archive namespace and assets filtered through a profile

Parameters:
 NAMESPACE (namespace + run name)
 ASSET
 DIR
 PROFILE
 CMD - return commands only, do not execute it (1)

Returns:
 scalar: 0 if file(s) imported; 1 if no files imported

=cut
=cut
sub laimport_profile {
        my $self = shift @_;

	# has to be named "laimport" to avoid clashing with existing funciton
	# "import"...

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

	#print STDERR "Importing to Mediaflux/LiveArc and archiving...\n";

        # parse params
        my $options = {};

        # read params from @_ and put in %options
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                #defined($_[($i + 1)]) || warn "Odd number of params: $this_sub : $!";
                $options->{$_[$i]} = $_[($i + 1)];
        }

	$options->{'NAMESPACE'}	 	= $self->LA_NAMESPACE   if(!  $options->{'NAMESPACE'});

	my $status	= 1;

	# --app exec import -namespace /test/ -lp torrent_ingest.fcp -mode live /results/analysis/output/Home/Analysis_T01-106_390/

	# MUST IMPORT AS ARCHIVE OR THIS WON"T WORK (SET IN PROFILE)

	# with profile: (can't specify name and won't archive)
	# import -namespace /test/ -lp torrent_ingest.fcp -mode live /results/analysis/output/Home/Analysis_T01-106_390/
	my $cmd	 = $self->{'LA_IMPORT'};
	$cmd	.= qq{ -namespace };
	$cmd	.= $options->{'NAMESPACE'};
	$cmd	.= qq{ -lp $options->{'PROFILE'} $options->{'DIR'} };

	# get name of bottom level directory -> will be asset name
	my ($v, $tempname)	= File::Spec->splitpath($options->{'DIR'});


	print STDERR "$cmd\n$rnc\n" if($self->{'VERBOSE'});

	if(! $options->{'CMD'}) {

		my $r	= `$cmd`;
	
		print STDERR "IMPORT rv: $r\n" if($self->{'VERBOSE'});
	
		# RETURN VALUES 
		# live: imported 0 file(s)
		# live: imported 1 file(s)
		# :version "2"
	
		$r =~ /imported\s+(\d+)\s+/;
		my $numfiles = $1;
	
		if($numfiles > 0) {
			$status = 0;
		}
		elsif($r =~ /version/) {
			$status = 0;
		}


		$status	= 1;

		# get ID of new asset
		my $id	= $self->asset_exists(NAMESPACE -> $options->{'NAMESPACE'}, ASSET => $tempname);
		$r	= `$rnc`;
			
		# rename asset to desired name
		my $rnc	= $self->{'LA_EXECUTE'};
		$rnc	.= qq{ asset.move :id $id :name $options->{'ASSET'} };

		if($r =~ /version/) {
			$status = 0;
		}

		return($status);
	}
	else {
		return($cmd);
	}
}
=cut

################################################################################
=pod

B<server_log()> 
 Update an asset

Parameters:
 NAME      - log name (http,https,local,tcpip,internal,install,server,syslog)

Returns:
 scalar - status of command or command line string

=cut
sub server_log {
        my $self = shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

	print STDERR "Getting LiveArc server log...\n" if($self->{'VERBOSE'});

        # parse params
        my $options = {};

        # read params from @_ and put in %options
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                #defined($_[($i + 1)]) || warn "Odd number of params: $this_sub : $!";
                $options->{$_[$i]} = $_[($i + 1)];
        }

	my $cmd = qq{$self->{'LA_EXECUTE'} server.log.display :name $options->{'NAME'} :size 100000000};

	my $r	= `$cmd`;

	# parse ugliness out
	$r =~ s/   :line -nb //g;

	return($r);
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
