package QCMG::DB::TrackLite;

##############################################################################
#
#  Module:   QCMG::DB::TrackLite.pm
#  Creator:  John V Pearson
#  Created:  2010-09-27
#
#  This class is a portal to information held in the Tracklite MySQL
#  (formerly SQLLite) database.
#
#  $Id: FileObject.pm 317 2010-09-24 03:21:24Z j.pearson $
#
##############################################################################

=pod

=head1 NAME

QCMG::DB::TrackLite -- Common functions for querying the TrackLite database

=head1 SYNOPSIS

Most common use:

 my $tl = QCMG::DB::TrackLite->new();
 $tl->connect();

To query a different database or as a different user:

 my $tl = QCMG::DB::TrackLite->new(
            host    => 'smg-other.imb.edu.au',
            db      => 'new_dbname',
            user    => 'anotherusername',
            passwd  => 'newpasswd'
          );
 $tl->connect();

=head1 DESCRIPTION

This class contains methods for connecting to a database, querying the database,
and gathering bits of information about entities stored in a database.

=head1 REQUIREMENTS

 Data::Dumper
 Carp
 DBI

=cut

use strict;
use warnings;
use Data::Dumper;
use Carp qw( carp croak );
use DBI;

use vars qw( $SVNID $REVISION );

( $REVISION ) = '$Revision: 317 $ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id: FileObject.pm 317 2010-09-24 03:21:24Z j.pearson $'
    =~ /\$Id:\s+(.*)\s+/;

################################################################################

=pod

=head1 METHODS

B<new()> (constructor)

Create a new instance of this class.

Parameters:
 can be run with no parameters; default host, db, user, password are set

  OR

 host		hostname
 db		database name
 user		username
 passwd		password
 dbh		database handle
 verbose 	level of verbosity

Returns:
 a new instance of this class.

=cut
sub new {
    my $class  = shift;
    my %params = @_;

    my $self = {
        host    => ( $params{host}    ? $params{host}    : undef ),
        db      => ( $params{db}      ? $params{db}      : undef ),
        user    => ( $params{user}    ? $params{user}    : undef ),
        passwd  => ( $params{passwd}  ? $params{passwd}  : undef ),
        dbh     => '',
        verbose => ( $params{verbose} ? $params{verbose} : 0 )
        };


    bless $self, $class;
}

################################################################################

=pod

B<host()>

Returns hostname of TrackLite database

Parameters:
 none

Returns:
 scalar: hostname

=cut
sub host {
    my $self = shift;
    return $self->{host} = shift if @_;
    return $self->{host};
}

################################################################################

=pod

B<db()>

Returns name of TrackLite database

Parameters:
 none

Returns:
 scalar: database name

=cut
sub db {
    my $self = shift;
    return $self->{db} = shift if @_;
    return $self->{db};
}

################################################################################

=pod

B<user()>

Returns username used to log in to TrackLite database

Parameters:
 none

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

B<passwd()>

Returns password used to log in to TrackLite database

Parameters:
 none

Returns:
 scalar: password

=cut
sub passwd {
    my $self = shift;
    return $self->{passwd} = shift if @_;
    return $self->{passwd};
}

################################################################################

=pod

B<dbh()>

Returns database handle

Parameters:
 none

Returns:
 scalar: database handle 

=cut
sub dbh {
    my $self = shift;
    return $self->{dbh};
}

################################################################################

=pod

B<verbose()>

Returns verbosity level

Parameters:
 none

Returns:
 scalar: integer defining level of verbose messages

=cut
sub verbose {
    my $self = shift;
    return $self->{verbose};
}

################################################################################

=pod

B<connect()>

Connects to TrackLite database

Parameters:
 none

Returns:
 scalar: database handle

=cut
sub connect {
    my $self = shift;
    
    my $connect_string = 'DBI:Pg:dbname=' . $self->db . ';host=' . $self->host;
    #my $connect_string = 'DBI:mysql:' . $self->db . ';host=' . $self->host;

    my $dbh = DBI->connect( $connect_string,
                            $self->user,
                            $self->passwd,
                            { RaiseError         => 1,
                              ShowErrorStatement => 1,
                              AutoCommit         => 0 } ) ||
        croak "can't connect to database server: $!\n";

    $dbh->{'ChopBlanks'}=1; # copes with padded spaces if type=char
    $self->{dbh} = $dbh;
    
    # Prepare key queries - not sure that this is actually worthwhile

    return $dbh;
}

################################################################################

=pod

B<by_run_sample_number()>

Queries TrackLite database by run sample number; returns field name and value
for all fields

Parameters:
 run sample number

Returns:
 scalar: reference to hash (key = field name; value = hash (keys: ctr, name,
         value; values: number of field, field name, value)

=cut
sub by_run_sample_number {
    my $self = shift @_;
    my $id   = shift @_;

	my $dbh = $self->{dbh};

    my $sql = 'SELECT * from all_details WHERE run_sample_number = ?;';
    $self->{run_sample_number_sth} = $dbh->prepare($sql);
	print STDERR "$sql\n";

    my $sth = $self->{run_sample_number_sth};
    my $rv = $sth->execute( $id ) or croak $sth->errstr; 

    my $ra_names   = $sth->{NAME};

    my $ra_rows = $sth->fetchall_arrayref;
    my @rows = @{ $ra_rows };

    # There should be one and only one row so let's check
    my $count = scalar(@rows);
    if ($count == 0) {
        croak "No rows found with id $id";
    }
    elsif ($count > 1) {
        croak "Too many rows found ($count) with id $id";
    }

    my %fields = ();
    my $ctr = 0;
    foreach my $field (@{ $ra_rows->[0] }) {
        $fields{ $ra_names->[$ctr] } = { ctr   => $ctr,
                                         name  => $ra_names->[$ctr],
                                         value => $field };
        $ctr++;
    }

    return \%fields;
}


################################################################################

=pod

B<libraries()>

Queries TrackLite for all mapsets with library information so that
mapsets that need to be merged at a library-level can be identified.
There is a variant method (B<libraries_icgc()>) which only returns
records for mapsets annotated as being in one of the ICGC projects:
ICGC-Pancreas or ICGC-Ovarian.

Parameters:
 none

Returns:
 scalar: query results as a reference to an array of rows (arrays).

Each row returned has the following fields:

 0  run_id
 1  mapset_name
 2  sample_set
 3  project_id
 4  library_type
 5  pooled_library_id
 6  primary_library_id
 7  run_type
 8  input_type
 9  experiment_type

=cut
sub libraries {
    my $self = shift;

	my $dbh = $self->{dbh};

	my $sql = qq{
        SELECT run_sample_number AS run_id,
               concat_ws(., run_name,individual_sample_location,barcode) AS mapset_name,
               sample_set,
               project_id,
               library_type,
               pooled_library_id,
               primary_library_id,
               run_type,
               input_type,
               experiment_type
        FROM tracklite_run
        WHERE run_status != 'ABORTED'
        ORDER BY run_id
		};

	print STDERR "$sql\n";
    $self->{libraries} = $dbh->prepare($sql);

    my $sth = $self->{libraries};
    my $rv = $sth->execute() or croak $sth->errstr; 
    my $ra_rows = $sth->fetchall_arrayref;

    return $ra_rows;
}


sub libraries_icgc {
    my $self = shift;

	my $dbh = $self->{dbh};

	my $sql = qq{
        SELECT run_sample_number AS run_id,
               concat(run_name,'.',individual_sample_location,'.',barcode) AS mapset_name,
               sample_set,
               project_id,
               library_type,
               pooled_library_id,
               primary_library_id,
               run_type,
               input_type,
               experiment_type
        FROM tracklite_run
        WHERE run_status != 'ABORTED'
        AND (project_id = 'ICGC-Pancreas' OR project_id = 'ICGC-Ovarian')
        ORDER BY run_id
		};

	print STDERR "$sql\n";
    $self->{libraries_icgc} = $dbh->prepare($sql);

    my $sth = $self->{libraries_icgc};
    my $rv = $sth->execute() or croak $sth->errstr; 
    my $ra_rows = $sth->fetchall_arrayref;

    return $ra_rows;
}


################################################################################

################################################################################

=pod

B<mapped_mapsets()>

Queries TrackLite for all mapsets that are annotated as being mapped.

Parameters:
 none

Returns:
 scalar: query results as a reference to an array.

=cut
sub mapped_mapsets {
    my $self = shift;

	my $dbh = $self->{dbh};

	my $sql = qq{
        SELECT run_sample_number AS run_id,
               concat_ws('.', run_name,IF(individual_sample_location is NULL, '?', individual_sample_location),IF(barcode is NULL, '?', barcode)) AS mapset_name,
               sample_set,
               project_id,
               mapped_by,
               mapped_on,
               library_type,
               barcode_type,
               run_type
        FROM tracklite_run
        WHERE mapping_status='Complete' and
              run_status != 'ABORTED'
		};

	print STDERR "$sql\n";
    $self->{mapped_mapsets} = $dbh->prepare($sql);

    my $sth = $self->{mapped_mapsets};
    my $rv = $sth->execute() or croak $sth->errstr; 
    my $ra_rows = $sth->fetchall_arrayref;

    return $ra_rows;
}


################################################################################

=pod

B<mapsets_from_slide_name()>

Queries TrackLite for all mapsets that are from a given slide.

Parameters:
 scalar: slide name

Returns:
 scalar: query results as a reference to an array.

=cut
sub mapsets_from_slide_name {
    my $self  = shift;
    my $slide = shift;

	my $dbh = $self->{dbh};

# SELECT array_to_string(ARRAY(SELECT field_lambda FROM table_lambda GROUP BY id), ';');

	my $sql = qq{
        SELECT run_sample_number AS run_id,
               run_name,individual_sample_location,barcode,
               sample_set,
               project_id,
               run_type,
		primary_library_id
        FROM tracklite_run
        WHERE run_name = ?
		};

=pod
$VAR1 = [
          [
            2038,
            'S0417_20120403_1_LMP',
            'nopd',
            'nobc',
            'AOCS-005',
            'ICGC-Ovarian',
            'LMP',
            'Library_20100326_B'
          ]
        ];
=cut

	print STDERR "$sql\n";
    $self->{mapsets_from_slide_name} = $dbh->prepare($sql);

    my $sth = $self->{mapsets_from_slide_name};
    my $rv = $sth->execute( $slide ) or croak $sth->errstr; 
    my $ra_rows = $sth->fetchall_arrayref;

	#print Dumper $ra_rows;

	my $new_rows;
	push @{$new_rows->[0]}, shift @{$ra_rows->[0]};
	my $mapset = join ".", shift @{$ra_rows->[0]}, shift @{$ra_rows->[0]}, shift @{$ra_rows->[0]};
	push @{$new_rows->[0]}, $mapset;
	push @{$new_rows->[0]}, @{$ra_rows->[0]};
    #return $ra_rows;
	#print Dumper $new_rows;
	return $new_rows;
}


################################################################################

=pod

B<retrieve_filedetails()>

Queries TrackLite database by run name; returns metadata from *_run_definition.txt
file

Parameters:
 run name

Returns:
 scalar: query results as a reference to a hash.

=cut
sub retrieve_filedetails {
    my $self		= shift @_;
    my $runname		= shift @_;

	my $dbh = $self->{dbh};

	my $sql = qq{
		SELECT
			project_id, sample_set, experiment_type,
			input_type, run_type, run_name, results_folder_sample_name, 
			barcode_type, barcode, 
			CONCAT_WS('_', run_name, results_folder_sample_name, barcode) file 
            	FROM
			tracklite_run t
            	WHERE	
			run_name = ? 
            	OR
			CONCAT_WS('_', run_name, barcode) = ?
            	OR
			CONCAT_WS('_', run_name, results_folder_sample_name, barcode) = ? 
		};

	print STDERR "$sql\n";
	#print STDERR "$runname: $sql\n";

    $self->{retrieve_filedetails} = $dbh->prepare($sql);

    my $sth = $self->{retrieve_filedetails};
    my $rv = $sth->execute( $runname, $runname, $runname ) or croak $sth->errstr; 

    my $ra_names   = $sth->{NAME};

    my $ra_rows = $sth->fetchall_arrayref;
    my @rows = @{ $ra_rows };

    # There should be one and only one row so let's check
    my $count = scalar(@rows);
    if ($count == 0) {
        croak "No rows found with run name $runname";
    }
    elsif ($count > 1) {
        croak "Too many rows found ($count) with run name $runname";
    }

    my %fields = ();
    my $ctr = 0;
    foreach my $field (@{ $ra_rows->[0] }) {
        $fields{ $ra_names->[$ctr] } = { ctr   => $ctr,
                                         name  => $ra_names->[$ctr],
                                         value => $field };

		print STDERR "$ra_names->[$ctr] => $field\n";
        $ctr++;
    }

    return \%fields;
}

################################################################################

=pod

B<update_ingest_solid()>

Updates a field in tracklite_run using the run name

Parameters:
 TAG_LENGTH	- hash of tags and their lengths (f3, f5, r3)
 CSFASTA	- hash of tags and their csfasta files
 PRIMARY	- hash of tags and their primary directory path
 RUN_NAME	- run name*
 RUN_TYPE	- run type (FRAGMENT, PAIRED-END, MATE-PAIR)
 BARCODE 	- barcode (or 'none')*

  * used to identify database record to update; if barcode is 'none' it is not
  * used as a field for matching

Returns:

=cut
sub update_ingest_solid {
	my $self = shift @_;

	#print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

	# parse params
	# QUERY - query statement
	my $options = {};

	# read params from @_ and put in %options
	for(my $i=0; $i<=$#_; $i+=2) {
		my $this_sub = (caller(0))[0]."::".(caller(0))[3];
		defined($_[($i + 1)]) || 
			die "Odd number of params: $this_sub : $!";
		$options->{uc($_[$i])} = $_[($i + 1)];
	}

	my %field;	# hash of tracklite_run field names; keys = values

	# f3_tag_length   50
	# f5_tag_length   0
	# r3_tag_length   5
	foreach my $t (keys %{$options->{'TAG_LENGTH'}}) {
		my $f = $t;

		$f =~ s/bc/r3/;

		$field{$f."_tag_length"} = $options->{'TAG_LENGTH'}->{$t};
	}

	# f3_csfasta      S0414_20100601_1_FragBC_bcSample1_F3_bcA10_01.csfasta
	# f5_csfasta
	# r3_csfasta
	foreach my $t (keys %{$options->{'CSFASTA'}}) {
		my $f = $t;
		$f =~ s/\_reads//;
		$field{$f."_csfasta"} = qq{"$options->{'CSFASTA'}->{$t}"};#"
	}

	# f3_reads        primary.20100606194838899
	# f5_reads        na
	# r3_reads        primary.20100602061409632

	# f3_fullpath     bcSample1/results.F1B1/libraries/bcA10_01/primary.20100606194838899/reads
	# f5_fullpath
	# r3_fullpath
	foreach my $t (keys %{$options->{'PRIMARY'}}) {
		my $f = $t;
		$f =~ s/\_reads//;

		$options->{'PRIMARY'}->{$t} =~ s/\/reads$//;
 		my ($v,$d,$file) = File::Spec->splitpath($options->{'PRIMARY'}->{$t});
		$field{$t} = qq{"$file"};

		$options->{'PRIMARY'}->{$t} =~ s/(.+?$options->{'RUN_NAME'}\/)(.+)/$2/;
		$field{$f."_fullpath"} = qq{"$options->{'PRIMARY'}->{$t}"};#"

	}

	# translate Bioscope run type into Tracklite run type; if type is not
	# FRAGMENT, PAIRED-END, MATE-PAIR leave it as is
	#
	# run_type        Frag
	if(   $options->{'RUN_TYPE'} =~ /frag/i) {
		$field{'run_type'}	= qq{"Frag"};
	}
	elsif($options->{'RUN_TYPE'} =~ /paired/i) {
		$field{'run_type'}	= qq{"PE"};
	}
	elsif($options->{'RUN_TYPE'} =~ /mate/i) {
		$field{'run_type'}	= qq{"LMP"};
	}
	else {
		$field{'run_type'}	= qq{"$options->{'RUN_TYPE'}"};#"
	}

	# create empty barcode for field matching if no barcode present
	#
	# barcode 1
	# barcode_type    SeriesA
	#
	# barcode
	# barcode_type    na
	$options->{'BARCODE'}	= '' if($options->{'BARCODE'} eq 'none');

	### build SQL statement
	my $sql = qq{UPDATE tracklite_run SET };
	foreach my $k (keys %field) {
		$sql		.= qq{ $k = $field{$k}, };
	}
	$sql			 =~ s/,\s+$//;
	$sql 			.= qq{ WHERE run_name = "$options->{'RUN_NAME'}" };#"
	$sql			.= qq{ AND barcode = "$options->{'BARCODE'}"};#"

	print STDERR "$sql\n" if($self->{verbose} > 0);
	###

	my $dbh = $self->{dbh};
	my $sth = $dbh->prepare($sql);
	my $rv  = $sth->execute or croak $sth->errstr; 

	# return number of rows updated
	return($rv);

}

################################################################################

=pod

B<query_db()> 

Submit an SELECT-type SQL query to the database. This is a more generic function
to allow non-prefab queries to be made.

Parameters:
 query => sql statement that returns rows

Returns:
 scalar: query results as a reference to a hash.

=cut
sub query_db {
	my $self = shift @_;

	my ($sth, $error, @result);

	#print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

	# parse params
	# QUERY - query statement
	my $options = {};

	# read params from @_ and put in %options
	for(my $i=0; $i<=$#_; $i+=2) {
		my $this_sub = (caller(0))[0]."::".(caller(0))[3];
		defined($_[($i + 1)]) || 
			die "Odd number of params: $this_sub : $!";
		$options->{uc($_[$i])} = $_[($i + 1)];
	}

	my $sql = $options->{'QUERY'};

	my $dbh = $self->{dbh};

	$sql = $options->{'QUERY'};

	print STDERR "$sql\n" if($self->{verbose} > 0);

	$sth = $dbh->prepare($options->{'QUERY'});
	$sth->execute || print STDERR "$sql\n";

	# generate ref to hash (get col names)
	@result = ();
	while(my $row = $sth->fetchrow_hashref) {
		push @result, $row;
	}

	$sth->finish;

	return \@result;
}


sub DESTROY {
   my $self = shift;
   $self->dbh->rollback;  # good practice when AutoCommit is off
   $self->dbh->disconnect;
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
