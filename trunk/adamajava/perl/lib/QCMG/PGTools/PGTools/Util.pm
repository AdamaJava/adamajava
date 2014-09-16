package PGTools::Util;

use strict;
use List::Util qw/shuffle/;
use Exporter;
use Data::Dumper;
use FindBin;
use IO::File;
use Mojo::JSON;
use Cwd qw/abs_path/;
use Fcntl qw/:flock/;
use File::Slurp qw/read_file/;
use IO::File;
use Text::CSV;
use LWP::UserAgent;
use Carp qw/ croak confess/;

use base 'Exporter';

use File::Path qw/
  make_path
  remove_tree
/;


our @EXPORT = qw/
  abs_path
  croak
  confess
  foreach_csv_row
  strip_comments
  download
  randomize 
  defined_and_exists
  create_unless_exists
  normalize
  extension
  exit_with_message
  run_parallel
  run_serial
  parts
  directory
  file
  file_without_extension
  files_within_directory
  run_command
  _run_command
  run_pgtool_command
  trim
  number
  read_bed_file
  must_have
  must_have_extension
  must_be_defined
  debug
/;


sub randomize {
  my ( $text ) = @_;
  join( '', shuffle( split //, $text ) );
}

sub defined_and_exists {
  my ( $file ) = @_;

  $file && -e $file;
}

sub normalize {
  my ( $text, $eol ) = @_;
  join "\n", ( $text =~ /(.{0,60})/g );
}

sub extension {
  my $file = shift;

  my ( $ext ) = $file =~ /\.(.*)$/;

  $ext;
}

sub parts {
  my $file = shift;
  my ( $dir, $filename ) = $file =~ /^(?:(.*)\/)?([^\/]+)$/;

  return ( $dir, $filename );

}

sub directory {
  ( parts shift )[0];
}

sub file {
  ( parts shift )[1];
}

sub file_without_extension {
  my $file = file shift;

  my ( $name ) =  $file =~ /(.*)\.[^\.]+$/;

  $name;
}


sub exit_with_message {
  my $message = shift;

  print "$message \n";
  exit 1;
}

sub run_parallel {
  print "Running ... \n";

  my @commands = @_;
  my @pids = ();

  for ( @commands ) {

    next unless ref eq 'CODE';

    my $pid = fork;
    unless( $pid ) {
      $_->();
      exit 0;
    }

    else {
      push @pids, $pid;
    }

  }

  1 while wait > 0;
}


sub run_serial {
  print "Running ... \n";

  my @commands = @_;
  my @pids = ();

  for ( @commands ) {

    next unless ref eq 'CODE';

    my $status = $_->();

    confess "Failed: $! " unless $status;

  }

}


sub create_unless_exists {
  my $directory = shift;

  make_path $directory unless -d $directory;

}

sub run_command {
  my ( $command, $output_handlers, %attrs ) = @_;

  my %options = (
    command     => $command,
    pid         => undef, 
    attrs       => \%attrs,
    label       => ( $attrs{label} || $command ),
    exit_status => undef 
  );


  write_command_status( %options ) if $ENV{PGTOOLS_RUN_STATUS_FILE};

  @options{ qw/pid exit_status/ } = _run_command( @_ );

  write_command_status( %options ) if $ENV{PGTOOLS_RUN_STATUS_FILE};

  $options{exit_status} == 0;

}

sub _run_command {
  my $command         = shift;
  my $output_handler  = shift;
  my %attrs           = @_; 

  my $handler = sub { };
  my $valid_handler_given = ref( $output_handler ) eq 'CODE';

  if( $ENV{ PGTOOLS_DEBUG } && !$valid_handler_given ) { 
    $output_handler = sub { print @_; }
  } 

  elsif( ! $valid_handler_given ) {
    $output_handler = $handler;
  }

  print "Running: $command - PID - $$ \n";

  my $pid = open my $output, "$command |";

  # don't print anything
  $output_handler->( $_ ) while <$output>;

  # close the output file
  close $output;

  # return pid and exit status
  ( $pid, $? );

}

sub write_command_status {
  my %options   = @_;
  my $file      = $ENV{PGTOOLS_RUN_STATUS_FILE};
  my $semaphore = "$file.lock";


  {

    # wait while the file still exists
    while( -e $semaphore ) {
      sleep 1;
    } 

    # open a semaphore file
    open my $sm, '>', $semaphore or die( "Cant open semaphore for writing!" );

    my $contents;
    my $json = Mojo::JSON->new;

    if( -e $file ) {
      $contents = $json->decode( scalar( read_file( $file ) ) );
    } else {
      $contents = { };
    }

    # open the status file
    open my $fh, '>', $file or die( "Cant open file for writing: " . $! );

    # update the structure
    $contents->{ $options{label} } = \%options;

    # write it back
    print $fh $json->encode( $contents );

    # remove lock and close
    close $sm;
    close $fh;

    # remove file
    unlink $semaphore;

  }

}



sub run_pgtool_command {

  my $command = shift;
  my @others  = @_;

  # print $command, "\n";
  # return 0;

  my $status;
  unless( $status = run_command( "perl $0 $command", sub { }, @others ) ) {
    confess( $! );
  } 

  return $status;

}


sub trim {
  my $string = shift;

  $string =~ s/^\s+//;
  $string =~ s/\s+$//;

  $string;
}


sub files_within_directory {
  my $path = shift;

  

}

sub number { }

sub strip_comments {
  my ( $text ) = @_;

  # strip comments
  $text =~ s/^\s*#.*$//gm;

  $text;
}

sub read_bed_file {
  my $file = shift;

  my $fh = IO::File->new( $file, 'r') 
    or confess( "Can't open the BED File" );

  my @data;
  while( my $line = $fh->getline ) {

    my ( $chromosome, $start, $end, $type, $score, $strand, @others ) = split /\s+/, $line; 

    push @data, {
      chromosome  => $chromosome,
      type        => $type,
      start       => $start,
      end         => $end,
      score       => $score,
      strand      => $strand,
    };

  }

  \@data

}


sub must_have {
  my ( $name, $path ) = @_;
  die " $name doesn't exist " unless defined_and_exists $path;
}

sub must_have_extension {
  my ( $extension, $path ) = @_;

  die "$path must have extenstion: $extension"
    unless $path =~ /$extension$/;

}

sub must_be_defined {
  my ( $name, $path ) = @_;

  die "$name not given " unless defined( $path );
}

sub debug {
  unless( $ENV{PGTOOLS_NO_DEBUG} ) {
    print "$_ \n" for @_;
  }
}


sub download {
  my ( $url, $path ) = @_;

  my $ua = LWP::UserAgent->new;

  print "Downloading: $url \n";

  my $res = $ua->get( $url  );

  if( $res->is_success ) {

    my $fh  = IO::File->new( $path, 'w' );

    print "Saving ...\n";

    $fh->printflush( $res->decoded_content );
    $fh->close;

  } else {

    die "
      The download of $url failed.
    ";

  }


}

# assumes first row is columns
sub foreach_csv_row {
  my ( $file, $handler ) = @_;

  confess "File does not exist: $file " 
    unless -e $file;

  confess "No handler given" 
    unless ref( $handler ) eq 'CODE';

  my $fh  = IO::File->new( $file, 'r' );
  my $csv = Text::CSV->new;

  $csv->column_names( 
    $csv->getline( $fh )
  );

  while( my $row = $csv->getline_hr( $fh ) ) {
    $handler->( $row );
  }

  $fh->close;

}

sub check_dependencies {
}


1;
__END__

