package QCMG::BaseClass::Config;

=head1 NAME

QCMG::BaseClass::Config - Load and save configuration in a standard format

=head1 AUTHORS

=over 3

=item Qinying Xu (Christina) (q.xu@imb.uq.edu.au)

=cut


=head1 SYNOPSIS

  # use this module
  use QCMG::BaseClass::Config;

  # this module can be inherited as well
  use Object::InsideOut 'QCMG::BaseClass::Config';

  # load all parameters to the two dimension hash table reference, eg. $conf_para
  my $obj = QCMG::BaseClass::Config->new('fname' => "example.conf"); 
  my $config = {};
  $obj->read_config($config);

  #  Extract the value of a key/value pair from a specified section...
  $config_value = $config{Section_label}{key};

=head1 DESCRIPTION

The configuration language is deliberately simple and limited, 
and the module works hard to handle as much information (section order, comments, etc.)
as possible.

=cut

use Object::InsideOut;
use Carp;

# Attr:
my @f_conf : Field : Arg('Name' => 'fname') : Get(f_conf);


# method
sub read_config {
    my ($self, $hash_ref) = @_;

    # read the configure file information into an array reference
    my $sections = _load_config($f_conf[$$self]);

    # copy information from the array to hash ref
    foreach my $sec (@$sections) { $sec->copy_to($hash_ref) }
}

# vitual method
sub select_parameter { }

sub _load_config {
    use Carp;
    my ($fname) = @_;
    my @sections = ();
    my $seen = {}; # to store section id ref, make sure section id must be unique
    my $seenKey = {}; # to strore parameter key for each section, make sure key name are unque in each section

    # create an empty new section, and add it into an array
    push @sections, QCMG::BaseClass::Config::Section->new(name => q{});

    # read whole configure file into a data
    open(CONF, "$fname") or die "can't open configure file: $fname\n";
    my $text = do { local $/; <CONF> };
    close(CONF);

    my $l_text = length($text);
    pos($text) = 0;
    while (pos($text) < $l_text) {

        # jump over all empty lines
        if ($text =~ m/\G [^\S\n]*(?:\n|\z)+ /gcxms) { next }

        # jump ove all comments line which start as #
        if ($text =~ m/\G \s*[#][^\n]*(?:\n|\z)+  /gcxms) { next }

        # create an section object and add it into array
        if ($text =~ m/\G [^\S\n]*[[][^\S\n]* ([\w\- ]+)[^\S\n]* []] [^\S\n]* (?:\n|\z)+ /gcxms) {
            croak "Error in configure file: $fname -- more than two sections are named [$1]\n"
              unless !exists($seen->{$1});

            # transfer any uppercase in section name to lowercase
            my $sec = $1;
            $sec =~ tr/A-Z/a-z/;

            push @sections, QCMG::BaseClass::Config::Section->new(name => $sec);

            # push section id into $seen hash
            $seen->{$1} = 1;

            # reset $seenKey hash for new section
            $seenKey = {};

            next;
        }

        # add key values into section objects
        # the \" for eg. run_seqlogo= "/data/weblogo/seqlogo -f <input> -F GIF -h 10 -w 30 -k 0 -o <output> -c"
        # the \/ for eg. f2m=/data/cxu/mapping_strategy/matching/f2m.pl
        if (($text =~ m/\G [^\S\n]*([\w\-]+)[^\S\n]*[=:][^\S\n]*([\w\-\.\,\/\\]+)\s*(?:\n|\z)+ /gcxms)
            || ($text =~ m/\G [^\S\n]*([\w\-]+)[^\S\n]*[=:][^\S\n]*\"([^\n]+)\"\s*(?:\n|\z)+ /gcxms)) {
                
            my ($key, $value) = ($1, $2);

            # add the new key and value into recent section object
            if (exists $seenKey->{$key}) {
                my $sec = $sections[-1]->Name();
                croak "Error in configure file $fname -- more than two paramters are named \n\t\t$key\n in section [$sec]\n";
            }

            # transfer any uppercase in parameter name to lowercase
            $key =~ tr/A-Z/a-z/;
            $sections[-1]->add_KeyVal($key, $value);

            # push key name into $seenKey hash
            $seenKey->{$key} = 1;

            # add next value of this key into recent sction object
            while ($text =~ m/\G [^\S\n]*[=:][^\S\n]* ([\w\-\.\,\"\/\\]+)\s*(?:\n|\z)+ /gcxms) {
                $value = $1;
                $sections[-1]->add_KeyVal($key, $value);
            }
            next;
        }

        # find a line with wired information in configure file
        else {
            $text =~ m/\G ([^\n]{1,})  /gcxms;
            croak "Error in configure file: $fname, near:\n$1\n";
        }
    }
    return \@sections;
}

package QCMG::BaseClass::Config::Section;
{

    use Object::InsideOut;

    # Attr:
    my @name_of : Field : Arg('Name' => 'name', 'Mandatory' =>1) :Accessor(Name);
    my @components_of : Field;
    my %seen : Field;

    # methods:
    sub add_KeyVal {
        my ($self, $key, $value) = @_;

        # for exsiting key in @componets_of, push the value to keyval array
        if (exists $seen{$$self}->{$key}) {
            $seen{$$self}->{$key}->multivalue($value);
        }

        # for new key, create an new keyVal instant and push it to components_of
        else {
            my $keyval = QCMG::BaseClass::Config::KeyVal->new(
                'key'   => $key,
                'value' => $value
            );
            push @{$components_of[$$self]}, $keyval;
            $self->set(\%seen, {$key => $keyval});
        }
    }

    sub copy_to {
        my ($self, $hash_ref) = @_;

        # create an hash ref to stroe section parameters
        my $keyvals = {};

        # add section name as key into hash_ref, which point to the new hash table $keyvals
        $hash_ref->{$name_of[$$self]} = $keyvals;

        # add section parameters into the new hash table
        # here each $comp point to a QCMG::BaseClass::Config::KeyVal class
        foreach my $comp (@{$components_of[$$self]}) {
            $comp->copy_to($keyvals);
        }
    }

}
1;

package QCMG::BaseClass::Config::KeyVal;
{
    use Object::InsideOut;

    # Attr:
    my @key_of : Field : Arg('Name' => 'key', 'Mandatory' => 1);
    my @first_value : Field : Arg('Name' => 'value', 'Mandatory' => 1);    # store single value here
    my @vals : Field;    # if have multi value, then store all value here

    # methods:
    sub _init : Init {
        my ($self, $args) = @_;
        push @{$vals[$$self]},
          $first_value[$$self];    # push the first value into an array
    }

    sub copy_to {
        my ($self, $hash_ref) = @_;

        if (scalar(@{$vals[$$self]}) > 1) {
            $hash_ref->{$key_of[$$self]} = \@{$vals[$$self]};
        }
        else { $hash_ref->{$key_of[$$self]} = $first_value[$$self] }

    }

    sub multivalue {
        my ($self, $value) = @_;
        push @{$vals[$$self]}, $value;

    }

}
1;
__END__

=back

=head2 Configuration language

=head3 Comments

A comment starts with a C<#> character and runs to the end of the same line:

    #  This is a comment

Comments can be placed almost anywhere in a configuration file, except inside
a section label, or in the key or value of a configuration variable:

    #  Valid comment
    [ # Not a comment, just a weird section label ]

    #  Valid comment
    key: value  # Not a comment, just part of the value

=head3 Sections

A configuration file consists of one or more I<sections>, each of which is introduced by a label in square brackets. The restriction on selection lable is that they must be by themselve on a single line; and it only allow alphanumeric, whitespace, '_' and '_'. The section label must be unique, it will treat the label with differnt case and contain whitespace as same, see example below:

    [SECTION 1]       # OK, it is equal to [section 1]
	
    [Section 1]       # ERROR: it is equal to [section 1], since it is not case senstive. 

    [ SECTION 1 ]     # ERROR: it is equal to [section 1], since it ignor the whitspace next to square brackets 


=head3 Configuration variables

Each non-empty line within a section must consist of the specification of a I<configuration variable>. Each such variable consists of a key and a data value. The key/value separator can be an equals sign; the key only allow alphanumeric and '_', it is case insensitive; The value is with flexible formart: 1) it suggest to only have alphnumeric and '_'; 2) it allows specila letter '\','/' and '.' for file or program name and location; 3)oher special letters are allow but must add a quotation mark  on the value if it contain other special letters; and 4)it is case senstive.

    name = George  
     age = 47
    email = "q.xu@imb.uq.edu.au"
    fold = /data/imb/

Note: 

1)you can't put a comment on the same line as a configuration variable; 

2)each key value must be unique in each session.


=head3 Multi-line configuration values

A single value can be continued over two or more lines. If the line immediately after a configuration variable starts with the separator character used in the variable's definition, then the value of the variable is then a list. For example:

	Children = Bairu
		 = Olivia
		 = Other


the corresponding value of the C<'address'> configuration variable is: C<S<['Bairu', 'Olivia', 'Other']>>

=head3 An example

    #  A new section...
    [SECTION 1]
    #  A simple key (just an identifier)...
    simple = simple_value

    #  Several values for the same key...
    multi-value = "this is value 1"
    		= "this is value 2"
    		= "this is value 3"

    [section 2]
  
    simple = SIMPLE_value
    COMPLEX = /data/people/complex.txt

The above configuration would be read into a hash whose internal structure looked like this:

    {
       'section 1' => {
          'simple'           => 'simple_value',
          'multi-value' => [ 'this is value 1', 'this is value 2', 'this is value 3']
        }
	'section 2' => {
          'simple'           => 'SIMPLE_value',
    	  'complex' = '/data/people/complex.txt'
	}
	
    }


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
