package Mojo::Base;

use strict;
use warnings;

# Mojo modules are modern!
require feature if $] >= 5.010;

# No imports because we get subclassed, a lot!
require Carp;

# "Kids, you tried your best and you failed miserably.
#  The lesson is, never try."
sub import {
  my $class = shift;
  return unless my $flag = shift;

  # No limits!
  no strict 'refs';
  no warnings 'redefine';

  # Base
  if ($flag eq '-base') { $flag = $class }

  # Module
  else {
    my $file = $flag;
    $file =~ s/::|'/\//g;
    require "$file.pm" unless $flag->can('new');
  }

  # ISA
  my $caller = caller;
  push @{"${caller}::ISA"}, $flag;

  # Can haz?
  *{"${caller}::has"} = sub { attr($caller, @_) };

  # Mojo modules are strict!
  strict->import;
  warnings->import;

  # Mojo modules are modern!
  feature->import(':5.10') if $] >= 5.010;
}

sub new {
  my $class = shift;
  bless
    exists $_[0] ? exists $_[1] ? {@_} : {%{$_[0]}} : {},
    ref $class || $class;
}

# Performance is very important for something as often used as accessors,
# so we optimize them by compiling our own code, don't be scared, we have
# tests for every single case
sub attr {
  my $class   = shift;
  my $attrs   = shift;
  my $default = shift;

  # Check arguments
  Carp::croak('Attribute generator called with too many arguments') if @_;
  return unless $class && $attrs;
  $class = ref $class || $class;

  # Check default
  Carp::croak('Default has to be a code reference or constant value')
    if ref $default && ref $default ne 'CODE';

  # Create attributes
  $attrs = [$attrs] unless ref $attrs eq 'ARRAY';
  my $ws = '  ';
  for my $attr (@$attrs) {

    Carp::croak(qq/Attribute "$attr" invalid/)
      unless $attr =~ /^[a-zA-Z_]\w*$/;

    # Header
    my $code = "sub {\n";

    # No value
    $code .= "${ws}if (\@_ == 1) {\n";
    unless (defined $default) {

      # Return value
      $code .= "$ws${ws}return \$_[0]->{'$attr'};\n";
    }
    else {

      # Return value
      $code .= "$ws${ws}return \$_[0]->{'$attr'} ";
      $code .= "if exists \$_[0]->{'$attr'};\n";

      # Return default value
      $code .= "$ws${ws}return \$_[0]->{'$attr'} = ";
      $code .=
        ref $default eq 'CODE'
        ? '$default->($_[0])'
        : '$default';
      $code .= ";\n";
    }
    $code .= "$ws}\n";

    # Store value
    $code .= "$ws\$_[0]->{'$attr'} = \$_[1];\n";

    # Return invocant
    $code .= "${ws}\$_[0];\n";

    # Footer
    $code .= '};';

    # We compile custom attribute code for speed
    no strict 'refs';
    no warnings 'redefine';
    *{"${class}::$attr"} = eval $code;

    # This should never happen (hopefully)
    Carp::croak("Mojo::Base compiler error: \n$code\n$@\n") if $@;

    # Debug mode
    if ($ENV{MOJO_BASE_DEBUG}) {
      warn "\nATTRIBUTE: $class->$attr\n";
      warn "$code\n\n";
    }
  }
}

1;
__END__

=head1 NAME

Mojo::Base - Minimal Base Class For Mojo Projects

=head1 SYNOPSIS

  package Cat;
  use Mojo::Base -base;

  has 'mouse';
  has paws => 4;
  has [qw/ears eyes/] => 2;

  package Tiger;
  use Mojo::Base 'Cat';

  has stripes => 42;

  package main;

  my $mew = Cat->new(mouse => 'Mickey');
  print $mew->paws;
  print $mew->paws(5)->paws;

  my $rawr = Tiger->new(stripes => 23);
  print $rawr->ears * $rawr->stripes;

=head1 DESCRIPTION

L<Mojo::Base> is a simple base class for L<Mojo> projects.

=head1 FUNCTIONS

L<Mojo::Base> exports the following functions if imported with the C<-base>
flag or a base class.

  # Automatically enables "strict" and "warnings"
  use Mojo::Base -base;
  use Mojo::Base 'SomeBaseClass';

Both forms save a lot of typing.

  # use Mojo::Base -base;
  use strict;
  use warnings;
  use feature ':5.10';
  use Mojo::Base;
  push @ISA, 'Mojo::Base';
  sub has { Mojo::Base::attr(__PACKAGE__, @_) }

  # use Mojo::Base 'SomeBaseClass';
  use strict;
  use warnings;
  use feature ':5.10';
  require SomeBaseClass;
  push @ISA, 'SomeBaseClass';
  use Mojo::Base;
  sub has { Mojo::Base::attr(__PACKAGE__, @_) }

=head2 C<has>

  has 'name';
  has [qw/name1 name2 name3/];
  has name => 'foo';
  has name => sub {...};
  has [qw/name1 name2 name3/] => 'foo';
  has [qw/name1 name2 name3/] => sub {...};

Create attributes, just like the C<attr> method.

=head1 METHODS

L<Mojo::Base> implements the following methods.

=head2 C<new>

  my $instance = BaseSubClass->new;
  my $instance = BaseSubClass->new(name => 'value');
  my $instance = BaseSubClass->new({name => 'value'});

This base class provides a basic object constructor.
You can pass it either a hash or a hash reference with attribute values.

=head2 C<attr>

  __PACKAGE__->attr('name');
  __PACKAGE__->attr([qw/name1 name2 name3/]);
  __PACKAGE__->attr(name => 'foo');
  __PACKAGE__->attr(name => sub {...});
  __PACKAGE__->attr([qw/name1 name2 name3/] => 'foo');
  __PACKAGE__->attr([qw/name1 name2 name3/] => sub {...});

Create attributes.
An arrayref can be used to create more than one attribute.
Pass an optional second argument to set a default value, it should be a
constant or a sub reference.
The sub reference will be excuted at accessor read time if there's no set
value.

=head1 DEBUGGING

You can set the C<MOJO_BASE_DEBUG> environment variable to get some advanced
diagnostics information printed to C<STDERR>.

  MOJO_BASE_DEBUG=1

=head1 SEE ALSO

L<Mojolicious>, L<Mojolicious::Guides>, L<http://mojolicio.us>.

=cut
