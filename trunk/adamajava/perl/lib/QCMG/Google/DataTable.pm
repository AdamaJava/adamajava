package QCMG::Google::DataTable;

###########################################################################
#
#  Module:   QCMG::Google::DataTable.pm
#  Creator:  John V Pearson
#  Created:  2011-05-29
#
#  Class to represent the google.visualization.DataTable() data
#  container used by most core Google Charts.  See also:
#  http://code.google.com/apis/chart/interactive/docs/reference.html#DataTable
#  http://code.google.com/apis/chart/interactive/docs/reference.html#dataparam
#
#  $Id$
#
###########################################################################

use strict;
use warnings;

use Carp qw( carp croak );
use Data::Dumper;
use POSIX qw( floor ceil );
use QCMG::Util::QLog;
use vars qw( $VERSION );

( $VERSION ) = '$Revision$ ' =~ /\$Revision:\s+([^\s]+)/;


###########################################################################
#                          PUBLIC METHODS                                 #
###########################################################################


sub new {
    my $class  = shift;
    my %params = @_;

    croak "new() requires a name parameter"
        unless (exists $params{name} and defined $params{name});
    croak "name parameter supplied to new() must not contain whitespace"
        if ($params{name} =~ /\s/);

    my $self = { name        => $params{name},
                 verbose     => ($params{verbose} || 0),
                 cols        => [],
                 rows        => [],
                 acts        => '',
                 formats     => [],
                 version     => '0.6',
                 svn_version => $VERSION };

    bless $self, $class;
}


sub name {
    my $self = shift;
    return $self->{name} = shift if @_;
    return $self->{name};
}

sub verbose {
    my $self = shift;
    return $self->{verbose} = shift if @_;
    return $self->{verbose};
}


sub cols {
    my $self = shift;
    return @{ $self->{cols} };
}

sub col_count {
    my $self = shift;
    return scalar( @{ $self->{cols} } );
}

sub rows {
    my $self = shift;
    return @{ $self->{rows} };
}

sub row_count {
    my $self = shift;
    return scalar( @{ $self->{rows} } );
}

sub version {
    my $self = shift;
    return $self->{version};
}

sub svn_version {
    my $self = shift;
    return $self->{version};
}

sub javascript_name {
    my $self = shift;
    return $self->name . '_Data';
}


# This is not working yet - it tries to make the data tables using
# calls to the google javascript functions.
sub as_javascript_function_calls {
    my $self = shift;
    my $mode = (@_ ? shift : 1);

    my $text = '';
    if ($mode == 1) {
        $text .= "\nvar ". $self->javascript_name .
                 " = new google.visualization.DataTable()\n";

        # Add columns
        foreach my $col ($self->cols) {
            $text .= $self->javascript_name .
                     ".addColumn('$col->[2]', '$col->[1]\');\n";
        }

        # Add rows
        my @rows_as_text = ();
        my $last_col = $self->col_count-1;
        foreach my $row ($self->rows) {
            my @fields_as_text = ();
            # We need to output each field in the correct format based on
            # the corresponding column type
            foreach my $col (0..$last_col) {
                my $val = $row->[$col];
                # Pull out type of this column
                my $col_type = $self->{cols}->[$col]->[2];
                print Dumper $val, $col_type;
                if ($col_type =~ /number/i) {
                    # Check for a format
                    if (exists $self->{formats}->[$col]) {
                        push @fields_as_text, 
                             sprintf($self->{formats}->[$col],$val);
                    }
                    else {
                        push @fields_as_text, $val;
                    }
                }
                else {
                    push @fields_as_text, "'$val'";
                }
            }
            print Dumper, \@fields_as_text;
            my $this_row = '    [' . join(', ',@fields_as_text) . "]";
            push @rows_as_text, $this_row;
        }
        # Build the addRows([ ... ]) call
        $text .= $self->javascript_name . ".addRows([\n" .
                 join( @rows_as_text, ",\n" ) .
                 "\n]);\n\n";
    }
    else {
        # Nothing as of yet
    }

    return $text;
}


sub as_javascript_literal {
    my $self = shift;
    my $mode = (@_ ? shift : 1);

    my $text = '';
    
    # Data Table modes
    if ($mode >= 1 and $mode <= 2 ) {
        # Add columns
        $text .= "{\ncols: [";
        my @cols_as_text = 
            map{ "{id:\'$_->[0]\', label:\'$_->[1]\', type:\'$_->[2]\'}" }
            $self->cols;
        $text .= join(', ',@cols_as_text) . "],\n";

        # Add rows
        $text .= 'rows: [';
        my @rows_as_text = ();
        my $last_col = $self->col_count-1;
        foreach my $row ($self->rows) {
            my $row_text = '{c:[';
            my @fields_as_text = ();
            # We need to output each field in the correct format based on
            # the corresponding column type
            foreach my $col (0..$last_col) {
                my $val = $row->[$col];
                # Pull out type of this column
                my $col_type = $self->{cols}->[$col]->[2];
                if ($col_type =~ /number/i) {
                    # Check for a format
                    if (exists $self->{formats}->[$col]) {
                        push @fields_as_text, 
                             '{v:'. sprintf($self->{formats}->[$col],$val) .'}';
                    }
                    else {
                        push @fields_as_text, "{v:$val}";
                    }
                }
                else {
                    push @fields_as_text, "{v:'$val'}";
                }
            }
            $row_text .= join(', ',@fields_as_text);
            $row_text .= ']}';
            push @rows_as_text, $row_text;
        }
        $text .= join(",\n",@rows_as_text);
        $text .= "]\n} ";
    }
    
    # Data Table modes
    if ($mode == 1) {
        $text = sprintf ("\nvar %s = new google.visualization.DataTable(\n%s, %s );\n",
                         $self->javascript_name, 
                         $text, 
                         $self->version
                        );
    }else {
        # Nothing as of yet
    }
    

    return $text;
}

# Useful to turn into a JSON object!
sub as_perl_object {
    my $self = shift;
    my $mode = (@_ ? shift : 1);

    my $object = { cols => [], rows => [] };
    
    # Data Table modes
    if ($mode == 1) {
        
        # Add columns
        foreach my $col ( $self->cols() ){
            push ( @{$object->{cols}} , { id => $col->[0], label => $col->[1], type => $col->[2]} ); 
        }
        
        # Add rows
        my $last_col = $self->col_count-1;
        foreach my $row ( $self->rows() ) {   
            my $column_values = [];
            foreach my $col (0..$last_col) {
                # Pull out type of this column
                my $col_type = $self->{cols}->[$col]->[2];
                my $val = $row->[$col];
                if ($col_type =~ /number/i) {
                    # Check for a format
                    if (exists $self->{formats}->[$col]) {
                        $val = sprintf($self->{formats}->[$col], $val);
                    }
                }
                # Collect row column values.
                push ( @{$column_values}, { v => $val } );
            }
            push ( @{$object->{rows}} , { c => $column_values }  );           
        }
    }else {
        # Nothing as of yet
    }
    
    return $object;
}


sub add_col {
    my $self  = shift;
    my $id    = shift;
    my $label = shift;
    my $type  = shift;

    push @{ $self->{cols} }, [ $id, $label, $type ];
}


sub add_row {
    my $self = shift;
    my @vals = @_;

    if (scalar(@vals) != $self->col_count) {
        carp 'There are ', scalar( @vals ), ' values in the call to add_row() ',
             ' but there are ', $self->col_count, ' columns in the DataTable ',
             ' - these values must match';
        return undef;
    }

    push @{ $self->{rows} }, \@vals;
}


sub add_format {
    my $self   = shift;
    my $column = shift;
    my $format = shift;

    if ($column > $self->col_count) {
        carp "Invalid column [$column] in add_format() - there are only ".
             $self->col_count. ' columns in the DataTable';
        return undef;
    }

    $self->{formats}->[$column] = $format;
}


sub trim {
    my $self = shift;
    my $val  = shift;
    my $idx  = shift || 1;
    my $mode = shift || 'cumul_max';  # cumul_max, trailing_zeros
    my $acts = shift || '';

    #print "My inputs: ",join(',',$val,$idx,$acts),"\n";

    if ($self->row_count == 0) {
        carp 'Cannot trim ' . $self->name . " because it has no data";
        return undef;
    }

    # Trimming only makes sense for non-empty 2+ column tables where the
    # first column is a text label and the second is a number
    croak 'You can only trim a DataTable with 2 or more columns'
        unless ($self->col_count >= 2);
    croak "You can only trim a DataTable if column [$idx] is a number"
        unless ($self->{cols}->[$idx]->[2] =~ /^number$/i);

    # Column ($acts) actions
    # _  do nothing, empty field
    # +  add all values (only relevant for numerics)
    # .  take the value of the first trimmed record

    # If $acts string not supplied then build one based on types of
    # columns (field 2 from each col record).
    if (! $acts) {
        $acts = '';
        foreach my $col ($self->cols) {
            $acts .= ($col->[2] =~ /number/i) ? '+' : '_';
        }
    }
    $self->{acts} = $acts;

    # Make sure the number of actions matches the number of columns
    my @actions = split //, $acts;
    croak "Incorrect number of characters in column actions [$acts],".
          ' should be '. $self->col_count
          unless ($self->col_count == scalar(@actions));

    my @new_rows = ();

    # Default mode, trim table based on max cumulatoive value in a column
    if ($mode =~ /cumul_max/i) {

        qlogprint( 'Trimming ' . $self->name . " to $val\n" ) if $self->verbose;

        my $total = 0;
        my $keepctr = 0;
        my @rows = $self->rows;
        while ($total < $val) {
           my $row = shift @rows;
           $total += $row->[$idx];
           push @new_rows, $row;
           $keepctr++;
        }

        my $dropctr = $self->row_count - $keepctr;
        qlogprint( "  keeping $keepctr lines and trimming $dropctr\n" )
            if $self->verbose;

        # If there are any rows left, then start construction of "the rest" 
        # record based on the first of the remaining rows and the column actions

        if (@rows) {
            my @final_row = @{ shift @rows };
            foreach my $ctr (0..($self->col_count-1)) {
                # + and . are already fine but for _ we need to blank out the
                # value and for 1, we set the value to 1
                if ($actions[$ctr] eq '_') { 
                    $final_row[$ctr] = '';
                }
                elsif ($actions[$ctr] eq '1') { 
                    $final_row[$ctr] = '1';
                }
            }
            $final_row[0] = '>'.$new_rows[$#new_rows]->[0];

            # Now process all remaining rows, again obeying column actions
            while (my $row = shift @rows) {
                foreach my $ctr (0..($self->col_count-1)) {
                    if ($actions[$ctr] eq '+') {
                        $final_row[$ctr] += $row->[$ctr];
                    }
                }
            }

            # Save our hard-won final record
            push @new_rows, \@final_row;
        }
        else {
            qlogprint( 'Trimming '. $self->name ." to $val removes 0 rows\n" );
        }
    }
    # Non-default mode, trim table of all trailing rows with 0 in
    elsif ($mode =~ /trailing_zeros/i) {
        qlogprint( 'Trimming ' . $self->name . " of trailing 0 rows\n" )
            if $self->verbose;

        my $dropctr = 0;
        my @rows = reverse $self->rows;
        while ($rows[0]->[$idx] == 0) {
           shift @rows;
           $dropctr++;
        }

        # Anything left must be the first non-0 row and any rows before it
        @new_rows = reverse @rows;

        my $keepctr = $self->row_count - $dropctr;
        qlogprint( "  keeping $keepctr lines and trimming $dropctr\n" )
            if $self->verbose;
    }
    else {
        croak "mode [$mode] in trim() is not understood";
    }

    # Put new rows back into data structure
    $self->{rows} = \@new_rows;
}


sub bin {
    my $self   = shift;
    my $value  = shift || 50;
    my $method = shift || 'bin_count';  # or 'member_count'
    my $acts   = shift || '';
    
    if ($self->row_count == 0) {
        carp 'Cannot bin ' . $self->name . " because it has no data";
        return undef;
    }

    # Column actions
    # r separate first and last value strings with a '-' to create a range
    # + add all values (only relevant for numerics)
    # 1 take the value of the first element
    # n take the value of the last element
    # < take the value of the smallest element
    # > take the value of the largest element

    # If $acts string not supplied then build one based on types of
    # columns (field 2 from each col record).
    if (! $acts) {
        $acts = '';
        foreach my $ctr (0..($self->col_count-1)) {
            my $col_def = $self->{cols}->[$ctr];
            if ($ctr == 0) {
               # default is to do range making on first column
               $acts .= 'r';
            }
            else {
               # all other numeric fields get added and others get blanked
               $acts .= ($col_def->[2] =~ /number/i) ? '+' : '1';
            }
        }
    }
    $self->{acts} = $acts;

    # make sure the number of actions matches the number of columns
    my @actions = split //, $acts;
    croak "Incorrect number of characters in column actions [$acts],".
          ' should be '. $self->col_count
          unless ($self->col_count == scalar(@actions));

    qlogprint( 'Binning '. $self->name ." as $method:$value\n" )
        if $self->verbose;

    # Need to work out how many rows go into a bin
    my $bin = 0;
    if ($method =~ /bin_count/i) {
        $bin = ceil( $self->row_count / $value );
    }
    elsif ($method =~ /member_count/i) {
        $bin = $value;
    }
    else {
        croak "Illegal bin method [$method] - must be bin_count/member_count";
    }
    $self->{'rows_per_bin'} = $bin;

    # In the special case where the bin size is 1, then we should do
    # nothing so return straight away
    return $bin if ($bin == 1);

    my @rows     = $self->rows;
    my @new_rows = ();
    my $ctr      = 0;

    my $ra_last_row = [];
    my $ra_new_row  = [];
    my $ra_range1   = [];
    my $ra_min      = [];
    my $ra_max      = [];

    while (my $row = shift @rows) {
        $ra_last_row = $row;  # we'll need this for processing final row
        $ctr++;

        if ($ctr == 1) {
            # initialise bin row based on first row of new bin
            $ra_new_row = [];
            $ra_range1  = [];
            $ra_min     = [];
            $ra_max     = [];
            foreach my $ctr (0..($self->col_count-1)) {
                if ($actions[$ctr] eq 'r') {
                    $ra_range1->[$ctr] = $row->[$ctr];
                }
                elsif ($actions[$ctr] eq '+') {
                    $ra_new_row->[$ctr] = $row->[$ctr];
                }
                elsif ($actions[$ctr] eq '1') {
                    $ra_new_row->[$ctr] = $row->[$ctr];
                }
                elsif ($actions[$ctr] eq 'n') {
                    # Do nothing!
                }
                elsif ($actions[$ctr] eq '<') {
                    $ra_min->[$ctr] = $row->[$ctr];
                }
                elsif ($actions[$ctr] eq '>') {
                    $ra_max->[$ctr] = $row->[$ctr];
                }
                else {
                    croak 'Illegal column action [' . $actions[$ctr] .
                          "] in bin()";
                }
            }
        }
        elsif ($ctr % $bin == 0) {
            # Finalise processing and write out the completed record
            foreach my $ctr (0..($self->col_count-1)) {
                if ($actions[$ctr] eq 'r') {
                    $ra_new_row->[$ctr] = $ra_range1->[$ctr] .'-'. $row->[$ctr];
                }
                elsif ($actions[$ctr] eq '+') {
                    $ra_new_row->[$ctr] += $row->[$ctr];
                }
                elsif ($actions[$ctr] eq '1') {
                    # Do nothing!
                }
                elsif ($actions[$ctr] eq 'n') {
                    $ra_new_row->[$ctr] = $row->[$ctr];
                }
                elsif ($actions[$ctr] eq '<') {
                    $ra_min->[$ctr] = $row->[$ctr] 
                        if ($ra_min->[$ctr] > $row->[$ctr]);
                    $ra_new_row->[$ctr] = $ra_min->[$ctr];
                }
                elsif ($actions[$ctr] eq '>') {
                    $ra_max->[$ctr] = $row->[$ctr] 
                        if ($ra_max->[$ctr] < $row->[$ctr]);
                    $ra_new_row->[$ctr] = $ra_max->[$ctr];
                }
                else {
                    croak 'Illegal column action [' . $actions[$ctr] .
                          "] in bin()";
                }
            }
            push @new_rows, $ra_new_row;
            $ctr = 0;
        }
        else {
            # Process this row
            foreach my $ctr (0..($self->col_count-1)) {
                if ($actions[$ctr] eq 'r') {
                    # Do nothing!
                }
                elsif ($actions[$ctr] eq '+') {
                    $ra_new_row->[$ctr] += $row->[$ctr];
                }
                elsif ($actions[$ctr] eq '1') {
                    # Do nothing!
                }
                elsif ($actions[$ctr] eq 'n') {
                    # Do nothing!
                }
                elsif ($actions[$ctr] eq '<') {
                    $ra_min->[$ctr] = $row->[$ctr] 
                        if ($ra_min->[$ctr] > $row->[$ctr]);
                    $ra_new_row->[$ctr] = $ra_min->[$ctr];
                }
                elsif ($actions[$ctr] eq '>') {
                    $ra_max->[$ctr] = $row->[$ctr] 
                        if ($ra_max->[$ctr] < $row->[$ctr]);
                    $ra_new_row->[$ctr] = $ra_max->[$ctr];
                }
                else {
                    croak 'Illegal column action [' . $actions[$ctr] .
                          "] in bin()";
                }
            }
        }
    }

    # Add on final category
    if ($ctr != 0) {
        # Finalise processing and write out the completed record
        foreach my $ctr (0..($self->col_count-1)) {
            if ($actions[$ctr] eq 'r') {
                # Consider one or both could be >X format or there could
                # be a single row or it could be multiple rows:
                if ($ra_range1->[$ctr] =~ /^>/) {
                    $ra_new_row->[$ctr] = $ra_range1->[$ctr];
                }
                elsif ($ra_last_row->[$ctr] =~ /^>/) {
                    $ra_new_row->[$ctr] = '>=' . $ra_range1->[$ctr];
                }
                elsif ($ra_range1->[$ctr] eq $ra_last_row->[$ctr]) {
                    $ra_new_row->[$ctr] = $ra_range1->[$ctr];
                }
                else {
                    $ra_new_row->[$ctr] = $ra_range1->[$ctr] .'-'.
                                          $ra_last_row->[$ctr];
                }
            }
            elsif ($actions[$ctr] eq '+') {
                # Do nothing!
            }
            elsif ($actions[$ctr] eq '1') {
                # Do nothing!
            }
            elsif ($actions[$ctr] eq 'n') {
                $ra_new_row->[$ctr] = $ra_last_row->[$ctr];
            }
            elsif ($actions[$ctr] eq '<') {
                # Do nothing!
            }
            elsif ($actions[$ctr] eq '>') {
                # Do nothing!
            }
            else {
                croak 'Illegal column action ['. $actions[$ctr] ."] in bin()";
            }
        }
        push @new_rows, $ra_new_row;
    }

    # Put new rows back into data structure
    $self->{rows} = \@new_rows;
    
    # Always return bin size.
    return $bin;
}


sub add_percentage {
    my $self = shift;
    my $idx  = shift || 1;

    croak "You can only add_percentage() if column [$idx] is a number"
        unless ($self->{cols}->[$idx]->[2] =~ /^number$/i);
    croak 'Cannot trim ' . $self->name . " because it has no data"
        if ($self->row_count == 0);

    qlogprint( "Adding percentage on field [$idx] to " . $self->name . "\n" )
        if $self->verbose;

    # Add new column
    my $new_label = ($self->cols)[$idx]->[1].' %';
    $self->add_col( $self->col_count, $new_label, 'number' );
    my $new_idx = $self->col_count-1;

    # Populate new column
    my $total = $self->_total($idx);
    foreach my $row (@{$self->{rows}}) {
       $row->[$new_idx] = $row->[$idx] / $total;
    }
}


sub add_cumulative_percentage {
    my $self = shift;
    my $idx  = shift || 1;

    croak "You can only add_cumulative_percentage() if column [$idx] is a number"
        unless ($self->{cols}->[$idx]->[2] =~ /^number$/i);
    croak 'Cannot trim ' . $self->name . " because it has no data"
        if ($self->row_count == 0);

    qlogprint( "Adding cumulative percentage on field [$idx] to " .
               $self->name . "\n" ) if $self->verbose;

    # Add new column
    my $new_label = ($self->cols)[$idx]->[1].' cumul%';
    $self->add_col( $self->col_count, $new_label, 'number' );
    my $new_idx = $self->col_count-1;

    # Populate new column
    my $total = $self->_total($idx);
    my $cumul_total = 0;
    foreach my $row (@{$self->{rows}}) {
       $cumul_total += $row->[$idx];
       $row->[$new_idx] = $cumul_total / $total;
    }
}


sub drop_col {
    my $self = shift;
    my $idx  = shift || 0;

    # Check that it's a valid column
    if ($idx >=0 and $idx <= ($self->col_count - 1)) {
        delete $self->{cols}->[$idx];
        foreach my $ctr (0..($self->row_count-1)) {
            delete $self->{rows}->[$ctr]->[$idx];
        }
    }
    else { 
        qlogprint( "Illegal column [$idx] specified for drop_col" );
        return undef;
    }
}


sub _total {
    my $self = shift;
    my $idx  = shift || 1;

    # Calculate the total 
    my $total = 0;
    foreach my $row ($self->rows) {
       #print 'adding ', $row->[$idx], " to $total gives ";
       $total += $row->[$idx];
       #print "$total\n";
    }

    return $total;
}


1;
__END__


=head1 NAME

QCMG::Google::DataTable - Data Container for Google Charts


=head1 SYNOPSIS

 use QCMG::Google::DataTable;

 my $dt = QCMG::Google::DataTable->new( name => 'table1', verbose => 0 );
 $dt->add_col( 'col_A', 'Library', 'string');
 $dt->add_col( 'col_B', 'On-target %', 'number');
 $dt->add_col( 'col_C', 'Off-target %', 'number');

 $dt->add_row( 'APGI_1992_ND', 36.2, 63.8 );
 $dt->add_row( 'APGI_1992_TD', 37.4, 62.6 );
 $dt->add_row( 'APGI_2017_ND', 48.7, 51.3 );

 print $dt->as_javascript_literal;


=head1 DESCRIPTION

This module provides an abstract representation of the primary data
container for Google Charts - the DataTable.  It includes a method that
will output a block of Javascript that defines a
google.visualization.DataTable() javascript variable with the name of
the QCMG::Google::DataTable object.  See also:

 http://code.google.com/apis/chart/interactive/docs/reference.html#DataTable
 http://code.google.com/apis/chart/interactive/docs/reference.html#dataparam


=head1 PUBLIC METHODS

=over

=item B<new()>
 
 my $cc = QCMG::Google::DataTable->new( name => 'data1', verbose => 0 );

Takes (optional) name and verbose parameters.

=item B<add_col()>
 
 $dt->add_col( 'col_A', 'Library', 'string');

This method takes 3 values that represent the id, label and type of the
column.  id is a string used to identify the column in any
columne-related operations, label is the string that will be output for
this column on a Google Chart, and type is the sort of values that are
contained in the column and should be on of I<string>, I<number> or
I<date>.

=item B<add_row()>
 
 $dt->add_row( 'APGI_1992_ND', 36.2, 63.8 );

This method takes as many values as there are columns in the data table.

=item B<col_count()>

=item B<row_count()>

=item B<verbose()>

=item B<as_javascript_literal()>

Takes a single mode value (currently must be I<1>) and returns a block
of javascript that deinfes a variable with the name of the DataTable,
a type of google.visualization.DataTable(), and with the columns and rows
as defined in this object.

=item B<add_percentage()>

 $dt->add_percentage();
 $dt->add_percentage(2);

Takes the name of an existing (numerical) column and adds a new column
that contains a number representing the percentage that this row, for the
named column, is of the whole.  If no column is specified, 1 is assumed.

=item B<add_cumulative_percentage()>

 $dt->add_cumulative_percentage();
 $dt->add_cumulative_percentage(2);

Takes the name of an existing (numerical) column and adds a new column
that contains a number representing the cumulative percentage that this
row and all prceeding rows, for the named column, is of the whole.  If
no column is specified, 1 is assumed.

=item B<trim()>

 $dt->trim( 0.99 );
 $dt->trim( 0.99, 2 );
 $dt->trim( 0.99, 2, '.+-' );

This method will reduce the number of rows in a datatable by truncating
it based on a specified maximum cumulative value for a specified field
(default=1).  This is particularly useful if you have a field that
repesents a percentage but your data table has a long tail that you
would like to truncate.  The default column used for the cumulative
total is 1 but you can supply an optional second parameter to trim if
you'd like to use a column beyond 1.  The datatable must have a cateory
string in column 0 for trim to work properly.  When the table is
trimmed, a "the rest" data record is appended.  The 0th field of this
record will have a string ">X" where X is the specified max and all
remaining numeric fields will have the sum of the values for that column
across all of the records to be trimmed.

This method can also take an optional third parameter (in which case the
second parameter becomes mandatory) that tells the method how to process
the columns of the table during truncation.  The string must contain a
character for each column where those characters come from the set:

=over

B<_> : (underscore) make the column empty in the truncation record

B<+> : (plus) add all values from the truncated rows and place into the
truncation record (only relevant for numerics)

B<.> : (period) take the value of the first trimmed record and ignore the
content in this column in subsequent records

B<1> : (one) set the value to 1 - useful for columns created using the
B<add_cumulative_percentage()> method where the cumulative total should
be 1 after all the trimmed records are considered

=back

If the column actions parameter is not supplied, a default will be
created where all number fields are added (B<+>) and all other fields
are made blank (B<_>).

=item B<bin()>

 $dt->bin();
 $dt->bin( 50 );
 $dt->bin( 50, 'bin_count' );
 $dt->bin( 3, 'member_count' );
 $dt->bin( 3, 'member_count', 'r+n' );

Binning reduces the number of rows in a datatable by condensing multiple
rows into a single row.  There are 2 methods of doing this - specify the
maximum number of bins you would like (B<bin_count>) or the number of
rows to place into each bin (B<member_count>).  Note that the binning
will always place the same number of rows into each bin (except possibly
the last) so B<bin_count> only allows you to specify the maximum and it
is quite likely that you will get fewer rows. For example, if you have
90 rows and you specify a B<bin_count> of 40, you will get 30 bins because
3 rows per bin the largest bin that returned 40 or less bins.

As with trimming, you can specify an optional string that specifies how
the bin method handles the aggregation of the data in each column.  The
valid characters are shown below:

=over

B<r> : (r) separate first and last value strings with a '-' to create a
range.

B<+> : (plus) add all values (only relevant for numerics)

B<1> : (one) take the value of the first element

B<n> : (n) take the value of the last element - this is particularly
useful for columns that contain cumulative percentages (as created by
B<add_cumulative_percentage()> method).

B<\<> (lt) take the value of the smallest element

B<\>> (gt) take the value of the largest element

=back

=item B<drop_col()>

 $dt->drop_col(2);

Removes a column from the datatable.  This is particularly useful in
cases where add percentage or cumulative percentage columns which you
then use for trimming or binning but you don't want in the final
display.  It drops the column definition and all of the row values.

=item B<add_format()>

 $dt->add_format(2,'%.4f');

Adds an sprintf-style format for outputting numeric fields.  The intent
for this method was to allow for trimming of high-precision floating
numbers which look awkward in a text table.  Note that it applies the
formatting to the number in the javascript datatable so don't use this
to do text-ish processing on the numbers or they won't look like numbers
to the Google Chart javascript.  Stick to trimming digits.

=back


=head1 SEE ALSO

=over

=item QCMG::Google::Chart::*

=back


=head1 AUTHORS

=over

=item John Pearson L<mailto:j.pearson@uq.edu.au>

=back


=head1 VERSION

$Id$


=head1 COPYRIGHT

Copyright (c) The University of Queensland 2011-2014

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
