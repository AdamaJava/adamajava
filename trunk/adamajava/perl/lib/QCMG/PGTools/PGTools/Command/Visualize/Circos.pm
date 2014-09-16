package PGTools::Command::Visualize::Circos;

use strict;
use PGTools::Util;
use PGTools::Util::Path;
use PGTools::Configuration;
use File::Temp qw/tempfile/;
use File::Basename qw/dirname/;
use File::Spec::Functions;
use Data::Dumper;
use Cwd;
use parent 'PGTools::Command';

sub run
{
  my $class = shift;
  my $config = PGTools::Configuration->new->config;
  my $options = shift;
  my $max_plots = 8;

  die "No Circos configuration found, can't visualize"
    unless $config->{circos_path};

  die "Circos path doesn't exist, please modify 'circos_path' in config.json"
    unless -d $config->{circos_path};


  my $circos_config = join '', <DATA>;

  my $output_dir = $options->{output} || dirname( 
    ( grep { $_ } map { $options->{ "plot$_"} } ( 1 .. $max_plots ) )[ 0 ] || Cwd::getcwd() 
  ); 

  # prepare plots
  my $plots = "
    outputdir   = $output_dir
    <plots> 
      type            = tile
      layers_overflow = hide
  ";

  # must be auto generated
  # for possibly several more plots
  my @bg = ( '' );
  my $plot_count = 0;
  my @radii = $class->get_radii;
  my @stroke_colors = $class->get_stroke_colors;


  for my $plot_id (  1 .. $max_plots  ) {

    my $plot = $options->{"plot$plot_id"};
    my @radius = @{ shift @radii };
    my $stroke_color = shift @stroke_colors;

    my $plot_file;
    if( $class->is_bed_file( $plot ) ) {
      $plot_file = $class->prepare_input_file( $plot );
    }

    else {
      $plot_file = $plot;
    }



    if( -e $plot ) {
      $plot_count++;

      my $plot_config = "
        <plot>
          file        = $plot 
          r1          = $radius[ 0 ] 
          r0          = $radius[ 1 ] 
          orientation = out

          layers      = 15
          margin      = 0.02u
          thickness   = 15
          padding     = 8

          stroke_thickness = 1
          stroke_color     = $stroke_color 

          <backgrounds>
            <background>
              color = vvlgrey
            </background>
          </backgrounds>

        </plot>

      ";

      $plots .= $plot_config;
    }


  } 

  $plots .= "
    </plots>
  ";

  $circos_config .= $plots;

  my ( $fh, $config_file ) = tempfile( SUFFIX => '.conf' );

  # get the config file ready
  print $fh $circos_config;

  # run circos
  run_command "perl @{[ $config->{circos_path} ]}/bin/circos -conf $config_file";

  print "DONE \n";

}


sub prepare_input_file {
  my ( $class, $file ) = @_;

  my ( $fh, $filename ) = tempfile( SUFFIX => '.txt' );
  my $entries = read_bed_file( $file );

  for my $entry ( @$entries ) {
    print $fh sprintf( "%s\t%d\t%d\n", map { s/chr/hs/g; } @{ $entry }{ qw/chromosome start end/ } );
  }

  close( $fh );

  $filename;
}

sub is_bed_file {
  my ( $class, $filename ) = @_;

  $filename =~ /\.bed$/;

}

sub get_radii {
  my $class       = shift;
  my $items       = shift || 8;
  my $start       = 98;
  my $width       = 12;
  my $gap         = 2;

  # initialize
  my @radii = ( );
  for my $plot ( 1 .. $items ) {

    push @radii, [
      $class->to_r( $start ),
      $class->to_r( $start - $width )
    ];

    # reset start
    $start -= ( $width - $gap );

    # increase gap
    $gap++;

  }

  @radii;

}

sub to_r {
  my ( $class, $value ) = @_;
  sprintf( '%.2f', ( $value / 100 ) );
}

sub get_stroke_colors {
  my @primary = qw/
      yellow
      red 
      blue
      green
      purple
      grey
      orange
  /;

  my @prefixes = (
    qw/vl vvl vd vvd l d/, ''
  );

  my @colors = ( );

  for my $prefix ( @prefixes ) {
    for my $primary ( @primary ) {
      push @colors, $prefix . $primary;
    }
  }

  @colors;

}


1;
__DATA__

<<include etc/colors_fonts_patterns.conf>>

<ideogram>

<spacing>
default = 0.01r
break   = 0.5r
</spacing>

radius           = 0.775r
thickness        = 30p
fill             = yes
fill_color       = black
stroke_thickness = 2
stroke_color     = black
show_label       = yes
label_font       = default
label_radius     = dims(image,radius)-30p
label_size       = 24
label_parallel   = yes
label_case       = lower
label_format     = eval(sprintf("chr%s",var(label)))

show_bands            = yes
fill_bands            = yes
band_stroke_thickness = 2
band_stroke_color     = white
band_transparency     = 0

radius*       = 0.825r

</ideogram>


show_ticks          = yes
show_tick_labels    = no

<ticks>

radius           = dims(ideogram,radius_outer)
orientation      = out
label_multiplier = 1e-6
color            = black
size             = 20p
thickness        = 3p
label_offset     = 5p

<tick>
spacing        = .1u
show_label     = no
</tick>

<tick>
spacing        = .5u
show_label     = yes
label_size     = 20p
format         = %d
</tick>

<tick>
spacing        = 1u
show_label     = yes
label_size     = 24p
format         = %d
</tick>

</ticks>


<image>
<<include etc/image.conf>>
</image>

karyotype   = data/karyotype/karyotype.human.txt

chromosomes_units           = 1000000
chromosomes_display_default = yes

<<include etc/housekeeping.conf>>

