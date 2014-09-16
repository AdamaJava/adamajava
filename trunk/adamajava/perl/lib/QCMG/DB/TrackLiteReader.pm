package QCMG::DB::TrackLiteReader;

###############################################################################
#
#  Module:   QCMG::DB::TrackLiteReader.pm
#  Creator:  Matthe Anderson
#  Created:  2012-02-24
#
#  This class allows retrival of metadata from the MySQL database Tracklite. 
#
#  $Id
#
###############################################################################

=pod

=head1 NAME

QCMG::DB::TrackLiteReader -- Basic read-only functions for retiveing metedata 
                             from the TrackLite database.

=head1 SYNOPSIS

Most common use:
 my $tl = QCMG::DB::TrackLiteReader->new();

Retrieving mapset, library and finals.

=head1 DESCRIPTION

This class is primarily created for the QCMG::DB::Metadata and 
QCMG::DB::MetadataReader modules to create a Metadata data Object. 
It contains methods for connecting to the TrackLite database and querying the 
database for metedata on mapsets, libraries and finals.

For a more comprehensive access to TrackLite use QCMG::DB::TrackLite

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

sub new {
    my $class   = shift;
    my $debug   = shift;
    
    my $db_settings = {
        host    => undef
        db      => undef
        user    => undef
        passwd  => undef
    };
    
    my $self = {
        tracklite_conn     => '',
        tracklite_cursor   => '',
        debug           => ( $debug ?  1 :  0  )
        #verbose         => ( $params{verbose} ? $params{verbose} : 0 )
    };    
    my $connect_string = 'DBI:Pg:dbname='.$db_settings->{db}.';host='.$db_settings->{host};
    $self->{tracklite_conn} = DBI->connect( $connect_string,
                                        $db_settings->{user},
                                        $db_settings->{passwd},
                                        { RaiseError         => 1,
                                          ShowErrorStatement => 1,
                                          AutoCommit         => 0 } ) ||
        croak "can't connect to database server: $!\n";
    $self->{tracklite_conn}->{'ChopBlanks'}=1; # copes with padded spaces if type=char
    
    bless $self, $class;
    
}

sub DESTROY {
   my $self = shift;
   
   if ( $self->{tracklite_conn} ) {
       $self->{tracklite_conn}->disconnect();   
   }
}



sub _debug {
    my $self = shift;
    my $message = shift;
    
    if ($self->{debug}) {
       print "$message\n";
    }            
}

sub _metadata_of_all_resources{
    my $self = shift; 
    
    my $query_stmt = qq{
        SELECT 
        	tracklite_run.project_id  AS "Parent Project", 
        	replace(replace(tracklite_run.sample_set, \'-\', \'_\'), \'.\', \'_\') AS "Patient ID", 
        	tracklite_run.experiment_type AS "Experiment Type", 
        	tracklite_run.input_type AS "Source AND Material Type",
        	replace( replace(tracklite_run.sample_set, \'-\', \'_\'), \'.\', \'_\' ) || \'.\' || tracklite_run.experiment_type || \'.\' || tracklite_run.input_type AS "Final",
        	tracklite_run.primary_library_id AS "Library",   
        	tracklite_run.run_name ||\'.\'|| tracklite_run.individual_sample_location ||\'.\'|| tracklite_run.barcode AS "Mapset"
        FROM public.tracklite_run 
        WHERE tracklite_run.run_status != \'ABORTED\' AND tracklite_run.sample_set IS NOT NULL
    };
    
    $self->{tracklite_cursor} = $self->{tracklite_conn}->prepare($query_stmt);
    return $self->{tracklite_cursor}->execute();
}

sub _final_of_mapset{
    my $self        = shift; 
    my $resource    = shift;
    
    my $query_stmt = qq{ 
        SELECT DISTINCT 
            replace( replace( tracklite_run.sample_set, \'-\', \'_\' ), \'.\', \'_\' ) || \'.\' || tracklite_run.experiment_type || \'.\' || tracklite_run.input_type AS "Final"
        FROM public.tracklite_run
        WHERE  tracklite_run.run_name ||\'.\'|| tracklite_run.individual_sample_location ||\'.\'|| tracklite_run.barcode =  ?
    };
    
    $self->{tracklite_cursor} = $self->{tracklite_conn}->prepare($query_stmt);
    $self->{tracklite_cursor}->bind_param(1, $resource);
    $self->{tracklite_cursor}->execute();
    my @final = $self->{tracklite_cursor}->fetchrow_array();
    if ( @final ){
         $self->{tracklite_cursor}->finish();
        return $final[0];
    }else{
        return 0;
    }
}

sub _final_of_library{
    my $self        = shift; 
    my $resource    = shift;
    
    my $query_stmt = qq{ 
        SELECT DISTINCT 
            replace( replace( tracklite_run.sample_set, \'-\', \'_\' ), \'.\', \'_\' ) || \'.\' || tracklite_run.experiment_type || \'.\' || tracklite_run.input_type AS "Final"
        FROM public.tracklite_run
        WHERE tracklite_run.primary_library_id = ?
    };
    
    $self->{tracklite_cursor} = $self->{tracklite_conn}->prepare($query_stmt);
    $self->{tracklite_cursor}->bind_param(1, $resource);
    $self->{tracklite_cursor}->execute();
    my @final = $self->{tracklite_cursor}->fetchrow_array();
    if ( @final ){
        $self->{tracklite_cursor}->finish();
        return $final[0];
    }else{
        return 0;
    }
}

sub _metadata_of_final {
    my $self        = shift; 
    my $resource    = shift;
    
    my $query_stmt = qq{ 
        SELECT
            tracklite_run.project_id AS "Parent Project",
            replace(replace(tracklite_run.sample_set, \'-\', \'_\'), \'.\', \'_\') AS "Patient ID",
            tracklite_run.experiment_type AS "Experiment Type",
            tracklite_run.input_type AS "Source AND Material Type",
            replace( replace(tracklite_run.sample_set, \'-\', \'_\'), \'.\', \'_\' ) || \'.\' || tracklite_run.experiment_type || \'.\' || tracklite_run.input_type AS "Final",
            tracklite_run.primary_library_id AS "Primary Library",   
            tracklite_run.run_name ||\'.\'|| tracklite_run.individual_sample_location ||\'.\'|| tracklite_run.barcode AS "Mapset"
        FROM tracklite_run 
        WHERE replace( replace(tracklite_run.sample_set, \'-\', \'_\'), \'.\', \'_\' ) || \'.\' || tracklite_run.experiment_type || \'.\' || tracklite_run.input_type = ?
        AND tracklite_run.run_status != \'ABORTED\'
    };
    $self->{tracklite_cursor} = $self->{tracklite_conn}->prepare($query_stmt);
    $self->{tracklite_cursor}->bind_param(1, $resource);
    return $self->{tracklite_cursor}->execute();
}


sub _metadata_of_donor {
    my $self        = shift; 
    my $resource    = shift;
    
    my $query_stmt = qq{ 
        SELECT
            tracklite_run.project_id  AS "Parent Project", 
    	    replace(replace(tracklite_run.sample_set, \'-\', \'_\'), \'.\', \'_\') AS "Patient ID", 
    	    tracklite_run.experiment_type AS "Experiment Type", 
    	    tracklite_run.input_type AS "Source AND Material Type",
    	    replace( replace(tracklite_run.sample_set, \'-\', \'_\'), \'.\', \'_\' ) || \'.\' || tracklite_run.experiment_type || \'.\' || tracklite_run.input_type AS "Final",
    	    tracklite_run.primary_library_id AS "Library",   
    	    tracklite_run.run_name ||\'.\'|| tracklite_run.individual_sample_location ||\'.\'|| tracklite_run.barcode AS "Mapset"
        FROM public.tracklite_run 
        WHERE replace( replace( tracklite_run.sample_set, \'-\', \'_\'), \'.\', \'_\' ) = ?
        AND tracklite_run.run_status != \'ABORTED\'
    };
    $self->{tracklite_cursor} = $self->{tracklite_conn}->prepare($query_stmt);
    $self->{tracklite_cursor}->bind_param(1, $resource);
    return $self->{tracklite_cursor}->execute();
}



sub fetch_metadata {
    my $self = shift; 
    
    #my ($parent, $patient, $experiment, $source, $material, $final, $library, $mapset);
    #$self->{tracklite_cursor}->bind_columns(\$parent, \$patient, \$experiment, \$source, \$material, \$final, \$library, \$mapset);
    my ($parent, $patient, $experiment, $source_AND_material, $final, $library, $mapset);
    $self->{tracklite_cursor}->bind_columns(\$parent, \$patient, \$experiment, \$source_AND_material, \$final, \$library, \$mapset);
    

    my @metadata = ();
    while ( $self->{tracklite_cursor}->fetchrow_arrayref() ) {
        my ($source, $material) = $self->_split_input_type($source_AND_material);
        if ($parent eq "ICGC-Pancreas") {
            $parent = "ICGC-Pancreatic";
        }
        $parent = lc ($parent);
        $parent =~ s/-/_/g;
        
        my $mapset_details = {
            'Parent Project'    => $parent,
            'Patient ID'        => $patient,
            'Experiment Type'   => $experiment,
            'Source Type'       => $source,
            'Material Type'     => $material,
            'Final'             => $final,
            'Primary Library'   => $library,
            'Mapset'            => $mapset
        };
        push(@metadata, $mapset_details);
    }    
    return \@metadata;
}

sub all_resources_metadata{
    my $self = shift;
    if ( $self->_metadata_of_all_resources() ){
        return 1; # True
    }else{
        return 0;
        # croak $sth->errstr;
    }
}

sub resource_metadata{
    my $self            = shift;
    my $resource_type   = shift;
    my $resource        = shift;
    
    if ( $resource_type eq "donor") {
        $self->_metadata_of_donor($resource);
        $self->_debug("Donor");
        
    }elsif ( $resource_type eq "final") {
        $self->_metadata_of_final($resource);
        $self->_debug("Final");
        
    }elsif ($resource_type eq "library") {
        my $final = $self->_final_of_library($resource);
        if ($final){
            $self->_debug($final);
            $self->_metadata_of_final($final);
        }else{
            return 0;
        }
        $self->_debug("Library")
        
    }elsif ($resource_type eq "mapset") {
        my $final = $self->_final_of_mapset($resource);
        if ($final){
            $self->_debug($final);
            $self->_metadata_of_final($final);
        }else{
            return 0;
        }
        $self->_debug("Mapset");
        
    }else{
        $self->_debug("Nothing");
        return 0;
    }
    return 1; # True
}



#
# Make Source type and Material from input type
sub _split_input_type {
    my $self = shift;
    my $input_type = shift;
    
    my $source = '?';
    my $material = '?';
    
    if ( $input_type and length($input_type) == 2 ) {
        if ( $input_type =~ /^.D$/ ){ 
            $material = "DNA (D)";
        }elsif( $input_type =~ /^.R$/ ) {
            $material = "RNA (R)";
        }
        #else{
        #    croak "Unknown material type from $input_type\n";
        #}
        
        if ( $input_type =~ /^A./ ) {
            $source = "Adjacent Normal (A)";
        }elsif ( $input_type =~ /^C./ ) {
            $source = "Cell Line (C)";
        }elsif ( $input_type =~ /^M./ ) {
            $source = "Metastasis (M)";
        }elsif ( $input_type =~ /^N./ ) {
            $source = "Normal (N)";
        }elsif ( $input_type =~ /^T./ ) {
            $source = "Tumour (T)";
        }elsif ( $input_type =~ /^X./ ) {
            $source = "Xenograft (X)";
        }
        #else{
        #    croak "Unknown material type from $input_type\n";
        #}
    }
    
    return ($source, $material); 
}



# Pre prepare statments for execution
#sub prepare_statements {
#   my $self = shift;
#   my $queries = {mapset_final => '', library_final => '', metadata => ''};
#   
#   $queries->{mapset_final} = '
#       
#       SELECT DISTINCT CONCAT_WS(\'.\', tracklite_run.sample_set, tracklite_run.experiment_type, tracklite_run.input_type) AS "Final"
#       FROM tracklite_run 
#       WHERE CONCAT_WS('.', tracklite_run.run_name, tracklite_run.individual_sample_location, tracklite_run.barcode) =  ?
#   ';
#   
#   $queries->{library_final} = '
#       SELECT DISTINCT CONCAT_WS(\'.\', tracklite_run.sample_set, tracklite_run.experiment_type, tracklite_run.input_type) AS "Final"
#       FROM tracklite_run 
#       WHERE primary_library_id = ?
#   ';
#   
#   $queries->{metadata} = '
#       SELECT
#       tracklite_run.project_id AS "Project ID",
#       tracklite_run.sample_set AS "Donor",
#       tracklite_run.experiment_type AS "Experiment Type",
#       tracklite_run.input_type AS "Source AND Material Type",
#       CONCAT_WS('.', tracklite_run.sample_set, tracklite_run.experiment_type, tracklite_run.input_type) AS "Final",
#       tracklite_run.primary_library_id AS "Library",   
#       CONCAT_WS('.', tracklite_run.run_name, tracklite_run.individual_sample_location, tracklite_run.barcode) AS "Mapset"
#       FROM tracklite_run 
#       WHERE CONCAT_WS('.', tracklite_run.sample_set, tracklite_run.experiment_type, tracklite_run.input_type) = ?
#       AND tracklite_run.run_status != "ABORTED"
#   ';
#
#   $self->{sth}
#   $sth = $self->{dbh}->prepare_cached($statement);
#   
#
#}




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
