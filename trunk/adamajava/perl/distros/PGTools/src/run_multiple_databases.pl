use strict;
use Mojo::JSON;
use FindBin;
use File::Slurp qw/read_file/;
use File::Temp qw/tempfile/;
use IO::File;
use Data::Dumper;
use File::Spec::Functions;
use PGTools::Util qw/
  create_unless_exists
  file_without_extension
  run_pgtool_command
  strip_comments
/;

my $json        = Mojo::JSON->new;
my $config_file = "src/config.json";

die "File Doesn't exist" unless -e $config_file;


my $content = strip_comments( scalar( read_file( $config_file ) ) );
my $config      = $json->decode( $content ); 

# prefix that will used as container for
# pgtool scratch paths
my $prefix      = 'output2';


# all databases go here
my @databases = qw(
  /home/venkat/bio/phaseII/final_dbs/NONCODE.SIEVED.UNIQUE.DIGESTED.REFSEQ.ENS.SP.combined.proteinDB.fa
  /home/venkat/bio/phaseII/final_dbs/PG.SIEVED.UNIQUE.REFSEQ.ENS.SP.combined.proteinDB.fa
  /home/venkat/bio/phaseII/final_dbs/SPLICEDB.UNIQUE.SIEREFSEQ.ENS.digested.pepseq.fa
  /home/venkat/bio/phaseII/final_dbs/UTRDB.fa
);

# the input file
my $input_file = 'tcga.test.mgf';

create_unless_exists $prefix;

for my $database ( @databases ) {

  next if $database =~ /#/;

  print $database, "\n";
  # prepare new config
  $config->{msearch}{database} = $database;

  # write new config
  my ( $fh, $filename ) = tempfile( 'configXXXXXX', SUFFIX => '.json' );
  print $fh $json->encode( $config );

  # set the scratch path
  my $new_path = catfile( $prefix, file_without_extension( $database ) );
  create_unless_exists $new_path;

  $ENV{ PGTOOLS_SCRATCH_PATH } = $new_path;
  $ENV{ PGTOOLS_CONFIG_PATH } = $filename;

  # run stuff
  if( -e $input_file ) {
    system "./src/pgtools phase2 $input_file ";
  }

}



